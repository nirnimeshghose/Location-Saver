package com.example.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.location.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;

    private double wayLatitude = 0.0, wayLongitude = 0.0;
    private double speed = 0;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private android.widget.Button btnLocation;
    private TextView txtLocation;
    private android.widget.Button btnContinueLocation;
    private TextView txtContinueLocation;
    private StringBuilder stringBuilder;
    private Calendar calendar;
    private FileOutputStream outputStream = null;

    private boolean isContinue = false;
    private boolean isGPS = false;

    String FILE_NAME = "myLocation";

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.txtContinueLocation = (TextView) findViewById(R.id.txtContinueLocation);
        this.btnContinueLocation = (Button) findViewById(R.id.btnContinueLocation);
        this.txtLocation = (TextView) findViewById(R.id.txtLocation);
        this.btnLocation = (Button) findViewById(R.id.btnLocation);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FILE_NAME = FILE_NAME + Double.toString(Math.random()) + ".txt";
        File file = new File(FILE_NAME);
        try {
            outputStream = openFileOutput(FILE_NAME, MODE_APPEND);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1 * 1000); // 1 seconds
        locationRequest.setFastestInterval(1 * 1000); // 1 seconds

        new GpsUtils(this).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;
            }
        });

        locationCallback = new LocationCallback() {
            private Location lastLocation;
            private Calendar calendar;

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();
                        if (this.lastLocation != null&&location.distanceTo(this.lastLocation)!=0)
                            speed = (location.distanceTo(this.lastLocation)*1000) / (location.getTime() - this.lastLocation.getTime());
                        //Log.e(LTE_TAG,Double.toString(speed));
                        //if there is speed from location
                        if (location.hasSpeed()){
                            //get location speed
                            speed = location.getSpeed();
                            //Log.e(LTE_TAG,Double.toString(speed)+" from Location");
                        }
                        this.lastLocation = location;
                        calendar = Calendar.getInstance();
                        long timeNow = calendar.getTimeInMillis();

                        if (!isContinue) {
                            txtLocation.setText(String.format(Locale.US, "%s  %s  %s  %s",timeNow,speed, wayLatitude, wayLongitude));
                            try {
                                outputStream.close();
                                Log.e("TAG","File closed");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            stringBuilder.append(timeNow);
                            stringBuilder.append("  ");
                            stringBuilder.append(speed);
                            stringBuilder.append("  ");
                            stringBuilder.append(wayLatitude);
                            stringBuilder.append("  ");
                            stringBuilder.append(wayLongitude);
                            stringBuilder.append("\n\n");
                            txtContinueLocation.setText(stringBuilder.toString());
                            try {
                                outputStream.write(stringBuilder.toString().getBytes());
                                Log.e("TAG","Data Written");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (!isContinue && mFusedLocationClient != null) {
                            mFusedLocationClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }
            }
        };

        btnLocation.setOnClickListener(v -> {

            if (!isGPS) {
                Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_SHORT).show();
                return;
            }
            isContinue = false;
            getLocation();
        });

        btnContinueLocation.setOnClickListener(v -> {
            if (!isGPS) {
                Toast.makeText(this, "Please turn on GPS", Toast.LENGTH_SHORT).show();
                return;
            }
            isContinue = true;
            stringBuilder = new StringBuilder();
            getLocation();
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstants.LOCATION_REQUEST);

        } else {
            if (isContinue) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                    if (location != null) {
                        wayLatitude = location.getLatitude();
                        wayLongitude = location.getLongitude();
                        if (this.lastLocation != null&&location.distanceTo(this.lastLocation)!=0)
                            speed = (location.distanceTo(this.lastLocation)*1000) / (location.getTime() - this.lastLocation.getTime());
                        //Log.e(LTE_TAG,Double.toString(speed));
                        //if there is speed from location
                        if (location.hasSpeed()){
                            //get location speed
                            speed = location.getSpeed();
                            //Log.e(LTE_TAG,Double.toString(speed)+" from Location");
                        }
                        this.lastLocation = location;
                        calendar = Calendar.getInstance();
                        long timeNow = calendar.getTimeInMillis();
                        txtLocation.setText(String.format(Locale.US, "%s  %s  %s  %s",timeNow,speed, wayLatitude, wayLongitude));
                    } else {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                });
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (isContinue) {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    } else {
                        mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                            if (location != null) {
                                wayLatitude = location.getLatitude();
                                wayLongitude = location.getLongitude();
                                if (this.lastLocation != null&&location.distanceTo(this.lastLocation)!=0)
                                    speed = (location.distanceTo(this.lastLocation)*1000) / (location.getTime() - this.lastLocation.getTime());
                                //Log.e(LTE_TAG,Double.toString(speed));
                                //if there is speed from location
                                if (location.hasSpeed()){
                                    //get location speed
                                    speed = location.getSpeed();
                                    //Log.e(LTE_TAG,Double.toString(speed)+" from Location");
                                }
                                this.lastLocation = location;
                                calendar = Calendar.getInstance();
                                long timeNow = calendar.getTimeInMillis();
                                txtLocation.setText(String.format(Locale.US, "%s  %s  %s  %s",timeNow,speed, wayLatitude, wayLongitude));
                            } else {
                                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            }
                        });
                    }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConstants.GPS_REQUEST) {
                isGPS = true; // flag maintain before get location
            }
        }
    }
}