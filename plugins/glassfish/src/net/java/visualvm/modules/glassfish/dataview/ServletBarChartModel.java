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
package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.ServletMonitor;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.lib.profiler.ui.charts.AbstractBarChartModel;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ServletBarChartModel extends AbstractBarChartModel {

    final private WebModuleVirtualServerMonitor monitor;
    final private Map<String, ServletMonitor> servletMap;
    private String[] labels;
    private int maxYValue = Integer.MIN_VALUE;
    
    public ServletBarChartModel(WebModuleVirtualServerMonitor monitor) {
        this.monitor = monitor;
        servletMap = new HashMap<String, ServletMonitor>();
        refresh();
    }

    @Override
    public String getXAxisDesc() {
        return "Servlets";
    }

    @Override
    public String[] getXLabels() {
        try {
            getLock().readLock().lock();
            labels = new String[servletMap.size()];
            int i = 0;
            for (Map.Entry<String, ServletMonitor> entry : servletMap.entrySet()) {
                labels[i++] = entry.getKey();
            }
            return labels;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    public String getYAxisDesc() {
        return "Processing time";
    }

    @Override
    public int[] getYValues() {
        try {
            getLock().readLock().lock();
            int[] values = new int[labels.length];
            int i = 0;
            for (String label : labels) {
                if (servletMap.containsKey(label)) {
                    int value = (int) servletMap.get(label).getAltServletStats().getProcessingTime().getCount();
                    if (value > maxYValue) {
                        maxYValue = value;
                    }
                    values[i++] = value;
                }
            }
            return values;
        } finally {
            getLock().readLock().unlock();
        }
    }

    public int getMaxYValue() {
        try {
            getLock().readLock().lock();
            return maxYValue;
        } finally {
            getLock().readLock().unlock();
        }
    }

    public void refresh() {
        try {
            getLock().writeLock().lock();
            servletMap.clear();
            servletMap.putAll(monitor.getServletMonitorMap());
        } finally {
            getLock().writeLock().unlock();
        }
    }
}
