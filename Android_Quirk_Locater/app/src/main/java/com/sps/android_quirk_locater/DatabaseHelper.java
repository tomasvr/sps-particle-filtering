package com.sps.android_quirk_locater;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "quirk2";

    private static final String TABLE_NAME_SCAN = "rssi_cell_table";
    private static final String SCAN_CELL = "cell";
    private static final String SCAN_RSSI = "rssi";
    private static final String TABLE_NAME_GAUS = "gaus_table";
    private static final String GAUS_CELL = "cell";
    private static final String GAUS_BSSID = "bssid";
    private static final String GAUS_MEAN = "mean";
    private static final String GAUS_SD = "sd";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createScanTable = "CREATE TABLE " + TABLE_NAME_SCAN + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SCAN_CELL + " INTEGER NOT NULL, " + SCAN_RSSI +" INTEGER NOT NULL)";
        String createGausTable = "CREATE TABLE " + TABLE_NAME_GAUS + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                GAUS_CELL + " INTEGER NOT NULL, " + GAUS_BSSID +" TEXT NOT NULL, " + GAUS_MEAN + " REAL NOT NULL, " + GAUS_SD + " REAL NOT NULL)";
        db.execSQL(createScanTable);
        db.execSQL(createGausTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_SCAN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_GAUS);
        onCreate(db);
    }

    void addScanDataAP(int cell, int rssi, String bssid) {
        SQLiteDatabase db = this.getWritableDatabase();
        String AP_TABLE_NAME = "[AP_" + bssid + "]";
            //bssid serves as table name
        String createScanTable = "CREATE TABLE IF NOT EXISTS " + AP_TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SCAN_CELL + " INTEGER NOT NULL, " + SCAN_RSSI +" INTEGER NOT NULL)";
        db.execSQL(createScanTable);
        ContentValues contentValues = new ContentValues();
        contentValues.put(SCAN_CELL, cell);
        contentValues.put(SCAN_RSSI, rssi);
        //Log.d(TAG, "add scan data: cell: " + cell + " rssi: " + rssi + " bssid: " + bssid + " to " + AP_TABLE_NAME);
        long result = db.insert(AP_TABLE_NAME, null, contentValues);
        //if data is inserted incorrectly it will return -1
        if (result == -1) {
            Log.d(TAG, "failed to add scan data");
        } //else {
            //Log.d(TAG, "scan data added successfully");
        //}
    }

    public Cursor getRssiCountData(String bssid, int cell){
        SQLiteDatabase db = this.getWritableDatabase();
        String AP_TABLE_NAME = "[AP_" + bssid + "]";
        String query = "SELECT " + SCAN_RSSI + ", count(rssi)" + " FROM " + AP_TABLE_NAME +
                " WHERE " + SCAN_CELL + " = '" + cell + "'" + " GROUP BY rssi";
        Cursor data = db.rawQuery(query, null);
        return data;
    }
//
    public boolean checkIfApTableExists(String bssid) {
        boolean exists = false;
        SQLiteDatabase db = this.getWritableDatabase();
        String AP_TABLE_NAME = "AP_" + bssid;
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + AP_TABLE_NAME + "'";
        Cursor data = db.rawQuery(query, null);
        if (!data.moveToFirst()) {
            exists = false;
        }
        else {
            exists = true;
        }
        data.close();
        return exists;
    }


}
