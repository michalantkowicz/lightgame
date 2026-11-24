package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/**
 * A small picture that floats up over a character on the map and fades away: what it feels right now (a heart
 * over the player stroking a cow); or one the character tosses up and catches ({@link #tossOver}: an item going from
 * the backpack into the quest's slot). It is drawn over everything on the map, where the character stood when it
 * appeared.
 */
public class FloatingPicture extends Image {
    /** How far it floats up and how long it takes (it fades in the last third). */
    private static final float RISE = 40, SECONDS = 1.2f;
    /**
     * Tossed: it starts this far up the character (its hands), goes this high, up and down this long, this many
     * times; caught the last time, it is gone.
     */
    private static final float TOSS_FROM = 0.2f, TOSS_HEIGHT = 200, TOSS_SECONDS = 0.6f;
    private static final int TOSSES = 1;

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

    /** The character tosses it up and catches it; then it is gone (put away). */
    public void tossOver(Group character) {
        setPosition(Math.round(character.getX() + (character.getWidth() - getWidth()) / 2f),
                character.getY() + character.getHeight() * TOSS_FROM);
        SequenceAction tosses = Actions.sequence();
        for (int toss = 0; toss < TOSSES; toss++) {
            tosses.addAction(Actions.moveBy(0, TOSS_HEIGHT, TOSS_SECONDS / 2, Interpolation.pow2Out));
            tosses.addAction(Actions.moveBy(0, -TOSS_HEIGHT, TOSS_SECONDS / 2, Interpolation.pow2In));
        }
        tosses.addAction(Actions.removeActor());
        addAction(tosses);
        character.getParent().addActor(this);
    }
}
