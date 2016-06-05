package com.jiee.smartplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.Locale;

public class IRCodeMode extends Activity {

    String title;
    TextView subToolbarTitle;
    ImageButton btn_ir;
    int gid;
    MySQLHelper sql;
    Cursor cs;
    ImageButton btn[] = new ImageButton[254];
    ImageButton btn_close[] = new ImageButton[254];
    ImageButton btn_edit;
    RelativeLayout ir_layout[] = new RelativeLayout[254];
    View v[] = new View[254];
    int irGroupCode[] = new int[254];
    TextView txt[] = new TextView[254];
    UDPCommunication con;
    String ip;
    String devid;
    HTTPHelper http;
    ViewGroup layout;
    BroadcastReceiver serverReplied;
    boolean  mIsEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsEditMode = false;

        con = new UDPCommunication(this);
        http = new HTTPHelper(this);
        sql = HTTPHelper.getDB(this);

        setContentView(R.layout.activity_ircode_mode);

        Intent intent = getIntent();
        gid = intent.getIntExtra("groupId", 0);
        ip = intent.getStringExtra("ip");
        devid = intent.getStringExtra("devid");

        layout = (ViewGroup)findViewById(R.id.ir_edit_linearlayout);

        serverReplied = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateView();
            }
        };

        MySQLHelper sql =  HTTPHelper.getDB(this);
        Cursor c = sql.getIRGroupBySID(gid);                              //get all codes per group
        if(c.getCount() > 0){
            c.moveToFirst();
            title = c.getString(1);
        }
        subToolbarTitle = (TextView)findViewById(R.id.sub_toolbar);
        subToolbarTitle.setText(title);

        btn_edit = (ImageButton)findViewById(R.id.btn_ic_new);
        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_ir.setVisibility(View.VISIBLE);

                mIsEditMode = !mIsEditMode;

                for( View btn : btn_close ) {
                    if( btn!=null ) {
                        btn.setVisibility( mIsEditMode?View.VISIBLE:View.GONE );
                    }
                }

            }
        });

        btn_ir = (ImageButton)findViewById(R.id.btn_add_ir);
        btn_ir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ip != null && !ip.isEmpty()) {
                    Intent i = new Intent(IRCodeMode.this, R5_Custom.class);
                    i.putExtra("ip", ip);
                    i.putExtra("gid", gid);
                    i.putExtra("devid", devid);
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(IRCodeMode.this, R.string.msg_deskLampBtn, Toast.LENGTH_LONG).show();
                }
            }
        });

        updateView();
    }

    public void updateView(){
        layout.removeAllViews();
        layout.addView(btn_ir);



        cs = sql.getIRCodesByGroup(gid);
        int j = 0;
        if(cs.getCount() > 0){
            cs.moveToFirst();
            for(int i = 0; i < cs.getCount(); i++){
                v[i] = getLayoutInflater().inflate(R.layout.ir_buttons, layout, false);
                irGroupCode[i] = cs.getInt(1);
                btn[i] = (ImageButton)v[i].findViewById(R.id.btn_ir);
                btn[i].setOnClickListener(new IRGOOnClickListener(cs.getInt(3)));
                btn[i].setOnTouchListener(new IROnTouchListener(i, j));
                if(cs.getString(4) != null && !cs.getString(4).isEmpty()) {
                    Picasso.with(IRCodeMode.this).load(cs.getString(4)).into(btn[i]);
                } else {
                    String filePath = "http://rgbetanco.com/jiEE/icons/btn_power_pressed.png";
                    Picasso.with(IRCodeMode.this).load(filePath).into(btn[i]);
                }
                btn_close[i] = (ImageButton)v[i].findViewById(R.id.ir_close);
                btn_close[i].setOnClickListener(new IRDELOnClickListener(cs.getInt(9)));
                btn_close[i].setVisibility(View.GONE);
                txt[i] = (TextView)v[i].findViewById(R.id.txt_ir_edit_mode);
                txt[i].setText(cs.getString(2));
                ir_layout[i] = (RelativeLayout)v[i].findViewById(R.id.btn_ir_layout_background);
                switch (j) {
                    case 0:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_co);
                        break;
                    case 1:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_ir);
                        break;
                    case 2:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_nightlight);
                        break;
                    case 3:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_outlet);
                        j = -1;
                        break;
                }

                ir_layout[i].setOnClickListener(new IRGOOnClickListener(cs.getInt(3)));
                ir_layout[i].setOnTouchListener(new IROnTouchListener(i, j));

                j++;

                layout.addView(v[i]);
                cs.moveToNext();
            }
        } else {
            btn_edit.setVisibility(View.GONE);
        }
    }

    public class IROnTouchListener implements View.OnTouchListener
    {
        int i;
        int j;
        public IROnTouchListener(int index, int background){
            this.i = index;
            this.j = background;
        }
        @Override
        public boolean onTouch(View v, MotionEvent event){
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                ir_layout[i].setBackground(getResources().getDrawable(R.drawable.round_corners_btn_m1_nightlight_pressed));
            } else {
                switch (j) {
                    case 0:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_co);
                        break;
                    case 1:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_ir);
                        break;
                    case 2:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_nightlight);
                        break;
                    case 3:
                        ir_layout[i].setBackgroundResource(R.drawable.round_corners_btn_m1_outlet);
                        j = -1;
                        break;
                }
            }
            return false;
        }
    }

    public class IRGOOnClickListener implements View.OnClickListener
    {
        int filename;
        public IRGOOnClickListener(int filename) {
            this.filename = filename;
        }
        @Override
        public void onClick(View v)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String token = "";
                    Cursor c = sql.getToken();
                    if(c.getCount()>0){
                        c.moveToFirst();
                        token = c.getString(1);
                    }
                    if(token != null && !token.isEmpty()) {
                        String param = "devctrl?token=" + token + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + devid+"&send=0";
                        try {
                            http.setDeviceStatus(param, (byte) filename, GlobalVariables.IR_SERVICE);
                        } catch (Exception e) {
                            Log.i("HTTP", "SEND IR CODE EXCEPTION");
                        }
                    }
                }
            }).start();

            Toast.makeText(IRCodeMode.this, getApplicationContext().getString(R.string.processing_ir_command), Toast.LENGTH_SHORT).show();
            //con.sendIRFileName( devid, filename);
        }

    }

    public class IRDELOnClickListener implements View.OnClickListener
    {
        int i;
        public IRDELOnClickListener(int index) {
            this.i = index;
        }
        @Override
        public void onClick(View v) {
            if (i > -1) {

            /*
            int groupId = 0;
            Cursor cu = sql.getIRGroupBySID(gid);
            if(cu.getCount()>0){
                cu.moveToFirst();
                groupId = cu.getInt(4);
            }
            cu.close();
            sql.deleteIRCodesBySID(groupId);
            */
                sql.deleteIRCode(i);
                updateView();

            }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String token = "";
                        Cursor c = sql.getToken();
                        if (c.getCount() > 0) {
                            c.moveToFirst();
                            token = c.getString(1);
                        }
                        int groupId = 0;
                        c.close();
                        Cursor cu = sql.getIRGroupBySID(gid);
                        if (cu.getCount() > 0) {
                            cu.moveToFirst();
                            groupId = cu.getInt(4);
                        }
                        cu.close();
                        int iconId = 0;

                        String action = "del";
                        String type = "button";
                        if (token != null && !token.isEmpty()) {
                            http.manageIRButton(devid, GlobalVariables.IR_SERVICE, type, action, groupId, i, "", iconId, 0, 0, false);
                        }
                        Intent i = new Intent("serverReplied");
                        sendBroadcast(i);
                    }
                }).start();
            }
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateView();
        registerReceiver(serverReplied, new IntentFilter("serverReplied"));
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(serverReplied);
    }
}
