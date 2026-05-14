package rsdltj.myplugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class OllamaConfigurable implements Configurable {
    private JComponent myMainPanel;
    private JTextField urlField;
    private JTextField modelField;
    private JTextArea promptArea;
    private JTextField thinkingField;
    private final OllamaSettings settingsState;
    private JTextArea blockCommentPromptArea;
    private JTextArea conventionPromptArea;
    // New fields for Convention Checker settings
    private JTextField maxLineLengthField;
    private JSpinner maxSelectionCharsSpinner;

    public OllamaConfigurable() {
        this.settingsState = ApplicationManager.getApplication().getService(OllamaSettings.class);
    }

    @Override
    public String getDisplayName() {
        return "Ollama AI";
    }

    @Override
    public @Nullable JComponent createComponent() {

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(Box.createVerticalStrut(2));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(4));

        if (settingsState.showSetupHelp) {
            JLabel sectionTitle = new JLabel("Instructions on how to set up the AI for the plugin");
            sectionTitle.setFont(sectionTitle.getFont().deriveFont(Font.BOLD, 12f));
            sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(sectionTitle);
            panel.add(Box.createVerticalStrut(4));

            JPanel helpPanel = new JPanel(new BorderLayout(5, 5));
            helpPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            helpPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            Color hintColor = JBColor.namedColor("Label.infoForeground", JBColor.GRAY);
            JLabel instructions = getJLabel(hintColor);

            JLabel linkLabel = new JLabel("<html><a href=''>🔗 Download Ollama</a></html>");
            linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            linkLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openUrl();
                }
            });

            JPanel textArea = new JPanel(new BorderLayout(0, 5));
            textArea.add(instructions, BorderLayout.CENTER);
            textArea.add(linkLabel, BorderLayout.SOUTH);

            JCheckBox dontShowAgain = new JCheckBox("Don't show this again");
            dontShowAgain.addActionListener(e -> settingsState.showSetupHelp = !dontShowAgain.isSelected());

            helpPanel.add(textArea, BorderLayout.CENTER);
            helpPanel.add(dontShowAgain, BorderLayout.SOUTH);
            panel.add(helpPanel);
        }
        // I want to tell the user they can bind their own keyboard shortcuts
        JLabel instructionsShortcutHint = new JLabel("You can map this plugin's shortcuts in: Settings → Keymap");
        instructionsShortcutHint.setFont(instructionsShortcutHint.getFont().deriveFont(11f));
        instructionsShortcutHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(instructionsShortcutHint);

        //To standardize the horizontal lines, we create an extracted method
        panel.add(Box.createVerticalStrut(4));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(8));

        JLabel ollamaSettingsTitle = new JLabel("Ollama Settings");
        ollamaSettingsTitle.setFont(ollamaSettingsTitle.getFont().deriveFont(Font.BOLD, 12f));
        ollamaSettingsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ollamaSettingsTitle);
        panel.add(Box.createVerticalStrut(4));

        JPanel urlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        urlRow.setOpaque(false);
        urlRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        urlField = new JTextField(28);
        urlRow.add(new JLabel("Ollama URL:"));
        urlRow.add(urlField);
        panel.add(urlRow);

        panel.add(Box.createVerticalStrut(4));

        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        modelRow.setOpaque(false);
        modelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        modelField = new JTextField(28);
        modelRow.add(new JLabel("Model:"));
        modelRow.add(modelField);
        panel.add(modelRow);

        panel.add(Box.createVerticalStrut(4));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(8));

        JPanel thinkingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        thinkingRow.setOpaque(false);
        thinkingRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinkingField = new JTextField(28);
        thinkingRow.add(new JLabel("Thinking Message:"));
        thinkingRow.add(thinkingField);
        panel.add(thinkingRow);

        panel.add(Box.createVerticalStrut(8));

        panel.add(Box.createVerticalStrut(4));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(8));

        // Explanation Prompt label
        JLabel promptLabel = new JLabel("Explanation Prompt:");
        promptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(promptLabel);
        panel.add(Box.createVerticalStrut(2));

        // Explanation Prompt text area
        promptArea = new JTextArea(4, 35);
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setBorder(BorderFactory.createLineBorder(Gray._200));
        JBScrollPane promptScroll = new JBScrollPane(promptArea);
        promptScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        promptScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(promptScroll);
        panel.add(Box.createVerticalStrut(8));

        panel.add(Box.createVerticalStrut(16));

        // Block Comment Prompt label
        JLabel blockPromptLabel = new JLabel("Block Comment Prompt:");
        blockPromptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(blockPromptLabel);
        panel.add(Box.createVerticalStrut(2));

        // Block Comment Prompt text area
        blockCommentPromptArea = new JTextArea(4, 35);
        blockCommentPromptArea.setLineWrap(true);
        blockCommentPromptArea.setWrapStyleWord(true);
        blockCommentPromptArea.setBorder(BorderFactory.createLineBorder(Gray._200));
        JBScrollPane blockPromptScroll = new JBScrollPane(blockCommentPromptArea);
        blockPromptScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        blockPromptScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(blockPromptScroll);
        panel.add(Box.createVerticalStrut(4));

        /*
            We have to tell the user that for their custom prompts to work, they need to reference the code with %s
            We also break up the explanation so it all fits in the screen without scrolling to the right by using HTML
         */
        JLabel blockPromptHint = new JLabel("<html>You can change any of these prompts to get the type of results you want,<br>but to make it work, you need to type \"%s\" where the code would be<br>when you send the prompt to the AI.</html>");
        blockPromptHint.setForeground(Gray._200);
        blockPromptHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        blockPromptHint.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
        panel.add(blockPromptHint);

        panel.add(Box.createVerticalStrut(8));

        // This will show the user what the current keyboard shortcut is for this action
        String commentShortcut = getCurrentShortcut("rsdltj.myplugin.actions.commenting.CommentAction");
        JPanel shortcutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        shortcutRow.setOpaque(false);
        shortcutRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel shortcutLabel = new JLabel("Comment Code shortcut: " + commentShortcut);
        shortcutRow.add(shortcutLabel);
        panel.add(shortcutRow);
        panel.add(Box.createVerticalStrut(8));

        panel.add(Box.createVerticalStrut(4));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(8));

        panel.add(Box.createVerticalStrut(16));

        // Convention Checker Prompt label
        JPanel conventionLabelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        conventionLabelRow.setOpaque(false);
        conventionLabelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        conventionLabelRow.add(new JLabel("Convention Checker Prompt:"));
        panel.add(conventionLabelRow);
        panel.add(Box.createVerticalStrut(2));

        // Convention Checker textarea
        conventionPromptArea = new JTextArea(4, 35);
        conventionPromptArea.setLineWrap(true);
        conventionPromptArea.setWrapStyleWord(true);
        conventionPromptArea.setBorder(BorderFactory.createLineBorder(Gray._200));
        JBScrollPane conventionScroll = new JBScrollPane(conventionPromptArea);
        conventionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        conventionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(conventionScroll);
        panel.add(Box.createVerticalStrut(4));

        // Link + hint row
        JPanel conventionHintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        conventionHintRow.setOpaque(false);
        conventionHintRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel conventionsLink = new JLabel("<html><a href='#'>📖 Official Java Code Conventions</a></html>");
        conventionsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        conventionsLink.setForeground(JBColor.BLUE);
        conventionsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                com.intellij.ide.BrowserUtil.browse("https://www.oracle.com/java/technologies/javase/codeconventions-introduction.html");
            }
        });

        JLabel conventionHint = new JLabel("<html>You can change any of these prompts to get the type of results you want,<br>but to make it work, you need to type \"%s\" where the code would be<br>when you send the prompt to the AI.</html>");
        conventionHint.setForeground(Gray._200);

        JPanel linkHintPanel = new JPanel(new BorderLayout(0, 2));
        linkHintPanel.setOpaque(false);
        linkHintPanel.add(conventionsLink, BorderLayout.NORTH);
        linkHintPanel.add(conventionHint, BorderLayout.SOUTH);
        conventionHintRow.add(linkHintPanel);
        panel.add(conventionHintRow);

        panel.add(Box.createVerticalStrut(8));

        // Convention Checker shortcut hint
        String conventionShortcut = getCurrentShortcut("rsdltj.myplugin.actions.convention.ConventionAction");
        JPanel conventionShortcutRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        conventionShortcutRow.setOpaque(false);
        conventionShortcutRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel conventionShortcutLabel = new JLabel("Fix Conventions shortcut: " + conventionShortcut);
        conventionShortcutRow.add(conventionShortcutLabel);
        panel.add(conventionShortcutRow);
        panel.add(Box.createVerticalStrut(8));

        // Convention Checker max line length allowed (Default is 120 like the Java convention)
        JPanel lineLengthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lineLengthRow.setOpaque(false);
        lineLengthRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        maxLineLengthField = new JTextField(String.valueOf(settingsState.maxLineLength), 10);
        lineLengthRow.add(new JLabel("Max Line Length:"));
        lineLengthRow.add(maxLineLengthField);
        lineLengthRow.add(new JLabel("chars"));
        panel.add(lineLengthRow);
        panel.add(Box.createVerticalStrut(4));

        // Maximum amount of characters seleceted for Convention Checker (I don't recommend increasing it uch, as it gets demanding very quick)
        JPanel selectionCharsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        selectionCharsRow.setOpaque(false);
        selectionCharsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(settingsState.maxSelectionChars, 1000, 20000, 500);
        maxSelectionCharsSpinner = new JSpinner(spinnerModel);
        selectionCharsRow.add(new JLabel("Max Selection Chars:"));
        selectionCharsRow.add(maxSelectionCharsSpinner);
        panel.add(selectionCharsRow);

        panel.add(Box.createVerticalStrut(4));
        panel.add(createSeparator());
        panel.add(Box.createVerticalStrut(8));

        // --- Revert to Defaults Button ---
        JButton revertButton = new JButton("Revert to Defaults");
        revertButton.addActionListener(e -> {
            int result = Messages.showYesNoDialog(
                    "Are you sure you want to revert all settings and prompts to their original defaults?\nThis will overwrite any custom changes.",
                    "Revert to Defaults",
                    Messages.getQuestionIcon()
            );
            if (result == Messages.YES) {
                // 1. Reset the settings object in memory
                settingsState.resetToDefaults();
                // 2. Refresh the UI with the new defaults
                reset();
                // 3. Save immediately to disk
                apply();
                // 4. Notify user
                Messages.showInfoMessage("All settings and prompts have been reverted to defaults and saved.", "Success");
            }
        });

        JPanel revertRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        revertRow.setOpaque(false);
        revertRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        revertRow.add(revertButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(revertRow);
        panel.add(Box.createVerticalStrut(4));
        // --- End Revert Button ---

        myMainPanel = panel;
        return myMainPanel;
    }

    private JComponent createSeparator() {
        Color separatorColor = JBColor.namedColor("Separator.separatorColor", new JBColor(Gray._220, Gray._100));
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(separatorColor);
        separator.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        return separator;
    }

    private static @NotNull JLabel getJLabel(Color hintColor) {
        String hintHex = String.format("#%02x%02x%02x", hintColor.getRed(), hintColor.getGreen(), hintColor.getBlue());

        return new JLabel(
                "<html><b>Setup Ollama (one-time):</b><br>" +
                        "1. Download from <a href=''>ollama.com</a><br>" +
                        "2. Open terminal & run: <code>ollama pull llama3.2</code><br>" +
                        "3. Keep Ollama running in background<br>" +
                        "<span style='color:" + hintHex + "; font-size:11px'>Free, local, no API keys required</span></html>"
        );
    }

    @Override
    public void reset() {
        urlField.setText(settingsState.ollamaUrl);
        modelField.setText(settingsState.modelName);
        promptArea.setText(settingsState.explanationPrompt);
        thinkingField.setText(settingsState.thinkingMessage);
        blockCommentPromptArea.setText(settingsState.blockCommentPrompt);
        conventionPromptArea.setText(settingsState.conventionPrompt);
        // Reset new convention checker settings
        maxLineLengthField.setText(String.valueOf(settingsState.maxLineLength));
        maxSelectionCharsSpinner.setValue(settingsState.maxSelectionChars);
    }

    @Override
    public boolean isModified() {
        boolean baseModified = !urlField.getText().trim().equals(settingsState.ollamaUrl) ||
                !modelField.getText().trim().equals(settingsState.modelName) ||
                !promptArea.getText().trim().equals(settingsState.explanationPrompt) ||
                !thinkingField.getText().trim().equals(settingsState.thinkingMessage) ||
                !blockCommentPromptArea.getText().trim().equals(settingsState.blockCommentPrompt) ||
                !conventionPromptArea.getText().trim().equals(settingsState.conventionPrompt);

        // Check convention checker settings
        try {
            int lineLen = Integer.parseInt(maxLineLengthField.getText().trim());
            int selChars = (int) maxSelectionCharsSpinner.getValue();
            boolean settingsModified = lineLen != settingsState.maxLineLength || selChars != settingsState.maxSelectionChars;
            return baseModified || settingsModified;
        } catch (NumberFormatException e) {
            return true; // Treat invalid input as modified to trigger validation
        }
    }

    @Override
    public void apply() {
        settingsState.ollamaUrl = urlField.getText().trim();
        settingsState.modelName = modelField.getText().trim();
        settingsState.explanationPrompt = promptArea.getText().trim();
        settingsState.thinkingMessage = thinkingField.getText().trim();
        settingsState.blockCommentPrompt = blockCommentPromptArea.getText().trim();
        settingsState.conventionPrompt = conventionPromptArea.getText().trim();

        // Save convention checker settings with validation
        try {
            int lineLen = Integer.parseInt(maxLineLengthField.getText().trim());
            int selChars = (int) maxSelectionCharsSpinner.getValue();

            if (lineLen > 0 && selChars > 0) {
                settingsState.maxLineLength = lineLen;
                settingsState.maxSelectionChars = selChars;
            } else {
                com.intellij.openapi.ui.Messages.showWarningDialog("Values must be greater than 0.", "Invalid Input");
            }
        } catch (NumberFormatException e) {
            com.intellij.openapi.ui.Messages.showErrorDialog("Please enter valid numbers.", "Invalid Input");
        }
    }

    @Override
    public void disposeUIResources() {
        myMainPanel = null;
        urlField = null;
        modelField = null;
        promptArea = null;
        thinkingField = null;
        conventionPromptArea = null;
        maxLineLengthField = null;
        maxSelectionCharsSpinner = null;
    }

    // To show the current keyboard shortcut in the settings menu, we have to create a method to fetch it
    private String getCurrentShortcut(String actionId) {
        try {
            com.intellij.openapi.keymap.Keymap keymap = com.intellij.openapi.keymap.KeymapManager.getInstance().getActiveKeymap();
            com.intellij.openapi.actionSystem.Shortcut[] shortcuts = keymap.getShortcuts(actionId);
            if (shortcuts.length > 0) {
                return com.intellij.openapi.keymap.KeymapUtil.getShortcutText(shortcuts[0]);
            }
        } catch (Exception ignored) {}
        return "No shortcut found";
    }

    private void openUrl() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://ollama.com/download"));
        } catch (Exception e) {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection("https://ollama.com/download"), null);
            com.intellij.openapi.ui.Messages.showInfoMessage("URL copied to clipboard: " + "https://ollama.com/download", "Browser Not Available");
        }
    }
}