# Marketing

Listing copy and artwork for CurseForge and Modrinth.

| Path | What it is |
|------|------------|
| `curseforge/description.md` | The project description (Markdown; also works on Modrinth). Images load from this repo's `main` branch. |
| `curseforge/summary.txt` | The one-line summary (under 250 characters). |
| `images/banner.png` | 1800×600 header banner. |
| `images/icon.png` | 512×512 project icon (also the mod's in-jar `logo.png`). |
| `images/card-*.png` | 1600×900 feature cards used in the description and as gallery images. |
| `images/header-*.png`, `images/divider.png` | Transparent section headers and a divider for the description. |
| `images/screenshots/*.jpg` | Real in-game captures (Minecraft 26.3, Fabric) used by the cards and the gallery. |
| `source/render.cjs` | Renders every PNG above from the screenshots and the bundled fonts. |
| `source/fonts/` | Fredoka and Nunito, both under the SIL Open Font License (licence files included). |

The look: a village square at noon. Cream plaster and a dotted canvas, dark-oak outlines, emerald green (the
villagers' currency) for highlights, terracotta for labels, rounded Fredoka headings, and speech bubbles everywhere:
section headers are bubbles spoken by a pixel villager, cards frame their screenshots with thick rounded outlines,
and the divider is a row of emeralds on a dashed oak line.

## Regenerating the images

Needs Node.js and Playwright with Chromium:

```bash
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs                 # everything
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs card-requests   # one asset
```

The release name, the supported Minecraft versions and loaders, and the AI provider presets are at the top of
`render.cjs`. Supporting a new Minecraft version means adding one line there, plus one row to the compatibility table
in `description.md`. After changing the icon, copy `images/icon.png` to every workspace's
`common/src/main/resources/logo.png`.

## Retaking the screenshots

`tools/qa/marketing_shots.py` captures every screenshot from a dev client (see `tools/qa/` and `CONTRIBUTING.md`): a
1920×1080 window under Xvfb, on a 26.3 Fabric dev server with a default (not flat) world, with `mock_ai.py` running:

```bash
python3 tools/qa/mock_ai.py &
tools/qa/devrun.sh prepare-server 26.3 fabric showcase 7214950366214 normal
tools/qa/devrun.sh start-server 26.3 fabric && tools/qa/devrun.sh start-client 26.3 fabric
sed -i 's/guiScale:2/guiScale:3/' 26.3/fabric/runs/client/options.txt   # then restart the client: readable chat
python3 tools/qa/rcon.py "op Builder"
python3 tools/qa/marketing_shots.py              # or a few: offer complete gossip
```

The village is the plains village `/locate structure minecraft:village_plains` finds from spawn in that world. Every
screenshot is a single unedited frame of the game. What is staged:

* The villagers being photographed are held still with `NoAI` (otherwise they walk out of frame). Their names, traits
  and requests are the ones the mod gave them.
* Time is set to noon and the weather to clear.
* The replies are the mod's **built-in dialogue** (the stand-in AI provider is set to fail, as it would without a
  key), so what the bubbles say is exactly what players without an AI provider see.
* `thinking.jpg` shows the "..." bubble while a request is waiting for an AI provider; the provider there is the local
  QA stand-in (`tools/qa/mock_ai.py`), slowed down so the moment can be photographed.
* For `complete.jpg` the requested items (or kills) were provided with commands before handing the request in.

| Screenshot | Scene |
|------------|-------|
| `crowd.jpg` | Three villagers answering in speech bubbles |
| `square.jpg` | The same conversation from further back (banner) |
| `offer.jpg` | A request offered: the bubble and the chat card with [Accept] and [Decline] |
| `complete.jpg` | The request handed in: thanks, payment and the "Request complete" line |
| `gossip.jpg` | A neighbour greeting the player with the news it heard |
| `thinking.jpg` | The "..." bubble while the AI is thinking |
| `advancements.jpg` | The Smart Villagers advancement tab |
