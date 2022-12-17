package com.riczz.meterreader.enums;

import android.content.Context;

import com.riczz.meterreader.R;

public enum MeterType {
    GAS(8, 3),
    ELECTRIC(7, 1);

    private final int numberOfDigits;
    private final int fractionalDigits;
    private final int wholeDigits;

    MeterType(int numberOfDigits, int fractionalDigits) {
        this.numberOfDigits = numberOfDigits;
        this.fractionalDigits = fractionalDigits;
        this.wholeDigits = numberOfDigits - fractionalDigits;
    }

    public int getNumberOfDigits() {
        return numberOfDigits;
    }

    public int getFractionalDigits() {
        return fractionalDigits;
    }

    public int getWholeDigits() {
        return wholeDigits;
    }

    public static String getMeterCategoryName(Context context, MeterType meterType) {
        if (meterType == MeterType.GAS) {
            return context.getString(R.string.meter_type_gas);
        } else if (meterType == MeterType.ELECTRIC) {
            return context.getString(R.string.meter_type_electric);
        }
        return null;
    }
}
