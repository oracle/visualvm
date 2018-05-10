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

package org.graalvm.visualvm.sampler;

import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import java.util.Timer;
import org.graalvm.visualvm.lib.common.ProfilingSettings;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractSamplerSupport {

    public abstract DataViewComponent.DetailsView[] getDetailsView();

    public abstract boolean startSampling(ProfilingSettings settings, int samplingRate, int refreshRate);

    public abstract void stopSampling();

    public abstract void terminate();


    protected abstract Timer getTimer();


    public static abstract class Refresher {
        private static final long REFRESH_THRESHOLD = 100;
        private long lastRefresh;

        public synchronized final void refresh() {
            if (checkRefresh()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRefresh >= REFRESH_THRESHOLD) {
                    lastRefresh = currentTime;
                    doRefresh();
                }
            }
        }

        public abstract void setRefreshRate(int refreshRate);
        public abstract int getRefreshRate();

        protected abstract boolean checkRefresh();
        protected abstract void doRefresh();
    }

}
