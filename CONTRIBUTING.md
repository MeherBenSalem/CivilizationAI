# Contributing

Thanks for helping improve Smart Villagers AI.

## How to contribute

1. Fork the repository and create a feature branch from `main`.
2. Make the change in `1.21.1/common/` (the reference workspace), then run `python3 tools/sync_common.py` to copy it to
   `1.20.1/`, `26.1.2/`, `26.2/` and `26.3/`. Anything whose name or shape differs between Minecraft versions belongs
   in `platform/Mc.java`, which has one copy per workspace.
3. Build every workspace you touched (`./gradlew build` from that folder, with Java 17 for 1.20.1, 21 for 1.21.1 and
   25 for 26.x), or all of them with `tools/build_release.sh`. This also runs the unit tests.
4. Run the in-game checks (below) on the builds you changed.
5. Open a pull request describing the change and how you tested it. Do not commit API keys, `secrets.toml` or
   personal paths.

## Layout

Each workspace is a MultiLoader Gradle build:

- `common/`: everything that matters.
  - `chat/`: proximity chat, activation, villager selection, etiquette, the conversation flow.
  - `ai/`: the OpenAI-compatible provider, prompt building, built-in dialogue.
  - `village/`: village requests, standing, news and the favour ledger. Mostly Minecraft-free and unit-tested;
    `RequestService` applies it in the world.
  - `display/`: chat lines and speech bubbles. `villager/`: persona, memory, context. `voice/`: optional speech.
  - `platform/Mc.java`: the version-specific calls. `mixin/`: villager data and text-display setters.
- `fabric/`, `forge/`, `neoforge/`: entry points and event wiring only.

Generated, not edited by hand:

```bash
python3 tools/datagen.py       # advancements and the language file, in each version's format
python3 tools/sync_common.py    # common code from 1.21.1 to the other workspaces
```

## Testing in game

`tools/qa/` drives dev servers over RCON and a real client under Xvfb (software rendering), with a mock
OpenAI-compatible provider so the AI path is tested without a key. It needs `xdotool`, ImageMagick (`import`) and
Mesa (26.3 clients need EGL: `libegl1 libegl-mesa0 libgl1-mesa-dri libgles2`).

```bash
Xvfb :1 -screen 0 1920x1080x24 &
python3 tools/qa/mock_ai.py &                    # 127.0.0.1:18080; logs every prompt to /tmp/sv-qa/ai-requests.jsonl
tools/qa/devrun.sh prepare-server 26.3 fabric    # eula, RCON 25575 (password qa), a fresh flat world, mod config -> mock
tools/qa/devrun.sh start-server 26.3 fabric
tools/qa/devrun.sh start-client 26.3 fabric      # joins localhost:25565 as "Builder"
python3 tools/qa/villager_checks.py --log /tmp/sv-qa/fabric-server.log --client-log /tmp/sv-qa/fabric-client.log \
    --world 26.3/fabric/runs/server/qa --shots /tmp/sv-qa/shots
tools/qa/devrun.sh stop-client; tools/qa/devrun.sh stop-server
```

`villager_checks.py` types into the real client's chat and checks: provider status, consent, an AI reply and the
prompt behind it, the name tag, the speech bubble (riding, showing the reply, expiring), a request offered, accepted
by saying yes, progress, finished (items or kills, emeralds, gossip, advancement, ledger), news reaching a neighbour
(AI prompt and built-in dialogue), built-in dialogue when the provider fails, the button commands, refusal after
hurting a villager, the cooldown, `/villagerai requests`, Hero of the Village Square after ten requests, loading
1.x villager data, removal of an orphaned bubble, and a clean server log.

`tools/qa/run_matrix.sh` does all of that on a fresh world for every build (eleven):

```bash
tools/qa/run_matrix.sh                         # all builds; results in /tmp/sv-qa/results/
tools/qa/run_matrix.sh 26.3:fabric 1.20.1:forge
```

Notes: the 1.20.1 Forge dev server needs `advertiseDedicatedServerToLan = false` (written by `prepare-server`); every
dev client joins as `Builder`; do not edit sources while a dev server is compiling.

## Coding expectations

- Keep saved data compatible: the villager NBT keys (`smartvillagers.Memory`, `PersonaOverride`, `Village`), the
  ledger file and config keys are what existing worlds and servers use.
- Keep logic Minecraft-free where it can be, and test it in `common/src/test`.
- The AI only words replies: anything with a gameplay effect is decided by the mod.
- Prefer small, reviewable PRs; say which versions and loaders you verified.

## Issues

Use GitHub Issues for bugs and feature requests. Include the Minecraft version, mod loader, mod version, AI provider
(never your key) and steps to reproduce.

## Licensing

By contributing, you agree that your contributions are licensed under the Apache License, Version 2.0.
Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).
