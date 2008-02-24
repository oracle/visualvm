/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.snapshot;

import com.sun.tools.visualvm.core.datasource.Snapshot;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.filechooser.FileFilter;


/**
 * Category describing a snapshot type.
 * Category should return POSITION_NONE for getPreferredPosition() if it's not to be shown in UI.
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotCategory<X extends Snapshot> implements Positionable {
    
    private static final String PREFIX_DIVIDER = "-";
    
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
    public String getName() {
        return name;
    }

    /**
     * Returns type of snapshots described by this category.
     * 
     * @return type of snapshots described by this category.
     */
    public Class<X> getType() {
        return type;
    }
    
    /**
     * Returns preferred position of this category within other categories when presented in UI.
     * 
     * @return preferred position of this category within other categories when presented in UI.
     */
    public int getPreferredPosition() {
        return preferredPosition;
    }
    
    /**
     * Returns prefix of files containing the snapshots.
     * 
     * @return prefix of files containing the snapshots.
     */
    protected String getPrefix() {
        return prefix;
    }
    
    /**
     * Returns suffix of files containing the snapshots.
     * 
     * @return suffix of files containing the snapshots.
     */
    protected String getSuffix() {
        return suffix;
    }
    
    protected boolean isSnapshot(File file) {
        return isSnapshot(file.getName());
    }
    
    protected boolean isSnapshot(String fileName) {
        String pref = getPrefix();
        String suff = getSuffix();
        if (pref != null && !fileName.startsWith(pref + PREFIX_DIVIDER)) return false;
        if (suff != null && !fileName.endsWith(suff)) return false;
        return true;
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
    
    public FilenameFilter getFilenameFilter() {
        return new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return isSnapshot(name);
            }
        };
    }
    
    public FileFilter getFileFilter() {
        return new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || isSnapshot(f);
            }
            public String getDescription() {
                String suff = getSuffix();
                return getName() + (suff != null ? " (" + suff + ")" : "");
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
