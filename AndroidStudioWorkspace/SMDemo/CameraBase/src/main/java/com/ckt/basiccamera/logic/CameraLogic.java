package com.ckt.basiccamera.logic;

import com.ckt.basiccamera.log.Log;

import android.hardware.Camera;
import android.os.Handler;

public class CameraLogic implements Runnable {
    public static final String TAG = "CameraLogic";
    public static final int STATUS_CLOSED = -1;
    public static final int STATUS_OPENNED = 0;
    public static final int STATUS_PREVIEW = 1;
    private int status;
    private short cameraId;
    private volatile boolean release;
    private Camera mCamera;
    private Handler uiHandler;
    private int cameraOpendMsg;

    private static CameraLogic mInstance = new CameraLogic();

    /**
     * @return instance of camera logic
     */
    public static CameraLogic getInstance() {
        return mInstance;
    }

    /**
     * @param h
     *            main handler
     * @param mseeage
     *            message for camera open
     */
    public void init(Handler h, int mseeage) {
        uiHandler = h;
        cameraOpendMsg = mseeage;
    }

    private CameraLogic() {
    }

    /**
     * @param id
     *            camera id
     */
    public void startCamera(short id) {
        release = false;
        status = STATUS_CLOSED;
        cameraId = id;
        Thread t = new Thread(this);
        t.setName("CameraOpener");
        t.start();
    }

    /**
     * should call after {@link cameraOpendMsg} received
     * 
     * @return hardware camera, null means open failed or released;
     */
    public Camera getCamera() {
        return mCamera;
    }

    /**
     * @return the current opened camera id
     */
    public short getCameraId() {
        return cameraId;
    }

    /**
     * start preview
     */
    public void startPreview() {
        if (status != STATUS_CLOSED && mCamera != null) {
            status = STATUS_PREVIEW;
            mCamera.startPreview();
        }
    }

    /**
     * stop preview
     */
    public void stopPreview() {
        if (status == STATUS_PREVIEW && mCamera != null) {
            status = STATUS_OPENNED;
            mCamera.stopPreview();
        }
    }

    /**
     * release camera
     */
    public void release() {
        if (STATUS_CLOSED != status && mCamera != null && !release) {
            mCamera.release();
            status = STATUS_CLOSED;
        }
        release = true;
        mCamera = null;
    }

    public boolean isReleased() {
        return release;
    }

    public int getStatus() {
        return status;
    }

    public void run() {
        Log.d(TAG, "open");
        mCamera = null;
        mCamera = Camera.open(cameraId);
        Log.d(TAG, "open end");
        if (mCamera == null || release) {
            status = STATUS_CLOSED;
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        } else {
            status = STATUS_OPENNED;
        }
        if (uiHandler != null && !release) {
            uiHandler.sendEmptyMessage(cameraOpendMsg);
        }
        return;
    }

}
