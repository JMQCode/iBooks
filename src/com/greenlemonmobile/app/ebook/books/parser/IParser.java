package com.greenlemonmobile.app.ebook.books.parser;

import android.util.Pair;
import android.util.SparseArray;

import com.greenlemonmobile.app.ebook.books.model.Book;
import com.greenlemonmobile.app.ebook.books.model.Chapter;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;

import java.io.InputStream;
import java.util.ArrayList;

public interface IParser {
    
    enum ParserType {
        Epub, UMD, Html, CHM, EBK2, EBK3
    }
    
    void constructChapterList(SparseArray<Chapter> chapterList, Book book);
    
    ParserType getParserType();
	
	String getChapterContent(String id);
	
	long getChapterContentSize(String id);
	
	Pair<? extends InputStream, Long> getFileStream(String id);
	
	ArrayList<NavPoint> getNavMap();
	
	String getFileLocalPath(String entryName);

	void close();
}
