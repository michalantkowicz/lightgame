package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.mantkowicz.light.actor.FishType;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * The fishing game's window ({@link LeapRound}'s rules): a panel in the middle of the dimmed screen (like the settings
 * window, without its title and check mark), two rows of rippling water at its bottom and a thin green zone at its
 * top. While the fish is in the green zone a paw points at it, pulsing (tap now!). The hooked fish
 * leaps out of the water; a tap anywhere while it is in the green zone catches it, and a caught fish appears on the
 * right (orange, green, blue, from the bottom up), a miss puts a pink X on the left (from the bottom up). Three of
 * either end it. While it is open, taps don't reach the map or the HUD.
 */
public class LeapGame extends Group {
    /** What the game tells whoever opened it. */
    public interface Listener {
        void caught();

        void missed();

        /** It is over: three catches (won) or three misses. */
        void over(boolean won);
    }

    private static final float WIDTH = 520, HEIGHT = 920, MARGIN = 44;
    /** Rows of water at the bottom. */
    private static final int WATER_ROWS = 2;
    /** The paw over a fish in the green zone: pixels per pack pixel, how much bigger it pulses, how fast. */
    private static final float PAW_SCALE = 7, PULSE = 1.2f, PULSE_SECONDS = 0.25f;
    /** Pixels per pack pixel for the pictures in it (a 16 px fish is 80 px). */
    private static final float SCALE = 5;
    private static final float ICON = 16 * SCALE, TILE = 16 * 4;
    /** The leaping fish: bigger than the marks (its frames have empty space around it). */
    private static final float LEAPER = 16 * 10;
    private static final FishType[] CAUGHT_COLOURS = {FishType.ORANGE, FishType.GREEN, FishType.BLUE};

    private final LeapRound round;
    private final Listener listener;
    private final ResourcesService resources;
    private final Animation<TextureRegion> leap;
    private final Image fish, paw;
    private final Animation<TextureRegion> ripple;
    private final List<Image> waterTiles = new ArrayList<>();
    private float time;
    private final List<Image> marks = new ArrayList<>();
    private final float left, bottom, water, top;
    private boolean told;

    /**
     * @param ui    the UI stage: the game covers what that shows, its window in the middle
     * @param level how hard, 0 .. 1 ({@link LeapRound})
     */
    public LeapGame(ResourcesService resources, Stage ui, float level, Listener listener) {
        this.resources = resources;
        this.listener = listener;
        this.round = new LeapRound(level);
        setName("FISHING_GAME");
        Rectangle screen = ScreenArea.of(ui);
        setBounds(screen.x, screen.y, screen.width, screen.height);
        float screenWidth = screen.width, screenHeight = screen.height;
        Image dim = new Image(resources.getDrawable("black.png"));
        dim.setSize(screenWidth, screenHeight);
        dim.getColor().a = 0.4f; // the face under it still shows how the game goes
        addActor(dim);

        left = (screenWidth - WIDTH) / 2f;
        bottom = (screenHeight - HEIGHT) / 2f;
        Image panel = new Image(UiStyle.stretching(resources, UiImage.PANEL));
        panel.setBounds(left, bottom, WIDTH, HEIGHT);
        addActor(panel);

        water = bottom + MARGIN + TILE * (WATER_ROWS - 1) + TILE * 0.75f; // the water's surface
        top = bottom + HEIGHT - MARGIN - 10;
        // one green pixel from the middle of the stamina bar's piece, stretched (its edges are dark)
        TextureRegion bar = resources.getUiImage(UiImage.BAR_FULL);
        Image green = new Image(new TextureRegion(bar, bar.getRegionWidth() / 2, bar.getRegionHeight() / 2, 1, 1));
        float greenBottom = water + round.getGreenFrom() * (top - water);
        green.setBounds(left + MARGIN + ICON + 16, greenBottom, WIDTH - 2 * (MARGIN + ICON + 16), top - greenBottom);
        green.getColor().a = 0.8f;
        addActor(green);

        leap = resources.getAnimation(Sprite.FISH_LEAP, Action.LEAP);
        fish = new Image(new TextureRegionDrawable(leap.getKeyFrame(0)));
        fish.setSize(LEAPER, LEAPER);
        fish.setVisible(false);
        addActor(fish);

        ripple = resources.getAnimation(Sprite.WATER_TILE, Action.RIPPLE);
        int tiles = Math.round((WIDTH - 2 * MARGIN) / TILE);
        float tileWidth = (WIDTH - 2 * MARGIN) / tiles; // the rows fill the panel's inside
        for (int row = 0; row < WATER_ROWS; row++) {
            for (int i = 0; i < tiles; i++) {
                Image tile = new Image(new TextureRegionDrawable(ripple.getKeyFrame(0)));
                tile.setBounds(left + MARGIN + i * tileWidth, bottom + MARGIN + row * TILE, tileWidth, TILE);
                addActor(tile);
                waterTiles.add(tile);
            }
        }

        // the paw points at a fish in the green zone, pulsing: tap now!
        TextureRegion pawPicture = resources.getUiImage(UiImage.PAW_POINTING);
        paw = new Image(pawPicture);
        paw.setSize(pawPicture.getRegionWidth() * PAW_SCALE, pawPicture.getRegionHeight() * PAW_SCALE);
        paw.setOrigin(0, paw.getHeight());
        paw.setVisible(false);
        paw.setTouchable(Touchable.disabled);
        paw.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(PULSE, PULSE, PULSE_SECONDS, Interpolation.sine),
                Actions.scaleTo(1, 1, PULSE_SECONDS, Interpolation.sine))));
        addActor(paw);

        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                show(round.tap());
                return true; // the map and the HUD under it don't get it
            }
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        show(round.update(delta));
        time += delta;
        for (Image tile : waterTiles) {
            ((TextureRegionDrawable) tile.getDrawable()).setRegion(ripple.getKeyFrame(time, true));
        }
        fish.setVisible(round.isLeaping());
        if (round.isLeaping()) {
            ((TextureRegionDrawable) fish.getDrawable()).setRegion(
                    leap.getKeyFrames()[Math.min(leap.getKeyFrames().length - 1,
                            (int) (round.progress() * leap.getKeyFrames().length))]);
            fish.setPosition(left + (WIDTH - LEAPER) / 2f, water + round.height() * (top - water) - LEAPER / 2f);
        }
        paw.setVisible(round.isCatchable());
        if (paw.isVisible()) { // its finger (the picture's top left) just right of the fish's middle
            paw.setPosition(fish.getX() + LEAPER * 0.65f, fish.getY() + LEAPER / 2f - paw.getHeight());
        }
        if (round.isOver() && !told) {
            told = true;
            listener.over(round.isWon());
        }
    }

    /** A catch puts a fish on the right, a miss an X on the left (both from the bottom up). */
    private void show(LeapRound.Result result) {
        if (result == LeapRound.Result.CAUGHT) {
            int i = round.getCaught() - 1;
            TextureRegion picture = resources.getAnimation(Sprite.FISH_CATCH, Action.CAUGHT)
                    .getKeyFrames()[CAUGHT_COLOURS[Math.min(i, CAUGHT_COLOURS.length - 1)].getFrame()];
            mark(picture, left + WIDTH - MARGIN - ICON, i);
            listener.caught();
        } else if (result == LeapRound.Result.MISSED) {
            mark(resources.getUiImage(UiImage.MISS), left + MARGIN, round.getMissed() - 1);
            listener.missed();
        }
    }

    private void mark(TextureRegion picture, float x, int place) {
        Image mark = new Image(picture);
        mark.setScaling(Scaling.fit); // its own shape (the X is a bit taller than wide)
        mark.setBounds(x, water + 40 + place * (ICON + 24), ICON, ICON);
        marks.add(mark);
        addActor(mark);
    }

    /** The rules and the score (for tests). */
    public LeapRound getRound() {
        return round;
    }
}
