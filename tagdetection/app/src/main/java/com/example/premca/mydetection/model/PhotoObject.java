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

package com.example.premca.mydetection.model;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/*
 * A database for each photo.
 */

public class PhotoObject extends RealmObject {

    @PrimaryKey
    private int id;

    private String picturePath;

    private String lat;

    private String lng;

    private long miliTimestamp;

    private RealmList<DetectionPosition> detections = new RealmList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public RealmList<DetectionPosition> getDetectionPositions() {
        return this.detections;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public long getMiliTimestamp() {
        return miliTimestamp;
    }

    public void setMiliTimestamp(long miliTimestamp) {
        this.miliTimestamp = miliTimestamp;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    // add position of square
    public void addPosition(DetectionPosition position) {
        detections.add(position);
    }


    public static int getNextId(Realm realm) {
        Number currentIdNum = realm.where(PhotoObject.class).max("id");
        int nextId;
        if (currentIdNum == null) {
            nextId = 1;
        } else {
            nextId = currentIdNum.intValue() + 1;
        }
        return nextId;
    }
}
