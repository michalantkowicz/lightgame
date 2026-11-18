package com.mantkowicz.light.feature;

import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.map.TileDoor;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.ToolButton;

/**
 * The rabbit came out of a door and stands still on it: the HUD's tool button shows a door, and tapping it takes the
 * rabbit back through, as if it had walked onto the door again ({@link Travel}). It goes once the rabbit walks off. It
 * comes before the scythe on a door with grass. Only on the map the player is in.
 */
public class DoorToolFeature implements PlayerFeature {
    /** The tool's name on the button (for tests and descriptions). */
    public static final String DOOR = "door";

    private final GameContext context;
    private final Player player;
    private final Hud hud;
    private final DoorFeature doors;
    private final Travel travel;

    public DoorToolFeature(GameContext context, Player player, Hud hud, DoorFeature doors, Travel travel) {
        this.context = context;
        this.player = player;
        this.hud = hud;
        this.doors = doors;
        this.travel = travel;
    }

    @Override
    public void run(float delta) {
        ToolButton tool = hud.getToolButton();
        boolean shown = DOOR.equals(tool.getToolName());
        TileDoor door = doorToGoBackThrough();
        if (door != null && !shown) {
            tool.setTool(DOOR, context.getResourcesService().getUiImage(UiImage.DOOR_ICON), this::goBack);
        } else if (door == null && shown) {
            tool.resetTool();
        }
    }

    @Override
    public void playerLeft() {
        if (DOOR.equals(hud.getToolButton().getToolName())) {
            hud.getToolButton().resetTool();
        }
    }

    /** The door the rabbit came out of, if it stands still on it; else null. */
    private TileDoor doorToGoBackThrough() {
        return Status.IDLE.equals(player.getStatus()) ? doors.cameOutOnto(player) : null;
    }

    private void goBack() {
        TileDoor door = doorToGoBackThrough();
        if (door != null) {
            travel.go(player, door);
        }
    }
}
