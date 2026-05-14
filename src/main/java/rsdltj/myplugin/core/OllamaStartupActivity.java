package rsdltj.myplugin.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class OllamaStartupActivity implements StartupActivity.DumbAware {

    //To apply everything coded in the process manager, we need a start-up activity, so it calls the methods when we open the IDE
    @Override
    public void runActivity(@NotNull Project project) {
        OllamaProcessManager manager = ApplicationManager.getApplication().getService(OllamaProcessManager.class);
        if (!manager.isRunning()) {

            // All UI operations must run on the EDT
            ApplicationManager.getApplication().invokeLater(() -> {

                //instead of starting it without warning, we allow the user to choose not to, in case they don't intend to use it or don't want to spend resources on the AI currently
                int choice = Messages.showYesNoDialog(
                        project,
                        "Ollama is not running. Start it automatically?",
                        "AI Assistant",
                        Messages.getQuestionIcon()
                );

                //If the user chooses so, we attempt to start the AI, and notify the user if it does or doesn't work
                if (choice == Messages.YES) {

                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            manager.start();
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Messages.showInfoMessage("Ollama started successfully.", "AI Assistant")
                            );
                        } catch (Exception e) {
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Messages.showErrorDialog("Failed to start Ollama. Please run it manually.", "AI Assistant")
                            );
                        }
                    });
                }
            });
        }
    }
}