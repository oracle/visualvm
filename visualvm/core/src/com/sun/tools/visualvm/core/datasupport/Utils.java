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

package com.sun.tools.visualvm.core.datasupport;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public final class Utils {

    public static <X, Y> boolean containsSubclass(Set<? extends Class<? extends Y>> classes, X superclassInstance) {
        for (Class<? extends Y> classs : classes) if (classs.isInstance(superclassInstance)) return true;
        return false;
    }

    public static <X, Y> boolean containsSuperclass(Set<? extends Class<? extends Y>> classes, X subclassInstance) {
        Class subclass = subclassInstance.getClass();
        for (Class<? extends Y> classs : classes) if (classs.isAssignableFrom(subclass)) return true;
        return false;
    }

    public static <X, Y extends X, Z extends X> Set<Z> getFilteredSet(Set<Y> set, Class<Z> filter) {
        Set<Z> filteredSet = new HashSet();
        for (Y item : set) if (filter.isInstance(item)) filteredSet.add((Z)item);
        return filteredSet;
    }

}
