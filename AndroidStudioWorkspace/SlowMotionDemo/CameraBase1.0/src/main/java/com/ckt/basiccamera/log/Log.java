package com.ckt.basiccamera.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.os.Environment;

/**
 * * @author qianghua.song
 */
public class Log {
    public static final byte LOG_SYSTEM = 1;
    public static final byte LOG_FILE = 2;
    public static final byte LOG_ALL = LOG_SYSTEM | LOG_FILE;

    public static final byte LEVEL_V = 2;
    public static final byte LEVEL_D = 3;
    public static final byte LEVEL_I = 4;
    public static final byte LEVEL_W = 5;
    public static final byte LEVEL_E = 6;
    public static final char[] LEVEL_CHAR = { '0', '0', 'v', 'd', 'i', 'w', 'e' };

    private static String SDCARD_DIR = Environment
            .getExternalStorageDirectory().getPath() + File.separator;
    private static String LOG_DIR = SDCARD_DIR + "log" + File.separator;
    static {
        File file = new File(LOG_DIR);
        if (!file.exists()) {
            file.mkdirs();
            if (!file.exists()) {
                LOG_DIR = LOG_DIR.replace("sdcard2", "sdcard");
                file.mkdirs();
            }
        }
    }

    private static final String LOG_PRE = "Log_";
    private static final String LOG_POSTPRE = ".txt";
    private static SimpleDateFormat msgformat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd");

    private static boolean log_system = true;
    private static boolean log_file = false;
    private static byte log_level = 1;

    public static void enableLog() {
        deleteLog(7);
        setLogType(LOG_ALL);
        filterLog(LEVEL_V);
    }

    public static void disableLog() {
        log_system = false;
        log_file = false;
    }

    public static void filterLog(byte level) {
        log_level = level;
    }

    public static void setLogType(byte type) {
        log_system = (type & LOG_SYSTEM) != 0;
        log_file = (type & LOG_FILE) != 0;
    }

    public static void deleteLog(int dayBefore) {
        if (dayBefore < 1) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 1);
        cal.add(Calendar.DATE, 1 - dayBefore);

        final long time = cal.getTimeInMillis();
        File file = new File(SDCARD_DIR);
        if (!file.exists()) {
            return;
        }
        File[] log = file.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.getName().startsWith(LOG_PRE)
                        && file.lastModified() < time;
            }

        });
        if (log == null || log.length == 0) {
            return;
        }
        for (File tmp : log) {
            tmp.delete();
        }
    }

    public static void v(String tag, Object msg) {
        log(LEVEL_V, tag, "obj:" + msg);
    }

    public static void v(String tag, String msg) {
        log(LEVEL_V, tag, msg);
    }

    public static void d(String tag, Object msg) {
        log(LEVEL_D, tag, "obj:" + msg);
    }

    public static void d(String tag, String msg) {
        log(LEVEL_D, tag, msg);
    }

    public static void i(String tag, Object msg) {
        log(LEVEL_I, tag, "obj:" + msg);
    }

    public static void i(String tag, String msg) {
        log(LEVEL_I, tag, msg);
    }

    public static void w(String tag, Object msg) {
        log(LEVEL_W, tag, "obj:" + msg);
    }

    public static void w(String tag, String msg) {
        log(LEVEL_W, tag, msg);
    }

    public static void e(String tag, Object msg) {
        log(LEVEL_E, tag, "obj:" + msg);
    }

    public static void e(String tag, String msg) {
        log(LEVEL_E, tag, msg);
    }

    public static void v(String tag, String msg, Throwable tr) {
        log(LEVEL_V, tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
    }

    public static void d(String tag, String msg, Throwable tr) {
        log(LEVEL_D, tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
    }

    public static void i(String tag, String msg, Throwable tr) {
        log(LEVEL_I, tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
    }

    public static void w(String tag, String msg, Throwable tr) {
        log(LEVEL_W, tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
    }

    public static void e(String tag, String msg, Throwable tr) {
        log(LEVEL_E, tag, msg + '\n' + android.util.Log.getStackTraceString(tr));
    }

    private static void log(int level, String tag, String msg) {
        // tag = Thread.currentThread().getName() + " ASURALOG " + tag;
        if (log_system) {
            switch (level) {
            case LEVEL_E:
                android.util.Log.e(tag, msg);
                break;
            case LEVEL_W:
                android.util.Log.w(tag, msg);
                break;
            case LEVEL_I:
                android.util.Log.i(tag, msg);
                break;
            case LEVEL_D:
                android.util.Log.d(tag, msg);
                break;
            case LEVEL_V:
                android.util.Log.v(tag, msg);
                break;
            }
        }

        if (log_file && level >= log_level) {
            writeLog(level, tag, msg);
        }
    }

    private static void writeLog(int level, String tag, String text) {
        Date nowtime = new Date();
        String needWriteMessage = msgformat.format(nowtime) + " "
                + LEVEL_CHAR[level] + " " + tag + ":" + text;
        File file = new File(LOG_DIR, LOG_PRE + logfile.format(nowtime)
                + LOG_POSTPRE);
        try {
            FileWriter filerWriter = new FileWriter(file, true);
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);
            bufWriter.newLine();
            bufWriter.flush();
            bufWriter.close();
            filerWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}