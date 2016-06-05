package com.jiee.smartplug;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;
import com.squareup.picasso.Picasso;

import java.util.Locale;

//This class add ir groups to the database
public class IREditMode extends Activity {

    Context context = this;
    MySQLHelper sql;
    ImageButton btn[] = new ImageButton[254];
    ImageButton btn_close[] = new ImageButton[254];
    ImageButton btn_edit;
    ImageButton btn_add_ir;
    View v[] = new View[254];
    int irGroupCode[] = new int[254];
    RelativeLayout ir_layout[] = new RelativeLayout[254];
    TextView txt[] = new TextView[254];
    Cursor cs;
    String ip;
    String devid;
    ViewGroup layout;
    String token = "";
    BroadcastReceiver serverReplied;
    BroadcastReceiver gcm_notification;

    boolean mIsEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iredit_mode);

        mIsEditMode = false;

        sql = HTTPHelper.getDB(this);
        btn_edit = (ImageButton)findViewById(R.id.btn_ic_new);

        ip = getIntent().getStringExtra("ip");
        devid = getIntent().getStringExtra("devid");

        Cursor c = sql.getToken();
        if(c.getCount() > 0){
            c.moveToFirst();
            token = c.getString(1);
        }
        c.close();

        serverReplied = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateView();
            }
        };

        gcm_notification = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                getDataFromServer();
                updateView();
            }
        };


        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_add_ir.setVisibility(View.VISIBLE);

                mIsEditMode = !mIsEditMode;

                for( View btn : btn_close ) {
                    if( btn!=null ) {
                        btn.setVisibility( mIsEditMode?View.VISIBLE:View.GONE );
                    }
                }
            }
        });

        btn_add_ir = (ImageButton)findViewById(R.id.btn_add_ir);
        btn_add_ir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, R2_EditItem.class);
                i.putExtra("devid", devid);
                startActivity(i);

                /*
                Intent i = new Intent(context, IRAddNew.class);
                    i.putExtra("ip", ip);
                    i.putExtra("devid", devid);
                    startActivity(i);
                    */
            }
        });

        layout = (ViewGroup)findViewById(R.id.ir_edit_linearlayout);

        TextView toolbar_text = (TextView)findViewById(R.id.sub_toolbar);
        toolbar_text.setText(R.string.title_irControl);

     //   getDataFromServer();
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(serverReplied, new IntentFilter("serverReplied"));
        registerReceiver(gcm_notification, new IntentFilter("gcm_notification"));
        updateView();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(serverReplied);
        unregisterReceiver(gcm_notification);
        cs.close();
    }

    public class IRGOOnClickListener implements View.OnClickListener
    {
        int i;
        String ipx = "";
        public IRGOOnClickListener(int index, String localIp) {
            this.i = index;
            this.ipx = localIp;
        }
        @Override
        public void onClick(View v)
        {
            Intent intent = new Intent(context, IRCodeMode.class);
            intent.putExtra("groupId", i);
            intent.putExtra("ip", this.ipx);
            intent.putExtra("devid", devid);
            startActivity(intent);
        }

    }

    public class IRDELOnClickListener implements View.OnClickListener
    {
        int i;
        String groupName;
        String iconURL;
        int sid = 0;
        public IRDELOnClickListener(int index, String groupName, String iconURL, int sid) {
            this.i = index;
            this.groupName = groupName;
            this.iconURL = iconURL;
            this.sid = sid;
        }
        @Override
        public void onClick(View v) {
            int newIndex=0;
            if (sid > -1) {
                Cursor c = sql.getIRGroups(i);
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    newIndex = c.getInt(4);
                }
                MySQLHelper sql =  HTTPHelper.getDB(IREditMode.this);
                if (sql.deleteIRGroup(i)) {
                    sql.deleteIRCodesBySID(i);
                    System.out.println("Successfully deleted");
                }
                updateView();
            } else {
                Toast.makeText(IREditMode.this, R.string.connection_error, Toast.LENGTH_SHORT).show();
            }

            final String type = "group";
            final String action = "del";

            int iconId = 0;
            Cursor cur = sql.getIconByUrl(iconURL);
            if (cur.getCount() > 0) {
                cur.moveToFirst();
                iconId = cur.getInt(1);
            }
            final int nIndex = newIndex;
            final int nIconId = iconId;
            if (token != null && !token.isEmpty()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HTTPHelper http = new HTTPHelper(IREditMode.this);
                        int res = new Miscellaneous().getResolution(IREditMode.this);
                        http.manageIRGroup( devid, GlobalVariables.IR_SERVICE, type, action, nIndex, groupName, nIconId, res, -1, false);
                        Intent i = new Intent("serverReplied");
                        sendBroadcast(i);
                    }
                }).start();
            }
        }

    }

    public void getDataFromServer(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HTTPHelper http = new HTTPHelper(IREditMode.this);
                Miscellaneous misc = new Miscellaneous();
                http.getServerIR( devid, GlobalVariables.IR_SERVICE, misc.getResolution(IREditMode.this));
                Intent i = new Intent("serverReplied");
                sendBroadcast(i);
            }
        }).start();
    }

    public void updateView(){
        layout.removeAllViews();
        layout.addView(btn_add_ir);

        HTTPHelper http = new HTTPHelper(this);
        if(devid != null && !devid.isEmpty()){
            devid = M1.mac;
        }

        cs = sql.getIRGroups();
        int j = 0;
        if(cs.getCount() > 0){
            cs.moveToFirst();
            for(int i = 0; i < cs.getCount(); i++){
                v[i] = getLayoutInflater().inflate(R.layout.ir_buttons, layout, false);
                irGroupCode[i] = cs.getInt(0);
                btn[i] = (ImageButton)v[i].findViewById(R.id.btn_ir);
                btn[i].setOnClickListener(new IRGOOnClickListener(cs.getInt(4), ip));
                if(cs.getString(2) != null && !cs.getString(2).isEmpty()) {
                    Picasso.with(context).load(cs.getString(2)).into(btn[i]);
                } else {
                    String filePath = "http://rgbetanco.com/jiEE/icons/btn_power_pressed.png";
                    Picasso.with(context).load(filePath).into(btn[i]);
                }
                btn_close[i] = (ImageButton)v[i].findViewById(R.id.ir_close);
                btn_close[i].setOnClickListener(new IRDELOnClickListener(cs.getInt(0), cs.getString(1), cs.getString(2), cs.getInt(4)));
                btn_close[i].setVisibility(View.GONE);
                txt[i] = (TextView)v[i].findViewById(R.id.txt_ir_edit_mode);
                txt[i].setText(cs.getString(1));
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
                j++;
                ir_layout[i].setOnClickListener(new IRGOOnClickListener(cs.getInt(4), ip));
                layout.addView(v[i]);
                cs.moveToNext();
            }
        } else {
            btn_edit.setVisibility(View.GONE);
        }
    }

}
