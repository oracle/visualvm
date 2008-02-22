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
import java.util.Date;


/**
 * Category describing a snapshot type.
 * Category should return POSITION_NONE for getPreferredPosition() if it's not to be shown in UI.
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotCategory<X extends Snapshot> implements Positionable {
    
    /**
     * Category won't be displayed in UI.
     */
    public static final int POSITION_NONE = Integer.MIN_VALUE;

    private final String name;
    private final Class<X> type;
    private final String prefix;
    private final String suffix;
    private final int preferredPosition;
    private final SnapshotLoader loader;

    /**
     * Creates new instance of SnapshotCategory.
     * 
     * @param name name of the category,
     * @param type type of snapshots described by this category,
     * @param prefix prefix of files containing the snapshots,
     * @param suffix suffix of files containing the snapshots.
     */
    public SnapshotCategory(String name, Class<X> type, String prefix, String suffix, int preferredPosition, SnapshotLoader loader) {
        super();
        this.name = name;
        this.type = type;
        this.prefix = prefix;
        this.suffix = suffix;
        this.preferredPosition = preferredPosition;
        this.loader = loader;
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
    
    public SnapshotLoader getLoader() {
        return loader;
    };
    
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
        return fileName.startsWith(getPrefix()) && fileName.endsWith(getSuffix());
    }
    
    protected String getBaseFileName(String fileName) {
        String pref = getPrefix();
        String suff = getSuffix();
        if (pref != null && fileName.startsWith(pref)) fileName = fileName.substring(pref.length());
        if (suff != null && fileName.endsWith(suff)) fileName = fileName.substring(0, fileName.length() - suff.length());
        return fileName;
    }
    
    /**
     * Creates an unique name for a new snapshot.
     * 
     * @return unique name for a new snapshot.
     */
    public String createFileName() {
        return getPrefix() + System.currentTimeMillis() + getSuffix();
    }
    
    public String getDisplayName(X snapshot) {
        File file = snapshot.getFile();
        if (file != null) {
            String fileName = file.getName();
            if (isSnapshot(file)) {
                try {
                    long timeStamp = Long.parseLong(getBaseFileName(fileName));
                    return org.netbeans.lib.profiler.utils.StringUtils.formatUserDate(new Date(timeStamp));
                } catch (NumberFormatException e) {
                    return fileName;
                }
            }
            else return fileName;
        }
        else return snapshot.toString();
    }
}
