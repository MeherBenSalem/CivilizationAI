# Contributing to Smart Villagers AI

Thanks for helping improve the mod.

## Ground rules

- By contributing, you agree that your contributions are licensed under the **Apache License 2.0**.
- Keep changes focused. Prefer small pull requests.
- Do not commit API keys, `secrets.toml`, or personal paths.

## Development setup

1. Fork and clone the repository.
2. Use **Java 21** for the `main` (1.21.1) branch, or **Java 17** for `1.20.1`.
3. Import the Gradle project and run:

```bash
./gradlew build
./gradlew :fabric:runClient   # or :forge:runClient (on 1.20.1); :neoforge:runClient on main
```

Optional AI testing:

```bash
export DEEPSEEK_API_KEY=your_key
./gradlew :common:test
```

## Making changes

1. Create a branch from the Minecraft version branch you are targeting (`main`, `1.20.1`, or `26.2`).
2. Put shared logic in `common/`. Keep loader modules thin (events, ServiceLoader, metadata).
3. Add or update unit tests for pure logic when practical.
4. Run `./gradlew build` before opening a PR.

## Pull requests

- Describe what changed and how you tested it.
- Mention any loader-specific behavior.
- Link related issues.

## Code of conduct

Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).
