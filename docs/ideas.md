# Ideas

Possible improvements, not planned. Each has the date it came up, the problem, a suggested fix and when it would be
worth doing. When one gets done, remove it here and describe it in [changelog.md](changelog.md).

- **Squash and stretch for the hops** (2026‑11‑07). `HopPlugin` could also scale the avatar: a little taller and narrower at the top
  of the arc, flatter and wider just before and after touching the ground. The origin of the scaling would be the bottom centre of the picture. It's
  purely visual, like the hop itself: positions, tiles and goldens stay the same, and `HopTest` would only need a check on the scale.
- **Draw only what the camera sees** (2026‑11‑08). `Board.draw` draws every tile each frame, also the ones off screen; each
  tile draws all its painted map layers. Cheap for small maps (24x16 with ~4 painted layers per cell is ~1,500 drawings per
  frame, all from one atlas texture), but a 100x100 map would be ~40,000, which costs frame time on mobile and in the browser.
  Fix: draw only the tiles inside the camera's view (about 7x11 tiles on screen), so any map stays around ~1,000 drawings. Do
  it once maps grow beyond roughly 50x50. Pixel output for visible tiles stays the same.
- **Map layers above the characters** (2026‑11‑08). All map layers are drawn under the actors (the board is drawn first),
  so tree canopies or roofs painted on a layer end up under the rabbit. Add a layer property in Tiled, e.g. `above = true`:
  the loader keeps those layers apart and the game draws them after the actors (still per tile, still from the atlas).
  Do it before painting trees or roofs that the player should walk behind.
- **Retire the old single-texture pictures** (2026‑11‑08). Maps made with tools/assets draw everything from the atlases
  (their tiles are built without a background: `TileFactory.createMapTile`). The old pictures (`floor-tile.png`,
  `wall-tile.png`, `player.png`, `dog.png`, `chest.png`, …, preloaded in `ResourcesService`) are still used by the
  original house map `android/assets/map.tmx` (e2e tests no longer run on it, only `ShippedMapsTest` loads it, and the
  board tests place a few furniture pictures through `TestMap.with`). To remove them: rebuild the house as a level in
  Tiled (or drop it), then delete the pictures, their preload list and the background constructors of
  `FloorTile`/`WallTile`. Do it once the atlases feel settled.
- **Map roles from Tiled custom types instead of layer names** (2026‑11‑08). Today the game reads roles from names typed
  by hand in Tiled: layers called `collision` (not walkable, never drawn) and `walls` (not walkable, blocks light), and
  the map property `ambientLight` (`TmxTiledMapLoader` constants). Problems: a typo or rename (`Collision`, `walls 2`)
  silently turns the layer into a plain drawn one; blocking things must be painted twice (once to see, once on
  `collision`); only one layer per role. (`atlas` is fine: the tool writes it, nobody types it.)
  Suggested fix: the tool defines custom types in `assets-src/lightgame.tiled-project` (Tiled 1.9+): a layer class
  `GameLayer` with checkboxes `blocksWalking` and `blocksLight`, and a map class `GameMap` with `ambientLight` (float,
  default 1). In Tiled roles are picked from a dropdown and ticked, layers can have any name (`fences`, `rocks`), and any
  drawn layer can block (an invisible one still works for invisible barriers). The loader reads the properties instead of
  names; the tool warns about leftovers (a layer named `collision` without the class, unknown properties). Needs:
  migrating `level1`, `PackedMapTest` (its `LevelVariant` layers), the READMEs.
- **Animated objects placed from Tiled maps** (2026‑11‑08). Today sprite sheets reach the game only through the `Sprite`
  enum and actors written in Java (the rabbit). For animals or objects placed in a level: a map object in the `objects`
  layer with an `objectClass` (e.g. `ANIMAL`) and a `sprite` property holding a `Sprite` constant (e.g. `COW_BROWN`); the
  loader checks it with `Sprite.valueOf` and creates the actor. The tool already reads every map, so it can also check
  that each `sprite` value is in the enum (or pack those sheets too), and it can write the list of sheet names into
  `assets-src/lightgame.tiled-project` as a custom enum property type, so Tiled shows a dropdown instead of free text.
  Design it together with the first map-placed animal.
- **Door animation** (2026‑11‑08). `Tilesets/Building parts/door animation sprites.png` is only a tileset: its frames
  overlap (a 3px pillar plus the door leaf every 16px; the wide door the same with 48px), so no frame grid cuts them right
  (see the note in `tools/assets/pack.json`). To animate doors, `pack.json` needs frames given as rectangles
  (`"frames": [[x, y, w, h], ...]` per row, plus how to anchor frames of different widths), and `cut_frames` in the tool
  has to support them. Check the result on the preview page.
- **Row names out of `pack.json`** (2026‑11‑09). `tools/assets/pack.json` mixes two things: the pack's layout (files,
  frame sizes, offsets, variants, terrains), which changes only with a new version of the pack, and the names of
  animation rows (`idle`, `walk_left`, `open_front`…), which change whenever the game starts using another animation.
  So a file that reads as "describe the pack once" gets edited for game decisions: naming the cow's `idle` and `walk`
  and the chest's `open_front` for `Cow` and `Chest` did exactly that. Fix: move the row names to a game-owned file,
  e.g. `tools/assets/animation-names.json` (`{"cow": ["idle", "walk"], "chest": ["open_front", "open_side"], "rabbit":
  ["idle_down", …]}`), keyed by the `pack.json` entry, so the colour variants (`cow_brown`, `cow_pink`…) share their
  entry's names as they do now. The tool merges it with `pack.json` and warns about names for entries that don't exist;
  the output (`animations.json`, the `Action` enum) stays the same. Then `pack.json` is touched only when the pack
  changes, and a new animated object means its `Sprite` line, its row names in that file and, when it's placed from a
  map, a `SpriteActor` subclass with its `TileObjectType` line. Update the READMEs (step 2 of "Adding an animated
  sprite" in README.md, "Sprites in the game" in tools/assets/README.md, whose note that animals still use `row_NN` is
  already out of date) and the `_help` notes in `pack.json`.
