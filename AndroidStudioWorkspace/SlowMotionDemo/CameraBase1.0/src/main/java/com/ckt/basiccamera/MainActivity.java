package com.ckt.basiccamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ckt.basiccamera.camera.Actor;
import com.ckt.basiccamera.camera.PhotoActor;
import com.ckt.basiccamera.camera.VideoActor;
import com.ckt.basiccamera.log.Log;
import com.ckt.basiccamera.logic.CameraLogic;
import com.ckt.basiccamera.logic.FileSaver;
import com.ckt.basiccamera.logic.SaveRequest;
import com.ckt.basiccamera.logic.FileSaver.OnFileSaveListener;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private final String TAG = "MainActivity";
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private ImageView mThumbView;
    private ViewGroup container;
    private FileSaver mFileSaver;
    private Camera mCamera;
    private short mCameraId = 0;
    private CameraLogic mCameraLogic;
    private final static int MSG_CAMERA_OPENED = 1;
    private final static int UPDATE_THUMB_VIEW = 2;
    private Actor mActor;
    private Uri uri;

    //录像权限请求码
    private static final int REQUEST_VIDEO_PERMISSION = 2;
    //录像权限
    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private final Handler mainHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CAMERA_OPENED:
                    mCamera = mCameraLogic.getCamera();
                    mCameraId = mCameraLogic.getCameraId();
                    mActor.updateCameraParamters();
                    startPreview();
                case UPDATE_THUMB_VIEW:
                    mThumbView.setImageBitmap((Bitmap)msg.obj);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (PermissionActivity.checkAndRequestPermission(this, VIDEO_PERMISSIONS)) {
            finish();
        }

        mCameraLogic = CameraLogic.getInstance();
        mCameraLogic.init(mainHandler, MSG_CAMERA_OPENED);
        mFileSaver = new FileSaver(this);
        mFileSaver.setOnFileSaveListener(onFileSavelistenner);
        mThumbView = (ImageView) findViewById(R.id.thumbnail);
        mThumbView.setOnClickListener(onClickLisetener);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder = null;

        container = (ViewGroup) findViewById(R.id.container);
        mActor = new VideoActor(MainActivity.this, container);
        mActor.setFileSaver(mFileSaver);
    }

    public OnClickListener onClickLisetener =  new OnClickListener(){
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(mActor instanceof VideoActor){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "video/*");
                startActivity(intent);
            }else if(mActor instanceof PhotoActor){
                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
                intent.setData(uri);
                MainActivity.this.startActivity(intent);
            }
        }
    };
    public OnFileSaveListener  onFileSavelistenner = new OnFileSaveListener(){
        @Override
        public void onFileSaved(SaveRequest request) {
            // TODO Auto-generated method stub
            Message msg = mainHandler.obtainMessage();
            msg.what = UPDATE_THUMB_VIEW;
            uri = request.getUri();
            msg.obj = request.createThumbnail(100, 100*100).getBitmap();
            mainHandler.sendMessage(msg);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        mActor.start();
        startCamera();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        mActor.stop();
        mCameraLogic.stopPreview();
        mCameraLogic.release();
        mCamera = null;
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFileSaver.exit();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.i(TAG, "surfaceChanged");
        mSurfaceHolder = holder;
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed");
        mSurfaceHolder = null;
    }

    private void startPreview() {
        if (mCamera != null && mSurfaceHolder != null) {
            try {
                if (!mCameraLogic.isReleased()) {
                    mCamera.setPreviewDisplay(mSurfaceHolder);
                    mCamera.setDisplayOrientation(90);
                }
                mCameraLogic.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startCamera() {
        if (mCamera == null) {
            mCameraLogic.startCamera(mCameraId);
        }
    }
}
