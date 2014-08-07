package com.greenlemonmobile.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.HashMap;

public class ImageBuffer {
    
    private static HashMap<String, SoftReference<Bitmap>> sImageBuffers;
    
    static {
        sImageBuffers = new HashMap<String, SoftReference<Bitmap>>();
    }
    
    public static void shutDown() {
        sImageBuffers.clear();
    }
    
    public static Bitmap getBitmap(Context context, String imagePath) {
        Bitmap bitmap = null;
        
        if (!TextUtils.isEmpty(imagePath)) {
        	File file = new File(imagePath);
        	if (!file.exists() || !file.canRead())
        		return null;
            String hashKey = Md5Encrypt.md5(imagePath);
            
            if (sImageBuffers.containsKey(hashKey)) {
               SoftReference<Bitmap> reference = sImageBuffers.get(hashKey);
               bitmap = reference.get();
            }
            
            if (bitmap == null)
            {                
                try {
                    bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        SoftReference<Bitmap> reference = new SoftReference<Bitmap>(bitmap);
                        sImageBuffers.put(hashKey, reference);
                    }
                } catch (OutOfMemoryError error) {
                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        return bitmap;
    }
}
