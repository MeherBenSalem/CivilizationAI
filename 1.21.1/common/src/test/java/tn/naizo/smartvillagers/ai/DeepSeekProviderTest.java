package tn.naizo.smartvillagers.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekProviderTest {
    @Test
    void resolvesSdkBaseUrlToChatCompletions() {
        assertEquals(
                "https://api.deepseek.com/chat/completions",
                DeepSeekProvider.resolveChatCompletionsUrl("https://api.deepseek.com"));
        assertEquals(
                "https://api.deepseek.com/chat/completions",
                DeepSeekProvider.resolveChatCompletionsUrl("https://api.deepseek.com/"));
        assertEquals(
                "https://api.deepseek.com/v1/chat/completions",
                DeepSeekProvider.resolveChatCompletionsUrl("https://api.deepseek.com/v1"));
    }

    @Test
    void keepsFullChatCompletionsUrl() {
        assertEquals(
                "https://api.deepseek.com/chat/completions",
                DeepSeekProvider.resolveChatCompletionsUrl(
                        "https://api.deepseek.com/chat/completions"));
    }

    @Test
    void parseResponseReadsMessageContent() {
        String body = """
                {"choices":[{"message":{"role":"assistant","content":"Hello traveler!"}}]}
                """;
        AiResponse response = DeepSeekProvider.parseResponse(200, body);
        assertTrue(response.ok());
        assertEquals("Hello traveler!", response.text());
    }

    @Test
    void parseResponseTreatsNullContentAsEmpty() {
        String body = """
                {"choices":[{"message":{"role":"assistant","content":null,"reasoning_content":"..."}}]}
                """;
        AiResponse response = DeepSeekProvider.parseResponse(200, body);
        assertTrue(!response.ok());
        assertEquals("Empty AI response", response.error());
    }
}
