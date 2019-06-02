package com.akvashnin.qrcodescan.ds;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.util.Pair;

import java.util.Map;

public class DataStorage extends ContentProvider {

    // Defines the database name and version
    private static final String DB_NAME = "com.akvashnin.qrcodescan";
    private static final int DB_VERSION = 3;

    // Table with barcodes name
    private static final String BARCODES_TABLE_NAME = "barcodes";
    private static final String PHOTOS_TABLE_NAME = "photos";


    // Barcodes table fields name
    public class Barcode {
        public static final String ID    = "_id";
        public static final String VALUE = "VALUE";
    }

    // Barcodes table fields name
    public class Photos {
        public static final String ID           = "_id";
        public static final String FILE_NAME    = "FILE_NAME";
    }

    public static final Uri BARCODE_CONTENT_URI =
            Uri.parse("content://" + DB_NAME + "/" + BARCODES_TABLE_NAME);

    public static final Uri PHOTOS_CONTENT_URI =
            Uri.parse("content://" + DB_NAME + "/" + PHOTOS_TABLE_NAME);

    static final String BARCODE_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + DB_NAME + "." + BARCODES_TABLE_NAME;

    static final String PHOTOS_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + DB_NAME + "." + PHOTOS_TABLE_NAME;

    static final String BARCODE_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + DB_NAME + "." + BARCODES_TABLE_NAME;

    static final String PHOTOS_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + DB_NAME + "." + PHOTOS_TABLE_NAME;

    static final int URI_BARCODES = 1;
    static final int URI_BARCODES_ID = 2;
    static final int URI_PHOTOS = 3;
    static final int URI_PHOTOS_ID = 4;

    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(DB_NAME, BARCODES_TABLE_NAME, URI_BARCODES);
        uriMatcher.addURI(DB_NAME, BARCODES_TABLE_NAME + "/#", URI_BARCODES_ID);
        uriMatcher.addURI(DB_NAME, PHOTOS_TABLE_NAME, URI_PHOTOS);
        uriMatcher.addURI(DB_NAME, PHOTOS_TABLE_NAME + "/#", URI_PHOTOS_ID);
    }

    DatabaseHelper dbHelper;
    SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        int uriId = uriMatcher.match(uri);
        String id;

        switch (uriId) {
            case URI_BARCODES:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Barcode.ID + " ASC";
                }
                break;
            case URI_BARCODES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Barcode.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Barcode.ID + " = " + id;
                }
                break;
            case URI_PHOTOS:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Photos.ID + " ASC";
                }
                break;
            case URI_PHOTOS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Photos.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Photos.ID + " = " + id;
                }
                break;

            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        Pair<String, Uri> table = resolveTables(uriId);

        db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(table.first, projection, selection,
                selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), table.second);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        int uriId = uriMatcher.match(uri);
        if (uriId != URI_BARCODES && uriId != URI_PHOTOS)
            throw new IllegalArgumentException("Wrong URI: " + uri);

        Pair<String, Uri> table = resolveTables(uriId);

        db = dbHelper.getWritableDatabase();
        long rowID = db.insert(table.first, null, values);
        Uri resultUri = ContentUris.withAppendedId(table.second, rowID);

        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int uriId = uriMatcher.match(uri);
        String id;

        switch (uriId) {
            case URI_BARCODES:
            case URI_PHOTOS:
                break;
            case URI_BARCODES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Barcode.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Barcode.ID + " = " + id;
                }
                break;
            case URI_PHOTOS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Photos.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Photos.ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        Pair<String, Uri> table = resolveTables(uriId);

        db = dbHelper.getWritableDatabase();
        int cnt = db.update(table.first, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriId = uriMatcher.match(uri);
        String id;
        switch (uriId) {
            case URI_BARCODES:
            case URI_PHOTOS:
                break;
            case URI_BARCODES_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Barcode.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Barcode.ID + " = " + id;
                }
                break;
            case URI_PHOTOS_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Photos.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Photos.ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        db = dbHelper.getWritableDatabase();

        Pair<String, Uri> table = resolveTables(uriId);

        int cnt = db.delete(table.first, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case URI_BARCODES:
                return BARCODE_CONTENT_TYPE;
            case URI_BARCODES_ID:
                return BARCODE_CONTENT_ITEM_TYPE;
            case URI_PHOTOS:
                return PHOTOS_CONTENT_TYPE;
            case URI_PHOTOS_ID:
                return PHOTOS_CONTENT_ITEM_TYPE;
        }
        return null;
    }

    private Pair<String,Uri> resolveTables(int uriId) {
        switch (uriId) {
            case URI_BARCODES:
            case URI_BARCODES_ID:
                return new Pair<>(BARCODES_TABLE_NAME, BARCODE_CONTENT_URI);

            case URI_PHOTOS:
            case URI_PHOTOS_ID:
                return new Pair<>(PHOTOS_TABLE_NAME, PHOTOS_CONTENT_URI);
        }
        throw new IllegalArgumentException("Wrong URI");
    }


    // Helper class that actually creates and manages the provider's underlying data repository.
    protected static final class DatabaseHelper extends SQLiteOpenHelper {

        // A string that defines the SQL statement for creating a table
        private static final String SQL_CREATE_BARCODES_TABLE = "CREATE TABLE " +
                BARCODES_TABLE_NAME + "(" +                               // The columns in the table
                Barcode.ID + " INTEGER PRIMARY KEY, " +
                Barcode.VALUE + " TEXT)";

        private static final String SQL_CREATE_PHOTOS_TABLE = "CREATE TABLE " +
                PHOTOS_TABLE_NAME + "(" +                               // The columns in the table
                Photos.ID + " INTEGER PRIMARY KEY, " +
                Photos.FILE_NAME + " TEXT)";

        private static final String SQL_DROP_BARCODES_TABLE = "DROP TABLE IF EXISTS " + BARCODES_TABLE_NAME;
        private static final String SQL_DROP_PHOTOS_TABLE = "DROP TABLE IF EXISTS " + PHOTOS_TABLE_NAME;

        // Instantiates an open helper for the provider's SQLite data repository
        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        // Creates the data repository if it doesn't exist
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Creates tables
            db.execSQL(SQL_CREATE_BARCODES_TABLE);
            db.execSQL(SQL_CREATE_PHOTOS_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DROP_BARCODES_TABLE);
            db.execSQL(SQL_DROP_PHOTOS_TABLE);
            onCreate(db);
        }
    }

}
