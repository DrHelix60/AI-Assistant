package rsdltj.myplugin.actions.convention;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import rsdltj.myplugin.ai.AiResponseCleaner;
import rsdltj.myplugin.ai.OllamaClient;
import rsdltj.myplugin.settings.OllamaSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConventionAction extends AnAction {

    // Log for possible errors
    private static final Logger LOG = Logger.getInstance(ConventionAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            return;
        }

        // Get the VirtualFile for the current editor
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (file == null || !file.getName().endsWith(".java")) {
            Messages.showWarningDialog(
                    "Please open a Java file first.",
                    "Not a Java File"
            );
            return;
        }

        // Get the selected text (or full file if nothing selected)
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText;
        TextRange selectionRange;

        if (selectionModel.hasSelection()) {
            selectedText = selectionModel.getSelectedText();
            selectionRange = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
        } else {
            // No selection: use entire file
            selectedText = editor.getDocument().getText();
            selectionRange = new TextRange(0, selectedText.length());
        }

        // Validate selection size using settings
        OllamaSettings settings = ApplicationManager.getApplication()
                .getService(OllamaSettings.class);

        int maxChars = settings.maxSelectionChars;
        if (selectedText.length() > maxChars) {
            Messages.showWarningDialog(
                    project,
                    "Selection is too large (" + selectedText.length() + " chars).\n\n" +
                            "Please select at most " + maxChars + " characters for convention checking.\n" +
                            "You can adjust this limit in Settings → Ollama AI → Convention Checker Settings.",
                    "Selection Too Large"
            );
            return;
        }

        // Validate prompt has %s placeholder
        if (!settings.conventionPrompt.contains("%s")) {
            LOG.warn("Convention prompt missing %s placeholder");
            ApplicationManager.getApplication().invokeLater(() ->
                    Messages.showErrorDialog("Settings error: Prompt must contain %s placeholder", "Configuration Error")
            );
            return;
        }

        // STEP 1: Pre-process line length violations in pure Java (fast, deterministic)
        String preProcessedText = fixLineLengthJava(selectedText, settings.maxLineLength);

        // Prepare prompt with max line length value
        String formattedPrompt = settings.conventionPrompt
                .replace("[MAX_LINE_LENGTH]", String.valueOf(settings.maxLineLength))
                .replace("%s", preProcessedText);

        // While it loads we open the thinking window
        rsdltj.myplugin.ui.LoadingMessage loadingDialog =
                rsdltj.myplugin.ui.LoadingMessage.show(project, settings.thinkingMessage);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                OllamaClient.sendPromptAsync(settings.ollamaUrl, settings.modelName, formattedPrompt)
                        .thenAccept(aiResponse -> {
                            rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);

                            if (aiResponse == null || aiResponse.isBlank()) {
                                LOG.warn("Ollama returned empty response for convention check");
                                ApplicationManager.getApplication().invokeLater(() ->
                                        Messages.showErrorDialog("AI returned an empty response.", "Error")
                                );
                                return;
                            }

                            // We clean what the AI sends using our centralized pipeline (trim, Markdown fences, Unicode escapes)
                            String cleanResponse = AiResponseCleaner.clean(aiResponse);

                            // Then check if AI actually made any changes
                            if (cleanResponse.equals(preProcessedText)) {
                                ApplicationManager.getApplication().invokeLater(() ->
                                        Messages.showMessageDialog("No convention violations found.", "Convention Check", Messages.getInformationIcon())
                                );
                                return;
                            }

                            // If the AI does propose changes, we show them to the user
                            ApplicationManager.getApplication().invokeLater(() -> {
                                ConventionResult resultDialog = new ConventionResult(project, preProcessedText, cleanResponse);

                                // If the user clicks "Apply", replace the selection safely
                                if (resultDialog.showAndGet() && resultDialog.isApplySelected()) {
                                    WriteCommandAction.runWriteCommandAction(project, () -> {
                                        // Replace only the selected range (or full document if no selection)
                                        editor.getDocument().replaceString(
                                                selectionRange.getStartOffset(),
                                                selectionRange.getEndOffset(),
                                                cleanResponse
                                        );
                                        // Move cursor to start of changed region for orientation
                                        editor.getCaretModel().moveToOffset(selectionRange.getStartOffset());
                                    });
                                }
                            });

                        })
                        .exceptionally(error -> {
                            LOG.warn("Convention check failed", error);
                            rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Messages.showErrorDialog("Convention check failed. Check logs.", "Error")
                            );
                            return null;
                        });
            } catch (Exception ex) {
                LOG.warn("Unexpected error in ConventionAction", ex);
                rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);
            }
        });
    }

    // Pre-process: fix simple line length violations with pure Java logic
    private String fixLineLengthJava(String code, int maxLen) {
        String[] lines = code.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            // Skip comments, strings, and already-short lines
            if (line.length() <= maxLen || line.trim().startsWith("//") || line.trim().startsWith("*")) {
                result.append(line).append("\n");
                continue;
            }

            // Try to break at natural points
            String broken = breakLongLine(line, maxLen);
            result.append(broken).append("\n");
        }
        return result.toString();
    }

    // Break a long line at natural points (commas, operators, parentheses)
    private String breakLongLine(String line, int maxLen) {
        if (line.length() <= maxLen) return line;

        // Find best break point: prefer after comma, operator, or before argument
        int breakPoint = -1;
        boolean inString = false;
        boolean inChar = false;

        for (int i = 0; i < maxLen; i++) {
            char c = line.charAt(i);
            // Track string/char literals to avoid breaking inside them
            if (c == '"' && (i == 0 || line.charAt(i-1) != '\\')) inString = !inString;
            if (c == '\'' && (i == 0 || line.charAt(i-1) != '\\')) inChar = !inChar;
            if (inString || inChar) continue;

            // Prefer breaking after these tokens
            if (c == ',' || c == '+' || c == '-' || c == '*' || c == '/' ||
                    c == '&' || c == '|' || c == '=' || c == '<' || c == '>') {
                breakPoint = i + 1;
            }
            // Or before these tokens (if no better point found)
            if ((c == '(' || c == '[' || c == '{') && breakPoint == -1) {
                breakPoint = i;
            }
        }

        // If we found a good break point, split the line
        if (breakPoint > 0 && breakPoint < line.length()) {
            String indent = getIndent(line);
            String part1 = line.substring(0, breakPoint).trim();
            String part2 = line.substring(breakPoint).trim();
            return part1 + "\n" + indent + "    " + part2;
        }
        return line; // Fallback: return as-is if no good break point
    }

    private String getIndent(String line) {
        Matcher m = Pattern.compile("^(\\s*)").matcher(line);
        return m.find() ? m.group(1) : "";
    }

    @Override
    public @NotNull com.intellij.openapi.actionSystem.ActionUpdateThread getActionUpdateThread() {
        // Our update() checks are lightweight, so EDT is safe and the fastest
        return com.intellij.openapi.actionSystem.ActionUpdateThread.EDT;
    }

    // With this Override we can ensure the action will be performed independently of the cursor state, as we want to affect the whole file
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        // Only enable if we have an active project and editor
        boolean enabled = project != null && editor != null;

        // Restrict to Java files only
        if (enabled) {
            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            enabled = file != null && file.getName().endsWith(".java");
        }

        e.getPresentation().setEnabledAndVisible(enabled);
    }
}