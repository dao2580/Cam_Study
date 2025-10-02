package vn.edu.usth.camer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CamerDB.db";
    private static final int DATABASE_VERSION = 1;

    // User Table
    private static final String TABLE_USER = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    // History Table
    private static final String TABLE_HISTORY = "history";
    private static final String COL_HISTORY_ID = "id";
    private static final String COL_HISTORY_USER_ID = "user_id";
    private static final String COL_IMAGE_PATH = "image_path";
    private static final String COL_DETECTED_TEXT = "detected_text";
    private static final String COL_TRANSLATED_TEXT = "translated_text";
    private static final String COL_SOURCE_LANG = "source_lang";
    private static final String COL_TARGET_LANG = "target_lang";
    private static final String COL_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT, " +
                COL_EMAIL + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(createUserTable);

        String createHistoryTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HISTORY_USER_ID + " INTEGER, " +
                COL_IMAGE_PATH + " TEXT, " +
                COL_DETECTED_TEXT + " TEXT, " +
                COL_TRANSLATED_TEXT + " TEXT, " +
                COL_SOURCE_LANG + " TEXT, " +
                COL_TARGET_LANG + " TEXT, " +
                COL_TIMESTAMP + " INTEGER)";
        db.execSQL(createHistoryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // ========== USER METHODS ==========

    public boolean registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USER, null, values);
        return result != -1;
    }

    public int loginUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER,
                new String[]{COL_USER_ID},
                COL_EMAIL + "=? AND " + COL_PASSWORD + "=?",
                new String[]{email, password},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int userId = cursor.getInt(0);
            cursor.close();
            return userId;
        }
        if (cursor != null) cursor.close();
        return -1;
    }

    public boolean checkEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER,
                new String[]{COL_USER_ID},
                COL_EMAIL + "=?",
                new String[]{email},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // ========== HISTORY METHODS ==========

    public boolean saveHistory(int userId, String imagePath, String detectedText,
                               String translatedText, String sourceLang, String targetLang) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HISTORY_USER_ID, userId);
        values.put(COL_IMAGE_PATH, imagePath);
        values.put(COL_DETECTED_TEXT, detectedText);
        values.put(COL_TRANSLATED_TEXT, translatedText);
        values.put(COL_SOURCE_LANG, sourceLang);
        values.put(COL_TARGET_LANG, targetLang);
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        long result = db.insert(TABLE_HISTORY, null, values);
        return result != -1;
    }

    public ArrayList<HashMap<String, String>> getHistory(int userId) {
        ArrayList<HashMap<String, String>> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null,
                COL_HISTORY_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, COL_TIMESTAMP + " DESC");

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", cursor.getString(cursor.getColumnIndexOrThrow(COL_HISTORY_ID)));
                map.put("image_path", cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)));
                map.put("detected_text", cursor.getString(cursor.getColumnIndexOrThrow(COL_DETECTED_TEXT)));
                map.put("translated_text", cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSLATED_TEXT)));
                map.put("source_lang", cursor.getString(cursor.getColumnIndexOrThrow(COL_SOURCE_LANG)));
                map.put("target_lang", cursor.getString(cursor.getColumnIndexOrThrow(COL_TARGET_LANG)));

                // Format timestamp
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                map.put("time", sdf.format(new Date(timestamp)));

                historyList.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return historyList;
    }

    public boolean deleteHistory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_HISTORY, COL_HISTORY_ID + "=?",
                new String[]{String.valueOf(id)}) > 0;
    }

    public int getHistoryCount(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_HISTORY, null,
                COL_HISTORY_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}