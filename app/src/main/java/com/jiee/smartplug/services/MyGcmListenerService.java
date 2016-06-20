package com.jiee.smartplug.services;

/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.jiee.smartplug.R;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;

import org.apache.http.protocol.HTTP;

import java.util.HashSet;
import java.util.Locale;

public class MyGcmListenerService extends GcmListenerService {

//    MySQLHelper sql = new MySQLHelper(getApplicationContext());
    HTTPHelper http;

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {

        if( http==null )
            http = new HTTPHelper(this);

        System.out.println(data);
        MySQLHelper sql = HTTPHelper.getDB(this);
        String message = data.getString("message");
        String showFlag = data.getString("showFlag");
        String getDataFlag = data.getString("getDataFlag");
        String getDeviceFlag = data.getString("getDeviceFlag");
        String getAlarmFlag = data.getString("getAlarmFlag");
        String devId = data.getString("devid");
        Log.d(TAG, "From: " + from + " msg: " + message + " devid:" + devId + " (flag=" + showFlag + ", data=" + getDataFlag + ", dev=" + getDeviceFlag + ", alarm=" + getAlarmFlag + ")");

        if(getDataFlag.equals("true")){
            String param = "devget?token=" + Miscellaneous.getToken(getApplicationContext()) + "&hl=" + Locale.getDefault().getLanguage() + "&res=0&devid=" + devId;
            try {
                if(devId != null && !devId.isEmpty() && !devId.equals("null")) {
                    http.getDeviceStatus(param, devId, getApplicationContext(), false);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        if(getAlarmFlag.equals("true")){
            try {
                if(devId != null && !devId.isEmpty() && !devId.equals("null")) {
                    http.updateAlarms(devId);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        Intent intent = new Intent("gcm_notification");
        intent.putExtra("getDeviceFlag", getDeviceFlag);
        intent.putExtra("getDataFlag", getDataFlag);
        intent.putExtra("getAlarmFlag", getAlarmFlag);
        intent.putExtra("message", message);
        sendBroadcast(intent);


        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        if (showFlag!=null && showFlag.equals("true")) {
            sendNotification(message);
        }
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {

        System.out.println(message);

        NotificationManager notif=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify=new Notification(R.drawable.svc_0_small_off,"title",System.currentTimeMillis());
        PendingIntent pending= PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);

        notify.setLatestEventInfo(getApplicationContext(),"Subject",message,pending);
        notif.notify(0, notify);

//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                PendingIntent.FLAG_ONE_SHOT);
//
//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.ic_stat_ic_notification)
//                .setContentTitle("GCM Message")
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setSound(defaultSoundUri)
//                .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}