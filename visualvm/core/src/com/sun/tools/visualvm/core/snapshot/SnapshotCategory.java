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


/**
 * Category describing a snapshot type.
 *
 * @author Jiri Sedlacek
 */
public abstract class SnapshotCategory<X extends Snapshot> {

    private final String name;
    private final Class<X> type;
    private final String prefix;
    private final String suffix;

    /**
     * Creates new instance of SnapshotCategory.
     * 
     * @param name name of the category,
     * @param type type of snapshots described by this category,
     * @param prefix prefix of files containing the snapshots,
     * @param suffix suffix of files containing the snapshots.
     */
    public SnapshotCategory(String name, Class<X> type, String prefix, String suffix) {
        super();
        this.name = name;
        this.type = type;
        this.prefix = prefix;
        this.suffix = suffix;
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
     * Returns prefix of files containing the snapshots.
     * 
     * @return prefix of files containing the snapshots.
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Returns suffix of files containing the snapshots.
     * 
     * @return suffix of files containing the snapshots.
     */
    public String getSuffix() {
        return suffix;
    }
    
    /**
     * Creates an unique name for a new snapshot.
     * 
     * @return unique name for a new snapshot.
     */
    public String createSnapshotName() {
        return getPrefix() + System.currentTimeMillis() + getSuffix();
    }
}
