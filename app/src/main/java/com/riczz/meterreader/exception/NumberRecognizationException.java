package com.riczz.meterreader.exception;

public final class NumberRecognizationException extends BaseException {
    public NumberRecognizationException(int errorCode) {
        super(errorCode);
    }

    public NumberRecognizationException(String message, int errorCode) {
        super(message, errorCode);
    }
}
