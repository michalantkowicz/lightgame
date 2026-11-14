package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.Waterable;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.context.GameContext;
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
 * crop jump out, and the field starts again with a seedling. Then its button (the watering can) waters it: water falls
 * onto it and it grows a stage; ripe again, the crop can be picked again.
 */
public class Field extends SpriteActor implements Collectible, Waterable {
    /** The sign is drawn at half the world's pixel scale, so it fits in the corner. */
    private static final float SIGN_SCALE = WorldScale.PIXEL_SCALE / 2;
    /** How far the sign sticks out over the tile's left and top edges. */
    private static final float SIGN_OVERFLOW = WorldScale.PIXEL_SCALE;

    private final CropType crop;
    private final ResourcesService resources;
    private final Inventory inventory = new Inventory(1, new ArrayList<Item>());
    private final int ripeStage;
    private final TextureRegion cropIcon;
    private final TextureRegion canIcon;
    private int stage;
    /** The water falling onto it, while it is watered; null otherwise. */
    private Image splash;
    private float splashTime;

    public Field(GameContext context, CropType crop) {
        super(FIELD, context, crop.getPlant(), Action.GROW, "A field with " + crop.getName() + "s.");
        this.crop = crop;
        this.resources = context.getResourcesService();
        setName(crop.getName() + " field");
        setTouchable(Touchable.disabled);
        ripeStage = resources.getAnimation(crop.getPlant(), Action.GROW).getKeyFrames().length - 1;
        cropIcon = resources.getTexture(crop.getPicture());
        canIcon = resources.getAnimation(Sprite.WATERING_CAN, Action.ICON).getKeyFrame(0);
        addSign();
        stage = ripeStage;
        inventory.add(new Crop(crop, resources));
        showStage();
        addPlugin(this::updateSplash);
    }

    /** The sign, behind the plant, at the tile's top left corner and sticking out over it a little. */
    private void addSign() {
        TextureRegion picture = resources.getAnimation(crop.getSign(), Action.SIGN).getKeyFrame(0);
        Image sign = new Image(picture);
        sign.setTouchable(Touchable.disabled);
        sign.setSize(picture.getRegionWidth() * SIGN_SCALE, picture.getRegionHeight() * SIGN_SCALE);
        sign.setPosition(-SIGN_OVERFLOW, getHeight() + SIGN_OVERFLOW - sign.getHeight());
        addActorAt(0, sign);
    }

    public CropType getCrop() {
        return crop;
    }

    /** How far it has grown: 0 a seedling, {@link #getRipeStage()} ripe. */
    public int getGrowthStage() {
        return stage;
    }

    public int getRipeStage() {
        return ripeStage;
    }

    /** Is water falling onto it right now? */
    public boolean isBeingWatered() {
        return splash != null;
    }

    private void showStage() {
        playOnce(Action.GROW, stage, stage);
    }

    // watering

    @Override
    public boolean needsWater() {
        return stage < ripeStage;
    }

    @Override
    public void water() {
        if (!needsWater() || isBeingWatered()) {
            return;
        }
        Animation<TextureRegion> falling = resources.getAnimation(Sprite.WATERING, Action.SPLASH);
        TextureRegion first = falling.getKeyFrame(0);
        SpriteSheet sheet = resources.getSpriteSheet(Sprite.WATERING);
        float scale = WorldScale.PIXEL_SCALE;
        splash = new Image(first);
        splash.setTouchable(Touchable.disabled);
        splash.setSize(first.getRegionWidth() * scale, first.getRegionHeight() * scale);
        splash.setPosition(getWidth() / 2f + (sheet.trimX() - sheet.anchorX()) * scale,
                getHeight() / 2f + (sheet.trimY() - sheet.anchorY()) * scale);
        splashTime = 0;
        addActor(splash);
    }

    /** The water falls; once it has, the crop grows a stage (ripe: it can be picked). */
    private void updateSplash(float delta) {
        if (splash == null) {
            return;
        }
        splashTime += delta;
        Animation<TextureRegion> falling = resources.getAnimation(Sprite.WATERING, Action.SPLASH);
        if (falling.isAnimationFinished(splashTime)) {
            splash.remove();
            splash = null;
            grow();
            return;
        }
        ((TextureRegionDrawable) splash.getDrawable()).setRegion(falling.getKeyFrame(splashTime));
    }

    private void grow() {
        stage++;
        if (stage == ripeStage) {
            inventory.add(new Crop(crop, resources));
        }
        showStage();
    }

    // collecting

    /** Used from a neighbouring tile: the field blocks its own. */
    @Override
    public boolean isDistant() {
        return true;
    }

    /** The crop while it is ripe (picks it), the watering can while it grows (waters it). */
    @Override
    public TextureRegion getActionIcon() {
        return needsWater() ? canIcon : cropIcon;
    }

    /** The watering can: the button's picture while the crop grows (for tests and descriptions). */
    public TextureRegion getWateringCanIcon() {
        return canIcon;
    }

    @Override
    public boolean isBusy() {
        return isBeingWatered();
    }

    /** The top of its plant when it sticks out above the tile (ripe corn), else the tile's top. */
    @Override
    public float getTopY() {
        return Math.max(getHeight(), getAvatar().getY() + getAvatar().getHeight());
    }

    /** The crop is picked: it jumps out and a seedling is left. */
    @Override
    public void beforeCollect() {
        stage = 0;
        showStage();
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
