package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.ServletMonitor;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.appserv.management.monitor.statistics.AltServletStats;
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
import javax.management.j2ee.statistics.TimeStatistic;
import javax.swing.table.AbstractTableModel;
import net.java.visualvm.modules.glassfish.util.Touple;

class ServletTableModel extends AbstractStatsTableModel<WebModuleVirtualServerMonitor, ServletMonitor, AltServletStats> {
    public ServletTableModel(WebModuleVirtualServerMonitor aMonitor, Quantum refreshInterval) {
        super(aMonitor, refreshInterval);
    }
    
    @Override
    protected Map<String, ServletMonitor> getMonitorMap() {
        return monitor.getServletMonitorMap();
    }

    @Override
    protected AltServletStats getStats(ServletMonitor monitor) {
        return monitor.getAltServletStats();
    }

    @Override
    protected boolean isDisplayable(AltServletStats stats) {
        return stats.getRequestCount().getCount() > 0;
    }
}
