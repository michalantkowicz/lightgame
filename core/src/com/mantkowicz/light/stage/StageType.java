package com.mantkowicz.light.stage;

/**
 * The game's stages, by what they show: the world, texts over it, the radial menu, the UI. Game actors name the stage
 * they go on ({@link com.mantkowicz.light.actor.GameActorDefinition}); the stages say which one they are. A leaf: it uses
 * nothing of the game, so both sides can use it.
 */
public enum StageType {
    GAME_STAGE, NOTIFICATION_STAGE, MENU_STAGE, UI_STAGE
}
