package com.greenlemonmobile.app.ebook.books.model;

public class Chapter {
    
   public enum ChapterState {
        // The chapter hasn't been loaded (or even scheduled to be loaded) yet.
        NOT_LOADED,
        
        // Chapter segments are being loaded; pagination and indexing haven't started yet.
        LOADING,
        
        // Loading, pagination, and indexing complete; the chapter is ready.
        READY
    }
    
   public int index;
    
    /**
     * chapter title
     */
    public String title;
    
    public String id;
    
    /**
     * chapter identify (chapter src)
     */
    public String src;
    
    public String anchor;
    
    public long navLevel;
    
    /**
     * total page count
     */
    public int pageCount = 1;
    
    /**
     * the previous chapters' page count
     */
    public int previousPageCount = 0;
    
    /**
     * the previous chapters' file size
     */
    public long previousChapterFileSize;
    
    /**
     * total character count
     */
    public int characterCount = 0;
    
    /**
     * raw file size(html file size)
     */
    public long fileSize;
    
    /**
     * Render state
     */
    public Chapter.ChapterState chapterState = ChapterState.NOT_LOADED;
    
    /**
     * Pagination state
     */
    public Chapter.ChapterState paginationState = ChapterState.NOT_LOADED;
}
