# Light Game: Refactoring Plan

Goal: make the codebase **as simple, consistent and human‑readable as possible while keeping the current behaviour
and every tool it has**.
The plan is written for step‑by‑step execution by agents (or a human), with checkpoints to stop, review and
roll back.

- Scope: the `core` module (all game code) plus the small launcher touch points in `desktop`/`html`.
- Baseline: commit that contains the libGDX 1.14.2 / Gradle 9.8.2 / HTML5 migration. **The migration must be
  committed before Phase 0 starts.**
- Size today: **129 Java files, ~4,730 lines** in `core`. How much smaller it gets depends on the keep‑or‑remove answers (§5.2).
- **Direction (2026‑11‑06):** the game is a snapshot of past experiments, so unused code is often a tool that is temporarily
  not called. The plan therefore focuses on consistency (actors, plugins, features look the same; shared code in one place;
  debug tools in one place) and removes nothing without a per‑item decision.

---

## 0. Rules for whoever executes this plan

1. **One step = one commit.** Message format: `refactor(<step id>): <summary>`, for example `refactor(2.1): introduce GameContext`.
   Bug‑fix steps (Phase 7) use `fix(<id>): …`.
2. **Behaviour must not change** in Phases 0–6. If a step seems to need a behaviour change, stop and ask.
   Known bugs are listed in §3.4 and fixed **only** in Phase 7, after approval.
3. **Nothing is removed just because it is unused.** Deleting a method, class, debug tool or asset needs the human's approval
   for that item (§5.2). Unapproved items stay and keep working when their class is moved or reshaped. §3.3 lists them.
4. **No drive‑by changes.** Do only what the step says. If you find something new, add it to §8 ("Found during
   execution") and continue.
5. **Every step must pass the step gate (§6.1)** before committing. Every phase ends with a **checkpoint (§6.2)**: tag
   it, then STOP and wait for human review before starting the next phase.
6. **Rollback:** `git reset --hard refactor-cp<N>` returns to the last approved checkpoint. To replay a single step,
   reset to the previous step's commit and re‑execute it.
7. **GWT (HTML5) constraints.** `core` is also compiled to JavaScript by GWT, so do not use:
   - reflection: `Class.isInstance`, `Class.forName`, `getDeclaredFields`, …
   - `String.format`, threads or `synchronized`, `java.io` file APIs.

   `getClass()`, class literals as map keys, lambdas, anonymous classes, enums and `switch` on strings or enums are fine.
   `./gradlew html:dist` is part of every gate because it catches these.
8. **Style to keep:**
   - 4‑space indentation and explicit imports. Static imports are OK, as the code already uses them.
   - `private final` fields where possible, no Lombok, no new libraries in `core`.
   - Constants are `private static final`.
9. Don't touch `android/`, `html/src/**/emu/**`, or the build files, except where a step says so.
10. Decisions in §5 must be answered by the human **before** the steps that depend on them. Each step lists its
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

Items that would **remove** a class, method or extension point are not done automatically: they are listed with a K number and decided
one by one in §5.2.

- **Generated `equals`/`hashCode`** in `GameActor`, `GameBoardActor` and `Tile`. Ids are unique per instance, so they behave like identity
  anyway. They also hash **mutable** state (`tile`, `polygon`, `pluginsQueue`), which is a latent bug for `HashMap` keys. Switching to
  identity is equivalent; whether `id`/`GameActorType` then go is **K9**.
- **Enum‑with‑value wrappers:** `TileAttribute`, `TileObjectAttribute` (they document the map's property names, so they stay);
  single‑value enums `ShaderType` (K16), `ThumbnailType` (K8), `LightType` and enums where one value is used, `BlinkingIntensity`,
  `BlinkingSpeed` (K7). These are extension points.
- **Trivial listener classes in separate files:** `PlayerClickListener`, `MenuButtonListener`, `OpenCollectWindowButtonListener`,
  `CollectWindowCancelListener`, `CollectWindowOkListener`, `StopClickThroughClickListener`. Today some listeners are named classes and
  some are anonymous, with no rule (see C9).
- **Duplicated code** (moved to one common place in 3.5):
  - `LabelFactory` has two near‑identical methods.
  - `CollectWindow.getCollectibleTable()`/`getInventoryTable()` are identical.
  - `TileChest`/`TileDog`/`TilePlayer` are the same 10 lines.
  - The hex width formula appears twice and the cell‑position formula twice (`TmxTiledMapLoader`).
  - `Notification.getCenter()` duplicates `GameActor.getCenter()`.
  - Anonymous `Action` classes that only call one method (`BoardMovementPlugin`, `RemoveOnPlayerMovePlugin`), where libGDX has
    `Actions.run(..)` and `Actions.removeActor()`.
- **Pathfinding is O(n²) Dijkstra on a graph where every edge has weight 1** (`BoardService.getPath`). A BFS does the same job (D4).
- **`GameScreen` has empty `pause/resume/hide` methods.** Extending `ScreenAdapter` removes them.
- **"Constants" declared as instance fields:** `final` but not `static` (`MenuFeature`, `CollectWindow`, `Description`).

### 3.3 Unused today, kept on purpose

The game is a snapshot of earlier experiments: some usages were removed temporarily while playing with mechanics. **Nothing in this
table is deleted by the plan.** It is listed so that nobody "cleans it up" by accident, and so refactoring steps keep it working
(for example, a moved class keeps these methods). Clean‑up candidates that need a decision have a K number (§5.2).

| Item | What it is for |
|------|----------------|
| `CameraActor.setDebug` (camera tracking box) and the commented call in `CameraTrackingFeature` | Debug tool. Goes to the debug place (D6) |
| `CameraActor.centerAtWithZoom` | Camera helper |
| "Scared of darkness": `PlayerNotificationPlugin` (built, `addPlugin` commented out), `EnlargeAndFadeOutAnimation`, `NotificationType.PLAYER_NOTIFICATION`, `NotificationStage.doesContainNotification/removeAllNotificationsOfType/getNotificationCount`, `Tile.getNotificationCenterPosition`, `Player.getIdleLength`/`lastIdleChange`, `PhraseGroup.DARKNESS_EXCLAMATION` | Switched‑off feature (D2) |
| `Collectible.beforeCollect/afterCollect/getName/getTile/DEFAULT`; `Chest.setName("Nice Chest")` | Collect hooks, used by the collect flow once B1 is fixed |
| `Item.use/unuse` (`Lighter`: "TURN LIGHTER ON/OFF") | Item hooks |
| `Lighter` as a `GameBoardActor` with a thumbnail field | An item that could also lie on the board (K10) |
| `GameLight.getTile/setLightPosition/setLightColor/getLightType`; `TorchLight.COLOR` (overwritten by the map colour today) | Light helpers and default colour |
| `TileObject.objectType` (never read; `TileLight` passes `CHEST`, which looks like a copy‑paste slip) | Object type information. 3.6 fixes the slip: `TileLight` passes `LIGHT` |
| `Tile.getId`, `TileSet.properties`/`TilesetProperties` (tile size, written, never read) | Map information (K14) |
| `PhraseService.getPhrase`, `Inventory.getCapacity`, the `Inventory` constructor path for a `null` list | Small API |
| `GameEventService.getEvent(..)` | Peek at an event without consuming it |
| `BoardPath.setPath/getLastNode`, `GameBoardActor.remove()` | Path and board helpers |
| `LabelFactory.createLabel(WARNING, background, text)`, `FadeOutAnimation`, `NotificationType` | Notification tools (K6) |
| `MenuCloseEvent` payload (pointer position) | Where the menu was released (K17) |
| The "distant collectibles" hook (`Collectible.isDistant()`, neighbour loop in `BoardService.checkCollisions`) | Collecting from a neighbour tile |
| Unused assets: `android/assets/ui/` (uiskin), `map2_outer.tmx`, `tiles.tsx`, `player_detective.png`, `lighter.png` (preloaded, never fetched by that name) | K18 |

Real clean‑up candidates, not tools (decide in §5.2): the stray `new RayHandler(world)` in `Main` that is never used and leaks a
framebuffer (K1); commented‑out code (K2); `System.out.println` placeholders (K3).

### 3.4 Bugs found (**do not fix before Phase 7**; each needs approval)

| # | Bug | Evidence |
|---|-----|----------|
| B1 | **Walking onto the chest crashes** (confirmed in the headless spike). `PlayerCollectResolver` adds `ActionButton` to `getUiStage()`, which is `MenuStage`, and `MenuStage.addActor` throws `RuntimeException("Actor on this stage must be instanceof MenuButton")`. Even if it were allowed, `MenuFeature` calls `menuStage.clear()` on every menu close, which would delete the button and window, and `MenuStage.getButton()` would throw `ClassCastException`. | `PlayerCollectResolver.java:26`, `MenuStage.java:38` · scenario S15 |
| B2 | **Furniture (19 `SPRITE` objects: bed, bath, tv, …) is never drawn.** `TileSprite.prepare` builds an `Image` but the `tile.addActor(image)` line is commented out, because `Tile` is no longer an `Actor`. The tiles still block movement, so the player sees empty tiles that answer "no path". | `TileSprite.java:32`; visible in the HTML5 screenshot |
| B3 | **Any `WALL` tile crashes on load.** `WallTile.prepare` calls `Tile.stageToLocalCoordinates`, which throws `UnsupportedOperationException`. The tileset defines WALL (gid 2); the current map doesn't use it. | `Tile.java:218`, `WallTile.java:22` |
| B4 | **Debug outlines are drawn over the collect window** (`backgroundTable.debug()` turns on stage debug drawing). Only visible after B1 is fixed. | `CollectWindow.java:58` |
| B5 | **Resource leaks.** Every label loads a **new `BitmapFont`** from disk that is never disposed: each "no path" text, each menu button, each tooltip. Description tooltips fade to alpha 0 but are **never removed** from the stage. `GameScreen.dispose()` is never called and doesn't dispose `menuStage`, `notificationStage` or the shader. | `LabelFactory.java:14,31`, `Description.java:33`, `GameScreen.dispose()` |
| B6 | `PhraseService.getRandomPhrase` shuffles the enum's backing array in place. It's harmless but surprising. It gets fixed in step 5.6. | `PhraseService.java:20` |

B5 and B6 are invisible to the player and are fixed **inside** the refactoring steps that rewrite that code (4.1, 4.3, 5.6),
because the new code simply doesn't have the leak. All other bugs wait for Phase 7.

### 3.5 Consistency findings

The same kind of class is written in different ways. These are the main targets of Phase 3.

**Actors**

| # | Finding | Where |
|---|---------|-------|
| C1 | **Avatars load three ways.** `Dog`, `Chest` and `Player` each have an `AVATAR_RESOURCE_NAME` constant and call `getAssetManager().get(name, Texture.class)`; `Lighter` calls `getTexture("lighter")`; `GameActor.createAvatar` takes a `Texture`. | `actor/implementation/**` |
| C2 | **The description is an abstract method called from the base constructor** (A9). `Dog` returns a sentence, `Player` returns `"player"`, `Lighter` has one nobody can see. | `GameBoardActor.java:25` |
| C3 | **The inventory is declared twice:** `Collecting.getInventory()` (the player) and `Collectible.getInventory()` (the chest, and `Lighter`, which returns `null`). Sizes are magic numbers in each constructor (`3`, `2`), each with `new ArrayList<Item>()`. | `Player`, `Chest`, `Lighter` |
| C4 | **Status lives in `GameActor`, set in three places.** The base constructor calls the overridable `setStatus(IDLE)` before `Player`'s fields exist, and `Player` calls `setStatus(IDLE)` again at the end of its constructor. | `GameActor.java:30`, `Player.java:52` |
| C5 | **Listeners are attached differently.** The press‑and‑hold menu listener is added by `GameBoardActor`; the player's click listener is a separate class in the player package; `Chest` disables touch while `Dog` relies on the default. | `GameBoardActorListener`, `PlayerClickListener` |
| C6 | **Plugins are wired differently per actor.** `Player` builds three plugins inline (one built and not added); `ActionButton` builds two. | `Player`, `ActionButton` |

**Plugins and features**

| # | Finding | Where |
|---|---------|-------|
| C7 | **Constructor parameter order differs:** `BoardMovementPlugin(player, speed, configuration)`, `CollectPlugin(resolver, configuration)`, `RemoveOnPlayerMovePlugin(eventService, actor)`, `CenterActorByCameraPlugin(actor, camera, offset)`; features: `CameraTrackingFeature(actor, configuration)`, `MenuFeature(configuration)`, `FPSFeature(stage, offset)`. Some fields are `final`, others not. | `plugin/implementation`, `feature/implementation` |
| C8 | **Non‑plugins live in `plugin/`:** `CollectResolver`, `PlayerCollectResolver` and `OpenCollectWindowButtonListener` sit next to `Plugin`, while the plugins sit in `plugin/implementation`. | `plugin/` |
| C9 | **No rule for listeners and small actions:** some are named classes in their own file, some are anonymous, some are anonymous `Action` subclasses that libGDX already provides (`Actions.run`, `Actions.removeActor`). | see §3.2 |
| C10 | **Features add their own actors in different ways:** `CameraTrackingFeature` creates the `CameraActor` and adds it to the stage from the configuration; `FPSFeature` takes the stage directly. `GameScreen` registers features in two places (the camera one inside an event check). | `GameScreen.show()` |

**Debug tools**

| # | Finding | Where |
|---|---------|-------|
| C11 | **Debug switches are scattered and toggled by commenting code:** the camera tracking box (`CameraActor.setDebug`, call commented out), a brighter ambient light (commented line in `GameScreen.prepareLights`), table debug lines (`backgroundTable.debug()`, on by accident: B4), the FPS label (always on), the darkness exclamation (commented `addPlugin`). | various |

---

## 4. Target architecture

Packages are organised by feature, with no `api/`, `factory/` or `implementation/` layers. Sub‑packages exist only where they hold ~5+ classes.
The layout below assumes every K decision is **keep**; a class marked `(K#)` disappears only if that item is approved for removal.

```
com.mantkowicz.light
├── game/          Main, GameScreen, GameContext
├── debug/         Debug (all debug switches in one place, D6)
├── resources/     ResourcesService, ShaderType (K16), Thumbnail + ThumbnailType (K8), PhraseService, PhraseGroup
├── event/         GameEvent, GameEventService (queue), TileTouchedEvent, CollectEvent, PlayerMoveEvent,
│                  LookAtMeEvent, PlayerCreatedEvent, MenuOpenEvent, MenuCloseEvent, AddToStageEvent (K4)
├── board/         Board, BoardClickListener, BoardService, GameActorsByTile, BoardPath (K15),
│   │              Tile, AccessibleTile + NoPathTile (K13), FloorTile, WallTile, TileFactory (K13), TilePoint (K12), TileType
├── map/           TiledMapLoader (K14), TmxTiledMapLoader (+ hex grid math), TileSet (K14), TileObject, TileObjectType,
│                  TileObjectAttribute, TileObjectFactory, TileSprite, TileActorObject (TileChest/TileDog/TilePlayer), TileLight
├── actor/         GameActor, GameBoardActor, ActorListeners, Status, Player, Dog, Chest, CameraActor,
│                  Inventory, Item, Lighter, Collectible, Collecting
├── light/         GameLight, TorchLight, LightType, LightFactory, BlinkingAction, BlinkingIntensity, BlinkingSpeed (K7)
├── plugin/        Plugin, BoardMovementPlugin, CollectPlugin, CenterActorByCameraPlugin, RemoveOnPlayerMovePlugin,
│                  PlayerNotificationPlugin (D2)
├── feature/       Feature, CameraTrackingFeature, MenuFeature, FPSFeature, StageFeature (K4)
├── notification/  Notification, NotificationBuilder, NotificationAnimation + 3 animations, NotificationType, NotificationStyle (K6)
└── ui/            GameStage, MenuStage, NotificationStage, ActionButton, StageType (K4)
    ├── menu/      MenuButton, Interaction, Interactive (K11), DescriptionInteraction, Description, DescriptionDefinition (K4)
    └── collect/   CollectWindow, CollectItem, CollectItemType, CollectWindowItemState, CollectResolver (K5), PlayerCollectResolver (K5)
```

Principles:
- **One wiring object, `GameContext`.** It has final fields, getters only, and is built once in `GameScreen`. Classes take what
  they need from it **in their constructor** and store only those fields; they don't keep the context itself.
  `TileObject`s, which run at spawn time, are the exception and receive it as a method argument.
- **One shape per kind of class** (Phase 3):
  - an actor: `super(context, avatarName, description)`, then its own fields, then `addPlugin(..)` calls, then listeners;
  - a plugin: `new XPlugin(ownerActor, context, params…)`, all fields `private final`, one `run(delta)` that polls its event first;
  - a feature: `new XFeature(context, params…)`, same rules; features that show something add their actor in the constructor.
- **Events are keyed by their class:** `events.post(new TileTouchedEvent(tile))`,
  `TileTouchedEvent e = events.poll(TileTouchedEvent.class)` (returns `null` if absent), and `events.has(PlayerMoveEvent.class)`.
  Payloads have named getters, e.g. `getTile()`.
- **The map/data layer** (`map/`) turns Tiled data into tiles and spawn objects. **The board** (`board/`) is the runtime model.
  **Actors** (`actor/`) are scene2d actors that live on the board.
- The cycle `actor ↔ board` (actors know their `Tile`; `BoardService` knows actors) is acceptable for a game this size.
  Don't add interfaces just to break it.

---

## 5. Decisions needed from the human (answer before the dependent steps)

### 5.1 Design decisions

| # | Question | Options | Recommendation |
|---|----------|---------|----------------|
| D1 | Replace the 13 configuration interfaces with one `GameContext` class? Nothing is lost: every getter stays, in one class. | (a) one `GameContext`; (b) constructor injection of individual services everywhere; (c) keep interfaces but trim them | **(a)**: least code and clear to read. (b) makes some constructors take 6–7 parameters. |
| D2 | The switched‑off "scared of darkness" feature (§3.3)? | (a) delete (git history keeps it); (b) keep, switched off by a named flag in `Debug` instead of a commented‑out line; (c) re‑enable (behaviour change, goes to Phase 7) | **Answered: (b)**, keep it |
| D4 | Replace Dijkstra with BFS in path finding? | (a) BFS: same path **length**, but when several shortest paths exist it may pick a different one; (b) keep Dijkstra and only tidy it | **(a)**. Today's tie‑breaking depends on `HashMap` iteration order, so it isn't a designed behaviour. |
| D5 | Fix bugs B1–B4 in Phase 7? | per bug: fix / leave / delete the feature | B1: **fix** by adding a real UI stage (see 7.1). B2: **fix** by drawing sprites. B3: **fix** body creation. B4: **fix** by turning the debug lines into a `Debug` switch that is off. |
| D6 | Debug tools (C11): one place for all of them? | (a) a `Debug` class with `public static boolean` switches (camera box, bright ambient light, table debug lines, FPS label, darkness exclamation), values set so the game behaves exactly as today; (b) the same, but read from launcher arguments or system properties; (c) leave them where they are | **Answered: (a), with per‑object control**: switch each tool separately, and outline only chosen actor classes (e.g. only `Dog`), not all‑or‑nothing. |

(D3 became K8.)

### 5.2 Keep or remove (default: keep)

Each item stays unless you approve its removal. Answer per item; "keep" needs no answer. Approved removals are done in the step listed.

| # | Item | If removed | Step | Recommendation |
|---|------|-----------|------|----------------|
| K1 | Stray `new RayHandler(world)` in `Main` (never used, leaks a framebuffer) | One line and an import go | 3.6 | **Answered: remove.** Not needed for new lights: they use `GameScreen`'s `RayHandler` through `context.getRayHandler()` |
| K2 | Commented‑out code: `Tile` (actor‑era constructor, `setBackground`, `prepare`), `GameScreen` ambient line, `CameraTrackingFeature` debug line, `Player` `addPlugin` | Debug lines become `Debug` switches (D6) either way; the rest is deleted | 3.4/3.6 | **Answered: remove** (debug lines become `Debug` switches) |
| K3 | `System.out.println` placeholders in `Chest.beforeCollect/afterCollect` and `Lighter.use/unuse` | The methods stay; only the printing goes (or becomes `Gdx.app.log`) | 3.6 | **Answered: `Gdx.app.log`** |
| K4 | "Add to stage by event": `AddToStageEvent`, `StageFeature`, `StageType`, `AbstractStage.getStageType()`, `GameActorDefinition`, `DescriptionDefinition` | `DescriptionInteraction` adds the tooltip to the notification stage directly (7 types for one `addActor` call today) | 4.1 | your call: keep if other code should add actors to stages without knowing them |
| K5 | `CollectResolver` interface (one implementation) and `OpenCollectWindowButtonListener` | `CollectPlugin` builds the window and button itself | 4.2 | keep if non‑player collectors are planned |
| K6 | Notification tools: `NotificationBuilder`, `NotificationAnimation` + `FadeOutAnimation`, `EnlargeAndFadeOutAnimation`, `MoveUpperAndFadeOutAnimation`, `NotificationType`, `NotificationStyle` | One `Notification.warning(..)` factory; animations become plain libGDX actions | 4.3 | keep (they're tools); only fix the font leak |
| K7 | Light extension points: `LightFactory`, abstract `GameLight`, `BlinkingIntensity`/`BlinkingSpeed` enums | `TorchLight` is the only light class with constants | 4.4 | keep if more light types are planned |
| K8 | Thumbnail mechanism (`Thumbnail`, `ThumbnailType`, `getThumbnail`); today every item uses `collectButton` | `MenuButton`/`CollectItem` use `"collectButton"` directly | 4.5 | keep if per‑item icons are planned |
| K9 | Actor ids and `GameActorType` (only used by the generated `equals`) | Identity equality; enum and constructor parameter go | 5.1 | keep the enum (type info for debugging); identity equality either way |
| K10 | `Lighter` as a `GameBoardActor` (it's never placed on a board) | `Lighter` becomes a plain `Item` | 5.1 | keep if items should lie on the board later |
| K11 | `Interactive` interface (one implementer) | `GameBoardActor.getInteractions()` stays as a plain method | 5.1 | keep |
| K12 | `TilePoint` (6 corners + centre; only `CENTER` is used outside `Tile`) | Polygon built from a list; `positionFor(actor)` replaces `calculatePositionForActorAt(actor, CENTER)` | 5.3 | keep: placing actors at corners is a useful helper |
| K13 | Middle tile layer `AccessibleTile`/`NoPathTile` and `TileFactory` (`FloorTile` overrides `isAccessible` anyway) | `FloorTile`/`WallTile` extend `Tile`; the loader picks the class with a `switch` | 5.3 | your call |
| K14 | Map loader abstraction: `TiledMapLoader<T>` interface, `TmxTileMapLoaderProperties`, `TileSet` + `TilesetProperties` (written, never read) | One `TmxBoardLoader.load(mapFile)` returning `List<Tile>` | 5.5 | keep if other map formats are planned; `TilesetProperties` could be completed instead (read the tile size from it) |
| K15 | `BoardPath` (wraps a `List`; `isEmpty()` and `exists()` both exist; `setPath`/`getLastNode` unused) | `getPath` returns `List<Tile>` | 5.6 | keep, but make `exists()` mean `!isEmpty()` explicitly |
| K16 | `ShaderType` (one value) | `GameStage` loads its shader by path | 5.2 | keep if more shaders are planned |
| K17 | `MenuCloseEvent` payload (pointer position) | Payload‑less event | 2.1 | keep, with a named getter `getPointer()` |
| K18 | Unused assets (§3.3) | Files deleted; `"lighter.png"` leaves the preload list | 7.5 | **Answered: remove at the very end** of the refactoring (step 7.5) |
| K19 | `Plugin` vs `Feature`: identical interfaces (A10) | One interface | O1 | keep both: the names say per‑actor vs per‑screen |

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

### Phase 0: Safety net (done, tag `refactor-cp0`)

Regression tests that **play the real game** headlessly and observe only what a player could see. How they work, how to
run them, the rules for golden files and the scenario ids S1–S16 are in **[`e2e/README.md`](../e2e/README.md)**. Read it
before any phase. The outcome of Phase 0 (coverage, deviations, findings) is in §8. Visual (screenshot) tests are deferred.

### Phase 1: One wiring object

Comes first because every later step changes constructors: doing it first means each constructor is rewritten once.

**1.1 Introduce `GameContext`** (Risk M) **[D1]**
- Create `game/GameContext.java` with `private final` fields and getters:
  - `ResourcesService`, `GameEventService`, `PhraseService`;
  - `World`, `RayHandler`, `OrthographicCamera`;
  - `GameStage gameStage`, `NotificationStage notificationStage`, `MenuStage menuStage`;
  - `BoardService boardService`.
  - **Use exactly these names.** `getStage()` becomes `getGameStage()` and `getUiStage()` becomes `getMenuStage()`. That makes bug B1 visible in the code
    instead of hiding it.
  - Keep `getStages()` (the list of the three stages) while `StageFeature` uses it (K4).
- **Fix the construction order** so no setter is needed:
  1. Load the tiles: the loader needs only resources.
  2. Create `BoardService`. Change its constructor to take `List<Tile>` instead of `Board`, because it only uses `board.getTiles()`.
  3. Create `GameContext`.
  4. Create `Board(context, tiles)`.
  5. Call `prepare(context)` on each tile.
- Replace every use of `GamePrepareConfiguration` and of each `configuration/api/*` interface with `GameContext`. Constructors pull out
  the services they need into fields, as most already do. No getter is lost: they all move into `GameContext`.
- Delete the `configuration/` package (14 files, ~310 lines).
- Acceptance: the step gate passes, and `grep -r "configuration" core/src` returns nothing.

### Phase 2: Event system

**2.1 Class‑keyed event queue** (Risk M) **[K17]**
- In `GameEventService`:
  - Replace `Map<GameEventType, List<GameEvent>>` with `Map<Class<?>, List<GameEvent>>`, keyed by `event.getClass()`.
  - API: `post(GameEvent)`, `<T extends GameEvent> T poll(Class<T>)` (returns `null` if absent), `boolean has(Class<?>)`, and
    `<T extends GameEvent> T peek(Class<T>)`, which replaces today's unused `getEvent(..)` (returns without consuming).
  - Keep `updateEventsLivesAndClean()` unchanged.
- `GameEvent`:
  - Drop the type and the generic payload. Each event declares its own `final` field and a named getter:
    `TileTouchedEvent.getTile()`, `CollectEvent.getCollectible()`, `MenuOpenEvent.getActor()`, `LookAtMeEvent.getActor()`,
    `PlayerCreatedEvent.getPlayer()`, `AddToStageEvent.getDefinition()`, `PlayerMoveEvent.getPlayer()`, `MenuCloseEvent.getPointer()` (K17).
  - `PlayerMoveEvent` keeps `maxAge = 1`; hard‑code it in its constructor and drop the parameter.
  - Keep the `IMMORTAL` concept as a private detail of `GameEvent`. A protected constructor `GameEvent(int maxAge)` plus a no‑arg constructor meaning immortal is enough.
- Delete `GameEventType` (its information is the event class itself).
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
  button. Keep the aging logic unchanged and point it out in the commit message for review.

**▶ CP1**: covers Phases 1–2.

### Phase 3: Consistency

Make each kind of class look the same (§3.5, principles in §4). **Nothing is removed in this phase** except what K1–K3/K18 approve.

**3.1 Actors** (Risk M) — C1–C6
- Base constructor `GameBoardActor(GameContext context, String avatarName, String description)`:
  - it creates the avatar through the resources (one way for all actors, C1) and the description interaction;
  - `getDescription()` stays as a plain getter returning the stored text, so it is no longer abstract and no longer called from the
    constructor (C2, A9).
- Every actor follows the same order: `super(..)`, own fields, `addPlugin(..)` calls, listeners, touchability. Touchability is stated
  explicitly in each actor (`Dog` gets `setTouchable(enabled)`, which is the default, so nothing changes) (C5).
- Status (C4): `GameActor` initialises the field directly (`private Status status = IDLE`) instead of calling the overridable setter.
  `Player` drops its second `setStatus(IDLE)`. **Check:** `Player.setStatus(IDLE)` today records `lastIdleChange`; initialise
  `lastIdleChange` in `Player`'s constructor so `getIdleLength()` behaves the same.
- Inventories (C3): `Collecting` and `Collectible` both extend a small `InventoryOwner { Inventory getInventory(); }`. Inventory sizes become
  named constants (`INVENTORY_SIZE = 3` in `Player`, `2` in `Chest`).
- Listeners (C5): `GameBoardActorListener` and `PlayerClickListener` move next to each other (one file `ActorListeners` with two static
  nested classes, or both in `actor/`, whichever reads better) and are attached in the same place in the constructor.
- Keep every method listed in §3.3.

**3.2 Plugins** (Risk L) — C6–C9
- One constructor shape: `(owner actor, GameContext context, params…)`; all fields `private final`.
  `CenterActorByCameraPlugin(actor, context, offset)` takes the camera from the context.
- One body shape: poll or check the event first, then act. `RemoveOnPlayerMovePlugin` keeps its null check only if the owner can be null
  (it can't today: drop it only if that is certain, otherwise keep).
- Replace the anonymous one‑method `Action` classes with `Actions.run(..)` and `Actions.removeActor()` (same order, same frame).
- `BoardMovementPlugin`: replace `removePlayerActions` with `player.clearActions()`.
- The darkness plugin **[D2]**: with (b), `Player` adds it only when `Debug.DARKNESS_EXCLAMATION` is true (false by default, so behaviour is unchanged).

**3.3 Features** (Risk L) — C7, C10
- One constructor shape: `(GameContext context, params…)`. `FPSFeature(context, offset)`; `CameraTrackingFeature(context, actor)`.
- Features that show something add their actor in their constructor (both already do; make them use the context the same way).
- `GameScreen` registers all features in one method, `createFeatures(context)`, in today's order (camera, menu, stage, FPS).
  The camera feature is still created only when `PlayerCreatedEvent` arrives.
- Make the instance "constants" `private static final` (`MenuFeature`).

**3.4 Debug tools in one place** (Risk L) **[D6]**
- Create `debug/Debug.java`: an enum of tools that can be switched on one by one, plus a set of actor classes to outline (scene2d
  `actor.setDebug(true)` outlines one actor, so e.g. only dogs can be outlined). Defaults keep the game exactly as today:
  - `CAMERA_TRACKING_BOX = false` → `CameraTrackingFeature` calls `cameraActor.setDebug(..)` when true;
  - ~~`BRIGHT_AMBIENT_LIGHT`~~ dropped: the commented‑out ambient line has the same value as the active one (see §8);
  - `FPS_LABEL = true` → the FPS feature is registered when true;
  - `TABLE_DEBUG_LINES = true` → `CollectWindow` calls `backgroundTable.debug()` when true (today's behaviour, B4; Phase 7 sets it to false);
  - `DARKNESS_EXCLAMATION = false` (D2).
- The commented‑out debug lines are replaced by these switches.

**3.5 Shared code in one place** (Risk L)
- `LabelFactory`: one method with an optional background; the other delegates.
- `CollectWindow`: `getCollectibleTable`/`getInventoryTable` become one `createItemsTable(Inventory, CollectItemType)`.
- `TileChest`/`TileDog`/`TilePlayer`: one generic spawner, e.g. `TileActorObject(Function<GameContext, GameBoardActor>)`, or a shared base
  class with one abstract `createActor(context)`. Keep the three names if the base class reads better.
- `TmxTiledMapLoader`: write the hex width and cell‑position formulas once each, as private methods.
- `Notification.getCenter()` reuses the same calculation as `GameActor.getCenter()` (a static helper, or `Notification` extends `GameActor` if
  that changes nothing else).

**3.6 Small fixes and approved clean‑ups** (Risk L) **[K1, K2, K3]**
- `TileLight` passes `TileObjectType.LIGHT` instead of `CHEST` (the field is never read, so nothing changes).
- `final` on fields that are never reassigned; consistent field names (`boardGameActor` vs `gameBoardActor`).
- K1: delete the stray `RayHandler` in `Main`. K2: delete commented‑out code (debug lines were already turned into switches in 3.4).
  K3: `System.out.println` becomes `Gdx.app.log(<class name>, ..)`. K18 waits for 7.5.

**▶ CP2**

### Phase 4: Indirection layers (only what §5.2 approves)

Each step has a **keep** version (the default) and a **remove** version.

**4.1 Descriptions** (Risk M) **[K4]**
- Both versions: `Description` gets `removeActor()` appended to its action sequence, which fixes the B5 leak with no visible change, and
  `DESCRIPTION_MAX_WIDTH` becomes `private static final`.
- Remove version: `DescriptionInteraction.interact()` calls `notificationStage.addActor(new Description(..))` directly. Today the event is
  consumed later in the same frame by `StageFeature`, so adding immediately is equivalent. Delete the K4 classes.

**4.2 Collect flow** (Risk M) **[K5]**
- Both versions: `CollectWindow` takes `(GameContext, Inventory playerInventory, Inventory collectibleInventory)`, and its constants become
  `private static final`. Keep **exactly the stage used today** (`menuStage`; B1 is fixed in Phase 7) and keep the table debug switch.
- Remove version: `CollectPlugin` builds the window and the button itself; delete the K5 classes.

**4.3 Notifications and labels** (Risk M) **[K6]**
- Both versions: the **font is loaded once** in `ResourcesService` through `AssetManager` (`BitmapFont.class`, `"font.fnt"`) and disposed
  with it (fixes the B5 font leak). Labels keep the same `LabelStyle`: white default, `Color.FIREBRICK` for warnings.
- Remove version: `Notification.warning(resources, text, center)` builds the label with `parallel(moveBy(0, 20, 2f), fadeOut(2f))`;
  `LabelFactory` becomes `ResourcesService.createLabel(..)`; delete the K6 classes. Update `BoardClickListener`, `MenuButton`, `FPSFeature`
  and `Description`.

**4.4 Lights** (Risk L) **[K7]**
- Both versions: replace the inner `TileLightAttributes` class with plain `private final` fields on `TileLight`.
- Remove version: `TorchLight` absorbs `GameLight`; `BlinkingAction` uses `INTENSITY = 5`, `DELAY = 0.2f`; `TileLight` validates with
  `LightType.valueOf(..)` and creates `new TorchLight(rayHandler)`.

**4.5 Thumbnails** (Risk L) **[K8]** — only the remove version: `MenuButton` and `CollectItem` use `"collectButton"` directly.

**4.6 One rule for listeners** (Risk L) — C9
- A listener used once and shorter than ~5 lines becomes an anonymous class at its use site (`MenuButtonListener`,
  `CollectWindowCancelListener`, `CollectWindowOkListener`); `StopClickThroughClickListener` becomes a private factory method in
  `CollectWindow` (used twice). Larger listeners stay named classes. The logic of `CollectWindowItemListener` moves into
  `CollectWindow.onItemClicked(CollectItem)`.
- The actor listeners were already grouped in 3.1.

**▶ CP3**

### Phase 5: Simplify the domain model

**5.1 Actors** (Risk M) **[K9, K10, K11]**
- `GameActor`/`GameBoardActor`: replace the generated `equals`/`hashCode` with identity (delete both methods; equivalent, because ids are unique).
  K9 decides whether `id` and `GameActorType` stay.
- `Inventory`: keep the public API (both constructor paths, `getCapacity`); tidy the internals only. `InventoryTest` stays green.
- Keep `GameActor.setPosition(Vector2)`/`moveBy(Vector2)`. **Careful:** `GameActor.setPosition(Vector2)` must keep calling
  `super.setPosition(..)`. Otherwise `CameraActor.setPosition(float, float)` starts running (it doesn't today; see §8).
- `Player.draw`: the `else` branch is dead (`SpriteBatch.getShader()` is never `null`). Rewrite it as "draw without the custom shader".
- K10 and K11 remove versions as described in §5.2.

**5.2 Resources** (Risk M) **[K16]**
- `getTexture(name)`: atlas first, then preloaded textures. Use `assetManager.isLoaded(name)` instead of `catch (Throwable)`.
- Actors already load through the resources (3.1). `CollectItem` creates its drawables through `new TextureRegionDrawable(getTexture(..))`.
  `getAssetManager()` stays public (a tool), but game code doesn't use it.
- **Keep the preload list and the Linear filter settings unchanged.**

**5.3 Tiles** (Risk M) **[K12, K13]**
- Accessibility: `FloorTile` is blocked if any of its objects blocks. Compute it in `addTileObject`, which only happens at load time, so it is equivalent.
- **The loader sets the background region directly.** It uses the tile's `textureName` property if present, otherwise the class default.
- Identity `equals`/`hashCode` (delete both). `Tile.id`/`getId` stay.
- `getPolygonVertices()` returns `polygon.toArray(Vector2.class)`.
- **Keep B3 as it is.** Mark the throwing `stageToLocalCoordinates` with `// TODO bug B3`.
- K12 and K13 remove versions as described in §5.2.

**5.4 Map loader** (Risk M) **[K14]**
- Inside `TmxTiledMapLoader`: **use `GridPoint2`** (libGDX, value equality) for cell coordinates instead of `Tuple` keys, and keep the maps
  **local** to `load()`, not instance fields. Add `neighbourCells(col, row)` returning the 6 staggered‑odd neighbours.
- **Keep the nearest‑cell search for objects exactly as it is**, including that it scans empty cells and that a later object on the same cell
  overwrites an earlier one (`objectsMap.put`). Replace the linear `addTileObjects` scan with `objects.get(cell)`, which is equivalent
  because there is at most one object per cell.
- Keep `tiledMap.dispose()`. `TileObjectFactory` keeps its `assertMandatoryProperties` behaviour (same messages).
- `BoardLayoutTest` keeps the golden file **unchanged**.
- K14 remove version: one `TmxBoardLoader.load(mapFile)` returning `List<Tile>`.

**5.5 Board service and movement** (Risk M) **[D4, K15]**
- **[D4a]** Path finding as BFS from the start over accessible tiles, never expanding from an inaccessible tile. Otherwise **[D4b]** keep
  Dijkstra but simplify it. `PathFindingTest` must pass either way.
- `BoardMovementPlugin`: delete the `path.exists()` check inside the per‑node action (always true inside the loop).
- Leave `GameBoardActor.setTile` as it is, with its order: unregister the old tile, register the new one (which triggers
  collisions), then set the position. Keep the distant‑collectibles hook.

**5.6 Small cleanups** (Risk L)
- `PhraseService.getRandomPhrase` picks with `MathUtils.random(..)` instead of shuffling the backing list in place (fixes B6). `getPhrase` stays.
- `GameScreen`:
  - extend `ScreenAdapter`, which removes the empty methods;
  - `dispose()` disposes all three stages, the shader (through `GameStage.dispose()`) and the `RayHandler`;
  - `Main.dispose()` calls `getScreen().dispose()` before disposing the world and resources (fixes B5).
- `Board`: add the board at index 0 once (`gameStage.getRoot().addActorAt(0, board)`) instead of `toBack()` in every `draw()`, and convert
  the click point once, not per tile.
- `MenuStage` (A8):
  - make the selection explicit with `interactWithButtonUnderPointer()`; `MenuFeature` calls it, then `clear()`;
  - delete the `clear()` override;
  - replace the cast with an `instanceof MenuButton` check on the hit actor's parent;
  - keep the `addActor` type check, because B1 depends on it until Phase 7.

**▶ CP4**

### Phase 6: Package structure (pure moves)

**6.1 Move to the target layout in §4** (Risk L)
- Use `git mv` and fix package and import statements only. **No code changes** in this step.
- Check that `html/src/com/mantkowicz/light/game/client/HtmlLauncher.java` and `desktop/.../DesktopLauncher.java` still import
  `com.mantkowicz.light.game.Main`, which doesn't move.
- `core/src/LightGame.gwt.xml` (`<source path="com/mantkowicz/light" />`) doesn't need to change.
- Acceptance: the step gate passes.

**▶ CP5**: final report with before/after file and line counts.

### Phase 7: Bug fixes (behaviour changes, each one only after approval) **[D5]**

**7.1 B1: Collect flow crashes.** Add a fourth, plain `Stage`, `uiStage`, for `ActionButton` and `CollectWindow`:
- it shares the viewport;
- it is drawn after `menuStage`;
- in the input multiplexer it comes **before** `menuStage`;
- it lives in `GameContext`.

`MenuStage` stays exclusive to menu buttons, so `menuStage.clear()` no longer affects the collect UI. Re‑record the S15 golden with approval and
extend it to the full flow:
1. Walk onto the chest: a COLLECT button appears.
2. Click it: the window opens.
3. Select the lighter, move it to the player's inventory, and press OK; the inventories update.
4. CANCEL closes the window without changes.
5. Walking away fades out the button.

**7.2 B4: Debug lines.** Set `Debug.TABLE_DEBUG_LINES = false`, together with 7.1.

**7.3 B2: Furniture not drawn.** `TileSprite` keeps its region and offset, and `Tile.draw` draws its sprites after the background.
- The position is relative to the tile: `x + width/2 - w/2 + offset.x`, `y + height/2 - h/2 + offset.y` (today's magic `74`/`64` are
  `width/2`/`height/2` of the 148×128 tiles; check, and note it if not).

**7.4 B3: Walls.** In `WallTile`, create the body at the origin (`BodyDef` position `(0,0)`) with a `ChainShape` loop over the absolute polygon
vertices, and delete the throwing method. Check this by temporarily putting a wall tile into a test map: torch light must be
blocked by it.

**7.5 Unused assets (K18, approved for the end).** Remove `android/assets/ui/`, `map2_outer.tmx`, `tiles.tsx`, `player_detective.png` and
`lighter.png`, and `"lighter.png"` from the preload list in `ResourcesService`. For each removed file, `grep -rF <name>` over `core/src`,
`android/assets` and `html` must return nothing.

**▶ CP6**

### Optional

- **O1.** Merge `Plugin` and `Feature` into one interface (A10, K19). Only if the human finds the two names confusing.
- **O2.** Give the UI its own `ScreenViewport` camera. `ActionButton` and `CollectWindow` would then not need `CenterActorByCameraPlugin`. This is a visible
  behaviour change, because the UI would stop scaling with the world, so it needs a separate decision.

---

## 8. Found during execution

(Executing agents append here: date, step, finding, and what was done or deferred.)

### 2026‑11‑06, Phase 0 (regression tests)

**Done:**
- `e2e` module with the headless game, `GameDriver`, 18 gameplay tests (S1–S16, grouped by topic in 7 classes) and 7 unit tests
  (board layout, path finding, inventory). 16 golden files were recorded and reviewed by hand.
- B1 is confirmed and pinned by `CollectTest`, whose golden records the crash.

**Deviations from the testing design:**
- S5 ("unreachable") clicks a cell without a tile, because every accessible tile on the map is reachable.
- S7 walks back to (10,23), because (7,23) is off‑screen after walking east, and the driver refuses off‑screen clicks.
- The chest at (10,24) blocks the only corridor south, so no walking scenario goes south (it would hit B1).
- **Recording pitfall that was caught:** `MenuStage` polls `Gdx.input.getX()/getY()`, so the driver must move the scripted pointer
  on every press or drag. Without that, descriptions never appeared and the goldens were wrong. They were fixed before committing.

**Coverage** (`./gradlew e2e:test e2e:jacocoTestReport`): 82.5 % of `core` lines. Every uncovered line is one of:
- dead code from §3.3;
- reachable only through B1 or B3;
- an error path for invalid data (factory `default` throws, missing‑texture fallbacks);
- lifecycle code: `pause/resume/hide` are empty, and `dispose` is never called (B5).

**New findings** (step numbers updated to the revised plan; all of them are now covered in §3.3 or the steps):
- `GameEventService.getEvent(..)` is never called. Kept as `peek(..)` in 2.1.
- **"Distant collectibles" are never used.** No collectible returns `isDistant() == true`, so the neighbour loop in
  `BoardService.checkCollisions` never fires. Keep the behaviour in 5.5, because it's a cheap hook, but don't add tests for it.
- **The `else` branch of `Player.draw` is dead.** `SpriteBatch.getShader()` never returns `null` (it returns the default shader).
  The override simply means "draw the player without the custom shader". Simplify it in 5.1, keeping that behaviour.
- **`CameraActor.setPosition(float, float)` is never called.** `GameActor.setPosition(Vector2)` calls `super.setPosition(..)`,
  which skips the override; the camera is moved in `act()` instead. **Careful in 5.1:** if `GameActor.setPosition(Vector2)` stops using `super`,
  the override starts running. Keep the current behaviour: the camera position is applied in `act()`.
- `GameBoardActor.remove()` is only reached through dead code (`Chest.beforeCollect/afterCollect`). Keep it, because it's correct and
  B1's fix may use it.
- The `Inventory` constructor path for a `null` list is unused. Kept (§3.3).
- `LabelFactory.createLabel(WARNING, background, text)` (a warning with a background) is unused. Kept unless K6 is approved for removal.

### 2026‑11‑06, plan revised after step 1.1

- The old step 1.1 ("remove unused members") was executed and then **reverted at the human's request**: many unused methods are
  tools or hooks from earlier experiments (camera debug box, zoom helper, light setters, item hooks). Nothing was pushed.
- The plan was rewritten: rule 3 (no removal without per‑item approval), §3.3 now lists what is kept on purpose, §3.5 adds
  consistency findings C1–C11, §5.2 adds the keep‑or‑remove list K1–K19, and the phases were reordered (wiring and events first,
  then consistency) so constructors are rewritten once.

### 2026‑11‑06, Phase 1 (step 1.1)

- D1 taken as (a), the recommendation, when the human said to start refactoring.
- `Board.load(..)` became `Board.loadTiles(resources, mapName)`, so tiles exist before `BoardService` and `GameContext`; `BoardService` takes `List<Tile>`.
  `GameDriver` changed mechanically (one line).
- **Acceptance grep deviation:** `grep -r configuration core/src` still finds one line, the commented‑out code in `Tile.prepare`
  (`// ClickListener listener = new BoardClickListener(this, configuration);`). It is left untouched until K2 is decided.
- **Exception to "don't keep the context":** `PlayerCollectResolver` stores the `GameContext`, because it builds the window and button later,
  when a collect happens. Its shape is decided in 4.2 (K5).

### 2026‑11‑06, Phase 3

- 3.1: the actor shape is `super(type, context, DESCRIPTION)` plus `createAvatar(context, AVATAR_NAME)`, not the planned
  `super(context, avatarName, description)`: `GameActorType` stays until K9, and `Lighter` has no avatar. `createAvatar(Texture)` is kept.
  Actor listeners were renamed after what they do: `OpenMenuOnHoldListener` (was `GameBoardActorListener`) and `LookAtMeOnClickListener`
  (was `PlayerClickListener`, which works for any board actor).
- 3.4: **no "bright ambient light" tool.** The commented‑out line in `GameScreen.prepareLights` sets ambient alpha `1f`; the active line sets
  `2 * 0.5f`, which is also `1f`. It is removed with the other commented‑out code (K2). `Debug` holds four tools (camera box, FPS label,
  collect window lines, darkness exclamation) plus per‑class and per‑actor outlines. A throwaway test with every tool on and `Dog`
  outlined ran a walk and the menu without a crash.


### 2026‑11‑06, Phase 4

- Every K item (K4–K8) was kept, so only the "both versions" parts ran; 4.5 had nothing to do.
- 4.1: `DescriptionInteraction` takes `(actor, context, description)` like the plugins. The fade sequence uses `Actions.removeActor()`
  qualified, because `Group.removeActor(Actor)` hides the static import inside an actor.
- 4.2: **deviation:** `CollectWindow` keeps `(context, Collecting, Collectible)` instead of two `Inventory` parameters, so the
  `beforeCollect`/`afterCollect` hooks (never called today) stay reachable from the window. Only the constants became `static`.
- 4.3: the font is `ResourcesService.getFont()`; `LabelFactory.createLabel` and `NotificationBuilder` take the `BitmapFont` as their first
  argument. `DescriptionDefinition` carries the font next to the background. Filters and metrics are the same as before (the
  `BitmapFontLoader` defaults match `new BitmapFont(file)`).
- 4.6: `MenuButtonListener` became an anonymous class in `MenuButton` (with `HOVER_SCALE`/`HOVER_DURATION` constants);
  `CollectWindowOkListener`, `CollectWindowCancelListener` and `CollectWindowItemListener` became anonymous classes calling
  `CollectWindow` methods (`onItemClicked` holds the swap logic); `StopClickThroughClickListener` became `stopClickThrough()`.
  `OpenCollectWindowButtonListener` stays a named class (K5). The public `setChosenItem`/`getChosenItem`/`isAnyItemChosen` stay.

### 2026‑11‑06, Phase 5

- K9–K16 kept. D4 answered: **A\*, using gdx-ai** (`IndexedAStarPathFinder`, 1.8.2, the latest release). `BoardGraph` adapts the board
  (cost 1 per step between accessible neighbours; heuristic = straight-line distance divided by the longest neighbour step, so it never
  overestimates). Connections are built once, because accessibility is fixed at load time since 5.3. HTML5 needs the gdx-ai sources jar
  and `<inherits name="com.badlogic.gdx.ai"/>`. Walk and reroute goldens (which record exact routes) are unchanged, and a throwaway test
  matched A* path lengths against BFS for all 1,936 cell pairs (the map has 44 tiles).
- 5.2: new `ResourcesService.getDrawable(name)`, used by `CollectWindow` and `CollectItem`.
- 5.3: the tile texture property is `TileAttribute.TEXTURE_NAME`; the loader applies it before positioning the tile. Only floor tiles
  carry it in the tileset, so walls are unaffected. `Array.toArray(Class)` is deprecated in libGDX 1.14; `toArray(Vector2[]::new)` is used.
- 5.4: tiles now come out of the loader in cell order instead of `HashMap` order. Goldens are unchanged.
- 5.6: `PhraseService` uses its own `RandomXS128`. Using `MathUtils.random` shifted the shared seeded sequence and changed the torch
  flicker in the no-path golden. `MenuStage` has a `TODO bug B1` at its `addActor` check. `Stage.dispose()` calls `clear()`, which used
  to trigger the menu interaction; with the `clear()` override gone, disposing the menu stage is safe.

### 2026‑11‑06, Phase 7, B1 (collect flow)

- Fixed as planned in 7.1, as its own PR: a fourth stage, `UiStage` (an `AbstractStage` with `StageType.UI_STAGE`, so it fits `getStages()`
  and `StageFeature`), shares the viewport, is drawn last and gets input first. `PlayerCollectResolver` and `CollectWindow` use it.
- `GameDriver` gained `clickButton(text)`, `clickChestSlot(i)`, `clickPlayerSlot(i)`, `inventories()`, and snapshots list labels on the
  UI stage (empty in every other scenario, so no other golden changed). `collect` was re-recorded; `collect-cancel` is new.
- B4 (debug lines over the window) is left for its own PR.
- **B7, found and fixed in the same PR:** CANCEL removed the window but kept the swapped slots, and the COLLECT button reopened the
  **same** window, which showed the lighter in the player's slot although the inventories didn't change (OK would then save it).
  `OpenCollectWindowButtonListener` now takes a `Supplier<CollectWindow>` and opens a fresh window on every click. Both collect tests
  reopen the window and record its slots (`reopened window: ...`); before the fix the cancel test showed the bug.

### 2026‑11‑06, Phase 7, B4 (debug lines)

- Fixed as its own PR: `Debug.Tool.COLLECT_WINDOW_LINES` is off by default (`Debug.switchOn(COLLECT_WINDOW_LINES)` brings it back).
  `GameDriver.uiDebugLinesShown()` is recorded in the collect golden; recorded against the old default it said `true`.

### 2026‑11‑06, Phase 7, B2 (furniture)

- Fixed as its own PR: `TileObject.draw(tile, batch)` is a no-op hook; `TileSprite` loads its region in `prepare` and draws it centred
  on the tile plus its offset. `Board.draw` draws all backgrounds first, then `Tile.drawObjects`, so a neighbour's background never covers
  furniture. The old magic `74`/`64` were indeed half of the 148×128 tile size.
- New `BoardDrawingTest` (golden `board-drawing`) records every draw call of the board through a recording `SpriteBatch`: 44 backgrounds,
  then the 19 sprites.
