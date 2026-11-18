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
 * turns red on its way to the water's bottom and is gone; a fish scared off by a tap too early in its lane turns red
 * and jumps out of its lane to the side, fading. A miss fills one of the X shadows on the left (from the bottom up: as
 * many as the misses that lose). A tap with no fish of that lane in sight only presses the net. While it is open, taps don't reach the map or the HUD.
 */
public class RhythmGame extends Group {
    /** What the game tells whoever opened it. */
    public interface Listener {
        void caught();

        void missed();

        /** It is over, its window closed: won (fewer misses than lose it) or lost. */
        void over(boolean won);
    }

    private static final float WIDTH = 600, HEIGHT = 1000, MARGIN = 32, GAP = 12;
    /** A fish and an X mark (16 pack pixels, 5 screen pixels each), the water's tiles. */
    private static final float ICON = 16 * 4, FISH = 16 * 7, TILE = 16 * 4;
    /** The line: how thick, how far above the nets. */
    private static final float LINE = 16, LINE_ABOVE_NETS = 56;
    /** The net buttons: screen pixels per pack pixel for the button and for the net on it. */
    private static final int NET_SCALE = 6, NET_ICON_SCALE = 5;
    /** A caught fish: how much it grows, how long it takes to go. */
    private static final float CAUGHT_GROW = 1.15f, CAUGHT_SECONDS = 0.3f, SPARKLE = 80;
    /** A missed fish: red, it jumps this far out of its lane, this long; the X shadows' darkness. */
    private static final Color MISSED_RED = new Color(1f, 0.35f, 0.3f, 1f);
    private static final float MISSED_JUMP = 90, MISSED_SECONDS = 0.6f, GONE_SECONDS = 0.2f, SHADOW_ALPHA = 0.18f;
    private static final Color CAUGHT_GREEN = new Color(0.75f, 1f, 0.65f, 1f);
    /** How far a fish wiggles (degrees) and how fast; turned side-on, facing right (on the sheet they face up and right): a fish shape a child knows. */
    private static final float WIGGLE = 8, WIGGLE_SPEED = 9, SIDE_ON = -45;
    /** After the last catch or miss, the window waits this long (the last mark shows) and closes; won, longer. */
    private static final float CLOSE_AFTER = 0.8f, CELEBRATE_SECONDS = 1.8f;
    /** Won: the fish popping up (its size, its middle's height) and this many sparkles bursting out of it, about
     * twice as big as a chest's (pixels per art pixel). */
    private static final int STARS = 14;
    private static final float STAR_SCALE = 8, PRIZE = 16 * 12, PRIZE_Y = HEIGHT * 0.68f;

    private final RhythmRound round;
    private final Listener listener;
    private final ResourcesService resources;
    private final TextureRegion green;
    private final Animation<TextureRegion> ripple;
    private final List<Image> waterTiles = new ArrayList<>();
    private final List<RoundButton> nets = new ArrayList<>();
    private final List<Image> fishImages = new ArrayList<>();
    /** The fish scared off by a tap too early: they jump out by themselves. */
    private final List<Image> scared = new ArrayList<>();
    /** The fish that come: the clearest fish shapes of the sheet, yellow, blue, a clownfish. */
    private final TextureRegion[] swimmers;
    private final Image line;
    /** The panel and everything on it, in the middle of the screen (it stays there when the window is resized). */
    private final Group window = new Group();
    private final TextureRegion prize;
    private final Stage ui;
    private final Image dim;
    private final float lanesX, laneWidth, lineY, topY, waterBottom;
    private float time;
    private int marks;
    private boolean told;

    /**
     * @param ui    the UI stage: the game covers what that shows, its window in the middle
     * @param rules how hard ({@link RhythmRules#forLevel})
     * @param prize the fish the player gets for winning: it pops up when the game is won
     */
    public RhythmGame(ResourcesService resources, Stage ui, RhythmRules rules, TextureRegion prize,
            Listener listener) {
        this.prize = prize;
        this.resources = resources;
        this.listener = listener;
        this.round = new RhythmRound(rules);
        setName("FISHING_GAME");
        this.ui = ui;
        dim = new Image(resources.getDrawable("black.png"));
        dim.getColor().a = 0.4f; // the face under it still shows how the game goes
        addActor(dim);
        window.setSize(WIDTH, HEIGHT);
        addActor(window);
        coverTheScreen();
        Image panel = new Image(UiStyle.stretching(resources, UiImage.PANEL));
        panel.setSize(WIDTH, HEIGHT);
        window.addActor(panel);
        // one green pixel from the middle of the stamina bar's piece, stretched (its edges are dark)
        TextureRegion bar = resources.getUiImage(UiImage.BAR_FULL);
        green = new TextureRegion(bar, bar.getRegionWidth() / 2, bar.getRegionHeight() / 2, 1, 1);
        ripple = resources.getAnimation(Sprite.WATER_TILE, Action.RIPPLE);
        TextureRegion[] sheet = resources.getAnimation(Sprite.FISH_CATCH, Action.CAUGHT).getKeyFrames();
        swimmers = new TextureRegion[]{sheet[2], sheet[4],
                resources.getAnimation(Sprite.FISH_CATCH, Action.SWIM).getKeyFrames()[2]};

        lanesX = MARGIN + ICON + GAP;
        laneWidth = (WIDTH - 2 * MARGIN - ICON - GAP) / rules.lanes();
        TextureRegion net = resources.getAnimation(Sprite.FISHING_NET_ICON, Action.ICON).getKeyFrame(0);
        float netsY = MARGIN;
        for (int lane = 0; lane < rules.lanes(); lane++) {
            RoundButton button = new RoundButton(resources, net, UiImage.ACTION_BUTTON_DARK_BROWN,
                    UiImage.ACTION_BUTTON_DARK_BROWN_DOWN, NET_SCALE, NET_ICON_SCALE);
            button.setPosition(laneMiddle(lane) - button.getWidth() / 2f, netsY);
            button.setOrigin(button.getWidth() / 2f, button.getHeight() / 2f);
            button.setTouchable(Touchable.disabled); // the game takes the taps: a lane's net or anywhere in its lane
            nets.add(button);
        }
        lineY = netsY + nets.get(0).getHeight() + LINE_ABOVE_NETS;
        topY = HEIGHT - MARGIN - FISH / 2f;
        waterBottom = netsY + nets.get(0).getHeight() + GAP;
        addWater(waterBottom);
        line = addLine(rules, waterBottom);
        addMissShadows(rules.toLose());
        nets.forEach(window::addActor);
        for (RhythmRound.Fish fish : round.getFish()) {
            fishImages.add(fishImage(fish));
        }

        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                int lane = (int) Math.floor((x - window.getX() - lanesX) / laneWidth);
                float up = y - window.getY();
                if (lane >= 0 && lane < nets.size() && up >= 0 && up <= HEIGHT) { // on the panel only
                    tap(lane);
                }
                return true; // the map and the HUD under it don't get it
            }
        });
    }

    /** Over what the UI stage shows now, the panel in its middle. */
    private void coverTheScreen() {
        Rectangle screen = ScreenArea.of(getStage() != null ? getStage() : ui);
        setBounds(screen.x, screen.y, screen.width, screen.height);
        dim.setSize(screen.width, screen.height);
        window.setPosition(Math.round((screen.width - WIDTH) / 2f), Math.round((screen.height - HEIGHT) / 2f));
    }

    private float laneMiddle(int lane) {
        return lanesX + (lane + 0.5f) * laneWidth;
    }

    /** The lanes' water, from just above the nets to the panel's top, with a thin dark line between lanes. */
    private void addWater(float from) {
        float width = laneWidth * nets.size(), height = HEIGHT - MARGIN - from;
        int columns = Math.max(1, Math.round(width / TILE)), rows = Math.max(1, Math.round(height / TILE));
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Image tile = new Image(new TextureRegionDrawable(ripple.getKeyFrame(0)));
                tile.setBounds(lanesX + column * width / columns, from + row * height / rows, width / columns,
                        height / rows);
                window.addActor(tile);
                waterTiles.add(tile);
            }
        }
        for (int lane = 1; lane < nets.size(); lane++) {
            Image between = new Image(resources.getDrawable("black.png"));
            between.setBounds(lanesX + lane * laneWidth - 2, from, 4, height);
            between.getColor().a = 0.25f;
            window.addActor(between);
        }
    }

    /** The line, and a pale band around it: where a fish's middle is while a tap catches it. */
    private Image addLine(RhythmRules rules, float waterBottom) {
        float width = laneWidth * nets.size();
        float band = rules.window() / rules.fallSeconds() * (topY - lineY);
        Image zone = new Image(resources.getDrawable("black.png")); // the water a little darker there
        float zoneBottom = Math.max(waterBottom, lineY - band); // not over the nets
        zone.setBounds(lanesX, zoneBottom, width, lineY + band - zoneBottom);
        zone.getColor().a = 0.12f;
        window.addActor(zone);
        Image edge = new Image(resources.getDrawable("black.png")); // a dark edge: the line stands out on the water
        edge.setBounds(lanesX, lineY - LINE / 2f - 4, width, LINE + 8);
        edge.getColor().a = 0.6f;
        window.addActor(edge);
        Image bar = new Image(green);
        bar.setBounds(lanesX, lineY - LINE / 2f, width, LINE);
        window.addActor(bar);
        return bar;
    }

    private Image fishImage(RhythmRound.Fish fish) {
        Image image = new Image(swimmers[MathUtils.random(swimmers.length - 1)]);
        image.setScaling(Scaling.fit);
        image.setSize(FISH, FISH);
        image.setOrigin(FISH / 2f, FISH / 2f);
        image.setTouchable(Touchable.disabled);
        image.setPosition(laneMiddle(fish.getLane()) - FISH / 2f, topY - FISH / 2f);
        image.setVisible(false);
        window.addActor(image);
        return image;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (getStage() != null) {
            coverTheScreen(); // the window may have been resized (a phone turned)
        }
        for (RhythmRound.Fish fish : round.update(delta)) {
            missed(); // past the line: it swims on, red, and is gone at the water's bottom (moveFish)
        }
        time += delta;
        for (Image tile : waterTiles) {
            ((TextureRegionDrawable) tile.getDrawable()).setRegion(ripple.getKeyFrame(time, true));
        }
        // the line beats: bright on each beat (when fish reach it), fading till the next
        float sinceBeat = (round.getTime() - round.getRules().fallSeconds()) / round.getRules().beat();
        line.getColor().a = 0.35f + 0.65f * (1 - (sinceBeat - MathUtils.floor(sinceBeat)));
        List<RhythmRound.Fish> all = round.getFish();
        for (int i = 0; i < all.size(); i++) {
            moveFish(all.get(i), fishImages.get(i), i, delta);
        }
        if (round.isOver() && !told) {
            told = true;
            boolean won = round.isWon();
            if (won) {
                celebrate();
            }
            addAction(Actions.sequence(Actions.delay(won ? CELEBRATE_SECONDS : CLOSE_AFTER), Actions.run(() -> {
                remove(); // gone this frame: the next tap reaches the map or the HUD
                listener.over(won);
            })));
        }
    }

    /**
     * A fish swims down its lane, wiggling, from the top (fading in) past the line, redder the further past it (late),
     * and out at the water's bottom (never over the nets); once missed there it is all red and goes quickly.
     */
    private void moveFish(RhythmRound.Fish fish, Image image, int i, float delta) {
        if (fish.getState() == RhythmRound.State.CAUGHT || scared.contains(image)) {
            return; // it is going by itself
        }
        float progress = round.progress(fish);
        image.setVisible(progress >= 0);
        image.setY(topY + progress * (lineY - topY) - FISH / 2f);
        image.setRotation(SIDE_ON + MathUtils.sin(time * WIGGLE_SPEED + i) * WIGGLE);
        float middle = image.getY() + FISH / 2f;
        // it fades in at the top and out around the water's bottom, red by then (passing the line is late)
        float alpha = Math.min(MathUtils.clamp(progress * 8, 0, 1),
                MathUtils.clamp((middle - waterBottom + FISH / 4f) / (FISH / 2f), 0, 1));
        float late = MathUtils.clamp((lineY - middle) / ((lineY - waterBottom) / 2f), 0, 1);
        if (fish.getState() == RhythmRound.State.MISSED) {
            late = 1;
            alpha = Math.min(alpha, image.getColor().a - delta / GONE_SECONDS);
        }
        image.setColor(1, 1 - late * (1 - MISSED_RED.g), 1 - late * (1 - MISSED_RED.b), Math.max(0, alpha));
    }

    private void tap(int lane) {
        RoundButton net = nets.get(lane);
        net.clearActions();
        net.setScale(1);
        net.addAction(Actions.sequence(Actions.scaleTo(0.85f, 0.85f, 0.05f), Actions.scaleTo(1, 1, 0.12f,
                Interpolation.swingOut)));
        RhythmRound.Fish fish = round.tap(lane);
        if (fish != null && fish.getState() == RhythmRound.State.CAUGHT) {
            caught(fish, lane);
        } else if (fish != null) {
            scaredOff(fish);
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
        flash.setBounds(lanesX + lane * laneWidth, lineY - LINE / 2f - 8, laneWidth, LINE + 16);
        flash.setTouchable(Touchable.disabled);
        flash.addAction(Actions.sequence(Actions.alpha(0.5f), Actions.fadeOut(0.3f), Actions.removeActor()));
        window.addActorBefore(image, flash);
        Image sparkle = new Image(resources.getUiImage(UiImage.SPARKLE));
        sparkle.setSize(SPARKLE, SPARKLE);
        sparkle.setOrigin(SPARKLE / 2f, SPARKLE / 2f);
        sparkle.setPosition(laneMiddle(lane) - SPARKLE / 2f, lineY - SPARKLE / 2f);
        sparkle.setTouchable(Touchable.disabled);
        sparkle.setScale(0.3f);
        sparkle.addAction(Actions.sequence(Actions.parallel(Actions.scaleTo(1.2f, 1.2f, 0.2f, Interpolation.pow2Out),
                Actions.rotateBy(90, 0.2f), Actions.sequence(Actions.delay(0.1f), Actions.fadeOut(0.1f))),
                Actions.removeActor()));
        window.addActorBefore(image, sparkle); // behind the fish
        listener.caught();
    }

    /**
     * Tapped too early: it turns red and jumps out of its lane to the side (the left lane's to the left, the right one's
     * to the right, the middle one's either way), a little up and then down, turning and fading; a miss.
     */
    private void scaredOff(RhythmRound.Fish fish) {
        Image image = fishImages.get(round.getFish().indexOf(fish));
        scared.add(image);
        int lanes = nets.size();
        float side = fish.getLane() * 2 + 1 < lanes
                ? -1
                : fish.getLane() * 2 + 1 > lanes
                        ? 1
                        : MathUtils.randomSign();
        image.addAction(Actions.sequence(Actions.parallel(
                Actions.color(MISSED_RED, MISSED_SECONDS / 3f),
                Actions.moveBy(side * MISSED_JUMP, 0, MISSED_SECONDS),
                Actions.sequence(Actions.moveBy(0, MISSED_JUMP / 3f, MISSED_SECONDS / 3f, Interpolation.pow2Out),
                        Actions.moveBy(0, -MISSED_JUMP, MISSED_SECONDS * 2 / 3f, Interpolation.pow2In)),
                Actions.rotateBy(side * -60, MISSED_SECONDS),
                Actions.sequence(Actions.delay(MISSED_SECONDS / 2f), Actions.fadeOut(MISSED_SECONDS / 2f))),
                Actions.removeActor()));
        missed();
    }

    /** A miss: its X fills the next X shadow. */
    private void missed() {
        Image mark = missMark(marks);
        mark.setOrigin(ICON / 2f, ICON / 2f);
        mark.setScale(0);
        mark.addAction(Actions.scaleTo(1, 1, 0.25f, Interpolation.swingOut));
        marks++;
        window.addActor(mark);
        listener.missed();
    }

    /**
     * Won: the fish the player gets pops up big in the panel's upper middle (with a little bounce) and sparkles burst
     * out of it all around, of a few sizes, flying out, turning, shrinking and fading.
     */
    private void celebrate() {
        Image fish = new Image(prize);
        fish.setScaling(Scaling.fit);
        fish.setSize(PRIZE, PRIZE);
        fish.setOrigin(PRIZE / 2f, PRIZE / 2f);
        fish.setPosition((WIDTH - PRIZE) / 2f, PRIZE_Y - PRIZE / 2f);
        fish.setTouchable(Touchable.disabled);
        fish.setScale(0);
        fish.addAction(Actions.sequence(Actions.scaleTo(1, 1, 0.35f, Interpolation.swingOut),
                Actions.forever(Actions.sequence(Actions.rotateTo(-6, 0.3f, Interpolation.sine),
                        Actions.rotateTo(6, 0.3f, Interpolation.sine)))));
        TextureRegion star = resources.getUiImage(UiImage.SPARKLE);
        for (int i = 0; i < STARS; i++) {
            float scale = STAR_SCALE * MathUtils.random(0.6f, 1.2f); // not all alike
            Image image = new Image(star);
            image.setSize(star.getRegionWidth() * scale, star.getRegionHeight() * scale);
            image.setOrigin(image.getWidth() / 2f, image.getHeight() / 2f);
            image.setPosition(WIDTH / 2f - image.getWidth() / 2f, PRIZE_Y - image.getHeight() / 2f);
            image.setTouchable(Touchable.disabled);
            image.setScale(0.3f);
            float angle = (i + MathUtils.random(0.8f)) * MathUtils.PI2 / STARS; // all around, evenly
            float distance = MathUtils.random(170f, 300f), seconds = MathUtils.random(0.7f, 1.0f);
            image.addAction(Actions.sequence(Actions.delay(0.1f + MathUtils.random(0.15f)), Actions.parallel(
                    Actions.moveBy(MathUtils.cos(angle) * distance, MathUtils.sin(angle) * distance, seconds,
                            Interpolation.pow2Out),
                    Actions.rotateBy(MathUtils.randomSign() * 180, seconds),
                    Actions.sequence(Actions.scaleTo(1, 1, seconds * 0.3f),
                            Actions.scaleTo(0.3f, 0.3f, seconds * 0.7f)),
                    Actions.sequence(Actions.delay(seconds * 0.5f), Actions.fadeOut(seconds * 0.5f))),
                    Actions.removeActor()));
            window.addActor(image);
        }
        window.addActor(fish); // over the sparkles
    }

    /** An X in the column on the left, the {@code place}-th from the bottom. */
    private Image missMark(int place) {
        Image mark = new Image(resources.getUiImage(UiImage.MISS));
        mark.setScaling(Scaling.fit); // its own shape (the X is a bit taller than wide)
        mark.setBounds(MARGIN, lineY - ICON / 2f + place * (ICON + 24), ICON, ICON);
        mark.setTouchable(Touchable.disabled);
        return mark;
    }

    /** A shadow for each X there can be: how many misses lose, before any happen. */
    private void addMissShadows(int toLose) {
        for (int place = 0; place < toLose; place++) {
            Image shadow = missMark(place);
            shadow.setColor(0, 0, 0, SHADOW_ALPHA);
            window.addActor(shadow);
        }
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
