/*
 * ChmFile.java
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.chm4j.ChmEntry.Attribute;

/**
 * Represents a CHM file.
 * 
 * @author Yann D'Isanto
 */
public final class ChmFile {

    static {
        System.loadLibrary("chm4j");
    }
    
    /** The associated File object. */
    private File file;

    /**
     * Creates a new ChmFile from the specified pathname.
     * @param pathname The file pathname.
     * @throws java.io.FileNotFoundException If the file denoted by the 
     * specified pathname does not exist.
     */
    public ChmFile(String pathname) throws FileNotFoundException {
        this(new File(pathname));
    }

    /**
     * Creates a new ChmFile from the specified File.
     * @param file The file.
     * @throws java.io.FileNotFoundException If the file does not exist.
     */
    public ChmFile(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("file not found : " +
                    file.getAbsolutePath());
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("the file is a directory : " +
                    file.getAbsolutePath());
        }
        this.file = file;
    }

    /**
     * Returns an array of ChmEntry representing the chm root entries of this 
     * ChmFile according the specified attributes. 
     * If no attribute, the <code>ALL</code> one is used.
     * @param attributes The attributes that the entries must have.
     * @return An array of ChmEntry.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public ChmEntry[] entries(Attribute... attributes) throws IOException {
        return entries(null, attributes);
    }

    /**
     * Returns an array of ChmEntry representing the sub entries of the 
     * specified ChmEntry according the specified attributes. 
     * @param parent The parent ChmEntry 
     * @param attributes The attributes that the entries must have.
     * @return An array of ChmEntry.
     * @throws java.lang.IllegalArgumentException If the specified ChmEntry 
     * does not belong to this ChmFile.
     * @throws java.io.IOException If an I/O error occurs.
     */
    public ChmEntry[] entries(ChmEntry parent, Attribute... attributes) 
            throws IllegalArgumentException, IOException {
        if (parent != null) {
            if (!equals(parent.getChmFile())) {
                throw new IllegalArgumentException(
                        "the entry does not belong to this file");
            }
        }
        int flags = 0;
        for (Attribute type : attributes) {
            flags += type.getValue();
        }
        if (flags == 0) {
            flags = Attribute.ALL.getValue();
        }
        return entries(file.getCanonicalPath(), 
                parent == null ? "/" : parent.getPath(), flags);
    }

    /**
     * Returns an array of ChmEntry representing the sub entries of the 
     * specified entry path in the specified file according the specified flags. 
     * @param filename CHM file pathname.
     * @param path CHM entry path.
     * @param flags Flags representing the attributes that the entries must have.
     * @return An array of ChmEntry.
     * @throws java.io.IOException If an I/O error occurs.
     */
    private native ChmEntry[] entries(String filename, String path, int flags) throws IOException;

    /**
     * Returns the File associated to this ChmFile.
     * @return the File associated to this ChmFile.
     */
    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof ChmFile) {
            return file.equals(((ChmFile) obj).getFile());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.file != null ? this.file.hashCode() : 0);
        return hash;
    }
}
