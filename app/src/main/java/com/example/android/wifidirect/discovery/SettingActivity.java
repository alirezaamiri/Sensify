package com.example.android.wifidirect.discovery;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * Created by Alireza on 7/28/2015.
 */
public class SettingActivity extends Activity {

    private Button cancelButton;
    private Button applyButton;

    private CheckBox soundCb;
    private CheckBox lightCb;
    private CheckBox proximityCb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        Intent intent = getIntent();
        boolean sound = intent.getBooleanExtra("sound", true); //if it's a string you stored.
        boolean light = intent.getBooleanExtra("light", true);
        final boolean proximity = intent.getBooleanExtra("proximity",true);

        cancelButton = (Button) findViewById(R.id.cancel_setting);
        applyButton = (Button) findViewById(R.id.apply_setting);
        soundCb = (CheckBox) findViewById(R.id.soundCbSetting);
        lightCb = (CheckBox) findViewById(R.id.lightCbSetting);
        proximityCb = (CheckBox) findViewById(R.id.proximityCbSetting);

        if (sound){
            soundCb.setChecked(true);
        }else soundCb.setChecked(false);

        if (light){
            lightCb.setChecked(true);
        }else lightCb.setChecked(false);

        if (proximity){
            proximityCb.setChecked(true);
        }else proximityCb.setChecked(false);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent output = new Intent();
                setResult(RESULT_OK, output);
                finish();
            }
        });

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent output = new Intent();
                output.putExtra("light", lightCb.isChecked());
                output.putExtra("sound", soundCb.isChecked());
                output.putExtra("proximity", proximityCb.isChecked());
                setResult(RESULT_OK, output);
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() { }
}
