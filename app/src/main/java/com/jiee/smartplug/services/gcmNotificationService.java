package com.jiee.smartplug.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;

import com.jiee.smartplug.Http;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;

import java.util.Locale;

/**
 * Created by ronaldgarcia on 25/4/16.
 */
public class gcmNotificationService extends IntentService {

    public static String mac;

    Miscellaneous misc = new Miscellaneous();
    HTTPHelper httpHelper;

    public gcmNotificationService(){
        super("gcmNotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent){
        String getDataFlag = intent.getStringExtra("getDataFlag");
        String getAlarmFlag = intent.getStringExtra("getAlarmFlag");
        System.out.println(getDataFlag);
        if(getAlarmFlag.equals("true")) {
            if( httpHelper==null )
                httpHelper = new HTTPHelper(this);

            try {
                httpHelper.updateAlarms(mac);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Intent i = new Intent("gcmNotificationDone");
        sendBroadcast(i);
    }

}
