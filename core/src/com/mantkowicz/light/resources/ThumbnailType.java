package com.mantkowicz.light.resources;

public enum ThumbnailType {
    DEFAULT("collectButton");

    private final String regionName;

    ThumbnailType(String regionName) {
        this.regionName = regionName;
    }

    String getRegionName() {
        return regionName;
    }
}
