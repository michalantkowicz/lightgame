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

## 2026‑11‑14, the cow grazes when nobody feeds it (branch `cow-grazes`)

- A cow wishing for a carrot for **90 s** without getting one gives up: the wish (bubble or carrot button) goes and it
  **grazes**, three bites of grass in a row, each a bite (`GRAZING`, the sheet's row that was `row_05`, now named
  `graze` in `pack.json`; `Action.GRAZE` is generated) and a second of chewing (`CHEWING`, the `eat` row). Then it
  stands again, fed (hungry again 20 s later). While grazing it can't be stroked. From the bug hunt (a hungry cow
  wished for ever); the owner chose the animation and "a minute or two".
- Test: `CowTest.theCowGrazesWhenNobodyFeedsIt` (the states in order, each with its animation, the wish gone, not hungry
  right after). `everyStateShowsItsAnimation` feeds the cow every time, so it never grazes there. No golden changed.
## 2026‑11‑14, three bugs from the bug hunt (branch `three-bug-fixes`)

Found by headless test agents playing the game (and confirmed by hand), fixed together on the owner's request.

- **Taps went through the settings window to the map:** with the settings open, a tap on the window (not on its check
  mark) walked the player to the cell under it. The dimmed screen and the window took the tap, but had no listener, so
  the stage passed it on. `SettingsPanel` now takes every touch on it. Test: `HudTest.tapsOnTheMapDontWalkWhileTheSettingsAreOpen`.
- **No button for a chest next to the player at the start:** the button appears when someone steps next to a chest
  (`CollectibleFinder`); a chest put down after the player (to its right or above it) never got that step.
  `CollectibleFinder` now also reports a collectible when it is put on its tile. Test:
  `CollectTest.aChestNextToThePlayerAtTheStartHasItsButton`.
- **A chest with nowhere for its carrot to land opened anyway:** stars, lid up, carrot still inside (e.g. a chest at the
  end of a one-tile corridor: the player stands on its only free neighbour, so the carrot could never come out). Now it
  stays shut and its button shakes, like the cow's carrot button without a carrot (`ItemThrower.hasRoomNextTo`). Test:
  `CollectTest.withNoFreeCellTheChestStaysShutAndItsButtonShakes`.
- No golden changed.

## 2026‑11‑14, idea: all texts in one place (branch `idea-texts-in-one-place`)

- `docs/ideas.md`, "All texts in one place": the texts the player sees are spread over the code; collect them in one
  list for translations and to find every place that should become a picture. From the bug hunt's polish list. Docs
  only.

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

## 2026‑11‑14, more crops (branch `more-crops`)

- Besides the carrot there are **corn, a tomato, a pumpkin and a cucumber** (the farm items sheet, rows 1, 2, 4, 9 and
  14: `CropType`). They work like the carrot did: `Carrot` became `Crop`, an `Item` of a `CropType`. Items of the same
  kind are alike (`Item.isSameKindAs`, which a `Crop` answers by its type), so any carrot will do for a cow wishing for
  one, and only a carrot.
- **A chest** holds one crop picked at random when the level starts.
- **A cow** wishes, each time it gets hungry, for one of its **favourite dishes**, picked at random: the Tiled string
  property `favouriteDish` on the `COW` object, e.g. `CARROT,CUCUMBER`; without it or empty, any crop (owner's choice; `CropType.all()`, also for a `Cow` given an
  empty list). `CropType.randomOf` picks one of a list. An
  unknown name stops the level with a message listing the crops. The wish is picked on getting hungry, not on
  entering `WISHING`, so a cow stroked while it wishes comes back wishing for the same crop. A cow may wish for a crop
  no chest holds; then it gives up after 90 s and grazes, as before.
- `TileObjectType`'s sprite-actor factories get the object's Tiled properties (`BiFunction<GameContext,
  MapProperties, SpriteActor>`), so a sprite actor can be set up in Tiled. `tools/assets/README.md` documents
  `favouriteDish`.
- **Goldens re-recorded:** `collect` (the chest now throws out a cucumber, and with the shifted random draws the
  thrown-out crop lands at (3,2) instead of (4,4), so the last snapshot is far from the chest, without its button) and
  `torch-flicker` (the room's chest picks its crop with `MathUtils.random`, so the torch's flicker takes different
  numbers). No other golden changed.
- Tests: `CowTest.aCowMayWishForAnyCrop`, `theCowWishesOnlyForItsFavouriteDishes` (both dishes turn up over
  5 minutes and nothing else; the other one can't be given), the stroke test checks the wish stays the same; the cow
  tests feed whatever it wants (`GameDriver.cowWants`, `putIntoBackpack(crop)`). `everyStateShowsItsAnimation` runs
  until it has seen every state (up to 15 min) instead of a fixed 5: with the new random draws the cow first slept
  after 5 minutes. `CollectTest.everyChestHoldsACropPickedAtRandom` (12 chests, not all alike); the collect and HUD
  tests accept any crop. `GameDriver` lists crops by their kind ("Carrot", "Corn") and adds `chestContents`,
  `cowFavouriteDishes`.

## 2026‑11‑14, the field (branch `field`)

- **`Field`** (Tiled `objectClass` `FIELD`, string property `cropType`, e.g. `CORN`; `CARROT` without it): a
  `Collectible` like the chest (it blocks its tile, used from the left, the right and below) that starts ripe. Its
  action button shows the crop: the crop jumps out onto a free tile next to it (stars, as from a chest) and a seedling
  is left. While the crop grows the button shows the **watering can**: the rabbit plays its watering animation
  (`Player.water`: facing up below the field; facing down left or right of it, the owner didn't like the side poses; a
  one-off action in `AnimationPlugin.playOnce`; walking off stops it), water falls onto the field (the pack's `watering`
  sheet, row 0, 0.9 s; the button waits, faded), then it grows a stage. Ripe again, the crop is back on the button. The
  can never runs dry (owner's choice for now).
- Growing uses the pack's `Farming Plants.png`: corn has five stages and is two tiles tall, carrot, tomato, pumpkin
  and cucumber four. A **sign** with the crop (`signs.png`) stands at the tile's top left corner, at half the world's
  pixel scale, sticking out one pack pixel over the tile's left and top edges, behind the plant. The water from the
  watering can sheet doesn't line up with the premium rabbit's pose, so it falls onto the field's middle instead.
- `CropType` knows its plant and sign sprites; `CropType.named(name, fallback)` reads the Tiled property.
  `Waterable` (actor): something whose button waters it while it needs water; `PlayerCollectResolver.use` waters it
  before anything else, and keeps its button while it needs water.
- Assets: pack.json sheets `plant_corn`… `plant_cucumber` (row `grow`), `sign_corn`… `sign_cucumber` (row `sign`),
  `watering_can` (row `icon`, from the tools items sheet: the UI pack has no can) and the `watering` sheet's row 0
  named `splash` with its anchor on the falling water; new `Sprite` constants; `Action` gained `GROW`, `ICON`, `SIGN`,
  `SPLASH`.
- `AnimationPlugin.run` split (`followMovement`) to stay under PMD's complexity limit.
- Looked at it in a hidden LWJGL3 window: the five ripe fields with their signs, seedlings, water falling and the
  crop growing a stage, the rabbit watering, the button with the crop and then the can.
- `level1` (owner's request): one field per crop on the right of the island, column 19, rows 3 to 11 from the top
  with a free row between them (corn at the top, so its tall plant has room): corn, carrot, tomato, pumpkin, cucumber.
- Tests: `FieldTest` (picking, watering three times back to ripe and picking again, corn's five stages, the rabbit
  watering facing down beside the field, walking away while watering). No golden changed. `GameDriver`: `Kind.FIELD`, `field()`,
  `fieldButton()`.

## 2026‑11‑15, a field grows over time (branch `field-grows-over-time`)

- The owner found fields too generous: picking and three quick waterings gave a crop every ~15 s. Now **one
  watering** is enough: once the water has fallen, the crop grows a phase at once (instant reward) and then by itself,
  a phase every **30 s** (`Field.SECONDS_PER_PHASE`; 15 s felt too fast to the owner) until it is ripe (for how
  long, see the sprout below). While it grows the field has nothing to do, so its button fades out (`needsWater` is false and it holds nothing) and comes back with the crop once it is ripe. It grows also with
  nobody next to it.
- Each new phase the plant **bounces** (scales to 1.15 from its bottom and back, 0.3 s), so a child sees something
  happened. Looked at it in a hidden LWJGL3 window.
- While it grows the plant slowly **squashes and stretches** (5 % shorter and wider and back, 0.75 s each way, from
  its bottom), so a field visibly lives without looking like a crop to pick up (those bob up and down). It stops when
  the crop is ripe.
- The other options (the can runs dry and is refilled at water; seed bags to plant again) are in `docs/ideas.md`
  (PR #63), to do together with the owner.
- Tests: `FieldTest` rewritten for one watering and the 30 s phases (no button while growing, the button back when
  ripe, corn's four timed phases, growing with nobody next to it). `GameDriver.field()` says "growing". No golden
  changed.
- Growth **stages are called phases** now (`Field.getGrowthPhase()`, `SECONDS_PER_PHASE`, ...): the owner noted that
  "stage" already means scene2d's `Stage`.
- A field just picked is **empty but for a tiny sprout**, the same for every crop (the first picture of the blue
  flower's row in `Farming Plants.png`, sheet `plant_seedling`), drawn as its own picture since the plant sheets are
  trimmed differently. The crop's own pictures follow from phase 1, so every crop has one phase more (a carrot field
  ripens 1.5 min after its watering, corn 2 min). The owner wanted the field to look empty before it's watered.
- An empty field's **sign shows a question mark** instead of the crop: the pack's blank sign (`sign_blank`) with the UI
  pack's question icon (`UiImage.HINT_ICON`, the hint button's "?") on its board, 7 sign pixels tall. Once watered the
  sign shows the crop again. With seeds (`docs/ideas.md`) the player will choose what grows there, so the default
  shouldn't be given away.
- **Weeds.** Nobody chooses what grows on a field yet (seeds are an idea), so when the water has fallen on an empty
  field a **weed** may grow instead of the crop: 40 % (`Field.DEFAULT_WEED_CHANCE`), or Tiled's new `weedChance`
  property (0..1; tests use 0 and 1 so the dice don't decide). The weed is the rest of the sprout's row (the blue
  flower, sheet `plant_weed`, three phases after the sprout) and the sign keeps its question mark. Ripe, its button
  (showing the weed; `Weedable`) pulls it out: it disappears, nothing is gained, the field is empty again, and the
  HUD's face plays the premium emotes' sweat row (`TEEMO_SWEAT`, row 10) while saying "Beautiful weed but unusable!"
  for 3 s (`SpeechBubble` can now stay longer). Picking a real crop plays the happy row 3 times (`TEEMO_HAPPY`, row 16; once was easy to miss).
  The field tells the HUD through `FieldHarvestedEvent`. The owner's idea; looked at it in a hidden LWJGL3 window.
  Vanishing at once looked odd to the owner, so the pulled weed rises a little (4 art pixels) and fades out in 0.5 s
  while the sprout shows under it.
- An empty corn field's watering button sat as high as over ripe corn (the plant's picture box is two tiles tall in
  every phase). `Field.getTopY` now gives the tile's top for an empty field and the shown plant's top otherwise, so
  the can's button is as high as on any field; ripe corn's button stays over the plant.
- PMD then called `Field` and `Hud` possible God Classes, so parts moved out rather than adding exceptions: the sign
  and its question mark (`FieldSign`), the falling water (`WaterSplash`) and the face's reactions to a harvest
  (`FaceReactions`, first `HarvestReactions`); later also the weed's picture and fade-out (`WeedPlant`, now `WeedGrowth`) and the
  bounce and squash (`PlantMoves`).
- Review (owner): what grows after the sprout is now a `Growth`, the crop (`CropGrowth`: its pictures, ripe it puts
  the crop into the field) or a weed (`WeedGrowth`, formerly `WeedPlant`: its pictures, gives nothing, fades out when
  pulled); the field picks one when the water has fallen on it (the weed chance), so `Field` has no weed branches.
  Swapping the whole actor for a weed field was considered and left: the board, the buttons and the state would all
  have to move over. With seeds, planting would pick the crop without the dice. The HUD's `HarvestReactions` became `FaceReactions`
  (`reactions.react()`: it read as if the HUD updated the harvests), kept plain with a note to rework it once the
  face reacts to more things (no reaction framework yet). The happy face plays 3 times.
- **Bug: the right mouse button** (found by the owner in the HTML build) walked the rabbit but didn't press any
  button: the board took any mouse button, while scene2d's `ClickListener` (behind every button) listens to the left
  one only. Wanted: the right button behaves exactly like the left; fixed on this branch at the owner's request (too
  small for its own). `GameScreen` hands input to the stages through `AnyButtonInput` (an `InputMultiplexer`) that
  reports every mouse button as the left one; a second button pressed while one is held does nothing more, so
  pressing both is one click. The browser's right-click menu was already kept away by `html/webapp/index.html`.
  `MouseButtonsTest` (right click walks to the chest, opens it with its button, taps the hint; both buttons at once
  open the chest once) failed before the fix. `GameDriver`: `rightClick`, `rightClickButton`, `rightClickHint`,
  `clickButtonWithBothButtons`; `tap` takes the button.
- On this branch too, at the owner's request: their own level1 edits in Tiled (new tilesets: darker soil, mushrooms and
  flowers, trees and bushes, barn, fences, water objects, chicken houses; a new layer `graound0` with decorations), with
  the tiles atlas regenerated by `build_assets.py`.

## 2026‑11‑15, chickens (branch `chickens`)

- The owner's request: chickens like the cow, for now with a few differences; eating from the ground and eggs come
  later. Two kinds (`ChickenType`, Tiled's objectClass `CHICKEN` and `CHICKEN_BABY`): a chicken and a chick.
- **Restless.** They stand shorter (chicken 1.5–3 s, chick 0.8–2 s; the cow 3–6 s), walk faster (1 s and 0.7 s per
  tile; the cow 1.5 s) and walk more often; the chick most. In a seeded 10-minute run on open grass the chick takes the
  most steps, then the chicken, then the cow (`ChickenTest.chickensAreRestless`).
- **Pecking.** Standing, they often peck at the ground (`PECKING`, the peck row), looking for something: chicken 2 in
  6 choices, chick 2 in 7, besides walking and sitting down.
- **Wishes only for corn, bored soon.** Hungry 20 s after their last meal (the cow at first too) they wish for corn;
  after 15 s without it (the cow: 90 s) they peck at the ground three times instead (`FORAGING`) and are no longer
  hungry. Fed corn they eat (the eat row: pecking at it) and are happy.
- **Hearts.** The pack draws a chicken's hearts on the row above its love row, so `Chicken.playLove` plays the love
  row and the hearts row as a picture of its own one frame (16 art pixels) higher (`PlayOnceImage`).
- **Shared with the cow.** What both do (hunger, wishes and the bubble, being fed and stroked, waiting, feeding
  themselves) moved from `Cow` into `FarmAnimal`; `Cow` keeps its `CowState` and draws its random numbers in the same
  order, so the cow behaves exactly as before (every golden unchanged). `ChickenState` is the chickens' own state
  machine, as `CLAUDE.md` asks (one enum per creature).
- Assets: the `chicken` and `chicken_baby` sheets got row names (idle, walk, settle, rise, rest, sleep, peck, eat,
  hearts, love; duplicate rows keep `row_NN` names so they show in the preview but make no actions); `Sprite.CHICKEN`
  and `CHICKEN_BABY`; `Action` gained `HEARTS`, `PECK`, `RISE`.
- **Bugs found by test agents** (Haiku, five per round, each in its own worktree; owner's request), both in
  `CreatureFeature` and likely only now that animals move about: (1) only the first animal found next to the rabbit
  got buttons, so with a chicken on each side the second one, still wishing, could not be fed; (2) a button stayed
  over the animal it came for after that one walked away, while tapping it stroked or fed whichever animal was next
  to the rabbit now. Now each button is for one animal and stays with it (it fades once that animal is out of reach
  or done): the wish button for one wishing for something the rabbit carries (else any wishing one), the hand for
  that one too if it can be stroked. `ChickenTest.bothWishingNeighboursCanBeFed` and `aButtonStaysWithItsAnimal`
  failed before the fix; `ActionButton.getOver`, `GameDriver.buttonOver`.
  Round two (two agents: a stress run with 2 cows, 3 chickens and 2 chicks for 30 minutes, 608 taps; the button fix
  with a cow and chickens) found no bugs, and one case to improve: with the wish button over one animal, getting
  what another one next to the rabbit wants didn't move the button there (it shook "find"). Now a wish button stays
  only while its animal is still the one to give to (`ChickenTest.theGiveButtonGoesToTheAnimalTheRabbitCanFeed`,
  failed before).
- Owner's review of the open questions: the small heart over the player after a stroke is **smaller and starts
  higher** (1.5× instead of 2×, from just over its ears instead of over its face; looked at 2×, 1.5× and 1× side by
  side); the rest stays as it is.
- Owner's request: a stroke plays **once the hand has faded out**, so the button never covers the hearts. The tap
  strokes the animal at once, but it first stands still in the new state `BEING_STROKED` for as long as the hand
  fades (`RemoveWhenUnavailablePlugin.DURATION`, 0.75 s; no dice drawn, so the cow's other choices stay the same),
  then `STROKED` shows its hearts and the small heart floats up over the rabbit. For every animal, the cow too.
  (A first version only remembered the tap and stroked after the fade: the owner found strokes lost when the animal
  started walking meanwhile.) Chickens can now also be stroked while sitting down or getting up: next to the rabbit
  they never walk, so those were the moments a tap could still miss (`ChickenTest.everyStrokeIsPlayed`).
- **Bug (owner): the chick couldn't be tapped from far away.** Two causes, found with throwaway probes on level 1 on a
  big screen: (1) when none of the cells an animal is used from can be reached (in the bottom right corner the chick
  is used only from its left, and the chicken shut that cell in), a tap did nothing at all; now the rabbit walks as
  close as it can get, to the nearest reachable cell next to it (`BoardMovementPlugin.walkTarget`, for chests too;
  `ChickenTest.tappingAChickNobodyCanGetToWalksAsCloseAsPossible`). (2) With the rabbit far up the map, the bottom of
  the map is behind the HUD's bottom row, which caught every tap inside its whole box, also in the gaps between the
  slots, the face and the stamina bar where the map shows; now only its pieces keep their taps
  (`HudTest.aTapBetweenTheHudsPiecesReachesTheMap`). Both tests failed before. A first guess (chickens keeping still
  for a rabbit above them blocking a corridor) was wrong and reverted.
- Owner's request: **20 s between strokes** (`FarmAnimal.SECONDS_BETWEEN_STROKES`): once stroked, an animal can't be
  stroked again for 20 s and the hand doesn't come back meanwhile (`FarmAnimal.canBeStroked` = what it does allows
  it, `isStrokable` from its state, and the pause is over). For every animal; the first stroke works at once.
  `CowTest.strokingTheCow` checks the hand is back only after 20 s.
- Owner's request: **the cow walks a little more and wishes about once a minute**: it gets hungry 60 s after its last
  meal (was 20 s), stands 2–4 s between choices (was 3–6 s) and walks 3 times in 4 (was 2 in 3) rather than lying
  down. `CowTest` waits up to 63 s for the first wish; two tests checking the bubble "far away" now use a longer row
  and step the rabbit away, since the busier cow may wander next to it first. No golden changed.
- A wish is chosen **once per hunger** (`FarmAnimal.chooseWish`): stroked while wishing, an animal comes back wishing
  for the same crop. `CowState.STANDING` meant this already, but it chose again; `CowTest` passed only by the luck of
  the seeded dice, and the new state's timing showed it.
- Owner's request: a chicken **never walks away from the rabbit** next to it (diagonally too) or walking to a tile
  next to it (`Chicken.isPlayerNear`, the player's walk target from `BoardMovementPlugin.getTarget`): it pecks
  instead. It may still be hard to catch while the rabbit isn't coming. And it **keeps looking the way it walked**
  (`SpriteActor.walkToFreeNeighbour(seconds, keepFacing)`; the cow still turns back), the hearts mirrored with it.
  `ChickenTest`: `theyDontWalkAwayFromTheRabbitNextToThem`, `aChickWaitsForTheRabbitComingToIt` (both fail without
  the rule), `aChickenKeepsLookingTheWayItWent`. `Player.getWalkTarget` names `Tile` in
  full: `Player` is at PMD's import limit.
- Left as they are (owner): sleeping stays 4–8 s, though the chickens' rest and sleep rows look much alike; in level 1
  the chickens are off screen at the start; chickens look hungry about 39 % of the time (20 s hunger, 15 s wish); a
  wish bubble near the map's top edge can be cut off (no walkable ground planned there).
- `level1` (owner's request): a chicken and a chick at the bottom right, cells (17, 13) and (19, 13) from the top.
- Tests: `ChickenTest` (corn only, restless, bored soon, fed corn with hearts one frame up, stroking a chick, every
  state's animation for both, level 1). `GameDriver`: `Kind.CHICKEN` and `CHICKEN_BABY` and per-animal helpers
  (`animalState`, `animalAnimation`, `animalWish`, `animalWants`, `animalFavouriteDishes`, `chickenShowsHearts`,
  `chickenHeartsOffset`, `strokeAnimal`); wish bubbles are found per character, since several can wish at once
  (`PictureBubble.getCharacter`). Looked at both in a hidden LWJGL3 window.

## 2026‑11‑15 and 16, cutting grass: a scythe swing, its icon, growing grass (branch `cutting-grass`)

- The owner's request: an 8-frame scything animation for the player, facing down only, to be used next (cutting
  grass). The pack has no scythe, so `tools/assets/scythe_animation.py` (Pillow, run once per pack copy) makes it from
  the rabbit's `chop_down` row: the axe is taken out (its two greys and two browns appear nowhere else on the rabbit)
  and a scythe is drawn along where the axe was held, a 10 px handle and an 8 px curved blade with a light cutting
  edge, hooking the way each frame needs: held low on the left, raised up and left (the blade curving down, as a
  raised scythe), swept down past the rabbit with the pack's white arc, and ending low in front of its feet, the blade
  along the grass. Tried on the till row too, which swings straight up and down: no good for a scythe.
- The game draws the rabbit from one sheet (one trim box and anchor for all its rows), so the frames go into a copy of
  `Characters/Premium Charakter Spritesheet.png` with one more row, `scythe_down` (row 24), saved as `... with
  scythe.png` in the pack folder (owner's choice: a file next to the pack's, git-ignored like the pack). `pack.json`
  reads the rabbit from it and ignores the original; `Action` gained `SCYTHE`; the atlas is regenerated. README.md and
  tools/assets/README.md say to run the script once after copying the pack. Not used by the game yet.
- The owner's request: a **scythe item icon** in the style of the pack's `Objects/Items/tools and meterials.png`. The
  same script draws it pixel by pixel (16x16): the handle from the bottom left up to the right as the axe icon's (its
  three browns), a crescent blade across the top curving down at its tip (the icons' four greys), all in their 1 px
  light outline. Saved as `Objects/Items/scythe icon.png` in the pack folder; the sheet `scythe_icon` (row `icon`,
  like `watering_can`) and `Sprite.SCYTHE_ICON` put it into the atlas. Looked at it 16x and at game size next to the
  pack's axe and pickaxe.
- The owner's request: **growing grass, cut with the scythe** (feature flag `GROWING_GRASS`, a new `debug.FeatureFlag`
  enum for whole features, on as the game ships; the feature isn't even created when it is off).
  `feature.GrassFeature` holds the settings as constants: every 30 s (`SECONDS_BETWEEN_SPROUTS`) each tile the rabbit
  can get to (from its start; a tile only animals stand on counts, a chest's or a field's never does) without grass
  sprouts some with 1 % (`CHANCE`), 10 % next to grass, diagonally too (`CHANCE_NEXT_TO_GRASS`), all decided on the
  grass there was before. It grows a phase every 30 s like the crops (`Field.SECONDS_PER_PHASE`, now public), a still
  picture each from `Objects/Mushrooms, Flowers, Stones.png` row 2 (sheet `grass_tuft`, `Sprite.GRASS_TUFT`): a tuft
  and two tufts in a layer right over the board (under everyone), a bush and a big bush in a layer kept in front of
  the map, 90 % opaque (`TALL_ALPHA`), under only the wish bubbles (kept in front so wishes stay readable), the HUD and
  the action buttons. `actor.Grass` blocks nothing and taps go through it.
- Owner's review: grass **stands on its tile's bottom edge** (it was centred on the tile, like the sprites, so its
  blades floated 1–3 px above it): the packed box's bottom on the edge, and the single tuft (2 empty rows under it,
  `Grass.EMPTY_ROWS_BELOW`, measured from the pack) 2 px lower. `GrassTest` checks it (`grassBelowTileBottom`).
- Cutting: standing still on grass the HUD's **tool button** shows the scythe icon instead of the hint (owner: not an
  action button over the grass); tapped, the rabbit swings the scythe once (`Player.scythe`, 8 frames of 100 ms = 0.8 s, under the owner's 1 s, so once) and then the
  grass fades out in 0.5 s; walking off mid-swing (which stops the swing) leaves it. `AnimationPlugin.isPlayingOnce`.
- The grass has its own dice: a copy of the game's at the start (`RandomXS128` state, no roll taken), so the animals'
  random choices are the same with or without grass and no golden changed.
- Tests: `GrassTest` (sprouting and spreading only where the rabbit can go, none with the flag off, the phases and the
  tall layer, cutting, walking off mid-swing, blocking nothing). `GameDriver`: `growingGrass`, `grass`,
  `sproutGrass`, `grassDrawn`, `grassFadingOut`. Looked at the phases, the button and the swing in a hidden window.
- **Naming (owner):** the big round HUD button right of the face is the **tool button** (`ui.hud.ToolButton`, actor
  name "TOOL"; it was the hint button, `Hud.getHint`): it shows a tool for where the player stands (`setTool`, the
  scythe on grass) or, without one, the hint (question mark). The small round buttons over things on the map stay
  **action buttons**. `Hud.ACTION_RIGHT`/`ACTION_Y` (they placed the round HUD button) became `TOOL_RIGHT`/`TOOL_Y`;
  `GameDriver.tapToolButton`, `toolButton`. The tool's logic moved out of `Hud` into `ToolButton` (PMD: Hud a possible
  God Class otherwise). The goldens `hud` and `hud-hint` were re-recorded for the rename only: their HUD line says
  "tool button: hint" instead of "hint:" (nothing else changed); CLAUDE.md, hud-design.md and e2e/README.md say so too.
- Owner's request after the play-test: **one swing cuts all around**: the grass on the rabbit's tile and on all eight
  tiles around it (diagonally too) fades out. The scythe still comes only when the rabbit stands on grass (just next
  to it: the hint). Walking off mid-swing still leaves it all. `GrassTest.oneSwingCutsAllAround`,
  `grassOnlyNextToTheRabbitBringsNoScythe`.
- Owner's request: cut grass **gets smaller while it fades out** (`Grass.cutDown`): to a fifth of its size over the
  fade, which the owner then made 30 % quicker (`GrassFeature.FADE_SECONDS` 0.5 s → 0.35 s), towards its base (the blades' bottom stays on the tile's bottom edge). A first try squashed it flat as if
  mown; the owner found that weird. Clippings flying off could be added later from `GrassFeature` at the cut, without
  changing this. `GrassTest` checks it is smaller partway (`fadingGrassHeights`). Looked at frame by frame.
- Owner's request (small, on this branch): the **FPS label** (`Debug.Tool.FPS_LABEL`) has a black box behind its text
  (`LabelFactory.createLabel`'s background: a stretched black pixel with 6x2 px padding), sized to the text each frame.
- Owner's request: the **scythe icon redrawn after the owner's picture**: a straight handle standing on the right, a
  dark collar at its top and a long blade sweeping left from it, thick at the collar and narrowing to a point that
  curves down (it was a diagonal handle like the axe's with a crescent across the top). Same palette and outline.

## 2026‑11‑16, play-test fixes (branch `playtest-fixes`)

The owner went through the Haiku testers' list from PR #67 and picked these; the rest stays as it is (tall grass
hiding characters and the chickens' hard-to-get corn are on purpose; the hint, the texts kids can't read and the grass
tufts looking a bit like hearts wait for later work).

- **Grass spreads 10× more slowly:** `GrassFeature.CHANCE` 1 % → 0.1 %, `CHANCE_NEXT_TO_GRASS` 10 % → 1 % (a test
  island had 6 tufts after 2 minutes). Growing stays a phase every 30 s. On `GrassTest`'s 140-tile meadow: about 2
  tufts after 10 minutes, 17 after 30 (the spreading test now waits 30 minutes).
- **Grass from Tiled:** an object with `objectClass` `GRASS` and a `phase` property (1 a tuft … 4 a big bush; 1
  without it, kept within 1–4) puts grass on its tile (`map.TileGrass`, only a mark: `GrassFeature` plants it at the
  start, so with the feature flag off there is none). `Grass.growTo` sets its phase (also used when it grows by
  itself); `BoardService.getTiles`. `GrassTest.theMapPutsGrassOnTiles`, `withTheFlagOffTheMapsGrassIsntThere`;
  tools/assets/README.md lists the object.
- **The face answers taps:** a tap on the face or the stamina bar (both keep their taps from the map) makes it play
  a gesture once: happy, cheering, an ear twitch or a blink, at random and never the same twice in a row; a reaction
  already playing (the weed's sweat) isn't cut short (`FaceReactions.tapped`). Its dice are a copy of the game's,
  like the grass's (moved to `util.Dice.copyOfTheGames`), so the animals do the same as before. `HudTest`.
- **The chest's button is a gold star** (the UI pack's stars, piece 12, `UiImage.TREASURE`: treasure inside) instead
  of the open chest; opened, the chest stays open and its button goes for good (no closing, no opening it empty:
  the fields are for crops now). The open-chest picture and `Collectible.canBeClosed` on the chest went with it.
  Golden `collect` re-recorded: once the crop has landed there is no button line any more (the only change).
  `CollectTest.theOpenedChestStaysOpenWithoutAButton` replaces the test of closing and opening it again;
  `MouseButtonsTest` follows. Looked at the star in a hidden window.
- **Wish bubbles 25 % smaller and a little closer:** `PictureBubble` at 3 pack pixels per pixel (was 4), its picture
  shrunk as much; 4 px between the animal and the bubble (was 8; the smaller frame's empty bottom rows bring it closer
  too). Closer still, it covered the hand button over the cow. Looked at with the cow and a chicken.
- **Level 1 (the owner's edit in Tiled):** a few GRASS objects (phases 1–3) near the chickens, and the chicken and the
  chick moved up to (15,7) and (16,6); `ChickenTest.levelOneHasAChickenAndAChick` follows.

## 2026‑11‑16, chickens eat crops on the ground (branch `chickens-eat-crops`)

Owner's request: a crop the player left lying on the ground (e.g. a cucumber that jumped out of a field) gets eaten
by a chicken, slowly enough that the player can still save it.

- **Noticing:** standing or wishing, a chicken notices a crop up to 6 tiles away (diagonally too: within a 13×13
  square) that no other chicken goes for and that it can get next to; the nearest one by steps
  (`character.CropHunt`). A crop on the ground beats wishing for corn.
- **Going for it:** it stands looking for 5 s, then takes one step (up, down, left or right, never onto the crop's
  tile or anyone's), and again, until it stands beside the crop, left or right of it (first above and below too: the
  owner found pecking from below looked really bad).
- **The player first:** before each step it checks the player could still walk to the crop if it stood there
  (`BoardService.findSteps`, a breadth-first search that may pretend tiles are blocked or free, with the player's
  no-corner-cutting rule). If not, it gives that crop up for good (as if eaten: it doesn't go for it again) and goes
  back.
- **Pecking:** beside it, it turns to the crop (right of it it looks left, left of it right) and pecks (the peck row, looping) for 10 s; the crop pulses meanwhile, a little bigger and
  back (`ItemOnGround.setPecked`, up to 1.12×, 0.5 s), and lies as before once nobody pecks it. Then it is eaten
  (gone) and the chicken isn't hungry any more.
- **Going back:** eaten, or picked up by the player meanwhile, the chicken walks back at its usual pace to where it
  started, or, taken or out of reach, to the reachable tile nearest to it; then it goes on as before (also looking
  for more crops).
- New chicken states `GOING_FOR_CROP`, `STEPPING_TO_CROP`, `PECKING_CROP`, `GOING_BACK` (not strokable meanwhile).
  `SpriteActor.stepOnto` (a step onto a given tile, split out of `walkToFreeNeighbour`), `FarmAnimal.stayExactly`
  (a wait without dice, so the animals' other random choices stay as they were: no golden changed).
- Tests: `ChickenEatsCropsTest` (slow steps, never on the crop's tile, facing, the pulse, eaten after 10 s, back
  home, picked up first, never shutting the player out of a corridor or of another crop (also going back), already
  beside: looks first and never pecks in the player's way, a long way round: doesn't go, 6 tiles yes and 7 no, one
  chicken per crop).
- **Play-tested by Haiku agents** (owner's request; 4 rounds, 11 testers, each in its own copy of the repo, playing
  through `GameDriver` scenarios and hidden-window screenshots; level 1 for 20–30 minutes, stress runs with 5
  chickens and 155 crops). Fixed from their findings:
  - already beside the crop in a corridor between the player and the crop, it pecked at once and shut the player out
    for 10 s: the "never in the way" check now also runs where it stands while it looks and pecks;
  - already beside the crop it pecked at once: it looks 5 s first, as from further away;
  - on level 1 it waited 5 s in the only gap between two fields leading to *another* crop: the check now protects
    every crop on the ground the player could otherwise reach, not only its own (two flood fills from the player,
    with and without the chicken there);
  - pecking, it stood a whole tile from the crop: it leans 0.3 of a tile towards it (`Chicken.lean`, 0.2 s), and eases
    back when it stops (`standStraight`; a step it takes meanwhile carries on from where it is);
  - on level 1 a crop's near side got taken and it set off on a 15-step detour round a wall (110 s): it walks at
    most 12 steps to a crop (`CropHunt.MOST_STEPS`, twice the noticing distance), when noticing it and on every step;
  - done or giving up, it went back home even when home was the only way to another crop, and stayed there: going
    back it picks the tile nearest home where it isn't in the way (with none, a dead-end corridor, the nearest),
    again before every step (a crop dropped while it walks back counts too);
  - the eaten crop vanished in one frame: it shrinks and fades out in 0.35 s (`ItemOnGround.eatUp`; nobody can pick it
    up meanwhile); the pulse was hard to see: up to 1.2× (was 1.12×).
  `GameDriver`: `dropCrop`, `cropOnGround`, `cropOnGroundScale`. Looked at the pecking and the pulse in a hidden window.

## 2026‑11‑16, idea: no one-tile corridors (branch `idea-no-chokepoints`)

- `docs/ideas.md`: a `ShippedMapsTest` check that the shipped levels have no one-tile chokepoints, where a wandering
  chicken can shut the player out (found by the play-testers of the chickens eating crops; the owner wants it handled
  by level design).

## 2026‑11‑16, stroking a chicken that goes for a crop (branch `stroke-hunting-chicken`)

- Owner's decision after the play-tests: a chicken going for a crop (looking at it, `GOING_FOR_CROP`) or pecking at
  it (`PECKING_CROP`) had no stroke button, so a child tapping it got no answer. Now both states can be stroked (the
  usual 20 s between strokes still holds; not mid-step nor going back). Stroked, it gives the crop up for good
  (`CropHunt.giveUpForAStroke`: the crop lies still again, for anyone to take, and this chicken doesn't go for it
  again), stands still for the hand, shows its hearts (`STROKED`) and then goes back where it started (`GOING_BACK`
  instead of `STANDING`).
- The other play-test points the owner looked at stay as they are (walking out of a gate, the corn wish's 15 s, the
  pace, still hungry after losing the crop, grass over the chicken, the field button over the tile above the field,
  the peck pose); one-tile corridors are left to level design (idea in PR #70).
- Tests: `ChickenEatsCropsTest` (`goingForACropItHasItsStrokeButton`, `strokedWhileItGoesForTheCropItGivesItUp`,
  `strokedWhileItPecksItStops`).

## 2026‑11‑17, tapping the wish bubble and giving from a slot (branch `ccr-f4ec0085-lf63hs`)

- Owner's request: a child sees the wish and taps it, or taps the wished crop in the backpack, and expects it to work.
- **The wish bubble is tappable** (`PictureBubble.onTap`): a tap on it walks the player next to the animal, the same
  `WalkNextToOnTapListener` as a tap on the animal itself. A hold does nothing (the animal's menu stays on the animal).
- **A backpack slot gives its item to a wishing animal next to the player** (`CreatureFeature.giveFrom`, asked first by
  the HUD's slot tap through `Hud.setGiveAway`): when the animal the wish button is for wishes for what is in the
  slot, it gets it exactly as from that button, from this slot (the slot shakes, the animal eats). An empty slot, or
  one with something else, throws its item out as before. To keep `CreatureFeature` under PMD's God Class limit, the
  one-line `isNextToPlayer` was inlined into `canStroke`.
- Tests: `CowTest.tappingTheWishBubbleWalksNextToTheCow`, `CowTest.tappingTheSlotWithTheWishGivesItToTheCow`;
  `GameDriver.tapWishBubble`. No golden changed.

## 2026‑11‑17, doors to other maps: the chicken coop (branch `ccr-7d84cbcc-88svyo`)

- Owner's request: the player can go into the chicken coop and out again, and both maps keep living (a watered field
  grows while the player is in the coop). Animals live on in full and go through doors too; a door can say who may
  pass. Options discussed first: one big map with regions vs. separate maps all acting each frame (measured on
  desktop: a second farm-sized map acting unseen costs ~50 µs a frame, drawing it as part of one big map without
  culling ~150 µs plus twice the vertices); the owner chose separate maps.
- **`game.Area`**: what `GameScreen` built for its one map (board, game/notification/menu stages, its own physics world
  and lights, its own `GameEventService`, its features) is now one area per map, with its `GameContext`. `GameScreen`
  keeps the areas by file name, the UI stage and the HUD; each frame every area acts, the player's one is also drawn
  (with its lights) and gets the taps; the others act behind the scenes before its features (so what they tell the
  shared HUD, e.g. "no hint", is overridden by the player's map). Its PMD baseline exception moved with that code
  to `Area`. `Main` no longer makes the physics world or the event bus (each area has its own).
- **Doors**: Tiled's `DOOR` object (`TileDoor`: `door` name, `targetMap`, optional `passes`, e.g. `PLAYER,CHICKEN`),
  run by `DoorFeature`: whoever *ends its walk* on a door (walking over it takes nobody through) and is let through
  goes to the door with the same name on the other map (`Travel`, implemented by `GameScreen`). Whoever came out of a
  door stays until it has walked off and back on. A map is built the first time someone goes there (so a level's
  goldens don't change until then; building a farm-sized map takes ~12–15 ms, hidden behind the curtain).
- **The player** goes behind a `Curtain` (the screen darkens 0.3 s, the player comes out on the other map, the camera
  is over it at once, the screen lightens 0.3 s; taps wait meanwhile). Each map has its own `Player`, on the map only
  while the player is there (`GameBoardActor.leaveBoard`); they share the backpack and stamina
  (`Player(context, elsewhere)`), so each map's features keep their rabbit. The HUD follows the player:
  `Hud.enter(events, throwOut, giveAway)` replaces `Hud.setGiveAway` (it also rebinds the events and the slot's throw
  out). Action buttons over things on the map left go at once. With no free tile at the door, one comes out on the
  nearest free tile.
- **Animals**: a farm animal on a door comes out on the other side at once, as a new animal of the same kind
  (`FarmAnimal.moveTo`): as hungry, wishing for the same, as long since a stroke; its state machine starts again
  (it looks around first, then wishes again if hungry). Its tile is freed and the arrival tile blocked
  (`SpriteActor.leaveBoard`, `comeOnto`). Who goes through doors is the character's own say
  (`GameBoardActor.goesThroughDoors`, `moveTo`; the player and the farm animals), so a new one needs no change in
  `DoorFeature` or `GameScreen`. The dog doesn't (yet).
- `GrassFeature` kept an empty list of the tiles the rabbit can reach when it was first asked before the rabbit was on
  the map (its comment said "try again next time"); now it does try again, as a map entered later needs.
- **The coop** (`assets-src/maps/coop.tmx`, a simple version to be redrawn in Tiled): a dirt floor between two
  fences, a water trough, a torch (`ambientLight` 0.55), one chicken, its door (the ramp) letting in only the rabbit
  and chickens. Map property `growingGrass=false`: no grass grows inside. On level 1 the coop's ramp (17, 8) is
  walkable now and is its door. Made from tiles the atlas already had (the pack isn't in the build machine): the game
  copies were written with `build_assets.py`'s own functions, `tiles.atlas` is unchanged.
- Tests: `TravelTest` (the field grows while the rabbit is in the coop, the backpack goes along, the curtain, staying
  on the door; a chicken goes through by itself and wishes as hungry as it was; the cow stays out; every field of a
  chicken and a cow is carried through a door or named as starting again, so a field added later and forgotten in
  `moveTo` fails it),
  `ShippedMapsTest` (maps behind a door have no PLAYER; every door leads to a shipped map with its pair and can be
  walked to). `GameDriver` reads the player's area (`currentMap`, `visitedMaps`, `whereIs`, `doors`, `curtain`,
  `animalStateOnAnyMap`, `fieldsNotCarriedThroughADoor`); `TestMap.pathOf`. No golden changed.
- CLAUDE.md: what to mind when adding a feature (it runs while the player is on another map; the shared HUD) or a
  character that goes through doors.
- **After the owner's review** (same day; the owner picked each point):
  - **No room behind the door**: the curtain came down again and again (taps open one frame in each round). Now
    `GameScreen` checks for room first: without, no curtain, and the rabbit stays on the door until it has walked
    off (`Area.stayOnDoor`); the same if a hen takes the last tile while the curtain is coming down.
  - A door leading to its own map fails `ShippedMapsTest`.
  - Kept what the branch had removed without asking: `GameScreen.addFeature` (the FPS label uses it again) and
    `Hud.setGiveAway`. `Hud.enter` is now just `Hud.listenTo(events)`: the HUD is given, once, a throw-out and a
    give-away that ask the player's map (`GameScreen`), so they need no rebinding; a backpack slot's shake moved to
    `FaceReactions`, which reads those events anyway (keeps `Hud` under PMD's God Class limit).
  - `GameBoardActor.comeOnto` (overridden by `SpriteActor`, which blocks its tile) replaces the `instanceof` in
    `Area.welcome`.
  - **Player and map features**: `PlayerFeature` (camera, collecting, creatures, the new `GrassCuttingFeature`) runs
    only on the player's map; `playerLeft()` (nothing by default) takes away what it shows once on the shared HUD:
    the scythe (the hint is set by the new map every frame; a wish bubble hidden under a button comes back when the
    player is back). `GrassFeature` only grows grass now, on every map. The other maps no longer have to act before
    the player's one for the HUD to be right.
  - **What goes through doors in one object**: `FarmAnimal.Shared` (wish, hunger, last stroke, bites left) is handed to
    the animal on the other side; `Player.Shared` (backpack, stamina) is what every map's rabbit shares. The reflection
    test listing an animal's fields not carried over (`GameDriver.fieldsNotCarriedThroughADoor`) went with it.
  - Map property `featuresOff` (`FeatureFlag` names, e.g. `GROWING_GRASS` in the coop) replaces `growingGrass`.
  - **Every map a chain of doors leads to is built at the start** (`GameScreen.buildTheMapsBehindDoors`): no hitch
    mid-game when a hen first walks into the coop; they all live from the first frame. `GameDriver.visitedMaps` is
    `maps` now. No golden changed.
  - **Cheaper maps the player isn't on**: no physics step (only static walls, for the lights' shadows), their
    notification and menu stages don't act; both are cleared when the player leaves.
  - The FPS label needed nothing: it is on the shared UI stage already.
  - Tests: `TravelTest` (no room behind the door; the scythe goes when the rabbit goes; a chain of doors builds every
    map at the start).
- **After the owner's PR comments** (2026‑11‑18):
  - **The door on the tool button**: standing still on the door it came out of (also when there was no room on the
    other side), the rabbit has a door (`UiImage.DOOR_ICON`, the pack's "enter" icon in the question mark's style) on
    the tool button; tapped, it goes through again as if it had walked onto the door (`DoorToolFeature`, a
    `PlayerFeature`). It goes once the rabbit walks off. A tool now shows only over the hint and resets only itself,
    so on a door with grass the door comes first and the scythe follows once the rabbit is off it.
  - **Curtain**: in a wide browser window a strip at the right stayed uncovered: the UI's `ExtendViewport` showed
    more than the fixed bounds (3 × 800 wide). It now covers exactly what its stage shows, every frame.
  - `FarmAnimal.Needs` is `FarmAnimal.Shared`, as in `Player`.
  - `GameScreen` builds maps only at the start (`build`); going through a door just looks the map up (`behind`).
  - The Q/W keys (fade the game stage) are gone, with their test (`ShaderAndLightTest.qFadesAndWRestores`) and golden
    (`fade-shader`), as the owner asked. `GameStage.fadeOut/fadeIn` and `GameDriver.pressKey` stay (unused now).
  - Tests: `TravelTest.theDoorOnTheToolButtonTakesTheRabbitBack`; in the scythe test the coop's tool button shows the
    door now.

## 2026‑11‑16, fishing (branch `fishing`)

Owner's request, after choosing the minigame with Haiku play-testers on throwaway mock-ups (10 candidates, then
finalists, then new fights after the bite when the winner, bite-then-keep-the-fish-in-a-box, turned out to be
Stardew Valley's; the owner picked "bite, then catch the leap").

- **Fishing spots** in Tiled: objectClass `FISHING_SPOT` (`map.TileFishingSpot`, only a mark). Level 1 has one at the end of
  a little wooden pier on its left shore (the owner's), (2,6), with water two tiles to its left. No grass grows on a fishing spot.
- **The rod:** standing still on a fishing spot, the HUD's tool button shows the fishing rod (the Ocean Pack's
  `fishing_rod_2.png`).
- **Cast and bite** (`feature.FishingFeature`, feature flag `FISHING`): the rabbit casts (always to the left for now)
  and the camera centres on it (so the bobber and the paw are on screen); the bobber (`actor.Bobber`, the Ocean
  Pack's bobber and splash frames) drops into the water two tiles away with a splash, bobs 1–5 times at random, and
  is pulled under with a splash. The rabbit's rod shakes, the face says "!!!" and plays its happy animation, and a
  paw points at the place, pulsing, next to a circle emptying in 2 s (`ui.fishing.BitePrompt`; the settings gear's
  hold circle backwards). A tap anywhere in time hooks the fish; otherwise it is gone and the face is sad
  (`TEEMO_SAD`, the emotes' row 24).
- **The fishing game** (`ui.fishing.LeapGame` draws `LeapRound`'s rules): a window like the settings panel (no title,
  no check mark) with a row of water tiles at the bottom and a green zone at the top. The hooked fish (the Ocean
  Pack's `mediuml fish.png`, turning in the air) leaps again and again, some leaps into the green zone, some below
  it. A tap while it is in the green zone catches it (a fish on the right: orange, green, blue from the bottom up; the
  face happy), a tap while it is elsewhere in the air, or a green leap nobody taps, is a miss (a pink X on the left;
  the face sad); taps between leaps do nothing; one result per leap. Three catches win, three misses lose; at most
  15 leaps, at least 6 into the green zone, so doing nothing loses. Won: the rabbit pulls the line out, the face puts
  its sunglasses on (`TEEMO_COOL_ON`, rows 19 and 22), and a fish of a random colour (`actor.Fish`, the Ocean Pack's
  `Fish Sprites.png`) goes into the backpack, or next to the rabbit when it is full.
- **One difficulty number** (`FishingFeature.setLevel`, 0..1, 0.15 as it ships, the age-3 level of the mock-ups;
  later from the age setting): the leaps' speed, the green zone's size, how close the short leaps come to it, the
  pauses.
- **The rabbit fishing** is a sheet of its own (`rabbit_fishing`: its frames are cut to another box than the
  rabbit's sheet): `AnimationPlugin.pose` holds a pose from another sheet of the same character (looping, or once and
  then still) until it ends or the rabbit walks off, and `GameActor.fitAvatar` sizes and places the picture for that
  sheet. `Player.fish` / `stopFishing`. `Hud.say(text, picture, seconds)` is public (the "!!!").
- Assets: `pack.json` (`rabbit_fishing`, `fishing_bobber`, `fishing_rod_icon`, `fish_leap`, `fish_catch`,
  `water_tile`; the Ocean Pack's other files ignored for now), `ui.json` (`marks/miss`, the pink X), `UiImage`
  (`PAW_POINTING`, `MISS`, `TEEMO_SAD`, `TEEMO_COOL_ON`, `TEEMO_COOL`). No golden changed.
- Tests: `FishingTest` (the rod only on the spot, cast, bobber, bite, no tap in time, winning a fish, doing nothing
  loses, a miss below the green zone and taps between leaps, walking off, the flag off, level 1's spot).
  `GameDriver`: `fishing`, `bobber`, `fishingSpots`, `biteSecondsLeft`, `tapAnywhere`, `fishingGame`, `fishInGreen`,
  `fishLeaping`, `fishingLevel`. Looked at every step in a hidden window.
- **Play-tested by Haiku agents** (owner's request; each in its own copy of the repo, through `GameDriver` scenarios
  and hidden-window screenshots). Fixed from round 1:
  - a caught fish vanished when the backpack was full and no tile around was free (75 of 82 catches in a long run):
    it lands at the rabbit's feet then (`aCaughtFishIsNeverLost`);
  - the rabbit's rod ended a tile short of the bobber: a thin line goes from the rod's tip to the bobber
    (`Bobber.lineFrom`), and goes under with it at the bite;
  - on a phone the bobber could land off screen: casting centres the camera on the rabbit (`LookAtMeEvent`);
  - a 3-year-old's tap (0.75 s late) landed at the very end of the green zone: green leaps go higher at low levels
    (`LeapRound`'s lowest green top 0.92 at level 0, down to just above the zone at 1), about 1 s in the green at
    0.15;
  - the leaping fish was tiny: drawn at 8× (128 px), the paw at 7×; the dim behind the game is lighter (40 %) so the
    face's reactions show; the window closes in the same frame as its result (the next tap isn't swallowed).
  Fixed from round 2:
  - the difficulty was flat (5- and 8-year-olds won every game, a 1.2 s 3-year-old never): the leap's speed and the
    green zone go through three points instead of a straight line (`LeapRound.byAge`: at 0.15 / 0.45 / 0.8 a leap of
    2.2 / 1.4 / 1.1 s, the green zone from 0.58 / 0.75 / 0.82 up). Simulated children: 3-year-olds (0.75–1.2 s
    reaction) win 100 %, 5-year-olds 87 % (1.1 misses a game), 8-year-olds 77 % (1.5);
  - the rod left the tool button while fishing: it stays there the whole time (tapped then, nothing happens);
  - the leaping fish is drawn at 10×, the line 8 px thick.
  Left to the owner: a child tapping at random in the game loses about half the time (each tap with the fish in the
  air but not in the green zone is a miss); a tap on the grass while waiting walks off and takes the line out; won
  fish pile up on the shore when the backpack is full of crops; a cow may stand on the spot for a while.
- Owner's changes after trying it (2026‑11‑17): the green zone is half as tall (harder: the fish is in it about half
  as long at every level; `LeapRound` green from 0.79 / 0.875 / 0.91); two rows of rippling water (the tiles now
  animate); the catches and the Xs a bit higher above the water; while the fish is in the green zone the paw points
  at it, pulsing, as at the bite; some low leaps are quick (20 / 35 / 50 % of them at 0.15 / 0.45 / 0.8, in 55 % of
  a leap's time: low leaps aren't always the slow ones); the window is taller (920); with a full backpack and no free
  tile next to the rabbit the fish isn't collected (no pile at its feet). The owner moved level 1's spot onto a little
  wooden pier, (2,6): the camera centres on the rabbit only as far as the map's edge allows (`LookAtMeEvent` may
  carry a point), so no black shows beyond it.
- **Rebuilt on master after the doors (2026‑11‑18, owner's request)**, as one commit instead of a merge: the game had
  become several maps with two kinds of feature meanwhile, so fishing now fits that instead of the old `GameScreen`.
  - `FishingFeature` is a `PlayerFeature`, made in `Area.startFeatures`: only on the player's map, and a map can switch
    it off (`featuresOff=FISHING`). Gone to another map, `playerLeft()` takes the line out of the water, removes the
    bite's paw and the fishing game from the shared UI and puts the rod away.
  - The rod follows the tool button's rule: it shows only over the hint and resets only itself (the door and the
    scythe do the same).
  - Grass doesn't know about fishing any more: a `TileObject` can keep its tile clear (`keepsClear()`, the fishing
    spot does); `GrassFeature` asks that instead of `FishingFeature.isFishingSpot` (removed).
  - `BitePrompt` and `LeapGame` cover what their stage shows by themselves (`ui.fishing.ScreenArea`), so
    `FishingFeature` no longer works out the screen (and stays under PMD's import limit).
  - `Hud.say(text, picture, seconds)` is made public instead of adding an overload: `Hud` stays under PMD's God Class
    limit, the problem #74 was opened for (closed).
  - `TileObjectFactory`: objects with no properties (the player's start, the dog's, a fishing spot) are made in
    `marker` (keeps the switch under PMD's complexity limit with `DOOR` and `FISHING_SPOT`).
  - Level 1 has both the coop's door and the pier with its fishing spot (object 21); the assets were rebuilt with
    `build_assets.py`.
  - Tests: `FishingTest.aMapCanSwitchFishingOff`; `TestMap.withMapProperty`. No golden changed.
- **Longer wait, quicker bite (2026‑11‑18, owner's playtest)**: the bobber now bobs for 3–10 s at random
  (`LEAST_WAIT_SECONDS`, `MOST_WAIT_SECONDS`, whole bobs) instead of 1–5 bobs, which was too short; after the bite
  the player has 1.2 s to tap instead of 2 s, which was too long (`BITE_SECONDS`).
- **No fish sometimes, and a new fishing game (2026‑11‑18, owner's playtest)**: the leaping game wasn't challenging,
  funny or configurable.
  - About 30 % of casts get no fish (`FishingFeature.NO_FISH_CHANCE`): the bobber bobs the whole 10 s, then the line
    comes out with the sad face, as when a fish gets away. Tests switch it off (`GameDriver.fishingNoFishChance`), but
    `sometimesNoFishComes`.
  - The fishing game is now a rhythm game (`ui.fishing.RhythmGame`, rules in `RhythmRound`): in the same window, fish
    swim down three lanes onto a line, each reaching it on a half-second beat (12 fish in an 8 s run, one beat or a
    few apart, never three down one lane). A tap on a lane's net button (or anywhere in its lane) while its fish is on
    the line catches it (it grows, turns a little green and is gone, the line flashes); a fish past the line is a
    miss (a pink X on the left), three lose at once. The owner asked for exactly this layout and feel.
  - Decided here: a tap with nothing on the line tangles that net for a moment (it shakes, greys, catches nothing),
    so tapping all nets all the time doesn't win, without counting as a miss (kinder to a 3-year-old); the line pulses
    on the beat; a pale band around the line shows where a tap catches.
  - Configurable: `RhythmRules` holds every number (run, beat, fish, lanes, misses that lose, fall time, catch window,
    tangle time); `RhythmRules.forLevel` gives them for the levels of 3-, 5- and 8-year-olds, `FishingFeature.setRules`
    sets them by hand.
  - The fishing net's icon isn't in the packs: `tools/assets/net_icon.py` draws it (16x16, the item icons' palette),
    packed as `Sprite.FISHING_NET_ICON`.
  - `LeapGame` and `LeapRound` are removed (replaced, the owner's call); `Sprite.FISH_LEAP` stays, unused.
  - Tests: `FishingTest` plays the new game (`GameDriver.fishOnTheLine`, `tapNet`, `netTangled`), new
    `RhythmRoundTest`. No golden changed.
- **Play-tested by Haiku agents** (owner's request; 3 rounds, 9 testers, each in its own copy of the repo: simulated
  3-, 5- and 8-year-olds and cheaters on `RhythmRound`, `GameDriver` scenarios, hidden-window screenshots). Fixed:
  - tapping every net all the time won 100 % at the shipped level (a tangle was shorter than the catch window): a tap
    on a tangled net now tangles it again from then; a tap within 0.25 s after a catch in the same lane does nothing
    (a child's double tap mustn't cost the next fish);
  - the ages were off (a simulated 5-year-old won 11 % at its level, an 8-year-old 49 %, a 3-year-old under 10 %):
    window 0.4 / 0.28 / 0.14 s and tangle 1.0 / 0.9 / 0.8 s at 0.15 / 0.45 / 0.8 (fall unchanged). Simulated (the
    models are guesses, not children): 3-year-olds win 25-60 %, 5-year-olds 63-74 %, 8-year-olds 84 %; no timing-blind
    cheat wins over 10 % (the best, tapping the next fish's lane every 0.6 s, needs watching the fish: 17 % at 0.15);
  - the bite's paw and the game's window stayed where they were when the screen was turned (a part uncovered, a tap
    there didn't hook): both follow the stage every frame, the panel in its own group in the middle
    (`aResizedScreenStaysCovered`);
  - a tap in the dim above or below the panel worked a lane: only taps on the panel count;
  - looks: clearer fish (yellow, blue and a clownfish from the sheet's fourth row, `Action.SWIM`), side-on facing right (head first they read as blobs), bigger;
    the net redrawn side-on with a long handle (it read as a bucket); a tangled net really goes dark
    (`RoundButton.tint`: a group's colour doesn't reach its pictures); the line with a dark edge and a deeper pulse, the
    catch zone a darker band clipped at the water; narrower miss column, wider lanes; a caught fish grows less (1.15)
    and its sparkle and flash stay in its lane, behind it.
  - Left as it is: a stray tap costs that lane's next fish (the tangle; ignoring early taps would let tapping the next
    fish's lane all the time win); landscape windows make the panel small.
  - For the owner to decide (round 3, ten minutes on level 1): the backpack's two slots fill after two fish, and later
    fish aren't collected; fish have no use yet; the no-fish cast (about 11 s, then the sad face) and missed fish
    punish without teaching, maybe a gentler face; and, not about fishing, a tap far up the narrow strip north of the
    pier doesn't walk the rabbit (one tile at a time does).
- **Owner's observations after trying it (2026‑11‑18)**:
  - the bobber is drawn at three quarters of the pack's size (it was too big next to the rabbit);
  - casting sends one plain `LookAtMeEvent` for the rabbit: the camera centred on a point kept off the map's left
    edge, and when that centring ended, tracking pulled it back towards the rabbit, the slight move as the bobber
    landed (`FishingFeature.cameraPoint` removed, replaced as the owner asked; the event's point stays for others);
  - the X column shows a shadow for each X there can be (as many as the misses that lose), so it is clear how many
    mistakes are left;
  - the nets always work: no tangling any more (a tangled net couldn't catch the next fish, or try again), so
    `RhythmRules.tangleSeconds`, the double-tap rule and `RoundButton.tint` (only for the dark tangled net) are gone;
    a tap with nothing on the line only presses the net.
  - the bite's paw no longer points at the bobber: bigger (12x), in the bottom right of the screen, its finger a little
    right of and below the screen's middle (the rabbit), the circle under it; it re-centres when the screen is turned.
  - then: the paw closer to the middle; a tap too early, while a fish of that lane is in sight but not on the line,
    scares it off: it turns red and jumps out to the side, a miss (so tapping all the time loses again; a tap with no
    fish of that lane in sight does nothing); a fish passing the line doesn't wait at the water's bottom any more: it
    swims on, turning red, and is gone there (no jump).
  - then: no fish at all ends with the face sweating instead of sad; a caught fish no longer just appears in the
    backpack: it leaps out of the water where the bobber went under and flies to the rabbit in an arc, turning and
    growing a little (0.7 s, `ui.fishing.CatchFlight`, on the game stage), and goes into the backpack when it gets
    there.
  - then: a won game celebrates before its window closes (falling stars were tried first and didn't look good): the
    fish the player gets (chosen when it is hooked, so the same one then flies to the rabbit) pops up big in the
    window's upper middle with a little bounce and a wobble, and 14 sparkles (`UiImage.SPARKLE`, 5-10x, about twice a
    chest's) burst out of it all around, turning, shrinking and fading; the window stays 1.8 s after a win (0.8 s after
    a loss).
  - then: a bite with no tap in time shows the fish getting away: it leaps out of the water where the bobber went
    under almost straight up, head first, turns over at the top and drops back in head down (a fish of the fishing
    game, yellow, blue or a clownfish, moved by scene2d; the owner didn't want the pack's leaping fish), fading as it
    goes under, 1.2 s (`ui.fishing.GotAwayLeap`, made through `CatchFlight.gotAway` to keep `FishingFeature` under
    PMD's import limit).
  - then (2026‑11‑19): the fishing game's window sits in the middle of the screen above the HUD's bottom row (the
    face, the backpack: `Hud.getRow().getTop()`, passed as the game's `floor`), not over it, and the dim stops there,
    so the face's reactions show bright; taps on the HUD under it still don't get through. The panel is 960 tall
    (was 1000) so it has some room above and below on the smallest screen.
  - then: the bite's "tap now!" is a hand pointer instead of the pack's pointing cat paw (the owner found it weird):
    the UI pack has no hand, so `tools/assets/hand_pointer.py` draws one in the style of its mouse cursors
    (`UiImage.HAND_POINTING`, its finger tip `HAND_TIP_X`), the finger up, its tip just under the screen's middle (the
    rabbit), pulsing from the tip, the emptying circle under it. `UiImage.PAW_POINTING` stays, unused.
  - then: the hand is the clock too, the emptying circle under it is gone: white at first, it turns grey from its
    finger's tip down as the 1.2 s pass, a row of its pixels at a time (a grey copy under a white one cut shorter from
    the top). `UiImage.HOLD_CIRCLE` stays for the settings gear.
- **After the owner's PR review** (same day):
  - `RhythmRules`' age levels are fishing's own stand-in: a comment says that whoever builds the game's age
    (years-by-level) system, wanted in many places, must rework them completely or at least make them generic.
  - `RhythmRound.Fish` (and `State`, `getFish`, `tap`, `update`, `progress`) are package-private, so they don't clash
    with the item `actor.Fish`; `RhythmRoundTest` moved to the game's package (`e2e/test/.../ui/fishing`),
    `GameDriver` counts the fish with `getFishCount()`.
  - `Hud.say`'s comment no longer mentions fishing (the HUD knows nothing of it).
  - Kept, the owner's call: the `FISHING` feature flag, and `FishingFeature`'s public getters for tests (the pattern
    of the other features; `GameDriver` sits in another package).

## 2026‑11‑19, day and night (branch `day-night`)

- Owner's request, written anew (the `day-and-night` branch was the owner's proof of concept; its night-only lamps
  are left for later: the map just gets dark). Kept simple: no feature flag, no lights.
- **One clock for the whole game** (`light.DayClock`, ticked by `GameScreen` before the maps): day, dusk, night, dawn,
  again and again, from the morning; every map has the same time.
- **Settings in one place** (owner asked where): `light.DayNightSettings`, a small class next to the clock, `GAME` as
  the game ships: 3 min of day, 8 s of dusk, 40 s of night, 8 s of dawn, the night a quarter as bright as the map's
  daylight. Code rather than a file in assets or a Tiled property: one place to edit, checked by the compiler, works
  in GWT, and tests set short days with `DayNightSettings.use` (`GameDriver.dayAndNight`).
- **Light**: each map's `feature.DayNightFeature` sets its ambient light to the clock's part of the map's daylight
  (Tiled's `ambientLight`; smooth at dusk and dawn, `Interpolation.smooth`). The house (no `ambientLight`, lit by its
  lamps) stays as it is; the coop (0.55) dims too. The HUD stays bright.
- **Events** (owner's request): `light.NightStartedEvent` when the sun starts to set, `light.DayStartedEvent` when it
  starts to rise (the owner wanted the animals up then), posted by each map's `DayNightFeature` on its own bus, living
  one frame. Decided: the night starts at dusk, not when it is fully dark, so the animals settle down as it darkens.
- **Animals sleep the night** (`feature.BedtimeFeature` hears the events, tells every `FarmAnimal` on its map,
  `FarmAnimal.setNight`; carried through doors in its `Shared`): the cow and the chickens finish what they are doing,
  lie (sit) down and go to a new state, `SLEEPING_THE_NIGHT`, in `CowState` and `ChickenState`: the sleep animation
  and "zzz" over it (`actor.Zzz`, the UI pack's emoji, `UiImage.ZZZ`, gently bobbing; on the notification stage, over
  the map's lights, so it stays bright). Nothing wakes them at night: no stroke all night (`canBeStroked`), so no
  action button over them; a wishing animal drops its wish (the bubble goes) and wishes again in the morning; a
  chicken going for a crop goes back. At dawn they get up. A nap by day is as before.
- Tests: `DayNightTest` (the light through a day, the house, the animals sleeping and waking with no button, a
  wishing cow, the coop's chicken while the rabbit is on the farm), `DayClockTest`; `GameDriver.dayAndNight`,
  `dayAndNightAsInTests`, `timeOfDay`, `zzzOver`, `animalStateOn`, `cellOn`, `sleepsAwayOn`. No golden changed.
- Looked at it (throwaway screenshots): the farm at day, dusk and night, "zzz" by the heads of the cow, the chicken and
  the chick.
- **Chickens sleep in the coop** (owner's request, then the owner's pick: back out in the morning):
  - The farm's door to the coop has a new Tiled property, `bedtime` = `CHICKEN` (`TileDoor.isBedtimeFor`): who goes
    through it to sleep. Each map's `BedtimeFeature` gives its chickens the map's `character.Roost` (that door, and
    the doors chickens may use).
  - At night a chicken walks to that door (one step at a time, at its pace, over free tiles) and goes through; in the
    coop it walks to a free tile at least two tiles (diagonally too) from the entrance, picked at random, then sits
    down and sleeps (`ChickenState.GOING_TO_BED`, the walking in `character.CoopTrip`). One already resting or napping
    gets up first. In the morning those that came in for the night walk back to the door and out, and on to a free
    tile at least two tiles from the door, picked at random (owner's request), before they go on with their day
    (`LEAVING_THE_COOP`; `CoopTrip` goes with the chicken through the door). The coop's own chicken sleeps where it is
    and stays. A map without such a door: they sleep where they are.
  - Decided: with someone in the way it waits 3 s, then gives up and sleeps where it is (or stays in the coop).
- **Tests run with hour-long days** (`GameDriver` sets them; `DayNightTest` its own short ones): the game's settings
  (the owner tried 10 s days) don't change what other tests see, as with the camera zoom.
