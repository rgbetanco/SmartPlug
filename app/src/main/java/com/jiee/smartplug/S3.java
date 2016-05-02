package com.jiee.smartplug;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.jiee.smartplug.objects.Alarm;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;


public class S3 extends AppCompatActivity {

    Button monday;
    Button tuesday;
    Button wednesday;
    Button thursday;
    Button friday;
    Button saturday;
    Button sunday;

    Button btn_settings;

    Button init_time;
    Button end_time;
    Button init_IR;
    Button end_IR;
    private int init_hour;
    private int end_hour;
    private int init_minute;
    private int end_minute;

    private String device_id;
    private String name;
    private String init_ir_name;
    private String end_ir_name;
    private int service_id;
    private int alarm_id = -1;

    static final int TIME_DIALOG_ID_INIT = 999;
    static final int TIME_DIALOG_ID_END = 998;

    ImageView img_action;
    TextView txt_action;
    ImageView img_action_service;

    MySQLHelper sql;

    byte dow = 0b00000000;
    byte init_ir_code = -1;
    byte end_ir_code = -1;

    UDPCommunication udp;
    BroadcastReceiver timers_sent_successfully;
    BroadcastReceiver device_not_reached;
    boolean deviceStatusChangedFlag = false;
    boolean udpcommunication = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_s3);

        udp = new UDPCommunication();
        sql = HTTPHelper.getDB(this);

        img_action = (ImageView) findViewById(R.id.img_action);
        txt_action = (TextView) findViewById(R.id.txt_action);
        img_action_service = (ImageView) findViewById(R.id.img_action_service);

        Intent i = getIntent();
        service_id = i.getIntExtra("service_id", GlobalVariables.ALARM_RELAY_SERVICE);
        device_id = i.getStringExtra("device_id");
        alarm_id = i.getIntExtra("alarm_id", -1);
        txt_action.setText(M1.givenName);

        device_not_reached = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("BROADCAST DEVICE NOT REACHED");
                String error = "";
                error = intent.getStringExtra("error");
                if(error != null && !error.isEmpty()){
                    Toast.makeText(S3.this, getApplicationContext().getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(S3.this, getApplicationContext().getString(R.string.please_wait), Toast.LENGTH_LONG).show();
                }

            }
        };

        if(service_id == GlobalVariables.ALARM_RELAY_SERVICE){
            img_action_service.setImageResource(R.drawable.svc_0_big);
        }

        if(service_id == GlobalVariables.ALARM_NIGHLED_SERVICE){
            img_action_service.setImageResource(R.drawable.svc_1_big);
        }

        timers_sent_successfully = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                System.out.println("TIMERS SENT SUCCESSFULLY BROADCAST");
                deviceStatusChangedFlag = true;
                udpcommunication = true;
            }
        };

        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView sub_red_title = (TextView) findViewById(R.id.sub_toolbar_red);
        sub_red_title.setText(R.string.title_scheduleAction);

        init_IR = (Button) findViewById(R.id.init_IR);
        end_IR = (Button) findViewById(R.id.end_IR);

        init_IR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(S3.this, IRListCommands.class);
                i.putExtra("status", 0);
                startActivityForResult(i, 3);  // 3 was random chosen
            }
        });

        end_IR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(S3.this, IRListCommands.class);
                i.putExtra("status", 1);
                startActivityForResult(i, 3);  // 3 was random chosen
            }
        });

        if(service_id != GlobalVariables.ALARM_IR_SERVICE){
            init_IR.setVisibility(View.GONE);
            end_IR.setVisibility(View.GONE);
        }

        monday = (Button) findViewById(R.id.btn_monday);
        tuesday = (Button) findViewById(R.id.btn_tuesday);
        wednesday = (Button) findViewById(R.id.btn_wednesday);
        thursday = (Button) findViewById(R.id.btn_thursday);
        friday = (Button) findViewById(R.id.btn_friday);
        saturday = (Button) findViewById(R.id.btn_saturday);
        sunday = (Button) findViewById(R.id.btn_sunday);

        if(alarm_id >= 0){
            Cursor s = sql.getAlarmData(alarm_id);
            s.moveToFirst();
            init_hour = s.getInt(4);
            init_minute = s.getInt(5);
            end_hour = s.getInt(6);
            end_minute = s.getInt(7);
            init_ir_code = (byte)s.getInt(9);
            end_ir_code = (byte)s.getInt(10);
            dow = (byte)s.getInt(3);
            setDOW();
            s.close();
        } else {
            if(dow == 0) {
                dow |= (1 << 1);
            }
            setDOW();
        }

        /* returns 1-7. Sun-1, Mon-2 ... Sat-7 */

        monday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            dow ^= (1 << 1);
            setDOW();
            }
        });

        tuesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dow ^= (1 << 2);
                setDOW();
            }
        });

        wednesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dow ^= (1 << 3);
                setDOW();
            }
        });

        thursday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dow ^= (1 << 4);
                setDOW();
            }
        });

        friday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dow ^= (1 << 5);
                setDOW();
            }
        });

        saturday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dow ^= (1 << 6);
                setDOW();
            }
        });

        sunday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dow ^= (1 << 0);
                setDOW();
            }
        });

        init_time = (Button) findViewById(R.id.init_time);
        if(alarm_id != -1){
            init_time.setText( Miscellaneous.getTime(init_hour,init_minute));
        }
        init_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID_INIT);
            }
        });

        end_time = (Button) findViewById(R.id.end_time);
        if(alarm_id != -1){
            end_time.setText(Miscellaneous.getTime(end_hour,end_minute));
        }
        end_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(TIME_DIALOG_ID_END);
            }
        });

        if(alarm_id != -1){
            System.out.println("init_ir_code: "+init_ir_code);
            String init_name = "";
            Cursor r = sql.getIRCodeById(init_ir_code);
            if(r.getCount()>0){
                r.moveToFirst();
                init_name = r.getString(0);
            }
            r.close();
            init_IR.setText(init_name);

            String end_name = "";
            Cursor d = sql.getIRCodeById(end_ir_code);
            if(d.getCount()>0){
                d.moveToFirst();
                end_name = d.getString(0);
            }
            r.close();
            end_IR.setText(end_name);
        }

        btn_settings = (Button) findViewById(R.id.btn_settings);
        btn_settings.setText(R.string.btn_save);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_settings.setEnabled(false);
                final Alarm a = new Alarm();
                a.setAlarm_id(alarm_id);
                a.setDevice_id(M1.mac);
                a.setService_id(service_id);
                a.setDow(dow);
                a.setInit_hour(init_hour);
                a.setInit_minute(init_minute);
                a.setEnd_hour(end_hour);
                a.setEnd_minute(end_minute);
                a.setInit_ir(init_ir_code);
                a.setEnd_ir(end_ir_code);
                a.setSnooze(0);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sql = HTTPHelper.getDB(S3.this);
                        if(alarm_id >= 0) {
                            System.out.println("updating - dOW:"+dow+"Init Hour: "+init_hour+" Init Minute: "+init_minute+" End Hour: "+end_hour+" End Minute: "+end_minute+" MAC:"+M1.mac);
                            sql.updateAlarm(a);
                        } else {
                            System.out.println("inserting - dOW:"+dow+"Init Hour: "+init_hour+" Init Minute: "+init_minute+" End Hour: "+end_hour+" End Minute: "+end_minute+" MAC:"+M1.mac);
                            sql.insertAlarm(a);
                        }
                        Intent i = new Intent("device_not_reached");
                        if(udp.sendTimers(S3.this, M1.mac, M1.ip)) {

                            int counter = 10000;
                            while (!deviceStatusChangedFlag && counter > 0) {
                                counter--;
                                //waiting time
                            }
                        }

                        if(!deviceStatusChangedFlag) {
                            if(!udp.sendTimersHTTP(S3.this, M1.mac, 0)){
                                i.putExtra("error", "yes");
                            } else {
                                i.putExtra("error","");
                                deviceStatusChangedFlag = false;
                            }
                        } else {
                            udp.sendTimersHTTP(S3.this, M1.mac, 1);
                            i.putExtra("error","");
                            deviceStatusChangedFlag = false;
                        }

                        sendBroadcast(i);
                    }
                }).start();

                finish();

            }
        });

        if( savedInstanceState != null ) {
            alarm_id = savedInstanceState.getInt("alarmId");
            dow = savedInstanceState.getByte("dow");
            init_hour = savedInstanceState.getInt("init_hour");
            init_minute = savedInstanceState.getInt("init_minute");
            end_hour = savedInstanceState.getInt("end_hour");
            end_minute = savedInstanceState.getInt("end_minute");
            init_ir_code = savedInstanceState.getByte("init_ir_code");
            end_ir_code = savedInstanceState.getByte("end_ir_code");
            service_id = savedInstanceState.getInt("serviceId");
            device_id = savedInstanceState.getString("deviceId");
            init_ir_name = savedInstanceState.getString("init_ir_name");
            end_ir_name = savedInstanceState.getString("end_ir_name");
            if(init_time != null) {
                init_time.setText(Miscellaneous.getTime(init_hour, init_minute));
            }
            if(end_time != null) {
                end_time.setText(Miscellaneous.getTime(end_hour, end_minute));
            }
            if(init_ir_code >= 0) {
                init_IR.setText(init_ir_name);
            }
            if(end_ir_code >= 0) {
                end_IR.setText(end_ir_name);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putByte("dow", dow);
        outState.putInt("init_hour", init_hour);
        outState.putInt("end_hour", end_hour);
        outState.putInt("init_minute", init_minute);
        outState.putInt("end_minute", end_minute);
        outState.putByte("init_ir_code", init_ir_code);
        outState.putByte("end_ir_code", end_ir_code);
        outState.putInt("serviceId", service_id);
        outState.putString("deviceId", device_id);
        outState.putInt("alarmId", alarm_id);
        outState.putString("init_ir_name", init_ir_name);
        outState.putString("end_ir_name", end_ir_name);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sql = HTTPHelper.getDB(this);
        if(resultCode == 3){
            int localStatus = data.getIntExtra("status", -1);
            String groupName = data.getStringExtra("group");
            String IRName = data.getStringExtra("irName");

            int groupId = 0;
            int IRId = -1;

            Cursor c = sql.getIRGroupByName(groupName);
            if(c.getCount()>0){
                c.moveToFirst();
                groupId = c.getInt(4);
                for (int i = 0; i < c.getCount(); i++){
                    Cursor cur = sql.getIRCodesByGroup(groupId);
                    if(cur.getCount()>0){
                        cur.moveToFirst();
                        for(int j = 0; j < cur.getCount(); j++){
                            if(IRName.equals(cur.getString(2))){
                                IRId = cur.getInt(3);
                            }
                            cur.moveToNext();
                        }
                    }
                    cur.close();
                    c.moveToNext();
                }
            }
            c.close();

            switch (localStatus){
                case 0:
                    init_ir_name = IRName;
                    init_IR.setText(IRName);
                    init_ir_code = (byte)IRId;
                    break;
                case 1:
                    end_ir_name = IRName;
                    end_IR.setText(IRName);
                    end_ir_code = (byte)IRId;
                    break;
            }

            //System.out.println("init_ir_code: "+init_ir_code+", end_ir_code: "+end_ir_code);

        }

    }

    public void setDOW(){

        if(((dow >> 0) & 1) == 1) {
            sunday.setBackgroundResource(R.drawable.btn_dow_bkgnd_sel);
            sunday.setTextColor(getResources().getColor(R.color.black));
        } else {
            sunday.setBackgroundResource(R.drawable.btn_bkgnd);
            sunday.setTextColor(getResources().getColor(R.color.white));
        }

        if(((dow >> 1) & 1) == 1){
            monday.setBackgroundResource(R.drawable.btn_dow_bkgnd_sel);
            monday.setTextColor(getResources().getColor(R.color.black));
        } else {
            monday.setBackgroundResource(R.drawable.btn_bkgnd);
            monday.setTextColor(getResources().getColor(R.color.white));
        }

        if(((dow >> 2) & 1) == 1){
            tuesday.setBackgroundResource(R.drawable.btn_dow_bkgnd_sel);
            tuesday.setTextColor(getResources().getColor(R.color.black));
        } else {
            tuesday.setBackgroundResource(R.drawable.btn_bkgnd);
            tuesday.setTextColor(getResources().getColor(R.color.white));
        }

        if(((dow >> 3) & 1) == 1){
            wednesday.setBackgroundResource(R.drawable.btn_dow_bkgnd_sel);
            wednesday.setTextColor(getResources().getColor(R.color.black));
        } else {
            wednesday.setBackgroundResource(R.drawable.btn_bkgnd);
            wednesday.setTextColor(getResources().getColor(R.color.white));
        }

        if(((dow >> 4) & 1) == 1){
            thursday.setBackgroundResource(R.drawable.btn_dow_bkgnd_sel);
            thursday.setTextColor(getResources().getColor(R.color.black));
        } else {
            thursday.setBackgroundResource(R.drawable.btn_bkgnd);
            thursday.setTextColor(getResources().getColor(R.color.white));
        }

        if(((dow >> 5) & 1) == 1){
            friday.setBackgroundResource(R.drawable.btn_dow_bkgnd_sel);
            friday.setTextColor(getResources().getColor(R.color.black));
        } else {
            friday.setBackgroundResource(R.drawable.btn_bkgnd);
            friday.setTextColor(getResources().getColor(R.color.white));
        }

        if(((dow >> 6) & 1) == 1) {
            saturday.setBackgroundResource(R.drawable.btn_dow_bkgnd_sel);
            saturday.setTextColor(getResources().getColor(R.color.black));
        } else {
            saturday.setBackgroundResource(R.drawable.btn_bkgnd);
            saturday.setTextColor(getResources().getColor(R.color.white));
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID_INIT:
                // set time picker as current time
                return new TimePickerDialog(this, timePickerListener, init_hour, init_minute, false);
            case TIME_DIALOG_ID_END:
                // set time picker as current time
                return new TimePickerDialog(this, timePickerListenerEnd, end_hour, end_minute, false);

        }
        return null;
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener =
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int selectedHour,
                                      int selectedMinute) {
                    view.setIs24HourView(true);
                    init_hour = selectedHour;
                    init_minute = selectedMinute;

                    init_time.setText(Miscellaneous.getTime(init_hour, init_minute));

                }
            };

    private TimePickerDialog.OnTimeSetListener timePickerListenerEnd =
            new TimePickerDialog.OnTimeSetListener() {
                public void onTimeSet(TimePicker view, int selectedHour,
                                      int selectedMinute) {
                    view.setIs24HourView(true);
                    end_hour = selectedHour;
                    end_minute = selectedMinute;

                    // set current time into textview
                    end_time.setText(Miscellaneous.getTime(end_hour, end_minute));

                }
            };

    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(timers_sent_successfully, new IntentFilter("timers_sent_successfully"));
        registerReceiver(device_not_reached, new IntentFilter("device_not_reached"));
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(timers_sent_successfully);
        unregisterReceiver(device_not_reached);
    }
}
