package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.Player;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Direction;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * The HUD ("face is the button", docs/hud-design.md). Bottom centre: the player's face in a square frame, the backpack
 * slot on its left, the day as a picture on its right, stamina as apples under the face. Top right: the settings gear,
 * which opens the settings only after a 3-second hold. Nothing else; the world gets the rest of the screen.
 * <p>
 * Step 1 of the design: the day, stamina and backpack size are fixed values until the game has them (step 7); the
 * face isn't the action button yet (step 2).
 */
public class Hud extends Group {
    /** HUD pieces are drawn one step bigger than the rest of the UI ({@link ResourcesService#UI_SCALE}). */
    public static final int SCALE = 4;
    /** Fixed until the game counts stamina (step 7). */
    static final int APPLES = 3, FULL_APPLES = 3;
    /** One slot by design, for the youngest players. */
    static final int BACKPACK_SLOTS = 1;
    private static final float MARGIN = 16, GAP = 24, APPLE_SIZE = 50;
    /** Room for the gear's ring between the gear and the screen's corner. */
    private static final float GEAR_MARGIN = 44;
    /**
     * The rabbit in its idle frames (x, y from the top, width, height in the packed frame): the player's sprite stands
     * in for a portrait until the game has portraits.
     */
    private static final int[] RABBIT_FACE = {13, 7, 16, 18};

    private final ResourcesService resources;
    private final Player player;
    private final Group row = new Group();
    private final Portrait face;
    private final List<Image> slots = new ArrayList<>();
    private final List<Image> slotItems = new ArrayList<>();
    private final List<Item> shownItems = new ArrayList<>();
    private final Image day;
    private final List<Image> apples = new ArrayList<>();
    private final HoldButton gear;
    private SettingsPanel settings;
    private float laidOutWidth, laidOutHeight;

    public Hud(GameContext context, Player player) {
        this.resources = context.getResourcesService();
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

        face = new Portrait(resources, resources.getAnimation(Sprite.RABBIT, Action.IDLE, Direction.DOWN),
                ResourcesService.animationName(Sprite.RABBIT, Action.IDLE, Direction.DOWN), RABBIT_FACE, 8);
        row.addActor(face);

        for (int i = 0; i < BACKPACK_SLOTS; i++) {
            Image slot = new Image(UiStyle.stretching(resources, UiImage.SLOT));
            slot.setSize(30 * SCALE, 32 * SCALE);
            row.addActor(slot);
            slots.add(slot);
            Image item = new Image();
            item.setScaling(Scaling.fit);
            item.setSize(84, 84);
            row.addActor(item);
            slotItems.add(item);
            shownItems.add(null);
        }

        day = picture(UiImage.SUN, 3); // fixed until the game has a day and weather (step 7)
        row.addActor(day);

        for (int i = 0; i < APPLES; i++) {
            Image apple = new Image(UiStyle.image(resources, UiImage.APPLE), Scaling.fit);
            apple.setSize(APPLE_SIZE, APPLE_SIZE);
            apple.getColor().a = i < FULL_APPLES ? 1 : 0.3f;
            row.addActor(apple);
            apples.add(apple);
        }

        gear = new HoldButton(resources, UiImage.GEAR_BUTTON, SCALE, this::openSettings);
        addActor(gear);
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
        float faceX = (getWidth() - Portrait.WIDTH) / 2f, faceY = MARGIN + APPLE_SIZE + 8;
        face.setPosition(faceX, faceY);
        float faceMiddle = faceY + Portrait.HEIGHT / 2f;

        float x = faceX - GAP;
        for (int i = slots.size() - 1; i >= 0; i--) {
            Image slot = slots.get(i);
            x -= slot.getWidth();
            slot.setPosition(x, faceMiddle - slot.getHeight() / 2f);
            Image item = slotItems.get(i);
            item.setPosition(x + (slot.getWidth() - item.getWidth()) / 2f, slot.getY() + (slot.getHeight() - item.getHeight()) / 2f + 4);
            x -= GAP / 2f;
        }
        day.setPosition(faceX + Portrait.WIDTH + GAP, faceMiddle - day.getHeight() / 2f);

        float applesWidth = APPLES * APPLE_SIZE;
        for (int i = 0; i < apples.size(); i++) {
            apples.get(i).setPosition((getWidth() - applesWidth) / 2f + i * APPLE_SIZE, MARGIN);
        }

        // the row is only as wide as its pieces: taps beside it still reach the map
        float left = slots.get(0).getX(), right = day.getX() + day.getWidth();
        row.setBounds(left, 0, right - left, faceY + Portrait.HEIGHT);
        for (com.badlogic.gdx.scenes.scene2d.Actor child : row.getChildren()) {
            child.moveBy(-left, 0);
        }

        gear.setPosition(getWidth() - gear.getWidth() - GEAR_MARGIN, getHeight() - gear.getHeight() - GEAR_MARGIN);
        if (settings != null) {
            settings.remove();
            openSettings();
        }
    }

    /** The backpack shows what the player carries (its first slots). */
    private void showBackpack() {
        List<Item> items = player.getInventory().getItems();
        for (int i = 0; i < slotItems.size(); i++) {
            Item item = i < items.size() ? items.get(i) : null;
            if (item != shownItems.get(i)) {
                shownItems.set(i, item);
                slotItems.get(i).setDrawable(item == null ? null : UiStyle.image(resources, item.getThumbnailType().getImage()));
            }
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

    /** The open settings window, or null. */
    public SettingsPanel getSettings() {
        return settings != null && settings.getStage() != null ? settings : null;
    }

    /** What each backpack slot holds, null for empty. */
    public List<Item> getBackpack() {
        return shownItems;
    }

    public int getFullApples() {
        return FULL_APPLES;
    }

    public int getApples() {
        return APPLES;
    }

    public UiImage getDayPicture() {
        return UiImage.SUN;
    }

    /** The bottom row (face, backpack, day, apples): taps on it stay in the HUD. */
    public Group getRow() {
        return row;
    }
}
