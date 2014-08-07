package com.greenlemonmobile.app.ebook.entity;

public class FileInfo {
    public String fileName;

    public String filePath;

    public long fileSize;

    public boolean IsDir;

    public int Count;

    public long ModifiedDate;

    public boolean Selected;

    public boolean canRead;

    public boolean canWrite;

    public boolean isHidden;
    
    public boolean upback = false;

    public long dbId; // id in the database, if is from database
}
