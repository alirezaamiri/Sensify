package com.example.android.wifidirect.discovery;

import android.app.Activity;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alireza on 9/29/2015.
 */
public class WifiConnectivity {

    public static final String SERVICE_INSTANCE = "_device";
    public static final String SENDER_INSTANCE = "_sender";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    final String TAG = "Service Discovery";

    final WifiP2pManager managerFinal;
    final WifiP2pManager.Channel channelFinal;

    private WifiP2pDnsSdServiceRequest serviceRequest;
    private ArrayList<WiFiP2pService> deviceList;
    private final boolean doNotSearchFinal = false;


    Activity mainActivity;
    Handler resetDiscoveryHandler;
    TextView numberOfPeersTxtView;


    public WifiConnectivity(Activity activity, WifiP2pManager manager, WifiP2pManager.Channel channel,TextView numberOfPeersDiscovery){
        mainActivity = activity;
        managerFinal = manager;
        channelFinal = channel;
        this.numberOfPeersTxtView = numberOfPeersDiscovery;

        resetDiscoveryHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) { // for starting the discovery
                    deviceList.clear();
                    numberOfPeersTxtView.setText("Peers : 0");
                    stopDiscovery();
                    startDiscovery();
                    SensifyMainActivity.appendStatus("Service Discovery Reset");
                    sendEmptyMessageDelayed(0, 15000); /// reset discovery is done every 15 seconds
                }else{
                    super.handleMessage(msg);
                }
            }
        };
    }

    public void discoverService() {

        /*
         * Register listenerSs for DNS-D services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
        final TextView numberOfPeersTxtViewFinal = numberOfPeersTxtView;
        managerFinal.setDnsSdResponseListeners(channelFinal,
                new WifiP2pManager.DnsSdServiceResponseListener() {

                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                            // update the UI and add the item the discovered
                            // device.
                            WiFiP2pService service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            if (!doNotSearchFinal) {
                                deviceList.add(service);
                                numberOfPeersTxtViewFinal.setText(mainActivity.getString(R.string.number_of_peers_string, deviceList.size()));
                            }
                            Log.d(TAG, "onBonjourServiceAvailable "
                                    + instanceName);
                        } else if (instanceName.equalsIgnoreCase(SENDER_INSTANCE)) {
                            WiFiP2pService service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            connectP2p(service, 0);
                        }

                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {

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


    // used for connecting a peer to a group owner
    public void connectP2p(WiFiP2pService service, int intentValue) {
        final int intentValueFinal = intentValue;

        final WiFiP2pService serviceForConnect = service;
        managerFinal.requestGroupInfo(channelFinal, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                if (wifiP2pGroup != null) {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.groupOwnerIntent = intentValueFinal;
                    config.deviceAddress = serviceForConnect.device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
//                    if (sendData)
                    config.groupOwnerIntent = 0;// I want this device to be peer
                    managerFinal.connect(channelFinal, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            SensifyMainActivity.appendStatus("Connecting to service");
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            SensifyMainActivity.appendStatus("Failed connecting to service " + errorCode);
                        }
                    });
                } else {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.groupOwnerIntent = intentValueFinal;
                    config.deviceAddress = serviceForConnect.device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
                    managerFinal.connect(channelFinal, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            SensifyMainActivity.appendStatus("Connecting to service-no group");
                        }

                        @Override
                        public void onFailure(int errorCode) {
                            SensifyMainActivity.appendStatus("Failed connecting to service-no group " + errorCode);
                        }
                    });
                }
            }
        });

        stopDiscovery();
        resetDiscoveryHandler.removeMessages(0);//stop discovery
    }

    /**
     * Registers a local service and then initiates a service discovery
     */
    public void startRegistrationAndDiscovery() {
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        managerFinal.addLocalService(channelFinal, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                SensifyMainActivity.appendStatus("Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                SensifyMainActivity.appendStatus("Failed to add a service");
            }
        });
        discoverService();
    }

    public void startDiscovery(){
        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        final Handler handler1 = new Handler();
        managerFinal.addServiceRequest(channelFinal, serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
//                        appendStatus("Added service discovery request");
                        //There are supposedly a possible race-condition bug with the service discovery
                        // thus to avoid it, we are delaying the service discovery start here
                        handler1.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                managerFinal.discoverServices(channelFinal, new WifiP2pManager.ActionListener() {

                                    @Override
                                    public void onSuccess() {
//                                        appendStatus("Service discovery initiated");
                                    }

                                    @Override
                                    public void onFailure(int arg0) {
                                        SensifyMainActivity.appendStatus("Service discovery failed");
                                    }
                                });
                            }
                        }, 1000);
                    }

                    @Override
                    public void onFailure(int arg0) {
                        SensifyMainActivity.appendStatus("Failed adding service discovery request");
                    }
                });
    }

    public void stopDiscovery(){
        managerFinal.removeServiceRequest(channelFinal, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {
                SensifyMainActivity.appendStatus("error in removing discovery service");
            }
        });
    }


    public void createGroup(){
        Map<String, String> record = new HashMap<String, String>();

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SENDER_INSTANCE, SERVICE_REG_TYPE, record);
        managerFinal.addLocalService(channelFinal, service, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                SensifyMainActivity.appendStatus("Added Local Service for group");
            }

            @Override
            public void onFailure(int error) {
                SensifyMainActivity.appendStatus("Failed to add a service for group");
            }
        });
    }

    public ArrayList<WiFiP2pService> getDeviceList(){
        return deviceList;
    }

    public void addToDeviceList(WiFiP2pService peer){
        deviceList.add(peer);
    }

    public int getDeviceListSize(){
        return deviceList.size();
    }

    public void emptyDeviceList(){

    }

    public boolean isDeviceListEmpty(){
        return deviceList.isEmpty();
    }

}
