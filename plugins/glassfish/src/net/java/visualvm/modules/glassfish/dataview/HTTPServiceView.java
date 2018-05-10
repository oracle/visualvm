package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.ConnectionQueueMonitor;
import com.sun.appserv.management.monitor.FileCacheMonitor;
import com.sun.appserv.management.monitor.HTTPServiceMonitor;
import com.sun.appserv.management.monitor.KeepAliveMonitor;
import com.sun.appserv.management.monitor.statistics.KeepAliveStats;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.scheduler.Quantum;
import org.graalvm.visualvm.core.scheduler.ScheduledTask;
import org.graalvm.visualvm.core.scheduler.Scheduler;
import org.graalvm.visualvm.core.scheduler.SchedulerTask;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;

class HTTPServiceView extends DataSourceView {

    private static final String ICON_PATH = "net/java/visualvm/modules/glassfish/resources/logviewer_icon.png";
    private static final Logger LOGGER = Logger.getLogger(HTTPServiceView.class.getName());
    private DataViewComponent dvc;
    private HTTPServiceMonitor monitor;
    private ScheduledTask queueRefreshTask;
    private ScheduledTask cacheRefreshTask;
    private ScheduledTask kaRefreshTask;

    public HTTPServiceView(Application app, HTTPServiceMonitor monitor) {
        super(app, "HTTP Service", new ImageIcon(ImageUtilities.loadImage(ICON_PATH, true)).getImage(), POSITION_AT_THE_END, false);
        this.monitor = monitor;
        initComponents();
    }

    @Override
    public DataViewComponent createComponent() {
        return dvc;
    }

    private SimpleXYChartSupport connectionQueueChart;

    private void configureConnectionQueueVisualizer() {
        final ConnectionQueueMonitor cqm = monitor.getConnectionQueueMonitor();
        SimpleXYChartDescriptor desc = SimpleXYChartDescriptor.decimal(10, true, 500);
        desc.addLineItems("1min","5min","15min");
        connectionQueueChart = ChartFactory.createSimpleXYChart(desc);

//        ConnectionQueuePanel cqp = new ConnectionQueuePanel();
//        final ConnectionQueuePanel.Model model = new ConnectionQueuePanel.Model() {
//
//            ConnectionQueueMonitor queueMonitor = null;
//            {
//                if (monitor != null) {
//                    queueMonitor = monitor.getConnectionQueueMonitor();
//                }
//            }
//
//            @Override
//            public RangedLong getUtilization() {
//                if (queueMonitor == null) {
//                    return RangedLong.ZERO;
//                }
//                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();
//                long max = stats.getMaxQueued().getCount();
//                long current = stats.getCountQueued().getCount();
//                return new RangedLong(0L, max, current);
//            }
//
//            @Override
//            public int getRefusalRate() {
//                if (queueMonitor == null) {
//                    return 0;
//                }
//                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();
//
//                if (stats.getCountTotalQueued().getCount() == 0) {
//                    return 0;
//                }
//
//                return Math.round(((float) stats.getCountOverflows().getCount() / (float) stats.getCountTotalConnections().getCount()) * 100.0F);
//            }
//
//            @Override
//            public long getAverage1min() {
//                if (queueMonitor == null) {
//                    return 0L;
//                }
//                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();
//
//                return stats.getCountQueued1MinuteAverage().getCount();
//            }
//
//            @Override
//            public long getAverage5min() {
//                if (queueMonitor == null) {
//                    return 0L;
//                }
//                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();
//
//                return stats.getCountQueued5MinuteAverage().getCount();
//            }
//
//            @Override
//            public long getAverage15min() {
//                if (queueMonitor == null) {
//                    return 0L;
//                }
//                ConnectionQueueStats stats = queueMonitor.getConnectionQueueStats();
//
//                return stats.getCountQueued15MinuteAverage().getCount();
//            }
//        };

//        cqp.setModel(model);
        queueRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                try {
                    connectionQueueChart.addValues(timeStamp,
                            new long[] {cqm.getConnectionQueueStats().getCountQueued1MinuteAverage().getCount(),
                                        cqm.getConnectionQueueStats().getCountQueued5MinuteAverage().getCount(),
                                        cqm.getConnectionQueueStats().getCountQueued15MinuteAverage().getCount()});
                } catch (Exception e) {
                    if (!(e instanceof UndeclaredThrowableException)) {
                        LOGGER.log(Level.INFO,"onSchedule",e);
                    } else {
                        Scheduler.sharedInstance().unschedule(queueRefreshTask);
                        queueRefreshTask = null;
                    }
                }
            }
        }, Quantum.seconds(1));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Connection Queue", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView("Connection Queue", null, 10, connectionQueueChart.getChart(), null), DataViewComponent.TOP_LEFT);
    }

    private SimpleXYChartSupport fileCacheChart;

    private void configureFileCacheVisualizer() {
        SimpleXYChartDescriptor desc = SimpleXYChartDescriptor.decimal(10, false, 500);
        desc.addLineFillItems("Min");
        desc.addLineItems("Max","Current");
        fileCacheChart = ChartFactory.createSimpleXYChart(desc);

        final FileCacheMonitor fcm = monitor.getFileCacheMonitor();

//        FileCachePanel fcp = new FileCachePanel();
//        final FileCachePanel.Model model = new FileCachePanel.Model() {
//
//            FileCacheMonitor cacheMonitor = null;
//            {
//                if (monitor != null) {
//                    cacheMonitor = monitor.getFileCacheMonitor();
//                }
//            }
//
//            @Override
//            public RangedLong getUtilizationHeap() {
//                if (cacheMonitor == null) {
//                    return RangedLong.ZERO;
//                }
//                FileCacheStats stats = cacheMonitor.getFileCacheStats();
//                return new RangedLong(0L, stats.getMaxHeapCacheSize().getCount(), stats.getSizeHeapCache().getCount());
//            }
//
//            @Override
//            public RangedLong getUtilizationAll() {
//                if (cacheMonitor == null) {
//                    return RangedLong.ZERO;
//                }
//                FileCacheStats stats = cacheMonitor.getFileCacheStats();
//                return new RangedLong(0L, stats.getMaxEntries().getCount(), stats.getMaxEntries().getCount());
//            }
//
//            @Override
//            public RangedLong getUtilizationOpen() {
//                if (cacheMonitor == null) {
//                    return RangedLong.ZERO;
//                }
//                FileCacheStats stats = cacheMonitor.getFileCacheStats();
//                return new RangedLong(0L, stats.getMaxOpenEntries().getCount(), stats.getCountOpenEntries().getCount());
//            }
//
//            @Override
//            public RangedLong getHitRatio() {
//                if (cacheMonitor == null) {
//                    return RangedLong.ZERO;
//                }
//                FileCacheStats stats = cacheMonitor.getFileCacheStats();
//
//                long hits = stats.getCountContentHits().getCount();
//                long misses = stats.getCountContentMisses().getCount();
//                return new RangedLong(0L, hits + misses, misses);
//            }
//        };
//
//        fcp.setModel(model);
        final long[] minmax = new long[]{Long.MAX_VALUE, 0L};

        cacheRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                try {
                    long hits = fcm.getFileCacheStats().getCountContentHits().getCount();
                    long misses = fcm.getFileCacheStats().getCountContentMisses().getCount();
                    long percent = (hits + misses > 0) ? (hits * 100) / (hits + misses) : 0L;
                    if (percent > minmax[1]) {
                        minmax[1] = percent;
                    }
                    if (percent < minmax[0]) {
                        minmax[0] = percent;
                    }
                    fileCacheChart.addValues(timeStamp, new long[] {
                        minmax[0], minmax[1], percent
                    });
                } catch (Exception e) {
                    if (!(e instanceof UndeclaredThrowableException)) {
                        LOGGER.log(Level.INFO,"onSchedule",e);
                    } else {
                        Scheduler.sharedInstance().unschedule(cacheRefreshTask);
                        cacheRefreshTask = null;
                    }
                }
            }
        }, Quantum.seconds(1));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("File Cache", true), DataViewComponent.BOTTOM_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView("File Cache Hits", null, 10, fileCacheChart.getChart(), null), DataViewComponent.BOTTOM_LEFT);
    }

    private void configureHttpServiceVisualizer() {
        configureConnectionQueueVisualizer();
        configureFileCacheVisualizer();
        configureKeepAliveVisualizer();
    }

    private SimpleXYChartSupport keepAliveChart;

    private void configureKeepAliveVisualizer() {
        final KeepAliveMonitor kaMonitor = monitor.getKeepAliveMonitor();
        SimpleXYChartDescriptor desc = SimpleXYChartDescriptor.decimal(10, false, 500);
        desc.addLineItems("Refused","Flushed","Timed Out");
        keepAliveChart = ChartFactory.createSimpleXYChart(desc);

        kaRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                try {
                    KeepAliveStats stats = kaMonitor.getKeepAliveStats();
                    keepAliveChart.addValues(timeStamp, 
                            new long[] {stats.getCountRefusals().getCount(), stats.getCountFlushes().getCount(), stats.getCountTimeouts().getCount()});
                } catch (Exception e) {
                    if (!(e instanceof UndeclaredThrowableException)) {
                        LOGGER.log(Level.INFO,"onSchedule",e);
                    } else {
                        Scheduler.sharedInstance().unschedule(kaRefreshTask);
                        kaRefreshTask = null;
                    }
                }
            }
        }, Quantum.seconds(1));

        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Keep Alive", true), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView("Keep Alive", null, 10, keepAliveChart.getChart(), null), DataViewComponent.BOTTOM_RIGHT);
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
