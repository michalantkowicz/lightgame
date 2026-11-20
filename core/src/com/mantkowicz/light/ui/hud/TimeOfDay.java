package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.light.DayClock;
import com.mantkowicz.light.light.DayClock.Part;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

/**
 * The time of day at the HUD's top left, a picture first: the weather UI's frame (without its label) with a picture of
 * the part of the day in its left window (the sun by day, the setting sun at dusk, the moon at night, the sun behind a
 * cloud at dawn) and the day's scale in its right one (the sun at its top, the moon at its bottom) with an arrow over
 * it, from the bump on the frame's middle. The arrow only goes down, clockwise: when the sun starts to rise (dawn) it
 * jumps to nearly straight up, at the sun; then it goes down a step every fifth of the time until sunset; when the sun starts
 * to set (dusk) it goes one more step (steeply down), and at night it points nearly straight down, at the moon, until
 * sunrise. It follows the game's one {@link DayClock}. Its middle is level with the middle of the settings gear (top right).
 */
public class TimeOfDay extends Group {
    /** Screen pixels per pack pixel: 80% of the rest of the UI's ({@link ResourcesService#UI_SCALE}), owner's choice. */
    static final float SCALE = ResourcesService.UI_SCALE * 0.8f;
    /** Between the frame and the screen's left edge. */
    static final float MARGIN = 24;
    /** The arrows, clockwise from nearly straight up (at the sun) to nearly straight down (at the moon). */
    private static final UiImage[] ARROWS = {UiImage.DAY_CLOCK_ARROW_UP, UiImage.DAY_CLOCK_ARROW_STEEP_UP,
            UiImage.DAY_CLOCK_ARROW_UP_RIGHT, UiImage.DAY_CLOCK_ARROW_RIGHT, UiImage.DAY_CLOCK_ARROW_DOWN_RIGHT,
            UiImage.DAY_CLOCK_ARROW_STEEP_DOWN, UiImage.DAY_CLOCK_ARROW_DOWN};
    /**
     * Where each arrow's bottom left corner is in the frame (pack pixels, from the frame's bottom left): the slanted
     * ones with their tails on the bump of the frame's middle, the steepest ones starting next to it.
     */
    private static final int[] ARROW_X = {50, 52, 52, 52, 52, 52, 50}, ARROW_Y = {23, 21, 21, 19, 16, 14, 11};
    /** In the frame (pack pixels, from its bottom left): the windows' corners. */
    private static final int WINDOW_Y = 8, PICTURE_X = 8, SCALE_X = 50;

    private final ResourcesService resources;
    private final DayClock clock;
    /** What its middle is level with: the settings gear. */
    private final Actor alignedWith;
    private final Image picture = new Image(), arrow = new Image();
    private Part shownPart;
    private int shownArrow = -1;

    /** @param alignedWith what its middle is level with (the settings gear) */
    public TimeOfDay(ResourcesService resources, DayClock clock, Actor alignedWith) {
        this.resources = resources;
        this.clock = clock;
        this.alignedWith = alignedWith;
        setName("TIME OF DAY");
        setTouchable(Touchable.disabled); // taps go through to the map
        Image frame = scaled(resources, UiImage.DAY_CLOCK_FRAME);
        setSize(frame.getWidth(), frame.getHeight());
        addActor(frame);
        picture.setPosition(PICTURE_X * SCALE, WINDOW_Y * SCALE);
        addActor(picture);
        Image dayScale = scaled(resources, UiImage.DAY_CLOCK_SCALE);
        dayScale.setPosition(SCALE_X * SCALE, WINDOW_Y * SCALE);
        addActor(dayScale);
        addActor(arrow);
        show();
    }

    private static Image scaled(ResourcesService resources, UiImage piece) {
        Image image = new Image(resources.getUiImage(piece));
        image.setSize(image.getPrefWidth() * SCALE, image.getPrefHeight() * SCALE);
        return image;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        show();
        // at the left edge, its middle level with the gear's, whatever the screen's size (the HUD has placed the gear)
        setPosition(MARGIN, Math.round(alignedWith.getY() + (alignedWith.getHeight() - getHeight()) / 2f));
    }

    /** The picture and the arrow for the time it is now. */
    private void show() {
        Part part = clock.getPart();
        if (part != shownPart) {
            shownPart = part;
            set(picture, pictureFor(part));
        }
        int index = arrowFor(part, clock.getSunProgress());
        if (index != shownArrow) {
            shownArrow = index;
            set(arrow, ARROWS[index]);
            arrow.setPosition(ARROW_X[index] * SCALE, ARROW_Y[index] * SCALE);
        }
    }

    private void set(Image image, UiImage piece) {
        image.setDrawable(new TextureRegionDrawable(resources.getUiImage(piece)));
        image.setSize(image.getPrefWidth() * SCALE, image.getPrefHeight() * SCALE);
    }

    private static UiImage pictureFor(Part part) {
        switch (part) {
            case DAY :
                return UiImage.TIME_DAY;
            case DUSK :
                return UiImage.TIME_DUSK;
            case NIGHT :
                return UiImage.TIME_NIGHT;
            default :
                return UiImage.TIME_DAWN;
        }
    }

    /**
     * Which arrow (0 nearly straight up at the sun .. 6 nearly straight down at the moon): from sunrise to sunset ({@code progress}
     * through the dawn and the day) one step down every fifth (0 to 4); at dusk the one before the moon (5); at night
     * the moon (6).
     *
     * @param progress how far it is from sunrise to sunset ({@link DayClock#getSunProgress})
     */
    static int arrowFor(Part part, float progress) {
        switch (part) {
            case DAY :
            case DAWN :
                return Math.min(ARROWS.length - 3, (int) ((ARROWS.length - 2) * progress));
            case DUSK :
                return ARROWS.length - 2;
            default :
                return ARROWS.length - 1;
        }
    }

    // what it shows, for tests (e2e GameDriver)

    /** The part of the day its picture shows. */
    public Part getShownPart() {
        return shownPart;
    }

    /** Its arrow: 0 nearly straight up at the sun .. 6 nearly straight down at the moon. */
    public int getShownArrow() {
        return shownArrow;
    }
}
