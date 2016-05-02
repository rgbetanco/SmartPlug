package com.jiee.smartplug;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.adapters.ListAlarmsAdapter;
import com.jiee.smartplug.objects.AlarmList;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ronaldgarcia on 29/12/15.
 */
public class M1SnoozeDialog extends Dialog implements View.OnClickListener {

    Activity activity;
    ListView listView;
    String device_id;
    Miscellaneous mi;
    Button s2_schedule;
    Button s5;
    Button s10;
    Button s30;
    Button s59;
    Button add_new_timerl;
    Button sCancel;
    MySQLHelper sql;
    int service_id;
    List<AlarmList> values = new ArrayList<AlarmList>();
    UDPCommunication udp;
    BroadcastReceiver timer_delay;
    HTTPHelper http;
    int snooze = 0;
    boolean deviceStatusChangedFlag = false;
    GlobalVariables gb = new GlobalVariables();

    public M1SnoozeDialog(Activity a, String device_id, int snooze, int service_id){
        super(a);
        http = new HTTPHelper(a);
        this.activity = a;
        this.device_id = device_id;
        this.snooze = snooze;
        this.service_id = service_id;
        this.timer_delay = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                deviceStatusChangedFlag = true;
                Toast.makeText(activity, "Snooze sent successfully", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.m1_snooze_dialog);
        sql = new MySQLHelper(activity);
        udp = new UDPCommunication();

    //    activity.registerReceiver(timer_delay, new IntentFilter("set_timer_delay"));

        mi = new Miscellaneous();
        values = mi.populateAlarmList(activity, device_id, service_id);

        LayoutInflater inflater = getLayoutInflater();
        ViewGroup footer = (ViewGroup) inflater.inflate(R.layout.m1_snooze_dialog_buttons, listView,
                false);

        s5 = (Button)footer.findViewById(R.id.btn_snooze_five);
        if(snooze > 0) {
            s5.setText(R.string.delay_plus_five);
        }
        s5.setOnClickListener(this);

        s10 = (Button)footer.findViewById(R.id.btn_snooze_ten);
        if(snooze > 0) {
            s10.setText(R.string.delay_plus_ten);
        }
        s10.setOnClickListener(this);
        s30 = (Button)footer.findViewById(R.id.btn_snooze_thirty);
        if(snooze > 0) {
            s30.setText(R.string.delay_plus_thirty);
        }
        s30.setOnClickListener(this);

        s59 = (Button)footer.findViewById(R.id.btn_snooze_sixty);
        if(snooze > 0) {
            s59.setText(R.string.delay_plus_sixty);
        }
        s59.setOnClickListener(this);

        sCancel = (Button)footer.findViewById(R.id.btn_snooze_cancel);
        if(snooze > 0) {
            sCancel.setVisibility(View.VISIBLE);
        }
        sCancel.setOnClickListener(this);

        s2_schedule = (Button)footer.findViewById(R.id.btn_show_modify_timer);
        s2_schedule.setOnClickListener(this);

        add_new_timerl = (Button)footer.findViewById(R.id.btn_add_new_timer);
        add_new_timerl.setOnClickListener(this);

        TextView sub_toolbar = (TextView) findViewById(R.id.sub_toolbar);
        if(snooze == 0) {
            sub_toolbar.setText(R.string.timer_set);
        } else {
            String snoozing = getContext().getString(R.string.timer_set_snoozing)+" "+snooze+" "+getContext().getString(R.string.minutes);
            sub_toolbar.setText(snoozing.toString());
        }

        listView = (ListView)findViewById(R.id.alarm_list_view);
        listView.addFooterView(footer);
        ListAlarmsAdapter listAlarmsAdapter = new ListAlarmsAdapter(activity, values);

        listView.setAdapter(listAlarmsAdapter);

    }

    @Override
    protected void onStop(){
        super.onStop();
//        activity.unregisterReceiver(timer_delay);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_snooze_five:
                sendSnooze(5);
                break;
            case R.id.btn_snooze_ten:
                sendSnooze(10);
                break;
            case R.id.btn_snooze_thirty:
                sendSnooze(30);
                break;
            case R.id.btn_snooze_sixty:
                sendSnooze(59);
                break;
            case R.id.btn_snooze_cancel:
                sendSnooze(0);
                break;

            case R.id.btn_show_modify_timer:
                Intent i = new Intent(activity, S2_Schedule.class);
                i.putExtra("device_id", device_id);
                i.putExtra("service_id", service_id);
                activity.startActivity(i);
                break;
            case R.id.btn_add_new_timer:
                Intent in = new Intent(activity, S3.class);
                in.putExtra("device_id", device_id);
                in.putExtra("service_id", service_id);
                activity.startActivity(in);
                break;
        }
        dismiss();
    }

    public void sendSnooze(final int min){
        final int minutes = min;

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(service_id == gb.ALARM_RELAY_SERVICE){
                    snooze = sql.getRelaySnooze(device_id);
                    if(minutes > 0) {
                        snooze += minutes;
                    } else {
                        snooze = 0;
                    }
                }
                if(service_id == gb.ALARM_NIGHLED_SERVICE){
                    snooze = sql.getLedSnooze(device_id);
                    if(minutes > 0) {
                        snooze += minutes;
                    } else {
                        snooze = 0;
                    }
                }
                if(service_id == gb.ALARM_IR_SERVICE){
                    snooze = sql.getIRSnooze(device_id);
                    if(minutes > 0){
                        snooze += minutes;
                    } else {
                        snooze = 0;
                    }
                }

                if(udp.delayTimer(snooze, 1, activity, service_id, 0)) {   //SENDING SNOOZE OF 5 MINUTES TO THE DEVICE
                    int counter = 10000;
                    while (!deviceStatusChangedFlag && counter > 0) {
                        counter--;
                        //waiting time
                    }
                }

                if(!deviceStatusChangedFlag){
                    Intent i = new Intent("device_not_reached");
                    if(!udp.delayTimer(snooze, 0, activity, service_id, 0)){
                        i.putExtra("error", "yes");
                        activity.sendBroadcast(i);
                    } else {
                        if(service_id == gb.ALARM_RELAY_SERVICE){
                            if(minutes > 0) {
                                sql.updateDeviceSnooze(device_id, gb.ALARM_RELAY_SERVICE, snooze);
                                snooze += minutes;
                            } else {
                                sql.updateDeviceSnooze(device_id, gb.ALARM_RELAY_SERVICE, 0);
                                snooze = 0;
                            }
                        }
                        if(service_id == gb.ALARM_NIGHLED_SERVICE){
                            if(minutes > 0) {
                                sql.updateDeviceSnooze(device_id, gb.ALARM_NIGHLED_SERVICE, snooze);
                                snooze += minutes;
                            } else {
                                sql.updateDeviceSnooze(device_id, gb.ALARM_NIGHLED_SERVICE, 0);
                                snooze = 0;
                            }
                        }
                        Intent j = new Intent("status_changed_update_ui");
                        activity.sendBroadcast(j);
                     //   Toast.makeText(activity, activity.getApplicationContext().getString(R.string.timer)+" "+getContext().getString(R.string.snooze)+" "+snooze+" "+getContext().getString(R.string.minutes), Toast.LENGTH_SHORT).show();
                        deviceStatusChangedFlag = false;
                    }

                } else {
                    if(service_id == gb.ALARM_RELAY_SERVICE){
                        if(minutes > 0) {
                            sql.updateDeviceSnooze(device_id, service_id, snooze);
                            snooze += minutes;
                        } else {
                            sql.updateDeviceSnooze(device_id, service_id, 0);
                            snooze = 0;
                        }
                    }
                    if(service_id == gb.ALARM_NIGHLED_SERVICE){
                        if(minutes > 0) {
                            sql.updateDeviceSnooze(device_id, service_id, snooze);
                            snooze += minutes;
                        } else {
                            sql.updateDeviceSnooze(device_id, service_id, 0);
                            snooze = 0;
                        }
                    }
                    if(service_id == gb.ALARM_IR_SERVICE){
                        if(minutes > 0) {
                            sql.updateDeviceSnooze(device_id, service_id, snooze);
                            snooze += minutes;
                        } else {
                            sql.updateDeviceSnooze(device_id, service_id, 0);
                            snooze = 0;
                        }
                    }
                    Intent i = new Intent("status_changed_update_ui");
                    activity.sendBroadcast(i);
        //            udp.delayTimer(snooze, 0, activity, service_id, 1);
        //            Toast.makeText(activity, activity.getApplicationContext().getString(R.string.timer)+" "+getContext().getString(R.string.snooze)+" "+snooze+" "+getContext().getString(R.string.minutes), Toast.LENGTH_SHORT).show();
                    deviceStatusChangedFlag = false;
                }
            }
        }).start();
    }

}
