/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.core.snapshot;

import org.graalvm.visualvm.core.datasupport.Positionable;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.filechooser.FileFilter;


/**
 * Category describing a snapshot type.
 * Category should return POSITION_NONE for getPreferredPosition() if it's not to be shown in UI.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class SnapshotCategory<X extends Snapshot> implements Positionable {
    
    private static final String PREFIX_DIVIDER = "-";   // NOI18N
    
    /**
     * Category won't be displayed in UI.
     */
    public static final int POSITION_NONE = Integer.MIN_VALUE;

    private final String name;
    private final Class<X> type;
    private final String prefix;
    private final String suffix;
    private final int preferredPosition;

    /**
     * Creates new instance of SnapshotCategory.
     * 
     * @param name name of the category,
     * @param type type of snapshots described by this category,
     * @param prefix prefix of files containing the snapshots (can be null),
     * @param suffix suffix of files containing the snapshots (can be null).
     * @param preferredPosition preferred position of this category within other categories when presented in UI.
     */
    public SnapshotCategory(String name, Class<X> type, String prefix, String suffix, int preferredPosition) {
        super();
        this.name = name;
        this.type = type;
        this.prefix = prefix;
        this.suffix = suffix;
        this.preferredPosition = preferredPosition;
    }

    /**
     * Returns name of the category.
     * 
     * @return name of the category.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns type of snapshots described by this category.
     * 
     * @return type of snapshots described by this category.
     */
    public final Class<X> getType() {
        return type;
    }
    
    /**
     * Returns preferred position of this category within other categories when presented in UI.
     * 
     * @return preferred position of this category within other categories when presented in UI.
     */
    public final int getPreferredPosition() {
        return preferredPosition;
    }
    
    /**
     * Returns true if the category can restore snapshot from a saved file, false otherwise.
     * 
     * @return true if the category can restore snapshot from a saved file, false otherwise.
     */
    public boolean supportsOpenSnapshot() {
        return false;
    }
    
    /**
     * Opens a saved snapshot.
     * Default implementation does nothing, custom implementations should open
     * an Open File dialog and open the chosen snapshot.
     * Throws an UnsupportedOperationException if supportsOpenSnapshot() returns false.
     * 
     * @param file saved snapshot.
     */
    public void openSnapshot(File file) {
        throw new UnsupportedOperationException("Open snapshot not supported"); // NOI18N
    }
    
    
    /**
     * Returns prefix of files containing the snapshots.
     * 
     * @return prefix of files containing the snapshots.
     */
    protected final String getPrefix() {
        return prefix;
    }
    
    /**
     * Returns suffix of files containing the snapshots.
     * 
     * @return suffix of files containing the snapshots.
     */
    protected final String getSuffix() {
        return suffix;
    }
    
    protected boolean isSnapshot(File file) {
//        String pref = getPrefix();
        String suff = getSuffix();
        // Fix for #92 - supported snapshot is detected just based on the SUFFIX by default
//        if (pref != null && !fileName.startsWith(pref + PREFIX_DIVIDER)) return false;
        if (suff != null && !file.getName().endsWith(suff)) return false;
        return true;
    }
    
    protected boolean isSnapshot(String fileName) {
        return isSnapshot(new File(fileName));
    }
    
    protected String getBaseFileName(String fileName) {
        String pref = getPrefix();
        String suff = getSuffix();
        if (pref != null && fileName.startsWith(pref + PREFIX_DIVIDER)) fileName = fileName.substring(pref.length() + 1);
        if (suff != null && fileName.endsWith(suff)) fileName = fileName.substring(0, fileName.length() - suff.length());
        return fileName;
    }
    
    protected String getTimeStamp(String fileName) {
        String timeStamp = null;
        
        try {
            long time = Long.parseLong(getBaseFileName(fileName));
            return SnapshotsSupport.getInstance().getTimeStamp(time);
        } catch (NumberFormatException e) {}
        
        return timeStamp;
    }
    
    /**
     * Creates an unique name for a new snapshot.
     * 
     * @return unique name for a new snapshot.
     */
    public String createFileName() {
        String pref = getPrefix();
        String suff = getSuffix();
        String fileName = System.currentTimeMillis() + "";
        if (pref != null) fileName = pref + PREFIX_DIVIDER + fileName;
        if (suff != null) fileName = fileName + suff;
        return fileName;
    }
    
    /**
     * Returns a FilenameFilter for the category.
     * 
     * @return FilenameFilter for the category.
     */
    public FilenameFilter getFilenameFilter() {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return isSnapshot(new File(dir,name));
            }
        };
    }
    
    /**
     * Returns a FileFilter for the category.
     * 
     * @return FileFilter for the category.
     */
    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || isSnapshot(f);
            }
            public String getDescription() {
                String suff = getSuffix();
                return getName() + (suff != null ? " (*" + suff + ")" : "");    // NOI18N
            }
        };
    }
    
//    public FileView getFileView() {
//        return new FileView() {
////            public Icon getIcon(File f) {
////                return new ImageIcon(SnapshotCategory.this.getI);
////            }
//            public String getName(File file) {
//                if (isSnapshot(file)) return getDisplayName(file);
//                else return null;
//            }
//        };
//    }
    
}
