package com.ckt.basiccamera.logic;

import java.io.File;

import com.ckt.basiccamera.util.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.location.Location;
import android.net.Uri;

/* 
 /**
 * @author qianghua.song
 * all save request must extends this to do media save action
 */
public abstract class SaveRequest {
    protected final String TAG = getClass().getSimpleName();
    public static final String TYPE_JPEG = ".jpg";
    public static final String TYPE_RAW = ".raw";
    public static final String TYPE_3GP = ".3gp";
    public static final String TYPE_MPEG_4 = ".mp4";
    public static final String MIMETYPE_JPEG = "image/jpeg";
    public static final String MIMETYPE_RAW = "image/raw";
    public static final String MIMETYPE_3GP = "video/3gpp";
    public static final String MIMETYPE_MPEG_4 = "video/mp4";
    protected int mFileType;
    /* media taken time in mills */
    protected long mDateTaken;
    protected String mTitle;
    protected String mSuffix;
    protected String mDisplayName;
    /*
     * file mimetype "video/mp4"; "video/3gpp";"image/x-jps";"image/jpeg";
     * "image/mpo";
     */
    protected String mMimeType;
    /* actually file path */
    protected String mPath;
    /* add a '.' to mPath, to save tmp data */
    protected String mTmpPath; //
    protected Location mLocation;
    protected int mWidth;
    protected int mHeight;
    protected int mOrientation;
    protected int mCaptureRotation;
    protected byte[] mData;
    protected long mDataSize;
    protected Uri mUri;
    protected ContentResolver mResolver;

    /* flag to control database saving */
    protected boolean mIgnoreSaveDatabase;

    public SaveRequest() {
        mDateTaken = System.currentTimeMillis();
    }

    public SaveRequest(ContentResolver resolver) {
        mResolver = resolver;
    }

    /**
     * must be called to prepare some basic fields of the file save request,
     */
    public abstract void prepareRequest(String dir, String title);

    /**
     * @return the result of the file save action
     */
    public abstract boolean saveRequest();

    /*
     * save media information into MediaStore.
     */
    public abstract boolean saveDatabase(ContentResolver resolver);

    /**
     * @param thumbWidth
     *            the desired thumbnail width
     * @param roundPixels
     *            the requested corner round pixcels, zero means no round
     *            corner.
     * @return desired thumbnail
     */
    public abstract Thumbnail createThumbnail(int thumbWidth, int roundPixels);

    /**
     * tell the system new media has created
     * 
     * @param context
     */
    public void broadcastNewMedia(Context context) {
        Util.broadcastNewPicture(context, mUri);
    }

    public final void setData(byte[] data) {
        mData = data;
        mDataSize = mData.length;
    }

    public final int getFileType() {
        return mFileType;
    }

    /**
     * if called, next should call prepareRequest() to prepare the request
     * 
     * @param mimeType
     *            mime type of the media file
     * @param suffix
     *            suffix of the mimeType media file
     */
    public final void setMimeTypeAndSuffix(String mimeType, String suffix) {
        mMimeType = mimeType;
        mSuffix = suffix;
    }

    public final String getMimeType() {
        return mMimeType;
    }

    public final long getDateTaken() {
        return mDateTaken;
    }

    public final String getTitle() {
        return mTitle;
    }

    /**
     * @return this media file suffix such as ".jpg",".3gp",".mp4"
     */
    public final String getSuffix() {
        return mSuffix;
    }

    public final String getDisplayName() {
        return mDisplayName;
    }

    public final String getPath() {
        return mPath;
    }

    /**
     * used to avoid use of really file
     * 
     * @return
     */
    public final String getTemporaryPath() {
        if (mTmpPath == null) {
            File f = new File(mPath);
            String name = f.getName();
            mTmpPath = f.getParent() + File.separator + "." + name;
        }
        return mTmpPath;
    }

    public final Location getLocation() {
        return mLocation;
    }

    /**
     * set location to the request
     * 
     * @param loc
     */
    public final void setLocation(Location loc) {
        mLocation = loc;
    }

    public final void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public final Uri getUri() {
        return mUri;
    }

    /**
     * used in get latest thumbnail, and set the uri to the thumbnail
     * 
     * @param uri
     */
    public final void setUri(Uri uri) {
        mUri = uri;
    }

    /**
     * flag to ignore save media info to database, true will also affect the
     * thumbnail creation
     * 
     * @param ignore
     *            true for not save database
     */
    public final void setIgnoreSaveMediaStore(boolean ignore) {
        mIgnoreSaveDatabase = ignore;
    }

    /**
     * set the same rotation with Camera.parameters
     * 
     * @param rotation
     */
    public final void setCaptureRotation(int rotation) {
        mCaptureRotation = rotation;
    }
}
