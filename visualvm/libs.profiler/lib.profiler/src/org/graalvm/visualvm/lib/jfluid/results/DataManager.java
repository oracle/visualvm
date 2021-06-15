/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A common functionality for DataManagers
 *
 * @author Ian Formanek
 */
public abstract class DataManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Set listeners = new HashSet();

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Listeners ---------------------------------------------------------------

    /**
     * Adds new threadData Listener.
     *
     * @param listener threadData listener to add
     */
    public void addDataListener(DataManagerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes threadData listener.
     *
     * @param listener threadData listener to remove
     */
    public void removeDataListener(DataManagerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all listeners about the threadData change.
     */
    protected void fireDataChanged() {
        if (listeners.isEmpty()) {
            return;
        }

        Set toNotify;

        synchronized (listeners) {
            toNotify = new HashSet(listeners);
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            final DataManagerListener listener = ((DataManagerListener) iterator.next());
            listener.dataChanged();
        }
    }

    /**
     * Notifies all listeners about the reset of threads data.
     */
    protected void fireDataReset() {
        if (listeners.isEmpty()) {
            return;
        }

        Set toNotify;

        synchronized (listeners) {
            toNotify = new HashSet(listeners);
        }

        Iterator iterator = toNotify.iterator();

        while (iterator.hasNext()) {
            final DataManagerListener listener = ((DataManagerListener) iterator.next());
            listener.dataReset();
        }
    }
}
