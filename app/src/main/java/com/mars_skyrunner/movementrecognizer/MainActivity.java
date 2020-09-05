package com.mars_skyrunner.movementrecognizer;

import androidx.appcompat.app.AppCompatActivity;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import umich.cse.yctung.androidlibsvm.LibSVM;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.microsoft.band.BandClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.FutureTask;

public class MainActivity extends AppCompatActivity {


    private String LOG_TAG = MainActivity.class.getSimpleName();
    public static BandClient client = null;
    public static ToggleButton sampleButton ;
    public static long TIMER_DURATION = 4;
    public int SVM_SAMPLE_SIZE = 100;
    TextView clock ;
    FutureTask task = null;
    public static long timeBasedCSVDate = 0;
    ArrayList<Long> sampleTimeStamps;
    int sampleTimeStampsIterator;
    private Long timeStampReference;
    public static String labelPrefix = "up_";
    private ArrayList<String> sampleDataset = new ArrayList<>();
    svm_model model;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File dir = getOutputDirectory();
        model = getMovementModel(dir);

        Log.v(LOG_TAG,"MODEL TRAINED");


    }

    /**
     *Helper method to show loader on screen.
     *
     * @param loadingState  set to true to show loader on screen, false otherwise.
     */
    private void showLoadingView(boolean loadingState) {

        if (loadingState) {
            findViewById(R.id.main_layout).setVisibility(View.GONE);
            findViewById(R.id.loading_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.main_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.loading_layout).setVisibility(View.GONE);
        }

    }


    /**
     *Creates directory to store output files.
     */
    private File getOutputDirectory() {

        File directory = null; // return value

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

    private svm_model getMovementModel(File dir) {

        File trainDataFile = new File(dir,"trainDataset.csv");
        ArrayList<ArrayList<String>> trainDataset = getTrainDataset(trainDataFile);

        File trainLabelsFile = new File(dir,"trainLabels.csv");
        ArrayList<String> labelsDataset = getTrainLabelsDataset(trainLabelsFile);

        int variables = trainDataset.get(0).size();
        svm_problem prob = new svm_problem();
        prob.l = trainDataset.size();//LENGTH
        prob.y = new double[  prob.l ];//LABELS
        prob.x = new svm_node[prob.l][variables];//SAMPLES

        for(int i=0;i<prob.l;i++)//SAMPLE
        {

            ArrayList<String> sample = trainDataset.get(i);
            prob.y[i] = Double.parseDouble(labelsDataset.get(i).trim());

            for(int j = 0 ; j < variables ; j++){ //FEATURE

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
        //            Log.v(LOG_TAG,"MODEL TRAINED");

        return model;
    }



    private ArrayList<ArrayList<String>> getTrainDataset(File trainDataFile) {

        ArrayList<ArrayList<String>> trainDataset = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(new FileReader(trainDataFile));
            String line;

            while ((line = br.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(line,",");

                ArrayList<String> sample = new ArrayList<>();

                while (st.hasMoreTokens()) {
                    sample.add(st.nextToken());
                }

                trainDataset.add(sample);

            }

            br.close();
        }
        catch (IOException e) {
            Log.e(LOG_TAG,"BufferedReader : IOException : " + e.toString());
        }

        return trainDataset;

    }


    private ArrayList<String> getTrainLabelsDataset(File trainLabelsFile) {

        ArrayList<String> trainLabelsDataset = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(new FileReader(trainLabelsFile));
            String line;

            while ((line = br.readLine()) != null) {

                StringTokenizer st = new StringTokenizer(line,",");

                while (st.hasMoreTokens()) {
                    trainLabelsDataset.add(st.nextToken());
                }

            }

            br.close();
        }
        catch (IOException e) {
            Log.e(LOG_TAG,"BufferedReader : IOException : " + e.toString());
        }

        return trainLabelsDataset;
    }


}
