# Asset pipeline

`build_assets.py` turns the **Sprout Lands** pack into Tiled tilesets and game atlases. Run it whenever the pack
changes, or after editing a level in Tiled.

```sh
pip3 install pillow                                   # once
python3 tools/assets/build_assets.py "tools/assets/Sprout Lands - Sprites - premium pack"   # first run
python3 tools/assets/build_assets.py                  # same pack as last time
```

The tool isn't part of the Gradle build: the game uses its committed output, so building never needs the pack or
Python.

If you forget to run the tool after saving a level or editing `pack.json`, `./gradlew e2e:test` fails
(`AssetsUpToDateTest`): the generated files record the SHA-1 of their sources.

## What it makes

| Where | What | Git |
|---|---|---|
| `assets-src/tilesets/*.tsx` | One Tiled tileset per sheet, 16×16, with terrains (Wang sets) and animated tiles | committed |
| `assets-src/tilesets/images/` | Copies of the pack's sheets (Tiled shows them) | **ignored** (license) |
| `assets-src/maps/*.tmx` | Your levels. `level1.tmx` is created once if there's no level yet | committed |
| `assets-src/lightgame.tiled-project` | Tiled project with the maps and tilesets folders | committed |
| `android/assets/maps/` | Game copies of the levels + `tiles.atlas`: **only the tiles the levels use** | committed |
| `android/assets/sprites/` | `sprites.atlas` with **only the sheets in the game's `Sprite` enum**, `animations.json` | committed |
| `core/.../resources/Action.java` | Enum of the actions named in `pack.json` (`WALK`, `IDLE`, `OPEN`...) | committed |
| `build/assets-preview/` | `index.html` (open in a browser): every animation of the pack playing, the levels, terrain overlays, warnings | ignored |

Generated files are rewritten only when they change. Don't edit `.tsx` files by hand; change `pack.json` and run the
tool again.

## Making levels in Tiled

1. In Tiled: *File → Open Project* → `assets-src/lightgame.tiled-project` (the Project panel lists all maps and
   tilesets), then open `maps/level1.tmx`, or create a new map there (orthogonal, 16×16 tiles, fixed size).
   Edit the maps in `assets-src/maps/`, never the generated copies in `android/assets/maps/`.
2. Add tilesets from `assets-src/tilesets/` (*Map → Add External Tileset*).
3. Paint with the **Terrain Brush** (the Terrains panel): grass, soil, stone, bushes, tilled dirt... pick the right
   edges and corners by themselves. Plain fill tiles come with rare variations (flowers, tufts).
4. Layers, from the bottom: any number of tile layers (water, ground, decorations...) are drawn on top of each other.
   Two layer names mean something to the game:
   - `collision`: cells with any tile here can't be walked on (the layer is never drawn: keep it hidden);
   - `walls`: like `collision`, and it also blocks light. It is drawn if visible.
5. The `objects` layer holds the player and other objects: an **ellipse** with a property `objectClass` (`PLAYER`,
   `CHEST`, `COW`, `LIGHT`, `DOG`, `SPRITE`). Add it as a property of type `ObjectClass` and pick the value from the
   dropdown. The tool fills the dropdown from the game's `TileObjectType` enum and turns `objectClass` properties typed
   as plain text into it. Tiled reads the project only when it opens it: after a new `TileObjectType` constant, run the
   tool and reopen the project (or restart Tiled).
6. Map property `ambientLight` (float, 0 = dark … 1 = daylight). Without it the level is dark, like the house.
7. Save, run `python3 tools/assets/build_assets.py` (under a second), and start the game (`Main.DEFAULT_MAP`).

Mirrored (X, Y) and rotated (Z) tiles, layer opacity and layer offsets look in the game as in Tiled. Maps must have a
fixed size (not infinite) and 16 px tiles; the tool or the game says so otherwise. Tile layer format must be CSV or
Base64 (uncompressed, zlib or gzip). libGDX can't read zstd.
The tool deletes game copies in `android/assets/maps/` whose level was removed from `assets-src/maps/`, so never put
hand-made maps there.

## pack.json

It describes what's in the pack, so the tool doesn't need to be told again:

- **tilesets**: sheets you can paint with. `terrain` turns on terrain detection. Detection reads the tiles'
  transparency. Sheets that draw outlines into their inner corners borrow the layout of a clean cut-out sheet
  instead (`layoutFrom`). `wang` sets pieces by hand. `animations` makes animated tiles (the water).
- **sprites**: animation sheets cut into frames. `frame` is the frame size, `offset` where the grid starts, `rows`
  names the rows (`"auto"` gives `row_00`, `row_01`...). `duration` is ms per frame, `playMode` is `LOOP` or `NORMAL`
  (play once). `variants` lists colours with the same layout. Empty frames are skipped. Name rows `action` or
  `action_direction` (`open`, `walk_left`): every action becomes a constant of the generated `Action` enum.
- **ignore**: previews and duplicates.

The rabbit's rows have real names. Animals and objects use `row_NN` for now: check the preview page and name rows in
`pack.json` when you need them.

## Sprites in the game

Only the sheets listed in `core/src/com/mantkowicz/light/resources/Sprite.java` are packed (the license allows what the
game uses, not the whole pack). To use another sheet, e.g. the brown cow:

1. Add `COW_BROWN("cow_brown")` to `Sprite` (a typo stops the tool with a "did you mean" hint).
2. Name the rows you need in `pack.json` (e.g. `walk_down`...) and run the tool.
3. In the code: `resourcesService.getAnimation(Sprite.COW_BROWN, Action.WALK, Direction.DOWN)`, or without a direction
   for rows like `open`. No animation names are typed in Java.

A sheet in `Sprite` that wasn't packed, or a missing animation, stops the game at start with a message saying what to
do, and `AssetsUpToDateTest` fails.

**When the pack changes:** run the tool. Images that `pack.json` doesn't mention are listed as warnings, along with
missing files, empty named rows and terrains that can't be detected reliably. The same list is in
`build/assets-preview/report.txt`.

## License

Sprout Lands is by Cup Nooble. The pack itself may not be redistributed, so its sheets never go into git (see
`.gitignore`). The game's packed atlases are allowed, with credit: see `CREDITS.md`.
