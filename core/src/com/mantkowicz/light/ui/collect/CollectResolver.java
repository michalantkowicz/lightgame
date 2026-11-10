package com.mantkowicz.light.ui.collect;

import com.mantkowicz.light.actor.Collectible;

public interface CollectResolver {
    void resolveCollect(Collectible collectible);

    /** Called every frame, after the collecting actor's movement. */
    void update();
}
