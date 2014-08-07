package com.greenlemonmobile.app.ebook.books.parser;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class RawZipFile extends ZipFile implements ZipWrapper {

    public RawZipFile(File file, int mode) throws IOException {
        super(file, mode);
    }

    public RawZipFile(File file) throws ZipException, IOException {
        super(file);
    }

    public RawZipFile(String name) throws IOException {
        super(name);
    }

}
