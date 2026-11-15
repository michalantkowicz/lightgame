package com.mantkowicz.light.character;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.mantkowicz.light.actor.Crop;
import com.mantkowicz.light.actor.CropType;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.function.IntConsumer;

/**
 * The field's crop growing: its own pictures (the crop's GROW row, drawn by the field itself) after the sprout; ripe,
 * the crop goes into the field, to jump out when it is picked.
 */
class CropGrowth implements Growth {
    private final Image plant;
    private final IntConsumer showFrame;
    private final CropType crop;
    private final ResourcesService resources;
    private final int ripePhase;
    private final TextureRegion picture;

    /**
     * @param plant     the field's own picture (its sprite is the crop's plant)
     * @param showFrame shows a frame of the crop's GROW row on it, counted from 0
     */
    CropGrowth(Image plant, IntConsumer showFrame, CropType crop, ResourcesService resources) {
        this.plant = plant;
        this.showFrame = showFrame;
        this.crop = crop;
        this.resources = resources;
        this.ripePhase = resources.getAnimation(crop.getPlant(), Action.GROW).getKeyFrames().length;
        this.picture = resources.getTexture(crop.getPicture());
    }

    @Override
    public Image getPlant() {
        return plant;
    }

    @Override
    public int getRipePhase() {
        return ripePhase;
    }

    @Override
    public void showPhase(int phase) {
        showFrame.accept(Math.max(0, phase - 1));
    }

    /** The crop. */
    @Override
    public TextureRegion getRipePicture() {
        return picture;
    }

    @Override
    public boolean givesCrop() {
        return true;
    }

    @Override
    public void ripe(Inventory inside) {
        inside.add(new Crop(crop, resources));
    }
}
