# Changelog

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
