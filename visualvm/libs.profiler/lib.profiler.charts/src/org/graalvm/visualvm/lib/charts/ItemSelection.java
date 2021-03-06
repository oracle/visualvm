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

/**
 *
 * @author Jiri Sedlacek
 */
public interface ItemSelection {

    public static final int DISTANCE_UNKNOWN = Integer.MAX_VALUE;


    public ChartItem getItem();

    public int getDistance();


    public static class Default implements ItemSelection {

        private ChartItem item;
        private int distance;


        public Default(ChartItem item) {
            this(item, DISTANCE_UNKNOWN);
        }

        public Default(ChartItem item, int distance) {
            this.item = item;
            this.distance = distance;
        }


        public ChartItem getItem() {
            return item;
        }

        public int getDistance() {
            return distance;
        }


        public boolean equals(Object o) {
            if (!(o instanceof ItemSelection)) return false;
            ItemSelection selection = (ItemSelection)o;
            return selection.getItem() == item;
        }

        public int hashCode() {
            return item.hashCode();
        }

    }

}
