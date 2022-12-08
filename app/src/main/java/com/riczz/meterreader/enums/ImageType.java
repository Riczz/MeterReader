package com.riczz.meterreader.enums;

import java.util.Locale;

public enum ImageType {
    FRAME_DETECTION("Frame detection"),
    DIAL_SEARCH("Dial search"),
    SKEWNESS_CORRECTION("Skewness correction"),
    COLOR_CORRECTION("Color correction"),
    DIGIT_DETECTION("Digit detection");

    private final String name;

    ImageType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getFolderName() {
        return name.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }
}
