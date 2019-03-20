package com.example.premca.mydetection.model;

import android.graphics.RectF;

import java.lang.reflect.Array;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class FotoObject extends RealmObject {

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

    public void addPosition(DetectionPosition position) {
        detections.add(position);
    }


    public static int getNextId(Realm realm) {
        Number currentIdNum = realm.where(FotoObject.class).max("id");
        int nextId;
        if (currentIdNum == null) {
            nextId = 1;
        } else {
            nextId = currentIdNum.intValue() + 1;
        }
        return nextId;
    }
}
