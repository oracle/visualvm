package net.java.visualvm.modules.glassfish.dataview;

import com.sun.appserv.management.monitor.TransactionServiceMonitor;
import com.sun.appserv.management.monitor.statistics.TransactionServiceStats;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.scheduler.Quantum;
import com.sun.tools.visualvm.core.scheduler.ScheduledTask;
import com.sun.tools.visualvm.core.scheduler.Scheduler;
import com.sun.tools.visualvm.core.scheduler.SchedulerTask;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import net.java.visualvm.modules.glassfish.ui.TransactionsPanel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.Utilities;

class TransactionServiceView extends DataSourceView {

    private static final String ICON_PATH = "net/java/visualvm/modules/glassfish/resources/logviewer_icon.png";
    private DataViewComponent dvc;
    private TransactionServiceMonitor monitor;
    private ScheduledTask transRefreshTask;

    public TransactionServiceView(Application app, TransactionServiceMonitor monitor) {
        super("Transaction Service", new ImageIcon(Utilities.loadImage(ICON_PATH, true)).getImage(), POSITION_AT_THE_END);
        this.monitor = monitor;

        initComponents();
    }

    @Override
    public DataViewComponent getView() {
        return dvc;
    }

    private void configureTransactionalServiceVisualizer() {
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Transactional Service", false), DataViewComponent.BOTTOM_RIGHT);
        final TransactionsPanel.Model model = new TransactionsPanel.Model() {

            @Override
            public int getCommitsPercent() {
                if (monitor == null) {
                    return 0;
                }
                try {
                    TransactionServiceStats stats = monitor.getTransactionServiceStats();

                    return Math.round(((float) stats.getCommittedCount().getCount() / (float) (stats.getCommittedCount().getCount() + stats.getRolledbackCount().getCount())) * 100.0F);
                } catch (RuntimeException e) {
                    return 0;
                }
            }

            @Override
            public long getActive() {
                if (monitor == null) {
                    return 0;
                }
                try {
                    TransactionServiceStats stats = monitor.getTransactionServiceStats();

                    return stats.getActiveCount().getCount();
                } catch (RuntimeException e) {
                    return 0;
                }
            }
        };
        TransactionsPanel panel = new TransactionsPanel();
        panel.setModel(model);
        transRefreshTask = Scheduler.sharedInstance().schedule(new SchedulerTask() {

            @Override
            public void onSchedule(long timeStamp) {
                model.refresh(timeStamp);
                model.notifyObservers();
            }
        }, Quantum.seconds(1));
        dvc.addDetailsView(new DataViewComponent.DetailsView("Transactional Service", null, panel, null), DataViewComponent.BOTTOM_RIGHT);
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
