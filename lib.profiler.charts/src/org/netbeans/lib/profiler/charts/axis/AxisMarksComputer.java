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

package org.netbeans.lib.profiler.charts.axis;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.charts.ChartContext;

/**
 *
 * @author Jiri Sedlacek
 */
public interface AxisMarksComputer {

    public static final Iterator<AxisMark> EMPTY_ITERATOR =
        new Iterator<AxisMark>() {
            public boolean hasNext() { return false; }
            public AxisMark next() { throw new NoSuchElementException(); }
            public void remove() { throw new IllegalStateException(); }
        };


    public Iterator<AxisMark> marksIterator(int start, int end);


    public static abstract class Abstract implements AxisMarksComputer {

        protected final ChartContext context;

        protected final int orientation;

        protected final boolean horizontal;
        protected final boolean reverse;


        public Abstract(ChartContext context, int orientation) {
            
            this.context = context;
            
            this.orientation = orientation;

            horizontal = orientation == SwingConstants.HORIZONTAL;
            reverse = horizontal ? context.isRightBased() :
                                   context.isBottomBased();
        }

        // Return minimum distance between two axis marks
        protected int getMinMarksDistance() {
            return 50;
        }

        // Returns true if the configuration changed and axis should be repainted
        protected boolean refreshConfiguration() {
            return true;
        }

    }


    public static abstract class AbstractIterator implements Iterator<AxisMark> {
        public void remove() {
            throw new UnsupportedOperationException(
                      "AxisMarksComputer does not support remove()"); // NOI18N
        }
    }

}
