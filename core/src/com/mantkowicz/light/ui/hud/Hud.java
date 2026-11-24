package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.quest.QuestJournal;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

/**
 * The HUD ("face is the button", docs/hud-design.md). Bottom centre: the player's face in a frame, three backpack
 * slots on its left ({@link BackpackSlots}), the player's stamina as a bar under the face ({@link StaminaBar}); on its right the big round
 * <b>tool button</b> ({@link ToolButton}): what the player can do where it stands (e.g. the scythe on grass), else the
 * hint (a question mark, which makes the face cheer and say "play around!" in a bubble over it). What there is to do
 * with the things on the map is on the small <b>action buttons</b> over them ({@link ActionButton}). Top right: the settings gear, which opens the
 * settings only after a 3-second hold. Nothing else; the world gets the rest of the screen.
 * <p>
 * The face reacts to the field: picking a crop makes it happy, pulling out a weed makes it sweat and say so; tapped
 * (or the stamina bar under it), it plays a gesture ({@link FaceReactions}).
 * <p>
 * Unlike the design, the action isn't on the face but on its own round button beside it (the tool button); the day picture is left out
 * for now.
 */
// baseline (docs/code-quality-plan.md): over the PMD limit before the check came, to be refactored
@SuppressWarnings("PMD.CouplingBetweenObjects")
public class Hud extends Group {
    /** HUD pieces are drawn one step bigger than the rest of the UI ({@link ResourcesService#UI_SCALE}). */
    public static final int SCALE = 4;
    /**
     * GAP: between the face and the pieces beside it. The slots are a group, apart from the face: GAP is about the gap
     * between two slots ({@link BackpackSlots#SLOT_GAP}) × φ².
     */
    private static final float MARGIN = 16, GAP = 20;
    /** The face's bottom; the stamina bar is BAR_GAP under it. */
    private static final float FACE_Y = MARGIN + 58, BAR_GAP = 10;
    /**
     * The pieces beside the face (the slots, the tool button round or wide) have their middles on one line this high
     * above the face's bottom: the face's lower golden section (its height / φ², about 70 of 184).
     */
    static final float ROW_MIDDLE = (float) (Portrait.HEIGHT / Math.pow((1 + Math.sqrt(5)) / 2, 2));
    /** What the player says when the tool button is tapped without a tool (later: where to go next). */
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
    /** Between the gear and the screen's corner: room for the circle at its top right while it is held. */
    private static final float GEAR_MARGIN = 48;

    private final ResourcesService resources;
    private final Player player;
    private final Group row = new Group();
    private final Portrait face;
    private final BackpackSlots backpack;
    /** The quest going on, over the backpack, or null before {@link #showQuests}. */
    private QuestPanel questPanel;
    private final ItemFlights flights;
    private final StaminaBar stamina;
    private final HoldButton gear;
    /** The big round button right of the face: the tool for where the player stands, or the hint. */
    private final ToolButton toolButton;
    private final FaceReactions reactions;
    private SpeechBubble bubble;
    /** What the face says when the tool button is tapped without a tool, and the picture after it (or null). */
    private String hintText = HINT;
    private TextureRegion hintPicture;
    private SettingsPanel settings;
    private float laidOutWidth, laidOutHeight;

    /**
     * @param throwOut throws the item in a backpack slot (0 = the left one) out next to the player, when its slot is
     *                 tapped; false when it has nowhere to land (the slot shakes)
     */
    public Hud(GameContext context, Player player, IntPredicate throwOut) {
        this.resources = context.getResourcesService();
        this.player = player;
        setTouchable(Touchable.childrenOnly);

        // taps on the bottom row's pieces stay in the HUD (they don't walk the player to a cell hidden behind them);
        // taps in the gaps between them reach the map (an animal showing there can be tapped)
        row.setTouchable(Touchable.childrenOnly);
        addActor(row);

        face = new Portrait(resources, "teemo", AWAKE);
        row.addActor(face);
        backpack = new BackpackSlots(resources, player.getInventory().getCapacity(), throwOut, row);
        flights = new ItemFlights(this, backpack);
        reactions = new FaceReactions(context.getGameEventService(), resources, face,
                (text, seconds) -> say(text, null, seconds), backpack::shake);
        keepTaps(face, reactions::tapped);

        stamina = new StaminaBar(resources, player.getStamina());
        keepTaps(stamina, reactions::tapped);
        row.addActor(stamina);

        gear = new HoldButton(resources, UiImage.GEAR_BUTTON, SCALE, this::openSettings);
        addActor(gear);

        toolButton = new ToolButton(resources, this::showHint, backpack.getWidth()); // wide, as wide as the backpack
        addActor(toolButton);
    }

    /**
     * The quest going on, in a row just over the backpack slots ({@link QuestPanel}): over the first two slots who
     * asks for what, over the third the quest's slot.
     *
     * @param throwOutSlot throws what is in the quest's slot out next to the player; false when it has nowhere to land
     */
    public void showQuests(QuestJournal journal, BooleanSupplier throwOutSlot) {
        questPanel = new QuestPanel(resources, journal, 2 * BackpackSlots.SLOT_WIDTH + BackpackSlots.SLOT_GAP,
                BackpackSlots.SLOT_GAP, BackpackSlots.SLOT_WIDTH, BackpackSlots.SLOT_HEIGHT, throwOutSlot);
        addActor(questPanel);
        flights.setQuestPanel(questPanel);
        placeQuestPanel();
    }

    /** Just over the backpack slots, its left edge on theirs. */
    private void placeQuestPanel() {
        if (questPanel != null && !backpack.getSlots().isEmpty()) {
            Actor first = backpack.getSlots().get(0);
            questPanel.setPosition(row.getX() + first.getX(),
                    row.getY() + first.getY() + first.getHeight() + BackpackSlots.SLOT_GAP);
        }
    }

    public QuestPanel getQuestPanel() {
        return questPanel;
    }

    /** Items flying out of the slots: to an animal on the map, into the quest's slot. */
    public ItemFlights getFlights() {
        return flights;
    }

    /**
     * The map the player is in (the first one, or another one through a door): from now on the HUD reacts to what
     * happens there. What it shows (the backpack, the stamina) goes with the player.
     */
    public void listenTo(GameEventService events) {
        reactions.listenTo(events);
    }

    /**
     * A tap on a backpack slot first offers its item here (0 = the left slot), e.g. to a cow next to the player wishing
     * for it; only when this says false does the item jump out next to the player.
     */
    public void setGiveAway(IntPredicate giveAway) {
        backpack.setGiveAway(giveAway);
    }

    /** Taps on this piece stay in the HUD (they don't reach the map behind it) and do this (the face answers). */
    private static void keepTaps(com.badlogic.gdx.scenes.scene2d.Actor piece, Runnable onTap) {
        piece.setTouchable(Touchable.enabled);
        piece.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                onTap.run();
                return true;
            }
        });
    }

    private Image picture(UiImage image, float scale) {
        Image picture = new Image(resources.getUiImage(image));
        picture.setSize(picture.getPrefWidth() * scale, picture.getPrefHeight() * scale);
        return picture;
    }

    @Override
    public void act(float delta) {
        followCamera();
        backpack.show(player.getInventory().getItems(), delta);
        toolButton.tintGoing(backpack.getSwapTint()); // the item that would go pulses on the tool button too
        reactions.react(); // a slot an item was taken out of shakes, the face reacts
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
        float middle = faceY + ROW_MIDDLE;

        backpack.layOut(faceX - GAP, middle);

        stamina.setPosition(Math.round((getWidth() - stamina.getWidth()) / 2f), faceY - BAR_GAP - stamina.getHeight());

        // the row is only as wide as its pieces: taps beside it still reach the map
        float left = backpack.getSlots().get(0).getX(), right = faceX + Portrait.WIDTH;
        row.setBounds(left, 0, right - left, faceY + Portrait.HEIGHT);
        for (com.badlogic.gdx.scenes.scene2d.Actor child : row.getChildren()) {
            child.moveBy(-left, 0);
        }

        // its right edge as far right of the face as the slots' left edge is left of it: the same margin on both sides
        toolButton.place(faceX + Portrait.WIDTH + GAP + backpack.getWidth(), middle);
        placeBubble();
        placeQuestPanel();
        gear.setPosition(getWidth() - gear.getWidth() - GEAR_MARGIN, getHeight() - gear.getHeight() - GEAR_MARGIN);
        if (settings != null) {
            settings.remove();
            openSettings();
        }
    }

    /** The face says the hint in a bubble over it (a new tap starts it again), as when the tool button is tapped without a tool. */
    public void showHint() {
        say(hintText, hintPicture, SpeechBubble.SECONDS);
        face.play(UiImage.TEEMO_CHEER, SpeechBubble.SECONDS); // cheering while it says it
    }

    /**
     * A bubble over the face (replacing one still there) says something for a while, then fades out.
     *
     * @param picture shown after the text, or null
     */
    public void say(String text, TextureRegion picture, float seconds) {
        if (bubble != null) {
            bubble.remove();
        }
        bubble = new SpeechBubble(resources, text, picture, seconds);
        addActor(bubble);
        placeBubble();
    }

    /**
     * What the tool button says from now on (without a tool), e.g. "find" and a carrot next to a cow wishing for one.
     *
     * @param picture shown after the text, or null
     */
    public void setHint(String text, TextureRegion picture) {
        hintText = text;
        hintPicture = picture;
    }

    /** Back to the hint for when there is nothing to do ("play around!"). */
    public void resetHint() {
        setHint(HINT, null);
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

    /** The big round button right of the face: the tool for where the player stands, or the hint. */
    public ToolButton getToolButton() {
        return toolButton;
    }

    /** The picture after the text the tool button says now (without a tool) ("find" and a carrot), or null. */
    public TextureRegion getHintPicture() {
        return hintPicture;
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
        return backpack.getSlots();
    }

    /** The backpack slots themselves: what is chosen for a swap, the pictures in them. */
    public BackpackSlots getBackpackSlots() {
        return backpack;
    }

    /** What each backpack slot holds, null for empty. */
    public List<Item> getBackpack() {
        return backpack.getShownItems();
    }

    public StaminaBar getStamina() {
        return stamina;
    }

    /** The bottom row (backpack, face, stamina bar): taps on it stay in the HUD. */
    public Group getRow() {
        return row;
    }
}
