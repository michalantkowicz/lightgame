package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.Waterable;
import com.mantkowicz.light.actor.Weedable;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.SpriteSheet;
import com.mantkowicz.light.resources.ThumbnailType;

import java.util.ArrayList;

import static com.mantkowicz.light.actor.GameActorType.FIELD;

/**
 * A field with one kind of crop (Tiled's cropType, a carrot without it) and a sign at its top left corner showing it.
 * It starts ripe. Like a chest it blocks its tile and is used from a neighbouring one: its button (the crop) makes the
 * crop jump out, and the field is empty again but for a tiny sprout, the same for every crop (phase 0), and its sign
 * shows a question mark instead of the crop (with seeds, later, what grows there will be the player's choice); the
 * crop's own pictures are the phases after it. Then its button (the watering can) waters it: water falls
 * onto it and it grows a phase at once, then by itself a phase every {@link #SECONDS_PER_PHASE} seconds, without a
 * button meanwhile (the plant bounces a little at each phase and slowly squashes and stretches while it grows); ripe
 * again, the crop can be picked again (the HUD's face is happy). So a field gives a crop only now and then.
 * <p>
 * Nobody chose what grows on it (no seeds yet, see docs/ideas.md), so when the water has fallen on an empty field a
 * weed may grow instead of the crop ({@link #DEFAULT_WEED_CHANCE}, Tiled's weedChance): the sprout's row goes on, a
 * blue flower, and the sign keeps its question mark. Its button pulls the ripe weed out: it
 * rises a little and fades out, nothing is gained (the face sweats and says so), and the field is empty again.
 * <p>
 * What grows after the sprout is a {@link Growth}: the crop ({@link CropGrowth}) or a weed ({@link WeedGrowth}),
 * picked when the water has fallen on the empty field.
 */
public class Field extends SpriteActor implements Collectible, Waterable, Weedable {
    /** How likely a weed grows instead of the crop, when Tiled's weedChance doesn't say. */
    public static final float DEFAULT_WEED_CHANCE = 0.4f;
    /** Watered, it grows a phase at once and then one every this many seconds until it is ripe. */
    static final float SECONDS_PER_PHASE = 30;

    private final CropType crop;
    private final ResourcesService resources;
    private final GameEventService events;
    private final Inventory inventory = new Inventory(1, new ArrayList<Item>());
    private final float weedChance;
    private final TextureRegion canIcon;
    /** The sign: the crop, or blank with a question mark while the field is empty (phase 0) or a weed grows. */
    private final FieldSign sign;
    /** The tiny sprout of an empty field (phase 0), shown instead of the plant. */
    private final Image seedling;
    private final CropGrowth cropGrowth;
    private final WeedGrowth weedGrowth;
    /** What grows now (or grew last, while the field is empty): the crop or a weed. */
    private Growth growth;
    private int phase;
    /** The water falling onto it, while it is watered; null otherwise. */
    private WaterSplash splash;
    /** Watered: it grows by itself until it is ripe. */
    private boolean growing;
    /** Seconds since its last phase while it grows. */
    private float sincePhase;

    /** @param weedChance how likely a weed grows instead of the crop after watering, 0 never, 1 always */
    public Field(GameContext context, CropType crop, float weedChance) {
        super(FIELD, context, crop.getPlant(), Action.GROW, "A field with " + crop.getName() + "s.");
        this.crop = crop;
        this.resources = context.getResourcesService();
        this.events = context.getGameEventService();
        this.weedChance = weedChance;
        setName(crop.getName() + " field");
        setTouchable(Touchable.disabled);
        canIcon = resources.getAnimation(Sprite.WATERING_CAN, Action.ICON).getKeyFrame(0);
        sign = new FieldSign(resources, crop.getSign(), getHeight());
        addActorAt(0, sign); // behind the plant
        seedling = sheetImage(Sprite.PLANT_SEEDLING, Action.GROW);
        addActor(seedling);
        cropGrowth = new CropGrowth(getAvatar(), frame -> playOnce(Action.GROW, frame, frame), crop, resources);
        weedGrowth = place(new WeedGrowth(resources), Sprite.PLANT_WEED);
        addActor(weedGrowth);
        growth = cropGrowth; // it starts ripe, with its crop
        phase = growth.getRipePhase();
        growth.ripe(inventory);
        showPhase();
        addPlugin(this::updateGrowing);
    }

    public CropType getCrop() {
        return crop;
    }

    /** How far it has grown: 0 an empty field (a tiny sprout), {@link #getRipePhase()} ripe. */
    public int getGrowthPhase() {
        return phase;
    }

    /** The phase it is ripe at: the crop's, or the weed's while one grows. */
    public int getRipePhase() {
        return growth.getRipePhase();
    }

    /** Does a weed grow on it (instead of the crop)? */
    public boolean isWeed() {
        return growth == weedGrowth;
    }

    public float getWeedChance() {
        return weedChance;
    }

    /** Does its sign show the crop (not the question mark of an empty field or a weed)? */
    public boolean isCropOnSign() {
        return sign.showsCrop();
    }

    /** Watered, is it growing by itself now (no button until it is ripe)? */
    public boolean isGrowing() {
        return growing;
    }

    /** Is water falling onto it right now? */
    public boolean isBeingWatered() {
        return splash != null;
    }

    /** Phase 0 the sprout; then what grows (the crop or a weed), its first picture at phase 1. */
    private void showPhase() {
        boolean empty = phase == 0;
        sign.showCrop(!empty && growth.givesCrop());
        seedling.setVisible(empty);
        cropGrowth.getPlant().setVisible(!empty && growth == cropGrowth);
        weedGrowth.getPlant().setVisible(!empty && growth == weedGrowth);
        if (!empty) {
            growth.showPhase(phase);
        }
    }

    /**
     * A picture from the first frame of a sheet's row, at the world's pixel scale, with the sheet's anchor on the
     * tile's middle (like the plant itself).
     */
    private Image sheetImage(Sprite sprite, Action action) {
        Image image = new Image(resources.getAnimation(sprite, action).getKeyFrame(0));
        image.setTouchable(Touchable.disabled);
        return place(image, sprite);
    }

    /** Sizes a picture of the sheet at the world's pixel scale and puts the sheet's anchor on the tile's middle. */
    private <T extends Image> T place(T image, Sprite sprite) {
        SpriteSheet sheet = resources.getSpriteSheet(sprite);
        float scale = WorldScale.PIXEL_SCALE;
        image.setSize(image.getPrefWidth() * scale, image.getPrefHeight() * scale);
        image.setPosition(getWidth() / 2f + (sheet.trimX() - sheet.anchorX()) * scale,
                getHeight() / 2f + (sheet.trimY() - sheet.anchorY()) * scale);
        return image;
    }

    // watering

    /** A crop still to grow and nobody has watered it since it was picked. */
    @Override
    public boolean needsWater() {
        return phase < getRipePhase() && !growing;
    }

    @Override
    public void water() {
        if (!needsWater() || isBeingWatered()) {
            return;
        }
        splash = place(new WaterSplash(resources, this::watered), Sprite.WATERING);
        addActor(splash);
    }

    /** The water has fallen: the crop (or a weed) grows a phase and goes on growing by itself. */
    private void watered() {
        splash = null;
        sincePhase = 0;
        if (phase == 0) { // nobody chose what grows: maybe a weed
            growth = MathUtils.random() < weedChance ? weedGrowth : cropGrowth;
        }
        grow();
    }

    /** Watered, a phase every {@link #SECONDS_PER_PHASE} seconds until it is ripe. */
    private void updateGrowing(float delta) {
        if (!growing) {
            return;
        }
        sincePhase += delta;
        if (sincePhase >= SECONDS_PER_PHASE) {
            sincePhase = 0;
            grow();
        }
    }

    /** A phase on; it goes on growing until it is ripe. */
    private void grow() {
        phase++;
        growing = phase < getRipePhase();
        if (!growing) {
            growth.ripe(inventory);
        }
        showPhase();
        PlantMoves.bounce(growth.getPlant(), growing); // a bounce, then squashing while it grows
    }

    // collecting

    /** Used from a neighbouring tile: the field blocks its own. */
    @Override
    public boolean isDistant() {
        return true;
    }

    /** The crop while it is ripe (picks it), the weed (pulls it out), the watering can after picking (waters it). */
    @Override
    public TextureRegion getActionIcon() {
        return needsWater() ? canIcon : growth.getRipePicture();
    }

    /** The watering can: the button's picture while the crop grows (for tests and descriptions). */
    public TextureRegion getWateringCanIcon() {
        return canIcon;
    }

    @Override
    public boolean isBusy() {
        return isBeingWatered();
    }

    /**
     * The top of its plant when it sticks out above the tile (ripe corn), else the tile's top: an empty field's button
     * (the watering can) is as high as any other field's.
     */
    @Override
    public float getTopY() {
        if (phase == 0) {
            return getHeight();
        }
        Image plant = growth.getPlant();
        return Math.max(getHeight(), plant.getY() + plant.getHeight());
    }

    /** The crop is picked: it jumps out and the field is empty, a tiny sprout left; the face is happy. */
    @Override
    public void beforeCollect() {
        phase = 0;
        showPhase();
        events.post(new FieldHarvestedEvent(false));
    }

    // weeds

    /** A ripe weed grows on it. */
    @Override
    public boolean hasWeed() {
        return isWeed() && phase == growth.getRipePhase();
    }

    /** The weed disappears and the field is empty again (the face sweats: no use). */
    @Override
    public void pullWeed() {
        if (!hasWeed()) {
            return;
        }
        weedGrowth.pullOut();
        growth = cropGrowth; // the next time, the dice again
        phase = 0;
        showPhase();
        events.post(new FieldHarvestedEvent(true));
    }

    /** Is a pulled weed still fading out? */
    public boolean isWeedFadingOut() {
        return weedGrowth.isFadingOut();
    }

    @Override
    public void afterCollect() {
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public Actor getActor() {
        return this;
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }
}
