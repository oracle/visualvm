package org.visualvm.demodescriptorprovider;

import org.visualvm.demodescriptorprovider.panels.MemoryMonitor5;
import org.visualvm.demodescriptorprovider.panels.MemoryMonitor2;
import org.visualvm.demodescriptorprovider.panels.MemoryMonitor1;
import org.visualvm.demodescriptorprovider.panels.MemoryMonitor4;
import org.visualvm.demodescriptorprovider.panels.MemoryMonitor3;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.openide.util.Utilities;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;

class DemoDataSourceView extends DataSourceView {

    private DataViewComponent dvc;
    private static final String IMAGE_PATH = "" +
            "org/visualvm/demodescriptorprovider/icon.png"; // NOI18N

    public DemoDataSourceView(DemoDataSource ds) {
        super(ds, "Memory Monitor", new ImageIcon(
                Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, true);
    }

    @Override
    protected DataViewComponent createComponent() {

        //Data area for master view:
        MemoryMonitor1 panel1 = new MemoryMonitor1();
        MemoryMonitor2 panel2 = new MemoryMonitor2();
        MemoryMonitor3 panel3 = new MemoryMonitor3();
        MemoryMonitor4 panel4 = new MemoryMonitor4();
        MemoryMonitor5 panel5 = new MemoryMonitor5();

        panel1.setPreferredSize(new Dimension(650, 200));
        panel2.setPreferredSize(new Dimension(650, 200));
        panel3.setPreferredSize(new Dimension(650, 200));
        panel4.setPreferredSize(new Dimension(650, 200));
        panel5.setPreferredSize(new Dimension(650, 200));

        ScrollableContainer container1 = new ScrollableContainer(panel1);

        JScrollPane jScrollPane1 = new JScrollPane(panel1);
        JScrollPane jScrollPane2 = new JScrollPane(panel2);
        JScrollPane jScrollPane3 = new JScrollPane(panel3);
        JScrollPane jScrollPane4 = new JScrollPane(panel4);
        JScrollPane jScrollPane5 = new JScrollPane(panel5);

        JPanel panel = new JPanel();
        final HTMLTextArea area = new HTMLTextArea();
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(area.getBackground());
        area.setText("<a href=\"" + "hello" + "\">www.hello.org</a>");
        panel.add(new ScrollableContainer(area), BorderLayout.CENTER);

        //Master view:
        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("", null, null);

        //Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration =
                new DataViewComponent.MasterViewConfiguration(false);

        //Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);

        //Add configuration details to the component, which are the show/hide checkboxes at the top:
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Show Code Cache", true), DataViewComponent.TOP_LEFT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Show Eden Space", true), DataViewComponent.TOP_RIGHT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Show Survivor Space", true), DataViewComponent.BOTTOM_LEFT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Show Gen", true), DataViewComponent.BOTTOM_RIGHT);

        //Add detail views to the component:
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Code Cache", null, 30, jScrollPane1, null), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Eden Space", null, 30, jScrollPane2, null), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Survivor Space", null, 30, jScrollPane3, null), DataViewComponent.BOTTOM_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Old Gen", null, 30, jScrollPane4, null), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Perm Gen", null, 30, jScrollPane5, null), DataViewComponent.BOTTOM_RIGHT);

        return dvc;

    }
}

