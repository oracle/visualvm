/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.LinkedList;
import java.util.List;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class BarChart extends JComponent implements ComponentListener, AncestorListener, ChartModelListener, Accessible {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    List horizAxisXes = new LinkedList();
    int horizAxisHeight = 0;
    int horizLegendWidth = 0;
    int vertAxisWidth = 0;
    int vertLegendHeight = 0;
    private AccessibleContext accessibleContext;
    private BarChartModel model;
    private Graphics2D offScreenGraphics;
    private Image offScreenImage;
    private Insets insets;
    private Paint axisMeshPaint = new Color(80, 80, 80, 50);
    private Paint axisPaint = Color.BLACK;
    private Paint fillPaint = Color.CYAN;
    private Paint outlinePaint = Color.BLACK;
    private Stroke axisStroke = new BasicStroke(1f);
    private Stroke outlineStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND);
    private boolean draw3D = false;
    private boolean modelIncorrect = true;
    private boolean offScreenImageInvalid;
    private boolean offScreenImageSizeInvalid;
    private int drawHeight;
    private int drawWidth;
    private int leftOffset = 0;
    private int maxHeight;
    private int rightOffset = 0;
    private int topOffset = 0;
    private int xSpacing = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BarChart */
    public BarChart() {
        offScreenImageSizeInvalid = true;
        addAncestorListener(this);
        addComponentListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAccessibleContext(AccessibleContext accessibleContext) {
        this.accessibleContext = accessibleContext;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    // --- Public interface ------------------------------------------------------
    public void setDraw3D(boolean draw3D) {
        if (this.draw3D != draw3D) {
            this.draw3D = draw3D;
            chartDataChanged();
        }
    }

    public boolean getDraw3D() {
        return draw3D;
    }

    public void setFillPaint(Paint fillPaint) {
        if (!this.fillPaint.equals(fillPaint)) {
            this.fillPaint = fillPaint;
            chartDataChanged();
        }
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setFont(Font font) {
        if (!getFont().equals(font)) {
            super.setFont(font);
            chartDataChanged();
        }
    }

    public void setLeftOffset(int leftOffset) {
        if (this.leftOffset != leftOffset) {
            this.leftOffset = leftOffset;
            chartDataChanged();
        }
    }

    public int getLeftOffset() {
        return leftOffset;
    }

    public void setModel(BarChartModel model) {
        // automatically unregister itself as a ChartModelListener from current model
        if (this.model != null) {
            this.model.removeChartModelListener(this);
        }

        // automatically register itself as a ChartModelListener for new model
        if (model != null) {
            model.addChartModelListener(this);
        }

        // set new model
        this.model = model;

        // process data change
        chartDataChanged();
    }

    public BarChartModel getModel() {
        return model;
    }

    public void setOutlinePaint(Paint outlinePaint) {
        if (!this.outlinePaint.equals(outlinePaint)) {
            this.outlinePaint = outlinePaint;
            chartDataChanged();
        }
    }

    public Paint getOutlinePaint() {
        return outlinePaint;
    }

    public void setOutlineStroke(Stroke outlineStroke) {
        if (!this.outlineStroke.equals(outlineStroke)) {
            this.outlineStroke = outlineStroke;
            chartDataChanged();
        }
    }

    public Stroke getOutlineStroke() {
        return outlineStroke;
    }

    public void setRightOffset(int rightOffset) {
        if (this.rightOffset != rightOffset) {
            this.rightOffset = rightOffset;
            chartDataChanged();
        }
    }

    public int getRightOffset() {
        return rightOffset;
    }

    public void setTopOffset(int topOffset) {
        if (this.topOffset != topOffset) {
            this.topOffset = topOffset;
            chartDataChanged();
        }
    }

    public int getTopOffset() {
        return topOffset;
    }

    public void setXSpacing(int xSpacing) {
        if (this.xSpacing != xSpacing) {
            this.xSpacing = xSpacing;
            chartDataChanged();
        }
    }

    public int getXSpacing() {
        return xSpacing;
    }

    public void ancestorAdded(AncestorEvent event) {
        chartDataChanged();
    }

    public void ancestorMoved(AncestorEvent event) {
    }

    public void ancestorRemoved(AncestorEvent event) {
    }

    // Used for public chart update & listener implementation
    public void chartDataChanged() {
        if (model != null) {
            // assume model is incorrect
            modelIncorrect = true;

            // check model correctness
            String[] xItems = model.getXLabels();
            int[] yItems = model.getYValues();

            if (xItems == null) {
                throw new RuntimeException("X labels cannot be null!"); // NOI18N
            }

            if (yItems == null) {
                throw new RuntimeException("Y values cannot be null!"); // NOI18N
            }

            if ((xItems.length - 1) != yItems.length) {
                throw new RuntimeException("Incorrect x-y values count!"); // NOI18N
            }

            // model is correct
            modelIncorrect = false;

            // update max yvalue
            maxHeight = getMaxY(yItems);

            // update axes metrics
            if ((getFont() != null) && (getGraphics() != null) && (getGraphics().getFontMetrics() != null)) {
                horizAxisHeight = getFont().getSize() + 10;
                horizLegendWidth = (int) getGraphics().getFontMetrics().getStringBounds(model.getXAxisDesc(), getGraphics())
                                             .getWidth();

                int maxYMarkWidth = (int) getGraphics().getFontMetrics()
                                              .getStringBounds(Integer.toString(maxHeight) + "0", getGraphics()).getWidth() + 10;
                int vertLegendWidth = (int) getGraphics().getFontMetrics().getStringBounds(model.getYAxisDesc(), getGraphics())
                                                .getWidth();
                vertLegendHeight = getFont().getSize();
                vertAxisWidth = Math.max(maxYMarkWidth, vertLegendWidth + 4);
            } else {
                horizAxisHeight = 0;
                horizLegendWidth = 0;
                vertAxisWidth = 0;
            }
        }

        // paintComponent() may be running and would clear offScreenImageInvalid flag,
        // so this code has to be invoked later
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    offScreenImageInvalid = true;
                    repaint();
                }
            });
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        offScreenImageSizeInvalid = true;
        repaint();
    }

    // ---------------------------------------------------------------------------

    // --- ComponentListener & AncestorListener implementation ---------------------
    public void componentShown(ComponentEvent e) {
    }

    // ---------------------------------------------------------------------------

    // --- Static tester ---------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        BarChart barChart = new BarChart();

        BarChartModel barChartModel = new AbstractBarChartModel() {
            public String[] getXLabels() {
                return new String[] { "37", "41", "45", "49", "53", "57" };
            } // NOI18N

            public String getXAxisDesc() {
                return "[ms]";
            } // NOI18N

            public String getYAxisDesc() {
                return "[freq]";
            } // NOI18N

            public int[] getYValues() {
                return new int[] { 55, 60, 90, 80, 10 };
            }
        };

        barChart.setModel(barChartModel);

        barChart.setBackground(Color.WHITE);
        barChart.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        barChart.setPreferredSize(new Dimension(300, 200));

        barChart.setDraw3D(true);
        barChart.setLeftOffset(20);
        barChart.setRightOffset(5);
        barChart.setTopOffset(30);
        barChart.setXSpacing(10);

        JFrame frame = new JFrame("BarChart Tester"); // NOI18N
        frame.getContentPane().add(barChart);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    // ---------------------------------------------------------------------------

    // --- Internal implementation -----------------------------------------------
    public void paintComponent(Graphics g) {
        // super.paintComponent
        super.paintComponent(g);

        // check if ChartModel is assigned
        if ((model == null) || modelIncorrect) {
            return;
        }

        // check if the offScreenImage has to be updated
        if (offScreenImageSizeInvalid) {
            updateOffScreenImageSize();
        }

        // paint component to the offScreenImage
        if (offScreenImageInvalid) {
            drawChart(offScreenGraphics);
        }

        // paint offScreenImage to the output Graphics
        g.drawImage(offScreenImage, insets.left, insets.top, this);
    }

    protected void drawBar(Graphics2D g2, int startX, int startY, int width, int height) {
        Polygon topSide = null;
        Polygon rightSide = null;

        g2.setPaint(fillPaint);
        g2.fillRect(startX, startY, width, height);

        if (draw3D) {
            topSide = new Polygon();
            topSide.addPoint(startX, startY);
            topSide.addPoint(startX + (width / 3), startY - (width / 3));
            topSide.addPoint(startX + width + (width / 3), startY - (width / 3));
            topSide.addPoint(startX + width, startY);

            rightSide = new Polygon();
            rightSide.addPoint(startX + width, startY);
            rightSide.addPoint(startX + width + (width / 3), startY - (width / 3));
            rightSide.addPoint(startX + width + (width / 3), (startY + height) - (width / 3));
            rightSide.addPoint(startX + width, startY + height);

            if (fillPaint instanceof Color) {
                g2.setPaint(((Color) fillPaint).brighter());
            }

            g2.fillPolygon(topSide);

            if (fillPaint instanceof Color) {
                g2.setPaint(((Color) fillPaint).darker());
            }

            g2.fillPolygon(rightSide);
        }

        g2.setStroke(outlineStroke);
        g2.setPaint(outlinePaint);
        g2.drawRect(startX, startY, width, height);

        if (draw3D) {
            g2.drawPolygon(topSide);
            g2.drawPolygon(rightSide);
        }
    }

    protected void drawChart(Graphics2D g2) {
        // clear component area
        g2.setColor(getBackground());
        g2.fillRect(0, 0, drawWidth + 1, drawHeight + 1);

        // fetch data from model
        String[] xItems = model.getXLabels();
        int[] yItems = model.getYValues();

        // process only if data available and component has valid size
        if ((yItems.length > 0) && (drawWidth > 0) && (drawHeight > 0)) {
            // default outline stroke
            int outlineStrokeWidth = 0;

            // most likely stroke will be descendant of BasicStroke, set correct stroke width
            if (outlineStroke instanceof BasicStroke) {
                outlineStrokeWidth = (int) Math.ceil((((BasicStroke) outlineStroke).getLineWidth() - 1) / 2);
            }

            // initialize basic scene description
            int barsCount = yItems.length; // number of bars to be drawn
            int drawableWidth = drawWidth // effective width of drawing area
                                - vertAxisWidth // width of vertical axis
                                - horizLegendWidth // width of horizontal axis legend
                                - leftOffset // extra left spacing (between vertical axis and first bar)
                                - rightOffset // extra right spacing (betweel last bar and end of horizontal axis)
                                - (barsCount * xSpacing) // spacing between bars + one more before horizontal axis legend
                                - ((horizLegendWidth == 0) ? 0 : 5) // extra space before horizontal axis legend
                                - (outlineStrokeWidth * 2); // effective stroke width
            int drawableHeight = drawHeight // effective height of drawing area
                                 - topOffset // extra top spacing (between highest bar and end of vertical axis)
                                 - horizAxisHeight // height of horizontal axis
                                 - (outlineStrokeWidth * 2); // effective stroke width

            // initialize drawing status
            int drawnWidth = 0;
            int horizontal3DCorrection = 0;
            int vertical3DCorrection = 0;
            int currentX = vertAxisWidth + leftOffset + outlineStrokeWidth;
            horizAxisXes.clear();

            if (draw3D) {
                horizontal3DCorrection = drawableWidth / barsCount / 3;
                drawableWidth -= horizontal3DCorrection;
                vertical3DCorrection = drawableWidth / barsCount / 3;
                drawableHeight -= vertical3DCorrection;
            }

            // draw vertical chart axis
            drawVerticalAxis(g2, vertical3DCorrection, yItems);

            // draw each bar
            for (int i = 0; i < barsCount; i++) {
                int width = (int) ((drawableWidth - drawnWidth) / (barsCount - i));
                int height = (int) ((drawableHeight * yItems[i]) / (float) maxHeight);
                int horizLegendX = ((i == 0) ? Math.max(currentX - (xSpacing / 2), vertAxisWidth) : (currentX - (xSpacing / 2)));
                drawBar(g2, currentX, drawHeight - horizAxisHeight - height - outlineStrokeWidth, width, height);
                horizAxisXes.add(new Integer(horizLegendX));
                currentX += (width + xSpacing);
                drawnWidth += width;
            }

            horizAxisXes.add(new Integer(Math.min(currentX - (xSpacing / 2), drawWidth)));

            // draw horizontal chart axis
            drawHorizontalAxis(g2, horizAxisXes, xItems);
        }

        // offScreen image is now valid
        offScreenImageInvalid = false;
    }

    protected void drawHorizontalAxis(Graphics2D g2, List horizAxisXes, String[] xItems) {
        g2.setPaint(axisPaint);
        g2.setStroke(axisStroke);

        g2.drawLine(vertAxisWidth - 3, drawHeight - horizAxisHeight, drawWidth, drawHeight - horizAxisHeight);

        for (int i = 0; i < horizAxisXes.size(); i++) {
            int x = ((Integer) horizAxisXes.get(i)).intValue();
            g2.drawLine(x, drawHeight - horizAxisHeight + 1, x, drawHeight - horizAxisHeight + 3);
            drawHorizontalAxisLegendItem(g2, x, xItems[i]);
        }

        g2.drawString(model.getXAxisDesc(), drawWidth - horizLegendWidth - 2, drawHeight - 5);
    }

    protected void drawHorizontalAxisLegendItem(Graphics2D g2, int x, String string) {
        int legendWidth = (int) g2.getFontMetrics().getStringBounds(string, g2).getWidth();
        int legendX = Math.min(x - (legendWidth / 2), drawWidth - legendWidth - horizLegendWidth - 3);
        g2.drawString(string, legendX, drawHeight - 5);
    }

    protected void drawVerticalAxis(Graphics2D g2, int vertical3DCorrection, int[] yItems) {
        g2.setPaint(axisPaint);
        g2.setStroke(axisStroke);

        g2.drawLine(vertAxisWidth, 0, vertAxisWidth, drawHeight - horizAxisHeight);

        double factor = (double) (drawHeight - horizAxisHeight - topOffset - vertical3DCorrection) / (double) (maxHeight);
        long optimalUnits = DecimalAxisUtils.getOptimalUnits(factor, 30);

        long firstMark = 0;
        long currentMark = firstMark;
        int markPosition = drawHeight - horizAxisHeight - (int) (currentMark * factor);

        while (markPosition >= (vertLegendHeight + 5)) {
            g2.setPaint(axisPaint);
            g2.setStroke(axisStroke);
            g2.drawLine(vertAxisWidth - 3, markPosition, vertAxisWidth - 1, markPosition);

            drawVerticalAxisLegendItem(g2, markPosition, Long.toString(currentMark));

            g2.setPaint(axisMeshPaint);
            g2.drawLine(vertAxisWidth, markPosition, vertAxisWidth + drawWidth, markPosition);

            currentMark += optimalUnits;
            markPosition = drawHeight - horizAxisHeight - (int) (currentMark * factor);
        }

        g2.setPaint(axisPaint);
        g2.drawString(model.getYAxisDesc(), 2, vertLegendHeight);
    }

    protected void drawVerticalAxisLegendItem(Graphics2D g2, int y, String string) {
        int legendWidth = (int) g2.getFontMetrics().getStringBounds(string, g2).getWidth();
        int legendHeight = vertLegendHeight;
        int legendX = vertAxisWidth - legendWidth - 5;
        int legendY = Math.max((y + (legendHeight / 2)) - 2, (2 * legendHeight) + 3);
        g2.drawString(string, legendX, legendY);
    }

    protected void updateOffScreenImageSize() {
        insets = getInsets();

        drawWidth = getWidth() - insets.left - insets.right;
        drawHeight = getHeight() - insets.top - insets.bottom - 1;

        offScreenImage = createImage(drawWidth + 1, drawHeight + 1);
        offScreenGraphics = (Graphics2D) offScreenImage.getGraphics();

        offScreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offScreenImageSizeInvalid = false;
        offScreenImageInvalid = true;
    }

    private int getMaxY(int[] yItems) {
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < yItems.length; i++) {
            if (max < yItems[i]) {
                max = yItems[i];
            }
        }

        return max;
    }

    // ---------------------------------------------------------------------------
}
