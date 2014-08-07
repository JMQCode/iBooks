package com.greenlemonmobile.app.ebook.books.parser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import com.greenlemonmobile.app.ebook.books.model.Book;
import com.greenlemonmobile.app.ebook.books.model.Chapter;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class EBKParser implements IParser {
    
//    static {
//        System.loadLibrary("ebk2");
//    }
    
    private Context mContext;
    private String mBookPath;
    
    private String mBookName;
    
    private String mBookContent;
    
    public EBKParser(Context context, String bookPath) {
        super();
        this.mContext = context;
        this.mBookPath = bookPath;
        
        try {
            byte[] buffer = null;
            if (mBookPath.toLowerCase().endsWith(".ebk2")) {
                buffer = getBookContent(mBookPath);
                mBookContent = new String(buffer, "UNICODE");
            }
        } catch (UnsupportedEncodingException e) {
            
        }
    }
    
    private native byte[] getBookContent(String ebkPath);
    
    @Override
    public void constructChapterList(SparseArray<Chapter> chapterList, Book book) {
        Chapter chapter = new Chapter();
        chapter.index = 0;
        chapter.id = "0";
        chapter.navLevel = 0;
        chapter.title = "正文";
        chapterList.append(0, chapter);
    }
    
    @Override
    public ParserType getParserType() {
        // TODO Auto-generated method stub
        return ParserType.EBK2;
    }

    @Override
    public String getChapterContent(String id) {
        return !TextUtils.isEmpty(mBookContent) ? mBookContent : "";
    }
    @Override
    public long getChapterContentSize(String entryName) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Pair<? extends InputStream, Long> getFileStream(String entryName) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ArrayList<NavPoint> getNavMap() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileLocalPath(String entryName) {
        // TODO Auto-generated method stub
        return null;
    }

}
