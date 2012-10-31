package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.TransactionServiceMonitor;
import com.sun.appserv.management.monitor.statistics.TransactionServiceStats;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.charts.ChartFactory;
import com.sun.tools.visualvm.charts.SimpleXYChartDescriptor;
import com.sun.tools.visualvm.charts.SimpleXYChartSupport;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.uisupport.HTMLTextArea;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;

class TransactionServiceView extends DataSourceView {

    private static final String ICON_PATH = "net/java/visualvm/modules/glassfish/resources/logviewer_icon.png";
    private DataViewComponent dvc;
    private TransactionServiceMonitor monitor;
    private ScheduledTask transRefreshTask;

    public TransactionServiceView(Application app, TransactionServiceMonitor monitor) {
        super(app, "Transaction Service", new ImageIcon(ImageUtilities.loadImage(ICON_PATH, true)).getImage(), POSITION_AT_THE_END, false);
        this.monitor = monitor;

        initComponents();
    }

    @Override
    public DataViewComponent createComponent() {
        return dvc;
    }

    private SimpleXYChartSupport transactionalServiceChart;

    private void configureTransactionalServiceVisualizer() {
        SimpleXYChartDescriptor desc = SimpleXYChartDescriptor.decimal(10, false, 500);
        desc.addFillItems("Count","Maximum Time");
        transactionalServiceChart = ChartFactory.createSimpleXYChart(desc);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Transactional Service", false), DataViewComponent.BOTTOM_RIGHT);

        final TransactionServiceStats tss = monitor.getTransactionServiceStats();

        transRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                try {
                    transactionalServiceChart.addValues(timeStamp,
                            new long[]{tss.getActiveCount().getCount()});
                } catch (Exception e) {
                    if (!(e instanceof UndeclaredThrowableException)) {
                        Logger.getLogger(TransactionServiceView.class.getName()).log(Level.INFO,"onSchedule",e);
                    } else {
                        Scheduler.sharedInstance().unschedule(transRefreshTask);
                        transRefreshTask = null;
                    }
                }
            }
        }, Quantum.seconds(1));
        dvc.addDetailsView(new DataViewComponent.DetailsView("Transactional Service", null, 10, transactionalServiceChart.getChart(), null), DataViewComponent.BOTTOM_RIGHT);
    }

    private void initComponents() {
        HTMLTextArea generalDataArea = new HTMLTextArea();
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

        DataViewComponent.MasterView monitoringMasterView = new DataViewComponent.MasterView("", null, generalDataArea);
        DataViewComponent.MasterViewConfiguration monitoringMasterConfiguration = new DataViewComponent.MasterViewConfiguration(true);
        dvc = new DataViewComponent(monitoringMasterView, monitoringMasterConfiguration);
        configureTransactionalServiceVisualizer();
    }
}
