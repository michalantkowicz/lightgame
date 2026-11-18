package com.mantkowicz.light.ui.fishing;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
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
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.RoundButton;
import com.mantkowicz.light.ui.UiStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * The fishing game's window ({@link RhythmRound}'s rules): a panel in the middle of the dimmed screen (like the
 * settings window, without its title), rippling water in lanes, a green line near the bottom and under each lane a
 * round button with a net. Fish swim down the lanes, wiggling; tapping a lane's net (or anywhere in its lane) while a
 * fish is on the line catches it: it grows, turns green and is gone, and the line flashes. A fish that passes the line
 * fades away and puts a pink X on the left (from the bottom up). A tap with nothing on the line tangles that net for
 * a moment: it shakes and goes grey. While it is open, taps don't reach the map or the HUD.
 */
public class RhythmGame extends Group {
    /** What the game tells whoever opened it. */
    public interface Listener {
        void caught();

        void missed();

        /** It is over, its window closed: won (fewer misses than lose it) or lost. */
        void over(boolean won);
    }

    private static final float WIDTH = 600, HEIGHT = 1000, MARGIN = 44, GAP = 16;
    /** A fish and an X mark (16 pack pixels, 5 screen pixels each), the water's tiles. */
    private static final float ICON = 16 * 5, FISH = 16 * 6, TILE = 16 * 4;
    /** The line: how thick, how far above the nets. */
    private static final float LINE = 14, LINE_ABOVE_NETS = 56;
    /** A caught fish: how much it grows, how long it takes to go. */
    private static final float CAUGHT_GROW = 1.6f, CAUGHT_SECONDS = 0.3f;
    private static final Color CAUGHT_GREEN = new Color(0.75f, 1f, 0.65f, 1f);
    /** How far a fish wiggles (degrees) and how fast. */
    private static final float WIGGLE = 12, WIGGLE_SPEED = 9;
    /** The fish's colours: pink, orange, yellow, green, blue (the first five of the fish sheet). */
    private static final int COLOURS = 5;
    /** After the last catch or miss, the window waits this long (the last mark shows) and closes. */
    private static final float CLOSE_AFTER = 0.8f;

    private final RhythmRound round;
    private final Listener listener;
    private final ResourcesService resources;
    private final TextureRegion green;
    private final Animation<TextureRegion> ripple;
    private final List<Image> waterTiles = new ArrayList<>();
    private final List<RoundButton> nets = new ArrayList<>();
    private final List<Image> fishImages = new ArrayList<>();
    private final Image line;
    private final float left, bottom, lanesX, laneWidth, lineY, topY;
    private float time;
    private int marks;
    private boolean told;

    /**
     * @param ui    the UI stage: the game covers what that shows, its window in the middle
     * @param rules how hard ({@link RhythmRules#forLevel})
     */
    public RhythmGame(ResourcesService resources, Stage ui, RhythmRules rules, Listener listener) {
        this.resources = resources;
        this.listener = listener;
        this.round = new RhythmRound(rules);
        setName("FISHING_GAME");
        Rectangle screen = ScreenArea.of(ui);
        setBounds(screen.x, screen.y, screen.width, screen.height);
        Image dim = new Image(resources.getDrawable("black.png"));
        dim.setSize(screen.width, screen.height);
        dim.getColor().a = 0.4f; // the face under it still shows how the game goes
        addActor(dim);

        left = (screen.width - WIDTH) / 2f;
        bottom = (screen.height - HEIGHT) / 2f;
        Image panel = new Image(UiStyle.stretching(resources, UiImage.PANEL));
        panel.setBounds(left, bottom, WIDTH, HEIGHT);
        addActor(panel);
        // one green pixel from the middle of the stamina bar's piece, stretched (its edges are dark)
        TextureRegion bar = resources.getUiImage(UiImage.BAR_FULL);
        green = new TextureRegion(bar, bar.getRegionWidth() / 2, bar.getRegionHeight() / 2, 1, 1);
        ripple = resources.getAnimation(Sprite.WATER_TILE, Action.RIPPLE);

        lanesX = left + MARGIN + ICON + GAP;
        laneWidth = (WIDTH - 2 * MARGIN - ICON - GAP) / rules.lanes();
        TextureRegion net = resources.getAnimation(Sprite.FISHING_NET_ICON, Action.ICON).getKeyFrame(0);
        float netsY = bottom + MARGIN;
        for (int lane = 0; lane < rules.lanes(); lane++) {
            RoundButton button = new RoundButton(resources, net, UiImage.ACTION_BUTTON_DARK_BROWN,
                    UiImage.ACTION_BUTTON_DARK_BROWN_DOWN, 6, 4);
            button.setPosition(laneMiddle(lane) - button.getWidth() / 2f, netsY);
            button.setOrigin(button.getWidth() / 2f, button.getHeight() / 2f);
            button.setTouchable(Touchable.disabled); // the game takes the taps: a lane's net or anywhere in its lane
            nets.add(button);
        }
        lineY = netsY + nets.get(0).getHeight() + LINE_ABOVE_NETS;
        topY = bottom + HEIGHT - MARGIN - FISH / 2f;
        addWater(netsY + nets.get(0).getHeight() + GAP);
        line = addLine(rules);
        nets.forEach(this::addActor);
        for (RhythmRound.Fish fish : round.getFish()) {
            fishImages.add(fishImage(fish));
        }

        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int lane = (int) Math.floor((x - lanesX) / laneWidth);
                if (lane >= 0 && lane < nets.size()) {
                    tap(lane);
                }
                return true; // the map and the HUD under it don't get it
            }
        });
    }

    private float laneMiddle(int lane) {
        return lanesX + (lane + 0.5f) * laneWidth;
    }

    /** The lanes' water, from just above the nets to the panel's top, with a thin dark line between lanes. */
    private void addWater(float from) {
        float width = laneWidth * nets.size(), height = bottom + HEIGHT - MARGIN - from;
        int columns = Math.max(1, Math.round(width / TILE)), rows = Math.max(1, Math.round(height / TILE));
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Image tile = new Image(new TextureRegionDrawable(ripple.getKeyFrame(0)));
                tile.setBounds(lanesX + column * width / columns, from + row * height / rows, width / columns,
                        height / rows);
                addActor(tile);
                waterTiles.add(tile);
            }
        }
        for (int lane = 1; lane < nets.size(); lane++) {
            Image between = new Image(resources.getDrawable("black.png"));
            between.setBounds(lanesX + lane * laneWidth - 2, from, 4, height);
            between.getColor().a = 0.25f;
            addActor(between);
        }
    }

    /** The line, and a pale band around it: where a fish's middle is while a tap catches it. */
    private Image addLine(RhythmRules rules) {
        float width = laneWidth * nets.size();
        float band = rules.window() / rules.fallSeconds() * (topY - lineY);
        Image zone = new Image(green);
        zone.setBounds(lanesX, lineY - band, width, 2 * band);
        zone.getColor().a = 0.3f;
        addActor(zone);
        Image bar = new Image(green);
        bar.setBounds(lanesX, lineY - LINE / 2f, width, LINE);
        addActor(bar);
        return bar;
    }

    private Image fishImage(RhythmRound.Fish fish) {
        TextureRegion picture = resources.getAnimation(Sprite.FISH_CATCH, Action.CAUGHT)
                .getKeyFrames()[MathUtils.random(COLOURS - 1)];
        Image image = new Image(picture);
        image.setScaling(Scaling.fit);
        image.setSize(FISH, FISH);
        image.setOrigin(FISH / 2f, FISH / 2f);
        image.setTouchable(Touchable.disabled);
        image.setPosition(laneMiddle(fish.getLane()) - FISH / 2f, topY - FISH / 2f);
        image.setVisible(false);
        addActor(image);
        return image;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        for (RhythmRound.Fish fish : round.update(delta)) {
            missed(fish);
        }
        time += delta;
        for (Image tile : waterTiles) {
            ((TextureRegionDrawable) tile.getDrawable()).setRegion(ripple.getKeyFrame(time, true));
        }
        // the line beats: bright on each beat (when fish reach it), fading till the next
        float sinceBeat = (round.getTime() - round.getRules().fallSeconds()) / round.getRules().beat();
        line.getColor().a = 0.55f + 0.45f * (1 - (sinceBeat - MathUtils.floor(sinceBeat)));
        List<RhythmRound.Fish> all = round.getFish();
        for (int i = 0; i < all.size(); i++) {
            moveFish(all.get(i), fishImages.get(i), i);
        }
        for (int lane = 0; lane < nets.size(); lane++) {
            nets.get(lane).setColor(round.isTangled(lane) ? Color.GRAY : Color.WHITE);
        }
        if (round.isOver() && !told) {
            told = true;
            boolean won = round.isWon();
            addAction(Actions.sequence(Actions.delay(CLOSE_AFTER), Actions.run(() -> {
                remove(); // gone this frame: the next tap reaches the map or the HUD
                listener.over(won);
            })));
        }
    }

    /** A coming or missed fish swims down its lane, wiggling, from the top (fading in) past the line. */
    private void moveFish(RhythmRound.Fish fish, Image image, int i) {
        if (fish.getState() == RhythmRound.State.CAUGHT) {
            return; // it is going by itself
        }
        float progress = round.progress(fish);
        image.setVisible(progress >= 0);
        image.setY(topY + progress * (lineY - topY) - FISH / 2f);
        image.setRotation(MathUtils.sin(time * WIGGLE_SPEED + i) * WIGGLE);
        if (fish.getState() == RhythmRound.State.COMING) {
            image.getColor().a = MathUtils.clamp(progress * 8, 0, 1);
        }
    }

    private void tap(int lane) {
        RoundButton net = nets.get(lane);
        net.clearActions();
        net.setScale(1);
        RhythmRound.Fish fish = round.tap(lane);
        if (fish != null) {
            net.addAction(Actions.sequence(Actions.scaleTo(0.85f, 0.85f, 0.05f), Actions.scaleTo(1, 1, 0.12f,
                    Interpolation.swingOut)));
            caught(fish, lane);
        } else if (round.isTangled(lane)) {
            net.addAction(Actions.sequence(Actions.moveBy(-10, 0, 0.04f), Actions.moveBy(20, 0, 0.08f),
                    Actions.moveBy(-10, 0, 0.04f)));
        }
    }

    /** It grows, turns green and is gone; the line flashes over its lane. */
    private void caught(RhythmRound.Fish fish, int lane) {
        Image image = fishImages.get(round.getFish().indexOf(fish));
        image.getColor().a = 1;
        image.addAction(Actions.sequence(Actions.parallel(
                Actions.scaleTo(CAUGHT_GROW, CAUGHT_GROW, CAUGHT_SECONDS, Interpolation.pow2Out),
                Actions.color(CAUGHT_GREEN, CAUGHT_SECONDS / 2f),
                Actions.sequence(Actions.delay(CAUGHT_SECONDS / 2f), Actions.fadeOut(CAUGHT_SECONDS / 2f))),
                Actions.removeActor()));
        Image flash = new Image(green);
        flash.setBounds(lanesX + lane * laneWidth, lineY - FISH / 2f, laneWidth, FISH);
        flash.setTouchable(Touchable.disabled);
        flash.addAction(Actions.sequence(Actions.alpha(0.7f), Actions.fadeOut(0.3f), Actions.removeActor()));
        addActorBefore(image, flash);
        listener.caught();
    }

    /** It fades away below the line, and an X pops up on the left (from the bottom up). */
    private void missed(RhythmRound.Fish fish) {
        Image image = fishImages.get(round.getFish().indexOf(fish));
        image.addAction(Actions.fadeOut(0.3f));
        Image mark = new Image(resources.getUiImage(UiImage.MISS));
        mark.setScaling(Scaling.fit); // its own shape (the X is a bit taller than wide)
        mark.setBounds(left + MARGIN, lineY - ICON / 2f + marks * (ICON + 24), ICON, ICON);
        mark.setOrigin(ICON / 2f, ICON / 2f);
        mark.setScale(0);
        mark.addAction(Actions.scaleTo(1, 1, 0.25f, Interpolation.swingOut));
        marks++;
        addActor(mark);
        listener.missed();
    }

    /** The rules and the score (for tests). */
    public RhythmRound getRound() {
        return round;
    }

    /** A lane's net button (for tests: where to tap). */
    public RoundButton getNet(int lane) {
        return nets.get(lane);
    }
}
