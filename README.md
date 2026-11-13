# lightgame

A libGDX game (desktop, Android, HTML). Run it with `./gradlew desktop:run`.

## Building and checks

```sh
./gradlew build html:dist   # what CI runs: compile, format check, PMD, all tests, the HTML5 (GWT) build
./gradlew spotlessApply     # format the code (the build fails on unformatted code)
./gradlew e2e:test          # the headless gameplay tests and the architecture tests only
./gradlew html:serve        # the HTML5 version at http://localhost:8080
```

`./gradlew build` checks more than the tests (details and reasons in
[docs/code-quality-plan.md](docs/code-quality-plan.md)):

- **Format** (Spotless): the Eclipse profile in `config/eclipse-formatter.xml`. Fix with `./gradlew spotlessApply`.
- **Complexity** (PMD, `config/pmd.xml`): limits per method and class; the report is `core/build/reports/pmd/main.html`.
- **Architecture** (ArchUnit, `e2e/.../architecture/ArchitectureTest.java`): the packages' layers, no package cycles, naming
  conventions, nothing the HTML5 build can't run.
- **Behaviour** (`e2e/`): the real game played headlessly against golden files; see [e2e/README.md](e2e/README.md).

CI (`.github/workflows/tests.yml`) runs `./gradlew build` and `html:dist` on every pull request to `master`; a pull
request can be merged only when it passes. Android needs an Android SDK and isn't built by CI.

## Docs

- [docs/changelog.md](docs/changelog.md): what each feature branch changed, and why.
- [docs/ideas.md](docs/ideas.md): possible improvements, not planned yet.
- [docs/code-quality-plan.md](docs/code-quality-plan.md): the build's checks (format, complexity, architecture) and why.
- [docs/hud-design.md](docs/hud-design.md): the HUD as built, and what is planned for it.
- [CLAUDE.md](CLAUDE.md): short notes for AI agents (and anyone new): how to work here, the architecture in brief.
- [e2e/README.md](e2e/README.md): the headless regression tests; [tools/assets/README.md](tools/assets/README.md): the
  asset tool.

## Assets

The graphics come from two **Sprout Lands** packs by Cup Nooble: the sprites pack (tiles, characters, animals) and
the UI pack (buttons, dialog boxes, icons, fonts). The packs themselves stay out of the repo (their license forbids
redistributing them). A tool turns it into what the game and Tiled need, and only that output is committed.

### Setup (once per machine)

1. Install Pillow: `pip3 install pillow`.
2. Unzip both packs into `tools/assets/`, keeping their folder names (`tools/assets/Sprout Lands - Sprites - premium
   pack/`, `tools/assets/Sprout Lands - UI Pack - Premium pack/`). Those folders are git-ignored. Anywhere else works
   too, as long as it isn't committed.
3. Run the tool with the sprites pack's path. It finds the UI pack next to it (or give it with `--ui`) and remembers
   both paths for next time:
   ```sh
   python3 tools/assets/build_assets.py "tools/assets/Sprout Lands - Sprites - premium pack"
   ```

### Day to day

1. Open `assets-src/lightgame.tiled-project` in Tiled. Edit or add levels in `assets-src/maps/`.
2. Run the tool. It takes about a second and rewrites only what changed:
   ```sh
   python3 tools/assets/build_assets.py
   ```
3. Start the game. To check the graphics, open `build/assets-preview/index.html`: every animation, every UI piece and
   font, the levels, warnings.
4. Commit your changes together with what the tool changed (in `assets-src/`, `android/assets/`, `Action.java`).

Run the tool after any change to a level, to `tools/assets/pack.json` or `tools/assets/ui.json`, to the packs, or to
the lists of what the game uses (`Sprite`, `UiImage`, `UiFont` in `core/src/com/mantkowicz/light/resources/`: only
those are packed). You don't need it for
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

### Using a UI piece or font (e.g. a dialog box)

1. Find it on the preview page (`build/assets-preview/index.html#ui`): every piece of the UI pack with its name, e.g.
   `dialog_box`, `icons/017`, `emotes/row_03` (nine-patches are also shown stretched, animations play).
2. Add `DIALOG_BOX("dialog_box")` to the `UiImage` enum (a font: `PIXEL_8X14("pixel_8x14")` to `UiFont`).
3. Run the tool. It packs only the listed pieces into `android/assets/ui-pack/ui.atlas` and makes the listed fonts.
4. Use it: `resourcesService.getUiImage(UiImage.X)`, `getUiNinePatch(UiImage.X)` for pieces that stretch,
   `getUiAnimation(UiImage.X)` for animated ones, `getFont(UiFont.X)`.

How sheets are cut (grids, separate shapes, nine-patch borders, names for pieces) is in `tools/assets/ui.json`; see
[tools/assets/README.md](tools/assets/README.md#ui-pack).

### Adding an animated sprite (e.g. a brown cow)

1. Find the sheet on the preview page (`build/assets-preview/index.html`), e.g. `cow_brown`.
2. In `tools/assets/pack.json`, name the rows you need: `action` or `action_direction`, e.g. `idle_down`, `walk_left`,
   `eat`. Unnamed rows are called `row_00`, `row_01`...
3. Add `COW_BROWN("cow_brown")` to the `Sprite` enum.
4. Run the tool. It packs the sheet and adds any new actions to the generated `Action` enum (e.g. `Action.EAT`).
5. Use it in code: `resourcesService.getAnimation(Sprite.COW_BROWN, Action.WALK, Direction.LEFT)`, or without a
   direction for rows like `eat`. Animations are never named by strings in Java.

Mistakes are caught early:
- a typo in the `Sprite`, `UiImage` or `UiFont` enum stops the tool with a "did you mean" hint;
- a sheet that isn't packed, or an animation that doesn't exist, stops the game at start with a message;
- `./gradlew e2e:test` fails if you forgot to run the tool.

To remove a sprite, delete its line from `Sprite` and run the tool.

### What goes where

| Where | What | In git |
|---|---|---|
| `tools/assets/pack.json` | Describes the pack: which sheets are tiles, how animations are cut, row names. Written by hand | yes |
| `tools/assets/ui.json` | Describes the UI pack: how sheets are cut into pieces, nine-patch borders, animations, fonts. Written by hand | yes |
| `assets-src/maps/` | Your levels (edit these in Tiled) | yes |
| `assets-src/tilesets/` | Tilesets for Tiled, generated | yes |
| `assets-src/tilesets/images/` | Copies of the pack's sheets, so Tiled can show them | **no** (license) |
| `android/assets/maps/` | Game copies of the levels + atlas of the tiles they use, generated | yes |
| `android/assets/sprites/` | Atlas of the sprite sheets the game uses (`Sprite` enum) + `animations.json`, generated | yes |
| `android/assets/ui-pack/` | Atlas of the UI pieces the game uses (`UiImage` enum), bitmap fonts (`UiFont`), `ui.json`, generated | yes |
| `core/.../resources/Action.java` | Animation actions (`WALK`, `IDLE`...) from the row names in `pack.json`, generated | yes |
| `build/assets-preview/` | Preview page | no |

Never edit generated files by hand: change the levels or `pack.json` and run the tool.
**New version of the pack?** Run the tool with its path. If it warns about images `pack.json` doesn't know, add them
there.

More detail (adding a sprite to the game, Tiled layers, `pack.json` options): [tools/assets/README.md](tools/assets/README.md).
Credits: [CREDITS.md](CREDITS.md).
