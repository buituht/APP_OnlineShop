package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDAO {
    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;

    public FavoriteDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void addFavorite(String email, String productId) {
        open();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_FAV_USER_EMAIL, email);
        values.put(DatabaseHelper.KEY_FAV_PRODUCT_ID, productId);
        db.insertWithOnConflict(DatabaseHelper.TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        close();
    }

    public void removeFavorite(String email, String productId) {
        open();
        db.delete(DatabaseHelper.TABLE_FAVORITES,
                DatabaseHelper.KEY_FAV_USER_EMAIL + " = ? AND " + DatabaseHelper.KEY_FAV_PRODUCT_ID + " = ?",
                new String[]{email, productId});
        close();
    }

    public boolean isFavorite(String email, String productId) {
        open();
        Cursor cursor = db.query(DatabaseHelper.TABLE_FAVORITES, null,
                DatabaseHelper.KEY_FAV_USER_EMAIL + "=? AND " + DatabaseHelper.KEY_FAV_PRODUCT_ID + "=?",
                new String[]{email, productId}, null, null, null);
        boolean fav = cursor.getCount() > 0;
        cursor.close();
        close();
        return fav;
    }

    public List<String> getFavoriteIds(String email) {
        open();
        List<String> list = new ArrayList<>();
        Cursor cursor = db.query(DatabaseHelper.TABLE_FAVORITES,
                new String[]{DatabaseHelper.KEY_FAV_PRODUCT_ID},
                DatabaseHelper.KEY_FAV_USER_EMAIL + " = ?",
                new String[]{email}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        close();
        return list;
    }
}
