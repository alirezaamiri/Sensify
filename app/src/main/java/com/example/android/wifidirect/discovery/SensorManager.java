package com.example.android.wifidirect.discovery;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by Alireza on 6/18/2015.
 */
public class SensorManager implements SensorEventListener {

    private WiFiServiceDiscoveryActivity activity;
    private android.hardware.SensorManager sensorManager;
    private Sensor lightSensor;

    private float lux;

    public SensorManager(WiFiServiceDiscoveryActivity c){
        this.activity = c;
        lux = 0;


    }

    public float getLightSensor(){
        if (lux==0){lux = -1;}
        return lux;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}