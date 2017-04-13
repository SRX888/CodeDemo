package com.srx.video;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import android.support.annotation.NonNull;

import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.srx.util.FileUtil;
import com.srx.util.PermissionActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "CameraActivity";
    //录像权限请求码
    private static final int REQUEST_VIDEO_PERMISSION = 2;
    //录像权限
    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //Sensor方向，大多数设备是90度
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    //Sensor方向，一些设备是270度
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final String SMPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "SlowMotion" + File.separator;

    //sensor的方向为90度时，屏幕方向与Sensor方向的对应关系
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //sensor的方向为270度时，屏幕方向与Sensor方向的对应关系
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }
    private AutoFitTextureView mTextureView;
    private Button mButton;
    private Size mVideoSize;//视频大小
    private Size mPreviewSize;//预览大小
    private Integer mSensorOrientation;//Sensor方向
    private MediaRecorder mMediaRecorder;//用于录制视频的MediaRecorder对象
    private Semaphore mCameraLock = new Semaphore(1);//Camera互斥锁
    private String mCameraId;//摄像头ID（通常0代表后置摄像头，1代表前置摄像头）
    private CameraDevice mCameraDevice;//代表摄像头的成员变量
    private CameraCaptureSession mCameraCaptureSession;//定义CameraCaptureSession成员变量
    private HandlerThread mBackgroundThread;//定义后台线程
    private Handler mBackgroundHandler;//定义后台线程的Handler
    private CaptureRequest.Builder mPreviewRequestBuilder;//预览请求的CaptureRequest.Builder对象
    private boolean mIsRecordingVideo;//是否正在录像
    private File mDir;//存放视频的父目录
    private File mDirpath;//存放视频的父目录
    private String mVideoFileAbsolutePath;//视频的保存位置

    protected static final int VIDEO_ControlRateConstant = 2;


    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            openCamera(width, height);//打开相机
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraLock.release();
            mCameraDevice = cameraDevice;
            startPreview();//开始预览
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            CameraActivity.this.finish();
        }
    };

    /**
     * 开始预览
     */
    private void startPreview() {
        if (mCameraDevice == null || !mTextureView.isAvailable() || mPreviewSize == null) {
            return;
        }

        closeCameraCaptureSession();//关闭CameraCaptureSession，预览和录像都创建了CameraCaptureSession，位防止内存溢出，在预览和录像前都需要关闭
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        //设置预览大小
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        try {
            if(mCameraDevice!=null){
                //创建作为预览的CaptureRequest.Builder对象
                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                Log.i(TAG, "Preview Builder:" + mPreviewRequestBuilder.toString());
                //将mTextureView的surface作为CaptureRequest.Builder的目标
                mPreviewRequestBuilder.addTarget(surface);
                //创建用于预览的CameraCaptureSession
                mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull  CameraCaptureSession cameraCaptureSession) {
                        if (mCameraDevice == null) {
                            return;
                        }
                        mCameraCaptureSession = cameraCaptureSession;
                        Log.i(TAG, "Preview Session:" + mCameraCaptureSession.toString());
                        updatePreview();//更新预览
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Toast.makeText(CameraActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
                    }
                }, mBackgroundHandler);}
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新预览
     */
    private void updatePreview() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    //预览
                    mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    throw new RuntimeException("无法访问相机");
                }
            }
        });
    }

    /**
     * 打开相机
     * @param width  SurfaceTexture的宽
     * @param height  SurfaceTexture的高
     */
    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {


        if (this.isFinishing()) {
            return;
        }
        setCameraInfo(width, height);

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("相机打开超时");
            }

            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);

        } catch (InterruptedException e) {
            throw new RuntimeException("打开相机时中断");
        } catch (CameraAccessException e) {
            throw new RuntimeException("无法访问相机");
        }
    }

    /**
     * 设置Camera信息
     * @param width  SurfaceTexture的宽
     * @param height  SurfaceTexture的高
     */
    private void setCameraInfo(int width, int height) {
        CameraManager cameraManager =  (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mCameraId = cameraId;
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);

                int orientation = getResources().getConfiguration().orientation;
                //若是横屏
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    //若是竖屏
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                mSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                mMediaRecorder = new MediaRecorder();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 选择最佳大小
     * @param choices  可供大小选择
     * @param width  宽
     * @param height  高
     * @param aspectRatio  指定宽高比
     * @return  返回最佳的大小
     */
    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        //若有足够大的，选择最小的一个
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    /**
     * 选择视频大小
     * @param choices  可供大小选择
     * @return  返回视频大小
     */
    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        //若没有找到合适的，返回最后一个
        return choices[choices.length - 1];
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PermissionActivity.checkAndRequestPermission(this, VIDEO_PERMISSIONS)) {
            finish();
        }
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mDirpath = new File(SMPATH);
        if (!mDirpath.exists()) {
            Log.i("file ", "exists");
            mDirpath.mkdirs();
        }

        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);
        mButton = (Button) findViewById(R.id.video);
        mButton.setOnClickListener(this);

//        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            mDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
//            Log.i(TAG, "onCreate:mDir =="+mDir);
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();//开启后台线程
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * 开启后台线程
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            throw new RuntimeException("停止后台线程时中断");
        }
    }

    /**
     * 关闭摄像头
     */
    private void closeCamera() {
        try {
            mCameraLock.acquire();
            closeCameraCaptureSession();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (mMediaRecorder != null) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("关闭相机时中断");
        } finally {
            mCameraLock.release();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video:
                if (mIsRecordingVideo) {
                    stopRecordingVideo();

                } else {
                    startRecordingVideo();
                }
                break;
        }
    }

    /**
     * 停止录像
     */
    private void stopRecordingVideo() {
        mIsRecordingVideo = false;
        mButton.setText(R.string.record);
        new Thread(new Runnable() {
            @Override
            public void run() {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }
        }).start();


        Toast.makeText(this, "保存到：" + mVideoFileAbsolutePath, Toast.LENGTH_SHORT).show();
        mVideoFileAbsolutePath = null;
        startPreview();//开始预览
    }

    /**
     * 开始录像
     */
    private void startRecordingVideo() {
        if (mCameraDevice == null || !mTextureView.isAvailable() || mPreviewSize == null) {
            return;
        }

        closeCameraCaptureSession();//关闭CameraCaptureSession
        setMediaRecorder();//设置MediaRecorder信息
        SurfaceTexture surfaceTexture  = mTextureView.getSurfaceTexture();
        //设置预览大小
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        try {
            //创建作为录像的CaptureRequest.Builder对象
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            Log.i(TAG, "Video Builder:" + mPreviewRequestBuilder.toString());
            Surface previewSurface  = new Surface(surfaceTexture);
            //将mTextureView的Surface作为CaptureRequest.Builder的目标
            mPreviewRequestBuilder.addTarget(previewSurface);

            Surface mediaRecorderSurface = mMediaRecorder.getSurface();
            //将mMediaRecorder的Surface作为CaptureRequest.Builder的目标
            mPreviewRequestBuilder.addTarget(mediaRecorderSurface);
            //add by srx

            MediaFormat format = MediaFormat.createVideoFormat("video/mp4", mPreviewSize.getWidth(), mPreviewSize.getHeight());
            format.setInteger("bitrate-mode", VIDEO_ControlRateConstant);

            //创建用于录像的CameraCaptureSession，录像使用两个Surface：previewSurface和mediaRecorderSurface
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mediaRecorderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraCaptureSession = cameraCaptureSession;
                    Log.i(TAG, "Video Session:" + mCameraCaptureSession.toString());
                    updatePreview();//更新预览

                    CameraActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mButton.setText(R.string.stop);
                            mIsRecordingVideo = true;
                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraActivity.this, R.string.failed, Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭CameraCaptureSession
     */
    private void closeCameraCaptureSession() {
        if (mCameraCaptureSession != null) {
            Log.i(TAG, "close Session:" + mCameraCaptureSession.toString());
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    /**
     * 设置MediaRecorder信息
     */
    private void setMediaRecorder() {
        //设置音频源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置视频源
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //设置输出格式
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        if (mVideoFileAbsolutePath == null || mVideoFileAbsolutePath.isEmpty()) {
            mVideoFileAbsolutePath = new File(mDirpath, FileUtil.getFileName(false)).getAbsolutePath();
            Log.i(TAG, "video file:" + mVideoFileAbsolutePath);
        }
        //设置输出路径
        mMediaRecorder.setOutputFile(mVideoFileAbsolutePath);
        //设置视频编码二进制比特率
        mMediaRecorder.setVideoEncodingBitRate(14000000);
        //设置视频帧率（每秒多少帧）
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setCaptureRate(120);



        //设置视频大小
//        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());

        mMediaRecorder.setVideoSize(1280, 720);
        //mVideoSize.getWidth()== 640 //mVideoSize.getHeight()== 480
        // (640,480)
        Log.i("srx", "setMediaRecorder:mVideoSize.getWidth()== "+mVideoSize.getWidth());
        Log.i("srx", "setMediaRecorder:mVideoSize.getHeight()== "+mVideoSize.getHeight());
        //设置视频编码
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置音频编码
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //获得屏幕方向
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            //Sensor方向为90度时
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                //根据屏幕方向设置视频方向
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            //Sensor方向为270度时
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                //根据屏幕方向设置视频方向
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * activity的onRequestPermissionsResult会被回调来通知结果（通过第三个参数）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_VIDEO_PERMISSION:
                if (grantResults.length == VIDEO_PERMISSIONS.length) {
                    for (int grantResult : grantResults) {
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            stopApp(this);
                            break;
                        }
                    }
                } else {
                    stopApp(this);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 停止Activity：APP停止运行
     */
    private void stopApp(Activity activity) {
        Toast.makeText(activity, R.string.sorry, Toast.LENGTH_SHORT).show();
        activity.finish();
    }


}
