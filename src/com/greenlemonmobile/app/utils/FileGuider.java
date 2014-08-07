
package com.greenlemonmobile.app.utils;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

public class FileGuider {// TODO

    public static int SPACE_ONLY_INTERNAL = 0;// 浼樺厛閫夋嫨鍐呴儴瀛樺偍绌洪棿鐒跺悗澶栭儴瀛樺偍绌洪棿
    public static int SPACE_ONLY_EXTERNAL = 1;// 浼樺厛閫夋嫨澶栭儴瀛樺偍绌洪棿鐒跺悗鍐呴儴瀛樺偍绌洪棿
    public static int SPACE_PRIORITY_INTERNAL = 2;// 浼樺厛閫夋嫨鍐呴儴瀛樺偍绌洪棿鎴栧閮ㄥ瓨鍌ㄧ┖闂�
    public static int SPACE_PRIORITY_EXTERNAL = 3;
    
    private static String applicationDir = "/.iBooks/";

    private int space;// 浼樺厛閫夋嫨鍐呴儴瀛樺偍绌洪棿鎴栧閮ㄥ瓨鍌ㄧ┖闂�
    private boolean immutable;// 褰撲紭鍏堥�鎷╃殑瀛樺偍绌洪棿涓嶅瓨鍦ㄦ垨鑰呯┖闂翠笉瓒虫椂鍙惁閫夋嫨鍙︿竴瀛樺偍绌洪棿
    private long TotalSize;// 鎬荤┖闂�
    private long AvailableSize;// 鍙敤绌洪棿
    private String childDirName;// 瀛愮洰褰�
    private String fileName;// 鏂囦欢鍚�
    private int mode;// 鏉冮檺
    private int internalType;// 鍐呴儴瀛樺偍绌洪棿绫诲瀷
    private File root;

    private Context context;

    public int getSpace() {
        return space;
    }

    public FileGuider(Context context, int space) {
        this.space = space;        
        this.context = context;
        root = getRoot();
    }

    public void setSpace(int space) {
        this.space = space;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    public long getTotalSize() {
        return TotalSize;
    }

    public void setTotalSize(long totalSize) {
        TotalSize = totalSize;
    }

    public long getAvailableSize() {
        return AvailableSize;
    }

    public void setAvailableSize(long availableSize) {
        AvailableSize = availableSize;
    }

    public String getChildDirName() {
        return childDirName;
    }

    public void setChildDirName(String childDirName) {
        this.childDirName = childDirName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getInternalType() {
        return internalType;
    }

    public String getParentPath() throws IOException {
        if (childDirName == null) {
            return root.getAbsolutePath() + applicationDir;
        }
        return root.getAbsolutePath() + applicationDir + childDirName;
    }

    public void checkParentPath() throws IOException {
        File f = new File(getParentPath());
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public File getRoot() {
        File root = null;
        long availableSize = getAvailableSize();
        if (SPACE_ONLY_INTERNAL == space) {
            root = context.getFilesDir();
        } else if (SPACE_ONLY_EXTERNAL == space) {
            root = Environment.getExternalStorageDirectory();
        } else if (SPACE_PRIORITY_INTERNAL == space) {
            if (getAvailableInternalMemorySize() > availableSize // 鍐呴儴瀛樺偍绌洪棿瓒冲
            ) {
                root = context.getFilesDir();
            } else if (externalMemoryAvailable()
                    && getAvailableExternalMemorySize() > availableSize) {
                root = Environment.getExternalStorageDirectory();
            }
        } else if (SPACE_PRIORITY_EXTERNAL == space) {
            if (externalMemoryAvailable()
                    && getAvailableExternalMemorySize() > availableSize) {
                root = Environment.getExternalStorageDirectory();
            } else if (getAvailableInternalMemorySize() > availableSize) {// 鍐呴儴瀛樺偍绌洪棿瓒冲
                root = context.getFilesDir();
            }
        }
        return root;
    }

    public String getFilePath() {
        try {
            checkParentPath();
            String path = getParentPath() + (TextUtils.isEmpty(fileName) ? "" : "/" + getFileName());
            return path;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public File getFile() {
        File file = new File(getFilePath());
        return file;
    }

    public void setInternalType(int internalType) {
        this.internalType = internalType;
    }

    public static boolean isReady() {
        return externalMemoryAvailable();
    }

    public static boolean externalMemoryAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static long getAvailableInternalMemorySize() {

        File path = Environment.getDataDirectory();

        StatFs stat = new StatFs(path.getPath());

        long blockSize = stat.getBlockSize();

        long availableBlocks = stat.getAvailableBlocks();

        return availableBlocks * blockSize;

    }

    public static long getAvailableExternalMemorySize() {

        if (externalMemoryAvailable()) {

            File path = Environment.getExternalStorageDirectory();

            StatFs stat = new StatFs(path.getPath());

            long blockSize = stat.getBlockSize();

            long availableBlocks = stat.getAvailableBlocks();

            return availableBlocks * blockSize;

        } else {

            return 0;

        }

    }
}
