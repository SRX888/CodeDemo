package com.example.srx;

/**
 * Created by ruixiang.shen on 2/25/17.
 */

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.srx.PermissionUtil.RequestPermissionsActivity;
import com.example.zoomanddrafting.R;

import java.io.File;
import java.io.FileNotFoundException;


public class PhotoViewer extends Activity implements OnTouchListener {

	private static final String TAG = "PhotoViewer";
	public static final int RESULT_CODE_NOFOUND = 200;


    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    DisplayMetrics dm;
    ImageView imgView;
    ImageView imgView1;
    Bitmap bitmap;

    Uri pathToUri;

    /** 最小缩放比例*/
    float minScaleR = 1.0f;
    /** 最大缩放比例*/
    static final float MAX_SCALE = 10f;

    /** 初始状态*/
    static final int NONE = 0;
    /** 拖动*/
    static final int DRAG = 1;
    /** 缩放*/
    static final int ZOOM = 2;
    
    /** 当前模式*/
    int mode = NONE;

    PointF prev = new PointF();
    PointF mid = new PointF();
    float dist = 1f;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(RequestPermissionsActivity.startPermissionActivity(this)) {
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        
		setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        imgView = (ImageView) findViewById(R.id.imageView1);// 获取控件
        imgView1 = (ImageView) findViewById(R.id.imageView2);

        Button btn= (Button) findViewById(R.id.btn);
        Button btn1= (Button) findViewById(R.id.btn1);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);
            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,2);
            }
        });

        imgView.setOnTouchListener(this);// 设置触屏监听
        imgView1.setOnTouchListener(this);
        dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);// 获取分辨率
//        minZoom();
        center();
        imgView.setImageMatrix(matrix);
        imgView1.setImageMatrix(matrix);
        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK ) {
            Log.i("tag", "requestCode=====:"+requestCode );
            Log.i("tag", "resultCode=====:"+resultCode );
            Uri uri = data.getData();
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor actualimagecursor = managedQuery(uri, proj, null, null, null);
            int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            actualimagecursor.moveToFirst();
            Log.i("tag", "onActivityResult:actual_image_column_index== "+actualimagecursor);
            String img_path = actualimagecursor.getString(actual_image_column_index);
            Log.i("tag", "onActivityResult: img_path=="+img_path);
            File file = new File(img_path);
            Toast.makeText(PhotoViewer.this, file.toString(), Toast.LENGTH_SHORT).show();
            Log.i("tag", "onActivityResult: file.toString()==="+file.toString());
            pathToUri = FilePathToUri(this, img_path);
            Log.i("tag", "onActivityResult: pathToUri======"+pathToUri);
//            String prefix = "content://com.android.providers.media.documents/document/image";
//            if(pathToUri.toString().startsWith(prefix) &&
//                    DocumentsContract.isDocumentUri(this, pathToUri)){
//                pathToUri = transformUri(pathToUri);
//            }
            if(requestCode==1) {
                Glide
                        .with(this)
                        .load(pathToUri)
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.error)
                        .crossFade(2000)

                        .into(imgView);
            }

            Log.i("tag", "onActivityResult:1111111111111111111111111 ");

            if(requestCode==2){
            Glide
                    .with(this)
                    .load(pathToUri)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.error)
                    .crossFade(2000)

                    .into(imgView1);
        }}


         bitmap = decodeUriAsBitmap(pathToUri);
    }



    public void SureOnClick(View v)
    {
    	
    }
    
    /**
     * 触屏监听
     */
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
        // 主点按下
        case MotionEvent.ACTION_DOWN:
            savedMatrix.set(matrix);
            prev.set(event.getX(), event.getY());
            mode = DRAG;
            break;
        // 副点按下
        case MotionEvent.ACTION_POINTER_DOWN:
            dist = spacing(event);
            // 如果连续两点距离大于10，则判定为多点模式
            if (spacing(event) > 10f) {
                savedMatrix.set(matrix);
                midPoint(mid, event);
                mode = ZOOM;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            mode = NONE;
            //savedMatrix.set(matrix);
            break;
        case MotionEvent.ACTION_MOVE:
            if (mode == DRAG) {
                matrix.set(savedMatrix);
                matrix.postTranslate(event.getX() - prev.x, event.getY()
                        - prev.y);
            } else if (mode == ZOOM) {
                float newDist = spacing(event);
                if (newDist > 10f) {
                    matrix.set(savedMatrix);
                    float tScale = newDist / dist;
                    matrix.postScale(tScale, tScale, mid.x, mid.y);
                }
            }
            break;
        }
        imgView.setImageMatrix(matrix);
        imgView1.setImageMatrix(matrix);

        CheckView();
        return true;
    }

    /**
     * 限制最大最小缩放比例，自动居中
     */
    private void CheckView() {
        float p[] = new float[9];
        matrix.getValues(p);
        if (mode == ZOOM) {
            if (p[0] < minScaleR) {
            	//Log.d("", "当前缩放级别:"+p[0]+",最小缩放级别:"+minScaleR);
                matrix.setScale(minScaleR, minScaleR);
            }
            if (p[0] > MAX_SCALE) {
            	//Log.d("", "当前缩放级别:"+p[0]+",最大缩放级别:"+MAX_SCALE);
                matrix.set(savedMatrix);
            }
        }
        center();
    }

    /**
     * 最小缩放比例，最大为100%
     */
    private void minZoom() {
        minScaleR = Math.min(
                (float) dm.widthPixels / (float) bitmap.getWidth(),
                (float) dm.heightPixels / (float) bitmap.getHeight());
        if (minScaleR < 1.0) {
            matrix.postScale(minScaleR, minScaleR);
        }
    }

    private void center() {
        center(true, true);
    }

    /**
     * 横向、纵向居中
     */
    protected void center(boolean horizontal, boolean vertical) {
        if(bitmap!=null) {
            Matrix m = new Matrix();
            m.set(matrix);

            RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());

            m.mapRect(rect);

            float height = rect.height();
            float width = rect.width();

            float deltaX = 0, deltaY = 0;

            if (vertical) {
                // 图片小于屏幕大小，则居中显示。大于屏幕，上方留空则往上移，下方留空则往下移
                int screenHeight = dm.heightPixels;
                if (height < screenHeight) {
                    deltaY = (screenHeight - height) / 2 - rect.top;
                } else if (rect.top > 0) {
                    deltaY = -rect.top;
                } else if (rect.bottom < screenHeight) {
                    deltaY = imgView.getHeight() - rect.bottom;
                    deltaY = imgView1.getHeight() - rect.bottom;
                }
            }

            if (horizontal) {
                int screenWidth = dm.widthPixels;
                if (width < screenWidth) {
                    deltaX = (screenWidth - width) / 2 - rect.left;
                } else if (rect.left > 0) {
                    deltaX = -rect.left;
                } else if (rect.right < screenWidth) {
                    deltaX = screenWidth - rect.right;
                }
            }
            matrix.postTranslate(deltaX, deltaY);
        }
    }

    /**
     * 两点的距离
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 两点的中点
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
    
    public static class ImageViewOnClickListener implements View.OnClickListener
	{
    	private Context context;
    	private String img_path;
    	public ImageViewOnClickListener(Context context, String img_path)
    	{
    		this.img_path = img_path;
    		this.context = context;
    	}
		@Override
		public void onClick(View v) {
			if(img_path!=null)
			{
				Intent intent = new Intent(context,PhotoViewer.class);
				intent.putExtra("PhotoPath", img_path);
				context.startActivity(intent);
			}
			
		}
	}


    private Bitmap decodeUriAsBitmap(Uri uri) {

        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }


    public static Uri FilePathToUri(Context context, String path) {

        Log.d("TAG", "filePath is " + path);
        if (path != null) {
            path = Uri.decode(path);
            Log.d("TAG", "path2 is " + path);
            ContentResolver cr = context.getContentResolver();
            StringBuffer buff = new StringBuffer();
            buff.append("(")
                    .append(MediaStore.Images.ImageColumns.DATA)
                    .append("=")
                    .append("'" + path + "'")
                    .append(")");
            Cursor cur = cr.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.ImageColumns._ID},
                    buff.toString(), null, null);
            int index = 0;
            for (cur.moveToFirst(); !cur.isAfterLast(); cur
                    .moveToNext()) {
                index = cur.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                // set _id value
                index = cur.getInt(index);
            }
            if (index == 0) {
                //do nothing
            } else {
                Uri uri_temp = Uri
                        .parse("content://media/external/images/media/"
                                + index);
                Log.d("TAG", "uri_temp is " + uri_temp);
                if (uri_temp != null) {
                    return uri_temp;
                }
            }

        }
        return null;

    }


}