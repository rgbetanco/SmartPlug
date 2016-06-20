package com.jiee.smartplug;

/* TO DO: DELETE THIS CLASS, IT IS NOT BEING USED
 *
*/


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.jiee.smartplug.utils.NetworkUtil;

public class AddDevice extends Activity {

    Activity context = this;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        TextView toolbar_text = (TextView) findViewById(R.id.sub_toolbar_yellow);
        toolbar_text.setText("Add Device");

        Button btn_startsmartconfig = (Button)findViewById(R.id.btn_init_smartconfig);
        final Button btn_deviceList = (Button)findViewById(R.id.btn_device_list);

        btn_startsmartconfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("WiFi Name: " + NetworkUtil.getWifiName(context));
                btn_deviceList.setVisibility(View.GONE);
                if (!NetworkUtil.getWifiName(context).toString().equals("")) {
                    SmartConfigDialog smd = new SmartConfigDialog(context);
                    smd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    smd.show();
                } else {
                    Toast.makeText(context, "Please connect to a WiFi first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context c, Intent i ){
                btn_deviceList.setVisibility(View.VISIBLE);
            }
        };

        btn_deviceList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, NewDeviceList.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume(){
        registerReceiver(receiver, new IntentFilter("smartconfig"));
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(receiver);
    }
}