package com.jiee.smartplug;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.http.HttpResponseCache;
import android.os.StrictMode;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.services.RegistrationIntentService;
import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.services.mDNSTesting;
import com.jiee.smartplug.services.mDNSservice;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    String token;
    Http http = new Http();
    HTTPHelper httpHelper;
    MySQLHelper mySQLHelper;
    NetworkUtil networkUtil;
    String response;
    Context context = this;
    View view;
    ProgressBar login_progress;
    Intent j;
    Intent k;
    Miscellaneous misc = new Miscellaneous();

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        httpHelper = new HTTPHelper(this);
        mySQLHelper = HTTPHelper.getDB(this);

        token = Miscellaneous.getToken(this);

        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        prefs = getPreferences(Context.MODE_PRIVATE);
        if(prefs.getBoolean("firstLaunch", true)){
            prefs.edit().putBoolean("firstLaunch", false).commit();
            Intent ix = new Intent(MainActivity.this, HelpView.class);
            startActivity(ix);
        }

        try {
            File httpCacheDir = new File(context.getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i("CACHE", "HTTP response cache installation failed:" + e);
        }

        networkUtil = new NetworkUtil();

        k = new Intent(this, UDPListenerService.class);
        startService(k);


        /*
        need to remove later when the server is working
        */

    //    token = "123123";
    //    mySQLHelper.insertToken(token);

        //UPDATE ICONS

        //http.updateIcons(this);

        login_progress = (ProgressBar) findViewById(R.id.login_progress);

        final Toolbar myToolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(myToolbar);

        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        ImageButton btn_settings = (ImageButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.GONE);

        final EditText txt_username = (EditText) findViewById(R.id.txt_username);
        txt_username.setTextColor(getResources().getColor(R.color.black));
        final EditText txt_password = (EditText) findViewById(R.id.txt_password);
        txt_password.setTextColor(getResources().getColor(R.color.black));

        txt_username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        txt_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }

        });

        TextView txt_reset_password = (TextView) findViewById(R.id.txt_forgot_password);
        txt_reset_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent reset_password = new Intent(v.getContext(), ResetPassword.class);
                startActivity(reset_password);
            }
        });

        TextView txt_create_account = (TextView) findViewById(R.id.txt_create_account);
        txt_create_account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent create_account = new Intent(v.getContext(), CreateAccount.class);
                startActivity(create_account);
            }
        });

        Button btn_ok = (Button) findViewById(R.id.btn_login);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view = v;
                if (token != null && !token.isEmpty()) {               //i got a token on my database

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            String tosend = "devlist?token="+ token+"&hl="+ Locale.getDefault().getLanguage()+"&res=1";
                            if(httpHelper.getDeviceList(tosend)) {
                                //   registerOnGCM();
                                Cursor c = mySQLHelper.getPlugData();
                                if (c.getCount() > 0) {
                                    Intent i = new Intent(view.getContext(), ListDevices.class);
                                    startActivity(i);
                                    finish();
                                } else {
                                    Intent add = new Intent(view.getContext(), NewDeviceList.class);
                                    startActivity(add);
                                    finish();
                                }

                                if (!c.isClosed()) {
                                    c.close();
                                }
                            }
                        }
                    }).start();

                } else {
                    login_progress.setVisibility(View.VISIBLE);         //I dont have any token on my database
                    HTTPHelper http = new HTTPHelper(MainActivity.this);
                    try {
                        boolean success = http.login("login?user=" + txt_username.getText().toString().trim() + "&pwd=" + txt_password.getText().toString().trim() + "&hl="+Locale.getDefault().getLanguage());
                        if(success){
                            Cursor d = mySQLHelper.getToken();
                            if(d.getCount() > 0){
                                d.moveToFirst();
                                token = d.getString(1);
                            }
                            d.close();
                            if(token != null && !token.isEmpty()){
                                getGallery();

                                String tosend = "devlist?token="+ token+"&hl="+ Locale.getDefault().getLanguage()+"&res=1";
                                if(httpHelper.getDeviceList(tosend)) {

                                    getGallery();
                                    Cursor c = mySQLHelper.getPlugData();
                                    if (c.getCount() > 0) {
                                        Intent i = new Intent(view.getContext(), ListDevices.class);
                                        startActivity(i);
                                        finish();
                                    } else {
                                        Intent add = new Intent(view.getContext(), NewDeviceList.class);
                                        startActivity(add);
                                        finish();
                                    }
                                    c.close();

                                }

                            } else {
                                txt_password.setText("");
                                login_progress.setVisibility(View.GONE);
                                Toast.makeText(MainActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            txt_password.setText("");
                            login_progress.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
        });
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

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
    }

    //hide the keyboard when the edit text field loose focus
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();            //FORCE THE CACHE DATA TO BE SAVED ON DISK
        }
//        stopService(j);
//        stopService(k);

    }


}
