package com.riczz.meterreader.enums;

public enum MeterType {
    GAS(8, 3),
    ELECTRIC(7, 1);

    private final int numberOfDigits;
    private final int fractionalDigits;

    MeterType(int numberOfDigits, int fractionalDigits) {
        this.numberOfDigits = numberOfDigits;
        this.fractionalDigits = fractionalDigits;
    }

    public int getNumberOfDigits() {
        return numberOfDigits;
    }

    public int getFractionalDigits() {
        return fractionalDigits;
    }
}
