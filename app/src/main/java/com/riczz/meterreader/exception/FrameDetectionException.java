package com.riczz.meterreader.exception;

public final class FrameDetectionException extends BaseException {
    public FrameDetectionException(int errorCode) {
        super(errorCode);
    }

    public FrameDetectionException(String message, int errorCode) {
        super(message, errorCode);
    }
}
