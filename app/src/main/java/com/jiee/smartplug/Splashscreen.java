package com.jiee.smartplug;

/** TO DO:
 *
 /*
 Splashscreen.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 The splash screen will show at the opening of the app, during a short period of time the app would also try to connet to the server.
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;

import java.util.Locale;

public class Splashscreen extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 1000;
    MySQLHelper mySQLHelper;
    String token;
    HTTPHelper httpHelper;
    Miscellaneous misc;
    BroadcastReceiver done;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        mySQLHelper = HTTPHelper.getDB(this);
        httpHelper = new HTTPHelper(this);
        misc = new Miscellaneous();
        done = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int ok = intent.getIntExtra("ok", 0);
                if(ok > 0){

                }
            }
        };

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {

                token = misc.getToken(Splashscreen.this);

                if (token != null && !token.isEmpty()) {
                    Intent k = new Intent(Splashscreen.this, UDPListenerService.class);
                    startService(k);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String tosend = "devlist?token="+token+"&hl="+ Locale.getDefault().getLanguage()+"&res=1";
                                System.out.println(tosend);
                                httpHelper.getDeviceList(tosend);
                                Cursor c = mySQLHelper.getPlugData();
                                if (c.getCount() > 0) {
                                    Intent i = new Intent(Splashscreen.this, ListDevices.class);
                                    startActivity(i);
                                    finish();
                                } else {
                                    Intent add = new Intent(Splashscreen.this, NewDeviceList.class);
                                    startActivity(add);
                                    finish();
                                }
                                c.close();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            getGallery();
                        }
                    }).start();

                } else {
                    Intent i = new Intent(Splashscreen.this, MainActivity.class);
                    startActivity(i);
                    finish();
                }
            }
        }, SPLASH_TIME_OUT);

    }

    public void getGallery(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String param = "gallery?token="+token+"&hl="+ Locale.getDefault().getLanguage()+"&res=3";
                try {
                    httpHelper.saveGallery(param);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
