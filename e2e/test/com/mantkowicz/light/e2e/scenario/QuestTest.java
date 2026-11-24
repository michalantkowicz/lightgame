package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quests (quest.Quest, QuestJournal, feature.QuestFeature, ui.hud.QuestPanel): an animal with quests (Tiled's quests
 * property) offers one when it should start (a bubble with its picture over the animal); next to it, a button with
 * the same picture starts it. One quest goes on at a time. A row over the backpack shows it: the animal's face, an
 * arrow, what it waits for, the time left (a line, green, yellow, red), the X; the golden quest slot. What it waits for, picked up,
 * goes into the slot first (one carried in the backpack too); given to the animal the quest goes on, and what it gives
 * lands next to the animal. Cancelled (the X) or out of time, what is in the slot jumps out.
 */
class QuestTest {
    static {
        // starts at once (when nothing else goes on): a carrot for milk, half a minute
        GameDriver.registerQuest("testCarrotForMilk", "carrot", "milk", 30, false);
        // offered even while another quest goes on: corn for an egg
        GameDriver.registerQuest("testPushyCornForEgg", "corn", "egg", 180, true);
    }

    /** The rabbit under a cow with two quests: a carrot for milk, then corn for an egg. */
    private static final TestMap TWO_QUESTS = TestMap.of(
            "~~~~~~~~~",
            "~.......~",
            "~...K...~",
            "~...P...~",
            "~.......~",
            "~~~~~~~~~")
            .with('K', "COW", Map.of("quests", "testCarrotForMilk,testPushyCornForEgg"));

    /** The rabbit under a cow with the quest, room around them. */
    private static final TestMap MEADOW = TestMap.of(
            "~~~~~~~~~",
            "~.......~",
            "~...K...~",
            "~...P...~",
            "~.......~",
            "~~~~~~~~~")
            .with('K', "COW", Map.of("quests", "testCarrotForMilk"));

    /** The rabbit between a cow (carrot for milk) and a chicken (corn for an egg, offered whatever goes on). */
    private static final TestMap TWO = TestMap.of(
            "~~~~~~~",
            "~.....~",
            "~.KPH.~",
            "~.....~",
            "~~~~~~~")
            .with('K', "COW", Map.of("quests", "testCarrotForMilk"))
            .with('H', "CHICKEN", Map.of("quests", "testPushyCornForEgg"));

    /** A bubble with its picture far from the rabbit; next to it, the button instead; tapped, the quest starts. */
    @Test
    void aQuestIsOfferedAndStartedWithItsButton() {
        GameDriver game = GameDriver.start(MEADOW.write("quest-meadow"));
        game.seconds(3);
        assertEquals("testCarrotForMilk", game.questOffered(Kind.COW));
        assertEquals("hidden", game.cowBubble(), "next to it: no bubble...");
        assertEquals(List.of(1f), game.buttonAlphas("QUEST"), "...the button with the carrot instead");
        assertEquals(List.of(), game.buttonAlphas("GIVE"), "its favourite is the quest's carrot too: one button");
        assertEquals("none", game.quest());
        assertFalse(game.questPanelShown());

        game.click(new Cell(1, 1));
        game.seconds(4);
        assertEquals("carrot", game.cowBubble(), "far from it: the bubble with the carrot");
        assertEquals(List.of(), game.buttonAlphas("QUEST"));
        game.tapBubble(Kind.COW);
        game.seconds(4);
        assertEquals(List.of(1f), game.buttonAlphas("QUEST"), "tapping the bubble walked the rabbit next to it");

        game.clickButton("QUEST");
        game.frames(2);
        assertTrue(game.quest().startsWith("testCarrotForMilk COW_FACE -> carrot slot=- line=100% "), game.quest());
        assertTrue(game.questPanelShown(), "the row over the backpack shows it");
        assertEquals("none", game.questOffered(Kind.COW));
        game.frames(30);
        assertEquals("none", game.cowBubble(), "no bubble while it goes on");
        assertTrue(game.describeHud().contains("day clock: DAY arrow=0 at=(24.0,1124.0) size=220.8x120.0"),
                "the clock stays as it is: " + game.describeHud());
        assertFalse(game.crashed());
    }

    /**
     * Picked up while the quest waits for it, a carrot goes into the quest's slot (the backpack has room); the next
     * one into the backpack. Given to the cow (from the slot first), it eats it, the quest is done, and its milk lands
     * next to the cow.
     */
    @Test
    void whatItWaitsForGoesIntoTheSlotAndIsGivenFromThere() {
        GameDriver game = startedQuest();
        Cell rabbit = game.playerCell();
        game.dropCrop("carrot", new Cell(rabbit.col() - 1, rabbit.row()));
        game.dropCrop("carrot", new Cell(rabbit.col() + 1, rabbit.row()));
        game.seconds(1);
        game.click(new Cell(rabbit.col() - 1, rabbit.row()));
        game.seconds(1.5f);
        assertTrue(game.quest().contains("slot=Carrot"), game.quest());
        assertTrue(game.describeHud().contains("backpack: [-, -, -]"), "not in the backpack: " + game.describeHud());
        game.click(new Cell(rabbit.col() + 1, rabbit.row()));
        game.seconds(2);
        assertTrue(game.describeHud().contains("backpack: [Carrot, -, -]"), "the second one: " + game.describeHud());

        game.click(rabbit);
        game.seconds(2);
        assertEquals(List.of(1f), game.buttonAlphas("DELIVER"), "next to the cow: the button with the carrot");
        assertEquals(List.of(), game.buttonAlphas("GIVE"), "its favourite is the same: only the quest's button");
        game.clickButton("DELIVER");
        game.frames(2);
        assertTrue(List.of("EATING", "GETTING_UP").contains(game.cowState()), "the cow eats it: " + game.cowState());
        assertEquals("none", game.quest(), "done");
        assertTrue(game.describeHud().contains("backpack: [Carrot, -, -]"),
                "given from the slot: " + game.describeHud());
        assertTrue(game.questPanelShown(), "the row stays while the carrot flies to the cow");
        assertEquals(List.of(), game.itemsOnGround(), "no milk before the carrot has landed");
        boolean hearts = false;
        for (int frame = 0; frame < 15 * 60 && game.itemsOnGround().isEmpty(); frame++) {
            game.frames(1);
            hearts |= "HAPPY".equals(game.cowState());
        }
        assertTrue(hearts, "the cow showed its hearts before the milk came");
        assertFalse(List.of("EATING", "HAPPY").contains(game.cowState()), "the milk comes after: " + game.cowState());
        game.seconds(1.5f);
        assertTrue(game.itemsOnGround().stream().anyMatch(item -> item.startsWith("Milk cell=")),
                "then its milk landed next to it: " + game.itemsOnGround());
        assertFalse(game.questPanelShown(), "the row over the backpack is gone");
        assertFalse(game.crashed());
    }

    /**
     * A carrot in the backpack goes into the quest's slot a second after the quest starts (it flies over);
     * given to the cow from there, the quest is done.
     */
    @Test
    void aCarriedOneGoesIntoTheSlot() {
        GameDriver game = GameDriver.start(MEADOW.write("quest-meadow"));
        game.putCarrotIntoBackpack();
        game.seconds(3);
        game.clickButton("QUEST");
        game.frames(2);
        assertTrue(game.questPanelShown(), "first the row with the empty slot...");
        assertTrue(game.quest().contains("slot=-"), game.quest());
        assertTrue(game.describeHud().contains("backpack: [Carrot, -, -]"), game.describeHud());
        game.seconds(0.9f);
        assertTrue(game.quest().contains("slot=-"), "...for a second: " + game.quest());
        game.seconds(0.2f);
        assertTrue(game.quest().contains("slot=Carrot"), "then moved into the slot: " + game.quest());
        assertTrue(game.describeHud().contains("backpack: [-, -, -]"), game.describeHud());
        assertEquals(List.of("carrot"), game.flyingItems(), "it flies from the backpack into the quest's slot");
        game.seconds(2);
        assertEquals(List.of(), game.flyingItems(), "landed");
        assertEquals(List.of(1f), game.buttonAlphas("DELIVER"));
        game.clickButton("DELIVER");
        game.frames(2);
        assertEquals("none", game.quest());
        assertEquals(List.of("carrot"), game.flyingItems(), "it flies from the quest's slot to the cow");
        assertFalse(game.crashed());
    }

    /** A tap on the quest row stays in the HUD: the player doesn't walk to the tile under it. */
    @Test
    void theRowCatchesTaps() {
        GameDriver game = startedQuest();
        Cell rabbit = game.playerCell();
        game.tapQuestRow();
        game.seconds(2);
        assertEquals(rabbit, game.playerCell(), "the rabbit stayed");
        assertFalse("none".equals(game.quest()), "the quest goes on");
        assertFalse(game.crashed());
    }

    /** Cancelled with the X: what is in the slot jumps out next to the rabbit, and the quest row is gone. */
    @Test
    void cancellingThrowsOutWhatIsInTheSlot() {
        GameDriver game = startedQuest();
        putIntoTheSlot(game);
        game.tapCancelQuest();
        game.frames(2);
        assertEquals("none", game.quest());
        game.seconds(1);
        assertFalse(game.questPanelShown());
        assertTrue(game.itemsOnGround().stream().anyMatch(item -> item.startsWith("Carrot cell=")),
                "the carrot jumped out: " + game.itemsOnGround());
        assertFalse(game.crashed());
    }

    /**
     * The line under the quest shrinks with its time: green, yellow from 30 % left, red from 10 %; out of time, the
     * quest ends and what is in the slot jumps out.
     */
    @Test
    void outOfTimeTheQuestEnds() {
        GameDriver game = startedQuest();
        putIntoTheSlot(game);
        Set<String> colours = new HashSet<>();
        for (int frame = 0; frame < 40 * 60 && !"none".equals(game.quest()); frame++) {
            String quest = game.quest();
            colours.add(quest.substring(quest.lastIndexOf(' ') + 1));
            game.frames(1);
        }
        assertEquals("none", game.quest(), "it ran out (30 s)");
        assertEquals(Set.of("72c65bff", "f2cc4cff", "e55144ff"), colours, "green, yellow, red");
        game.seconds(1);
        assertTrue(game.itemsOnGround().stream().anyMatch(item -> item.startsWith("Carrot cell=")),
                "the carrot jumped out: " + game.itemsOnGround());
        assertFalse(game.crashed());
    }

    /**
     * One quest at a time: while the cow's goes on, the chicken offers its own, but its button shakes. Tapping the
     * quest's slot throws its carrot out; picked up again it goes back into the slot.
     */
    @Test
    void oneQuestAtATime() {
        GameDriver game = GameDriver.start(TWO.write("quest-two"));
        game.seconds(3);
        Kind first = game.buttonOver("QUEST");
        Kind other = first == Kind.COW ? Kind.CHICKEN : Kind.COW;
        game.clickButton("QUEST");
        game.frames(2);
        assertFalse("none".equals(game.quest()));
        for (int frame = 0; frame < 3 * 60 && !(other == game.buttonOver("QUEST")); frame++) {
            game.frames(1);
        }
        if (other == game.buttonOver("QUEST")) { // the other one offers too (the chicken's quest is pushy)
            String going = game.quest();
            game.clickButton("QUEST");
            game.frames(2);
            assertTrue(game.actionButtonShaking("QUEST"), "another quest goes on: it shakes");
            assertEquals(going, game.quest(), "the same quest goes on");
        }
        assertFalse(game.crashed());
    }

    /** Tapping the quest's slot throws what is in it out; picked up again, it goes back into the slot. */
    @Test
    void tappingTheSlotThrowsItsThingOut() {
        GameDriver game = startedQuest();
        putIntoTheSlot(game);
        Cell rabbit = game.playerCell();
        game.tapQuestSlot();
        game.seconds(1);
        assertTrue(game.quest().contains("slot=-"), game.quest());
        Cell carrot = game.itemCell();
        assertTrue(game.neighbours(rabbit).contains(carrot), "next to the rabbit");
        game.click(carrot);
        game.seconds(2);
        assertTrue(game.quest().contains("slot=Carrot"), "back in the slot: " + game.quest());
        assertFalse(game.crashed());
    }

    /** With two quests the animal offers them in turn: done with the first, it offers the second (found by a play-tester). */
    @Test
    void theSecondQuestComesAfterTheFirst() {
        GameDriver game = GameDriver.start(TWO_QUESTS.write("quest-two-quests"));
        game.putCarrotIntoBackpack();
        game.seconds(3);
        assertEquals("testCarrotForMilk", game.questOffered(Kind.COW));
        game.clickButton("QUEST");
        game.seconds(2);
        game.clickButton("DELIVER");
        game.frames(2);
        assertEquals("none", game.quest(), "the first one done");
        String offered = "none";
        for (int frame = 0; frame < 5 * 60 && "none".equals(offered); frame++) {
            game.frames(1);
            offered = game.questOffered(Kind.COW);
        }
        assertEquals("testPushyCornForEgg", offered, "then the second");
        assertFalse(game.crashed());
    }

    /** On level 1 the cow asks for a pumpkin for milk (giveMilk) a while into the day. */
    @Test
    void onLevelOneTheCowOffersMilk() {
        GameDriver game = GameDriver.start(GameDriver.defaultMap());
        String offered = "none";
        for (int frame = 0; frame < 40 * 60 && "none".equals(offered); frame++) {
            game.frames(1);
            offered = game.questOffered(Kind.COW);
        }
        assertEquals("giveMilk", offered);
        assertFalse(game.crashed());
    }

    /** The rabbit next to the cow, its quest started. */
    private static GameDriver startedQuest() {
        GameDriver game = GameDriver.start(MEADOW.write("quest-meadow"));
        game.seconds(3);
        game.clickButton("QUEST");
        game.frames(2);
        assertFalse("none".equals(game.quest()), "started");
        return game;
    }

    /** A carrot lands next to the rabbit, which steps onto it (into the quest's slot) and back. */
    private static void putIntoTheSlot(GameDriver game) {
        Cell rabbit = game.playerCell();
        Cell beside = new Cell(rabbit.col() - 1, rabbit.row());
        game.dropCrop("carrot", beside);
        game.seconds(1);
        game.click(beside);
        game.seconds(1.5f);
        game.click(rabbit);
        game.seconds(1.5f);
        assertTrue(game.quest().contains("slot=Carrot"), game.quest());
    }
}
