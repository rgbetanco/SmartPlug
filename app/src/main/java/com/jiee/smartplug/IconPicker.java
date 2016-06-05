package com.jiee.smartplug;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jiee.smartplug.adapters.ListIconsAdapter;
import com.jiee.smartplug.utils.GlobalVariables;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/*

    Designed by Chinsoft Company, 2016. Copyrighted

 */

public class IconPicker extends Activity {

    GridView list;
    ImageButton btn_gallery;
    ImageButton btn_camera;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String callerActivity = null;

    public final static int DECODE_FLAG_NONE    = 0x0000; // return as is
    public final static int DECODE_FLAG_CROP    = 0x0001; // crop image to fit within bounds
    public final static int DECODE_FLAG_FIT     = 0x0002; // fit within given bounds
    public final static int DECODE_FLAG_CROPFIT  = 0x0003; // crop and fit image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_picker);

        callerActivity = getIntent().getStringExtra("activity");

        btn_gallery = (ImageButton)findViewById(R.id.btn_gallery);
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select File"), 502);
            }
        });

        btn_camera = (ImageButton)findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 501);
            }
        });


        list = (GridView)findViewById(R.id.listViewIcons);
        ListIconsAdapter adapter = new ListIconsAdapter(this);
        list.setAdapter(adapter);

    }

    public static Rect calculateBitmapCropSrcRect( int srcW, int srcH, int dstW, int dstH ) {
        final float srcAspect = (float)srcW / (float)srcH;
        final float dstAspect = (float)dstW / (float)dstH;

        if( srcAspect > dstAspect ) {
            final int finalW = (int)(srcH * dstAspect);
            final int l = (srcW - finalW) / 2;
            return new Rect( l, 0, l + finalW, srcH );
        } else {
            final int finalH = (int)(srcW / dstAspect);
            final int t = (srcH - finalH) / 2;
            return new Rect( 0, t, srcW, t + finalH );
        }
    }

    public static Bitmap cropBitmap( Bitmap bitmap, int w, int h, int flags ) {
        final int srcW = bitmap.getWidth();
        final int srcH = bitmap.getHeight();

        // if scaleToFit == false, crop out w,h from bitmap
        float scale = 1;
        if( srcW < w )
            scale = (float)srcW / w;

        if( srcH < h ) {
            float scaleH = (float)srcH / h;
            if( scaleH < scale )
                scale = scaleH;
        }

        if( scale < 1 ) {
            w *= scale;
            h *= scale;
        }

        Rect  srcRect;
        RectF dstRect = new RectF(0,0,w,h);

        if( (flags & DECODE_FLAG_FIT) == DECODE_FLAG_FIT ) {
            srcRect = calculateBitmapCropSrcRect( srcW, srcH, w, h );
        } else {
            // crop image to center
            int l = (srcW - w) / 2;
            int t = (srcH - h) / 2;

            srcRect = new Rect(l,t,w+l,h+t);
        }

        Bitmap scaledBitmap = null;

        // crop without rounding the edges... we could just crop, unless we're also scaling down (in which case, we
        // scale down to save memory)
        if( srcRect.width()==0 )
            return null;   // error.

        // crop the source rectangle, while applying a matrix to resize it to the proper size
        scale = dstRect.width() / srcRect.width();

        Matrix m = null;
        if( scale<1 ) {
            m = new Matrix();
            m.postScale(scale, scale);
        }
        scaledBitmap = Bitmap.createBitmap(bitmap, srcRect.left, srcRect.top, srcRect.width(), srcRect.height(), m, true);

        return scaledBitmap;
    }

    public Boolean savebitmap(Bitmap bmp) throws IOException {

        boolean toReturn = false;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        bmp = cropBitmap( bmp, 400, 400, DECODE_FLAG_CROPFIT );

        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        String file = null;
        if(callerActivity.equals("M2A")) {
            file = GlobalVariables.plugfile;
        } else if(callerActivity.equals("IR_Group")){
            file = GlobalVariables.irgroupfile;
        } else if(callerActivity.equals("IR_Command")){
            file = GlobalVariables.irfile;
        }
        try {
            FileOutputStream fo = getApplicationContext().openFileOutput(file, getApplicationContext().MODE_PRIVATE);
            fo.write(bytes.toByteArray());
            fo.close();
            toReturn = true;
        } catch (Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 501 && resultCode == Activity.RESULT_OK) {

            Bitmap bp = (Bitmap) data.getExtras().get("data");
            setIcon(bp);

        } else if(requestCode == 502 && resultCode == Activity.RESULT_OK) {
            Bitmap bm = null;
            if (data != null) {
                try {
                    bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
                    setIcon(bm);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    public void setIcon(Bitmap bitmap){
        try {
            savebitmap(bitmap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
        Uri tempUri = getImageUri(getApplicationContext(), bitmap);
        //    Uri tempUri = getImageUri(getApplicationContext(), bitmap);

        // CALL THIS METHOD TO GET THE ACTUAL PATH
        File finalFile = new File(getRealPathFromURI(tempUri));

        Intent i = new Intent();
        i.putExtra("url", tempUri.toString());
        i.putExtra("custom", 1);
        this.setResult(R2_EditItem.SELECT_ICON_CODE, i);
        this.finish();
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }


}
