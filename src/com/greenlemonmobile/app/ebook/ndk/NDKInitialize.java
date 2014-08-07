package com.greenlemonmobile.app.ebook.ndk;

import org.ebookdroid.EBookDroidLibraryLoader;

import android.app.Activity;

public class NDKInitialize {
    static{
    	EBookDroidLibraryLoader.load();
    }
    // decode the libreadium.so to zip format and check the activity's signature
    public static native byte[] environmentDetect(Activity activity, byte[] buffers, int size, boolean isDebug);
    
    // encode the readium.zip to libreadium.so format
    public static native byte[] constructEnvironment(Activity activity, byte[] buffers, int size);
}
