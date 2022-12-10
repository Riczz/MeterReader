package com.riczz.meterreader.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.riczz.meterreader.database.model.ElectricMeterConfig;
import com.riczz.meterreader.database.model.GasMeterConfig;

public final class DBHandler extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 6;
    public static final String DATABASE_NAME = "config.db";

    public DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public GasMeterConfig getGasMeterConfig() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(DBContract.SQL_SELECT_GAS_ROW, null);

        if (cursor.moveToFirst()) {
            GasMeterConfig config = new GasMeterConfig(
                    cursor.getInt(7) != 0,
                    cursor.getDouble(3),
                    cursor.getDouble(4),
                    cursor.getDouble(5),
                    cursor.getDouble(6),
                    cursor.getDouble(1),
                    cursor.getDouble(8),
                    cursor.getDouble(9),
                    cursor.getDouble(10),
                    cursor.getDouble(11),
                    cursor.getDouble(12),
                    cursor.getDouble(14),
                    cursor.getDouble(15),
                    cursor.getInt(2),
                    cursor.getDouble(13)
            );

            cursor.close();
            return config;
        }
        return null;
    }

    public void updateConfig(GasMeterConfig config) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBContract.Entry.COL_DIGIT_FRAME_EXTENSION_MULTIPLIER, config.getDigitFrameExtensionMultiplier());
        values.put(DBContract.Entry.COL_DIGIT_FRAME_MAX_EXTENSION_COUNT, config.getDigitFrameMaxExtensionCount());
        values.put(DBContract.Entry.COL_MIN_DIAL_AREA, config.getMinDialArea());
        values.put(DBContract.Entry.COL_MAX_SKEWNESS_DEG, config.getMaxSkewnessDeg());
        values.put(DBContract.Entry.COL_MAX_BLACK_INTENSITY_RATIO, config.getMaxBlackIntensityRatio());
        values.put(DBContract.Entry.COL_GAMMA_MULTIPLIER, config.getGammaMultiplier());
        values.put(DBContract.Entry.COL_USE_COLOR_CORRECTION, config.isUseColorCorrection() ? 1 : 0);
        values.put(DBContract.Entry.COL_DIGIT_MAX_WIDTH_RATIO, config.getDigitMaxWidthRatio());
        values.put(DBContract.Entry.COL_DIGIT_MAX_HEIGHT_RATIO, config.getDigitMaxHeightRatio());
        values.put(DBContract.Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MIN, config.getDigitHeightWidthRatioMin());
        values.put(DBContract.Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MAX, config.getDigitHeightWidthRatioMax());
        values.put(DBContract.Entry.COL_DIGIT_MIN_BORDER_DIST, config.getDigitMinBorderDist());
        values.put(DBContract.Entry.COL_DIGIT_BLACK_BORDER_THICKNESS, config.getDigitBlackBorderThickness());
        values.put(DBContract.Entry.COL_MAX_CIRCULARITY, config.getMaxCircularity());
        db.update(DBContract.Entry.TABLE_NAME_GAS_CFG, values, null, null);
        db.close();
    }

    public ElectricMeterConfig getElectricMeterConfig() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(DBContract.SQL_SELECT_ELECTRIC_ROW, null);

        if (cursor.moveToFirst()) {
            ElectricMeterConfig config = new ElectricMeterConfig(
                    cursor.getInt(7) != 0,
                    cursor.getDouble(3),
                    cursor.getDouble(4),
                    cursor.getDouble(5),
                    cursor.getDouble(6),
                    cursor.getDouble(1),
                    cursor.getDouble(8),
                    cursor.getDouble(9),
                    cursor.getDouble(10),
                    cursor.getDouble(11),
                    cursor.getDouble(12),
                    cursor.getDouble(15),
                    cursor.getDouble(16),
                    cursor.getInt(2),
                    cursor.getDouble(13),
                    cursor.getDouble(14)
            );

            cursor.close();
            return config;
        }
        return null;
    }

    public void updateConfig(ElectricMeterConfig config) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBContract.Entry.COL_DIGIT_FRAME_EXTENSION_MULTIPLIER, config.getDigitFrameExtensionMultiplier());
        values.put(DBContract.Entry.COL_DIGIT_FRAME_MAX_EXTENSION_COUNT, config.getDigitFrameMaxExtensionCount());
        values.put(DBContract.Entry.COL_MIN_DIAL_AREA, config.getMinDialArea());
        values.put(DBContract.Entry.COL_MAX_SKEWNESS_DEG, config.getMaxSkewnessDeg());
        values.put(DBContract.Entry.COL_MAX_BLACK_INTENSITY_RATIO, config.getMaxBlackIntensityRatio());
        values.put(DBContract.Entry.COL_GAMMA_MULTIPLIER, config.getGammaMultiplier());
        values.put(DBContract.Entry.COL_USE_COLOR_CORRECTION, config.isUseColorCorrection() ? 1 : 0);
        values.put(DBContract.Entry.COL_DIGIT_MAX_WIDTH_RATIO, config.getDigitMaxWidthRatio());
        values.put(DBContract.Entry.COL_DIGIT_MAX_HEIGHT_RATIO, config.getDigitMaxHeightRatio());
        values.put(DBContract.Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MIN, config.getDigitHeightWidthRatioMin());
        values.put(DBContract.Entry.COL_DIGIT_HEIGHT_WIDTH_RATIO_MAX, config.getDigitHeightWidthRatioMax());
        values.put(DBContract.Entry.COL_DIGIT_MIN_BORDER_DIST, config.getDigitMinBorderDist());
        values.put(DBContract.Entry.COL_DIGIT_BLACK_BORDER_THICKNESS, config.getDigitBlackBorderThickness());
        values.put(DBContract.Entry.COL_MIN_RECTANGULARITY, config.getMinRectangularity());
        values.put(DBContract.Entry.COL_MAX_LINE_DISTANCE, config.getMaxLineDistance());
        db.update(DBContract.Entry.TABLE_NAME_GAS_CFG, values, null, null);
        db.close();
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBContract.SQL_CREATE_GAS_CONFIG_TABLE);
        db.execSQL(DBContract.SQL_CREATE_ELECTRIC_CONFIG_TABLE);
        db.execSQL(DBContract.SQL_INSERT_DEFAULT_GAS_ROW);
        db.execSQL(DBContract.SQL_INSERT_DEFAULT_ELECTRIC_ROW);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DBContract.SQL_DELETE_GAS_TABLE);
        db.execSQL(DBContract.SQL_DELETE_ELECTRIC_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
