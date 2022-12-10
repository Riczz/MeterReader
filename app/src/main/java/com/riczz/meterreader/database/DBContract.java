package com.riczz.meterreader.database;

import android.provider.BaseColumns;

import java.util.Locale;

public final class DBContract {

    private DBContract() {
    }

    static final class Entry implements BaseColumns {
        static final String
                TABLE_NAME_GAS_CFG = "gas_meter_config",
                TABLE_NAME_ELECTRIC_CFG = "electric_meter_config",
                COL_ID = "id",
                COL_MIN_DIAL_AREA = "min_dial_area",
                COL_MAX_SKEWNESS_DEG = "max_skewness_deg",
                COL_MAX_BLACK_INTENSITY_RATIO = "max_black_intensity_ratio",
                COL_GAMMA_MULTIPLIER = "gamma_multiplier",
                COL_USE_COLOR_CORRECTION = "use_color_correction",
                COL_DIAL_FRAME_WIDTH_MULTIPLIER = "dial_frame_width_multiplier",
                COL_DIGIT_FRAME_EXTENSION_MULTIPLIER = "digit_frame_extension_multiplier",
                COL_DIGIT_FRAME_MAX_EXTENSION_COUNT = "digit_frame_max_extension_count",
                COL_DIGIT_MAX_WIDTH_RATIO = "digit_max_width_ratio",
                COL_DIGIT_MAX_HEIGHT_RATIO = "digit_max_height_ratio",
                COL_DIGIT_HEIGHT_WIDTH_RATIO_MIN = "digit_height_width_ratio_min",
                COL_DIGIT_HEIGHT_WIDTH_RATIO_MAX = "digit_height_width_ratio_max",
                COL_DIGIT_MIN_BORDER_DIST = "digit_min_border_dist",
                COL_DIGIT_BLACK_BORDER_THICKNESS = "digit_black_border_thickness",
                COL_MAX_CIRCULARITY = "max_circularity",
                COL_MIN_RECTANGULARITY = "min_rectangularity",
                COL_MAX_LINE_DISTANCE = "max_line_distance";
    }

    static final String SQL_CREATE_GAS_CONFIG_TABLE = String
            .format(Locale.ROOT,
                    "CREATE TABLE IF NOT EXISTS %s (%s INTEGER NOT NULL PRIMARY KEY ASC AUTOINCREMENT, " +
                            "%s REAL NOT NULL DEFAULT %.2f, " +
                            "%s INTEGER NOT NULL DEFAULT %d, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s BOOLEAN NOT NULL DEFAULT %d CHECK (%s IN (0, 1)), " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f);",
                    Entry.TABLE_NAME_GAS_CFG, Entry.COL_ID,
                    Entry.COL_DIGIT_FRAME_EXTENSION_MULTIPLIER, 1.05d,
                    Entry.COL_DIGIT_FRAME_MAX_EXTENSION_COUNT, 2,
                    Entry.COL_MIN_DIAL_AREA, 1000.000d,
                    Entry.COL_MAX_SKEWNESS_DEG, 30.000d,
                    Entry.COL_MAX_BLACK_INTENSITY_RATIO, 0.950d,
                    Entry.COL_GAMMA_MULTIPLIER, 3.500d,
                    Entry.COL_USE_COLOR_CORRECTION, 1, Entry.COL_USE_COLOR_CORRECTION,
                    Entry.COL_DIGIT_MAX_WIDTH_RATIO, 0.150d,
                    Entry.COL_DIGIT_MAX_HEIGHT_RATIO, 0.650d,
                    Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MIN, 0.100d,
                    Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MAX, 6.500d,
                    Entry.COL_DIGIT_MIN_BORDER_DIST, 0.020d,
                    Entry.COL_MAX_CIRCULARITY, 0.6d,
                    Entry.COL_DIGIT_BLACK_BORDER_THICKNESS, 0.275d,
                    Entry.COL_DIAL_FRAME_WIDTH_MULTIPLIER, 4.000d
            );

    static final String SQL_SELECT_GAS_ROW = String
            .format(Locale.ROOT, "SELECT * FROM %s;", Entry.TABLE_NAME_GAS_CFG);

    static final String SQL_INSERT_DEFAULT_GAS_ROW = String
            .format(Locale.ROOT, "INSERT INTO %s DEFAULT VALUES;", Entry.TABLE_NAME_GAS_CFG);

    static final String SQL_DELETE_GAS_TABLE =
            "DROP TABLE IF EXISTS " + Entry.TABLE_NAME_GAS_CFG + ";";

    static final String SQL_CREATE_ELECTRIC_CONFIG_TABLE = String
            .format(Locale.ROOT,
                    "CREATE TABLE IF NOT EXISTS %s (%s INTEGER NOT NULL PRIMARY KEY ASC AUTOINCREMENT, " +
                            "%s REAL NOT NULL DEFAULT %.2f, " +
                            "%s INTEGER NOT NULL DEFAULT %d, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s BOOLEAN NOT NULL DEFAULT %d CHECK (%s IN (0, 1)), " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f, " +
                            "%s REAL NOT NULL DEFAULT %.3f);",
                    Entry.TABLE_NAME_ELECTRIC_CFG, Entry.COL_ID,
                    Entry.COL_DIGIT_FRAME_EXTENSION_MULTIPLIER, 1.05d,
                    Entry.COL_DIGIT_FRAME_MAX_EXTENSION_COUNT, 5,
                    Entry.COL_MIN_DIAL_AREA, 300.000d,
                    Entry.COL_MAX_SKEWNESS_DEG, 30.000d,
                    Entry.COL_MAX_BLACK_INTENSITY_RATIO, 0.820d,
                    Entry.COL_GAMMA_MULTIPLIER, 3.500d,
                    Entry.COL_USE_COLOR_CORRECTION, 0, Entry.COL_USE_COLOR_CORRECTION,
                    Entry.COL_DIGIT_MAX_WIDTH_RATIO, 0.100d,
                    Entry.COL_DIGIT_MAX_HEIGHT_RATIO, 0.750d,
                    Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MIN, 0.100d,
                    Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MAX, 8.750d,
                    Entry.COL_DIGIT_MIN_BORDER_DIST, 0.020d,
                    Entry.COL_MIN_RECTANGULARITY, 0.650d,
                    Entry.COL_MAX_LINE_DISTANCE, 15.000d,
                    Entry.COL_DIGIT_BLACK_BORDER_THICKNESS, 0.275d,
                    Entry.COL_DIAL_FRAME_WIDTH_MULTIPLIER, 13.000d
            );

    static final String SQL_SELECT_ELECTRIC_ROW = String
            .format(Locale.ROOT, "SELECT * FROM %s;", Entry.TABLE_NAME_ELECTRIC_CFG);

    static final String SQL_INSERT_DEFAULT_ELECTRIC_ROW = String
            .format(Locale.ROOT, "INSERT INTO %s DEFAULT VALUES;", Entry.TABLE_NAME_ELECTRIC_CFG);


    static final String SQL_DELETE_ELECTRIC_TABLE =
            "DROP TABLE IF EXISTS " + Entry.TABLE_NAME_ELECTRIC_CFG + ";";
}
