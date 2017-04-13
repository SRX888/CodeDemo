package com.srx.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtil {
    public static String getFileName(boolean isPicture) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
        String datetime =  formatter.format(new Date(System.currentTimeMillis()));
        //若是图片
        if (isPicture) {
            return "IMG_" + datetime + ".jpg";
        } else {
            //若是视频
            return "VID_" + datetime + ".mp4";
        }
    }
}
