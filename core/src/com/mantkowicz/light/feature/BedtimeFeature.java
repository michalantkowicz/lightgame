package com.mantkowicz.light.feature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileContent;
import com.mantkowicz.light.character.Chicken;
import com.mantkowicz.light.character.FarmAnimal;
import com.mantkowicz.light.character.Roost;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.light.DayStartedEvent;
import com.mantkowicz.light.light.NightStartedEvent;
import com.mantkowicz.light.map.TileDoor;

import java.util.ArrayList;
import java.util.List;

/**
 * The farm animals of the map sleep at night: when the night starts ({@link NightStartedEvent}) they go to sleep
 * (once done with what they are doing), when the day starts ({@link DayStartedEvent}) they wake up. Also an animal that
 * comes onto the map at night (out of a door) is told it is night. The chickens are told the map's {@link Roost}: on
 * the farm the door to the coop (Tiled's door property {@code bedtime}), in the coop its entrance.
 */
public class BedtimeFeature implements Feature {
    private final GameEventService events;
    private final Stage gameStage;
    private final Roost roost;
    private boolean night;

    public BedtimeFeature(GameContext context) {
        events = context.getGameEventService();
        gameStage = context.getGameStage();
        roost = roostOf(context);
    }

    /** The map's door the chickens sleep behind, if any, and the doors they may go through. */
    private static Roost roostOf(GameContext context) {
        Tile bedtimeDoor = null;
        List<Tile> doors = new ArrayList<>();
        for (Tile tile : context.getBoardService().getTiles()) {
            for (TileContent content : tile.getTileObjects()) {
                if (content instanceof TileDoor door && door.lets(GameActorType.CHICKEN)) {
                    doors.add(tile);
                    if (door.isBedtimeFor(GameActorType.CHICKEN)) {
                        bedtimeDoor = tile;
                    }
                }
            }
        }
        return new Roost(bedtimeDoor, doors);
    }

    @Override
    public void run(float delta) {
        if (events.poll(NightStartedEvent.class) != null) {
            night = true;
        }
        if (events.poll(DayStartedEvent.class) != null) {
            night = false;
        }
        for (Actor actor : gameStage.getActors()) {
            if (actor instanceof FarmAnimal animal) {
                animal.setNight(night);
            }
            if (actor instanceof Chicken chicken) {
                chicken.setRoost(roost);
            }
        }
    }
}
