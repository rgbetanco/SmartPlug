package com.jiee.smartplug;

/** TO DO:
 *
 /*
 S2_Schedule.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 This class list all the alarms per service and allow the user to edit or delete any alarm.
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.jiee.smartplug.adapters.ListTimersAdapter;
import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

import java.util.List;

public class S2_Schedule extends Activity {

    ListView list;
    ListTimersAdapter adapter;
    String device_id;
    int service_id;
    BroadcastReceiver listchange;
    BroadcastReceiver timers_sent_successfully;
    BroadcastReceiver gcm_notification;
    UDPCommunication udp;
    ImageButton btn_menu_new;
    Button btn_update;
    boolean udpconnection = false;
    HTTPHelper http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        udp = new UDPCommunication(this);
        http = new HTTPHelper(this);

        setContentView(R.layout.activity_s2__schedule);

        btn_menu_new = (ImageButton)findViewById(R.id.btn_ic_new);

        Intent i = getIntent();

        device_id = i.getStringExtra("device_id");
        service_id = i.getIntExtra("service_id", GlobalVariables.ALARM_RELAY_SERVICE);

        btn_update = (Button)findViewById(R.id.btn_update_alarms);
        btn_update.setVisibility(View.GONE);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(S2_Schedule.this, getApplicationContext().getString(R.string.please_wait), Toast.LENGTH_LONG).show();
                udpconnection = false;
                udp.sendTimers(M1.mac);
                //        udp.sendTimers(M1.mac, S2_Schedule.this, 0);
                //udp.setDeviceTimersUDP(M1.mac, S2_Schedule.this);     // This is sending both UDP and HTTP to server
            }
        });

        list = (ListView)findViewById(R.id.list_schedule);
        adapter = new ListTimersAdapter(this, device_id, service_id);
        list.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        gcm_notification = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.getAlarms();
                adapter.notifyDataSetChanged();
            }
        };

        listchange = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.getAlarms();
                adapter.notifyDataSetChanged();

                Toast.makeText(S2_Schedule.this, getApplicationContext().getString(R.string.please_wait), Toast.LENGTH_LONG).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(M1.mac != null && !M1.mac.isEmpty()) {
                            udp.sendTimers(M1.mac);
                            udp.sendTimersHTTP(M1.mac, 0);
                        } else {
                            System.out.println("M1 MAC EMPTY oR NULL");
                        }
                    }
                }).start();
            }
        };

        timers_sent_successfully = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                udpconnection = true;
                Toast.makeText(S2_Schedule.this, getApplicationContext().getString(R.string.alarms_sent_success), Toast.LENGTH_LONG).show();
            }
        };

        btn_menu_new.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), S3.class);
                i.putExtra("service_id", service_id);
                i.putExtra("device_id", device_id);
                startActivity(i);
                finish();
            }
        });

    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(listchange, new IntentFilter("alarm_list_changed"));
        registerReceiver(timers_sent_successfully, new IntentFilter("timers_sent_successfully"));
        registerReceiver(gcm_notification, new IntentFilter("gcm_notification"));
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(listchange);
        unregisterReceiver(timers_sent_successfully);
        unregisterReceiver(gcm_notification);
    }
}
