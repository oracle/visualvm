package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.ConnectionQueueMonitor;
import com.sun.appserv.management.monitor.FileCacheMonitor;
import com.sun.appserv.management.monitor.HTTPServiceMonitor;
import com.sun.appserv.management.monitor.KeepAliveMonitor;
import com.sun.appserv.management.monitor.statistics.ConnectionQueueStats;
import com.sun.appserv.management.monitor.statistics.FileCacheStats;
import com.sun.appserv.management.monitor.statistics.KeepAliveStats;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import net.java.visualvm.modules.glassfish.ui.ConnectionQueuePanel;
import net.java.visualvm.modules.glassfish.ui.FileCachePanel;
import net.java.visualvm.modules.glassfish.ui.GenericModel.RangedLong;
import net.java.visualvm.modules.glassfish.ui.KeepAlivePanel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.Utilities;

class HTTPServiceView extends DataSourceView {

    private static final String ICON_PATH = "net/java/visualvm/modules/glassfish/resources/logviewer_icon.png";
    private DataViewComponent dvc;
    private HTTPServiceMonitor monitor;
    private ScheduledTask queueRefreshTask;
    private ScheduledTask cacheRefreshTask;
    private ScheduledTask kaRefreshTask;

    public HTTPServiceView(Application app, HTTPServiceMonitor monitor) {
        super("HTTP Service", new ImageIcon(Utilities.loadImage(ICON_PATH, true)).getImage(), POSITION_AT_THE_END);
        this.monitor = monitor;
        initComponents();
    }

    //~ Methods --------------------------------------------------------------------------------------------------------------
    public boolean isCloseable() {
        return true;
    }

    @Override
    public DataViewComponent getView() {
        return dvc;
    }

    private void configureConnectionQueueVisualizer() {
        ConnectionQueuePanel cqp = new ConnectionQueuePanel();
        final ConnectionQueuePanel.Model model = new ConnectionQueuePanel.Model() {

            ConnectionQueueMonitor queueMonitor = null;
            {
                if (monitor != null) {
                    queueMonitor = monitor.getConnectionQueueMonitor();
                }
            }

            @Override
            public RangedLong getUtilization() {
                if (queueMonitor == null) {
                    return RangedLong.ZERO;
                }
                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();
                long max = stats.getMaxQueued().getCount();
                long current = stats.getCountQueued().getCount();
                return new RangedLong(0L, max, current);
            }

            @Override
            public int getRefusalRate() {
                if (queueMonitor == null) {
                    return 0;
                }
                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();

                if (stats.getCountTotalQueued().getCount() == 0) {
                    return 0;
                }

                return Math.round(((float) stats.getCountOverflows().getCount() / (float) stats.getCountTotalConnections().getCount()) * 100.0F);
            }

            @Override
            public long getAverage1min() {
                if (queueMonitor == null) {
                    return 0L;
                }
                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();

                return stats.getCountQueued1MinuteAverage().getCount();
            }

            @Override
            public long getAverage5min() {
                if (queueMonitor == null) {
                    return 0L;
                }
                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();

                return stats.getCountQueued5MinuteAverage().getCount();
            }

            @Override
            public long getAverage15min() {
                if (queueMonitor == null) {
                    return 0L;
                }
                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();

                return stats.getCountQueued15MinuteAverage().getCount();
            }
        };

        cqp.setModel(model);
        queueRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                model.refresh(timeStamp);
                model.notifyObservers();
            }
        }, Quantum.seconds(1));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Connection Queue", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView("Connection Queue", null, cqp, null), DataViewComponent.TOP_LEFT);
    }

    private void configureFileCacheVisualizer() {
        FileCachePanel fcp = new FileCachePanel();
        final FileCachePanel.Model model = new FileCachePanel.Model() {

            FileCacheMonitor cacheMonitor = null;
            {
                if (monitor != null) {
                    cacheMonitor = monitor.getFileCacheMonitor();
                }
            }

            @Override
            public RangedLong getUtilizationHeap() {
                if (cacheMonitor == null) {
                    return RangedLong.ZERO;
                }
                FileCacheStats stats = cacheMonitor.getFileCacheStats();
                return new RangedLong(0L, stats.getMaxHeapCacheSize().getCount(), stats.getSizeHeapCache().getCount());
            }

            @Override
            public RangedLong getUtilizationAll() {
                if (cacheMonitor == null) {
                    return RangedLong.ZERO;
                }
                FileCacheStats stats = cacheMonitor.getFileCacheStats();
                return new RangedLong(0L, stats.getMaxEntries().getCount(), stats.getMaxEntries().getCount());
            }

            @Override
            public RangedLong getUtilizationOpen() {
                if (cacheMonitor == null) {
                    return RangedLong.ZERO;
                }
                FileCacheStats stats = cacheMonitor.getFileCacheStats();
                return new RangedLong(0L, stats.getMaxOpenEntries().getCount(), stats.getCountOpenEntries().getCount());
            }

            @Override
            public RangedLong getHitRatio() {
                if (cacheMonitor == null) {
                    return RangedLong.ZERO;
                }
                FileCacheStats stats = cacheMonitor.getFileCacheStats();

                long hits = stats.getCountContentHits().getCount();
                long misses = stats.getCountContentMisses().getCount();
                return new RangedLong(0L, hits + misses, misses);
            }
        };

        fcp.setModel(model);
        cacheRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                model.refresh(timeStamp);
                model.notifyObservers();
            }
        }, Quantum.seconds(1));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("File Cache", true), DataViewComponent.BOTTOM_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView("File Cache", null, fcp, null), DataViewComponent.BOTTOM_LEFT);
    }

    private void configureHttpServiceVisualizer() {
        configureConnectionQueueVisualizer();
        configureFileCacheVisualizer();
        configureKeepAliveVisualizer();
    }

    private void configureKeepAliveVisualizer() {
        KeepAlivePanel kap = new KeepAlivePanel();
        final KeepAlivePanel.Model model = new KeepAlivePanel.Model() {

            KeepAliveMonitor kaMonitor = null;
            {
                if (monitor != null) {
                    kaMonitor = monitor.getKeepAliveMonitor();
                }
            }

            @Override
            public long getClosed() {
                if (kaMonitor == null) {
                    return 0;
                }
                KeepAliveStats stats = kaMonitor.getKeepAliveStats();

                return stats.getCountFlushes().getCount();
            }

            @Override
            public long getRejected() {
                if (kaMonitor == null) {
                    return 0;
                }
                KeepAliveStats stats = kaMonitor.getKeepAliveStats();

                return stats.getCountRefusals().getCount();
            }

            @Override
            public long getTimedOut() {
                if (kaMonitor == null) {
                    return 0;
                }
                KeepAliveStats stats = kaMonitor.getKeepAliveStats();

                return stats.getCountTimeouts().getCount();
            }

            @Override
            public RangedLong getUtilization() {
                if (kaMonitor == null) {
                    return RangedLong.ZERO;
                }
                KeepAliveStats stats = kaMonitor.getKeepAliveStats();
                return new RangedLong(0L, stats.getMaxConnections().getCount(), stats.getCountConnections().getCount());
            }
        };

        kap.setModel(model);
        kaRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                model.refresh(timeStamp);
                model.notifyObservers();
            }
        }, Quantum.seconds(1));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Keep Alive", true), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView("Keep Alive", null, kap, null), DataViewComponent.BOTTOM_RIGHT);
    }

    private void initComponents() {
        HTMLTextArea generalDataArea = new HTMLTextArea();
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

        //            DisplayArea monitoringDisplayArea = new DisplayArea();
        //            monitoringDisplayArea.setClosable(true);
        DataViewComponent.MasterView monitoringMasterView = new DataViewComponent.MasterView("", null, generalDataArea);
        DataViewComponent.MasterViewConfiguration monitoringMasterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
        dvc = new DataViewComponent(monitoringMasterView, monitoringMasterConfiguration);
        configureHttpServiceVisualizer();
    }
}
