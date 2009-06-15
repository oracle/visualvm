/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.charts.xy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.charts.ChartItem;
import org.netbeans.lib.profiler.charts.ChartSelectionModel;
import org.netbeans.lib.profiler.charts.ItemPainter;
import org.netbeans.lib.profiler.charts.ItemsModel;
import org.netbeans.lib.profiler.charts.axis.AxisComponent;
import org.netbeans.lib.profiler.charts.axis.AxisMarksPainter;
import org.netbeans.lib.profiler.charts.axis.BytesMarksPainter;
import org.netbeans.lib.profiler.charts.axis.PercentLongMarksPainter;
import org.netbeans.lib.profiler.charts.axis.SimpleLongMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimeAxisUtils;
import org.netbeans.lib.profiler.charts.axis.TimeMarksPainter;
import org.netbeans.lib.profiler.charts.axis.TimelineMarksComputer;
import org.netbeans.lib.profiler.charts.swing.CrossBorderLayout;
import org.netbeans.lib.profiler.charts.swing.LongRect;
import org.netbeans.lib.profiler.charts.xy.BytesXYItemMarksComputer;
import org.netbeans.lib.profiler.charts.xy.DecimalXYItemMarksComputer;
import org.netbeans.lib.profiler.charts.xy.XYItemPainter;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYChart;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItem;
import org.netbeans.lib.profiler.charts.xy.synchronous.SynchronousXYItemsModel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ColorIcon;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.graphs.GraphsUI;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class SimpleXYChartUtils {
    
    // --- Public chart types constants ----------------------------------------

    public static final int TYPE_DECIMAL = 0;
    public static final int TYPE_BYTES = 1;
    public static final int TYPE_PERCENT = 2;


    // --- Private constants ---------------------------------------------------

    private static final NumberFormat INT_FORMATTER;
    private static final NumberFormat PERCENT_FORMATTER;

    private static final int DEFAULT_BUFFER_STEP;

    private static final Color AXIS_FONT_COLOR;

    private static final int VALUES_SPACING;
    private static final int TIMELINE_SPACING;

    private static       int LEGEND_HEIGHT;
    private static final int DETAILS_HEIGHT_THRESHOLD;

    private static final Map<String, Format> FORMATS;
    
    // --- Static initializer --------------------------------------------------

    static {
        INT_FORMATTER = NumberFormat.getIntegerInstance();
        INT_FORMATTER.setGroupingUsed(true);

        PERCENT_FORMATTER = NumberFormat.getPercentInstance();
        PERCENT_FORMATTER.setMinimumFractionDigits(1);
        PERCENT_FORMATTER.setMaximumIntegerDigits(2);

        DEFAULT_BUFFER_STEP = 50;

        AXIS_FONT_COLOR = new Color(90, 90, 90);

        VALUES_SPACING = Math.max(new TimeMarksPainter().getFont().getSize(), 15) + 10;
        TIMELINE_SPACING = 80;

        LEGEND_HEIGHT = -1;
        DETAILS_HEIGHT_THRESHOLD = 180;

        FORMATS = new HashMap();
    }
    
    
    // --- Public utils --------------------------------------------------------

    public static XYStorage createStorage(int valuesBuffer) {
        return new XYStorage(valuesBuffer, DEFAULT_BUFFER_STEP);
    }

    public static SynchronousXYItemsModel createItemsModel(XYStorage storage,
                                                           String[] itemNames,
                                                           long minValue,
                                                           long maxValue) {

        SynchronousXYItem[] items = new SynchronousXYItem[itemNames.length];
        for (int i = 0; i < items.length; i++)
            items[i] = storage.addItem(itemNames[i], minValue, maxValue);

        return new SynchronousXYItemsModel(storage, items);
    }

    public static XYPaintersModel createPaintersModel(float[] lineWidths,
                                                      Color[] lineColors,
                                                      Color[] fillColors1,
                                                      Color[] fillColors2,
                                                      ItemsModel itemsModel) {

        ChartItem[] items = new ChartItem[itemsModel.getItemsCount()];
        for (int i = 0; i < items.length; i++)
            items[i] = itemsModel.getItem(i);

        ItemPainter[] painters = new ItemPainter[lineWidths.length];
        for (int i = 0; i < painters.length; i++)
            painters[i] = XYPainter.absolutePainter(lineWidths[i],
                                                    lineColors  == null ? null : lineColors[i],
                                                    fillColors1 == null ? null : fillColors1[i],
                                                    fillColors2 == null ? null : fillColors2[i]);
        return new XYPaintersModel(items, painters);
    }

    public static JComponent createChartUI(int chartType, Color[] itemColors,
                                            long initialYMargin, boolean hideItems,
                                            XYStorage storage,
                                            SynchronousXYItemsModel itemsModel,
                                            XYPaintersModel paintersModel) {

        // Chart
        SynchronousXYChart chart = new SynchronousXYChart(itemsModel, paintersModel);

        chart.setFitsHeight(true);
        chart.setFitsWidth(true);
        chart.setBottomBased(true);
        chart.setViewInsets(new Insets(10, 0, 0, 0));
        chart.setInitialDataBounds(new LongRect(System.currentTimeMillis(), 0,
                                       2500, initialYMargin));

        chart.addPreDecorator(new XYBackground());

        // Horizontal axis
        TimelineMarksComputer hComputer = new TimelineMarksComputer(storage,
                         chart.getChartContext(), SwingConstants.HORIZONTAL) {
            protected int getMinMarksDistance() { return TIMELINE_SPACING; }
        };
        AxisComponent hAxis =
                new XYAxisComponent(chart, hComputer, customizeMarksPainter(
                                    new TimeMarksPainter()), SwingConstants.SOUTH,
                                    AxisComponent.MESH_FOREGROUND);

        // Vertical axis
        AxisComponent vAxis = null;
        if (chartType == TYPE_PERCENT) {
            SynchronousXYItem item = itemsModel.getItem(0);
            XYItemPainter painter = (XYItemPainter)paintersModel.getPainter(item);
            DecimalXYItemMarksComputer vComputer = new DecimalXYItemMarksComputer(
                         item, painter, chart.getChartContext(),
                         SwingConstants.VERTICAL) {
                protected int getMinMarksDistance() { return VALUES_SPACING; }
            };
            vAxis = new XYAxisComponent(chart, vComputer, customizeMarksPainter(
                         new PercentLongMarksPainter(0, 1000)), SwingConstants.WEST,
                         AxisComponent.MESH_FOREGROUND);
        } else if (chartType == TYPE_BYTES) {
            SynchronousXYItem item = itemsModel.getItem(0);
            XYItemPainter painter = (XYItemPainter)paintersModel.getPainter(item);
            BytesXYItemMarksComputer vComputer = new BytesXYItemMarksComputer(
                         item, painter, chart.getChartContext(),
                         SwingConstants.VERTICAL) {
                protected int getMinMarksDistance() { return VALUES_SPACING; }
            };
            vAxis = new XYAxisComponent(chart, vComputer, customizeMarksPainter(
                         new BytesMarksPainter()), SwingConstants.WEST,
                         AxisComponent.MESH_FOREGROUND);
        } else {
            SynchronousXYItem item = itemsModel.getItem(0);
            XYItemPainter painter = (XYItemPainter)paintersModel.getPainter(item);
            final DecimalXYItemMarksComputer vComputer = new DecimalXYItemMarksComputer(
                         item, painter, chart.getChartContext(),
                         SwingConstants.VERTICAL) {
                protected int getMinMarksDistance() { return VALUES_SPACING; }
            };
            vAxis = new XYAxisComponent(chart, vComputer, customizeMarksPainter(
                         new SimpleLongMarksPainter()), SwingConstants.WEST,
                         AxisComponent.MESH_FOREGROUND);
        }

        // Tooltip support
        XYTooltipPainter tooltipPainter = new XYTooltipPainter(createTooltipModel(
                                                               chartType,
                                                               itemColors,
                                                               storage,
                                                               itemsModel));
        chart.addOverlayComponent(new XYTooltipOverlay(chart, tooltipPainter));
        chart.getSelectionModel().setHoverMode(ChartSelectionModel.HOVER_EACH_NEAREST);

        // Hovering support
        XYSelectionOverlay selectionOverlay = new XYSelectionOverlay();
        chart.addOverlayComponent(selectionOverlay);
        selectionOverlay.registerChart(chart);
        chart.getSelectionModel().setMoveMode(ChartSelectionModel.SELECTION_LINE_V);

        // Chart panel
        JPanel chartPanel = new JPanel(new CrossBorderLayout());
        chartPanel.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        chartPanel.setBorder(BorderFactory.createMatteBorder(
                             10, 10, 0, 10, GraphsUI.CHART_BACKGROUND_COLOR));
        chartPanel.add(chart, new Integer[] { SwingConstants.CENTER });
        chartPanel.add(hAxis, new Integer[] { SwingConstants.SOUTH,
                                              SwingConstants.SOUTH_WEST });
        chartPanel.add(vAxis, new Integer[] { SwingConstants.WEST,
                                              SwingConstants.SOUTH_WEST });

        // Legend panel
        JComponent legendPanel = createLegendPanel(itemColors, hideItems,
                                                   itemsModel, paintersModel);

        // Chart container
        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        chartContainer.add(legendPanel, BorderLayout.SOUTH);

        return chartContainer;
    }

    public static DetailsHandle createDetailsArea(final String[] detailsItems,
                                                  JComponent chartContainer) {
        final JPanel detailsContainer = new JPanel(new BorderLayout());
        detailsContainer.setBorder(BorderFactory.createMatteBorder(
                             8, 10, 0, 10, GraphsUI.CHART_BACKGROUND_COLOR));
        final HTMLTextArea detailsArea = new HTMLTextArea();
        detailsArea.setText(createDetailsString(detailsItems, null));
        detailsContainer.add(detailsArea, BorderLayout.CENTER);
        chartContainer.add(detailsContainer, BorderLayout.NORTH);

        chartContainer.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                detailsContainer.setVisible(e.getComponent().getHeight() >
                                            DETAILS_HEIGHT_THRESHOLD);
            }
        });

        return new DetailsHandle() {
            public void updateDetails(String[] details) {
                try {
                    int selStart = detailsArea.getSelectionStart();
                    int selEnd   = detailsArea.getSelectionEnd();
                    detailsArea.setText(createDetailsString(detailsItems, details));
//                    detailsArea.setCaretPosition(0);
                    detailsArea.select(selStart, selEnd);
                } catch (Exception e) {}
            }
        };
    }

    public static JComponent createLegendPanel(Color[] itemColors, boolean hideItems,
                                               SynchronousXYItemsModel itemsModel,
                                               final XYPaintersModel paintersModel) {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, hideItems ? 5 : 10, 0));
        legendPanel.setOpaque(false);
        
        for (int i = 0; i < itemColors.length; i++) {
            final SynchronousXYItem item = itemsModel.getItem(i);
            ColorIcon icon = new ColorIcon(itemColors[i], Color.DARK_GRAY, 10, 10);
            JComponent legendItem = null;

            if (hideItems) {
                final XYPainter painter = (XYPainter)paintersModel.getPainter(item);
                legendItem = new IconCheckBox(item.getName(), icon, painter.isPainting()) {
                    public Dimension getPreferredSize() {
                        Dimension ps = super.getPreferredSize();
                        ps.height = getLegendHeight();
                        return ps;
                    }
                    protected void fireActionPerformed(ActionEvent e) {
                        painter.setPainting(isSelected());
                        paintersModel.painterChanged(painter);
                    }
                };
            } else {
                legendItem = new JLabel(item.getName(), icon, JLabel.HORIZONTAL) {
                    public Dimension getPreferredSize() {
                        Dimension ps = super.getPreferredSize();
                        ps.height = getLegendHeight();
                        return ps;
                    }
                };
            }
            
            legendItem.setOpaque(false);
            legendPanel.add(legendItem);

        }

        JPanel legendContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        legendContainer.setOpaque(true);
        legendContainer.setBackground(GraphsUI.CHART_BACKGROUND_COLOR);
        legendContainer.setBorder(BorderFactory.createCompoundBorder(
                                  BorderFactory.createMatteBorder(
                                  0, 10, 0, 0, GraphsUI.CHART_BACKGROUND_COLOR),
                                  BorderFactory.createLineBorder(Color.WHITE)));
        legendContainer.add(legendPanel);

        return legendContainer;
    }

    public static XYTooltipModel createTooltipModel(final int chartType,
                                                    final Color[] itemColors,
                                                    final XYStorage storage,
                                                    final SynchronousXYItemsModel itemsModel) {

        return new XYTooltipModel() {

            public String getTimeValue(long timestamp) {
                int timestamps = storage.getTimestampsCount();
                if (timestamps == 0) return formatTime(timestamp, timestamp, timestamp);
                else return formatTime(timestamp, storage.getTimestamp(0),
                                       storage.getTimestamp(timestamps - 1));
            }

            public int getRowsCount() {
                return itemsModel.getItemsCount();
            }

            public String getRowName(int index) {
                return itemsModel.getItem(index).getName();
            }

            public Color getRowColor(int index) {
                return itemColors[index];
            }

            public String getRowValue(int index, long itemValue) {
                switch (chartType) {
                    case TYPE_BYTES  : return formatBytes(itemValue);
                    case TYPE_PERCENT: return formatPercent(itemValue);
                    default:           return formatDecimal(itemValue);
                }
            }

        };
    }

    public static AxisMarksPainter customizeMarksPainter(AxisMarksPainter.Abstract painter) {
        painter.setForeground(AXIS_FONT_COLOR);
        painter.setFont(smallerFont(painter.getFont()));
        return painter;
    }

    public static String formatDecimal(long value) {
        return INT_FORMATTER.format(value);
    }

    public static String formatBytes(long value) {
        String bytesFormat = NbBundle.getMessage(SimpleXYChartUtils.class,
                                                "SimpleXYChartUtils_BytesFormat"); // NOI18N
        return MessageFormat.format(bytesFormat, new Object[] { formatDecimal(value) });
    }

    public static String formatPercent(long value) {
        return PERCENT_FORMATTER.format(value / 1000f);
    }

    public static String formatTime(long timestamp, long startTime, long endTime) {
        long time = System.currentTimeMillis();
        String formatString = TimeAxisUtils.getFormatString(1000, startTime, endTime);
        return getFormat(formatString).format(new Date(timestamp));
    }

    public static Font smallerFont(Font font) {
        if (UIUtils.isWindowsLookAndFeel())
            return new Font(font.getName(), font.getStyle(), font.getSize() - 1);
        else

            return new Font(font.getName(), font.getStyle(), font.getSize() - 2);
    }


    // --- Private implementation ----------------------------------------------

    private static String createDetailsString(String[] detailsItems, String[] values) {
        StringBuilder sb = new StringBuilder();
        String itemFormat = NbBundle.getMessage(SimpleXYChartUtils.class,
                                                "SimpleXYChartUtils_DetailsItemFormat"); // NOI18N

        sb.append("<table border='0' cellpadding='0' cellspacing='3' width='100%'>"); // NOI18N
        sb.append("<tr>"); // NOI18N
        int i = 0;
        while (i < detailsItems.length) {
            if (detailsItems.length == 1) sb.append("<td><nobr>"); // NOI18N
            else sb.append("<td width='50%'><nobr>"); // NOI18N
            sb.append(MessageFormat.format(itemFormat, new Object[] { detailsItems[i],
                                           values == null ? "" : values[i] })); // NOI18N
            sb.append("</nobr></td>"); // NOI18N
            if (i % 2 == 1 && i + 1 < detailsItems.length) sb.append("</tr><tr>"); // NOI18N
            i++;
        }
        sb.append("</tr>"); // NOI18N
        sb.append("</table>"); // NOI18N

        return sb.toString();
    }

    private static synchronized Format getFormat(String formatString) {
        Format format = FORMATS.get(formatString);
        if (format == null) {
            format = new SimpleDateFormat(formatString);
            FORMATS.put(formatString, format);
        }
        return format;
    }

    private static synchronized int getLegendHeight() {
        if (LEGEND_HEIGHT == -1)
            LEGEND_HEIGHT = Math.max(new JLabel("X").getPreferredSize().height, // NOI18N
                                     new JCheckBox("X").getPreferredSize().height); // NOI18N
        return LEGEND_HEIGHT;
    }


    // --- DetailsHandle -------------------------------------------------------

    public static interface DetailsHandle {
        public void updateDetails(String[] details);
    }


    // --- IconCheckBox --------------------------------------------------------

    private static class IconCheckBox extends JCheckBox {

        private static final int CHECKBOX_OFFSET = getCheckBoxOffset();

        private final JCheckBox renderer;

        public IconCheckBox(String text, Icon icon, boolean selected) {
            renderer = new JCheckBox(text, icon) {
                public boolean hasFocus() {
                    return IconCheckBox.this.hasFocus();
                }
            };
            renderer.setOpaque(false);
            renderer.setBorderPainted(false);
            setSelected(selected);
            setBorderPainted(false);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.translate(renderer.getX(), renderer.getY());
            renderer.paint(g);
            g.translate(-renderer.getX(), -renderer.getY());
        }


        public void setBounds(int x, int y, int width, int height) {
            Dimension d = super.getPreferredSize();
            renderer.setBounds(d.width - CHECKBOX_OFFSET, 0,
                               width - d.width + CHECKBOX_OFFSET, height);
            super.setBounds(x, y, width, height);
        }

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width += renderer.getPreferredSize().width - CHECKBOX_OFFSET;
            return d;
        }


        private static int getCheckBoxOffset() {
            return 3;
        }

    }

}
