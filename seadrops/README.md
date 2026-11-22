# SeaDrops (Fabric, Minecraft 26.1+)

Client-side Fabric mod for Hypixel SkyBlock fishing. Plays animations on your screen:
catch alerts per sea creature, and custom boss drop sequences that only play when something actually drops.
Built to sit alongside the SkyBlock Enhanced modpack (Fabric, 26.1+).

## Boss drop sequences
Only play when a drop message ("RARE DROP! <item> ...") arrives after that boss spawned. Nothing plays on spawn
(turn "Boss spawn alerts" on in the menu if you also want the old catch animation).

| Boss | Sequence |
|---|---|
| Titanoboa | the serpent slithers in, a slash cuts its head off, the item rises from the stump |
| Wiki Tiki | a glowing tiki mask shakes, cracks and shatters |
| Lord Jawbus | lava jaws rise, snap shut with a shockwave, then spit the item out |
| Ragnarok | rune circle, three meteor strikes, a pillar of light |
| Blue Ringed Octopus | ink cloud, tentacles, pulsing blue rings |
| Thunder | three lightning strikes, the last one huge, thunder sound |
| Radioactive Vial | bubbling vial cracks and bursts into green gas (plays for the item, even from Jawbus) |
| Any item worth over 1M | coin shower (threshold adjustable in the menu: 500K, 1M, 2M, 5M, 10M) |

**Boss drops only animate when worth the threshold** (default 1M; the "Boss: valuable only" toggle on the Previews
tab turns that off). If the price can't be looked up, the drop still animates, since "RARE DROP!"-type messages only
appear for rare items. Bazaar items use the higher of buy and sell price.

**When it plays:** only after you kill the boss and it drops something (drop messages appear on the kill), and only if
the drop is worth the threshold. The same value rule applies to every sequence below.

Item entries, from the wiki drop tables (screenshots):

| Item | Sequence |
|---|---|
| Titanoboa Shed | Titanoboa |
| Tiki Mask, Troubled Bubble | Wiki Tiki |
| Chain of the End Times, Brimstone Handle, Burnt Texts | Ragnarok |
| Octopus Tendril | Blue Ringed Octopus |
| Radioactive Vial | Radioactive Vial (credited to Lord Jawbus) |
| Magma Lord Fragment | Lord Jawbus (always plays, ignores the value threshold) |
| Snake Eyes | Titanoboa |
| Flash I (enchanted book) | Thunder |

Items several bosses share (Aquamarine Dye: Wiki Tiki and Octopus; Carmine Dye: Ragnarok, Thunder and Jawbus;
Bobbin' Scriptures: Wiki Tiki and Jawbus) can't be told apart by name, so they use whichever boss spawned last.
 Common drops (Thunder Fragment, Magma Lord
Fragment and similar, 50% or more) don't trigger a drop message at all.

Every sequence ends by revealing the item: glowing gem, item name, "BOSS DROP" and the estimated coin value.

**How drops are matched to a boss:** the drop message doesn't name the mob, so SeaDrops remembers the last boss that
spawned for 3 minutes (`bossWindowSec`) and attributes the next drop to it. Two bosses back to back inside that
window, or a drop after the window, can pick the wrong sequence.

**Value alerts** use public price data (the Hypixel bazaar API and a lowest-BIN JSON, `lowestBinUrl`), fetched at most
every 10 minutes, and no information about you is sent. Turn "Value alerts" off and no requests are made. Item names
are turned into ids by upper-casing and underscores, which works for most items but not pets or enchanted books.

## Catch animations
Per-location themes (ocean, Crimson Isle, thunder, Jerry's Workshop, Backwater Bayou, Spooky, Crystal Hollows) and a
rarity-coloured burst for "GREAT CATCH!"-style messages. Higher rarity = more particles and rings.

## Menu
`/seadrops` opens it. Settings tab: toggles for mod, sound, screen flash, mob animations, generic catches, drop
animations, value alerts, boss spawn alerts, a minimum-rarity cycle, and a toggle plus preview per location.
Previews tab: play every drop sequence and rarity burst, and set the value alert threshold.
Command previews: `/seadrops lava`, `/seadrops mythic`, `/seadrops drop titanoboa` (also wikitiki, jawbus, ragnarok,
octopus, thunder, vial, coins).

## Why it should coexist with the pack
- No mixins, and it never cancels or edits chat messages - it only listens.
- Draws on its own HUD layers (added last), so SkyHanni / Skyblocker / Odin screens are untouched.
- Only needs Fabric API, which the pack already ships.
- SkyHanni and Skyblocker have their own alerts; those will show alongside these, so turn theirs off if you only want
  SeaDrops.

## Build the jar without installing anything (GitHub)
1. Create a free account and a new empty repository at https://github.com/new
2. Upload everything in this folder, including the hidden `.github` folder (drag the folder contents into the repo
   page, or use "Add file > Upload files").
3. Open the repository's **Actions** tab, pick **Build SeaDrops** and run it (it also runs on every upload).
4. When it finishes, open the run and download the **seadrops-jar** artifact. Unzip it: that's your mod jar.
If the build fails, open the failed step, copy the red error lines, and send them to me.

## Build locally
Needs **JDK 25** and Gradle 9.4 (Loom 1.15, Fabric Loader 0.18.4). This was written without being able to compile
against Minecraft, so the first build may flag small API mismatches.
1. Get the Gradle wrapper from the Fabric Example Mod for 26.1 (https://github.com/FabricMC/fabric-example-mod),
   then copy `src/`, `build.gradle`, `gradle.properties` and `settings.gradle` over it.
2. `./gradlew build`
3. Put `build/libs/seadrops-1.0.0.jar` (not `-sources`) in the pack's `mods` folder.

Targets Minecraft **26.1.2**. For **26.2**, change `minecraft_version` and `fabric_version` in `gradle.properties`
(see https://fabricmc.net/develop) and `"minecraft": "~26.1"` in `fabric.mod.json`.

## Config: `config/seadrops.properties`
- All menu settings are saved here.
- `triggers`: generic catch phrases, separated by `|`.
- `customMobs`: `phrase~Name~rarity~theme` entries separated by `|`, e.g. `appears from the swamp~Swamp Thing~epic~bayou`.
- `customItems`: give any item a sequence: `phrase~scene~hexcolor` separated by `|`, e.g. `Jawbus Follower~jawbus~FF4A1A`.
  Add `~always` to ignore the value threshold: `Jawbus Follower~jawbus~FF4A1A~always`.
  Scenes: behead, tiki, jawbus, ragnarok, octopus, thunder, vial, coins, slash.
- Unverified (I couldn't confirm these): the Backwater Bayou mobs and Ragnarok are matched on their name only, with
  guessed rarities, and the "RARE DROP!" style drop-message format is assumed. If a drop doesn't trigger, check the exact
  chat line and adjust.
