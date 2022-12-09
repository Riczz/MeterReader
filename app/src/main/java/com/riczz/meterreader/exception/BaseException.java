package com.riczz.meterreader.exception;

import android.content.Context;

import com.riczz.meterreader.R;

public class BaseException extends Exception {
    private final int errorCode;

    public BaseException() {
        super("");
        this.errorCode = -1;
    }

    public BaseException(int errorCode) {
        super("");
        this.errorCode = errorCode;
    }

    public BaseException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public static String getDetails(Context context, int errorCode) {
        switch (errorCode) {
            case 101:
                return context.getString(R.string.error_code_101);
            case 103:
                return context.getString(R.string.error_code_103);
            case 104:
                return context.getString(R.string.error_code_104);
            case 105:
                return context.getString(R.string.error_code_105);
            case 106:
                return context.getString(R.string.error_code_106);
            case 201:
                return context.getString(R.string.error_code_201);
            case 202:
                return context.getString(R.string.error_code_202);
            default:
                return context.getString(R.string.error_code_notfound);
        }
    }
}
