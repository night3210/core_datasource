package com.pproduct.datasource.core;

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

    private static boolean mEnableLogs = true;
    private static String mFilesDir;
    private static String mLogFile;
    private static FileOutputStream fo=null;

    public static void setLogcatMinLoglevel(int logcatMinLoglevel) {
        LogUtils.logcatMinLoglevel = logcatMinLoglevel;
    }

    public static void setFileMinLoglevel(int fileMinLoglevel) {
        LogUtils.fileMinLoglevel = fileMinLoglevel;
    }

    private static int logcatMinLoglevel = Log.VERBOSE;
    private static int fileMinLoglevel = Log.VERBOSE;

    private LogUtils() {
        throw new AssertionError("non-instantiable class");
    }

    public static void logv(String message) {
        log(message, Log.VERBOSE);
    }
    public static void logi(String message) {
        log(message, Log.INFO);
    }
    public static void loge(String message) {
        log(message, Log.ERROR);
    }
    public static void log(String message, int loglevel) {
        if (!mEnableLogs)
            return;
        String fullClassName = Thread.currentThread().getStackTrace()[5]
                .getClassName();
        String className = fullClassName.substring(fullClassName
                .lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[4]
                .getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[4]
                .getLineNumber();
        String logString = className + "." + methodName + "():" + lineNumber + ": " + message;
        if(loglevel >= logcatMinLoglevel)
            Log.println(loglevel, LOG_TAG, logString);
        try {
            if(loglevel >= fileMinLoglevel)
                writeExceptionToFile(logString,loglevel);

        } catch (Exception e) {
            e.printStackTrace();
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
        mFilesDir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/data/forschoolers";
        mLogFile = mFilesDir+"/bug-reports/customlogs.txt";
        final File screenshotsDir = new File(mFilesDir);

        //noinspection ResultOfMethodCallIgnored
        boolean result=screenshotsDir.mkdirs();
        if(!result) {
            Log.e(LOG_TAG,"CANNOT CREATE LOG DIRS");
        }
        clearLogFile();
        initLogFile();
        LogUtils.loge("files dir:"+mLogFile);
    }
    public static String getLogLocation(){
        return mLogFile;
    }

    private static void initLogFile() {
        try {
            fo=new FileOutputStream(mLogFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fo = null;
        }
    }

    private static void clearLogFile() {
        File logfile = new File(mLogFile);
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
