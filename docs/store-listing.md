# Store listing paste notes

Do **not** publish from this file. Copy the blocks below into CurseForge and Modrinth when you are ready.

Verified against this branch (`SmartVillagersConfig` `[voice]` keys, Simple Voice Chat soft-dep, `ApiCredentials`, loader `getConfigDirectory()`).

## Paste checklist

1. Open the mod page on [Modrinth](https://modrinth.com/) and/or [CurseForge](https://www.curseforge.com/).
2. Edit the **description** — replace the **Configuration**, **Installation**, and API FAQ sections with the [Description replacement](#description-replacement-markdown) block below. Leave marketing feature copy as-is unless you want a later pass.
3. Remove any mention of:
   - `[smartvillagers.api]` or `apiKey` inside the generated `config.toml`
   - downloading a “DeepSeek API” jar
   - right-clicking a villager to talk (use normal chat while standing nearby)
4. For **1.1.0** (voice output), paste the [1.1.0 changelog](#changelog-110-voice-output) when you upload the new jars. Do not publish from this repo.
5. Reply to comments with the blocks below (API/config, or Riri / “do they speak out loud?”).

## Changelog 1.1.0 (voice output)

Use when uploading 1.1.0 jars.

### Modrinth

```markdown
Optional spoken villager voice (text still default).

- Villagers stay text chat unless [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) is installed on server and clients.
- Config: `voice.enabled`, `voice.volume`, `voice.range` (0 = `proximity.responseRadius`), `voice.fallbackToText`.
- Language follows the AI reply / your Minecraft language. No separate TTS locale setting.
- Missing Simple Voice Chat = text-only, no crash.
```

### CurseForge

```text
Optional spoken villager voice (text still default).

- Villagers stay text chat unless Simple Voice Chat is installed on server and clients.
- Config: voice.enabled, voice.volume, voice.range (0 = proximity.responseRadius), voice.fallbackToText.
- Language follows the AI reply / your Minecraft language. No separate TTS locale setting.
- Missing Simple Voice Chat = text-only, no crash.
```

## Changelog (description-only, no new jar)

Use when you save an edited description without uploading a new file.

### Modrinth

```markdown
Documentation update (no new jar): corrected setup instructions.

- DeepSeek API key: create at https://platform.deepseek.com/api_keys (not a downloadable file).
- Gameplay settings: `config/smartvillagers/config.toml` (created on first launch).
- API key location: `DEEPSEEK_API_KEY` environment variable or `config/smartvillagers/secrets.toml` (never auto-generated).
- Talk to villagers with normal chat while standing nearby (not right-click).
```

### CurseForge

```text
Documentation update (no new jar): corrected setup instructions.

- DeepSeek API key: create at https://platform.deepseek.com/api_keys (not a downloadable file).
- Gameplay settings: config/smartvillagers/config.toml (created on first launch).
- API key location: DEEPSEEK_API_KEY environment variable or config/smartvillagers/secrets.toml (never auto-generated).
- Talk to villagers with normal chat while standing nearby (not right-click).
```

## CurseForge comment reply (Riri — speak out loud / other languages)

```text
Hi Riri — they use text chat by default.

Spoken voice is optional: install Simple Voice Chat (same loader as this mod) on the server and on clients, keep voice.enabled = true in config/smartvillagers/config.toml, and nearby players hear the villager through Voice Chat.

Language: there is no separate TTS language menu. Villagers answer in the same language you type. The AI prompt also gets your Minecraft language (en_us, fr_fr, ja_jp, …) and the spoken line is that same reply.

If Simple Voice Chat is not installed, nothing crashes — you just get text. voice.fallbackToText = true (default) still shows the chat line when they speak.
```

## CurseForge comment reply

Players asked: “how to download and add the API” and “where does the config generate?”

```text
There is no DeepSeek API jar to download.

1) Create a key on DeepSeek’s official console:
   https://platform.deepseek.com/api_keys
   (docs: https://api-docs.deepseek.com/)

2) Add it in ONE place (do not put it in the generated config.toml):
   • Environment variable DEEPSEEK_API_KEY
   • OR create config/smartvillagers/secrets.toml yourself:
     apiKey = "your_key_here"

The settings file is created on first game/server launch at:
  config/smartvillagers/config.toml
(relative to your Minecraft instance or dedicated server root — same config folder other mods use).

That generated file has gameplay settings only. secrets.toml is never auto-created.

Fabric builds also need the Fabric API *mod* in mods/ (Modrinth/CurseForge “Fabric API”). That is unrelated to DeepSeek.

Talk by standing near a villager and using normal chat. Then /villagerai reload after changing the key.
```

## Description replacement (Markdown)

Works on Modrinth as-is. CurseForge accepts the same Markdown in the description editor.

Copy everything from `### Configuration` through `### FAQ replacements` into the listing, replacing the old setup sections.

### Configuration

Smart Villagers AI creates **gameplay settings** on first launch:

`config/smartvillagers/config.toml`

That path is under your **Minecraft instance** or **dedicated server** root (the same `config` folder other mods use). The file is written only if it does not already exist.

The generated file does **not** include an API key. Do not look for `[smartvillagers.api]` or `apiKey` there.

Live AI replies need a DeepSeek **API key** (not a downloadable file):

1. Create a key at [https://platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys) — official page linked from [DeepSeek API Docs](https://api-docs.deepseek.com/).
2. Set `DEEPSEEK_API_KEY` on the game/server process, **or** create `config/smartvillagers/secrets.toml` yourself:

```toml
apiKey = "YOUR_DEEPSEEK_API_KEY"
```

`secrets.toml` is never generated automatically. Prefer the environment variable on servers. Never share your key.

Without a key, villagers still use local fallback dialogue.

### Installation

1. Install Fabric, Forge, or NeoForge for the file you downloaded.
2. **Fabric only:** also install [Fabric API](https://modrinth.com/mod/fabric-api) into `mods/`.
3. Place Smart Villagers AI in `mods/`.
4. Launch the game or server once. Confirm `config/smartvillagers/config.toml` appeared.
5. Optionally add a DeepSeek key (`DEEPSEEK_API_KEY` or `config/smartvillagers/secrets.toml`).
6. Restart or run `/villagerai reload`.
7. Stand near a villager and chat normally (not right-click).

### FAQ replacements

**Do I need to download the DeepSeek API?**
No. Create a key at [platform.deepseek.com/api_keys](https://platform.deepseek.com/api_keys) and add it as described above.

**Where is the config file?**
`<instance or server>/config/smartvillagers/config.toml`, created on first launch if missing.

**Do I need a DeepSeek API key?**
Only for live AI. Fallback dialogue works without one.

**Do villagers speak out loud?**
Text by default. Install Simple Voice Chat (server + clients) and keep `voice.enabled = true`. Language follows the chat/AI reply; there is no separate TTS language setting. Without Simple Voice Chat, text only (no crash).

Full paths and the real default `config.toml` keys: [README](../README.md).
