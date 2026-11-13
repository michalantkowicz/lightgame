package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.event.BackpackItemEjectedEvent;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.RoundButton;
import com.mantkowicz.light.ui.UiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The HUD ("face is the button", docs/hud-design.md). Bottom centre: the player's face in a frame, two backpack
 * slots on its left, the player's stamina as a bar under the face ({@link StaminaBar}); on its right a round button:
 * the hint (a question mark, which makes the face cheer and say "play around!" in a bubble over it) or, over it when
 * there is something to do, the action button ({@link ActionButton}). Top right: the settings gear, which opens the
 * settings only after a 3-second hold. Nothing else; the world gets the rest of the screen.
 * <p>
 * Unlike the design, the action isn't on the face but on its own round button beside it; the day picture is left out
 * for now.
 */
// baseline (docs/code-quality-plan.md): over the PMD limit before the check came, to be refactored
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class Hud extends Group {
    /** HUD pieces are drawn one step bigger than the rest of the UI ({@link ResourcesService#UI_SCALE}). */
    public static final int SCALE = 4;
    /** Two slots by design: few enough for the youngest players. */
    static final int BACKPACK_SLOTS = 2;
    /** GAP: between the face and the pieces beside it; SLOT_GAP: between two backpack slots. */
    private static final float MARGIN = 16, GAP = 40, SLOT_GAP = 12;
    /** A backpack slot (80% of the 120x128 it was at first) and the item drawn in it. */
    private static final float SLOT_WIDTH = 96, SLOT_HEIGHT = 104, ITEM_SIZE = 64; // a 16 px item drawn 4x
    /** The face's bottom; the stamina bar is BAR_GAP under it. */
    private static final float FACE_Y = MARGIN + 58, BAR_GAP = 10;
    /**
     * Where the action button stands: its right edge this far right of the screen's middle (as far as the slots' left
     * edge is left of it), its bottom this high above the screen's bottom (on the face's bottom, like the slots).
     */
    public static final float ACTION_RIGHT = Portrait.WIDTH / 2f + GAP + BACKPACK_SLOTS * SLOT_WIDTH
            + (BACKPACK_SLOTS - 1) * SLOT_GAP, ACTION_Y = FACE_Y;
    /** What the player says when the hint button is tapped (later: where to go next). */
    static final String HINT = "play around!";
    /**
     * The face's idle by the stamina bar's face: awake (green: still, blinking, twitching an ear), tired (yellow:
     * yawning), asleep (red: falls asleep, then sleeps with a bubble).
     */
    static final Portrait.Idle AWAKE = new Portrait.Idle("awake", null, UiImage.TEEMO_STILL, 2.5f,
            UiImage.TEEMO_BLINK, UiImage.TEEMO_BLINK, UiImage.TEEMO_EAR),
            TIRED = new Portrait.Idle("tired", null, UiImage.TEEMO_STILL, 1, UiImage.TEEMO_YAWN),
            ASLEEP = new Portrait.Idle("asleep", UiImage.TEEMO_FALL_ASLEEP, null, 0, UiImage.TEEMO_SLEEP,
                    UiImage.TEEMO_SLEEP_MORE);
    /** Between the face's top and the tip of its bubble's pointer. */
    private static final float BUBBLE_GAP = 6;
    /** How far a shaken slot moves up and down, and how long the first move takes (the whole shake is 6 times that). */
    private static final float SHAKE = 8, SHAKE_SECONDS = 0.04f;
    /** Between the gear and the screen's corner: room for the circle at its top right while it is held. */
    private static final float GEAR_MARGIN = 48;

    private final ResourcesService resources;
    private final GameEventService events;
    private final Player player;
    /** Throws the item in a backpack slot out next to the player. */
    private final IntConsumer throwOut;
    private final Group row = new Group();
    private final Portrait face;
    private final List<Image> slots = new ArrayList<>();
    private final List<Image> slotItems = new ArrayList<>();
    private final List<Item> shownItems = new ArrayList<>();
    private final StaminaBar stamina;
    private final HoldButton gear;
    private final RoundButton hint;
    private SpeechBubble bubble;
    private SettingsPanel settings;
    private float laidOutWidth, laidOutHeight;

    /**
     * @param throwOut throws the item in a backpack slot (0 = the left one) out next to the player, when its slot is
     *                 tapped
     */
    public Hud(GameContext context, Player player, IntConsumer throwOut) {
        this.throwOut = throwOut;
        this.resources = context.getResourcesService();
        this.events = context.getGameEventService();
        this.player = player;
        setTouchable(Touchable.childrenOnly);

        // taps on the bottom row stay in the HUD: they don't walk the player to a cell hidden behind it
        row.setTouchable(Touchable.enabled);
        row.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
        addActor(row);

        face = new Portrait(resources, "teemo", AWAKE);
        row.addActor(face);

        for (int i = 0; i < BACKPACK_SLOTS; i++) {
            Image slot = new Image(UiStyle.stretching(resources, UiImage.SLOT));
            slot.setSize(SLOT_WIDTH, SLOT_HEIGHT);
            int index = i;
            slot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    throwOut.accept(index); // what is in it jumps out next to the player
                }
            });
            row.addActor(slot);
            slots.add(slot);
            Image item = new Image();
            item.setScaling(Scaling.fit);
            item.setSize(ITEM_SIZE, ITEM_SIZE);
            item.setTouchable(Touchable.disabled); // taps go to the slot under it
            row.addActor(item);
            slotItems.add(item);
            shownItems.add(null);
        }

        stamina = new StaminaBar(resources, player.getStamina());
        row.addActor(stamina);

        gear = new HoldButton(resources, UiImage.GEAR_BUTTON, SCALE, this::openSettings);
        addActor(gear);

        hint = new RoundButton(resources, resources.getUiImage(UiImage.HINT_ICON));
        hint.setName("HINT");
        hint.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showHint();
            }
        });
        addActor(hint);
    }

    private Image picture(UiImage image, float scale) {
        Image picture = new Image(resources.getUiImage(image));
        picture.setSize(picture.getPrefWidth() * scale, picture.getPrefHeight() * scale);
        return picture;
    }

    @Override
    public void act(float delta) {
        followCamera();
        showBackpack();
        for (BackpackItemEjectedEvent ejected = events
                .poll(BackpackItemEjectedEvent.class); ejected != null; ejected = events
                        .poll(BackpackItemEjectedEvent.class)) {
            shake(ejected.getSlot());
        }
        UiImage mood = stamina.getMood();
        face.setIdle(mood == UiImage.MOOD_SAD ? ASLEEP : mood == UiImage.MOOD_NEUTRAL ? TIRED : AWAKE);
        super.act(delta);
    }

    /** Stays on the screen's edges whatever the window size (the UI camera stays in the middle of the screen). */
    private void followCamera() {
        if (getStage() == null) {
            return;
        }
        Viewport viewport = getStage().getViewport();
        Camera camera = getStage().getCamera();
        setBounds(camera.position.x - viewport.getWorldWidth() / 2f, camera.position.y - viewport.getWorldHeight() / 2f,
                viewport.getWorldWidth(), viewport.getWorldHeight());
        if (getWidth() != laidOutWidth || getHeight() != laidOutHeight) {
            layOut();
        }
    }

    private void layOut() {
        laidOutWidth = getWidth();
        laidOutHeight = getHeight();
        float faceX = (getWidth() - Portrait.WIDTH) / 2f, faceY = FACE_Y;
        face.setPosition(faceX, faceY);
        // the pieces beside the face line up with the bottom of its frame
        float ground = faceY;

        float x = faceX - GAP;
        for (int i = slots.size() - 1; i >= 0; i--) {
            Image slot = slots.get(i);
            x -= slot.getWidth();
            slot.setPosition(x, ground);
            Image item = slotItems.get(i);
            item.setPosition(x + (slot.getWidth() - item.getWidth()) / 2f,
                    slot.getY() + (slot.getHeight() - item.getHeight()) / 2f + 4);
            x -= SLOT_GAP;
        }

        stamina.setPosition(Math.round((getWidth() - stamina.getWidth()) / 2f), faceY - BAR_GAP - stamina.getHeight());

        // the row is only as wide as its pieces: taps beside it still reach the map
        float left = slots.get(0).getX(), right = faceX + Portrait.WIDTH;
        row.setBounds(left, 0, right - left, faceY + Portrait.HEIGHT);
        for (com.badlogic.gdx.scenes.scene2d.Actor child : row.getChildren()) {
            child.moveBy(-left, 0);
        }

        hint.setPosition(getWidth() / 2f + ACTION_RIGHT - hint.getWidth(), ACTION_Y); // where the action button goes over it
        placeBubble();
        gear.setPosition(getWidth() - gear.getWidth() - GEAR_MARGIN, getHeight() - gear.getHeight() - GEAR_MARGIN);
        if (settings != null) {
            settings.remove();
            openSettings();
        }
    }

    /** A quick shake up and down of a backpack slot and what is in it: its item jumped out by itself. */
    private void shake(int slot) {
        for (Image piece : List.of(slots.get(slot), slotItems.get(slot))) {
            piece.clearActions();
            piece.addAction(Actions.sequence(
                    Actions.moveBy(0, SHAKE, SHAKE_SECONDS, Interpolation.sine),
                    Actions.moveBy(0, -2 * SHAKE, 2 * SHAKE_SECONDS, Interpolation.sine),
                    Actions.moveBy(0, 2 * SHAKE, 2 * SHAKE_SECONDS, Interpolation.sine),
                    Actions.moveBy(0, -SHAKE, SHAKE_SECONDS, Interpolation.sine)));
        }
    }

    /** The backpack shows what the player carries (its first slots). */
    private void showBackpack() {
        List<Item> items = player.getInventory().getItems();
        for (int i = 0; i < slotItems.size(); i++) {
            Item item = i < items.size() ? items.get(i) : null;
            if (item != shownItems.get(i)) {
                shownItems.set(i, item);
                slotItems.get(i).setDrawable(item == null ? null : new TextureRegionDrawable(item.getPicture()));
            }
        }
    }

    /** The face says the hint in a bubble over it (a new tap starts it again). */
    private void showHint() {
        if (bubble != null) {
            bubble.remove();
        }
        bubble = new SpeechBubble(resources, HINT);
        addActor(bubble);
        placeBubble();
        face.play(UiImage.TEEMO_CHEER, SpeechBubble.SECONDS); // cheering while it says it
    }

    /** The bubble's pointer just over the middle of the face's top. */
    private void placeBubble() {
        if (bubble != null) {
            bubble.setPosition(Math.round(face.getX() + row.getX() + (Portrait.WIDTH - bubble.getWidth()) / 2f),
                    face.getY() + Portrait.HEIGHT + BUBBLE_GAP);
        }
    }

    private void openSettings() {
        settings = new SettingsPanel(resources, getWidth(), getHeight());
        addActor(settings);
    }

    // what the HUD shows, for tests (e2e GameDriver)

    public Portrait getFace() {
        return face;
    }

    public HoldButton getGear() {
        return gear;
    }

    /** The round button right of the face when there is nothing to do (the action button covers it otherwise). */
    public RoundButton getHint() {
        return hint;
    }

    /** The bubble over the face, or null. */
    public SpeechBubble getBubble() {
        return bubble != null && bubble.getStage() != null ? bubble : null;
    }

    /** The open settings window, or null. */
    public SettingsPanel getSettings() {
        return settings != null && settings.getStage() != null ? settings : null;
    }

    /** The backpack slots, left to right: tapping one throws out what is in it. */
    public List<Image> getSlots() {
        return slots;
    }

    /** What each backpack slot holds, null for empty. */
    public List<Item> getBackpack() {
        return shownItems;
    }

    public StaminaBar getStamina() {
        return stamina;
    }

    /** The bottom row (backpack, face, stamina bar): taps on it stay in the HUD. */
    public Group getRow() {
        return row;
    }
}
