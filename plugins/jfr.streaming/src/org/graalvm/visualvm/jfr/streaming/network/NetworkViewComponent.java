/*
 * Copyright (c) 2022, 2022 Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.jfr.streaming.network;

import java.awt.BorderLayout;
import java.text.MessageFormat;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.charts.ChartFactory;
import org.graalvm.visualvm.charts.SimpleXYChartDescriptor;
import org.graalvm.visualvm.charts.SimpleXYChartSupport;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 */
class NetworkViewComponent extends JPanel {

    private static final String UNKNOWN = NbBundle.getMessage(NetworkViewComponent.class, "LBL_Unknown"); // NOI18N

    private boolean liveModel;
    private boolean networkMonitoringSupported;
    private String panelName;

    private SimpleXYChartSupport chartSupport;

    NetworkViewComponent(NetworkModel model) {
        initModels(model);
        initComponents();
    }

    DataViewComponent.DetailsView getDetailsView() {
        return new DataViewComponent.DetailsView(panelName, null, 10, this, null);
    }

    private void refresh(NetworkModel model) {
        if (networkMonitoringSupported) {
            long readRate = model.getReadRate();
            long writeRate = model.getWriteRate();

            if (liveModel) {
                chartSupport.addValues(model.getTimestamp(), new long[]{readRate, writeRate});
            }
            chartSupport.updateDetails(new String[]{formatKpbs(readRate / 1024),
                formatKpbs(writeRate / 1024)});
        }
    }

    private String formatKpbs(long value) {
        String bytesFormat = NbBundle.getMessage(NetworkViewComponent.class,
                "LBL_Format_Kibps"); // NOI18N
        return MessageFormat.format(bytesFormat, new Object[]{chartSupport.formatDecimal(value)});

    }

    private void initModels(final NetworkModel model) {
        liveModel = model.isLive();
        networkMonitoringSupported = true;
        panelName = NbBundle.getMessage(NetworkViewComponent.class, "LBL_Network"); // NOI18N

        if (networkMonitoringSupported) {
            String READ_RATE = NbBundle.getMessage(NetworkViewComponent.class, "LBL_Read_rate"); // NOI18N
            String READ_RATE_LEG = NbBundle.getMessage(NetworkViewComponent.class, "LBL_Read_rate_leg"); // NOI18N
            String WRITE_RATE = NbBundle.getMessage(NetworkViewComponent.class, "LBL_Write_rate"); // NOI18N
            String WRITE_RATE_LEG = NbBundle.getMessage(NetworkViewComponent.class, "LBL_Write_rate_leg"); // NOI18N
            SimpleXYChartDescriptor chartDescriptor
                    = SimpleXYChartDescriptor.bitsPerSec(10 * 1024 * 1024, false, model.getChartCache());

            chartDescriptor.addLineItems(READ_RATE_LEG, WRITE_RATE_LEG);
            chartDescriptor.setDetailsItems(new String[]{READ_RATE, WRITE_RATE});

            chartSupport = ChartFactory.createSimpleXYChart(chartDescriptor);
            chartSupport.setZoomingEnabled(!liveModel);

            model.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    refresh(model);
                }
            });
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);

        if (networkMonitoringSupported) {
            add(chartSupport.getChart(), BorderLayout.CENTER);
            chartSupport.updateDetails(new String[]{UNKNOWN, UNKNOWN, UNKNOWN});
        } else {
            add(new NotSupportedDisplayer(NotSupportedDisplayer.JVM),
                    BorderLayout.CENTER);
        }
    }
}
