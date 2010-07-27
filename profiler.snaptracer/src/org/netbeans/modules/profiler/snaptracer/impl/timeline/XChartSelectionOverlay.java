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

import org.netbeans.lib.profiler.charts.ChartComponent;
import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.ChartSelectionListener;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.ItemSelection;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;

/**
 *
 * @author Jiri Sedlacek
 */
public class XChartSelectionOverlay extends ChartOverlay {

    private static final boolean FORCE_SPEED = Utils.forceSpeed();
    private static int REPAINT_COLLAPSE_LIMIT = 20;

    private ChartComponent chart;
    private int selectionMode;
    private Rectangle selectionBounds;

    private SelectionListener selectionListener;

    private boolean renderingOptimized;

    private Paint linePaint;
    private Stroke lineStroke;
    private int lineWidth = -1;

    private Paint fillPaint;

    private Stroke oddPerfStroke;
    private Stroke evenPerfStroke;

    private boolean drawTop;
    private boolean drawBottom;
    private boolean drawLeft;
    private boolean drawRight;


    public XChartSelectionOverlay() {
        selectionListener = new SelectionListener();
        initDefaultValues();
    }
    

    // --- Public API ----------------------------------------------------------

    public final void registerChart(ChartComponent chart) {
        unregisterListener();
        this.chart = chart;
        selectionMode = chart.getSelectionModel().getSelectionMode();
        registerListener();
    }

    public final void unregisterChart(ChartComponent chart) {
        unregisterListener();
        this.chart = null;
    }


    public final void setRenderingOptimized(boolean renderingOptimized) {
        this.renderingOptimized = renderingOptimized;
    }

    public final boolean isRenderingOptimized() {
        return renderingOptimized;
    }


    public final void setLineStroke(Stroke lineStroke) {
        this.lineStroke = lineStroke;
        lineWidth = -1;
    }

    public final Stroke getLineStroke() {
        return lineStroke;
    }

    public final void setLinePaint(Paint linePaint) {
        this.linePaint = linePaint;
    }

    public final Paint getLinePaint() {
        return linePaint;
    }

    public final void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
    }

    public final Paint getFillPaint() {
        return fillPaint;
    }

    public final void setLineMode(boolean drawTop, boolean drawLeft,
                                  boolean drawBottom, boolean drawRight) {
        this.drawTop = drawTop;
        this.drawLeft = drawLeft;
        this.drawBottom = drawBottom;
        this.drawRight = drawRight;
    }


    // --- Private implementation ----------------------------------------------

    private void registerListener() {
        if (chart == null) return;
        chart.getSelectionModel().addSelectionListener(selectionListener);
    }

    private void unregisterListener() {
        if (chart == null) return;
        chart.getSelectionModel().removeSelectionListener(selectionListener);
    }

    private void initDefaultValues() {
        setRenderingOptimized(true);

        Color systemSelection = Utils.getSystemSelection();

        setLineStroke(new BasicStroke(1));
        setLinePaint(systemSelection);

        setFillPaint(new Color(systemSelection.getRed(),
                               systemSelection.getGreen(),
                               systemSelection.getBlue(), 80));

        oddPerfStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 6, 6 }, 6);
        evenPerfStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 6, 6 }, 0);

        setLineMode(true, true, true, true);
    }


    private int getLineWidth() {
        if (lineWidth == -1)
            lineWidth = FORCE_SPEED ? 1 :
                        (int)Math.ceil(Utils.getStrokeWidth(lineStroke));
        return lineWidth;
    }

    private Rectangle normalizeRect(Rectangle rect, int border) {
        /*if (chart.fitsWidth()) {
            rect.x = 0;
            rect.width = getWidth();
        } else*/ if (rect.width < 0) {
            rect.x += rect.width;
            rect.width = 0 - rect.width;
        }

////        /*if (chart.fitsHeight()) {
////            rect.y = 0;
////            rect.height = getHeight();
////        } else*/ if (rect.height < 0) {
////            rect.y += rect.height;
////            rect.height = 0 - rect.height;
////        }
        rect.y = 0;
        rect.height = getHeight();

        rect.grow(border, border);

        return rect;
    }


    public void paint(Graphics g) {
//        System.err.println(">>> " + System.currentTimeMillis() + " paint clip: " + g.getClipBounds());
        if (selectionBounds == null) return;
        
        Graphics2D g2 = (Graphics2D)g;

//        if (selectionMode == ChartSelectionModel.SELECTION_LINE_V ||
//            selectionMode == ChartSelectionModel.SELECTION_CROSS) {
//            if (!forceSpeed && linePaint != null && lineStroke != null) {
//                g2.setPaint(linePaint);
//                g2.setStroke(lineStroke);
//                g2.drawLine(selectionBounds.x, 0, selectionBounds.x, getHeight());
//            } else if (forceSpeed) {
//                g2.setPaint(Color.BLACK);
//                g2.setStroke(evenPerfStroke);
//                g2.drawLine(selectionBounds.x, 0, selectionBounds.x, getHeight());
//                g2.setPaint(Color.WHITE);
//                g2.setStroke(oddPerfStroke);
//                g2.drawLine(selectionBounds.x, 0, selectionBounds.x, getHeight());
//            }
//        }

//        if (selectionMode == ChartSelectionModel.SELECTION_LINE_H ||
//            selectionMode == ChartSelectionModel.SELECTION_CROSS) {
//            if (!forceSpeed && linePaint != null && lineStroke != null) {
//                g2.setPaint(linePaint);
//                g2.setStroke(lineStroke);
//                g2.drawLine(0, selectionBounds.y, getWidth(), selectionBounds.y);
//            } else if (forceSpeed) {
//                g2.setPaint(Color.BLACK);
//                g2.setStroke(evenPerfStroke);
//                g2.drawLine(0, selectionBounds.y, getWidth(), selectionBounds.y);
//                g2.setPaint(Color.WHITE);
//                g2.setStroke(oddPerfStroke);
//                g2.drawLine(0, selectionBounds.y, getWidth(), selectionBounds.y);
//            }
//        }

//        if (selectionMode == ChartSelectionModel.SELECTION_RECT) {
        Rectangle bounds = normalizeRect(new Rectangle(selectionBounds), 0);

        if (selectionBounds.width != 0 /*|| selectionBounds.height != 0*/) {
//System.err.println(">>> rect... " + g2.getClipBounds());
            if (bounds.width == 0 || bounds.height == 0 ||
                chart.fitsWidth() && chart.fitsHeight()) return;

            if (fillPaint != null && !FORCE_SPEED) {
                Rectangle clip = g.getClipBounds();
                if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());

                g2.setPaint(fillPaint);
                g2.fill(clip.intersection(bounds));
            }

            if (!FORCE_SPEED && linePaint != null && lineStroke != null) {
                g2.setPaint(linePaint);
                g2.setStroke(lineStroke);
                drawRect(g2, bounds.x, bounds.y, bounds.width, bounds.height);
            } else if (FORCE_SPEED) {
                g2.setPaint(Color.BLACK);
                g2.setStroke(evenPerfStroke);
                drawRect(g2, bounds.x, bounds.y, bounds.width, bounds.height);
                g2.setPaint(Color.WHITE);
                g2.setStroke(oddPerfStroke);
                drawRect(g2, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } else {
            if (!FORCE_SPEED) {
                g2.setPaint(linePaint != null ? linePaint : fillPaint);
                g2.setStroke(lineStroke);
                g.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);
            } else if (FORCE_SPEED) {
                g2.setPaint(Color.BLACK);
                g2.setStroke(evenPerfStroke);
                g.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);
                g2.setPaint(Color.WHITE);
                g2.setStroke(oddPerfStroke);
                g.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);
            }
        }

    }

    private void drawRect(Graphics g, int x, int y, int w, int h) {
        if (drawTop) g.drawLine(x, y, x + w - 1, y);
        if (drawLeft) g.drawLine(x, y, x, y + h - 1);
        if (drawRight) g.drawLine(x + w - 1, y + h - 1, x + w - 1, y);
        if (drawBottom) g.drawLine(x + w - 1, y + h - 1, x, y + h - 1);
    }


    private class SelectionListener implements ChartSelectionListener {

        public void selectionModeChanged(int newMode, int oldMode) {
            selectionMode = newMode;
            if (selectionBounds == null) return;
            int lineW = getLineWidth();

//            if (oldMode == ChartSelectionModel.SELECTION_LINE_V ||
//                oldMode == ChartSelectionModel.SELECTION_CROSS)
//                paintImmediately(selectionBounds.x - lineW / 2, 0, lineW, getHeight());
//            if (oldMode == ChartSelectionModel.SELECTION_LINE_H ||
//                oldMode == ChartSelectionModel.SELECTION_CROSS)
//                paintImmediately(0, selectionBounds.y - lineW / 2, getWidth(), lineW);
            if (oldMode == ChartSelectionModel.SELECTION_RECT)
                paintImmediately(normalizeRect(new Rectangle(selectionBounds), lineW));

//            if (newMode == ChartSelectionModel.SELECTION_LINE_V ||
//                newMode == ChartSelectionModel.SELECTION_CROSS)
//                paintImmediately(selectionBounds.x - lineW / 2, 0, lineW, getHeight());
//            if (newMode == ChartSelectionModel.SELECTION_LINE_H ||
//                newMode == ChartSelectionModel.SELECTION_CROSS)
//                paintImmediately(0, selectionBounds.y - lineW / 2, getWidth(), lineW);
            if (newMode == ChartSelectionModel.SELECTION_RECT)
                paintImmediately(normalizeRect(new Rectangle(selectionBounds), lineW));
        }

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {
            selectionBounds = newBounds;
            int lineW = getLineWidth();

//            if (selectionMode == ChartSelectionModel.SELECTION_LINE_V ||
//                selectionMode == ChartSelectionModel.SELECTION_CROSS)
//                vLineBoundsChanged(newBounds, oldBounds, lineW);
//
//            if (selectionMode == ChartSelectionModel.SELECTION_LINE_H ||
//                selectionMode == ChartSelectionModel.SELECTION_CROSS)
//                hLineBoundsChanged(newBounds, oldBounds, lineW);

            if (selectionMode == ChartSelectionModel.SELECTION_RECT)
                rectBoundsChanged(newBounds, oldBounds, lineW);
        }

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {}

        public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {}

//        private void vLineBoundsChanged(Rectangle newBounds, Rectangle oldBounds, int lineW) {
//            if (newBounds != null && oldBounds != null) {
//                int dirtyWidth = Math.abs(oldBounds.x - newBounds.x);
//                if (dirtyWidth - lineW <= REPAINT_COLLAPSE_LIMIT) {
//                    paintImmediately(Math.min(oldBounds.x, newBounds.x) - lineW / 2, 0, dirtyWidth + lineW, getHeight());
//                    return;
//                }
//            }
//
//            if (newBounds != null) paintImmediately(newBounds.x - lineW / 2, 0, lineW, getHeight());
//            if (oldBounds != null) paintImmediately(oldBounds.x - lineW / 2, 0, lineW, getHeight());
//        }
//
//        private void hLineBoundsChanged(Rectangle newBounds, Rectangle oldBounds, int lineW) {
//            if (newBounds != null && oldBounds != null) {
//                int dirtyHeight = Math.abs(oldBounds.y - newBounds.y);
//                if (dirtyHeight - lineW <= REPAINT_COLLAPSE_LIMIT) {
//                    paintImmediately(0, Math.min(oldBounds.y, newBounds.y) - lineW / 2, getWidth(), dirtyHeight + lineW);
//                    return;
//                }
//            }
//
//            if (newBounds != null) paintImmediately(0, newBounds.y - lineW / 2, getWidth(), lineW);
//            if (oldBounds != null) paintImmediately(0, oldBounds.y - lineW / 2, getWidth(), lineW);
//        }

        private void rectBoundsChanged(Rectangle newBounds, Rectangle oldBounds, int lineW) {
            if (newBounds != null && oldBounds != null) {
                int x = newBounds.x + newBounds.width;
                int y = newBounds.y + newBounds.height;

                if (renderingOptimized) { // Painting just selection changes
                    int selX = newBounds.x;
                    int selY = newBounds.y;

                    int oldX = oldBounds.x + oldBounds.width;
                    int oldY = oldBounds.y + oldBounds.height;

                    int dx = Math.min(x, oldX);
                    int dwidth = Math.max(x, oldX) - dx;
                    int dy = Math.min(y, oldY);
                    int dheight = Math.max(y, oldY) - dy;

                    boolean crossX = oldBounds.width * newBounds.width < 0;
                    boolean crossY = oldBounds.height * newBounds.height < 0;

                    if (crossX || crossY) {
                        // Cross-quadrant move
                        if (crossX && !crossY) {
                            int cheight = oldBounds.height < 0 ?
                                  Math.min(oldBounds.height, newBounds.height) :
                                  Math.max(oldBounds.height, newBounds.height);
                            paintRect(dx, selY, dwidth, cheight, lineW);
                        } else if (!crossX && crossY) {
                            int cwidth = oldBounds.width < 0 ?
                                    Math.min(oldBounds.width, newBounds.width) :
                                    Math.max(oldBounds.width, newBounds.width);
                            paintRect(selX, dy, cwidth, dheight, lineW);
                        } else {
                            paintRect(dx, dy, dwidth, dheight, lineW);
                        }
                    } else {
                        // Move within the same quadrant
                        if (selX <= x) {
                            if (selY <= y) {
                                paintRect(dx, selY, dwidth, dy - selY + dheight, lineW);
                                paintRect(selX, dy, dx - selX + dwidth, dheight, lineW);
                            } else {
                                paintRect(dx, dy, dwidth, selY - dy, lineW);
                                paintRect(selX, dy, dx - selX, dheight, lineW);
                            }
                        } else {
                            if (selY <= y) {
                                paintRect(dx, selY, dwidth, dy - selY + dheight, lineW);
                                paintRect(dx + dwidth, dy, selX - dx, dheight, lineW);
                            } else {
                                paintRect(dx, dy, dwidth, selY - dy, lineW);
                                paintRect(dx + dwidth, dy, selX - dx - dwidth, dheight, lineW);
                            }
                        }
                    }
                } else { // Painting whole selection area
                    Rectangle oldB = normalizeRect(oldBounds, lineW);
                    Rectangle newB = normalizeRect(new Rectangle(newBounds), lineW);
                    repaint(oldB.union(newB));
                }
                return;
            }

            if (oldBounds != null)
                paintImmediately(normalizeRect(new Rectangle(oldBounds), lineW));
            else if (newBounds != null)
                paintImmediately(normalizeRect(new Rectangle(newBounds), lineW));
        }

        private void paintRect(int x, int y, int w, int h, int t) {
            paintImmediately(normalizeRect(new Rectangle(x, y, w, h), t));
        }

    }

}
