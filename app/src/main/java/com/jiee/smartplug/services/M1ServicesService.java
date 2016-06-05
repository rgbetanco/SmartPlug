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
import com.jiee.smartplug.utils.Miscellaneous;
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
    public static String mac = "";

    UDPCommunication udp;
    MySQLHelper sql;
    HTTPHelper httpHelper;

    public M1ServicesService (){
        super("M1ServicesService");
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
        M1.deviceStatusChangedFlag = false;
        if(udp.setDeviceStatus( mac, serviceId, action, true)){
            //int counter = 40000;
            //while (!M1.deviceStatusChangedFlag && counter > 0) {
            //    counter--;
                //waiting time
            //}
        }
    }
}
