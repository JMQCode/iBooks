
package com.greenlemonmobile.app.ebook.books.httpd;

import android.util.Pair;

import com.greenlemonmobile.app.ebook.books.parser.IParser;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyHTTPD extends NanoHTTPD {

    public static int HTTPD_PORT = 1234;
    public static String HTTPD_URL_BASE = "http://localhost:1234/";

    private final IParser mParser;

    public MyHTTPD(int port, File wwwroot, IParser parser) throws IOException {
        super(port, wwwroot);

        mParser = parser;
    }

    @Override
    public Response serveFile(String uri, Properties header, File homeDir,
            boolean allowDirectoryListing) {
        Response res = null;

        if (mParser != null) {
            synchronized (mParser) {
                try {
                    uri = uri.startsWith("/") ? uri.substring(1) : uri;

                    // Get MIME type from file name extension, if possible
                    String mime = null;
                    int dot = uri.lastIndexOf('.');
                    if (dot >= 0)
                        mime = (String) theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
                    if (mime == null)
                        mime = MIME_DEFAULT_BINARY;
    
                    Pair<? extends InputStream, Long> resouce = mParser.getFileStream(uri);
                    if (resouce.first != null) {
                        res = new Response(HTTP_OK, mime, resouce.first);
                        res.addHeader("Content-Length", "" + resouce.second);
                    } else {
                        //Log.e("xxxxxxxxxxxxxxxxxxxxxxxxxx", "resouce file (" + uri + ") fail to get from zip!!!");
                        res = new Response( HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: Reading file failed." );
                    }
                } catch (Exception e) {
                    res = new Response( HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: Reading file failed." );
                } finally {
                }
            }
        }

        res.addHeader("Accept-Ranges", "bytes"); // Announce that the file
                                                 // server accepts partial
                                                 // content requestes
        return res;
    }

}
