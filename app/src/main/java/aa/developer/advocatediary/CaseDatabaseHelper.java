package aa.developer.advocatediary;

import android.app.backup.BackupManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by ameyaapte1 on 29/1/17.
 */

public class CaseDatabaseHelper extends SQLiteOpenHelper {
    // Database Info
    public static final String DATABASE_NAME = "CaseDatabase.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_CASES = "Cases";

    // Case Table Columns
    private static final String KEY_CASE_ID = "_id";
    private static final String KEY_CASE_NEXT_DATE = "ndate";
    private static final String KEY_CASE_PARTIES = "title";
    private static final String KEY_CASE_NO = "case_no";
    private static final String KEY_CASE_JSON = "jcase";
    private static final String KEY_CASE_HTML = "html";


    private static final String TAG = "DBHelper";

    private static CaseDatabaseHelper sInstance;

    /**
     * Constructor should be private to prevent direct instantiation.
     * Make a call to the static method "getInstance()" instead.
     */
    private CaseDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static synchronized CaseDatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new CaseDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }



    // Called when the database connection is being configured.
    // Configure database settings for things like foreign key support, write-ahead logging, etc.
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
            db.disableWriteAheadLogging();
        }
    }

    // Called when the database is created for the FIRST time.
    // If a database already exists on disk with the same DATABASE_NAME, this method will NOT be called.
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CASES_TABLE = "CREATE TABLE " + TABLE_CASES +
                "(" +
                KEY_CASE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + // Define a primary key
                KEY_CASE_PARTIES + " TEXT," +
                KEY_CASE_NO + " TEXT UNIQUE," +
                KEY_CASE_HTML + " TEXT," +
                KEY_CASE_JSON + " TEXT," +
                KEY_CASE_NEXT_DATE + " INTEGER" +
                ")";
        db.execSQL(CREATE_CASES_TABLE);
    }

    // Called when the database needs to be upgraded.
    // This method will only be called if a database already exists on disk with the same DATABASE_NAME,
    // but the DATABASE_VERSION is different than the version of the database that exists on disk.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion > newVersion) {
            // Simplest implementation is to drop all old tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CASES);
            onCreate(db);
        }
    }



    boolean updateCaseDateHtml(String case_no, String next_date, String html) {
        // Create and/or open the database for writing
        SQLiteDatabase db = getWritableDatabase();
        int changes = 0;

        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            if (!next_date.equals("Decided")) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
                Date date = simpleDateFormat.parse(next_date);
                values.put(KEY_CASE_NEXT_DATE, date.getTime());
                values.put(KEY_CASE_HTML, html);
            } else {
                values.put(KEY_CASE_NEXT_DATE, -1);
                values.put(KEY_CASE_HTML, html);
            }
            changes = db.update(TABLE_CASES, values, KEY_CASE_NO + "=\"" + case_no + "\"", null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add post to database");
            return false;
        } finally {
            db.endTransaction();
            return changes != 0;
        }

    }

    // Insert a post into the database
    void addCase(HashMap<String, String> hashMap) {
        // Create and/or open the database for writing
        SQLiteDatabase db = getWritableDatabase();

        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            // The user might already exist in the database (i.e. the same user created multiple posts).
            String parties, case_no, html, json, next_date;
            parties = hashMap.get("parties");
            case_no = hashMap.get("case_no");
            html = hashMap.get("html");
            json = hashMap.get("json");
            next_date = hashMap.get("next_date");
            Long time;
            if (!next_date.equals("Decided")) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
                time = simpleDateFormat.parse(next_date).getTime();
            } else {
                time = -1L;
            }

            ContentValues values = new ContentValues();

            values.put(KEY_CASE_PARTIES, parties);
            values.put(KEY_CASE_NO, case_no);
            values.put(KEY_CASE_HTML, html);
            values.put(KEY_CASE_JSON, json);
            values.put(KEY_CASE_NEXT_DATE, time);


            // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
            db.insertOrThrow(TABLE_CASES, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add post to database");
        } finally {
            db.endTransaction();

        }
    }

    //Delete Case
    void removeCase(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_CASES, KEY_CASE_ID + "=?", new String[]{id});
            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.d(TAG, "Error while trying to add post to database");
        } finally {
            db.endTransaction();
        }

    }

    //Get Cursor INEFFICIENT
    public Cursor getAllEntries() {
        String CASES_SELECT_QUERY = String.format("SELECT * FROM %s ORDER BY %s ASC", TABLE_CASES, KEY_CASE_NEXT_DATE);
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(CASES_SELECT_QUERY, null);
    }

    int getCount() {
        String CASES_COUNT_QUERY = String.format("SELECT %s FROM %s", KEY_CASE_ID, TABLE_CASES);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(CASES_COUNT_QUERY, null);
        return cursor.getCount();
    }

    Cursor getListEntries(int filter) {
        Calendar calendar = Calendar.getInstance();
        Long now = calendar.getTimeInMillis();
        Long start, end;
        switch (filter) {
            case 1:  //Day
                start = now;
                calendar.roll(Calendar.DAY_OF_YEAR, 1);
                end = calendar.getTimeInMillis();
                break;
            case 2:  //Week
                start = now;
                calendar.roll(Calendar.WEEK_OF_YEAR, 1);
                end = calendar.getTimeInMillis();
                break;
            case 3: //Month
                start = now;
                calendar.roll(Calendar.MONTH, 1);
                end = calendar.getTimeInMillis();
                break;
            case 4: //Decided
                start = -10L;
                end = 0L;
                break;
            default:  //ALl
                start = 0L;
                end = 0L;
                break;
        }
        String CASES_SELECT_QUERY;
        if (start == 0 && end == 0)
            CASES_SELECT_QUERY = String.format("SELECT _id,title,case_no,ndate FROM %s WHERE ndate > 0 ORDER BY %s ASC", TABLE_CASES, KEY_CASE_NEXT_DATE);
        else {
            CASES_SELECT_QUERY = String.format("SELECT _id,title,case_no,ndate FROM %s WHERE ndate >= %s AND ndate <= %s ORDER BY %s ASC", TABLE_CASES, start, end, KEY_CASE_NEXT_DATE);
        }

        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(CASES_SELECT_QUERY, null);
    }

    Cursor getUpdatableEntries() {
        String now = String.valueOf(System.currentTimeMillis());
        String CASES_SELECT_QUERY = String.format("SELECT jcase FROM %s WHERE %s <= %s AND %s > 0 ", TABLE_CASES, KEY_CASE_NEXT_DATE, now, KEY_CASE_NEXT_DATE);
        //String CASES_SELECT_QUERY = String.format("SELECT jcase FROM %s WHERE %s > 0 ", TABLE_CASES, KEY_CASE_NEXT_DATE, KEY_CASE_NEXT_DATE);
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(CASES_SELECT_QUERY, null);
    }


    Cursor searchCase(String keyword, int filter) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;
        Calendar calendar = Calendar.getInstance();
        Long now = calendar.getTimeInMillis();
        Long start, end;
        switch (filter) {
            case 1:  //Day
                start = now;
                calendar.roll(Calendar.DAY_OF_YEAR, 1);
                end = calendar.getTimeInMillis();
                break;
            case 2:  //Week
                start = now;
                calendar.roll(Calendar.WEEK_OF_YEAR, 1);
                end = calendar.getTimeInMillis();
                break;
            case 3: //Month
                start = now;
                calendar.roll(Calendar.MONTH, 1);
                end = calendar.getTimeInMillis();
                break;
            case 4: //Decided
                start = -10L;
                end = 0L;
                break;
            default:  //Pending
                start = 0L;
                end = 0L;
                break;
        }
        if (start == 0 && end == 0)
            cursor = db.query(TABLE_CASES, new String[]{"_id", "title", "case_no", "ndate"}, KEY_CASE_NEXT_DATE + " > 0 AND (" + KEY_CASE_PARTIES + " LIKE ? OR " + KEY_CASE_NO + " LIKE ? )", new String[]{"%" + keyword + "%", "%" + keyword + "%"}, null, null, KEY_CASE_NEXT_DATE + " ASC");
        else {
            cursor = db.query(TABLE_CASES, new String[]{"_id", "title", "case_no", "ndate"}, "( " + KEY_CASE_NEXT_DATE + " >= ? AND " + KEY_CASE_NEXT_DATE + " <= ? ) AND ( " + KEY_CASE_PARTIES + " LIKE ? OR " + KEY_CASE_NO + " LIKE ? )", new String[]{start.toString(), end.toString(), "%" + keyword + "%", "%" + keyword + "%"}, null, null, KEY_CASE_NEXT_DATE + " ASC");
        }
        return cursor;
    }

    String getCaseHTML(String id) {
        String CASES_SELECT_QUERY = String.format("SELECT " + KEY_CASE_HTML + " FROM %s WHERE " + KEY_CASE_ID + " = %s", TABLE_CASES, id);
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(CASES_SELECT_QUERY, null);
        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndexOrThrow(KEY_CASE_HTML));

    }

    String getCaseDate(String case_no) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
        String CASES_SELECT_QUERY = String.format("SELECT " + KEY_CASE_NEXT_DATE + " FROM %s WHERE " + KEY_CASE_NO + " = %s", TABLE_CASES, "\"" + case_no + "\"");
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(CASES_SELECT_QUERY, null);
        cursor.moveToFirst();
        return simpleDateFormat.format(new Date(Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CASE_NEXT_DATE)))));

    }
}

