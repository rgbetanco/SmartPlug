package com.jiee.smartplug;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class IconPicker extends Activity {

    GridView list;
    ImageButton btn_gallery;
    ImageButton btn_camera;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_picker);

        btn_gallery = (ImageButton)findViewById(R.id.btn_gallery);
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

    public File savebitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        File f = new File(Environment.getDataDirectory() + File.separator, "plugicon.jpg");
        //    f.createNewFile();
        String file = "plugicon.jpg";
        try {
            FileOutputStream fo = getApplicationContext().openFileOutput(file, getApplicationContext().MODE_PRIVATE);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return f;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 501 && resultCode == Activity.RESULT_OK) {

            Bitmap bp = (Bitmap) data.getExtras().get("data");
            try {
                savebitmap(bp);
            } catch (Exception ex){
                ex.printStackTrace();
            }

//            File image = new File(Environment.getDataDirectory() + File.separator, "plugicon.jpg");
//            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//            Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(),bmOptions);
//            bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()*0.5), (int)(bitmap.getHeight()*0.5), true);

            // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
            Uri tempUri = getImageUri(getApplicationContext(), bp);
        //    Uri tempUri = getImageUri(getApplicationContext(), bitmap);

            // CALL THIS METHOD TO GET THE ACTUAL PATH
            File finalFile = new File(getRealPathFromURI(tempUri));

            Intent i = new Intent();
            i.putExtra("url", tempUri.toString());
            i.putExtra("custom", 1);
            this.setResult(R2_EditItem.SELECT_ICON_CODE, i);
            this.finish();

        /*
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                OkHttpClient client = new OkHttpClient();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
            //    bitmaps[0].compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                //there are some my custom fields form
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("submit", "")
                        .addFormDataPart("name", "icon")
                        .addFormDataPart("photo", "tmp_photo_" + System.currentTimeMillis(), RequestBody.create(MediaType.parse("image/jpeg"), byteArray))
                        .build();

                Request request = new Request.Builder()
                        .url("www.example.com/upload_image.php")
                        .post(requestBody)
                        .build();

                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        Log.d("BACKGROUND", "doInBackground: upload success");
                    } else {
                        Log.d("BACKGROUND", "doInBackground: upload failed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(bp);
        */
    } else {
        //another results
        super.onActivityResult(requestCode, resultCode, data);
    }

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
