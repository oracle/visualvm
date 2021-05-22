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
package org.graalvm.visualvm.jfr.impl;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.core.ui.actions.MultiDataSourceAction;
import org.graalvm.visualvm.jfr.JFRSnapshotSupport;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 *
 */
class JFRStopAction extends MultiDataSourceAction<DataSource> {
    private static JFRStopAction INSTANCE;
    public static synchronized JFRStopAction instance() {
        if (INSTANCE == null) {
            INSTANCE = new JFRStopAction();
        }
        return INSTANCE;
    }

    private Set<Application> lastSelectedApplications = new HashSet();
    private final PropertyChangeListener stateListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            updateState(ActionUtils.getSelectedDataSources());
        }
    };

    private JFRStopAction() {
        super(DataSource.class);
        putValue(NAME, NbBundle.getMessage(JFRDumpAction.class, "MSG_JFR_Stop")); // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(JFRDumpAction.class, "LBL_JFR_Stop"));    // NOI18N
    }

    protected void actionPerformed(Set<DataSource> dataSources, ActionEvent actionEvent) {
        for (DataSource dataSource : dataSources) {
            if (dataSource instanceof Application) {
                Application application = (Application) dataSource;
                if (application.isLocalApplication()) {
                    JFRSnapshotSupport.jfrStopRecording(application);
                } else {
                    JFRSnapshotSupport.remoteJfrStopRecroding(application);
                }
                updateState(dataSources);
            }
        }
    }

    protected boolean isEnabled(Set<DataSource> dataSources) {
        for (DataSource dataSource : dataSources) {
            if (dataSource instanceof Application) {
                // TODO: Listener should only be registered when JFR dump is supported for the application
                Application application = (Application) dataSource;
                lastSelectedApplications.add(application);
                application.addPropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
                if (application.getState() != Stateful.STATE_AVAILABLE) {
                    return false;
                }
                if (application.isLocalApplication()) {
                    if (!JFRSnapshotSupport.supportsJfrStop(application)) {
                        return false;
                    }
                } else {
                    if (!JFRSnapshotSupport.supportsRemoteJfrStop(application)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    protected void updateState(Set<DataSource> dataSources) {
        if (!lastSelectedApplications.isEmpty()) {
            for (Application application : lastSelectedApplications) {
                application.removePropertyChangeListener(Stateful.PROPERTY_STATE, stateListener);
            }
        }
        lastSelectedApplications.clear();
        super.updateState(dataSources);
    }
}
