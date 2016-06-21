package com.jiee.smartplug;

/* TO DO: DELETE THIS CLASS IF NOT USED IN THE FUTURE.
 *
/*
 DetectIR.java
 Author: Chinsoft Ltd. | www.chinsoft.com

 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.services.UDPListenerService;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

public class DetectIR extends Activity {

    boolean keeprunning;
    Context context = this;
    BroadcastReceiver brec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_ir);

        brec = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int name = intent.getIntExtra("filename", 0);
                MySQLHelper sql = HTTPHelper.getDB(context);
                //sql.insertIRCodes(name, M1.mac);
                //sql.close();
                //Intent i = new Intent(context, IREditMode.class);
                //startActivity(i);
                //finish();
            }
        };
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(brec, new IntentFilter("ir_filename"));
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(brec);
        keeprunning = false;
    }
}
