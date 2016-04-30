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


import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.jiee.smartplug.ListDevices;
import com.jiee.smartplug.MainActivity;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;

import java.io.IOException;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};
    public static String regToken = "";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken("725253730549",GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            // TODO: Implement this method to send any registration to your app's servers.
            sendRegistrationToServer(token);

            // Subscribe to topic channels
            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(GlobalVariables.SENT_TOKEN_TO_SERVER, true).apply();
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(GlobalVariables.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(GlobalVariables.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        MediaType TEXT = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        regToken = token;
        String param = "regpush?token="+ListDevices.token+"&hl="+ Locale.getDefault().getLanguage()+"&type=A&devtoken="+token;
        System.out.println(param);
        String url = GlobalVariables.DOMAIN+param;
        RequestBody body = RequestBody.create(TEXT, GlobalVariables.DOMAIN+param);
        Request request = new Request.Builder().url(url).post(body).build();
        String jsonData = "";
        try {
            Response response = client.newCall(request).execute();
            jsonData = response.body().string().toString();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(jsonData);
        try {
            /*
            JSONObject Jobject = new JSONObject(jsonData);
            String token = Jobject.getString("token");
            int r = Jobject.getInt("r");
            if(r == 0){
                sql.insertToken(token);
                toReturn = true;
            }
            */
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]
}
