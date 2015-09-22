
package com.example.android.wifidirect.discovery;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to UI thread for UI updates.
 */
public class MessageManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    private boolean sendData;
    public byte[] bufferSend;

    public MessageManager(Socket socket, Handler handler,boolean sendData) {
        this.socket = socket;
        this.handler = handler;
        this.sendData = sendData;
    }

    private InputStream iStream;
    private OutputStream oStream;
    private static final String TAG = "MessageHandler";

    @Override
    public void run() {
        try {

            iStream = socket.getInputStream();
            oStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            bufferSend = buffer;
            int bytes;
            if (sendData)
                handler.obtainMessage(WiFiServiceDiscoveryActivity.SEND_NEEDED_SENSORS, this).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = iStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
                    Log.d(TAG, "Rec:" + new String(bufferSend, 0, bytes));
                    handler.obtainMessage(WiFiServiceDiscoveryActivity.MESSAGE_READ,
                            bytes, -1, this).sendToTarget();
                    String temp = new String(bufferSend, 0, bytes);
                    if (temp.contains("done"))
                        socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            oStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

}
