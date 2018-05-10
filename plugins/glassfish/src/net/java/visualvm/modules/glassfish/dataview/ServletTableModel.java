package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.ServletMonitor;
import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.appserv.management.monitor.statistics.AltServletStats;
import org.graalvm.visualvm.core.scheduler.Quantum;
import java.util.Map;

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
        return true;
//        return stats.getRequestCount().getCount() > 0;
    }
}
