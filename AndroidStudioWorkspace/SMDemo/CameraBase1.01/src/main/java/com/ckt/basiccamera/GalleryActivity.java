package com.ckt.basiccamera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;

public class GalleryActivity extends Activity {
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        image = (ImageView) findViewById(R.id.image);
        Uri uri = getIntent().getData();
        Bitmap bitmap = getBitmapFromUri(uri);
        if(bitmap != null){
            image.setImageBitmap(bitmap);
        }else{
            image.setImageResource(R.mipmap.ic_launcher);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            // 读取uri所在的图片
            if(uri == null){
                return null;
            }
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    this.getContentResolver(), uri);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
