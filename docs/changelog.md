# Changelog

What each feature branch changed and what was found on the way, newest last. Add an entry per branch: date, branch,
what changed, and anything a later reader should know (decisions, trade-offs, what the tests cover).
The refactoring that came before is in [refactoring-plan.md](refactoring-plan.md) (§8 has its journal).

## 2026‑11‑07, square tiles (branch `square-tiles`)

- Hexes are replaced by a square grid of 128×128 tiles with **8 neighbours**. Every step costs 1 in path finding, a diagonal one too
  (review on PR #18), so the A* heuristic is the Chebyshev distance in steps. A diagonal step is only allowed when both tiles it passes
  between are accessible (no cutting the corners of furniture). Diagonal steps take √2 longer to walk, so the walking speed is the same
  in every direction.
- Code: `TmxTiledMapLoader` (cell position `x·w, y·h`, `neighbourCells` returns 8), `Tile` + `TilePoint` (4 corners), `BorderLightPosition`
  (TOP/RIGHT/BOTTOM/LEFT), `BoardGraph` (weighted connections, corner rule), `BoardMovementPlugin` (step duration by distance),
  `GameDriver` geometry.
- Assets: square versions of `floor-tile`, `wall-tile`, `tileFloor`, `woodenFloorEven`, `woodenFloorOdd` (PNG files, their atlas regions
  and the tileset), generated from the hex art: flat tiles keep the hex edge shading; the wooden floors extend the planks along their
  direction; the tiled floor maps the hex's triangles onto the square's four. Grass and road tiles are still hexes (not used by the map).
- `map.tmx` is an orthogonal map. Copying the hex cells one to one left many tiles touching only at corners, so the map was laid out again:
  the same rooms (living room, bedroom, kitchen, bathroom) as solid rectangles with doors, the corridor with the player (10,22), dog (9,22),
  chest (10,24) and torch (10,25) on their old cells, and the furniture in its rooms (a few pieces moved so nothing is walled off). All 45
  walkable tiles are reachable from the start. All goldens were re-recorded.

## 2026‑11‑07, smooth movement (branch `smooth-movement`, on top of `square-tiles`)

- **Constant speed:** `BoardMovementPlugin` walks the whole path with one `FollowPathAction` (a linear `TemporalAction` along a polyline),
  `secondsPerTile` seconds per 128 px (`Player.SECONDS_PER_TILE = 0.4`, slowed down from 0.2 after playing), instead of a `sineOut` move
  per tile. The player is `MOVEMENT` for the whole walk and `IDLE` only at the end.
- **Path smoothing:** `BoardService.getPath` still returns the A* tile path. The plugin keeps only its corners: from each point it jumps to
  the furthest later tile centre reachable in a straight line (`BoardService.isWalkableLine`: sampled every 4 px with a quarter-tile
  clearance square, so it never crosses or clips a missing or blocked tile).
- **Tile bookkeeping:** `GameBoardActor.setTile` = new `enterTile` (unregister/register, collisions) + snap to the centre; walking uses
  `enterTile` when the player's centre crosses into a tile. A `PlayerMoveEvent` ("left the tile") is posted that frame and the new tile is
  entered the next frame, so a COLLECT button shown by the new tile isn't removed by the same event. Walking past the chest shows the
  button briefly and fades it, as before.
- **Re-routing:** a touch is taken at any time (not only between tiles): the walk restarts from where the player is, through the current
  tile's centre if the straight line to the next tile isn't walkable.
- Tests: new `SmoothMovementTest` (constant 5.33 px/frame, no stop, a diagonal stretch; no leftover COLLECT button after walking past the
  chest). Movement goldens re-recorded (walk arrives 15 frames sooner: no idle frame per tile).
- **Hops (the player will be a rabbit):** new `HopPlugin` lifts only the actor's picture (`GameActor.getAvatar()`, the `Image` child) in a
  sine arc while the status is `MOVEMENT`: `Player.HOP_HEIGHT = 10` px, `HOP_DURATION = SECONDS_PER_TILE / 2` (two hops per tile). The
  walk ends, the rabbit comes down from where it is (a rising hop turns into a falling one), so it never hops once more after stopping.
  Positions, tiles, camera and goldens are unaffected; `HopTest` checks height, landings, no hop after stopping and rest.

## 2026‑11‑08, Sprout Lands assets (branch `sprout-lands-assets`)

- **Tooling:** `tools/assets/build_assets.py` (Python + Pillow) turns the pack into Tiled tilesets (`assets-src/tilesets`,
  terrains detected from transparency or borrowed from a clean sheet with the same layout, animated water) and game
  atlases: `android/assets/maps/tiles.atlas` (only the tiles the levels use) and `android/assets/sprites/sprites.atlas`
  (only the sheets in the `Sprite` enum, plus `animations.json`). Previews of the whole pack in `build/assets-preview/`. See
  `tools/assets/README.md`.
- **License:** the pack's own sheets are git-ignored (`assets-src/tilesets/images/`); packed atlases are committed with
  `CREDITS.md`.
- **Game:** `TmxTiledMapLoader` loads packed maps (any number of tile layers drawn per tile, `collision` and `walls`
  layers, 16 px tiles scaled to 128 by `WorldScale`, map property `ambientLight`) via `AtlasTmxMapLoader` and the asset
  manager; map.tmx still loads the old way. New `BlockedTile` (not walkable, doesn't block light). `Main` starts
  `maps/level1.tmx`; the tests keep map.tmx (`HeadlessGame.TEST_MAP`).
- **Rabbit:** the player is the rabbit sprite (one tile big, the picture sticks out), animated by `AnimationPlugin`
  (`walk_` + direction from the actual movement, no flicker on diagonals; back to `idle_down` when it stops). `HopPlugin` hops relative to
  `GameActor.getAvatarRestY()`. Goldens re-recorded: only the player's bottom-left corner (its size is now 128x128)
  and the description label above it moved. New `LevelTest`.
- **Typed animations:** `Sprite` (hand-written: the sheets the game uses, the only ones packed), `Action` (generated from
  the row names in `pack.json`) and `Direction`; `ResourcesService.getAnimation(Sprite, Action[, Direction])`, so no
  animation names are typed in Java. Missing sheets or frames fail at start. `AssetsUpToDateTest` (needs neither the
  pack nor Python) fails when a level, `pack.json` or the `Sprite` enum changed without a run of the tool; generated
  files record the SHA-1 of their sources. The tool is run with Python only, it isn't part of the Gradle build.
- **Review fixes (PR #21):** map tiles keep Tiled's mirroring/rotation, layer opacity and offset (`TileGraphic`, drawn
  like libGDX's renderer); a packed map is recognised by its map-level `atlas` property (parsed, not searched in the
  text); the atlas loader is registered for that file only; 16 px tiles are checked; a non-numeric `ambientLight` is
  explained; sprite frames are reachable through `getTexture`. Tool: mailbox grid fixed, the door sheet is a tileset
  only (its frames overlap, no grid fits), packer edge case at the page border, embedded tilesets and infinite maps
  rejected with a hint, UTF-8 files. New `PackedMapTest` (on `LevelVariant` copies of level1) and more `LevelTest`.

## 2026‑11‑09, UI viewport and camera zoom (branch `ui-viewport`)

- **Camera zoom:** `Main.CAMERA_ZOOM` (1 = as designed, below 1 = closer, above 1 = see more) is set once on the world
  camera in `GameScreen.show()`. Clicks and lights already take it into account. The tests don't depend on it:
  `HeadlessGame` passes `TEST_CAMERA_ZOOM = 1` through the new `Main(mapName, cameraZoom)`.
  The HTML and Android launchers have their own `CAMERA_ZOOM` constant (`Main.CAMERA_ZOOM` for now), passed with
  `new Main(cameraZoom)`; desktop uses `Main.CAMERA_ZOOM`.
- **UI split:** the world (game, menu and notification stages: things that point at the board) keeps one
  `ExtendViewport`; the UI stage got its own, which never moves, so buttons and windows keep their place and size at any
  zoom. Both are resized in `GameScreen.resize`. `CenterActorByCameraPlugin` now centres an actor on its own stage's
  camera (no more following the world camera by hand); the debug FPS label moved to the UI stage.
- Goldens re-recorded: only UI positions changed (now in UI-stage coordinates; each button is at the same place
  relative to its camera, so the same place on screen) and the FPS label's stage. New `CameraZoomTest`: at zoom 0.5
  cells are twice as big and still clickable while the COLLECT button doesn't move or grow; at zoom 2 cells are half as
  big.

## 2026‑11‑09, a cow and the Sprout Lands chest (branch `cow-and-chest`)

- **Sprite actors:** a board actor drawn from one Sprout Lands sheet is a small class extending `SpriteActor` (picture
  one tile big, blocks its tile, `playAnimation`/`playBackwards(Action)`), with behaviours added as plugins. It is
  registered with one `TileObjectType` line, e.g. `COW(Cow::new)`, `CHEST(Chest::new)`; the generic `TileSpriteActor`
  creates it from the map (no per-type tile class: `TileCow`/`TileChest` are gone). Adding one = its class + that line +
  its `Sprite` line. No `Creature` layer in between: nothing shares more than `SpriteActor` gives yet.
- **Cow:** `Cow` (`Sprite.COW_BROWN`, plays its `idle` row), placed in Tiled with objectClass `COW`. The brown cow's
  first two rows are named `idle` and `walk` in `pack.json`; the rest stay `row_02`… for now. On level1 at (4,9).
- **Chest:** drawn from the Sprout Lands sheet everywhere (`Sprite.CHEST`; `chest.png` is no longer used). It blocks
  its tile and is used from a neighbouring one (`isDistant()`); clicking it walks to the free neighbour with the
  shortest walk (`BoardMovementPlugin.walkTarget`, `BoardService.isUsedFromNeighbour`, no "no path" notification).
  The lid opens when the collect window opens and shuts when it closes: `Collectible.beforeCollect()`/`afterCollect()`
  (unused until now) are called by `CollectWindow`. On level1 at (8,9).
- **`SpriteAnimationPlugin`:** plays one animation on an actor's picture (forwards, or backwards for the closing lid);
  used by the cow and the chest.
- **House map:** its chest stood in the corridor at (10,24), which a blocking chest would cut; it moved to the corner at
  (8,24), where blocking changes no route. Goldens re-recorded: the chest's cell, the player stopping next to it at
  (8,23), the camera following, the board layout, and a COLLECT button where a walk now ends next to the chest.
  `CollectTest`, `CameraZoomTest` and `SmoothMovementTest` point at the new spot. New `LevelTest` cases for the cow and
  the chest.

## 2026‑11‑09, tests on their own maps (branch `test-maps`)

- Done: the idea "Feature tests on their own small maps". Every e2e test now brings its own map, so editing a level
  never breaks a feature test. `TestMap.of("~~~~~", "~.P.~", ...)` draws one cell per character (`.` grass, `~` water,
  `x` blocked, `#` wall, space = no tile, `P`/`C`/`M`/`D`/`*` player, chest, cow, dog, torch, `a`…`z` marked cells
  found with `cell('a')`, other characters as objects with properties through `with`) and writes a packed map that
  draws the game's `tiles.atlas` (plain grass and animated water only).
- `LevelVariant` now changes copies of a `TestMap` instead of level1 (`LevelVariant.of(map)`), for what text can't
  show: extra layers, mirrored tiles, opacity, map properties.
- `LevelTest` is split: `AnimationTest` (rabbit, cow and chest animations, on test maps) and `ShippedMapsTest`, which
  checks every map in android/assets without cells or goldens: it loads, the rabbit starts on a free cell, every chest
  has a free neighbour the rabbit can reach (a copy of level1 with the chest out at sea fails it). The old hexagonal
  `map2_outer.tmx` passes too.
- All goldens re-recorded, checked by comparing them without the lines that depend on the map (cells, camera, dog,
  torch). Identical: collect, collect-cancel, walk, no-tile, camera-tracking, look-at-player, resize, start,
  fade-shader, menu-tap, menu-cancel; unchanged: stop, torch-flicker. `no-path` and the menu goldens keep their texts'
  offsets from the clicked cell, the dog and the player. `reroute` lost a COLLECT line (the house route passed the
  chest). `board-layout`, `board-drawing` and `path-lengths` describe the new maps.
- Snapshots leave out the dog line on maps without a dog; board descriptions also cover tiles drawn from an atlas.

## 2026‑11‑09, colours and dropdowns for map objects (branch `idea-map-placed-sprites`)

- **Colours:** each sprite actor's `TileObjectType` line lists the sprites it can be drawn with, the default first
  (`COW(Cow::new, Sprite.COW_BROWN, Sprite.COW_GREEN, …)`); the actor gets its sprite in the constructor. In Tiled an
  optional `sprite` property on the object picks another one (`COW_PINK`). All five cow colours are in `Sprite` now
  (the sprites atlas stays 1024×512). The cow's description names its colour.
- **Dropdowns in Tiled:** the tool reads `TileObjectType` (like it reads `Sprite`) and writes the enums `ObjectClass`
  and `Sprite` into `assets-src/lightgame.tiled-project`. It marks `objectClass`/`sprite` properties typed as plain
  text with their enum (only adds `propertytype`; `level1.tmx` got it), so Tiled shows dropdowns.
- **Mistakes are explained:** the tool stops on an unknown `objectClass` (with a "did you mean"), a `sprite` the object
  can't use, or an object without `objectClass`, naming the map and object. The game says the same with the map and
  cell (it used to fail on `TileObjectType.valueOf` without saying where), for maps that didn't go through the tool.
- Tests: `MapObjectsTest` (default and picked colour, the four mistakes). The tool's checks were tried on broken copies
  of `level1`.
- Keep `TileObjectType` lines in the form `NAME(Class::new, Sprite.A, Sprite.B)`: the tool reads them with a regex.

