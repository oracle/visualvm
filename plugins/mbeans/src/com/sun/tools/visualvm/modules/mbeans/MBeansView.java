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

package com.sun.tools.visualvm.modules.mbeans;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class MBeansView extends DataSourceView {

    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/mbeans/ui/resources/mbeans.png"; // NOI18N
    private Application application;
    private MBeansTab mbeansTab;
    
    public MBeansView(Application application) {
        super(application, Resources.getText("LBL_MBeans"), new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 50, false); // NOI18N
        this.application = application;
    }

    @Override
    protected void removed() {
        if (mbeansTab != null) {
            mbeansTab.dispose();
        }
    }

    protected DataViewComponent createComponent() {
        DataViewComponent dvc = null;
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        if (jmx == null || jmx.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
            JTextArea textArea = new JTextArea("\n\n" + Resources.getText("LBL_ConnectionNotEstablished")); // NOI18N
            textArea.setEditable(false);
            dvc = new DataViewComponent(
                new DataViewComponent.MasterView(Resources.getText("LBL_MBeansBrowser"), null, textArea), // NOI18N
                new DataViewComponent.MasterViewConfiguration(true));
        } else {           
            // MBeansTab
            mbeansTab = new MBeansTab(application);
            jmx.addPropertyChangeListener(WeakListeners.propertyChange(mbeansTab, jmx));

            // MBeansTreeView
            MBeansTreeView mbeansTreeView = new MBeansTreeView(mbeansTab);

            // MBeansAttributesView
            MBeansAttributesView mbeansAttributesView = new MBeansAttributesView(mbeansTab);

            // MBeansOperationsView
            MBeansOperationsView mbeansOperationsView = new MBeansOperationsView(mbeansTab);

            // MBeansNotificationsView
            MBeansNotificationsView mbeansNotificationsView = new MBeansNotificationsView(mbeansTab);

            // MBeansMetadataView
            MBeansMetadataView mbeansMetadataView = new MBeansMetadataView(mbeansTab);

            DataViewComponent.MasterView monitoringMasterView = new DataViewComponent.MasterView(Resources.getText("LBL_MBeansBrowser"), null, new JLabel(" ")); // NOI18N
            DataViewComponent.MasterViewConfiguration monitoringMasterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
            dvc = new DataViewComponent(monitoringMasterView, monitoringMasterConfiguration);
        
            dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.33, 0, -1, -1, -1, -1));

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(Resources.getText("LBL_MBeans"), false), DataViewComponent.TOP_LEFT); // NOI18N
            dvc.addDetailsView(new DataViewComponent.DetailsView(Resources.getText("LBL_MBeans"), null, 10, mbeansTreeView, null), DataViewComponent.TOP_LEFT); // NOI18N
            
            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(Resources.getText("LBL_Details"), false), DataViewComponent.TOP_RIGHT); // NOI18N
            dvc.addDetailsView(new DataViewComponent.DetailsView(Resources.getText("LBL_Attributes"), null, 10, mbeansAttributesView, null), DataViewComponent.TOP_RIGHT); // NOI18N
            dvc.addDetailsView(new DataViewComponent.DetailsView(Resources.getText("LBL_Operations"), null, 20, mbeansOperationsView, null), DataViewComponent.TOP_RIGHT); // NOI18N
            dvc.addDetailsView(new DataViewComponent.DetailsView(Resources.getText("LBL_Notifications"), null, 30, mbeansNotificationsView, null), DataViewComponent.TOP_RIGHT); // NOI18N
            dvc.addDetailsView(new DataViewComponent.DetailsView(Resources.getText("LBL_Metadata"), null, 40, mbeansMetadataView, null), DataViewComponent.TOP_RIGHT); // NOI18N

            mbeansTab.setView(dvc);
            mbeansTab.getButtonAt(0).setEnabled(false); // Disable "Attributes"
            mbeansTab.getButtonAt(1).setEnabled(false); // Disable "Operations"
            mbeansTab.getButtonAt(2).setEnabled(false); // Disable "Notifications"
            mbeansTab.getButtonAt(3).setEnabled(false); // Disable "Metadata"
        }
        return dvc;
    }
}
