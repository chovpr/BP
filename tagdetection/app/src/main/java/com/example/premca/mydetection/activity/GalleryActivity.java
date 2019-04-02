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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.premca.mydetection.R;
import com.example.premca.mydetection.component.MyRectangle;
import com.example.premca.mydetection.model.DetectionPosition;
import com.example.premca.mydetection.model.PhotoObject;

import io.realm.Realm;
import io.realm.RealmResults;

/*
 * This class implements gallery with photos
 * Adapters take care of each photo in the fragment.
 */

public class GalleryActivity extends BaseActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        Realm mRealm = Realm.getDefaultInstance();
        RealmResults photos = mRealm.where(PhotoObject.class).findAll();

        // Create the adapter that will return a fragment for each
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), photos);

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        Button delete = findViewById(R.id.button_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            // Deleting current photo.
            public void onClick(View view) {
                mSectionsPagerAdapter.deletePhoto(mViewPager.getCurrentItem());
            }
        });

        // Button for the map, starting the intent.
        Button showMap = findViewById(R.id.button_show_map);
            showMap.setOnClickListener(v -> {
                if (photos.size() > 0) {
                    PhotoObject photo = (PhotoObject) mRealm.copyFromRealm(photos.get(mViewPager.getCurrentItem()));
                    String uri = "geo:" + photo.getLat() + "," + photo.getLng() + "?q=" + photo.getLat() + "," + photo.getLng() + "TEXT";
                    Log.e("uri", uri);

                    Uri gmmIntentUri = Uri.parse(uri);
                    if (photo.getLat().equals("null")) {
                        String message = "Chyba! Neplatné GPS souřadnice.";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    } else {
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    }
                }
                else {
                    String message = "Není zde nic k zobrazení!";
                    Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                }
            });
    }

    // Fragment controlling the view of photo and rectangles

    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_FOTO_ID = "foto_id";
        public static int fragHeight;
        public static int fragWidth;

        public static PlaceholderFragment newInstance(int sectionNumber, PhotoObject fotoObject) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putInt(ARG_FOTO_ID, fotoObject.getId());
            fragment.setArguments(args);
            Log.e("FRAGMENT", fragment.getClass().getName());
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_gallery_image, container, false);

            rootView.post(new Runnable() {
                @Override
                public void run() {
                    fragHeight=rootView.getMeasuredHeight(); // for instance
                    fragWidth=rootView.getMeasuredWidth();
                }
            });



            int fotoId = getArguments().getInt(ARG_FOTO_ID);
            Realm mRealm = Realm.getDefaultInstance();
            PhotoObject photo = mRealm.where(PhotoObject.class).equalTo("id", fotoId).findFirst();

            Log.e("PHOTO DETECTIONS", String.valueOf(photo.getDetectionPositions().size()));
            ImageView imageView = rootView.findViewById(R.id.fragment_gallery_image);
            imageView.setImageDrawable(Drawable.createFromPath(photo.getPicturePath()));

            RelativeLayout relativeLayout = rootView.findViewById(R.id.canvas_container);

            // Drawing rectangles.
            for (int i = 0; i < photo.getDetectionPositions().size(); i++) {

                DetectionPosition position = photo.getDetectionPositions().get(i);
                Log.e("I", String.valueOf(i));

                Log.e("RECTANGLE", position.toString() +"   "+ String.valueOf(position.getTop()));
                MyRectangle rectangle = new MyRectangle(getActivity());
                rectangle.setPosition(position);
                // Put all rectangles to the relativeView with the image.
                relativeLayout.addView(rectangle);
            }
            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        private RealmResults<PhotoObject> photos;

        SectionsPagerAdapter(FragmentManager fm, RealmResults<PhotoObject> photos) {
            super(fm);
            this.photos = photos;
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1, photos.get(position));
        }

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public int getItemPosition(Object object) {
            // refresh all fragments when data set changed
            return PagerAdapter.POSITION_NONE;
        }

        public void deletePhoto(int position) {
            if (getCount() > 0) {
                Realm mRealm = Realm.getDefaultInstance();
                mRealm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        photos.get(position).deleteFromRealm();
                    }
                });
                notifyDataSetChanged();
            }
            else   {
                String message = "Není zde nic ke smazání!";

            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
