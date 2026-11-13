package com.mantkowicz.light.light;

import box2dLight.RayHandler;

public class LightFactory {
    public static GameLight createLight(RayHandler rayHandler, LightType lightType) {
        switch (lightType) {
            case TORCH :
                return new TorchLight(rayHandler);
        }
        throw new IllegalArgumentException("No implementation for lightType " + String.valueOf(lightType));
    }
}
