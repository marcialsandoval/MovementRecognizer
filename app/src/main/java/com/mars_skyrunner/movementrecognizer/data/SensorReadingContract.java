package com.mars_skyrunner.movementrecognizer.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


/**
 * API Contract for SensorReading Database for MovementRecognizer app.
 */
public  final class SensorReadingContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private SensorReadingContract() {}

    /**
     * Content Provider name
     */
    public static final String CONTENT_AUTHORITY = "com.mars_skyrunner.movementrecognizer";

    /**
     * Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     * the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);


    /**
     * Possible path (appended to base content URI for possible URI's)
     * For instance, content://com.mars_skyrunner.movementrecognizer/readings/ is a valid path for
     * looking at SensorReading data.
     */

    public static final String PATH_READINGS = "readings";

    /**
     * Inner class that defines constant values for the SensorReadings database table.
     * Each entry in the table represents a single SensorReading.
     */

    public static final class ReadingEntry implements BaseColumns {

        /** The content URI to access the sensor data in the provider */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_READINGS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of records.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_READINGS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single record.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_READINGS;

        /** Name of database table for sensor readings */
        public final static String TABLE_NAME = "readings_table";

        /**
         * Unique ID number for the SensorReading (only for use in the database table).
         *
         * Type: INTEGER
         */

        public final static String _ID = BaseColumns._ID;


        /**
         * Timestamp of the SensorReading.
         *
         * Type: TEXT
         */
        public final static String COLUMN_TIME = "time";


        /**
         * Sensorid from the SensorReading.
         *
         * Type: TEXT
         */
        public final static String COLUMN_SENSOR_ID = "sensor_id";

        /**
         * Value of the Sensor from the SensorReading.
         *
         * Type: TEXT
         */
        public final static String COLUMN_SENSOR_VALUE = "sensor_value";


        /**
         * Sensor sample rate of the SensorReading.
         *
         * Type: TEXT
         */
        public final static String COLUMN_SAMPLE_RATE ="sample_rate";

    }



}
