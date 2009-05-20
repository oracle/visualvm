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

package org.netbeans.lib.profiler.charts.xy;

import org.netbeans.lib.profiler.charts.ChartItemChange;
import org.netbeans.lib.profiler.charts.swing.LongRect;

/**
 *
 * @author Jiri Sedlacek
 */
public interface XYItemChange extends ChartItemChange {

    public XYItem getItem();

    public int[] getValuesIndexes();

    public LongRect getOldValuesBounds();

    public LongRect getNewValuesBounds();

    public LongRect getDirtyValuesBounds();


    public static class Default extends ChartItemChange.Default implements XYItemChange {

        private final int[] valuesIndexes;
        private final LongRect oldBounds;
        private final LongRect newBounds;
        private final LongRect dirtyBounds;


        public Default(XYItem item, int[] valuesIndexes, LongRect oldBounds,
                       LongRect newBounds, LongRect dirtyBounds) {
            super(item);
            this.valuesIndexes = valuesIndexes;
            this.oldBounds = oldBounds;
            this.newBounds = newBounds;
            this.dirtyBounds = dirtyBounds;
        }


        public XYItem getItem() { return (XYItem)super.getItem(); }

        public int[] getValuesIndexes() { return valuesIndexes; }

        public LongRect getOldValuesBounds() { return oldBounds; }

        public LongRect getNewValuesBounds() { return newBounds; }

        public LongRect getDirtyValuesBounds() { return dirtyBounds; }

    }

}
