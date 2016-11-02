package com.night3210.datasource.core;

/**
 * Created by Developer on 2/11/2016.
 */

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class print logs to logcat
 * @author Denys Kravchenko
 *
 */
public final class LogUtils {
    private static String LOG_TAG = "datasource";

    private static boolean enableLogs = true;
    private static String filesDir;
    private static String logFile;
    private static FileOutputStream fo=null;
    private static int logcatMinLoglevel = Log.VERBOSE;
    private static int fileMinLoglevel = Log.VERBOSE;

    public static void setLogcatMinLoglevel(int logcatMinLoglevel) {
        LogUtils.logcatMinLoglevel = logcatMinLoglevel;
    }
    public static void setFileMinLoglevel(int fileMinLoglevel) {
        LogUtils.fileMinLoglevel = fileMinLoglevel;
    }
    private LogUtils() {
        throw new AssertionError("non-instantiable class");
    }
    public static void logv(String message) {
        log(message, Log.VERBOSE);
    }
    public static void logi(String message) {
        log(message, Log.INFO);
    }
    public static void logw(String message) {
        log(message, Log.WARN);
    }
    public static void loge(String message) {
        log(message, Log.ERROR);
    }
    public static void log(String message, int loglevel) {
        if (!enableLogs)
            return;
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if(trace.length<5) {
            new Exception().printStackTrace();
            throw new RuntimeException("Too small stacktrace for log");
        }
        String fullClassName = trace[5].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = trace[4].getMethodName();
        int lineNumber = trace[4].getLineNumber();

        String logString = Thread.currentThread().getName()
                + " thread, "+className + "." + methodName
                + "():" + lineNumber + ": " + message;

        if(loglevel >= logcatMinLoglevel)
            Log.println(loglevel, LOG_TAG, logString);
        try {
            if(loglevel >= fileMinLoglevel)
                writeExceptionToFile(logString,loglevel);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
    private static void writeExceptionToFile(String logString, int loglevel) throws IOException {
        String logLevelStr = "v";
        String endstring = "\r\n";
        switch (loglevel) {
            case Log.VERBOSE: logLevelStr="v";break;
            case Log.INFO: logLevelStr="i";break;
            case Log.ERROR: logLevelStr="e";break;
            default:break;
        }
        fo.write(logLevelStr.getBytes());
        fo.write(logString.getBytes());
        fo.write(endstring.getBytes());
    }

    public static void printStackTrace(Exception e) {
        e.printStackTrace();
    }
    public static void initialize(Application app) {
        filesDir = Environment.getDataDirectory()+"/logs/";
        logFile = filesDir + "customlogs.txt";
        final File screenshotsDir = new File(filesDir);
        boolean result=screenshotsDir.mkdirs();
        if(!result) {
            Log.e(LOG_TAG, "CANNOT CREATE LOG DIRS "+filesDir);
        }
        clearLogFile();
        initLogFile();
        LogUtils.logi("files dir:"+ logFile);
    }
    public static String getLogLocation(){
        return logFile;
    }

    private static void initLogFile() {
        try {
            fo=new FileOutputStream(logFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fo = null;
        }
    }
    private static void clearLogFile() {
        File logfile = new File(logFile);
        if(logfile.exists())
            logfile.delete();
    }
    public static void finish() {
        if(fo==null)
            return;
        try {
            fo.flush();
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
