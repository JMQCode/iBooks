/*
 * ChmEntry.java
 * 10th of June 2008
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.chm4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a CHM entry.
 * 
 * @author Yann D'Isanto
 */
public final class ChmEntry {

    /** The ChmFile this ChmEntry belongs to */
    private ChmFile chmFile;
    
    /** The  of this ChmEntry (currently unused) */
    private long start;
    
    /** The length of this ChmEntry. */
    private long length;
    
    
    /** The space of this ChmEntry (currently unused) */
    private int space;
    
    /** The flags of this ChmEntry */
    private int flags;
    
    /** The path of this ChmEntry */
    private String path;
    
    /** The attributes of this ChmEntry */
    private Attribute[] attributes;

    /**
     * Creates a ChmEntry.
     * @param chmFile The ChmFile this entry belongs to.
     * @param start The offset of this ChmEntry.
     * @param length The length of this ChmEntry.
     * @param space The space of this ChmEntry (currently unused).
     * @param flags The flags of this ChmEntry.
     * @param path The path of this ChmEntry.
     */
    ChmEntry(ChmFile chmFile, long start, long length, int space, int flags, String path) {
        if(chmFile == null) {
            throw new NullPointerException("chm file is null");
        }
        this.chmFile = chmFile;
        this.start = start;
        this.length = length;
        this.space = space;
        this.flags = flags;
        this.path = path;
        List<Attribute> list = new ArrayList<Attribute>();
        for (Attribute attribute : Attribute.values()) {
            if((flags & attribute.value) == attribute.value) {
                list.add(attribute);
            }
        }
        attributes = list.toArray(new Attribute[0]);
    }
    
    /**
     * Returns An InputStream associated to this ChmEntry. 
     * 
     * @return An InputStream associated to this ChmEntry.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public InputStream getInputStream() throws IOException {
        byte[] data = readContent(getChmFile().getFile().getCanonicalPath(), path);
        return new ByteArrayInputStream(data);
    }
    
    public byte[] getRawData() throws IOException {
        return readContent(getChmFile().getFile().getCanonicalPath(), path);
    }

    /**
     * Tries to determine the type of the entry based on the characters
     * at the beginning of the input stream of this entry.
     * @return a guess at the content type, or null if none can be determined.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public String guessContentType() throws IOException {
        return URLConnection.guessContentTypeFromStream(getInputStream());
    }

    /**
     * Returns the ChmFile this ChmEntry belongs to.
     * @return the ChmFile this ChmEntry belongs to.
     */
    public ChmFile getChmFile() {
        return chmFile;
    }

    /**
     * Returns the length of this ChmEntry.
     * @return the length of this ChmEntry.
     */
    public long getLength() {
        return length;
    }

    /**
     * Returns the path of this ChmEntry.
     * @return the path of this ChmEntry.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns true if this ChmEntry possess the specified Attribute.
     * @param attribute the attribute to test.
     * @return true if this ChmEntry possess the specified Attribute.
     */
    public boolean hasAttribute(Attribute attribute) {
        return (flags & attribute.value) == attribute.value;
    }
    
    /**
     * Returns an array of the Attributes possessed by this ChmEntry.
     * @return an array of the Attributes possessed by this ChmEntry.
     */
    public Attribute[] getAttributes() {
        return attributes.clone();
    }
    
    /**
     * Returns an array of ChmEntry representing the sub entries of this 
     * ChmEntry according the specified attributes. 
     * @param attributes The attributes that the entries must have.
     * @return An array of ChmEntry.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public ChmEntry[] entries(Attribute ...attributes) throws IOException {
        return chmFile.entries(this, attributes);
    }
    
    @Override
    public String toString() {
        return path;
    }
    
    
    private native byte[] readContent(String filename, String path);
    
    
    /** Enumeration of the available attributes for ChmEntry. */
    public enum Attribute { 
        
        /** ALL entries */
        ALL       (31),
        
        /** DIRECTORY attribute */
        DIRECTORY      (16),
        
        /** FILE attribute */
        FILE     (8), 
        
        /** META attribute */
        META      (2),
        
        /** NORMAL attribute */
        NORMAL    (1), 
        
        /** SPECIAL attribute */
        SPECIAL   (4);
        
        /** Value of the attribute (used by chmlib) */
        private int value;
        
        private Attribute(int value) {
            this.value = value;
        }
        
        int getValue() {
            return value;
        }
    }
}
