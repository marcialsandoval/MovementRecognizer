package com.mars_skyrunner.movementrecognizer.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.mars_skyrunner.movementrecognizer.data.SensorReadingContract.ReadingEntry;

/**
 * {@link ContentProvider} for MovementRecognizer app.
 */
public class SensorReadingProvider extends ContentProvider {

    /**
     * Tag for the log messages
     */
    public final String LOG_TAG = SensorReadingProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the readings table
     */
    private static final int READINGS = 100;

    /**
     * URI matcher code for the content URI for a single reading in the readings table
     */
    private static final int READING_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        sUriMatcher.addURI(SensorReadingContract.CONTENT_AUTHORITY, SensorReadingContract.PATH_READINGS, READINGS);
        sUriMatcher.addURI(SensorReadingContract.CONTENT_AUTHORITY, SensorReadingContract.PATH_READINGS + "/#", READING_ID);
    }

    /**
     * Database helper object
     */
    private SensorReadingsDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new SensorReadingsDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {


        String projectionString = "";

        for(int i = 0; i < projection.length ; i++ ){

            projectionString += projection[i] + " , ";

        }

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            
            case READINGS:
                // For the readings code, query the readings table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the readings table.

                cursor = database.query(ReadingEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
                
            case READING_ID:
                // For the RECORD_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.mars_skyrunner.movementrecognizer/readings/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.

                selection = ReadingEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the readings table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(ReadingEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        
        switch (match) {
            case READINGS:
                Log.w(LOG_TAG,"insert readings/master_readings");
                return insertReading(uri, contentValues);

            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a record into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertReading(Uri uri, ContentValues values) {

        Log.w(LOG_TAG,"insertReading : ContentValues: " + values.toString());

        // Check that the sensorName is not null
        String sensorName = values.getAsString(ReadingEntry.COLUMN_SENSOR_ID);
        if (sensorName == null) {
            throw new IllegalArgumentException("SensorReading requires a sensor id");
        }

        // Check that the sensorName is not null
        String sensorValue = values.getAsString(ReadingEntry.COLUMN_SENSOR_VALUE);
        if (sensorName == null) {
            throw new IllegalArgumentException("SensorReading requires a sensor value");
        }

        // Check that the sensorName is not null
        String sampleRate = values.getAsString(ReadingEntry.COLUMN_SAMPLE_RATE);
        if (sampleRate == null) {
            throw new IllegalArgumentException("SensorReading requires a sensor value");
        }

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        String tableName = "";

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {

            case READINGS:
            case READING_ID:
                tableName = ReadingEntry.TABLE_NAME;
                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Insert the new record with the given values
        long id = database.insert(tableName, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }else{
            Log.w(LOG_TAG, "Record inserted for " + uri);
        }

        // Notify all listeners that the data has changed for the record content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        Uri answer = ContentUris.withAppendedId(uri, id);
        Log.w(LOG_TAG, "Record Uri: " + answer);

        return answer;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case READINGS:
                return updateSensorReading(uri, contentValues, selection, selectionArgs);

            case READING_ID:
                selection = ReadingEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateSensorReading(uri, contentValues, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update readings in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more readings).
     * Return the number of rows that were successfully updated.
     */
    private int updateSensorReading(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // If the {@link ReadingEntry#COLUMN_READING_DATE} key is present,
        // check that the date value is not null.

        // If the {@link ReadingEntry#_COLUMN_SENSOR_ID} key is present,
        // check that the time text is not null.
        if (values.containsKey(ReadingEntry.COLUMN_SENSOR_ID)) {
            String text = values.getAsString(ReadingEntry.COLUMN_SENSOR_ID);
            if (text == null) {
                throw new IllegalArgumentException("SensorReading requires a sensor name");
            }
        }

        // If the {@link ReadingEntry#COLUMN_SENSOR_VALUE} key is present,
        // check that the time text is not null.
        if (values.containsKey(ReadingEntry.COLUMN_SENSOR_VALUE)) {
            String text = values.getAsString(ReadingEntry.COLUMN_SENSOR_VALUE);
            if (text == null) {
                throw new IllegalArgumentException("SensorReading requires a sensor name");
            }
        }

        // If the {@link ReadingEntry#COLUMN_SAMPLE_RATE} key is present,
        // check that the time text is not null.
        if (values.containsKey(ReadingEntry.COLUMN_SAMPLE_RATE)) {
            String text = values.getAsString(ReadingEntry.COLUMN_SAMPLE_RATE);
            if (text == null) {
                throw new IllegalArgumentException("SensorReading requires a sample rate");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();


        String tableName = "";

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {

            case READINGS:
            case READING_ID:
                tableName = ReadingEntry.TABLE_NAME;
                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(tableName, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);

        switch (match) {

            case READINGS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ReadingEntry.TABLE_NAME, selection, selectionArgs);
                break;

            case READING_ID:
                // Delete a single row given by the ID in the URI
                selection = ReadingEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ReadingEntry.TABLE_NAME, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {

            case READINGS:
                return ReadingEntry.CONTENT_LIST_TYPE;

            case READING_ID:
                return ReadingEntry.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}