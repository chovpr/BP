package com.example.premca.mydetection.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.premca.mydetection.R;

import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

public abstract class BaseActivity extends AppCompatActivity {


    protected Double lat;
    protected Double lng;
    protected Float acc;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }*/





        super.onCreate(savedInstanceState);
        SmartLocation.with(this)
                .location(new LocationGooglePlayServicesWithFallbackProvider(this))
                .config(LocationParams.NAVIGATION)
                .start(location -> {
                    this.lat = location.getLatitude();
                    this.lng = location.getLongitude();
                    this.acc = location.getAccuracy();
                });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = null;
        //noinspection SimplifiableIfStatement
        if (id == R.id.photo_action) {
            intent = new Intent(this, MainActivity.class);
        }

        if (id == R.id.gallery_action) {
            intent = new Intent(this, GalleryActivity.class);
        }

        if (id == R.id.info_action) {
            intent = new Intent(this, InfoActivity.class);

        }

        startActivity(intent);
        finish();

        return super.onOptionsItemSelected(item);
    }

    /// PERMISSIONS


}
