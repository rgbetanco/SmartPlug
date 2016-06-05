package com.jiee.smartplug.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.jiee.smartplug.Http;
import com.jiee.smartplug.ListDevices;
import com.jiee.smartplug.M1;
import com.jiee.smartplug.MainActivity;
import com.jiee.smartplug.NewDeviceList;
import com.jiee.smartplug.R;
import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.services.CrashCountDown;
import com.jiee.smartplug.services.ListDevicesServicesService;
import com.jiee.smartplug.services.M1ServicesService;
import com.jiee.smartplug.services.RegistrationIntentService;
import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by ronaldgarcia on 14/12/15.
 */
public class ListDevicesAdapter extends BaseAdapter {
    Activity act;
    MySQLHelper mySQLHelper;
    List<JSmartPlug> SmartPlugsList;
//    JSmartPlug js;
    int relay = 0;
    HTTPHelper http;
    UDPCommunication udp;
    UDPListenerService UDPBinding;
    ProgressBar pb;
    TextView name;
    String plugname;
    byte action;
    int serviceId;
    public static String selectedIP;
    public static String mac;
    public static String param;
    Handler mHandler;
    int globlaPosition = 0;
    boolean deviceStatusChangedFlag = false;
    CrashCountDown crashTimer;

    final LayoutInflater inflater;

    public ListDevicesAdapter(Activity a, MySQLHelper o, UDPListenerService UDPBinding){
        udp = new UDPCommunication();

        inflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.act = a;
        this.mySQLHelper = o;
        this.UDPBinding = UDPBinding;
        SmartPlugsList = getSmartPlugsList();
        http = new HTTPHelper(a);
        crashTimer = new CrashCountDown(this.act);

        pb = (ProgressBar)a.findViewById(R.id.DeviceListProgress);
    }

    public void setDeviceStatusChangedFlag(boolean p){
        deviceStatusChangedFlag = p;
    }

    @Override
    public int getCount() {
        return SmartPlugsList.size();
    }

    @Override
    public Object getItem(int position) {
        return SmartPlugsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_devices, parent, false);
        }

        SwipeLayout swipeLayout = (SwipeLayout) convertView.findViewById(R.id.swipe);
        //set show mode.
        swipeLayout.setShowMode(SwipeLayout.ShowMode.LayDown);

        //add drag edge.(If the BottomView has 'layout_gravity' attribute, this line is unnecessary)
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, convertView.findViewById(R.id.bottom_wrapper));

        swipeLayout.addSwipeListener(new SwipeLayout.SwipeListener() {
            @Override
            public void onClose(SwipeLayout layout) {
                //when the SurfaceView totally cover the BottomView.
            }

            @Override
            public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {
                //you are swiping.
            }

            @Override
            public void onStartOpen(SwipeLayout layout) {

            }

            @Override
            public void onOpen(SwipeLayout layout) {
                //when the BottomView totally show.
            }

            @Override
            public void onStartClose(SwipeLayout layout) {

            }

            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {
                //when user's hand released.
            }
        });

        ImageButton deleteDevice = (ImageButton) convertView.findViewById(R.id.btn_delete_device);
        deleteDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("SENDING REFORMAT DEVICE COMMAND");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        udp.sendReformatCommand(SmartPlugsList.get(position).getIp());
                    }
                }).start();

                mac = SmartPlugsList.get(position).getId();
                param = "devdel?token=" + ListDevices.token + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + mac;
                selectedIP = SmartPlugsList.get(position).getIp();

            }

        });

        name = (TextView) convertView.findViewById(R.id.plugname);

        ImageView im1 = (ImageView) convertView.findViewById(R.id.imageView1);
        im1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoM1(position);
            }
        });

        RelativeLayout device_layout = (RelativeLayout) convertView.findViewById(R.id.device_layout);
        device_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoM1(position);
            }
        });

        if(SmartPlugsList.size()>0) {


            relay = SmartPlugsList.get(position).getRelay();

            if (SmartPlugsList.get(position).getBackground() == 0) {
                convertView.setBackgroundResource(R.drawable.round_corners_btn_listdevices_one);
            } else if (SmartPlugsList.get(position).getBackground() == 1) {
                convertView.setBackgroundResource(R.drawable.round_corners_btn_listdevices_two);
            } else if (SmartPlugsList.get(position).getBackground() == 2) {
                convertView.setBackgroundResource(R.drawable.round_corners_btn_listdevices_three);
            } else if (SmartPlugsList.get(position).getBackground() == 3) {
                convertView.setBackgroundResource(R.drawable.round_corners_btn_listdevices_four);
            }
            plugname = SmartPlugsList.get(position).getName();
            name.setText(plugname);
            final String givenName = SmartPlugsList.get(position).getGivenName();
            if (givenName != null && !givenName.isEmpty()) {
                if (givenName.equals("null")) {
                    Log.d("Exception", "null");
                }
                name.setText(givenName);
            } else {
                name.setText(SmartPlugsList.get(position).getName());
            }

            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gotoM1(position);
                }
            });

            ImageButton btn_w = (ImageButton) convertView.findViewById(R.id.btn_warning);
            btn_w.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //this is the x button used previously
                }
            });

            ImageButton btn_s = (ImageButton) convertView.findViewById(R.id.btn_schedule);
            btn_s.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            ImageButton btn_schedule = (ImageButton) convertView.findViewById(R.id.btn_schedule);
            if (deviceHasAlarm(SmartPlugsList.get(position).getId())) {
                btn_schedule.setVisibility(View.VISIBLE);
                if (SmartPlugsList.get(position).getSnooze() > 0) {
                    btn_schedule.setImageResource(R.drawable.btn_timer_delay);
                }
            } else {
                btn_schedule.setVisibility(View.INVISIBLE);
            }

            ImageButton btn_warning = (ImageButton) convertView.findViewById(R.id.btn_warning);
            if (SmartPlugsList.get(position).getCo_sensor() > 0 || SmartPlugsList.get(position).getHall_sensor() > 0) {
                btn_warning.setVisibility(View.VISIBLE);
            } else {
                btn_warning.setVisibility(View.INVISIBLE);
            }

            final ImageButton btn_o = (ImageButton) convertView.findViewById(R.id.btn_switch);
            if (relay == 0) {
                btn_o.setImageResource(R.drawable.btn_power);
            } else {
                btn_o.setImageResource(R.drawable.btn_power_pressed);
            }
            btn_o.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //    Intent i = new Intent("adapter_onClick");
                    //    i.putExtra("start", 0);
                    //    act.sendBroadcast(i);
                    //Toast.makeText(act.getApplicationContext(), ""+SmartPlugsList.get(position).getIp(), Toast.LENGTH_SHORT).show();

                    Cursor u = mySQLHelper.getPlugDataByID(SmartPlugsList.get(position).getId());
                    if (u.getCount() > 0) {
                        u.moveToFirst();
                        switch (u.getInt(12)) {                                                                  //RELAY
                            case 0:
                                relay = 0;
                                break;
                            case 1:
                                relay = 1;
                                break;
                        }
                    }
                    u.close();
                    serviceId = GlobalVariables.ALARM_RELAY_SERVICE;

                    if (relay == 0) {
                        action = 0x01;
                    } else {
                        action = 0x00;
                    }

                    Intent iService = new Intent(act, ListDevicesServicesService.class);
                    ListDevicesServicesService.ip = SmartPlugsList.get(position).getIp();
                    ListDevicesServicesService.serviceId = serviceId;
                    ListDevicesServicesService.action = action;
                    ListDevicesServicesService.mac = SmartPlugsList.get(position).getId();
                    act.startService(iService);

                    crashTimer.setTimer(2);
                    crashTimer.startTimer();

                    /*

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent("repeatingTaskDone");
                            udp.setDeviceStatus(SmartPlugsList.get(position).getIp(), serviceId, action);
                            int counter = 1;
                            while (!deviceStatusChangedFlag && counter > 0) {
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                                counter--;
                                //waiting time
                            }

                            if (deviceStatusChangedFlag) {
                                if (serviceId == gb.ALARM_RELAY_SERVICE) {
                                    mySQLHelper.updatePlugRelayService(action, SmartPlugsList.get(position).getId());
                                }
                                if (serviceId == gb.ALARM_NIGHLED_SERVICE) {
                                    mySQLHelper.updatePlugNightlightService(action, SmartPlugsList.get(position).getId());
                                }
                                act.sendBroadcast(i);
                                try {
                                    http.setDeviceStatus("devctrl?token=" + ListDevices.token + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + SmartPlugsList.get(position).getId() + "&send=1&ignoretoken="+ RegistrationIntentService.regToken, action, serviceId);
                                } catch (Exception e){
                                    e.printStackTrace();
                                }
                            } else {

                                try {
                                    if (http.setDeviceStatus("devctrl?token=" + ListDevices.token + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + SmartPlugsList.get(position).getId()+"&send=0&ignoretoken="+ RegistrationIntentService.regToken, action, serviceId)) {
                                        if (serviceId == gb.ALARM_RELAY_SERVICE) {
                                            mySQLHelper.updatePlugRelayService(action, SmartPlugsList.get(position).getId());
                                        }
                                        if (serviceId == gb.ALARM_NIGHLED_SERVICE) {
                                            mySQLHelper.updatePlugNightlightService(action, SmartPlugsList.get(position).getId());
                                        }
                                        act.sendBroadcast(i);
                                        Log.i("LISTADAPTER", "devctrl?token=" + ListDevices.token + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + SmartPlugsList.get(position).getId() + " ACTION: " + action + " SERVICE: " + serviceId);
                                    } else {
                                        i.putExtra("errorMessage", "yes");
                                    }
                                } catch (Exception e) {
                                    i.putExtra("errorMessage", "yes");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                    */
                    //deviceStatusChangedFlag = false;

                }

            });

            ImageView iv = (ImageView) convertView.findViewById(R.id.imageView1);

            if (SmartPlugsList.get(position).getIcon() != null && !SmartPlugsList.get(position).getIcon().isEmpty()) {
                Picasso.with(act).load(SmartPlugsList.get(position).getIcon()).into(iv);
            } else {
                Picasso.with(act).load("http://flutehuang-001-site2.ctempurl.com/Images/see_Electric_ight_1_white_bkgnd.png").into(iv);
            }


        }

        return convertView;
    }

    public void deleteDevice(){
        try {
            System.out.println(param);
            if(mySQLHelper.deletePlugData(mac)) {
                System.out.println("Data removed successfully " + mac);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            udp.sendResetCommand(selectedIP);
                            http.removeDevice(param, mac);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            getData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void gotoM1(int position){
        this.globlaPosition = position;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent("adapter_onClick");
                i.putExtra("start", 0);
                i.putExtra("name", SmartPlugsList.get(globlaPosition).getName());
                act.sendBroadcast(i);
            }
        }).start();

    }

    public void getData(){
        SmartPlugsList.clear();
        SmartPlugsList = getSmartPlugsList();
        notifyDataSetChanged();
    }


    public List<JSmartPlug> getSmartPlugsList (){

        List<JSmartPlug> list = new ArrayList<JSmartPlug>();

        Cursor c = mySQLHelper.getPlugData();

        int background = 0;

        if(c.getCount() > 0){
            c.moveToFirst();
            for (int i = 0; i < c.getCount(); i++){
                JSmartPlug sm = new JSmartPlug();
                sm.setDbid(c.getInt(0));
                sm.setId(c.getString(2));
                sm.setName(c.getString(1));
                sm.setGivenName(c.getString(21));
                sm.setIp(c.getString(3));
                sm.setRelay(c.getInt(12));
                sm.setIcon(c.getString(17));
                sm.setSnooze(c.getInt(22));
                sm.setHall_sensor(c.getInt(13));
                sm.setCo_sensor(c.getInt(14));
                if(background > 3){
                    background = 0;
                }
                sm.setBackground(background);
                background++;
                list.add(sm);
                c.moveToNext();
            }
        }

        if(!c.isClosed()){
            c.close();
        }

        return list;
    }

    private boolean deviceHasAlarm(String id){
        boolean toReturn = false;
        Cursor c = mySQLHelper.getAlarmData(id);
        if(c.getCount()>0){
            toReturn = true;
        }
        return toReturn;
    }

}
