package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.WebModuleVirtualServerMonitor;
import com.sun.appserv.management.monitor.WebServiceEndpointMonitor;
import com.sun.appserv.management.monitor.statistics.WebServiceEndpointAggregateStats;
import org.graalvm.visualvm.core.scheduler.Quantum;
import java.util.Map;

class WSTableModel extends AbstractStatsTableModel<WebModuleVirtualServerMonitor, WebServiceEndpointMonitor, WebServiceEndpointAggregateStats> {
    public WSTableModel(WebModuleVirtualServerMonitor aMonitor, Quantum refreshInterval) {
        super(aMonitor, refreshInterval);
    }
    
    @Override
    protected Map<String, WebServiceEndpointMonitor> getMonitorMap() {
        return monitor.getWebServiceEndpointMonitorMap();
    }

    @Override
    protected WebServiceEndpointAggregateStats getStats(WebServiceEndpointMonitor monitor) {
        return monitor.getWebServiceEndpointAggregateStats();
    }

    @Override
    protected boolean isDisplayable(WebServiceEndpointAggregateStats stats) {
        return true;
//        return stats.getTotalFaults().getCount() + stats.getTotalNumSuccess().getCount() > 0;
    }
}
