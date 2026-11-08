# lightgame

A libGDX game (desktop, Android, HTML). Run it with `./gradlew desktop:run`.

## Assets

| Directory | What | Git |
|---|---|---|
| `android/assets/` | Everything the game loads (all platforms read it) | committed |
| `android/assets/maps/`, `android/assets/sprites/` | Generated: the levels, `tiles.atlas`, `sprites.atlas`, `animations.json` | committed |
| `assets-src/` | Tiled project, tilesets (`.tsx`) and the levels you edit | committed |
| `assets-src/tilesets/images/` | Copies of the Sprout Lands sheets, for Tiled only | **ignored** (license) |
| `tools/assets/` | The asset tool and `pack.json` (what's in the pack) | committed |
| `build/assets-preview/` | Preview page for checking the output | ignored |

The Sprout Lands pack lives outside the repo. Its license forbids committing it, so only the packed atlases are
committed, with the credit in `CREDITS.md`.

### Working with assets

```sh
pip3 install pillow                                              # once
python3 tools/assets/build_assets.py "/path/to/Sprout Lands pack"  # first run, or when the pack changes
python3 tools/assets/build_assets.py --maps                      # after editing a level in Tiled
```

- **Levels:** open `assets-src/lightgame.tiled-project` in Tiled, edit the maps in `assets-src/maps/`, save, then
  run `--maps`. Never edit `android/assets/maps/` by hand.
- **New graphics:** run the tool on the pack and fix any warnings it prints by updating `tools/assets/pack.json`.
- **Check the result:** open `build/assets-preview/index.html` in a browser.
- **Fresh clone:** run the tool once with the pack path, so Tiled can find the images.

Details: [tools/assets/README.md](tools/assets/README.md).
