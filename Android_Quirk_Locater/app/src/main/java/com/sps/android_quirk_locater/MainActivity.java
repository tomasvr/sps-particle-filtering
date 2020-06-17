package com.sps.android_quirk_locater;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.sps.android_quirk_locater.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity{

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creates tabs: https://www.youtube.com/watch?v=h4HwU_ENXYM
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        Log.d("Permission", "Permission has been granted");
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                    System.exit(0);
                }
                return;
            }
        }
    }

    // Customizable toast
    private void toastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

// TODO: voor elke access point een eigen 'tab' voor de database (dynamisch)
// TODO: Gaussian en Raw aparte tabellen (multi model Gaussian, 2 Gaussian over elkaar)
// TODO: Bayesian bepalen: PMF bepalen voor elke cell van elke access point


// TODO: Debug window that shows the amount of samples at that moment (during training phase)
// TODO: use STEP_COUNTER to count the amount of steps
// TODO: use ROTATION_VECTOR to get the direction
// TODO: Recreate the lay out with shapes
// TODO: Put particles equally distributed in all cells (5000-10000)
// TODO: Based on the parameters above, remove particles that are unlikely to be the location

