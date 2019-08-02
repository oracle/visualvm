/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.console.r;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.ProfilerStorage;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "CustomRQueries_SaveFailed=Failed to save R scripts.",
    "CustomRQueries_LoadFailed=Failed to load saved R scripts."
}) 
public final class CustomRQueries {
    
    private static final String SAVED_R_QUERIES_FILENAME = "rqueries"; // NOI18N
    
    private static final String PROP_QUERY_NAME_KEY = "r-query-name"; // NOI18N
    private static final String PROP_QUERY_DESCR_KEY = "r-query-descr"; // NOI18N
    private static final String PROP_QUERY_SCRIPT_KEY = "r-query-script"; // NOI18N
    
    
    private static CustomRQueries INSTANCE;
    
    private List<RQueries.Query> customQueries;
    
    
    public static synchronized CustomRQueries instance() {
        if (INSTANCE == null) INSTANCE = new CustomRQueries();
        return INSTANCE;
    }
    
    
    public synchronized boolean isEmpty() {
        return customQueries.isEmpty();
    }
    
    public synchronized void add(RQueries.Query query) {
        customQueries.add(0, query);
        save();
    }
    
    public synchronized void save(RQueries.Query query) {
        for (RQueries.Query q : customQueries) {
            if (q.getName().equals(query.getName())) {
                q.setScript(query.getScript());
                save();
                break;
            }
        }
    }
    
    public synchronized void set(List<RQueries.Query> queries) {
        customQueries.clear();
        customQueries.addAll(queries);
        save();
    }
    
    public synchronized List<RQueries.Query> list() {
        List<RQueries.Query> list = new ArrayList();
        for (RQueries.Query query : customQueries)
            list.add(new RQueries.Query(query.getScript(), query.getName(), query.getDescription()));
        return list;
    }
    
    
    private void save() {
        new RequestProcessor("R Scripts Saver").post(new Runnable() { // NOI18N
            public void run() {
                try {
                    Properties p = listToProperties(list());
                    ProfilerStorage.saveGlobalProperties(p, SAVED_R_QUERIES_FILENAME);
                } catch (Exception e) {
                    ProfilerDialogs.displayError(Bundle.CustomRQueries_SaveFailed());
                    Exceptions.printStackTrace(e);
                }
            }
        });
    }
    
    
    private static List<RQueries.Query> propertiesToList(List<RQueries.Query> queries, Properties properties) {
        int i = 0;
        while (properties.containsKey(PROP_QUERY_NAME_KEY + "-" + i)) { // NOI18N
            String name = properties.getProperty(PROP_QUERY_NAME_KEY + "-" + i); // NOI18N
            String description = properties.getProperty(PROP_QUERY_DESCR_KEY + "-" + i, null); // NOI18N
            String script = properties.getProperty(PROP_QUERY_SCRIPT_KEY + "-" + i, ""); // NOI18N
            
            if (name != null && script != null) queries.add(new RQueries.Query(script, name, description));
            
            i++;
        }
        
        return queries;
    }

    private static Properties listToProperties(List<RQueries.Query> queries) {
        Properties properties = new Properties();
        
        int i = 0;
        for (RQueries.Query query : queries) {
            properties.put(PROP_QUERY_NAME_KEY + "-" + i, query.getName().trim()); // NOI18N
            properties.put(PROP_QUERY_SCRIPT_KEY + "-" + i, query.getScript().trim()); // NOI18N
            
            String descr = query.getDescription();
            if (descr != null) properties.put(PROP_QUERY_DESCR_KEY + "-" + i, descr); // NOI18N
            
            i++;
        }

        return properties;
    }
    
    
    private CustomRQueries() {
        assert !SwingUtilities.isEventDispatchThread();
        
        customQueries = new ArrayList();
        
        try {
            Properties p = new Properties();
            ProfilerStorage.loadGlobalProperties(p, SAVED_R_QUERIES_FILENAME);
            propertiesToList(customQueries, p);
        } catch (Exception e) {
            ProfilerDialogs.displayError(Bundle.CustomRQueries_LoadFailed());
            Exceptions.printStackTrace(e);
        }
    }
    
}
