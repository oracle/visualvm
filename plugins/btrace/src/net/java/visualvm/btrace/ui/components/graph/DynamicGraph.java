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
package net.java.visualvm.btrace.ui.components.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.netbeans.lib.profiler.ui.charts.AbstractSynchronousXYChartModel;
import org.netbeans.lib.profiler.ui.charts.SynchronousXYChartAAA;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class DynamicGraph extends JPanel implements PropertyChangeListener {

    protected enum Style {

        LINE, FILL
    }
    final private AtomicBoolean dataAvailable = new AtomicBoolean(false);
    final private List<LegendItem> legendItems = new ArrayList<LegendItem>();

    // @ThreadSafe
    final private class GraphModel extends AbstractSynchronousXYChartModel {

        volatile private int capacity;
        volatile private long lastTimeStamp = System.currentTimeMillis();
        volatile private long firstTimeStamp = lastTimeStamp;
        final private Map<LegendItem, Long> peaks = new HashMap<LegendItem, Long>();
        final private List<Long> timeStamps = new ArrayList<Long>();
        final private List<Map<LegendItem, Long>> chartValues = new ArrayList<Map<LegendItem, Long>>();

        @Override
        public int getItemCount() {
            return capacity;
        }

        @Override
        public long getMaxXValue() {
            return lastTimeStamp;
        }

        @Override
        public long getMaxYValue(int seriesIndex) {
            return getPeak(seriesIndex);
        }

        @Override
        public long getMinXValue() {
            return firstTimeStamp;
        }

        @Override
        public long getMinYValue(int seriesIndex) {
            return 0;
        }

        @Override
        public Color getSeriesColor(int seriesIndex) {
            synchronized (legendItems) {
                if (legendItems.size() == 0) {
                    return Color.WHITE;
                }
                LegendItem li = legendItems.get(seriesIndex);
                if (li == null) {
                    return Color.WHITE;
                }
                return li.getItemColor();
            }
        }

        @Override
        public int getSeriesCount() {
            synchronized (legendItems) {
                return legendItems.size();
            }
        }

        @Override
        public String getSeriesName(int seriesIndex) {
            synchronized (legendItems) {
                if (legendItems.size() == 0) {
                    return "N/A";
                }
                LegendItem li = legendItems.get(seriesIndex);
                if (li == null) {
                    return "N/A";
                }
                return li.getItemName();
            }
        }

        @Override
        public long getXValue(int itemIndex) {
            synchronized (timeStamps) {
                return timeStamps.get(itemIndex);
            }
        }

        @Override
        public long getYValue(int itemIndex, int seriesIndex) {
            synchronized (legendItems) {
                if (legendItems.size() == 0) {
                    return 0L;
                }
                LegendItem li = legendItems.get(seriesIndex);
                if (li == null) {
                    return 0L;
                }
                synchronized (chartValues) {
                    Map<LegendItem, Long> map = chartValues.get(itemIndex);
                    if (map == null) {
                        return 0L;
                    }
                    Long ret = map.get(li);
                    return ret != null ? ret : 0L;
                }
            }
        }

        public void addValues(long timeStamp, Long[] values) {
            if (timeStamp > lastTimeStamp) {
                lastTimeStamp = timeStamp;
            }
            for (int i = 0; i < values.length; i++) {
                setPeak(i, values[i]);
            }
            synchronized (legendItems) {
                synchronized (chartValues) {
                    Map<LegendItem, Long> ntity = new HashMap<LegendItem, Long>();
                    for (int i = 0; i < values.length; i++) {
                        ntity.put(legendItems.get(i), values[i]);
                    }
                    chartValues.add(ntity);
                }
            }

            synchronized (timeStamps) {
                timeStamps.add(timeStamp);
            }

            capacity++;
            if (chart != null && chart.isReady()) {
                fireChartDataChanged();
            }
        }

        private long getPeak(int seriesIndex) {
            synchronized (legendItems) {
                if (legendItems.size() == 0) {
                    return 0L;
                }
                synchronized (peaks) {
                    Long peak = peaks.get(legendItems.get(seriesIndex));
                    return peak == null ? 0 : peak;
                }
            }
        }

        private void setPeak(int seriesIndex, long peak) {
            synchronized (legendItems) {
                synchronized (peaks) {
                    LegendItem item = legendItems.get(seriesIndex);
                    assert item != null;
                    Long oldPeak = peaks.get(item);
                    peaks.put(item, Math.max(peak, oldPeak == null ? 0 : oldPeak));
                }
            }
        }
    };
    private GraphModel chartModel = new GraphModel();
    private SynchronousXYChartAAA chart;
    private JPanel chartPanel;
    private GraphLegend legend;
    private JSplitPane splitPane;

    public DynamicGraph(List<ValueProvider> valueProviders, int itemsToShow) {
        initComponents(valueProviders, itemsToShow);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(GraphLegend.PROPERTY_COLOR)) {
            refreshUI();
        } else if (evt.getPropertyName().equals(GraphLegend.PROPERTY_LEGEND)) {
            if (chart != null) {
                refreshLegendItems();
                chartModel = new GraphModel();
                chart.setModel(chartModel);
                refreshUI();
            }
        }
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (splitPane == null) {
            return;
        }

        splitPane.setBackground(bg);
        chartPanel.setBackground(bg);
        chart.setBackground(bg);
        chart.setBackgroundPaint(bg);
        legend.setBackground(bg);
    }

    private void initComponents(List<ValueProvider> providers, int itemsToShow) {
        setupLegend(providers, itemsToShow);

        chart = new SynchronousXYChartAAA(getPaintStyle() == Style.LINE ? SynchronousXYChartAAA.TYPE_LINE : SynchronousXYChartAAA.TYPE_FILL);
        chart.setOpaque(false);
        chart.setModel(chartModel);

        chart.setBackgroundPaint(getBackground());
        chart.setBackground(getBackground());
        chart.setFitToWindow(true);
        chart.setCopyAcceleration(SynchronousXYChartAAA.COPY_ACCEL_RASTER);

        long curTime = System.currentTimeMillis();

        setLayout(new BorderLayout());
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBackground(getBackground());
        chartPanel.setOpaque(false);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 8));
        chartPanel.add(chart, BorderLayout.CENTER);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartPanel, legend);
        splitPane.setBackground(getBackground());
        splitPane.setOpaque(false);
        splitPane.setResizeWeight(1d);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
        setOpaque(false);

        refreshUI();
    }

    private void refreshUI() {
        if (chart != null) {
            chart.chartDataChanged();
        }
    }

    private void setupLegend(List<ValueProvider> providers, int itemsVissible) {
        legend = new GraphLegend();
        legend.setOpaque(false);
        legend.setBackground(getBackground());
        legend.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        legend.addPropertyChangeListener(this);

        List<LegendItem> items = new ArrayList<LegendItem>(providers.size());

        int counter = 0;
        for (ValueProvider provider : providers) {
            LegendItem item = new LegendItem(provider);
            if (counter++ < itemsVissible) {
                item.setVisible(true);
            }
            items.add(item);
        }
        legend.setItems(items);
        refreshLegendItems();
    }

    private void refreshLegendItems() {
        synchronized (legendItems) {
            legendItems.clear();
            legendItems.addAll(legend.getVisibleItems());
        }
    }

    protected abstract Style getPaintStyle();

    public void update(long timeStamp) {
        if (chartModel == null) {
            return;
        }

        if (dataAvailable.compareAndSet(false, true)) {
            if (chart != null) {
                long curTime = System.currentTimeMillis();
                chart.setupInitialAppearance(curTime, curTime + 1200, 0, 1);
            }
        }
        if (legendItems.size() == 0) {
            return;
        }

        Long[] values = new java.lang.Long[legendItems.size()];
        int counter = 0;
        for (LegendItem item : legendItems) {
            values[counter++] = item.getValueProvider().getValue();
        }
        chartModel.addValues(timeStamp, values);
    }

    public void update() {
        update(System.currentTimeMillis());
    }
}
