package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Scaling;

import java.util.ArrayList;
import java.util.List;

/**
 * Items flying out of the HUD's slots, in an arc, drawn over everything: given to an animal, an item flies from its
 * slot (a backpack slot or the quest's) to the animal on the map; carried when a quest wants it, it flies from its
 * backpack slot into the quest's slot (which shows it once it has landed). Only a picture: what happens to the item
 * has happened already.
 */
public class ItemFlights {
    /** {@link #toScreen}'s and {@link #toQuestSlot}'s slot for the quest's slot, besides the backpack's 0, 1, 2. */
    public static final int QUEST_SLOT = -1;
    /** How long a flight to the map takes (what the animal gives back comes once it has landed). */
    public static final float TO_MAP_SECONDS = 0.8f;
    /** How long a flight between slots takes, how much higher than the higher of its ends it goes, how big it ends. */
    private static final float SECONDS = 0.5f, ARC = 90, END_SCALE = 0.8f;

    private final Group hud;
    private final BackpackSlots backpack;
    private QuestPanel questPanel;

    ItemFlights(Group hud, BackpackSlots backpack) {
        this.hud = hud;
        this.backpack = backpack;
    }

    void setQuestPanel(QuestPanel questPanel) {
        this.questPanel = questPanel;
    }

    /**
     * The item flies from a slot to this point of the screen (pixels, y down; e.g. an animal on the map).
     *
     * @param name what it shows, e.g. "carrot" (for tests)
     */
    public void toScreen(String name, TextureRegion picture, int fromSlot, Vector2 screen) {
        if (hud.getStage() == null) {
            return;
        }
        Vector2 to = hud.stageToLocalCoordinates(hud.getStage().screenToStageCoordinates(screen.cpy()));
        if (fromSlot == QUEST_SLOT) {
            questPanel.holdFor(TO_MAP_SECONDS); // the row stays until it has landed, even if the quest is done
        }
        fly(name, picture, middleOf(slot(fromSlot)), to, TO_MAP_SECONDS, () -> {
        });
    }

    /** The item flies from a backpack slot into the quest's slot, which shows it once it has landed. */
    public void toQuestSlot(String name, TextureRegion picture, int fromSlot) {
        if (questPanel == null) {
            return;
        }
        questPanel.expectArrival();
        fly(name, picture, middleOf(slot(fromSlot)), middleOf(questPanel.getSlot()), SECONDS, questPanel::arrived);
    }

    private void fly(String name, TextureRegion picture, Vector2 from, Vector2 to, float seconds, Runnable landed) {
        Image item = new Image(picture);
        item.setName("flying " + name);
        item.setScaling(Scaling.fit);
        item.setTouchable(Touchable.disabled);
        float size = BackpackSlots.ITEM_SIZE;
        item.setBounds(from.x - size / 2f, from.y - size / 2f, size, size);
        item.setOrigin(size / 2f, size / 2f);
        float top = Math.max(from.y, to.y) + ARC;
        item.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(to.x - from.x, 0, seconds),
                        Actions.sequence(Actions.moveBy(0, top - from.y, seconds / 2, Interpolation.pow2Out),
                                Actions.moveBy(0, to.y - top, seconds / 2, Interpolation.pow2In)),
                        Actions.scaleTo(END_SCALE, END_SCALE, seconds)),
                Actions.run(landed),
                Actions.removeActor()));
        hud.addActor(item);
    }

    private Actor slot(int slot) {
        return slot == QUEST_SLOT ? questPanel.getSlot() : backpack.getSlots().get(slot);
    }

    /** The middle of a slot, in the HUD's coordinates. */
    private Vector2 middleOf(Actor slot) {
        return slot.localToActorCoordinates(hud, new Vector2(slot.getWidth() / 2f, slot.getHeight() / 2f));
    }

    /** The names of the items flying now, e.g. ["carrot"]: for tests. */
    public List<String> describe() {
        List<String> flying = new ArrayList<>();
        for (Actor child : hud.getChildren()) {
            if (child.getName() != null && child.getName().startsWith("flying ")) {
                flying.add(child.getName().substring("flying ".length()));
            }
        }
        return flying;
    }
}
