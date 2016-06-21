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

/** TO DO:
 *
 /*
 ListDevicesServicesService.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 Queue all the click events on the plug icon in the ListDevices activity

 */

public class ListDevicesServicesService extends IntentService {

    public static String ip = "";
    public static int serviceId = 0;
    public static byte action = 0;
    public static String mac = "";

    UDPCommunication udp;
    MySQLHelper sql;
    HTTPHelper httpHelper;

    public ListDevicesServicesService (){
        super("ListDevicesServicesService");
    }
    @Override
    protected void onHandleIntent(Intent intent){

        if( udp==null )
            udp = new UDPCommunication(this);

        if( sql==null )
            sql = HTTPHelper.getDB(this);

        if( httpHelper==null )
            httpHelper = new HTTPHelper(this);

        String url = "";

        if(udp.setDeviceStatus(mac, serviceId, action, true)){
            System.out.println("ACTION: "+action);
         //   int counter = 20000;
         //   while (!ListDevices.deviceStatusChangedFlag && counter > 0) {
         //       counter--;
                //waiting time
         //   }
        } else {
            System.out.println("Action not carried out !!!!!!!");
        }

    }

}
