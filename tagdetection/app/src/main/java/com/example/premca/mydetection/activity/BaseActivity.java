/*
 * Copyright 2019 Přemysl Chovaneček. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.premca.mydetection.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

         super.onCreate(savedInstanceState);
         // Start the location looper.
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
        // Start selected intent - run activity.
        startActivity(intent);
        finish();

        return super.onOptionsItemSelected(item);
    }
}
