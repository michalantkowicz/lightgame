package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/**
 * "Zzz" over a sleeping character, gently bobbing up and down until it is removed. It is on the stage drawn over the
 * map's lights (the notifications'), so it stays bright at night.
 */
public class Zzz extends Image {
    /** Screen pixels per pixel of the picture; how far it bobs and how long one way takes. */
    private static final float SCALE = 3, BOB = 8, BOB_SECONDS = 0.9f;
    /**
     * Where its bottom-left corner is on the character's tile, a part of its width and height: by the head of an animal
     * lying down (low in its tile), over its back.
     */
    private static final float ACROSS = 0.55f, UP = 0.45f;

    private final Stage stage;

    /** @param stage where it is shown: over the map's lights */
    public Zzz(TextureRegion picture, Stage stage) {
        super(picture);
        this.stage = stage;
        setName("zzz");
        setTouchable(Touchable.disabled);
        setSize(picture.getRegionWidth() * SCALE, picture.getRegionHeight() * SCALE);
    }

    /**
     * Over the character (which doesn't move while it sleeps); call it every frame: when it isn't shown (yet, or any
     * more: the notifications are cleared when the player leaves the map) it comes back.
     */
    public void keepOver(Group character) {
        if (getStage() != null) {
            return;
        }
        clearActions();
        setPosition(Math.round(character.getX() + character.getWidth() * ACROSS),
                Math.round(character.getY() + character.getHeight() * UP));
        addAction(Actions.forever(Actions.sequence(Actions.moveBy(0, BOB, BOB_SECONDS, Interpolation.sine),
                Actions.moveBy(0, -BOB, BOB_SECONDS, Interpolation.sine))));
        stage.addActor(this);
    }
}
