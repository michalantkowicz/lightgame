package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;
import com.mantkowicz.light.resources.ResourcesService;

/**
 * A character's face in the HUD: an animated picture in a square frame (the pack's inventory slot, stretched). For
 * now the player's own sprite stands in for a portrait (docs/hud-design.md: real portraits come later).
 */
public class Portrait extends Group {
    public static final float WIDTH = 176, HEIGHT = 184;
    private final Animation<TextureRegion> animation;
    private final String name;
    private final Image picture;
    private final float scale;
    private final int cropX, cropY, cropWidth, cropHeight;
    private float time;

    /**
     * @param name what the portrait shows, e.g. "rabbit/idle_down" (for tests and descriptions)
     * @param crop the part of each frame to show, x, y (from the top), width, height in frame pixels: a sprite frame
     *             is mostly empty around the character
     */
    public Portrait(ResourcesService resources, Animation<TextureRegion> animation, String name, int[] crop, float scale) {
        this.animation = animation;
        this.name = name;
        this.scale = scale;
        this.cropX = crop[0];
        this.cropY = crop[1];
        this.cropWidth = crop[2];
        this.cropHeight = crop[3];
        setSize(WIDTH, HEIGHT);
        Image frame = new Image(UiStyle.stretching(resources, UiImage.SLOT));
        frame.setSize(WIDTH, HEIGHT);
        addActor(frame);
        picture = new Image(new TextureRegionDrawable(new TextureRegion(animation.getKeyFrame(0))));
        addActor(picture);
        show(animation.getKeyFrame(0));
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        show(animation.getKeyFrame(time, true));
    }

    private void show(TextureRegion frame) {
        ((TextureRegionDrawable) picture.getDrawable()).getRegion().setRegion(frame, cropX, cropY, cropWidth, cropHeight);
        picture.setSize(cropWidth * scale, cropHeight * scale);
        picture.setPosition((WIDTH - picture.getWidth()) / 2f, (HEIGHT - picture.getHeight()) / 2f + scale);
    }

    public String getPictureName() {
        return name;
    }
}
