package com.greenlemonmobile.app.ebook.books.parser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.greenlemonmobile.app.ebook.books.model.Book;
import com.greenlemonmobile.app.ebook.books.model.Chapter;
import com.greenlemonmobile.app.ebook.books.parser.EpubParser.NavPoint;

public class HtmlParser implements IParser {

	private String mBookPath;
	private Context mContext;

	public HtmlParser(Context context, String bookpath) {
		mContext = context;
		mBookPath = bookpath;
	}

	@Override
	public void constructChapterList(SparseArray<Chapter> chapterList, Book book) {
		Chapter chapter = new Chapter();
		chapter.index = 0;
		chapter.title = mBookPath;
		chapter.navLevel = 0;
		chapterList.put(0, chapter);
	}
	
    @Override
    public ArrayList<NavPoint> getNavMap() {
        ArrayList<NavPoint> navMap = new ArrayList<NavPoint>();
        NavPoint point = new NavPoint();
        point.id = "0";
        point.src = mBookPath;
        point.navLabel = mBookPath;
        point.navLevel = 0;
        navMap.add(point);
        return navMap;
    }

    @Override
    public String getFileLocalPath(String entryName) {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public ParserType getParserType() {
		return ParserType.Html;
	}

	@Override
	public String getChapterContent(String chapterIndex) {
		StringBuilder builder = new StringBuilder();
		FileInputStream is = null;

		try {
            if (mBookPath.toLowerCase().endsWith(".html")) {
                is = new FileInputStream(mBookPath);
                BufferedInputStream bin = new BufferedInputStream(is);
                BufferedReader reader = new BufferedReader(new InputStreamReader(bin, "UTF-8"));
                
                String line = reader.readLine();
                while (line != null) {
                    builder.append(line);
                    builder.append("\r\n");
                    line = null;
                    line = reader.readLine();
                }
            } else {
                byte[] bytes = getFileContent(mBookPath, "UTF-8");
                builder.append(new String(bytes));
            }
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return builder.toString();
	}

	@Override
	public long getChapterContentSize(String entryName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Pair<? extends InputStream, Long> getFileStream(String entryName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}
    private static boolean isUTF8Buf(FileInputStream inStream, long l) {
        if (inStream == null || l <= 0)
            return false;

        BufferedInputStream bin = new BufferedInputStream(inStream);
        byte[] buffers = new byte[3];
        try {
            bin.read(buffers);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // ��BOMͷ
        if (l >= 3 && (buffers[0] & 0xff) == 0xEF
                && (buffers[1] & 0xff) == 0xBB && (buffers[2] & 0xff) == 0xBF) {
            return true;
        }

        // û��BOMͷ���ļ��ж�
        byte[] szMaxBuf = new byte[7]; // UTF8�ַ��Ϊ6���ֽ�
        int nUTF8CurPos = 0;
        int nBadCount = 0;
        int nGoodCount = 0;

        while (nUTF8CurPos < l) {
            if (nUTF8CurPos >= 3) {
                buffers = new byte[1];
                try {
                    bin.read(buffers);
                    szMaxBuf[0] = buffers[0];
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else
                szMaxBuf[0] = buffers[nUTF8CurPos];
            nUTF8CurPos++;
            if (((szMaxBuf[0] & 0xff) & 0x80) == 0)// Ansi��ĸ
            {
                continue;
            } else if (((szMaxBuf[0] & 0xff) & 0xE0) == 0xC0)// ��λ
            {
                if (nUTF8CurPos + 1 == l) {
                    break;
                }
                if (nUTF8CurPos >= 3) {
                    buffers = new byte[1];
                    try {
                        bin.read(buffers);
                        szMaxBuf[1] = buffers[0];
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else
                    szMaxBuf[1] = buffers[nUTF8CurPos];
                if (((szMaxBuf[1] & 0xff) & 0x80) == 0x80)
                    nGoodCount++;
                else
                    return false;// nBadCount++;
                nUTF8CurPos++;
            } else if (((szMaxBuf[0] & 0xff) & 0xF0) == 0xE0) // ��λ
            {
                if (nUTF8CurPos + 2 >= l) {
                    break;
                }
                if (nUTF8CurPos >= 3) {
                    buffers = new byte[2];
                    try {
                        bin.read(buffers);
                        szMaxBuf[1] = buffers[0];
                        szMaxBuf[2] = buffers[1];
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else
                    System.arraycopy(buffers, nUTF8CurPos, szMaxBuf, 1, 2);
                // memcpy(szMaxBuf+1,buffers+ nUTF8CurPos, 2);

                if (((szMaxBuf[1] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[2] & 0xff) & 0x80) == 0x80)
                    nGoodCount++;
                else
                    return false;// nBadCount++;
                nUTF8CurPos += 2;
            } else if (((szMaxBuf[0] & 0xff) & 0xF8) == 0xF0)// ��λ
            {
                if (nUTF8CurPos + 3 >= l) {
                    break;
                }
                if (nUTF8CurPos >= 3) {
                    buffers = new byte[3];
                    try {
                        bin.read(buffers);
                        szMaxBuf[1] = buffers[0];
                        szMaxBuf[2] = buffers[1];
                        szMaxBuf[3] = buffers[2];
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else
                    System.arraycopy(buffers, nUTF8CurPos, szMaxBuf, 1, 3);
                // memcpy(szMaxBuf+1,buffers+ nUTF8CurPos,3);
                if (((szMaxBuf[1] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[2] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[3] & 0xff) & 0x80) == 0x80)
                    nGoodCount++;
                else
                    return false;// nBadCount++;
                nUTF8CurPos += 3;
            } else if (((szMaxBuf[0] & 0xff) & 0xFC) == 0xF8)// ��λ
            {
                if (nUTF8CurPos + 4 >= l) {
                    break;
                }
                if (nUTF8CurPos >= 3) {
                    buffers = new byte[4];
                    try {
                        bin.read(buffers);
                        szMaxBuf[1] = buffers[0];
                        szMaxBuf[2] = buffers[1];
                        szMaxBuf[3] = buffers[2];
                        szMaxBuf[4] = buffers[3];
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else
                    System.arraycopy(buffers, nUTF8CurPos, szMaxBuf, 1, 4);
                // memcpy(szMaxBuf+1,buffers+ nUTF8CurPos, 4);

                if (((szMaxBuf[1] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[2] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[3] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[4] & 0xff) & 0x90) == 0x80)
                    nGoodCount++;
                else
                    return false;// nBadCount++;
                nUTF8CurPos += 4;
            } else if (((szMaxBuf[0] & 0xff) & 0xFE) == 0xFC)// ��λ
            {
                if (nUTF8CurPos + 5 >= l) {
                    break;
                }
                if (nUTF8CurPos >= 3) {
                    buffers = new byte[5];
                    try {
                        bin.read(buffers);
                        szMaxBuf[1] = buffers[0];
                        szMaxBuf[2] = buffers[1];
                        szMaxBuf[3] = buffers[2];
                        szMaxBuf[4] = buffers[3];
                        szMaxBuf[5] = buffers[4];
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                } else
                    System.arraycopy(buffers, nUTF8CurPos, szMaxBuf, 1, 5);
                // memcpy(szMaxBuf+1,buffers+ nUTF8CurPos, 5);

                if (((szMaxBuf[1] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[2] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[3] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[4] & 0xff) & 0x80) == 0x80
                        && ((szMaxBuf[5] & 0xff) & 0x80) == 0x80)
                    nGoodCount++;
                else
                    return false;// nBadCount++;
                nUTF8CurPos += 5;
            } else {
                return false;// nBadCount++;
            }
        }

        return (nGoodCount > nBadCount);
    }

    // Bytes Encoding Form
    // 00 00 FE FF UTF-32, big-endian
    // FF FE 00 00 UTF-32, little-endian
    // FE FF UTF-16, big-endian
    // FF FE UTF-16, little-endian
    // EF BB BF UTF-8
    //
    //
    // http://unicode.org/faq/utf_bom.html#BOM
    // A: Here are some guidelines to follow:
    //
    // 1. A particular protocol (e.g. Microsoft conventions for .txt files) may
    // require use of the BOM on certain Unicode data streams, such as files.
    // When you need to conform to such a protocol, use a BOM.
    // 2. Some protocols allow optional BOMs in the case of untagged text. In
    // those cases,
    // 2.1 Where a text data stream is known to be plain text, but of unknown
    // encoding, BOM can be used as a signature. If there is no BOM, the
    // encoding could be anything.
    // 2.2 Where a text data stream is known to be plain Unicode text (but not
    // which endian), then BOM can be used as a signature. If there is no BOM,
    // the text should be interpreted as big-endian.
    // 3. Some byte oriented protocols expect ASCII characters at the beginning
    // of a file. If UTF-8 is used with these protocols, use of the BOM as
    // encoding form signature should be avoided.
    // 4. Where the precise type of the data stream is known (e.g. Unicode
    // big-endian or Unicode little-endian), the BOM should not be used. In
    // particular, whenever a data stream is declared to be UTF-16BE, UTF-16LE,
    // UTF-32BE or UTF-32LE a BOM must not be used. (See also Q: What is the
    // difference between UCS-2 and UTF-16?.)
    private static byte[] getFileContent(final String inFile, final String outCharset) {
        byte[] bytes = null;
        if (TextUtils.isEmpty(inFile) || TextUtils.isEmpty(outCharset))
            return bytes;
        boolean converted = false;
        FileInputStream inStream = null;
        ByteArrayOutputStream outStream = null;
        String inCharset = "UTF-8";
        String defaultCharset = "GBK";
        File file = null;
        boolean hasBOM = false;
        int bomLength = 2;
        try {
            file = new File(inFile);
            inStream = new FileInputStream(inFile);
            long length = file.length();
            if (file.canRead()) {
                byte[] buffers = new byte[(int) Math.min(length, 4)];
                if (buffers != null) {
                    inStream.read(buffers, 0, (int) Math.min(length, 4));
                    if (length >= 2 && (buffers[0] & 0xff) == 0xFE
                            && (buffers[1] & 0xff) == 0xFF) {
                        // UTF-16, big-endian
                        inCharset = "UTF-16BE";
                        hasBOM = true;
                        bomLength = 2;
                    } else if (length >= 2 && (buffers[0] & 0xff) == 0xFF
                            && (buffers[1] & 0xff) == 0xFE) {
                        // UTF-16, little-endian
                        inCharset = "UTF-16LE";
                        hasBOM = true;
                        bomLength = 2;
                    } else if (length >= 4 && (buffers[0] & 0xff) == 0x00
                            && (buffers[1] & 0xff) == 0x00
                            && (buffers[2] & 0xff) == 0xFE
                            && (buffers[3] & 0xff) == 0xFF) {
                        // UTF-32, big-endian
                        inCharset = "UTF-32BE";
                        hasBOM = true;
                        bomLength = 4;
                    } else if (length >= 4 && (buffers[0] & 0xff) == 0xFF
                            && (buffers[1] & 0xff) == 0xFE
                            && (buffers[2] & 0xff) == 0x00
                            && (buffers[3] & 0xff) == 0x00) {
                        // UTF-32, little-endian
                        inCharset = "UTF-32LE";
                        hasBOM = true;
                        bomLength = 4;
                    } else if (length >= 3 && (buffers[0] & 0xff) == 0xEF
                            && (buffers[1] & 0xff) == 0xBB
                            && (buffers[2] & 0xff) == 0xBF) {
                        // UTF-8
                        inCharset = "UTF-8";
                        hasBOM = true;
                        bomLength = 3;
                    }

                    if (hasBOM && inCharset.equalsIgnoreCase(outCharset)) {
                        inStream.close();
                        inStream = new FileInputStream(inFile);
                        buffers = new byte[bomLength];
                        inStream.read(buffers);

                        outStream = new ByteArrayOutputStream();                        
                        int count = 1024 * 8;
                        byte[] buffer = new byte[count];
                        int read = 0;
                        while ((read = inStream.read(buffer, 0, count)) != -1) {
                            outStream.write(buffer, 0, read);
                        }
                        outStream.flush();
                    } else {
                        inStream.close();
                        inStream = new FileInputStream(inFile);
                        // buffers = new byte[(int) length];
                        // inStream.read(buffers, 0, (int) length);
                        outStream = new ByteArrayOutputStream();
                        if (hasBOM) {
                            // String content = new String(buffers, inCharset);
                            // buffers = null;
                            // if (!TextUtils.isEmpty(content)) {
                            // outStream.write(content.getBytes(outCharset));
                            // }

                            // SDK ����Ҫutf8 bom
                            buffers = new byte[bomLength];
                            inStream.read(buffers);

                            BufferedInputStream bin = new BufferedInputStream(
                                    inStream);
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(bin, inCharset));

                            String line = reader.readLine();
                            while (line != null) {
                                outStream.write(line.getBytes(outCharset));
                                line = reader.readLine();
                            }
                            reader.close();
                        } else {
                            if (isUTF8Buf(inStream, length)) {
                                // SDK ����Ҫutf8 bom
                                // byte[] bom = new byte[3];
                                // bom[0] = (byte) 0xEF;
                                // bom[1] = (byte) 0xBB;
                                // bom[2] = (byte) 0xBF;
                                // outStream.write(bom);

                                // String content = new String(buffers,
                                // inCharset);
                                // buffers = null;
                                // if (!TextUtils.isEmpty(content)) {
                                // outStream.write(content.getBytes(outCharset));
                                // }
                                inStream.close();
                                inStream = new FileInputStream(inFile);

                                BufferedInputStream bin = new BufferedInputStream(
                                        inStream);
                                BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(bin, inCharset));

                                String line = reader.readLine();
                                while (line != null) {
                                    outStream.write(line.getBytes(outCharset));
                                    line = reader.readLine();
                                }
                                reader.close();
                            } else {
                                inCharset = defaultCharset;
                                // ���ж�BOM
                                if (length < 2) {
                                    inCharset = defaultCharset;
                                } else if (length < 4) {
                                    if ((buffers[0] & 0xff) == 0xFE
                                            && (buffers[1] & 0xff) == 0xFF) {
                                        // UTF-16, big-endian
                                        inCharset = "UTF-16BE";
                                        hasBOM = true;
                                    } else if ((buffers[0] & 0xff) == 0xFF
                                            && (buffers[1] & 0xff) == 0xFE) {
                                        // UTF-16, little-endian
                                        inCharset = "UTF-16LE";
                                        hasBOM = true;
                                    }
                                }
                                // String content = new String(buffers,
                                // inCharset);
                                // buffers = null;
                                // if (!TextUtils.isEmpty(content)) {
                                // SDK ����Ҫutf8 bom
                                // if (!hasBOM) {
                                // byte[] bom = new byte[3];
                                // bom[0] = (byte) 0xEF;
                                // bom[1] = (byte) 0xBB;
                                // bom[2] = (byte) 0xBF;
                                // outStream.write(bom);
                                // }
                                BufferedInputStream bin = new BufferedInputStream(
                                        inStream);
                                BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(bin, inCharset));

                                String line = reader.readLine();
                                if (line == null) {
                                    inStream.close();
                                    inStream = new FileInputStream(inFile);
                                    buffers = new byte[(int) length];
                                    inStream.read(buffers, 0, (int) length);
                                    String content = new String(buffers,
                                            inCharset);
                                    buffers = null;
                                    if (!TextUtils.isEmpty(content)) {
                                        outStream.write(content
                                                .getBytes(outCharset));
                                    }
                                } else {
                                    while (line != null) {
                                        outStream.write(line.getBytes(outCharset));
                                        line = reader.readLine();
                                    }
                                }
                            }
                        }
                        outStream.flush();
                    }
                    bytes = outStream.toByteArray();
                    converted = true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null)
                    inStream.close();
                if (outStream != null)
                    outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }
}
