package rsdltj.myplugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import rsdltj.myplugin.ai.OllamaClient;
import rsdltj.myplugin.settings.OllamaSettings;

public class TestAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(TestAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        OllamaSettings settings = ApplicationManager.getApplication().getService(OllamaSettings.class);

        if (settings.ollamaUrl.isBlank() || settings.modelName.isBlank()) {
            Messages.showWarningDialog("Please configure Ollama URL and Model in Settings → Tools → Ollama AI", "Configuration Missing");
            return;
        }

        Messages.showInfoMessage("Calling Ollama...\n(This may take a few seconds while the model loads)", "Connecting");

        OllamaClient.sendPromptAsync(settings.ollamaUrl, settings.modelName, "Reply with exactly one word: Hello")
                .thenAccept(response -> {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showInfoMessage("Ollama says:\n\n" + response, "Success ✅")
                    );
                })
                .exceptionally(error -> {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showErrorDialog("Ollama call failed:\n" + error.getMessage(), "API Error ❌")
                    );
                    LOG.warn("Test Ollama call failed", error);
                    return null;
                });
    }
}