package com.mars_skyrunner.movementrecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mars_skyrunner.movementrecognizer.data.SensorReadingContract;
import com.microsoft.band.BandClient;
import com.microsoft.band.ConnectionState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * MainActivity Activity recognizes movements using an accelerometer and gyroscope values coming from sensors embedded on a Microsoft Band 2
 * wrist band. The recognition is made using an offline execution, meaning that the system records the sensor data first using
 * a SQLite Database. The recognition is performed afterwards, by training a SVM model using the libsvm library by Chih-Chung Chang and Chih-Jen Lin.
 * @author  Marcial Sandoval Gastelum
 * @version 1.1
 * @since   2020-09-08
 */

public class MainActivity extends AppCompatActivity {

    private String LOG_TAG = MainActivity.class.getSimpleName();
    public static BandClient client = null;
    public static ToggleButton sampleButton;
    public static long TIMER_DURATION = 4;
    public int SVM_SAMPLE_SIZE = 100;
    TextView clockTV, loaderTV;
    FutureTask task = null;
    public static long timeBasedCSVDate = 0;
    ArrayList<Long> sampleTimeStamps;
    int sampleTimeStampsIterator;
    private Long timeStampReference;
    private ArrayList<String> sampleDataset = new ArrayList<>();
    svm_model model;
    ImageView upIV, dwnIV, clockIV;
    boolean bandConnectionState = false;
    TextView predictionTextView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {

            case R.id.refresh:

                //Restart BandSensorsSubscription Asyntask
                BandSensorsSubscriptionLoader sensortask = new BandSensorsSubscriptionLoader(this);
                BandSensorsSubscriptionLoader.unregisterListeners();
                sensortask.execute();

                return true;
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Executes BandSensorsSubscription Asyntask
        BandSensorsSubscriptionLoader sensortask = new BandSensorsSubscriptionLoader(this);
        sensortask.execute();

        //Gets the apps root storage directory.
        File dir = getOutputDirectory();

        //Using the training dataset previously saved on the root directory, a SVM Model is trained and retrieved.
        model = getMovementModel(dir);

        loaderTV = findViewById(R.id.loader_tv);
        predictionTextView = (TextView) findViewById(R.id.prediction);
        upIV = findViewById(R.id.arm_up_iv);
        dwnIV = findViewById(R.id.arm_down_iv);
        clockIV = findViewById(R.id.clock_iv);
        clockTV = findViewById(R.id.minutes);

        Log.v(LOG_TAG, "MODEL TRAINED");

        sampleButton = (ToggleButton) findViewById(R.id.sample_btn);
        sampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (bandConnectionState) {
                    upIV.setVisibility(View.GONE);
                    dwnIV.setVisibility(View.GONE);

                    clockIV.setVisibility(View.GONE);
                    clockTV.setVisibility(View.VISIBLE);

                    sampleButton.setEnabled(false);

                    showLoadingView(true);
                    loaderTV.setText("sampling...");
                    predictionTextView.setText("");

                    Log.v(LOG_TAG, "sampleButton setOnClickListener");
                    clockTV.setText("" + TIMER_DURATION);
                    task = new FutureTask(new CounterCallable(MainActivity.this, 0, TIMER_DURATION, 1));

                    ExecutorService pool = Executors.newSingleThreadExecutor();
                    pool.submit(task);
                    pool.shutdown();

                    timeBasedCSVDate = System.currentTimeMillis();

                } else {
                    sampleButton.setChecked(false);
                    Toast.makeText(MainActivity.this, "Lost connection with MS Band.", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }


    private BroadcastReceiver timeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            long seconds = intent.getExtras().getLong(getClass().getPackage() + ".TIME");
            clockTV.setText("" + (TIMER_DURATION - seconds));

            Log.v(LOG_TAG, " timeReceiver seconds: " + seconds);

            if (seconds == TIMER_DURATION) {

                Log.v(LOG_TAG, "timeReceiver  seconds == TIMER_DURATION ");
                loaderTV.setText("processing...");

                resetTimer();

                clockIV.setVisibility(View.VISIBLE);
                clockTV.setVisibility(View.GONE);

                sampleButton.setEnabled(true);
                sampleButton.setChecked(false);

                Bundle extraBundle = new Bundle();
                extraBundle.putLong(Constants.SENSOR_TIME, timeBasedCSVDate);


                getLoaderManager().restartLoader(Constants.CREATE_CSV_LOADER, extraBundle, saveDataCursorLoader);

            }

        }


    };

    private void resetTimer() {

        clockTV.setText("");

    }


    private BroadcastReceiver displayVaueReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String value = intent.getStringExtra(Constants.VALUE);

            Log.v(LOG_TAG, "displayVaueReceiver: value: " + value);

            if (value.equals(ConnectionState.CONNECTED.toString())) {
                bandConnectionState = true;
            } else {
                bandConnectionState = false;
            }

            appendToUI(value);

        }


    };


    /**
     * Helper method to show loader on screen.
     *
     * @param loadingState set to true to show loader on screen, false otherwise.
     */
    private void showLoadingView(boolean loadingState) {

        if (loadingState) {
            //  findViewById(R.id.main_layout).setVisibility(View.GONE);
            findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
        } else {
            //findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }

    }


    /**
     * This method retrieves the root storage directory from MovementRecognizer app.
     * If the directory does not exist, it creates it. This is the same directory
     * on which every output file will be stored.
     * @return File Containing MovementRecognizer app root storage directory.
     */
    private File getOutputDirectory() {

        File directory = null; // In here, the return value directory file is going to be stored.

        //Checks whether the android device has an external storage, if it does, the root directory
        //is going to be retrieved/created from there. If it does not, the root directory
        //is going to be retrieved/created on the internal memory.

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {

            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();

            directory = new File(baseDir
                    + "/MovementRecognizer/");

            // if no directory exists, create new directory
            if (!directory.exists()) {
                directory.mkdir();
            }

        }

        return directory;

    }

    /**
     * This method trains a support vector machine model. The training dataset is stored previously
     * on the MovementRecognizer apps root file directory. The svm model training is made using the
     * the libsvm library by Chih-Chung Chang and Chih-Jen Lin.
     *
     * @param dir MovementRecognizer apps root file directory.
     * @return svm_model Trained SVM model for movement classifying.
     */
    private svm_model getMovementModel(File dir) {

        //Retrieve train dataset features
        File trainDataFile = new File(dir, "trainDataset.csv");
        //Maps train dataset features to an arraylist
        ArrayList<ArrayList<String>> trainDataset = getTrainDataset(trainDataFile);

        //Retrieve train dataset labels
        File trainLabelsFile = new File(dir, "trainLabels.csv");

        //Maps train dataset labels to an arraylist
        ArrayList<String> labelsDataset = getTrainLabelsDataset(trainLabelsFile);

        int variables = trainDataset.get(0).size();
        svm_problem prob = new svm_problem();
        prob.l = trainDataset.size();//LENGTH
        prob.y = new double[prob.l];//LABELS
        prob.x = new svm_node[prob.l][variables];//SAMPLES

        for (int i = 0; i < prob.l; i++)//SAMPLE
        {

            ArrayList<String> sample = trainDataset.get(i);
            prob.y[i] = Double.parseDouble(labelsDataset.get(i).trim());

            for (int j = 0; j < variables; j++) { //FEATURE

                double featureValue = Double.parseDouble(sample.get(j).trim());

                prob.x[i][j] = new svm_node();
                prob.x[i][j].index = j;
                prob.x[i][j].value = featureValue;

            }

        }

        svm_parameter param = new svm_parameter();

        // default values
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.LINEAR;
        param.C = 0.03125;
        param.degree = 2;
        param.gamma = 0;
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 40;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];

        // build model & classify
        svm_model model = svm.svm_train(prob, param);

        return model;
    }

    /**
     * This method maps the train dataset features from a .csv file to an Arraylist.
     * It reads each row from the .csv file and convert it into an ArrayList<String> sample,
     * each value in this ArrayList<String> form represents a feature of that sample.
     *
     * @param trainDataFile .csv file containing the train dataset features.
     * @return  ArrayList<ArrayList<String>> An arraylist containing each sample from the training
     * dataset in the form of an Arraylist<String>.
     */
    private ArrayList<ArrayList<String>> getTrainDataset(File trainDataFile) {

        ArrayList<ArrayList<String>> trainDataset = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(new FileReader(trainDataFile));
            String line;

            while ((line = br.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(line, ",");

                ArrayList<String> sample = new ArrayList<>();

                while (st.hasMoreTokens()) {
                    sample.add(st.nextToken());
                }

                trainDataset.add(sample);

            }

            br.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "BufferedReader : IOException : " + e.toString());
        }

        return trainDataset;

    }

    /**
     * This method maps the train dataset labels from a .csv file to an Arraylist.
     * It reads each row from the .csv file and convert it into a String value to be
     * added to the outputs Arraylist<String>.
     *
     * @param trainLabelsFile .csv file containing the train dataset labels.
     * @return  ArrayList<String> An arraylist containing each samples label from the training
     * dataset.
     */
    private ArrayList<String> getTrainLabelsDataset(File trainLabelsFile) {

        ArrayList<String> trainLabelsDataset = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(new FileReader(trainLabelsFile));
            String line;

            while ((line = br.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(line, ",");

                while (st.hasMoreTokens()) {
                    trainLabelsDataset.add(st.nextToken());
                }

            }

            br.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "BufferedReader : IOException : " + e.toString());
        }

        return trainLabelsDataset;
    }

    /**
     * This method displays a message on the band status text view.
     *
     * @param value Message to be displayed.
     */
    private void appendToUI(String value) {

        TextView textView = (TextView) findViewById(R.id.stts);
        textView.setText(value);

    }


    private LoaderManager.LoaderCallbacks<Cursor> saveDataCursorLoader
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

            Log.v(LOG_TAG, "SAMPLE BASED CSV");

            // Define a projection that specifies the columns from the table we care about.
            String[] projection3 = {
                    "MAX(" + SensorReadingContract.ReadingEntry.COLUMN_SAMPLE_RATE + ")",
                    SensorReadingContract.ReadingEntry.COLUMN_SENSOR_ID
            };

            String selection3 = SensorReadingContract.ReadingEntry.COLUMN_TIME + ">?";
            //String selection2 = null;

            String selectionArg3 = "" + timeBasedCSVDate;

            String[] selectionArgs3 = {selectionArg3};
            //String[] selectionArgs2 = null;

            String sortOrder3 = SensorReadingContract.ReadingEntry.COLUMN_TIME;
            //String sortOrder2 = ReadingEntry._ID;

            return new CursorLoader(MainActivity.this,   // Parent activity context
                    SensorReadingContract.ReadingEntry.CONTENT_URI,   // Provider content URI to query
                    projection3,             // Columns to include in the resulting Cursor
                    selection3,                   //  selection clause
                    selectionArgs3,                   //  selection arguments
                    sortOrder3);                  //  sort order


        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {

            Log.v(LOG_TAG, "saveDataCursorLoader: onLoadFinished");
            Log.v(LOG_TAG, "SAMPLE BASED CSV");


            try {

                int rowcount = c.getCount();
                int colcount = c.getColumnCount();

                Log.v(LOG_TAG, "rowcount: " + rowcount);
                Log.v(LOG_TAG, "colcount: " + colcount);

                if (rowcount > 0) {

                    c.moveToFirst();
                    c.moveToPosition(0);
                    String maxSampleRate = c.getString(0).trim();
                    String maxSampleRateSensorID = c.getString(1).trim();

                    Log.v(LOG_TAG, "Max Sample Rate Sensor ID : " + maxSampleRateSensorID);

                    Bundle bundle = new Bundle();
                    bundle.putString("maxSampleRate", maxSampleRate);
                    bundle.putString("maxSampleRateSensorID", maxSampleRateSensorID);

                    // Kick off the  loader
                    getLoaderManager().restartLoader(Constants.SAMPLE_BASED_LOADER, bundle, SampleBasedCSVFileLoader);


                    //Save datapoint loader destroyed, so that if user comes back from
                    //CSV file viewer, it does not create a new one
                    getLoaderManager().destroyLoader(Constants.CREATE_CSV_LOADER);

                } else {
                    //Save datapoint loader destroyed, so that if user comes back from
                    //CSV file viewer, it does not create a new one
                    getLoaderManager().destroyLoader(Constants.CREATE_CSV_LOADER);
                    Toast.makeText(MainActivity.this, "No records to export CSV", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "FileWriter IOException: " + e.toString());
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

            Log.v(LOG_TAG, "saveDataCursorLoader: onLoaderReset");

        }

    };


    private LoaderManager.LoaderCallbacks<Cursor> SampleBasedCSVFileLoader
            = new LoaderManager.LoaderCallbacks<Cursor>() {

        //Once the max sample rate has been selected, proceed to retrieve samples timestamps

        Context mContext = MainActivity.this;

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

            Log.v(LOG_TAG, "SampleBasedCSVFileLoader: onCreateLoader");

            // Define a projection that specifies the columns from the table we care about.
            String[] projection = {
                    SensorReadingContract.ReadingEntry._ID,
                    SensorReadingContract.ReadingEntry.COLUMN_TIME,
                    SensorReadingContract.ReadingEntry.COLUMN_SAMPLE_RATE,
                    SensorReadingContract.ReadingEntry.COLUMN_SENSOR_ID,
                    SensorReadingContract.ReadingEntry.COLUMN_SENSOR_VALUE};

            String sortOrder = SensorReadingContract.ReadingEntry._ID;

            // This loader will execute the ContentProvider's query method on a background thread
            //<> : Not equal to

            String selection = SensorReadingContract.ReadingEntry.COLUMN_SAMPLE_RATE + "=? AND " + SensorReadingContract.ReadingEntry.COLUMN_TIME + ">? AND " + SensorReadingContract.ReadingEntry.COLUMN_SENSOR_ID + "=?";

            String saveTimeSelecionArg = "" + timeBasedCSVDate;

            String[] selectionArgs = {bundle.getString("maxSampleRate"), saveTimeSelecionArg, bundle.getString("maxSampleRateSensorID")};

            return new CursorLoader(mContext,   // Parent activity context
                    SensorReadingContract.ReadingEntry.CONTENT_URI,   // Provider content URI to query
                    projection,             // Columns to include in the resulting Cursor
                    selection,                   //  selection clause
                    selectionArgs,                   //  selection arguments
                    sortOrder);                  //  sort order

        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            Log.v(LOG_TAG, "SampleBasedCSVFileLoader: onLoadFinished");

            //sampleTimeStamps stores sample time stamps that are going to be searched for all sensors,
            // in order to concatenate
            // each sensor reading into a single vector for each timestamp

            sampleTimeStamps = new ArrayList<>();

            try {

                int rowcount = c.getCount();

                if (rowcount > 0) {

                    c.moveToFirst();

                    for (int i = 0; i < rowcount; i++) {

                        c.moveToPosition(i);

                        long timeStamp = Long.parseLong(c.getString(1).trim());
                        sampleTimeStamps.add(timeStamp);

                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "FileWriter IOException: " + e.toString());
            }

            timeStampReference = sampleTimeStamps.get(0);

            for (int i = 0; i < sampleTimeStamps.size(); i++) {
                Log.v(LOG_TAG, "CSV TIME STAMP: " + (sampleTimeStamps.get(i) - timeStampReference));
            }

            long minTime = sampleTimeStamps.get(0);
            long maxTime;

            maxTime = sampleTimeStamps.get((0 + 1));

            sampleTimeStampsIterator = 0;

            Bundle bundle = new Bundle();
            bundle.putLong("minTime", minTime);
            bundle.putLong("maxTime", maxTime);


            // Kick off the  loader
            getLoaderManager().restartLoader(Constants.TIME_STAMP_SENSOR_READING_LOADER, bundle, timeStampSensorReadingLoader);

            //Save datapoint loader destroyed, so that if user comes back from
            //CSV file viewer, it does not create a new one
            getLoaderManager().destroyLoader(Constants.SAMPLE_BASED_LOADER);

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(LOG_TAG, "SampleBasedCSVFileLoader: onLoaderReset");
        }

    };


    private LoaderManager.LoaderCallbacks<Cursor> timeStampSensorReadingLoader
            = new LoaderManager.LoaderCallbacks<Cursor>() {


        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

            Log.v(LOG_TAG, "timeStampSensorReadingLoader: onCreateLoader");

            long minTime = bundle.getLong("minTime");
            long maxTime = bundle.getLong("maxTime");

            Log.v(LOG_TAG, "timeStampSensorReadingLoader: minTime: " + minTime);
            Log.v(LOG_TAG, "timeStampSensorReadingLoader: maxTime: " + maxTime);

            if (minTime != maxTime) {

                // Define a projection that specifies the columns from the table we care about.
                String[] projection = {
                        SensorReadingContract.ReadingEntry._ID,
                        SensorReadingContract.ReadingEntry.COLUMN_TIME,
                        SensorReadingContract.ReadingEntry.COLUMN_SAMPLE_RATE,
                        SensorReadingContract.ReadingEntry.COLUMN_SENSOR_ID,
                        SensorReadingContract.ReadingEntry.COLUMN_SENSOR_VALUE};

                String sortOrder = SensorReadingContract.ReadingEntry._ID;

                // This loader will execute the ContentProvider's query method on a background thread

                String selection = SensorReadingContract.ReadingEntry.COLUMN_TIME + ">=?  AND " + SensorReadingContract.ReadingEntry.COLUMN_TIME + "<?";

                String[] selectionArgs = {("" + minTime), ("" + maxTime)};

                return new CursorLoader(MainActivity.this,   // Parent activity context
                        SensorReadingContract.ReadingEntry.CONTENT_URI,   // Provider content URI to query
                        projection,             // Columns to include in the resulting Cursor
                        selection,                   //  selection clause
                        selectionArgs,                   //  selection arguments
                        sortOrder);                  //  sort order

            }

            return null;


        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {

            Log.v(LOG_TAG, "timeStampSensorReadingLoader: onLoadFinished ");

            Bundle b = c.getExtras();

            CursorLoader p = (CursorLoader) loader;
            String[] selArgs = p.getSelectionArgs();
            String timeStamp = selArgs[0];

            ArrayList<Integer> selectedSensorID = getSelectedSensorID();
            String[] sensorReadings = new String[selectedSensorID.size()];

            FileWriter fw2 = null;

            try {

                int rowcount = c.getCount();
                int colcount = c.getColumnCount();

                Log.v(LOG_TAG, "rowcount: " + rowcount);
                Log.v(LOG_TAG, "colcount: " + colcount);

                if (rowcount > 0) {

                    sensorReadings = getSensorReadings(c, selectedSensorID);
                    String values = "";

                    for (int i = 0; i < selectedSensorID.size(); i++) {

                        values += sensorReadings[i] + ",";
//                        values += sensorReadings[i];

                    }


                    String sample = (Long.parseLong(timeStamp.trim()) - timeStampReference) + "," + values;

                    Log.v(LOG_TAG, "timeStampSensorReadingLoader sample:  " + sampleTimeStampsIterator);
                    Log.v(LOG_TAG, "sample:  " + sample);

                    sampleDataset.add(sample);

                } else {

                    Toast.makeText(MainActivity.this, "No existen registros guardados", Toast.LENGTH_SHORT).show();

                }


            } catch (Exception e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "FileWriter IOException: " + e.toString());
            }

            sampleTimeStampsIterator++;

            if (sampleTimeStampsIterator == (sampleTimeStamps.size() - 1)) {

                sampleTimeStampsIterator = 0;
                createSampleBasedCSV();
                //Save datapoint loader destroyed, so that if user comes back from
                //CSV file viewer, it does not create a new one
                getLoaderManager().destroyLoader(Constants.TIME_STAMP_SENSOR_READING_LOADER);

            } else {

                long minTime = sampleTimeStamps.get(sampleTimeStampsIterator);
                long maxTime;

                maxTime = sampleTimeStamps.get((sampleTimeStampsIterator + 1));

                Bundle bundle = new Bundle();
                bundle.putLong("minTime", minTime);
                bundle.putLong("maxTime", maxTime);

                // Kick off the  loader
                getLoaderManager().restartLoader(Constants.TIME_STAMP_SENSOR_READING_LOADER, bundle, timeStampSensorReadingLoader);

            }


        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

            Log.v(LOG_TAG, "timeStampSensorReadingLoader: onLoaderReset");

        }
    };


    private ArrayList<Integer> getSelectedSensorID() {

        ArrayList<Integer> result = new ArrayList<>();

        result.add(Constants.ACCELEROMETER_SENSOR_ID);
        result.add(Constants.GYROSCOPE_SENSOR_ID);

        return result;

    }


    private String[] getSensorReadings(Cursor c, ArrayList<Integer> selectedSensorID) {

        String[] answer = new String[selectedSensorID.size()];

        for (int i = 0; i < selectedSensorID.size(); i++) {

            answer[i] = getReading(c, selectedSensorID.get(i));

        }

        return answer;
    }


    private String getReading(Cursor c, Integer sensorID) {

        /*QUERY
        *                 String[] projection = {
                        ReadingEntry._ID,
                        ReadingEntry.COLUMN_TIME,
                        ReadingEntry.COLUMN_SAMPLE_RATE,
                        ReadingEntry.COLUMN_SENSOR_ID,
                        ReadingEntry.COLUMN_SENSOR_VALUE};

        * */

        int rowcount = c.getCount();
        c.moveToFirst();

        String reading;

        Log.v(LOG_TAG, "getReading sensorID: " + sensorID);

        if (sensorID == Constants.ACCELEROMETER_SENSOR_ID || sensorID == Constants.GYROSCOPE_SENSOR_ID) {

            reading = "NaN,NaN,NaN";

        } else {

            reading = "NaN";

        }

        for (int i = 0; i < rowcount; i++) {

            c.moveToPosition(i);

            String currentID = c.getString(3);
            String interestID = "" + sensorID;

            if (currentID.equals(interestID)) {

                reading = c.getString(4);

            }

        }

        return reading;
    }


    private void createSampleBasedCSV() {

        File outputDirectory = getOutputDirectory();
        String srLabel = "sample.csv";
        File saveFile = getCsvOutputFile(outputDirectory, srLabel);

        try {
            FileWriter fw = new FileWriter(saveFile);
            BufferedWriter bw = new BufferedWriter(fw);


            for (int i = 0; i < sampleDataset.size(); i++) {

                bw.write(sampleDataset.get(i));
                bw.newLine();
            }

            bw.flush();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "FileWriter IOException: " + e.toString());
        }

        Log.w(LOG_TAG, "Datapoint Exported Successfully.");

        ArrayList<String> sample = formatSampleForSVM();
        svm_node[] test = new svm_node[sample.size()];

        for (int j = 0; j < sample.size(); j++) { //FEATURE

            double featureValue = Double.parseDouble(sample.get(j).trim());

            test[j] = new svm_node();
            test[j].index = j;
            test[j].value = featureValue;

        }

        double prediction = svm.svm_predict(model, test);
        Log.w(LOG_TAG, "prediction: " + prediction);

        int backgroundColor;
        String predictionText;


        if (prediction == 0.0) {
            predictionText = "UP";
            dwnIV.setVisibility(View.GONE);
            upIV.setVisibility(View.VISIBLE);
            //backgroundColor = getResources().getColor(R.color.up);
        } else {
            predictionText = "DOWN";
            dwnIV.setVisibility(View.VISIBLE);
            upIV.setVisibility(View.GONE);
            //backgroundColor = getResources().getColor(R.color.down);
        }


        //predictionTextView.setBackgroundColor(backgroundColor);
        predictionTextView.setText(predictionText);

        showLoadingView(false);

        //If SaveDatapoint button is clicked while MSBand still connected, it saves the
        //newest values
        sampleDataset.clear();

        getLoaderManager().destroyLoader(Constants.TIME_STAMP_SENSOR_READING_LOADER);

    }


    private File getCsvOutputFile(File dir, String filename) {

        return new File(dir, filename);
    }

    private ArrayList<String> formatSampleForSVM() {

        if (sampleDataset.size() > SVM_SAMPLE_SIZE) {

            int randomNumbers = sampleDataset.size() - SVM_SAMPLE_SIZE;
            ArrayList<Integer> exceededIndex = getIndexToDelete(randomNumbers);

            for (int i = 0; i < exceededIndex.size(); i++) {

                sampleDataset.remove(i);

            }


        } else {

            if (sampleDataset.size() < SVM_SAMPLE_SIZE) {

                int rowsToAdd = SVM_SAMPLE_SIZE - sampleDataset.size();

                for (int i = 0; i < rowsToAdd; i++) {

                    sampleDataset.add("0,0,0,0,0,0,0");

                }

            }

        }


        Log.v(LOG_TAG, "sampleDataset.size() : " + sampleDataset.size());


        ArrayList<ArrayList<String>> tokenizedSampleDataset = new ArrayList<>();


        for (int i = 0; i < sampleDataset.size(); i++) {//SAMPLES

            ArrayList<String> sampleToken = new ArrayList<>();

            String sample = sampleDataset.get(i);

            StringTokenizer st = new StringTokenizer(sample, ",");

            while (st.hasMoreTokens()) {
                sampleToken.add(st.nextToken());
            }

            tokenizedSampleDataset.add(sampleToken);

        }

        ArrayList<String> answer = new ArrayList<>();

        for (int i = 0; i < tokenizedSampleDataset.get(0).size(); i++) {//FEATURES

            for (int j = 0; j < tokenizedSampleDataset.size(); j++) {

                answer.add(tokenizedSampleDataset.get(j).get(i));

            }
        }

        return answer;
    }


    private ArrayList<Integer> getIndexToDelete(int randomNumbers) {

        Log.v(LOG_TAG, "getIndexToDelete");
        //101	115	56	9	62	23	35	113	19	88	39	53	12	7	94	8	108	42	46	37	4	74	36

        ArrayList<Integer> indexToDelete = new ArrayList<>();

        ArrayList<Integer> list = new ArrayList<Integer>();

        for (int i = 0; i < sampleDataset.size(); i++) {
            list.add(i);
        }

        Random rand = new Random();

        while (indexToDelete.size() < randomNumbers) {
            int index = rand.nextInt(list.size());
            indexToDelete.add(list.get(index));
            list.remove(index);
        }

        return indexToDelete;

    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(displayVaueReceiver);
        unregisterReceiver(timeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BandSensorsSubscriptionLoader.unregisterListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Register broadcast receiver to print values on screen from BandSensorsSubscriptionLoader
        registerReceiver(displayVaueReceiver, new IntentFilter(Constants.DISPLAY_VALUE));

        //Register broadcast receiver to display timer
        registerReceiver(timeReceiver, new IntentFilter(getClass().getPackage() + ".BROADCAST"));
    }
}
