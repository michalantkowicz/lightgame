package com.mantkowicz.light.feature;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.mantkowicz.light.actor.Creature;
import com.mantkowicz.light.actor.FloatingPicture;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.BackpackItemEjectedEvent;
import com.mantkowicz.light.quest.QuestJournal;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.hud.ActionButton;
import com.mantkowicz.light.ui.hud.Hud;

import java.util.List;

/**
 * What the player does with a creature (a cow, a chicken) the player stands next to (on a tile it is used from:
 * {@link Tile#getUsedFromNeighbours()}), on action buttons in a row over the creature:
 * <ul>
 * <li>it can be stroked: a button with a hand (on the left); tapping it strokes the creature: it stands still while
 * the hand fades out, then shows its hearts (not covered by the button), and a heart floats up over the player;</li>
 * <li>it can be fed: a button with its favourite thing (right of the hand), whenever what it does allows it (not while
 * it walks or eats, not at night). Tapping it takes one out of the backpack (its slot shakes) and gives it to the
 * creature, which eats it; when the player doesn't carry one, the button shakes and the face says "find" with the
 * thing's picture (also what the HUD's hint button says next to it). Tapping the backpack slot with it does the same
 * as the button ({@link #giveFrom}).</li>
 * </ul>
 * Its quests have their buttons too ({@link QuestFeature}). The buttons fade out when what they do is no longer
 * possible (the player walks away, the creature walks or eats). With several creatures next to the player (restless
 * chickens) each button is for one of them and stays over it: the favourite's for one whose favourite the player
 * carries (else any that can be fed), the hand for that one too if it can be stroked (else any that can).
 * <p>
 * Only on the map the player is in ({@link PlayerFeature}); nothing to take away when it goes: its buttons go with the
 * map, and the player's new map sets the hint.
 */
public class CreatureFeature implements PlayerFeature {
    /** What the hint says next to a creature whose favourite thing the player doesn't carry; the picture follows. */
    static final String FIND = "find";
    /**
     * The heart floating up over the player after a stroke: screen pixels per pack pixel (smaller than the UI's), and
     * how far over its tile it starts (right over its ears).
     */
    private static final float HEART_SCALE = 1.5f, HEART_GAP = 0;

    /** Places in the row of buttons over the creature: the hand, its favourite, its quest. */
    static final int STROKE_PLACE = 0, GIVE_PLACE = 1, QUEST_PLACE = 2;

    private final GameContext context;
    private final QuestJournal quests;
    private final Player player;
    private final Hud hud;
    private final TextureRegion hand, heart;
    private ActionButton strokeButton, giveButton;
    /** The quests' buttons (a backpack slot is offered to them first), or null. */
    private QuestFeature questFeature;
    /** Stroked: the small heart floats up over the player once the hand has faded out. */
    private boolean heartAfterTheHand;

    /** @param quests the quests of the whole game: what one waits for has the quest's button, not the favourite's */
    public CreatureFeature(GameContext context, Player player, Hud hud, QuestJournal quests) {
        this.quests = quests;
        this.context = context;
        this.player = player;
        this.hud = hud;
        hand = context.getResourcesService().getUiImage(UiImage.HAND_ICON);
        heart = context.getResourcesService().getUiImage(UiImage.HEART);
    }

    /** The quests' buttons: a tapped backpack slot is offered to them first ({@link #giveFrom}). */
    public void setQuests(QuestFeature questFeature) {
        this.questFeature = questFeature;
    }

    @Override
    public void run(float delta) {
        heartOnceTheHandIsGone();
        List<Creature> near = CreatureButtons.nextTo(player, context.getBoardService());
        Creature feeding = creatureToFeed(near);
        if (feeding != null && CreatureButtons.slotWith(player, feeding.getFavourite()) < 0) {
            hud.setHint(FIND, feeding.getFavourite().getPicture());
        } else {
            hud.resetHint();
        }
        if (feeding != null && !CreatureButtons.isShown(giveButton)) {
            giveButton = CreatureButtons.add(context, "GIVE", feeding.getFavourite().getPicture(), () -> give(feeding),
                    () -> canGive(feeding), feeding);
            giveButton.setPlace(GIVE_PLACE);
        }
        Creature strokable = strokableCreature(near, feeding);
        if (strokable != null && !CreatureButtons.isShown(strokeButton)) {
            strokeButton = CreatureButtons.add(context, "STROKE", hand, () -> stroke(strokable),
                    () -> canStroke(strokable), strokable);
            strokeButton.setPlace(STROKE_PLACE);
        }
    }

    /**
     * Of the creatures next to the player, the one the favourite's button is for: one that can be fed whose favourite
     * the player carries, else the first one that can be fed; null when none can. A creature whose quest waits for its
     * favourite, or offers a quest with its favourite's picture, is left to its quest's button ({@link QuestFeature}).
     */
    private Creature creatureToFeed(List<Creature> near) {
        Creature first = null;
        for (Creature creature : near) {
            if (!creature.canBeFed() || QuestFeature.asksFor(quests, creature, creature.getFavourite())) {
                continue;
            }
            if (CreatureButtons.slotWith(player, creature.getFavourite()) >= 0) {
                return creature;
            }
            if (first == null) {
                first = creature;
            }
        }
        return first;
    }

    /** The one the hand is for: the one to feed if it can be stroked (the buttons stay together), else the first. */
    private static Creature strokableCreature(List<Creature> near, Creature feeding) {
        if (feeding != null && feeding.canBeStroked()) {
            return feeding;
        }
        for (Creature creature : near) {
            if (creature.canBeStroked()) {
                return creature;
            }
        }
        return null;
    }

    /**
     * Its button stays while it is still the one to feed: next to the player and able to be fed, and no other one next
     * to the player has its favourite carried when it hasn't (then its button fades and comes over that one).
     */
    private boolean canGive(Creature creature) {
        return creatureToFeed(CreatureButtons.nextTo(player, context.getBoardService())) == creature;
    }

    private void give(Creature creature) {
        if (!canGive(creature)) {
            return; // a fading button catches the click but does nothing
        }
        int slot = CreatureButtons.slotWith(player, creature.getFavourite());
        if (slot < 0) {
            giveButton.shake(); // nothing to give: the face says what to find
            hud.showHint();
            return;
        }
        handOver(slot, creature);
    }

    /**
     * A tap on a backpack slot works as the favourite's button when what is in it is the favourite thing of a creature
     * next to the player that can be fed: the creature gets it from this slot (it shakes).
     *
     * @return false when it isn't (an empty slot, something else, nobody to feed): the slot does what it usually does
     */
    public boolean giveFrom(int slot) {
        if (questFeature != null && questFeature.deliverFrom(slot)) {
            return true; // what a quest next to the player waits for
        }
        Creature feeding = creatureToFeed(CreatureButtons.nextTo(player, context.getBoardService()));
        if (feeding == null || !feeding.getFavourite().isSameKindAs(player.getInventory().getItems().get(slot))) {
            return false;
        }
        handOver(slot, feeding);
        return true;
    }

    /** Takes the item out of the backpack slot (the HUD shakes it), it flies to the creature, which gets it. */
    private void handOver(int slot, Creature creature) {
        Item item = player.getInventory().take(slot);
        context.getGameEventService().post(new BackpackItemEjectedEvent(slot));
        CreatureButtons.flyTo(context, hud, item, slot, creature);
        creature.give(item);
    }

    private boolean canStroke(Creature creature) {
        return CreatureButtons.nextTo(player, context.getBoardService()).contains(creature) && creature.canBeStroked();
    }

    /**
     * Strokes it at once: it stands still while the hand fades out and then shows its hearts (so nothing covers them,
     * and it can't walk off meanwhile); then a small heart floats up over the player.
     */
    private void stroke(Creature creature) {
        if (!canStroke(creature)) {
            return;
        }
        creature.stroke();
        heartAfterTheHand = true;
    }

    private void heartOnceTheHandIsGone() {
        if (heartAfterTheHand && !CreatureButtons.isShown(strokeButton)) {
            heartAfterTheHand = false;
            new FloatingPicture("heart", heart, HEART_SCALE).showOver(player, HEART_GAP);
        }
    }

}
