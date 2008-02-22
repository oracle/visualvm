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

package com.sun.tools.visualvm.core.threaddump;

import com.sun.tools.visualvm.core.datasource.ThreadDump;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.DataSourceWindowFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
class ThreadDumpViewProvider implements DataSourceViewsProvider<ThreadDump>{
    
    private final Map<ThreadDump, DataSourceView> viewsCache = new HashMap();
    

    public boolean supportsViewFor(ThreadDump threadDump) {
        return true;
    }
    
    boolean hasCachedView(ThreadDump threadDump) {
        return viewsCache.get(threadDump) != null;
    }

    public synchronized Set<? extends DataSourceView> getViews(final ThreadDump threadDump) {
        DataSourceView view = viewsCache.get(threadDump);
        if (view == null) {
            view = new ThreadDumpView(threadDump) {
                protected void removed() {
                    super.removed();
                    viewsCache.remove(threadDump);
                }
            };
            viewsCache.put(threadDump, view);
        }
        return Collections.singleton(view);
    }

    void initialize() {
        DataSourceWindowFactory.sharedInstance().addViewProvider(this, ThreadDump.class);
    }

}
