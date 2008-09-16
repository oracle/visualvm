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
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;
import javax.swing.AbstractAction;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.NbBundle;

/**
 * Abstract Action which can be used as a basis for any DataSource-aware action.
 *
 * @author Jiri Sedlacek
 */
public abstract class DataSourceAction<X extends DataSource> extends AbstractAction {

    private final Class<X> scope;
    private boolean initialized = false;


    /**
     * Creates new instance of DataSourceAction available for defined DataSource type.
     * 
     * @param scope DataSource type for the action.
     */
    public DataSourceAction(Class<X> scope) {
        this.scope = scope;
    }
    
    
    /**
     * Updates enabled state based on currently selected DataSources in Applications window.
     * 
     * @param selectedDataSources currently selected DataSources in Applications window.
     */
    protected abstract void updateState(Set<X> selectedDataSources);

    /**
     * Displays a dialog that the action cannot be invoked in current context.
     */
    protected void notifyCannotPerform() {
        NetBeansProfiler.getDefaultNB().displayError(NbBundle.getMessage(DataSourceAction.class, "MSG_Cannot_perform_action_in_this_context")); // NOI18N
    }
    
    /**
     * Initializes the action. By default registers selection listener which invokes updateState(Set<DataSource>) on selection change.
     */
    protected void initialize() {
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                Set<X> selectedFiltered = Utils.getFilteredSet(selected, getScope());
                if (selectedFiltered.size() == selected.size()) DataSourceAction.this.updateState(selectedFiltered);
                else updateState(Collections.EMPTY_SET);
            }
        });
        
        updateState(ActionUtils.getSelectedDataSources(getScope()));
    }
    
    public final Object getValue(String key) {
        doInitialize();
        return super.getValue(key);
    }
    
    public final boolean isEnabled() {
        doInitialize();
        return super.isEnabled();
    }
    
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        doInitialize();
        super.addPropertyChangeListener(listener);
    }


    /**
     * Returns DataSource type for this action.
     * 
     * @return DataSource type for this action.
     */
    public final Class<X> getScope() {
        return scope;
    }
    
    
    private synchronized void doInitialize() {
        if (initialized) return;
        initialized = true;
        initialize();
    }
    
}
