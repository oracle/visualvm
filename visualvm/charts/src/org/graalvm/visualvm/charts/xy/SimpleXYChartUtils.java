/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.charts.xy;

import org.graalvm.visualvm.charts.swing.RotateLabelUI;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import org.graalvm.visualvm.uisupport.TransparentToolBar;
import org.graalvm.visualvm.uisupport.UISupport;
import org.graalvm.visualvm.uisupport.VerticalLayout;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.LabelUI;
import org.graalvm.visualvm.lib.charts.ChartContext;
import org.graalvm.visualvm.lib.charts.ChartDecorator;
import org.graalvm.visualvm.lib.charts.ChartItem;
import org.graalvm.visualvm.lib.charts.ChartItemChange;
import org.graalvm.visualvm.lib.charts.ChartSelectionModel;
import org.graalvm.visualvm.lib.charts.ItemPainter;
import org.graalvm.visualvm.lib.charts.ItemsModel;
import org.graalvm.visualvm.lib.charts.axis.AxisComponent;
import org.graalvm.visualvm.lib.charts.axis.AxisMarksPainter;
import org.graalvm.visualvm.lib.charts.axis.BytesMarksPainter;
import org.graalvm.visualvm.lib.charts.axis.BitsPerSecMarksPainter;
import org.graalvm.visualvm.lib.charts.axis.TimeAxisUtils;
import org.graalvm.visualvm.lib.charts.axis.TimeMarksPainter;
import org.graalvm.visualvm.lib.charts.axis.TimelineMarksComputer;
import org.graalvm.visualvm.lib.charts.swing.CrossBorderLayout;
import org.graalvm.visualvm.lib.charts.swing.LongRect;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.BytesXYItemMarksComputer;
import org.graalvm.visualvm.lib.charts.xy.DecimalXYItemMarksComputer;
import org.graalvm.visualvm.lib.charts.xy.XYItemPainter;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItem;
import org.graalvm.visualvm.lib.charts.xy.synchronous.SynchronousXYItemsModel;
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
    public static final int TYPE_BITS_PER_SEC = 3;


    // --- Private constants ---------------------------------------------------

    private static final NumberFormat DECIMAL_FORMATTER;
    private static final NumberFormat PERCENT_FORMATTER;

    private static final int DEFAULT_BUFFER_STEP;

    private static final Color AXIS_FONT_COLOR;
    private static final Color BACKGROUND_COLOR;

    private static final int VALUES_SPACING;
    private static final int TIMELINE_SPACING;

    private static       int LEGEND_HEIGHT;
    private static final int DETAILS_HEIGHT_THRESHOLD;

    private static final Map<String, Format> FORMATS;
    
    // --- Static initializer --------------------------------------------------

    static {
        DECIMAL_FORMATTER = NumberFormat.getNumberInstance();
        DECIMAL_FORMATTER.setGroupingUsed(true);
        DECIMAL_FORMATTER.setMaximumFractionDigits(2);

        PERCENT_FORMATTER = NumberFormat.getPercentInstance();
        PERCENT_FORMATTER.setMinimumFractionDigits(1);
        PERCENT_FORMATTER.setMaximumIntegerDigits(3);

        DEFAULT_BUFFER_STEP = 50;

        AXIS_FONT_COLOR = !UISupport.isDarkResultsBackground() ? new Color(90, 90, 90) : new Color(165, 165, 165);
        BACKGROUND_COLOR = UISupport.getDefaultBackground();

        VALUES_SPACING = Math.max(new TimeMarksPainter().getFont().getSize(), 15) + 10;
        TIMELINE_SPACING = 80;

        LEGEND_HEIGHT = -1;
        DETAILS_HEIGHT_THRESHOLD = 180;

        FORMATS = new HashMap<>();
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

        ItemPainter[] painters = new ItemPainter[items.length];
        for (int i = 0; i < painters.length; i++)
            painters[i] = XYPainter.absolutePainter(lineWidths  == null ? null : lineWidths[i],
                                                    lineColors  == null ? null : lineColors[i],
                                                    fillColors1 == null ? null : fillColors1[i],
                                                    fillColors2 == null ? null : fillColors2[i]);
        return new XYPaintersModel(items, painters);
    }

    public static JComponent createChartUI(String chartTitle, String xAxisDescription,
                                           String yAxisDescription, int chartType,
                                           Color[] itemColors, long initialYMargin,
                                           boolean hideItems, boolean legendVisible,
                                           boolean supportsZooming, double chartFactor,
                                           NumberFormat customFormat, XYStorage storage,
                                           SynchronousXYItemsModel itemsModel,
                                           XYPaintersModel paintersModel,
                                           long limitYValue) {

        // Chart
        final boolean hasAxisLabel = xAxisDescription != null || yAxisDescription != null;
        
        final XYStorage _storage = storage;
        SimpleXYChart chart = new SimpleXYChart(itemsModel, paintersModel) {
            protected void itemsChanged(List<ChartItemChange> itemChanges) {
                if (_storage.isFull()) updateChart(); // full repaint to handle removed items
                else super.itemsChanged(itemChanges);
            }
            public void setBounds(int x, int y, int w, int h) {
                super.setBounds(x, y, w, h);
                
                JScrollBar scroller = (JScrollBar)getClientProperty("scroller"); // NOI18N
                if (scroller != null) {
                    int xpos = getX() - 1;
                    if (hasAxisLabel) xpos += getParent().getX();
                    scroller.setBounds(xpos, 0, getWidth() + 1, scroller.getHeight());
                }
            }
        };

        chart.setFitsHeight(true);
        chart.setFitsWidth(true);
        chart.setBottomBased(true);
        chart.setViewInsets(new Insets(10, 0, 0, 0));
        chart.setInitialDataBounds(new LongRect(System.currentTimeMillis(), 0,
                                       2500, initialYMargin));
        
        chart.addPreDecorator(new XYBackground());
        if (limitYValue != 0) {
            chart.addPreDecorator(createMaxHeapDecorator(limitYValue));
        }
        
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
                         new XYPercentMarksPainter(0, 100, chartFactor)),
                         SwingConstants.WEST, AxisComponent.MESH_FOREGROUND);
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
        } else if (chartType == TYPE_BITS_PER_SEC) {
            SynchronousXYItem item = itemsModel.getItem(0);
            XYItemPainter painter = (XYItemPainter)paintersModel.getPainter(item);
            BytesXYItemMarksComputer vComputer = new BytesXYItemMarksComputer(
                         item, painter, chart.getChartContext(),
                         SwingConstants.VERTICAL) {
                protected int getMinMarksDistance() { return VALUES_SPACING; }
            };
            vAxis = new XYAxisComponent(chart, vComputer, customizeMarksPainter(new BitsPerSecMarksPainter()), SwingConstants.WEST,
                         AxisComponent.MESH_FOREGROUND);
        } else {
            SynchronousXYItem item = itemsModel.getItem(0);
            XYItemPainter painter = (XYItemPainter)paintersModel.getPainter(item);
            final DecimalXYItemMarksComputer vComputer = new DecimalXYItemMarksComputer(
                         item, painter, chart.getChartContext(),
                         SwingConstants.VERTICAL) {
                protected int getMinMarksDistance() { return VALUES_SPACING; }
            };
            NumberFormat format = customFormat != null ? customFormat : DECIMAL_FORMATTER;
            vAxis = new XYAxisComponent(chart, vComputer, customizeMarksPainter(
                         new XYDecimalMarksPainter(chartFactor, format)),
                         SwingConstants.WEST, AxisComponent.MESH_FOREGROUND);
        }

        // Tooltip support
        XYTooltipPainter tooltipPainter = new XYTooltipPainter(createTooltipModel(
                                                               chartType,
                                                               itemColors,
                                                               chartFactor,
                                                               customFormat,
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
        chartPanel.add(chart, new Integer[] { SwingConstants.CENTER });
        chartPanel.add(hAxis, new Integer[] { SwingConstants.SOUTH,
                                              SwingConstants.SOUTH_WEST });
        chartPanel.add(vAxis, new Integer[] { SwingConstants.WEST,
                                              SwingConstants.SOUTH_WEST });

        // Chart container
        JPanel chartContainer = hasAxisLabel ? new JPanel(new BorderLayout()) :
                                               chartPanel;

        if (hasAxisLabel) {
            chartPanel.setOpaque(false);

            if (xAxisDescription != null)
                chartContainer.add(createXAxisLabel(xAxisDescription), BorderLayout.SOUTH);
            if (yAxisDescription != null)
                chartContainer.add(createYAxisLabel(yAxisDescription), BorderLayout.WEST);

            chartContainer.add(chartPanel, BorderLayout.CENTER);
        }

        chartContainer.setBackground(BACKGROUND_COLOR);

        // Caption panel
        JPanel captionPanel = new JPanel(new BorderLayout());
        captionPanel.setBackground(BACKGROUND_COLOR);
        if (chartTitle != null) captionPanel.add(createTitleLabel(chartTitle),
                                                 BorderLayout.NORTH);
        
        // Side panel
        JPanel sidePanel = new JPanel(new VerticalLayout(false));
        sidePanel.setOpaque(false);
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10));

        // Legend panel
        JComponent legendPanel = createLegendPanel(itemColors, hideItems,
                                                   itemsModel, paintersModel);
        legendPanel.setVisible(legendVisible);
        
        // Scroller panel
        JPanel scrollerPanel = new JPanel(null) {
            public Dimension getPreferredSize() {
                Component c = getComponentCount() > 0 ? getComponent(0) : null;
                if (c != null && c.isVisible()) {
                    Dimension size = c.getSize();
                    size.width += c.getX();
                    return size;
                } else {
                    return new Dimension();
                }
            }
        };
        scrollerPanel.setOpaque(false);
        
        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(scrollerPanel, BorderLayout.NORTH);
        bottomPanel.add(legendPanel, BorderLayout.SOUTH);
        
        // Chart view
        JPanel chartView = new JPanel(new BorderLayout());
        chartView.setBackground(BACKGROUND_COLOR);
        chartView.add(captionPanel, BorderLayout.NORTH);
        chartView.add(chartContainer, BorderLayout.CENTER);
        chartView.add(sidePanel, BorderLayout.EAST);
        chartView.add(bottomPanel, BorderLayout.SOUTH);
        
        chartView.putClientProperty("chart", chart); // NOI18N
        chartView.putClientProperty("sidePanel", sidePanel); // NOI18N
        chartView.putClientProperty("legendPanel", legendPanel); // NOI18N
        chartView.putClientProperty("scrollerPanel", scrollerPanel); // NOI18N
        
        if (supportsZooming) setZoomingEnabled(chartView, supportsZooming);

        return chartView;
    }

    public static DetailsHandle createDetailsArea(final String[] detailsItems,
                                                  JComponent chartContainer) {        
        final HTMLTextArea detailsArea = new HTMLTextArea();
        detailsArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        detailsArea.setText(createDetailsString(detailsItems, null));

        BorderLayout containerLayout = (BorderLayout)chartContainer.getLayout();
        JComponent containerNorth = (JComponent)containerLayout.
                                    getLayoutComponent(BorderLayout.NORTH);
        containerNorth.add(detailsArea, BorderLayout.CENTER);

        final JComponent containerCenter = (JComponent)containerLayout.
                                           getLayoutComponent(BorderLayout.CENTER);
        containerCenter.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 0));

        chartContainer.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                boolean visible = e.getComponent().getHeight() > DETAILS_HEIGHT_THRESHOLD;
                detailsArea.setVisible(visible);
                containerCenter.setBorder(BorderFactory.createEmptyBorder(
                                          visible ? 6 : 10, 10, 0, 0));
            }
        });

        return new DetailsHandle() {
            public void updateDetails(String[] details) {
                try {
                    int selStart = detailsArea.getSelectionStart();
                    int selEnd   = detailsArea.getSelectionEnd();
                    detailsArea.setText(createDetailsString(detailsItems, details));
                    detailsArea.select(selStart, selEnd);
                } catch (Exception e) {}
            }
        };
    }

    public static JComponent createLegendPanel(Color[] itemColors, boolean hideItems,
                                               SynchronousXYItemsModel itemsModel,
                                               final XYPaintersModel paintersModel) {
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, hideItems ? 5 : 10, 0));
        legendPanel.setBackground(BACKGROUND_COLOR);
        
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
        legendContainer.setBackground(BACKGROUND_COLOR);
        legendContainer.setBorder(BorderFactory.createMatteBorder(
                                  0, 10, 0, 0, BACKGROUND_COLOR));
        legendContainer.add(legendPanel);

        return legendContainer;
    }
    
    public static void setZoomingEnabled(JComponent chartUI, boolean enabled) {
        SimpleXYChart chart = (SimpleXYChart)chartUI.getClientProperty("chart"); // NOI18N
        
        if (chart.isZoomingEnabled() == enabled) return;
        else chart.setZoomingEnabled(enabled);
        
        JPanel sidePanel = (JPanel)chartUI.getClientProperty("sidePanel"); // NOI18N
        JPanel scrollerPanel = (JPanel)chartUI.getClientProperty("scrollerPanel"); // NOI18N
        
        if (enabled) {
            TransparentToolBar toolbar = new TransparentToolBar(false);
            for (Action action : chart.getActions()) toolbar.addItem(action);
            sidePanel.add(toolbar);
            
            JScrollBar scroller = chart.getScroller();
            scroller.setSize(scroller.getPreferredSize());
            scrollerPanel.add(scroller);
            chart.putClientProperty("scroller", scroller); // NOI18N
        } else {
            sidePanel.removeAll();
            scrollerPanel.removeAll();
            chart.putClientProperty("scroller", null); // NOI18N
        }
        
        sidePanel.setVisible(enabled);
        
        chartUI.doLayout();
        chartUI.repaint();
    }
    
    public static void setLegendVisible(JComponent chartUI, boolean visible) {
        JPanel legendPanel = (JPanel)chartUI.getClientProperty("legendPanel"); // NOI18N
        legendPanel.setVisible(visible);
        
        chartUI.doLayout();
        chartUI.repaint();
    }

    public static XYTooltipModel createTooltipModel(final int chartType,
                                                    final Color[] itemColors,
                                                    final double chartFactor,
                                                    final NumberFormat customFormat,
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
                double value = itemValue * chartFactor;
                switch (chartType) {
                    case TYPE_BYTES  : return formatBytes((long)value);
                    case TYPE_PERCENT: return formatPercent(value);
                    case TYPE_BITS_PER_SEC  : return formatBitsPerSec((long)value);
                    default:           return formatDecimal(value, customFormat);
                }
            }

        };
    }

    public static AxisMarksPainter customizeMarksPainter(AxisMarksPainter.Abstract painter) {
        painter.setForeground(AXIS_FONT_COLOR);
        painter.setFont(smallerFont(painter.getFont()));
        return painter;
    }

    public static String formatDecimal(double value) {
        return DECIMAL_FORMATTER.format(value);
    }
    
    public static String formatDecimal(double value, NumberFormat format) {
        return format != null ? format.format(value) : formatDecimal(value);
    }

    public static String formatBytes(long value) {
        String bytesFormat = NbBundle.getMessage(SimpleXYChartUtils.class,
                                                "SimpleXYChartUtils_BytesFormat"); // NOI18N
        return MessageFormat.format(bytesFormat, new Object[] { formatDecimal(value) });
    }

    public static String formatBitsPerSec(long value) {
        String bpsFormat = NbBundle.getMessage(SimpleXYChartUtils.class,
                                                "SimpleXYChartUtils_BitsPerSecFormat"); // NOI18N
        return MessageFormat.format(bpsFormat, new Object[] { formatDecimal(value) });
    }

    public static String formatPercent(double value) {
        return PERCENT_FORMATTER.format(value / 100);
    }

    public static String formatTime(long timestamp, long startTime, long endTime) {
        String formatString = TimeAxisUtils.getFormatString(1000, startTime, endTime);
        return getFormat(formatString).format(new Date(timestamp));
    }

    public static Font smallerFont(Font font) {
        return font.deriveFont((float)font.getSize() - 2);
    }

    public static Font boldFont(Font font) {
        return font.deriveFont(Font.BOLD);
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

    private static JLabel createTitleLabel(String text) {
        JLabel label = createRotatedLabel(text, RotateLabelUI.R0);
        label.setBorder(BorderFactory.createEmptyBorder(8, 3, 0, 3));
        label.setFont(boldFont(label.getFont()));
        return label;
    }

    private static JLabel createXAxisLabel(String text) {
        JLabel label = createRotatedLabel(text, RotateLabelUI.R0);
        label.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return label;
    }

    private static JLabel createYAxisLabel(String text) {
        JLabel label = createRotatedLabel(text, RotateLabelUI.R270);
        label.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        return label;
    }

    private static JLabel createRotatedLabel(String text, final LabelUI labelUI) {
        return new JLabel(text, SwingConstants.CENTER) {
            public void updateUI() { if (getUI() != labelUI) setUI(labelUI); }
        };
    }

    private static final Color  HEAP_LIMIT_FILL_COLOR = !UISupport.isDarkResultsBackground() ?
                               new Color(220, 220, 220) : new Color(100, 100, 100);

    private static ChartDecorator createMaxHeapDecorator(final long limitYValue) {
        return new ChartDecorator() {
            public void paint(Graphics2D g, Rectangle dirtyArea,
                              ChartContext context) {

                int limitHeight = Utils.checkedInt(context.getViewY(limitYValue));
                if (limitHeight <= context.getViewportHeight()) {
                    g.setColor(HEAP_LIMIT_FILL_COLOR);
                    if (context.isBottomBased())
                        g.fillRect(0, 0, context.getViewportWidth(), limitHeight);
                    else
                        g.fillRect(0, limitHeight, context.getViewportWidth(),
                                   context.getViewportHeight() - limitHeight);
                }
            }
        };
    }

    // --- DetailsHandle -------------------------------------------------------

    public static interface DetailsHandle {
        public void updateDetails(String[] details);
    }


    // --- IconCheckBox --------------------------------------------------------

    private static class IconCheckBox extends JCheckBox {

        private static final int CHECKBOX_OFFSET = getCheckBoxOffset();

        private final JCheckBox renderer;

        IconCheckBox(String text, Icon icon, boolean selected) {
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
            if (UISupport.isWindowsLookAndFeel()) return 3;
            else if (UISupport.isNimbusLookAndFeel()) return -3;
            else if (UISupport.isMetalLookAndFeel()) return 3;
            else if (UISupport.isAquaLookAndFeel()) return 6;
            else return 0;
        }

    }

}
