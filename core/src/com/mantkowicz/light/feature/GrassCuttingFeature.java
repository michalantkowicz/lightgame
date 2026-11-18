package com.mantkowicz.light.feature;

import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.ToolButton;

import static com.mantkowicz.light.feature.GrassFeature.SCYTHE;

/**
 * The rabbit cuts the grass ({@link GrassFeature}) with its scythe: standing still on grass it has the scythe on the
 * HUD's tool button (the big round button right of the face, the hint elsewhere, also just next to grass): tapped, it
 * swings it once (0.8 s) and the grass on its tile and on every tile around it (diagonally too) fades out
 * ({@link GrassFeature#FADE_SECONDS}). Walking off meanwhile, the grass stays. Only on the map the player is in.
 */
public class GrassCuttingFeature implements PlayerFeature {
    private final GameContext context;
    private final Player player;
    private final Hud hud;
    private final GrassFeature grass;
    /** Where the rabbit stands swinging its scythe, until the grass there and around is cut; null otherwise. */
    private Tile cuttingAt;

    public GrassCuttingFeature(GameContext context, Player player, Hud hud, GrassFeature grass) {
        this.context = context;
        this.player = player;
        this.hud = hud;
        this.grass = grass;
    }

    @Override
    public void run(float delta) {
        cut();
        showScythe();
    }

    /** No swing here any more, and the tool button is the hint again. */
    @Override
    public void playerLeft() {
        cuttingAt = null;
        if (scytheShown()) {
            hud.getToolButton().resetTool();
        }
    }

    /**
     * The scythe has swung: the grass on the rabbit's tile and on every tile around it (diagonally too) fades out; if
     * the rabbit walked off meanwhile, it all stays.
     */
    private void cut() {
        if (cuttingAt == null || player.isScything()) {
            return;
        }
        Tile at = cuttingAt;
        cuttingAt = null;
        if (Status.MOVEMENT.equals(player.getStatus()) || player.getTile() != at) {
            return;
        }
        grass.cutAround(at);
    }

    /**
     * Standing still on grass (or swinging at it) the rabbit has the scythe on the HUD's tool button; elsewhere (also
     * just next to grass) the button is the hint again. Only over the hint: another tool (the door) comes first.
     */
    private void showScythe() {
        boolean scythe = cuttingAt != null || canCut();
        if (scythe && ToolButton.HINT.equals(hud.getToolButton().getToolName())) {
            hud.getToolButton().setTool(SCYTHE,
                    context.getResourcesService().getAnimation(Sprite.SCYTHE_ICON, Action.ICON)
                            .getKeyFrame(0),
                    this::cutHere);
        } else if (!scythe && scytheShown()) {
            hud.getToolButton().resetTool();
        }
    }

    /** Is the scythe on the tool button now? */
    private boolean scytheShown() {
        return SCYTHE.equals(hud.getToolButton().getToolName());
    }

    /** The tool button with the scythe tapped: the rabbit swings it at the grass where it stands and around. */
    private void cutHere() {
        if (canCut()) {
            cuttingAt = player.getTile();
            player.scythe();
        }
    }

    /** The rabbit stands still on grass and isn't swinging already. */
    private boolean canCut() {
        Tile tile = player.getTile();
        return cuttingAt == null && tile != null && Status.IDLE.equals(player.getStatus()) && grass.hasGrass(tile);
    }
}
