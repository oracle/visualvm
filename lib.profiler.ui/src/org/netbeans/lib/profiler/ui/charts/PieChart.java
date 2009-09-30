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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;


/**
 *
 * @author Jiri Sedlacek
 */
public class PieChart extends JComponent implements ComponentListener, ChartModelListener, Accessible {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Color evenSelectionSegmentsColor = Color.WHITE;
    private static Stroke evenSelectionSegmentsStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                                                                        new float[] { 5, 5 }, 0);
    private static Color oddSelectionSegmentColor = Color.BLACK;
    private static Stroke oddSelectionSegmentStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                                                                      new float[] { 5, 5 }, 5);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AccessibleContext accessibleContext;
    private Area pieArea;
    private Graphics2D offScreenGraphics;
    private Image offScreenImage;
    private Insets insets = new Insets(0, 0, 0, 0);
    private PieChartModel model;
    private Vector arcs = new Vector();
    private Vector bottoms = new Vector();
    private Vector selectedItems = new Vector();
    private boolean draw3D = true; // (chartHeight > 0)
    private boolean offScreenImageInvalid;
    private boolean offScreenImageSizeInvalid;
    private int chartHeight = 15; // height of the 3D chart (0 means 2D chart)
    private int drawHeight;
    private int drawWidth;
    private int focusedItem = -1;
    private int initialAngle = 0; // start of first item (degrees)
    private int pieCenterY; // (pieHeight / 2)
    private int pieHeight; // (drawHeight - chartHeight)

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of PieChart */
    public PieChart() {
        offScreenImageSizeInvalid = true;

        addComponentListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAccessibleContext(AccessibleContext accessibleContext) {
        this.accessibleContext = accessibleContext;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    public void setChartHeight(int chartHeight) {
        this.chartHeight = chartHeight;
        draw3D = (chartHeight > 0);
    }

    public int getChartHeight() {
        return chartHeight;
    }

    public void setFocusedItem(int focusedItem) {
        if (this.focusedItem != focusedItem) {
            this.focusedItem = focusedItem;
            offScreenImageInvalid = true;
            repaint();
        }

        ;
    }

    public int getItemIndexAt(int x, int y) {
        // switch to geometry coordinate space
        x -= insets.left;
        y -= insets.top;

        // test arcs
        for (int i = 0; i < arcs.size(); i++) {
            if (((Arc2D) arcs.get(i)).contains(x, y)) {
                return i;
            }
        }

        // test 3D bottoms
        if (draw3D) {
            for (int i = 0; i < bottoms.size(); i++) {
                Area area = (Area) bottoms.get(i);

                if (area == null) {
                    continue;
                } else if (area.contains(x, y)) {
                    return i;
                }
            }
        }

        // no item hit
        return -1;
    }

    public void setModel(PieChartModel model) {
        // automatically unregister itself as a ChartModelListener from current model
        if (this.model != null) {
            this.model.removeChartModelListener(this);
        }

        // automatically register itself as a ChartModelListener for new model
        if (model != null) {
            model.addChartModelListener(this);
        }

        this.model = model;

        chartDataChanged();
    }

    public PieChartModel getModel() {
        return model;
    }

    public void setSelectedItem(int selectedItem) {
        if (selectedItems.contains(selectedItem) && (selectedItems.size() == 1)) {
            return;
        }

        selectedItems.clear();
        selectedItems.add(selectedItem);

        offScreenImageInvalid = true;
        repaint();
    }

    public int[] getSelectedItems() {
        int[] items = new int[selectedItems.size()];

        for (int i = 0; i < selectedItems.size(); i++) {
            items[i] = ((Integer) selectedItems.get(i)).intValue();
        }

        return items;
    }

    public void setStartAngle(int initialAngle) {
        this.initialAngle = initialAngle;
    }

    public int getStartAngle() {
        return initialAngle;
    }

    public void addSelectedItem(int selectedItem) {
        if (selectedItems.contains(selectedItem)) {
            return;
        }

        selectedItems.add(selectedItem);

        offScreenImageInvalid = true;
        repaint();
    }

    // Used for public chart update & listener implementation
    public void chartDataChanged() {
        //    offScreenImageInvalid = true;
        //    repaint();
        selectAllItems(); // also invalidates offscreen image and repaints
    }

    // --- ComponentListener implementation --------------------------------------
    public void componentHidden(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentResized(ComponentEvent e) {
        offScreenImageSizeInvalid = true;
        repaint();
    }

    public void componentShown(ComponentEvent e) {
    }

    public void deselectAllItems() {
        selectedItems.clear();

        offScreenImageInvalid = true;
        repaint();
    }

    // --- Main (Tester Frame) ---------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final PieChart pieChart = new PieChart();
        pieChart.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        pieChart.setPreferredSize(new Dimension(300, 200));

        DynamicPieChartModel pieChartModel = new DynamicPieChartModel();
        pieChartModel.setupModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }, // NOI18N
                                 new Color[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW });
        pieChartModel.setItemValues(new double[] { 10, 5, 15, 7 });

        JFrame frame = new JFrame("PieChart Tester"); // NOI18N
        frame.getContentPane().add(pieChart);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        pieChart.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int clickedItem = pieChart.getItemIndexAt(e.getX(), e.getY());
                    pieChart.toggleItemSelection(clickedItem);
                }
            });

        pieChart.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    int focusedItem = pieChart.getItemIndexAt(e.getX(), e.getY());

                    if (focusedItem != -1) {
                        pieChart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        pieChart.setCursor(Cursor.getDefaultCursor());
                    }

                    pieChart.setFocusedItem(focusedItem);
                }
            });

        pieChart.setModel(pieChartModel);
    }

    public void paintComponent(Graphics g) {
        // super.paintComponent
        super.paintComponent(g);

        // check if ChartModel is assigned
        if (model == null) {
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

    public void removeSelectedItem(int selectedItem) {
        if (!selectedItems.contains(selectedItem)) {
            return;
        }

        selectedItems.remove((Integer) selectedItem);

        offScreenImageInvalid = true;
        repaint();
    }

    public void resetFocusedItem() {
        if (focusedItem != -1) {
            focusedItem = -1;
            offScreenImageInvalid = true;
            repaint();
        }
    }

    public void selectAllItems() {
        for (int i = 0; i < model.getItemCount(); i++) {
            selectedItems.add(i);
        }

        offScreenImageInvalid = true;
        repaint();
    }

    public void toggleItemSelection(int selectedItem) {
        if (selectedItems.contains(selectedItem)) {
            removeSelectedItem(selectedItem);
        } else {
            addSelectedItem(selectedItem);
        }
    }

    protected Color getDisabledColor(Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        return new Color(r, g, b, 50);
    }

    protected void drawChart(Graphics2D g2) {
        arcs.clear();
        bottoms.clear();

        Area focusedItemArea = null;

        g2.setColor(getBackground());
        g2.fillRect(0, 0, drawWidth + 1, drawHeight + 1);

        g2.setStroke(new BasicStroke(0.5f));

        if (!model.hasData()) {
            // no data to display, draws only pie outline
            Area rectAreaUpper = new Area(new Rectangle2D.Double(0, pieCenterY + 1, drawWidth - 1, pieCenterY + pieHeight));
            rectAreaUpper.subtract(pieArea);

            Area rectAreaLower = new Area(rectAreaUpper);
            rectAreaLower.transform(AffineTransform.getTranslateInstance(0, chartHeight));
            rectAreaUpper.subtract(rectAreaLower);

            g2.setPaint(Color.BLACK);
            g2.draw(rectAreaUpper);
            g2.drawArc(0, 0, drawWidth - 1, pieHeight, 0, 180);
        } else {
            // data collected, draws standard pie
            int startAngle = this.initialAngle;
            int extentAngle;

            Point2D startPoint;
            Point2D endPoint;

            Point2D bottomStartPoint;
            Point2D bottomEndPoint;

            boolean startPointVisible;
            boolean endPointVisible;

            double left = 0;
            double right = 0;
            double width = 0;

            double top = 0;
            double bottom = 0;
            double height = 0;

            Arc2D.Double arc;
            Rectangle2D.Double rectangle;

            int pieParts = model.getItemCount();

            for (int i = 0; i < pieParts; i++) {
                if (model.getItemValueRel(i) == 0) {
                    continue;
                }

                extentAngle = (int) Math.min(Math.ceil(model.getItemValueRel(i) * 360), 360 - startAngle);

                if (extentAngle == 0) {
                    continue;
                }

                arc = new Arc2D.Double(0, 1, drawWidth - 1, pieHeight, startAngle, extentAngle, Arc2D.PIE);

                arcs.add(arc);

                if (i == focusedItem) {
                    if (pieParts == 1) {
                        focusedItemArea = new Area(new Ellipse2D.Double(0, 1, drawWidth - 1, pieHeight));
                    } else {
                        focusedItemArea = new Area(arc);
                    }
                }

                if (draw3D) {
                    startPoint = arc.getStartPoint();
                    endPoint = arc.getEndPoint();

                    startPointVisible = ((startAngle < 0) || (startAngle > 180));
                    endPointVisible = (((startAngle + extentAngle) < 0) || ((startAngle + extentAngle) > 180));

                    if (startPointVisible && endPointVisible) {
                        // both endpoints visible
                        if (startPoint.getX() < endPoint.getX()) {
                            // whole pie is visible
                            left = startPoint.getX();
                            right = endPoint.getX();
                            width = right - left;

                            top = Math.min(startPoint.getY(), endPoint.getY()) - 1;
                            bottom = drawHeight;
                            height = bottom - top;

                            Area bottomArea = drawChartPartSide(g2, pieArea, left, top, width, height,
                                                                selectedItems.contains(i) ? model.getItemColor(i).darker()
                                                                                          : getDisabledColor(model.getItemColor(i)
                                                                                                                  .darker()));
                            bottoms.add(bottomArea);

                            if ((i == focusedItem) && (focusedItemArea != null)) {
                                focusedItemArea.add(bottomArea);
                            }
                        } else {
                            // pie is splitted into two parts
                            left = startPoint.getX();
                            right = drawWidth - 1;
                            width = right - left;

                            top = pieCenterY + 1;
                            bottom = drawHeight;
                            height = bottom - top;

                            Area bottomArea = drawChartPartSide(g2, pieArea, left, top, width, height,
                                                                selectedItems.contains(i) ? model.getItemColor(i).darker()
                                                                                          : getDisabledColor(model.getItemColor(i)
                                                                                                                  .darker()));

                            left = 0;
                            right = endPoint.getX();
                            width = right - left;

                            top = pieCenterY + 1;
                            bottom = drawHeight;
                            height = bottom - top;

                            Area bottomArea2 = drawChartPartSide(g2, pieArea, left, top, width, height,
                                                                 selectedItems.contains(i) ? model.getItemColor(i).darker()
                                                                                           : getDisabledColor(model.getItemColor(i)
                                                                                                                   .darker()));
                            bottomArea.add(bottomArea2);
                            bottoms.add(bottomArea);

                            if ((i == focusedItem) && (focusedItemArea != null)) {
                                focusedItemArea.add(bottomArea);
                            }
                        }
                    } else if (startPointVisible || endPointVisible) {
                        // one endpoint visible
                        if (startPointVisible && !endPointVisible) {
                            left = startPoint.getX();
                            right = drawWidth - 1;
                            width = right - left;

                            top = pieCenterY + 1;
                            bottom = drawHeight;
                            height = bottom - top;

                            Area bottomArea = drawChartPartSide(g2, pieArea, left, top, width, height,
                                                                selectedItems.contains(i) ? model.getItemColor(i).darker()
                                                                                          : getDisabledColor(model.getItemColor(i)
                                                                                                                  .darker()));
                            bottoms.add(bottomArea);

                            if ((i == focusedItem) && (focusedItemArea != null)) {
                                focusedItemArea.add(bottomArea);
                            }
                        } else {
                            left = 0;
                            right = endPoint.getX();
                            width = right - left;

                            top = pieCenterY + 1;
                            bottom = drawHeight;
                            height = bottom - top;

                            Area bottomArea = drawChartPartSide(g2, pieArea, left, top, width, height,
                                                                selectedItems.contains(i) ? model.getItemColor(i).darker()
                                                                                          : getDisabledColor(model.getItemColor(i)
                                                                                                                  .darker()));
                            bottoms.add(bottomArea);

                            if ((i == focusedItem) && (focusedItemArea != null)) {
                                focusedItemArea.add(bottomArea);
                            }
                        }
                    } else if (extentAngle >= 180) {
                        // no endpoint visible
                        left = 0;
                        right = drawWidth - 1;
                        width = right - left;

                        top = pieCenterY + 1;
                        bottom = drawHeight;
                        height = bottom - top;

                        Area bottomArea = drawChartPartSide(g2, pieArea, left, top, width, height,
                                                            selectedItems.contains(i) ? model.getItemColor(i).darker()
                                                                                      : getDisabledColor(model.getItemColor(i)
                                                                                                              .darker()));
                        bottoms.add(bottomArea);

                        if ((i == focusedItem) && (focusedItemArea != null)) {
                            focusedItemArea.add(bottomArea);
                        }
                    } else {
                        // no bottom visible
                        bottoms.add(null);
                    }
                }

                g2.setPaint(selectedItems.contains(i) ? model.getItemColor(i) : getDisabledColor(model.getItemColor(i)));
                g2.fill(arc);
                //        g2.setPaint(model.getItemColor(i).darker().darker());
                //        g2.setPaint(Color.BLACK);
                //        g2.draw(arc);
                startAngle += extentAngle;
            }
        }

        if (focusedItemArea != null) {
            g2.setColor(evenSelectionSegmentsColor);
            g2.setStroke(evenSelectionSegmentsStroke);
            g2.draw(focusedItemArea);

            g2.setColor(oddSelectionSegmentColor);
            g2.setStroke(oddSelectionSegmentStroke);
            g2.draw(focusedItemArea);
        }

        offScreenImageInvalid = false;
    }

    protected Area drawChartPartSide(Graphics2D g2, Area pieArea, double left, double top, double width, double height,
                                     Color color) {
        Area rectAreaUpper = new Area(new Rectangle2D.Double(left, top, width, height));
        rectAreaUpper.subtract(pieArea);

        Area rectAreaLower = new Area(rectAreaUpper);
        rectAreaLower.transform(AffineTransform.getTranslateInstance(0, chartHeight));
        rectAreaUpper.subtract(rectAreaLower);
        rectAreaUpper.transform(AffineTransform.getTranslateInstance(0, -1));
        rectAreaUpper.subtract(pieArea);

        g2.setPaint(color);
        g2.fill(rectAreaUpper);

        //    g2.setPaint(Color.BLACK);
        //    g2.draw(rectAreaUpper);
        return rectAreaUpper;
    }

    // --- Protected implementation ------------------------------------------------
    protected void updateOffScreenImageSize() {
        insets = getInsets();

        drawWidth = getWidth() - insets.left - insets.right;
        drawHeight = getHeight() - insets.top - insets.bottom - 1;

        pieHeight = drawHeight - chartHeight;
        pieCenterY = pieHeight / 2;

        offScreenImage = createImage(drawWidth + 1, drawHeight + 1);
        offScreenGraphics = (Graphics2D) offScreenImage.getGraphics();

        offScreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offScreenImageSizeInvalid = false;
        offScreenImageInvalid = true;

        pieArea = new Area(new Ellipse2D.Double(0, 0, drawWidth - 1, pieHeight));
    }
}
