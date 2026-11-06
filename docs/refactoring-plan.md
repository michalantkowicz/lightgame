# Light Game: Refactoring Plan

Goal: make the codebase **as simple and human‑readable as possible while keeping the current behaviour**.
The plan is written for step‑by‑step execution by agents (or a human), with checkpoints to stop, review and
roll back.

- Scope: the `core` module (all game code) plus the small launcher touch points in `desktop`/`html`.
- Baseline: commit that contains the libGDX 1.14.2 / Gradle 9.8.2 / HTML5 migration. **The migration must be
  committed before Phase 0 starts.**
- Size today: **129 Java files, ~4,730 lines** in `core`. Rough estimate after the plan: **~55 files, ~2,500 lines**.

---

## 0. Rules for whoever executes this plan

1. **One step = one commit.** Message format: `refactor(<step id>): <summary>`, for example `refactor(2.1): introduce GameContext`.
   Bug‑fix steps (Phase 7) use `fix(<id>): …`.
2. **Behaviour must not change** in Phases 0–6. If a step seems to need a behaviour change, stop and ask.
   Known bugs are listed in §3.4 and fixed **only** in Phase 7, after approval.
3. **No drive‑by changes.** Do only what the step says. If you find something new, add it to §8 ("Found during
   execution") and continue.
4. **Every step must pass the step gate (§6.1)** before committing. Every phase ends with a **checkpoint (§6.2)**: tag
   it, then STOP and wait for human review before starting the next phase.
5. **Rollback:** `git reset --hard refactor-cp<N>` returns to the last approved checkpoint. To replay a single step,
   reset to the previous step's commit and re‑execute it.
6. **GWT (HTML5) constraints.** `core` is also compiled to JavaScript by GWT, so do not use:
   - reflection: `Class.isInstance`, `Class.forName`, `getDeclaredFields`, …
   - `String.format`, threads or `synchronized`, `java.io` file APIs.

   `getClass()`, class literals as map keys, lambdas, anonymous classes, enums and `switch` on strings or enums are fine.
   `./gradlew html:dist` is part of every gate because it catches these.
7. **Style to keep:**
   - 4‑space indentation and explicit imports. Static imports are OK, as the code already uses them.
   - `private final` fields where possible, no Lombok, no new libraries in `core`.
   - Constants are `private static final`.
8. Don't touch `android/`, `html/src/**/emu/**`, or the build files, except where a step says so.
9. Decisions in §5 must be answered by the human **before** the steps that depend on them. Each step lists its
   decision dependency.

---

## 1. How the game works today (the behaviour to preserve)

`Main` creates a Box2D `World`, `ResourcesService` and `GameEventService`, then opens `GameScreen`.
`GameScreen.show()` does the rest:

1. **Stages.** It creates three stages that share **one** `ExtendViewport(800×1280)`, and therefore one camera:
   `GameStage` (board and actors), `NotificationStage` (floating texts and tooltips) and `MenuStage` (radial menu).
   Input goes to `MenuStage` first, then to `GameStage`.
2. **Lighting.** It creates a `RayHandler` (box2dlights) with almost black ambient light.
3. **Map.** It loads `map.tmx`, a hex map with staggered odd columns, into `Tile`s. Map objects (ellipses) are snapped to the
   nearest cell and become `TileObject`s: `SPRITE` (blocks the tile), `CHEST`, `DOG`, `PLAYER` and `LIGHT` (a torch on
   the tile border).
4. **Spawning.** `tile.prepare(config)` spawns the actors and lights. The player is reported through `PlayerCreatedEvent`, so
   the screen can start camera tracking.
5. **Features.** These run every frame: `CameraTrackingFeature`, `MenuFeature`, `StageFeature` and `FPSFeature`.
   Actors run their own per‑actor `Plugin`s.
6. **Events.** A polled queue (`GameEventService`). Events are immortal until consumed, except `PlayerMoveEvent`,
   which lives one frame.

User‑visible behaviour, which the manual smoke test in §6.3 checks:

- Clicking an accessible tile walks the player along a shortest path, tile by tile. Clicking during a walk re‑routes at the next tile.
  The camera follows the player softly when the player leaves a 420×420 box.
- Clicking an inaccessible tile shows a red "no path" text that floats up and fades.
- Clicking the player centres the camera on the player with an animation.
- Pressing and holding an actor for 0.5 s (dog or player; the chest is not touchable) opens a radial menu with one button.
  Hovering scales the button. Releasing over the button shows a description tooltip next to the actor.
- `Q` desaturates and darkens the game stage (the player is excluded); `W` restores it.
- An FPS label is shown at the camera offset (300, 520).
- Torch lights flicker.
- Walking onto the chest is meant to show a "COLLECT" button that opens the collect window (swap items, OK/CANCEL).
  **Code analysis says this currently crashes. See bug B1.**

---

## 2. Dependencies and build

- **Dependencies are already current:** libGDX 1.14.2, box2dlights 1.5 (latest release, from 2019), Gradle 9.8.2, GWT 2.12.2
  (pinned on purpose, see `html/build.gradle`). **No dependency changes are needed** for this refactoring.
- **Only addition:** a test‑only `e2e` module with the libGDX headless backend, JUnit and JaCoCo (Phase 0). `core` stays untouched.
- **Box2D is used only as the shadow geometry for box2dlights.** The only code that creates Box2D bodies is `WallTile`,
  and the current map has no wall tiles. So today the `World` is empty and lights cast no shadows. Keep `World` and
  `RayHandler`, because they are required by box2dlights.
- **There are no tests at all today.** Phase 0 adds a safety net before anything is touched.

---

## 3. Findings

### 3.1 Architecture

| # | Finding | Where |
|---|---------|-------|
| A1 | **The configuration layer is a service locator hidden behind 13 interfaces.** One class, `GamePrepareConfiguration`, implements 4 of the 13 interfaces in `configuration/api` (the others reach it through inheritance), and these interfaces re‑declare the same getters with `@Override`. `PlayerConfiguration` extends almost all of them, and every actor receives it, so the segregation isolates nothing. Some code bypasses the interfaces and takes the concrete class (`Tile.prepare(GamePrepareConfiguration)`, `TileObject.prepare(..)`). It also has a mutable `setBoardService()` because of the construction order. | `configuration/**` (14 files, ~310 lines) |
| A2 | **Confusing stage naming.** `getStage()` returns the game stage, and `getUiStage()` returns **`MenuStage`**. This naming directly causes bug B1. | `GamePrepareConfiguration`, `GameScreen.prepareConfiguration()` |
| A3 | **The event type is declared twice.** Every event has a `GameEventType` enum value **and** its own class, and callers pass both: `removeEventFromQueue(TILE_TOUCHED, TileTouchedEvent.class)`. The payload is an untyped `getEventObject()`, not a named accessor. | `service/event/**` |
| A4 | **Indirection chains that do one simple thing:** <br>• Showing a tooltip goes `DescriptionInteraction` → `AddToStageEvent(DescriptionDefinition implements GameActorDefinition)` → `StageFeature` → `Map<StageType,Stage>` (built from `AbstractStage.getStageType()`) → `notificationStage.addActor(new Description(..))`. That is 7 types for one `addActor` call. <br>• Collecting goes `CollectPlugin` → `CollectResolver` (interface, 1 implementation) → `PlayerCollectResolver` → `OpenCollectWindowButtonListener` → `ActionButton`. <br>• A notification goes `NotificationBuilder` + `LabelFactory` + `NotificationAnimation` (interface with 3 implementations, only 1 really used) + `NotificationType` + `NotificationStyle`. <br>• A light goes `LightFactory` + `LightType` + abstract `GameLight` + `TorchLight` + `BlinkingAction` + `BlinkingIntensity`/`BlinkingSpeed` enums (one value of each is used). <br>• A map loads through `TiledMapLoader<T>` (generic interface, 1 implementation) + `TmxTileMapLoaderProperties` (wraps one String) + `TileSet` (wraps a List, plus `TilesetProperties`, which are written and never read) + `Tuple`. | see paths |
| A5 | **Over‑deep inheritance.** `Tile` → `AccessibleTile`/`NoPathTile` → `FloorTile`/`WallTile`, plus a `TileFactory`. `FloorTile` overrides `isAccessible()` anyway, so the middle layer means nothing. | `board/tile/**` |
| A6 | **Packaging by technical role, not by feature.** Packages like `factory/`, `implementation/`, `api/`, `service/` and one‑class packages (`thumbnail`, `util`, `interaction`, `lights/factory`, `board/path`) scatter related code. `board/object/implementation/<x>/` has 5 sub‑packages with one class each. | whole tree |
| A7 | **Inconsistent resource access.** Some code calls `resourcesService.getTexture(..)`, other code calls `getAssetManager().get(.., Texture.class)` directly (`Dog`, `Chest`, `Player`, `CollectItem`). `getTexture` uses `catch (Throwable)` for control flow. | `ResourcesService.java:58,73` and callers |
| A8 | **Hidden side effect.** `MenuStage.clear()` also *triggers the interaction* of the button under the pointer, and it casts `actor.getParent()` to `MenuButton` (`FIXME` at `MenuStage.java:30`). Menu selection happens inside a method named `clear`. | `stage/MenuStage.java` |
| A9 | **Overridable methods called from constructors.** `GameActor` calls `setStatus(IDLE)`, which `Player` overrides and which runs before Player's fields are set. `GameBoardActor` calls abstract `getDescription()`. Both work today by luck. | `GameActor.java:30`, `GameBoardActor.java:25` |
| A10 | **`Plugin` and `Feature` are identical interfaces** (`void run(float delta)`). The distinction is documented (per actor vs per screen), so keeping both is acceptable. This is listed as optional step O1. | `plugin/Plugin.java`, `feature/Feature.java` |

### 3.2 Boilerplate

- **Generated `equals`/`hashCode`** in `GameActor`, `GameBoardActor` and `Tile`. Ids are unique per instance, so they behave like identity
  anyway. They also hash **mutable** state (`tile`, `polygon`, `pluginsQueue`), which is a latent bug for `HashMap` keys.
  Delete them and use identity. This also makes `id`/`ID_COUNTER`/`ID_SEQUENCE` and the whole `GameActorType` enum unused.
- **Enum‑with‑value wrappers:**
  - String keys: `TileAttribute` (1 value), `TileObjectAttribute`.
  - Single‑value enums: `ShaderType` (1 value), `ThumbnailType` (1 value), `LightType` (1 value).
  - Enums where only one value is used: `BlinkingIntensity`, `BlinkingSpeed`.
- **Trivial listener classes in separate files:** `PlayerClickListener`, `MenuButtonListener`, `OpenCollectWindowButtonListener`,
  `CollectWindowCancelListener`, `CollectWindowOkListener`, `StopClickThroughClickListener`.
- **Duplicated code:**
  - `LabelFactory` has two near‑identical methods.
  - `CollectWindow.getCollectibleTable()`/`getInventoryTable()` are identical.
  - `TileChest`/`TileDog`/`TilePlayer` are the same 10 lines.
  - The hex width formula appears twice and the cell‑position formula twice (`TmxTiledMapLoader`).
  - `Notification.getCenter()` duplicates `GameActor.getCenter()`.
- **Unneeded wrappers:** `BoardPath` (wraps a `List`, has both `isEmpty()` and `exists()`, plus unused `setPath`/`getLastNode`),
  `GameActorsByTile` (a `Map` with 3 methods).
- **`TilePoint` enum and a `switch`** to build 6 polygon corners. Only `CENTER` is used from outside `Tile`.
- **Pathfinding is O(n²) Dijkstra on a graph where every edge has weight 1** (`BoardService.getPath`). A 15‑line BFS
  does the same job (decision D4).
- **`GameScreen` has empty `pause/resume/hide` methods.** Extending `ScreenAdapter` removes them.
- **"Constants" declared as instance fields:** `final` but not `static` (`MenuFeature`, `CollectWindow`, `Description`).

### 3.3 Dead code (verified by search; none of it is reachable)

| Item | Where |
|------|-------|
| Stray `new RayHandler(world)` that is never used and leaks an FBO | `Main.java:27` |
| Disabled "scared of darkness" feature: `PlayerNotificationPlugin` is built but `addPlugin` is commented out. It drags along `EnlargeAndFadeOutAnimation`, `NotificationType.PLAYER_NOTIFICATION`, `NotificationStage.doesContainNotification/removeAllNotificationsOfType/getNotificationCount`, `Tile.getNotificationCenterPosition`, `Player.getIdleLength`/`lastIdleChange` and `PhraseGroup.DARKNESS_EXCLAMATION` (decision D2) | `Player.java:46` and listed files |
| `Collectible.beforeCollect/afterCollect/getName/getTile/DEFAULT`; `Chest`'s `System.out` implementations; `Chest.setName("Nice Chest")` | `actor/` |
| `Item.use/unuse` and `Lighter`'s implementations; `Lighter.thumbnail` field (loaded, never read). `Lighter` extends `GameBoardActor` and implements `Collectible` but is never placed on a board or stage, because it only ever lives inside an inventory | `actor/implementation/item/Lighter.java` |
| `CameraActor.setDebug`, `centerAtWithZoom`; commented `setDebug` call in `CameraTrackingFeature` | |
| `GameLight.getTile/setLightPosition/setLightColor/getLightType`; `TorchLight.COLOR`, which is always overwritten by the map colour (default `555555`) | `lights/` |
| `TileObject.objectType` field (never read; `TileLight` even passes `CHEST`) | `TileLight.java:25` |
| `Tile.getId`, `Tile.id`, commented‑out actor code in `Tile`, `TileSet.properties`/`TilesetProperties`, `TmxTiledMapLoader.getTilesetProperties` | |
| `PhraseService.getPhrase`; `Inventory.getCapacity`; `NotificationType` (after D2 nothing reads it); `FadeOutAnimation` (only a builder default that is always overridden) | |
| `MenuCloseEvent` payload (a pointer position nobody reads) | |
| Unused assets: `android/assets/ui/` (uiskin), `map2_outer.tmx`, `tiles.tsx`, `player_detective.png`, `lighter.png` (preloaded but never fetched by that name; the atlas region `lighter` is used). **Keep** `grass*`, `road*`, `tileFloor`, `woodenFloor*`, `floor-tile`, `wall-tile` PNGs: `lightgame_tileset.tsx` references them and `TmxMapLoader` loads them. | `android/assets` |

### 3.4 Bugs found (**do not fix before Phase 7**; each needs approval)

| # | Bug | Evidence |
|---|-----|----------|
| B1 | **Walking onto the chest crashes** (confirmed in the headless spike). `PlayerCollectResolver` adds `ActionButton` to `getUiStage()`, which is `MenuStage`, and `MenuStage.addActor` throws `RuntimeException("Actor on this stage must be instanceof MenuButton")`. Even if it were allowed, `MenuFeature` calls `menuStage.clear()` on every menu close, which would delete the button and window, and `MenuStage.getButton()` would throw `ClassCastException`. | `PlayerCollectResolver.java:26`, `MenuStage.java:38` · scenario S15 |
| B2 | **Furniture (19 `SPRITE` objects: bed, bath, tv, …) is never drawn.** `TileSprite.prepare` builds an `Image` but the `tile.addActor(image)` line is commented out, because `Tile` is no longer an `Actor`. The tiles still block movement, so the player sees empty tiles that answer "no path". | `TileSprite.java:32`; visible in the HTML5 screenshot |
| B3 | **Any `WALL` tile crashes on load.** `WallTile.prepare` calls `Tile.stageToLocalCoordinates`, which throws `UnsupportedOperationException`. The tileset defines WALL (gid 2); the current map doesn't use it. | `Tile.java:218`, `WallTile.java:22` |
| B4 | **Debug outlines are drawn over the collect window** (`backgroundTable.debug()` turns on stage debug drawing). Only visible after B1 is fixed. | `CollectWindow.java:58` |
| B5 | **Resource leaks.** Every label loads a **new `BitmapFont`** from disk that is never disposed: each "no path" text, each menu button, each tooltip. Description tooltips fade to alpha 0 but are **never removed** from the stage. `GameScreen.dispose()` is never called and doesn't dispose `menuStage`, `notificationStage` or the shader. | `LabelFactory.java:14,31`, `Description.java:33`, `GameScreen.dispose()` |
| B6 | `PhraseService.getRandomPhrase` shuffles the enum's backing array in place. It's harmless but surprising. It gets fixed naturally in step 5.7. | `PhraseService.java:20` |

B5 and B6 are invisible to the player and are fixed **inside** the refactoring steps that rewrite that code (4.3, 4.1, 5.7),
because the new code simply doesn't have the leak. All other bugs wait for Phase 7.

---

## 4. Target architecture

Packages are organised by feature, with no `api/`, `factory/` or `implementation/` layers. Sub‑packages exist only where they hold ~5+ classes.

```
com.mantkowicz.light
├── game/        Main, GameScreen, GameContext
├── resources/   Resources (textures, nine-patches, skin, font, label creation), Phrases
├── event/       GameEvent, GameEvents (queue), TileTouchedEvent, CollectEvent, PlayerMoveEvent,
│                LookAtMeEvent, PlayerCreatedEvent, MenuOpenEvent, MenuCloseEvent
├── board/       Board, BoardClickListener, Tile, FloorTile, WallTile, BoardService (actors on tiles,
│                collisions, path finding)
├── map/         TmxBoardLoader (+ hex grid math), TileObject, TileSprite, TileChest, TileDog,
│                TilePlayer, TileLight, LightType
├── actor/       GameActor, GameBoardActor, Player, Dog, Chest, CameraActor, Inventory, Item, Lighter,
│                Collectible
├── light/       TorchLight, BlinkingAction
├── plugin/      Plugin, BoardMovementPlugin, CollectPlugin, CenterActorByCameraPlugin, RemoveOnPlayerMovePlugin
├── feature/     Feature, CameraTrackingFeature, MenuFeature, FPSFeature
└── ui/          GameStage, MenuStage, NotificationStage, Notification, ActionButton
    ├── menu/    MenuButton, Interaction, DescriptionInteraction, Description
    └── collect/ CollectWindow, CollectItem
```

Principles:
- **One wiring object, `GameContext`.** It has final fields, getters only, and is built once in `GameScreen`. Classes take what
  they need from it **in their constructor** and store only those fields; they don't keep the context itself.
  `TileObject`s, which run at spawn time, are the exception and receive it as a method argument.
- **Events are keyed by their class:** `events.post(new TileTouchedEvent(tile))`,
  `TileTouchedEvent e = events.poll(TileTouchedEvent.class)` (returns `null` if absent), and `events.has(PlayerMoveEvent.class)`.
  Payloads have named getters, e.g. `getTile()`.
- **The map/data layer** (`map/`) turns Tiled data into tiles and spawn objects. **The board** (`board/`) is the runtime model.
  **Actors** (`actor/`) are scene2d actors that live on the board.
- The cycle `actor ↔ board` (actors know their `Tile`; `BoardService` knows actors) is acceptable for a game this size.
  Don't add interfaces just to break it.

---

## 5. Decisions needed from the human (answer before the dependent steps)

| # | Question | Options | Recommendation |
|---|----------|---------|----------------|
| D1 | Replace the 13 configuration interfaces with one `GameContext` class? | (a) one `GameContext`; (b) constructor injection of individual services everywhere; (c) keep interfaces but trim them | **(a)**: least code and clear to read. (b) makes some constructors take 6–7 parameters. |
| D2 | Delete the disabled "scared of darkness" feature (`PlayerNotificationPlugin` and the code listed in §3.3)? | (a) delete (git history keeps it); (b) keep and leave disabled; (c) re‑enable (behaviour change, goes to Phase 7) | **(a)** |
| D3 | Remove the thumbnail mechanism (`Thumbnail`, `ThumbnailType`)? Today every thumbnail is the same atlas region, `collectButton`. | (a) remove; use `"collectButton"` directly in `MenuButton` and `CollectItem`; (b) keep as a hook for per‑item icons | **(a)**. It's easy to re‑add a `getIcon()` method when a second icon actually exists. |
| D4 | Replace Dijkstra with BFS in path finding? | (a) BFS: same path **length**, but when several shortest paths exist it may pick a different one; (b) keep Dijkstra and only tidy it | **(a)**. Today's tie‑breaking depends on `HashMap` iteration order, so it isn't a designed behaviour. |
| D5 | Fix bugs B1–B4 in Phase 7? | per bug: fix / leave / delete the feature | B1: **fix** by adding a real UI stage (see 7.1). B2: **fix** by drawing sprites. B3: **fix** body creation. B4: **fix** by removing `.debug()`. |

---

## 6. Verification

### 6.1 Step gate (after every step, before committing)

```bash
./gradlew build e2e:test          # compiles everything; plays all gameplay scenarios headless + unit tests
./gradlew html:dist               # GWT compile = proves code is still HTML5-compatible (~20 s)
```

Both must pass, with no new compiler warnings (`-Xlint:deprecation` is on). **Goldens must not change.** If a step needs to
touch `e2e/`, only `GameDriver` (and the unit‑test helpers) may change, and only mechanically: imports, renamed getters.

### 6.2 Checkpoint (end of every phase)

1. Run the step gate.
2. A human does the **manual play‑through (§6.3)**. It is the only check of how the game *looks*, since there are no visual tests yet.
   An agent reports that it is pending.
3. Optional: the HTML5 smoke test (§6.4).
4. Tag the commit: `git tag refactor-cp<N>`.
5. **STOP.** Write a short report: steps done, files deleted or added, line count (`find core/src -name '*.java' | xargs wc -l | tail -1`),
   anything added to §8. Wait for approval.

### 6.3 Manual play‑through (desktop: `./gradlew desktop:run`)

The gameplay scenarios cover the behaviour. The play‑through checks what they can't see: rendering (light, shader look, sprites),
real mouse feel, and anything odd.

1. The window shows the wooden hex floor, a flickering torch glow, the player, the dog, the chest and the FPS number.
2. Click a distant floor tile. The player walks tile by tile; the camera follows softly near the edge.
3. Click a new tile while the player is walking. The player re‑routes after the current step.
4. Click a blocked tile (furniture tiles look empty, B2). A red "no path" text floats up and fades.
5. Click the player. The camera glides to centre on the player.
6. Press and hold the dog ~1 s. A menu button pops out with animation, hovering scales it, and releasing over it shows the
   dog's description tooltip ("This is the dog that I know…"), which fades after ~2 s. Releasing elsewhere shows nothing.
7. Press `Q`. Everything except the player is darker and desaturated. Press `W` to restore.
8. **Until B1 is fixed, don't walk onto the chest**: the game crashes (confirmed by the spike, pinned by scenario S15).

### 6.4 HTML5 smoke test (optional, macOS)

```bash
./gradlew html:dist && (jwebserver -p 8080 -b 127.0.0.1 -d "$PWD/html/build/dist" &) && sleep 2
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --enable-unsafe-swiftshader \
  --use-angle=swiftshader --window-size=800,1000 --virtual-time-budget=15000 --enable-logging=stderr --v=0 \
  --user-data-dir=/tmp/lg-chrome --screenshot=/tmp/lg.png http://127.0.0.1:8080/ 2>&1 | grep CONSOLE
pkill -f jwebserver
```

Check that there are no console errors and that the screenshot shows the board, the light and the actors.

---

## 7. The plan

Legend: **[D#]** = depends on that decision · **Risk**: L/M/H.

### Phase 0: Safety net

The full design, including the spike that proved it works, is in **[`regression-testing.md`](regression-testing.md)**. Read it before
starting Phase 0. The tests **play the real game** headlessly and observe only what a player could see. Visual (screenshot) tests are deferred.

**0.1 Baseline** (Risk L)
- Check that the migration commit exists and `git status` is clean, and that `./gradlew build html:dist` passes.

**0.2 `e2e` module and headless game** (Risk M)
- Create the `e2e/` module (regression-testing §3.1): the `settings.gradle` include, dependencies and the `test` task.
- Implement `NoOpGL`, `FixedGraphics`, `ScriptedInput` and `HeadlessGame` from the spike notes (§8).
- Acceptance: a smoke test starts the game headless, runs 60 frames and stops it without errors.

**0.3 `GameDriver` and `Snapshot`** (Risk M)
- Implement the driver API (§3.3) and the observations (§3.4). The driver is the **only** code that references game classes.
- Acceptance: running the same script twice produces identical snapshot text.

**0.4 Scenarios and goldens** (Risk M)
- Implement scenarios S1–S16 (§4). Hard‑code the cells; for S2/S3 pick targets with a **unique shortest path** (§4.1).
- Record the goldens: `./gradlew e2e:test -Dgolden.update=true`.
  **Review every golden by hand** against the behaviour in §1. A golden that records wrong behaviour
  (other than the known bugs B1–B5) must be raised before CP0.
- S15 records today's B1 crash on purpose.

**0.5 Focused unit tests** (Risk L)
- In `e2e/test/.../unit`:
  - `MapSnapshotTest`: tile cells, positions, sizes, background region names and neighbour cells, plus each object's cell,
    neutral kind and whether it blocks. Compared against `golden/map.txt`.
  - `PathFindingTest`: ~10 fixed start/end pairs on the loaded map, including an unreachable pair and an inaccessible target.
    Assert the length, that consecutive cells are neighbours, and that the path ends at the target and excludes the start.
  - `InventoryTest`: null padding up to capacity, re‑padding on set, and over capacity throws.
- These go through `GameDriver`‑style helpers too, so that renames touch one place.

**0.6 Coverage review and CI** (Risk L)
- Add JaCoCo to `e2e:test` and review the `core` coverage report. Every uncovered line must be dead code listed in §3.3 or get a
  scenario. Record the outcome in §8.
- CI already exists: `.github/workflows/tests.yml` runs `./gradlew build` (which includes `e2e:test`) and `html:dist` on every push to `master` and every pull request into
  `master`. Nothing to add. **Confirm** that the goldens recorded on macOS pass on the Linux runner: push the Phase 0 commits and
  check the run. If a golden differs only by float noise, round that observation more coarsely in `Snapshot`, then re‑record and review.

**▶ CP0**: tag `refactor-cp0`. Report: scenarios and goldens created, coverage summary, and any surprises found while recording.

### Phase 1: Delete dead code (pure deletions)

**1.1 Remove unused members** (Risk L). Delete everything in §3.3 **except** the D2 feature and the assets:
- `Main.java:27`, the stray `RayHandler` (removing it also removes an unused import).
- `Collectible`: `beforeCollect`, `afterCollect`, `getName`, `getTile`, `DEFAULT`. Remove `Chest`'s implementations and `setName`, and `Lighter`'s
  `beforeCollect`/`afterCollect`.
- `Item.use/unuse` and the `Lighter` implementations; the `Lighter.thumbnail` field.
- `CameraActor.setDebug`/`centerAtWithZoom`; the commented line in `CameraTrackingFeature`.
- `GameLight.getTile/tile/setLightPosition/setLightColor/getLightType` and its `lightType` field (and the constructor parameter);
  `TorchLight.COLOR` and its `setColor(COLOR)` call.
- `TileObject.objectType` and the constructor parameter in all subclasses; remove the `TileObjectType` imports that become unused
  (the enum itself stays, because the loader uses it).
- The commented‑out code in `Tile` (constructor, `setBackground`, `prepare`). **Leave `Tile.id`, `equals` and `hashCode` alone**:
  removing the id from `equals` would change semantics. They all go in 5.3, where tiles switch to identity.
- `TileSet.properties`/`TilesetProperties`/`setProperties` and `TmxTiledMapLoader.getTilesetProperties`.
- `PhraseService.getPhrase` and `Inventory.getCapacity`. (`FadeOutAnimation` stays until 4.3: it is still the builder's default.)
- Acceptance: the step gate passes and the golden files are unchanged.

**1.2 Delete the disabled "scared of darkness" feature** (Risk L) **[D2]**
- Delete `PlayerNotificationPlugin`, `EnlargeAndFadeOutAnimation`, the `PLAYER_NOTIFICATION` value,
  `NotificationStage.doesContainNotification/removeAllNotificationsOfType/getNotificationCount`,
  `Tile.getNotificationCenterPosition` (and the `NOTIFICATION_OFFSET` constant), `Player.getIdleLength`, and `Player.lastIdleChange` with its
  handling in `setStatus` (keep the `PlayerMoveEvent` logic), plus `PhraseGroup.DARKNESS_EXCLAMATION`.
- Delete the plugin construction lines in `Player`, and `Notification.getLabel` if it becomes unused.

**1.3 Delete unused assets** (Risk L)
- Remove `android/assets/ui/`, `map2_outer.tmx`, `tiles.tsx`, `player_detective.png` and `lighter.png`. Also remove `"lighter.png"` from the
  preload list in `ResourcesService`.
- Re‑run the reference check from the analysis: for each removed file, `grep -rF <name>` over `core/src`, `android/assets` and `html`
  must return nothing.

**▶ CP1**

### Phase 2: One wiring object

**2.1 Introduce `GameContext`** (Risk M) **[D1]**
- Create `game/GameContext.java` with `private final` fields and getters:
  - `Resources`/`ResourcesService`, `GameEventService`, `PhraseService`;
  - `World`, `RayHandler`, `OrthographicCamera`;
  - `GameStage gameStage`, `NotificationStage notificationStage`, `MenuStage menuStage`;
  - `BoardService boardService`.
  - **Use exactly these names.** `getStage()` becomes `getGameStage()` and `getUiStage()` becomes `getMenuStage()`. That makes bug B1 visible in the code
    instead of hiding it.
- **Fix the construction order** so no setter is needed:
  1. Load the tiles: the loader needs only resources.
  2. Create `BoardService`. Change its constructor to take `List<Tile>` instead of `Board`, because it only uses `board.getTiles()`.
  3. Create `GameContext`.
  4. Create `Board(context, tiles)`.
  5. Call `prepare(context)` on each tile.
- Replace every use of `GamePrepareConfiguration` and of each `configuration/api/*` interface with `GameContext`. Constructors pull out
  the services they need into fields, as most already do.
- Delete the `configuration/` package (14 files, ~310 lines) and the `StagesConfiguration.getStages()` list. `StageFeature` still needs the
  stages until 4.1, so give it the three stages explicitly.
- Acceptance: the step gate passes, and `grep -r "configuration" core/src` returns nothing.

### Phase 3: Event system

**3.1 Class‑keyed event queue** (Risk M)
- In `GameEventService`:
  - Replace `Map<GameEventType, List<GameEvent>>` with `Map<Class<?>, List<GameEvent>>`, keyed by `event.getClass()`.
  - API: `post(GameEvent)`, `<T extends GameEvent> T poll(Class<T>)` (returns `null` if absent), `boolean has(Class<?>)`.
  - Keep `updateEventsLivesAndClean()` unchanged.
- `GameEvent`:
  - Drop the type and the generic payload. Each event declares its own `final` field and a named getter:
    `TileTouchedEvent.getTile()`, `CollectEvent.getCollectible()`, `MenuOpenEvent.getActor()`, `LookAtMeEvent.getActor()`,
    `PlayerCreatedEvent.getPlayer()`, `AddToStageEvent.getDefinition()`.
  - `MenuCloseEvent` becomes payload‑less, and the `new Vector2(x, y)` in `GameBoardActorListener` goes away.
  - `PlayerMoveEvent` keeps `maxAge = 1`; hard‑code it in its constructor and drop the parameter.
  - Keep the `IMMORTAL` concept as a private detail of `GameEvent`. A protected constructor `GameEvent(int maxAge)` plus a no‑arg constructor meaning immortal is enough.
- Delete `GameEventType`. Move `LookAtMeEvent` out of the one‑class `camera` sub‑package into `implementation` for now; Phase 6 moves everything.
- Rewrite the call sites:
  - The pattern `if (has(X)) { X e = removeEventFromQueue(TYPE, X.class); … }` becomes
    `X e = events.poll(X.class); if (e != null) { … }`.
  - **Careful with `BoardMovementPlugin`.** It must only consume the event when the player is idle:
    `if (player is IDLE) { TileTouchedEvent e = events.poll(..); if (e != null) … }`. Do **not** poll first.
  - **Careful with `MenuFeature`.** It is an if/else‑if: menu‑open has priority, and close is only consumed when there is no open in that
    frame. Keep that order.
  - **Careful with `RemoveOnPlayerMovePlugin`.** It only checks `has(PlayerMoveEvent.class)` and never consumes, because aging removes the event. Keep that.
- Event behaviour is covered by the gameplay scenarios: the menu open/close priority (S8–S10) and waiting until idle (S3).
  **Exception:** the one‑frame `PlayerMoveEvent` has no observable effect until B1 is fixed, because its only consumer is the COLLECT
  button. Keep the aging logic in `updateEventsLivesAndClean()` unchanged and point it out in the commit message for review.

**▶ CP2**: covers Phases 2–3.

### Phase 4: Remove indirection layers

**4.1 Show descriptions directly** (Risk M)
- `DescriptionInteraction.interact()` calls `notificationStage.addActor(new Description(resources, actor, text))`. Today the
  event is consumed later in the same frame by `StageFeature`, so adding immediately is equivalent.
- In `Description`:
  - take `(Resources, GameActor parent, String text)`;
  - make `DESCRIPTION_MAX_WIDTH` `private static final`;
  - **append `removeActor()` to its action sequence**, which fixes the B5 leak with no visible change.
- Delete `AddToStageEvent`, `StageFeature`, `GameActorDefinition`, `DescriptionDefinition`, `AbstractStage` (the stages extend `Stage`
  directly) and `StageType`, plus all `getStageType()` methods.

**4.2 Collect flow** (Risk M)
- `CollectPlugin` polls `CollectEvent` and directly builds the `CollectWindow`, plus an `ActionButton` whose click listener
  adds the window to the stage. Keep **exactly the stage that is used today** (`menuStage`, even though it is wrong; B1 is fixed in Phase 7).
- Delete `CollectResolver`, `PlayerCollectResolver` and `OpenCollectWindowButtonListener`.
- `CollectWindow` takes `(GameContext context, Inventory playerInventory, Inventory collectibleInventory)`.
  Delete the `Collecting` interface; `Player.getInventory()` stays as a plain method.
- Merge `getCollectibleTable`/`getInventoryTable` into one `createItemsTable(Inventory, CollectItemType)`.
- Make the constants (`WIDTH`, `HEIGHT`, `PADDING`, `PADDING_ITEM`) `private static final`.
- **Keep `backgroundTable.debug()`** for now (B4).

**4.3 Notifications and labels** (Risk M)
- The **font is loaded once**: in `ResourcesService`, through `AssetManager` (`BitmapFont.class`, `"font.fnt"`), disposed with the
  manager. This fixes the B5 font leak.
- `ResourcesService.createLabel(Color color, String text)` and `createLabel(Color, Drawable background, String text)` replace
  `LabelFactory`. They give the same `LabelStyle`: white default, `Color.FIREBRICK` for warnings.
- `Notification` gets a static factory for its only real use:
  `Notification.warning(Resources, String text, Vector2 center)`. It builds the label, adds
  `parallel(moveBy(0, 20, 2f), fadeOut(2f))` and centres itself. It keeps its self‑removal in `act()`.
- Delete `NotificationBuilder`, `LabelFactory`, `NotificationAnimation`, `MoveUpperAndFadeOutAnimation`, `FadeOutAnimation`,
  `NotificationType` and `NotificationStyle`. Delete `Notification.getCenter()` if unused.
- Update the callers: `BoardClickListener`, `MenuButton` (white label `"button"`), `FPSFeature` and `Description`.

**4.4 Lights** (Risk L)
- Make `TorchLight` the only light class: it is an `Actor` or `Group` that owns its `PointLight`. Its `setPosition` and `setColor` forward
  to the light, as `GameLight` does today. Fold `GameLight` into it.
- `BlinkingAction` keeps its behaviour with the constants `INTENSITY = 5` and `DELAY = 0.2f`. Delete `BlinkingIntensity`/`BlinkingSpeed`.
- Delete `LightFactory`. `TileLight` validates `lightType` with `LightType.valueOf(..)`; keep the 1‑value enum, which validates the map data,
  then does `new TorchLight(rayHandler)`.
- Replace the inner `TileLightAttributes` class with plain `private final` fields on `TileLight` (type, color, border position).

**4.5 Remove the thumbnail mechanism** (Risk L) **[D3]**
- `MenuButton` and `CollectItem` use `resources.getRegion("collectButton")`. Delete `Thumbnail`, `ThumbnailType`,
  `ResourcesService.getThumbnail` and all `getThumbnailType()` implementations. `Item` becomes a marker interface; that is fine.

**4.6 Inline trivial listeners** (Risk L)
- Turn these into anonymous classes at their single use site:
  - `PlayerClickListener`, `MenuButtonListener`, `CollectWindowCancelListener` and `CollectWindowOkListener`;
  - `StopClickThroughClickListener`: a small private static factory method in `CollectWindow`, because it is used twice.
- Move the logic of `CollectWindowItemListener` into a `CollectWindow.onItemClicked(CollectItem)` method. The listener becomes a one‑line anonymous class.

**▶ CP3**

### Phase 5: Simplify the domain model

**5.1 Actors** (Risk M)
- Delete `equals`/`hashCode` from `GameActor` and `GameBoardActor`. Also delete `id`/`ID_COUNTER`, the `GameActorType` enum and its constructor
  parameter everywhere.
- **Move status into `Player`.** `Status` is used only by the player. `GameActor` no longer has a status, and the constructor no longer calls an
  overridable method (A9).
- Change the constructor to `GameBoardActor(GameContext context, String avatarRegion, String description)`. It creates the avatar and the
  description interaction, so the abstract `getDescription()` goes away (A9). `Dog` shrinks to a constructor call.
  - Avatars come from `resources.getRegion("dog.png")`. `Image` renders a `Texture` and a `TextureRegion` identically.
- Make `Lighter` a plain `class Lighter implements Item {}`. It is not a scene2d actor, so its menu interaction and listener disappear with no visible effect:
  it is never on a stage. `TileChest` creates it without a context.
- `Collectible` keeps only `isDistant()` and `getInventory()`. Delete `Interactive`: `GameBoardActor.getInteractions()` stays as a plain method.
- `Inventory`: change the constructor to `Inventory(int capacity)` (null‑padded) and rename `setItemsAndCompleteWithNulls` to `setItems`, keeping the same
  semantics. `InventoryTest` stays green.
- Keep `GameActor.setPosition(Vector2)`/`moveBy(Vector2)`; they make the camera code read better.

**5.2 Resources** (Risk M)
- Rename `ResourcesService` to `Resources`. API:
  - `getRegion(name)`: atlas first, then preloaded textures. Use `assetManager.isLoaded(name)`, not `catch (Throwable)`.
  - `getNinePatch(name)`, `getSkin()`, `getFont()`, `createLabel(..)`, `dispose()`.
- **Make `getAssetManager()` private.** `CollectItem` creates its drawables through `new TextureRegionDrawable(resources.getRegion(..))`.
- Delete `ShaderType`. `GameStage` loads `shader/fadeOut/vertex.shd` and `fragment.shd` itself (`Gdx.files.internal`).
- **Keep the preload list and the Linear filter settings unchanged.**

**5.3 Tiles** (Risk M)
- Delete `AccessibleTile`, `NoPathTile` and `TileFactory`. `FloorTile` and `WallTile` extend `Tile` directly, and the loader picks the class with a
  `switch` on `TileType`.
- **The loader sets the background region directly.** It uses the tile's `textureName` property if present, otherwise the class default, so
  `Tile.attributes` and `FloorTile.prepare`'s texture logic go away. `setBackground` becomes private.
- **Accessibility:**
  - `FloorTile` is blocked if any of its objects blocks. Compute it in `addTileObject`, which only happens at load time, so it is equivalent.
  - Rename `TileObject.isAccessible()` to `isBlocking()` with the inverted meaning. Only `TileSprite` returns `true`, so the default
    becomes `false` in `TileObject` and the other subclasses drop their override.
- Delete `equals`/`hashCode` (identity) and `TilePoint`. Build the polygon in one method that lists the 6 corners. Replace
  `calculatePositionForActorAt(actor, CENTER)` with `positionFor(Actor actor)`, which returns the bottom‑left position that centres the actor
  on the tile.
- `getPolygonVertices()` returns `polygon.toArray(Vector2.class)`.
- **Keep B3 as it is.** Move the throwing `stageToLocalCoordinates` into `WallTile` as a private method with `// TODO bug B3`.
- `Tile.prepare(GameContext)` keeps only `object.prepare(this, context)` for each object.

**5.4 Tile objects** (Risk L)
- Delete `TileObjectAttribute`. Use `private static final String` keys in the classes that read them: the loader reads
  `objectClass`, `TileSprite` reads `textureName`/`textureOffset`, and `TileLight` reads `lightType`/`lightColorHex`/`borderLightPosition`.
  Same for `TileAttribute` (`tileClass`).
- `TileObjectFactory.createTileObject` becomes a private method of the loader. Keep `assertMandatoryProperties` behaviour (same messages).
- Keep `TileChest`, `TileDog`, `TilePlayer` and `TileSprite` as tiny separate classes; they read well. **Don't change `TileSprite.prepare`** (B2 decides).

**5.5 Map loader** (Risk M)
- Replace `TiledMapLoader`, `TmxTiledMapLoader`, `TmxTileMapLoaderProperties`, `TileSet` and `Tuple` with
  `TmxBoardLoader { TmxBoardLoader(Resources); List<Tile> load(String mapFile) }`.
- **Use `GridPoint2`** (libGDX, value equality) for cell coordinates and keep the maps **local** to `load()`, not instance fields.
- Extract the hex math into private methods that are each written once:
  - `cellWidth(tileHeight)`;
  - `cellPosition(col, row)` and `cellCenter(col, row)`;
  - `neighbourCells(col, row)`, which returns the 6 staggered‑odd neighbours.
- **Keep the nearest‑cell search for objects exactly as it is**, including that it scans empty cells and that a later object on the same cell
  overwrites an earlier one (`objectsMap.put`, line 130). Replace the linear `addTileObjects` scan with `objects.get(cell)`, which is equivalent
  because there is at most one object per cell.
- Keep `tiledMap.dispose()`.
- `MapSnapshotTest` keeps the golden file **unchanged**.

**5.6 Board service and movement** (Risk M) **[D4]**
- Fold `GameActorsByTile` into `BoardService` (`Map<Tile, List<GameActor>>`). Collision checks stay the same: the same tile, plus `isDistant`
  neighbours, and an event per collectible.
- Delete `BoardPath`. `getPath` returns `List<Tile>` (start excluded, target included, empty if unreachable).
- **[D4a]** Implement it as BFS from the start over accessible tiles, never expanding from an inaccessible tile. Otherwise **[D4b]** keep Dijkstra but
  simplify it. `PathFindingTest` must pass either way.
- `BoardMovementPlugin`:
  - replace `removePlayerActions` with `player.clearActions()`;
  - replace the anonymous status actions with `Actions.run(() -> player.setStatus(..))`;
  - delete the `path.exists()` check inside the per‑node action, because it is always true inside the loop.
- Leave `GameBoardActor.setTile` as it is, with its order: unregister the old tile, register the new one (which triggers
  collisions), then set the position.

**5.7 Small cleanups** (Risk L)
- `PhraseService` and `PhraseGroup` become one enum, `Phrase`, with `String random()` returning
  `values[MathUtils.random(values.length - 1)]`. It no longer shuffles, which fixes B6.
- `GameScreen`:
  - extend `ScreenAdapter`, which removes the empty methods;
  - `dispose()` disposes all three stages, the shader (through `GameStage.dispose()`) and the `RayHandler`;
  - `Main.dispose()` calls `getScreen().dispose()` before disposing the world and resources (fixes B5).
- `Board`:
  - delete `toBack()` from `draw()` and add the board at index 0 once (`gameStage.getRoot().addActorAt(0, board)`);
  - convert the click point once, not per tile.
- `MenuStage` (A8):
  - make the selection explicit with `interactWithButtonUnderPointer()`;
  - `MenuFeature` calls it, then `clear()`;
  - delete the `clear()` override;
  - replace the cast with an `instanceof MenuButton` check on the hit actor's parent;
  - keep the `addActor` type check, because B1 depends on it until Phase 7.
- Make the remaining instance "constants" `static` (`MenuFeature`).

**▶ CP4**

### Phase 6: Package structure (pure moves)

**6.1 Move to the target layout in §4** (Risk L)
- Use `git mv` and fix package and import statements only. **No code changes** in this step.
- Check that `html/src/com/mantkowicz/light/game/client/HtmlLauncher.java` and `desktop/.../DesktopLauncher.java` still import
  `com.mantkowicz.light.game.Main`, which doesn't move.
- `core/src/LightGame.gwt.xml` (`<source path="com/mantkowicz/light" />`) doesn't need to change.
- Acceptance: the step gate passes, and no package is left with only 1 class, except where §4 says so.

**▶ CP5**: final report with before/after file and line counts.

### Phase 7: Bug fixes (behaviour changes, each one only after approval) **[D5]**

**7.1 B1: Collect flow crashes.** Add a fourth, plain `Stage`, `uiStage`, for `ActionButton` and `CollectWindow`:
- it shares the viewport;
- it is drawn after `menuStage`;
- in the input multiplexer it comes **before** `menuStage`;
- it lives in `GameContext`.

`MenuStage` stays exclusive to menu buttons, so `menuStage.clear()` no longer affects the collect UI. Test the full flow:
1. Walk onto the chest: a COLLECT button appears.
2. Click it: the window opens.
3. Select the lighter, move it to the player's inventory, and press OK; the inventories update.
4. CANCEL closes the window without changes.
5. Walking away fades out the button.

**7.2 B4: Debug lines.** Delete `backgroundTable.debug()`, together with 7.1.

**7.3 B2: Furniture not drawn.** `TileSprite` keeps its region and offset, and `Tile.draw` draws its sprites after the background.
- The position is relative to the tile:
  `x + 74 - w/2 + offset.x`, `y + 64 - h/2 + offset.y`.
- Replace the magic `74`/`64` with `width/2`, `height/2`. Check that the result is identical for the 148×128 tiles (74 and 64), and note it if not.

**7.4 B3: Walls.** In `WallTile`, create the body at the origin (`BodyDef` position `(0,0)`) with a `ChainShape` loop over the absolute polygon
vertices, and delete the throwing method. Check this by temporarily putting a wall tile into a test map: torch light must be
blocked by it.

**▶ CP6**

### Optional

- **O1.** Merge `Plugin` and `Feature` into one interface (A10). Only do this if the human finds the two names confusing.
- **O2.** Give the UI its own `ScreenViewport` camera. `ActionButton` and `CollectWindow` would then not need `CenterActorByCameraPlugin`. This is a visible
  behaviour change, because the UI would stop scaling with the world, so it needs a separate decision.

---

## 8. Found during execution

(Executing agents append here: date, step, finding, and what was done or deferred.)
