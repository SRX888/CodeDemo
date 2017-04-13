package com.ckt.basiccamera.logic;

import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ckt.basiccamera.log.Log;

import android.content.ContentResolver;
import android.content.Context;

/*
 * @author qianghua.song
 * thread to manage the media saving
 */
public class FileSaver extends Thread {
    private final static String TAG = "FileSaver";
    private Context mContext;
    private ContentResolver mResolver;
    private OnFileSaveListener onFileSaveListener;
    private ArrayList<SaveRequest> requests = new ArrayList<SaveRequest>();
    final Lock lock = new ReentrantLock();
    final Condition notEmpty = lock.newCondition();
    volatile boolean run = true;

    public FileSaver(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        setName(TAG);
        start();
    }

    /**
     * exit the file saver thread
     */
    public void exit() {
        lock.lock();
        try {
            run = false;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * do save with the given request
     * 
     * @param request
     */
    public void save(SaveRequest request) {
        lock.lock();
        try {
            requests.add(request);
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void setOnFileSaveListener(OnFileSaveListener l) {
        onFileSaveListener = l;
    }

    public void run() {
        while (run) {
            SaveRequest request = null;
            lock.lock();
            try {
                if (requests.size() == 0) {
                    notEmpty.await();
                }
                if (requests.size() > 0) {
                    request = requests.remove(0);
                }
            } catch (InterruptedException e) {
            } finally {
                lock.unlock();
            }
            if (request != null && request.saveRequest()) {
                if (request.saveDatabase(mResolver)) {
                    request.broadcastNewMedia(mContext);
                    Log.d(TAG, "saved:" + request.getUri());
                }
                if (onFileSaveListener != null) {
                    // may do create thumbnail
                    onFileSaveListener.onFileSaved(request);
                }
            }
        }
        return;
    }

    /**
     * Callback listener for media file save
     * 
     * @author admin
     * 
     */
    public interface OnFileSaveListener {
        /**
         * Callback on media file save
         * 
         * @param request
         *            file save request
         */
        void onFileSaved(SaveRequest request);
    }
}
