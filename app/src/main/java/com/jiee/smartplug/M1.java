package com.jiee.smartplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.services.CrashCountDown;
import com.jiee.smartplug.services.M1ServicesService;
import com.jiee.smartplug.services.RegistrationIntentService;
import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.services.gcmNotificationService;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;
import com.jiee.smartplug.utils.UDPCommunication;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class M1 extends AppCompatActivity {

    public static String id;
    public static String ip;
    public static String name;
    public static String givenName;
    public static String mac;
    MySQLHelper sql;
    BroadcastReceiver udp_update_ui;
    BroadcastReceiver device_status_changed;
    BroadcastReceiver gcm_notification;
    BroadcastReceiver gcm_notification_done;
    BroadcastReceiver timer_crash_reached;
    BroadcastReceiver http_device_status;
    BroadcastReceiver device_not_reached;
    BroadcastReceiver timers_sent_successfully;
    BroadcastReceiver device_status_set;
    BroadcastReceiver mDNS_Device_Removed;
    BroadcastReceiver m1updateui;
    ProgressBar progressBar;

    ImageButton plug_icon;
    RelativeLayout btn_outlet;
    RelativeLayout btn_nightlight;
    ImageButton btn_settings;
    ImageButton btn_plug_alarm;
    ImageButton btn_nightled_alarm;
    ImageView img_warn2;
    ImageButton btn_ir;
    ImageButton btn_ir_alarm;
    ImageButton btn_co;
    ImageView warning_icon;
    ImageView warning_icon_co;
    ImageView nightled_icon;
    ImageView jsplug_icon;
    TextView plug_name;
    Animation animation;
    NetworkUtil networkUtil;
    RelativeLayout warning_layout;
    TextView warning_text;
    ImageButton btn_warning;
    Http http;
    HTTPHelper httpHelper;
    int relay = 0;
    int nightlight = 0;
    JSmartPlug js;
    Intent i;
    UDPListenerService mBoundService;
    boolean mServiceBound = false;
    byte action;
    int serviceId;
    RelativeLayout btn_layout_outlet;
    RelativeLayout btn_layout_nightled;
    RelativeLayout btn_layout_ir;
    RelativeLayout btn_layout_co;
    RelativeLayout overlay;
    CrashCountDown crashTimer;
    UDPCommunication udp;
    boolean udpconnection = false;
    String url = "";
    Handler mHandler;
    int relaySnooze = 0;
    int ledSnooze = 0;
    int irSnooze = 0;
    public static boolean deviceStatusChangedFlag = false;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_m1);

        token = Miscellaneous.getToken(this);

        udp = new UDPCommunication(this);
        http = new Http();
        httpHelper = new HTTPHelper(this);
        crashTimer = new CrashCountDown(this);

        progressBar = (ProgressBar) findViewById(R.id.M1ProgressBar);
        progressBar.setVisibility(View.GONE);

        jsplug_icon = (ImageView) findViewById(R.id.imageView);

        btn_layout_outlet = (RelativeLayout) findViewById(R.id.btn_outlet);
        btn_layout_nightled = (RelativeLayout) findViewById(R.id.btn_nightled);
        btn_layout_ir = (RelativeLayout) findViewById(R.id.btn_ir);
        btn_layout_co = (RelativeLayout) findViewById(R.id.btn_co);

        btn_co = (ImageButton) findViewById(R.id.co_icon);

        img_warn2 = (ImageView) findViewById(R.id.img_warn2);
        img_warn2.setVisibility(View.GONE);

        warning_text = (TextView) findViewById(R.id.msg_warning);
        warning_layout = (RelativeLayout) findViewById(R.id.warning_layout);

        btn_warning = (ImageButton) findViewById(R.id.btn_warning);
        btn_warning.setVisibility(View.GONE);
        btn_warning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                warning_layout.setVisibility(View.GONE);
            }
        });

        networkUtil = new NetworkUtil();

        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        plug_name = (TextView) findViewById(R.id.plug_name);

        //Animation
        animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);

        plug_icon = (ImageButton) findViewById(R.id.plug_icon);
        btn_outlet = (RelativeLayout) findViewById(R.id.btn_outlet);
        btn_settings = (ImageButton) findViewById(R.id.btn_settings);
        btn_plug_alarm = (ImageButton) findViewById(R.id.plug_alarm_icon);
        btn_nightled_alarm = (ImageButton) findViewById(R.id.nightled_alarm_icon);
        warning_icon = (ImageView) findViewById(R.id.warning_icon);
        nightled_icon = (ImageView) findViewById(R.id.nightled_icon);
        warning_icon_co = (ImageView) findViewById(R.id.warning_icon_co);

        warning_icon.setVisibility(View.GONE);
        warning_icon_co.setVisibility(View.GONE);

        btn_ir_alarm = (ImageButton) findViewById(R.id.btn_ir_alarm);

        Intent i = getIntent();
        mac = i.getStringExtra("mac");
        ip = i.getStringExtra("ip");
        System.out.println("IP: " + ip);
        name = i.getStringExtra("name");
        givenName = i.getStringExtra("givenName");

        if (givenName != null && !givenName.isEmpty()) {
            plug_name.setText(givenName);
        } else {
            plug_name.setText(name);
        }

        btn_ir = (ImageButton) findViewById(R.id.ir_icon);
        btn_ir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(M1.this, IREditMode.class);
                if (mac != null && !mac.isEmpty()) {
                    i.putExtra("ip", ip);
                    i.putExtra("devid", mac);
                    startActivity(i);
                } else {
                    System.out.println("MAC IS EMPTY HERE");
                }
            }
        });

        btn_layout_ir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(M1.this, IREditMode.class);
                //       if(ip != null && !ip.isEmpty()){
                i.putExtra("ip", ip);
                startActivity(i);
                //        }
            }
        });

        btn_ir.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_layout_ir.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_ir_pressed));
                } else {
                    btn_layout_ir.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_ir));
                }
                return false;
            }
        });

        btn_layout_ir.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_layout_ir.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_ir_pressed));
                } else {
                    btn_layout_ir.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_ir));
                }
                return false;
            }
        });

        sql = HTTPHelper.getDB(this);

        overlay = (RelativeLayout) findViewById(R.id.overlay);
        int opacity = 200; // from 0 to 255
        overlay.setBackgroundColor(opacity * 0x1000000); // black with a variable alpha
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        overlay.setLayoutParams(params);
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.GONE);

        timers_sent_successfully = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("TIMERS SENT SUCCESSFULLY BROADCAST");
                deviceStatusChangedFlag = true;
            }
        };

        timer_crash_reached = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!M1.deviceStatusChangedFlag){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String url = "devctrl?token=" + Miscellaneous.getToken(getApplicationContext()) + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + mac + "&send=0&ignoretoken="+ RegistrationIntentService.regToken;
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
                        }
                    }).start();

                } else {

                    if (serviceId == GlobalVariables.ALARM_RELAY_SERVICE) {
                        sql.updatePlugRelayService(action, mac);
                    }

                    if (serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE) {
                        sql.updatePlugNightlightService(action, mac);
                    }
                    Intent i = new Intent("status_changed_update_ui");
                    sendBroadcast(i);

                    /*
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String url = "devctrl?token=" + Miscellaneous.getToken(getApplicationContext()) + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + mac + "&send=1&ignoretoken="+ RegistrationIntentService.regToken;

                            try {
                                httpHelper.setDeviceStatus(url, (byte) action, serviceId);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    */
                }
                M1.deviceStatusChangedFlag = false;

                /*
                img_warn2.setVisibility(View.VISIBLE);
                warning_text.setText(getApplicationContext().getString(R.string.no_udp_Connection));
                warning_text.setVisibility(View.VISIBLE);
                btn_warning.setVisibility(View.VISIBLE);
                udpconnection = false;
                */
            }
        };

        http_device_status = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String error = "";
                error = intent.getStringExtra("error");
                if (error != null && !error.isEmpty()) {
                    removeGrayOutView();
                    Toast.makeText(M1.this, getApplicationContext().getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                }
                updateUI();
            }
        };

        device_status_set = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //        updateUI();
                startRepeatingTask();
            }
        };

        gcm_notification = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI();
                /*
                String getDataFlag = intent.getStringExtra("getDataFlag");
                String getAlarmFlag = intent.getStringExtra("getAlarmFlag");
                gcmNotificationService.activity = M1.this;
                gcmNotificationService.mac = M1.mac;
                Intent intent1 = new Intent(M1.this, gcmNotificationService.class);
                intent1.putExtra("getDataFlag",getDataFlag);
                intent1.putExtra("getAlarmFlag",getAlarmFlag);
                startService(intent1);
                */
                //   Toast.makeText(M1.this, "I GOT THE PUSH NOTIFICATION", Toast.LENGTH_SHORT).show();
                /*
                String getDataFlag = intent.getStringExtra("getDataFlag");
                System.out.println(getDataFlag);
                if(getDataFlag.equals("true")) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                httpHelper.updateAlarms();
                            } catch (Exception e){
                                e.printStackTrace();
                            }

                            try {
                                httpHelper.getDeviceStatus("devget?token=" + token + "&hl=" + Locale.getDefault().getLanguage() + "&res=0&devid=" + mac, mac);
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();

                }
                startRepeatingTask();
        */
            }
        };

        gcm_notification_done = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI();
            }
        };

        udp_update_ui = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                removeGrayOutView();
                //   startRepeatingTask();
                updateUI();
            }
        };

        device_status_changed = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                deviceStatusChangedFlag = true;
                startRepeatingTask();
                removeGrayOutView();
            }
        };

        mDNS_Device_Removed = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String serviceName = intent.getStringExtra("name");
                sql.updatePlugIP(serviceName, "");
                System.out.println(serviceName);
            }
        };

        device_not_reached = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("BROADCAST DEVICE NOT REACHED");
                String error = "";
                error = intent.getStringExtra("error");
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(M1.this, getApplicationContext().getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(M1.this, getApplicationContext().getString(R.string.please_wait), Toast.LENGTH_LONG).show();
                }
                removeGrayOutView();

            }
        };

        m1updateui = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String devid = intent.getStringExtra("id");
                Log.i("BROADCAST", "BROADCAST RECEIVED FROM DEVICE + " + devid );

                //startRepeatingTask();
                updateUI();
            }
        };

        btn_plug_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //I need to check on the database if there are alarms already set to this device
                Cursor c = sql.getAlarmData(mac, GlobalVariables.ALARM_RELAY_SERVICE);
                int alarmCount = c.getCount();
                final M1SnoozeDialog m1d = new M1SnoozeDialog(M1.this, mac, relaySnooze, GlobalVariables.ALARM_RELAY_SERVICE, alarmCount);
                m1d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                m1d.show();
                c.close();
            }
        });

        btn_nightled_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor c = sql.getAlarmData(mac, GlobalVariables.ALARM_NIGHLED_SERVICE);
                int alarmCount = c.getCount();
                final M1SnoozeDialog m1d = new M1SnoozeDialog(M1.this, mac, ledSnooze, GlobalVariables.ALARM_NIGHLED_SERVICE, alarmCount);
                m1d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                m1d.show();
                c.close();
            }
        });

        btn_ir_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor c = sql.getAlarmData(mac, GlobalVariables.ALARM_IR_SERVICE);
                int alarmCount = c.getCount();
                final M1SnoozeDialog m1d = new M1SnoozeDialog(M1.this, mac, irSnooze, GlobalVariables.ALARM_IR_SERVICE, alarmCount);
                m1d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                m1d.show();
                c.close();
            }
        });

        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(M1.this, M2A_Item_Settings.class);
                startActivity(i);
            }
        });

        nightled_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendService(GlobalVariables.ALARM_NIGHLED_SERVICE);
            }
        });

        btn_layout_nightled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendService(GlobalVariables.ALARM_NIGHLED_SERVICE);
            }
        });

        nightled_icon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_layout_nightled.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_nightlight_pressed));
                } else {
                    btn_layout_nightled.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_nightlight));
                }
                return false;
            }
        });

        btn_layout_nightled.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_layout_nightled.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_nightlight_pressed));
                } else {
                    btn_layout_nightled.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_nightlight));
                }
                return false;
            }
        });

        plug_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendService(GlobalVariables.ALARM_RELAY_SERVICE);
            }
        });

        btn_outlet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendService(GlobalVariables.ALARM_RELAY_SERVICE);
            }
        });

        plug_icon.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_layout_outlet.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_outlet_pressed));
                } else {
                    btn_layout_outlet.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_outlet));
                }
                return false;
            }
        });

        btn_outlet.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_layout_outlet.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_outlet_pressed));
                } else {
                    btn_layout_outlet.setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_outlet));
                }
                return false;
            }
        });

        mHandler = new Handler();

        getDataFromServer();

        startRepeatingTask();

        removeGrayOutView();

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                if (ip != null) {
                    short command = 0x0007;
                    if (udp.queryDevices(mac, command)) {
                        removeGrayOutView();
                        //    crashTimer.setTimer(2);
                        //    crashTimer.startTimer();
                    } else {
                        System.out.println("IP IS NULL");
                    }
                    udpconnection = false;
                }
                removeGrayOutView();
            } finally {
                updateUI();
                stopRepeatingTask();
                //    mHandler.postDelayed(mStatusChecker, 7000);
            }
        }
    };

    public void getDataFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HTTPHelper http = new HTTPHelper(M1.this);
                http.getServerIR(mac, GlobalVariables.IR_SERVICE, Miscellaneous.getResolution(M1.this));
                Intent i = new Intent("serverReplied");
                sendBroadcast(i);
            }
        }).start();
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    public void updateUI() {
        removeGrayOutView();
        Cursor u = sql.getPlugDataByID(mac);
        if (u.getCount() > 0) {
            u.moveToFirst();
            if (u.getString(21) != null && !u.getString(21).isEmpty()) {
                plug_name.setText(u.getString(21));
            } else {
                plug_name.setText(u.getString(1));
            }
            switch (u.getInt(12)) {                                                                  //RELAY
                case 0:
                    relay = 0;
                    plug_icon.setImageResource(R.drawable.svc_0_big_off);
                    break;
                case 1:
                    relay = 1;
                    plug_icon.setImageResource(R.drawable.svc_0_big);
                    break;
            }
            switch (u.getInt(13)) {                                                                  //HALL EFFECT SENSOR
                case 0:
                    warning_icon.setVisibility(View.GONE);
                    //warning_icon.setImageResource(R.drawable.marker_warn);
                    //warning_icon.clearAnimation();
                    warning_text.setText("");
                    img_warn2.setVisibility(View.GONE);
                    btn_warning.setVisibility(View.GONE);
                    break;
                case 1:
                    warning_icon.setVisibility(View.VISIBLE);
                    warning_text.setVisibility(View.VISIBLE);
                    //warning_icon.setImageResource(R.drawable.marker_warn2);
                    //warning_icon.startAnimation(animation);
                    ((AnimationDrawable) warning_icon.getDrawable()).start();
                    warning_layout.setVisibility(View.VISIBLE);
                    img_warn2.setVisibility(View.VISIBLE);
                    btn_warning.setVisibility(View.VISIBLE);
                    warning_text.setText(R.string.msg_ha_warning);
                    break;
            }
            switch (u.getInt(14)) {                                                                  //CO SENSOR
                case 0:
                    warning_icon_co.setVisibility(View.GONE);
                    //warning_icon_co.setImageResource(R.drawable.marker_warn);
                    //warning_icon_co.clearAnimation();
                    btn_co.setImageResource(R.drawable.svc_3_big);
                    break;
                case 1:
                    warning_layout.setVisibility(View.VISIBLE);
                    btn_co.setImageResource(R.drawable.svc_3_big);
                    warning_icon_co.setVisibility(View.VISIBLE);
                    ((AnimationDrawable) warning_icon_co.getDrawable()).start();

                    img_warn2.setVisibility(View.VISIBLE);
                    warning_text.setText(getApplicationContext().getString(R.string.msg_co_warning));
                    warning_text.setVisibility(View.VISIBLE);
                    btn_warning.setVisibility(View.VISIBLE);

                    //warning_icon_co.setImageResource(R.drawable.marker_warn2);
                    //warning_icon_co.startAnimation(animation);
                    img_warn2.setVisibility(View.VISIBLE);
                    btn_warning.setVisibility(View.VISIBLE);
                    warning_text.setText(R.string.msg_co_warning);
                    break;
                case 3:
                    warning_layout.setVisibility(View.VISIBLE);
                    btn_co.setImageResource(R.drawable.svc_3_big_off);
                    img_warn2.setVisibility(View.VISIBLE);
                    btn_warning.setVisibility(View.VISIBLE);
                    warning_text.setText(R.string.USB_not_plugged_in);
                    break;
            }
            switch (u.getInt(15)) {                                                                  //NIGHT LIGHT
                case 0:
                    nightlight = 0;
                    nightled_icon.setImageResource(R.drawable.svc_1_big_off);
                    break;
                case 1:
                    nightlight = 1;
                    nightled_icon.setImageResource(R.drawable.svc_1_big);
                    break;
            }

            relaySnooze = u.getInt(22);
            if (relaySnooze == 0) {
                Cursor c = sql.getAlarmData(mac, GlobalVariables.ALARM_RELAY_SERVICE);
                if (c.getCount() > 0) {
                    btn_plug_alarm.setImageResource(R.drawable.btn_timer_on);
                } else {
                    btn_plug_alarm.setImageResource(R.drawable.btn_timer_off);
                }
                c.close();
            } else {
                btn_plug_alarm.setImageResource(R.drawable.btn_timer_delay);
            }

            ledSnooze = u.getInt(23);
            if (ledSnooze == 0) {
                Cursor e = sql.getAlarmData(mac, GlobalVariables.ALARM_NIGHLED_SERVICE);
                if (e.getCount() > 0) {
                    btn_nightled_alarm.setImageResource(R.drawable.btn_timer_on);
                } else {
                    btn_nightled_alarm.setImageResource(R.drawable.btn_timer_off);
                }
                e.close();
            } else {
                btn_nightled_alarm.setImageResource(R.drawable.btn_timer_delay);
            }

            irSnooze = u.getInt(24);
            if (irSnooze == 0) {
                Cursor f = sql.getAlarmData(mac, GlobalVariables.ALARM_IR_SERVICE);
                if (f.getCount() > 0) {
                    btn_ir_alarm.setImageResource(R.drawable.btn_timer_on);
                } else {
                    btn_ir_alarm.setImageResource(R.drawable.btn_timer_off);
                }
                f.close();
            } else {
                btn_ir_alarm.setImageResource(R.drawable.btn_timer_delay);
            }

            if (u.getString(17) != null && !u.getString(17).isEmpty()) {
                Picasso.with(this).load(u.getString(17).toString()).into(jsplug_icon);
            }

            nightled_icon.setEnabled(true);
            plug_icon.setEnabled(true);

        }
        if (!u.isClosed()) {
            u.close();
        }
/*
        relaySnooze = sql.getRelaySnooze();
        if(relaySnooze > 0){
            btn_plug_alarm.setImageResource(R.drawable.btn_timer_delay);
        } else {
            Cursor c = sql.getAlarmData(mac, gb.ALARM_RELAY_SERVICE);
            if(c.getCount()>0){
                btn_plug_alarm.setImageResource(R.drawable.btn_timer_on);
            } else {
                btn_plug_alarm.setImageResource(R.drawable.btn_timer_off);
            }
            c.close();
        }

        ledSnooze = sql.getLedSnooze();
        if(ledSnooze > 0){
            btn_nightled_alarm.setImageResource(R.drawable.btn_timer_delay);
        } else {
            Cursor e = sql.getAlarmData(mac, gb.ALARM_NIGHLED_SERVICE);
            if(e.getCount()>0){
                btn_nightled_alarm.setImageResource(R.drawable.btn_timer_on);
            } else {
                btn_nightled_alarm.setImageResource(R.drawable.btn_timer_off);
            }
            e.close();
        }
*/
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UDPListenerService.MyBinder myBinder = (UDPListenerService.MyBinder) service;
            mBoundService = myBinder.getService();
            mServiceBound = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (ip != null) {
                        short command = 0x0007;
                        if (udp.queryDevices(mac, command)) {
                         //   crashTimer.setTimer(0);
                         //   crashTimer.startTimer();
                        } else {
                            System.out.println("IP IS NULL");
                        }
                        udpconnection = false;
                    }
                }
            }).start();
        }
    };


    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        startRepeatingTask();
        registerReceiver(udp_update_ui, new IntentFilter("status_changed_update_ui"));
        registerReceiver(device_status_changed, new IntentFilter("device_status_changed"));
        registerReceiver(gcm_notification, new IntentFilter("gcm_notification"));
        registerReceiver(gcm_notification_done, new IntentFilter("gcmNotificationDone"));
        registerReceiver(timer_crash_reached, new IntentFilter("timer_crash_reached"));
        registerReceiver(http_device_status, new IntentFilter("http_device_status"));
        registerReceiver(device_not_reached, new IntentFilter("device_not_reached"));
        registerReceiver(timers_sent_successfully, new IntentFilter("timers_sent_successfully"));
        registerReceiver(device_status_set, new IntentFilter("device_status_set"));
        registerReceiver(mDNS_Device_Removed, new IntentFilter("mDNS_Device_Removed"));
        registerReceiver(m1updateui, new IntentFilter("m1updateui"));
        try {
            Intent intent = new Intent(this, UDPListenerService.class);
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(udp_update_ui);
        unregisterReceiver(device_status_changed);
        unregisterReceiver(gcm_notification);
        unregisterReceiver(gcm_notification_done);
        unregisterReceiver(timer_crash_reached);
        unregisterReceiver(http_device_status);
        unregisterReceiver(device_not_reached);
        unregisterReceiver(timers_sent_successfully);
        unregisterReceiver(device_status_set);
        unregisterReceiver(mDNS_Device_Removed);
        unregisterReceiver(m1updateui);
        udpconnection = true;
        try {
            if (mServiceBound) {
                unbindService(mServiceConnection);
                mServiceBound = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        stopRepeatingTask();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void sendService(int sId) {
        grayOutView();
        plug_icon.setEnabled(false);
        nightled_icon.setEnabled(false);
        Toast.makeText(this, getApplicationContext().getString(R.string.processing_command), Toast.LENGTH_SHORT).show();
        serviceId = sId;
        progressBar.setVisibility(View.VISIBLE);
        if (serviceId == GlobalVariables.ALARM_RELAY_SERVICE) {
            if (relay == 0) {
                action = 0x01;
            } else {
                action = 0x00;
            }
        }

        if (serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE) {
            if (nightlight == 0) {
                action = 0x01;
            } else {
                action = 0x00;
            }
        }

        img_warn2.setVisibility(View.VISIBLE);
        warning_text.setText(getApplicationContext().getString(R.string.please_wait_done));
        warning_text.setVisibility(View.VISIBLE);
        btn_warning.setVisibility(View.VISIBLE);

        Intent iService = new Intent(this, M1ServicesService.class);
        M1ServicesService.ip = ip;
        M1ServicesService.serviceId = serviceId;
        M1ServicesService.action = action;
        M1ServicesService.mac = mac;
        startService(iService);

        crashTimer.setMicroTimer();
        crashTimer.startTimer();

        SystemClock.sleep(300);

        plug_icon.setEnabled(true);
        nightled_icon.setEnabled(true);

/*
        udpconnection = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                deviceStatusChangedFlag = false;
                if(udp.setDeviceStatus(ip, serviceId, action)){
                    Intent deviceStatusSet = new Intent("device_status_set");
                    sendBroadcast(deviceStatusSet);
                    int counter = 2;
                    while (!deviceStatusChangedFlag && counter > 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                        counter--;
                        //waiting time
                    }
                }

            }
        }).start();
*/
        progressBar.setVisibility(View.GONE);

    }

    public void grayOutView() {
        //    overlay.invalidate(); // update the view
        //    overlay.setVisibility(View.VISIBLE);
    }

    public void removeGrayOutView() {
        //    overlay.invalidate(); // update the view
        //    overlay.setVisibility(View.INVISIBLE);
    }
}