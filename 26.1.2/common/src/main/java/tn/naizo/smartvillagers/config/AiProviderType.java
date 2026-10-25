package tn.naizo.smartvillagers.config;

import java.net.URI;
import java.util.Locale;

/** Presets use OpenAI Chat Completions, with CUSTOM for other compatible servers. */
public enum AiProviderType {
    DEEPSEEK("https://api.deepseek.com/chat/completions", "DEEPSEEK_API_KEY"),
    NANOGPT("https://nano-gpt.com/api/v1/chat/completions", "NANOGPT_API_KEY"),
    NANOGPT_SUBSCRIPTION("https://nano-gpt.com/api/subscription/v1/chat/completions", "NANOGPT_API_KEY"),
    OPENAI("https://api.openai.com/v1/chat/completions", "OPENAI_API_KEY"),
    OPENROUTER("https://openrouter.ai/api/v1/chat/completions", "OPENROUTER_API_KEY"),
    OLLAMA("http://localhost:11434/v1/chat/completions", ""),
    LMSTUDIO("http://localhost:1234/v1/chat/completions", ""),
    CUSTOM("", ""),
    INVALID("", "");

    private final String endpoint;
    private final String keyEnvironment;

    AiProviderType(String endpoint, String keyEnvironment) {
        this.endpoint = endpoint;
        this.keyEnvironment = keyEnvironment;
    }

    public String endpoint(String customUrl) {
        return this == CUSTOM ? customUrl : endpoint;
    }

    public String keyEnvironment() {
        return keyEnvironment;
    }

    public String tokenParameter() {
        return this == OPENAI ? "max_completion_tokens" : "max_tokens";
    }

    public static AiProviderType parse(String value, String legacyEndpoint) {
        if (value == null || value.isBlank()) {
            // Old configs may already point at another service. Preserve that URL.
            try {
                return "api.deepseek.com".equalsIgnoreCase(URI.create(legacyEndpoint).getHost())
                        ? DEEPSEEK : CUSTOM;
            } catch (RuntimeException ignored) {
                return CUSTOM;
            }
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return INVALID;
        }
    }
}
