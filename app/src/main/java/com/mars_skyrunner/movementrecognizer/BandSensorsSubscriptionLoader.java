package com.mars_skyrunner.movementrecognizer;

import android.content.Intent;
import android.content.Context;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;


import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.GsrSampleRate;
import com.microsoft.band.sensors.SampleRate;
import java.util.concurrent.TimeoutException;

import static com.mars_skyrunner.movementrecognizer.MainActivity.client;
import static com.mars_skyrunner.movementrecognizer.MainActivity.sampleButton;

/**
 * Permorm MSBand Sensors suscription by using an AsyncTask to perform the
 * processing
 */

public class BandSensorsSubscriptionLoader extends AsyncTask<Void,Void,ConnectionState> {

    //Tag for log messages
    private static final String LOG_TAG = BandSensorsSubscriptionLoader.class.getName();
    Context mContext ;


    public BandSensorsSubscriptionLoader(Context context){
        mContext = context;
    }


    @Override
    protected ConnectionState doInBackground(Void... voids) {
        ConnectionState answer = null;

        Log.v(LOG_TAG, "BandSensorsSubscriptionLoader doInBackground");

        try {

            ConnectionState clientState = getConnectedBandClient();

            if (ConnectionState.CONNECTED == clientState) {

                answer = ConnectionState.CONNECTED;

                try {

                    client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, getSampleRate("31"));

                } catch (BandIOException e) {
                    e.printStackTrace();
                    appendToUI("Sensor reading error", Constants.ACCELEROMETER);
                }


                try {

                    client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener, getSampleRate("31"));

                } catch (BandIOException e) {
                    e.printStackTrace();
                    appendToUI("Sensor reading error", Constants.GYROSCOPE);
                }

                appendToUI(answer.toString(), Constants.BAND_STATUS);


            } else {

                answer = clientState;

            }

            Log.v(LOG_TAG,"answer.toString(): " + answer.toString());

        } catch (BandException e) {

            String exceptionMessage = "";

            switch (e.getErrorType()) {
                case UNSUPPORTED_SDK_VERSION_ERROR:
                    Log.e(LOG_TAG,"Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.");
                    exceptionMessage = "SDK Version unsupported";
                    break;
                case SERVICE_ERROR:
                    Log.e(LOG_TAG,"Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.");
                    exceptionMessage = "Microsoft Health BandService is not available.";
                    break;
                default:
                    exceptionMessage = "Unknown error occured: " + e.getMessage() ;
                    break;
            }

            Log.e(LOG_TAG, exceptionMessage);
            appendToUI(exceptionMessage, Constants.BAND_STATUS);

            Log.e(LOG_TAG,"BandException: " + e.toString());

        } catch (Exception e) {
            Log.e(LOG_TAG, "BandSensorsSubscriptionTask: " + e.getMessage());



        }



        return answer;

    }




    private void appendToUI(String value, String sensor) {
        Log.v(LOG_TAG,"appendToUI: sensor " + sensor + " , value : " + value);

        Intent appendToUiIntent = new Intent(Constants.DISPLAY_VALUE);
        appendToUiIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        appendToUiIntent.putExtra(Constants.SENSOR,sensor);
        appendToUiIntent.putExtra(Constants.VALUE,value);
        mContext.sendBroadcast(appendToUiIntent);
    }

    private ConnectionState getConnectedBandClient() throws InterruptedException, BandException {

        Log.v(LOG_TAG, "getConnectedBandClient");

        Log.v(LOG_TAG, "client == null");
        BandInfo[] devices = BandClientManager.getInstance().getPairedBands();

        if (devices.length == 0) {

            Log.v(LOG_TAG, "devices.length == 0");

            return client.getConnectionState();

        } else {
            Log.v(LOG_TAG, "devices.length =! 0");
        }

        client = BandClientManager.getInstance().create(mContext, devices[0]);

        Log.v(LOG_TAG, "pairedBand : " + devices[0].getName());

        appendToUI("Band is connecting...", Constants.BAND_STATUS);


        ConnectionState state  = client.getConnectionState();

        try {
            state = client.connect().await(1,java.util.concurrent.TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            Log.e(LOG_TAG,"TimeoutException: " + e.toString());
        }

        return state;
    }



    private GsrSampleRate getGsrSampleRate(String sampleRateSelection) {

        GsrSampleRate sampleRate = null;

   /*
                            * MS200: A value representing a sample rate of every 200 milliseconds
                              MS5000 : A value representing a sample rate of every 5000 milliseconds
                            * */

        String answer = "";

        switch (sampleRateSelection){
            case "5": // = 1 / 0.2 s
                sampleRate = GsrSampleRate.MS200;
                answer = "GsrSampleRate.MS200";

                break;
            case "0.2": // = 1 / 5 s
                sampleRate = GsrSampleRate.MS5000;
                answer = "GsrSampleRate.MS5000";
                break;
        }

        Log.w(LOG_TAG,"getGsrSampleRate : " + answer);
        return sampleRate;
    }


    private SampleRate getSampleRate(String sampleRateSelection) {

        SampleRate sampleRate = null;
                 /*
                            *  MS128 : A value representing a sample rate of every 128 milliseconds
                               MS16 : A value representing a sample rate of every 16 milliseconds
                               MS32 : A value representing a sample rate of every 32 milliseconds

                            * */

        String answer ="";

        switch (sampleRateSelection){
            case "8": // = 1 / 0.128 s
                sampleRate = SampleRate.MS128;
                answer = "SampleRate.MS128";
                break;
            case "31":// = 1 / 0.032 s
                sampleRate = SampleRate.MS32;
                answer = "SampleRate.MS32";
                break;
            case "62":// = 1 / 0.016 s
                sampleRate = SampleRate.MS16;
                answer = "SampleRate.MS16";
                break;
        }

        Log.w(LOG_TAG,"getSampleRate : " + answer);
        return sampleRate;
    }



    private String getSensorSamplingRate(int sensorID){

        String sampleRate = "";

        //if the sensor is Heart Rate, Skin Temp.,UV,Barometer or Altimeter, sample rate is 1hz
        //if the sensor is Ambient light, sample rate is 2hz
        // else , value change

        switch (sensorID) {
            case Constants.HEART_RATE_SENSOR_ID:
                sampleRate = "1"; // hz
                break;

            case Constants.RR_INTERVAL_SENSOR_ID:
                sampleRate = "Value change";
                break;

            case Constants.ACCELEROMETER_SENSOR_ID:
                sampleRate = "31";  // hz
                break;

            case Constants.ALTIMETER_SENSOR_ID:
                sampleRate = "1"; // hz
                break;


            case Constants.AMBIENT_LIGHT_SENSOR_ID:
                sampleRate = "2"; // hz
                break;

            case Constants.BAROMETER_SENSOR_ID:
                sampleRate = "1"; // hz
                break;

            case Constants.GSR_SENSOR_ID:
                sampleRate = "5" ; // hz
                break;

            case Constants.CALORIES_SENSOR_ID:
                sampleRate = "Value change";
                break;

            case Constants.DISTANCE_SENSOR_ID:
                sampleRate = "Value change";
                break;

            case Constants.BAND_CONTACT_SENSOR_ID:
                sampleRate = "Value change";
                break;

            case Constants.GYROSCOPE_SENSOR_ID:
                sampleRate = "31"; // hz
                break;

            case Constants.PEDOMETER_SENSOR_ID:
                sampleRate = "Value change";
                break;

            case Constants.SKIN_TEMPERATURE_SENSOR_ID:
                sampleRate = "1"; // hz
                break;

            case Constants.UV_LEVEL_SENSOR_ID:
                sampleRate = "1"; // hz
                break;

        }


        return sampleRate;

    }


    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
            if (event != null) {

                if (sampleButton.isChecked()) {

                    String sensorValue = event.getAngularVelocityX() + "," + event.getAngularVelocityY() + "," + event.getAngularVelocityZ();

                    //1.ωX = in  °/s
                    //2.ωY = in  °/s
                    //3.ωZ = in  °/s

                    Log.v(LOG_TAG,"mGyroscopeEventListener: ");
                    Log.v(LOG_TAG," event.getAngularVelocityX(): " +   event.getAngularVelocityX());
                    Log.v(LOG_TAG," event.getAngularVelocityY(): " +   event.getAngularVelocityY());
                    Log.v(LOG_TAG," event.getAngularVelocityZ(): " +   event.getAngularVelocityZ());

                    createSensorReadingObject(Constants.GYROSCOPE_SENSOR_ID, sensorValue, getSensorSamplingRate(Constants.GYROSCOPE_SENSOR_ID));
                }


            }
        }
    };




    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {



        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {

                if (sampleButton.isChecked()) {

                    String sensorValue = event.getAccelerationX() + "," + event.getAccelerationY() + "," + event.getAccelerationZ();

                    //1. X in g's
                    //2. Y in g's
                    //3. Z in g's

                    Log.v(LOG_TAG,"mAccelerometerEventListener: ");
                    Log.v(LOG_TAG," event.getAccelerationX(): " +   event.getAccelerationX());
                    Log.v(LOG_TAG," event.getAccelerationY(): " +   event.getAccelerationY());
                    Log.v(LOG_TAG," event.getAccelerationZ(): " +   event.getAccelerationZ());

                    createSensorReadingObject(Constants.ACCELEROMETER_SENSOR_ID, sensorValue, getSensorSamplingRate(Constants.ACCELEROMETER_SENSOR_ID));
                }

            }
        }
    };

    private void createSensorReadingObject(int sensorID , String sensorValue, String sensorSampleRate){


        long currentTime = System.currentTimeMillis();

        SensorReading sensorReading = new SensorReading(mContext,sensorID,sensorValue,sensorSampleRate,currentTime);

        Intent sendObjectIntent = new Intent(mContext, DbInsertionService.class);
        sendObjectIntent.putExtra(Constants.SERVICE_EXTRA,sensorReading);
        mContext.startService(sendObjectIntent);

    }


    @Override
    protected void onPostExecute(ConnectionState cs) {
        super.onPostExecute(cs);

        Log.v(LOG_TAG, "bandSensorSubscriptionLoader: onLoadFinished ");

        String userMsg = "";

        if (cs != null) {
            Log.v(LOG_TAG, "cs != null");
            Log.v(LOG_TAG, cs.toString());

            switch (cs) {

                case CONNECTED:

                  //  userMsg = "Band is bound to MS Health's band communication service and connected to its corresponding MS Band";

                    break;

                case BOUND:
                    //userMsg = "Band is bound to MS Health's band comm. service";
                    break;

                case BINDING:
                   // userMsg = "Band is binding to MS Health's band comm. service";
                    break;

                case UNBOUND:
                   // userMsg = "Band is not bound to MS Health's band comm. service";
                    break;

                case DISPOSED:
                  //  userMsg = "Band has been disposed of by MS Health's band comm. service";
                    break;

                case UNBINDING:
                  //  userMsg = "Band is unbinding from MS Health's band comm. service";
                    break;

                case INVALID_SDK_VERSION:
                  //  userMsg = "MS Health band comm. service version mismatch";
                    break;

                default:
               //     userMsg = "Band Suscription failed.";
                    break;

            }

            userMsg = cs.toString();

            Log.v(LOG_TAG, userMsg);

            appendToUI(userMsg, Constants.BAND_STATUS);

        } else {

            //Band isnt paired with phone
            appendToUI("Band isn't paired with your phone.", Constants.BAND_STATUS);
            Log.v(LOG_TAG, "cs == null");
            Log.v(LOG_TAG, "Band isn't paired with your phone.");

        }
    }

    public static void unregisterListeners(){
        try {
            client.getSensorManager().unregisterAccelerometerEventListeners();
            client.getSensorManager().unregisterGyroscopeEventListeners();
        } catch (BandIOException e) {
            e.printStackTrace();
        }

    }
}