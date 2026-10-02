# AI providers (Smart Villagers AI 1.1.5+)

Edit the **server or singleplayer instance's** `config/smartvillagers/config.toml`,
then restart or run `/villagerai reload`. Keep only one `[ai]` section.
Use `/villagerai status` to inspect the provider, resolved endpoint and key source.
`/villagerai test` is an operator diagnostic that sends a real request even without
player consent; normal chat still requires consent.

## NanoGPT subscription with a DeepSeek model

Yes, NanoGPT supports using a personal subscription through its API.
Create an API key **in NanoGPT**, then select an exact included model ID from
`GET https://nano-gpt.com/api/subscription/v1/models?detailed=true`
(use your NanoGPT Bearer key). Copy the model's `id`, not its display name.
The available DeepSeek IDs can change; the direct DeepSeek ID is not necessarily
the NanoGPT subscription ID. See [NanoGPT's official support page](https://nano-gpt.com/support).

Change these settings in your existing `[ai]` section:

```toml
[ai]
provider = "NANOGPT_SUBSCRIPTION"
model = "deepseek-chat"
apiKey = "REPLACE_WITH_YOUR_NANOGPT_API_KEY"
maxTokens = 2048
tokenLimitParameter = "AUTO"
```

On 2 October 2026, NanoGPT's subscription model catalog listed `deepseek-chat`
as a non-reasoning model. It is an example, not a guarantee of future account
availability; confirm the ID in the current catalog before using it.

This preset always sends to
`https://nano-gpt.com/api/subscription/v1/chat/completions`.
It ignores an old `apiBaseUrl`, sends no upstream `provider` selection, and never
switches automatically to the pay-as-you-go endpoint. Account billing/overage
settings still apply; disable paid overage in NanoGPT if desired.
A personal subscription is for personal use. NanoGPT directs shared or multi-user
services to pay-as-you-go/team billing; choose the appropriate account and preset
for a public Minecraft server.

## Provider presets

All presets use **OpenAI-compatible Chat Completions**. Native Anthropic Messages,
Google Gemini and OpenAI Responses formats are different protocols; use a gateway
exposing Chat Completions to connect those providers.

| `ai.provider` | Chat Completions endpoint | Provider key environment variable |
|---|---|---|
| `DEEPSEEK` | `https://api.deepseek.com/chat/completions` | `DEEPSEEK_API_KEY` |
| `NANOGPT` (pay-as-you-go) | `https://nano-gpt.com/api/v1/chat/completions` | `NANOGPT_API_KEY` |
| `NANOGPT_SUBSCRIPTION` | `https://nano-gpt.com/api/subscription/v1/chat/completions` | `NANOGPT_API_KEY` |
| `OPENAI` | `https://api.openai.com/v1/chat/completions` | `OPENAI_API_KEY` |
| `OPENROUTER` | `https://openrouter.ai/api/v1/chat/completions` | `OPENROUTER_API_KEY` |
| `OLLAMA` | `http://localhost:11434/v1/chat/completions` | Optional local key |
| `LMSTUDIO` | `http://localhost:1234/v1/chat/completions` | Optional local key |
| `CUSTOM` | `ai.apiBaseUrl` | General environment variable or TOML key |

Keys belong to the endpoint's provider: a NanoGPT key cannot authenticate with
DeepSeek's direct API. Set `ai.model` to the exact ID offered by your provider.
For local presets, start the local server and load/download that model first.
For a local server on another machine or port, select `CUSTOM`.

```toml
[ai]
provider = "CUSTOM"
endpointMode = "AUTO"
apiBaseUrl = "https://your-gateway.example/api/v1"
model = "your-model-id"
apiKey = "your-provider-key"
```

A base URL is expanded with `/chat/completions`; a full URL ending in
`/chat/completions` is preserved. Set `ai.endpointMode = "FULL"` to use a complete
nonstandard route such as `/proxy/chat`; `AUTO` expands base URLs. Legacy custom
routes are migrated to `FULL` when the old client treated them as full URLs.
URL credentials, query strings and fragments are rejected. Remote endpoints
require HTTPS. Only literal localhost/127.0.0.1/::1 endpoints can use HTTP or run without a
key; use a key (or your local server's placeholder key) for remote hosts.

Credential priority: `SMARTVILLAGERS_API_KEY`, the **selected** provider's
environment variable, `config/smartvillagers/secrets.toml` (`apiKey`), then
`config.toml` (`ai.apiKey`). The legacy `DEEPSEEK_API_KEY` continues to work with
`DEEPSEEK`; it is not sent to other providers. When switching providers, replace
any general environment or TOML key still belonging to the previous provider.

Existing configurations gain `ai.provider`, `ai.maxTokens` and
`ai.tokenLimitParameter` without replacing their model or API key. Existing custom
endpoints are migrated to `CUSTOM`. Presets select their own endpoint; edit
`apiBaseUrl` only when using `CUSTOM`.

## Model parameters and troubleshooting

- `ai.maxTokens` controls the completion budget, including reasoning (default
  2048, range 1–32768). `ai.maxReplyChars` independently clips the villager's line.
- `ai.tokenLimitParameter = "AUTO"` uses `max_completion_tokens` for `OPENAI`
  and `max_tokens` for the other presets. Override with `max_tokens`,
  `max_completion_tokens` or `NONE` if your model/server requires it.
- DeepSeek thinking-disable fields are sent only with the `DEEPSEEK` preset.
- 401/403: check the selected provider's key, key source and permissions.
- 402: check subscription/balance and the API key's billing restrictions.
- 404: check the endpoint and exact model ID.
- 400/422: check model parameters and the Chat Completions protocol.
- 429: check rate limits and subscription allowance; wait for reset if exhausted.
- An empty reasoning-only completion needs more `maxTokens` or a non-thinking
  model. Reasoning text is never used as a villager's spoken reply.
- Network timeout, malformed URL or unsupported response: local dialogue is
  used. Provider response bodies and private prompts are not logged.

Official references: [DeepSeek](https://api-docs.deepseek.com/),
[NanoGPT Chat Completions](https://docs.nano-gpt.com/api-reference/endpoint/chat-completion),
[OpenAI](https://developers.openai.com/api/reference/resources/chat),
[OpenRouter](https://openrouter.ai/docs/api_reference/overview),
[Ollama](https://docs.ollama.com/api/openai-compatibility),
[LM Studio](https://lmstudio.ai/docs/developer/openai-compat).
