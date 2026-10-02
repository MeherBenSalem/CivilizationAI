# Smart Villagers AI 1.1.5

- Add provider presets for DeepSeek, NanoGPT pay-as-you-go and subscription, OpenAI, OpenRouter, Ollama and LM Studio, plus custom OpenAI-compatible endpoints.
- Fix prefixed base URLs, including NanoGPT's subscription API, by resolving the Chat Completions route correctly.
- Send DeepSeek-specific thinking controls only to DeepSeek. Add configurable completion token budgets and token parameter compatibility.
- Preserve existing custom endpoints and keys; select environment keys for the chosen provider. Local loopback servers can run without a key.
- Add useful authentication, model, quota, billing and network diagnostics while keeping keys, prompts and provider response bodies out of logs.
- Keep local dialogue fallback, privacy consent and optional voice support.

Supports Minecraft 1.20.1 (Fabric/Forge), 1.21.1 (Fabric/Forge/NeoForge) and 26.2 (Fabric/NeoForge).

NanoGPT subscription setup: choose `ai.provider = "NANOGPT_SUBSCRIPTION"`, use a NanoGPT API key and an exact model ID from its subscription-included model list. The mod does not automatically switch to a paid endpoint. See the [provider setup guide](https://github.com/MeherBenSalem/CivilizationAI/blob/main/docs/ai-providers.md).
