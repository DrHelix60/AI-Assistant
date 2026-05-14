package rsdltj.myplugin.actions.commenting;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

public class ExplanationResult extends DialogWrapper {
    private final JTextArea explanationArea;
    private String userChoice;

    // With this field we can control whether the "Generate Block Comment" button appears
    private final boolean showGenerateBlockButton;

    public ExplanationResult(Project project, String explanation, String originalCode, boolean showGenerateBlockButton) {
        super(project);
        setTitle("AI Explanation");
        explanationArea = new JTextArea(explanation);
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        this.showGenerateBlockButton = showGenerateBlockButton;
        init();
    }

    // The panel to show the answer and options
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(600, 400));

        JBScrollPane scrollPane = new JBScrollPane(explanationArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // This first method is to apply the new commented text into the original code
    @Override
    protected Action @NotNull [] createActions() {
        // Start with a list to build actions dynamically
        java.util.List<Action> actionsList = new java.util.ArrayList<>();

        Action applyCommented = new AbstractAction("Apply Commented Version") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                userChoice = "apply_commented";
                close(OK_EXIT_CODE);
            }
        };
        actionsList.add(applyCommented);

        // The plugin gives the option of generating a comment paragraph to place on top of the selected code rather than encrusting comments
        if (showGenerateBlockButton) {
            Action generateSummary = new AbstractAction("Generate Summary Comment") {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    userChoice = "generate_summary";
                    close(OK_EXIT_CODE);
                }
            };
            actionsList.add(generateSummary);
        }

        // This function allows the user to ignore the proposed options, in case they aren't convinced
        Action cancel = new AbstractAction("Dismiss") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                userChoice = "dismiss";
                close(CANCEL_EXIT_CODE);
            }
        };
        actionsList.add(cancel);

        return actionsList.toArray(new Action[0]);
    }

    public String getUserChoice() {
        return userChoice;
    }
}