
package com.greenlemonmobile.app.ebook.entity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.greenlemonmobile.app.constant.DefaultConstant;
import com.greenlemonmobile.app.ebook.R;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser;
import com.greenlemonmobile.app.ebook.db.MyContentProvider;
import com.greenlemonmobile.app.utils.FileGuider;
import com.greenlemonmobile.app.utils.FileUtil;
import com.greenlemonmobile.app.utils.ImageTool;
import com.greenlemonmobile.app.utils.Md5Encrypt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class LocalBook {
    
    public enum OrderColumn {
        BY_TITLE, BY_AUTHOR, BY_ADDITION_DATE, BY_LAST_ACCESS_TIME
    }
    
    public enum OrderType {
        ASC, DESC
    }
    
    public String id = "";
    public String title = "";
    public String author = "";
    public String file = "";
    public long size = 0;
    public String detail_image = "";
    public String list_image = "";
    public String big_image = "";
    public String medium_image = "";
    public String small_image = "";
    public long addition_date = 0;
    public long last_access_date = 0;
    public long total_page = 0;
    public long current_page = 0;
    public long total_offset = 0;
    public long current_offset = 0;
    public boolean selected;
    
    private Context context;
    
    public LocalBook(Context context) {
        this.context = context;
    }
    
    public void load() {
        if (TextUtils.isEmpty(id))
            throw new IllegalStateException("Please set the book id and try again");
        
        Cursor cursor = context.getContentResolver().query(MyContentProvider.BOOK_CONTENT_URI, null, "id=?", new String[] {id}, null);
        if (cursor != null && cursor.moveToNext()) {
            constructFromCursor(cursor);
        }
        
        if (cursor != null)
            cursor.close();
    }
    
    public void delete() {
        if (TextUtils.isEmpty(id))
            throw new IllegalStateException("Please set the book id and try again");
        context.getContentResolver().delete(MyContentProvider.BOOK_CONTENT_URI, "id=?", new String[]{id});
        
        FileGuider savePath = new FileGuider(context, FileGuider.SPACE_PRIORITY_EXTERNAL);
        savePath.setSpace(FileGuider.SPACE_PRIORITY_EXTERNAL);
        savePath.setImmutable(true);
        savePath.setChildDirName("books/"+ id);
        
        FileUtil.DeleteDirectory(savePath.getFile());
    }
    
    public void save() {
        String where = "id=?";
        String[] selectionArgs = new String[] {id};
        ContentValues values = new ContentValues();
        
        values.put("title", title);
        values.put("author", author);
        values.put("file", file);
        values.put("size", size);
        values.put("detail_image", detail_image);
        values.put("list_image", list_image);
        values.put("big_image", big_image);
        values.put("medium_image", medium_image);
        values.put("small_image", small_image);
        values.put("addition_date", addition_date);
        values.put("last_access_date", last_access_date);
        values.put("total_page", total_page);
        values.put("current_page", current_page);
        values.put("total_offset", total_offset);
        values.put("current_offset", current_offset);
        if (0 == context.getContentResolver().update(MyContentProvider.BOOK_CONTENT_URI, values, where, selectionArgs)) {
            values.put("id", id);
            context.getContentResolver().insert(MyContentProvider.BOOK_CONTENT_URI, values);
        }
    }
    
    public void constructFromCursor(Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndex("id"));
        title = cursor.getString(cursor.getColumnIndex("title"));
        author = cursor.getString(cursor.getColumnIndex("author"));
        file = cursor.getString(cursor.getColumnIndex("file"));
        size = cursor.getLong(cursor.getColumnIndex("size"));
        detail_image = cursor.getString(cursor.getColumnIndex("detail_image"));
        list_image = cursor.getString(cursor.getColumnIndex("list_image"));
        big_image = cursor.getString(cursor.getColumnIndex("big_image"));
        medium_image = cursor.getString(cursor.getColumnIndex("medium_image"));
        small_image = cursor.getString(cursor.getColumnIndex("small_image"));
        addition_date = cursor.getLong(cursor.getColumnIndex("addition_date"));
        last_access_date = cursor.getLong(cursor.getColumnIndex("last_access_date"));
        total_page = cursor.getLong(cursor.getColumnIndex("total_page"));
        current_page = cursor.getLong(cursor.getColumnIndex("current_page"));
        total_offset = cursor.getLong(cursor.getColumnIndex("total_offset"));
        current_offset = cursor.getLong(cursor.getColumnIndex("current_offset"));
    }
    
    public static LocalBook getLocalBook(Context context, String filePath) {
    	String id = Md5Encrypt.md5(filePath);
    	Cursor cursor = context.getContentResolver().query(MyContentProvider.BOOK_CONTENT_URI, null, "id=?", new String[]{id}, null);
    	LocalBook book = null;
    	if (cursor != null && cursor.moveToNext()) {
    		book = new LocalBook(context);
    		book.constructFromCursor(cursor);
    	}
    	
    	if (cursor != null)
    		cursor.close();
    	
    	return book;
    }
    
    public static String importAssetLocalBook(Activity context, String path) {
    	
        FileGuider savePath = new FileGuider(context, FileGuider.SPACE_PRIORITY_EXTERNAL);
        savePath.setSpace(FileGuider.SPACE_PRIORITY_EXTERNAL);
        savePath.setImmutable(true);
        savePath.setChildDirName("books/"+ Md5Encrypt.md5(path));
        savePath.setFileName(path);
        savePath.setMode(Context.MODE_WORLD_WRITEABLE);
        
        InputStream is = null;
        FileOutputStream os = null;
        
        try {
        	is = context.getAssets().open(path);
        	os = new FileOutputStream(savePath.getFilePath());
        	byte[] buffer = new byte[DefaultConstant.DEFAULT_BUFFER_SIZE];
        	int len = 0;
        	while ((len = is.read(buffer)) > 0) {
        		os.write(buffer, 0, len);
        	}
        	os.flush();
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	try {
        		if (is != null)
        			is.close();
        		if (os != null)
        			os.close();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
        
    	FileInfo file = FileUtil.GetFileInfo(new File(savePath.getFilePath()), null, false);
    	importLocalBook(context, file);
    	
    	return file.filePath;
    }
    
    public static void importLocalBook(Activity context, FileInfo file) {
    	if (file.filePath.toLowerCase().endsWith(DefaultConstant.EPUB_SUFFIX)) {
            EpubParser epubParser = new EpubParser(context, file.filePath);

            EpubParser.Metadata metadata = epubParser.getBookInfo();
            String IMAGE_SUFFIX = ".img";
            LocalBook book = new LocalBook(context);
            book.id = Md5Encrypt.md5(file.filePath);
            book.title = (TextUtils.isEmpty(metadata.title) || metadata.title.equals("\n")) ? FileUtil.getNameFromFilename(file.fileName) : metadata.title;
            book.author = metadata.creator;
            book.file = file.filePath;
            book.size = file.fileSize;
            book.addition_date = System.currentTimeMillis();
            
            if (!TextUtils.isEmpty(metadata.cover)) {
                FileGuider savePath = new FileGuider(context, FileGuider.SPACE_PRIORITY_EXTERNAL);
                savePath.setSpace(FileGuider.SPACE_PRIORITY_EXTERNAL);
                savePath.setImmutable(true);
                savePath.setChildDirName("books/"+ book.id);
                savePath.setFileName(book.id + IMAGE_SUFFIX);
                savePath.setMode(Context.MODE_WORLD_WRITEABLE);
                
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                options.inJustDecodeBounds = true;
                
                BitmapFactory.decodeFile(epubParser.getFileLocalPath(metadata.cover), options);
                options.inJustDecodeBounds = false;
                
                int w = options.outWidth;
                int h = options.outHeight;
                float width = context.getResources().getDisplayMetrics().widthPixels;
                float height = options.outHeight * (width / options.outWidth);
                options.inSampleSize = (int) ((w * h) / (height * width));
                if (((w * h) % (height * width)) > 0) {
                    options.inSampleSize++;
                }
                if (options.inSampleSize == 0)
                    options.inSampleSize = 1;
                
                Bitmap bitmap = null;
                Bitmap src = null;
                try {
                    src = BitmapFactory.decodeFile(epubParser.getFileLocalPath(metadata.cover), options);
                    if (src != null) {
                        int dstWidth = (int) context.getResources().getDimension(R.dimen.thumbnailBigImageWidth);
                        int dstHeight = (int) (options.outHeight * (dstWidth * 1.0f / options.outWidth));
                        bitmap = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, false);
                        if (bitmap != null)
                            ImageTool.saveFile(bitmap, savePath.getFilePath());
                    }
                    book.big_image = book.medium_image = book.small_image = book.detail_image = book.list_image = savePath.getFilePath();
                } catch (OutOfMemoryError error) {
                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bitmap != null)
                        bitmap.recycle();
                    if (src != null)
                        src.recycle();
                }
            }
            book.save();
            epubParser.close();
    	} else {
            LocalBook book = new LocalBook(context);
            book.id = Md5Encrypt.md5(file.filePath);
            book.title = FileUtil.getNameFromFilename(file.fileName);
            book.file = file.filePath;
            book.size = file.fileSize;
            book.addition_date = System.currentTimeMillis();
            book.save();
    	}
    }
    
    public static void getLocalBookList(Context context, LocalBook.OrderColumn orderColumn, LocalBook.OrderType orderType, ArrayList<LocalBook> localBooks) {
    	localBooks.clear();
    	
        String sortOrder = "";
        switch (orderColumn) {
            case BY_TITLE:
                sortOrder += "title ";
                break;
            case BY_AUTHOR:
                sortOrder += "author ";
                break;
            case BY_ADDITION_DATE:
                sortOrder += "addition_date ";
                break;
            case BY_LAST_ACCESS_TIME:
                sortOrder += "last_access_date ";
                break;
        }
        
        switch (orderType) {
            case ASC:
                sortOrder += "ASC";
                break;
            case DESC:
                sortOrder += "DESC";
                break;
        }
        Cursor cursor = context.getContentResolver().query(MyContentProvider.BOOK_CONTENT_URI, null, null, null, sortOrder);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                LocalBook book = new LocalBook(context);
                book.constructFromCursor(cursor);
                localBooks.add(book);
            }
        }
        if (cursor != null)
            cursor.close();
        return;
    }
    
    public static boolean isBuildInBookHasSaved(Context context, String bookName) {
    	boolean bookExisted = false;
    	Cursor cursor = context.getContentResolver().query(MyContentProvider.BUILD_IN_BOOK_CONTENT_URI, null, "id=?", new String[] {Md5Encrypt.md5(bookName)}, null);
    	if (cursor != null && cursor.moveToNext()) {
    		bookExisted = true;
    	}
    	
    	return bookExisted;
    }
    
    public static void setBuildInBookSaved(Context context, String bookName) {
        String where = "id=?";
        String[] selectionArgs = new String[] {Md5Encrypt.md5(bookName)};
        ContentValues values = new ContentValues();
        
        values.put("saved", 1);
        if (0 == context.getContentResolver().update(MyContentProvider.BUILD_IN_BOOK_CONTENT_URI, values, where, selectionArgs)) {
            values.put("id", Md5Encrypt.md5(bookName));
            context.getContentResolver().insert(MyContentProvider.BUILD_IN_BOOK_CONTENT_URI, values);
        }
    }
}
