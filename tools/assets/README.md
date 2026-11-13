# Asset pipeline

`build_assets.py` turns the **Sprout Lands** packs (sprites and UI) into Tiled tilesets, game atlases and fonts. Run it whenever the pack
changes, or after editing a level in Tiled.

```sh
pip3 install pillow                                   # once
python3 tools/assets/build_assets.py "tools/assets/Sprout Lands - Sprites - premium pack"   # first run
python3 tools/assets/build_assets.py                  # same packs as last time
python3 tools/assets/build_assets.py --ui "/path/to/Sprout Lands - UI Pack - Premium pack"   # if it isn't in tools/assets
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
| `android/assets/ui-pack/` | `ui.atlas` with **only the UI pieces in the game's `UiImage` enum**, bitmap fonts of `UiFont`, `ui.json` | committed |
| `core/.../resources/Action.java` | Enum of the actions named in `pack.json` (`WALK`, `IDLE`, `OPEN`...) | committed |
| `build/assets-preview/` | `index.html` (open in a browser): every animation of the pack playing, every UI piece and font, the levels, terrain overlays, warnings | ignored |

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
5. The `objects` layer holds the player and other objects: an **ellipse** with a string property `objectClass`
   (`PLAYER`, `CHEST`, `COW`, `LIGHT`, `DOG`, `SPRITE`), as in `android/assets/map.tmx`.
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
  names the rows: `"auto"` gives `row_00`, `row_01`...; a list names them in order (an empty name skips a row); a
  dict such as `{"0": "idle"}` packs only the rows it names. `duration` is ms per frame, `playMode` is `LOOP` or `NORMAL`
  (play once). `variants` lists colours with the same layout. Empty frames are skipped. `anchor: [x, y]` is the
  frame's pixel (from its top left) drawn on the tile's middle, for sheets that draw their character away from the
  frame's middle: the cow stands low in its 32x32 frame, so `[16, 21]` (its body's middle). Default: the frame's middle. Name rows `action` or
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

## UI pack

The Sprout Lands UI pack (buttons, dialog boxes, icons, emotes, fonts) is a second pack. The tool finds it in
`tools/assets/` (a folder with "UI Pack" in its name) or takes it once with `--ui "/path/to/pack"`, and remembers it in
`tools/assets/.ui-pack-path`. `tools/assets/ui.json` says how its sheets are cut; every image of the pack is either in
it or ignored on purpose (others are listed as warnings).

- **images**: one entry per sheet. `file` is a path in the pack, or a glob: then every matching file is a piece of its
  own, named `prefix` + its file name (`backdrops_a1`, `mouse_catpaw_mouse_icon`).
  - nothing more: the whole image is one piece, named `name` (`dialog_box`);
  - `"frame": [w, h]`: a grid, pieces `name/000`, `name/001`... row by row (empty cells skipped, numbers kept).
    `"offset": [x, y]` is where it starts; `"columns"` and `"rowCount"` limit it to a few cells of a big sheet (the
    stamina bar's segments, found only on `ALL UI ASSETS on one sheet.png`);
  - `"pieces": "auto"`: every separate shape is a piece, numbered in reading order. `"gap": n` joins shapes closer
    than n px (the letters of a word). Shapes that touch come out as one piece: use a grid then;
  - `"names": {"17": "heart"}` renames pieces (`icons/heart`), so code doesn't depend on numbers;
  - `"split": [left, right, top, bottom]`: a nine-patch. The borders keep their size, the middle stretches. For every
    piece of the entry, or `{"piece": [...]}` for some. The preview shows it stretched to twice its size;
  - `"rotate": 90` turns the image counter-clockwise (90, 180 or 270) before cutting it, e.g. the dialog box with its
    pointer at the bottom (`dialog_box_down`);
  - `"animation": {"duration": ms, "playMode": "LOOP" or "NORMAL"}` with a `frame` grid: every row is an animation,
    `name` itself when there's one row, else `name/row_00`... (or name them with `rows`, as in `pack.json`).
  Pieces are trimmed to their visible pixels; an animation's frames share one box, so they don't jump.
- **fonts**: TTF fonts turned into libGDX bitmap fonts (`.fnt` + `.png`) at their pixel size: the smallest size with
  sharp edges (8 or 9 px for the small ones, 18 for the 8x14 one); `size` forces one. Only ASCII: the pack's fonts
  have nothing else (lower case letters are drawn as capitals). Glyphs are white, so labels can tint them. Scale them
  by whole numbers to keep them sharp.
- **ignore**: previews, examples, and sheets that only repeat single files kept as pieces (e.g. `Selectorst.png`).

Only what the game lists is packed: pieces in `core/.../resources/UiImage.java` (`DIALOG_BOX("dialog_box")`) go into
`android/assets/ui-pack/ui.atlas`, fonts in `UiFont.java` (`PIXEL_8X14("pixel_8x14")`) into `android/assets/ui-pack/fonts/`.
`android/assets/ui-pack/ui.json` records frames, frame durations, nine-patch borders and fonts for the game, and the SHA-1 of
`tools/assets/ui.json` (`AssetsUpToDateTest` checks it). In the game: `resourcesService.getUiImage`,
`getUiNinePatch`, `getUiAnimation`, `getFont(UiFont)`. A name that isn't in ui.json stops the tool with a "did you mean"
hint; a piece the game lists but wasn't packed stops the game at start.

## License

Both Sprout Lands packs (sprites, UI) are by Cup Nooble, under the same terms. The packs themselves may not be
redistributed, so their sheets and TTF fonts never go into git (see `.gitignore`); the preview page stays in `build/`.
What the game uses is allowed in the repo, with credit and the license terms: the packed atlases and the bitmap fonts
made from the TTF fonts. Keep it that way: pack only the sheets, pieces and fonts the game uses (the `Sprite`, `UiImage` and
`UiFont` enums; a sheet in `Sprite` is packed with all its rows), never sheets for later. See `CREDITS.md`.
