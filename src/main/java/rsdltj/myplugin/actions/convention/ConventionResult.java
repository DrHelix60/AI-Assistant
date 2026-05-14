package rsdltj.myplugin.actions.convention;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

public class ConventionResult extends DialogWrapper {
    private final String originalCode;
    private final String correctedCode;
    private boolean applySelected;

    public ConventionResult(Project project, String originalCode, String correctedCode) {
        super(project);
        setTitle("Convention Fix Preview");
        this.originalCode = originalCode;
        this.correctedCode = correctedCode;
        init();
    }

    // The panel to show the proposed corrections, and the option to apply or dismiss
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(850, 600));

        // Header
        JLabel header = new JLabel("Proposed changes:");
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        // Warning message
        JLabel warningLabel = new JLabel("⚠️ Note: Changes will only update this selection. You may need to manually refactor references elsewhere.");
        warningLabel.setForeground(JBColor.namedColor("Label.warningForeground", new JBColor(new Color(0xCC7722), new Color(0xE6A23C))));
        warningLabel.setFont(warningLabel.getFont().deriveFont(11f));

        // We set the header and warning message on the top section
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
        headerPanel.setOpaque(false);
        headerPanel.add(header, BorderLayout.NORTH);
        headerPanel.add(warningLabel, BorderLayout.SOUTH);
        panel.add(headerPanel, BorderLayout.NORTH);

        // Now, we divide the available space, so we can have the before and after on each side
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5); // Equal space for both panels

        // The original code panel (with change markers)
        String markedOriginal = markChangedLines(originalCode, correctedCode);
        JTextArea originalArea = new JTextArea(markedOriginal);
        originalArea.setEditable(false);
        originalArea.setLineWrap(true);
        originalArea.setWrapStyleWord(true);
        originalArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JPanel originalPanel = new JPanel(new BorderLayout(5, 5));
        originalPanel.add(new JLabel("Original Selection:"), BorderLayout.NORTH);
        originalPanel.add(new JBScrollPane(originalArea), BorderLayout.CENTER);

        // Proposed corrected code panel
        JTextArea correctedArea = new JTextArea(correctedCode);
        correctedArea.setEditable(false);
        correctedArea.setLineWrap(true);
        correctedArea.setWrapStyleWord(true);
        correctedArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JPanel correctedPanel = new JPanel(new BorderLayout(5, 5));
        correctedPanel.add(new JLabel("Corrected Selection:"), BorderLayout.NORTH);
        correctedPanel.add(new JBScrollPane(correctedArea), BorderLayout.CENTER);

        splitPane.setTopComponent(originalPanel);
        splitPane.setBottomComponent(correctedPanel);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // The buttons for the user to choose
    @Override
    protected Action @NotNull [] createActions() {

        // This button will change the code in the selection to the one proposed by the AI
        Action apply = new AbstractAction("Apply Changes") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                applySelected = true;
                close(OK_EXIT_CODE);
            }
        };

        // This button simply closes the window, so the code doesn't get applied
        Action cancel = new AbstractAction("Dismiss") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                applySelected = false;
                close(CANCEL_EXIT_CODE);
            }
        };

        return new Action[] { apply, cancel };
    }

    public boolean isApplySelected() {
        return applySelected;
    }

    // This extracted method finds what lines have been altered, to mark them
    private String markChangedLines(String original, String corrected) {
        String[] originalLines = original.split("\n", -1);
        String[] correctedLines = corrected.split("\n", -1);
        StringBuilder result = new StringBuilder();

        int maxLines = Math.max(originalLines.length, correctedLines.length);
        for (int i = 0; i < maxLines; i++) {
            String origLine = i < originalLines.length ? originalLines[i] : "";
            String corrLine = i < correctedLines.length ? correctedLines[i] : "";

            // If lines differ (ignoring leading/trailing whitespace), mark the original
            if (!origLine.trim().equals(corrLine.trim())) {
                result.append("← CHANGED: ").append(origLine).append("\n");
            } else {
                result.append(origLine).append("\n");
            }
        }
        return result.toString();
    }
}