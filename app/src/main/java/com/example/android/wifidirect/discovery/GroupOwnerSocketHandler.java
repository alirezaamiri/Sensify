
package com.example.android.wifidirect.discovery;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of a ServerSocket handler. This is used by the wifi p2p
 * group owner.
 */
public class GroupOwnerSocketHandler extends Thread {

    private final int THREAD_COUNT = 10;
    private Handler handler;
    private static final String TAG = "ServerSocket";
    private boolean isSender;
    ServerSocket socket = null;
    int i ;

    public GroupOwnerSocketHandler(Handler handler,boolean isSender) throws IOException {
        try {
            i = 0;
            this.isSender = isSender;
            this.handler = handler;
            Log.d("GroupOwnerSocketHandler", "Socket Started");
            socket = new ServerSocket();

        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdownNow();
            throw e;
        }

    }

    /**
     * A ThreadPool for client sockets.
     */
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 1000, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Override
    public void run() {
        while (true) {
            try {

                try{
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(4545+i));
                }catch(Exception e){
                    i++;
                    continue;
                }
                i++;
                // A blocking operation. Initiate a ChatManager instance when
                // there is a new connection
                Log.d(TAG, "Launching the I/O handler" + i);
                pool.submit(new MessageManager(socket.accept(), handler, true));
//                pool.execute(new MessageManager(socket.accept(), handler,true));// it is the sender

            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                        Log.d(TAG, "socket close"+i);
                    }
                } catch (IOException ioe) {

                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
    }

}
