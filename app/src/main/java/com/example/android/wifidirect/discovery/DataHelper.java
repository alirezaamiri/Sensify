package com.example.android.wifidirect.discovery;

import android.hardware.Sensor;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Created by Alireza on 9/29/2015.
 */
public class DataHelper {

    ArrayList<String[]> peersResponse;

    public DataHelper(){
        peersResponse = new ArrayList<String[]>(10);
    }

    public String getCheckedSensor(CheckBox lightCB, CheckBox soundCB, CheckBox proximityCB){
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

    public String getSensorValue(int roomNumber, Sensor lightSensor, Sensor proximitySensor,String sensor,float lux,float sound ,float proximityValue,boolean lightIsAllowed,boolean soundIsAllowed, boolean proximityIsAllowed){
        String result="";
        if (sensor.contains("light")){
            float luxResult = Math.round((lux/lightSensor.getMaximumRange())*10000);
            result += "light:"+((lightIsAllowed==true)?luxResult:" Not Allowed")+":";
        }
        if (sensor.contains("sound")){
            double soundResult = Math.round(20 * Math.log10(sound / 20) * 100);
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

    public String beautifyResult(String input){
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

    public void addToPeersResponse(String[] response){
        peersResponse.add(response);
    }


    public ArrayList<String[]> getPeersResponse(){
        return peersResponse;
    }


}
