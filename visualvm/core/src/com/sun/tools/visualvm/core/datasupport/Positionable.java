/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.core.datasupport;

import java.util.Comparator;

/**
 * Common interface for entities that can specify their position within other entities.
 *
 * @author Jiri Sedlacek
 */
public interface Positionable {
    
    /**
     * Entity will be placed at the current last position.
     */
    public static final int POSITION_AT_THE_END = Integer.MAX_VALUE;
    
    /**
     * Comparator based on getPreferredPosition() value.
     */
    public static final Comparator COMPARATOR = new PositionableComparator();
    
    /**
     * Returns preferred position of this entity within other entities.
     * 
     * @return preferred position of this entity within other entities.
     */
    public int getPreferredPosition();
    
    /**
     * Implementation of Comparator based on getPreferredPosition() value.
     */
    static final class PositionableComparator implements Comparator {
        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         *
         * @param   o the object to be compared.
         * @return  a negative integer, zero, or a positive integer as this object
         *		is less than, equal to, or greater than the specified object.
         *
         * @throws ClassCastException if the specified object's type prevents it
         *         from being compared to this object.
         */
        public int compare(Object o1, Object o2) {
            Positionable p1 = (Positionable)o1;
            Positionable p2 = (Positionable)o2;
            
            int position1 = p1.getPreferredPosition();
            int position2 = p2.getPreferredPosition();
            
            if (position1 == position2) return 0;
            if (position1 > position2) return 1;
            return -1;
        }
        
    }

}
