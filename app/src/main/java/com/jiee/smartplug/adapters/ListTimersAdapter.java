package com.jiee.smartplug.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jiee.smartplug.M1;
import com.jiee.smartplug.M1SnoozeDialog;
import com.jiee.smartplug.R;
import com.jiee.smartplug.S2_Schedule;
import com.jiee.smartplug.S3;
import com.jiee.smartplug.YesNoDialog;
import com.jiee.smartplug.objects.Alarm;
import com.jiee.smartplug.objects.AlarmList;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

/**
 * Created by ronaldgarcia on 4/2/16.
 */
public class ListTimersAdapter extends BaseAdapter {

    private final Activity context;
    private List<Alarm> alarms = new ArrayList<Alarm>();
    private TextView txt_timer;
    private ImageButton btn_icon;
    private TextView txt_service;
    private ImageButton btn_service;
    private ImageButton btn_edit_timer;
    private ImageButton btn_delete_timer;
    int service_id;
    String device_id;
    GlobalVariables gb = new GlobalVariables();
    Miscellaneous misc = new Miscellaneous();
    UDPCommunication udp = new UDPCommunication();
    MySQLHelper sql;
    int globalposition;

    public ListTimersAdapter(Activity c, String device_id, int service_id){
        this.context = c;
        this.device_id = device_id;
        this.service_id = service_id;
        getAlarms();
    }

    public void getAlarms(){
        this.alarms.clear();
        this.alarms = misc.populateAlarm(context, device_id, service_id);
    }

    @Override
    public int getCount() {
        return alarms.size();
    }

    @Override
    public Object getItem(int position) {
        return alarms.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent){
        globalposition = position;
        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.s2_schedule_adapter, parent, false);
        }

        txt_timer = (TextView) convertView.findViewById(R.id.txt_timer);
        btn_icon = (ImageButton) convertView.findViewById(R.id.btn_icon);
        txt_service = (TextView) convertView.findViewById(R.id.txt_service);
        btn_service = (ImageButton) convertView.findViewById(R.id.btn_service);
        btn_edit_timer = (ImageButton) convertView.findViewById(R.id.btn_edit_timer);
        btn_delete_timer = (ImageButton) convertView.findViewById(R.id.btn_delete_timer);
        String name = "";
        if(((alarms.get(position).getDow() >> 1) & 1) == 1){
            name += "Sun,";
        }
        if(((alarms.get(position).getDow() >> 2) & 1) == 1){
            name += "Mon,";
        }
        if(((alarms.get(position).getDow() >> 3) & 1) == 1){
            name += "Tue,";
        }
        if(((alarms.get(position).getDow() >> 4) & 1) == 1){
            name += "Wed,";
        }
        if(((alarms.get(position).getDow() >> 5) & 1) == 1){
            name += "Thu,";
        }
        if(((alarms.get(position).getDow() >> 6) & 1) == 1){
            name += "Fri,";
        }
        if(((alarms.get(position).getDow() >> 7) & 1) == 1){
            name += "Sat,";
        }

        String iH = String.format("%02d", alarms.get(position).getInit_hour());
        String iM = String.format("%02d", alarms.get(position).getInit_minute());
        String eH = String.format("%02d", alarms.get(position).getEnd_hour());
        String eM = String.format("%02d", alarms.get(position).getEnd_minute());

        name += " "+iH+":"+iM+" to "+eH+":"+eM;

        txt_timer.setText(name);
        if(service_id == gb.ALARM_RELAY_SERVICE){
            txt_service.setText(context.getApplicationContext().getString(R.string.btn_outlet));
            btn_service.setImageResource(R.drawable.svc_0_small);
        } else if(service_id == gb.ALARM_NIGHLED_SERVICE){
            txt_service.setText(context.getApplicationContext().getString(R.string.btn_nightLight));
            btn_service.setImageResource(R.drawable.svc_1_small);
        }

        btn_delete_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final YesNoDialog cd = new YesNoDialog(context, 4, alarms.get(position).getAlarm_id());   // 1 = logout, 2 = M1 : device_id : service_id
                cd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                cd.show();
            }
        });

        btn_delete_timer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    btn_delete_timer.setImageResource(R.drawable.ic_delete_pressed);
                }
                return false;
            }
        });

        btn_edit_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, S3.class);
                i.putExtra("service_id", service_id);
                i.putExtra("alarm_id", alarms.get(position).getAlarm_id());
                context.startActivity(i);
                context.finish();
            }
        });

        btn_edit_timer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    btn_edit_timer.setImageResource(R.drawable.ic_edit_pressed);
                }
                return false;
            }
        });

        return convertView;
    }

}
