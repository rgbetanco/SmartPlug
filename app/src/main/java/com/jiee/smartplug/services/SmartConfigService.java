package com.jiee.smartplug.services;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

import com.integrity_project.smartconfiglib.SmartConfig;
import com.integrity_project.smartconfiglib.SmartConfigListener;
import com.jiee.smartplug.utils.NetworkUtil;
import com.jiee.smartplug.utils.SmartConfigConstants;

/** TO DO:
 *
 /*
 SmartConfigService.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 This class starts the smart config routine to provide username and password to the smartplug, this is an
 adaptation and implementation of the original TI library

 */

public class SmartConfigService extends IntentService {

    byte[] freeData;
    SmartConfig smartConfig;
    SmartConfigListener smartConfigListener;

    String local_ssid;
    String local_pass;

    public SmartConfigService() {

        super("SmartConfigService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String ssid = intent.getStringExtra("ssid");
        String pass = intent.getStringExtra("pass");

        local_ssid = ssid;
        local_pass = pass;

        long endTime = System.currentTimeMillis() + 20*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    smartconfig_start();
                    wait(endTime - System.currentTimeMillis());
                    stopSmartConfig();
                    //broadcast back
                    Intent intent1 = new Intent("smartconfig");
                    intent1.putExtra("DONE", true);
                    sendBroadcast(intent1);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void smartconfig_start() {
        int networkState = NetworkUtil.getConnectionStatus(this);
        if (networkState != NetworkUtil.WIFI) { //if user isn't connected to wifi
            Toast.makeText(this,"Please connect to a WiFi network", Toast.LENGTH_SHORT).show();
        } else {
            // show "no password" dialog if user didn't enter a password
            if (local_pass.toString().equals("")) {
                Toast.makeText(this, "Password cannot be blank", Toast.LENGTH_SHORT).show();
            } else {

            //    Intent i = new Intent(this, UDPListenerService.class);
            //    Intent j = new Intent(this, mDNSTesting.class);
                try{
            //        stopService(i);
            //        stopService(j);
                } catch (Exception e){
                    e.printStackTrace();
                }
                startSmartConfig();
            }
        }
    }

    public void startSmartConfig() {

        String gateway = NetworkUtil.getGateway(this);

        freeData = new byte[1];
        freeData[0] = 0x03;

        smartConfig = null;
        smartConfigListener = new SmartConfigListener() {
            @Override
            public void onSmartConfigEvent(SmartConfigListener.SmtCfgEvent event, Exception e) {}
        };
        try {
            smartConfig = new SmartConfig(smartConfigListener, freeData, local_pass, null, gateway, local_ssid, (byte) 0, "");
            smartConfig.transmitSettings();
            System.out.println("SmartConfig started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopSmartConfig() {
        try {
            smartConfig.stopTransmitting();
            System.out.println("SmartConfig stopped");
            Intent i = new Intent("smartconfig_stopped");
            sendBroadcast(i);
        } catch (Exception e) {
            e.printStackTrace();
        }


    //    Intent i = new Intent(this, UDPListenerService.class);
    //    Intent j = new Intent(this, mDNSTesting.class);
        try{
    //        startService(i);
    //        startService(j);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

}
