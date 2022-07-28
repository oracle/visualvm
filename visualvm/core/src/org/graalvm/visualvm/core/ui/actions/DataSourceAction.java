/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui.actions;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.core.explorer.ExplorerSelectionListener;
import org.graalvm.visualvm.core.explorer.ExplorerSupport;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;
import javax.swing.AbstractAction;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
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
        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message(
                NbBundle.getMessage(DataSourceAction.class,
                "MSG_Cannot_perform_action_in_this_context"), // NOI18N
                NotifyDescriptor.ERROR_MESSAGE));
    }
    
    /**
     * Initializes the action.
     * By default registers selection listener which invokes
     * {@link #updateState(Set) updateState(Set&lt;DataSource&gt;)} on selection change.
     */
    protected void initialize() {
        ExplorerSupport.sharedInstance().addSelectionListener(new ExplorerSelectionListener() {
            public void selectionChanged(Set<DataSource> selected) {
                Set<X> selectedFiltered = Utils.getFilteredSet(selected, getScope());
                if (selectedFiltered.size() == selected.size()) DataSourceAction.this.updateState(selectedFiltered);
                else updateState(Collections.emptySet());
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
