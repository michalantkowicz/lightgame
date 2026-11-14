# Changelog

What each feature branch changed and what was found on the way, newest last. Add an entry per branch: date, branch,
what changed, and anything a later reader should know (decisions, trade-offs, what the tests cover).
The first refactoring came before this file; its plan (`docs/refactoring-plan.md`, with its journal) is in git history.

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

## 2026‑11‑10, COLLECT button stays next to the chest (branch `claude/collect-button-stays-near-chest`)

- **Bug:** standing next to the chest and stepping to another cell that is also next to it, the COLLECT button started fading out
  (any player move removed it) and a new button popped up on the new cell.
- **Fix:** the button now stays while the player is within reach of the collectible: on its cell, or for a distant one (the chest) on
  any of its 8 neighbours. It fades out only once the player's cell leaves that area. `RemoveOnPlayerMovePlugin` is replaced by
  `RemoveWhenUnavailablePlugin` (fades out when a condition stops holding), `ActionButton` takes that condition, and
  `PlayerCollectResolver` keeps one button per collectible, so entering another cell in reach doesn't add a second one.
- The button stays clickable while walking within reach, but the window opens only where the player stands still: COLLECT
  clicked during a walk works like clicking the chest (`TileTouchedEvent` on its cell), so `BoardMovementPlugin` re-routes to the
  cell in reach the player is on (or the nearest one) and the window opens on arrival (`CollectResolver.update`, run every frame by
  `CollectPlugin`). Before, clicking COLLECT while walking away opened the window with the player leaving the chest.
  `OpenCollectWindowButtonListener` is gone; `PlayerCollectResolver` opens the windows (still a fresh one per click).
- **Reach is the 4 side cells only.** A chest is used from up, down, left or right, not diagonally, because the characters
  only face those four directions (and later actions, like chopping a tree, need the same). `Tile.getSideNeighbours()` gives
  them; collecting (`BoardService.checkCollisions`), "walk next to the chest" (`BoardMovementPlugin.walkTarget`) and the
  button's reach use it. Walking and path finding still use all 8 neighbours.
- Going around a chest now passes a corner cell, out of reach: the button starts fading there, and fades back in (the same
  button) when the player reaches the next side cell before it's gone (`RemoveWhenUnavailablePlugin`).
- The fading button stays touchable: before, a click on it went through to the board under it and walked the player
  there. Now it catches the click, and the click does nothing while the player is out of reach (no window, the walk goes
  on). Test: `CollectTest.clickOnFadingButtonDoesNothing` (on a taller map, so there are cells under the button).
- Test: `CollectTest.walkingAroundTheChestKeepsOneButton` (was `buttonStaysWhileWalkingAroundTheChest`) checks every frame of the step that exactly one COLLECT button is shown at
  full alpha (now: from below the chest to its left side, exactly one button on every frame, back at full alpha on arrival),
  and that it's gone after walking away (fails on the old code at the first frame). `noButtonDiagonallyNextToTheChest`: no
  button on a corner cell, and clicking the chest from there walks to a side. `AnimationTest` and `ShippedMapsTest` check
  side cells for chests.
  `CollectTest.collectWhileWalkingAwayStopsNextToTheChest` clicks COLLECT just after starting to walk away: no window during the
  walk, then the player stands next to the chest with the window open. The goldens are unchanged.

## 2026‑11‑11, UI pack in the asset tool (branch `ui-pack`)

- `build_assets.py` reads a second pack, the Sprout Lands UI pack (found in `tools/assets/*UI Pack*` or given once with
  `--ui`, remembered in `.ui-pack-path`), described in `tools/assets/ui.json`: sheets cut as a whole, as a grid, or
  into separate shapes (`"pieces": "auto"`, flood fill + joining overlapping boxes, reading order), optional piece
  names, nine-patch borders (`split`, written into the atlas, so `atlas.createPatch` works) and row animations. All 200
  files of the pack are either described or ignored on purpose (0 warnings).
- Like sprites, only what the game lists is packed: `UiImage` (pieces) → `android/assets/ui-pack/ui.atlas`, `UiFont` → bitmap
  fonts in `android/assets/ui-pack/fonts/`. The TTF fonts are rendered with Pillow at their pixel size (smallest size with
  no grey edges), ASCII only (the fonts have nothing else); the generated glyphs match Pillow's own drawing of the TTF
  pixel for pixel. TTF isn't loaded directly because the web build can't read it without extra libraries.
- `ResourcesService`: `getUiImage`, `getUiNinePatch`, `getUiAnimation`, `getFont(UiFont)`; the UI atlas is kept apart
  from `getTexture`'s atlases so piece names can't clash. Listed pieces and fonts are checked at start.
- To start with, the enums list `DIALOG_BOX`, `CONTINUE_INDICATOR` (an animation) and `PIXEL_8X14`, so every path is
  exercised; no screen uses them yet.
- Preview page: a UI section with every piece by sheet (nine-patches also stretched, animations playing) and a sample
  of every font.
- Tests: `UiAssetsTest` (everything listed loads; a nine-patch, an animation, a font measure as expected),
  `AssetsUpToDateTest` checks `ui.json`'s SHA-1 and that the packed pieces and fonts are the enums' ones.
- License: `CREDITS.md` names both packs and where their parts are, with the terms (the same for both packs).
- Known limits: shapes that touch come out as one piece with `"pieces": "auto"` (e.g. three small hearts in
  `inventory`), so such sheets may need a grid later; the dialog boxes' splits keep the tail on the left in the
  stretching middle, so a tall box stretches the tail too.

## 2026‑11‑12, the UI drawn with the UI pack (branch `ui-pack`)

- All UI comes from the Sprout Lands UI pack, drawn at `ResourcesService.UI_SCALE` = 3 (one pack pixel is 3x3 screen
  pixels, the font's too), built by `ui/UiStyle`:
  - COLLECT, OK, CANCEL: the blank wide button (`play_button/blank`, pressed `blank_down`), a nine-patch, 220x81 at
    least, dark brown text (`UiStyle.TEXT`);
  - the collect window: the blank settings panel (`settings_menu/panel`, nine-patch) on the same dimmed screen; slots
    are the inventory's octagons: light (empty or holding), darker when pressed, dark when chosen, the square one
    where the chosen item can go;
  - the description bubble: `dialog_box` (nine-patch) with dark text, up to 420 px wide;
  - pictures (`ThumbnailType` now names a `UiImage`): the radial menu's describe button is the "?" icon button, the
    lighter is the sun emoji (the pack has no lighter);
  - all text (buttons, notifications, descriptions, FPS) uses the pack's 7x7 pixel font (`getFont()`), capitals only.
- `ui.json` names the pieces used (`names`) and gives the panel and button their nine-patch borders.
- Goldens re-recorded (approved look change): only texts' positions and colours changed (COLLECT/OK/CANCEL brown and
  moved with the new button sizes, menu "button" labels and "no path" moved with the new font, descriptions brown).
  Walking, collecting, inventories and menu behaviour are the same.
- Not used any more, kept for now: the old skin (`skin/`, still loaded), `font.fnt`, `table_background.png`,
  `item_background*.png`, and the unused old `android/assets/ui/` skin.

## 2026‑11‑12, HUD step 1: the bottom row (branch `hud-1-bottom-row`)

Step 1 of the HUD design ("face is the button", `docs/hud-design.md` on branch `ccr-dcc79c9a-n8m2ou`):

- `ui/hud/Hud` on the UI stage, drawn at 4x (`Hud.SCALE`, one step above `UI_SCALE`). Bottom centre: the player's face
  in a frame (`Portrait`, 184x184), two backpack slots on its left showing the
  player's first two inventory items, the stamina bar under the face. Top right: the settings gear (`HoldButton`), 48 px from
  the corner. No day picture for now (the sun said nothing); its place right of the face is the round button's.
- The face looks like the UI pack's Teemo emote preview: Teemo from the premium emote sheet standing on the bottom bar
  of the dark wooden backdrop (`backdrops_c1`, a nine-patch stretched to fit one 32x32 emote frame). It is mostly still
  and every 2.5 s plays a gesture in a fixed order: blink, blink, ear twitch (`TEEMO_STILL`, `TEEMO_BLINK`, `TEEMO_EAR`).
- The gear works only after a 3 s hold: a circle at its top right (the pack's stamina circle with black outline, 37
  frames following the hold: empty, filling, a green check), a little outside the button so a finger on it doesn't
  hide it, fills; letting go early empties it. The gear is 48 px from the screen's corner, room for the circle. It opens `SettingsPanel`: the settings panel on the dimmed screen with its title and a check mark to
  close. The settings themselves come in step 4.
- Stamina: `Stamina` (0..1) belongs to the player (`Player.getStamina()`): `use(amount)` lowers it (time passing, hard
  work), `fill()` fills it (sleeping), `set(value)`; it stays between 0 and 1. The HUD's `StaminaBar` shows it under
  the face as 14 segments from the pack (drawn 2x, 138 px: 3/4 of the face's width), with a tiny
  face from the pack on its left (`tiny_emotes`, drawn 2x like the segments): happy (green) with more than half left,
  neutral (yellow) at half or less, sad (red) at a quarter or less; the face and the bar together centred under the face (found only on its all-in-one sheet, cut with the tool's grid limits
  `columns`/`rowCount`): green for what is left, brown for what is used; any strength left shows at least one green
  segment. Nothing in the game lowers it yet.
- 40 px between the face and the pieces beside it, 12 px between the two slots; the slots are 96x104
  (80% of their first size). The slots and the action button line up with the bottom of the face's frame.
- Taps on the bottom row stay in the HUD (they don't walk the player to a cell behind it); taps beside it reach the map.
  The HUD follows the UI camera, so it keeps to the screen's edges after a resize.
- Right of the face there is always a round button (`RoundButton`: the pack's round brown button with a picture and
  no text; pressed, it sinks and the picture with it). With nothing to do it is the hint: a question mark (the pack's
  icon), and tapping it makes the face say "play around!" in a `SpeechBubble` over it for 2.5 s, then the bubble
  fades. The bubble is the pack's dialog box turned 90° (`dialog_box_down`: the tool's new `rotate` option in ui.json)
  so its pointer is at the bottom, cut in two: the box stretches around the text (a nine-patch from 9 parts, its edges
  taken from a plain row and column away from the pointer) and the small pointer is drawn on its own at the middle of
  the box's bottom. Later the hint will say where to go next (and show arrows). While the bubble shows, the face
  cheers (`TEEMO_CHEER`, the premium emote sheet's row 15).
- The face can react: `Portrait.play(animation, seconds)` plays a face animation over and over for that long instead of
  the idle, `Portrait.play(animation)` plays it once; then the face goes back to being still and to its gestures by
  itself. A new reaction replaces the one playing. `getReaction()` says what plays (null when idle).
- The face's idle follows the stamina bar's face: `Portrait.Idle` (an intro played once, then a still face for a
  pause and the next gesture, over and over; no pause: the gestures follow each other), set with `setIdle`. Awake
  (green): still, blinking, twitching an ear every 2.5 s. Tired (yellow): yawning (row 29) after every second of
  stillness. Asleep (red): falls asleep once (row 41), then sleeps with a bubble growing and shrinking (rows 42, 43).
  Reactions still play over any idle.
- The action button is no longer a COLLECT text button but a round button over the hint with a picture of what it
  acts on, no text: `Collectible.getActionIcon()`, for the chest the closed chest as it looks on the map (cropped to its
  visible 16x14 px, drawn 4x on a button drawn 6x, 108x120). It appears right of the face, its right edge as far from
  the screen's edge as the slots' left edge (64 px on an 800 px wide screen), its bottom on the face's bottom
  (`Hud.ACTION_RIGHT`, `Hud.ACTION_Y`); pressed, it sinks (the pack's pressed piece) and the chest with it.
  This differs from the design (step 2 puts the action on the face). Its name, COLLECT, is only for tests: `GameDriver`
  finds it by name in `clickButton`, `buttonAlphas` and `buttonOnScreen`, and goldens show it as
  `action button on ui stage: COLLECT (a picture)` instead of the COLLECT text. Goldens `collect` and `collect-cancel`
  re-recorded: only that line changed. New golden `hud-hint`.
- New pack pieces: the slot as a nine-patch, the apple (no longer drawn, kept in `UiImage`), the gear, the check mark, two selector dots (no longer drawn, kept in `UiImage`), the stamina circle, the round brown
  button and its pressed piece, the question mark icon, the dialog box turned 90°, the bar segments.
- The cow stands in the middle of its tile: it is drawn low in its 32x32 frame, so its sheet has `"anchor": [16, 21]`
  in pack.json (the frame's point drawn on the tile's middle; default the frame's middle), passed to the game in
  animations.json (`SpriteSheet.anchorX/Y`). Test: `AnimationTest.theCowStandsInTheMiddleOfItsTile`.
- Tests: `HudTest` (golden `hud`): the row's layout, the backpack after collecting the lighter, the gear's hold (2 s:
  67 % held, nothing opens; let go: empty; 3 s: open; check mark: closed), a tap on the face doesn't walk, layout after
  a resize, the action button's place (over the hint), the hint's bubble over the face (shown, fading, gone) and the face cheering meanwhile, reactions (for a while, once,
  replaced).


## 2026‑11‑13, the chest throws out a carrot (branch `chest-pops-carrot`)

The collect window is gone (it was too complicated): `CollectWindow`, `CollectItem`, `CollectItemType`,
`CollectWindowItemState` and the golden `collect-cancel` are deleted.

- The chest holds a `Carrot` (an `Item`, from the sprites pack's farm items: `Sprite.FARM_ITEMS`, row 2). Its button
  works as before (it appears on the chest's four sides, fades out when the player leaves, opens once the player stands
  still), but opening it lifts the lid, which stays open, pops 7 small stars (`Sparkles`, the UI pack's small star
  flying out, growing, shrinking and fading) and throws everything inside out. With nothing left, the button goes.
- `ItemThrower`: an item flies (`ItemOnGround.flyOnto`: an arc, 0.6 s) onto a random free tile next to where it comes
  from: walkable, nobody and nothing on it, no other item flying there (all 8 neighbours). With no free tile it stays
  where it is. The choice uses `MathUtils.random`, which the tests seed.
- `ItemOnGround`: an item lying on a tile, drawn 6x over the pack's item shadow, bobbing up and down; it doesn't block
  the tile and clicks go through it, so clicking it walks there. Stepping onto its tile picks it up into the backpack if
  a slot is free.
- The backpack is the player's inventory (2 slots, as the HUD shows: `Player.INVENTORY_SIZE`); the HUD's slots show
  each item's picture (`Item.getPicture()`, the same as on the ground, drawn 4x). Tapping a slot throws its item out
  from the player onto a free tile next to it (no stars), from where it can be picked up again.
- New: `Inventory.add`/`take`, `BoardService.getGameActors(tile)`, `Player.throwOut(slot)`, `Hud.getSlots()`.
  `Collectible.afterCollect()` and `Lighter` are no longer used, kept.
- Tests: `CollectTest` (S15, golden `collect`) rewritten: open the chest, the carrot lands next to it, walk onto it,
  throw it out from the backpack, pick it up again; it lands only on a free cell; with no free cell it stays in the
  chest; an empty slot throws nothing; the button tests as before. `AnimationTest`: the chest opens and stays open.
  `HudTest`: the backpack shows the carrot, then empties when it is thrown out. `GameDriver`: `itemsOnGround`,
  `itemCell`, `starsOnScreen`, `tapBackpackSlot`; scene snapshots list items on the ground.

## 2026‑11‑13, a full backpack makes room (branch `full-backpack-swaps`)

- Stepping onto an item with the backpack full: the item in its first slot jumps out onto a free tile next to the
  player (as when the slot is tapped) and the slot shakes up and down in the HUD (8 px, 0.24 s: the resolver posts a
  `BackpackItemEjectedEvent`, the HUD polls it). A second later (`MAKING_ROOM_SECONDS`), if the player still stands
  there, the new item goes into that slot. The pause makes it clear which item goes and which comes (with the same
  carrots everywhere, swapping at once looked like the player threw away the one stepped on). Only the first slot
  empties by itself, nothing moves between slots; the second is emptied by tapping it. With no free tile around
  nothing changes; the item stays where it lies. `CollectResolver.update` gets the frame's delta for the pause.
- `Inventory.isFull()`.
- Test: `CollectTest.aFullBackpackThrowsOutItsFirstItemToTakeTheNewOne` (a dead-end map: which carrot goes where and
  when, the first slot shakes and settles back); `GameDriver`: `putCarrotIntoBackpack`, `backpackItems`,
  `itemsOnGroundThemselves`, `backpackSlotShaking`.

## 2026‑11‑13, code format checked by Spotless (branch `quality-1-spotless`)

Phase 1 of [code-quality-plan.md](code-quality-plan.md).

- `com.diffplug.spotless` 8.11.0 in the root `build.gradle`: all Java in core, desktop, html and e2e (not the vendored
  GWT emulation sources in `html/src/**/emu/**`) is formatted by the Eclipse JDT formatter 4.41 (pinned) with
  `config/eclipse-formatter.xml`, a profile matching the existing style (4 spaces, 120 columns, continuation indent 8,
  hand-wrapped lines kept, comments untouched, enum constants one per line when they don't fit on one), imports in
  IntelliJ's default order, unused imports removed, no trailing spaces, a final newline.
- `./gradlew spotlessApply` formats; `./gradlew build` runs `spotlessCheck`, so CI (the required `test` check) fails on
  unformatted code with "Run './gradlew spotlessApply' to fix these violations".
- One formatting-only commit (50 files, +287/−193 lines), listed in `.git-blame-ignore-revs`.

## 2026‑11‑13, architecture tests (branch `quality-2-archunit`)

Replaced the same day by real rules (`quality-5-layers` below); the dependency list described here is gone.

Phase 2 of [code-quality-plan.md](code-quality-plan.md).

- `ArchitectureTest` (e2e module, ArchUnit 1.5.1, reads the compiled game classes):
  - the game's packages depend on each other only as listed in `e2e/architecture/package-dependencies.txt` (today's
    75 dependencies). A new one fails with the classes that make it; a listed one that is gone fails too, asking to
    delete the line, so the list only shrinks by itself. `-Darchitecture.update=true` rewrites the list.
  - `debug`, `resources`, `util` use no other game package.
  - scenario tests use no game class, only `GameDriver` (the rule of e2e/README.md, now enforced).
  - writes `e2e/build/reports/architecture/metrics.md` (Lakos RACD 0.787, NCCD 3.24; Martin's coupling and
    instability per package); CI adds it to the run's summary page. Not a gate.
- Each check was seen failing on a planted violation (a `resources` class using an actor, a stale line in the list,
  a scenario test using a game class).

## 2026‑11‑13, PMD complexity limits (branch `quality-3-pmd`)

Phase 3 of [code-quality-plan.md](code-quality-plan.md).

- `core` runs PMD 7.29.0 with `config/pmd.xml`: cognitive complexity 15, cyclomatic 10 per method (80 per class),
  NPath 200, at most 10 parameters, NCSS 60 per method and 300 per class, coupling between objects 20, 30 imports,
  no god classes; plus `ClassWithOnlyPrivateConstructorsShouldBeFinal`, `UseCollectionIsEmpty`,
  `UnnecessaryConstructor`. `./gradlew build` fails on a finding (`core:pmdMain`, report in
  `core/build/reports/pmd/`, attached by CI when a run fails).
- The 16 findings the code had are marked `@SuppressWarnings("PMD.<Rule>")` with a "baseline" comment in 10 places
  (`TmxTiledMapLoader` and its `loadPackedMap`, `createObjects`; `ResourcesService` and its `loadUi`; `Tile`;
  `GameContext`'s constructor; `GameScreen`; `Hud`; `Portrait.act`), so new code meets the limits from now on.
- Fixed instead: `BlockedTile`'s empty constructor (the compiler's is the same), `Board` uses `isEmpty()`.
- Seen failing on a planted method (cognitive complexity 31).

## 2026‑11‑13, the action button belongs to the HUD (branch `quality-4a-action-button`)

Phase 4, step 1 of [code-quality-plan.md](code-quality-plan.md).

- `ActionButton` moved from `ui` to `ui.hud`: it is a HUD element (it stands where `Hud` says, over the hint). `ui`
  (stages, `RoundButton`, `UiStyle`) no longer depends on `actor`, `game` or `plugin`; instead `ui.collect` uses
  `ui.hud` (the resolver shows the button) and `ui.hud` uses `plugin` (the button fades with
  `RemoveWhenUnavailablePlugin`). `package-dependencies.txt`: 75 → 74.

## 2026‑11‑13, StageType in a leaf package (branch `quality-4b-stage-type`)

Phase 4, step 2 of [code-quality-plan.md](code-quality-plan.md).

- `StageType` moved from `ui` to the new package `com.mantkowicz.light.stage`, which uses nothing of the game (a
  leaf, checked by `ArchitectureTest` like `debug`, `resources`, `util`). `actor` no longer depends on `ui`; `actor`,
  `feature`, `ui` and `ui.menu` depend on `stage` instead, which can never be part of a cycle.
  `package-dependencies.txt`: 74 → 77 lines, but one fewer link inside the cycle (13 packages are still in it, mostly
  through `GameContext`).

## 2026‑11‑13, collecting is a feature, not part of the player (branch `quality-4c-collect-feature`)

Phase 4, step 3 of [code-quality-plan.md](code-quality-plan.md).

- `CollectFeature` (feature package, created in `GameScreen` once the player exists) owns the `PlayerCollectResolver`
  and runs the `CollectPlugin` every frame; `Player` no longer creates them and no longer has `throwOut`. The HUD
  gets a plain `IntConsumer` that throws out a slot's item (`CollectFeature::throwOut`), so it knows nothing about
  collecting either.
- `actor` no longer depends on `ui.collect`; `feature` depends on `plugin` and `ui.collect` instead (a top-level
  package using lower ones). `package-dependencies.txt`: 77 → 78 lines.
- Collecting now runs after the stages each frame (features run there) instead of inside the player's update: a full
  backpack's slot starts shaking one frame (16 ms) later; `CollectTest` waits that frame. No golden changed.

## 2026‑11‑13, the board reports "no path", a feature shows it (branch `quality-4d-no-path-event`)

Phase 4, step 4 of [code-quality-plan.md](code-quality-plan.md).

- `BoardClickListener` posts a `NoPathEvent` for a clicked tile nobody can walk to; the new `NoPathFeature` shows the
  "no path" text over it (same text, style and animation as before). `board` no longer depends on `notification` or
  `ui`; `feature` depends on `board` instead. `package-dependencies.txt`: 78 → 77 lines.
- The text now appears one frame (16 ms) later (features run after the stages each frame): golden `no-path`
  re-recorded, its only change being the text 0.2 px lower at the same moments (it has risen one frame less).

## 2026‑11‑13, the packed map loader in steps (branch `quality-4e-split-map-loader`)

Phase 4, step 5 of [code-quality-plan.md](code-quality-plan.md).

- `TmxTiledMapLoader.loadPackedMap` (cognitive complexity 41, cyclomatic 24, NPath 954) is split into steps:
  `sortLayers` (drawn, collision, walls), `checkTileSize`, `createTile` with `cellGraphics`, `readAmbientLight`;
  `createObjects` (cognitive 16) hands the nearest-cell search to `nearestCell` (same scan order, same tie-break).
  Behaviour unchanged: no golden or map test changed. Their four PMD baseline suppressions are gone; the class itself
  is still over the limits for size and coupling (GodClass, CouplingBetweenObjects 28, ExcessiveImports 31), so its
  class-level suppression stays until the loader is split into classes.

## 2026‑11‑13, the intended architecture as rules (branch `quality-5-layers`)

Phase 5 of [code-quality-plan.md](code-quality-plan.md): replaces the package dependency list of phase 2, which only
froze the existing state.

- `ArchitectureTest` now states the architecture: five layers (leaves, world, behaviour, UI, application) using only
  the layers below; no mutual use inside a layer (the board doesn't know actors, the event bus uses nothing of the
  game, the basic UI doesn't know the menu/HUD/collecting, features don't know `game`); naming conventions (`*Event`,
  `*Plugin`, `*Feature`); nothing the HTML5 build can't run (reflection, `java.util.concurrent`, `String.format`);
  scenario tests via `GameDriver`; plus the metrics report.
- The violations the code still has are frozen in `e2e/architecture/frozen/` (183 class-to-class dependencies, the
  layers' 120 in 31 classes): they pass, any new one fails; fixed ones are deleted from the files by the test, and a
  new CI step (`Architecture baseline is committed`) fails until that is committed. Seen working both ways: a planted
  `board → ui` dependency failed; recorded on purpose and removed again, the baseline shrank back by itself.
- `e2e/architecture/package-dependencies.txt` is gone.

## 2026‑11‑13, GameContext out of the application layer (branch `arch-1-context`)

Step 1 of emptying the architecture baseline ([code-quality-plan.md](code-quality-plan.md), phase 5).

- `GameContext` moved from `game` to the new package `context`, in the world layer (`ArchitectureTest`). It hands out
  the stages as plain libGDX `Stage`s and the stage map as `Map<StageType, Stage>`, so it knows nothing of the UI.
  `MenuFeature` gets the `MenuStage` from `GameScreen` (it uses the button under the pointer); finding and removing
  notifications of a type is `notification.Notifications` (`NotificationStage` keeps its methods, now calling it).
- The board doesn't use the context any more (a new, unfrozen rule): `Board` and `BoardClickListener` take the
  board service and the event service, `Tile.prepare` takes the physics world, and `GameScreen` prepares the map
  objects on each tile itself (same order as before: a tile's objects, then its own physics).
- Baseline: 183 → 78 recorded dependencies (all "uses game.GameContext" gone, "features know game" emptied). 8 kept
  ones were re-recorded only because their wording names the moved class; one is new, `Tile.getTileObjects()`
  returning map objects, part of the board-uses-map tangle that step 4 removes. No golden changed.

## 2026‑11‑13, events live with what they are about (branch `arch-2-events`)

Step 2 of emptying the architecture baseline ([code-quality-plan.md](code-quality-plan.md), phase 5).

- The event bus (`event`: `GameEvent`, `GameEventService`) keeps only the events that carry nothing of the game
  (`MenuCloseEvent`, `BackpackItemEjectedEvent`) and `CollectEvent` (moves in step 3). The others moved next to what
  they are about: `TileTouchedEvent`, `NoPathEvent` to `board`; `PlayerMoveEvent`, `PlayerCreatedEvent`,
  `MenuOpenEvent`, `LookAtMeEvent`, `AddToStageEvent` to `actor`. Only packages and imports changed.
- Baseline: 78 → 57 recorded dependencies (the event bus rule's 21 for these seven). No golden changed.

## 2026‑11‑13, the board doesn't know actors (branch `arch-3-board-occupants`)

Step 3 of emptying the architecture baseline ([code-quality-plan.md](code-quality-plan.md), phase 5).

- `BoardService` keeps track of `TileOccupant`s (new, in `board`: something standing on a tile, which may be used
  from a neighbour) instead of game actors: `registerOccupant`, `unregisterOccupant`, `getOccupants`,
  `isUsedFromNeighbour`. `GameBoardActor` is a `TileOccupant`; `Collectible` answers "used from a neighbour" with
  `isDistant()`.
- Finding what can be collected when something steps onto a tile moved out of the board into
  `actor.CollectibleFinder`, which `BoardService` tells through `OccupantListener` (set by `GameScreen` before anything
  is on the board, so it runs at the same moment as before). `CollectEvent` moved to `actor`.
- Baseline: 57 → 40 recorded dependencies. "The board does not know actors", "the event bus uses nothing of the game"
  and "features do not know the game package" have no violations left: they are plain rules now, their baseline
  files are gone. No golden changed.

## 2026‑11‑13, the board doesn't know the map (branch `arch-4-board-without-map`)

Step 4 of emptying the architecture baseline ([code-quality-plan.md](code-quality-plan.md), phase 5).

- Loading a map moved from `Board` (`loadTileSet`, `loadTiles`) to `map.Maps`; `GameScreen` calls it.
- Tiles hold `TileContent`s (new, in `board`: whether it blocks the tile, what it draws on it) instead of the map's
  `TileObject`s; `TileObject` implements it. `GameScreen` prepares the contents that are map objects.
- Baseline: 40 → 28 recorded dependencies (all of the board's uses of the map). No golden changed.

## 2026‑11‑13, characters above plugins (branch `arch-5-characters`)

Step 5 of emptying the architecture baseline ([code-quality-plan.md](code-quality-plan.md), phase 5).

- The concrete characters `Player`, `Dog`, `Cow`, `Chest`, their base `SpriteActor`, `PlayerCreatedEvent` and the
  player-only `PlayerNotificationPlugin` moved from `actor`/`plugin` to the new package `character`. With `map` (which
  creates them) it is a new layer, characters, between behaviour (plugins, notifications) and the UI. `actor` keeps the
  base classes and what isn't a character (items, inventory, stamina, collectibles).
- `Plugin` moved to `actor`: it is the actor's extension point; the plugins stay in `plugin`. `BoardMovementPlugin`
  walks any `GameBoardActor` (it used nothing of the player but status, actions and tiles); `PlayerMoveEvent` carries
  a `GameBoardActor` and stays in `actor`.
- New plain rules: plugins don't know characters, characters don't know the map.
- Baseline: 28 → 13 recorded dependencies (all of the actors' uses of plugins). No golden changed.

## 2026‑11‑13, the architecture rules hold without exceptions (branch `arch-6-small-ones`)

Step 6, the last, of emptying the architecture baseline ([code-quality-plan.md](code-quality-plan.md), phase 5).

- `CollectPlugin` moved to `ui.collect` (only the collect feature uses it), `MenuStage` to `ui.menu`, the interfaces
  `Interaction` and `Interactive` to `actor`. Actors no longer build the menu's description button: `MenuFeature`
  adds it in front of the actor's own interactions when the menu opens (the same buttons in the same order).
- The UI pack's text colour is `resources.Palette.TEXT`; `UiStyle.TEXT` and the notification labels use it.
- Baseline: 13 → 0. Every rule now holds outright: the frozen baseline (`e2e/architecture/frozen/`), the
  `-Darchitecture.update` switch and the CI step checking the baseline are gone. With no cycles left, ArchUnit's own
  "packages have no cycles" rule is added (it couldn't be used before: today's code hit its 100-cycle report limit);
  seen failing on a planted `board → light` dependency.
- Lakos over the game's 16 packages: RACD 0.375 (was 0.787 over 13 at the start), NCCD 1.78 (was 3.24). No golden
  changed.

## 2026‑11‑13, docs cleaned up, notes for agents (branch `docs-cleanup`)

- `docs/refactoring-plan.md` removed (the first refactoring is long finished and the game it described has changed; it
  stays in git history). `docs/code-quality-plan.md` rewritten from a plan with its history into a short description of
  what the build checks and why (the steps stay in this file).
- `docs/ideas.md`: "Animated objects placed from Tiled maps" removed (done another way: one `objectClass` and class per
  object, e.g. `COW(Cow::new)`; the Tiled dropdown was dropped in PR #28); "Interactable objects" and "Game modes by age"
  updated to today's code (no collect window, the chest throws out its items, 2 backpack slots).
- `docs/hud-design.md` brought into master (it was only on branch `ccr-dcc79c9a-n8m2ou`, while the code pointed to it)
  and rewritten: what is built (with what changed from the first design) and what is planned.
- `README.md`: a "Building and checks" section (the commands, what `./gradlew build` checks, CI).
- `CLAUDE.md`: short notes for agents (how to work here, commands, architecture, gotchas), read by Claude Code at the
  start of every session in the repository.
- Tried out on four small tasks by fresh agents (a PMD exception removed, a new HUD reaction, a changed hint text, a new
  animal). All four were done correctly; their questions led to clearer notes: work on a branch but commit only when
  asked, the changelog date, keeping docs that quote changed things up to date, events that carry nothing staying in
  `event`, when to take screenshots, where test results are, and in `tools/assets/README.md` how `rows` names rows and
  that a sheet in `Sprite` is packed whole.

## 2026‑11‑13, the cow lives: a state machine (branch `cow-state-machine`)

The cow gets a life, written as a gdx-ai state machine that other creatures (and later people) can copy.

- **`CowState`** (`character`) is a plain gdx-ai `State<Cow>` enum, run by a `DefaultStateMachine` in `Cow`: every state
  says what the cow does on entering it, when it goes on to which state, and how it answers the player. No framework
  on top and no config files: to change the behaviour, edit the enum (its comment draws the transitions). `Cow` only
  has the helpers the states call (`stayFor`, `isTimeUp`, `isHungry`, `showWish`...). The player's actions reach the
  states as gdx-ai messages (`MessageManager`, delivered at once).
- **What it does:** stands (3–6 s), then walks one step onto a free tile next to it (twice as likely) or lies down;
  lying, it rests, may fall asleep, gets up again. 20 s after the start or its last carrot it gets hungry (which also
  gets it up): standing, it
  shows a bubble with a carrot (`PictureBubble`, the pack's grey speech bubble) and waits until it gets one; then it
  eats, and shows a heart. The bubble is drawn in front of everyone on the map (below the HUD).
- **The round button** (`CreatureFeature`, for any `actor.Creature`): next to the cow (its four sides) it shows a hand
  (the pack's paw) and strokes it (a heart over the cow, a small one floats up over the player) when the cow stands,
  rests or sleeps; while the cow wishes for a carrot the player carries, it shows the carrot and gives it (the slot
  shakes). Without a carrot the hint button stays, and says "find" with the carrot's picture (`SpeechBubble` can end
  with a picture, `Hud.setHint`).
- **Walking:** `SpriteActor.walkToFreeNeighbour` (a random free side tile; the picture is mirrored while going left,
  and back to its default when it arrives). Sprite actors block their tile, so tiles are now blocked and freed during
  the game: `BoardService.setAccessible` (the path finder's graph is rebuilt). The tile the cow goes to is blocked at
  once, the one it leaves is freed on arrival. If the player was on its way to that tile, it steps back to the middle
  of the tile it is on (`BoardMovementPlugin`); it never shares a tile with the cow. A cow stepping next to a full
  chest doesn't get the chest's button (`PlayerCollectResolver` checks the player's reach).
- **Tapping the cow** walks the player next to it, like a chest (`WalkNextToOnTapListener`; a hold still opens its
  description).
- Fixed on the way: `Board.draw` used whatever colour the batch was left with by the last actor of the frame before,
  so a picture fading out on top of the game stage (a happy face that floated up over a fed cow, since removed: it didn't look good) dimmed the whole map to black. The
  board now sets its own colour.
- **Assets:** the cow sheet's rows are named (`settle`: lies down and gets up, `rest`, `sleep`, `eat`, `love`; `row_05`
  is still unused), `SpriteActor.playOnce` plays a row or a part of it once. New UI pieces: `PICTURE_BUBBLE`, `HEART`,
  `HAND_ICON`.
- **Debug:** `Debug.Tool.STATE_LABELS` writes each creature's state under it (`StateLabelPlugin`, for anything with a
  state).
- Tests: `CowTest` (stroking, wishing and feeding, every state with its animation, the walk, the player stepping back,
  the debug label). No golden changed.
- Known limits, kept simple on purpose: an item thrown onto a tile the cow walks to at the same moment lands under the
  cow (it can be picked up once the cow moves on); next to a chest and a cow at once, their buttons share one place;
  with two creatures next to the player the button is for the first one found.

## 2026‑11‑13, chest items near the player, used from three sides (branch `chest-and-reach`)

- What jumps out of a chest lands on the free tile next to the chest that is **closest to the player** (a random one of
  those as close), not on a random free one, so a small player doesn't have to walk around the chest to pick it up.
  `ItemThrower.throwOut` got a `near` point; items thrown out of the backpack (tapping a slot, making room) still land on
  a random free tile next to the player.
- A chest and a creature (the cow) are used only from the tile **left of, right of or below** them, no longer from the
  one above: the action button is going to move over the chest or creature, and there it would cover the player. One
  rule, `Tile.getUsedFromNeighbours()`, for all of it: showing a chest's button (`CollectibleFinder`), keeping it
  (`PlayerCollectResolver.isInReach`), walking next to a tapped chest or cow (`BoardMovementPlugin.walkTarget`) and the
  cow's stroke/give button (`CreatureFeature`). The cow still steps in all four directions (`getSideNeighbours()`).
- Golden `collect.txt` re-recorded (the carrot lands at (4,3), beside the player, instead of (2,5) behind the chest).
- Tests: `CollectTest` checks the carrot lands next to the player; `CollectTest.noButtonAboveTheChest`,
  `CowTest.noButtonAboveTheCow`; the chest checks in `AnimationTest` and `ShippedMapsTest` use the new rule
  (`GameDriver.usedFromNeighbours`).

## 2026‑11‑13, idea: one round-button resolver, updated after the cow (branch `idea-one-action-resolver`)

- `docs/ideas.md`, "Interactable objects": the cow (branch `cow-state-machine`) got its own resolver,
  `CreatureFeature`, beside the chest's. The entry now says what a shared resolver has to cover because of it (offers
  that change with a creature's state, wish rules in the creature, two things in reach at once, who owns the hint) and
  why it is worth doing (the PMD numbers of `CreatureFeature`). Docs only.

## 2026‑11‑13, the action button over what it acts on (branch `action-button-over`)

- The **action button** (`ActionButton`) no longer covers the hint right of the face: it sits over what it acts on and
  follows it (on the UI stage, so it keeps its size and catches taps before the map; `Collectible.getActor()` and
  `Creature.getActor()` say what it sits over). The chest's button shows the **open chest** (the last frame of its
  opening; `Chest.getClosedIcon()` keeps the old picture). The question mark right of the face stays as it was (what it
  should do later is open). It is the pack's **dark brown** round button (yellow, blue and violet were tried on the map;
  dark brown fits the wooden HUD), drawn at 5 screen pixels per pack pixel instead of the hint's 6 (90×100, about 80 %), its picture
  at 3, 16 px over what it acts on. `RoundButton` takes the colour and scale.
- **Cow:** the stroke and give buttons are over it. When it wishes for a carrot, the button with the carrot takes the
  wish bubble's place while the player is next to it (`Creature.setWishShown`), also without a carrot in the backpack
  (owner's choice): then tapping it shakes the button (`ActionButton.shake`) and the hint still says "find" with a
  carrot. Walking away brings the bubble back.
- A chest and a cow next to the player now show one button each, over each (before, both buttons sat in one place).
- **Several buttons over one thing** (usually one, at most about three) stand in a row centred over it, ordered by
  `ActionButton.setPlace`. The cow keeps its **hand while it wishes** for a carrot: hand left, carrot right. A stroke
  while wishing (`CowState.WISHING` can be stroked now) shows the heart and goes back to wishing (via STANDING, still
  hungry), so the carrot button fades for the heart and comes back.
- **Closing a chest:** once open, the chest keeps its button, now showing the closed chest; tapping it shuts the lid,
  and the button shows the open chest again. Opened again, nothing comes out (it doesn't refill) and there are no
  stars. `Collectible.canBeClosed`/`isOpen` (the chest says yes; the button no longer goes when it is empty) and
  `getActionIcon` follows the state; `ActionButton.setPicture` changes the picture. While the lid moves
  (`Collectible.isBusy`, 0.5 s) the button is unavailable, so it fades a little (its fade-out takes 0.75 s) and comes back
  with the new picture, like the cow's hand during a stroke. Open, the button sits over the lid, which sticks out above
  the tile (`Collectible.getTopY`, `ActionButton.setTopY`: 32 px higher). Golden `collect.txt`: the chest's button now
  stays in the two snapshots next to the emptied open chest, over its lid. Test: `CollectTest.theOpenChestClosesAndOpensAgainEmpty`.
- Tapping the carrot without one also makes the face say "find" with the carrot (`Hud.showHint`, as the hint button
  does), not only shakes the button.
- `CreatureFeature.run` reached PMD's complexity limit and was split (`updateGiveButton`).
- Golden `collect.txt` re-recorded: only the button's position (from (628,74) beside the face to (355,976) over the
  chest). Tests: `CowTest.theButtonTakesTheBubblesPlaceNextToTheCow`, `strokingTheCowWhileItWishesForACarrot`, the hand left of
  the carrot, the give button shaking and the face saying "find" without a carrot,
  `HudTest.theActionButtonAppearsOverTheChest`; `CameraZoomTest` now checks only that the button doesn't grow (it moves
  with the chest).

## 2026‑11‑14, polish from the bug hunt (branch `bug-hunt-polish`)

The owner's picks from the bug hunt's polish list (the rest was judged fine as it is).

- **Action buttons fade in** when they appear (`ActionButton`, 0.3 s, quicker than the 0.75 s fade-out so a tap gets
  its answer soon). Before, a button that had faded out completely popped back at full opacity.
- **A backpack slot shakes** when its item has nowhere to land (no free tile next to the player), like an action
  button that can't act. `Hud` takes the throw as an `IntPredicate` (false: nothing thrown), from
  `CollectFeature.throwOut` / `PlayerCollectResolver.throwOut`.
- **Tapping the walking rabbit stops it** in the middle of the tile it is on. A tap on the rabbit's own tile lands on the
  rabbit (it recentres the camera), never on the board, so it couldn't stop the walk; the rabbit now also posts a tap
  on the tile under it while walking (`StopWalkingOnTapListener`).
- Tests: `CollectTest.theButtonFadesIn`, `CollectTest.aSlotWithNowhereToThrowShakes`,
  `WalkingTest.tappingTheWalkingRabbitStopsIt`. No golden changed.
