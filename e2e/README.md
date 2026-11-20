# Regression tests

These tests play the real game headlessly, with scripted clicks, holds, drags and key presses. They compare what a
player could see with text files in `golden/`. A few unit tests cover the board layout, what the board draws, path finding, walls blocking light and the inventory.
The game itself is not changed for testing.

## Running

```bash
./gradlew e2e:test                          # all tests, headless, about a second
./gradlew e2e:test -Dgolden.update=true     # re-record golden files (see the rules below)
./gradlew e2e:test e2e:jacocoTestReport     # coverage: e2e/build/reports/jacoco/test/html
```

On a mismatch the test fails with a diff and writes what it saw to `build/golden-actual/<name>.txt`.
CI (`.github/workflows/tests.yml`) runs `./gradlew build` and `html:dist` on every push and pull request to `master`,
and uploads those files when it fails. The golden files are recorded on macOS and pass unchanged on Linux.

## Rules

- **Only `GameDriver` touches game classes.** Tests and golden files speak in player terms: cells, the player, the dog,
  visible texts, the camera. When a refactoring renames or moves a class, only `GameDriver` changes, and only
  mechanically (an import, a getter name).
- **A changed golden file means changed behaviour.** Don't re-record to make a test pass. Re-record only for an
  approved behaviour change (for example a bug fix), and review the diff in the commit.
- **Record only what a player sees or what defines game state.** Never record object counts or internal fields that a
  refactoring can change invisibly. Texts are recorded only while visible (effective alpha > 0.01).
- **Every test brings its own map** (`TestMap`, drawn as text in the test), never a shipped level, so editing a level
  never breaks a feature test. Shipped maps are only checked by `ShippedMapsTest`, without cells or goldens. Point at
  cells with letters on the test map (`map.cell('a')`), or hard-code them, for example `new Cell(3, 2)`. Don't compute
  them from iteration order. Pick walking targets with a unique shortest path (e.g. along a corridor), so a different
  path-finding algorithm gives the same golden.
- **Click only what is on screen.** The driver throws if a target is outside the camera view, like a real player can't
  click it.

## Layout

```
test/com/mantkowicz/light/architecture/
└── ArchitectureTest.java  layers (leaves, world, behaviour, characters, UI, application) use only the layers below; no
                           package cycles; naming; nothing the browser can't run; scenario tests use only GameDriver;
                           writes build/reports/architecture/metrics.md (docs/code-quality-plan.md)

test/com/mantkowicz/light/e2e/
├── GameDriver.java     actions (start, click, press, drag, pressKey, resize, stop) and observations (snapshot)
├── Cell.java           (col,row) address of a tile; the stable way to point at the map
├── Snapshot.java       sorted, rounded, line-based text, so golden files diff nicely
├── Golden.java         compares with golden/<name>.txt, or rewrites it in update mode
├── TestMap.java        small maps drawn as text for one test (`.` grass, `~` water, `P` player, `C` chest...);
│                       `pathOf` gives a map's path before it is written (a door's targetMap)
├── LevelVariant.java   changed copies of a TestMap (extra layers, mirrored tiles, map properties), in e2e/build/test-maps
├── headless/           HeadlessGame, NoOpGL, FixedGraphics, ScriptedInput
├── scenario/           gameplay tests, written against GameDriver only
└── unit/               BoardLayoutTest, BoardDrawingTest, PathFindingTest, WallTest, InventoryTest, AssetsUpToDateTest,
                        UiAssetsTest

test/com/mantkowicz/light/ui/fishing/
└── RhythmRoundTest.java  the fishing game's rules; in the game's package to reach its package-private fish

test/com/mantkowicz/light/light/
└── DayClockTest.java   the time of day: its parts in order, the light dimming and coming back smoothly
```

Scenario ids (S1…) date from the first refactoring; older changelog entries use them, so keep them stable:

| Id | Behaviour | Test class | Golden |
|----|-----------|------------|--------|
| S1, S16 | Start; stop without errors | `StartAndStopTest` | `start`, `stop` |
| S2–S5 | Walk; re-route mid-walk; tapping the walking rabbit stops it; "no path" text; click without a tile | `WalkingTest` | `walk`, `reroute`, `no-path`, `no-tile` |
| S6, S7 | Camera centres on the clicked player; camera follows the player | `CameraTest` | `look-at-player`, `camera-tracking` |
| S8–S11 | Radial menu: dog description, cancel, short tap, player description | `MenuTest` | `menu-*` |
| S13 | torch flicker | `ShaderAndLightTest` | `torch-flicker` |
| S14 | Resize | `ResizeTest` | `resize` |
| S15 | Collecting: the chest's button (a gold star) opens it (stays open, stars), the crop inside (one picked at random per chest) jumps out onto a free cell next to it; the empty chest stays open and has no button any more; walking onto it puts it into the backpack; tapping its slot throws it out next to the player (no free cell: the slot shakes); with the backpack full the item stays, and standing on it the tool button widens into a swap (the left slot's item pulsing, in its slot and on the button together, a slot tap chooses another, the button swaps and swaps back; round again off the item and while walking across one; before the scythe); no free cell: the item stays (a chest stays shut, its button shakes); a chest next to the player at the start has its button; the button's reach, fading out and fading in | `CollectTest` | `collect` |
| — | A field (Tiled's `cropType`, carrots without it) starts ripe and blocks its cell; its button shows the crop, which jumps out next to it, leaving an empty field with a tiny sprout (the same for every crop) and a question mark on its sign; then the button shows the watering can: the rabbit waters (facing up below the field, down beside it; water falls, the button waits), once watered the crop grows a phase at once and then one every 30 s with no button, until ripe again and the button is back (the sprout, then the crop's own pictures; corn: six phases); walking away while watering stops the rabbit, the field still grows; picking a crop makes the face happy (3 times over); a weed (Tiled's `weedChance`, 0.4 without it) grows instead from the sprout, keeps the "?" sign, and pulled out it fades out and gives nothing while the face sweats and says so for 3 s; an empty corn field's button is as high as any field's | `FieldTest` | — |
| — | Grass (FeatureFlag `GROWING_GRASS`): sprouts and spreads slowly (0.1 %, 1 % next to grass) only where the rabbit can go (never on a chest or a field), none with the flag off; the map's GRASS objects put grass on tiles as far grown as their phase (1-4) says, none with the flag off; a phase every 30 s, tall from the third (over everyone, 90 % opaque); standing on it the HUD's tool button shows the scythe; one swing cuts the grass on the rabbit's tile and all eight around (then it fades out; no action button over the grass), walking off mid-swing leaves it; it blocks nothing | `GrassTest` | — |
| — | The right mouse button does what the left does: walks on the board and presses buttons (action, hint); both buttons pressed together are one click | `MouseButtonsTest` | — |
| — | Animations: the rabbit's walk per direction, no flicker on diagonals, frame centred on its tile; the cow's idle animation; the chest's lid opening and staying open | `AnimationTest` | — |
| — | Tapping the cow walks next to it. The cow's life (its state machine): stroking it with the action button over it (hearts), its wish for a crop, one of its favourite dishes (Tiled's `favouriteDish`, any crop without it) picked at random each time it gets hungry (a bubble in front of everyone on the map, replaced by the button with the carrot next to it), the hand staying next to the crop (stroked while wishing, it wishes for the same again), the button shaking and the face saying "find" with the crop without it (another crop won't do), giving the crop (the slot shakes, it eats, a heart), not fed for 90 s it gives up and grazes (three bites), every state showing its animation, its step onto a free cell (mirrored going left, cells blocked and freed), the player stepping back from a cell the cow took, the debug state label | `CowTest` | — |
| — | Chickens (Tiled's CHICKEN, CHICKEN_BABY): wish only for corn, bored after 15 s they peck three times instead; fed corn they eat and hearts rise over them (also when stroked); restless: in 10 minutes the chick walks more than the chicken, the chicken more than the cow, and both peck often; every state has its animation; level 1 has both on its right; they never walk away from the rabbit next to them or coming to them; after a step they keep looking that way; with two animals next to the rabbit each button belongs to its animal | `ChickenTest` | — |
| — | The frog (Tiled's FROG, `colour` green or pale, either at random without it): it can't be stroked or fed (no buttons); it keeps more than two tiles from the rabbit and the animals, hopping away (into the water rather than along the land), and blocks nobody's way (the rabbit walks onto its tile); a step is two hops of half a tile; at night it goes into the water and stays there; chased in by day it comes out after a while; a crop lying nearby it eats with its tongue when nobody is near; water is what the map draws with its water tileset only; level 1 has a green and a pale frog by the river | `FrogTest` | — |
| — | Chickens eat crops lying on the ground: up to 6 tiles away a chicken notices one (7: not), goes for it a step every 5 s, never onto its tile; beside it (left or right, never above or below; from above it goes round), turned to it, it pecks while the crop pulses, and after 10 s the crop is gone and the chicken goes back where it started; picked up first, it goes back; a step, or standing where it looks or pecks, that would leave the player no way to any crop on the ground makes it give its crop up for good, and going back it doesn't stop in such a place; already beside the crop it looks first too; a crop more than 12 steps round: it doesn't go; one chicken per crop; going for it or pecking it has its stroke button, and stroked it shows its hearts, gives the crop up for good and goes back | `ChickenEatsCropsTest` | — |
| — | Fishing (FeatureFlag `FISHING`): the rod on the tool button only on a fishing spot; cast to the left, the bobber in the water two tiles away, bobbing 3–10 s, then a bite (the rod shakes, "!!!", the happy face, the hand pointer turning grey over 1.2 s); no tap in time: gone, sad face, cast again; now and then no fish at all (bobs 10 s, sad face); tapped: the fishing game (fish down three lanes onto a line, a tap on a lane's net catches the fish on the line, playing well wins a fish into the backpack with the sunglasses face, a fish past the line is a miss with the sad face and three lose, a tap too early scares that lane's fish off (a miss), with no fish in sight it does nothing; still covering the screen after a resize); walking off while waiting takes the line out; level 1's spot (the end of its pier); none with the flag off or the map's `featuresOff=FISHING` | `FishingTest`, `RhythmRoundTest` (the game's rules: 12 fish on beats within 8 s, never three down one lane, a tap too early scares the fish off, with no fish in sight nothing) | — |
| — | Day and night (`light.DayClock`, tuned in `light.DayNightSettings`): the game starts in the morning, the same time on every map; a map with daylight (Tiled's `ambientLight`) dims at dusk to the night's light and brightens at dawn, one without (the house) stays as it is; the night starts at dusk, the day at dawn (`NightStartedEvent`, `DayStartedEvent` on every map): the cow and the chickens lie down and sleep the night with "zzz" over them and no button over them (no stroke, a wish is dropped until the morning), on every map, and get up at dawn; chickens first walk into the coop through the door with Tiled's `bedtime` (CHICKEN), the cow into the barn the same way (`bedtime` COW), sleep 2 to 4 tiles from the entrance and walk back out in the morning, on to a tile 2 to 4 tiles from the door (the coop's own chicken stays); the day clock at the top left shows the part of the day and its arrow only goes down: it jumps to nearly straight up at sunrise, steps down until sunset, one more at sunset and points nearly straight down at the moon all night; other tests have hour-long days, whatever the game's settings | `DayNightTest` (short days: `GameDriver.dayAndNight`), `DayClockTest` (the parts of the day, the light changing smoothly) | — |
| — | HUD bottom row (face, backpack showing what the player carries, stamina bar (lowered, filled), the hint button right of the face, whose bubble says "play around!" over the face; the action button over a chest) and the settings gear that opens only after a 3 s hold; taps on the row don't walk; a tap on the face or the stamina bar makes the face play a gesture (random, never the same twice in a row; not while it reacts to something else); stays on the screen's edges after a resize | `HudTest` | `hud`, `hud-hint`, `hud-stamina` |
| — | Every shipped map (android/assets): loads, the rabbit starts on a free cell (a map behind a door has no PLAYER), every chest can be reached, every door leads to another shipped map with a door of the same name and can be walked to; the game starts on level1 | `ShippedMapsTest` | — |
| — | Doors to other maps: the rabbit ending its walk on a door goes behind a curtain (darker, then lighter again) to the door with the same name on the other map, with its backpack; every map a chain of doors leads to is built at the start; it stays on the door it came out of until it walks off and back on, or taps the door on the tool button; with no room behind the door the curtain doesn't come down and it stays on the door, free to walk off; the scythe goes from the tool button when it leaves; a watered field grows meanwhile on the map it left; a chicken goes through by itself and is as hungry on the other side; a door with `passes` keeps the cow out | `TravelTest` | — |
| — | Camera zoom: the world gets bigger or smaller, clicks still hit the right cell, buttons keep their place and size | `CameraZoomTest` | — |
| — | Hops while walking; constant walking speed, no stops, straight diagonals | `HopTest`, `SmoothMovementTest` | — |
| — | Packed maps: layers drawn bottom first, animated water, mirrored/rotated/transparent tiles, `walls` and `collision` layers, `ambientLight`, clear errors | `PackedMapTest` | — |

## How the headless game works

- **No real OpenGL.** The headless backend sets no `Gdx.gl`. `NoOpGL` is a proxy that answers "compiled", "linked" and
  "framebuffer complete" and hands out ids, so shaders and box2dlights' framebuffer can be created.
  `ShaderProgram.pedantic = false`, because the fake GL reports no uniforms.
- **We drive the frames.** `HeadlessApplication` only loads natives and sets up `Gdx.files` (its own loop runs at 1 update
  per second and does nothing). `HeadlessGame.frame()` calls `game.render()` once.
- **Deterministic.** `FixedGraphics` returns delta 1/60 s, 60 fps and a 400×640 window. `MathUtils.random` is seeded
  with 42 (torch flicker). No wall clock and no sleeping: time is counted in frames. Every test starts a fresh game.
  `PhraseService` shuffles with an unseeded `Random`, which is safe only while each phrase group has one entry.
- **Scripted input.** `ScriptedInput` holds the pointer position, because `MenuStage` polls `Gdx.input.getX/Y`
  instead of using events. Clicks go to the game's `InputMultiplexer`. A key press lasts exactly one frame.
- **Working directory is `android/assets`**, because `Gdx.files.internal` resolves against it.

## What these tests can't catch

How things look (lights, the shader, sprites, layout), real mouse and touch timing, the HTML5 runtime (CI only
compiles it) and performance. Do a short manual play-through after changes that affect these.

### Visual tests, if ever needed

Run the same scenarios with real OpenGL in a hidden LWJGL3 window (`setInitialVisible(false)`, vsync off) and compare
screenshots with per-platform golden PNGs, with a small tolerance. An earlier experiment gave pixel-identical results
across runs on one machine. Notes from it:

- The LWJGL3 backend reassigns `Gdx.graphics` and `Gdx.input` every frame, so wrap them inside `render()`, for a fixed
  delta and the scripted pointer.
- Read pixels with `Pixmap.createFromFrameBuffer` before the backend swaps buffers.
- On macOS the JVM needs `-XstartOnFirstThread`, so this has to be a `JavaExec` main, not a JUnit test.
- The backend calls the game, not the other way round: run the scenario on a worker thread that hands frame tickets to
  the render callback.
