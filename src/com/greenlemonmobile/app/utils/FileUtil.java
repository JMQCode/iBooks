
package com.greenlemonmobile.app.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import com.greenlemonmobile.app.constant.DefaultConstant;
import com.greenlemonmobile.app.ebook.entity.FileInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class FileUtil {
	public static final String READIUM_FOLDER = ".readium/";
    public static final String ROOT_PATH = "/";
    
    private static String ANDROID_SECURE = ".android_secure";
    
    public static void DeleteDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteDirectory(child);

        fileOrDirectory.delete();
    }
    
    public static void DeleteDirectory(String fileOrDirectory) {        
        DeleteDirectory(new File(fileOrDirectory));
    }
    
    // storage, G M K B
    public static String convertStorage(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else
            return String.format("%d B", size);
    }

    public static boolean isSDCardReady() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    // if path1 contains path2
    public static boolean containsPath(String path1, String path2) {
        String path = path2;
        while (path != null) {
            if (path.equalsIgnoreCase(path1))
                return true;

            if (path.equals(ROOT_PATH))
                break;
            path = new File(path).getParent();
        }

        return false;
    }

    public static String makePath(String path1, String path2) {
        if (path1.endsWith(File.separator))
            return path1 + path2;

        return path1 + File.separator + path2;
    }

    public static String getSdDirectory() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static boolean isNormalFile(String fullName) {
        return !fullName.endsWith(ANDROID_SECURE);
    }

    public static FileInfo GetFileInfo(String filePath) {
        File lFile = new File(filePath);
        if (!lFile.exists())
            return null;

        FileInfo lFileInfo = new FileInfo();
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = FileUtil.getNameFromFilepath(filePath);
        lFileInfo.ModifiedDate = lFile.lastModified();
        lFileInfo.IsDir = lFile.isDirectory();
        lFileInfo.filePath = filePath;
        lFileInfo.fileSize = lFile.length();
        return lFileInfo;
    }

    public static FileInfo GetFileInfo(File f, FilenameFilter filter, boolean showHidden) {
        FileInfo lFileInfo = new FileInfo();
        String filePath = f.getPath();
        File lFile = new File(filePath);
        lFileInfo.canRead = lFile.canRead();
        lFileInfo.canWrite = lFile.canWrite();
        lFileInfo.isHidden = lFile.isHidden();
        lFileInfo.fileName = f.getName();
        lFileInfo.ModifiedDate = lFile.lastModified();
        lFileInfo.IsDir = lFile.isDirectory();
        lFileInfo.filePath = filePath;
        if (lFileInfo.IsDir) {
            int lCount = 0;
            File[] files = lFile.listFiles(filter);

            // null means we cannot access this dir
            if (files == null) {
                return null;
            }

            for (File child : files) {
                if ((!child.isHidden() || showHidden)
                        && FileUtil.isNormalFile(child.getAbsolutePath())) {
                    lCount++;
                }
            }
            lFileInfo.Count = lCount;

        } else {

            lFileInfo.fileSize = lFile.length();

        }
        return lFileInfo;
    }

    public static Drawable getApkIcon(Context context, String path) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            try {
                return pm.getApplicationIcon(appInfo);
            } catch (OutOfMemoryError e) {
                // Log.e(LOG_TAG, e.toString());
            }
        }
        return null;
    }

    public static String getExtFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(dotPosition + 1, filename.length());
        }
        return "";
    }

    public static String getNameFromFilename(String filename) {
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            return filename.substring(0, dotPosition);
        }
        return "";
    }

    public static String getPathFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(0, pos);
        }
        return "";
    }

    public static String getNameFromFilepath(String filepath) {
        int pos = filepath.lastIndexOf('/');
        if (pos != -1) {
            return filepath.substring(pos + 1);
        }
        return "";
    }

    // return new file path if successful, or return null
    public static String copyFile(String src, String dest) {
        File file = new File(src);
        if (!file.exists() || file.isDirectory()) {
            // Log.v(LOG_TAG, "copyFile: file not exist or is directory, " +
            // src);
            return null;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        try {
            fi = new FileInputStream(file);
            File destPlace = new File(dest);
            if (!destPlace.exists()) {
                if (!destPlace.mkdirs())
                    return null;
            }

            String destPath = FileUtil.makePath(dest, file.getName());
            File destFile = new File(destPath);
            int i = 1;
            while (destFile.exists()) {
                String destName = FileUtil.getNameFromFilename(file.getName()) + " " + i++ + "."
                        + FileUtil.getExtFromFilename(file.getName());
                destPath = FileUtil.makePath(dest, destName);
                destFile = new File(destPath);
            }

            if (!destFile.createNewFile())
                return null;

            fo = new FileOutputStream(destFile);
            int count = DefaultConstant.DEFAULT_BUFFER_SIZE;
            byte[] buffer = new byte[count];
            int read = 0;
            while ((read = fi.read(buffer, 0, count)) != -1) {
                fo.write(buffer, 0, read);
            }

            // TODO: set access privilege

            return destPath;
        } catch (FileNotFoundException e) {
            // Log.e(LOG_TAG, "copyFile: file not found, " + src);
            e.printStackTrace();
        } catch (IOException e) {
            // Log.e(LOG_TAG, "copyFile: " + e.toString());
        } finally {
            try {
                if (fi != null)
                    fi.close();
                if (fo != null)
                    fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static boolean shouldShowFile(String path) {
        return shouldShowFile(new File(path));
    }

    public static boolean shouldShowFile(File file) {
        // if (file.isHidden())
        // return false;

        // if (file.getName().startsWith("."))
        // return false;

        return true;
    }

    public static boolean setText(View view, int id, String text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    public static boolean setText(View view, int id, int text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return false;

        textView.setText(text);
        return true;
    }

    // comma separated number
    public static String convertNumber(long number) {
        return String.format("%,d", number);
    }

    public static class SDCardInfo {
        public long total;

        public long free;
    }

    public static SDCardInfo getSDCardInfo() {
        String sDcString = android.os.Environment.getExternalStorageState();

        if (sDcString.equals(android.os.Environment.MEDIA_MOUNTED)) {
            File pathFile = android.os.Environment.getExternalStorageDirectory();

            try {
                android.os.StatFs statfs = new android.os.StatFs(pathFile.getPath());
                long nTotalBlocks = statfs.getBlockCount();
                long nBlocSize = statfs.getBlockSize();

                long nAvailaBlock = statfs.getAvailableBlocks();

                long nFreeBlock = statfs.getFreeBlocks();

                SDCardInfo info = new SDCardInfo();
                info.total = nTotalBlocks * nBlocSize;
                info.free = nAvailaBlock * nBlocSize;

                return info;
            } catch (IllegalArgumentException e) {
                // Log.e(LOG_TAG, e.toString());
            }
        }

        return null;
    }

    public static void showNotification(Context context, Intent intent,
            String title, String body, int drawableId) {
        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(drawableId, body,
                System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notification.defaults = Notification.DEFAULT_SOUND;
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_ONE_SHOT);
        notification.setLatestEventInfo(context, title, body, contentIntent);
        manager.notify(drawableId, notification);
    }

    public static String formatDateString(Context context, long time) {
        DateFormat dateFormat = android.text.format.DateFormat
                .getDateFormat(context);
        DateFormat timeFormat = android.text.format.DateFormat
                .getTimeFormat(context);
        Date date = new Date(time);
        return dateFormat.format(date) + " " + timeFormat.format(date);
    }
}
