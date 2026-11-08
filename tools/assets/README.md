# Asset pipeline

`build_assets.py` turns the **Sprout Lands** pack into Tiled tilesets and game atlases. Run it whenever the pack
changes, or after editing a level in Tiled.

```sh
pip3 install pillow                                   # once
python3 tools/assets/build_assets.py "/path/to/Sprout Lands - Sprites - premium pack"
python3 tools/assets/build_assets.py                  # same pack as last time
python3 tools/assets/build_assets.py --maps           # only re-pack the levels (after editing them in Tiled)
```

## What it makes

| Where | What | Git |
|---|---|---|
| `assets-src/tilesets/*.tsx` | One Tiled tileset per sheet, 16×16, with terrains (Wang sets) and animated tiles | committed |
| `assets-src/tilesets/images/` | Copies of the pack's sheets (Tiled shows them) | **ignored** (license) |
| `assets-src/maps/*.tmx` | Your levels. `level1.tmx` is created once if there's no level yet | committed |
| `assets-src/lightgame.tiled-project` | Tiled project with the maps and tilesets folders | committed |
| `android/assets/maps/` | Game copies of the levels + `tiles.atlas`: **only the tiles the levels use** | committed |
| `android/assets/sprites/` | `sprites.atlas` with every animation from `pack.json`, `animations.json` | committed |
| `build/assets-preview/` | `index.html` (open in a browser): every animation playing, the levels, terrain overlays, warnings | ignored |

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
   (`PLAYER`, `CHEST`, `LIGHT`, `DOG`, `SPRITE`), as in `android/assets/map.tmx`.
6. Map property `ambientLight` (float, 0 = dark … 1 = daylight). Without it the level is dark, like the house.
7. Save, run `python3 tools/assets/build_assets.py --maps`, and start the game (`Main.DEFAULT_MAP`).

Tile layer format must be CSV or Base64 (uncompressed, zlib or gzip). libGDX can't read zstd.

## pack.json

It describes what's in the pack, so the tool doesn't need to be told again:

- **tilesets**: sheets you can paint with. `terrain` turns on terrain detection. Detection reads the tiles'
  transparency. Sheets that draw outlines into their inner corners borrow the layout of a clean cut-out sheet
  instead (`layoutFrom`). `wang` sets pieces by hand. `animations` makes animated tiles (the water).
- **sprites**: animation sheets cut into frames. `frame` is the frame size, `offset` where the grid starts, `rows`
  names the rows (`"auto"` gives `row_00`, `row_01`...). `duration` is ms per frame, `playMode` is `LOOP` or `NORMAL`
  (play once). `variants` lists colours with the same layout. Empty frames are skipped. In the game an animation is
  `sheet/row`, e.g. `rabbit/walk_left` → `resourcesService.getAnimation("rabbit/walk_left")`.
- **ignore**: previews and duplicates.

The rabbit's rows have real names. Animals and objects use `row_NN` for now: check the GIFs in
`build/assets-preview/animations/` and rename rows in `pack.json` when you need one.

**When the pack changes:** run the tool. Images that `pack.json` doesn't mention are listed as warnings, along with
missing files, empty named rows and terrains that can't be detected reliably. The same list is in
`build/assets-preview/report.txt`.

## License

Sprout Lands is by Cup Nooble. The pack itself may not be redistributed, so its sheets never go into git (see
`.gitignore`). The game's packed atlases are allowed, with credit: see `CREDITS.md`.
