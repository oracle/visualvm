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

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TimelinePanel extends JPanel {

    private final ChartPanel chartPanel;
    private final RowMouseHandler mouseHandler;


    // --- Constructor ---------------------------------------------------------
    
    public TimelinePanel(TimelineSupport support) {
        super(new BorderLayout());
        setOpaque(false);

        ProbesPanel probesPanel = new ProbesPanel(support);
        chartPanel = new ChartPanel(support.getChart(), support);

        add(probesPanel, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);

        new ProbesWheelHandler(chartPanel, probesPanel).register();
        mouseHandler = new RowMouseHandler(support, probesPanel);
        mouseHandler.register();
    }


    // --- Public interface ----------------------------------------------------

    public void reset() {
        chartPanel.resetChart();
        resetSelection();
    }

    public void resetSelection() {
        chartPanel.resetSelection();
    }

    public void updateActions() {
        chartPanel.updateActions();
    }

    public Action zoomInAction() {
        return chartPanel.zoomInAction();
    }

    public Action zoomOutAction() {
        return chartPanel.zoomOutAction();
    }

    public Action toggleViewAction() {
        return chartPanel.toggleViewAction();
    }

    public AbstractButton mouseZoom() {
        return chartPanel.mouseZoom();
    }

    public AbstractButton mouseHScroll() {
        return chartPanel.mouseHScroll();
    }

    public AbstractButton mouseVScroll() {
        return chartPanel.mouseVScroll();
    }


    // --- Private implementation ----------------------------------------------

    private static class ProbesWheelHandler implements MouseWheelListener {

        private final ChartPanel chartPanel;
        private final ProbesPanel probesPanel;

        ProbesWheelHandler(ChartPanel chartPanel, ProbesPanel probesPanel) {
            this.chartPanel = chartPanel;
            this.probesPanel = probesPanel;
        }

        void register() {
            probesPanel.addMouseWheelListener(this);
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            chartPanel.vScroll(e);
        }

    }

    private static class RowMouseHandler extends MouseAdapter {

        private static final int RESIZE_RANGE = 3;

        private final TimelineSupport support;
        private final TimelineChart chart;
        private TimelineSelectionManager selection;
        private final ProbesPanel probesPanel;

        private int baseY;
        private int baseHeight;
        private TimelineChart.Row draggingRow;


        RowMouseHandler(TimelineSupport support, ProbesPanel probesPanel) {
            this.support = support;
            this.chart = support.getChart();
            this.selection = (TimelineSelectionManager)chart.getSelectionModel();
            this.probesPanel = probesPanel;
        }


        void register() {
            chart.addMouseListener(this);
            chart.addMouseMotionListener(this);
            probesPanel.getMouseTarget().addMouseListener(this);
            probesPanel.getMouseTarget().addMouseMotionListener(this);
        }


        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            updateRowState(e, SwingUtilities.isLeftMouseButton(e));
            chart.updateSelection(false, this);
            selection.setEnabled(draggingRow == null);
            updateCursor();
        }

        public void mouseReleased(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            chart.updateSelection(true, this);

            if (draggingRow == null && e.getSource() == chart)
                support.indexSelectionChanged(selection.getStartIndex(),
                                              selection.getEndIndex());
            
            updateRowState(e, false);
            updateCursor();

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    selection.setEnabled(true);
                }
            });
        }

        public void mouseMoved(MouseEvent e) {
            updateRowState(e, false);
            updateCursor();
        }

        public void mouseDragged(MouseEvent e){
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            if (draggingRow != null) {
                boolean checkStep = (e.getModifiers() & Toolkit.getDefaultToolkit().
                                     getMenuShortcutKeyMask()) == 0;
                chart.setRowHeight(draggingRow.getIndex(), baseHeight + e.getY() - baseY, checkStep);
            }
        }


        private void updateRowState(MouseEvent e, boolean updateSelection) {
            baseY = e.getY();
            draggingRow = chart.getNearestRow(baseY, RESIZE_RANGE, true);
            if (draggingRow != null) {
                baseHeight = draggingRow.getHeight();
            }
        }

        private void updateCursor() {
            if (draggingRow != null) {
                Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                chart.setCursor(resizeCursor);
                probesPanel.setCursor(resizeCursor);
            } else {
                Cursor defaultCursor = Cursor.getDefaultCursor();
                chart.setCursor(defaultCursor);
                probesPanel.setCursor(defaultCursor);
            }
        }

    }

}
