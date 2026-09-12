# Store listing paste notes

Do **not** publish from this file. Copy the blocks below into the CurseForge and Modrinth descriptions when you are ready.

Verified against `main` (`SmartVillagersConfig`, `ApiCredentials`, loader `getConfigDirectory()`). The live listings still describe a generated `[smartvillagers.api] apiKey` block and “right-click a villager”. Those do not match this code.

Replace the **Configuration**, **Installation**, and API FAQ sections with the text below. Leave marketing feature copy as-is unless you want a later pass.

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

Full paths and the real default `config.toml` keys: [README](../README.md).
