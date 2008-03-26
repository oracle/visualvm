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

package com.sun.tools.visualvm.core.ui.actions;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.explorer.ExplorerSelectionListener;
import com.sun.tools.visualvm.core.explorer.ExplorerSupport;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.netbeans.modules.profiler.NetBeansProfiler;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSourceAction<X extends DataSource> extends AbstractAction {
    
    private static final Logger LOGGER = Logger.getLogger(DataSourceAction.class.getName());

    private final Class<X> scope;
    private boolean initialized = false;
    private boolean initializedChecked = false;


    public DataSourceAction(Class<X> scope) {
        this.scope = scope;
    }
    
    
    protected abstract void updateState(Set<X> selectedDataSources);

    protected void notifyCannotPerform() {
        NetBeansProfiler.getDefaultNB().displayError("Cannot perform action in this context");
    }
    
    protected synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                if (selected.isEmpty()) selected = Collections.singleton(DataSource.ROOT);
                Set<X> selectedFiltered = Utils.getFilteredSet(selected, getScope());
                if (selectedFiltered.size() == selected.size()) DataSourceAction.this.updateState(selectedFiltered);
                else updateState(Collections.EMPTY_SET);
            }
        });
        
        updateState(ActionUtils.getSelectedDataSources(getScope()));
    }
    
    public Object getValue(String key) {
        if (!initializedChecked) {
            initializedChecked = true;
            if (!initialized) LOGGER.log(Level.WARNING, "Registered action not initialized: " + this, this);
        }
        return super.getValue(key);
    }


    public final Class<X> getScope() {
        return scope;
    }
    
}
