/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.memory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.results.memory.LivenessMemoryResultsSnapshot;
import org.graalvm.visualvm.lib.profiler.api.GoToSource;
import org.graalvm.visualvm.lib.ui.UIUtils;


/**
 * This class implements presentation frames for Object Liveness Profiling.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class SnapshotLivenessResultsPanel extends LivenessResultsPanel implements ActionListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle"); // NOI18N
    private static final String GO_SOURCE_POPUP_ITEM = messages.getString("SnapshotLivenessResultsPanel_GoSourcePopupItem"); // NOI18N
    private static final String STACK_TRACES_POPUP_ITEM = messages.getString("SnapshotLivenessResultsPanel_StackTracesPopupItem"); // NOI18N
                                                                                                                                   // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JMenuItem popupShowSource;
    private JMenuItem popupShowStacks;
    private JPopupMenu popup;
    private LivenessMemoryResultsSnapshot snapshot;
    private int allocTrackEvery;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnapshotLivenessResultsPanel(LivenessMemoryResultsSnapshot snapshot, MemoryResUserActionsHandler actionsHandler,
                                        int allocTrackEvery) {
        super(actionsHandler);
        this.snapshot = snapshot;
        this.allocTrackEvery = allocTrackEvery;

        fetchResultsFromSnapshot();
        //prepareResults();
        initColumnsData();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == popupShowStacks) {
            actionsHandler.showStacksForClass(selectedClassId, getSortingColumn(), getSortingOrder());
        } else if (source == popupShowSource && popupShowSource != null) {
            showSourceForClass(selectedClassId);
        }
    }

    protected String getClassName(int classId) {
        return snapshot.getClassName(classId);
    }

    protected String[] getClassNames() {
        return snapshot.getClassNames();
    }

    protected int getPercentsTracked() {
        return 100 / allocTrackEvery;
    }

    protected JPopupMenu getPopupMenu() {
        if (popup == null) {
            popup = new JPopupMenu();

            if (GoToSource.isAvailable()) {
                Font boldfont = popup.getFont().deriveFont(Font.BOLD);

                popupShowSource = new JMenuItem();
                popupShowSource.setText(GO_SOURCE_POPUP_ITEM);
                popupShowSource.setFont(boldfont);
                popup.add(popupShowSource);
                popupShowSource.addActionListener(this);
            }

            if (snapshot.containsStacks()) {
                if (GoToSource.isAvailable()) popup.addSeparator();
                popupShowStacks = new JMenuItem();
                popupShowStacks.setText(STACK_TRACES_POPUP_ITEM);
                popup.add(popupShowStacks);
                popupShowStacks.addActionListener(this);
            }
        }

        return popup;
    }

    protected void performDefaultAction(int classId) {
        showSourceForClass(classId);
    }

    private void fetchResultsFromSnapshot() {
        nTrackedAllocObjects = UIUtils.copyArray(snapshot.getNTrackedAllocObjects());
        nTrackedLiveObjects = UIUtils.copyArray(snapshot.getNTrackedLiveObjects());
        trackedLiveObjectsSize = UIUtils.copyArray(snapshot.getTrackedLiveObjectsSize());
        nTotalAllocObjects = UIUtils.copyArray(snapshot.getnTotalAllocObjects());
        avgObjectAge = UIUtils.copyArray(snapshot.getAvgObjectAge());
        maxSurvGen = UIUtils.copyArray(snapshot.getMaxSurvGen());
        nInstrClasses = snapshot.getNInstrClasses();

        nTrackedItems = snapshot.getNTrackedItems();
        // Operations necessary for correct bar representation of results
        maxValue = snapshot.getMaxValue();
        nTotalTrackedBytes = snapshot.getNTotalTrackedBytes();
        nTotalTracked = snapshot.getNTotalTracked();

        initDataUponResultsFetch();
    }
}
