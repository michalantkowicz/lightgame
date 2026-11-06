package com.mantkowicz.light.resources;

import com.badlogic.gdx.math.RandomXS128;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhraseService {
    private final Map<PhraseGroup, List<String>> phrases;
    /** Own generator, so picking a phrase doesn't shift the shared MathUtils sequence (torch flicker etc.). */
    private final RandomXS128 random = new RandomXS128();

    public PhraseService() {
        this.phrases = new HashMap<>();
        for (PhraseGroup phraseGroup : PhraseGroup.values()) {
            phrases.put(phraseGroup, phraseGroup.getValues());
        }
    }

    public String getRandomPhrase(PhraseGroup phraseGroup) {
        List<String> phrasesOfGroup = phrases.get(phraseGroup);
        return phrasesOfGroup.get(random.nextInt(phrasesOfGroup.size()));
    }

    public String getPhrase(PhraseGroup phraseGroup) {
        return phrases.get(phraseGroup).get(0);
    }
}
