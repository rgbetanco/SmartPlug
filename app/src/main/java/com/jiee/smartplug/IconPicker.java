package com.jiee.smartplug;

import android.app.Activity;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jiee.smartplug.adapters.ListIconsAdapter;

import java.util.ArrayList;

public class IconPicker extends Activity {

    ListView list;
    ImageButton btn_setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_picker);

        btn_setting = (ImageButton)findViewById(R.id.btn_settings);
        btn_setting.setVisibility(View.GONE);

        list = (ListView)findViewById(R.id.listViewIcons);
        ListIconsAdapter adapter = new ListIconsAdapter(this);
        list.setAdapter(adapter);

    }
}
