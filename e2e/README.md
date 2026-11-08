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
- **Hard-code cells**, for example `new Cell(10, 23)`. Don't compute them from iteration order. Pick walking targets
  with a unique shortest path, so a different path-finding algorithm gives the same golden.
- **Click only what is on screen.** The driver throws if a target is outside the camera view, like a real player can't
  click it.

## Layout

```
test/com/mantkowicz/light/e2e/
├── GameDriver.java     actions (start, click, press, drag, pressKey, resize, stop) and observations (snapshot)
├── Cell.java           (col,row) address of a tile; the stable way to point at the map
├── Snapshot.java       sorted, rounded, line-based text, so golden files diff nicely
├── Golden.java         compares with golden/<name>.txt, or rewrites it in update mode
├── LevelVariant.java   changed copies of the packed starter level (walls, mirrored tiles...), in e2e/build/test-maps
├── headless/           HeadlessGame, NoOpGL, FixedGraphics, ScriptedInput
├── scenario/           gameplay tests, written against GameDriver only
└── unit/               BoardLayoutTest, BoardDrawingTest, PathFindingTest, WallTest, InventoryTest, AssetsUpToDateTest
```

Scenario ids are used in `docs/refactoring-plan.md`:

| Id | Behaviour | Test class | Golden |
|----|-----------|------------|--------|
| S1, S16 | Start; stop without errors | `StartAndStopTest` | `start`, `stop` |
| S2–S5 | Walk; re-route mid-walk; "no path" text; click without a tile | `WalkingTest` | `walk`, `reroute`, `no-path`, `no-tile` |
| S6, S7 | Camera centres on the clicked player; camera follows the player | `CameraTest` | `look-at-player`, `camera-tracking` |
| S8–S11 | Radial menu: dog description, cancel, short tap, player description | `MenuTest` | `menu-*` |
| S12, S13 | Q/W fade shader; torch flicker | `ShaderAndLightTest` | `fade-shader`, `torch-flicker` |
| S14 | Resize | `ResizeTest` | `resize` |
| S15 | The collect flow: walk onto the chest, COLLECT, move the lighter to the player, OK; CANCEL drops changes; walking away removes the button | `CollectTest` | `collect`, `collect-cancel` |
| — | The packed starter level (the game's default): water blocks walking, the rabbit's walk per direction, no flicker on diagonals, frame centred on its tile; the cow's idle animation; the chest used from next to it, opening and shutting with the collect window | `LevelTest` | — |
| — | Camera zoom: the world gets bigger or smaller, clicks still hit the right cell, buttons keep their place and size | `CameraZoomTest` | — |
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
