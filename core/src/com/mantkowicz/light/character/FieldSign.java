package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.SpriteSheet;
import com.mantkowicz.light.resources.UiImage;

/**
 * A field's sign at its tile's top left corner, sticking out over it a little: the crop, or the blank sign with a
 * question mark on its board while nobody knows what grows there (an empty field, or a weed).
 */
class FieldSign extends Group {
    /** The sign is drawn at half the world's pixel scale, so it fits in the corner. */
    private static final float SCALE = WorldScale.PIXEL_SCALE / 2;
    /** How far the sign sticks out over the tile's left and top edges. */
    private static final float OVERFLOW = WorldScale.PIXEL_SCALE;
    /**
     * The question mark: as tall as the sign's board (7 of its 16 pixels), centred on the board (its middle 8 pixels
     * from the sign's left and 9.5 from its bottom).
     */
    private static final float UNKNOWN_HEIGHT = 7, UNKNOWN_X = 8, UNKNOWN_Y = 9.5f;

    private final ResourcesService resources;
    private final Sprite cropSign;
    private final Image sign;
    private final Image unknown;

    /**
     * @param tileHeight the field's height: the sign's top sticks out over it
     */
    FieldSign(ResourcesService resources, Sprite cropSign, float tileHeight) {
        this.resources = resources;
        this.cropSign = cropSign;
        setTouchable(Touchable.disabled);
        sign = new Image(picture(cropSign));
        sign.setSize(sign.getPrefWidth() * SCALE, sign.getPrefHeight() * SCALE);
        sign.setPosition(-OVERFLOW, tileHeight + OVERFLOW - sign.getHeight());
        addActor(sign);

        SpriteSheet blank = resources.getSpriteSheet(Sprite.SIGN_BLANK);
        unknown = new Image(resources.getUiImage(UiImage.HINT_ICON));
        float height = UNKNOWN_HEIGHT * SCALE;
        unknown.setSize(unknown.getPrefWidth() * height / unknown.getPrefHeight(), height);
        unknown.setPosition(sign.getX() + (UNKNOWN_X - blank.trimX()) * SCALE - unknown.getWidth() / 2,
                sign.getY() + (UNKNOWN_Y - blank.trimY()) * SCALE - height / 2);
        addActor(unknown);
    }

    /** The crop, or the question mark on the blank sign. */
    void showCrop(boolean known) {
        ((TextureRegionDrawable) sign.getDrawable()).setRegion(picture(known ? cropSign : Sprite.SIGN_BLANK));
        unknown.setVisible(!known);
    }

    boolean showsCrop() {
        return !unknown.isVisible();
    }

    private TextureRegion picture(Sprite sheet) {
        return resources.getAnimation(sheet, Action.SIGN).getKeyFrame(0);
    }
}
