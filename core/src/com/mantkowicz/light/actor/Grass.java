package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.SpriteSheet;

/**
 * Grass grown on a tile, only to look at: it blocks nothing, nobody steps around it, taps go through it. It grows a
 * phase every so often, a still picture each (a tuft, two tufts, a bush, a big bush), standing on the tile's bottom
 * edge; short it stays behind whoever
 * stands there, tall it is drawn over them, a little see-through (where it is drawn is up to
 * {@code feature.GrassFeature}). Cut with the scythe, it fades out.
 */
public class Grass extends Image {
    /**
     * Each phase's empty rows (art pixels) under its lowest blade, inside the sheet's packed box (the box ends at the
     * lowest pixel of any phase): only the single tuft stands 2 px higher. The grass is lowered by them, so every phase
     * stands on the tile's bottom edge.
     */
    private static final int[] EMPTY_ROWS_BELOW = {2, 0, 0, 0};

    private final Tile tile;
    private final TextureRegion[] phases;
    private final float secondsPerPhase;
    private final float tallAlpha;
    private int phase;
    private float sincePhase;
    private boolean cut;

    /**
     * @param secondsPerPhase it grows a phase every this many seconds, until its last
     * @param tallAlpha       how see-through it is once tall (1 not at all)
     */
    public Grass(ResourcesService resources, Tile tile, float secondsPerPhase, float tallAlpha) {
        this(resources, resources.getAnimation(Sprite.GRASS_TUFT, Action.GROW).getKeyFrames(), tile, secondsPerPhase,
                tallAlpha);
    }

    private Grass(ResourcesService resources, TextureRegion[] phases, Tile tile, float secondsPerPhase,
            float tallAlpha) {
        super(phases[0]);
        this.tile = tile;
        this.phases = phases;
        this.secondsPerPhase = secondsPerPhase;
        this.tallAlpha = tallAlpha;
        setTouchable(Touchable.disabled);
        // centred on the tile like every sprite (the sheet's anchor), at the world's pixel scale; standing on its bottom
        SpriteSheet sheet = resources.getSpriteSheet(Sprite.GRASS_TUFT);
        float scale = WorldScale.PIXEL_SCALE;
        setSize(phases[0].getRegionWidth() * scale, phases[0].getRegionHeight() * scale);
        setX(tile.getCenter().x + (sheet.trimX() - sheet.anchorX()) * scale);
        standOnTheTilesBottom();
    }

    /** Its lowest blade on the tile's bottom edge. */
    private void standOnTheTilesBottom() {
        float bottom = tile.getCenter().y - tile.getHeight() / 2f;
        setY(bottom - EMPTY_ROWS_BELOW[Math.min(phase, EMPTY_ROWS_BELOW.length - 1)] * WorldScale.PIXEL_SCALE);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (cut || phase == phases.length - 1) {
            return;
        }
        sincePhase += delta;
        if (sincePhase >= secondsPerPhase) {
            sincePhase = 0;
            phase++;
            ((TextureRegionDrawable) getDrawable()).setRegion(phases[phase]);
            standOnTheTilesBottom();
            if (isTall()) {
                getColor().a = tallAlpha;
            }
        }
    }

    public Tile getTile() {
        return tile;
    }

    /** How far it has grown: 0 a tuft ... {@link #getLastPhase()} a big bush. */
    public int getPhase() {
        return phase;
    }

    public int getLastPhase() {
        return phases.length - 1;
    }

    /** Tall (from its third phase, a bush): drawn over whoever stands there. */
    public boolean isTall() {
        return phase >= 2;
    }

    /** Cut down: it fades out and is gone. */
    public void cutDown(float fadeSeconds) {
        cut = true;
        addAction(Actions.sequence(Actions.fadeOut(fadeSeconds), Actions.removeActor()));
    }

    public boolean isCut() {
        return cut;
    }
}
