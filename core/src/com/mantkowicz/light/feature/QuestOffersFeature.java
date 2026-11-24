package com.mantkowicz.light.feature;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.character.FarmAnimal;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.quest.CreatureQuests;
import com.mantkowicz.light.quest.QuestJournal;

/**
 * The animals of the map ask their quests now and then whether one should start ({@link CreatureQuests#update}): on
 * every map, also while the player is elsewhere (as the animals live on there).
 */
public class QuestOffersFeature implements Feature {
    private final Stage gameStage;
    private final QuestJournal quests;

    public QuestOffersFeature(GameContext context, QuestJournal quests) {
        this.gameStage = context.getGameStage();
        this.quests = quests;
    }

    @Override
    public void run(float delta) {
        for (Actor actor : gameStage.getActors()) {
            if (actor instanceof FarmAnimal animal) {
                animal.getQuests().update(delta, quests);
            }
        }
    }
}
