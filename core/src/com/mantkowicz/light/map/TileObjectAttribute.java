package com.mantkowicz.light.map;

public enum TileObjectAttribute {
    OBJECT_CLASS("objectClass"),
    TEXTURE_NAME("textureName"),
    TEXTURE_OFFSET("textureOffset"),
    LIGHT_TYPE("lightType"),
    LIGHT_COLOR_HEX("lightColorHex"),
    BORDER_LIGHT_POSITION("borderLightPosition"),
    /** A cow's favourite crop, e.g. "CARROT" (with several, the first); a carrot without it. */
    FAVOURITE_DISH("favouriteDish"),
    /** An animal's quests (see quest.QuestBook), e.g. "giveMilk" or several: "giveMilk,layEgg"; none without it. */
    QUESTS("quests"),
    /** What grows on a field, e.g. "CORN"; a carrot without it. */
    CROP_TYPE("cropType"),
    /** How likely a field grows a weed instead of its crop after watering, 0..1, e.g. "0" for never; 0.4 without it. */
    WEED_CHANCE("weedChance"),
    /** How far grass (objectClass GRASS) has grown: 1 a tuft, 2 two tufts, 3 a bush, 4 a big bush; 1 without it. */
    PHASE("phase"),
    /**
     * A door's name (objectClass DOOR), the same on both of its maps: going through it, one comes out of the door with
     * this name on the other map.
     */
    DOOR("door"),
    /** The map a door leads to, e.g. "maps/coop.tmx". */
    TARGET_MAP("targetMap"),
    /** Who may go through a door, e.g. "PLAYER,CHICKEN" (the player, chickens and chicks); everyone without it. */
    PASSES("passes"),
    /**
     * Who goes through a door to sleep, e.g. "CHICKEN" on the farm's door to the coop: at night they walk in, in the
     * morning back out; nobody without it.
     */
    BEDTIME("bedtime"),
    /** A frog's colour (objectClass FROG): "green" or "pale"; one of them at random without it. */
    COLOUR("colour");

    private final String value;

    TileObjectAttribute(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
