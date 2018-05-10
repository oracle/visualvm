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

package org.graalvm.visualvm.core.datasupport;

import java.util.Collections;
import java.util.Set;


/**
 * An event containing information about added, removed and actual items of a monitored repository.
 *
 * @author Tomas Hurka
 */
public final class DataChangeEvent<X> {

    private static final Set EMPTY_SET = Collections.EMPTY_SET;

    private Set<X> current;
    private Set<X> added;
    private Set<X> removed;


    /**
     * Creates new DataChangeEvent.
     * 
     * @param current actual items of a monitored repository,
     * @param added items recently added to a monitored repository,
     * @param removed items recently removed from a monitored repository.
     */
    public DataChangeEvent(Set<X> current, Set<X> added, Set<X> removed) {
        this.current = current == null ? EMPTY_SET : current;
        this.added = added == null ? EMPTY_SET : added;
        this.removed = removed == null ? EMPTY_SET : removed;
    }

    /**
     * Returns actual items of a monitored repository.
     * 
     * @return actual items of a monitored repository.
     */
    public Set<X> getCurrent() {
        return current;
    }

    /**
     * Returns items recently added to a monitored repository,
     * 
     * @return items recently added to a monitored repository,
     */
    public Set<X> getAdded() {
        return added;
    }

    /**
     * Returns items recently removed from a monitored repository.
     * 
     * @return items recently removed from a monitored repository.
     */
    public Set<X> getRemoved() {
        return removed;
    }

}
