package com.riczz.meterreader.config;

import android.provider.BaseColumns;

import java.util.Locale;

public final class ConfigContract {

    private ConfigContract() {
    }

    public static final class Entry implements BaseColumns {
        public static final String
                IMGPROC_TABLE_NAME = "imgproc_config",
                COLUMN_NAME_BLUR_SIGMA_X = "blur_sigma_x",
                COLUMN_NAME_DEFAULT_KERNEL_SIZE = "default_kernel_size",
                COLUMN_NAME_DEFAULT_THRESHOLD_VALUE = "default_threshold_value",
                COLUMN_NAME_MORPHOLOGY_KERNEL_TYPE = "morphology_kernel_type",
                COLUMN_NAME_DIAL_FRAME_WIDTH_MULTIPLIER = "dial_frame_width_multiplier",
                COLUMN_NAME_MAX_CIRCULARITY = "max_circularity",
                COLUMN_NAME_MIN_DIAL_AREA = "min_dial_area";
    }

    public static final String SQL_INSERT_IMGPROC_ROW = String
            .format(Locale.ROOT, "INSERT INTO %s VALUES ('%s','%s','%s','%s','%s','%s','%s');",
                    Entry.IMGPROC_TABLE_NAME,
                    Config.ImgProc.blurSigmaX, Config.ImgProc.defaultKernelSize,
                    Config.ImgProc.defaultThresholdValue, Config.ImgProc.morphologyKernelType,
                    Config.ImgProc.dialFrameWidthMultiplier, Config.ImgProc.maxCircularity,
                    Config.ImgProc.minDialArea);

    public static final String SQL_CREATE_DEFAULT_IMGPROC_ROW =
            "INSERT INTO %s VALUES ()";

    public static final String SQL_CREATE_IMGPROC_TABLE = String
            .format(Locale.ROOT,
                    "CREATE TABLE IF NOT EXISTS %s (%s REAL NOT NULL DEFAULT 5.0, " +
                            "%s TINYINT(2) UNSIGNED NOT NULL DEFAULT 5 " +
                            "%s INTEGER UNSIGNED NOT NULL DEFAULT 50, %s INTEGER UNSIGNED NOT NULL DEFAULT 0, " +
                            "%s REAL NOT NULL DEFAULT 4.0 CHECK(%s >= 1.0), %s REAL NOT NULL DEFAULT 0.6, " +
                            "%s REAL NOT NULL DEFAULT 1000.0);",
                    Entry.IMGPROC_TABLE_NAME, Entry.COLUMN_NAME_BLUR_SIGMA_X,
                    Entry.COLUMN_NAME_DEFAULT_KERNEL_SIZE, Entry.COLUMN_NAME_DEFAULT_THRESHOLD_VALUE,
                    Entry.COLUMN_NAME_MORPHOLOGY_KERNEL_TYPE, Entry.COLUMN_NAME_DIAL_FRAME_WIDTH_MULTIPLIER,
                    Entry.COLUMN_NAME_DIAL_FRAME_WIDTH_MULTIPLIER, Entry.COLUMN_NAME_MAX_CIRCULARITY,
                    Entry.COLUMN_NAME_MIN_DIAL_AREA);

    public static final String SQL_DELETE_IMGPROC_TABLE =
            "DROP TABLE IF EXISTS " + Entry.IMGPROC_TABLE_NAME + ";";
}
