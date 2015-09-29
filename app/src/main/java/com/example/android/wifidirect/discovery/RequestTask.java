package com.example.android.wifidirect.discovery;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Alireza on 9/23/2015.
 */
public class RequestTask extends AsyncTask<String, String, String> {

    private Exception exception;
    @Override
    protected String doInBackground(String... uri) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                Log.d("parallel", "Data is sent to Server");
            } else{
                Log.d("parallel","close");
                //Closes the connection.
                response.getEntity().getContent().close();
//                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {

            Log.d("parallel", "client protocol exception");
        } catch (IOException e) {
            for (StackTraceElement ste : e.getStackTrace()) {
                Log.e("parallel",ste.toString());
            }
            Log.e("parallel","io exception");
//            e.printStackTrace();
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //Do anything with response..
    }
}

