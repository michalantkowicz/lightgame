package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.actor.Inventory;

/**
 * What grows on a {@link Field} after its sprout (phase 0): its crop ({@link CropGrowth}) or a weed
 * ({@link WeedGrowth}). The field picks one when the water has fallen on it and asks it for its pictures, when it is
 * ripe and what ripening gives; with seeds, later, planting would pick the crop without the dice.
 */
interface Growth {
    /** Its picture on the field (shown from phase 1 on; it bounces and squashes as it grows). */
    Image getPlant();

    /** The field's phase it is ripe at: its pictures come after the field's sprout. */
    int getRipePhase();

    /** Shows its picture for a phase of the field, the first one at phase 1. */
    void showPhase(int phase);

    /** What the field's button shows once it is ripe. */
    TextureRegion getRipePicture();

    /** Is it the field's crop? The sign shows the crop then; a question mark otherwise. */
    boolean givesCrop();

    /** It is ripe: what it gives goes into the field (the crop), or nothing (a weed). */
    void ripe(Inventory inside);
}
