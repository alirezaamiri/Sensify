package com.example.android.wifidirect.discovery;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ProgressBar;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.result);
        room_1_light_bar = (ProgressBar) findViewById(R.id.room_1_light);
        room_1_sound_bar = (ProgressBar) findViewById(R.id.room_1_sound);
        room_2_light_bar = (ProgressBar) findViewById(R.id.room_2_light);
        room_2_sound_bar = (ProgressBar) findViewById(R.id.room_2_sound);

        room_1_light_bar.setMax(100);
        room_1_sound_bar.setMax(100);
        room_2_light_bar.setMax(100);
        room_2_sound_bar.setMax(100);

        room_1_light = 35;
        room_1_sound = 100;
        room_2_light = 78;
        room_2_sound = 12;

        room_1_light_bar.setProgress((int)room_1_light);
        room_1_sound_bar.setProgress((int)room_1_sound);
        room_2_light_bar.setProgress((int)room_2_light);
        room_2_sound_bar.setProgress((int)room_2_sound);



    }
}
