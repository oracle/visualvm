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

import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import javax.swing.BorderFactory;
import net.java.visualvm.btrace.datasource.ProbeDataSource;
import net.java.visualvm.btrace.utils.HTMLTextArea;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ProbeView extends DataSourceView {
    private DataViewComponent dataView = null;
    private ProbeDataSource probe;

    public ProbeView(ProbeDataSource probe) {
        super(DataSourceDescriptorFactory.getDescriptor(probe).getName(), DataSourceDescriptorFactory.getDescriptor(probe).getIcon(), POSITION_AT_THE_END);
        this.probe = probe;
    }

    @Override
    public synchronized DataViewComponent getView() {
        if (dataView == null) {
            initialize();
        }
        return dataView;
    }

    private void initialize() {
        HTMLTextArea generalDataArea = new HTMLTextArea();
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));
        generalDataArea.setText(getProbeInfo());

        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("", null, generalDataArea);
        DataViewComponent.MasterViewConfiguration masterConfiguration = new DataViewComponent.MasterViewConfiguration(false);

        dataView = new DataViewComponent(masterView, masterConfiguration);

        ProbeViewPanel pvp = new ProbeViewPanel();
        dataView.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Console Output", true), DataViewComponent.BOTTOM_LEFT);
        dataView.addDetailsView(new DataViewComponent.DetailsView("Console Output", null, pvp, null), DataViewComponent.BOTTOM_LEFT);

        pvp.setInputStream(probe.getInputStream());
    }

    @Override
    public boolean isClosable() {
        return true;
    }
    
    private String getProbeInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<h1>").append(probe.getConfig().getName()).append(" (").append(probe.getConfig().getCategory()).append(")").append("</h1>");;
        sb.append("<b>").append("Description:").append("</b><br/>");
        sb.append(cleanse(probe.getConfig().getDescription()));
        sb.append("</html>");
        
        return sb.toString();
    }
    
    private static String cleanse(String html) {
        String cleansed = html.replace("<html>","").replace("</html>", "").replace("<br>", "").replace("<br/>", "").replace("\n", " ").replace("\r", " ").replace("  ", " ").trim();
        return cleansed;
    }
}
