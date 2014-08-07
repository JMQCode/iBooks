
package com.greenlemonmobile.app.ebook.books.parser;

import android.content.Context;
import android.os.Environment;
import android.util.Pair;
import android.util.SparseArray;

import com.greenlemonmobile.app.ebook.books.model.Book;
import com.greenlemonmobile.app.ebook.books.model.Chapter;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;

import org.chm4j.ChmEntry;
import org.chm4j.ChmFile;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class CHMParser implements IParser {
    private static final String TEMP_FOLDER_NAME = "temp";
    private static final int BUFFER_SIZE = 1024 * 8;
    private static final int CHAR_8K = 1024 * 8;
    
    private Context mContext;
    private String mBookPath;
    private ArrayList<ChmEntry> mChmEntry;
    private ArrayList<Chapter> mChapter;
    private String mSavePath;
    
    public CHMParser(Context context, String bookPath) {
        super();
        this.mContext = context;
        this.mBookPath = bookPath;
        
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            mSavePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + TEMP_FOLDER_NAME;
        else
            mSavePath = mContext.getFilesDir().getAbsolutePath() + File.separator + TEMP_FOLDER_NAME;
        
        mChmEntry = new ArrayList<ChmEntry>();
        mChapter = new ArrayList<Chapter>();
        
        parser();
    }
    
    private void parser() {
        try {
            ChmFile cFile = new ChmFile(mBookPath);
            ChmEntry.Attribute attributes = ChmEntry.Attribute.ALL;
            ChmEntry[] entries = cFile.entries(attributes);
            for (ChmEntry entry : entries) {
                if (entry.getPath().toLowerCase().endsWith(".hhc")) {
                    parserHhc(entry);
                }
                
                listChmEntry(entry, attributes);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void listChmEntry(ChmEntry entry, ChmEntry.Attribute attributes) throws IOException {
        if (entry.hasAttribute(ChmEntry.Attribute.DIRECTORY)) {
            for (ChmEntry e : entry.entries(attributes)) {
                listChmEntry(e, attributes);
            }
        } else {
        	mChmEntry.add(entry);
        }
    }
    
    private void parserHhc(ChmEntry entry) {
        InputStream is = null;
        ByteArrayOutputStream os = null; 
        try {
            byte[] rawData = entry.getRawData();
            int length = rawData.length;
            
            String content = new String(rawData, 0, length, "US-ASCII");
            try {
                if (rawData[0] == 0xFF && rawData[1] == 0xFE)
                {
                    content = new String(rawData, 0, rawData.length, "UNICODE");
                }
                else if ((rawData[0] & 0xff) == 0xEF && (rawData[1] & 0xff) == 0xBB && (rawData[2] & 0xff) == 0xBF)
                {
                    content = new String(rawData, 0, length, "UTF-8");
                } else {
                    boolean allAscII = true;
                    for (int index = 0; index < rawData.length; ++index) {
                        if ((rawData[index] & 0x80) != 0) {
                            allAscII = false;
                            break;
                        }
                    }
                    if (!allAscII)
                        content = new String(rawData, 0, rawData.length, "GB2312");
                    else
                        content = new String(rawData, 0, length, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                content = new String(rawData, 0, length, "UTF-8");
            }
            content = content.toLowerCase();
            content = content.replace("<li>", "");
            
            int ulStartTagIndex = content.indexOf("<ul>");
            int ulEndTagIndex = content.lastIndexOf("</ul>");
            
            content = content.substring(ulStartTagIndex, ulEndTagIndex - 5);
            content = content.replace("type=\"text/sitemap\">", "type=\"text/sitemap\"");
            content = content.replace("\">", "\"/>");
            content = content.replace("type=\"text/sitemap\"", "type=\"text/sitemap\">");
            
            is = new ByteArrayInputStream(content.getBytes());
            
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            
            int navLevel = 0;
            String tagName = "";
            int index = 0;
            Chapter chapter = null;
            xpp.setInput(is, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
            	tagName = xpp.getName();
                switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                	if (tagName.equalsIgnoreCase("ul")) {
                		++navLevel;
                	} else if (tagName.equalsIgnoreCase("object")) {
                		chapter = new Chapter();
                	} else if (tagName.equalsIgnoreCase("param")) {
                		String name = xpp.getAttributeValue(null, "name");
                		String value = xpp.getAttributeValue(null, "value");
                		
                		if (chapter != null) {
	                		if (name.equalsIgnoreCase("Name")) {
	                			chapter.title = value;
	                		} if (name.equalsIgnoreCase("Local")) {
	                			chapter.id = value;
	                		}
                		}
                	}
                	break;
                case XmlPullParser.END_TAG:
                	if (tagName.equalsIgnoreCase("ul")) {
                		--navLevel;
                	} else if (tagName.equalsIgnoreCase("object")) {
                		chapter.index = index;
                		chapter.navLevel = navLevel;
                		chapter.src = Integer.toString(index);
                		if (chapter.id == null)
                		    chapter.id = Integer.toString(index);
                		index++;
                		mChapter.add(chapter);
                		chapter = null;
                	}
                	break;
                case XmlPullParser.TEXT:
                	break;
                }
                eventType = xpp.next();
            }
        } catch (IOException ex) {
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                
            }
        }
    }

    @Override
    public void constructChapterList(SparseArray<Chapter> chapterList, Book book) {
		int index = 0;
		for (Chapter object : mChapter)
			chapterList.put(index++, object);
    }
    
    @Override
    public ArrayList<NavPoint> getNavMap() {
        ArrayList<NavPoint> navMap = new ArrayList<NavPoint>();
        
        for (Chapter object : mChapter) {
            NavPoint navPoint = new NavPoint();
            navPoint.id = object.id;
            navPoint.navLabel = object.title;
            navPoint.navLevel = object.navLevel;
            navPoint.src = object.src;
            
            navMap.add(navPoint);
        }
        return navMap;
    }

    @Override
    public ParserType getParserType() {
        return ParserType.CHM;
    }

    @Override
    public String getChapterContent(String id) {
        String content = "";
        int chapterIndex = Integer.parseInt(id);
        
        if (mChapter != null) {
        	Chapter chapter = mChapter.get(chapterIndex);
        	
        	ChmEntry entry = null;
        	for (ChmEntry object : mChmEntry) {
        		if (object.getPath().toLowerCase().contains(chapter.id.toLowerCase())) {
        			entry = object;
        			break;
        		}
        	}
        	
        	if (entry != null) {
				try {
					byte[] rawData = entry.getRawData();
		            content = new String(rawData, 0, rawData.length, "US-ASCII");
		            try {
		                if (rawData[0] == 0xFF && rawData[1] == 0xFE)
		                {
		                    content = new String(rawData, 0, rawData.length, "UNICODE");
		                }
		                else if ((rawData[0] & 0xff) == 0xEF && (rawData[1] & 0xff) == 0xBB && (rawData[2] & 0xff) == 0xBF)
		                {
		                    content = new String(rawData, 0, rawData.length, "UTF-8");
		                } else {
		                    boolean allAscII = true;
		                    for (int index = 0; index < rawData.length; ++index) {
		                        if ((rawData[index] & 0x80) != 0) {
		                            allAscII = false;
		                            break;
		                        }
		                    }
		                    if (!allAscII)
		                        content = new String(rawData, 0, rawData.length, "GB2312");
		                    else
		                        content = new String(rawData, 0, rawData.length, "UTF-8");
		                }
		            } catch (UnsupportedEncodingException e) {
		                content = new String(rawData, 0, rawData.length, "UTF-8");
		            }
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
        return content;
    }

    @Override
    public long getChapterContentSize(String entryName) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Pair<? extends InputStream, Long> getFileStream(String entryName) {
        InputStream is = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = null;
        Long length = 0L;

        if (mChmEntry != null) {
        	ChmEntry entry = null;
        	for (ChmEntry object : mChmEntry) {
        		if (object.getPath().contains(entryName)) {
        			entry = object;
        			break;
        		}
        	}
			try {
				if (entry != null) {
					is = entry.getInputStream();

					bis = new BufferedInputStream(is, CHAR_8K);
					baos = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int count;
					while ((count = bis.read(buffer)) != -1) {
						baos.write(buffer, 0, count);
					}
					baos.flush();
					
					is.close();
					
					// Get the raw body as a byte []
					byte[] fbuf = baos.toByteArray();
					
					length = (long) fbuf.length;

					// Create a BufferedReader for easily reading it as string.
					is = new ByteArrayInputStream(fbuf, 0, fbuf.length);
				}
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {
                    if (bis != null)
                        bis.close();
                    if (baos != null)
                        baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (is != null && length > 0)
            return Pair.create(is, length);
        else
            return Pair.create(null, Long.valueOf(0));
    }
    
    public String getFileLocalPath(String entryName) {
        InputStream is = null;
        BufferedInputStream bis = null;
        FileOutputStream fo = null;

        String path = mSavePath + File.separator + entryName;
        if (mChmEntry != null) {
            ChmEntry entry = null;
            for (ChmEntry object : mChmEntry) {
                if (object.getPath().contains(entryName)) {
                    entry = object;
                    break;
                }
            }
            File file = new File(path);
            if (file.isFile() && file.exists())
                return path;
            file.getParentFile().mkdirs();
            try {
                if (entry != null) {
                    is = entry.getInputStream();

                    bis = new BufferedInputStream(is, CHAR_8K);
                    fo = new FileOutputStream(path);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int count;
                    while ((count = bis.read(buffer)) != -1) {
                        fo.write(buffer, 0, count);
                    }
                    fo.flush();
                    
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {
                    if (bis != null)
                        bis.close();
                    if (fo != null)
                        fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return path;
    }

    @Override
    public void close() {
    	if (mChapter != null)
    		mChapter.clear();
    	mChapter = null;
    	
    	if (mChmEntry != null)
    		mChmEntry.clear();
    	mChmEntry = null;
    }
}
