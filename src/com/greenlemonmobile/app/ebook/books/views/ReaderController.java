package com.greenlemonmobile.app.ebook.books.views;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Pair;
import android.util.SparseArray;

import com.greenlemonmobile.app.ebook.books.model.Chapter;
import com.greenlemonmobile.app.ebook.books.model.ReaderSettings;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;
import com.greenlemonmobile.app.ebook.books.views.WebReader.ThemeMode;

import java.util.ArrayList;

public interface ReaderController {
    public class BitmapInfor {
        public boolean isOverLast;
        public boolean issucced;
        public Bitmap mBitmap;
        public int mPageCount;
    }
    
    /**
     * 初始化阅读版式
     * @param themes
     */
    void initializeTheme(ArrayList<ThemeMode> themes);
    
    /**
     * 初始化
     * @param activity
     * @param viewMode 显示模式有两种，一种是直接让webview显示章节内容，另外一种是采用截图的方式，使用ImageView来显示
     * @param internalGestureDetector 是否允许使用内部的gesture detector
     */
    void initializeReader(Activity activity);
    
    /**
     * 打开图书
     * @param bookUrl
     * @param encryptionKey
     * @param random
     */
    void openBook(String bookUrl, String encryptionKey, String random);
    
    /**
     * 顺序阅读的章节列表
     * @return
     */
    SparseArray<Chapter> getSequenceReadingChapterList();
    
    /**
     * 目录跳转的章节列表
     * @return
     */
    ArrayList<NavPoint> getTOC();
    
    /**
     * 关闭图书
     */
    void closeBook();
    
    /**
     * 当出现内存过低时调用
     */
    void onLowMemory();
    
    /**
     * 用户按下了后退键
     * @return
     */
    @Deprecated
    boolean onBackKeyPressed();
    
    /**
     * 更改当前的排版设置
     * @param settings
     */
    void applyRuntimeSettings(ReaderSettings settings);
    
    /**
     * 窗口大小发生改变，例如：横竖屏转换
     */
    void viewSizeChanged();
    
    /**
     * 跳转到给定的书签页
     * TODO: 需要提供书签的定义
     */
    void gotoBookmark();
    
    /**
     * 跳转到指定的百分比
     * 
     * @param percent
     */
    @Deprecated
    void gotoPercent(float percent);
    
    /**
     * 跳转到指定的页数
     * @param pageIndex
     */
    @Deprecated
    void gotoPage(int pageIndex);
    
    /**
     * 加载某一张的某一页
     * @param chapterIndex
     * @param pageIndex
     */
    @Deprecated
    void loadChapter(int chapterIndex, int pageIndex);
    
    /**
     * 加载指定的章
     * @param chapter
     */
    @Deprecated
    void loadChapter(Chapter chapter);
    
    /**
     * 判断该章是否已经加载完毕
     * @param chapterIndex
     * @return
     */
    @Deprecated
    boolean isChapterReady(int chapterIndex);
    
    
    /**
     * 判断当前是否有正在加载的章节（不含后台运行的分页、缓存）
     * @return
     */
    @Deprecated
    boolean isChapterLoading();
    
    /**
     * 查询后台分页是否已经完成
     * @return
     */
    boolean isPaginatingFinish();
    
    /**
     * 获取当前显示的章节序号，based 0
     * @return
     */
    @Deprecated
    int getCurrentChapterIndex();
    
    /**
     * 获取当前显示的章节的名称
     * @return
     */
    @Deprecated
    String getCurrentChapterTitle();
    
    /**
     * 获取当前显示的页面在本章内的序号，based 0
     * @return
     */
    @Deprecated
    int getCurrentPageIndex();
    
    /**
     * 获取当前显示的章节总页数
     * @return
     */
    @Deprecated
    int getCurrentChapterPageCount();
    
    /**
     * 获取指定页码的截图
     * @param pageIndex
     * @return
     */
    @Deprecated
    Bitmap getPageBitmapAsync(int pageIndex);
    
    /**
     * 获取指定章节的名称
     * @return
     */
    String getChapterTitle(int chapterIndex);
    
    /**
     * 获取指定章节章节总页数
     * @return
     */
    int getChapterPageCount(int chapterIndex);
    
    /**
     * 获取指定章节的前面所有章节的页数，分页未完成时返回0
     * @param chapterIndex
     * @return
     */
    int getPreviousChaptersPageCount(int chapterIndex);
    
    
    /**
     * 获取本书的总页数，分页未完成前返回0
     * @return
     */
    int getTotalPageCount();
    
    /**
     * 返回对应全局页码对应的章节序号和章内页码序号
     * 
     * @param globalPageIndex
     * @return
     */
    Pair<Integer, Integer> getCorrespondChapterPage(int globalPageIndex);
    
    /**
     * 下一页
     * 
     * @return true: 向下翻页成功，false: 当前页已经是最后一页
     */
    @Deprecated
    boolean nextPage();
    
    /**
     * 上一页
     * 
     * @return true: 向上翻页成功, false: 当前页已经是第一页
     */
    @Deprecated
    boolean previousPage();
    
    /**
     * 获取指定页码的截图， 必须在非ui线程中调用
     * @param chapterIndex
     * @param pageIndex
     * @param bitmap
     * @return
     */
    boolean getPageBitmap(int chapterIndex, int pageIndex, BitmapInfor bitmapInfo);
   
    
    /**
     * 切换文字选择模式，用于文字选择复制等操作
     * @param selectionMode
     */
    void switchTextSelectMode(boolean selectionMode);
}
