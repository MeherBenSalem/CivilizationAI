# Smart Villagers AI

Villagers you can talk to. Stand near a villager, speak in normal chat, and they answer — with optional DeepSeek AI and rich, editable personalities.

## Features

- **Proximity chat** — no chat commands needed to talk; activation is smart (look-at, name, greeting, nearby)
- **Personalities** — deterministic per-villager personas, plus player-editable overrides
- **DeepSeek AI** — OpenAI-compatible chat completions with local fallback dialogue
- **Privacy opt-in** — player consent before messages are sent to an AI provider
- **MultiLoader** — Fabric, Forge, and NeoForge on Minecraft 1.21.1

## Requirements

- Minecraft 1.21.1
- Java 21
- One of: Fabric Loader + Fabric API, Forge, or NeoForge
- Optional: a DeepSeek API key for AI replies

## Installation

1. Install the loader of your choice and drop the matching jar from [Releases](https://github.com/MeherBenSalem/CivilizationAI/releases) into `mods/`.
2. Set your API key (optional but recommended):

```bash
export DEEPSEEK_API_KEY=your_key_here
```

Or create `config/smartvillagers/secrets.toml`:

```toml
apiKey = "your_key_here"
```

Never commit API keys. Prefer the environment variable.

3. Start the game or dedicated server. On first join you may be asked to accept AI privacy consent.

## Usage

- Walk up to a villager and chat normally.
- Villager replies appear as proximity chat (or action bar, via config).
- Admin / persona commands (not required for talking):

| Command | Purpose |
|---------|---------|
| `/villagerai consent accept\|decline` | Privacy consent |
| `/villagerai persona get [villager]` | Show persona |
| `/villagerai persona set name\|trait\|style\|backstory <text>` | Edit looked-at villager |
| `/villagerai persona clear` | Reset to defaults |
| `/villagerai reload` | Reload config (op) |
| `/villagerai status` | Status (op) |

## Building

```bash
./gradlew build
```

Jars are produced under `fabric/build/libs/`, `forge/build/libs/`, and `neoforge/build/libs/`.

```bash
./gradlew :neoforge:runClient
./gradlew :fabric:runClient
./gradlew :forge:runClient
```

## Version branches

| Branch | Minecraft | Loaders |
|--------|-----------|---------|
| `main` | 1.21.1 | Fabric, Forge, NeoForge |
| `1.20.1` | 1.20.1 | Fabric, Forge |
| `26.2` | 26.2 | Fabric, NeoForge |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). By contributing you agree that your contributions are licensed under Apache-2.0.

## Security

See [SECURITY.md](.github/SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
