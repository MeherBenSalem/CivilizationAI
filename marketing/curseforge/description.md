![Smart Villagers AI](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/banner.png)

**Villagers who talk back, remember you and ask for your help.** Walk up to a villager and chat normally. It answers
in character, in a speech bubble above its head, with the AI of your choice or with built-in dialogue when you have
none. Ask it for work and you get a real job: bring the harvest in, fetch iron for the forge, clear the zombies off
the fields. Finish it and you are paid in emeralds, the villager thinks better of you, and the whole street hears about it.

Fabric, Forge and NeoForge · Minecraft 1.20.1, 1.21.1, 26.1.2, 26.2 and 26.3 · works with any OpenAI-compatible AI
provider, a local model, or no AI at all

![What's new in 2.0](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-whats-new.png)

Version 2.0, **The Village Square Update**, makes talking to villagers part of the game instead of a novelty:

- **Village requests**: ask a villager for work in your own words, accept, finish it, get paid in emeralds and XP
- **Reputation and gossip**: finished requests improve your vanilla reputation (better prices); neighbours hear about
  what you did and bring it up; villagers you have hurt won't give you work
- **Speech bubbles** above the villager who is talking, and a "..." bubble while the AI thinks
- **Three advancements**, from your first chat to Hero of the Village Square
- **Smarter context**: biome, raids, nearby monsters, what you're holding, your standing and any open request
- **Better without AI**: the built-in dialogue now talks shop, notices night and rain, and passes on village news
- **Better manners**: villagers no longer answer messages meant for another player
- **Minecraft 26.1.2**, and your 1.x worlds and configs carry over

![Village requests](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-requests.png)

![Villagers who need you](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/card-requests.png)

Say **"any work for me?"** (or "need help?", "got any errands?", "¿tienes trabajo?", "as-tu une quête ?") to a
villager with a trade. It offers a request that fits its profession, or asks you to deal with the monsters around the
village. A card in chat shows the terms with **[Accept]** and **[Decline]**; you can also just say "yes".

| Profession | Asks for |
|------------|----------|
| Farmer | wheat, carrots, potatoes, beetroot, pumpkins, bone meal |
| Librarian | paper, books, feathers, ink sacs |
| Cartographer | paper, glass panes, compasses |
| Cleric | rotten flesh, gold ingots, redstone, glass bottles |
| Armorer, Weaponsmith, Toolsmith | iron ingots, coal, plus a lava bucket, flint or sticks |
| Butcher | porkchops, beef, chicken, mutton, sweet berries |
| Fisherman | cod, salmon, string, coal |
| Fletcher | sticks, flint, string, feathers |
| Leatherworker | leather, rabbit hide, flint |
| Shepherd | white wool, string, shears |
| Mason | clay, stone, quartz, andesite |
| Any of them | defeat zombies, skeletons, spiders, creepers or pillagers |

- Experienced villagers ask for more and pay more (a master asks for up to twice as much)
- Progress is counted from your inventory, or from your kills since you accepted: ask "how many?" any time
- Hand it in by talking to the villager again: the items are taken, emeralds and XP are paid
- `/villagerai requests` lists what you have open
- Up to 3 open requests, a short wait before the same villager asks again, nothing from nitwits or the unemployed

![Reputation and gossip](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-reputation.png)

![Word gets around](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/card-reputation.png)

Villagers read the reputation Minecraft already keeps for you. A finished request adds the same positive gossip that
trading does, so prices can drop, and villagers pass gossip to each other. Neighbours within 48 blocks hear about the
job ("I heard you brought 16 wheat for Kael the farmer") and bring it up when you talk to them.

Every villager treats you as a **stranger**, a **friend** (after a favour or good trading) or, after ten favours, a
**hero** of the village. Hurt a villager and it turns **wary** or **hostile**: it will not give you work until the bad
gossip fades.

![Speech bubbles](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-bubbles.png)

![See who's talking](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/card-bubbles.png)

Replies float above the head of the villager who said them, under its name. While the AI is thinking you see "...",
then the answer, for a few seconds depending on its length. Bubbles are vanilla text displays riding the villager, so
everyone standing nearby sees the conversation. Turn them off with `display.speechBubbles = false`.

![AI providers](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-ai.png)

![Any AI, or none](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/card-ai.png)

Pick a preset in `config/smartvillagers/config.toml`: **DeepSeek, OpenAI, OpenRouter, NanoGPT** (pay-as-you-go or
subscription), **Ollama** or **LM Studio** for a model on your own machine, or any **OpenAI-compatible endpoint**. With
no provider, requests, reputation, gossip and bubbles all still work with the built-in dialogue.

- The villager's prompt includes its persona and profession, your last conversations, the biome, time and weather,
  raids and nearby monsters, what you're holding, your standing, news it heard and any open request
- The AI only words the reply. Offers, progress and payments are decided by the mod, so nobody can talk a villager
  into handing out free emeralds
- Players accept AI consent once before their chat is sent to a provider; keys and prompts are never written to logs
- Rate limits per player and per minute, a concurrency cap and a reply length limit keep costs predictable
- Optional: villagers speak their replies out loud through Simple Voice Chat

![Advancements](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/card-advancements.png)

![Commands](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-commands.png)

| Command | Who | What |
|---------|-----|------|
| `/villagerai consent accept` or `decline` | everyone | Allow or refuse AI replies |
| `/villagerai requests` | everyone | Your open requests and progress |
| `/villagerai request accept` or `decline` | everyone | Answer the last offer (the chat buttons) |
| `/villagerai request assign <villager> [fetch or slay]` | op | Have a villager offer you a request |
| `/villagerai persona get / set / clear` | op (or everyone, if allowed) | A villager's name, trait, style, backstory and your standing |
| `/villagerai memory clear [villager]` | op | Forget conversations, requests and news |
| `/villagerai status`, `reload`, `test`, `debug` | op | Diagnostics |

![Configuration](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-config.png)

```toml
[display]
speechBubbles = true

[proximity]
ignorePlayerToPlayerChat = true

[requests]
enabled = true
maxActive = 3            # open requests per player
cooldownMinutes = 10     # before the same villager offers you another (20 = one Minecraft day)
reputationReward = 10    # vanilla positive gossip per finished request (0 to 25)

[ai]
provider = "DEEPSEEK"    # DEEPSEEK, NANOGPT, NANOGPT_SUBSCRIPTION, OPENAI, OPENROUTER, OLLAMA, LMSTUDIO, CUSTOM
model = "deepseek-flash"
apiKey = ""
```

Existing 1.x configs get the new keys on first start; nothing is renamed. The full list is in the
[README](https://github.com/MeherBenSalem/CivilizationAI#configuration) and the
[provider guide](https://github.com/MeherBenSalem/CivilizationAI/blob/main/docs/ai-providers.md).

![Downloads and compatibility](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-compatibility.png)

![Every loader, five versions](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/card-everywhere.png)

| Minecraft | Java | Loaders |
|-----------|------|---------|
| 1.20.1 | 17+ | Fabric (with Fabric API), Forge 47 |
| 1.21.1 | 21+ | Fabric (with Fabric API), Forge 52, NeoForge |
| 26.1.2 | 25+ | Fabric (with Fabric API), NeoForge |
| 26.2 | 25+ | Fabric (with Fabric API), NeoForge 26.2.0.93 or newer |
| 26.3 | 25+ | Fabric (with Fabric API), NeoForge 26.3.0.97 or newer |

Files are named `smartvillagers-<loader>-<minecraft>-2.0.0.jar`. Install the mod on the server, or in your game for
single player. Simple Voice Chat is optional (server and clients) for spoken replies.

![FAQ](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-faq.png)

**Do I need an AI key?** No. Without one, villagers use the built-in dialogue, and requests, reputation, gossip and
speech bubbles work exactly the same. With a key (or a local model) their replies are written by the AI.

**Does it cost money?** Only if your AI provider charges. Ollama and LM Studio run models on your own computer.

**Can the AI give me items or change prices?** No. The AI only writes what the villager says. Requests and rewards
come from the mod, which tells the AI what happened so the words match.

**Do I need a new world?** No. 1.x villagers keep their names, personas and memories, and 1.x configs are updated in
place.

**Will villagers answer when I talk to my friends?** Not when your message names another player who is online
(configurable). You can also require a prefix, or only answer villagers you look at.

**Is my chat sent anywhere?** Only to the AI provider you configure, and only after each player accepts with
`/villagerai consent accept`.

![Gallery](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/header-gallery.png)

![A village square](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/screenshots/square.jpg)

![A request](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/screenshots/offer.jpg)

![Request complete](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/screenshots/complete.jpg)

![News travels](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/screenshots/gossip.jpg)

![Divider](https://raw.githubusercontent.com/MeherBenSalem/CivilizationAI/main/marketing/images/divider.png)

[Discord](https://discord.gg/SAmd9fhNhr) · [Source and issues](https://github.com/MeherBenSalem/CivilizationAI) · [Support us](https://nightbeam.dev/support) · Made by NightBeam Studio
