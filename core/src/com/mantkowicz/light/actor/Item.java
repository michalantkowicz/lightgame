package com.mantkowicz.light.actor;

import com.mantkowicz.light.resources.Thumbnail;

public interface Item extends Thumbnail {
    void use();

    void unuse();
}
