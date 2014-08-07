
package com.greenlemonmobile.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageTool {

    // 这个函数会对图片的大小进行判断，并得到合适的缩放比例，比如2即1/2,3即1/3
    static int computeSampleSize(BitmapFactory.Options options, int height,
            int width) {
        int w = options.outWidth;
        int h = options.outHeight;
        int candidate = (w * h) / (height * width);
        if (((w * h) % (height * width)) > 0) {
            candidate++;
        }
        if (candidate == 0)
            return 1;

        if (true)
            ;
        // Log.i("ImageTool", "for w/h " + w + "/" + h + " returning "
        // + candidate + "(" + (w / candidate) + " / "
        // + (h / candidate));
        return candidate;
    }

    public static Bitmap getBitmap(Context context, InputStream is,
            int height, int width) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            // 先指定原始大小
            options.inSampleSize = 1;
            // 只进行大小判断
            options.inJustDecodeBounds = true;
            // 调用此方法得到options得到图片的大小
            is.mark(0);
            BitmapFactory.decodeStream(is, null, options);
            // 我们的目标是在800pixel的画面上显示。
            // 所以需要调用computeSampleSize得到图片缩放的比例

            options.inSampleSize = computeSampleSize(options, height, width);
            // OK,我们得到了缩放的比例，现在开始正式读入BitMap数据
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            is.reset();
            // 根据options参数，减少所需要的内存
            Bitmap sourceBitmap = BitmapFactory.decodeStream(is, null, options);
            return sourceBitmap;
        } catch (OutOfMemoryError error) {
            System.gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getImageFromStream(Context context, String imagePath,
            int height, int width) {
        InputStream is;
        try {
            is = new FileInputStream(imagePath);
            return getBitmap(context, is, height, width);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getScaleImage(Bitmap bitmap, int resizedWidth,
            int resizedHeight, boolean isenlarge, boolean isScale, boolean isFill) {

        Bitmap BitmapOrg = bitmap;

        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();

        int newWidth = resizedWidth;
        int newHeight = resizedHeight;

        if (!isenlarge) {
            if (newWidth > width && newHeight > height) {
                return bitmap;
            }
        }

        // calculate the scale
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Log.i("ImageTool", "width==" + width);
        Log.i("ImageTool", "height==" + height);
        Log.i("ImageTool", "resizedWidth==" + resizedWidth);
        Log.i("ImageTool", "resizedHeight==" + resizedHeight);
        Log.i("ImageTool", "scaleWidth0==" + scaleWidth);
        Log.i("ImageTool", "scaleHeight0==" + scaleHeight);
        if (!isenlarge) {
            if (isScale) {
                scaleWidth = scaleWidth < scaleHeight ? scaleWidth
                        : scaleHeight;
                scaleHeight = scaleWidth;
            }
        } else {
            if (isScale) {
                scaleWidth = scaleWidth > scaleHeight ? scaleWidth
                        : scaleHeight;
                scaleHeight = scaleWidth;
            }
        }
        Log.i("ImageTool", "scaleWidth1==" + scaleWidth);
        Log.i("ImageTool", "scaleHeight1==" + scaleHeight);

        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the Bitmap
        matrix.postScale(scaleWidth, scaleHeight);
        // if you want to rotate the Bitmap
        // matrix.postRotate(45);

        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
        if (isFill) {
            int ShearWidth = 0;
            int ShearHeight = 0;
            if (resizedBitmap.getWidth() > resizedWidth) {
                ShearWidth = resizedBitmap.getWidth() - resizedWidth;
            }
            if (resizedBitmap.getHeight() > resizedHeight) {
                ShearHeight = resizedBitmap.getHeight() - resizedHeight;
            }
            Bitmap tempBmp = Bitmap.createBitmap(resizedBitmap, ShearWidth / 2, ShearHeight / 2,
                    resizedBitmap.getWidth() - ShearWidth, resizedBitmap.getHeight() - ShearHeight);
            resizedBitmap.recycle();
            resizedBitmap = tempBmp;
            Log.i("ImageTool", "scaleWidth2==" + resizedBitmap.getWidth());
            Log.i("ImageTool", "scaleHeight3==" + resizedBitmap.getHeight());
        }
        // make a Drawable from Bitmap to allow to set the Bitmap
        // to the ImageView, ImageButton or what ever
        return resizedBitmap;
    }

    /**
     * 保存文件
     * 
     * @param bm
     * @param fileName
     * @throws IOException
     */
    public static boolean saveFile(Bitmap bm, String filePath) {
        try {
            File myCaptureFile = new File(filePath);
            if (!myCaptureFile.exists()) {
                myCaptureFile.createNewFile();
            }
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
            bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            bos.flush();
            bos.close();
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 1.放大缩小图片
     * 
     * @param bitmap
     * @param w
     * @param h
     * @return
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidht = ((float) w / width);
        float scaleHeight = ((float) h / height);
        matrix.postScale(scaleWidht, scaleHeight);
        Bitmap newbmp = Bitmap.createBitmap(bitmap, 0, 0, width, height,
                matrix, true);
        return newbmp;
    }

    /**
     * 2.获得圆角图片的方法
     * 
     * @param bitmap
     * @param roundPx
     * @return
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     * 4.将Drawable转化为Bitmap
     * 
     * @param drawable
     * @return
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, drawable
                .getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
}
