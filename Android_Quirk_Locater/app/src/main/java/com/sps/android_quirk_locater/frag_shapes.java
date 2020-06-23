package com.sps.android_quirk_locater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link frag_shapes#newInstance} factory method to
 * create an instance of this fragment.
 */
public class frag_shapes extends Fragment implements View.OnClickListener, SensorEventListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public frag_shapes() {
        // Required empty public constructor
    }

    /* Sensor Variables */
    private TextView textCurrentHeading, textDistanceSum, textcurrentWinner;
    private SensorManager mSensorManager;
    private Sensor mSensorRotationVector;
    private Sensor mSensorStepCounter;
    private double currentHeading;
    private float oldSteps = -1;
    private boolean particlesInitalized = false;
    private float distanceSum = 0;

    /* The cells, walls and particles */
    private List<ShapeDrawable> verwalls; // Vertical walls
    private List<ShapeDrawable> verboundwalls; // Vertical walls with boundaries
    private List<ShapeDrawable> horwalls; // Horizontal walls
    private List<particleInfo> particles; // Particles
    private List<ShapeDrawable> rectCells; // The whole cell (collision detection)
    private List<double[]> cells; // Cell coordinates (dimensions)
    private List<Integer[]> neighbors; // Cell neighbors
    private Button iniParticles, moveParticles; // Buttons
    private Canvas canvas; // Canvas to draw in

    private int[] cellParticleCount;

    /* System and particles parameters */
    int width, height; // width and height of the screen (resolution)
    int nParticles = 200; // amount of particles in each cell at the start
    int particleSize = 16; // Particle diameter (/2 = radius)
    double walkStep = 0.5; // How many meters each step is (approximately)

    /* Layout specific parameters, set in onCreate() */
    double wallSize = 25; // Default: 25 (Phu's home) - Determine the size of the wall
    double scaleFactor = 110; // Default: 110 - Scale the layout by this amount to fill the screen
    double yOffset = 100; // Default: 100 - Offset to create a white gap at the top
    int numCells = 8; // Default: 8 cells

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment frag2.
     */
    // TODO: Rename and change types and number of parameters
    public static frag_shapes newInstance(String param1, String param2) {
        frag_shapes fragment = new frag_shapes();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        // X | Y | Width | Height (Top left is 0,0) - Relative coordinates
        cells = new ArrayList<>();;
        neighbors = new ArrayList<>();

        // Comment out which layout you want to create
        // Phu's Home Parameters
//        cells.add(new double[] {0.0, 0.0, 3.0, 2.5}); // room 0
//        cells.add(new double[] {3.0, 0.0, 2.0, 5.0}); // room 1
//        cells.add(new double[] {5.0, 0.0, 2.0, 5.0}); // room 2
//        cells.add(new double[] {7.0, 0.0, 2.0, 5.0}); // room 3
//        cells.add(new double[] {9.0, 0.0, 2.3, 5.0}); // room 4
//        cells.add(new double[] {11.3, 0.0, 2.3, 5.0}); // room 5
//        cells.add(new double[] {13.6, 0.0, 2.3, 5.0}); // room 6
//        cells.add(new double[] {15.9, 0.0, 2.3, 5.0}); // room 7
//
//        neighbors.add(new Integer[] {0, 1}); // room 0
//        neighbors.add(new Integer[] {0, 1, 2}); // room 1
//        neighbors.add(new Integer[] {1, 2, 3}); // room 2
//        neighbors.add(new Integer[] {2, 3, 4}); // room 3
//        neighbors.add(new Integer[] {3, 4, 5}); // room 4
//        neighbors.add(new Integer[] {4, 5, 6}); // room 5
//        neighbors.add(new Integer[] {5, 6, 7}); // room 6
//        neighbors.add(new Integer[] {6, 7}); // room 7
//
//        wallSize = 5;
//        scaleFactor = 110;
//        yOffset = 100;
//        numCells = 8;

        // Tomas's Home Parameters
        cells.add(new double[] {0.0, 7.0, 5.0, 1.4}); // room 0
        cells.add(new double[] {3.0, 8.4, 2.0, 1.4}); // room 1
        cells.add(new double[] {5.0, 4.6, 1.1, 2.4}); // room 2
        cells.add(new double[] {5.0, 7.0, 18.0, 1.4}); // room 3
        cells.add(new double[] {23.0, 7.0, 18.0, 1.4}); // room 4
        cells.add(new double[] {39.9, 8.4, 1.1, 2.4}); // room 5
        cells.add(new double[] {41.0, 7.0, 5.0, 1.4}); // room 6
        cells.add(new double[] {41.0, 5.6, 2.0, 1.4}); // room 7
        cells.add(new double[] {26.0, 0.0, 2.3, 5.9}); // room 8
        cells.add(new double[] {27.2, 5.9, 1.1, 1.1}); // room 9

        neighbors.add(new Integer[] {0, 1, 2, 3}); // room 0
        neighbors.add(new Integer[] {0, 1, 2, 3}); // room 1
        neighbors.add(new Integer[] {0, 1, 2, 3}); // room 2
        neighbors.add(new Integer[] {0, 1, 2, 3, 4}); // room 3
        neighbors.add(new Integer[] {3, 4, 9, 5, 6, 7}); // room 4
        neighbors.add(new Integer[] {4, 5, 6, 7}); // room 5
        neighbors.add(new Integer[] {4, 5, 6, 7}); // room 6
        neighbors.add(new Integer[] {4, 5, 6, 7}); // room 7
        neighbors.add(new Integer[] {8, 9}); // room 8
        neighbors.add(new Integer[] {4, 8 , 9}); // room 9

        wallSize = 8;
        scaleFactor = 45;
        yOffset = 100;
        numCells = 10;

        // Get the screen dimensions
        //Log.d("Create", "Creating view");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        //Log.d("Create", "Done, x: " + width + ", y: " + height);

        // Sensors
        mSensorManager = (SensorManager) getActivity().getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!= null) {
            mSensorStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            toastMessage("Assigned sensor");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)!= null) {
            mSensorRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        //debug
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_frag_shapes, container, false);

        // Setup the buttons
        iniParticles = (Button) rootView.findViewById(R.id.iniParticles);
        iniParticles.setOnClickListener(this);

        // Setup the canvas
        ImageView canvasView = (ImageView) rootView.findViewById(R.id.canvas);
        Bitmap blankBitmap = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(blankBitmap);
        canvasView.setImageBitmap(blankBitmap);

        //debug

        textCurrentHeading = (TextView) rootView.findViewById(R.id.textCurrentHeading);
        textDistanceSum = (TextView) rootView.findViewById(R.id.textDistanceSum);
        textcurrentWinner = (TextView) rootView.findViewById(R.id.textCurrentWinner);

        return rootView;
    }

    /**
     * Create customizable toast messages
     * @param message Message that you want to show
     */
    // Customizable toast
    private void toastMessage(String message){
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    /**
     * The layout is created using the home parameters (onCreate())
     * Visual layout and the borders for the particles are determined here
     */
    private void createLayOut(){
        verwalls = new ArrayList<>();
        horwalls = new ArrayList<>();
        verboundwalls = new ArrayList<>();
        rectCells = new ArrayList<>();

        double[] coordinates; // Coordinates (dimensions) of the layout

        // Create the lines for each cell
        for (int i = 0; i < numCells; i++){
            //Log.d("Loop", "Iteration number: " + i + " -- " + cells.get(i)[0] + "/" + cells.get(i)[1] + "/" + cells.get(i)[2] + "/" + cells.get(i)[3]);
            coordinates = cells.get(i);

            ShapeDrawable dtop = new ShapeDrawable(new RectShape());
            dtop.setBounds((int) (coordinates[0]*scaleFactor), (int) (coordinates[1]*scaleFactor+coordinates[3]*scaleFactor+yOffset),
                    (int) (coordinates[0]*scaleFactor+coordinates[2]*scaleFactor), (int) (coordinates[1]*scaleFactor+coordinates[3]*scaleFactor+wallSize+yOffset));
            horwalls.add(dtop);

            ShapeDrawable dbot = new ShapeDrawable(new RectShape());
            dbot.setBounds((int) (coordinates[0]*scaleFactor), (int) (coordinates[1]*scaleFactor+yOffset),
                    (int) (coordinates[0]*scaleFactor+coordinates[2]*scaleFactor), (int) (coordinates[1]*scaleFactor+wallSize+yOffset));
            horwalls.add(dbot);

            ShapeDrawable dleft = new ShapeDrawable(new RectShape());
            dleft.setBounds((int) (coordinates[0]*scaleFactor), (int) (coordinates[1]*scaleFactor+yOffset),
                    (int) (coordinates[0]*scaleFactor+wallSize), (int) (coordinates[1]*scaleFactor+coordinates[3]*scaleFactor+yOffset+wallSize));
            verwalls.add(dleft);

            ShapeDrawable dright = new ShapeDrawable(new RectShape());
            dright.setBounds((int) (coordinates[0]*scaleFactor+coordinates[2]*scaleFactor), (int) (coordinates[1]*scaleFactor+yOffset),
                    (int) (coordinates[0]*scaleFactor+coordinates[2]*scaleFactor+wallSize),  (int) (coordinates[1]*scaleFactor+coordinates[3]*scaleFactor+yOffset+wallSize));
            verwalls.add(dright);

            ShapeDrawable cell = new ShapeDrawable(new RectShape());
            cell.getPaint().setColor(Color.YELLOW);
            cell.setBounds((int) (coordinates[0]*scaleFactor+wallSize), (int) (coordinates[1]*scaleFactor+yOffset+wallSize),
                    (int) (coordinates[0]*scaleFactor+coordinates[2]*scaleFactor), (int) (coordinates[1]*scaleFactor+coordinates[3]*scaleFactor+yOffset));
            rectCells.add(cell);
        }
    }

    /**
     * Create particles after user pressed the initialize particles button
     */
    private void initializeParticles(){
        particles = new ArrayList<>();
        double[] coordinates; // Coordinates (dimensions) of the layout
        double xParticle; // x-coordinate of the particle
        double yParticle; // y-coordinate of the particle

        for (int j = 0; j < numCells; j++) {
            coordinates = cells.get(j);
            for (int i = 0; i < nParticles; i++) {
                ShapeDrawable p = new ShapeDrawable(new OvalShape());
                p.getPaint().setColor(Color.RED);

                xParticle = Math.random() * (coordinates[2]*scaleFactor-2*wallSize); // Randomly choose value between X distance
                yParticle = Math.random() * (coordinates[3]*scaleFactor-2*wallSize);

                //Log.d("Particle location", "x: " + xParticle + ", y: " + yParticle);

                p.setBounds((int) (coordinates[0]*scaleFactor+xParticle-(particleSize/2)+wallSize), (int) (coordinates[1]*scaleFactor+yParticle-(particleSize/2)+yOffset+wallSize),
                        (int) (coordinates[0]*scaleFactor+xParticle+(particleSize/2)+wallSize),  (int) (coordinates[1]*scaleFactor+yParticle+(particleSize/2)+yOffset+wallSize));

                particles.add(new particleInfo(p, new ArrayList<Integer>(Arrays.asList(j))));
                Log.d("Particle", "Added particle: x: " + xParticle + ", y: " + yParticle + " in cell " + particles.get(particles.size()-1).getCell().toString());
            }
        }
        particlesInitalized = true;
    }

    /**
     * Check for particles that are not in a cell currently, if they are, check for neighbor consistency
     * If not, remove them and respawn new particles
     */
    public void checkOutliers() {
        int removeParticles = 0;
        boolean legalMoveFlag = false;
        boolean particleContains = false;

        // reset cell particle count
        cellParticleCount = new int[numCells];
        Arrays.fill(cellParticleCount, 0);
        // Go through each particle and determine the state (legal/illegal)
        Iterator<particleInfo> particle = particles.iterator();
        while(particle.hasNext()) {
            particleInfo p = particle.next();
            ArrayList<Integer> oldCells = p.getCell();
            ArrayList<Integer> newCells = determineCell(p);
            particleContains = false;

            for (ShapeDrawable cells : rectCells) {
                Rect firstRect = new Rect(cells.getBounds());
                if (firstRect.contains(p.getShape().getBounds()) || firstRect.intersect(p.getShape().getBounds())) {
                    particleContains = true;
                    //Log.d("Cell", "Inside the cell");
                    break;
                }
            }

            if (!particleContains) { // Particle is not inside a cell
                particle.remove();
                //Log.d("Cells", "Outside the cells");
                removeParticles++;
            } else { // Particle is in a cell, determine if the movement was legal (no illegal jumps)
                legalMoveFlag = false;
                for (int i = 0; i < oldCells.size(); i++) {
                    if (legalMoveFlag) break;
                    for (int j = 0; j < newCells.size(); j++) {
                        if (oldCells.get(i).equals(newCells.get(j)) || neighborCells(oldCells.get(i), newCells.get(j))) {
                            // No illegal jumps or wall collision
                            cellParticleCount[newCells.get(j)] += 1;
                            legalMoveFlag = true;
                            p.setCell(newCells); // Update the new cell for the next movement
                            //Log.d("Neighbor Cells", "Valid jump");
                            break;
                        }
                    }
                }

                if (!legalMoveFlag) { // Particle is in a cell, but made an illegal move (not moving to nearby neighbor)
                    particle.remove();
                    //Log.d("Neighbor Cells", "Invalid neighbor jump");
                    removeParticles++;
                }
            }
        }

        //Log.d("Particles", "Particles left: " + particles.size());

        // Resample the same amount of particles that were removed
        if (removeParticles != 0) {
            //Log.d("Particles", "Respawning " + removeParticles + " particles");
            respawnParticles(removeParticles);
        }
    }

    /**
     * Determine which cell a certain particle is in
     * @param inputParticle Particle that you want to know the cell from
     * @return Return the cell(s) that the particle currently is in
     */
    public ArrayList<Integer> determineCell(particleInfo inputParticle) {
        ArrayList<Integer> containCells = new ArrayList<>();

        for (int i = 0; i < rectCells.size(); i++) {
            ShapeDrawable cells = rectCells.get(i);
            Rect firstRect = new Rect(cells.getBounds());

            if (firstRect.intersect(inputParticle.getShape().getBounds())) {
                containCells.add(i);
            }
        }

        return containCells;
    }

    /**
     * Check if the two cells are neighbors
     * @param cellA first cell
     * @param cellB second cell
     * @return a boolean where true means that the cells are neighbors
     */
    public boolean neighborCells(int cellA, int cellB) {
        //Log.d("Neighbor Check", "Cell " + cellA + " and cell " + cellB);

        for (int i = 0; i < neighbors.get(cellA).length; i++) {
            if (neighbors.get(cellA)[i] == cellB) {
                //Log.d("Neighbor Check", "Cell " + cellA + " and cell " + cellB + " -> True");
                return true; // true if they are neighbor cells
            }
        }
        //Log.d("Neighbor Check", "Cell " + cellA + " and cell " + cellB + " -> False");
        return false;
    }

    /**
     * Move the particles around, based on the input from the ROTATION_VECTOR and Step Counter
     * @param rotation rotation of the smartphone [radians]
     * @param step steps down [unit] ~ (pixels/m) scaling factor
     */
    public void moveParticles(double rotation, double step) {
        rotation = Math.toDegrees(rotation);
        double radians;
        // translate direction to movement
        if (rotation < 92 && rotation > 0) {
            // North,
            radians = Math.PI;
        }
        else if (rotation <= 0 && rotation > -120) {
            // East
            radians = Math.PI * 0.5;
        }
        else if ( (rotation < -120 && rotation >= -180) || (rotation <= 180 && rotation > 160) ) {
            // South
            radians = 0;
        }
        //else if (rotation < 180 && rotation >= 92 ) {
        else {
            radians = Math.PI * -0.5;
        }

        step = step * walkStep * scaleFactor;
        Iterator<particleInfo> particle = particles.iterator();
        while(particle.hasNext()) {
            particleInfo p = particle.next();
            Rect particleDimensions = p.getShape().getBounds();

            p.getShape().setBounds((int) (particleDimensions.left + Math.sin(radians)*step ), (int) (particleDimensions.top + Math.cos(radians)*step),
                    (int) (particleDimensions.right+ Math.sin(radians)*step ), (int) (particleDimensions.bottom + Math.cos(radians)*step ));
        }
    }

    /**
     * Refresh the canvas with new info
     */
    public void refreshCanvas() {
        canvas.drawColor(Color.WHITE);
        for(ShapeDrawable rect : rectCells) // Debugging purposes for the cells
            rect.draw(canvas);
        for(ShapeDrawable wall : verwalls)
            wall.draw(canvas);
        for(ShapeDrawable wall : horwalls)
            wall.draw(canvas);
        for(particleInfo particle : particles)
            particle.getShape().draw(canvas);
    }

    /**
     * Respawn particles if they collided with a wall
     * @param numRespawn amount of particles that needs to be respawn
     */
    public void respawnParticles(int numRespawn) {
        double stepSize = wallSize*5;

        // Rare occassion during debug: all particles are moving illegally -> message
        if (particles.size() == 0) {
            toastMessage("All particles are gone, good job. Time to reinitialize");
            return;
        }

        for (int i = 0; i < numRespawn; i++) {
            ShapeDrawable pref = particles.get(new Random().nextInt(particles.size())).getShape(); // Grab random reference particle
            ShapeDrawable pnew = new ShapeDrawable(new OvalShape()); // Newly created particle
            //pnew.getPaint().setColor(Color.BLUE); // Debug purposes
            pnew.getPaint().setColor(Color.RED);

            double xDistance = (new Random().nextBoolean() ? 1 : -1 ) * Math.random() * stepSize; // Spawn random distance from reference particle
            double yDistance = (new Random().nextBoolean() ? 1 : -1 ) * Math.random() * stepSize;

            //Log.d("Particle location", "x: " + xParticle + ", y: " + yParticle); left top right bottom

            pnew.setBounds((int) (pref.getBounds().left+xDistance), (int) (pref.getBounds().top+yDistance),
                    (int) (pref.getBounds().right+xDistance),  (int) (pref.getBounds().bottom+yDistance));
            particleInfo p = new particleInfo(pnew);
            ArrayList<Integer> pCells = determineCell(p);
            p.setCell(pCells);
            particles.add(p);
        }
        checkOutliers(); // Check for outliers
    }

    @SuppressLint("DefaultLocale")
    public void updateParticleCount() {
        float total = 0;
        int largest = 0;
        for (int i = 0; i < numCells; i++) {
            rectCells.get(i).getPaint().setColor(Color.YELLOW);
            total += cellParticleCount[i];
            if (cellParticleCount[i] > cellParticleCount[largest]) {
                largest = i;
            }
        }
        float percentage = cellParticleCount[largest] / total * 100;
        rectCells.get(largest).getPaint().setColor(Color.BLUE);
        textcurrentWinner.setText(String.format("Current winner is cell %d with %.2f %%", (largest), percentage));
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {
        /* If step counter has recorded steps, process them */
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            toastMessage("Detecting changes");
            float steps = event.values[0];
            /* Wait until particles are initalized */
            if  (oldSteps == -1 || !particlesInitalized) {
                oldSteps = steps;
                Log.d("old steps set", "old steps now: " + oldSteps);
                return;
            }
            float distance = steps - oldSteps;
            distanceSum += distance;
            textDistanceSum.setText(String.valueOf(distanceSum));
            oldSteps = steps;
            moveParticles(currentHeading, distance);
            checkOutliers();
            refreshCanvas();
            updateParticleCount();
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float x =  event.values[0];
            float y =  event.values[1];
            float z =  event.values[2];
            float w =  event.values[3];

            // alternative calculation
//            double mag = Math.sqrt(w*w +z*z);
//            w /= mag;
//            z /= mag;
//            currentHeading = 2*Math.acos(w);

            // Calculate yaw rotation of the phone to estimate heading direction
            double siny_cosp = 2.0 * (w * z + x * y);
            double cosy_cosp = 1.0 - 2.0 * (y * y + z * z);
            currentHeading = Math.atan2(siny_cosp, cosy_cosp);
            //textCurrentHeading.setText(String.format(" Yaw: %.3f", Math.toDegrees(heading)));
            textCurrentHeading.setText(String.format(" Yaw: %.3f", currentHeading));

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorStepCounter, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorRotationVector, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iniParticles: { // Initialize Particles
                Log.d("Button Click", "Initialize particles has been clicked");
                initializeParticles();
                createLayOut();
                refreshCanvas();
                break;
            }
        }
    }
}

