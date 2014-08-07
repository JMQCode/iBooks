package com.greenlemonmobile.app.ebook;

import org.ebookdroid.EBookDroidApp;
import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.cache.CacheManager;
import org.ebookdroid.common.log.EmergencyHandler;
import org.ebookdroid.common.log.LogContext;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.emdev.utils.android.VMRuntimeHack;

import android.app.Application;

import com.greenlemonmobile.app.utils.CrashHandler;

public class iBooksReaderApp extends Application {

    /**
     * {@inheritDoc}
     *
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        EBookDroidApp.init(this);

        EmergencyHandler.init(this);
        LogContext.init(this);
        SettingsManager.init(this);
        CacheManager.init(this);

        VMRuntimeHack.preallocateHeap(AppSettings.current().heapPreallocate);
        
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler.getInstance(this));
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Application#onLowMemory()
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        BitmapManager.clear("on Low Memory: ");
    }
}
