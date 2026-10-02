package tn.naizo.smartvillagers.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tn.naizo.smartvillagers.Constants;
import tn.naizo.smartvillagers.config.AiProviderType;
import tn.naizo.smartvillagers.config.ApiCredentials;
import tn.naizo.smartvillagers.config.SmartVillagersConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** OpenAI Chat Completions transport shared by the supported provider presets. */
public final class OpenAiCompatibleProvider implements AiProvider {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();

    @Override
    public String name() {
        return SmartVillagersConfig.get().provider().name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public boolean isConfigured() {
        SmartVillagersConfig.Snapshot config = SmartVillagersConfig.get();
        try {
            URI endpoint = URI.create(resolveChatCompletionsUrl(config.apiBaseUrl(), config.endpointMode()));
            return config.provider() != AiProviderType.INVALID && !config.model().isBlank()
                    && (ApiCredentials.apiKey().isPresent() || isLoopback(endpoint));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public CompletableFuture<AiResponse> complete(AiRequest request) {
        return send(SmartVillagersConfig.get(), ApiCredentials.apiKey().orElse(""),
                PromptBuilder.buildSystemPrompt(request.context()),
                PromptBuilder.buildUserPrompt(request.playerMessage()));
    }

    static CompletableFuture<AiResponse> send(SmartVillagersConfig.Snapshot config, String key,
                                               String systemPrompt, String userPrompt) {
        try {
            if (config.provider() == AiProviderType.INVALID) {
                return CompletableFuture.completedFuture(AiResponse.failure("Unknown ai.provider; check configuration"));
            }
            URI endpoint = URI.create(resolveChatCompletionsUrl(config.apiBaseUrl(), config.endpointMode()));
            if (config.model().isBlank()) {
                return CompletableFuture.completedFuture(AiResponse.failure("Set ai.model to the provider's exact model ID"));
            }
            if ((key == null || key.isBlank()) && !isLoopback(endpoint)) {
                return CompletableFuture.completedFuture(AiResponse.failure("API key not configured for selected provider"));
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            buildRequestBody(config, systemPrompt, userPrompt), StandardCharsets.UTF_8));
            if (key != null && !key.isBlank()) builder.header("Authorization", "Bearer " + key.trim());
            return CLIENT.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenApply(response -> parseResponse(response.statusCode(), response.body(), config.maxReplyChars()))
                    .exceptionally(error -> {
                        // Exception messages may contain a URL; never log credentials or player prompts.
                        Constants.LOG.warn("AI request failed (network error or timeout); local dialogue will be used");
                        return AiResponse.failure("AI connection failed or timed out; check endpoint and network");
                    });
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(AiResponse.failure(
                    "Invalid AI configuration; check provider, endpoint, key and tokenLimitParameter"));
        }
    }

    static String buildRequestBody(SmartVillagersConfig.Snapshot config, String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.addProperty("stream", false);
        String parameter = config.tokenLimitParameter().trim();
        if (parameter.equalsIgnoreCase("AUTO")) parameter = config.provider().tokenParameter();
        if (!parameter.equalsIgnoreCase("NONE")) {
            if (!parameter.equals("max_tokens") && !parameter.equals("max_completion_tokens")) {
                throw new IllegalArgumentException("Unsupported token limit parameter");
            }
            body.addProperty(parameter, config.maxTokens());
        }
        // These extensions are specific to DeepSeek's direct API.
        if (config.provider() == AiProviderType.DEEPSEEK) {
            JsonObject thinking = new JsonObject();
            thinking.addProperty("type", "disabled");
            body.add("thinking", thinking);
            body.addProperty("reasoning_effort", "none");
        }
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemPrompt == null ? "" : systemPrompt);
        messages.add(system);
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userPrompt == null ? "" : userPrompt);
        messages.add(user);
        body.add("messages", messages);
        return body.toString();
    }

    static String resolveChatCompletionsUrl(String configured) {
        return resolveChatCompletionsUrl(configured, "AUTO");
    }

    static String resolveChatCompletionsUrl(String configured, String mode) {
        String url = configured == null ? "" : configured.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        URI uri = URI.create(url);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Expected HTTP(S) endpoint without credentials, query or fragment");
        }
        if ("http".equalsIgnoreCase(uri.getScheme()) && !isLoopback(uri)) {
            throw new IllegalArgumentException("Remote endpoints require HTTPS");
        }
        if ("FULL".equalsIgnoreCase(mode.trim())) return url;
        if (!"AUTO".equalsIgnoreCase(mode.trim())) throw new IllegalArgumentException("Invalid endpoint mode");
        return url.endsWith("/chat/completions") ? url : url + "/chat/completions";
    }

    private static boolean isLoopback(URI endpoint) {
        String host = endpoint.getHost();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "[::1]".equals(host) || "::1".equals(host);
    }

    public static String displayEndpoint(SmartVillagersConfig.Snapshot config) {
        try {
            return resolveChatCompletionsUrl(config.apiBaseUrl(), config.endpointMode());
        } catch (RuntimeException ignored) {
            return "invalid endpoint (check ai.apiBaseUrl)";
        }
    }

    static AiResponse parseResponse(int status, String body) {
        return parseResponse(status, body, SmartVillagersConfig.get().maxReplyChars());
    }

    private static AiResponse parseResponse(int status, String body, int maxChars) {
        if (status < 200 || status >= 300) {
            String hint = switch (status) {
                case 401, 403 -> "check the selected provider's API key and permissions";
                case 402 -> "check provider balance, subscription and billing mode";
                case 404 -> "check endpoint and exact model ID";
                case 429 -> "provider quota or rate limit reached; check subscription allowance";
                case 400, 422 -> "check model ID and tokenLimitParameter; use a Chat Completions endpoint";
                default -> "provider unavailable; try again later";
            };
            Constants.LOG.warn("AI provider returned HTTP {}: {}", status, hint);
            return AiResponse.failure("AI provider HTTP " + status + ": " + hint);
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) return AiResponse.failure("AI provider reported an error; check model and account");
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) return AiResponse.failure("Empty AI response");
            JsonObject choice = choices.get(0).getAsJsonObject();
            String content = readMessageContent(choice.getAsJsonObject("message"));
            if (content.isEmpty()) {
                return AiResponse.failure("Empty AI response; try a non-thinking model or increase ai.maxTokens");
            }
            if (content.length() > Math.max(1, maxChars)) content = content.substring(0, Math.max(1, maxChars)).trim();
            return AiResponse.success(content);
        } catch (RuntimeException error) {
            return AiResponse.failure("Invalid AI response; endpoint must support OpenAI Chat Completions");
        }
    }

    static String readMessageContent(JsonObject message) {
        if (message == null) return "";
        JsonElement content = message.get("content");
        if (content == null || content.isJsonNull()) return "";
        if (content.isJsonPrimitive() && content.getAsJsonPrimitive().isString()) return content.getAsString().trim();
        if (content.isJsonArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonElement part : content.getAsJsonArray()) {
                if (!part.isJsonObject()) continue;
                JsonElement value = part.getAsJsonObject().get("text");
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    if (text.length() > 0) text.append(' ');
                    text.append(value.getAsString().trim());
                }
            }
            return text.toString().trim();
        }
        return "";
    }
}
