package com.jiee.smartplug.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;

import com.jiee.smartplug.ListDevices;
import com.jiee.smartplug.M1;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

import java.util.Locale;

/**
 * Created by ronaldgarcia on 22/4/16.
 */
public class ListDevicesServicesService extends IntentService {

    public static String ip = "";
    public static int serviceId = 0;
    public static byte action = 0;
    public static String mac = "";

    UDPCommunication udp = new UDPCommunication();
    MySQLHelper sql;
    HTTPHelper httpHelper;

    public ListDevicesServicesService (){
        super("ListDevicesServicesService");
    }
    @Override
    protected void onHandleIntent(Intent intent){

        if( sql==null )
            sql = HTTPHelper.getDB(this);

        if( httpHelper==null )
            httpHelper = new HTTPHelper(this);

        String url = "";

        if(udp.setDeviceStatus(ip, serviceId, action)){
            System.out.println("ACTION: "+action);
            int counter = 2;
            while (!ListDevices.deviceStatusChangedFlag && counter > 0) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e){
                    e.printStackTrace();
                }
                counter--;
                //waiting time
            }
        } else {
            System.out.println("Action not carried out !!!!!!!");
        }

        if(!ListDevices.deviceStatusChangedFlag){
            url = "devctrl?token=" + Miscellaneous.getToken(this) + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + mac + "&send=0&ignoretoken="+ RegistrationIntentService.regToken;
            try {
                if (httpHelper.setDeviceStatus(url, (byte) action, serviceId)) {
                    if (serviceId == GlobalVariables.ALARM_RELAY_SERVICE) {
                        sql.updatePlugRelayService(action, mac);
                    }

                    if (serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE) {
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

            if (serviceId == GlobalVariables.ALARM_RELAY_SERVICE) {
                sql.updatePlugRelayService(action, mac);
            }

            if (serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE) {
                sql.updatePlugNightlightService(action, mac);
            }
            Intent i = new Intent("status_changed_update_ui");
            sendBroadcast(i);

            url = "devctrl?token=" + Miscellaneous.getToken(this) + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + mac + "&send=1&ignoretoken="+ RegistrationIntentService.regToken;

            try {
                httpHelper.setDeviceStatus(url, (byte) action, serviceId);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
