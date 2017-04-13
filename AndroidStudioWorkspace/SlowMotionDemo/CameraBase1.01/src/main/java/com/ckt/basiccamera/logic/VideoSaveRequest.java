package com.ckt.basiccamera.logic;

import java.io.File;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.ckt.basiccamera.util.BitmapUtil;
import com.ckt.basiccamera.util.Util;

public class VideoSaveRequest extends SaveRequest {
    // add video size check, avoid too tiny video to save.
    private static final int MIN_VIDEO_SIZE = 10 * 1024;// 10Kbyte
    private long mDuration;

    public VideoSaveRequest() {
        super();
        mSuffix = TYPE_MPEG_4;
        mMimeType = MIMETYPE_MPEG_4;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    @Override
    public void prepareRequest(String dir, String title) {
        mTitle = title;
        mDisplayName = mTitle + mSuffix;
        File tmp = new File(dir);
        if (!tmp.exists()) {
            tmp.mkdirs();
        }
        mPath = tmp.getPath() + File.separator + mDisplayName;
        File tmpFile = new File(mPath);
        if (tmpFile.exists()) {
            int i = 0;
            int id = -1;
            int lastId = 0;

            String orginTitle = mTitle;
            String[] titles = title.split("_");
            String endString = titles[titles.length - 1];
            try {
                id = Integer.parseInt(endString);
                lastId = title.lastIndexOf(endString);
            } catch (Exception e) {
                Log.e(TAG, "prepareRequest " + e.toString());
                e.printStackTrace();
                id = -1;
            }
            while (tmpFile.exists()) {
                if (id != -1) {
                    mTitle = title.substring(0, lastId - 1) + i + "_"
                            + endString;
                } else {
                    mTitle = orginTitle + i;
                }
                mDisplayName = mTitle + mSuffix;
                mPath = tmp.getPath() + File.separator + mDisplayName;
                tmpFile = new File(mPath);
                i++;
            }
        }
        mTmpPath = null;
        Log.d(TAG, "prepareRequest ok:" + mPath);
    }

    @Override
    public boolean saveRequest() {
        File temp = new File(getTemporaryPath());
        Log.d(TAG, "saveRequest path=" + mPath);
        if (!temp.exists()) {
            Log.e(TAG, "saveRequest video file not created");
            return false;
        }
        mDataSize = temp.length();
        Log.d(TAG, "saveRequest video,mDataSize=" + mDataSize);
        if (temp.exists() && mDataSize < MIN_VIDEO_SIZE) {
            Log.e(TAG, "saveRequest delete error video,mDataSize=" + mDataSize);
            temp.delete();
            return false;
        }
        if (mDuration <= 0) {
            mDuration = Util.getVideoDuration(mTmpPath);
            Log.d(TAG, "saveRequest video,mDuration=" + mDuration);
        }
        File file = new File(mPath);
        return temp.renameTo(file);
    }

    @Override
    public boolean saveDatabase(ContentResolver resolver) {
        ContentValues values = new ContentValues();
        values.put(Video.Media.TITLE, mTitle);
        values.put(Video.Media.DISPLAY_NAME, mDisplayName);
        values.put(Video.Media.DATE_TAKEN, mDateTaken);
        values.put(Video.Media.MIME_TYPE, mMimeType);
        values.put(Video.Media.DATA, mPath);
        values.put(Video.Media.SIZE, mDataSize);
        // values.put(Video.Media.STEREO_TYPE, mStereoType);
        if (mLocation != null) {
            values.put(Video.Media.LATITUDE, mLocation.getLatitude());
            values.put(Video.Media.LONGITUDE, mLocation.getLongitude());
        }
        values.put(Video.Media.RESOLUTION, mWidth + "x" + mHeight);
        values.put(Video.Media.DURATION, mDuration);
        try {
            mUri = resolver.insert(Video.Media.EXTERNAL_CONTENT_URI, values);
        } catch (RuntimeException e) {
            Log.w("mediaStore", "video save database error:" + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public Thumbnail createThumbnail(int thumbWidth, int roundPixels) {
        Thumbnail t = new Thumbnail(mUri, false);
        Bitmap bitmap = BitmapUtil.createVideoImage(mPath);
        if (bitmap != null) {
            bitmap = BitmapUtil.createSquareImage(bitmap, thumbWidth,
                    roundPixels);
            Log.d(TAG, "create new video Thumbnail:" + mUri);
        }
        t.setBitmap(bitmap);
        return t;
    }

    public void broadcastNewMedia(Context context) {
        Util.broadcastNewVideo(context, mUri);
    }

}
