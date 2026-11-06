# Light Game: Regression Testing Strategy

Goal: before any refactoring, pin down **everything the game does today** with automated tests that
**play the real game**, so every refactoring step can prove it changed nothing.

This document is the design. Phase 0 of [`refactoring-plan.md`](refactoring-plan.md) implements it.

---

## 1. Is it possible in libGDX? Yes. Proven by a spike (2026‑11‑06)

A throwaway spike, kept outside the repo and with no changes to game code, showed this works:

| What was tried | Result |
|----------------|--------|
| Run the **real** `Main` → `GameScreen` (all stages, Box2D world, RayHandler, plugins, features) headless, with a no‑op OpenGL | ✅ Works. 44 tiles load, actors spawn, ~0.5 s for a full scenario |
| Inject clicks through the game's own `InputMultiplexer`, advance frames manually | ✅ Clicking a tile walks the player there; the camera follows |
| Long‑press the dog → radial menu → release over the button | ✅ 1 menu button; the description tooltip appears on the notification stage |
| Click a visible blocked tile | ✅ "no path" `Notification` appears |
| Walk onto the chest | ✅ **Bug B1 confirmed**: `RuntimeException: Actor on this stage must be instanceof MenuButton` |
| Two identical runs | ✅ Identical state (positions and camera match exactly) |
| Real OpenGL in a **hidden** desktop window, fixed timestep, seeded randomness, scripted input, screenshots | ✅ Correct images (board, torch glow, actors, camera after walking) |
| Two identical screenshot runs on the same machine | ✅ **Pixel‑identical PNGs** |

The last two rows concern visual (screenshot) tests. They are **deferred for now** (§9); the notes are kept for later.

The spike's key tricks are in §8, so they don't have to be rediscovered.

---

## 2. The test pyramid for this project

| Layer | What it runs | What it checks | Speed | Where it runs |
|-------|--------------|----------------|-------|---------------|
| **Gameplay tests (E2E, headless)** | The real game with a no‑op GL, driven by scripted input and fixed frames | Everything a player can observe, as **state**: positions, cells, camera, texts on screen, menus, shading on/off, crashes | ~0.5 s per scenario | JUnit, every step, CI |
| **Focused unit tests** | Small pure pieces | Map geometry and neighbours, path finding on fixed pairs, inventory | ms | JUnit, every step, CI |

The gameplay tests are the main safety net. Unit tests exist only where a refactoring step rewrites an algorithm
(map loader 5.5, path finding 5.6) and a precise failure message is worth more than a scenario diff.

Rendering itself (how lights, the shader and sprites actually look) is not tested automatically yet. The manual
play‑through at each checkpoint covers it, and visual tests can be added later (§9).

The HTML5 build keeps its existing checks: `html:dist` must compile, plus the optional headless‑Chrome smoke test.

---

## 3. Architecture of the test code

### 3.1 New Gradle module `e2e/`

Keep test plumbing out of `core`, because `core` must stay GWT‑compatible.

```
e2e/
├── build.gradle            deps: project(':core'), gdx-backend-headless, natives-desktop (gdx + box2d),
│                           junit-jupiter, jacoco
├── test/com/mantkowicz/light/e2e/
│   ├── GameDriver.java         ← the ONLY class that touches game internals (page-object pattern)
│   ├── Cell.java               ← (col,row) value object, the stable way to address tiles
│   ├── Snapshot.java           ← deterministic text rendering of observable state
│   ├── headless/HeadlessGame.java   ← sets up Gdx globals, creates Main, runs frames
│   ├── headless/NoOpGL.java, FixedGraphics.java, ScriptedInput.java
│   ├── scenario/*Test.java     ← scenarios S1–S16 grouped by topic, written against GameDriver only
│   └── unit/*Test.java         ← focused unit tests (§2)
└── golden/
    └── <scenario>.txt          ← text goldens (platform independent)
```

- `settings.gradle`: `include 'e2e'`. `html` and `desktop` don't depend on it.
- `e2e/build.gradle`: `sourceSets.test.java.srcDirs = ['test/']` and
  `test { useJUnitPlatform(); workingDir = rootProject.file('android/assets'); jvmArgs '--enable-native-access=ALL-UNNAMED' }`.
- Everything lives in the test source set: the module produces no artifact.
- Keep `HeadlessGame` behind a small seam (e.g. `frames(n)`, `runOnGameThread(Runnable)`) so a real‑GL backend can be added later
  for visual tests (§9) without touching scenarios. Don't build that backend now.

### 3.2 `GameDriver`: why one class matters

The refactoring renames and moves almost every class. If scenarios touched `Player`, `GameScreen` or `MenuStage`
directly, every step would break dozens of tests, and agents would be tempted to "fix" a test instead of the code.
So:

- **Scenarios and goldens speak in player terms only:** cells, "the player", "the dog", visible texts, the camera.
- **Only `GameDriver` knows the classes.** When a refactoring step renames something, only `GameDriver` changes, and that
  change must be mechanical, such as an import or a getter name.
- **Goldens never change during Phases 1–6.** A changed golden means changed behaviour. Stop and ask.

`GameDriver` reaches internals in two ways:
- stages through `((InputMultiplexer) Gdx.input.getInputProcessor()).getProcessors()` (index 0 = menu, 1 = game);
- the rest (for example `notificationStage`) through reflection on `game.getScreen()`.

Reflection is fine here: tests run on the JVM only.

### 3.3 `GameDriver` API (actions)

| Method | Meaning |
|--------|---------|
| `start()` | New `Main`, `create()`, `resize(400, 640)`, run 30 frames |
| `frames(n)` / `seconds(s)` | Advance the game by fixed 1/60 s frames |
| `click(Cell)` | touchDown + 1 frame + touchUp on the tile centre. **Fails if the tile isn't on screen** (camera frustum), like a real player |
| `click(Actor kind)` | Same, on an actor's centre (`PLAYER`, `DOG`, `CHEST`) |
| `pressAndHold(kind, seconds)` / `releaseOverMenuButton(i)` / `releaseAt(Cell)` | For the radial menu. Moves the scripted pointer, because `MenuStage` reads `Gdx.input.getX/Y` |
| `hoverMenuButton(i)` | Moves the pointer over a button (enter/exit scaling) |
| `pressKey(Keys.Q)` | Makes `isKeyJustPressed(Q)` true for exactly one frame |
| `resize(w, h)` | Forwards to the game |
| `stop()` | `dispose()`; must not throw |

### 3.4 Observations, and what goes into `Snapshot`

Record only things a player could see or that define game state. **Never** record object counts or internal fields that a
bug fix or refactoring changes invisibly.

- **Player:** current cell, position (1 decimal), walking or idle.
- **Actors on the board:** kind → cell (`DOG`, `CHEST`).
- **Camera:** position (1 decimal) and zoom.
- **Visible texts:** every label with effective alpha > 0.01 on the notification stage, as `text @ (x,y) alpha=0.00`.
  This covers the "no path" text and description tooltips. Using effective alpha keeps the golden stable when B5 is fixed:
  the fully faded tooltip is no longer recorded either way.
- **Menu:** the number of buttons, each button's position and scale.
- **UI widgets** (after B1 is fixed): COLLECT button, collect window and its slots, as item kind or empty.
- **Shading:** whether the game stage batch has the fade shader set (`Q`/`W`).
- **Lights:** position and colour of each torch, and the current distance (seeded randomness makes it deterministic).
- **Errors:** the exception class and message if a frame threw.

`Snapshot.toString()` produces sorted, rounded, line‑based text, so goldens diff nicely in git.

### 3.5 Golden files

- Each scenario test records the snapshots at named moments and compares them with `golden/<scenario>.txt`.
- Running with `-Dgolden.update=true` rewrites the goldens. This is allowed **only** at CP0 (creating them) and in Phase 7
  bug‑fix steps (with approval, and the diff reviewed in the commit).
- On a mismatch, the test prints a unified diff and writes `build/golden-actual/<scenario>.txt`.

---

## 4. Scenarios: the coverage matrix

Each row is one scenario class; together they cover every behaviour in §1 of the refactoring plan.
The "Moments" column lists when snapshots are taken.

| # | Scenario | Script | Moments | Checks (behaviour) |
|---|----------|--------|---------|--------------------|
| S1 | **Start** | `start()` | `start` | Board, actors at their spawn cells, torch, initial camera on the player, FPS label |
| S2 | **Walk** | click a reachable cell 3–4 steps away; snapshot **every frame** while walking (cells only) | `walk-path` (cell sequence), `arrived` | Path follows neighbours, one tile per step, arrives, status back to idle |
| S3 | **Re‑route** | click far cell A, then cell B after 1 step | `rerouted` | Changes target at the next tile boundary |
| S4 | **No path** | click a visible blocked (furniture) cell; snapshot at 0 s, 1 s, 2.1 s | `np-0`, `np-1`, `np-gone` | Red "no path" text appears, rises 20 px, fades, is removed |
| S5 | **Unreachable** | click a cell without a tile (every accessible tile on this map is reachable) | `unreachable` | Player doesn't move, no text |
| S6 | **Look at me** | walk away from the player's centre, then click the player; snapshot at 0.5 s and 1.1 s | `look-mid`, `look-done` | The camera animates to the player |
| S7 | **Camera tracking** | walk the player far, several times; snapshot each frame (camera only, 1 decimal) | `tracking` | Soft follow outside a 420×420 box and gentle stop (0.96 decay) |
| S8 | **Menu → description** | press and hold the dog 0.6 s, hover the button, release over it; snapshots during the animation and after 1 s, 3 s, 4.1 s | `menu-anim`, `menu-hover`, `desc-1`, `desc-3`, `desc-gone` | Radial menu animation, hover scale 1.5, tooltip text and position, fade after 2 s |
| S9 | **Menu cancelled** | press and hold the dog 0.6 s, release elsewhere | `menu-cancel` | Menu cleared, no tooltip |
| S10 | **Short tap** | tap the dog (< 0.5 s) | `tap` | No menu |
| S11 | **Player menu** | press and hold the player | `player-menu` | Player description ("player") |
| S12 | **Fade shader** | `Q`, snapshot, `W`, snapshot | `shaded`, `unshaded` | The fade shader is set on / removed from the game stage batch. (What it looks like is checked manually.) |
| S13 | **Light flicker** | 120 frames, record the torch distance per frame | `flicker` | Distance stays in 240 ± 5 and changes every 0.2 s |
| S14 | **Resize** | `resize(800, 1280)`, then `resize(300, 500)` | `resized` | Viewport and camera stay consistent, clicks still map to the right cells |
| S15 | **Collect (known bug B1)** | walk onto the chest | `collect` | **Today:** the snapshot records the crash (`RuntimeException … MenuButton`). In Phase 7 (7.1) this golden is re‑recorded with approval to show the COLLECT button, window, swap, OK and CANCEL |
| S16 | **Shutdown** | `stop()` | – | `dispose()` throws nothing |

Rules for picking cells in scenarios:
- **Hard‑code cells** (e.g. `new Cell(9, 22)`) once at CP0. Don't compute "the first accessible tile in iteration order": iteration order
  is a `HashMap` detail that a refactoring may change.
- Every clicked cell must be on screen at the moment of the click; the driver enforces this.

### 4.1 Interaction with planned behaviour changes

| Planned change | Effect on tests |
|----------------|-----------------|
| D4 (BFS instead of Dijkstra) | The S2 `walk-path` cell sequence **may** differ where several shortest paths exist. Choose S2/S3 target cells at CP0 that have a **unique** shortest path, verified by a one‑off check that counts shortest paths. The goldens then stay valid for both algorithms. |
| B5 fixes (font loaded once, tooltip removed after fade, dispose) | No golden change: observations use visible texts only. |
| Phase 7 B1 | Re‑record the S15 golden with approval and extend S15 to cover the COLLECT flow. |
| Phase 7 B2/B3/B4 | Not observable as state today. The fix step adds a snapshot field (e.g. drawn sprites per cell, wall bodies) and records it, with approval. |

---

## 5. Determinism checklist

All of these are verified by the spike unless marked.

- **Fixed frame time:** `Graphics.getDeltaTime()` returns 1/60 (a `MockGraphics` subclass).
- **Constant FPS:** `getFramesPerSecond()` returns 60 (the FPS label would otherwise differ).
- **Seeded randomness:** `MathUtils.random.setSeed(42)` before `game.create()` (torch flicker).
  `PhraseService` uses `Collections.shuffle` with an unseeded `Random`, but the only live phrase group has one entry, so it's deterministic.
  If more phrases are added, the driver must record "a text from group X", not the exact text.
- **Fixed window:** 400×640, the same as the desktop launcher.
- **No wall clock:** `TimeUtils` is used only in dead code (removed in 1.2). The driver doesn't sleep; it counts frames.
- **One game per test**, a fresh `Main` each time. Static counters in game code (`ID_COUNTER`) don't affect observations.

---

## 6. Running the tests

```bash
./gradlew e2e:test                          # gameplay scenarios + unit tests (headless, seconds)
./gradlew e2e:test -Dgolden.update=true     # ONLY at CP0 / approved Phase 7 steps
```

Pass `golden.update` through to the test JVM in `e2e/build.gradle` (`systemProperty 'golden.update', System.getProperty('golden.update')`).

**CI:** `.github/workflows/tests.yml` (modelled on the photos repo) runs on every push to `master` and on every pull request into `master`:
`./gradlew build` (all modules' tests, so `e2e:test` too) and `html:dist`, on `ubuntu-latest` with Java 21. The tests need no display.
On failure it uploads the test reports and `build/golden-actual/` diffs as a run artifact. The job clears `ANDROID_HOME`, because the
runner's preinstalled Android SDK would otherwise pull in the untested Android module.

Goldens are recorded on macOS and checked on Linux. Java float math is identical across platforms (strict IEEE since Java 17),
and libGDX's `MathUtils.sin/cos` use lookup tables, so the snapshots should match. Rounding to 1 decimal adds margin.
CP0 confirms this with a real CI run.

**Coverage:** add JaCoCo (0.8.16) to `e2e:test`, with the report over `core` classes. At CP0, review the report: every line that
is **not** covered must be either dead code listed in plan §3.3 or added as a scenario. Record the result in plan §8. Coverage
is not a gate percentage; it's a tool to find untested behaviour. If JaCoCo has trouble with JDK 25, run the test task on a JDK 21 toolchain.

---

## 7. What these tests cannot catch, and the remaining manual checks

- **Rendering:** how lights, the fade shader, sprites and layout actually look. Deferred to visual tests (§9).
- **Real input devices:** mouse hover timing and touch on Android. The scripted input calls the same `InputProcessor` methods.
- **HTML5 runtime differences:** covered only by the `html:dist` compile plus the optional Chrome smoke test.
- **Performance.**
- **Code that is unreachable because of B1:** the COLLECT button, the collect window, item swapping, and the button fading out on
  the next move (`PlayerMoveEvent`). Nothing can observe this until 7.1 fixes B1; S15 is then extended to cover it.

So each checkpoint still includes a **short manual play‑through** (2 minutes) by a human. It's no longer the main safety net.

---

## 8. Spike notes: the tricks that make it work

### 8.1 No‑op GL that reports success (headless)

The headless backend sets no `Gdx.gl` at all, and a plain mock makes shader compilation "fail". This proxy answers
"compiled/linked/complete" and hands out ids:

```java
GL20 gl = (GL20) Proxy.newProxyInstance(GL20.class.getClassLoader(), new Class[]{GL20.class}, (p, m, a) -> {
    switch (m.getName()) {
        case "glGetShaderiv": case "glGetProgramiv":
            int pname = (Integer) a[1];
            ((IntBuffer) a[2]).put(0, (pname == GL20.GL_COMPILE_STATUS || pname == GL20.GL_LINK_STATUS) ? 1 : 0);
            return null;
        case "glCheckFramebufferStatus": return GL20.GL_FRAMEBUFFER_COMPLETE;   // RayHandler's FBO
        case "glGetString": return "noop";
        case "glGetShaderInfoLog": case "glGetProgramInfoLog": return "";
        case "glGetIntegerv": ((IntBuffer) a[1]).put(0, 4096); return null;
    }
    if (m.getName().startsWith("glGen") || m.getName().startsWith("glCreate")) return ids.getAndIncrement();
    Class<?> r = m.getReturnType();
    return r == boolean.class ? false : r == int.class ? 0 : r == float.class ? 0f : null;
});
Gdx.gl = Gdx.gl20 = gl;
ShaderProgram.pedantic = false;   // the fake GL reports no uniforms; the game sets "saturation"/"brightness"
```

### 8.2 Headless setup and frame loop

```java
HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
cfg.updatesPerSecond = 1;                        // the backend's own loop idles; we drive frames ourselves
new HeadlessApplication(new ApplicationAdapter() {}, cfg);   // loads natives, sets Gdx.app / Gdx.files
Gdx.graphics = new FixedGraphics();              // extends MockGraphics: 400x640, delta 1/60, fps 60
Gdx.input = input = new ScriptedInput();         // extends MockInput: stores the processor, pointer x/y, keys
// ... NoOpGL as above
MathUtils.random.setSeed(42);
Main game = new Main(); game.create(); game.resize(400, 640);
for (int i = 0; i < n; i++) game.render();       // one frame
```

- Working directory = `android/assets`, because `Gdx.files.internal` resolves against it.
- A screen position from a world position: `Vector2 s = viewport.project(world.cpy()); s.y = screenHeight - s.y;`.
- **Only click on‑screen targets.** The first spike version clicked an off‑screen blocked tile and (correctly) nothing happened.

---

## 9. Later: visual regression tests (deferred)

Not part of the current work. Kept here so the spike results aren't lost. When rendering changes become the risk
(for example when fixing B2, furniture drawing), add a second backend that runs the same scenarios with real OpenGL in a hidden
LWJGL3 window and compares screenshots with golden PNGs, per platform, with a small tolerance. The spike showed
pixel‑identical results across runs on the same machine.

### 9.1 Real GL, hidden window (LWJGL3)

```java
cfg.setWindowedMode(400, 640); cfg.setInitialVisible(false); cfg.disableAudio(true);
cfg.useVsync(false); cfg.setForegroundFPS(0);
// in the wrapping ApplicationListener.render():
Graphics real = Gdx.graphics;           // the backend re-assigns Gdx.graphics/Gdx.input every frame,
Gdx.graphics = (Graphics) Proxy.newProxyInstance(..., (p, m, a) ->   // so wrap them inside render()
        m.getName().equals("getDeltaTime") ? DT
      : m.getName().equals("getFramesPerSecond") ? 60 : m.invoke(real, a));
game.render();
Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
PixmapIO.writePNG(file, pm, -1, true);  // read pixels before the backend swaps buffers
Gdx.graphics = real;
```

- macOS: the JVM needs `-XstartOnFirstThread`, so visual tests would be a `JavaExec` main, not a JUnit test (Gradle runs tests off the main thread).
- `Gdx.input` needs the same per‑frame wrapping (a scripted pointer for `MenuStage`), and clicks are injected into the game's
  `InputMultiplexer`.
- The LWJGL3 backend calls *us*. To run linear scenario scripts, `Lwjgl3Backend` runs the scenario on a worker thread.
  `frames(n)` hands n frame tickets to the render callback and waits; every driver action is a `Runnable` executed on the
  render thread (`runOnGameThread`). `HeadlessGame` runs everything on the test thread.
