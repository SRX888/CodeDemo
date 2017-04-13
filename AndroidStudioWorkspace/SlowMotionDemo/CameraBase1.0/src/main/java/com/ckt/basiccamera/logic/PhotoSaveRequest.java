package com.ckt.basiccamera.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.ckt.basiccamera.util.BitmapUtil;
import com.ckt.basiccamera.util.Exif;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

/* 
 /**
 * @author qianghua.song
 * save request about normal picture save
 */
public class PhotoSaveRequest extends SaveRequest {
    private boolean mIsSaveBurstPath = false;

    public PhotoSaveRequest() {
        super();
        mSuffix = TYPE_JPEG;
        mMimeType = MIMETYPE_JPEG;
    }

    public PhotoSaveRequest(long dataTaken) {
        mSuffix = TYPE_JPEG;
        mMimeType = MIMETYPE_JPEG;
    }
    public boolean isIsSaveBurstPath() {
        return mIsSaveBurstPath;
    }

    public void setIsSaveBurstPath(boolean mIsSaveBurstPath) {
        this.mIsSaveBurstPath = mIsSaveBurstPath;
    }

    public void prepareRequest(String dir, String title) {
        mTitle = title;
        mDisplayName = mTitle + mSuffix;
        File tmp = new File(dir);
        if (!tmp.exists()) {
            tmp.mkdirs();
        }
        mPath = tmp.getPath() + File.separator + mDisplayName;
        File tmpFile = new File(mPath);
        if(tmpFile.exists() ){
            int i = 0;
            int id = -1;//if it is burst photo ,the id not equals -1
            int lastId = 0;//the position of burst photo's id of title

            String orginTitle = mTitle;
            String []titles = title.split("_");
            String endString = titles[titles.length-1];
            try{
                id = Integer.parseInt(endString);
                lastId = title.lastIndexOf(endString);
            }catch (Exception e) {
                Log.e(TAG,"prepareRequest "+e.toString());
                e.printStackTrace();
                id = -1;
            }
            while (tmpFile.exists()) {
                if(id != -1){
                    mTitle = title.substring(0, lastId-1)+i+"_"+endString;
                }else{
                    mTitle = orginTitle+i;
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
        if (mData == null) {
            return false;
        }
        mOrientation = Exif.getOrientation(mData);
        if ((mCaptureRotation + mOrientation) % 180 != 0) {
            mWidth = mWidth ^ mHeight;
            mHeight = mWidth ^ mHeight;
            mWidth = mWidth ^ mHeight;
        }
        FileOutputStream out = null;
        try {
            // when take photo, it's ok to write to final path.
            out = new FileOutputStream(mPath);
            out.write(mData);
            out.flush();
            out.close();
            // new File(getTemporaryPath()).renameTo(new File(mPath));
            Log.d(TAG, "saveRequest done");
            return true;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            new File(mPath).delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }
        return false;
    }

    @Override
    public boolean saveDatabase(ContentResolver resolver) {
        if (mIgnoreSaveDatabase) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(ImageColumns.TITLE, mTitle);
        values.put(ImageColumns.DISPLAY_NAME, mDisplayName);
        values.put(ImageColumns.DATE_TAKEN, mDateTaken);
        values.put(ImageColumns.MIME_TYPE, mMimeType);
        values.put(ImageColumns.DATA, mPath);
        values.put(ImageColumns.SIZE, mDataSize);
        values.put(ImageColumns.ORIENTATION, mOrientation);
        values.put(ImageColumns.WIDTH, mWidth);
        values.put(ImageColumns.HEIGHT, mHeight);
        if (mLocation != null) {
            values.put(ImageColumns.LATITUDE, mLocation.getLatitude());
            values.put(ImageColumns.LONGITUDE, mLocation.getLongitude());
        }
        // values.put(ImageColumns.MPO_TYPE, r.mMpoType);
        // values.put(ImageColumns.STEREO_TYPE, r.mStereoType);// should be
        // rechecked
        try {
            mUri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
            Log.d(TAG, "saveDatabase:" + values.toString());
            return true;
        } catch (Throwable th) {
            // Here we keep google default, don't
            // follow check style
            // This can happen when the external volume is already mounted,
            // but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the
            // user
            // cannot click the thumbnail to review the picture.
            Log.d(TAG, th.getMessage());
        }
        return false;
    }

    public Thumbnail createThumbnail(int thumbWidth, int roundPixels) {
        Thumbnail t = new Thumbnail(mUri, false);
        Bitmap bitmap = BitmapUtil.createImage(mData, mWidth, mHeight,
                thumbWidth);
        if (bitmap != null) {
            bitmap = BitmapUtil.createSquareImage(bitmap, thumbWidth,
                    roundPixels);
            Log.d(TAG, "create new Thumbnail:" + mUri);
        }
        t.setBitmap(bitmap);
        return t;
    }

}
