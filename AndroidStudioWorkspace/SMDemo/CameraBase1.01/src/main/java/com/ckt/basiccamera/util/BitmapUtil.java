package com.ckt.basiccamera.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;

import com.ckt.basiccamera.log.Log;

/**
 * * @author qianghua.song
 */
public class BitmapUtil {
    private static final String TAG = "BitmapUtil";

    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * @param data
     *            origin image data
     * @param originWidth
     *            origin image width
     * @param originHeight
     *            origin image height
     * @param desiredWidth
     *            desired bitmap width, must smaller than input width;
     * @return desired bitmap created by subset of the data
     */
    public static Bitmap createImage(byte[] data, int originWidth,
            int originHeight, int desiredWidth) {
        int width = Math.min(originWidth, originHeight);
        int ratio = (int) Math.ceil((double) width / desiredWidth);
        int inSampleSize = Integer.highestOneBit(ratio);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
                    options);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, e.getMessage());
        }
        return bitmap;
    }

    /**
     * @param path
     *            video media file path
     * @return this first frame of the video
     */
    public static Bitmap createVideoImage(String path) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            bitmap = retriever.getFrameAtTime(0);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }
        return bitmap;
    }

    /**
     * @param input
     *            input bitmap, after the method called, the input already
     *            recycled
     * @param desiredWidth
     *            desired bitmap width, must smaller than input width;
     * @param roundPixels
     *            the requested corner round pixcels, zero means no round
     *            corner.
     * @return this desired bitmap;
     */
    public static Bitmap createSquareImage(Bitmap input, int desiredWidth,
            int roundPixels) {
        if (input == null) {
            return null;
        }
        int min = Math.min(input.getWidth(), input.getHeight());
        if (desiredWidth > min) {
            return input;
        }
        Bitmap bitmap = null;
        if (desiredWidth <= min) {
            bitmap = Bitmap.createBitmap(input,
                    (input.getWidth() - desiredWidth) >> 1,
                    (input.getHeight() - desiredWidth) >> 1, desiredWidth,
                    desiredWidth);
            if (bitmap != input) {
                recycleBitmap(input);
            }
        }
        if (roundPixels > 0 && roundPixels <= min >> 1 && bitmap != null) {
            Bitmap dst = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(dst);
            Paint paint = new Paint();
            Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            RectF rectF = new RectF(rect);
            paint.setAntiAlias(true);
            canvas.drawRoundRect(rectF, roundPixels, roundPixels, paint);
            paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
            canvas.drawBitmap(bitmap, null, rect, paint);
            recycleBitmap(bitmap);
            bitmap = dst;
        }
        return bitmap;
    }

    public static Bitmap rotateImage(Bitmap bitmap, int orientation) {
        if (orientation != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(orientation, bitmap.getWidth() * 0.5f,
                    bitmap.getHeight() * 0.5f);
            try {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (rotated != bitmap) {
                    recycleBitmap(bitmap);
                }
                return rotated;
            } catch (Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        }
        return bitmap;
    }

    public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory
                    .decodeByteArray(jpegData, 0, jpegData.length, options);
            if (options.mCancel || options.outWidth == -1
                    || options.outHeight == -1) {
                return null;
            }
            options.inSampleSize = computeSampleSize(options, -1,
                    maxNumOfPixels);
            options.inJustDecodeBounds = false;

            options.inDither = false;
            options.inPreferredConfig = Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length,
                    options);
        } catch (OutOfMemoryError ex) {
            Log.d(TAG, ex.getMessage());
            return null;
        }
    }

    public static int computeSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels);

        int roundedSize;
        if (initialSize <= 8) {
            roundedSize = 1;
            while (roundedSize < initialSize) {
                roundedSize <<= 1;
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8;
        }

        return roundedSize;
    }

    private static int computeInitialSampleSize(BitmapFactory.Options options,
            int minSideLength, int maxNumOfPixels) {
        double w = options.outWidth;
        double h = options.outHeight;

        int lowerBound = (maxNumOfPixels < 0) ? 1 : (int) Math.ceil(Math.sqrt(w
                * h / maxNumOfPixels));
        int upperBound = (minSideLength < 0) ? 128 : (int) Math.min(
                Math.floor(w / minSideLength), Math.floor(h / minSideLength));

        if (upperBound < lowerBound) {
            return lowerBound;
        }

        if (maxNumOfPixels < 0 && minSideLength < 0) {
            return 1;
        } else if (minSideLength < 0) {
            return lowerBound;
        } else {
            return upperBound;
        }
    }
}
