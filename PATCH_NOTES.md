# Smart Villagers AI 1.1.4

- Put your DeepSeek key in `config/smartvillagers/config.toml` as `ai.apiKey` (or keep using `DEEPSEEK_API_KEY` / `secrets.toml`).
- DeepSeek thinking is fully off so replies are not empty. Default model remains `deepseek-flash`.
- Villagers get a visible nametag from their persona.
- Stand next to a villager and chat — you no longer need a perfect crosshair.
- New command: `/villagerai test` (op). Stand next to a villager; it prints identity, API key source, and whether Simple Voice Chat is ready, then forces a reply.
- Fix replies being silently dropped: the delay queue was ticked once per dimension (overworld + nether + end), so the villager lookup often ran in the wrong world. `/villagerai test` now prints the reply and `Voice spoken: yes/no` directly.
- Villagers now **read the reply out loud** on Windows using the system TTS voice, played through Simple Voice Chat. Unmute the **Smart Villagers** volume category if you still hear nothing.

Voice still needs [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) on the server and client. Without it, villagers stay text-only and `/villagerai test` reports `Simple Voice Chat: missing (text only)`.
