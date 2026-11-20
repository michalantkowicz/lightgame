package com.mantkowicz.light.e2e.scenario;

import com.mantkowicz.light.e2e.Cell;
import com.mantkowicz.light.e2e.GameDriver;
import com.mantkowicz.light.e2e.GameDriver.Kind;
import com.mantkowicz.light.e2e.TestMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Day and night (light.DayClock, DayNightSettings): the same time on every map, from the morning; a map with daylight
 * (Tiled's ambientLight) dims at dusk to the night's light and brightens at dawn, one without stays as it is. The night
 * starts at dusk, the day at dawn: the farm animals go to sleep ("zzz" over them, no button over them: nothing wakes
 * them) and get up in the morning, on every map. Chickens sleep in the coop: they walk in through the farm's door to it
 * (Tiled's door property bedtime) and sleep at least two tiles from its entrance, and go back out in the morning.
 */
class DayNightTest {
    /** The rabbit next to a cow (and a chicken) that can't walk anywhere; by daylight. */
    private static final TestMap PEN = TestMap.of(
            "~~~~~~~",
            "~~M~H~~",
            "~~P.~~~",
            "~~~~~~~")
            .with('H', "CHICKEN", Map.of())
            .withMapProperty("ambientLight", "1");

    /**
     * Short days for the tests: 3 s of day, 1 s of dusk, 6 s of night, 1 s of dawn; the night a quarter as bright. (A
     * game starts half a second in: {@code GameDriver.start} lets it settle.)
     */
    private static void shortDays() {
        GameDriver.dayAndNight(3, 1, 6, 1, 0.25f);
    }

    @AfterEach
    void dayAndNightAsInTests() {
        GameDriver.dayAndNightAsInTests();
    }

    @Test
    void theLightFollowsTheDay() {
        shortDays();
        GameDriver game = GameDriver.start(PEN.write("day-night-pen"));
        assertEquals("DAY", game.timeOfDay(), "the game starts in the morning");
        assertEquals("0.00,0.00,0.00,1.00", game.ambientLight(), "full daylight");
        game.seconds(3); // 3.5 s in
        assertEquals("DUSK", game.timeOfDay());
        float dusk = alpha(game.ambientLight());
        assertTrue(dusk < 1 && dusk > 0.25f, "dimming: " + dusk);
        game.seconds(1); // 4.5 s
        assertEquals("NIGHT", game.timeOfDay());
        assertEquals("0.00,0.00,0.00,0.25", game.ambientLight(), "a quarter of the map's daylight");
        game.seconds(6); // 10.5 s
        assertEquals("DAWN", game.timeOfDay());
        float dawn = alpha(game.ambientLight());
        assertTrue(dawn < 1 && dawn > 0.25f, "brightening: " + dawn);
        game.seconds(1);
        assertEquals("DAY", game.timeOfDay(), "and the next day");
        assertEquals("0.00,0.00,0.00,1.00", game.ambientLight());
        assertFalse(game.crashed());
    }

    /** The house has no daylight (no ambientLight): lit by its lamps, it stays as it is at night. */
    /**
     * The day clock at the top left shows the part of the day (its picture) and an arrow over the day's scale (0 the
     * sun, nearly straight up .. 6 the moon, nearly straight down). It only goes down, clockwise: when the sun starts to rise it jumps
     * to nearly straight up, then it goes down a step every fifth of the time until sunset; at sunset (dusk) one more step,
     * and at night nearly straight down at the moon until sunrise.
     */
    @Test
    void theDayClockFollowsTheDay() {
        GameDriver.dayAndNight(9, 1, 2, 1, 0.25f); // from sunrise to sunset 10 s: a step every 2 s
        GameDriver game = GameDriver.start();
        List<String> shown = new ArrayList<>();
        for (int frame = 0; frame < 13 * 60; frame++) { // a whole day (it starts half a second in)
            String now = game.dayClockShows();
            if (shown.isEmpty() || !shown.get(shown.size() - 1).equals(now)) {
                shown.add(now);
            }
            game.frames(1);
        }
        assertEquals(List.of("DAY 0", "DAY 1", "DAY 2", "DAY 3", "DAY 4", "DUSK 5", "NIGHT 6", "DAWN 0", "DAY 0"),
                shown);
        assertTrue(game.describeHud().contains("day clock: DAY arrow=0 at=(24.0,1124.0) size=220.8x120.0"),
                "at the left edge, its middle level with the gear's (y 1184): " + game.describeHud());
        assertFalse(game.crashed());
    }

    @Test
    void aMapWithoutDaylightStaysAsItIs() {
        shortDays();
        GameDriver game = GameDriver.start(TestMap.of("~~~", "~P~", "~~~").write("day-night-house"));
        String light = game.ambientLight();
        game.seconds(5);
        assertEquals("NIGHT", game.timeOfDay());
        assertEquals(light, game.ambientLight());
    }

    @Test
    void theAnimalsSleepTheNight() {
        shortDays();
        GameDriver game = GameDriver.start(PEN.write("day-night-pen"));
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "by day: the hand over the cow");
        assertFalse(game.zzzOver(Kind.COW));

        game.seconds(4); // 4.5 s in: the night has started
        assertEquals("SLEEPING_THE_NIGHT", game.animalState(Kind.COW), "the cow lay down and fell asleep");
        assertEquals("SLEEPING_THE_NIGHT", game.animalState(Kind.CHICKEN), "the chicken too");
        assertEquals("cow_brown/sleep", game.animalAnimation(Kind.COW));
        assertTrue(game.zzzOver(Kind.COW), "zzz over the cow");
        assertTrue(game.zzzOver(Kind.CHICKEN), "and over the chicken");
        game.frames(30); // the button fades out (5 s)
        assertEquals(List.of(), game.buttonAlphas("STROKE"), "no hand: nothing wakes them");
        assertNull(game.buttonOver("STROKE"));

        game.seconds(4.5f); // 9.5 s: the whole night (it ends 10 s in)
        assertEquals("NIGHT", game.timeOfDay());
        assertEquals("SLEEPING_THE_NIGHT", game.animalState(Kind.COW), "asleep the night through");
        assertEquals(List.of(), game.buttonAlphas("STROKE"));

        game.seconds(1.2f); // 10.7 s: the sun has started to rise
        assertEquals("DAWN", game.timeOfDay());
        assertFalse(game.zzzOver(Kind.COW), "awake: no zzz");
        assertFalse(game.zzzOver(Kind.CHICKEN));
        assertFalse(game.animalState(Kind.COW).contains("SLEEPING"), "the cow gets up");
        assertFalse(game.animalState(Kind.CHICKEN).contains("SLEEPING"), "the chicken too");
        game.seconds(2);
        assertEquals(List.of(1f), game.buttonAlphas("STROKE"), "by day the hand is back");
        assertFalse(game.crashed());
    }

    /** A cow wishing for a crop when the night comes goes to sleep (no bubble, no button) and wishes again in the morning. */
    @Test
    void aWishingCowGoesToSleepAndWishesAgainInTheMorning() {
        GameDriver.dayAndNight(62, 1, 6, 1, 0.25f); // the cow gets hungry 60 s in
        GameDriver game = GameDriver.start(PEN.write("day-night-pen"));
        game.seconds(61);
        assertEquals("WISHING", game.animalState(Kind.COW));
        game.seconds(2.5f);
        assertEquals("SLEEPING_THE_NIGHT", game.animalState(Kind.COW), "hungry or not, it sleeps");
        assertEquals("none", game.cowWish(), "no bubble");
        game.seconds(6);
        assertEquals("WISHING", game.animalState(Kind.COW), "in the morning it wishes again");
        assertFalse(game.crashed());
    }

    /** The night comes on every map: the chicken in the coop sleeps too, while the rabbit is on the farm. */
    @Test
    void animalsOnOtherMapsSleepToo() {
        shortDays();
        GameDriver game = GameDriver.start(GameDriver.defaultMap());
        String coop = game.maps().stream().filter(map -> map.endsWith("coop.tmx")).findFirst().orElseThrow();
        game.seconds(4.5f); // 5 s in
        assertEquals("SLEEPING_THE_NIGHT", game.animalStateOn(coop, Kind.CHICKEN));
        game.seconds(6.5f); // 11.5 s
        assertFalse(game.animalStateOn(coop, Kind.CHICKEN).contains("SLEEPING"), "and wakes up at dawn");
    }

    /** The farm: a chicken, the rabbit, the door to the coop (chickens sleep behind it: Tiled's bedtime). */
    private static TestMap farm(String coop, boolean withChicken) {
        return TestMap.of(
                "~~~~~~~~",
                "~P....O~",
                "~..." + (withChicken ? "H" : ".") + "..~",
                "~~~~~~~~")
                .with('H', "CHICKEN", Map.of())
                .with('O', "DOOR", Map.of("door", "coop", "targetMap", TestMap.pathOf(coop), "passes",
                        "PLAYER,CHICKEN", "bedtime", "CHICKEN"))
                .withMapProperty("ambientLight", "1");
    }

    /** The coop: its door to the farm (top left), room to sleep; its own chicken, or none. */
    private static TestMap coop(String farm, boolean withChicken) {
        return TestMap.of(
                "~~~~~~~",
                "~O....~",
                "~.....~",
                "~.." + (withChicken ? "H" : ".") + "..~",
                "~~~~~~~")
                .with('H', "CHICKEN", Map.of())
                .with('O', "DOOR", Map.of("door", "coop", "targetMap", TestMap.pathOf(farm), "passes",
                        "PLAYER,CHICKEN"));
    }

    /** Where the coop's door is: (1,3), counted from the bottom. */
    private static final Cell COOP_DOOR = new Cell(1, 3);

    /** Where the farm's door to the coop is: (6,2), counted from the bottom. */
    private static final Cell FARM_DOOR = new Cell(6, 2);

    /**
     * At night the farm's chicken walks to the coop's door and goes in; inside, it sleeps on a free tile at least two
     * tiles from the entrance. In the morning it goes back out to the farm, and on to a free tile at least two tiles
     * from the door.
     */
    @Test
    void theChickenSleepsInTheCoop() {
        GameDriver.dayAndNight(3, 1, 20, 10, 0.25f); // a long dawn: time to come out and walk away from the door
        String coop = coop("bedtime-farm", false).write("bedtime-coop");
        String farm = farm("bedtime-coop", true).write("bedtime-farm");
        GameDriver game = GameDriver.start(farm);
        assertEquals(farm, game.whereIs(Kind.CHICKEN), "by day on the farm");

        game.seconds(14); // 14.5 s: the night started 3 s in
        assertEquals(coop, game.whereIs(Kind.CHICKEN), "it went into the coop");
        assertEquals("SLEEPING_THE_NIGHT", game.animalStateOn(coop, Kind.CHICKEN));
        assertTrue(apart(game.cellOn(coop, Kind.CHICKEN), COOP_DOOR) >= 2
                && apart(game.cellOn(coop, Kind.CHICKEN), COOP_DOOR) <= 4,
                "away from the entrance, not too far: " + game.cellOn(coop, Kind.CHICKEN));
        assertTrue(game.sleepsAwayOn(coop));

        game.seconds(20); // 34.5 s: the day started 24 s in (the next night 38 s in)
        assertEquals(farm, game.whereIs(Kind.CHICKEN), "back out on the farm in the morning");
        assertFalse(game.animalStateOn(farm, Kind.CHICKEN).contains("SLEEPING"));
        assertFalse(game.sleepsAwayOn(farm));
        assertTrue(apart(game.cellOn(farm, Kind.CHICKEN), FARM_DOOR) >= 2
                && apart(game.cellOn(farm, Kind.CHICKEN), FARM_DOOR) <= 4,
                "away from the door, not too far: " + game.cellOn(farm, Kind.CHICKEN));
        assertFalse(game.animalStateOn(farm, Kind.CHICKEN).equals("LEAVING_THE_COOP"), "there, it goes on");
        assertFalse(game.crashed());
    }

    /** A wide farm: the cow, the rabbit, the barn's door (the cow sleeps behind it: Tiled's bedtime COW). */
    private static TestMap cowFarm(String barn) {
        return TestMap.of(
                "~~~~~~~~~~~~~~",
                "~P..........B~",
                "~.......N....~",
                "~............~",
                "~~~~~~~~~~~~~~")
                .with('N', "COW", Map.of())
                .with('B', "DOOR", Map.of("door", "barn", "targetMap", TestMap.pathOf(barn), "passes", "PLAYER,COW",
                        "bedtime", "COW"))
                .withMapProperty("ambientLight", "1");
    }

    /** A wide barn: its door (top left) and room far beyond 4 tiles from it. */
    private static TestMap barn(String farm) {
        return TestMap.of(
                "~~~~~~~~~~~~~~",
                "~B...........~",
                "~............~",
                "~............~",
                "~~~~~~~~~~~~~~")
                .with('B', "DOOR", Map.of("door", "barn", "targetMap", TestMap.pathOf(farm), "passes", "PLAYER,COW"));
    }

    /**
     * The cow does as the chickens: at night it walks to the barn's door and goes in; inside, it sleeps on a free tile 2
     * to 5 tiles from the entrance. In the morning it goes back out, and on to a free tile 2 to 4 tiles from the door.
     */
    @Test
    void theCowSleepsInTheBarn() {
        GameDriver.dayAndNight(3, 1, 40, 25, 0.25f); // the cow is slow: a long night and dawn
        String barn = barn("barn-farm").write("barn-barn");
        String farm = cowFarm("barn-barn").write("barn-farm");
        GameDriver game = GameDriver.start(farm);
        Cell farmDoor = new Cell(12, 3), barnDoor = new Cell(1, 3);
        assertEquals(farm, game.whereIs(Kind.COW), "by day on the farm");

        game.seconds(40); // 40.5 s: the night started 3 s in, it ends 44 s in
        assertEquals(barn, game.whereIs(Kind.COW), "it went into the barn");
        assertEquals("SLEEPING_THE_NIGHT", game.animalStateOn(barn, Kind.COW));
        int inside = apart(game.cellOn(barn, Kind.COW), barnDoor);
        assertTrue(inside >= 2 && inside <= 4, "2 to 4 tiles from the entrance: " + game.cellOn(barn, Kind.COW));
        assertTrue(game.sleepsAwayOn(barn, Kind.COW));

        game.seconds(30); // 70.5 s: the day started 44 s in (the next night 94 s in)
        assertEquals(farm, game.whereIs(Kind.COW), "back out on the farm in the morning");
        assertFalse(game.sleepsAwayOn(farm, Kind.COW));
        int outside = apart(game.cellOn(farm, Kind.COW), farmDoor);
        assertTrue(outside >= 2 && outside <= 4, "2 to 4 tiles from the door: " + game.cellOn(farm, Kind.COW));
        assertFalse(game.animalStateOn(farm, Kind.COW).equals("LEAVING_THE_BARN"), "there, it goes on");
        assertFalse(game.crashed());
    }

    /** The coop's own chicken sleeps in the coop, where it is, and stays there in the morning. */
    @Test
    void theCoopsOwnChickenStaysIn() {
        GameDriver.dayAndNight(3, 1, 8, 1, 0.25f);
        String coop = coop("own-farm", true).write("own-coop");
        String farm = farm("own-coop", false).write("own-farm");
        GameDriver game = GameDriver.start(farm);
        game.seconds(6); // 6.5 s: the night started 3 s in
        assertEquals("SLEEPING_THE_NIGHT", game.animalStateOn(coop, Kind.CHICKEN));
        game.seconds(7.5f); // 14 s: the sun started to rise 12 s in (the next night only 16 s in)
        assertEquals(coop, game.whereIs(Kind.CHICKEN), "it lives there");
        assertFalse(game.animalStateOn(coop, Kind.CHICKEN).contains("SLEEPING"));
    }

    private static int apart(Cell a, Cell b) {
        return Math.max(Math.abs(a.col() - b.col()), Math.abs(a.row() - b.row()));
    }

    private static float alpha(String ambientLight) {
        return Float.parseFloat(ambientLight.substring(ambientLight.lastIndexOf(',') + 1));
    }
}
