package com.mars_skyrunner.movementrecognizer;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;

/**
 * SensorReading class represent a capture of a sensor state of the Microsoft Band 2.
 * @author Marcial Sandoval Gastelum
 * @version 1.1
 * @since 2020-09-08
 */
public class SensorReading  implements  Parcelable{


    //Object attributes
    String mSensorReadingRate;
    int mSensorID;
    String mSensorReading;
    Context mContext;
    long mSensorReadingTime;

    /**
     * This constructor creates a SensorReading instance.
     * @param context of the activity that created the instance.
     * @param sensorID of the sensor that made the reading.
     * @param sensorReading value of the sensor
     * @param rate sensors sampling rate
     * @param time timestamp of the sensor reading
     *
     */
    public SensorReading(Context context, int sensorID, String sensorReading, String rate, long time){

        mContext = context;
        mSensorID = sensorID;
        mSensorReading = sensorReading;
        mSensorReadingRate = rate;
        mSensorReadingTime = time;
    }

    protected SensorReading(Parcel in) {
        mSensorReadingRate = in.readString();
        mSensorID = in.readInt();
        mSensorReading = in.readString();
        mSensorReadingTime = in.readLong();
    }

    public static final Creator<SensorReading> CREATOR = new Creator<SensorReading>() {
        @Override
        public SensorReading createFromParcel(Parcel in) {
            return new SensorReading(in);
        }

        @Override
        public SensorReading[] newArray(int size) {
            return new SensorReading[size];
        }
    };


    /**
     * This method retrieves the sensor name.
     * @return  String sensor label
     */
    public String getSensorName() {

        switch(mSensorID){

            case Constants.HEART_RATE_SENSOR_ID:

                return Constants.HEART_RATE_SENSOR_LABEL;

            case Constants.RR_INTERVAL_SENSOR_ID:

                return Constants.RR_INTERVAL_SENSOR_LABEL;

            case Constants.ACCELEROMETER_SENSOR_ID:

                return Constants.ACCELEROMETER_SENSOR_LABEL;

            case Constants.ALTIMETER_SENSOR_ID:

                return Constants.ALTIMETER_SENSOR_LABEL;

            case Constants.AMBIENT_LIGHT_SENSOR_ID:

                return Constants.AMBIENT_LIGHT_SENSOR_LABEL;

            case Constants.BAROMETER_SENSOR_ID:

                return Constants.BAROMETER_SENSOR_LABEL;

            case Constants.GSR_SENSOR_ID:

                return Constants.GSR_SENSOR_LABEL;

            case Constants.CALORIES_SENSOR_ID:

                return Constants.CALORIES_SENSOR_LABEL;

            case Constants.DISTANCE_SENSOR_ID:

                return Constants.DISTANCE_SENSOR_LABEL;

            case Constants.GYROSCOPE_SENSOR_ID:

                return Constants.GYROSCOPE_SENSOR_LABEL;

            case Constants.PEDOMETER_SENSOR_ID:

                return Constants.PEDOMETER_SENSOR_LABEL;

            case Constants.SKIN_TEMPERATURE_SENSOR_ID:

                return Constants.SKIN_TEMPERATURE_SENSOR_LABEL;

            case Constants.UV_LEVEL_SENSOR_ID:

                return Constants.UV_LEVEL_SENSOR_LABEL;

            case Constants.BAND_STATUS_SENSOR_ID:

                return Constants.BAND_STATUS_SENSOR_LABEL;

            case Constants.BAND_CONTACT_SENSOR_ID:

                return Constants.BAND_CONTACT_SENSOR_LABEL;





        }

        return null;
    }

    /**
     * This method retrieves the sensor value.
     * @return  String sensor value
     */
    public String getSensorReading() {
        return mSensorReading;
    }

    /**
     * This method retrieves updates the sensor value.
     * @param mSensorReading new sensor value
     */
    public void setSensorReading(String mSensorReading) {
        this.mSensorReading = mSensorReading;
    }

    /**
     * This method retrieves the sensor reading timestamp.
     * @return  long sensor reading timestamp.
     */
    public long getSensorTime() {
        return mSensorReadingTime;
    }


    /**
     * This method retrieves the sensors sampling rate.
     * @return  String sensors sampling rate.
     */
    public String getSensorReadingRate() {
        return mSensorReadingRate;
    }

    /**
     * This method retrieves the sensors readings year from the sensors reading timestamp.
     * @return  String Sensors readings year from the sensors reading timestamp.
     */
    public String getSensorReadingYear() {

        return new SimpleDateFormat("yyyy").format(mSensorReadingTime);
    }

    /**
     * This method retrieves the sensors readings month from the sensors reading timestamp.
     * @return  String Sensors readings month from the sensors reading timestamp.
     */
    public String getSensorReadingMonth() {
        return  new SimpleDateFormat("MM").format(mSensorReadingTime);
    }

    /**
     * This method retrieves the sensors readings day from the sensors reading timestamp.
     * @return  String Sensors readings day from the sensors reading timestamp.
     */
    public String getSensorReadingDay() {
        return  new SimpleDateFormat("dd").format(mSensorReadingTime);
    }

    /**
     * This method retrieves the sensors readings hour from the sensors reading timestamp.
     * @return  String Sensors readings hour from the sensors reading timestamp.
     */
    public String getSensorReadingHH() {
        return new SimpleDateFormat("HH").format(mSensorReadingTime);
    }


    /**
     * This method retrieves the sensors readings month from the sensors reading timestamp.
     * @return  String Sensors readings month from the sensors reading timestamp.
     */
    public String getSensorReadingMM() {
        return new SimpleDateFormat("mm").format(mSensorReadingTime);
    }

    /**
     * This method retrieves the sensors readings seconds from the sensors reading timestamp.
     * @return  String Sensors readings seconds from the sensors reading timestamp.
     */
    public String getSensorReadingSS() {
        return new SimpleDateFormat("ss").format(mSensorReadingTime);
    }

    /**
     * This method retrieves the sensors unique id.
     * @return  int Sensors unique id.
     */
    public int  getSensorID() {
        return mSensorID;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mSensorReadingRate);
        parcel.writeInt(mSensorID);
        parcel.writeString(mSensorReading);
        parcel.writeLong(mSensorReadingTime);
    }
}