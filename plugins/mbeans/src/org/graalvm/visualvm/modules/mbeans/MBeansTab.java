/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.tools.jmx.CachedMBeanServerConnection;
import org.graalvm.visualvm.tools.jmx.CachedMBeanServerConnectionFactory;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.modules.mbeans.options.GlobalPreferences;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.beans.*;
import java.io.*;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import org.openide.util.RequestProcessor;

class MBeansTab extends JPanel implements
        NotificationListener, PropertyChangeListener,
        TreeSelectionListener, TreeWillExpandListener {
    private static final Logger LOGGER = Logger.getLogger(MBeansTab.class.getName());
    private static final RequestProcessor worker = new RequestProcessor("MBeansTab Processor"); // NOI18N

    private Application application;
    private DataViewComponent view;
    private XTree tree;
    private XSheet sheet;
    private XDataViewer viewer;
    private CachedMBeanServerConnection cachedMBSC;
    
    public static String getTabName() {
        return Resources.getText("LBL_MBeans"); // NOI18N
    }
    
    public MBeansTab(Application application) {
        this.application = application;
        addPropertyChangeListener(this);
        setupTab();
    }
    
    public RequestProcessor getRequestProcessor() {
        return worker;
    }
    
    public XDataViewer getDataViewer() {
        return viewer;
    }
    
    public XTree getTree() {
        return tree;
    }
    
    public XSheet getSheet() {
        return sheet;
    }
    
    public JPanel getAttributesPanel() {
        return sheet.getAttributes();
    }
    
    public JPanel getOperationsPanel() {
        return sheet.getOperations();
    }
    
    public JPanel getNotificationsPanel() {
        return sheet.getNotifications();
    }
    
    public JPanel getMetadataPanel() {
        return sheet.getMetadata();
    }

    public DataViewComponent getView() {
        return view;
    }

    public void setView(DataViewComponent view) {
        this.view = view;
    }

    public JButton getButtonAt(int position) {
        JComponent containerPanel = (JComponent) view.getComponent(0);
        JComponent detailsPanel = (JComponent) containerPanel.getComponent(1);
        JSplitPane detailsVerticalSplitter = (JSplitPane) detailsPanel.getComponent(0);
        JComponent detailsTopPanel = (JComponent) detailsVerticalSplitter.getLeftComponent();
        JSplitPane detailsTopHorizontalSplitter = (JSplitPane) detailsTopPanel.getComponent(0);
        JComponent detailsTopRightArea = (JComponent) detailsTopHorizontalSplitter.getRightComponent();
        JComponent captionArea = (JComponent) detailsTopRightArea.getComponent(0);
        JComponent tabsContainer = (JComponent) captionArea.getComponent(0);
        JComponent tabButtonContainer = (JComponent) tabsContainer.getComponent(position);
        return (JButton) tabButtonContainer.getComponent(0);
    }
    
    public void dispose() {
        removePropertyChangeListener(this);
        tree.removeTreeSelectionListener(this);
        tree.removeTreeWillExpandListener(this);
        sheet.dispose();
    }
    
    public int getUpdateInterval() {
        return GlobalPreferences.sharedInstance().getPlottersPoll() * 1000;
    }
    
    public void buildMBeanServerView() {
        SwingWorker<Set<ObjectName>, Void> sw = new SwingWorker<Set<ObjectName>, Void>() {
            @Override
            public Set<ObjectName> doInBackground() {
                // Register listener for MBean registration/unregistration
                //
                try {
                    getMBeanServerConnection().addNotificationListener(
                            MBeanServerDelegate.DELEGATE_NAME,
                            MBeansTab.this,
                            null,
                            null);
                } catch (InstanceNotFoundException e) {
                    // Should never happen because the MBeanServerDelegate
                    // is always present in any standard MBeanServer
                    //
                    LOGGER.throwing(MBeansTab.class.getName(), "buildMBeanServerView", e); // NOI18N
                } catch (IOException e) {
                    LOGGER.throwing(MBeansTab.class.getName(), "buildMBeanServerView", e); // NOI18N
                    return null;
                }
                // Retrieve MBeans from MBeanServer
                //
                Set<ObjectName> mbeans = null;
                try {
                    mbeans = getMBeanServerConnection().queryNames(null,null);
                } catch (IOException e) {
                    LOGGER.throwing(MBeansTab.class.getName(), "buildMBeanServerView", e); // NOI18N
                    return null;
                }
                return mbeans;
            }
            @Override
            protected void done() {
                try {
                    // Wait for mbsc.queryNames() result
                    Set<ObjectName> mbeans = get();
                    // Do not display anything until the new tree has been built
                    //
                    tree.setVisible(false);
                    // Cleanup current tree
                    //
                    tree.removeAll();
                    // Add MBeans to tree
                    //
                    tree.addMBeansToView(mbeans);
                    // Display the new tree
                    //
                    tree.setVisible(true);
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    LOGGER.log(Level.SEVERE, "Problem at MBean tree construction", t); // NOI18N
                }
            }
        };
        worker.post(sw);
    }
    
    public MBeanServerConnection getMBeanServerConnection() {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        return jmx == null ? null : jmx.getMBeanServerConnection();
    }
    
    public CachedMBeanServerConnection getCachedMBeanServerConnection() {
        if (cachedMBSC == null) {
            cachedMBSC =
                CachedMBeanServerConnectionFactory.getCachedMBeanServerConnection(
                    getMBeanServerConnection(), getUpdateInterval());
        }
        return cachedMBSC;
    }
    
    private void setupTab() {
        // set up the split pane with the MBean tree and MBean sheet panels
        setLayout(new BorderLayout());
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(160);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        
        // set up the MBean tree panel (left pane)
        tree = new XTree(this);
        tree.setCellRenderer(new XTreeRenderer());
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        tree.addTreeWillExpandListener(this);
        JScrollPane theScrollPane = new JScrollPane(
                tree,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(theScrollPane, BorderLayout.CENTER);
        mainSplit.add(treePanel, JSplitPane.LEFT, 0);
        
        // set up the MBean sheet panel (right pane)
        viewer = new XDataViewer(this);
        sheet = new XSheet(this);
        mainSplit.add(sheet, JSplitPane.RIGHT, 0);
        
        add(mainSplit);
    }
    
    /* notification listener:  handleNotification */
    public void handleNotification(
            final Notification notification, Object handback) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (notification instanceof MBeanServerNotification) {
                    ObjectName mbean =
                            ((MBeanServerNotification) notification).getMBeanName();
                    if (notification.getType().equals(
                            MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
                        tree.addMBeanToView(mbean);
                    } else if (notification.getType().equals(
                            MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
                        tree.removeMBeanFromView(mbean);
                    }
                }
            }
        });
    }
    
    /* property change listener:  propertyChange */
    public void propertyChange(PropertyChangeEvent evt) {
        if (JmxModel.CONNECTION_STATE_PROPERTY.equals(evt.getPropertyName())) {
            ConnectionState newState = (ConnectionState) evt.getNewValue();
            switch (newState) {
                case CONNECTED:
                    buildMBeanServerView();
                    break;
                case DISCONNECTED:
                    dispose();
                    break;
            }
        }
    }
    
    /* tree selection listener: valueChanged */
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        sheet.displayNode(node);
    }
    
    /* tree will expand listener: treeWillExpand */
    public void treeWillExpand(TreeExpansionEvent e)
    throws ExpandVetoException {
//        TreePath path = e.getPath();
//        // if first path component has already been expanded do nothing
//        // else build the tree branch for the given domain
//        if (!tree.hasBeenExpanded(path)) {
//            DefaultMutableTreeNode node =
//                    (DefaultMutableTreeNode) path.getLastPathComponent();
//            // TODO: build branch for given domain - queryNames("d:*", null);
//            // - if not already expanded, create from scratch
//            // - if already expanded, update
//        }
    }
    
    /* tree will expand listener: treeWillCollapse */
    public void treeWillCollapse(TreeExpansionEvent e)
    throws ExpandVetoException {
    }
}
