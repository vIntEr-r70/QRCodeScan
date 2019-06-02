package com.akvashnin.qrcodescan.ds;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

public class DataStorage extends ContentProvider {

    // Defines the database name and version
    private static final String DB_NAME = "com.akvashnin.qrcodescan";
    private static final int DB_VERSION = 2;

    // Table with barcodes name
    private static final String BARCODES_TABLE_NAME = "barcodes";

    // Barcodes table fields name
    public class Barcode {
        public static final String ID    = "_id";
        public static final String VALUE = "VALUE";
    }

    public static final Uri BARCODE_CONTENT_URI =
            Uri.parse("content://" + DB_NAME + "/" + BARCODES_TABLE_NAME);

    static final String BARCODE_CONTENT_TYPE = "vnd.android.cursor.dir/vnd."
            + DB_NAME + "." + BARCODES_TABLE_NAME;

    // одна строка
    static final String BARCODE_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd."
            + DB_NAME + "." + BARCODES_TABLE_NAME;

    static final int URI_BARCODES = 1;
    static final int URI_BARCODES_ID = 2;

    // описание и создание UriMatcher
    private static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(DB_NAME, BARCODES_TABLE_NAME, URI_BARCODES);
        uriMatcher.addURI(DB_NAME, BARCODES_TABLE_NAME + "/#", URI_BARCODES_ID);
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

        switch (uriMatcher.match(uri)) {
            case URI_BARCODES:
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Barcode.ID + " ASC";
                }
                break;
            case URI_BARCODES_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Barcode.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Barcode.ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(BARCODES_TABLE_NAME, projection, selection,
                selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), BARCODE_CONTENT_URI);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (uriMatcher.match(uri) != URI_BARCODES)
            throw new IllegalArgumentException("Wrong URI: " + uri);

        db = dbHelper.getWritableDatabase();
        long rowID = db.insert(BARCODES_TABLE_NAME, null, values);
        Uri resultUri = ContentUris.withAppendedId(BARCODE_CONTENT_URI, rowID);

        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case URI_BARCODES:
                break;
            case URI_BARCODES_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Barcode.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Barcode.ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }
        db = dbHelper.getWritableDatabase();
        int cnt = db.update(BARCODES_TABLE_NAME, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return cnt;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case URI_BARCODES:
                break;
            case URI_BARCODES_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    selection = Barcode.ID + " = " + id;
                } else {
                    selection = selection + " AND " + Barcode.ID + " = " + id;
                }
                break;
            default:
                throw new IllegalArgumentException("Wrong URI: " + uri);
        }

        db = dbHelper.getWritableDatabase();

        int cnt = db.delete(BARCODES_TABLE_NAME, selection, selectionArgs);
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
        }
        return null;
    }


    // Helper class that actually creates and manages the provider's underlying data repository.
    protected static final class DatabaseHelper extends SQLiteOpenHelper {

        // A string that defines the SQL statement for creating a table
        private static final String SQL_CREATE_BARCODES_TABLE = "CREATE TABLE " +
                BARCODES_TABLE_NAME + "(" +                               // The columns in the table
                Barcode.ID + " INTEGER PRIMARY KEY, " +
                Barcode.VALUE + " TEXT)";

        private static final String SQL_DROP_BARCODES_TABLE = "DROP TABLE IF EXISTS " + BARCODES_TABLE_NAME;

        // Instantiates an open helper for the provider's SQLite data repository
        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        // Creates the data repository if it doesn't exist
        @Override
        public void onCreate(SQLiteDatabase db) {
            // Creates tables
            db.execSQL(SQL_CREATE_BARCODES_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DROP_BARCODES_TABLE);
            onCreate(db);
        }
    }

}
