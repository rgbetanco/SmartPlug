package com.jiee.smartplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.google.android.gms.maps.CameraUpdate;
import com.jiee.smartplug.adapters.ListDevicesAdapter;
import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.services.CrashCountDown;
import com.jiee.smartplug.services.ListDevicesServicesService;
import com.jiee.smartplug.services.ListDevicesUpdateAlarmService;
import com.jiee.smartplug.services.MqttCallbackHandler;
import com.jiee.smartplug.services.RegistrationIntentService;
import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.services.gcmNotificationService;
import com.jiee.smartplug.services.mDNSTesting;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;
import com.jiee.smartplug.utils.UDPCommunication;
import android.content.SharedPreferences;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.TooManyListenersException;

public class ListDevices extends Activity {

    MySQLHelper mySQLHelper;
    ImageButton bt;
    Activity act = this;
    Intent i;
    List<JSmartPlug> jSmartPlugs;
    Http http;
    HTTPHelper httpHelper;
    UDPListenerService UDPBinding;
    boolean UDPBound = false;
    BroadcastReceiver new_device_receiver;
    BroadcastReceiver device_removed_receiver;
    BroadcastReceiver device_info;
    BroadcastReceiver gcm_notification;
    BroadcastReceiver gcm_notification_done;
    BroadcastReceiver device_status_changed;
    BroadcastReceiver status_changed_update_ui;
    BroadcastReceiver adapter_onClick;
    BroadcastReceiver m1updateui;
    BroadcastReceiver repeatingTaskDone;
    BroadcastReceiver UpdateAlarmServiceDone;
    BroadcastReceiver http_device_status;
    BroadcastReceiver delete_sent;
    ListDevicesAdapter l;
    ListView list;
    NetworkUtil networkUtil;
    JSmartPlug jsTemp;
    String ip;
    String name;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    UDPCommunication con;
    Intent mDNS;
    String ipParam, macParam;
    Handler mHandler;
    RelativeLayout overlay;
    public static String token;
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Miscellaneous.mParentActivity = new WeakReference<Activity>(this);

        setContentView(R.layout.activity_list_devices);

        sharedpreferences = getSharedPreferences("properties", Context.MODE_PRIVATE);

        token = Miscellaneous.getToken(this);

        registerOnGCM();

        http = new Http();
        httpHelper = new HTTPHelper(this);
        con = new UDPCommunication(this);

        networkUtil = new NetworkUtil();
        mySQLHelper = HTTPHelper.getDB(this);

        bt = (ImageButton)findViewById(R.id.btn_new_plugs);

        overlay = (RelativeLayout)findViewById(R.id.overlay);
        /*
        int opacity = 200; // from 0 to 255
        overlay.setBackgroundColor(opacity * 0x1000000); // black with a variable alpha
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        overlay.setLayoutParams(params);
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.GONE);
        */

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(v.getContext(), NewDeviceList.class);
                startActivity(i);                                               //start activity
                finish();
            }
        });

        http_device_status = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String error = "";
                error = intent.getStringExtra("error");
                if(error != null && !error.isEmpty()){
                    removeGrayOutView();
                    Toast.makeText(ListDevices.this, getApplicationContext().getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                }
                l.getData();
            }
        };

        adapter_onClick = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getIntExtra("start", 0) == 0) {
                    grayOutView();
                }

                String name = intent.getStringExtra("name");
                if(name != null && !name.isEmpty()){
                    String tmp_mac = "";
                    String tmp_ip = "";
                    String tmp_name = "";
                    String tmp_givenName = "";
                    Cursor c = mySQLHelper.getPlugDataByName(name);
                    if(c.getCount()>0){
                        c.moveToFirst();
                        tmp_name = c.getString(1);
                        tmp_mac = c.getString(2);
                        tmp_ip = c.getString(3);
                        tmp_givenName = c.getString(21);

                    }
                    c.close();
                    Intent i = new Intent(act, M1.class);
                    i.putExtra("mac", tmp_mac);
                    i.putExtra("ip", tmp_ip);
                    i.putExtra("name", tmp_name);
                    i.putExtra("givenName", tmp_givenName);
                    System.out.println("MAC: " + tmp_mac + ", IP: " + tmp_ip);
                    act.startActivity(i);
                } else {
                    Toast.makeText(ListDevices.this, R.string.general_error, Toast.LENGTH_SHORT).show();
                }

            }
        };

        repeatingTaskDone = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String error = "";
                System.out.println("Repeating TASK DONE");
                error = intent.getStringExtra("errorMessage");
                if(error != null && !error.isEmpty()){
                    Toast.makeText(ListDevices.this, R.string.connection_error, Toast.LENGTH_LONG).show();
                } else {
                    System.out.println("ERROR MESSAGE IS NULL");
                }
                if (l != null) {
                    l.getData();
                    removeGrayOutView();
                }
            }
        };


        list = (ListView)findViewById(R.id.listplugs);
        m1updateui = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String devid = intent.getStringExtra("id");

                Log.i("BROADCAST", "BROADCAST RECEIVED FROM DEVICE + " + devid );

                if(l!=null) {
                    l.getData();
                }

                //startRepeatingTask(devid);
            }
        };

        gcm_notification = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(l!=null) {
                    l.getData();
                }
                /*
                String getDataFlag = intent.getStringExtra("getDataFlag");
                gcmNotificationService.activity = ListDevices.this;

                String gcm_mac = "";

                Cursor c = mySQLHelper.getPlugData();
                if(c.getCount() > 0){
                    c.moveToFirst();
                    for(int i = 0; i < c.getCount(); i++) {
                        macParam = c.getString(2);

                        gcmNotificationService.mac = macParam;
                        Intent intent1 = new Intent(ListDevices.this, gcmNotificationService.class);
                        intent1.putExtra("getDataFlag",getDataFlag);
                        startService(intent1);

                        c.moveToNext();
                    }
                }
                c.close();
                */
            }

        };

        UpdateAlarmServiceDone = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                while(l==null) {}
                  l.getData();
            }
        };

        gcm_notification_done = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                l.getData();
            }
        };

        status_changed_update_ui = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("DEVICE STATUS CHANGED UI");

                if(l!=null) {
                    l.setDeviceStatusChangedFlag(true);
                    l.getData();
                }
            }
        };

        device_status_changed = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
       //         l.setDeviceStatusChangedFlag(true);
                Log.i("BROADCAST", "DeviceStatusChanged");

                final String devid = intent.getStringExtra("id");
                startRepeatingTask(devid);
       //         l.getData();
            }
        };

        device_info = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ip = intent.getStringExtra("ip");
                final String id = intent.getStringExtra("id");

                jsTemp = UDPListenerService.js;
                jsTemp.setIp(ip);
                if (jsTemp.getIp() != null && !jsTemp.getIp().isEmpty()) {
                    if(jsTemp.getId() != null && !jsTemp.getId().isEmpty()) {
                //        mySQLHelper.updatePlugID(name, jsTemp.getId());
                        mySQLHelper.updatePlugServicesByIP(jsTemp);
                    }
                }

                MySQLHelper sql = new MySQLHelper(ListDevices.this);

                Cursor c = sql.getPlugDataByID(id);
                if(c.getCount() > 0){
                    c.moveToFirst();
                    final String model = c.getString(5);
                    final int buildnumber = c.getInt(6);
                    final int protocol = c.getInt(7);
                    final String hardware = c.getString(8);
                    final String firmware = c.getString(9);
                    final int firmwaredate = c.getInt(10);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String param = "devset?token="+ Miscellaneous.getToken(ListDevices.this)+"&hl="
                                    + Locale.getDefault().getLanguage()+"&devid="
                                    + id +"&model="+model+ "&buildnumber="+buildnumber+"&protocol="+protocol
                                    + "&hardware="+hardware+"&firmware="+firmware
                                    + "&firmwaredate="+firmwaredate+"&send=1";
                            try {
                                httpHelper.setDeviceSettings(param);
                            } catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }

            l.getData();
            }

        };

        new_device_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("BROADCAST", "Device found1");
                startRepeatingTask();
            }
        };

        device_removed_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String serviceName = intent.getStringExtra("name");
                mySQLHelper.updatePlugIP(serviceName, "");
                Log.i("BROADCAST", "Device removed - " + serviceName);
            }
        };

        delete_sent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                l.deleteDevice();
            }
        };

        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.DeviceListProgress);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context act, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(act);
                boolean sentToken = sharedPreferences.getBoolean(GlobalVariables.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Toast.makeText(act, R.string.gcm_send_message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(act, R.string.token_error_message, Toast.LENGTH_SHORT).show();
                }
            }
        };

        ImageButton btn_settings = (ImageButton) findViewById(R.id.btn_settings);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(act, S0.class);
                startActivity(i);
            }
        });

        mDNS = new Intent(this, mDNSTesting.class);

     //   if(!sharedpreferences.getString("alarms", "outdated").equals("updated")) {
            Cursor c = mySQLHelper.getPlugData();
            if (c.getCount() > 0) {
                c.moveToFirst();
                for (int i = 0; i < c.getCount(); i++) {
                    this.macParam = c.getString(2);
                    this.ipParam = c.getString(3);
                    System.out.println("MAC: " + macParam);

                    Intent ix = new Intent(this, ListDevicesUpdateAlarmService.class);
                    ix.putExtra("macParam", macParam);
                    startService(ix);

                    c.moveToNext();
                }
            }
            c.close();

        //    SharedPreferences.Editor editor = sharedpreferences.edit();
        //    editor.putString("alarms", "updated");
        //    editor.commit();
     //   }

        mHandler = new Handler();
     //   startRepeatingTask();

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {

            Cursor c = mySQLHelper.getPlugData();
            if(c.getCount()>0){
                c.moveToFirst();
                for(int i = 0; i < c.getCount(); i++) {
                    con.queryDevices(c.getString(2), (short)0x0007);

                    c.moveToNext();
                }
            }
            c.close();


        //    //    new Thread(new Runnable() {
        //        @Override
        //        public void run() {
        //            Intent intent = new Intent("repeatingTaskDone");
        //            if (ipParam != null && !ipParam.isEmpty()) {
        //                con.queryDevices(macParam, (short)0x0007);
        //                con.queryDevices(ipParam, command, macParam);
        //                    int counter = 000;
        //                    while (!deviceStatusChangedFlag && counter > 0) {
        //                        counter--;
        //                        //waiting time
        //                    }

                        /*
                        else {
                            if(!deviceStatusChangedFlag) {
                                try {
                                    if(!httpHelper.getDeviceStatus("devget?token=" + token + "&hl=" + Locale.getDefault().getLanguage() + "&res=0&devid=" + macParam, macParam)){
                                        intent.putExtra("errorMessage", "yes");
                                    }
                                } catch (Exception e) {
                                    intent.putExtra("errorMessage", "yes");
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        System.out.println("IP IS NULL");
                        if(!deviceStatusChangedFlag) {
                            try {
                                if(!httpHelper.getDeviceStatus("devget?token=" + token + "&hl=" + Locale.getDefault().getLanguage() + "&res=0&devid=" + macParam, macParam)){
                                    intent.putExtra("errorMessage", "yes");
                                }
                            } catch (Exception e) {
                               intent.putExtra("errorMessage", "yes");
                                e.printStackTrace();
                            }
                        }
*/
                    //}

            //        sendBroadcast(intent);
                    stopRepeatingTask();
                    //    mHandler.postDelayed(mStatusChecker, 7000);

         //       }
         //   }).start();
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void startRepeatingTask(final String devid) {
        new Runnable() {

            @Override
            public void run() {
                con.queryDevices( devid, (short)0x0007);
            }
        }.run();
    }

    void stopRepeatingTask() {
    //    mHandler.removeCallbacks(mStatusChecker);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UDPListenerService.MyBinder myBinder = (UDPListenerService.MyBinder) service;
            UDPBinding = myBinder.getService();
            l = new ListDevicesAdapter(act, mySQLHelper, UDPBinding);
            list.setAdapter(l);
            UDPBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            UDPBound = false;
        }
    };

    @Override
    protected void onResume(){
        super.onResume();
        //    if(networkUtil.getConnectionStatus(this) == 1) {
        startService(mDNS);
            Log.i("ListDevices", "onResume refreshing");
            startRepeatingTask();
            Intent j = new Intent(this, UDPListenerService.class);
            bindService(j, serviceConnection, Context.BIND_AUTO_CREATE);
            registerReceiver(device_info, new IntentFilter("device_info"));
            registerReceiver(new_device_receiver, new IntentFilter("mDNS_New_Device_Found"));
            registerReceiver(device_removed_receiver, new IntentFilter("mDNS_Device_Removed"));
            registerReceiver(gcm_notification, new IntentFilter("gcm_notification"));
            registerReceiver(gcm_notification_done, new IntentFilter("gcmNotificationDone"));
            //registerReceiver(device_status_changed, new IntentFilter("device_status_changed"));
            registerReceiver(status_changed_update_ui, new IntentFilter("status_changed_update_ui"));
            registerReceiver(m1updateui, new IntentFilter("m1updateui"));
            registerReceiver(adapter_onClick, new IntentFilter("adapter_onClick"));
            registerReceiver(repeatingTaskDone, new IntentFilter("repeatingTaskDone"));
            registerReceiver(UpdateAlarmServiceDone, new IntentFilter("UpdateAlarmServiceDone"));
            registerReceiver(http_device_status, new IntentFilter("http_device_status"));
            registerReceiver(delete_sent, new IntentFilter("delete_sent"));
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(GlobalVariables.REGISTRATION_COMPLETE));
    //    }
    }
    @Override
    protected void onPause(){
        super.onPause();
    //    if(networkUtil.getConnectionStatus(this) == 1) {
        removeGrayOutView();
        stopRepeatingTask();
    //    stopService(mDNS);
        unbindService(serviceConnection);
            unregisterReceiver(new_device_receiver);
            unregisterReceiver(device_removed_receiver);
            unregisterReceiver(device_info);
            unregisterReceiver(gcm_notification);
            unregisterReceiver(gcm_notification_done);
            //unregisterReceiver(device_status_changed);
            unregisterReceiver(status_changed_update_ui);
            unregisterReceiver(m1updateui);
            unregisterReceiver(adapter_onClick);
            unregisterReceiver(repeatingTaskDone);
            unregisterReceiver(UpdateAlarmServiceDone);
            unregisterReceiver(http_device_status);
            unregisterReceiver(delete_sent);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
    //    }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        stopService(mDNS);
        stopRepeatingTask();

        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("alarms", "outdated");
        editor.commit();
    }

    public void registerOnGCM(){
        if (Miscellaneous.checkPlayServices(this)) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
            Log.d("PLAY SERVICE", "Starting Registration Intent Service");
        }
    }

    public void getDeviceList(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                String param = "devlist?token="+token+"&hl="+ Locale.getDefault().getLanguage()+"&res=1";
                try {
                    System.out.println("GET DEVICE LIST IP: " + NewDeviceList.ip);
                    httpHelper.getDeviceList(param);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void grayOutView(){
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.VISIBLE);
    }

    public void removeGrayOutView(){
        if( l!=null )
            l.getData();
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.INVISIBLE);
    }

}