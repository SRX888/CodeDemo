package com.ckt.basiccamera.logic;

import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Class to handle thumbnail
 * 
 * @author qianghua.song
 * 
 */
public class Thumbnail {
    private Uri mUri;
    private String mPath;
    private String mMimeType;
    private Bitmap mBitmap;
    private boolean mFromFile = false;
    private boolean mVideo;

    public Thumbnail(Uri uri, boolean isVideo) {
        mUri = uri;
        mVideo = isVideo;
    }

    public Thumbnail(String path, boolean isVideo) {
        mPath = path;
        mVideo = isVideo;
        mFromFile = true;
    }

    public boolean isValid() {
        return mBitmap != null && (mUri != null || mPath != null);
    }

    public void setMimeType(String mimetype) {
        mMimeType = mimetype;
    }

    public final String getMimeType() {
        return mMimeType;
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setVideoThumb(boolean isVideo) {
        mVideo = isVideo;
    }

    public boolean isVideoThumb() {
        return mVideo;
    }

    public boolean isFromFile() {
        return mFromFile;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public Uri getUri() {
        return mUri;
    }
}
