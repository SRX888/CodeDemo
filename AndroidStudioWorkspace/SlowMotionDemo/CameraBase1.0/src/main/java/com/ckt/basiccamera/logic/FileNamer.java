package com.ckt.basiccamera.logic;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;

public class FileNamer {
    private SimpleDateFormat mFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    protected String IMG_PREFIX = "IMG_";
    protected String VID_PREFIX = "VID_";

    public String getMediaPath() {
        return Environment.getExternalStorageDirectory()
                .toString();
    }

    public String getPhotoPath() {
        return Environment.getExternalStorageDirectory()
                .toString() + File.separator + "Photo";
    }

    public String getVideoPath() {
        return Environment.getExternalStorageDirectory()
                .toString() + File.separator + "Video";
    }

    public String getPhotoName(long dateTaken) {
        String name = IMG_PREFIX + mFormat.format(new Date(dateTaken));
        return name;
    }

    public String getVideoName(long dateTaken, int frameRate) {
        String name = VID_PREFIX + mFormat.format(new Date(dateTaken));
        name += "_" + frameRate + "fps";
        return name;
    }
}
