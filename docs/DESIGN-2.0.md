# Smart Villagers AI 2.0: audit, market gaps and plan

This document records what 1.1.6 actually did (read from the code, not the store page), what players expect from a
"talk to villagers" mod today, where 1.1.6 fell short, and what 2.0 changes. Every 2.0 claim below is implemented
and checked in game by `tools/qa/villager_checks.py` on all eleven builds (see [QA-2.0.0](QA-2.0.0.md)).

## 1. Audit of 1.1.6

| Area | What the code did |
|------|-------------------|
| Chat | Proximity chat: a message near villagers (12 blocks) picks the best one by distance, crosshair and name. In `SMART` mode **any** greeting or question near a villager activated it, including messages players sent to each other. |
| AI | One OpenAI-compatible transport with presets (DeepSeek, NanoGPT, OpenAI, OpenRouter, Ollama, LM Studio, custom). Async, rate-limited, keys kept out of logs. Solid. |
| Prompt | Persona, profession flavour, rapport number, recent chat with that player, dimension, time, weather, the player's name and the villager's coordinates. |
| Memory | Per villager, per player: the last 8 exchanges and a "rapport" counter that went up by one per message and was never used for anything but the prompt. |
| Consequences | **None.** Nothing said to a villager changed anything in the world: no items, no prices, no reputation, no quests. The villager was a chatbot standing in a village. |
| Without a key | Fallback lines from 17 generic sentences, prefixed with "Name the profession:" inside a chat line that already started with "[Profession] Name:" (the name appeared twice). |
| Display | Chat line or action bar. Nothing in the world showed who was talking. |
| Identity | Every villager talked to gets a permanent visible name tag (kept, players like it). |
| Store page vs code | The CurseForge page promised memory of trades and reputation changes, reputation from defending villages, awareness of biome, equipment and nearby danger, `/villagerai info` and `/villagerai toggle`. None of these existed. Its roadmap promised "Dynamic Villager Quests" and "Village Gossip Networks". |
| Versions | 1.20.1 (Fabric, Forge), 1.21.1 (Fabric, Forge, NeoForge), 26.2 and 26.3 (Fabric, NeoForge). No 26.1.2. NeoForge 26.2 built against the first beta (26.2.0.1-beta). |
| Release names | Forge 1.21.1 jars were named `Smart Villagers AI-forge-...` (spaces), the others `smartvillagers-...`; no `releases/` folder. |
| Tooling | 80 unit tests; no in-game QA beyond "the server boots"; the publish workflow built everything on CI and had no jar-name detector or listing update. |

## 2. What players expect from an AI villager mod

Gathered from the store pages, comment sections and issue trackers of the AI-NPC mods in this space, and from the
long-running wishes on our own page (its roadmap is the most-asked list). Recurring expectations:

1. **Talking should matter.** The most common reaction to AI-NPC mods is that once the novelty wears off the villager
   is "just a chatbot": nothing in the game changes. The mods that try to fix this add quests, friendship or gifts.
2. **It must work without paying.** Most mods in the space require a paid cloud key and some have cost tables in
   their description; modpack makers note that an AI pack costs players a monthly fee. Players want local models
   and a game that still plays without any key.
3. **Villagers should know the world and the player**: biome, weather, time, danger, what the player holds, and
   above all how the player has treated the village (vanilla already tracks this as gossip and reputation).
4. **Word should spread**: villagers talking about the player to each other (our own "gossip network" promise).
5. **See who is talking.** Chat lines from several villagers are hard to follow; players ask for speech above heads.
6. **Multiplayer manners**: villagers must not answer messages players send to each other; server-side only, no
   client mod needed; rate limits and privacy consent.
7. **Every loader and version.** Most AI-villager mods are Fabric-only and target one Minecraft version; several are
   single-player only or rely on Windows-only speech.
8. **Honest pages**: what the description says should be in the game.

## 3. Gap table

| Expectation | 1.1.6 | 2.0 |
|-------------|-------|-----|
| Talking has consequences | nothing changes | **Village requests**: ask "any work for me?", the villager offers a profession-themed job (bring items or defeat mobs), you accept by saying yes or clicking [Accept], and finishing it pays emeralds and XP |
| Works without a key | 17 generic lines | Requests are fully playable without AI. Local dialogue gets profession small talk, reacts to night, rain, standing and news, and the duplicated name is gone |
| Reputation | a rapport counter only the AI saw | Uses vanilla reputation: finished requests add vanilla minor-positive gossip (part of the reputation that sets prices; villagers share gossip); villagers you hurt refuse work and say so |
| Gossip network | roadmap item | Neighbours within 48 blocks hear about a finished request and bring it up (AI prompt and local dialogue) |
| Renown | none | A per-world ledger of favours: villagers everywhere treat you as stranger, friend or hero; three advancements (Smart Villagers, A Favor for a Friend, Hero of the Village Square) |
| See who talks | chat only | **Speech bubbles** above the villager (vanilla text displays riding it: every player sees them, no client mod), a "..." bubble while the AI thinks |
| World awareness | dimension, time, weather | + biome, raids, hostile mobs nearby, the player's held item and health, the villager's level, the player's standing, favours, news and the open request |
| Player-to-player chat | villagers butted in | Messages that name another online player (and not the villager) are left alone (configurable) |
| AI and mechanics | n/a | The AI only words the reply; the mod decides offers, progress and rewards and tells the AI what happened, so a model cannot hand out free emeralds or invent quests |
| Commands | as listed in 1.1.6 | + `/villagerai requests`, `/villagerai request accept/decline` (the chat buttons), op `/villagerai request assign <villager> [fetch/slay]`; `persona get` shows standing and the open request |
| Versions | 1.20.1, 1.21.1, 26.2, 26.3 | + **26.1.2** (Fabric, NeoForge); NeoForge 26.2.0.93 and 26.3.0.97, ModDevGradle 2.0.147 |
| Store page | claims not in the code | Rewritten: every claim checked against code and QA |
| Tests / QA | 80 unit tests, boot checks | 106 unit tests; an in-game QA script (real server, real client typing in chat, a mock AI provider) on all 11 builds |

## 4. The plan as built

### The headline: village requests

The gap that stands out is that AI villager mods make villagers *talk* but not *matter*. 2.0 turns conversation into
gameplay without making the AI responsible for it:

* **Ask** any working villager for work in your own words ("any work for me?", "need help?", "¿tienes trabajo?").
  Intent is recognised locally (`RequestIntent`), so this works with no AI at all.
* **The offer** comes from `RequestCatalog`: 13 professions, each with its own wants (a farmer asks for wheat or
  pumpkins, a librarian for paper or books, a mason for stone...), and about a third of requests are village defence
  (zombies, skeletons, spiders, creepers, pillagers). Master villagers ask for more and pay more. A clickable card
  shows the terms with [Accept] and [Decline].
* **Progress** is measured from the game itself: items in your inventory, or your vanilla kill statistics since you
  accepted. No event hooks per loader, identical on every version.
* **Finishing** takes the items, pays emeralds and XP, adds vanilla minor-positive gossip (part of the
  reputation that sets trade prices; it spreads between villagers), records a favour in the world's ledger, grants
  advancements, and tells the neighbours.
* **Limits**: three open requests per player, a ten-minute cooldown per villager, offers withdrawn after two minutes,
  no work from villagers you have hurt (vanilla reputation -30 or lower), no work from nitwits or unemployed villagers.
  All configurable under `[requests]`.

### Speech bubbles

A vanilla `text_display` rides the villager, so it follows it and every nearby player sees it without installing
anything. It shows "..." while the AI is thinking and the reply for 5 to 15 seconds depending on its length. Bubbles are never saved on
purpose: they are removed on shutdown, and one left by a crash is removed the next time its villager ticks.

### Save compatibility

Villager data keeps the 1.1.6 layout (`smartvillagers.Memory`, `smartvillagers.PersonaOverride`) and adds one string,
`smartvillagers.Village`, holding requests, news and cooldowns in a versioned text format that every Minecraft version
saves the same way. The per-world ledger is `<world>/smartvillagers/ledger.json`. Existing configs gain the new keys
on first load; nothing is renamed. QA loads a villager written in the 1.x format on every build.

### Code

* `village/` is new and mostly Minecraft-free: `VillageRequest`, `RequestCatalog`, `RequestIntent`, `VillagerTies`,
  `VillageLedger`, `Standing`, `RequestDialogue`, `RequestEvent` (all unit-tested), plus `RequestService` which
  applies them in the world.
* `platform/Mc.java` holds every call whose name changed between 1.20.1 and 26.3 (registries, ids, advancements,
  click events, entity types, inventory). One copy per workspace; everything else in `common/` is identical and
  copied from 1.21.1 by `tools/sync_common.py`.
* Advancements and the language file are generated per version by `tools/datagen.py`.

### Out of scope for 2.0

* Villagers walking, following or fighting on command: the AI words replies; it does not drive villager AI.
* AI-invented requests: requests come from the catalogue so that rewards stay balanced and the AI cannot be talked
  into paying out.
* Moving 26.x chat history written by 1.21.1 worlds: 26.x stores chat history in a different layout (since 1.1.6);
  rapport, persona, requests and news carry over, older chat lines do not.
