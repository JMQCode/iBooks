
package com.greenlemonmobile.app.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.greenlemonmobile.app.ebook.CrashActivity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashHandler implements UncaughtExceptionHandler {
    
    private static CrashHandler crashHandler;
    private Context context;
    private SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private CrashHandler(Context context) {
        this.context = context;
    }

    public static synchronized CrashHandler getInstance(Context context) {
        if (crashHandler != null) {
            return crashHandler;
        } else {
            crashHandler = new CrashHandler(context);
            return crashHandler;
        }
    }

    public void uncaughtException(Thread arg0, Throwable arg1) {        
        // 1.获取当前程序的版本号. 版本的id
        String versioninfo = getVersionInfo();

        // 2.获取手机的硬件信息.
        String mobileInfo = getMobileInfo();

        // 3.把错误的堆栈信息 获取出来
        String errorinfo = getErrorInfo(arg1);

        String dateInfo = dataFormat.format(new Date());
        
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dateInfo);
        stringBuilder.append("\r\n\r\n");
        
        stringBuilder.append(versioninfo);
        stringBuilder.append("\r\n\r\n");
        
        stringBuilder.append(mobileInfo);
        stringBuilder.append("\r\n\r\n");
        
        stringBuilder.append(errorinfo);
        
        Intent intent = new Intent(context, CrashActivity.class);
        intent.putExtra("key0", stringBuilder.toString());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        
        // 干掉当前的程序
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 获取错误的信息
     * 
     * @param arg1
     * @return
     */
    private String getErrorInfo(Throwable arg1) {
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        arg1.printStackTrace(pw);
        pw.close();
        String error = writer.toString();
        return error;
    }

    /**
     * 获取手机的硬件信息
     * 
     * @return
     */
    private String getMobileInfo() {
        StringBuffer sb = new StringBuffer();
        // 通过反射获取系统的硬件信息
        try {

            Field[] fields = Build.class.getDeclaredFields();
            for (Field field : fields) {
                // 暴力反射 ,获取私有的信息
                field.setAccessible(true);
                String name = field.getName();
                String value = field.get(null).toString();
                sb.append(name + "=" + value);
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 获取手机的版本信息
     * 
     * @return
     */
    private String getVersionInfo() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return "版本号未知";
        }
    }
}
