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

package org.graalvm.visualvm.core.ui.actions;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.uisupport.UISupport;
import java.awt.event.ActionEvent;
import java.util.Set;

/**
 * Abstract DataSourceAction which can be used as a basis for any action available (enabled) just for one or more selected DataSources in Applications window..
 *
 * @author Jiri Sedlacek
 */
public abstract class MultiDataSourceAction<X extends DataSource> extends DataSourceAction<X> {
        
    /**
     * Creates new instance of MultiDataSourceAction available for defined DataSource type.
     * 
     * @param scope DataSource type for the action.
     */
    public MultiDataSourceAction(Class<X> scope) {
       super(scope);
    }
        
        
    public final void actionPerformed(ActionEvent e) {
        if (isEnabled()) {
            actionPerformed(ActionUtils.getSelectedDataSources(getScope()), e);
        } else {
            notifyCannotPerform();
        }
    }
        
        
    /**
     * Performs the action for the DataSources.
     * 
     * @param dataSources Set of DataSources for which to perform the action.
     * @param actionEvent ActionEvent for the action.
     */
    protected abstract void actionPerformed(Set<X> dataSources, ActionEvent actionEvent);
        
    /**
     * Returns true if the action is available (enabled) for the DataSources, false otherwise.
     * 
     * @param dataSources Set of DataSources for the action.
     * @return true if the action is available (enabled) for the DataSources, false otherwise.
     */
    protected abstract boolean isEnabled(Set<X> dataSources);
        
    protected void updateState(Set<X> selectedDataSources) {
        final boolean isEnabled = selectedDataSources.isEmpty() ? false : isEnabled(selectedDataSources);

        UISupport.runInEventDispatchThreadAndWait(new Runnable() {
            public void run() { setEnabled(isEnabled); }
        });
    }
        
 }
