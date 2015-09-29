package com.example.android.wifidirect.discovery;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;

import java.util.ArrayList;

/**
 * Created by Alireza on 8/27/2015.
 */
public class ResultActivity extends Activity {
    private float room_1_light;
    private float room_1_sound;
    private float room_2_light;
    private float room_2_sound;

    private ProgressBar room_1_light_bar;
    private ProgressBar room_1_sound_bar;
    private ProgressBar room_2_light_bar;
    private ProgressBar room_2_sound_bar;

    ArrayList<String[]> results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        results = (ArrayList<String[]>)intent.getSerializableExtra("data");

        setContentView(R.layout.result);
        room_1_light_bar = (ProgressBar) findViewById(R.id.room_1_light);
        room_1_sound_bar = (ProgressBar) findViewById(R.id.room_1_sound);
        room_2_light_bar = (ProgressBar) findViewById(R.id.room_2_light);
        room_2_sound_bar = (ProgressBar) findViewById(R.id.room_2_sound);

        room_1_light = getLightRoom1(results);
        room_1_sound = getSoundRoom1(results);
        room_2_light = getLightRoom2(results);
        room_2_sound = getSoundRoom2(results);

        int maxLight = (room_1_light>room_2_light)?(int)room_1_light:(int)room_2_light;
        int maxSound = (room_1_sound>room_2_sound)?(int)room_1_sound:(int)room_2_sound;

        room_1_light_bar.setMax(maxLight);
        room_1_sound_bar.setMax(maxSound);
        room_2_light_bar.setMax(maxLight);
        room_2_sound_bar.setMax(maxSound);


        room_1_light_bar.setProgress((int)room_1_light);
        room_1_sound_bar.setProgress((int)room_1_sound);
        room_2_light_bar.setProgress((int)room_2_light);
        room_2_sound_bar.setProgress((int)room_2_sound);

    }

    // 1 sound - 2 light - 3 proximity

    private int getLightRoom1(ArrayList<String[]> results){
        float average = 0;
        int counter = 0;
        for (int i = 0;i<results.size();i++){
            String[] temp =results.get(i);
            if (Integer.parseInt(temp[0])==1){
                average += Float.parseFloat(temp[2]);
                counter++;
            }
        }

        if (counter!=0)average  = average/counter;
        else average = 0;
        return (int)average;
    }

    private int getSoundRoom1(ArrayList<String[]> results){
        float average = 0;
        int counter = 0;
        for (int i = 0;i<results.size();i++){
            String[] temp =results.get(i);
            if (Integer.parseInt(temp[0])==1){
                average += Float.parseFloat(temp[1]);
                counter++;
            }
        }

        if (counter!=0)average  = average/counter;
        else average = 0;
        return (int)average;
    }

    private int getLightRoom2(ArrayList<String[]> results){
        float average = 0;
        int counter = 0;
        for (int i = 0;i<results.size();i++){
            String[] temp =results.get(i);
            if (Integer.parseInt(temp[0])==2){
                average += Float.parseFloat(temp[2]);
                counter++;
            }
        }

        if (counter!=0)average  = average/counter;
        else average = 0;
        return (int)average;
    }

    private int getSoundRoom2(ArrayList<String[]> results){
        float average = 0;
        int counter = 0;
        for (int i = 0;i<results.size();i++){
            String[] temp =results.get(i);
            if (Integer.parseInt(temp[0])==2){
                average += Float.parseFloat(temp[1]);
                counter++;
            }
        }

        if (counter!=0)average  = average/counter;
        else average = 0;
        return (int)average;
    }
}


