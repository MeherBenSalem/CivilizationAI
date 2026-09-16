# Smart Villagers AI 1.1.4

- Put your DeepSeek key in `config/smartvillagers/config.toml` as `ai.apiKey` (or keep using `DEEPSEEK_API_KEY` / `secrets.toml`).
- DeepSeek thinking is fully off so replies are not empty. Default model remains `deepseek-flash`.
- Villagers get a visible nametag from their persona.
- Stand next to a villager and chat — you no longer need a perfect crosshair.
- New command: `/villagerai test` (op). Stand next to a villager; it prints identity, API key source, and whether Simple Voice Chat is ready, then forces a reply.

Voice still needs [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) on the server and client. Without it, villagers stay text-only and `/villagerai test` reports `Simple Voice Chat: missing (text only)`.
