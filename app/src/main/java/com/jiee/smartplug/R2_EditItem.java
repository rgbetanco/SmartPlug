package com.jiee.smartplug;

/** TO DO:
 *
 /*
 R2_EditItem.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 This class allows the user to add the icon and name of the IR groups
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.jiee.smartplug.objects.BroadcastReply;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.squareup.picasso.Picasso;

import java.io.File;

public class R2_EditItem extends Activity {
    public static final int SELECT_ICON_CODE = 75;
    ImageButton ir_icon;
    String filePath = "http://rgbetanco.com/jiEE/icons/btn_power_pressed.png";
    Button save;
    EditText txt;
    String devid;
    String token;
    HTTPHelper http;
    Boolean customeIcon = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r2__edit_item);

        devid = getIntent().getStringExtra("devid");

        http = new HTTPHelper(this);

        ir_icon = (ImageButton)findViewById(R.id.ir_icon);
        Picasso.with(this).load(filePath).into(ir_icon);
        ir_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(R2_EditItem.this, IconPicker.class);
                intent.putExtra("activity", "IR_Group");
                startActivityForResult(intent, SELECT_ICON_CODE);
            }
        });

        txt = (EditText)findViewById(R.id.txt_name);

        save = (Button)findViewById(R.id.btn_settings);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = txt.getText().toString();
                MySQLHelper sql = HTTPHelper.getDB(R2_EditItem.this);
                if(!title.isEmpty()) {
                    Cursor c = sql.getToken();
                    if(c.getCount()>0){
                        c.moveToFirst();
                        token = c.getString(1);
                    }
                    final String type = "group";
                    final String action = "add";
                    final String groupName = txt.getText().toString();
                    int iconId = 0;
                    Cursor cur = sql.getIconByUrl(filePath);
                    if(cur.getCount()>0){
                        cur.moveToFirst();
                        iconId = cur.getInt(1);
                    }

                    final int groupId = sql.insertIRGroup(groupName, devid, filePath, 0, -1);

                    Miscellaneous mis = new Miscellaneous();
                    final int res = mis.getResolution(R2_EditItem.this);
                    final int nIconId = iconId;

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            http.manageIRGroup(devid, GlobalVariables.IR_SERVICE, type, action, 0, groupName, nIconId, res, groupId, customeIcon);
                            Intent i = new Intent("serverReplied");
                            sendBroadcast(i);

                        }
                    }).start();

                    finish();
                } else {
                    Toast.makeText(R2_EditItem.this, "Field must not be blank", Toast.LENGTH_SHORT).show();
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
            case SELECT_ICON_CODE:
                try {
                    filePath = urlReturnedIntent.getStringExtra("url");
                    Picasso.with(R2_EditItem.this).load(filePath).into(ir_icon);
                } catch(Exception e){
                    Picasso.with(R2_EditItem.this).load(filePath).into(ir_icon);
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
