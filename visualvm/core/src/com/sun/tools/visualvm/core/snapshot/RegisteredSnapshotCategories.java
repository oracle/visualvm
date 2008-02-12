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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A repository of registered SnapshotCategory instances.
 * This is currently used in the Overview tab to display number of saved snapshots
 * for an application/core dump. Snapshot providers don't need to register the types
 * here if not to be displayed in the UI, for example core dumps are displayed in
 * a separate entry in the explorer.
 *
 * @author Jiri Sedlacek
 */
public final class RegisteredSnapshotCategories {

    private static RegisteredSnapshotCategories sharedInstance;

    private final Set<SnapshotCategory> categories = Collections.synchronizedSet(new HashSet());


    /**
     * Returns singleton instance of RegisteredSnapshotCategories.
     * 
     * @return singleton instance of RegisteredSnapshotCategories.
     */
    public synchronized static RegisteredSnapshotCategories sharedInstance() {
        if (sharedInstance == null) sharedInstance = new RegisteredSnapshotCategories();
        return sharedInstance;
    }


    /**
     * Registers a SnapshotCategory.
     * 
     * @param category SnapshotCategory.
     */
    public void addCategory(SnapshotCategory category) {
        categories.add(category);
    }

    /**
     * Unregisters a SnapshotCategory.
     * 
     * @param category SnapshotCategory.
     */
    public void removeCategory(SnapshotCategory category) {
        categories.remove(category);
    }

    // TODO: implement some kind of stable sorting (introduce getPreferredPosition)
    /**
     * Returns list of registered SnapshotCategory instances.
     * 
     * @return list of registered SnapshotCategory instances.
     */
    public List<SnapshotCategory> getCategories() {
        return new ArrayList(categories);
    }
    
    
    private RegisteredSnapshotCategories() {}

}
