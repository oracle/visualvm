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

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.jmx.CachedMBeanServerConnection;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
import com.sun.tools.visualvm.core.ui.components.DisplayArea;
import com.sun.tools.visualvm.modules.mbeans.options.GlobalPreferences;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.beans.*;
import java.io.*;
import java.util.Set;
import javax.management.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

class MBeansTab extends JPanel implements
        NotificationListener, PropertyChangeListener,
        TreeSelectionListener, TreeWillExpandListener {
    
    private Application application;
    private DisplayArea displayArea;
    private XTree tree;
    private XSheet sheet;
    private XDataViewer viewer;
    private Worker worker;
    
    public static String getTabName() {
        return Resources.getText("MBeans");
    }
    
    public synchronized void workerAdd(Runnable job) {
        if (worker == null) {
            worker = new Worker(getTabName() + "-" + application.getPid());
            worker.start();
        }
        worker.add(job);
    }
    
    public MBeansTab(Application application) {
        this.application = application;
        addPropertyChangeListener(this);
        setupTab();
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
    
    public DisplayArea getDisplayArea() {
        return displayArea;
    }
    
    public void setDisplayArea(DisplayArea displayArea) {
        this.displayArea = displayArea;
    }
    
    public void dispose() {
        if (worker != null) {
            worker.stopWorker();
        }
        sheet.dispose();
    }
    
    public int getUpdateInterval() {
        return GlobalPreferences.sharedInstance().getPlottersPoll() * 1000;
    }
    
    public void buildMBeanServerView() {
        new SwingWorker<Set<ObjectName>, Void>() {
            @Override
            public Set<ObjectName> doInBackground() {
                // Register listener for MBean registration/unregistration
                //
                try {
                    getCachedMBeanServerConnection().addNotificationListener(
                            MBeanServerDelegate.DELEGATE_NAME,
                            MBeansTab.this,
                            null,
                            null);
                } catch (InstanceNotFoundException e) {
                    // Should never happen because the MBeanServerDelegate
                    // is always present in any standard MBeanServer
                    //
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                // Retrieve MBeans from MBeanServer
                //
                Set<ObjectName> mbeans = null;
                try {
                    mbeans = getCachedMBeanServerConnection().queryNames(null,null);
                } catch (IOException e) {
                    e.printStackTrace();
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
                    System.err.println("Problem at MBean tree construction");
                    t.printStackTrace();
                }
            }
        }.execute();
    }
    
    public MBeanServerConnection getMBeanServerConnection() {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        return jmx == null ? null : jmx.getMBeanServerConnection();
    }
    
    public CachedMBeanServerConnection getCachedMBeanServerConnection() {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        return jmx == null ? null : jmx.getCachedMBeanServerConnection();
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
                    sheet.dispose();
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
//            // - if not already expanded, create from scracth
//            // - if already expanded, update
//        }
    }
    
    /* tree will expand listener: treeWillCollapse */
    public void treeWillCollapse(TreeExpansionEvent e)
    throws ExpandVetoException {
    }
}
