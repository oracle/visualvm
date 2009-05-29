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

package org.netbeans.lib.profiler.charts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
public interface PaintersModel {

    public ItemPainter getPainter(ChartItem item);

    public void addPaintersListener(PaintersListener listener);

    public void removePaintersListener(PaintersListener listener);


    public static abstract class Abstract implements PaintersModel {

        private List<PaintersListener> listeners;


        public void addPaintersListener(PaintersListener listener) {
            if (listeners == null) listeners = new ArrayList();
            if (!listeners.contains(listener)) listeners.add(listener);
        }

        public void removePaintersListener(PaintersListener listener) {
            if (listeners != null) listeners.remove(listener);
            if (listeners.isEmpty()) listeners = null;
        }


        protected void firePaintersChanged() {
            if (listeners != null)
                for (PaintersListener listener : listeners)
                    listener.paintersChanged();
        }

        protected void firePaintersChanged(List<ItemPainter> changedPainters) {
            if (listeners != null)
                for (PaintersListener listener : listeners)
                    listener.paintersChanged(changedPainters);
        }

    }


    public static class Default extends Abstract {

        private final Map<ChartItem, ItemPainter> painters;


        public Default() {
            painters = new HashMap();
        }

        public Default(ChartItem[] items, ItemPainter[] painters) {
            this();

            if (items == null)
                throw new IllegalArgumentException("Items cannot be null"); // NOI18N
            if (painters == null)
                throw new IllegalArgumentException("Painters cannot be null"); // NOI18N
            if (items.length != painters.length)
                throw new IllegalArgumentException("Items don't match painters"); // NOI18N

            addPainters(items, painters);
        }


        public void addPainters(ChartItem[] addedItems, ItemPainter[] addedPainters) {
            for (int i = 0; i < addedItems.length; i++)
                painters.put(addedItems[i], addedPainters[i]);
        }

        public void removePainters(ChartItem[] removedItems) {
            for (int i = 0; i < removedItems.length; i++)
                painters.remove(removedItems[i]);
        }


        public ItemPainter getPainter(ChartItem item) {
            return painters.get(item);
        }

    }

}
