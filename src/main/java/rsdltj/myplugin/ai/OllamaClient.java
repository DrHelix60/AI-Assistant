package rsdltj.myplugin.ai;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class OllamaClient {

    //We create a log in case we need to debug offline
    private static final Logger LOG = Logger.getInstance(OllamaClient.class);

    //Despite working with our own device, we have to connect with the AI running on hte background as a server service
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    //With this method we define the prompt delivery, and how to treat the exceptions
    public static CompletableFuture<String> sendPromptAsync(String baseUrl, String model, String prompt) {
        return CompletableFuture.supplyAsync(() -> {

            /*
                We try to establish connection, if we succeed we make the request
                Our catch handles network issues so we don't crash the app
             */
            try {
                //With this we ensure the url has the apropiate structure
                String url = baseUrl.endsWith("/") ? baseUrl + "api/generate" : baseUrl + "/api/generate";

                //Our AI works with JSON, so we need to format the prompt information
                String jsonPayload = buildRequestJson(model, prompt);

                //We first create the request object and send it after
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(Duration.ofSeconds(180))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                //Code 200 means we have successfully connected with our AI, so if we don't receive that we show there is an issue
                if (response.statusCode() != 200) {
                    //We save the error code in the log and throw the exception
                    LOG.warn("Ollama API error: " + response.statusCode() + " | " + response.body());
                    throw new RuntimeException("Ollama error " + response.statusCode());
                }

                //This return will give us what the AI answered, after processing it with the extracted method
                return CleanJsonResponse(response.body());

            //We handle connection error to avoid crashing
            } catch (IOException | InterruptedException e) {
                //We save in the log that there was a network error
                LOG.warn("Network error calling Ollama", e);
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog("Could not connect to Ollama. Check if it is running in the background", "Connection Error")
                );
                throw new RuntimeException(e);
            }
        });
    }

    /*
        This formats the prompt delivery to be read as a JSON
        Setting stream to false avoids receiving the answer token by token, ensuring we only receive the full answer
     */
    private static String buildRequestJson(String model, String prompt) {
        String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return """
                {
                  "model": "%s",
                  "prompt": "%s",
                  "stream": false,
                  "num_predict": 8192
                }
                """.formatted(model, escapedPrompt);
    }

    //We now clean the JSON format into something easier to read by our plugin
    private static String CleanJsonResponse(String jsonResponse) {
        try {
            /*
                From the format we made to receive the AI's answer, we have to extract exactly what was told
                This index marks where the answer starts inside the whole String we received
             */
            int responseKeyIndex = jsonResponse.indexOf("\"response\"");

            // A value of -1 means no substring was found to process, meaning there was an error
            if (responseKeyIndex == -1) {
                LOG.warn("Unexpected Ollama response format: " + jsonResponse);
                return "Error: Could not parse AI response";
            }

            /*
                From the String we extract a substring starting at the previously found index
                We have to delimit where does the substring start and end
                It starts right after "response":", which is 12 characters long
                The end of the answer is marked by "
             */

            // Find the colon after "response", then skip whitespace and the opening quote
            int colonIndex = jsonResponse.indexOf(':', responseKeyIndex);
            if (colonIndex == -1) {
                LOG.warn("No colon found after 'response' key in: " + jsonResponse);
                return "Error: Malformed response JSON";
            }

            int textStart = colonIndex + 1;
            // Skip any leading whitespace
            while (textStart < jsonResponse.length() && Character.isWhitespace(jsonResponse.charAt(textStart))) {
                textStart++;
            }
            // Skip the opening quote
            if (textStart < jsonResponse.length() && jsonResponse.charAt(textStart) == '"') {
                textStart++;
            }

            // Find the end: look for the closing quote that's not escaped (handles \" inside response)
            int textEnd = textStart;
            while (textEnd < jsonResponse.length()) {
                char c = jsonResponse.charAt(textEnd);
                // Break on unescaped quote
                if (c == '"' && (textEnd == 0 || jsonResponse.charAt(textEnd - 1) != '\\')) {
                    break;
                }
                textEnd++;
            }

            if (textEnd <= textStart) {
                LOG.warn("Could not find end of response string in: " + jsonResponse);
                return "Error: Malformed response JSON";
            }

            // We remove the excess from the JSON format in an extracted method
            return getRaw(jsonResponse, textStart, textEnd);

        } catch (Exception e) {
            LOG.warn("Error parsing Ollama JSON response", e);
            LOG.info("Raw response was: " + jsonResponse);
            return "Error: Failed to parse AI response - check logs";
        }
    }

    // JSON processing
    private static @NotNull String getRaw(String jsonResponse, int textStart, int textEnd) {

        // Initial processing
        String raw = jsonResponse.substring(textStart, textEnd)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\t", "\t");

        // We remove the Markdown fences
        raw = raw.trim();
        if (raw.startsWith("```")) {
            int firstNewline = raw.indexOf('\n');
            if (firstNewline != -1) {
                raw = raw.substring(firstNewline + 1).trim();
            }
        }
        if (raw.endsWith("```")) {
            raw = raw.substring(0, raw.length() - 3).trim();
        }
        return raw;
    }
}