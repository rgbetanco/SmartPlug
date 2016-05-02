package com.jiee.smartplug.utils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.jiee.smartplug.objects.Alarm;
import com.jiee.smartplug.objects.AlarmList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.jiee.smartplug.R;

/**
 * Created by ronaldgarcia on 29/12/15.
 */
public class Miscellaneous {

    MySQLHelper sql;
    Cursor c;
    String service = "";
    String dow = "";
    int background = 0;
    GlobalVariables gb = new GlobalVariables();

    public Miscellaneous(){}

    public List<Alarm> populateAlarm(Activity activity, String device_id, int service_id){
        List<Alarm> alarms = new ArrayList<Alarm>();
        int i = 0;
        sql = new MySQLHelper(activity);
        c = sql.getAlarmData(device_id, service_id);
        if(c.getCount()>0){
            c.moveToFirst();
            while (i < c.getCount()){
                i++;
                Alarm alarm = new Alarm();
                alarm.setAlarm_id(c.getInt(0));
                alarm.setInit_hour(c.getInt(4));
                alarm.setInit_minute(c.getInt(5));
                alarm.setEnd_hour(c.getInt(6));
                alarm.setEnd_minute(c.getInt(7));
                alarm.setDow(c.getInt(3));
                alarms.add(alarm);
                c.moveToNext();
            }
        }
        c.close();
        return alarms;
    }

    public static String getDOWList(int dowFlags, String[] DOWs) {
        String dow = "";

        for( int dowCurr=0; dowCurr<7; dowCurr++ ) {
            if( (dowFlags & (1<<dowCurr)) != 0 ) {
                dow += DOWs[dowCurr];
            }
        }

        return dow;
    }

    public static String getTime( int h, int m) {
        return String.format("%02d:%02d", h, m);
    }

    public List<AlarmList> populateAlarmList(Activity activity, String device_id, int service_id){
        List<AlarmList> alarmList = new ArrayList<AlarmList>();
        int i = 0;
        sql = new MySQLHelper(activity);
        c = sql.getAlarmData(device_id, service_id);

        final String[] DOWs = activity.getResources().getStringArray(R.array.dow);

        if(c.getCount()> 0) {
            c.moveToFirst();
            while(i < c.getCount() ) {
                i++;
                AlarmList a = new AlarmList();
                if (c.getInt(2) == gb.ALARM_RELAY_SERVICE) {
                    service = "Plug";
                } else if(c.getInt(2) == gb.ALARM_NIGHLED_SERVICE) {
                    service = "Nightlight";
                }
                /* c.getInt(3) returns 1-7. Sun-1, Mon-2 ... Sat-7 */
                dow = getDOWList(c.getInt(3), DOWs);

                String iHM = getTime( c.getInt(4), c.getInt(5));
                String eHM = getTime( c.getInt(6), c.getInt(7));

                a.setName( iHM + "-" + eHM /* +"   " + service*/ + "   " + dow);
                a.setAlarm_id(c.getInt(0));
                if(background > 3){
                    background = 0;
                }
                a.setBackground(background);
                background++;
                alarmList.add(a);
                c.moveToNext();
            }

            sql.close();
        }

        return alarmList;
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public boolean checkPlayServices(Activity a) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(a);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(a, resultCode, gb.PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i("CHECKPLAYSERVICE", "This device is not supported.");
                a.finish();
            }
            return false;
        }
        return true;
    }

    public int process_long(byte a, byte b, byte c, byte d){

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(d);
        buffer.put(c);
        buffer.put(b);
        buffer.put(a);

        return buffer.getInt(0);
    }

    public short process_short(byte a, byte b){
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(b);
        buffer.put(a);
        return buffer.getShort(0);
    }

    public int getResolution(Activity a){
        DisplayMetrics metrics = a.getApplicationContext().getResources().getDisplayMetrics();

        int dpi = metrics.densityDpi;
        int res = 0;
        if (dpi <= DisplayMetrics.DENSITY_MEDIUM) {
            res = 0;
        } else if (dpi <= DisplayMetrics.DENSITY_HIGH) {
            res = 1;
        } else if (dpi <= 320 /* DisplayMetrics.DENSITY_XHIGH */) {
            res = 2;
        } else if (dpi <= 480 /* DisplayMetrics.DENSITY_XXHIGH */) {
            res = 3;
        } else {   // 640, DisplayMetrics.DENSITY_XXXHIGH
            res = 4;
        }
        return res;
    }

    public String getToken(Context context){
        sql = new MySQLHelper(context);
        String token = "";
        Cursor d = sql.getToken();
        if(d.getCount() > 0){
            d.moveToFirst();
            token = d.getString(1);
        }
        d.close();

        return token;
    }

}
