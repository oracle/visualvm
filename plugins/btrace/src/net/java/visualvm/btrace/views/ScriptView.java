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
package net.java.visualvm.btrace.views;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.JVMFactory;
import com.sun.tools.visualvm.application.MonitoredData;
import com.sun.tools.visualvm.application.MonitoredDataListener;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatListener;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModelFactory;
import com.sun.tools.visualvm.tools.jvmstat.MonitoredValue;
import java.awt.Color;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import net.java.visualvm.btrace.config.ProbeConfig.ProbeConnection;
import net.java.visualvm.btrace.datasource.ScriptDataSource;
import net.java.visualvm.btrace.ui.components.graph.DynamicGraph;
import net.java.visualvm.btrace.ui.components.graph.DynamicLineGraph;
import net.java.visualvm.btrace.ui.components.graph.ValueProvider;
import net.java.visualvm.btrace.utils.HTMLTextArea;
import org.openide.util.Exceptions;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ScriptView extends DataSourceView {

    private DataViewComponent dataView = null;
    private ScriptDataSource probe;
    volatile private StatsPanel statsPanel = null;
    volatile private DynamicGraph graph = null;

    private JvmstatListener mdl = new JvmstatListener() {
        public void dataChanged(JvmstatModel arg0) {
            if (statsPanel != null) {
                statsPanel.refresh();
            }
            if (graph != null) {
                graph.update();
            }
        }
    };

    public ScriptView(ScriptDataSource probe) {
        super(DataSourceDescriptorFactory.getDescriptor(probe).getName(), DataSourceDescriptorFactory.getDescriptor(probe).getIcon(), POSITION_AT_THE_END);
        this.probe = probe;
        JvmstatModelFactory.getJvmstatFor(probe.getApplication()).addJvmstatListener(mdl);
    }

    @Override
    public synchronized DataViewComponent getView() {
        if (dataView == null) {
            initialize();
        }
        return dataView;
    }

    private List<ValueProvider> getValueProviders() {
        Application app = probe.getApplication();
        JvmstatModel model = JvmstatModelFactory.getJvmstatFor(app);
        if (model != null) {
            List<ValueProvider> providers = new ArrayList<ValueProvider>();
            for (ProbeConnection connection : probe.getConfig().getConnections()) {
                final MonitoredValue mntr = model.findMonitoredValueByName(connection.jvmStatVar);
                providers.add(new ValueProvider(connection.jvmStatVar, connection.name) {

                    @Override
                    public long getValue() {
                        Object value = mntr.getValue();
                        if (value == null) {
                            return 0L;
                        }
                        return Long.parseLong(value.toString());
                    }
                });
            }
            return providers;
        }
        return new ArrayList<ValueProvider>();
    }

    private void initialize() {
        HTMLTextArea generalDataArea = new HTMLTextArea();
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        generalDataArea.setText(getProbeInfo());

        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("", null, generalDataArea);
        DataViewComponent.MasterViewConfiguration masterConfiguration = new DataViewComponent.MasterViewConfiguration(false);

        dataView = new DataViewComponent(masterView, masterConfiguration);

        statsPanel = new StatsPanel(probe.getConfig().getConnections());
        
        graph = new DynamicLineGraph(getValueProviders(), 10);
        graph.setBackground(Color.WHITE);
        graph.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        if (!probe.getConfig().getConnections().isEmpty()) {
            dataView.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Monitored Data", true), DataViewComponent.TOP_RIGHT);
            dataView.addDetailsView(new DataViewComponent.DetailsView("Monitored Data", null, graph, null), DataViewComponent.TOP_RIGHT);
        }
        dataView.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Console Output", true), DataViewComponent.BOTTOM_RIGHT);
        dataView.addDetailsView(new DataViewComponent.DetailsView("Console Output", null, new OutputPane(probe.getReader()), null), DataViewComponent.BOTTOM_RIGHT);
    }

    @Override
    public boolean isClosable() {
        return true;
    }

    private String getProbeInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<h1>").append(probe.getConfig().getName()).append(" (").append(probe.getConfig().getCategory()).append(")").append("</h1>");
        ;
        sb.append("<b>").append("Description:").append("</b><br/>");
        sb.append(cleanseHtml(probe.getConfig().getDescription()));
        sb.append("</html>");

        return sb.toString();
    }

    private static String cleanseHtml(String html) {
        String cleansed = html.replace("<html>", "").replace("</html>", "").replace("<br>", "").replace("<br/>", "").replace("\n", " ").replace("\r", " ").replace("  ", " ").trim();
        return cleansed;
    }
}
