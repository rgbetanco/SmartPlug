package com.jiee.smartplug;

/* TO DO:
 *
/*
 NewDeviceList.cpp
 Author: Chinsoft Ltd. | www.chinsoft.com
 NewDeviceList is an activity class that allows the user to add new devices and/or provide a authentication information to the new device.

 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.services.mDNSTesting;
import com.jiee.smartplug.services.mDNSservice;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;
import com.jiee.smartplug.utils.UDPCommunication;

import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewDeviceList extends AppCompatActivity {

    public static String ip;
    String id;
    Button btn_refresh;
    public ArrayAdapter<String> adapter;
    ArrayList<String> list = new ArrayList<String>();
    String[] values = new String[] { "Searching... please wait"};
    ArrayList<JSmartPlug> plugs = new ArrayList<JSmartPlug>();
    MySQLHelper mySQLHelper;
    Context context = this;
    BroadcastReceiver receiver;
    BroadcastReceiver new_device_receiver;
    BroadcastReceiver device_removed_receiver;
    BroadcastReceiver stop_searching_receiver;
    BroadcastReceiver device_info;
    BroadcastReceiver smartconfig_stopped;
    ProgressBar HeaderProgress;
    UDPCommunication con;
    List<JSmartPlug> items;
    JSmartPlug jsplug;
    Activity c = this;
    //mDNSservice mDNS;
    Intent i;
    NetworkUtil networkUtil;
    Handler handler;
    Http http;
    HTTPHelper httpHelper;
    String name;
    JSmartPlug plug;
    int current_position;
    boolean runThread = true;
    int delay = 1000; //milliseconds
    Intent j;
    RelativeLayout overlay;
    int globalposition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_device_list);

        con = new UDPCommunication(this);
        http = new Http();
        httpHelper = new HTTPHelper(this);
        mySQLHelper = HTTPHelper.getDB(this);
        networkUtil = new NetworkUtil();

        overlay = (RelativeLayout)findViewById(R.id.overlay);
        int opacity = 200; // from 0 to 255
        overlay.setBackgroundColor(opacity * 0x1000000); // black with a variable alpha
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        overlay.setLayoutParams(params);
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.INVISIBLE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        update_plugs();

        j = new Intent(this, mDNSTesting.class);

        TextView toolbar_text = (TextView) findViewById(R.id.sub_toolbar_yellow);
        toolbar_text.setText(getApplicationContext().getString(R.string.title_addDevice));

        ImageButton btn_settings = (ImageButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.GONE);

        HeaderProgress = (ProgressBar) findViewById(R.id.pbHeaderProgress);
        HeaderProgress.setVisibility(View.VISIBLE);

        btn_refresh = (Button) findViewById(R.id.btn_refresh_list);
        btn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //call the dialog
                if (!NetworkUtil.getWifiName(context).toString().equals("")) {
                    SmartConfigDialog smd = new SmartConfigDialog(c);
                    smd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    smd.show();
                } else {
                    Toast.makeText(context, "Please connect to a WiFi first", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //this would notify the activity when the smartconfig is done running
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                //HeaderProgress.setVisibility(View.GONE);

            }
        };

        smartconfig_stopped = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopService(j);
                startService(j);
            }
        };

        device_info = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ip = intent.getStringExtra("ip");
                id = intent.getStringExtra("id");

                    for (int i = 0; i < plugs.size(); i++) {
                        if(plugs.get(i).getIp()!=null && !plugs.get(i).getIp().isEmpty()&& ip!=null && !ip.isEmpty()) {
                            if (plugs.get(i).getIp().equals(ip)) {
                                plugs.get(i).setId(UDPListenerService.js.getId());
                                plugs.get(i).setModel(UDPListenerService.js.getModel());
                                plugs.get(i).setBuildno(UDPListenerService.js.getBuildno());
                                plugs.get(i).setProt_ver(UDPListenerService.js.getProt_ver());
                                plugs.get(i).setHw_ver(UDPListenerService.js.getHw_ver());
                                plugs.get(i).setFw_ver(UDPListenerService.js.getFw_ver());
                                plugs.get(i).setFw_date(UDPListenerService.js.getFw_date());
                                plugs.get(i).setFlag(UDPListenerService.js.getFlag());
                                System.out.println("DEVICE INFO RECEIVED IP: " + ip + " ID: " + plugs.get(i).getId() + " POSITION: " + i + " NAME:" + plugs.get(i).getName());
                            } else {
                                System.out.println("NOT UPDATING - DEVICE INFO RECEIVED IP: " + ip + " ID: " + id + " POSITION: " + i + " NAME:" + plugs.get(i).getName() + " Plug IP: " + plugs.get(i).getIp());
                            }
                        }
                    }
            }
        };

        device_removed_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
           //     update_list_new();
            }
        };

        new_device_receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                name = i.getStringExtra("name");
                ip = i.getStringExtra("ip");
                Log.i("NewDeviceList", "New Device Received "+name+", IP "+ip);
                update_list_new(name, ip);
            //    new Thread(new Runnable() {
            //        @Override
            //        public void run() {
                        short command = 0x0001;
                        con.queryDevices(ip, command);
            //        }
            //    }).start();
            }
        };

        final ListView listview = (ListView) findViewById(R.id.devicesfound);

        for (int ii = 0; ii < values.length; ++ii) {
            list.add(values[ii]);
        }
        adapter = new ArrayAdapter<String>(this, R.layout.custome_list_view, list);
        listview.setAdapter(adapter);
        adapter.clear();
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                globalposition = position;
                grayOutView();
                System.out.println("NAME: " + plugs.get(position).getName() + " - ID: " + plugs.get(position).getId()+" - POSITION: "+position);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Miscellaneous misc = new Miscellaneous();
                        for(int i = 0; i < plugs.size(); i++){

                            if(plugs.get(i).getName().equals(adapter.getItem(globalposition).toString())){
                                if(plugs.get(i).getId() != null && !plugs.get(i).getId().isEmpty()){
                                    mySQLHelper.insertPlug(plugs.get(i), 1);

                                    System.out.println("Right! - Plug id: " + plugs.get(i).getId() + ", Plug Ip: " + plugs.get(i).getIp());
                                    String param = "actdev?token="+misc.getToken(NewDeviceList.this)+"&hl="+Locale.getDefault().getLanguage()
                                            + "&devid="+plugs.get(i).getId()+"&title="+plugs.get(i).getName()+"&model="+plugs.get(i).getModel()
                                            + "&buildnumber="+plugs.get(i).getBuildno()+"&protocol="+plugs.get(i).getProt_ver()
                                            + "&hardware="+plugs.get(i).getHw_ver()+"&firmware="+plugs.get(i).getFw_ver()
                                            + "&firmwaredate="+plugs.get(i).getFw_date()+"&send=0";
                                    System.out.println(param);
                                    try {
                                        System.out.println("ACTIVATING DEVICE ON SERVER - ID:" + plugs.get(i).getId() + " IP:" + plugs.get(i).getIp() + " NAME:"+plugs.get(i).getName());
                                        if(!httpHelper.sendDeviceActivationKey(param, plugs.get(i).getIp(), plugs.get(i).getId(), plugs.get(i).getName())){
                                            System.out.println("Error adding new device");
                                     //       Toast.makeText(NewDeviceList.this, getApplicationContext().getString(R.string.error_adding_device), Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    break;
                                } else {
                                    System.out.println("Wrong! - Plug id: " + plugs.get(i).getId() + ", Plug Ip: " + plugs.get(i).getIp());
                            //        Toast.makeText(NewDeviceList.this, "Not Ready, Please try again in a second", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                System.out.println("NO PLUGS FOUND WHEN YOU CLICK");
                            //    Toast.makeText(NewDeviceList.this, getApplicationContext().getString(R.string.error_adding_new_device), Toast.LENGTH_SHORT).show();
                                short command = 0x0001;
                                con.queryDevices(ip, command);
                            //    removeGrayOutView();
                        }

                }
                    }
                }).start();

                Intent ix = new Intent(context, ListDevices.class);
                startActivity(ix);
                finish();

            }

        });

    }

    public void update_plugs(){
        plugs.clear();
        System.out.println("All Plugs removed");
        Cursor c = mySQLHelper.getPlugData();
        if(c.getCount()>0){
            c.moveToFirst();
            for(int i=0; i<c.getCount();i++){
                JSmartPlug jSmartPlug = new JSmartPlug();
                jSmartPlug.setName(c.getString(1));
                jSmartPlug.setIp(c.getString(3));
                plugs.add(jSmartPlug);
                c.moveToNext();
            }
        }
        c.close();
    }

    public void update_list_new(String name, String ip){

        //check if the name is not already in the database
        boolean plugExist = false;
        Cursor c = mySQLHelper.getPlugDataByName(name);
        if(c.getCount() > 0){
            plugExist = true;
        } else {

            if (name != null && !name.isEmpty() && ip != null && !ip.isEmpty()) {
                if (plugs.size() > 0) {
                    //check if the name is not already in the plugs array
                    for (int i = 0; i < plugs.size(); i++) {
                        if (plugs.get(i).getName().equals(name)) {
                            plugExist = true;
                        } else {
                            JSmartPlug jSmartPlug = new JSmartPlug();
                            jSmartPlug.setName(name);
                            jSmartPlug.setIp(ip);
                            System.out.println("Plug Added, Name: " + jSmartPlug.getName());
                            plugs.add(jSmartPlug);

                            adapter.add(name);
                            adapter.notifyDataSetChanged();
                        }
                    }
                } else {

                    JSmartPlug jSmartPlug = new JSmartPlug();
                    jSmartPlug.setName(name);
                    jSmartPlug.setIp(ip);
                    System.out.println("Plug Added, Name: " + jSmartPlug.getName());
                    plugs.add(jSmartPlug);
                }

                if (!plugExist) {
                    adapter.add(name);
                    adapter.notifyDataSetChanged();
                }
            }
        }

        c.close();

    }

    @Override
    protected void onResume(){
        super.onResume();
    //    if(networkUtil.getConnectionStatus(this) == 1) {
            update_plugs();
            registerReceiver(receiver, new IntentFilter("smartconfig"));
            registerReceiver(new_device_receiver, new IntentFilter("mDNS_New_Device_Found"));
            registerReceiver(device_removed_receiver, new IntentFilter("mDNS_Device_Removed"));
    //        registerReceiver(stop_searching_receiver, new IntentFilter("stopsearching"));
            registerReceiver(device_info, new IntentFilter("device_info"));
            registerReceiver(smartconfig_stopped, new IntentFilter("smartconfig_stopped"));
    //        update_list_new();
     //   }

        startService(j);

    }

    @Override
    protected void onPause(){
        super.onPause();
    //    if(networkUtil.getConnectionStatus(this) == 1) {
            try {
                unregisterReceiver(receiver);
                unregisterReceiver(new_device_receiver);
                unregisterReceiver(device_removed_receiver);
        //        unregisterReceiver(stop_searching_receiver);
                unregisterReceiver(device_info);
                unregisterReceiver(smartconfig_stopped);
            } catch (Exception e){
                e.printStackTrace();
            }
    //    }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try {
            stopService(j);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void grayOutView(){
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.VISIBLE);
    }

    public void removeGrayOutView(){
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.INVISIBLE);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent m = new Intent(this, ListDevices.class);
        startActivity(m);
        finish();
        return true;
    }

}
