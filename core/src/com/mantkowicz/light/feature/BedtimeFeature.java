package com.mantkowicz.light.feature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.actor.GameActorType;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileContent;
import com.mantkowicz.light.character.FarmAnimal;
import com.mantkowicz.light.character.Roost;
import com.mantkowicz.light.character.Sheltered;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.light.DayStartedEvent;
import com.mantkowicz.light.light.NightStartedEvent;
import com.mantkowicz.light.map.TileDoor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The farm animals of the map sleep at night: when the night starts ({@link NightStartedEvent}) they go to sleep
 * (once done with what they are doing), when the day starts ({@link DayStartedEvent}) they wake up. Also an animal that
 * comes onto the map at night (out of a door) is told it is night. Each animal is told its kind's {@link Roost} on the
 * map: on the farm the door it sleeps behind (Tiled's door property {@code bedtime}: CHICKEN on the coop's, COW on the
 * barn's), in the shelter its entrance.
 */
public class BedtimeFeature implements Feature {
    private final GameEventService events;
    private final Stage gameStage;
    /** Where each kind sleeps on this map: chickens (behind the coop's door), the cow (behind the barn's). */
    private final Map<GameActorType, Roost> roosts = new HashMap<>();
    private boolean night;

    public BedtimeFeature(GameContext context) {
        events = context.getGameEventService();
        gameStage = context.getGameStage();
        for (GameActorType kind : new GameActorType[]{GameActorType.CHICKEN, GameActorType.COW}) {
            roosts.put(kind, roostOf(context, kind));
        }
    }

    /** The map's door this kind sleeps behind, if any, and the doors it may go through. */
    private static Roost roostOf(GameContext context, GameActorType kind) {
        Tile bedtimeDoor = null;
        List<Tile> doors = new ArrayList<>();
        for (Tile tile : context.getBoardService().getTiles()) {
            for (TileContent content : tile.getTileObjects()) {
                if (content instanceof TileDoor door && door.lets(kind)) {
                    doors.add(tile);
                    if (door.isBedtimeFor(kind)) {
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
                Roost roost = roosts.get(animal.getGameActorType());
                if (roost != null && animal instanceof Sheltered sheltered) {
                    sheltered.setRoost(roost);
                }
            }
        }
    }
}
