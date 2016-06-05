package com.jiee.smartplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

import java.util.Locale;

public class R6_Record_IR extends Activity {
    int gid;
    String icon;
    String name;
    int ir_filename;                         //NAME OF THE FILE ON THE SMARTPLUG
    String ip;

    ImageButton btn_setting;
    Button btn_recordAgain;
    Button btn_testCommand;
    Button btn_addNew;
    ProgressBar pb;
    BroadcastReceiver brec;
    UDPCommunication con = new UDPCommunication();
    MySQLHelper sql;
    HTTPHelper http;
    Boolean customeIcon = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sql = HTTPHelper.getDB(this);
        http = new HTTPHelper(this);

        setContentView(R.layout.activity_r6__record__ir);

        Intent i = getIntent();
        name = i.getStringExtra("txt_name");                            //DISPLAY NAME
        gid = i.getIntExtra("groupId", 0);                              //GROUP ID
        icon = i.getStringExtra("icon");                                //URL PATH TO ICON
        customeIcon = i.getBooleanExtra("customeIcon", false);
        ip = i.getStringExtra("ip");

        sendIRRecordCommand();

        pb = (ProgressBar)findViewById(R.id.pbProgress);
        pb.setVisibility(View.VISIBLE);

        btn_setting = (ImageButton)findViewById(R.id.btn_settings);
        btn_setting.setVisibility(View.GONE);

        btn_recordAgain = (Button)findViewById(R.id.btn_recordAgain);
        btn_recordAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pb.setVisibility(View.VISIBLE);
                sendIRRecordCommand();
            }
        });

        btn_testCommand = (Button)findViewById(R.id.btn_testCommand);
        btn_testCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        con.sendIRFileName(ir_filename);
                    }
                }).start();

            }
        });

        btn_addNew = (Button)findViewById(R.id.btn_addnew);
        btn_addNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            sql.insertIRCodes(gid, name, ir_filename, icon, M1.mac, -1);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String token = "";
                    Cursor c = sql.getToken();
                    if(c.getCount()>0){
                        c.moveToFirst();
                        token = c.getString(1);
                    }
                    int groupId = 0;
                    c.close();
                    Cursor cu = sql.getIRGroupBySID(gid);
                    if(cu.getCount()>0){
                        cu.moveToFirst();
                        groupId = cu.getInt(4);
                    }
                    cu.close();
                    int iconId = 0;
                    Cursor cur = sql.getIconByUrl(icon);
                    if(cur.getCount()>0){
                        cur.moveToFirst();
                        iconId = cur.getInt(1);
                    }
                    String action = "add";
                    String type = "button";
                    if(token != null && !token.isEmpty()) {
                        if(!customeIcon) {
                            http.manageIRButton(M1.mac, GlobalVariables.IR_SERVICE, type, action, groupId, 0, name, iconId, ir_filename, Miscellaneous.getResolution(R6_Record_IR.this), customeIcon);
                        } else {
                            http.manageIRButton(M1.mac, GlobalVariables.IR_SERVICE, type, action, groupId, 0, name, 0, ir_filename, Miscellaneous.getResolution(R6_Record_IR.this), customeIcon);
                        }
                    }
                    Intent i = new Intent("serverReplied");
                    sendBroadcast(i);
                }
            }).start();

       //     Intent i = new Intent(R6_Record_IR.this, IREditMode.class);
       //     startActivity(i);
            finish();
            }
        });

        brec = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ir_filename = intent.getIntExtra("filename", 0);
                if(ir_filename == -1){
                    Toast.makeText(R6_Record_IR.this, getApplicationContext().getString(R.string.ir_timeout), Toast.LENGTH_SHORT).show();
                    btn_testCommand.setEnabled(false);
                    btn_addNew.setEnabled(false);
                } else {
                    btn_testCommand.setEnabled(true);
                    btn_addNew.setEnabled(true);
                }
                pb.setVisibility(View.GONE);
                System.out.println("IR filename: "+ir_filename);
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
    }

    public void sendIRRecordCommand(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                con.sendIRMode(ip);
            }
        }).start();
    }

    public void cancelIRRecordCommand(View view){
        System.out.println("CANCELING IR SCANNING");
        new Thread(new Runnable() {
            @Override
            public void run() {
                con.cancelIRMode();
            }
        }).start();
    }
}
