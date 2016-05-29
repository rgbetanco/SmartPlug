package com.jiee.smartplug;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;
import com.squareup.picasso.Picasso;

import java.io.File;

public class R5_Custom extends Activity {
    ImageButton ir_icon;
    Activity act = this;
    String filePath =  "http://rgbetanco.com/jiEE/icons/btn_power_pressed.png";
    Button save;
    EditText txt;
    int gid = 0;
    String ip;
    NetworkUtil util = new NetworkUtil();
    Boolean customeIcon = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r5_custom);

        Intent i = getIntent();
        gid = i.getIntExtra("gid", 0);
        ip = i.getStringExtra("ip");

        ir_icon = (ImageButton)findViewById(R.id.ir_icon);
        Picasso.with(this).load(filePath).into(ir_icon);
        ir_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(act, IconPicker.class);
                intent.putExtra("activity", "IR_Command");
                startActivityForResult(intent, R2_EditItem.SELECT_ICON_CODE);
            }
        });

        txt = (EditText)findViewById(R.id.txt_name);

        save = (Button)findViewById(R.id.btn_settings);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(util.getConnectionStatus(R5_Custom.this) == 1 || ip != null || !ip.isEmpty() ){
                    Intent i = new Intent(R5_Custom.this, R6_Record_IR.class);
                    i.putExtra("txt_name", txt.getText().toString());
                    i.putExtra("groupId", gid);
                    i.putExtra("icon", filePath);
                    i.putExtra("customeIcon", customeIcon);
                    i.putExtra("ip", ip);
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(R5_Custom.this, getApplicationContext().getString(R.string.no_udp_Connection), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent urlReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, urlReturnedIntent);

        if (urlReturnedIntent.getIntExtra("custom", 0) != 1){
            customeIcon = false;
        } else {
            customeIcon = true;
        }

        switch(requestCode) {
            case R2_EditItem.SELECT_ICON_CODE:
                try {
                    filePath = urlReturnedIntent.getStringExtra("url");
                    Picasso.with(act).load(filePath).into(ir_icon);
                } catch(Exception e){
                    Picasso.with(act).load(filePath).into(ir_icon);
                }
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
}
