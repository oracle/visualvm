/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.snaptracer.impl;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.Format;
import java.text.SimpleDateFormat;
import org.netbeans.modules.profiler.snaptracer.impl.swing.VisibilityHandler;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.TimelinePanel;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.axis.TimeAxisUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.snaptracer.impl.icons.TracerIcons;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.TimelineSupport;
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
