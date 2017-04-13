package com.ckt.basiccamera.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.ckt.basiccamera.log.Log;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * * @author qianghua.song
 */
public class Util {
    private static final String TAG = "Util";
    private static final double ASPECT_TOLERANCE = 0.001;

    public static boolean equals(double d1, double d2) {
        return Math.abs(d1 - d2) <= ASPECT_TOLERANCE;
    }

    public static String getBucketId(String directory) {
        return String.valueOf(directory.toLowerCase().hashCode());
    }

    public static final void scanNewMediaFile(Context context, String filePath) {
        File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            Uri data = Uri.fromFile(file);
            context.sendBroadcast(new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, data));
        }
    }

    public static void broadcastNewVideo(Context context, Uri uri) {
        context.sendBroadcast(new Intent(
                Camera.ACTION_NEW_VIDEO, uri));
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(
                Camera.ACTION_NEW_PICTURE, uri));
        // Keep compatibility in older version
        context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
        case Surface.ROTATION_0:
            return 0;
        case Surface.ROTATION_90:
            return 90;
        case Surface.ROTATION_180:
            return 180;
        case Surface.ROTATION_270:
            return 270;
        }
        return 0;
    }

    /**
     * @param context
     *            Activity context
     * @param cameraId
     *            camera id
     * @return the right display orientation of the current camera
     */
    public static int getCameraDisplayOrientation(Activity context, int cameraId) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = context.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
        case Surface.ROTATION_0:
            degrees = 0;
            break;
        case Surface.ROTATION_90:
            degrees = 90;
            break;
        case Surface.ROTATION_180:
            degrees = 180;
            break;
        case Surface.ROTATION_270:
            degrees = 270;
            break;
        }
        int result;
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /**
     * @param cameraId
     *            current used camera id;
     * @param orientation
     *            current orientation of the device;
     * @return the right orientation to set to capture file
     */
    public static int getCaptureRotation(int cameraId, int orientation) {
        // See android.hardware.Camera.Parameters.setRotation for
        // documentation.
        int rotation = 0;
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - orientation + 360) % 360;
            } else { // back-facing camera
                rotation = (info.orientation + orientation) % 360;
            }
        } else {
            // Get the right original orientation
            rotation = info.orientation;
        }
        return rotation;
    }

    public static long getVideoDuration(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            return Long
                    .valueOf(retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        } catch (IllegalArgumentException e) {
            return -1;
        } finally {
            retriever.release();
        }
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror,
            int displayOrientation, int viewWidth, int viewHeight) {
        if (true) {
            android.util.Log.v(TAG, "prepareMatrix mirror =" + mirror
                    + " displayOrientation=" + displayOrientation
                    + " viewWidth=" + viewWidth + " viewHeight=" + viewHeight);
        }
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

    public static void dumpRect(RectF rect, String msg) {
        android.util.Log.v(TAG, msg + "=(" + rect.left + "," + rect.top + ","
                + rect.right + "," + rect.bottom + ")");
    }

    public static boolean isFrontCamera(int cameraId) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            return true;
        }
        return false;
    }

    public static void setGpsParameters(Parameters parameters, Location loc) {
        // Clear previous GPS location from the parameters.
        parameters.removeGpsData();
        // We always encode GpsTimeStamp
        parameters.setGpsTimestamp(System.currentTimeMillis() / 1000);
        // Set GPS location.
        if (loc != null) {
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

            if (hasLatLon) {
                Log.d(TAG, "Set gps location");
                parameters.setGpsLatitude(lat);
                parameters.setGpsLongitude(lon);
                parameters.setGpsProcessingMethod(loc.getProvider()
                        .toUpperCase());
                if (loc.hasAltitude()) {
                    parameters.setGpsAltitude(loc.getAltitude());
                } else {
                    // for NETWORK_PROVIDER location provider, we may have
                    // no altitude information, but the driver needs it, so
                    // we fake one.
                    parameters.setGpsAltitude(0);
                }
                if (loc.getTime() != 0) {
                    // Location.getTime() is UTC in milliseconds.
                    // gps-timestamp is UTC in seconds.
                    long utcTimeSeconds = loc.getTime() / 1000;
                    parameters.setGpsTimestamp(utcTimeSeconds);
                }
            } else {
                loc = null;
            }
        }
    }

    public static void printListInt(List<int[]> l) {
        Log.v(TAG, "printListInt ENTER");
        for (int[] ilist : l) {
            for (int i : ilist) {
                Log.v(TAG, ilist.toString() + ":" + i);
            }
        }
        Log.v(TAG, "printListInt ENTER");
    }

    public static Size getOptimalPreviewSize(Activity currentActivity,
            List<Size> sizes, double targetRatio, boolean findMinalRatio) {
        if (sizes == null) {
            return null;
        }

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        Display display = currentActivity.getWindowManager()
                .getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int targetHeight = Math.min(point.x, point.y);

        if (findMinalRatio) {
            // Find minimal aspect ratio for that: special video size maybe not
            // have the mapping preview size.
            double minAspectio = Double.MAX_VALUE;
            for (Size size : sizes) {
                double aspectRatio = (double) size.width / size.height;
                if (Math.abs(aspectRatio - targetRatio) <= Math.abs(minAspectio
                        - targetRatio)) {
                    minAspectio = aspectRatio;
                }
            }

            Log.v(TAG, "getOptimalPreviewSize(" + targetRatio
                    + ") minAspectio=" + minAspectio);

            targetRatio = minAspectio;
        }

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (!equals(ratio, targetRatio)) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        // / M: This will happen when native return video size and wallpaper
        // want to get specified ratio.
        if (optimalSize == null) {
            Log.w(TAG, "No preview size match the aspect ratio" + targetRatio);
            /*
             * minDiff = Double.MAX_VALUE; for (Size size : sizes) { if
             * (Math.abs(size.height - targetHeight) < minDiff) { optimalSize =
             * size; minDiff = Math.abs(size.height - targetHeight); } }
             */
        }
        return optimalSize;
    }

    public static String MappingLevelToStringList(float level,
            List<String> supported) {
        Log.v(TAG, "MappingLevelToStringList:" + level);
        int size = supported.size();
        if (supported == null || size == 0) {
            return null;
        }
        float range = 1.0f / size;
        int index = 0;
        for (; index < size; index++) {
            if ((index + 1) * range > level) {
                return supported.get(index);
            }
        }

        return supported.get(size - 1);
    }

    public static int MappingLevelToIntList(float level, List<Integer> supported) {
        Log.v(TAG, "MappingLevelToIntList:" + level);
        int size = supported.size();
        if (supported == null || size == 0) {
            return 0;
        }
        float range = 1.0f / size;
        int index = 0;
        for (; index < size; index++) {
            if ((index + 1) * range > level) {
                return supported.get(index);
            }
        }
        return supported.get(size - 1);
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static String formatTime(long millis, boolean showMillis) {
        final int totalSeconds = (int) millis / 1000;
        final int millionSeconds = (int) (millis % 1000) / 10;
        final int seconds = totalSeconds % 60;
        final int minutes = (totalSeconds / 60) % 60;
        final int hours = totalSeconds / 3600;
        String text = null;
        if (showMillis) {
            if (hours > 0) {
                text = String.format(Locale.ENGLISH, "%d:%02d:%02d.%02d",
                        hours, minutes, seconds, millionSeconds);
            } else {
                text = String.format(Locale.ENGLISH, "%02d:%02d.%02d", minutes,
                        seconds, millionSeconds);
            }
        } else {
            if (hours > 0) {
                text = String.format(Locale.ENGLISH, "%d:%02d:%02d", hours,
                        minutes, seconds);
            } else {
                text = String.format(Locale.ENGLISH, "%02d:%02d", minutes,
                        seconds);
            }
        }
        return text;
    }

    public static Size getMaxSize(List<Size> values) {
        if (values == null | values.size() == 0) {
            return null;
        }
        Collections.sort(values, new SizeCompartor());
        return values.get(0);
    }

    public static class SizeCompartor<T extends Size> implements Comparator<T> {
        @Override
        public int compare(T l, T r) {
            return r.width * r.height - l.width * l.height;
        }

    }
}
