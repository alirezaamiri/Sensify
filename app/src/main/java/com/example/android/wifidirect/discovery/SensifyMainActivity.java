
package com.example.android.wifidirect.discovery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.*;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;




public class SensifyMainActivity extends Activity implements
        Handler.Callback,
        ConnectionInfoListener,SensorEventListener,
        BeaconConsumer{

    public static final String TAG = "sensifyTag";

    private BeaconManager beaconManager;

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int SEND_NEEDED_SENSORS = 0x400 + 2;
    private WifiP2pManager manager;

    static final int SERVER_PORT = 4545;

    DataHelper dataHelper;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;

    private Handler handler = new Handler(this);

    static private TextView statusTxtView;
    public TextView sensorValueTxtView;
    public TextView soundValueTxtView;
    public TextView proximityValueTxtView;
    private TextView numberOfPeersTxtView;
    private TextView roomNumberTxtView;
    private CheckBox lightCB;
    private CheckBox soundCB;
    private CheckBox proximityCB;

    private Button resultBtn;
    private Button connectBtn;
    private Button stopDiscoveryBtn;
    private Button startDiscoveryBtn;
    private ImageButton settingBtn;
    private Activity mainActivity;

    private boolean lightIsAllowed = true;
    private boolean soundIsAllowed = true;
    private boolean proximityIsAllowed = true;

    private boolean sendData = false;

    private MessageManager messageManager;
    private Handler resetDiscoveryHandler;


    private int roomNumber;
    private int peerNumber;
    private int peerNumberCounter;
    private int minRssi;

    private android.hardware.SensorManager mSensorManager;
    private AudioRecord audioRecorder = null;
    private int minSizeRecord;
    private Sensor lightSensor;
    private Sensor proximitySensor;
    private float lux;
    private float proximityValue;

    WifiConnectivity wifiConnectivity;

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.main_edited);

        // used for appendStatus function
        peerNumberCounter = 0;
        minRssi = -140;
        sendData = false;

        dataHelper = new DataHelper();

        statusTxtView = (TextView) findViewById(R.id.status_text);
        sensorValueTxtView = (TextView) findViewById(R.id.lightTxtView);
        soundValueTxtView = (TextView) findViewById(R.id.soundTxtView);
        proximityValueTxtView = (TextView) findViewById(R.id.proximityTxtView);
        numberOfPeersTxtView = (TextView) findViewById(R.id.numberOfPeers);
        roomNumberTxtView = (TextView) findViewById(R.id.roomNumber);
        lightCB = (CheckBox) findViewById(R.id.lightCb);
        soundCB = (CheckBox) findViewById(R.id.soundCb);
        proximityCB = (CheckBox) findViewById(R.id.proximityCb);
        connectBtn = (Button) findViewById(R.id.connect_btn);
        stopDiscoveryBtn = (Button) findViewById(R.id.stop_discovery_btn);
        startDiscoveryBtn = (Button) findViewById(R.id.start_discovery_btn);
        settingBtn = (ImageButton) findViewById(R.id.setting_btn);
        resultBtn = (Button) findViewById(R.id.result_btn);

        mainActivity = this;

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        wifiConnectivity = new WifiConnectivity(mainActivity,manager,channel,numberOfPeersTxtView);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mSensorManager = (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, lightSensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
        proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(this, proximitySensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);

        // Button click listeners
        stopDiscoveryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiConnectivity.stopDiscovery();
                resetDiscoveryHandler.removeMessages(0);//for stoping discovery
            }
        });
        startDiscoveryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetDiscoveryHandler.sendEmptyMessage(0);//for starting discovery
            }
        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingIntent = new Intent(SensifyMainActivity.this, SettingActivity.class);
                settingIntent.putExtra("light", lightIsAllowed); //Optional parameters
                settingIntent.putExtra("sound", soundIsAllowed);
                settingIntent.putExtra("proximity", proximityIsAllowed);
                SensifyMainActivity.this.startActivityForResult(settingIntent, 123);
            }
        });

        resultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] temp = new String[4];
                temp[0] = roomNumber+"";
                temp[1] = Math.round(20 * Math.log10(getMaxAmplitude() / 20) * 100)+"";
                temp[2] = lux+"";
                temp[3] = proximityValue+"";
                dataHelper.addToPeersResponse(temp);

                Intent resultIntent = new Intent(SensifyMainActivity.this, ResultActivity.class);
                resultIntent.putExtra("data", dataHelper.getPeersResponse());
                SensifyMainActivity.this.startActivityForResult(resultIntent, 321);
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float sound = Math.round(20 * Math.log10(getMaxAmplitude() / 20) * 100);
                openLink(lux, sound, roomNumber);
                sendData = true;
                if (wifiConnectivity.isDeviceListEmpty())
                    Toast.makeText(mainActivity,"There is no peers!!!...",Toast.LENGTH_SHORT).show();
                else{
                    peerNumber = wifiConnectivity.getDeviceListSize();
                    wifiConnectivity.createGroup();
                    wifiConnectivity.stopDiscovery();
                    resetDiscoveryHandler.removeMessages(0);
                }
            }
        });

        roomNumberTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                roomNumberTxtView.setText("Room: " + roomNumber);
            }
        });

        startRecording();
        wifiConnectivity.startRegistrationAndDiscovery();


        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=004C,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

    }

    private void openLink(float lightValue,float soundValue,int room) {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String macAddress = wInfo.getMacAddress();
        String strURL = "http://8-dot-sensify-go.appspot.com/postsensorvalue?nodeid="
                + macAddress + "&sensor1value=" + lightValue+"&sensor2value=" + soundValue+"&sensor3value=" + 1+"&tagcode="+room;
        new RequestTask().execute(strURL);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_READ:
                messageManager = (MessageManager)msg.obj;
                byte[] readBuf = (byte[]) messageManager.bufferSend;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Log.d(TAG, readMessage);
//                appendStatus("from peers : " + readMessage);
                if (readMessage.contains("sensors")) {
                    appendStatus("from peers : " + readMessage.replace('-',' '));
                    messageManager.write(dataHelper.getSensorValue(roomNumber, lightSensor, proximitySensor, readMessage, lux, (float) getMaxAmplitude(), proximityValue, lightIsAllowed, soundIsAllowed, proximityIsAllowed).getBytes(Charset.forName("UTF-8")));
                }else if (readMessage.contains("result")){
                    peerNumberCounter++;
                    appendStatus("result : " + dataHelper.beautifyResult(readMessage));
                    messageManager.write("done".getBytes(Charset.forName("UTF-8")));
                    Log.d("sensify","result received");
                    if (peerNumber == peerNumberCounter) { // it means we get data from all peers
                        manager.removeGroup(channel, new ActionListener() {
                            @Override
                            public void onSuccess() {
                                Log.d("sensify","Communication is terminated successfully...");
                            }

                            @Override
                            public void onFailure(int i) {
                                Log.d("sensify","Communication cannot be terminated!!!");
                            }
                        });
                    }
                }else if (readMessage.contains("done")){

                }
                break;

            case SEND_NEEDED_SENSORS:
                appendStatus("called sensor need");
                Object obj = msg.obj;
                messageManager = ((MessageManager) obj);
                messageManager.write(dataHelper.getCheckedSensor(lightCB, soundCB, proximityCB).getBytes(Charset.forName("UTF-8")));
        }
        return true;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Thread handler = null;
        /*
         * The group owner accepts connections using a server socket and then spawns a
         * client socket for every client. This is handled by {@code
         * GroupOwnerSocketHandler}
         */

        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new GroupOwnerSocketHandler(this.getHandler(), sendData);
                handler.start();
            } catch (IOException e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
                return;
            }
        } else {
            Log.d(TAG, "Connected as peer");
            handler = new ClientSocketHandler(
                    this.getHandler(),
                    p2pInfo.groupOwnerAddress, sendData);
            handler.start();
        }

    }

    public static void appendStatus(String status) {
        SimpleDateFormat dateFormat= new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        String cDateTime=dateFormat.format(new Date());
        String current = statusTxtView.getText().toString();
        statusTxtView.setText(current +cDateTime +": "+ status+ "\n");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            lux = Math.round((sensorEvent.values[0]/lightSensor.getMaximumRange())*10000);
            sensorValueTxtView.setText(""+lux+"\n"+sensorEvent.values[0]);
            soundValueTxtView.setText(""+Math.round(20 * Math.log10(getMaxAmplitude() / 20) * 100));
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY){
            proximityValue = sensorEvent.values[0];
            proximityValueTxtView.setText(""+(sensorEvent.values[0]/proximitySensor.getMaximumRange()));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };

    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);
                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED){
                                minSizeRecord= bufferSize;
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }
        return null;
    }

    public void startRecording() {
        audioRecorder = findAudioRecord();
        audioRecorder.startRecording();
    }

    public void stopRecording() {
        if (audioRecorder != null) {
            audioRecorder.stop();
        }
    }

    public double getMaxAmplitude(){
        short buffer[] = new short[minSizeRecord];
        int read = audioRecorder.read(buffer, 0, minSizeRecord);
        double p2 = buffer[buffer.length-1];
        double decibel;
        double max = 0;
        for (short s : buffer)
        {
            if (Math.abs(s) > max)
            {
                max = Math.abs(s);
            }
        }
        // this is for converting the value of microphone to decibel
//        if (p2==0)
//            decibel = Double.NEGATIVE_INFINITY;
//        else
//            decibel = 20.0*Math.log10(p2/65535.0);
        return max;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode== 123 && resultCode == RESULT_OK && data != null) {
            lightIsAllowed = data.getBooleanExtra("light",true);
            soundIsAllowed = data.getBooleanExtra("sound",true);
            proximityIsAllowed = data.getBooleanExtra("proximity",true);
        }
    }

    @Override
    protected void onStop() {
        if (manager != null && channel != null) {
            manager.removeGroup(channel, new ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                }

                @Override
                public void onSuccess() {
                }

            });
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        mSensorManager.unregisterListener(this, lightSensor);
        mSensorManager.unregisterListener(this, proximitySensor);
        stopRecording();
        beaconManager.unbind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, org.altbeacon.beacon.Region region) {
                if (beacons.size() > 0 ) {
                    for (Beacon b : beacons) {
                        int rssi = b.getRssi();
                        if ( rssi > minRssi ) {
                            Log.d("beacon","This is lower -->"+rssi);
                            minRssi = rssi;
                            roomNumber = b.getId2().toInt();
                        }
                    }
                    Log.d("beacon", "room number has been found");
                    try {
                        if (roomNumber != 0) roomNumberTxtView.setText("Room: " + roomNumber);
                    }catch(Exception e){

                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new org.altbeacon.beacon.Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {
            appendStatus("error for bluetooth ");
        }
    }
}