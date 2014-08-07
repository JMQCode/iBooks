package com.greenlemonmobile.app.ebook.books.parser;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import com.greenlemonmobile.app.ebook.books.model.Book;
import com.greenlemonmobile.app.ebook.books.model.Chapter;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.ZipEntry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class EpubParser implements IParser {
    private static final String TEMP_FOLDER_NAME = "temp";
    private static final int BUFFER_SIZE = 1024 * 8;
    private static final int CHAR_8K = 1024 * 8;
    
    // book metadata
    public class Metadata {
        public String title;
        public String creator;
        public String bookId;
        public String publisher;
        public String date;
        public String ISBN;
        public String language;
        public String cover;
        public String rights;
    }
    
    public class Manifest {
        public String id;
        public String href;
        public String media_type;
    }
    
    public static class NavPoint {
        public String id;
        public String playOrder;
        public long navLevel;
        public String navLabel;        
        public String src;
        public String anchor;
        public int chapterIndex;
        public int pageIndex;
    }
    
    public class Spine {
        public String idref;
        public boolean linear;
        public ArrayList<NavPoint> navcontent;
    }
    
    public class Guide {
        public String type;
        public String title;
        public String href;
    }
    
    private class NavPointSort implements Comparator<NavPoint> {
        @Override
        public int compare(NavPoint lhs, NavPoint rhs) {
            return lhs.playOrder.compareTo(rhs.playOrder);
        }
    }
    
    private boolean mSupportEncryption = false;
    private ZipWrapper mZipFile;
    private Metadata mMetadata;
    private HashMap<String, Manifest> mManifest;
    private ArrayList<NavPoint> mNavMap;
    private ArrayList<NavPoint> mSpineToc;
    private ArrayList<Spine> mSpine;
    private ArrayList<Guide> mGuide;
    private ArrayList<String> mCssFiles;
    private Context mContext;
    private ArrayList<String> mCacheFolder;
    private String mOpfFilePath;
    private String mNcxFilePath;
    private String mOpfRelativePath;
    private String mResourceRelativePath;
    private String mSavePath;
    
    public EpubParser(Context context, final String epubFile) {
        this(context, epubFile, null, null);
    }
    
    /**
     * Parser Epub/Jeb file
     * @param context
     * @param epubFile
     * @param cert
     * @param certlen
     * @param deviceid
     * @param random
     */
    public EpubParser(Context context, final String epubFile, String encryptionKey, String random) {
        mContext = context;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            mSavePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + TEMP_FOLDER_NAME;
        else
            mSavePath = mContext.getFilesDir().getAbsolutePath() + File.separator + TEMP_FOLDER_NAME;
        
        File file = new File(epubFile);
        if (file.isFile() && file.exists() && file.canRead()) {
            try {
                mZipFile = new RawZipFile(epubFile);
                mMetadata = new Metadata();
                mManifest = new HashMap<String, Manifest>();
                mNavMap = new ArrayList<NavPoint>();
                mSpineToc = new ArrayList<NavPoint>();
                mCssFiles = new ArrayList<String>();
                mSpine = new ArrayList<Spine>();
                mGuide = new ArrayList<Guide>();
                parser();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void close() {
        if (mZipFile != null) {
            try {
                mZipFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mZipFile = null;
        }
        
        if (mMetadata != null)
            mMetadata = null;
        
        if (mManifest != null) {
            mManifest.clear();
            mManifest = null;
        }
        
        if (mNavMap != null) {
            mNavMap.clear();
            mNavMap = null;
        }
        
        if (mSpineToc != null) {
            mSpineToc.clear();
            mSpineToc = null;
        }
        
        if (mCssFiles != null) {
            mCssFiles.clear();
            mCssFiles = null;
        }
        
        if (mSpine != null) {
            mSpine.clear();
            mSpine = null;
        }
        
        if (mGuide != null) {
            mGuide.clear();
            mGuide = null;
        }
        
        if (mCacheFolder != null) {
            for (String folder : mCacheFolder) {
                File file = new File(folder);
                DeleteRecursive(file);
            }
            mCacheFolder.clear();
            mCacheFolder = null;
        }
    }
    
    private void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();
    }
    
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
    
    public Metadata getBookInfo() {
        return mMetadata;
    }
    
    public ArrayList<NavPoint> getNavMap() {
        return mNavMap;
    }
    
    public ArrayList<Spine> getSpine()
    {
    	return mSpine;
    }
    
    public String getSpineItemRef(Spine spine)
    {
    	return mManifest.get(spine.idref).href;
    }
    
    public Pair<? extends InputStream, Long> getFileStream(String entryName) {
        InputStream is = null;
        BufferedInputStream bis = null;
        long streamSize = 0;
        ByteArrayOutputStream baos = null;
        String relativePath = mResourceRelativePath;
        
        if (entryName.startsWith("../") && !TextUtils.isEmpty(relativePath)) {
            if (relativePath.endsWith("/"))
                relativePath = relativePath.substring(0, relativePath.length() - 1);
            
            if (relativePath.lastIndexOf("/") != -1)
                relativePath = relativePath.substring(0, relativePath.lastIndexOf("/"));
            else
                relativePath = "";
        }
        if (entryName.startsWith("../"))
            entryName = entryName.substring(3);
        
        try {
            if (mZipFile != null) {
                ZipEntry entry = mZipFile.getEntry((TextUtils.isEmpty(mOpfRelativePath) ? "" :  mOpfRelativePath) + (TextUtils.isEmpty(relativePath) ? "" :  relativePath) + entryName);
                if (entry == null)
                    entry = mZipFile.getEntry((TextUtils.isEmpty(mOpfRelativePath) ? "" :  mOpfRelativePath) + entryName);
            
                if (entry != null) {
                    is = mZipFile.getInputStream(entry);
                    if (mSupportEncryption) {
                        baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[BUFFER_SIZE];
                        do
                        {
                            int count = is.read(buffer, 0, BUFFER_SIZE);
                            if (count < 0)
                                break;
                            baos.write(buffer, 0, count);
                        } while (true);
                        
                        baos.flush();
                        is.close();
                        
                        // Get the raw body as a byte []
                        byte [] fbuf = baos.toByteArray();
    
                        // Create a BufferedReader for easily reading it as string.
                        is = new ByteArrayInputStream(fbuf, 0, fbuf.length);
                        
                        streamSize = fbuf.length;
                    } else {
                        if (entry.getSize() != is.available()) {
                            bis = new BufferedInputStream(is, CHAR_8K);
                            baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int count;
                            while ((count = bis.read(buffer)) != -1) {
                                baos.write(buffer, 0, count);
                            }
                            baos.flush();
                            is.close();
                            
                            // Get the raw body as a byte []
                            byte [] fbuf = baos.toByteArray();
        
                            // Create a BufferedReader for easily reading it as string.
                            is = new ByteArrayInputStream(fbuf, 0, fbuf.length);
                        }
                        streamSize = entry.getSize();
                    }
                }
    
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
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
        
        if (is != null && streamSize > 0)
            return Pair.create(is, streamSize);
        else
            return Pair.create(null, Long.valueOf(0));
    }
    
    public String getFileLocalPath(String entryName) {
    	String relativePath = mResourceRelativePath;
    	
        if (entryName.startsWith("../") && !TextUtils.isEmpty(relativePath)) {
            if (relativePath.endsWith("/"))
            	relativePath = relativePath.substring(0, relativePath.length() - 1);
            
            if (relativePath.lastIndexOf("/") != -1)
            	relativePath = relativePath.substring(0, relativePath.lastIndexOf("/") + 1);
            else
            	relativePath = "";
        }
        if (entryName.startsWith("../"))
        	entryName = entryName.substring(3);
        String path = mSavePath + File.separator + (TextUtils.isEmpty(mOpfRelativePath) ? "" :  mOpfRelativePath) + (TextUtils.isEmpty(relativePath) ? "" :  relativePath) + entryName;
        File file = new File(path);
        if (file.isFile() && file.exists()) {
            if (mCacheFolder == null)
                mCacheFolder = new ArrayList<String>();
            if (!mCacheFolder.contains(file.getParentFile().getAbsolutePath()))
                mCacheFolder.add(file.getParentFile().getAbsolutePath());
            return path;
        }
        
        if (mZipFile != null) {
            ZipEntry entry = mZipFile.getEntry((TextUtils.isEmpty(mOpfRelativePath) ? "" :  mOpfRelativePath) + (TextUtils.isEmpty(relativePath) ? "" :  relativePath) + entryName);
            try {
                if (entry != null) {
                    if (mCacheFolder == null)
                        mCacheFolder = new ArrayList<String>();
                    
                    file.getParentFile().mkdirs();
                    
                    if (!mCacheFolder.contains(file.getParentFile().getAbsolutePath()))
                        mCacheFolder.add(file.getParentFile().getAbsolutePath());
                    InputStream is = mZipFile.getInputStream(entry);
                    FileOutputStream fo = null;
                    try {
                        fo = new FileOutputStream(path);
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int read = 0;
                        while ((read = is.read(buffer, 0, BUFFER_SIZE)) != -1) {
                            fo.write(buffer, 0, read);
                        }
                        fo.flush();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                    } finally {
                        try {
                            if (is != null)
                                is.close();
                            if (fo != null)
                                fo.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }
    
    public long getChapterContentSize(String entryName) {
    	for (Spine spine : mSpine) {
    		String src = mManifest.get(spine.idref).href;
            if (src.equals(entryName)) {
                ZipEntry entry = mZipFile.getEntry(mOpfRelativePath + entryName);
                if (entry != null) {
                    return entry.getSize();
                }
                break;
            }
        }
        return 0;
    }    
    
    public String getChapterContent(String entryName) {
        String content = "";
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        for (Spine spine : mSpine) {
        	String src = mManifest.get(spine.idref).href;
            if (src.equals(entryName)) {
                ZipEntry entry = mZipFile.getEntry(mOpfRelativePath + entryName);
                try {
                    if (entry != null) {
                        mResourceRelativePath = (entryName.lastIndexOf("/") != -1 ) ? (entryName.substring(0, entryName.lastIndexOf("/") + 1)) : "";
                        is = mZipFile.getInputStream(entry);
                        if (mSupportEncryption) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            baos = new ByteArrayOutputStream();
                            do {
                                int read = is.read(buffer, 0, BUFFER_SIZE);
                                if (read < 0)
                                    break;
                                baos.write(buffer, 0, read);
                            } while (true);
                            
                            content = new String(baos.toByteArray(), "UTF-8");
                        } else {
                            BufferedInputStream bin = new BufferedInputStream(is, CHAR_8K);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(bin, "UTF-8"), CHAR_8K);
                            
                            char[] buffer = new char[(int) entry.getSize()];
                            int size = reader.read(buffer);

                            reader.close();
                            bin.close();
                            
                            content = new String(buffer, 0, size);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                        
                        if (baos != null)
                            baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        return content;
    }
    
    public String getChapterContent2(String entryName) {
        StringBuilder builder = new StringBuilder();
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        for (Spine spine : mSpine) {
        	String src = mManifest.get(spine.idref).href;
            if (src.equals(entryName)) {
                ZipEntry entry = mZipFile.getEntry(mOpfRelativePath + entryName);
                try {
                    if (entry != null) {
                        mResourceRelativePath = (entryName.lastIndexOf("/") != -1) ? entryName.substring(0, entryName.lastIndexOf("/") + 1) : "";
                        is = mZipFile.getInputStream(entry);
                        if (mSupportEncryption) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            baos = new ByteArrayOutputStream();
                            do {
                                int read = is.read(buffer, 0, BUFFER_SIZE);
                                if (read < 0)
                                    break;
                                baos.write(buffer, 0, read);
                            } while (true);
                            
                            builder.append(new String(baos.toByteArray(), "UTF-8"));
                        } else {
                            BufferedInputStream bin = new BufferedInputStream(is, CHAR_8K);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(bin, "UTF-8"), CHAR_8K);
                            
                            String line = reader.readLine();
                            while (line != null) {
                                builder.append(line);
                                builder.append("\r\n");
                                line = null;
                                line = reader.readLine();
                            }
                            reader.close();
                            bin.close();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                        if (baos != null)
                            baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
        return builder.toString();
    }
    
    public void dump() {
    }

    private void parser() {
        if (mZipFile != null) {
            InputStream is = null;
            
            // 1. find the container.xml and find the opf file path
            if (mZipFile.getEntry("META-INF/container.xml") != null) {
                ZipEntry entry = mZipFile.getEntry("META-INF/container.xml");
                try {
                    is = mZipFile.getInputStream(entry);
                    parserMETA_INF_Container(is);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // 2. parser opf file
            if (mZipFile.getEntry(mOpfFilePath) != null) {
                ZipEntry entry = mZipFile.getEntry(mOpfFilePath);
                try {
                    is = mZipFile.getInputStream(entry);
                    parserOpf(is);
                    // cache the image file
                    //cacheFiles();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // 3. parser ncx file
            ZipEntry ncxEntry = TextUtils.isEmpty(mNcxFilePath) ? null : mZipFile.getEntry(mNcxFilePath);
            if (ncxEntry == null) {
                Enumeration<? extends ZipEntry> entries = mZipFile.entries();
                while (entries != null && entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory())
                        continue;
                    if (TextUtils.isEmpty(entry.getName()) || mOpfRelativePath == null)
                        break;
                    if (entry.getName().contains(mOpfRelativePath) && entry.getName().endsWith(".ncx")) {
                        ncxEntry = entry;
                        break;
                    }
                }
            }
            if (ncxEntry != null) {
                try {
                    is = mZipFile.getInputStream(ncxEntry);
                    parserNcx(is);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            if (mNavMap != null && mSpine != null && !mSpine.isEmpty())
            {
            	int nSpineLen = mSpine.size();
            	for (int index=0; index < nSpineLen; index++)
            	{
            		Spine spine = mSpine.get(index);
            		String herf = mManifest.get(spine.idref).href;
            		int nNavMapLen = mNavMap.size();
            		for(int i=0; i<nNavMapLen; i++)
            		{
            			NavPoint nav = mNavMap.get(i);
            			if(nav.src.equals(herf))
            			{
            				nav.chapterIndex = index;
            				if (spine.navcontent == null)
            					spine.navcontent = new ArrayList<NavPoint>();
            				spine.navcontent.add(nav);
            			}
            		}
            	}
            }
        }
    }
    
    public ArrayList<String> getCssFiles() {
        return mCssFiles;
    }
    
    private void cacheFiles() {
        if (mManifest != null && mManifest.size() > 0) {
            Set<String> keys = mManifest.keySet();
            for (String key : keys) {
                Manifest manifest = mManifest.get(key);
                if (manifest.media_type.equalsIgnoreCase("text/css")
                        || manifest.media_type.equalsIgnoreCase("font/opentype")
                        || manifest.media_type.equalsIgnoreCase("application/vnd.ms-opentype")
//                        || manifest.media_type.equalsIgnoreCase("video/mp4")
//                        || manifest.media_type.equalsIgnoreCase("video/webm")
//                        || manifest.media_type.equalsIgnoreCase("audio/mpeg")
                        || manifest.media_type.equalsIgnoreCase("text/javascript")
//                        || manifest.media_type.equalsIgnoreCase("image/jpeg")
//                        || manifest.media_type.equalsIgnoreCase("image/png")
//                        || manifest.media_type.equalsIgnoreCase("image/gif")
                        ) {
                    ZipEntry entry = mZipFile.getEntry(mOpfRelativePath + manifest.href);
                    try {
                        if (entry != null) {
                            if (mCacheFolder == null) {
                                mCacheFolder = new ArrayList<String>();
                            }
                            
                            String path = mSavePath + File.separator + (TextUtils.isEmpty(mOpfRelativePath) ? "" :  mOpfRelativePath) + manifest.href;
                            File file = new File(path);
                            file.getParentFile().mkdirs();
                            
                            if (!mCacheFolder.contains(file.getParentFile().getAbsolutePath())) {
                                mCacheFolder.add(file.getParentFile().getAbsolutePath());
                            }
                            
                            if (mCssFiles != null && manifest.media_type.equalsIgnoreCase("text/css")) {
                                mCssFiles.add(path);
                            }
                            
                            InputStream is = mZipFile.getInputStream(entry);
                            FileOutputStream fo = null;
                            try {
                                fo = new FileOutputStream(path);
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int read = 0;
                                while ((read = is.read(buffer, 0, BUFFER_SIZE)) != -1) {
                                    fo.write(buffer, 0, read);
                                }
                                fo.flush();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                            } finally {
                                try {
                                    if (is != null)
                                        is.close();
                                    if (fo != null)
                                        fo.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    private void parserNcx(InputStream is) {
        if (is == null)
            return;
        
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            
            NcxSAXParser saxParser = new NcxSAXParser(mNavMap);
            xr.setContentHandler(saxParser);
            xr.parse(new InputSource(new InputStreamReader(is)));
            
//            if (mNavMap.size() > 0) {
//                NavPointSort sort = new NavPointSort();
//                Collections.sort(mNavMap, sort);
//            }
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
    }
    
    private void parserMETA_INF_Container(InputStream is) {
        if (is == null)
            return;
        // XmlPullParser
        BufferedInputStream bin = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            
            bin = new BufferedInputStream(is, CHAR_8K);
            
            bin.mark(3);
            byte[] bom = new byte[3];
            int length = bin.read(bom);
            
            if (length == 3 && (bom[0] & 0xff) == 0xEF
                    && (bom[1] & 0xff) == 0xBB && (bom[2] & 0xff) == 0xBF) {
                // 有BOM�?
            } else
                bin.reset();
            
            xpp.setInput(bin, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if (xpp.getName().equalsIgnoreCase("rootfile")) {
                            for (int index = 0; index < xpp.getAttributeCount(); ++index) {
                                if (xpp.getAttributeName(index).equalsIgnoreCase("full-path")) {
                                    mOpfRelativePath = xpp.getAttributeValue(index);
                                    mOpfFilePath = mOpfRelativePath;
                                    mOpfRelativePath = mOpfRelativePath.substring(0 , mOpfRelativePath.lastIndexOf("/") + 1);
                                    break;
                                }
                            }
                            break;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        break;
                }
                eventType = xpp.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bin != null)
                    bin.close();
                bin = null;
            } catch (IOException e) {
            }
        }
    }
    
    private void parserOpf(InputStream is) {
        if (is == null)
            return;
        boolean enterMetadata = false;
        boolean enterManifest = false;
        boolean enterSpine = false;
        boolean enterGuide = false;
        String tagName = "";
        String tagId = "";
        String opf_role = "";
        String opf_event = "";
        int linearIndex = 0;
        BufferedInputStream bin = null;
        
        // XmlPullParser
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            
            bin = new BufferedInputStream(is, CHAR_8K);
            
            bin.mark(3);
            byte[] bom = new byte[3];
            int length = bin.read(bom);
            
            if (length == 3 && (bom[0] & 0xff) == 0xEF
                    && (bom[1] & 0xff) == 0xBB && (bom[2] & 0xff) == 0xBF) {
                // 有BOM�?
            } else
                bin.reset();
            
            xpp.setInput(bin, "UTF-8");
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tagName = xpp.getName();
                        if (tagName.equalsIgnoreCase("metadata"))
                            enterMetadata = true;
                        else if (tagName.equalsIgnoreCase("manifest"))
                            enterManifest = true;
                        else if (tagName.equalsIgnoreCase("spine"))
                            enterSpine = true;
                        else if (tagName.equalsIgnoreCase("guide"))
                            enterGuide = true;

                        if (enterMetadata) {
                            tagId = xpp.getAttributeValue(null, "id");
                            opf_role = xpp.getAttributeValue(null, "opf:role");
                            opf_event = xpp.getAttributeValue(null, "opf:event");
                            
                            if (tagName.equalsIgnoreCase("meta")) {
                                String name = xpp.getAttributeValue(null, "name");
                                String content = xpp.getAttributeValue(null, "content");
                                if (name != null && name.equalsIgnoreCase("cover"))
                                    mMetadata.cover = content;
                            }
                        } else if (enterManifest) {
                            if (tagName.equalsIgnoreCase("item")) {                                
                                Manifest item = new Manifest();
                                item.id = xpp.getAttributeValue(null, "id");
                                item.href = xpp.getAttributeValue(null, "href");
                                item.media_type = xpp.getAttributeValue(null, "media-type");
                                
                                if (!TextUtils.isEmpty(mMetadata.cover) && mMetadata.cover.equalsIgnoreCase(item.id))
                                    mMetadata.cover = item.href;
                                
                                mManifest.put(item.id, item);
                                
                                if (item.id.equalsIgnoreCase("ncx"))
                                    mNcxFilePath = (TextUtils.isEmpty(mOpfRelativePath) ? "" :  mOpfRelativePath) + item.href;
                            }
                        } else if (enterSpine) {
                            if (tagName.equalsIgnoreCase("itemref")) {
                                String linear = xpp.getAttributeValue(null, "linear");
                                
                                Spine item = new Spine();
                                item.idref = xpp.getAttributeValue(null, "idref");
                                item.linear = (linear == null) ? true : linear.equalsIgnoreCase("yes") ? true : false;
                                if (!item.linear)
                                    mSpine.add(item);
                                else
                                    mSpine.add(linearIndex++, item);
                            }
                        } else if (enterGuide) {
                            if (tagName.equalsIgnoreCase("reference")) {
                                Guide item = new Guide();
                                item.type = xpp.getAttributeValue(null, "type");
                                item.title = xpp.getAttributeValue(null, "title");
                                item.href = xpp.getAttributeValue(null, "href");
                                
                                mGuide.add(item);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        tagName = xpp.getName();
                        if (tagName.equalsIgnoreCase("metadata"))
                            enterMetadata = false;
                        else if (tagName.equalsIgnoreCase("manifest"))
                            enterManifest = false;
                        else if (tagName.equalsIgnoreCase("spine"))
                            enterSpine = false;
                        else if (tagName.equalsIgnoreCase("guide"))
                            enterGuide = false;
                        
                        break;
                    case XmlPullParser.TEXT:
                        String text = xpp.getText().trim();
                        if (enterMetadata) {
                            if (tagName.equalsIgnoreCase("dc:title") || tagName.equalsIgnoreCase("title"))
                                mMetadata.title = text;
                            else if (tagName.equalsIgnoreCase("dc:publisher") || tagName.equalsIgnoreCase("publisher"))
                                mMetadata.publisher = text;
                            else if (tagName.equalsIgnoreCase("dc:date") || tagName.equalsIgnoreCase("date"))
                                if (!TextUtils.isEmpty(opf_event) && opf_event.equalsIgnoreCase("epub-publication")) {
                                    mMetadata.date = text;
                                } else
                                    mMetadata.date = text;
                            else if (tagName.equalsIgnoreCase("dc:language") || tagName.equalsIgnoreCase("language"))
                                mMetadata.language = text;
                            else if (tagName.equalsIgnoreCase("dc:rights") || tagName.equalsIgnoreCase("rights"))
                                mMetadata.rights = text;
                            else if (tagName.equalsIgnoreCase("dc:identifier") || tagName.equalsIgnoreCase("identifier")) {
                                if (tagId != null) {
                                    if (tagId.equalsIgnoreCase("bookid"))
                                        mMetadata.bookId = text;
                                    else if (tagId.equalsIgnoreCase("ISBN"))
                                        mMetadata.ISBN = text;
                                }
                            }
                        }
                        break;
                }
                eventType = xpp.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bin != null)
                    bin.close();
                bin = null;
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void constructChapterList(SparseArray<Chapter> chapterList, Book book) {       
        ArrayList<EpubParser.Spine> spinearray = getSpine();
        if (spinearray != null)
        {
            int index = 0; 
            for (EpubParser.Spine spine : spinearray)
            {
                Chapter chapter = new Chapter();
                if (spine.navcontent == null)
                {
                    chapter.index = index;
                    chapter.src = getSpineItemRef(spine);
                    chapter.previousChapterFileSize = book.fileSize;
                    chapter.fileSize = getChapterContentSize(chapter.src);
                    book.fileSize += chapter.fileSize;
                    chapterList.put(index++, chapter);
                    continue;
                }
                EpubParser.NavPoint firstNav = spine.navcontent.get(0);
                chapter.index = index;
                chapter.id = firstNav.id;
                chapter.src = firstNav.src;
                chapter.anchor = firstNav.anchor;
                chapter.title = firstNav.navLabel;
                chapter.previousChapterFileSize = book.fileSize;
                chapter.fileSize = getChapterContentSize(firstNav.src);
                book.fileSize += chapter.fileSize;
                chapterList.put(index++, chapter);
            }
        }
    }

    @Override
    public ParserType getParserType() {
        // TODO Auto-generated method stub
        return IParser.ParserType.Epub;
    }
}
