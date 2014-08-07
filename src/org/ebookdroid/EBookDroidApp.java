package org.ebookdroid;

import java.io.File;

import org.ebookdroid.common.log.LogContext;
import org.emdev.utils.FileUtils;
import org.emdev.utils.android.AndroidVersion;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;

public class EBookDroidApp {

    public static final LogContext LCTX = LogContext.ROOT;

    public static Context context;

    public static String APP_VERSION;

    public static String APP_PACKAGE;

    public static File EXT_STORAGE;

    public static void init(Context context1) {
        context = context1.getApplicationContext();

        final PackageManager pm = context1.getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(context1.getPackageName(), 0);
            APP_VERSION = pi.versionName;
            APP_PACKAGE = pi.packageName;
            EXT_STORAGE = Environment.getExternalStorageDirectory();

            LCTX.i(context1.getString(pi.applicationInfo.labelRes) + " (" + APP_PACKAGE + ")" + " v" + APP_VERSION + "("
                    + pi.versionCode + ")");

            LCTX.i("Root             dir: " + Environment.getRootDirectory());
            LCTX.i("Data             dir: " + Environment.getDataDirectory());
            LCTX.i("External storage dir: " + Environment.getExternalStorageDirectory());
            LCTX.i("Files            dir: " + FileUtils.getAbsolutePath(context1.getFilesDir()));
            LCTX.i("Cache            dir: " + FileUtils.getAbsolutePath(context1.getCacheDir()));
            // LCTX.i("External cache   dir: " + getExternalCacheDir().getAbsolutePath());

            LCTX.i("VERSION     : " + AndroidVersion.VERSION);
            LCTX.i("BOARD       : " + Build.BOARD);
            LCTX.i("BRAND       : " + Build.BRAND);
            // LCTX.i("CPU_ABI     : " + Build.CPU_ABI);
            // LCTX.i("CPU_ABI2    : " + Build.CPU_ABI2);
            LCTX.i("DEVICE      : " + Build.DEVICE);
            LCTX.i("DISPLAY     : " + Build.DISPLAY);
            LCTX.i("FINGERPRINT : " + Build.FINGERPRINT);
            // LCTX.i("HARDWARE    : " + Build.HARDWARE);
            LCTX.i("ID          : " + Build.ID);
            // LCTX.i("MANUFACTURER: " + Build.MANUFACTURER);
            LCTX.i("MODEL       : " + Build.MODEL);
            LCTX.i("PRODUCT     : " + Build.PRODUCT);

        } catch (final NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
