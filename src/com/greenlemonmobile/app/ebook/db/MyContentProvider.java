package com.greenlemonmobile.app.ebook.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

public class MyContentProvider extends ContentProvider {
    
    private static final String AUTHORITY = "com.greenlemonmobile.app";
    
    public static final Uri BOOK_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MySQLiteOpenHelper.BOOK_TABLE_NAME);
    public static final Uri BOOKMARK_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MySQLiteOpenHelper.BOOKMARK_TABLE_NAME);
    public static final Uri BUILD_IN_BOOK_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MySQLiteOpenHelper.BUILD_IN_BOOK_TABLE_NAME);
    
    private static final UriMatcher uriMatcher;
    
    private static final int BOOK_URI_CODE = 1;
    private static final int BOOKMARK_URI_CODE = 2;
    private static final int BUILD_IN_BOOK_URI_CODE = 3;
    
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, MySQLiteOpenHelper.BOOK_TABLE_NAME, BOOK_URI_CODE);
        uriMatcher.addURI(AUTHORITY, MySQLiteOpenHelper.BOOKMARK_TABLE_NAME, BOOKMARK_URI_CODE);
        uriMatcher.addURI(AUTHORITY, MySQLiteOpenHelper.BUILD_IN_BOOK_TABLE_NAME, BUILD_IN_BOOK_URI_CODE);
    }
    
    private MySQLiteOpenHelper mSQLiteOpenHelper;
    
    @Override
    public boolean onCreate() {
        mSQLiteOpenHelper = new MySQLiteOpenHelper(getContext());
        return false;
    }

    @Override
    public int delete(Uri uri, String whereClause, String[] whereArgs) {
        int rows = 0;
        String table = dispatchTableName(uri);
        if (!TextUtils.isEmpty(table))
            rows = mSQLiteOpenHelper.getReadableDatabase().delete(table, whereClause, whereArgs);
        return rows;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case BOOK_URI_CODE:
                return "vnd.android.cursor.dir/" + MySQLiteOpenHelper.BOOK_TABLE_NAME;
            case BOOKMARK_URI_CODE:
                return "vnd.android.cursor.dir/" + MySQLiteOpenHelper.BOOKMARK_TABLE_NAME;
            case BUILD_IN_BOOK_URI_CODE:
            	return "vnd.android.cursor.dir/" + MySQLiteOpenHelper.BUILD_IN_BOOK_TABLE_NAME;
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = -1;
        String table = dispatchTableName(uri);
        if (!TextUtils.isEmpty(table))
            rowID = mSQLiteOpenHelper.getReadableDatabase().insert(table, null, values);
        if (rowID >= 0)
            return Uri.parse(BOOK_CONTENT_URI + "/" + Long.toString(rowID));
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] columns, String selection, String[] selectionArgs, String orderBy) {
        Cursor cursor = null;
        String table = dispatchTableName(uri);
        if (!TextUtils.isEmpty(table))
            cursor = mSQLiteOpenHelper.getReadableDatabase().query(table, columns, selection, selectionArgs, null, null, orderBy);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
        int rows = 0;
        String table = dispatchTableName(uri);
        if (!TextUtils.isEmpty(table))
            rows = mSQLiteOpenHelper.getReadableDatabase().update(table, values, whereClause, whereArgs);
        return rows;
    }

    private String dispatchTableName(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case BOOK_URI_CODE:
                return MySQLiteOpenHelper.BOOK_TABLE_NAME;
            case BOOKMARK_URI_CODE:
                return MySQLiteOpenHelper.BOOKMARK_TABLE_NAME;
            case BUILD_IN_BOOK_URI_CODE:
            	return MySQLiteOpenHelper.BUILD_IN_BOOK_TABLE_NAME;
            default:
                break;
        }
        return null;
    }
}
