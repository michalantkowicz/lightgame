# Code quality checks: plan

Formatting (Spotless), structure (ArchUnit) and complexity (PMD) checks in the build, so a pull request that breaks
the format, adds a new tangle between packages or a too complex method or class fails before it is merged. Written
2026‑11‑13 against master `4b9eb77`; every number below was measured on that commit.

## Decision

It is a good time. The game code (`core`) is small: 139 classes in 16 packages, ~6,900 lines, plus ~4,100 lines of
launchers and tests. The e2e tests (79) cover the behaviour, so refactorings found by the checks are safe. No pull
request is open, so a one-time reformat conflicts with nothing. Waiting only makes the reformat and the baseline bigger.

What is good today: most classes and methods are small and simple; three packages are clean leaves; the scenario tests
already follow their "GameDriver only" rule. What is not: 13 of the 16 packages form one cycle (below), and a few
classes carry most of the complexity.

The checks keep **today's state as the baseline** (nothing fails on the first day) and make it impossible to get
worse without a deliberate, reviewed change; each later clean-up tightens the baseline (a ratchet).

## What was measured

### Package structure (ArchUnit 1.5.1 on the compiled classes, and the imports)

- 75 package-to-package dependencies inside `com.mantkowicz.light`.
- `debug`, `resources`, `util` depend on no other game package; everything else (13 packages) is one strongly
  connected group: from each of them every other one can be reached. 17 pairs depend on each other directly, e.g.
  `actor ↔ board`, `actor ↔ event`, `actor ↔ plugin`, `board ↔ map`, `board ↔ game`, `game ↔ ui`.
- The hub is `game.GameContext`: it holds the stages (`ui`), the board service, events and resources, and 9 packages
  import it. Other links: `Player` creates `PlayerCollectResolver` (`actor → ui.collect`), `GameBoardActor` holds menu
  interactions (`actor → ui.menu`), `GameActorDefinition` uses `StageType` (`actor → ui`), `ActionButton` uses `Hud` and
  a plugin (`ui → ui.hud`, `ui → plugin`), `BoardClickListener` builds notifications (`board → notification`), `Board`
  runs the map loaders while the map builds tiles (`board ↔ map`).
- Lakos metrics over the 13 top-level packages: a package reaches on average 79 % of the others (RACD 0.787),
  NCCD 3.24 (about 1 means a balanced tree; higher means more tangled). Martin metrics per package are fine except the
  cycle: e.g. `feature` I=0.88 (uses a lot, used by little, as a top layer should), `resources` I=0.0 (stable leaf).
- ArchUnit's own cycle rule (`slices().should().beFreeOfCycles()`) reports its maximum of 100 cycles: too many to
  read, and freezing them as a baseline is fragile (one new dependency can change which 100 get reported).
- Compile-time constants are copied into the classes using them, so a dependency made only of constants is invisible
  in the bytecode (today `ActionButton` → `Hud.ACTION_RIGHT`). The bytecode also sees one link the imports don't
  (`map → ui`, through a type used without an import). Good enough for a gate; worth knowing.

### Complexity (PMD 7.29.0, `core` only)

Above PMD's default thresholds today:

| Rule (default threshold) | Over it |
|---|---|
| CognitiveComplexity (15) | `TmxTiledMapLoader.loadPackedMap` 41, `TmxTiledMapLoader.createObjects` 16 |
| CyclomaticComplexity, method (10) | `loadPackedMap` 24, `Portrait.act` 12, one `ResourcesService` method 11 |
| NPathComplexity (200) | `loadPackedMap` 954 |
| CouplingBetweenObjects (20) | `ResourcesService` 29, `TmxTiledMapLoader` 27, `Hud` 22, `GameScreen` 21 |
| ExcessiveImports (30) | `GameScreen` 35, `TmxTiledMapLoader` 31 |
| GodClass | `TmxTiledMapLoader`, `ResourcesService`, `Tile` |
| ExcessiveParameterList (10) | `GameContext` constructor 11 |

Everything else is under them; e.g. no other method above cognitive complexity 13, no class above 170 NCSS lines.

PMD's general "quickstart" rules find 67 things, nearly all style or deliberate: 33 × `OneDeclarationPerLine` (the
project writes `float A = 1, B = 2;` on purpose), unused private members (kept on purpose, see the project's rule),
`==` on objects where identity is meant, assignments in loop conditions. A few are worth fixing (4 utility classes
without a private constructor, one `size() == 0`, two useless parentheses), not worth a whole lint gate.

### Formatting (Spotless 8.11.0, all Java in core, desktop, html, e2e)

| Formatter | Files changed | Lines (+/−) |
|---|---|---|
| palantir-java-format 2.103.0 | 121 | 1394 / 1170 |
| google-java-format 2.0.0, AOSP, 120 columns | 135 | 2024 / 1740 |
| same, without reformatting javadoc | 122 | 1692 / 1325 |
| Eclipse JDT 4.41 with a profile matching the current style (below) | 57 | 303 / 257 |
| only trailing spaces, unused imports, final newline | 4 | 8 / 8 |

palantir and google put every argument on its own line once a call doesn't fit (e.g. `Hud`'s idle definitions grow
from 2 to 10 lines): a different style from the project's compact one. The Eclipse profile keeps the project's style
(4 spaces, 120 columns, continuation indent 8, wrap only where needed, never join lines wrapped by hand, comments left
as written, imports in IntelliJ's default order) and mostly breaks lines longer than 120 columns and sorts imports.
After it the build, all 79 tests and `html:dist` pass, and a second run changes nothing. The two vendored GWT
emulation files (`html/src/**/emu/**`, copies of libGDX and box2dlights sources) must be excluded: they alone would
be 832 changed lines.

### Cost

With a warm Gradle daemon: `spotlessCheck` ~1.0 s, PMD on `core` ~1.8 s, the ArchUnit import ~0.5 s (compiling `core`
takes ~0.4 s). Negligible next to the e2e tests and the GWT compile.

### Tools considered

| Tool | Latest | Verdict |
|---|---|---|
| ArchUnit (`com.tngtech.archunit:archunit-junit5`) | 1.5.1 (2026‑09‑25), reads Java 25/26 class files | **Use**: structure rules + Lakos/Martin metrics, plain JUnit tests |
| PMD (Gradle's built-in `pmd` plugin) | 7.29.0 (2026‑10‑30) | **Use** for complexity/size/coupling thresholds per method and class |
| Spotless (`com.diffplug.spotless`) | 8.11.0 (2026‑11‑02) | **Use** with the Eclipse JDT formatter |
| CK (mauricioaniche) | 0.7.0, last release 2022‑02 | No: unmaintained releases, only writes CSV, no gate |
| Checkstyle | 14.4.0 | No: its metrics overlap PMD's; one tool is enough |
| SonarQube Cloud, CodeScene | services | Not now: external service, account and token, sends the code out; good at "complexity added by this PR" if the in-build checks ever feel too coarse |
| jQAssistant | | No: a graph database for a 7k-line project is overkill |

## The plan

One branch and one pull request per phase, each from `origin/master`, each leaving the build green.

### Phase 1: Spotless and the one-time reformat

- Root `build.gradle`: `plugins { id 'com.diffplug.spotless' version '8.11.0' }` and
  ```groovy
  spotless {
      java {
          target 'core/src/**/*.java', 'desktop/src/**/*.java', 'html/src/**/*.java', 'e2e/test/**/*.java'
          targetExclude 'html/src/**/emu/**'        // vendored libGDX / box2dlights sources for GWT
          eclipse('4.41').configFile('config/eclipse-formatter.xml')   // pinned: upgrades never reformat silently
          importOrder('', 'javax|java', '\\#')      // IntelliJ's default: others, java/javax, static
          removeUnusedImports()
          trimTrailingWhitespace()
          endWithNewline()
      }
  }
  ```
- `config/eclipse-formatter.xml`: the profile used for the measurement: spaces, size 4, `lineSplit` 120,
  `continuation_indentation` 2, `join_wrapped_lines` false, comment formatting off, `use_on_off_tags` true (so
  `// @formatter:off` / `on` can protect a hand-aligned table), `alignment_for_enum_constants` 48 (constants that
  don't fit on one line go one per line; Eclipse's default joined `UiImage`'s constants into long lines). All other
  settings are Eclipse's defaults. With that setting the reformat is 50 files, +287/−193 lines.
- Two commits: the setup, then the reformat alone (`./gradlew spotlessApply`), and a `.git-blame-ignore-revs` file
  listing the reformat commit, so GitHub's blame skips it.
- `spotlessCheck` runs as part of `./gradlew build` (Spotless adds it to `check`), so CI needs no change. Fixing a
  failure: `./gradlew spotlessApply`. IntelliJ can import the same profile (Code Style → Java → Import Scheme →
  Eclipse XML Profile), so the IDE formats nearly the same way; `spotlessApply` has the last word.
- Merge it when no other branch is in flight. The old unmerged branches (`tiledMapRenderer`, `ccr-dcc79c9a-n8m2ou`)
  would need a `spotlessApply` before they could be merged.

### Phase 2: architecture tests (ArchUnit)

In the e2e module, package `com.mantkowicz.light.architecture` (it already depends on `core` and runs JUnit 5), with
`testImplementation 'com.tngtech.archunit:archunit-junit5:1.5.1'`:

1. **Allowed package dependencies.** `e2e/architecture/package-dependencies.txt` lists today's 75 links
   (`actor -> board`, ...). The test fails on any link not in the list, naming the classes that make it; a link that
   no longer exists makes it fail too, asking to delete the line, so clean-ups tighten the list (the ratchet). Adding
   a line is allowed, but it shows up in the pull request's diff for review. Deterministic, unlike frozen cycles.
2. **Leaves stay leaves.** `resources`, `debug`, `util` depend on no other game package.
3. **Scenario tests use `GameDriver` only** (the rule in `e2e/README.md`, today true): classes in `e2e.scenario` don't
   depend on game classes.
4. **Metrics to watch.** The test writes the Lakos numbers (RACD, NCCD) and Martin's coupling and instability per
   package to `e2e/build/reports/architecture/metrics.md`, which CI adds to the run's summary page; no gate on them
   (the dependency list already catches every new link).

### Phase 3: PMD complexity gate

- `core/build.gradle`: `apply plugin: 'pmd'`, `toolVersion = '7.29.0'`, `ruleSetFiles = files('../config/pmd.xml')`,
  `ruleSets = []`, `ignoreFailures = false`, `consoleOutput = true`; only `pmdMain` for `core` (the tests are not
  shipped code).
- `config/pmd.xml`: from `category/java/design.xml` CognitiveComplexity (15), CyclomaticComplexity (method 10,
  class 80), NPathComplexity (200), NcssCount (method 60, class 300), CouplingBetweenObjects (20),
  ExcessiveImports (30), ExcessiveParameterList (10), GodClass; plus the few cheap correctness rules that are clean
  or one fix away: `ClassWithOnlyPrivateConstructorsShouldBeFinal` (clean), `UseCollectionIsEmpty` and
  `UnnecessaryConstructor` (one finding each, fixed in this phase). Nothing about unused members (the project keeps
  them on purpose). Measured with exactly these rules and limits: 18 findings, the 16 in the table above plus those two.
- The baseline: the 16 findings in the table above get `@SuppressWarnings("PMD.<Rule>")` (in 10 places)
  with a `// baseline, see docs/code-quality-plan.md` comment, so new code is held to the limits from day one.
  Phase 4 removes them.
- `pmdMain` is part of `check`, so CI runs it in `./gradlew build`. CI uploads `**/build/reports/pmd/` with the test
  reports on failure (one line in `tests.yml`).

### Phase 4 (optional): clean-ups, smallest first

Each its own pull request; each removes lines from the dependency list or a suppression:

1. `ActionButton` into `ui.hud` (it is a HUD element now): removes `ui → ui.hud` and so the `ui ↔ ui.hud` pair (its
   plugin link moves along: `ui.hud → plugin` instead of `ui → plugin`).
2. `StageType` out of `ui` into a neutral place: removes `actor → ui`.
3. The collect resolver created by a feature instead of `Player` (and the HUD asking it, not the player, to throw an
   item out): removes `actor → ui.collect`.
4. `BoardClickListener` posts an event instead of building the notification: removes `board → notification`.
5. Split `TmxTiledMapLoader.loadPackedMap` (cognitive 41) into steps: removes the worst suppression.
6. The big one, later and only if wanted: split `GameContext` into what each part needs (the hub of the cycle).

## What a failing pull request looks like

- Format: "Run './gradlew spotlessApply' to fix these violations" (Spotless's own message).
- New package link: "new dependency `board -> ui.hud` (from `board.Foo` to `ui.hud.Bar`): remove it, or add it to
  `package-dependencies.txt` if it is meant".
- Complexity: PMD names the method or class, its value and the limit, e.g. "The method `x()` has a cognitive
  complexity of 18, current threshold is 15".

All three fail `./gradlew build`, which is the required `test` check of the master branch protection (already set,
also for admins), so such a pull request can't be merged. No change to the GitHub settings is needed.
