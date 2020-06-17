package com.sps.android_quirk_locater;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link frag1#newInstance} factory method to
 * create an instance of this fragment.
 */


public class frag1 extends Fragment implements View.OnClickListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    // Button array
    Button buttons[] = new Button[8];

    // Floating button
    private FloatingActionButton fab_locate;
    private FloatingActionButton fab_train;

    private Button clearbutton;

    // Training parameters
    private int[] training_Cell = new int[8];
    private int[] done_Cell = new int[8];

    private WifiManager wifiManager;

//    private int trainTimes = 50;
//    private int interMeasurementTime = 100;

    // Database
    private DatabaseHelper mDatabaseHelper;
    DatabaseHelper db_PMF;

    /* number of iterations during training/testing */
    private int trainTimes = 100;
    private int testTimes = 30;

    private int currentCell = 0;
    //todo: change start cell to 0

    /* keep track of scans done so far */
    // important: needs to start at 1
    private int scanCounter = 1;
    private int testcounter = 1;

    /* current operation mode, false = training, true = testing */
    private boolean testing = false;

    // Probability to be in a cell
    private double[] P_Cell = new double[8];

    private ArrayList<double[]> results = new ArrayList<double[]>();

    //private HashMap<String, ArrayList<Double>> result_probabilities_table = new HashMap<String, ArrayList<Double>>();

    // getActivity().getApplicationContext()
    // https://stackoverflow.com/questions/8215308/using-context-in-a-fragment

    public frag1() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment frag1.
     */
    // TODO: Rename and change types and number of parameters
    public static frag1 newInstance(String param1, String param2) {
        frag1 fragment = new frag1();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        /* When a new scan is completed the following method is executed */
        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                Log.d("new scan received", "scan iteration: " + scanCounter + "\n");
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    //Log.d("new scan success", "new scan" + "\n");
                    Log.d("Scanning", "Success");
                    //scanSuccess(); // TODO: uncomment
                } else {
                    Log.d("new scan failed", "no new scan" + "\n");
                    //scanFailure();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getActivity().getApplicationContext().registerReceiver(wifiScanReceiver, intentFilter);
        /* Initialize each cell probability to be equal (= 1/8) */
        for (int i = 0; i < 8; i++) {
            P_Cell[i] = 1.0/8.0;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_frag1, container, false);

        buttons[0] = (Button) rootView.findViewById(R.id.button_cell_0);
        buttons[1] = (Button) rootView.findViewById(R.id.button_cell_1);
        buttons[2] = (Button) rootView.findViewById(R.id.button_cell_2);
        buttons[3] = (Button) rootView.findViewById(R.id.button_cell_3);
        buttons[4] = (Button) rootView.findViewById(R.id.button_cell_4);
        buttons[5] = (Button) rootView.findViewById(R.id.button_cell_5);
        buttons[6] = (Button) rootView.findViewById(R.id.button_cell_6);
        buttons[7] = (Button) rootView.findViewById(R.id.button_cell_7);

        clearbutton = (Button) rootView.findViewById(R.id.button_clear);

        // Floating Action Buttons
        fab_locate = rootView.findViewById(R.id.fab_locate);
        fab_train = rootView.findViewById(R.id.fab_train);


        for (int i = 0; i < 8; i++) {
            buttons[i].setOnClickListener(this);
        }

        clearbutton.setOnClickListener(this);

        fab_locate.setOnClickListener(this);
        fab_train.setOnClickListener(this);

        return rootView;
    }

    // Customizable toast
    private void toastMessage(String message){
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Operation: error(on hold)/busy/done
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void updateColorCells() {
        for (int i = 0; i < 8; i++) {
            if (training_Cell[i] == 1) {
                buttons[i].setBackgroundTintList(ContextCompat.getColorStateList(getActivity().getApplicationContext(), R.color.busy));
            } else if (done_Cell[i] == 1) {
                buttons[i].setBackgroundTintList(ContextCompat.getColorStateList(getActivity().getApplicationContext(), R.color.done));
            } else {
                buttons[i].setBackgroundTintList(ContextCompat.getColorStateList(getActivity().getApplicationContext(), R.color.hold));
            }
        }
    }

    // Update the arrays to ensure that they stay consistent (e.g. no two cells want to train at the same time)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void updateCellArrays(int operation, int cellnumber) {
        if (operation == 2) { // Check for training
            if (training_Cell[cellnumber] == 1) {
                training_Cell[cellnumber] = 0; // Abort previous attempt to train the cell (manually or due to scan error)
            } else {
                training_Cell = new int[8];
                training_Cell[cellnumber] = 1;
            }
        } else if (operation == 3) { // Scan went successfully
            training_Cell[cellnumber] = 0;
            done_Cell[cellnumber] = 1;
        }
        updateColorCells();
    }

    // Check if there is a cell that is selected
    public boolean checkTrainingCell() {
        boolean check = false;

        for (int i = 0; i < 8; i++) {
            if (training_Cell[i] == 1) {
                check = true;
                break;
            }
        }
        return check;
    }

    // Check if all cells are done to start locating
    public boolean checkDoneCell() {
        for (int i = 0; i < 8; i++) {
            if (done_Cell[i] == 0) {
                return false;
            }
        }

        return true;
    }

    // Disable buttons (any) when currently in training or locating, re-enable them after
    public void toggleButtons() {
        for (int i = 0; i < 8; i++) {
            buttons[i].setEnabled(!buttons[i].isEnabled());
        }

        fab_train.setEnabled(!fab_train.isEnabled());
        fab_locate.setEnabled(!fab_locate.isEnabled());
    }

    /**
     * Insertion Sort helper function.
     * Sorts scanResults based on RSSI level
     */
    private List<ScanResult> insertionSort(List<ScanResult> scanResults) {
        int n = scanResults.size();
        for (int i = 1; i < n; ++i) {
            ScanResult key = scanResults.get(i);
            int j = i - 1;
            while (j >= 0 && scanResults.get(j).level < key.level) {
                scanResults.set(j + 1, scanResults.get(j));
                j = j - 1;
            }
            scanResults.set(j + 1, key);
        }
        return scanResults;
    }

    public void startWifiScan () {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("no scan permission", "please give location permission");
        } else {
            wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            assert wifiManager != null;
            boolean success = wifiManager.startScan();
            if (!success) {
                Log.d("SCAN START FAILED ", "scan start failed");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void scanSuccess() {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        /* If we are not in testing mode, execute train procedure below */
        if (!testing) {
            ArrayList<Integer> rssiDebug = new ArrayList<Integer>(); //debug
            /* iterate over all detected APs for current scan and store in DB */
            for (ScanResult scanResult : scanResults) {
                int rssi = scanResult.level;
                String bssid = scanResult.BSSID;
                rssiDebug.add(rssi);
                mDatabaseHelper.addScanDataAP(currentCell, rssi, bssid);
            }
            Log.d("Single Scan Result", "values: " + rssiDebug.toString() + "\n");
            /* If we are still busy training */
            if (scanCounter < trainTimes) {
                scanCounter++;
                if (scanCounter % 5 == 0) {
                    toastMessage("Scancounter is now: " + scanCounter);
                }
                startWifiScan();
            }
            /* After last training round */
            else {
                scanCounter = 1;
            }
        }
        /* If we are in testing mode, execute test procedure below */
        else {
            // Locating started
            // Determine the RSS value and look up the PMF value for that RSSI and the access point
            // 1. Scan the room and grab the AP and the RSSI value
            // 2. Go to the table of that AP and compare the found RSSI value with the PMF table
            // 3. Get the PMF value and get the cell probability
            // 4. Determine P(AP_x) and P(Cell_y)
            // 5. Determine P(Cell_y | AP_x) (for the same AP, all cells) - Normalize
            // 6. Determine the *average* of all probabilities (still not sure)

            /* Filter APs that did not appear during training */
            scanResults = filterUnknownAccessPoints(scanResults);

            /* For each AP, check for each cell probability of being in that cell with found rssi */
            int current_ap_index = 0;
            int number_of_aps = scanResults.size();
            for (ScanResult scanResult : scanResults) {
                    calculateProbabilitiesSingleAP(scanResult, current_ap_index);
                    current_ap_index++;
                    Log.d("AP progress", "finished ap: " + current_ap_index +"/" + number_of_aps);
            }
            for (int i = 0; i < results.size(); i++) {
                Log.d("RESULTS", "results for ap " + i + ": " + Arrays.toString(results.get(i)) + "\n");
            }

            // Once all access points have their probabilities determined, calculate the average out of all of them (?) //todo: or give weigth based on strength/ #samples
            // If the probability of one of the cells did not hit 0.95 yet, redo it (scan again, set the average of all of the AP to the P_Cell)

            double highest_probability_found = 0;
            int current_winner_cell = 0;
            for (int cell = 0; cell < 8; cell++) {
                double probabilityCell = 0;
                for (int ap = 0; ap < number_of_aps; ap++) {
                    probabilityCell += results.get(ap)[cell];
                }
                probabilityCell = probabilityCell / number_of_aps; // divide by number of apss
                Log.d("probabilityCell", "probabilityCell: " + probabilityCell);
                if (number_of_aps != 0) {
                    P_Cell[cell] = probabilityCell/number_of_aps;
                }
                if (probabilityCell/number_of_aps > highest_probability_found) {
                    highest_probability_found = probabilityCell / number_of_aps;
                    current_winner_cell = cell;
                }
            }
            if (highest_probability_found > 0.70) {
                Log.d("early answer", "found answer with >95%: " + current_winner_cell);
                toastMessage("Found the cell that you are currently in: " + current_winner_cell);
                buttons[current_winner_cell].setBackgroundTintList(ContextCompat.getColorStateList(getActivity().getApplicationContext(), R.color.decision));
                testcounter = 1; //reset
                return;
            }
            if (testcounter >= testTimes) { //stop testing
                Log.d("out of testrounds", "highest cell: " + current_winner_cell + " with probability: " + highest_probability_found);
                buttons[current_winner_cell].setBackgroundTintList(ContextCompat.getColorStateList(getActivity().getApplicationContext(), R.color.decision));
                return;
            }
            Log.d("no answer found", "no answer yet, starting new scan soon");
            Log.d("current results", "current results: " + Arrays.toString(P_Cell));
            // else continue scanning
            testcounter++;
            if (testcounter % 5
                    == 0) {
                toastMessage("Testcounter is now: " + testcounter);
            }
            startWifiScan();
        }
    }

    /**
     * Filter test scanResults to remove APs that did not appear during training
     */
    private List<ScanResult> filterUnknownAccessPoints(List<ScanResult> scanResults) {
        List<ScanResult> valuesToRemoves = new ArrayList<ScanResult>();
        for (ScanResult scanResult : scanResults) {
            String bssid = scanResult.BSSID;
            Log.d("bssid", " bssid: " + bssid);
            if(!mDatabaseHelper.checkIfApTableExists(bssid)) {
                valuesToRemoves.add(scanResult);
                Log.d("unknown bssid", "deleting unknown bssid: " + bssid);
            }
        }
        scanResults.removeAll(valuesToRemoves);
        return scanResults;
    }

    private void calculateProbabilitiesSingleAP(ScanResult scanResult, int current_ap_index){
        int rssi = scanResult.level;
        String bssid = scanResult.BSSID;
        //double frequency;
        //double total;
        double probability;
        //ArrayList<Double> P_AP = new ArrayList<Double>();
        double[] P_AP = new double[8]; //contains probability for each cell (for current ap)
        double sum_P_AP = 0.0;
        Log.d("sum_P_AP", "sum_P_AP initial value: " + sum_P_AP + "\n");
        /* For all cells, check probability of being in that cell given current rssi value from current AP */
        for (int cell = 0; cell < 8; cell++) {
            HashMap<Integer, Integer> pmf_table = getPmfMap(bssid, cell);
            double[] parameters = getGaussianParameters(pmf_table);
            double mean = parameters[0];
            double sd = parameters[1];
            NormalDistribution gaussian = new NormalDistribution(mean, sd);
            Log.d("Gaussian params", "mean: " + mean + " sd: " + sd + "\n");

            probability = gaussian.density(rssi);
            Log.d("probSingleAP", "probability: " + probability + "\n");
            /* record results */
            P_AP[cell] = probability;
            Log.d("probSingleAP", "P_Cell[cell]: " + P_Cell[cell] + "\n");

            sum_P_AP += (double) (probability * P_Cell[cell]);
            Log.d("probSingleAP", "sum_P_AP: " + sum_P_AP + "\n");

            //pmf_table = getPmfMap(bssid, cell); //todo: need to check for every cell
//            assert(pmf_table != null);
//            //Log.d("pmf", "pmf: " + pmf_table.toString());
//            //Log.d("rssi", "rssi: " + rssi);
//
//            if (pmf_table.get(rssi) == null) { //it is possible that rssi value does not appear in the current pmf
//                frequency = 0;
//            } else {
//                frequency = (double) pmf_table.get(rssi);
//            }
//            Log.d("probSingleAP", "frequency: " + frequency + "\n");
//            total = (double) pmf_table.get(1);
//            Log.d("probSingleAP", "total: " + total + "\n");
//            if (total != 0) {
//                probability = frequency/total;
//            } else {
//                probability = 0.0;
//            }
        }
        // Probability for ONE AP to be in a cell P(Cell_x | AP_y)
        //ArrayList<Double> P_Cell_AP = new ArrayList<Double>();
        double[] P_Cell_AP = new double[8];
        for (int cell = 0; cell < 8; cell++) {
            Log.d("possible nan?", "for cell: " + cell + "P_AP[CELL]: " + P_AP[cell] + " P_CELL[CELL]: " + P_Cell[cell] + " SUM_P_AP: " + sum_P_AP + "\n");
            if (sum_P_AP == 0) {
                P_Cell_AP[cell] = 0;
            } else {
                P_Cell_AP[cell] = (P_AP[cell] * P_Cell[cell] / sum_P_AP);
            }
            Log.d("possible nan?", "P_Cell_AP[cell]: " + P_Cell_AP[cell] + "\n");
        }
        Log.d("probSingleAP", "P_Cell_AP: " + Arrays.toString(P_Cell_AP) + "\n");
        //result_probabilities_table.put(bssid, P_Cell_AP);
        results.add(current_ap_index, P_Cell_AP);
    }

    /* Given an AP and cell, returns hashmap containing pmf for each recorded rssi */
    public HashMap<Integer, Integer> getPmfMap(String bssid, int cell) {
        /* Select all data from AP where cell is specified cell */
        HashMap<Integer, Integer> pmf = new HashMap<Integer, Integer>();
        Cursor data = mDatabaseHelper.getRssiCountData(bssid, cell);
        int rowCount = 0;
        while (data.moveToNext()) {
            int rssiValue = data.getInt(0);
            int rssiValueCount = data.getInt(1);
            rowCount += rssiValueCount;
            pmf.put(rssiValue, rssiValueCount);
            //Log.d("test ", "column names: " + Arrays.toString(data.getColumnNames()) + "\n");
            //Log.d("test ", "rssi: " + data.getInt(0) + "\n");
            //Log.d("test ", "rssi count: " + data.getInt(1) + "\n");
        }
        data.close();
        /* attention!: the row count is put under integer '1' in hashmap */
        pmf.put(1, rowCount);
        return pmf;
    }

    private double[] getGaussianParameters(HashMap<Integer, Integer> P_Table) {
        double total = 0.0;
        double std = 0.0;
        double[] result = new double[2];
        int samples = 1;
        for (Map.Entry<Integer, Integer> entry : P_Table.entrySet()) {
            total += entry.getKey()*entry.getValue();
            samples += entry.getValue();
        }
        double mean = total/samples;
        for (Map.Entry<Integer, Integer> entry : P_Table.entrySet()) {
            std += Math.pow(entry.getKey() - mean, 2) * entry.getValue();
        }
        std = Math.sqrt(std/samples);
        result[0] = mean;
        if (std == 0) {
            std = 1;
        }
        result[1] = std;
        return result;
    }

    /**
     * Given an AP and cell, returns gaussian distribution for each recorded rssi
     * */
    public NormalDistribution getGaussian(String bssid, int cell) {
        HashMap<Integer, Integer> pmf = getPmfMap(bssid, cell);
        WeightedObservedPoints obs = new WeightedObservedPoints();
        /* Iterate through all RSSI values found for current Access Point */
        if(pmf.size() < 4) {
            Map.Entry<Integer, Integer> maxEntry = null;
            for (Map.Entry<Integer, Integer> entry : pmf.entrySet()) {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                    maxEntry = entry;
                }
            }
            int mostFrequentRSSIvalue = maxEntry.getKey();
            return new NormalDistribution(mostFrequentRSSIvalue, 2);
            //return null; //not enough values so return //todo: what to return in this case
        }
        for(Map.Entry<Integer, Integer> entry : pmf.entrySet()) {
            Integer rssi_value = entry.getKey();
            if (rssi_value == 1) { //skip this key (contains total counts)
                continue;
            }
            Integer rssi_value_count = entry.getValue(); //contains frequency for rssi value
            Log.d("obs pair", "rssi: " + rssi_value + "count: " + rssi_value_count + "\n");
            obs.add(rssi_value, rssi_value_count);
        }
        /* Fit observations to gaussian curve */
        double[] parameters = GaussianCurveFitter.create().fit(obs.toList());
        Log.d("finished gaus ", "finised gaus for bssid: " + bssid + "\n");
        Log.d("gaus params ", "1: " + parameters[0] + "2: " + parameters[1] + "3: " + parameters[2] + "\n");
        /* Create gaussian distribution with found parameters */
        NormalDistribution normalDistribution = new NormalDistribution(parameters[1], parameters[2]);
        return normalDistribution;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void init_train(View v) {
        // https://stackoverflow.com/questions/10311834/how-to-check-if-location-services-are-enabled
        LocationManager lm = (LocationManager) getActivity().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        try {
            assert lm != null;
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {Log.d("Status", "GPS not enabled");}

        if (checkTrainingCell() && gps_enabled) {
            Log.d("Button Click", "Start training, buttons toggled");
            toggleButtons();
            for (int location = 0; location < 8; location++) {
                if (training_Cell[location] == 1) {
                    currentCell = location;
                    testing = false; //e.g. we are training
                    startWifiScan();
                    updateCellArrays(3, location);
                    Snackbar.make(v, "Done training cell " + location, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                }
            }
            toggleButtons(); // Enable buttons again
        } else if (!gps_enabled) {
            Log.d("Button Click", "GPS not enabled so aborted");
            Snackbar.make(v, "Please enable location", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            Log.d("Button Click", "Start training, but no cell selected");
            Snackbar.make(v, "Please select a cell before training", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void init_locate(View v) {
        LocationManager lm = (LocationManager) getActivity().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        // Initialize each cell probability to be equal (= 1/8)
        for (int i = 0; i < 8; i++) {
            P_Cell[i] = (double) (1.0/8.0);
        }
        try {
            assert lm != null;
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ignored) {}
        //if (checkDoneCell() && gps_enabled) {
        if (gps_enabled) {
            clearResults();
            Log.d("Button Click", "Locating started, buttons toggled");
            toggleButtons();
            testing = true;
            startWifiScan();
            toggleButtons();
            Snackbar.make(v, "Done locating, cell determined is shown in blue", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else if (!gps_enabled){
            Log.d("Button Click", "GPS not enabled so aborted");
            Snackbar.make(v, "Please enable location", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            Log.d("Button Click", "Locating started, but not all cells are trained");
            Snackbar.make(v, "Please train all cells before starting locating", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    private void clearResults() {
        results.clear();
        //todo: clear other intermediate results?
    }

    private void clearDatabase() {
        boolean deleted = getActivity().deleteDatabase("quirk2");
        if (deleted) {
            toastMessage("Data successfully cleared");
        } else {
            toastMessage("Data has not been cleared");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_cell_0: { // Training garden
                Log.d("Button Click", "Training garden has been clicked");
                updateCellArrays(2, 0);
                break;
            }
            case R.id.button_cell_1: { // Training room 1
                Log.d("Button Click", "Training room 1 has been clicked");
                updateCellArrays(2, 1);
                break;
            }
            case R.id.button_cell_2: { // Training room 2
                Log.d("Button Click", "Training room 2 has been clicked");
                updateCellArrays(2, 2);
                break;
            }
            case R.id.button_cell_3: { // Training hall
                Log.d("Button Click", "Training hall has been clicked");
                updateCellArrays(2, 3);
                break;
            }
            case R.id.button_cell_4: { // Training street 1
                Log.d("Button Click", "Training street 1 has been clicked");
                updateCellArrays(2, 4);
                break;
            }
            case R.id.button_cell_5: { // Training street 2
                Log.d("Button Click", "Training street 2 has been clicked");
                updateCellArrays(2, 5);
                break;
            }
            case R.id.button_cell_6: { // Training street 3
                Log.d("Button Click", "Training street 3 has been clicked");
                updateCellArrays(2, 6);
                break;
            }
            case R.id.button_cell_7: { // Training street 4
                Log.d("Button Click", "Training street 4 has been clicked");
                updateCellArrays(2, 7);
                break;
            }
            case R.id.fab_train: { // Start training
                Log.d("Button Click", "Training has been clicked");
                init_train(v);
                break;
            }
            case R.id.fab_locate: { // Start locating
                testing = true;
                Log.d("Button Click", "Locating has been clicked");
                init_locate(v);
                break;
            }
            case R.id.button_clear: {
                clearDatabase();
                Log.d("Clear db", "Clearing db");
                break;
            }
        }
    }
}
