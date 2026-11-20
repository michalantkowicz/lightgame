package com.mantkowicz.light.feature;

import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.ui.hud.BackpackSlots;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.ToolButton;

import java.util.function.IntPredicate;

/**
 * Swapping a backpack item for the item the player stands on, on the HUD's tool button. Standing still on an item with
 * the backpack full (it isn't picked up: nothing jumps out by itself), the tool button widens: the backpack item that
 * would go on its left, the swap arrows, the item on the ground on its right; that backpack item pulses (dark brown and
 * back). It is the left slot's at first; tapping another slot chooses that one instead (nothing moves). Tapping the
 * tool button swaps them: the item from the ground goes into that slot and the slot's item lies on the ground instead,
 * so tapping it again swaps them back. Stepping off, the button is round again and the left slot comes first next time.
 * <p>
 * The swap comes before every tool (the scythe on grass, the rod on a fishing spot, the door): it runs before them,
 * and they take the button only while it shows the hint.
 */
public class SwapFeature implements PlayerFeature {
    private final Player player;
    private final Hud hud;
    private final CollectFeature collecting;
    /** The backpack slot whose item would go. */
    private int slot;
    /** What a tap on a backpack slot asks this feature first (see {@link #choose}). */
    private final IntPredicate chooser = this::choose;

    public SwapFeature(Player player, Hud hud, CollectFeature collecting) {
        this.player = player;
        this.hud = hud;
        this.collecting = collecting;
    }

    @Override
    public void run(float delta) {
        slots().setChooseForSwap(chooser); // the map the player is in chooses (this feature runs only there)
        ItemOnGround offer = collecting.swapOffer();
        if (offer == null) {
            takeAway();
            return;
        }
        hud.getToolButton().setSwap(player.getInventory().getItems().get(slot).getPicture(),
                offer.getItem().getPicture(), this::swap);
        slots().setSwapSlot(slot);
    }

    /**
     * A backpack slot tapped: while a swap is on offer, its item is the one that would go (nothing moves yet).
     *
     * @return false with no swap on offer: the slot does what it usually does
     */
    boolean choose(int tapped) {
        if (!ToolButton.SWAP.equals(hud.getToolButton().getToolName()) || collecting.swapOffer() == null) {
            return false;
        }
        slot = tapped;
        slots().setSwapSlot(slot);
        return true;
    }

    private BackpackSlots slots() {
        return hud.getBackpackSlots();
    }

    private void swap() {
        collecting.swap(slot);
    }

    @Override
    public void playerLeft() {
        takeAway();
    }

    /** No swap on offer: the tool button round again (if it was the swap), nothing pulses, the left slot first again. */
    private void takeAway() {
        slot = 0;
        slots().setSwapSlot(-1);
        if (ToolButton.SWAP.equals(hud.getToolButton().getToolName())) {
            hud.getToolButton().resetTool();
        }
    }
}
