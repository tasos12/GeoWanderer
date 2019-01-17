package com.monk.geotraveler;

import java.util.HashMap;
import java.util.Map;

public class User {

    private float mDistanceMaxTodayValue = 0;
    private float mDistanceMaxValue = 0;
    private double mHomeLatitude = -1;
    private double mHomeLongitude = -1;

    public User(){

    }

    public User(float maxDistanceToday , float maxDistance){
        mDistanceMaxTodayValue = maxDistanceToday;
        mDistanceMaxValue = maxDistance;
    }

    public Map<String, Object> toMap(){
        HashMap<String, Object> result = new HashMap<>();

        result.put("maxDistance", mDistanceMaxValue);
        result.put("maxDistanceToday", mDistanceMaxTodayValue);

        return result;
    }

    public void setHomeLocation(double latitude, double longitude){
        setHomeLongitude(longitude);
        setHomeLatitude(latitude);
    }

    public void setUserInfo(float maxDistanceToday , float maxDistance){
        mDistanceMaxTodayValue = maxDistanceToday;
        mDistanceMaxValue = maxDistance;
    }

    public float getDistanceMaxTodayValue() {
        return mDistanceMaxTodayValue;
    }

    public void setDistanceMaxTodayValue(float mDistanceMaxTodayValue) {
        this.mDistanceMaxTodayValue = mDistanceMaxTodayValue;
    }

    public float getDistanceMaxValue() {
        return mDistanceMaxValue;
    }

    public void setDistanceMaxValue(float mDistanceMaxValue) {
        this.mDistanceMaxValue = mDistanceMaxValue;
    }

    public double getHomeLatitude() {
        return mHomeLatitude;
    }

    public void setHomeLatitude(double mHomeLatitude) {
        this.mHomeLatitude = mHomeLatitude;
    }

    public double getHomeLongitude() {
        return mHomeLongitude;
    }

    public void setHomeLongitude(double homeLongitude) {
        this.mHomeLongitude = homeLongitude;
    }

    public boolean isHomeLocationSet() {
        if(mHomeLongitude != -1 && mHomeLatitude != -1) return true;
        else return false;
    }
}
