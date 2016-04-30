package com.jiee.smartplug.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.jiee.smartplug.ListDevices;
import com.jiee.smartplug.M1;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;
import java.util.Locale;

/**
 * Created by ronaldgarcia on 22/4/16.
 */
public class M1ServicesService extends IntentService {

    public static String ip = "";
    public static int serviceId = 0;
    public static byte action = 0;
    public static String token = "";
    public static Activity activity = null;
    public static String mac = "";

    UDPCommunication udp = new UDPCommunication();
    MySQLHelper sql = new MySQLHelper(activity);
    GlobalVariables gb = new GlobalVariables();
    HTTPHelper httpHelper = new HTTPHelper(activity);

    public M1ServicesService (){
        super("M1ServicesService");
    }
    @Override
    protected void onHandleIntent(Intent intent){

        String url = "";
        if(udp.setDeviceStatus(ip, serviceId, action)){
            int counter = 2;
            while (!M1.deviceStatusChangedFlag && counter > 0) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
                counter--;
                //waiting time
            }
        }

        if(!M1.deviceStatusChangedFlag){
            url = "devctrl?token=" + token + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + mac + "&send=0&ignoretoken="+ RegistrationIntentService.regToken;
            try {
                if (httpHelper.setDeviceStatus(url, (byte) action, serviceId)) {
                    if (serviceId == gb.ALARM_RELAY_SERVICE) {
                        sql.updatePlugRelayService(action, mac);
                    }

                    if (serviceId == gb.ALARM_NIGHLED_SERVICE) {
                        sql.updatePlugNightlightService(action, mac);
                    }
                    Intent i = new Intent("status_changed_update_ui");
                    sendBroadcast(i);
                } else {
                    Intent i = new Intent("http_device_status");
                    i.putExtra("error", "yes");
                    sendBroadcast(i);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            if (serviceId == gb.ALARM_RELAY_SERVICE) {
                sql.updatePlugRelayService(action, mac);
            }

            if (serviceId == gb.ALARM_NIGHLED_SERVICE) {
                sql.updatePlugNightlightService(action, mac);
            }
            Intent i = new Intent("status_changed_update_ui");
            sendBroadcast(i);

            url = "devctrl?token=" + token + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + mac + "&send=1&ignoretoken="+ RegistrationIntentService.regToken;

            try {
                httpHelper.setDeviceStatus(url, (byte) action, serviceId);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }
}
