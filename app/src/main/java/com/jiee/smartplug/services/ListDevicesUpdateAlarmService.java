package com.jiee.smartplug.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.test.ActivityTestCase;

import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;

import java.util.Locale;

/**
 * Created by ronaldgarcia on 25/4/16.
 */
public class ListDevicesUpdateAlarmService extends IntentService {

    HTTPHelper httpHelper;

    public ListDevicesUpdateAlarmService(){
        super("ListDevicesUpdateAlarmService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if( httpHelper==null )
            httpHelper = new HTTPHelper( this );

        String macParam = intent.getStringExtra("macParam");

        try {
            if (macParam != null && !macParam.isEmpty()) {
                httpHelper.updateAlarms(macParam);
            } else {
                System.out.println("Mac id is empty");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String param = "devget?token=" + Miscellaneous.getToken( getApplicationContext() ) + "&hl=" + Locale.getDefault().getLanguage() + "&res=0&devid=" + macParam;
            httpHelper.getDeviceStatus(param, macParam, getApplicationContext());
        } catch (Exception e){
            e.printStackTrace();
        }

        Intent b = new Intent("UpdateAlarmServiceDone");
        sendBroadcast(b);
    }
}
