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
public interface ChartConfigurationListener {

    // Called immediately after the change
    public void offsetChanged(long oldOffsetX, long oldOffsetY,
                              long newOffsetX, long newOffsetY);

    // Called immediately after the change
    public void scaleChanged(double oldScaleX, double oldScaleY,
                             double newScaleX, double newScaleY);

    // Called immediately after the change
    public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                  long dataWidth, long dataHeight,
                                  long oldDataOffsetX, long oldDataOffsetY,
                                  long oldDataWidth, long oldDataHeight);

    // Called before paintContents(Graphics, Rectangle)
    public void contentsWillBeUpdated(long offsetX, long offsetY,
                            double scaleX, double scaleY,
                            long lastOffsetX, long lastOffsetY,
                            double lastScaleX, double lastScaleY);

    // Called after paintContents(Graphics, Rectangle)
    // The actual change may/not be already displayed depending on buffer type
    public void contentsUpdated(long offsetX, long offsetY,
                            double scaleX, double scaleY,
                            long lastOffsetX, long lastOffsetY,
                            double lastScaleX, double lastScaleY,
                            int shiftX, int shiftY);


    public abstract class Adapter implements ChartConfigurationListener {

        public void offsetChanged(long oldOffsetX, long oldOffsetY,
                                  long newOffsetX, long newOffsetY) {}

        public void scaleChanged(double oldScaleX, double oldScaleY,
                                 double newScaleX, double newScaleY) {}

        public void dataBoundsChanged(long dataOffsetX, long dataOffsetY,
                                      long dataWidth, long dataHeight,
                                      long oldDataOffsetX, long oldDataOffsetY,
                                      long oldDataWidth, long oldDataHeight) {}

        public void contentsWillBeUpdated(long offsetX, long offsetY,
                                double scaleX, double scaleY,
                                long lastOffsetX, long lastOffsetY,
                                double lastScaleX, double lastScaleY) {}

        public void contentsUpdated(long offsetX, long offsetY,
                                double scaleX, double scaleY,
                                long lastOffsetX, long lastOffsetY,
                                double lastScaleX, double lastScaleY,
                                int shiftX, int shiftY) {}

    }

}
