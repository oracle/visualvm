/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.model;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class JFRModelProvider extends AbstractModelProvider<JFRModel, DataSource> {
    
    private static final Logger LOGGER = Logger.getLogger(JFRModelProvider.class.getName());
    
    
    private final String id;
    private final int priority;
    
    
    protected JFRModelProvider(String id, int priority) {
        this.id = id;
        this.priority = priority;
    }
    
    
    protected abstract JFRModel createModel(String id, File file) throws Exception;
    
    
    @Override
    public final JFRModel createModelFor(final DataSource dataSource) {
        if (dataSource instanceof JFRSnapshot) {
            JFRSnapshot snapshot = (JFRSnapshot)dataSource;
            File file = snapshot.getFile();
            try {
                return createModel(id, file);
            } catch (OutOfMemoryError e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        DataSourceWindowManager.sharedInstance().closeDataSource(dataSource);
                        ProfilerDialogs.displayError("<html><br><b>Not enough memory to open JFR snapshot.</b><br><br>Please increase VisualVM heap size using the -Xmx parameter.</html>", "Out Of Memory", null);
                    }
                });
                
                LOGGER.log(Level.SEVERE, "Not enough memory to load JFR snapshot (" + id + "): " + file);   // NOI18N
                
                return JFRModel.OOME;
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not load JFR snapshot (" + id + "): " + file);   // NOI18N
            }
        }
        
        return null;
    }
    
    
    @Override
    public final int priority() {
        return priority;
    }
    
    @Override
    public final String toString() {
        return id;
    }
    
}
