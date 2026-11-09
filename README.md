# lightgame

A libGDX game (desktop, Android, HTML). Run it with `./gradlew desktop:run`.

## Docs

- [docs/changelog.md](docs/changelog.md): what each feature branch changed, and why.
- [docs/ideas.md](docs/ideas.md): possible improvements, not planned yet.
- [docs/refactoring-plan.md](docs/refactoring-plan.md): the finished refactoring (analysis, decisions, the phases).
- [e2e/README.md](e2e/README.md): the headless regression tests; [tools/assets/README.md](tools/assets/README.md): the
  asset tool.

## Assets

The graphics come from the **Sprout Lands** pack by Cup Nooble. The pack itself stays out of the repo (its license
forbids redistributing it). A tool turns it into what the game and Tiled need, and only that output is committed.

### Setup (once per machine)

1. Install Pillow: `pip3 install pillow`.
2. Unzip the pack into `tools/assets/`, keeping its folder name (`tools/assets/Sprout Lands - Sprites - premium pack/`).
   That folder is git-ignored. Anywhere else works too, as long as it isn't committed.
3. Run the tool with the pack's path. It remembers the path for next time:
   ```sh
   python3 tools/assets/build_assets.py "tools/assets/Sprout Lands - Sprites - premium pack"
   ```

### Day to day

1. Open `assets-src/lightgame.tiled-project` in Tiled. Edit or add levels in `assets-src/maps/`.
2. Run the tool. It takes about a second and rewrites only what changed:
   ```sh
   python3 tools/assets/build_assets.py
   ```
3. Start the game. To check the graphics, open `build/assets-preview/index.html`: every animation, the levels, warnings.
4. Commit your changes together with what the tool changed (in `assets-src/`, `android/assets/`, `Action.java`).

Run the tool after any change to a level, to `tools/assets/pack.json`, to the pack, or to the list of sprite sheets
the game uses (`core/src/com/mantkowicz/light/resources/Sprite.java`: only those are packed). You don't need it for
other Java changes. If you forget, `./gradlew e2e:test` fails and tells you to run it.

### How the atlases are packed

The tool packs two atlases, each a single texture:

- **Tiles** (`android/assets/maps/tiles.atlas`): only the tiles your levels use, plus the frames of animated tiles
  like water. Paint with a new tile in Tiled, run the tool, and it's in.
- **Sprites** (`android/assets/sprites/sprites.atlas`): only the sheets listed in the `Sprite` enum
  (`core/src/com/mantkowicz/light/resources/Sprite.java`). `tools/assets/pack.json` says how each sheet is cut: frame
  size, row names, frame duration. Each row becomes one animation, e.g. `rabbit/walk_left`; empty frames are dropped.
  Frame durations and play modes go into `animations.json`.

The preview page shows every sheet in the pack, so you can pick what to use.

### Adding an animated sprite (e.g. a brown cow)

1. Find the sheet on the preview page (`build/assets-preview/index.html`), e.g. `cow_brown`.
2. In `tools/assets/pack.json`, name the rows you need: `action` or `action_direction`, e.g. `idle_down`, `walk_left`,
   `eat`. Unnamed rows are called `row_00`, `row_01`...
3. Add `COW_BROWN("cow_brown")` to the `Sprite` enum.
4. Run the tool. It packs the sheet and adds any new actions to the generated `Action` enum (e.g. `Action.EAT`).
5. Use it in code: `resourcesService.getAnimation(Sprite.COW_BROWN, Action.WALK, Direction.LEFT)`, or without a
   direction for rows like `eat`. Animations are never named by strings in Java.
6. To place it in a map: a `SpriteActor` subclass (like `Cow`) and its line in `TileObjectType` listing the sprites it
   can use, e.g. `COW(Cow::new, Sprite.COW_BROWN, Sprite.COW_PINK)`. In Tiled pick `COW` in the object's `objectClass`
   dropdown, and optionally a `sprite` (`COW_PINK`); the first one on the line is the default. Run the tool and reopen
   the project in Tiled (or restart it) to see new values in the dropdowns.

Mistakes are caught early:
- a typo in the `Sprite` enum stops the tool with a "did you mean" hint;
- an unknown `objectClass` or a `sprite` an object can't use stops the tool (and the game), naming the map and object;
- a sheet that isn't packed, or an animation that doesn't exist, stops the game at start with a message;
- `./gradlew e2e:test` fails if you forgot to run the tool.

To remove a sprite, delete its line from `Sprite` and run the tool.

### What goes where

| Where | What | In git |
|---|---|---|
| `tools/assets/pack.json` | Describes the pack: which sheets are tiles, how animations are cut, row names. Written by hand | yes |
| `assets-src/maps/` | Your levels (edit these in Tiled) | yes |
| `assets-src/tilesets/` | Tilesets for Tiled, generated | yes |
| `assets-src/tilesets/images/` | Copies of the pack's sheets, so Tiled can show them | **no** (license) |
| `android/assets/maps/` | Game copies of the levels + atlas of the tiles they use, generated | yes |
| `android/assets/sprites/` | Atlas of the sprite sheets the game uses (`Sprite` enum) + `animations.json`, generated | yes |
| `core/.../resources/Action.java` | Animation actions (`WALK`, `IDLE`...) from the row names in `pack.json`, generated | yes |
| `build/assets-preview/` | Preview page | no |

Never edit generated files by hand: change the levels or `pack.json` and run the tool.
**New version of the pack?** Run the tool with its path. If it warns about images `pack.json` doesn't know, add them
there.

More detail (adding a sprite to the game, Tiled layers, `pack.json` options): [tools/assets/README.md](tools/assets/README.md).
Credits: [CREDITS.md](CREDITS.md).
