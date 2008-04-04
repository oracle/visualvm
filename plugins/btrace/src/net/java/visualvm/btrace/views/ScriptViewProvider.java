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

package net.java.visualvm.btrace.views;

import com.sun.tools.visualvm.core.snapshot.Snapshot;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import com.sun.tools.visualvm.core.ui.DataSourceViewsProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.java.visualvm.btrace.datasource.ScriptDataSource;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ScriptViewProvider implements DataSourceViewsProvider<ScriptDataSource> {
    private final static ScriptViewProvider INSTANCE = new ScriptViewProvider();
    
    private final Map<ScriptDataSource, ScriptView> viewMap = new  HashMap<ScriptDataSource, ScriptView>();
    
    public Set<? extends DataSourceView> getViews(ScriptDataSource dataSource) {
        return Collections.singleton(getView(dataSource));
    }
    
    private DataSourceView getView(ScriptDataSource dataSource) {
        synchronized(viewMap) {
            if (viewMap.containsKey(dataSource)) {
                return viewMap.get(dataSource);
            }
            ScriptView view = new ScriptView(dataSource);
            viewMap.put(dataSource, view);
            return view;
        }
    }

    public boolean supportsViewsFor(ScriptDataSource dataSource) {
        return true;
    }
    
    public static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewsProvider(INSTANCE, ScriptDataSource.class);
    }
    
    public static void shutdown() {
        DataSourceViewsManager.sharedInstance().removeViewsProvider(INSTANCE);
        INSTANCE.viewMap.clear();
    }

    public void saveViews(ScriptDataSource script, Snapshot snapshot) {
        // TODO implement later
    }

    public boolean supportsSaveViewsFor(ScriptDataSource script) {
        return false;
    }
    
}
