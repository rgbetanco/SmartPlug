package com.jiee.smartplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;
import com.jiee.smartplug.utils.UDPCommunication;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

public class M2A_Item_Settings extends Activity {

    MySQLHelper sql;

    String icon;
    String name;
    String wifi;
    int notify_on_power_outage = 0;
    int notify_on_co_warning = 0;
    int notify_on_timer_activated = 0;

    Button save;
    Button toolbar_title;
    ImageButton btn_icon;
    TextView txt_jsname;
    TextView txt_wifi;
    TextView txt_mac;
    CheckBox cbx_power;
    CheckBox cbx_co;
    CheckBox cbx_timer;
    TextView txt_remove;
    //TextView txt_program;
    CheckBox cbx_cosensor;
    TextView txt_hardware;
    TextView txt_firmware;
    TextView txt_ota;
    boolean inrange;
    //LinearLayout l1;
    LinearLayout l2;
    LinearLayout l3;
    LinearLayout l4;
    LinearLayout l5;
    LinearLayout l6;
    LinearLayout l7;
    HTTPHelper http;
    UDPCommunication udp;
    NetworkUtil networkUtil;
    RelativeLayout overlay;

    BroadcastReceiver ota_sent;
    BroadcastReceiver ota_finished;
    BroadcastReceiver device_info;
    UDPCommunication con;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_m2a_item_settings);

        wifi = NetworkUtil.getWifiName(this);
        http = new HTTPHelper(this);
        sql = HTTPHelper.getDB(this);
        udp = new UDPCommunication();
        networkUtil = new NetworkUtil();
        con = new UDPCommunication();

        overlay = (RelativeLayout)findViewById(R.id.overlay);
        int opacity = 200; // from 0 to 255
        overlay.setBackgroundColor(opacity * 0x1000000); // black with a variable alpha
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        overlay.setLayoutParams(params);
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.GONE);

        ota_sent = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                grayOutView();
            }
        };

        ota_finished = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int count = 10;
                while(count > 0){
                    count--;
                    SystemClock.sleep(1000);
                }
                short command = 0x0001;
                con.queryDevices(M1.ip, command, M1.mac);
                removeGrayOutView();
            }
        };

        device_info = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                System.out.println("DEVICE INFO BROADCAST RECEIVED");

                sql.updateDeviceVersions(M1.mac, UDPListenerService.js.getModel(), UDPListenerService.js.getBuildno(),
                        UDPListenerService.js.getProt_ver(), UDPListenerService.js.getHw_ver(), UDPListenerService.js.getFw_ver(), UDPListenerService.js.getFw_date());

                Cursor c = sql.getPlugDataByID(M1.mac);
                if(c.getCount() > 0){
                    c.moveToFirst();
                    final String model = c.getString(5);
                    final int buildnumber = c.getInt(6);
                    final int protocol = c.getInt(7);
                    final String hardware = c.getString(8);
                    final String firmware = c.getString(9);
                    final int firmwaredate = c.getInt(10);

                    txt_hardware.setText(hardware);
                    txt_firmware.setText(firmware);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String param = "devset?token="+ Miscellaneous.getToken(M2A_Item_Settings.this)+"&hl="
                                    + Locale.getDefault().getLanguage()+"&devid="
                                    + M1.mac +"&model="+model+ "&buildnumber="+buildnumber+"&protocol="+protocol
                                    + "&hardware="+hardware+"&firmware="+firmware
                                    + "&firmwaredate="+firmwaredate+"&send=1";
                            try {
                                System.out.println(param);
                                http.setDeviceSettings(param);
                            } catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                c.close();
            }
        };

        toolbar_title = (Button) findViewById(R.id.toolbar_title);
        btn_icon = (ImageButton) findViewById(R.id.js_icon);
        txt_jsname = (EditText) findViewById(R.id.txt_jsname);
        txt_wifi = (TextView) findViewById(R.id.txt_wifi);
        txt_mac = (TextView) findViewById(R.id.txt_mac);
     //   cbx_power = (CheckBox) findViewById(R.id.cbx_power);
     //   cbx_co = (CheckBox) findViewById(R.id.cbx_co);
     //   cbx_timer = (CheckBox) findViewById(R.id.cbx_timer);
        txt_remove = (TextView) findViewById(R.id.txt_remove);
        save = (Button) findViewById(R.id.btn_settings);
        //txt_program = (TextView) findViewById(R.id.txt_program);
        cbx_cosensor = (CheckBox) findViewById(R.id.cbx_cosensor);
        txt_hardware = (TextView) findViewById(R.id.id_hardware);
        txt_firmware = (TextView) findViewById(R.id.id_firmware);
        txt_ota = (TextView) findViewById(R.id.txt_ota);
        //l1 = (LinearLayout) findViewById(R.id.layout_irtransmitter);
        l2 = (LinearLayout) findViewById(R.id.layout_idsensor);
        l3 = (LinearLayout) findViewById(R.id.layout_hardware);
        l4 = (LinearLayout) findViewById(R.id.layout_firmware);
     //   l5 = (LinearLayout) findViewById(R.id.layout_notify_power);
     //   l6 = (LinearLayout) findViewById(R.id.layout_notify_co);
     //   l7 = (LinearLayout) findViewById(R.id.layout_notify_timer);
        System.out.println("MAC:" + M1.mac);
        Cursor c = sql.getPlugDataByID(M1.mac);
        if (c.getCount() > 0) {
            c.moveToFirst();
            try {
                icon = c.getString(17).toString();
            } catch (Exception e) {
                Log.d("NULL", "ICON FIELD IS NULL");
            }
            try {
                if(c.getString(21)!=null && !c.getString(21).isEmpty()) {
                    name = c.getString(21).toString();
                } else {
                    if(c.getString(1)!=null && !c.getString(1).isEmpty()) {
                        name = c.getString(1).toString();
                    } else {
                        name = "unknown";
                    }
                }
            } catch (Exception e) {
                Log.d("NULL", "NAME FIELD IS NULL");
            }
            notify_on_power_outage = c.getInt(18);
            notify_on_co_warning = c.getInt(19);
            notify_on_timer_activated = c.getInt(20);
            if (c.getInt(14) == 1) {
                cbx_cosensor.setChecked(true);
            } else {
                cbx_cosensor.setChecked(false);
            }
            txt_jsname.setHint(name);
            toolbar_title.setText(name);
            if(c.getString(8) != null && !c.getString(8).isEmpty()) {
                txt_hardware.setText(c.getString(8).toString());
            }
            if(c.getString(9) != null && !c.getString(9).isEmpty()) {
                txt_firmware.setText(c.getString(9).toString());
            }

            if (M1.ip != null && !M1.ip.isEmpty()) {
                System.out.println("IP IS NOT NULL :" + M1.ip);
                inrange = true;
                txt_jsname.setEnabled(true);
                btn_icon.setEnabled(true);
                //l1.setVisibility(View.VISIBLE);
                l2.setVisibility(View.VISIBLE);
                l3.setVisibility(View.VISIBLE);
                l4.setVisibility(View.VISIBLE);
                //l5.setVisibility(View.GONE);
                //l6.setVisibility(View.GONE);
                //l7.setVisibility(View.GONE);
            } else {
                System.out.println("IP iS NULL ");
                inrange = false;
                txt_jsname.setEnabled(false);
                btn_icon.setEnabled(false);
                save.setVisibility(View.GONE);
                //l1.setVisibility(View.GONE);
                l2.setVisibility(View.GONE);
                l3.setVisibility(View.GONE);
                l4.setVisibility(View.GONE);
                //l5.setVisibility(View.VISIBLE);
                //l6.setVisibility(View.VISIBLE);
                //l7.setVisibility(View.VISIBLE);
            }

            c.close();
            if (icon != null && !icon.isEmpty()) {
                Picasso.with(this).load(icon).into(btn_icon);
            }
            txt_jsname.setText(name);
            if(wifi!=null && !wifi.isEmpty()) {
                txt_wifi.setText(wifi);
            } else {
                txt_wifi.setText(R.string.connect_to_wifi);
            }
            /*
            if (notify_on_power_outage == 1) {
                cbx_power.setChecked(true);
            } else {
                cbx_power.setChecked(false);
            }
            if (notify_on_co_warning == 1) {
                cbx_co.setChecked(true);
            } else {
                cbx_co.setChecked(false);
            }
            if (notify_on_timer_activated == 1) {
                cbx_timer.setChecked(true);
            } else {
                cbx_timer.setChecked(false);
            }
            */

            if (M1.mac != null) {
                String temp_mac = M1.mac.substring(0, 2).toUpperCase() + ":" + M1.mac.substring(2, 4).toUpperCase() + ":" + M1.mac.substring(4, 6).toUpperCase() + ":" + M1.mac.substring(6, 8).toUpperCase() + ":" + M1.mac.substring(8, 10).toUpperCase() + ":" + M1.mac.substring(10, 12).toUpperCase();
                txt_mac.setText(temp_mac);
            }

            //btn_icon call the gallery to select icon
            btn_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(M2A_Item_Settings.this, IconPicker.class);
                    startActivityForResult(intent, R2_EditItem.SELECT_ICON_CODE);
                }
            });

            txt_remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final YesNoDialog cd = new YesNoDialog(M2A_Item_Settings.this, 3);   // 1 = logout, 2 = M1 : device_id : service_id
                    cd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    cd.show();
                }
            });

            txt_ota.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(M1.ip != null && !M1.ip.isEmpty()){
                        if(networkUtil.getConnectionStatus(M2A_Item_Settings.this) == 1) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    udp.sendOTACommand(M1.ip);
                                }
                            }).start();

                            Toast.makeText(M2A_Item_Settings.this, getApplicationContext().getString(R.string.please_wait), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(M2A_Item_Settings.this, getApplicationContext().getString(R.string.no_udp_Connection), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(M2A_Item_Settings.this, getApplicationContext().getString(R.string.ip_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    grayOutView();

                    /*
                    if (cbx_power.isChecked()) {
                        notify_on_power_outage = 1;
                    } else {
                        notify_on_power_outage = 0;
                    }
                    if (cbx_co.isChecked()) {
                        notify_on_co_warning = 1;
                    } else {
                        notify_on_co_warning = 0;
                    }
                    if (cbx_timer.isChecked()) {
                        notify_on_timer_activated = 1;
                    } else {
                        notify_on_timer_activated = 0;
                    }
                    */

                    name = txt_jsname.getText().toString();
                    if (M1.mac != null && inrange) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                if (sql.updatePlugNameNotify(M1.mac, name, notify_on_power_outage, notify_on_co_warning, notify_on_timer_activated, icon)) {
                                    Cursor cursor = sql.getIconByUrl(icon);
                                    String iconId = "";
                                    if(cursor.getCount() > 0){
                                        cursor.moveToFirst();
                                        iconId = cursor.getString(1);
                                    }
                                    String param = "devset?token="+ Miscellaneous.getToken(M2A_Item_Settings.this)+"&hl="+ Locale.getDefault().getLanguage()+"&devid="+M1.mac+"&icon="+iconId+"&title="+name+"&notify_power="+notify_on_power_outage+"&notify_timer="+notify_on_timer_activated+"&notify_danger="+notify_on_co_warning+"&send=1";
                                    System.out.println("DEBUGING: "+param);
                                    try {
                                        http.setDeviceSettings(param);
                                    } catch(Exception e){
                                        e.printStackTrace();
                                    }
                                    System.out.println("DB UPDATED SUCCESSFULLY");
                                } else {
                                    System.out.println("CHECK IF MAC ADDRESS IS NULL");
                                }

                            }
                        }).start();

                    }

                    finish();
                    removeGrayOutView();

                }
            });

            /*
            txt_program.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(M2A_Item_Settings.this, IREditMode.class);
                    startActivity(i);
                }
            });
            */
        }
        txt_wifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkUtil net = new NetworkUtil();
                int connectedto = net.getConnectionStatus(M2A_Item_Settings.this);
                if(connectedto == 0 || connectedto == 2){
                    startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent urlReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, urlReturnedIntent);
        Drawable iconLocal;
        switch(requestCode) {
            case R2_EditItem.SELECT_ICON_CODE:
                try {
                    if (urlReturnedIntent.getIntExtra("custom", 0) != 1){
                        InputStream is;
                        try {
                            is = this.getContentResolver().openInputStream(Uri.parse(urlReturnedIntent.getStringExtra("url")));
                            BitmapFactory.Options options=new BitmapFactory.Options();
                            options.inSampleSize = 10;
                            Bitmap preview_bitmap= BitmapFactory.decodeStream(is, null, options);

                            iconLocal = new BitmapDrawable(getResources(),preview_bitmap);

                        } catch (FileNotFoundException e) {
                            iconLocal = getResources().getDrawable(R.drawable.lamp);
                        }
                        btn_icon.setBackground(iconLocal);

                    } else {
                        btn_icon.setBackground(null);
                        icon = urlReturnedIntent.getStringExtra("url");
                        Uri Uricon = Uri.parse(icon);
                        Picasso.with(M2A_Item_Settings.this).load(Uricon).into(btn_icon);
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
        }
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        wifi = NetworkUtil.getWifiName(this);
        if(wifi!=null && !wifi.isEmpty()) {
            txt_wifi.setText(wifi);
        } else {
            txt_wifi.setText(R.string.connect_to_wifi);
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();            //FORCE THE CACHE DATA TO BE SAVED ON DISK
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(ota_sent, new IntentFilter("ota_sent"));
        registerReceiver(ota_finished, new IntentFilter("ota_finished"));
        registerReceiver(device_info, new IntentFilter("device_info"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                short command = 0x0001;
                con.queryDevices(M1.ip, command, M1.mac);
            }
        }).start();
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(ota_sent);
        unregisterReceiver(ota_finished);
        unregisterReceiver(device_info);
    }

    public void grayOutView(){
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.VISIBLE);
    }

    public void removeGrayOutView(){
        overlay.invalidate(); // update the view
        overlay.setVisibility(View.INVISIBLE);
    }
}
