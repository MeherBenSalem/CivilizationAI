# QA record: 2.0.0

`tools/qa/run_matrix.sh` (see [CONTRIBUTING](../CONTRIBUTING.md#testing-in-game)) on a fresh flat world per build: a
dev server and a real game client under Xvfb (software rendering), with the mock OpenAI-compatible provider
`tools/qa/mock_ai.py`. Everything the player says is typed into the client's chat; replies are read from the client's
log. All eleven builds passed on the sources of commit `a216bcb` (the mod code has not changed since); the jars in
`releases/` were built from the same sources by `tools/build_release.sh`, with the unit tests (107 per workspace, 0
failures, 1 skipped: the live DeepSeek smoke test, which needs a real key).

| Build | Checks | Failures | Request rolled in the main flow |
|-------|--------|----------|---------------------------------|
| 1.20.1 Fabric | 59 | 0 | defeat 6 skeletons |
| 1.20.1 Forge | 60 | 0 | bring 9 pumpkins |
| 1.21.1 Fabric | 60 | 0 | bring 6 pumpkins |
| 1.21.1 Forge | 60 | 0 | bring 6 pumpkins |
| 1.21.1 NeoForge | 60 | 0 | bring 21 potatoes |
| 26.1.2 Fabric | 59 | 0 | defeat 5 spiders |
| 26.1.2 NeoForge | 60 | 0 | bring 19 wheat |
| 26.2 Fabric | 59 | 0 | defeat 5 pillagers |
| 26.2 NeoForge | 60 | 0 | bring 9 pumpkins |
| 26.3 Fabric | 60 | 0 | bring 18 potatoes |
| 26.3 NeoForge | 60 | 0 | bring 24 potatoes |

The main request is rolled by the villager, so a build checks either a fetch or a hunt there; a fetch adds one check
("items taken"). Every build also runs an admin-assigned hunt and eight assigned fetches, so both kinds are covered
everywhere.

## What is checked on every build

| Group | Checks |
|-------|--------|
| setup, status | Three villagers summoned (farmer, librarian, weaponsmith); `/villagerai status` shows the custom provider ready, speech bubbles and requests on |
| consent | `/villagerai consent accept` typed by the player |
| ai | A greeting gets the provider's reply in chat; the prompt holds the persona, profession, biome and world facts, the standing; the model and endpoint are right |
| name tag, advancement | The villager shows its persona name; "Smart Villagers" is granted on the first chat |
| bubble | A text display rides the villager showing the reply, and is removed a few seconds later |
| request | "Do you have any work for me?" gets the offer card with [Accept] and [Decline]; the AI is told about the offer; the request is stored on the villager; "yes" accepts it; "how many do you still need?" reports 0/N; after the items are given (or the mobs defeated by the player), "done" completes it; the AI is told; emeralds are paid; items are taken; the request is cleared; minor-positive gossip is added; "A Favor for a Friend" is granted; the world's ledger holds one favour |
| news, standing | The librarian heard about it; the news and a "friendly" standing are in its AI prompt |
| fallback | With the provider failing, the librarian greets in local dialogue with the friendly greeting and the news |
| assign, buttons, slay | `/villagerai request assign <villager> slay` offers a hunt; `/villagerai request accept` (what the [Accept] button runs) accepts it; the kills complete it |
| refusal, cooldown | A villager hurt twice by the player refuses work; the farmer does not offer a second request right away |
| requests | `/villagerai requests` shows "2 finished", later the open request |
| hero | Eight more requests; "Hero of the Village Square" after ten; the hero greeting |
| abandon, death | "Never mind, I give up" drops an accepted request; a villager killed with an open request no longer counts towards the player's open requests |
| compat | A villager written with 1.x data (rapport, persona override) loads it unchanged |
| bubble (orphan) | A speech bubble left on a villager (as after a crash) is removed |
| log | No errors at all in the server log, none about the mod, mixins or advancements, and "Smart Villagers AI ready" |

## Found and fixed by this QA

* Turning to a second villager kept the first one answering: the previous partner's session bonus outweighed the
  crosshair. Villager selection now weights how directly you look at a villager.
* A villager dying with an accepted request kept occupying one of the player's three request slots, and an accepted
  request could not be dropped. Death now frees it, and "never mind" / "I give up" drops it.
* The 1.21.1 Forge dev server never answers an RCON command that prints nothing (for example `gamemode` on a player
  already in that mode). Not a mod issue; the checks now type such commands as the player and reconnect when RCON
  stays silent.
* Vanilla logs an error when a text display is summoned without `alignment`; the QA's orphan-bubble test now sets it
  (the mod's own bubbles are created in code and are unaffected).
* Bubbles disappeared before short replies could be read; they now stay 5 to 15 seconds depending on length, and the
  "thinking" bubble shows the villager's name like a reply does. Unemployed villagers no longer show as "[None]".
