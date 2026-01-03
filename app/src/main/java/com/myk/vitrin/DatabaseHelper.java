package com.myk.vitrin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public DatabaseHelper(Context context) {
        super(context, "vitrin_final.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE ads (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, price TEXT, description TEXT, category TEXT, subcategory TEXT, imageUri TEXT)");

        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");

        db.execSQL("CREATE TABLE subcategories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, parent_category TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS ads");
        db.execSQL("DROP TABLE IF EXISTS categories");
        db.execSQL("DROP TABLE IF EXISTS subcategories");
        onCreate(db);
    }


    public void addAd(Ad ad) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", ad.getTitle());
        values.put("price", ad.getPrice());
        values.put("description", ad.getDescription());
        values.put("category", ad.getCategory());
        values.put("subcategory", ad.getSubcategory());
        values.put("imageUri", ad.getImageUri());
        db.insert("ads", null, values);
        db.close();
    }

    public void updateAd(Ad ad) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", ad.getTitle());
        values.put("price", ad.getPrice());
        values.put("description", ad.getDescription());
        values.put("category", ad.getCategory());
        values.put("subcategory", ad.getSubcategory());
        values.put("imageUri", ad.getImageUri());
        db.update("ads", values, "id=?", new String[]{String.valueOf(ad.getId())});
        db.close();
    }

    public void deleteAd(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("ads", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }


    public List<String> getCategoryNames() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM categories", null);
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)); } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void addCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        db.insert("categories", null, values);
        db.close();
    }


    public void updateCategory(String oldName, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {

            ContentValues catValues = new ContentValues();
            catValues.put("name", newName);
            db.update("categories", catValues, "name=?", new String[]{oldName});

            ContentValues subValues = new ContentValues();
            subValues.put("parent_category", newName);
            db.update("subcategories", subValues, "parent_category=?", new String[]{oldName});

            ContentValues adValues = new ContentValues();
            adValues.put("category", newName);
            db.update("ads", adValues, "category=?", new String[]{oldName});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void deleteCategory(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {

            db.delete("ads", "category = ?", new String[]{name});

            db.delete("subcategories", "parent_category = ?", new String[]{name});

            db.delete("categories", "name = ?", new String[]{name});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public List<String> getSubCategoryNames(String parentCategory) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM subcategories WHERE parent_category = ?", new String[]{parentCategory});
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0)); } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void addSubCategory(String name, String parentCategory) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("parent_category", parentCategory);
        db.insert("subcategories", null, values);
        db.close();
    }

    public void updateSubCategory(String oldName, String newName, String parentCategory) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {

            ContentValues subValues = new ContentValues();
            subValues.put("name", newName);

            db.update("subcategories", subValues, "name=? AND parent_category=?", new String[]{oldName, parentCategory});

            ContentValues adValues = new ContentValues();
            adValues.put("subcategory", newName);
            db.update("ads", adValues, "subcategory=? AND category=?", new String[]{oldName, parentCategory});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void deleteSubCategory(String name, String parentCategory) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {

            db.delete("ads", "subcategory = ? AND category = ?", new String[]{name, parentCategory});

            db.delete("subcategories", "name = ? AND parent_category = ?", new String[]{name, parentCategory});

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }


    public List<Ad> getFilteredAds(String query, String category, String subcategory, long minPrice, long maxPrice, String sortOrder) {
        List<Ad> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ads WHERE (title LIKE ? OR description LIKE ?) ");
        List<String> args = new ArrayList<>();
        args.add("%" + query + "%");
        args.add("%" + query + "%");

        if (!category.equals("همه")) {
            sb.append("AND category = ? ");
            args.add(category);

            if (subcategory != null && !subcategory.isEmpty() && !subcategory.equals("همه")) {
                sb.append("AND subcategory = ? ");
                args.add(subcategory);
            }
        }

        sb.append("AND CAST(price AS INTEGER) >= ? AND CAST(price AS INTEGER) <= ? ");
        args.add(String.valueOf(minPrice));
        args.add(String.valueOf(maxPrice));

        sb.append("ORDER BY ");
        switch (sortOrder) {
            case "oldest": sb.append("id ASC"); break;
            case "expensive": sb.append("CAST(price AS INTEGER) DESC"); break;
            case "cheap": sb.append("CAST(price AS INTEGER) ASC"); break;
            default: sb.append("id DESC"); break;
        }

        Cursor cursor = db.rawQuery(sb.toString(), args.toArray(new String[0]));

        if (cursor.moveToFirst()) {
            do {
                list.add(new Ad(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getString(6)
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}