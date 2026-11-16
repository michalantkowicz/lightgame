package com.mantkowicz.light.character;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/** How a field's plant moves, always from its bottom: a bounce at each new phase, slow squashing while it grows. */
final class PlantMoves {
    /** Each new phase the plant grows a little taller for a moment: how much, and how long up and back down. */
    private static final float BOUNCE_SCALE = 1.15f, BOUNCE_SECONDS = 0.15f;
    /**
     * While it grows the plant slowly gets a little shorter and wider and back ("breathing", not the up and down bob of
     * a crop lying on the ground): by how much, and how long each way.
     */
    private static final float SQUASH = 0.05f, SQUASH_SECONDS = 0.75f;

    private PlantMoves() {
    }

    /**
     * The plant grows a little taller for a moment: something happened. Still growing, it then squashes and stretches
     * slowly until the next phase.
     */
    static void bounce(Image plant, boolean growing) {
        plant.setOrigin(plant.getWidth() / 2f, 0);
        plant.clearActions();
        plant.setScale(1);
        SequenceAction moves = Actions.sequence(Actions.scaleTo(BOUNCE_SCALE, BOUNCE_SCALE, BOUNCE_SECONDS),
                Actions.scaleTo(1, 1, BOUNCE_SECONDS));
        if (growing) {
            moves.addAction(Actions.forever(Actions.sequence(
                    Actions.scaleTo(1 + SQUASH, 1 - SQUASH, SQUASH_SECONDS, Interpolation.sine),
                    Actions.scaleTo(1, 1, SQUASH_SECONDS, Interpolation.sine))));
        }
        plant.addAction(moves);
    }
}
