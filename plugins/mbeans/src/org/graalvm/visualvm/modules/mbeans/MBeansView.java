/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.mbeans;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import org.openide.util.ImageUtilities;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class MBeansView extends DataSourceView {

    private static final String IMAGE_PATH = "org/graalvm/visualvm/modules/mbeans/ui/resources/mbeans.png"; // NOI18N
    private Application application;
    private MBeansTab mbeansTab;
    private MBeansTreeView mbeansTreeView;
    
    public MBeansView(Application application) {
        super(application, Resources.getText("LBL_MBeans"), new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 50, false); // NOI18N
        this.application = application;
    }

    @Override
    protected void removed() {
        if (mbeansTreeView != null) {
            mbeansTreeView.dispose();
        }
        if (mbeansTab != null) {
            mbeansTab.dispose();
        }
    }

    protected DataViewComponent createComponent() {
        DataViewComponent dvc = null;
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        if (jmx == null || jmx.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
            JTextArea textArea = new JTextArea();
            textArea.setBorder(BorderFactory.createEmptyBorder(25, 9, 9, 9));
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setText(Resources.getText("LBL_ConnectionNotEstablished")); // NOI18N
            dvc = new DataViewComponent(
                new DataViewComponent.MasterView(Resources.getText("LBL_MBeansBrowser"), null, textArea), // NOI18N
                new DataViewComponent.MasterViewConfiguration(true));
        } else {           
            // MBeansTab
            mbeansTab = new MBeansTab(application);
            jmx.addPropertyChangeListener(WeakListeners.propertyChange(mbeansTab, jmx));

            // MBeansTreeView
            mbeansTreeView = new MBeansTreeView(mbeansTab);
            jmx.addPropertyChangeListener(WeakListeners.propertyChange(mbeansTreeView, jmx));

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
