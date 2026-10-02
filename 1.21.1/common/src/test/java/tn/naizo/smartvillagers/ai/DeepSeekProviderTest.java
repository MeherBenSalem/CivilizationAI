package tn.naizo.smartvillagers.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleProviderTest {
    @Test
    void resolvesNanoGptSubscriptionBaseUrl() {
        assertEquals("https://nano-gpt.com/api/subscription/v1/chat/completions",
                OpenAiCompatibleProvider.resolveChatCompletionsUrl("https://nano-gpt.com/api/subscription/v1/"));
    }

    @Test
    void resolvesSdkBaseUrlToChatCompletions() {
        assertEquals(
                "https://api.deepseek.com/chat/completions",
                OpenAiCompatibleProvider.resolveChatCompletionsUrl("https://api.deepseek.com"));
        assertEquals(
                "https://api.deepseek.com/chat/completions",
                OpenAiCompatibleProvider.resolveChatCompletionsUrl("https://api.deepseek.com/"));
        assertEquals(
                "https://api.deepseek.com/v1/chat/completions",
                OpenAiCompatibleProvider.resolveChatCompletionsUrl("https://api.deepseek.com/v1"));
    }

    @Test
    void keepsFullChatCompletionsUrl() {
        assertEquals(
                "https://api.deepseek.com/chat/completions",
                OpenAiCompatibleProvider.resolveChatCompletionsUrl(
                        "https://api.deepseek.com/chat/completions"));
    }

    @Test
    void parseResponseReadsMessageContent() {
        String body = """
                {"choices":[{"message":{"role":"assistant","content":"Hello traveler!"}}]}
                """;
        AiResponse response = OpenAiCompatibleProvider.parseResponse(200, body);
        assertTrue(response.ok());
        assertEquals("Hello traveler!", response.text());
    }

    @Test
    void parseResponseTreatsNullContentAsEmpty() {
        String body = """
                {"choices":[{"message":{"role":"assistant","content":null,"reasoning_content":"..."}}]}
                """;
        AiResponse response = OpenAiCompatibleProvider.parseResponse(200, body);
        assertTrue(!response.ok());
        assertTrue(response.error().startsWith("Empty AI response"));
    }

    @Test
    void requestDisablesThinkingAndSetsReasoningEffortNone() {
        String json = OpenAiCompatibleProvider.buildRequestBody(tn.naizo.smartvillagers.config.SmartVillagersConfig.Snapshot.defaults(), "sys", "hi");
        JsonObject body = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("deepseek-flash", body.get("model").getAsString());
        assertEquals("disabled", body.getAsJsonObject("thinking").get("type").getAsString());
        assertEquals("none", body.get("reasoning_effort").getAsString());
        assertTrue(body.get("max_tokens").getAsInt() >= 256);
        assertEquals("sys", body.getAsJsonArray("messages").get(0).getAsJsonObject().get("content").getAsString());
        assertEquals("hi", body.getAsJsonArray("messages").get(1).getAsJsonObject().get("content").getAsString());
    }

    @Test
    void parseResponseReadsArrayContent() {
        String body = """
                {"choices":[{"message":{"role":"assistant","content":[{"type":"text","text":"Hello traveler!"}]}}]}
                """;
        AiResponse response = OpenAiCompatibleProvider.parseResponse(200, body);
        assertTrue(response.ok());
        assertEquals("Hello traveler!", response.text());
    }
}
