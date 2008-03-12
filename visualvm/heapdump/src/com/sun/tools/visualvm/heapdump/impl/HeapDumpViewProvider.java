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

package com.sun.tools.visualvm.heapdump.impl;

import com.sun.tools.visualvm.heapdump.HeapDump;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeapDumpViewProvider implements DataSourceViewsProvider<HeapDump>{
    
    private Map<HeapDump, DataSourceView> viewsCache = new HashMap();
    

    public boolean supportsViewsFor(HeapDump heapDump) {
        return true;
    }

    public synchronized Set<? extends DataSourceView> getViews(final HeapDump heapDump) {
        DataSourceView view = viewsCache.get(heapDump);
        if (view == null) {
            view = new HeapDumpView(heapDump) {
                public void removed() {
                    super.removed();
                    viewsCache.remove(heapDump);
                }
            };
            viewsCache.put(heapDump, view);
        }
        return Collections.singleton(view);
    }

    public void initialize() {
        DataSourceViewsFactory.sharedInstance().addViewProvider(this, HeapDump.class);
    }

}
