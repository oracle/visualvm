/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: synchronize
/**
 * A repository of registered SnapshotCategory instances.
 *
 * @author Jiri Sedlacek
 */
public final class RegisteredSnapshotCategories {

    private static RegisteredSnapshotCategories sharedInstance;

    private final Set<SnapshotCategoriesListener> listeners = Collections.synchronizedSet(new HashSet());
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
     * Adds a listener for changes of registered snapshot categories.
     * 
     * @param listener listener for changes of registered snapshot categories.
     */
    public void addCategoriesListener(SnapshotCategoriesListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a listener for changes of registered snapshot categories.
     * 
     * @param listener listener for changes of registered snapshot categories.
     */
    public void removeCategoriesListener(SnapshotCategoriesListener listener) {
        listeners.remove(listener);
    }
    

    /**
     * Registers a SnapshotCategory.
     * 
     * @param category SnapshotCategory.
     */
    public void registerCategory(SnapshotCategory category) {
        categories.add(category);
        fireCategoryRegistered(category);
    }

    /**
     * Unregisters a SnapshotCategory.
     * 
     * @param category SnapshotCategory.
     */
    public void unregisterCategory(SnapshotCategory category) {
        categories.remove(category);
        fireCategoryUnregistered(category);
    }

    /**
     * Returns list of registered SnapshotCategory instances to be shown in UI.
     * 
     * @return list of registered SnapshotCategory instances to be shown in UI.
     */
    public List<SnapshotCategory> getVisibleCategories() {
        List<SnapshotCategory> allCategories = new ArrayList(categories);
        List<SnapshotCategory> visibleCategories = new ArrayList();
        for (SnapshotCategory category : allCategories)
            if (category.getPreferredPosition() != SnapshotCategory.POSITION_NONE)
                visibleCategories.add(category);
        
        Collections.sort(visibleCategories, Positionable.COMPARATOR);
        return visibleCategories;
    }
    
    /**
     * Returns list of registered SnapshotCategory instances capable of opening snapshot files.
     * 
     * @return list of registered SnapshotCategory instances capable of opening snapshot files.
     */
    public List<SnapshotCategory> getOpenSnapshotCategories() {
        List<SnapshotCategory> allCategories = new ArrayList(categories);
        List<SnapshotCategory> openSnapshotCategories = new ArrayList();
        for (SnapshotCategory category : allCategories)
            if (category.supportsOpenSnapshot()) openSnapshotCategories.add(category);
        
        Collections.sort(openSnapshotCategories, Positionable.COMPARATOR);
        return openSnapshotCategories;
    }
    
    /**
     * Returns list of all registered SnapshotCategory instances.
     * 
     * @return list of all registered SnapshotCategory instances.
     */
    public List<SnapshotCategory> getAllCategories() {
        List<SnapshotCategory> allCategories = new ArrayList(categories);
        Collections.sort(allCategories, Positionable.COMPARATOR);
        return allCategories;
    }
    
    
    private void fireCategoryRegistered(SnapshotCategory category) {
        Set<SnapshotCategoriesListener> listenersSet = new HashSet(listeners);
        for (SnapshotCategoriesListener listener : listenersSet)
            listener.categoryRegistered(category);
    }
    
    private void fireCategoryUnregistered(SnapshotCategory category) {
        Set<SnapshotCategoriesListener> listenersSet = new HashSet(listeners);
        for (SnapshotCategoriesListener listener : listenersSet)
            listener.categoryUnregistered(category);
    }
    
    
    private RegisteredSnapshotCategories() {}

}
