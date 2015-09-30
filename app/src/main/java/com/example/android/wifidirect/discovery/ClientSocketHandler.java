
package com.example.android.wifidirect.discovery;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocketHandler extends Thread {

    private static final String TAG = "ClientSocketHandler";
    private Handler handler;
    private MessageManager chat;
    private InetAddress mAddress;
    private boolean isSender;
    private Socket socket;
    private int i;

    public ClientSocketHandler(Handler handler, InetAddress groupOwnerAddress, boolean isSender) {
        this.handler = handler;
        this.isSender = isSender;
        this.mAddress = groupOwnerAddress;
        this.socket = new Socket();
        i = 1;
    }

    @Override
    public void run() {
        try {
            if (socket.isConnected()) socket.close();
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                    SensifyMainActivity.SERVER_PORT), 5000);

            while (!socket.isConnected()){
                socket.connect(new InetSocketAddress(mAddress.getHostAddress(),
                        SensifyMainActivity.SERVER_PORT+i), 5000);
                i++;
            }
            Log.d(TAG, "Launching the I/O handler");
            chat = new MessageManager(socket, handler,isSender);
            new Thread(chat).start();

//            handler.obtainMessage(SensifyMainActivity.SEND_NEEDED_SENSORS,chat).sendToTarget();
//            Log.d(TAG, "message sent "+((chat==null)?"null":"not null"));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }

    public void closeSocket(){
        try {
            socket.close();
            socket = null;
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
