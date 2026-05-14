package rsdltj.myplugin.actions.commenting;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import rsdltj.myplugin.ai.OllamaClient;

import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;

public class CommentAction extends AnAction {

    //We create a log in case there are errors
    private static final Logger LOG = Logger.getInstance(CommentAction.class);
    @Override
    public void actionPerformed(AnActionEvent e) {

        // First we load up the current project
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        // If not inside a project then we exit the function
        if (project == null || editor == null) {
            return;
        }

        // With this we load the selected code as a String to later send to our AI
        String selectedText = editor.getSelectionModel().getSelectedText();

        // If there wa no text selected, value will be null, which allows us to show a warning message
        if (selectedText == null || selectedText.isBlank()) {
            com.intellij.openapi.ui.Messages.showWarningDialog(
                    "Please select some code first.",
                    "No Selection"
            );
            return;
        }

        // Fetch the plugin's persistent settings to load the settings
        rsdltj.myplugin.settings.OllamaSettings settings =
                com.intellij.openapi.application.ApplicationManager.getApplication()
                        .getService(rsdltj.myplugin.settings.OllamaSettings.class);

        /*
         * Temporary debug: print what we captured (remove later)
         */
        System.out.println("Selected code: " + selectedText.substring(0, Math.min(50, selectedText.length())) + "...");
        System.out.println("Using model: " + settings.modelName);

        // With everything ready, we display the thinking window, send the prompt and execute the actions

        // We trigger the pop-up thinking window before starting the async task
        rsdltj.myplugin.ui.LoadingMessage loadingDialog =
                rsdltj.myplugin.ui.LoadingMessage.show(project, settings.thinkingMessage);

        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {

                // Since we allow the user to make its own prompt, we have to check they did it with the right format, using the placeholder %s
                if (!settings.explanationPrompt.contains("%s")) {
                    LOG.warn("Prompt template missing %s placeholder");
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() ->
                            com.intellij.openapi.ui.Messages.showErrorDialog("Settings error: Prompt must contain %s placeholder", "Configuration Error")
                    );
                    return;
                }
                /*
                    First we prepare the delivery by replacing %s with the prompt, as this prompt can be changed in settings
                    Using literal replace() avoids String.format() parsing issues with % signs in the prompt text
                 */
                String formattedPrompt = settings.explanationPrompt
                        .replace("%s", selectedText);

                // We now call our AI and wait for the response
                OllamaClient.sendPromptAsync(settings.ollamaUrl, settings.modelName, formattedPrompt)
                        .thenAccept(aiResponse -> {
                            // Validate that we actually received a usable response
                            if (aiResponse == null || aiResponse.isBlank()) {
                                LOG.warn("Ollama returned empty or null response");
                                rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);
                                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() ->
                                        com.intellij.openapi.ui.Messages.showErrorDialog("AI returned an empty response.", "Error")
                                );
                                return;
                            }

                            // Debugging to check if we got an answer
                            System.out.println("[AI Response Received] Length: " + aiResponse.length() + " chars");

                            // Now that we are ready to show the result, we close the thinking window
                            rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);

                            // Clean the response using our centralized pipeline (trim, markdown fences, Unicode escapes)
                            String cleanResponse = rsdltj.myplugin.ai.AiResponseCleaner.clean(aiResponse);

                            // And show the window with the suggested code, giving the user the option to implement it or try something else
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                                rsdltj.myplugin.actions.commenting.ExplanationResult resultDialog =
                                        new ExplanationResult(project, cleanResponse, selectedText, true);

                                // If we receive a decision, we execute
                                if (resultDialog.showAndGet()) {
                                    String choice = resultDialog.getUserChoice();

                                    // If the user wants to directly apply the version with comments in between lines
                                    if ("apply_commented".equals(choice)) {
                                        // We already have cleanResponse from above—just apply it directly
                                        // No need to call Ollama again; just update the document on the EDT
                                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                                            com.intellij.openapi.editor.Document document = editor.getDocument();
                                            int startOffset = editor.getSelectionModel().getSelectionStart();
                                            int endOffset = editor.getSelectionModel().getSelectionEnd();

                                            document.replaceString(startOffset, endOffset, cleanResponse);
                                            editor.getSelectionModel().removeSelection();
                                        }));
                                    }

                                    // If the user prefers a single paragraph on top of the selected code
                                    else if ("generate_summary".equals(choice)) {
                                        // Close the first dialog before starting the block comment flow
                                        resultDialog.close(OK_EXIT_CODE);

                                        // Define the success handler: what to do when we get the block comment
                                        java.util.function.Consumer<String> blockSuccessHandler = blockResponse -> {
                                            // Clean the response using our centralized pipeline (trim, markdown fences, Unicode escapes)
                                            String cleanBlockComment = rsdltj.myplugin.ai.AiResponseCleaner.clean(blockResponse);

                                            // Show the result in a dialog on the EDT (Event Dispatch Thread)
                                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                                                // Reuse ExplanationResult to display the block comment
                                                rsdltj.myplugin.actions.commenting.ExplanationResult blockDialog =
                                                        new ExplanationResult(project, cleanBlockComment, selectedText, false);

                                                // If user clicks "Apply", insert the comment above the original selection
                                                if (blockDialog.showAndGet() && "apply_commented".equals(blockDialog.getUserChoice())) {
                                                    // Wrap the document edit in WriteCommandAction for undo support (Ctrl+Z)
                                                    com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                                                        // Get the document and the start offset of the original selection
                                                        com.intellij.openapi.editor.Document document = editor.getDocument();
                                                        int startOffset = editor.getSelectionModel().getSelectionStart();

                                                        // Insert the block comment + newline BEFORE the original code
                                                        document.insertString(startOffset, cleanBlockComment + "\n");

                                                        // Move cursor to after the inserted comment for clean UX
                                                        editor.getCaretModel().moveToOffset(startOffset + cleanBlockComment.length() + 1);

                                                        // Clear any remaining selection highlight
                                                        editor.getSelectionModel().removeSelection();
                                                    });
                                                }
                                            });
                                        };

                                        // Define the error handler: what to do if the AI call fails
                                        java.util.function.Consumer<Throwable> blockErrorHandler = error -> {
                                            LOG.warn("Block comment generation failed", error);
                                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() ->
                                                    com.intellij.openapi.ui.Messages.showErrorDialog("Failed to generate block comment.", "Error")
                                            );
                                        };

                                        // Format the block comment prompt by injecting the selected code
                                        String blockPrompt = settings.blockCommentPrompt.replace("%s", selectedText);

                                        // Call the helper method with our prompt and handlers
                                        callOllamaWithLoading(project, settings, blockPrompt, settings.thinkingMessage, blockSuccessHandler, blockErrorHandler);
                                    }
                                    // We don't need an "if" for dismiss, as it does nothing and the window closes
                                }
                            });
                        })
                        .exceptionally(error -> {
                            // In case of finding an error, we close the thinking window and log the error
                            rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);

                            LOG.warn("Ollama explanation failed in CommentAction", error);

                            // Besides that we show directly there was an issue to the user
                            com.intellij.openapi.application.ApplicationManager.getApplication()
                                    .invokeLater(() ->
                                            com.intellij.openapi.ui.Messages.showErrorDialog(
                                                    "AI explanation failed. Check logs for details.",
                                                    "Error"
                                            )
                                    );
                            return null;
                        });
            } catch (Exception b) {
                // If there is an unaccounted error
                rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);
                com.intellij.openapi.diagnostic.Logger.getInstance(CommentAction.class)
                        .warn("Unexpected error in CommentAction", b);
            }
        });
    }

    // This extracted method allows us to follow the same "send prompt and receive answer" logic to both actions
    private void callOllamaWithLoading(Project project, rsdltj.myplugin.settings.OllamaSettings settings,
                                       String prompt, String loadingMessage,
                                       java.util.function.Consumer<String> onSuccess,
                                       java.util.function.Consumer<Throwable> onError) {

        // First we open the "thinking" window
        rsdltj.myplugin.ui.LoadingMessage loadingDialog =
                rsdltj.myplugin.ui.LoadingMessage.show(project, loadingMessage);

        // With the thinking window open, we execute the AI call
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // First we send over the prompt
                OllamaClient.sendPromptAsync(settings.ollamaUrl, settings.modelName, prompt)
                        .thenAccept(response -> {

                            // Once the AI answers, we close the thinking window to open the one with the answer
                            rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);

                            // If we got a valid response, we pass it to the success handler
                            if (response != null && !response.isBlank()) {
                                onSuccess.accept(response);
                            } else {
                                LOG.warn("Ollama returned empty response for prompt: " + prompt.substring(0, Math.min(50, prompt.length())));
                            }
                        })
                        .exceptionally(error -> {
                            // We close thinking window on error
                            rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);
                            // And then pass the error to the error handler
                            onError.accept(error);
                            return null;
                        });
            } catch (Exception e) {
                // In case there are unexpected errors
                LOG.warn("Unexpected error in Ollama call", e);
                rsdltj.myplugin.ui.LoadingMessage.close(loadingDialog);
                onError.accept(e);
            }
        });
    }
}