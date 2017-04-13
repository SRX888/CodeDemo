package com.ckt.basiccamera.camera;

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.ckt.basiccamera.logic.CameraLogic;
import com.ckt.basiccamera.logic.FileNamer;
import com.ckt.basiccamera.logic.FileSaver;
import com.ckt.basiccamera.logic.SaveRequest;
import com.ckt.basiccamera.logic.FileSaver.OnFileSaveListener;
import com.ckt.basiccamera.util.Util;

public abstract class Actor {
    protected final String TAG = getClass().getSimpleName();
    protected Context mContext;
    protected ViewGroup mBaseContainer;
    protected View modeContentView = null;
    protected int previewHeight, previewWidth;
    protected CameraLogic mCameraLogic;
    protected Camera mCamera;
    protected short mCameraId;
    protected SaveRequest request;
    protected FileSaver mFileSaver;
    protected FileNamer mFileNamer;

    public Actor(Context context, ViewGroup container) {
        mContext = context;
        mBaseContainer = container;
        mCameraLogic = CameraLogic.getInstance();
        initContentView();
        if(mFileNamer == null){
             mFileNamer = new FileNamer();
        }
    }

    public void setFileSaver(FileSaver saver){
        mFileSaver = saver;
    }

    protected abstract void initContentView();

    public void updateCameraParamters() {
        if (CameraLogic.getInstance().getCamera() == null) {
            return;
        }
        mCamera = CameraLogic.getInstance().getCamera();
    }

    /**
     * add modeContentView in {@code start()}
     */
    public void start() {
        if (modeContentView != null) {
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            mBaseContainer.addView(modeContentView, lp);
        }
    }

    public void stop() {
        mBaseContainer.removeView(modeContentView);
    }

    protected final Size getMatchedSize(List<Size> sizes, int width, int height) {
        // find the matched size TODO
        Util.getMaxSize(sizes);
        float minDistance = 100;
        Size minSize = null;
        for(Size size :sizes){
            if(width * size.height == height * size.width){
                return size;
            }
        }
        for(Size size :sizes){
            if(((float)width/height - (float)size.width/size.height) < minDistance){
                minDistance = (float)width/height - (float)size.width/size.height;
                minSize.width = size.width;
                minSize.height = size.height;
            }
        }
        return minSize;
    }

}
