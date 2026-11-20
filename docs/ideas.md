# Ideas

Possible improvements, not planned. Each has the date it came up, the problem, a suggested fix and when it would be
worth doing. When one gets done, remove it here and describe it in [changelog.md](changelog.md).

- **Squash and stretch for the hops** (2026‑11‑07). `HopPlugin` could also scale the avatar: a little taller and narrower at the top
  of the arc, flatter and wider just before and after touching the ground. The origin of the scaling would be the bottom centre of the picture. It's
  purely visual, like the hop itself: positions, tiles and goldens stay the same, and `HopTest` would only need a check on the scale.
- **Draw only what the camera sees** (2026‑11‑08). `Board.draw` draws every tile each frame, also the ones off screen; each
  tile draws all its painted map layers. Cheap for small maps (24x16 with ~4 painted layers per cell is ~1,500 drawings per
  frame, all from one atlas texture), but a 100x100 map would be ~40,000, which costs frame time on mobile and in the browser.
  Fix: draw only the tiles inside the camera's view (about 7x11 tiles on screen), so any map stays around ~1,000 drawings. Do
  it once maps grow beyond roughly 50x50. Pixel output for visible tiles stays the same.
- **Map layers above the characters** (2026‑11‑08). All map layers are drawn under the actors (the board is drawn first),
  so tree canopies or roofs painted on a layer end up under the rabbit. Add a layer property in Tiled, e.g. `above = true`:
  the loader keeps those layers apart and the game draws them after the actors (still per tile, still from the atlas).
  Do it before painting trees or roofs that the player should walk behind.
- **Retire the old single-texture pictures** (2026‑11‑08). Maps made with tools/assets draw everything from the atlases
  (their tiles are built without a background: `TileFactory.createMapTile`). The old pictures (`floor-tile.png`,
  `wall-tile.png`, `player.png`, `dog.png`, `chest.png`, …, preloaded in `ResourcesService`) are still used by the
  original house map `android/assets/map.tmx` (e2e tests no longer run on it, only `ShippedMapsTest` loads it, and the
  board tests place a few furniture pictures through `TestMap.with`). To remove them: rebuild the house as a level in
  Tiled (or drop it), then delete the pictures, their preload list and the background constructors of
  `FloorTile`/`WallTile`. Do it once the atlases feel settled.
- **Map roles from Tiled custom types instead of layer names** (2026‑11‑08). Today the game reads roles from names typed
  by hand in Tiled: layers called `collision` (not walkable, never drawn) and `walls` (not walkable, blocks light), and
  the map property `ambientLight` (`TmxTiledMapLoader` constants). Problems: a typo or rename (`Collision`, `walls 2`)
  silently turns the layer into a plain drawn one; blocking things must be painted twice (once to see, once on
  `collision`); only one layer per role. (`atlas` is fine: the tool writes it, nobody types it.)
  Suggested fix: the tool defines custom types in `assets-src/lightgame.tiled-project` (Tiled 1.9+): a layer class
  `GameLayer` with checkboxes `blocksWalking` and `blocksLight`, and a map class `GameMap` with `ambientLight` (float,
  default 1). In Tiled roles are picked from a dropdown and ticked, layers can have any name (`fences`, `rocks`), and any
  drawn layer can block (an invisible one still works for invisible barriers). The loader reads the properties instead of
  names; the tool warns about leftovers (a layer named `collision` without the class, unknown properties). Needs:
  migrating `level1`, `PackedMapTest` (its `LevelVariant` layers), the READMEs.
- **Door animation** (2026‑11‑08). `Tilesets/Building parts/door animation sprites.png` is only a tileset: its frames
  overlap (a 3px pillar plus the door leaf every 16px; the wide door the same with 48px), so no frame grid cuts them right
  (see the note in `tools/assets/pack.json`). To animate doors, `pack.json` needs frames given as rectangles
  (`"frames": [[x, y, w, h], ...]` per row, plus how to anchor frames of different widths), and `cut_frames` in the tool
  has to support them. Check the result on the preview page.
- **Row names out of `pack.json`** (2026‑11‑09). `tools/assets/pack.json` mixes two things: the pack's layout (files,
  frame sizes, offsets, variants, terrains), which changes only with a new version of the pack, and the names of
  animation rows (`idle`, `walk_left`, `open_front`…), which change whenever the game starts using another animation.
  So a file that reads as "describe the pack once" gets edited for game decisions: naming the cow's `idle` and `walk`
  and the chest's `open_front` for `Cow` and `Chest` did exactly that. Fix: move the row names to a game-owned file,
  e.g. `tools/assets/animation-names.json` (`{"cow": ["idle", "walk"], "chest": ["open_front", "open_side"], "rabbit":
  ["idle_down", …]}`), keyed by the `pack.json` entry, so the colour variants (`cow_brown`, `cow_pink`…) share their
  entry's names as they do now. The tool merges it with `pack.json` and warns about names for entries that don't exist;
  the output (`animations.json`, the `Action` enum) stays the same. Then `pack.json` is touched only when the pack
  changes, and a new animated object means its `Sprite` line, its row names in that file and, when it's placed from a
  map, a `SpriteActor` subclass with its `TileObjectType` line. Update the READMEs (step 2 of "Adding an animated
  sprite" in README.md, "Sprites in the game" in tools/assets/README.md, whose note that animals still use `row_NN` is
  already out of date) and the `_help` notes in `pack.json`.
- **Interactable objects: one reach rule and one round-button resolver for chests, creatures, fields, trees**
  (2026‑11‑10, updated 2026‑11‑13 twice). A chest is used
  only from its left, its right and below it (`Tile.getUsedFromNeighbours()`: not diagonally, because the characters
  face only up, down, left and right, and not from above, where its button would cover the player),
  but the code using that rule knows only `Collectible`: `CollectibleFinder.entered` (shows the button),
  `BoardMovementPlugin.walkTarget` with `BoardService.isUsedFromNeighbour` (walks next to it) and
  `PlayerCollectResolver.isInReach` (keeps or fades the button), and opening means "throw out what is inside". A field
  to harvest or a tree to chop would have to pretend to be a `Collectible`. Suggested fix: an `Interactable` interface
  (its tile or tiles; its reach: from beside and below like a chest or a tree, or standing on it like a crop cell; its picture on
  the round button; the action), `Collectible` becoming one kind of it. One reach check (e.g. `isInReach(interactable,
  tile)`) used by all three places, so they can't drift apart; `PlayerCollectResolver` becomes a general action
  resolver: one button per object, fading out and back in as now, the action left to the object. Related: picking items
  up is player-only today (`PlayerCollectResolver.pickUp`); a generic per-actor pick-up plugin would let animals collect
  too (a character adding a plugin fits the architecture). Open questions: does the player harvest standing on the field
  or next to it, and is a patch one object per crop cell or one object over several cells? Do it with the first object
  that isn't a chest.
  *Update after the cow (branch `cow-state-machine`):* the cow was that first object, and it went the other way to stay
  small: `CreatureFeature` (for any `actor.Creature`) is a second resolver beside `PlayerCollectResolver` (they
  share only the tile rule, `Tile.getUsedFromNeighbours()`, since branch `chest-and-reach`). It is inside every PMD
  limit but the heaviest class in `feature` (80 statements, complexity 31, coupled to 12 classes: fifth highest in the
  game; its `run` reached the limit of 10 when the buttons moved over the creature, branch `action-button-over`, and
  had to be split), so the next kind of action there would break the build. What the shared resolver must also cover:
  - *A creature as a kind of `Interactable` whose offer changes with its state:* a paw to stroke it, the item it wishes
    for (tapped without one, the button shakes), or nothing (walking, eating). So an interactable answers "what do you offer this
    player right now" (picture and action, or none) instead of having one fixed picture.
  - *Rules that belong to the object, not the resolver:* whether an item fulfils a wish (today `CreatureFeature`
    compares item classes; should be e.g. `creature.accepts(item)`).
  - *Two things in reach at once:* each button sits over its own object since branch `action-button-over`, so a chest
    and a cow next to the player show one button each; the resolver must keep one button per object, not one in all.
  - *Who owns the hint:* `CreatureFeature` resets what the question mark says every frame, so nothing else can set a
    hint. The resolver (or the HUD) should take hints from whatever is in reach and fall back to the default.
  Worth doing before the second creature or the first field or tree.
- **Game modes by age: a simple backpack for the youngest** (2026‑11‑10). **Needs more discussion before any
  implementation: how the backpack should look and behave (for each mode) isn't decided yet.** The game should suit kids
  from about 3 years old up, with easier mechanics for the youngest. Today the player has 3 backpack slots (shown in the HUD); a chest's button
  throws its contents out, items are picked up by walking onto them and thrown out by tapping their slot, and with a
  full backpack the item stays on the ground: standing on it, the tool button offers to swap it for a backpack item
  (branch `backpack-swap`; until then a full backpack threw out its first slot's item, PR #35). Notes so far, from
  before that:
  - *Older kids:* a proper backpack, e.g. 8 slots, items of one type stacked in a slot, using and dropping items, maybe
    simple crafting. Refusing a pickup when full is fine at this age, with a hint like "drop something to make room".
  - *Youngest (around 3):* they can't read or plan ahead, and drags between slots are too fiddly, so everything is
    explained by animation and nothing is refused or lost. One carried item (or at most three) with no moving around;
    items are used automatically (carrying a carrot next to the cow feeds it, with a big happy reaction).
  - *Full slot for the youngest:* suggested is a **swap**: picking up a new item puts the carried one down where the new
    one came from (back into the chest, or on the ground), as one animation. Nothing is lost, it can be undone by walking
    back, and it matches "my hands are full". Rejected: refusing the pickup (feels like being told off), dropping the oldest
    item (something vanishes unchosen), asking which one to drop (a decision dialog), a bottomless backpack (carrying
    stops meaning anything). With three slots, one per kind (food, tool, gift), so a new item only replaces its own kind.
  - *Ideas to make "full" rare:* show the carried item in the rabbit's paws (above its head) instead of a backpack screen;
    pick up automatically on reaching a chest (no COLLECT button); split carried things (carrot, key, bucket) from
    collected treasures (flowers, eggs, coins), which fly into a counter or sticker album and never take a slot; place
    items near where they're used.
  - *Choosing the mode:* by the parent (first start, behind a simple parent gate, changeable in settings), named by play
    style rather than age (e.g. "Explorer" and "Adventurer").
  - *Code, once decided:* a game mode setting choosing the rules: what a pickup does (stack, refuse or swap), whether items
    are used by hand or automatically, and the presentation (item in the paws with auto-pickup, or the backpack window).
    Builds on "Interactable objects" above: using an item automatically is an action that runs without a button when the
    player is in reach.
- **UI pictures drawn on the game stage** (2026‑11‑13, extended 2026‑11‑15). Some pictures drawn on the map (game
  stage actors) come from the UI atlas, because they exist only in the UI pack and `sprites.atlas` is built from the
  sprites pack only. Each switch between atlas textures while drawing makes `SpriteBatch` send what it collected (one
  more draw call). Found by searching the code for `UiImage` on the game stage (2026‑11‑15):
  - the stars popping out of an opened chest (`Sparkles`, `UiImage.SPARKLE`): up to 2 more draw calls, for under a
    second;
  - the question mark on an empty field's sign (`Field`, `UiImage.HINT_ICON`, branch `field-grows-over-time`): about
    2 more per empty field (sign, "?", sprout), for as long as it stays empty;
  - the cow's wish bubble (`PictureBubble`, `UiImage.PICTURE_BUBBLE`, with the crop from `sprites.atlas` inside):
    about 2 more per wishing cow, for as long as it wishes;
  - the heart floating over the player after a stroke (`CreatureFeature`, `UiImage.HEART`): up to 2 more for a moment;
  - the cow's emotes (`Cow.showEmote`, tiny emotes): not called right now.

  (The hand on the stroke button and the whole HUD are on the UI stage: they stay with the UI atlas.) Nothing to notice
  now; it would matter with many such objects at once (e.g. 50 wishing animals: ~100 draw calls instead of a few), on
  weak phones and in the browser. **The owner's choice of fix:** duplicate the UI pack pieces the game stage uses into
  the game sprites, so the map draws from `sprites.atlas` only. The tool would copy chosen UI pack pieces into
  `sprites.atlas` (e.g. a list in `pack.json` naming the UI pack file and piece) and the game loads them as `Sprite`s;
  the places above use those, and `UiImage` stays for the UI only (`HINT_ICON` back to the hint button alone). Before
  doing it, count draw calls per frame (libGDX `GLProfiler`, or `SpriteBatch.renderCalls`) on a busy map to see the
  gain, and search again for `UiImage` on the game stage in case more came meanwhile.
- **All texts in one place** (2026‑11‑14). The words the game shows are spread over the code: `Hud.HINT` ("play
  around!"), `CreatureFeature.FIND` ("find"), `PhraseGroup.NO_PATH` ("no path"), the "SETTINGS" title
  (`SettingsPanel`), the radial menu's "button" caption (`MenuButton`), and the descriptions in the characters
  (`Player` "player", `Chest` "chest", `Lighter` "lighter", `Dog`, the cow's sentence in `Cow`'s constructor). Found
  by the bug hunt (2026‑11‑14): most players can't read, and some of these are placeholders. Suggested fix: one place
  that lists every text the player sees, e.g. a `Text` enum (key and English wording) or a libGDX `I18NBundle`
  properties file (it works in GWT), and the code asking for a key instead of writing the words. Two gains: a
  translation later only adds a file, and the list shows every place that still uses words, so each can be judged
  (keep it for readers, or replace it with a picture, as the "Built" HUD already does). Debug texts (the state labels)
  stay out of it. Open: enum or bundle; whether descriptions belong there or in the map data.
- **The watering can runs dry** (2026‑11‑15). A field gives crops too easily: the can never runs dry, so watering
  costs nothing. Suggested fix: the can holds a few pours (e.g. 3) and is filled again by walking next to water (the
  island is surrounded by it; later a well, the pack has `Objects/Water well.png`). An empty can shakes its button and
  the face says "find" with a water drop, like the cow's wish. Open: where the can's level is shown (a small meter on
  the field's button or a HUD slot), whether the rabbit needs a filling animation, and whether the can is an item in
  the backpack or always carried. Worth doing together with the owner, once growing over time has been played a while.
- **Seeds to plant a field again** (2026‑11‑15). After picking, a field could stay bare soil until it is given a seed
  bag of its crop: the farm items sheet's left column has a bag per crop (`Farming Plants items.png`, column 0), and
  chests could hold them besides crops. Giving works like feeding the cow (the button shows the bag; without one it
  shakes and the face says "find" with the bag). It ties fields to exploring the map. Open: one bag per crop kind or
  any bag fits any field; whether a bag could choose what grows (a field without `cropType`); what a chest holds then.
  Bigger than the can: a new item kind and chest contents. Worth doing together with the owner.
- **No one-tile corridors on the levels** (2026‑11‑16). Found by the play-testers of the chickens eating crops: a
  chicken that isn't going for a crop wanders as it likes, and when it stands in a one-tile corridor or gap (between
  water, fences or fields) the player can't get past it, sometimes for most of a minute (on level 1 one sat 50 s in the
  gap beside a field). The owner's view: it is a matter of level design, the levels shouldn't have such places. Suggested
  fix: a check in `ShippedMapsTest` that every shipped map has no chokepoint on the walkable cells, i.e. no single
  walkable cell whose blocking cuts the walkable area in two (a "cut vertex" of the grid, found by trying each cell or
  with Tarjan's algorithm), maybe allowing dead ends of one cell; it fails with the cells to widen. Worth doing when
  the next level is drawn, or when level 1 gets its next change (it has such gaps today, e.g. between the fields).
  Open: whether fields, chests and fences count as blocking (yes: they never move), and whether a gap an animal can't
  reach matters.
