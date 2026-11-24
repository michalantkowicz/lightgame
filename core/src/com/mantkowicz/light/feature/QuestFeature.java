package com.mantkowicz.light.feature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.mantkowicz.light.actor.Creature;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.character.FarmAnimal;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.BackpackItemEjectedEvent;
import com.mantkowicz.light.quest.CreatureQuests;
import com.mantkowicz.light.quest.Quest;
import com.mantkowicz.light.quest.QuestJournal;
import com.mantkowicz.light.quest.Thing;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.ui.collect.ItemThrower;
import com.mantkowicz.light.ui.hud.ActionButton;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.ItemFlights;

import java.util.List;

/**
 * The quests on the map the player is in ({@link QuestJournal}: one goes on at a time, in the whole game):
 * <ul>
 * <li>its clock: a quest going on runs out of time;</li>
 * <li>next to a creature offering a quest (a bubble over it), a button with the same picture instead of the bubble
 * (on the right of the row); tapping it starts the quest; with another quest going on, it shakes;</li>
 * <li>next to the creature whose quest waits for something, while it can be given something: a button with that thing
 * (instead of its favourite's when that is the same thing); tapping it gives it one, from the quest's slot first, else
 * from the backpack (it flies from its slot to the creature, which eats it, and the quest goes on); without one the button shakes and the face says
 * "find" with it (so does the hint). Tapping a backpack slot with it does the same ({@link #deliverFrom});</li>
 * <li>what the quest waits for, carried in the backpack, goes into the quest's slot a second after the slot is shown
 * empty (the backpack slot shakes, it flies over into the quest's slot);</li>
 * <li>what a quest gives (once what it was given has flown over to it, and it has eaten it and shown its hearts)
 * lands next to the creature (as from a chest), else next to the player, else in the
 * backpack; what was in the quest's slot when the quest was cancelled or ran out jumps out next to the player, and
 * with nowhere to land, it is gone (tapped, the slot throws its thing out as a backpack slot does: {@code
 * PlayerCollectResolver}).</li>
 * </ul>
 * Only on the map the player is in ({@link PlayerFeature}); its buttons go with the map.
 */
public class QuestFeature implements PlayerFeature {
    /** A carried item goes into the quest's slot once the slot has been shown empty this long. */
    private static final float SLOT_EMPTY_SECONDS = 1;
    /** What a quest gives comes at most this long after its thing was given, however long the creature takes. */
    private static final float MAX_GIFT_SECONDS = 10;
    private final GameContext context;
    private final Player player;
    private final Hud hud;
    private final QuestJournal journal;
    private final ResourcesService resources;
    private final ItemThrower thrower;
    private ActionButton questButton;
    /** What the animal gives back comes once what it was given has flown over to it... */
    private float rewardsWait;
    /** ...and it has eaten it and shown its hearts, but not later than this. */
    private float giftWait;
    /** How long the quest's slot has been empty while a quest goes on. */
    private float slotEmptyFor;
    /** The creature whose quest bubble the start button hides, or null. */
    private Creature hidingOffer;

    /** @param journal the quests of the whole game */
    public QuestFeature(GameContext context, Player player, Hud hud, QuestJournal journal) {
        this.context = context;
        this.player = player;
        this.hud = hud;
        this.journal = journal;
        this.resources = context.getResourcesService();
        this.thrower = new ItemThrower(context);
    }

    @Override
    public void run(float delta) {
        journal.update(delta);
        intoTheSlot(delta);
        rewardsOnceReady(delta);
        throwOutEjected();
        Creature asking = askingCreature();
        Thing waited = asking == null ? null : journal.waitingFrom(quests(asking));
        if (waited != null && !hasAny(waited)) {
            hud.setHint(CreatureFeature.FIND, waited.create(resources).getPicture());
        }
        if (!CreatureButtons.isShown(questButton) && hidingOffer != null) {
            hidingOffer.setOfferShown(true);
            hidingOffer = null;
        }
        if (asking != null && !CreatureButtons.isShown(questButton)) {
            addButton(asking, waited);
        }
    }

    /** A button over the creature: what its quest waits for (to give it), else the quest it offers (to start it). */
    private void addButton(Creature asking, Thing waited) {
        if (waited != null) {
            questButton = CreatureButtons.add(context, "DELIVER", waited.create(resources).getPicture(),
                    () -> deliver(asking), () -> isWaitingFor(asking), asking);
        } else {
            Quest offered = quests(asking).getOffered();
            questButton = CreatureButtons.add(context, "QUEST", offered.getIcon().create(resources).getPicture(),
                    () -> start(asking), () -> isOffering(asking), asking);
            asking.setOfferShown(false);
            hidingOffer = asking;
        }
        questButton.setPlace(CreatureFeature.QUEST_PLACE);
    }

    /** Of the creatures next to the player: one whose quest waits for something it can be given now, else one offering a quest. */
    private Creature askingCreature() {
        Creature offering = null;
        for (Creature creature : CreatureButtons.nextTo(player, context.getBoardService())) {
            if (isWaitingFor(creature)) {
                return creature;
            }
            if (offering == null && isOffering(creature)) {
                offering = creature;
            }
        }
        return offering;
    }

    private boolean isNear(Creature creature) {
        return CreatureButtons.nextTo(player, context.getBoardService()).contains(creature);
    }

    /** It offers a quest, and what it does allows taking something (not while it walks, eats or shows its hearts). */
    private boolean isOffering(Creature creature) {
        return quests(creature) != null && quests(creature).getOffered() != null && creature.canBeFed()
                && isNear(creature);
    }

    private boolean isWaitingFor(Creature creature) {
        return quests(creature) != null && journal.waitingFrom(quests(creature)) != null
                && creature.canBeFed() && isNear(creature);
    }

    /**
     * Does this creature's quest show something of this kind (it waits for it, or the quest it offers has its
     * picture)? Then the button with it is the quest's, not a second one alike.
     */
    static boolean asksFor(QuestJournal journal, Creature creature, Item kind) {
        CreatureQuests quests = quests(creature);
        if (quests == null) {
            return false;
        }
        Thing waited = journal.waitingFrom(quests);
        Thing offered = quests.getOffered() == null ? null : quests.getOffered().getIcon();
        return waited != null && waited.matches(kind) || waited == null && offered != null && offered.matches(kind);
    }

    /** The creature's quests (a farm animal's), or null. */
    static CreatureQuests quests(Creature creature) {
        return creature instanceof FarmAnimal animal ? animal.getQuests() : null;
    }

    private void start(Creature creature) {
        if (!isOffering(creature)) {
            return; // a fading button catches the click but does nothing
        }
        if (!journal.start(quests(creature))) {
            questButton.shake(); // another quest goes on
        }
    }

    private void deliver(Creature creature) {
        if (!isWaitingFor(creature)) {
            return;
        }
        Thing waited = journal.waitingFrom(quests(creature));
        if (waited.matches(journal.getSlot())) {
            giveTo(creature, journal.takeSlot(), ItemFlights.QUEST_SLOT); // out of the quest's slot first
            return;
        }
        int slot = CreatureButtons.slotWith(player, waited.create(resources));
        if (slot < 0) {
            questButton.shake(); // nothing to give: the face says what to find
            hud.showHint();
            return;
        }
        context.getGameEventService().post(new BackpackItemEjectedEvent(slot)); // its slot shakes
        giveTo(creature, player.getInventory().take(slot), slot);
    }

    /** The creature gets it (it flies there from its slot), and the quest goes on; its reward comes once it lands. */
    private void giveTo(Creature creature, Item item, int fromSlot) {
        CreatureButtons.flyTo(context, hud, item, fromSlot, creature);
        rewardsWait = ItemFlights.TO_MAP_SECONDS;
        giftWait = MAX_GIFT_SECONDS;
        creature.give(item);
        journal.delivered();
    }

    private boolean hasAny(Thing thing) {
        return thing.matches(journal.getSlot()) || CreatureButtons.slotWith(player, thing.create(resources)) >= 0;
    }

    /**
     * A tap on a backpack slot works as the quest's button when what is in it is what the quest of a creature next to
     * the player waits for: the creature gets it from this slot.
     *
     * @return false when it isn't: the slot does what it usually does
     */
    public boolean deliverFrom(int slot) {
        for (Creature creature : CreatureButtons.nextTo(player, context.getBoardService())) {
            Thing waited = quests(creature) == null ? null : journal.waitingFrom(quests(creature));
            Item item = player.getInventory().getItems().get(slot);
            if (waited != null && waited.matches(item) && isWaitingFor(creature)) {
                context.getGameEventService().post(new BackpackItemEjectedEvent(slot));
                giveTo(creature, player.getInventory().take(slot), slot);
                return true;
            }
        }
        return false;
    }

    /**
     * What the quest waits for, carried in the backpack, goes into the quest's slot once the slot has been shown empty
     * for {@link #SLOT_EMPTY_SECONDS} (first the row with the slot, then the item): the backpack slot shakes, and it
     * flies over into the quest's slot.
     */
    private void intoTheSlot(float delta) {
        boolean empty = journal.isActive() && journal.getSlot() == null;
        slotEmptyFor = empty ? slotEmptyFor + delta : 0;
        if (slotEmptyFor < SLOT_EMPTY_SECONDS) {
            return;
        }
        List<Item> items = player.getInventory().getItems();
        for (int slot = 0; slot < items.size(); slot++) {
            Item item = items.get(slot);
            if (item != null && journal.wantsInSlot(item)) {
                context.getGameEventService().post(new BackpackItemEjectedEvent(slot));
                journal.putInSlot(player.getInventory().take(slot));
                hud.getFlights().toQuestSlot(CreatureButtons.nameOf(item), item.getPicture(), slot);
                return;
            }
        }
    }

    /** What quests gave lands next to the creature that gave it, else next to the player, else in the backpack. */
    private void handOutRewards() {
        for (Thing reward : journal.takeRewards()) {
            Item item = reward.create(resources);
            GameBoardActor giver = giver();
            boolean landed = giver != null && giver.getTile() != null
                    && thrower.throwOut(item, giver.getTile(), giver.getCenter(), player.getCenter());
            if (!landed && !thrower.throwOut(item, player.getTile(), player.getCenter())) {
                player.getInventory().add(item); // nowhere to land: into the backpack, if there is room
            }
        }
    }

    /** The creature (on this map) whose quest gave the rewards, or null. */
    private FarmAnimal giver() {
        for (Actor actor : context.getGameStage().getActors()) {
            if (actor instanceof FarmAnimal animal && animal.getQuests() == journal.getRewardsFrom()) {
                return animal;
            }
        }
        return null;
    }

    /** What a quest gave comes once what it was given has landed, and the creature has eaten it and shown its hearts. */
    private void rewardsOnceReady(float delta) {
        rewardsWait = Math.max(0, rewardsWait - delta);
        giftWait = Math.max(0, giftWait - delta);
        if (rewardsWait == 0 && !giverTakesItsGift()) {
            handOutRewards();
        }
    }

    /** Is the creature whose quest gave the rewards still getting up for, eating or enjoying its gift? */
    private boolean giverTakesItsGift() {
        FarmAnimal giver = giftWait > 0 ? giver() : null;
        return giver != null && giver.isTakingAGift();
    }

    /** What was in the quest's slot when it ended jumps out next to the player; with nowhere to land, it is gone. */
    private void throwOutEjected() {
        List<Item> ejected = journal.takeEjected();
        for (Item item : ejected) {
            if (player.getTile() != null) {
                thrower.throwOut(item, player.getTile(), player.getCenter());
            }
        }
    }
}
