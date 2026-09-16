# Changelog

## 1.1.4

- Add `ai.apiKey` to `config/smartvillagers/config.toml` (env `DEEPSEEK_API_KEY` and `secrets.toml` still override it).
- Disable DeepSeek thinking with `reasoning_effort=none` so villager replies are not empty.
- Show persona names as villager nametags on first talk.
- Nearby chat (within 6 blocks) activates without a perfect crosshair.
- Add `/villagerai test` to probe AI, identity, mixin, and Simple Voice Chat in-game.
- Fallback lines always include the villager’s name and profession.

## 1.1.2

- Fix config load crash when Night-Config stores whole numbers as `Integer` (e.g. `ai.playerCooldownMs = 3000`). That ClassCastException forced defaults and broke reload.
- Log DeepSeek HTTP / request failures at WARN so bad `ai.model` values are visible in logs.
- Log voice TTS / playback failures at WARN; `/villagerai debug` now shows real pending-queue and in-flight counts.

## 1.1.1

- Fix Forge/NeoForge optional Simple Voice Chat dependency version range. SVC reports versions like `1.21.1-2.5.35`, so `[2.5.0,)` wrongly blocked loading when Voice Chat was installed.

## 1.1.0

Optional spoken villager replies via Simple Voice Chat.

- Text chat remains the default. Simple Voice Chat is a soft dependency (missing mod = text-only, no crash, one startup log line).
- Nearby players hear a built-in villager voice through SVC entity audio when `voice.enabled = true`.
- Config: `voice.enabled`, `voice.volume`, `voice.range` (`0` = `proximity.responseRadius`), `voice.fallbackToText`.
- Language follows the AI reply and the player's Minecraft locale. There is no separate TTS language setting.
- `/villagerai status` reports voice config and whether Simple Voice Chat is present.
- Existing `config.toml` files gain a `[voice]` section on next load if it is missing.

## 1.0.0

Initial MultiLoader release (Fabric, Forge, NeoForge) for Minecraft 1.21.1.
