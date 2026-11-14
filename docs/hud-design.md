# HUD design

The HUD for children aged 3–8, phone in portrait (800 × 1280). Pictures first, words only for children who read (the
age setting decides). Few, big tap targets that stay in the same place.

Chosen 2026‑11‑12 ("face is the button", from eleven mock-ups on the owner's private artifact page "Lightgame HUD
Variants"), then changed while building it. **Built** below is what the code does today (`ui.hud`); **Planned** is
still the target. The HUD is drawn at 4 pack pixels per pixel (`Hud.SCALE`), text at 3 (`ResourcesService.UI_SCALE`).

## Built

| Where | What |
|---|---|
| Bottom centre | **The face** (`Portrait`, 184 × 184): Teemo from the pack's emotes in a dark wooden frame (`backdrops_c1`). Its idle follows stamina: awake (blinks, twitches an ear), tired (yawns), asleep. Any code can make it react for a while or once (`Portrait.play`, e.g. cheering). |
| Under the face | **Stamina** (`StaminaBar`): 14 segments, green for what is left, with a mood face (happy, neutral at half or less, sad at a quarter or less). The model is `Stamina` on the player (`use`, `fill`, `set`); opening a chest or time passing don't lower it yet. |
| Left of the face | **Backpack**: 2 slots showing what the player carries. Tapping a slot throws its item out next to the player (with nowhere for it to land, the slot shakes). With a full backpack, stepping onto an item throws out the first slot's item (the slot shakes) and takes the new one a second later. |
| Right of the face | **The hint** (the pack's brown round button): a question mark; tapping it makes the face cheer and say "play around!" in a bubble over it (`SpeechBubble`: the dialog box turned so its pointer faces down). Next to a cow wishing for a carrot the player doesn't carry it says "find" with the carrot's picture. What it should do later is open. |
| On the map, over a chest | **Action button** (`ActionButton`, the pack's dark brown round button, a bit smaller than the hint, on the UI stage, following what it acts on): next to a chest it appears over it, showing the open chest; tapping it opens the chest, which throws its items out onto free tiles. Then it shows the closed chest and closes the lid; the chest can be opened and closed again as often as the player likes, but it doesn't refill. |
| On the map, over an animal | **Caring for it** (`CreatureFeature`), on action buttons in a row over the cow. A hand (the pack's paw) strokes it, also while it is hungry: a heart over it, a small one floats up over the player. When the cow wishes for a carrot, a second button with the carrot (right of the hand) takes the wish bubble's place and gives it (the slot shakes, the cow eats and shows a heart); without a carrot it shakes and the face says "find" with the carrot. |
| On the map | **Wishes as picture bubbles** (`PictureBubble`): a small grey bubble with a picture over a character, in front of everyone on the map; the cow shows a carrot when it is hungry (`CowState.WISHING`; not fed for 90 s it gives up and grazes), hidden while the player is next to it (the action button with the carrot is there). |
| Top right | **Settings gear** (`HoldButton`): opens only after a 3-second hold, while the pack's stamina circle fills at its top right. The panel (`SettingsPanel`) has only a title and a check mark to close. |

Collecting happens on the map: items lie on tiles, bobbing (`ItemOnGround`), and are picked up by walking onto them.

Changed from the first design: the action is a round button over the thing it acts on, not the face itself; stamina
is a bar, not apples; the day picture (sun, moon, weather) is left out until the game has a day; the backpack has 2
slots.

## Planned

In this order; each step its own branch, scenario tests through `GameDriver` (`e2e/README.md`).

1. **Settings.** Age (3–8, showing what it changes, e.g. "BUTTONS: PICTURE ONLY"), music, sounds. The age decides:
   words on buttons (readers only), talks on or off (off for 3–4), anything else age-dependent (see "Game modes by
   age" in `docs/ideas.md`).
2. **Talks (age 5+), no window.** The HUD slides down (only the gear stays) and the map darkens a little (×0.8). The
   other character's face on the left with its line in a speech bubble (`dialog_box`, text up to ~500 px wide); the
   player's face bottom right with 1–3 short answers in a row (`play_button/blank`, about 10 letters each, enforced by a
   check on the dialog data). A chosen answer and the previous line move up and fade. The last answer has the
   `continue_indicator` and ends the talk: the HUD comes back and a picture bubble over the character sums up what it
   wants. While a talk is open, map taps don't walk.
3. **Age 3–4 hint.** The hint button's "play around!" is a placeholder (next to a cow wishing for a carrot it already
   says "find" with a carrot): when a character wants something, tapping the
   hint shows a big arrow bouncing over the place to go, with a small picture of what is there. Open: what to do when
   that place is off screen (an arrow at the screen edge, or the camera gliding there and back).
4. **A real day.** Day, time and weather (the day picture next to the HUD, the time for readers), and stamina lowered
   by time and work, refilled by sleep.

Missing models for all this: dialog data (lines, 1–3 answers, which one ends, the summary
picture), the settings and where they are stored, a day clock and weather, portraits for every character that talks.
