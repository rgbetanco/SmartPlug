package com.jiee.smartplug;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jiee.smartplug.utils.UDPCommunication;

public class IRAddNew extends Activity {

    Context context = this;
    UDPCommunication con;
    String ip;
    String devid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iradd_new);

        con = new UDPCommunication(this);
        ip = getIntent().getStringExtra("ip");
        devid = getIntent().getStringExtra("devid");

        TextView toolbar_text = (TextView)findViewById(R.id.sub_toolbar);
        toolbar_text.setText(R.string.title_add_new);

        ImageButton detect_icon = (ImageButton)findViewById(R.id.detect_icon);
        detect_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        con.sendIRMode(devid, true);
                    }
                }).start();

                Intent i = new Intent(context, DetectIR.class);
                startActivity(i);
            }
        });

        ImageButton record_icon = (ImageButton)findViewById(R.id.record_icon);
        record_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, R2_EditItem.class);
                i.putExtra("devid", devid);
                startActivity(i);
                finish();
            }
        });

    }
}
