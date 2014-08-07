package com.greenlemonmobile.app.ebook.books.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

public interface ZipWrapper {
    
    ZipEntry getEntry(String entryName);
    
    InputStream getInputStream(ZipEntry entry) throws IOException;
    
    Enumeration<? extends ZipEntry> entries();
    
    void close() throws IOException;
}
