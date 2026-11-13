# lightgame: notes for agents

A libGDX game for kids aged about 3–8 (desktop, HTML5 via GWT, Android), drawn with the Sprout Lands packs. Simple,
readable, picture-first: young players can't read, so prefer pictures and animation over text.

## How to work here

- **Work on a branch, but commit, push or open a PR only when the owner asks.** One branch and one PR per feature,
  phase or bug fix, cut from `origin/master` with `git switch --no-track -c <branch> origin/master`. PRs are merged with merge commits (keeps
  `.git-blame-ignore-revs` valid). GitHub, `gh` CLI; the required check is the `test` job.
- **Keep changes small and on topic.** No scope creep, no "while I'm here". Estimate size honestly. Ask when a choice
  is the owner's (design, look, behaviour); otherwise decide and say what you decided.
- **Never delete code just because it's unused**: methods, classes, enum constants, debug tools stay unless the owner
  approves each one. The game is a snapshot of experiments; unused often means "not called right now".
- **Behaviour is guarded by e2e goldens** (`e2e/golden/`). Re-record (`-Dgolden.update=true`) only for an approved
  behaviour change and call it out in the PR, even a one-frame timing shift. Refactorings change no golden.
- **Every branch adds an entry to `docs/changelog.md`** (the day of the work, branch, what and why). Ideas that aren't
  done go to `docs/ideas.md` (own branch + PR); remove an idea when it's done. When you change something the docs
  describe or quote (a rule, a user-facing text, a PMD exception listed in `docs/code-quality-plan.md`), update them.
- **UI work: look at it** whenever what is drawn changes (layout, sprites, sizes); a text-only change needs no
  screenshot. Tests can't see pictures. Take screenshots with a throwaway `JavaExec` main in a hidden LWJGL3
  window (`setInitialVisible(false)`, `render(1/60f)`, `Pixmap.createFromFrameBuffer`: rows are already top-down), and
  never commit it.
- **Assets:** never commit the Sprout Lands pack images (license); only the generated atlases, fonts and maps. Tooling
  is Python only (`tools/assets/build_assets.py`, see README.md). The game uses only what the `Sprite`, `UiImage` and
  `UiFont` enums list; `Action` is generated.
- Don't send the owner's e-mail address to any service.

## Commands

```sh
./gradlew build html:dist          # everything CI runs: compile, Spotless, PMD, all tests, the GWT build
                                   # results: */build/reports/tests/test/index.html, core/build/reports/pmd/main.html
./gradlew spotlessApply            # format
./gradlew e2e:test                 # headless gameplay + architecture tests
./gradlew desktop:run              # play
python3 tools/assets/build_assets.py   # after changing levels, pack.json, ui.json or the asset enums
```

## Architecture (enforced by `ArchitectureTest`, details in `docs/code-quality-plan.md`)

Layers, each using only those below: leaves (`util`, `debug`, `resources`, `stage`) → world (`board`, `actor`,
`light`, `event` bus, `context`) → behaviour (`plugin`, `notification`) → characters (`character`, `map`) → UI (`ui`,
`ui.hud`, `ui.menu`, `ui.collect`) → application (`feature`, `game`). No package cycles. If a rule blocks a sensible
design, change the rule on purpose in its own PR.

- `GameScreen` builds everything and hands a `GameContext` (services, plain `Stage`s) to game objects.
- Actors (`GameActor`, `GameBoardActor`) get behaviour from per-actor **plugins** (`actor.Plugin`); screen-wide logic is
  a **feature** (`feature.Feature`), run after the stages each frame. Concrete characters live in `character`.
- A creature's behaviour is a gdx-ai state machine: a `State` enum next to its class (`CowState` for `Cow`), all its
  transitions in that one file, no framework on top. `Debug.Tool.STATE_LABELS` shows the states while playing.
- Events go through `GameEventService`; an event class lives next to what it is about (one that carries nothing of the
  game, e.g. `BackpackItemEjectedEvent`, may stay in `event`).
- The board tracks `TileOccupant`s and `TileContent`s and knows nothing of actors or the map.
- HUD (`ui.hud`): the face (`Portrait`, idle and reactions), backpack slots, stamina bar, the round action/hint button
  (also strokes or feeds a `Creature` next to the player: `CreatureFeature`), the hold-to-open settings gear. Collecting: a chest's button throws its items onto free tiles (`ItemThrower`), the
  player picks them up by walking onto them.

## Gotchas

- GWT: no reflection, `java.util.concurrent` or `String.format` in `core`; `html:dist` is the real check.
- Tests are deterministic: fixed 1/60 s frames, `MathUtils.random` seeded; scenario tests talk to the game only through
  `GameDriver` (see `e2e/README.md`).
- scene2d: a `NinePatchDrawable`'s minimum size is the whole picture (`setMinHeight` to go smaller); `Container.width()`
  sizes the child, padding comes on top.
