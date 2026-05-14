package rsdltj.myplugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;

public class LoadingMessage extends DialogWrapper {
    private final JLabel messageLabel;

    //Constructor
    public LoadingMessage(Project project, String message) {
        super(project);
        setTitle("AI Assistant");
        setModal(true);
        setModal(false); // Allow EDT to process close requests while dialog is visible
        setResizable(false);
        messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(messageLabel.getFont().deriveFont(14f));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        panel.add(messageLabel);
        return panel;
    }

    @Override
    protected Action[] createActions() {
        return new Action[0];
    }

    @Override
    public void doCancelAction() {
        /*
            We want to allow the close button to work, but not when using Esc, to avoid accidental exits
            For that, we check if the event came from ESC vs. window manager
         */
        if (isVisible()) {
            close(CANCEL_EXIT_CODE);
        }
    }

    /*
        To open and close the thinking window while allowing the actual thinking process in hte background we create a method to open and close it
        This allows us to time the open and close precisely, so it starts as we send the prompt, and closes once we receive the answer
     */

    // The method to open the window
    public static LoadingMessage show(Project project, String message) {
        LoadingMessage dialog = new LoadingMessage(project, message);
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(dialog::show);
        return dialog;
    }

    // The method to close it
    public static void close(LoadingMessage dialog) {
        if (dialog == null) return;
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            if (dialog.isVisible()) {
                dialog.close(com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE);
            }
        });
    }
}