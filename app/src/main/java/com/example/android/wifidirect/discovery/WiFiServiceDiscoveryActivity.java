
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
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * The main activity for the sample. This activity registers a local service and
 * perform discovery over Wi-Fi p2p network. It also hosts a couple of fragments
 * to manage chat operations. When the app is launched, the device publishes a
 * chat service and also tries to discover services published by other peers. On
 * selecting a peer published service, the app initiates a Wi-Fi P2P (Direct)
 * connection with the peer. On successful connection with a peer advertising
 * the same service, the app opens up sockets to initiate a chat.
 * is then added to the the main activity which manages
 * the interface and messaging needs for a chat session.
 */
public class WiFiServiceDiscoveryActivity extends Activity implements
        Handler.Callback,
        ConnectionInfoListener,SensorEventListener,
        BeaconConsumer{

    public static final String TAG = "sensifyTag";

    public WiFiP2pService serviceForConnect;
    private SimpleDateFormat dateFormat;

    private BeaconManager beaconManager;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_device";
    public static final String SENDER_SERVICE_INSTANCE = "_sender";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int SEND_NEEDED_SENSORS = 0x400 + 2;
    private WifiP2pManager manager;

    static final int SERVER_PORT = 4545;
    static int intentValue = 0;

    private final IntentFilter intentFilter = new IntentFilter();
    private Channel channel;
    private BroadcastReceiver receiver = null;
    private WifiP2pDnsSdServiceRequest serviceRequest;

    private Handler handler = new Handler(this);
    private ArrayList<WiFiP2pService> deviceList;

    private TextView statusTxtView;
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
    private boolean doNotSearch = false;

    private MessageManager messageManager;
    private Handler resetDiscoveryHandler;


    private int roomNumber;
    private int peerNumber;
    private int peerNumberCounter;
    private boolean checkRoom = true;
    private int minRssi;
    private ArrayList<String[]> peersResponse;

    private android.hardware.SensorManager mSensorManager;
    private AudioRecord audioRecorder = null;
    private int minSizeRecord;
    private Sensor lightSensor;
    private Sensor proximitySensor;
    private float lux;
    private float proximityValue;

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // used for appendStatus function
        peerNumberCounter = 0;
        dateFormat= new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        minRssi = -140;
        peersResponse = new ArrayList<String[]>(10);
        setContentView(R.layout.main_edited);
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
        mainActivity = this;

        resultBtn = (Button) findViewById(R.id.result_btn);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter
                .addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);



        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        sendData = false;


        deviceList = new ArrayList<WiFiP2pService>();
        startRecording();

        mSensorManager = (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, lightSensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
        proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener(this, proximitySensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);


        appendStatus(lightSensor.getMaximumRange() + "");

        resetDiscoveryHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) { // for starting the discovery
                    deviceList.clear();
                    numberOfPeersTxtView.setText("Peers : 0");
                    stopDiscovery();
                    startDiscovery();
                    appendStatus("Service Discovery Reset");
                    sendEmptyMessageDelayed(0, 15000); /// reset discovery is done every 15 seconds
                }else{
                    super.handleMessage(msg);
                }
            }
        };
        stopDiscoveryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopDiscovery();
                resetDiscoveryHandler.removeMessages(0);
//                resetDiscoveryHandler.sendEmptyMessage(1); // for stoping discovery
            }
        });
        startDiscoveryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetDiscoveryHandler.sendEmptyMessage(0);
            }
        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingIntent = new Intent(WiFiServiceDiscoveryActivity.this, SettingActivity.class);
                settingIntent.putExtra("light", lightIsAllowed); //Optional parameters
                settingIntent.putExtra("sound", soundIsAllowed);
                settingIntent.putExtra("proximity", proximityIsAllowed);
                WiFiServiceDiscoveryActivity.this.startActivityForResult(settingIntent, 123);
            }
        });

        startRegistrationAndDiscovery();

        resultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingIntent = new Intent(WiFiServiceDiscoveryActivity.this, ResultActivity.class);
                WiFiServiceDiscoveryActivity.this.startActivityForResult(settingIntent, 321);
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData = true;

                if (deviceList.isEmpty())
                    Toast.makeText(mainActivity,"There is no peers!!!...",Toast.LENGTH_SHORT).show();
                else{
//                    doNotSearch = true;
//                    Log.d("sssssssssssssss", deviceList.get(0).device.deviceName);
                    peerNumber = deviceList.size();
                    createGroup();
                    stopDiscovery();
                    resetDiscoveryHandler.removeMessages(0);
//                    connectP2p(deviceList.get(0));
                }
            }
        });

        roomNumberTxtView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                roomNumberTxtView.setText("Room: "+roomNumber);
            }
        });

        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=004C,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);

    }

    private void createGroup(){
        Map<String, String> record = new HashMap<String, String>();
//        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SENDER_SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Added Local Service for group");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service for group");
            }
        });


    }

    private void resetWholeprocess(){
//        manager.removeGroup(channel, new ActionListener() {
//            @Override
//            public void onSuccess() {
//
//            }
//
//            @Override
//            public void onFailure(int i) {
//                Log.d("sensify", "removing group has failed");
//            }
//        });

        channel = null;
        channel = manager.initialize(this, getMainLooper(), null);
    }

    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        manager.addLocalService(channel, service, new ActionListener() {

            @Override
            public void onSuccess() {
                appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                appendStatus("Failed to add a service");
            }
        });

        discoverService();

    }

    private void stopDiscovery(){
        manager.removeServiceRequest(channel, serviceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
//                appendStatus("remove discovery service");
            }

            @Override
            public void onFailure(int i) {
                appendStatus("error in removing discovery service");
            }
        });
    }

    private void startDiscovery(){
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        final Handler handler1 = new Handler();
        manager.addServiceRequest(channel, serviceRequest,
                new ActionListener() {
                    @Override
                    public void onSuccess() {
//                        appendStatus("Added service discovery request");
                        //There are supposedly a possible race-condition bug with the service discovery
                        // thus to avoid it, we are delaying the service discovery start here
                        handler1.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                manager.discoverServices(channel, new ActionListener() {

                                    @Override
                                    public void onSuccess() {
//                                        appendStatus("Service discovery initiated");
                                    }

                                    @Override
                                    public void onFailure(int arg0) {
                                        appendStatus("Service discovery failed");
                                    }
                                });
                            }
                        }, 1000);
                    }

                    @Override
                    public void onFailure(int arg0) {
                        appendStatus("Failed adding service discovery request");
                    }
                });
    }

    private void discoverService() {

        /*
         * Register listenerSs for DNS-D services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */

        manager.setDnsSdResponseListeners(channel,
                new DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?
//                        appendStatus("ssss -> " + instanceName);
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                            // update the UI and add the item the discovered
                            // device.
                            WiFiP2pService service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            if (!doNotSearch) {
                                deviceList.add(service);
                                numberOfPeersTxtView.setText(getString(R.string.number_of_peers_string, deviceList.size()));
                            }
                            Log.d(TAG, "onBonjourServiceAvailable "
                                    + instanceName);
                        } else if (instanceName.equalsIgnoreCase(SENDER_SERVICE_INSTANCE)) {
                            WiFiP2pService service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            connectP2p(service);
//                            appendStatus("I see some one: " + instanceName);
                        }

                    }
                }, new DnsSdTxtRecordListener() {

                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(TAG,
                                device.deviceName + " is "
                                        + record.get(TXTRECORD_PROP_AVAILABLE));
                    }
                });

        // After attaching listeners, create a service request and initiate
        // discovery.
        startDiscovery();
        resetDiscoveryHandler.sendEmptyMessage(0);
    }

    public void connectP2p(WiFiP2pService service) {
        serviceForConnect = service;
        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                if (wifiP2pGroup != null) {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.groupOwnerIntent = intentValue;
                    config.deviceAddress = serviceForConnect.device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
//                    if (sendData)
                    config.groupOwnerIntent = 0;// I want this device to be peer
                    channel = manager.initialize(mainActivity, getMainLooper(), null);
                    manager.connect(channel, config, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            appendStatus("Connecting to service");
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            appendStatus("Failed connecting to service " + errorCode);
                        }
                    });
//                    manager.removeGroup(channel, new ActionListener() {
//                        @Override
//                        public void onSuccess() {
//
//                        }
//
//                        @Override
//                        public void onFailure(int i) {
//                            Log.d("sensify", "removing group has failed");
//                        }
//                    });
                } else {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.groupOwnerIntent = intentValue;
                    config.deviceAddress = serviceForConnect.device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
                    manager.connect(channel, config, new ActionListener() {
                        @Override
                        public void onSuccess() {
                            appendStatus("Connecting to service-no group");
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            appendStatus("Failed connecting to service-no group " + errorCode);
                        }
                    });
                }
            }
        });

        stopDiscovery();
        resetDiscoveryHandler.removeMessages(0);//stop discovery
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
                if (readMessage.contains("sensors")){
                    appendStatus("from peers : " + readMessage.replace('-',' '));
                    messageManager.write(getSensorValue(readMessage).getBytes(Charset.forName("UTF-8")));
                }else if (readMessage.contains("result")){
                    peerNumberCounter++;
                    appendStatus("result : " + beautifyResult(readMessage));
                    messageManager.write("done".getBytes(Charset.forName("UTF-8")));
                    Log.d("sensify","result received" );
//                    deviceList.remove(0);
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
                break;

            case SEND_NEEDED_SENSORS:
                appendStatus("called sensor need");
                Object obj = msg.obj;
                messageManager = ((MessageManager) obj);
                messageManager.write(getCheckedSensor().getBytes(Charset.forName("UTF-8")));
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

    public void appendStatus(String status) {
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
//        if (p2==0)
//            decibel = Double.NEGATIVE_INFINITY;
//        else
//            decibel = 20.0*Math.log10(p2/65535.0);
        return max;
    }

    private String getCheckedSensor(){
        String result="";
        if (lightCB.isChecked())
            result += "light-";
        if (soundCB.isChecked())
            result += "sound-";
        if (proximityCB.isChecked())
            result += "proximity-";
        if (result != "") result = "sensors: " + result;
        return result;
    }

    private String getSensorValue(String sensor){
        String result="";
        if (sensor.contains("light")){
            float luxResult = Math.round((lux/lightSensor.getMaximumRange())*10000);
            result += "light:"+((lightIsAllowed==true)?luxResult:" Not Allowed")+":";
        }
        if (sensor.contains("sound")){
            double soundResult = Math.round(20 * Math.log10(getMaxAmplitude() / 20) * 100);
            result += "sound:"+((soundIsAllowed==true)?soundResult:" Not Allowed")+":";
        }
        if (sensor.contains("proximity")){
            float proximityResult = proximityValue/proximitySensor.getMaximumRange();
            result += "proximity:"+((proximityIsAllowed==true)?proximityResult:" Not Allowed")+":";
        }
        if (result != "") result = "result:" + result;
        result = result + "room:"+roomNumber;
        return result;
    }

    private String beautifyResult(String input){
        String result="";
        String[] temp = new String[4];

        input = input.substring(7);
        int clientRoomNumber = 0;
        StringTokenizer st = new StringTokenizer(input,":");
        while(st.hasMoreTokens()) {
            String firstPart = st.nextToken();
            if (firstPart.equalsIgnoreCase("room")){
                clientRoomNumber = Integer.parseInt(st.nextToken());
            }else {
                String secondPart = st.nextToken();
                result += "\n\t" + firstPart + " = " + secondPart;
                if(firstPart.contains("light")){
                    temp[1]=secondPart;
                }else if(firstPart.contains("sound")){
                    temp[2]=secondPart;
                }else if(firstPart.contains("proximity")){
                    temp[3]=secondPart;
                }
            }
        }
        temp[0] = (Integer.toString(clientRoomNumber));
        result += "\n\troom number = "+clientRoomNumber;
        peersResponse.add(temp);
        return result;
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
    protected void onRestart() {
        super.onRestart();
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