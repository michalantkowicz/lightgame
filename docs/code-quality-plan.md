# Code quality checks

What `./gradlew build` checks besides the tests, and why. All of it runs in CI (the required `test` check of
`master`), so a pull request that breaks any of it can't be merged. Set up 2026‑11‑13; the steps are in
[changelog.md](changelog.md) (branches `quality-*` and `arch-*`).

## Format: Spotless

- Spotless 8.11.0 with the Eclipse JDT formatter 4.41 (pinned) and `config/eclipse-formatter.xml`, a profile matching
  the code's own style: 4 spaces, 120 columns, continuation indent 8, hand-wrapped lines kept, comments untouched,
  enum constants one per line when they don't fit on one. Imports in IntelliJ's order, unused ones removed.
- Not formatted: the vendored GWT emulation sources in `html/src/**/emu/**`.
- A failure says "Run './gradlew spotlessApply'". The one-time reformat is listed in `.git-blame-ignore-revs`.

## Complexity: PMD

- PMD 7.29.0 on `core`, rules in `config/pmd.xml`: cognitive complexity 15, cyclomatic 10 per method (80 per class),
  NPath 200, at most 10 parameters, NCSS 60 per method and 300 per class, coupling between objects 20, 30 imports, no
  god classes, plus three cheap correctness rules. Nothing about unused members: the project keeps them on purpose.
- Known exceptions, marked `@SuppressWarnings("PMD.<Rule>")` with a `// baseline (docs/code-quality-plan.md)` comment:
  `TmxTiledMapLoader` (size, coupling, imports), `ResourcesService` and its `loadUi`, `Tile`, `GameContext`'s
  constructor, `GameScreen`, `Hud`, `Portrait.act`. Remove one when its code is refactored; never add one to new code.
- The report is `core/build/reports/pmd/main.html` (CI attaches it when a run fails).

## Architecture: ArchUnit

`e2e/test/com/mantkowicz/light/architecture/ArchitectureTest.java` (ArchUnit 1.5.1, reads the compiled classes):

- **Layers**, each using only the layers below it:
  1. leaves: `util`, `debug`, `resources`, `stage` (use nothing of the game);
  2. world: `board`, `actor`, `light`, `event` (only the bus), `context` (`GameContext`: what game objects get from the
     game screen, stages as plain libGDX `Stage`s);
  3. behaviour: `plugin`, `notification`;
  4. characters: `character` (the player, the dog, the cow, the chest...), `map` (creates them);
  5. UI: `ui` and its subpackages (`ui.hud`, `ui.menu`, `ui.collect`);
  6. application: `feature`, `game`.
- **No package cycles**, plus explicit rules for pairs that once used each other: the board doesn't know actors or the
  context, plugins don't know characters, characters don't know the map, the basic UI doesn't know its subpackages,
  features don't know `game`, the event bus uses nothing of the game.
- **Conventions**: `*Event` classes are `GameEvent`s, `*Plugin` are `actor.Plugin`s, `*Feature` are `Feature`s; no
  reflection, `java.util.concurrent` or `String.format` (the HTML5 build can't run them); scenario tests use only
  `GameDriver`.
- Not a gate: the test writes Lakos and Martin metrics to `e2e/build/reports/architecture/metrics.md`, shown on each
  CI run's summary page. At the start 13 of 16 packages were one cycle (RACD 0.787, NCCD 3.24); now none (RACD 0.375,
  NCCD 1.78).
- A rule that gets in the way of a sensible design is changed on purpose, in its own pull request; the code isn't
  twisted to satisfy it. Dependencies made only of constants (copied in by the compiler) are invisible to it.

## Not done

- Splitting `GameContext` into what each part needs (big, no longer part of any cycle).
- The PMD exceptions above.

## Tools considered and not used

CK (last release 2022, CSV only, no gate), Checkstyle (its metrics overlap PMD's), palantir-java-format and
google-java-format (both rewrap calls one argument per line: 2,500–3,700 changed lines against the Eclipse profile's
~550), SonarQube Cloud and CodeScene (external services needing accounts and tokens; worth a look only if "complexity
added by this PR" is ever wanted), jQAssistant (a graph database is too much for a ~7,000-line game).
