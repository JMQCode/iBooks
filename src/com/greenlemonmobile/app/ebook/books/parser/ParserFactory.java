package com.greenlemonmobile.app.ebook.books.parser;

import android.content.Context;

public class ParserFactory {
	public static IParser getParserInstance(Context context, String file) {
		IParser parser = null;
		if (file != null) {
    	    if (file.toLowerCase().endsWith(".epub"))
    	    	parser = new EpubParser(context, file);
    	    else if (file.toLowerCase().endsWith(".umd"))
    	    	parser = new UMDParser(context, file);
    	    else if (file.toLowerCase().endsWith(".html") || file.toLowerCase().endsWith(".htm")  || file.toLowerCase().endsWith(".txt"))
    	    	parser = new HtmlParser(context, file);
    	    else if (file.toLowerCase().endsWith(".chm"))
    	    	parser = new CHMParser(context, file);
		}
	    
	    return parser;
	}
}
