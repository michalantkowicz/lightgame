package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

/**
 * A small bubble with a picture in it over a character on the map: what it wishes for (a carrot over the cow).
 * Pictures, not words: the players can't read. It pops up (the pack's bubble
 * animation), then shows the picture. It follows its character around and is drawn over everyone else on the map
 * (someone standing behind the character doesn't cover it); the HUD is on its own stage, above it.
 */
public class PictureBubble extends Group {
    /** Screen pixels per pack pixel: the bubble, and the biggest a picture in it gets (it is scaled by whole numbers). */
    private static final int SCALE = 4;
    private static final float PICTURE_SIZE = 100;
    /** The bubble's frame is 44x56 pack pixels; the box's middle is 32 above its bottom (its pointer is under it). */
    private static final float BOX_MIDDLE_Y = 32;
    /** Between the character's tile and the bubble's frame. */
    private static final float GAP = 8;
    private static final float FADE_SECONDS = 0.15f;

    private final Animation<TextureRegion> pop;
    private final TextureRegionDrawable bubble;
    private final String name;
    private Group character;
    private float time;

    /**
     * @param name what the picture shows, e.g. "carrot" (for tests and descriptions)
     */
    public PictureBubble(ResourcesService resources, String name, TextureRegion picture) {
        this.name = name;
        setTouchable(Touchable.disabled);
        pop = resources.getUiAnimation(UiImage.PICTURE_BUBBLE);
        TextureRegion frame = pop.getKeyFrame(0);
        bubble = new TextureRegionDrawable(frame);
        Image background = new Image(bubble);
        background.setSize(frame.getRegionWidth() * SCALE, frame.getRegionHeight() * SCALE);
        addActor(background);
        setSize(background.getWidth(), background.getHeight());

        int scale = Math.max(1, (int) (PICTURE_SIZE / Math.max(picture.getRegionWidth(), picture.getRegionHeight())));
        Image inside = new Image(picture);
        inside.setSize(picture.getRegionWidth() * scale, picture.getRegionHeight() * scale);
        inside.setPosition(Math.round((getWidth() - inside.getWidth()) / 2f),
                Math.round(BOX_MIDDLE_Y * SCALE - inside.getHeight() / 2f));
        // the picture comes once the bubble has popped up
        inside.addAction(sequence(alpha(0), delay(pop.getAnimationDuration()), fadeIn(FADE_SECONDS)));
        addActor(inside);
    }

    /** Shows it over the character until it is removed ({@link #remove()}). */
    public void showOver(Group character) {
        this.character = character;
        character.getParent().addActor(this);
        follow();
    }

    /** Over its character, and in front of everything else on the map (whoever was added since). */
    private void follow() {
        setPosition(Math.round(character.getX() + (character.getWidth() - getWidth()) / 2f),
                character.getY() + character.getHeight() + GAP);
        toFront();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (character.getStage() == null) {
            remove(); // its character is gone
            return;
        }
        follow();
        time += delta;
        bubble.setRegion(pop.getKeyFrame(time));
    }

    /** What its picture shows, e.g. "carrot". */
    public String getPictureName() {
        return name;
    }
}
