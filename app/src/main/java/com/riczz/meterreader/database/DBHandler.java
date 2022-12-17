package com.riczz.meterreader.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.riczz.meterreader.database.model.ElectricMeterConfig;
import com.riczz.meterreader.database.model.GasMeterConfig;
import com.riczz.meterreader.enums.MeterType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class DBHandler extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 6;
    public static final String DATABASE_NAME = "config.db";
    private static final String LOG_TAG = DBHandler.class.getName();

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

    public boolean getBooleanValue(MeterType meterType, String column, boolean defaultValue) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DBContract.METER_TYPE_TABLE_NAMES.get(meterType),
                new String[]{column},
                null, null,
                null, null,
                null, "1"
        );
        cursor.moveToFirst();
        boolean propertyValue = defaultValue;

        if (cursor.getColumnCount() == 1) {
            try {
                propertyValue = cursor.getInt(0) != 0;
                Log.e("ASD", "PROPERTY VALUE GET: " + cursor.getInt(0));
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while getting boolean value from column " + column + ".");
            }
        }
        cursor.close();
        db.close();
        return propertyValue;
    }

    public double getDoubleValue(MeterType meterType, String column, double defaultValue) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(DBContract.METER_TYPE_TABLE_NAMES.get(meterType),
                new String[]{column},
                null, null,
                null, null,
                null, "1"
        );
        cursor.moveToFirst();
        double propertyValue = defaultValue;

        if (cursor.getColumnCount() == 1) {
            try {
                propertyValue = cursor.getDouble(0);
//                Log.e("ASD", "PROPERTY VALUE GET: " + propertyValue);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while getting double value from column " + column + ".");
            }
        }
        cursor.close();
        db.close();
        return propertyValue;
    }

    public void resetDefaults() {
        Set<MeterType> meterTypes = new HashSet<>(Arrays.asList(MeterType.values()));
        resetDefaults(meterTypes);
    }

    public void resetDefaults(Set<MeterType> categories) {
        SQLiteDatabase db = getWritableDatabase();
        if (categories.contains(MeterType.GAS)) {
            Log.e("ASD", "CONTAINS GAS!!!");
            db.execSQL(DBContract.SQL_CLEAR_GAS_TABLE);
            db.execSQL(DBContract.SQL_INSERT_DEFAULT_GAS_ROW);
        }
        if (categories.contains(MeterType.ELECTRIC)) {
            Log.e("ASD", "CONTAINS ELECTRIC!!!");
            db.execSQL(DBContract.SQL_CLEAR_ELECTRIC_TABLE);
            db.execSQL(DBContract.SQL_INSERT_DEFAULT_ELECTRIC_ROW);
        }
    }

    public void updateConfig(MeterType meterType, String column, boolean value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(column, value ? 1 : 0);
        db.update(DBContract.METER_TYPE_TABLE_NAMES.get(meterType), values, null, null);
        db.close();
    }

    public void updateConfig(MeterType meterType, String column, double value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(column, value);
        db.update(DBContract.METER_TYPE_TABLE_NAMES.get(meterType), values, null, null);
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
