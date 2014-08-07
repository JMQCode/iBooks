package com.greenlemonmobile.app.ebook.books.views;


public interface ReaderListener {
    /**
     * 输出调试信息
     * @param s
     */
    void d(String warning);

    /**
     * 输出错误信息
     * @param s
     */
    void e(String error);

    /**
     * 图书打开成功
     */
    void onBookReady();
    
    /**
     * 图书打开失败
     */
    void onBookError();
    
    /**
     * 开始后台分页
     */
    void onPaginationStarting();
    
    /**
     * 分页进度变化
     * @param progress
     */
    void onPaginationProgressChanged(int progress);
    
    /**
     * 后台分页结束
     */
    void onPaginationReady(int pageCount, int pageIndex);

    /**
     * 章节正在加载
     * @param chapterIndex
     */
    void onChapterLoading(int chapterIndex);

    /**
     * 章节加载成功
     * @param chapterIndex
     */
    void onChapterReady(int chapterIndex);
    
    /**
     * 当前页码发生改变
     * 
     * @param chapterIndex 当前章序号
     * @param pageIndex 当前页码
     * @param pageCount 文档总页码
     */
    void onCurrentPageChanged(int chapterIndex, int pageIndex, int pageCount);
    
    /**
     * 页面跳转完成通知
     * @param chapterIndex 当前章序号
     * @param pageIndex 当前页码
     */
    void onPageNavigationFinish(int chapterIndex, int pageIndex);
    
    /**
     * Theme更改完成通知
     */
    void onThemeApplied();
    
    /**
     * 通知外部需要显示或者关闭工具栏
     */
    void onShowToolbar();
}