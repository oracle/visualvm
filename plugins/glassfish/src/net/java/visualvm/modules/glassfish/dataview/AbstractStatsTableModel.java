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

import com.sun.appserv.management.monitor.MonitoringStats;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;
import javax.management.j2ee.statistics.TimeStatistic;
import javax.swing.table.AbstractTableModel;
import net.java.visualvm.modules.glassfish.util.Touple;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class AbstractStatsTableModel<PM, M extends MonitoringStats, S extends Stats> extends AbstractTableModel {

    final protected PM monitor;
    private ScheduledTask refresh;
    private final AtomicBoolean columnsInitialized = new AtomicBoolean(false);
    private final List<Touple<String, S>> statsList = new ArrayList<Touple<String, S>>();
    private String[] columnNames;

    public AbstractStatsTableModel(PM aMonitor, Quantum refreshInterval) {
        super();
        monitor = aMonitor;
        refresh = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            public void onSchedule(long timeStamp) {
                try {
                synchronized (statsList) {
                    statsList.clear();
                    for (Map.Entry<String, M> monitor : getMonitorMap().entrySet()) {
                        setColumnModel(monitor.getValue());
                        S stats = getStats(monitor.getValue());
                        if (!isDisplayable(stats)) {
                            continue;
                        }
                        statsList.add(new Touple(monitor.getKey(), stats));
                    }
                    fireTableDataChanged();
                }
                } catch (Exception e) {
                    Scheduler.sharedInstance().unschedule(refresh);
                    refresh = null;
                }
            }
        }, refreshInterval, true);
    }

    abstract protected Map<String, M> getMonitorMap();
    abstract protected S getStats(M monitor);
    abstract protected boolean isDisplayable(S stats);
    
    private void setColumnModel(M monitor) {
        if (columnsInitialized.compareAndSet(false, true)) {
            columnNames = monitor.getStatisticNames();
        }
    }

    public int getColumnCount() {
        if (columnsInitialized.get()) {
            return columnNames.length;
        } else {
            return 0;
        }
    }

    public int getRowCount() {
        synchronized (statsList) {
            return statsList.size();
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        Touple<String, S> entry;
        synchronized (statsList) {
            entry = statsList.get(rowIndex);
        }
        switch (columnIndex) {
            case 0:
                {
                    return entry.getX();
                }
            default:
                {
                    String name = columnNames[columnIndex - 1];
                    Statistic stat = entry.getY().getStatistic(name);
                    if (stat instanceof CountStatistic) {
                        return ((CountStatistic) stat).getCount();
                    } else if (stat instanceof TimeStatistic) {
                        TimeStatistic ts = (TimeStatistic) stat;
                        return ts.getCount() != 0 ? (double) ts.getTotalTime() / (double) ts.getCount() : 0.0;
                    }
                    return stat;
                }
        }
    }

    @Override
    public String getColumnName(int column) {
        if (columnsInitialized.get()) {
            switch (column) {
                case 0:
                    {
                        return "Name";
                    }
                default:
                    {
                        return columnNames[column - 1];
                    }
            }
        } else {
            return super.getColumnName(column);
        }
    }
}
