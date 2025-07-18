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

package org.graalvm.visualvm.lib.charts;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ItemsModel {

    public int getItemsCount();

    public ChartItem getItem(int index);

    public void addItemsListener(ItemsListener listener);

    public void removeItemsListener(ItemsListener listener);


    public static abstract class Abstract implements ItemsModel {

        private List<ItemsListener> listeners;


        public void addItemsListener(ItemsListener listener) {
            if (listeners == null) listeners = new ArrayList<>();
            if (!listeners.contains(listener)) listeners.add(listener);
        }

        public void removeItemsListener(ItemsListener listener) {
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) listeners = null;
            }
        }


        protected void fireItemsAdded(List<ChartItem> addedItems) {
            if (listeners != null)
                for (ItemsListener listener : listeners)
                    listener.itemsAdded(addedItems);
        }

        protected void fireItemsRemoved(List<ChartItem> removedItems) {
            if (listeners != null)
                for (ItemsListener listener : listeners)
                    listener.itemsRemoved(removedItems);
        }

        protected void fireItemsChanged(List<ChartItemChange> itemChanges) {
            if (listeners != null)
                for (ItemsListener listener : listeners)
                    listener.itemsChanged(itemChanges);
        }

    }

}
