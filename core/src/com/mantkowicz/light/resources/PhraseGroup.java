package com.mantkowicz.light.resources;

import java.util.Arrays;
import java.util.List;

public enum PhraseGroup {
    DARKNESS_EXCLAMATION("dude I'm scared of darkness!", "please take me away from here...", "did you hear that...?",
            "I feel like I'm going crazy"),
    NO_PATH("no path");

    private final String[] values;

    PhraseGroup(String... values) {
        this.values = values;
    }

    public List<String> getValues() {
        return Arrays.asList(values);
    }
}
