package com.ckt.basiccamera.camera;

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ckt.basiccamera.log.Log;
import com.ckt.basiccamera.logic.CameraLogic;
import com.ckt.basiccamera.logic.PhotoSaveRequest;
import com.ckt.basiccamera.util.Util;
import com.ckt.basiccamera.R;

public class PhotoActor extends Actor {
    private ImageView mCaptureBtn;

    public PhotoActor(Context context, ViewGroup container) {
        super(context, container);

    }

    protected void initContentView() {
        if (modeContentView == null) {
            modeContentView = LayoutInflater.from(mContext).inflate(
                    R.layout.camera_capture, null);
            mCaptureBtn = (ImageView) modeContentView
                    .findViewById(R.id.captureBtn);
            Log.i(TAG, "initContentView");
        }
    }

    @Override
    public void start() {
        super.start();
        mCaptureBtn.setOnClickListener(onClicklistener);
    }

    @Override
    public void stop() {
        super.stop();
        mCaptureBtn.setOnClickListener(null);
    }

    @Override
    public void updateCameraParamters() {
        super.updateCameraParamters();
        if (mCamera != null) {
            Parameters p = mCamera.getParameters();
            Size sizePicture = Util.getMaxSize(p.getSupportedPictureSizes());
            p.setPictureSize(sizePicture.width, sizePicture.height);
            Size sizePreview = getMatchedSize(p.getSupportedPreviewSizes(),
                    sizePicture.width, sizePicture.height);
            p.setPreviewSize(sizePreview.width, sizePreview.height);

            List<String> focusModes = p.getSupportedFocusModes();
            for (String tmp : focusModes) {
                if (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(tmp)) {
                    p.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    break;
                }
            }
            p.setPreviewFrameRate(30);
            p.setRecordingHint(false);
            p.setAntibanding("auto");
            p.setRotation(90);
            mCamera.setParameters(p);
            if (true /*
                      * frameRate != videoFrameRate || previewSize !=
                      * previewWidth * previewHeight
                      */) {
                mCameraLogic.stopPreview();
                mCameraLogic.startPreview();
            }
            Log.i(TAG, "updateCameraParamters");
        }
    }

    /**
     * @return true if success
     */
    protected boolean takePicture() {
        if (mCameraLogic.getStatus() != CameraLogic.STATUS_PREVIEW) {
            return false;
        }
        mCaptureBtn.setImageDrawable(mContext.getResources().getDrawable(
                R.mipmap.camera_shortcut_p));
        mCamera.takePicture(mShutterCallback, mRawPictureCallback, null,
                mPictureCallback);
        return true;
    }

    private OnClickListener onClicklistener = new OnClickListener() {
        public void onClick(View view) {
            takePicture();
        }
    };

    protected final ShutterCallback mShutterCallback = new ShutterCallback() {
        public void onShutter() {
            Log.v(TAG, "onShutter continuousShot:");
        }
    };

    private PictureCallback mPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCameraLogic.startPreview();
            mCaptureBtn.setImageDrawable(mContext.getResources().getDrawable(
                    R.mipmap.camera_shortcut));
            //save picture data
            request = new PhotoSaveRequest();
            request.prepareRequest(mFileNamer.getPhotoPath(),
                    mFileNamer.getPhotoName(request.getDateTaken()));
            request.setData(data);
            request.setCaptureRotation(0);
            //request.setMediaType(Constants.MEDIA_TYPE_IMAGE);
            Parameters p = camera.getParameters();
            request.setSize(p.getPictureSize().width, p.getPictureSize().height);
            mFileSaver.save(request);
            request = null;
        }

    };
    private PictureCallback mRawPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] rawData,
                                   Camera camera) {
        }
    };
}
