package rsdltj.myplugin.core;

import com.intellij.openapi.Disposable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

//Implementing disposable will trigger IntelliJ to call dispose when we close the IDE, triggering the AI background process to shut down
public class OllamaProcessManager implements Disposable {
    private Process ollamaProcess;


    //This process boots up our local AI when we start the IDE
    public void start() throws IOException {

        //This extracted method checks if its already running, so we don't try to start it again
        if (isRunning()) return;
        /*
            We build the start command in the CMD
            Rather than ollama run llama3.2, which triggers the "conversation style AI; we use ollama serve, which starts the AI in the background to answer API calls
         */
        ProcessBuilder pb = new ProcessBuilder("ollama", "serve");

        //Redirecting the outputs to a discard removes them, so the CMD window doesn't pop up and possibly startling a user unaware of how it works
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        //We send the CMD command, as if we pressed "enter"
        ollamaProcess = pb.start();

        //We give our AI some time to launch
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    }

    //We check if the AI is already running
    public boolean isRunning() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    //We override this class' dispose to make it call our stop function
    @Override
    public void dispose() {
        stop();
    }

    //Our stop function makes sure the AI shuts
    public void stop() {
        if (ollamaProcess != null && ollamaProcess.isAlive()) {

            //This .destroy acts as typing ctrl+c in the CMD terminal, which shuts the AI off
            ollamaProcess.destroy();
            try { ollamaProcess.waitFor(3, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}

            //After trying to "request" to shut down, we check if it stopped, if it didn't we forcibly stop it
            if (ollamaProcess.isAlive()) ollamaProcess.destroyForcibly();
            ollamaProcess = null;
        }
    }
}