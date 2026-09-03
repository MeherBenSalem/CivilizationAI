package tn.naizo.smartvillagers.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.config.ApiCredentials;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class DeepSeekProvider implements AiProvider {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public String name() {
        return "deepseek";
    }

    @Override
    public boolean isConfigured() {
        return ApiCredentials.apiKey().isPresent();
    }

    @Override
    public CompletableFuture<AiResponse> complete(AiRequest request) {
        OptionalKey key = ApiCredentials.apiKey()
                .map(OptionalKey::new)
                .orElse(null);
        if (key == null) {
            return CompletableFuture.completedFuture(AiResponse.failure("API key not configured"));
        }

        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        String systemPrompt = PromptBuilder.buildSystemPrompt(request.context());
        String userPrompt = PromptBuilder.buildUserPrompt(request.playerMessage());

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("max_tokens", Math.min(256, config.maxReplyChars()));

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemPrompt);
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userPrompt);
        messages.add(user);

        body.add("messages", messages);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.apiBaseUrl()))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + key.value())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        return CLIENT.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> parseResponse(response.statusCode(), response.body()))
                .exceptionally(error -> {
                    Constants.LOG.debug("DeepSeek request failed", error);
                    return AiResponse.failure("AI request failed");
                });
    }

    private static AiResponse parseResponse(int status, String body) {
        if (status < 200 || status >= 300) {
            Constants.LOG.debug("DeepSeek returned HTTP {}", status);
            return AiResponse.failure("AI provider error (" + status + ")");
        }

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return AiResponse.failure("Empty AI response");
            }
            String content = choices.get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString()
                    .trim();
            if (content.isEmpty()) {
                return AiResponse.failure("Empty AI response");
            }
            int maxChars = SmartVillagersConfig.get().maxReplyChars();
            if (content.length() > maxChars) {
                content = content.substring(0, maxChars).trim();
            }
            return AiResponse.success(content);
        } catch (Exception e) {
            Constants.LOG.debug("Failed to parse DeepSeek response", e);
            return AiResponse.failure("Invalid AI response");
        }
    }

    private record OptionalKey(String value) {
    }
}
