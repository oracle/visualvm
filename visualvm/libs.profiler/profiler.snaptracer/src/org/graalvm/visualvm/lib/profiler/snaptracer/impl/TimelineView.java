/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.Format;
import java.text.SimpleDateFormat;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing.VisibilityHandler;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.TimelinePanel;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.charts.axis.TimeAxisUtils;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.icons.TracerIcons;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.timeline.TimelineSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineView {

    private final TracerModel model;
    private TimelinePanel panel;

    private JButton selectAllButton;
    private JButton clearTimestampSelectionButton;
    private JLabel selectionLabel;

    private VisibilityHandler viewHandler;


    // --- Constructor ---------------------------------------------------------

    TimelineView(TracerModel model) {
        this.model = model;
    }


    // --- Internal interface --------------------------------------------------

    void reset() {
        if (panel != null) panel.reset();
    }

    void resetSelection() {
        if (panel != null) panel.resetSelection();
    }

    void updateActions() {
        if (panel != null) panel.updateActions();
    }

    Action zoomInAction() {
        if (panel != null) return panel.zoomInAction();
        return null;
    }

    Action zoomOutAction() {
        if (panel != null) return panel.zoomOutAction();
        return null;
    }

    Action toggleViewAction() {
        if (panel != null) return panel.toggleViewAction();
        return null;
    }

    AbstractButton mouseZoom() {
        if (panel != null) return panel.mouseZoom();
        return null;
    }

    AbstractButton mouseHScroll() {
        if (panel != null) return panel.mouseHScroll();
        return null;
    }

    AbstractButton mouseVScroll() {
        if (panel != null) return panel.mouseVScroll();
        return null;
    }


    void registerViewListener(VisibilityHandler viewHandler) {
        if (panel != null) {
            viewHandler.handle(panel);
        } else {
            this.viewHandler = viewHandler;
        }

    }

    boolean isShowing() {
        return panel != null && panel.isShowing();
    }

    // --- UI implementation ---------------------------------------------------

    @NbBundle.Messages({
        "TOOLTIP_SelectAll=Select all",
        "TOOLTIP_ClearMarks=Clear marks"
    })
    JComponent getView() {
        final TimelineSupport support = model.getTimelineSupport();
        panel = new TimelinePanel(support);

        if (viewHandler != null) {
            viewHandler.handle(panel);
            viewHandler = null;
        }
        
        ProfilerToolbar toolbar = ProfilerToolbar.create(true);
        FileObject npssFo = model.getSnapshot().getNpssFileObject();
        toolbar.add(new ExportSnapshotAction(npssFo));
        toolbar.addSeparator();
        
        toolbar.add(panel.zoomInAction());
        toolbar.add(panel.zoomOutAction());
        toolbar.add(panel.toggleViewAction());
        toolbar.addSeparator();

        ButtonGroup bg = new ButtonGroup();
        AbstractButton mz = panel.mouseZoom();
        bg.add(mz);
        toolbar.add(mz);
        AbstractButton mh = panel.mouseHScroll();
        bg.add(mh);
        toolbar.add(mh);
        toolbar.addSeparator();

        selectAllButton = new JButton(Icons.getIcon(TracerIcons.SELECT_ALL)) {
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { support.selectAll(); }
                });
            }
        };
        selectAllButton.setToolTipText(Bundle.TOOLTIP_SelectAll());
        toolbar.add(selectAllButton);
        
        clearTimestampSelectionButton = new JButton(Icons.getIcon(TracerIcons.MARK_CLEAR)) {
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { support.resetSelectedTimestamps(); }
                });
            }
        };
        clearTimestampSelectionButton.setToolTipText(Bundle.TOOLTIP_ClearMarks());
        toolbar.add(clearTimestampSelectionButton);

        toolbar.addSeparator();
        selectionLabel = new JLabel();
        toolbar.add(selectionLabel);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        container.add(toolbar.getComponent(), BorderLayout.NORTH);
        container.add(panel, BorderLayout.CENTER);

        support.addSelectionListener( new TimelineSupport.SelectionListener() {
            public void intervalsSelectionChanged() {
                updateSelectionToolbar();
            }
            public void indexSelectionChanged() {
                updateSelectionToolbar();
            }
            public void timeSelectionChanged(boolean timestampsSelected, boolean justHovering) {
                updateSelectionToolbar();
            }
        });

        updateSelectionToolbar();

        return container;
    }

    private static final Format df = new SimpleDateFormat(TimeAxisUtils.TIME_MSEC);

    @NbBundle.Messages({
        "LBL_Selection=Selection:",
        "LBL_None=<none>",
        "LBL_SingleSample=sample #{0}",
        "LBL_TwoSamples=samples #{0} to #{1}",
        "LBL_TwoTimes={0} to {1}",
        "LBL_EntireSnapshot=entire snapshot"
    })
    private void updateSelectionToolbar() {
        TimelineSupport support = model.getTimelineSupport();
        selectAllButton.setEnabled(!support.isSelectAll());
        clearTimestampSelectionButton.setEnabled(support.isTimestampSelection(false));
        
        int startIndex = support.getStartIndex();
        int endIndex = support.getEndIndex();
        String selection = " " + Bundle.LBL_Selection() + " ";
        if (startIndex == -1) {
            selection += Bundle.LBL_None();
        }  else if (startIndex == endIndex) {
            selection += df.format(support.getTimestamp(startIndex)) + ", " + // NOI18N
                    Bundle.LBL_SingleSample(startIndex);
        }  else {
            long startTime = support.getTimestamp(startIndex);
            long endTime = support.getTimestamp(endIndex);
            selection += Bundle.LBL_TwoTimes(df.format(startTime), df.format(endTime));
            selection += " (" + (endTime - startTime) + " ms)";
            selection += ", " + Bundle.LBL_TwoSamples(startIndex, endIndex);
        }

        if (support.isSelectAll())
            selection += ", " + Bundle.LBL_EntireSnapshot();
        selectionLabel.setText(selection);
    }

}
