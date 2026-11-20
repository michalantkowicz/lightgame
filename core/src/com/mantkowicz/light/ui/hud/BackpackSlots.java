package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * The HUD's backpack slots, left of the face ({@link Hud} lays them out): each shows what the player carries in it. A
 * tap on a slot gives its item to an animal next to the player wishing for it, or chooses it for the swap on offer, or
 * else throws it out next to the player (with nowhere for it to land, the slot shakes). The item chosen for a swap
 * pulses, from its own colours to dark brown and back.
 */
public class BackpackSlots {
    /** A slot (84x91: three of them fit left of the face on a screen 800 wide; two were 96x104) and its item. */
    static final float SLOT_WIDTH = 84, SLOT_HEIGHT = 91, ITEM_SIZE = 64; // a 16 px item drawn 4x
    /** How far a shaken slot moves up and down, and how long the first move takes (the whole shake is 6 times that). */
    private static final float SHAKE = 8, SHAKE_SECONDS = 0.04f;
    /** The item chosen for a swap goes from its own colours to this dark brown and back, once in this many seconds. */
    private static final Color SWAP_PULSE = new Color(0.42f, 0.26f, 0.16f, 1);
    private static final float SWAP_PULSE_SECONDS = 1.2f;

    private final List<Image> slots = new ArrayList<>();
    private final List<Image> slotItems = new ArrayList<>();
    private final List<Item> shownItems = new ArrayList<>();
    /** Asked first when a slot is tapped: gives its item away (true) instead of throwing it out. */
    private IntPredicate giveAway = slot -> false;
    /** Asked next when a slot is tapped: chooses it for the swap on offer (true) instead of throwing it out. */
    private IntPredicate chooseForSwap = slot -> false;
    /** The slot whose item would go in the swap on offer (it pulses), or -1 with no swap on offer. */
    private int swapSlot = -1;
    private float swapPulseTime;

    /**
     * @param throwOut throws the item in a slot (0 = the left one) out next to the player; false when it has nowhere
     *                 to land (the slot shakes)
     * @param row      where the slots and their items go (taps on them stay in the HUD)
     */
    BackpackSlots(ResourcesService resources, int count, IntPredicate throwOut, Group row) {
        for (int i = 0; i < count; i++) {
            Image slot = new Image(UiStyle.stretching(resources, UiImage.SLOT));
            slot.setSize(SLOT_WIDTH, SLOT_HEIGHT);
            int index = i;
            slot.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (giveAway.test(index) || chooseForSwap.test(index)) {
                        return; // given to an animal wishing for it, or chosen for the swap (the tool button swaps)
                    }
                    if (!throwOut.test(index)) { // what is in it jumps out next to the player...
                        shake(index); // ...or, with nowhere to land, the slot shakes
                    }
                }
            });
            row.addActor(slot);
            slots.add(slot);
            Image item = new Image();
            item.setScaling(Scaling.fit);
            item.setSize(ITEM_SIZE, ITEM_SIZE);
            item.setTouchable(Touchable.disabled); // taps go to the slot under it
            row.addActor(item);
            slotItems.add(item);
            shownItems.add(null);
        }
    }

    public void setGiveAway(IntPredicate giveAway) {
        this.giveAway = giveAway;
    }

    /**
     * A tap on a slot is offered here next (after giving away): while a swap is on offer it chooses the slot whose item
     * would go; only when this says false does the item jump out next to the player.
     */
    public void setChooseForSwap(IntPredicate chooseForSwap) {
        this.chooseForSwap = chooseForSwap;
    }

    /** The item in this slot would go in the swap on offer: it pulses ({@code -1}: no swap on offer). */
    public void setSwapSlot(int slot) {
        if (slot != swapSlot) {
            swapSlot = slot;
            swapPulseTime = 0;
        }
    }

    public int getSwapSlot() {
        return swapSlot;
    }

    /** How high above a slot's bottom the middle of its face is (over its side, the thicker bottom border). */
    float getFaceMiddle() {
        Drawable slot = slots.get(0).getDrawable();
        return (SLOT_HEIGHT + slot.getBottomHeight() - slot.getTopHeight()) / 2f;
    }

    /** Lines the slots up leftwards from {@code right} (the right slot's right edge), their bottoms on {@code bottom}. */
    void layOut(float right, float bottom, float gap) {
        float x = right;
        for (int i = slots.size() - 1; i >= 0; i--) {
            Image slot = slots.get(i);
            x -= slot.getWidth();
            slot.setPosition(x, bottom);
            Image item = slotItems.get(i);
            item.setPosition(x + (slot.getWidth() - item.getWidth()) / 2f,
                    slot.getY() + (slot.getHeight() - item.getHeight()) / 2f + 4);
            x -= gap;
        }
    }

    /** Shows what the player carries (its first slots) and pulses the item chosen for a swap. */
    void show(List<Item> items, float delta) {
        for (int i = 0; i < slotItems.size(); i++) {
            Item item = i < items.size() ? items.get(i) : null;
            if (item != shownItems.get(i)) {
                shownItems.set(i, item);
                slotItems.get(i).setDrawable(item == null ? null : new TextureRegionDrawable(item.getPicture()));
            }
        }
        swapPulseTime = (swapPulseTime + delta) % SWAP_PULSE_SECONDS;
        float dark = (1 - MathUtils.cos(MathUtils.PI2 * swapPulseTime / SWAP_PULSE_SECONDS)) / 2f;
        for (int i = 0; i < slotItems.size(); i++) {
            Color color = slotItems.get(i).getColor();
            color.set(Color.WHITE);
            if (i == swapSlot) {
                color.lerp(SWAP_PULSE, dark);
            }
        }
    }

    /** How the item chosen for a swap is tinted now (white: its own colours; also with no swap on offer). */
    Color getSwapTint() {
        return swapSlot < 0 ? Color.WHITE : slotItems.get(swapSlot).getColor();
    }

    /** A quick shake up and down of a slot and what is in it: its item jumped out by itself. */
    void shake(int slot) {
        for (Image piece : List.of(slots.get(slot), slotItems.get(slot))) {
            piece.clearActions();
            piece.addAction(Actions.sequence(
                    Actions.moveBy(0, SHAKE, SHAKE_SECONDS, Interpolation.sine),
                    Actions.moveBy(0, -2 * SHAKE, 2 * SHAKE_SECONDS, Interpolation.sine),
                    Actions.moveBy(0, 2 * SHAKE, 2 * SHAKE_SECONDS, Interpolation.sine),
                    Actions.moveBy(0, -SHAKE, SHAKE_SECONDS, Interpolation.sine)));
        }
    }

    /** The slots, left to right. */
    List<Image> getSlots() {
        return slots;
    }

    /** The pictures in the slots, left to right. */
    public List<Image> getSlotItems() {
        return slotItems;
    }

    /** What each slot holds, null for empty. */
    List<Item> getShownItems() {
        return shownItems;
    }
}
