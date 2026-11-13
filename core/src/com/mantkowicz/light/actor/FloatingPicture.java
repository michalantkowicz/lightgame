package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/**
 * A small picture that floats up over a character on the map and fades away: what it feels right now (a heart
 * over the player stroking a cow). It is drawn over everything on the map, where the character
 * stood when it appeared.
 */
public class FloatingPicture extends Image {
    /** How far it floats up and how long it takes (it fades in the last third). */
    private static final float RISE = 40, SECONDS = 1.2f;

    /**
     * @param name  what it shows, e.g. "heart" (for tests and descriptions)
     * @param scale screen pixels per pixel of the picture
     */
    public FloatingPicture(String name, TextureRegion picture, float scale) {
        super(picture);
        setName(name);
        setTouchable(Touchable.disabled);
        setSize(picture.getRegionWidth() * scale, picture.getRegionHeight() * scale);
    }

    /**
     * Shows it over the character; it goes by itself.
     *
     * @param gap how far over the character's tile it starts (a picture may stick out of its tile)
     */
    public void showOver(Group character, float gap) {
        setPosition(Math.round(character.getX() + (character.getWidth() - getWidth()) / 2f),
                character.getY() + character.getHeight() + gap);
        addAction(Actions.sequence(
                Actions.parallel(Actions.moveBy(0, RISE, SECONDS),
                        Actions.sequence(Actions.delay(SECONDS * 2 / 3), Actions.fadeOut(SECONDS / 3))),
                Actions.removeActor()));
        character.getParent().addActor(this); // on top: not hidden by whoever stands behind the character
    }
}
