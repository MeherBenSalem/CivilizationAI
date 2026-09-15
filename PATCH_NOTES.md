# Smart Villagers AI 1.1.2

- Fix config load crash: Night-Config stores `ai.playerCooldownMs = 3000` as Integer, but the mod cast it as Long and fell back to defaults (reload broken).
- Warn-log DeepSeek HTTP failures (wrong `ai.model` / key issues).
- Warn-log voice TTS/playback failures; `/villagerai debug` shows real pending and in-flight counts.

Recommended model: `deepseek-flash` (not `deepseek-flash`).
