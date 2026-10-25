package tn.naizo.smartvillagers.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tn.naizo.smartvillagers.config.ProviderTestConfig;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ProviderHttpTest {
    @Test
    void rejectsRemoteCleartextEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenAiCompatibleProvider.resolveChatCompletionsUrl("http://gateway.example/v1"));
    }

    @Test
    void fullEndpointModeKeepsNonstandardGatewayRoute() {
        assertEquals("https://gateway.example/proxy/chat",
                OpenAiCompatibleProvider.resolveChatCompletionsUrl("https://gateway.example/proxy/chat", "FULL"));
        assertThrows(IllegalArgumentException.class,
                () -> OpenAiCompatibleProvider.resolveChatCompletionsUrl("https://gateway.example/proxy/chat", "invalid"));
    }

    @Test
    void postsBearerAuthenticatedChatToPrefixedBaseAndParsesReply() throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<JsonObject> payload = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            payload.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
            byte[] reply = "{\"choices\":[{\"message\":{\"content\":\"Hello traveler!\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        try {
            var config = ProviderTestConfig.snapshot("CUSTOM", "http://127.0.0.1:" + server.getAddress().getPort() + "/api/subscription/v1/", "AUTO");
            var result = OpenAiCompatibleProvider.send(config, "test-key", "system", "hello").get(10, TimeUnit.SECONDS);
            assertTrue(result.ok(), result.error());
            assertEquals("Hello traveler!", result.text());
            assertEquals("/api/subscription/v1/chat/completions", path.get());
            assertEquals("Bearer test-key", auth.get());
            assertEquals("test-model", payload.get().get("model").getAsString());
            assertFalse(payload.get().has("thinking"));
            assertFalse(payload.get().has("reasoning_effort"));
            assertFalse(payload.get().has("provider"));
            assertFalse(payload.get().get("stream").getAsBoolean());
            assertEquals("hello", payload.get().getAsJsonArray("messages").get(1).getAsJsonObject().get("content").getAsString());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void loopbackCanRunWithoutKeyButRemoteCannot() throws Exception {
        var remote = ProviderTestConfig.snapshot("CUSTOM", "https://example.com/v1", "AUTO");
        assertTrue(OpenAiCompatibleProvider.send(remote, "", "s", "u").get().error().contains("API key not configured"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> auth = new AtomicReference<>();
        server.createContext("/", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] reply = "{\"choices\":[{\"message\":{\"content\":\"Local reply\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        try {
            var local = ProviderTestConfig.snapshot("CUSTOM", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1", "AUTO");
            assertTrue(OpenAiCompatibleProvider.send(local, "", "s", "u").get(10, TimeUnit.SECONDS).ok());
            assertNull(auth.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void malformedConfigReturnsFailureWithoutThrowingOrExposingSecrets() throws Exception {
        for (String url : new String[]{"bad url", "https://secret@example.com/v1", "https://example.com/v1?key=secret", "ftp://example.com/v1"}) {
            var result = OpenAiCompatibleProvider.send(ProviderTestConfig.snapshot("CUSTOM", url, "AUTO"), "secret", "s", "u").get();
            assertFalse(result.ok());
            assertFalse(result.error().contains("secret"));
        }
        assertFalse(OpenAiCompatibleProvider.send(ProviderTestConfig.snapshot("UNKNOWN", "", "AUTO"), "secret", "s", "u").get().ok());
    }

    @Test
    void providerPayloadsUseOnlyCompatibleExtensions() {
        for (String provider : new String[]{"NANOGPT", "NANOGPT_SUBSCRIPTION", "OPENAI", "OPENROUTER", "OLLAMA", "LMSTUDIO", "CUSTOM"}) {
            var config = ProviderTestConfig.snapshot(provider, "https://example.com/v1", "AUTO");
            var body = JsonParser.parseString(OpenAiCompatibleProvider.buildRequestBody(config, "s", "u")).getAsJsonObject();
            assertFalse(body.has("thinking"), provider);
            assertFalse(body.has("reasoning_effort"), provider);
            assertFalse(body.has("provider"), provider);
            assertTrue(body.has(provider.equals("OPENAI") ? "max_completion_tokens" : "max_tokens"), provider);
        }
        var body = JsonParser.parseString(OpenAiCompatibleProvider.buildRequestBody(
                ProviderTestConfig.snapshot("CUSTOM", "https://example.com", "NONE"), "s", "u")).getAsJsonObject();
        assertFalse(body.has("max_tokens"));
        assertFalse(body.has("max_completion_tokens"));
        assertThrows(IllegalArgumentException.class, () -> OpenAiCompatibleProvider.buildRequestBody(
                ProviderTestConfig.snapshot("CUSTOM", "https://example.com", "bad"), "s", "u"));
    }

    @Test
    void providerErrorsAreHelpfulAndDoNotEchoResponseBody() {
        for (int status : new int[]{400, 401, 402, 403, 404, 422, 429, 500}) {
            var response = OpenAiCompatibleProvider.parseResponse(status, "{\"error\":\"secret key and private prompt\"}");
            assertFalse(response.ok());
            assertTrue(response.error().contains(String.valueOf(status)));
            assertFalse(response.error().contains("secret"));
            assertFalse(response.error().contains("private"));
        }
        assertFalse(OpenAiCompatibleProvider.parseResponse(200, "not json").ok());
        assertFalse(OpenAiCompatibleProvider.parseResponse(200, "{\"error\":\"secret\"}").ok());
    }
}
