# Smart Villagers AI

Villagers you can talk to. Stand near a villager, speak in normal chat, and they answer — with optional DeepSeek AI, optional spoken voice via Simple Voice Chat, and rich, editable personalities.

## Features

- **Proximity chat** — no chat commands needed to talk; activation is smart (look-at, name, greeting, nearby)
- **Personalities** — deterministic per-villager personas, plus player-editable overrides
- **Multiple AI providers** — DeepSeek, NanoGPT subscription/PAYG, OpenAI, OpenRouter, Ollama, LM Studio and custom Chat Completions endpoints, with local fallback dialogue
- **Spoken voice (optional)** — text chat by default; when [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) is installed, nearby players can hear the villager. Language follows the AI reply / your game language (no separate TTS locale setting)
- **Privacy opt-in** — player consent before messages are sent to an AI provider
- **MultiLoader** — Minecraft 1.20.1 (Fabric/Forge), 1.21.1 (Fabric/Forge/NeoForge), and 26.2 (Fabric/NeoForge)

## Requirements

- Minecraft 1.20.1, 1.21.1 or 26.2, matching your jar
- Java 17 (1.20.1), Java 21 (1.21.1) or Java 25 (26.2)
- One of: Fabric Loader + [Fabric API](https://modrinth.com/mod/fabric-api), Forge, or NeoForge
- Optional: a DeepSeek **API key** for live AI replies (not a downloadable jar)
- Optional: [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) on the **server and clients** if you want villagers to speak out loud (soft dependency — without it the mod stays text-only and will not crash)

There are two different “APIs” people mix up:

| What | Is it a download? | Where |
|------|-------------------|-------|
| **Fabric API** (Fabric builds only) | Yes — a separate Minecraft mod | [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api). Put it in `mods/` next to this mod. |
| **DeepSeek API** (live villager AI) | No. You create a key in DeepSeek’s web console. | Official docs: [Your First API Call](https://api-docs.deepseek.com/). Create a key at [platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys). |

Forge and NeoForge do **not** need Fabric API.

## Multiple AI providers

For NanoGPT subscriptions, other hosted providers and local models, see the [AI provider setup guide](docs/ai-providers.md). Version 1.1.5 adds provider presets, a custom endpoint mode and token compatibility controls. Choose a NanoGPT API key and an included model with `NANOGPT_SUBSCRIPTION`.

## DeepSeek API key (optional)

The DeepSeek API is **not** a file you download and drop into `mods/`. Create a key on DeepSeek’s platform, then give it to this mod.

1. Open the official key page: [https://platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys) (linked from [DeepSeek API Docs](https://api-docs.deepseek.com/)).
2. Sign in, create a key, and copy it. Do not share it or commit it.
3. Add the key using **one** of these (first match wins):

```bash
export DEEPSEEK_API_KEY=your_key_here
```

Or put it in the generated **`config/smartvillagers/config.toml`**:

```toml
[ai]
apiKey = "your_key_here"
```

Or create **`config/smartvillagers/secrets.toml`** yourself (this file is **not** generated):

```toml
apiKey = "your_key_here"
```

For `DEEPSEEK`, priority: `SMARTVILLAGERS_API_KEY`, then `DEEPSEEK_API_KEY` environment variable, then `secrets.toml`, then `config.toml` `ai.apiKey`. On a dedicated server, set the env var on the process, or edit the **server’s** `config/smartvillagers/` files.

Without a key, villagers still talk using local fallback dialogue.

## Voice output (optional)

Replies are **text by default**. Spoken audio is a soft extra:

1. Install [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) (same Minecraft version, matching Fabric / Forge / NeoForge) on the dedicated server **and** every client that should hear speech.
2. Leave `voice.enabled = true` in `config/smartvillagers/config.toml` (the default).
3. Stand near a villager. Nearby players hear a built-in villager voice through Simple Voice Chat, at `voice.range` (or `proximity.responseRadius` when range is `0`).

**Languages:** there is no separate TTS language setting. Villagers answer in the same language you used in chat. The AI prompt also receives your Minecraft client language (for example `en_us`, `fr_fr`, `ja_jp`). Spoken audio uses that same reply text.

Without Simple Voice Chat, or with `voice.enabled = false`, villagers stay text-only. The log prints this **once** on startup. `voice.fallbackToText = true` (default) still shows chat even when speech plays.

On **Windows**, replies are read aloud with the system TTS voice (SAPI) through Simple Voice Chat. On other OSes, a built-in villager voice is used. Unmute the **Smart Villagers** category in Simple Voice Chat if you hear nothing.

## Installation

1. Install the loader for your file (Fabric, Forge, or NeoForge). Fabric also needs [Fabric API](https://modrinth.com/mod/fabric-api) in `mods/`.
2. Drop the matching Smart Villagers AI jar from [Releases](https://github.com/MeherBenSalem/CivilizationAI/releases) (or CurseForge / Modrinth) into `mods/`.
3. Start the game or dedicated server **once**. The mod creates the settings file on first load (see below).
4. Optionally add a DeepSeek key as described above. Then restart or run `/villagerai reload`.
5. Walk up to a villager and chat normally. On first join you may be asked to accept AI privacy consent.

## Config file (path and first-run)

On first successful load the mod creates:

```text
<instance or server root>/config/smartvillagers/config.toml
```

That is the loader config directory (`FMLPaths.CONFIGDIR` on Forge/NeoForge, `FabricLoader.getConfigDir()` on Fabric) plus `smartvillagers/config.toml`.

Examples:

- Singleplayer / most launchers: the instance folder’s `config/smartvillagers/config.toml` (same folder that already has other mods’ configs).
- Dedicated server: `<server>/config/smartvillagers/config.toml`.

**First-run behavior** (from `SmartVillagersConfig.load()`):

- The `config/smartvillagers/` directory is created if missing.
- If `config.toml` does **not** exist, defaults are written and saved.
- If `config.toml` already exists, it is loaded and **not** overwritten, except missing voice/API-key settings and provider compatibility settings are added during migration. Existing model/key/custom endpoint values are preserved.
- Put a DeepSeek key in `ai.apiKey`, or override it with `DEEPSEEK_API_KEY` / `secrets.toml`.
- `secrets.toml` is never created automatically.

Generated defaults look like this (key order may vary):

```toml
[proximity]
enabled = true
hearingRadius = 12.0
responseRadius = 16.0
activationMode = "SMART"
chatPrefix = "!"
requirePrefix = false

[display]
mode = "CHAT"
cancelGlobalChatWhenTalking = false

[privacy]
requirePlayerOptIn = true

[ai]
provider = "DEEPSEEK"
maxTokens = 2048
tokenLimitParameter = "AUTO"
endpointMode = "AUTO"
maxReplyChars = 180
playerCooldownMs = 3000
globalRequestsPerMinute = 30
maxConcurrent = 3
thinkingDelayMinTicks = 20
thinkingDelayMaxTicks = 60
apiBaseUrl = "https://api.deepseek.com/chat/completions"
model = "deepseek-flash"
apiKey = ""

[persona]
allowPlayersEditPersona = false

[voice]
enabled = true
volume = 1.0
range = 0.0
fallbackToText = true
```

`voice.range = 0` means “use `proximity.responseRadius`”. Existing 1.0.x configs get this section written on the next load if it is missing.

After editing, run `/villagerai reload` or restart. `/villagerai status` shows whether a key is configured and which source was used (`environment`, `secrets.toml`, `config.toml`, or `none`), plus whether Simple Voice Chat is present. `/villagerai test` (op) forces a nearby villager to reply and prints identity, API, and voice diagnostics.

## Usage

- Walk up to a villager and chat normally.
- Villager replies appear as proximity chat (or action bar, via config). If Simple Voice Chat is installed and voice is enabled, the same reply is also spoken to nearby players.
- Admin / persona commands (not required for talking):

| Command | Purpose |
|---------|---------|
| `/villagerai consent accept\|decline` | Privacy consent |
| `/villagerai persona get [villager]` | Show persona |
| `/villagerai persona set name\|trait\|style\|backstory <text>` | Edit looked-at villager |
| `/villagerai persona clear` | Reset to defaults |
| `/villagerai reload` | Reload config (op) |
| `/villagerai status` | Status (op) |
| `/villagerai test [villager]` | Force a reply and print AI / identity / voice diagnostics (op) |

## Building

```bash
./gradlew buildAll
```

Release jars are collected into `all-jars/`. Each Minecraft workspace also has loader-specific `build/libs/` directories. Set `JAVA_HOME_17`, `JAVA_HOME_21` and `JAVA_HOME_25` for the full build.

```bash
./gradlew :neoforge:runClient
./gradlew :fabric:runClient
./gradlew :forge:runClient
```

## Minecraft workspaces

| Directory | Minecraft | Loaders |
|--------|-----------|---------|
| `1.21.1/` | 1.21.1 | Fabric, Forge, NeoForge |
| `1.20.1/` | 1.20.1 | Fabric, Forge |
| `26.2/` | 26.2 | Fabric, NeoForge |

## Store listings

Paste-ready CurseForge / Modrinth notes (not published from this repo): [docs/store-listing.md](docs/store-listing.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). By contributing you agree that your contributions are licensed under Apache-2.0.

## Security

See [SECURITY.md](.github/SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
