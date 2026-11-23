package com.mantkowicz.light.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.ThumbnailType;

/** Something an animal gives (a bottle of milk, an egg): it lies on the ground and goes into the backpack, as a crop. */
public class Produce implements Item {
    private final ProduceType type;
    private final TextureRegion picture;

    public Produce(ProduceType type, ResourcesService resources) {
        this.type = type;
        picture = resources.getAnimation(type.getSprite(), Action.ICON).getKeyFrame(0);
    }

    public ProduceType getType() {
        return type;
    }

    @Override
    public TextureRegion getPicture() {
        return picture;
    }

    /** Produce of the same type is alike: any bottle of milk will do. */
    @Override
    public boolean isSameKindAs(Item other) {
        return other instanceof Produce produce && produce.type == type;
    }

    @Override
    public void use() {
    }

    @Override
    public void unuse() {
    }

    @Override
    public ThumbnailType getThumbnailType() {
        return ThumbnailType.DEFAULT;
    }

    /** Its type, e.g. "MILK" (for tests and descriptions). */
    @Override
    public String toString() {
        return type.name();
    }
}
