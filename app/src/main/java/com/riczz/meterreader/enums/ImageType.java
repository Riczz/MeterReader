package com.riczz.meterreader.enums;

import android.content.Context;

import com.riczz.meterreader.R;

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

    public static String getCategoryName(Context context, ImageType imageType) {
        return getCategoryName(context, MeterType.GAS, imageType);
    }

    public static String getCategoryName(Context context, MeterType meterType, ImageType imageType) {
        switch (imageType) {
            case FRAME_DETECTION:
                if (meterType == MeterType.GAS) {
                    return context.getString(R.string.category_frame_detection_gas);
                } else if (meterType == MeterType.ELECTRIC) {
                    return context.getString(R.string.category_frame_detection_electric);
                }
            case DIAL_SEARCH:
                return context.getString(R.string.category_dial_search);
            case SKEWNESS_CORRECTION:
                return context.getString(R.string.category_skewness_correction);
            case COLOR_CORRECTION:
                return context.getString(R.string.category_color_correction);
            case DIGIT_DETECTION:
                return context.getString(R.string.category_digit_detection);
            default:
                return "";
        }
    }
}
