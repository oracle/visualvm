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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.management.*;
import javax.swing.*;
import javax.swing.tree.*;
import org.graalvm.visualvm.modules.mbeans.XNodeInfo.Type;

import java.util.logging.Level;
import java.util.logging.Logger;
import static org.graalvm.visualvm.modules.mbeans.Resources.*;

@SuppressWarnings("serial")
class XSheet extends JPanel
        implements ActionListener, NotificationListener {
    private final static Logger LOGGER = Logger.getLogger(XSheet.class.getName());

    private JPanel topPanelAttributes;
    private JPanel topPanelOperations;
    private JPanel topPanelNotifications;
    private JPanel topPanelMetadata;

    // Node being currently displayed
    private volatile DefaultMutableTreeNode currentNode;

    // MBean being currently displayed
    private volatile XMBean mbean;

    // XMBeanAttributes container
    private XMBeanAttributes mbeanAttributes;

    // XMBeanOperations container
    private XMBeanOperations mbeanOperations;

    // XMBeanNotifications container
    private XMBeanNotifications mbeanNotifications;

    // XMBeanInfo container
    private XMBeanInfo mbeanInfo;

    // Refresh JButton (mbean attributes case)
    private JButton refreshButton;

    // Subscribe/Unsubscribe/Clear JButton (mbean notifications case)
    private JButton clearButton, subscribeButton, unsubscribeButton;

    // Reference to MBeans tab
    private MBeansTab mbeansTab;

    public XSheet(MBeansTab mbeansTab) {
        this.mbeansTab = mbeansTab;
        setupScreen();
    }

    public JPanel getAttributes() {
        return topPanelAttributes;
    }

    public JPanel getOperations() {
        return topPanelOperations;
    }

    public JPanel getNotifications() {
        return topPanelNotifications;
    }

    public JPanel getMetadata() {
        return topPanelMetadata;
    }

    public void dispose() {
        clearNotifications();
        clear();
        XDataViewer.dispose(mbeansTab);
        mbeanNotifications.dispose();
        displayEmptyNode();
    }

    private void setupScreen() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        // create attributes panel
        topPanelAttributes = new JPanel();
        topPanelAttributes.setLayout(new BorderLayout());
        // create operations panel
        topPanelOperations = new JPanel();
        topPanelOperations.setLayout(new BorderLayout());
        // create notifications panel
        topPanelNotifications = new JPanel();
        topPanelNotifications.setLayout(new BorderLayout());
        // create metadata panel
        topPanelMetadata = new JPanel();
        topPanelMetadata.setLayout(new BorderLayout());
        // create the refresh button
        String refreshButtonKey = "LBL_MBeansTab.refreshAttributesButton"; // NOI18N
        refreshButton = new JButton(getText(refreshButtonKey));
        refreshButton.setMnemonic(getMnemonicInt(refreshButtonKey));
        refreshButton.setToolTipText(getText(refreshButtonKey + ".toolTip")); // NOI18N
        refreshButton.addActionListener(this);
        // create the clear button
        String clearButtonKey = "LBL_MBeansTab.clearNotificationsButton"; // NOI18N
        clearButton = new JButton(getText(clearButtonKey));
        clearButton.setMnemonic(getMnemonicInt(clearButtonKey));
        clearButton.setToolTipText(getText(clearButtonKey + ".toolTip")); // NOI18N
        clearButton.addActionListener(this);
        // create the subscribe button
        String subscribeButtonKey = "LBL_MBeansTab.subscribeNotificationsButton"; // NOI18N
        subscribeButton = new JButton(getText(subscribeButtonKey));
        subscribeButton.setMnemonic(getMnemonicInt(subscribeButtonKey));
        subscribeButton.setToolTipText(getText(subscribeButtonKey + ".toolTip")); // NOI18N
        subscribeButton.addActionListener(this);
        // create the unsubscribe button
        String unsubscribeButtonKey = "LBL_MBeansTab.unsubscribeNotificationsButton"; // NOI18N
        unsubscribeButton = new JButton(getText(unsubscribeButtonKey));
        unsubscribeButton.setMnemonic(getMnemonicInt(unsubscribeButtonKey));
        unsubscribeButton.setToolTipText(getText(unsubscribeButtonKey + ".toolTip")); // NOI18N
        unsubscribeButton.addActionListener(this);
        // create XMBeanAttributes container
        mbeanAttributes = new XMBeanAttributes(mbeansTab);
        // create XMBeanOperations container
        mbeanOperations = new XMBeanOperations(mbeansTab);
        mbeanOperations.addOperationsListener(this);
        // create XMBeanNotifications container
        mbeanNotifications = new XMBeanNotifications();
        mbeanNotifications.addNotificationsListener(this);
        // create XMBeanInfo container
        mbeanInfo = new XMBeanInfo();
    }

    private boolean isSelectedNode(DefaultMutableTreeNode n, DefaultMutableTreeNode cn) {
        return (cn == n);
    }

    // Call on EDT
    private void showErrorDialog(Object message, String title) {
        new ThreadDialog(this, message, title, JOptionPane.ERROR_MESSAGE).run();
    }

    public boolean isMBeanNode(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof XNodeInfo) {
            XNodeInfo uo = (XNodeInfo) userObject;
            return uo.getType().equals(Type.MBEAN);
        }
        return false;
    }

    // Call on EDT
    public synchronized void displayNode(DefaultMutableTreeNode node) {
        clear();
        displayEmptyNode();
        if (node == null) {
            return;
        }
        currentNode = node;
        clearNotifications();
        Object userObject = node.getUserObject();
        if (userObject instanceof XNodeInfo) {
            XNodeInfo uo = (XNodeInfo) userObject;
            switch (uo.getType()) {
                case MBEAN:
                    displayMBeanAttributesNode(node);
                    displayMBeanOperationsNode(node);
                    displayMBeanNotificationsNode(node);
                    displayMBeanMetadataNode(node);
                    break;
                case NONMBEAN:
                    displayEmptyNode();
                    break;
                default:
                    displayEmptyNode();
                    break;
            }
        } else {
            displayEmptyNode();
        }
    }

    // Call on EDT
    private void displayMBeanMetadataNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.MBEAN)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        final XMBean xmb = mbean; 
        SwingWorker<MBeanInfo,Void> sw = new SwingWorker<MBeanInfo,Void>() {
            @Override
            public MBeanInfo doInBackground() throws InstanceNotFoundException,
                    IntrospectionException, ReflectionException, IOException {
                return xmb.getMBeanInfo();
            }
            @Override
            protected void done() {
                try {
                    MBeanInfo mbi = get();
                    if (xmb == mbean && mbi != null) {
                        if (!isSelectedNode(node, currentNode)) return;
                        mbeanInfo.loadMBeanInfo(xmb, mbi);
                        topPanelMetadata.invalidate();
                        topPanelMetadata.removeAll();
                        mbeansTab.getButtonAt(3).setEnabled(true);
                        JPanel mainPanelMetadata = new JPanel();
                        mainPanelMetadata.setLayout(new BorderLayout());
                        mainPanelMetadata.add(mbeanInfo, BorderLayout.CENTER);
                        topPanelMetadata.add(mainPanelMetadata, BorderLayout.CENTER);
                        JPanel southPanelMetadata = new JPanel();
                        topPanelMetadata.add(southPanelMetadata, BorderLayout.SOUTH);
                        topPanelMetadata.validate();
                        repaint();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    LOGGER.log(Level.SEVERE, "Couldn't get MBeanInfo for MBean [" + // NOI18N
                            xmb.getObjectName() + "]", t); // NOI18N

                    showErrorDialog(t.toString(),
                            Resources.getText("LBL_ProblemDisplayingMBean")); // NOI18N
                }
            }
        };
        mbeansTab.getRequestProcessor().post(sw);
    }

    // Call on EDT
    private void displayMBeanAttributesNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.MBEAN)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        final XMBean xmb = mbean;
        SwingWorker<MBeanInfo,Void> sw = new SwingWorker<MBeanInfo,Void>() {
            @Override
            public MBeanInfo doInBackground() throws InstanceNotFoundException,
                    IntrospectionException, ReflectionException, IOException {
                MBeanInfo mbi = xmb.getMBeanInfo();
                return mbi;
            }
            @Override
            protected void done() {
                try {
                    MBeanInfo mbi = get();
                    mbeanAttributes.loadAttributes(xmb, mbi);
                    if (xmb == mbean && mbi != null && mbi.getAttributes() != null && mbi.getAttributes().length > 0) {
                        if (!isSelectedNode(node, currentNode)) return;
                        topPanelAttributes.invalidate();
                        topPanelAttributes.removeAll();
                        mbeansTab.getButtonAt(0).setEnabled(true);
                        JPanel borderPanel = new JPanel(new BorderLayout());
                        borderPanel.setBorder(BorderFactory.createTitledBorder(
                                Resources.getText("LBL_AttributeValues"))); // NOI18N
                        borderPanel.add(new JScrollPane(mbeanAttributes));
                        JPanel mainPanelAttributes = new JPanel();
                        mainPanelAttributes.setLayout(new BorderLayout());
                        mainPanelAttributes.add(borderPanel, BorderLayout.CENTER);
                        topPanelAttributes.add(mainPanelAttributes, BorderLayout.CENTER);
                        // add the refresh button to the south panel
                        JPanel southPanelAttributes = new JPanel();
                        southPanelAttributes.add(refreshButton, BorderLayout.SOUTH);
                        southPanelAttributes.setVisible(true);
                        refreshButton.setEnabled(true);
                        topPanelAttributes.add(southPanelAttributes, BorderLayout.SOUTH);
                        topPanelAttributes.validate();
                        repaint();
                            }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    LOGGER.log(Level.SEVERE, "Problem displaying MBean " + // NOI18N
                            "attributes for MBean [" + // NOI18N
                            xmb.getObjectName() + "]", t); // NOI18N

                    showErrorDialog(t.toString(),
                            Resources.getText("LBL_ProblemDisplayingMBean")); // NOI18N
                }
            }
        };
        mbeansTab.getRequestProcessor().post(sw);
    }

    // Call on EDT
    private void displayMBeanOperationsNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.MBEAN)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        final XMBean xmb = mbean; 
        SwingWorker<MBeanInfo,Void> sw = new SwingWorker<MBeanInfo,Void>() {
            @Override
            public MBeanInfo doInBackground() throws InstanceNotFoundException,
                    IntrospectionException, ReflectionException, IOException {
                return xmb.getMBeanInfo();
            }
            @Override
            protected void done() {
                try {
                    MBeanInfo mbi = get();
                    if (xmb == mbean && mbi != null && mbi.getOperations() != null && mbi.getOperations().length > 0) {
                        if (!isSelectedNode(node, currentNode)) return;
                        mbeanOperations.loadOperations(xmb, mbi);
                        topPanelOperations.invalidate();
                        topPanelOperations.removeAll();
                        mbeansTab.getButtonAt(1).setEnabled(true);
                        JPanel borderPanel = new JPanel(new BorderLayout());
                        borderPanel.setBorder(BorderFactory.createTitledBorder(
                                Resources.getText("LBL_OperationInvocation"))); // NOI18N
                        borderPanel.add(new JScrollPane(mbeanOperations));
                        JPanel mainPanelOperations = new JPanel();
                        mainPanelOperations.setLayout(new BorderLayout());
                        mainPanelOperations.add(borderPanel, BorderLayout.CENTER);
                        topPanelOperations.add(mainPanelOperations, BorderLayout.CENTER);
                        JPanel southPanelOperations = new JPanel();
                        topPanelOperations.add(southPanelOperations, BorderLayout.SOUTH);
                        topPanelOperations.validate();
                        repaint();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    LOGGER.log(Level.SEVERE, "Problem displaying MBean " + // NOI18N
                            "operations for MBean [" + // NOI18N
                            xmb.getObjectName() + "]", t); // NOI18N
                    showErrorDialog(t.toString(),
                            Resources.getText("LBL_ProblemDisplayingMBean")); // NOI18N
                }
            }
        };
        mbeansTab.getRequestProcessor().post(sw);
    }

    // Call on EDT
    private void displayMBeanNotificationsNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.MBEAN)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        final XMBean xmb = mbean; 
        SwingWorker<Boolean,Void> sw = new SwingWorker<Boolean,Void>() {
            @Override
            public Boolean doInBackground() {
                return xmb.isBroadcaster();
            }
            @Override
            protected void done() {
                try {
                    Boolean isBroadcaster = get();
                    if (xmb == mbean && isBroadcaster != null && isBroadcaster.booleanValue()) {
                        if (!isSelectedNode(node, currentNode)) return;
                        mbeanNotifications.loadNotifications(xmb);
                        updateNotifications();
                        topPanelNotifications.invalidate();
                        topPanelNotifications.removeAll();
                        mbeansTab.getButtonAt(2).setEnabled(true);
                        JPanel borderPanel = new JPanel(new BorderLayout());
                        borderPanel.setBorder(BorderFactory.createTitledBorder(
                                Resources.getText("LBL_NotificationBuffer"))); // NOI18N
                        borderPanel.add(new JScrollPane(mbeanNotifications));
                        JPanel mainPanelNotifications = new JPanel();
                        mainPanelNotifications.setLayout(new BorderLayout());
                        mainPanelNotifications.add(borderPanel, BorderLayout.CENTER);
                        topPanelNotifications.add(mainPanelNotifications, BorderLayout.CENTER);
                        // add the subscribe/unsubscribe/clear buttons to the south panel
                        JPanel southPanelNotifications = new JPanel();
                        southPanelNotifications.add(subscribeButton, BorderLayout.WEST);
                        southPanelNotifications.add(unsubscribeButton, BorderLayout.CENTER);
                        southPanelNotifications.add(clearButton, BorderLayout.EAST);
                        southPanelNotifications.setVisible(true);
                        subscribeButton.setEnabled(true);
                        unsubscribeButton.setEnabled(true);
                        clearButton.setEnabled(true);
                        topPanelNotifications.add(southPanelNotifications, BorderLayout.SOUTH);
                        topPanelNotifications.validate();
                        repaint();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    LOGGER.log(Level.SEVERE, "Problem displaying MBean " + // NOI18N
                            "notifications for MBean [" + // NOI18N
                            xmb.getObjectName() + "]", t); // NOI18N
                    showErrorDialog(t.toString(),
                            Resources.getText("LBL_ProblemDisplayingMBean")); // NOI18N
                }
            }
        };
        mbeansTab.getRequestProcessor().post(sw);
    }

    // Call on EDT
    private void displayEmptyNode() {
        invalidate();
        topPanelAttributes.invalidate();
        topPanelAttributes.removeAll();
        topPanelAttributes.validate();
        topPanelAttributes.repaint();
        topPanelOperations.invalidate();
        topPanelOperations.removeAll();
        topPanelOperations.validate();
        topPanelOperations.repaint();
        topPanelNotifications.invalidate();
        topPanelNotifications.removeAll();
        topPanelNotifications.validate();
        topPanelNotifications.repaint();
        topPanelMetadata.invalidate();
        topPanelMetadata.removeAll();
        topPanelMetadata.validate();
        topPanelMetadata.repaint();
        mbeansTab.getButtonAt(0).setEnabled(false);
        mbeansTab.getButtonAt(1).setEnabled(false);
        mbeansTab.getButtonAt(2).setEnabled(false);
        mbeansTab.getButtonAt(3).setEnabled(false);
        validate();
        repaint();
    }

    /**
     * Subscribe button action.
     */
    private void registerListener() {
        SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground()
            throws InstanceNotFoundException, IOException {
                mbeanNotifications.registerListener(currentNode);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    updateNotifications();
                    validate();
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    LOGGER.log(Level.SEVERE, "Problem adding listener", t); // NOI18N

                    showErrorDialog(t.getMessage(),
                            Resources.getText("LBL_ProblemAddingListener")); // NOI18N
                }
            }
        };
        mbeansTab.getRequestProcessor().post(sw);
    }

    /**
     * Unsubscribe button action.
     */
    private void unregisterListener() {
        SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
            @Override
            public Boolean doInBackground() {
                return mbeanNotifications.unregisterListener(currentNode);
            }
            @Override
            protected void done() {
                try {
                    if (get()) {
                        updateNotifications();
                        validate();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    LOGGER.log(Level.SEVERE, "Problem removing listener", t); // NOI18N
                    showErrorDialog(t.getMessage(),
                            Resources.getText("LBL_ProblemRemovingListener")); // NOI18N
                }
            }
        };
        mbeansTab.getRequestProcessor().post(sw);
    }

    /**
     * Refresh button action.
     */
    private void refreshAttributes() {
        mbeanAttributes.refreshAttributes();
    }

    // Call on EDT
    private void updateNotifications() {
        if (mbeanNotifications.isListenerRegistered(mbean)) {
            long received = mbeanNotifications.getReceivedNotifications(mbean);
            updateReceivedNotifications(currentNode, received);
        } else {
            clearNotifications();
        }
    }

    /**
     * Update notification node label in MBean tree: "Notifications[received]".
     */
    // Call on EDT
    private void updateReceivedNotifications(
            DefaultMutableTreeNode emitter, long received) {
        String text = Resources.getText("LBL_Notifications") + "[" + received + "]"; // NOI18N
        updateNotificationsNodeLabel(emitter, text);
    }

    /**
     * Update notification node label in MBean tree: "Notifications".
     */
    // Call on EDT
    private void clearNotifications() {
        updateNotificationsNodeLabel(currentNode,
                Resources.getText("LBL_Notifications")); // NOI18N
    }

    /**
     * Update notification node label in MBean tree: "Notifications[0]".
     */
    // Call on EDT
    private void clearNotifications0() {
        updateNotificationsNodeLabel(currentNode,
                Resources.getText("LBL_Notifications") + "[0]"); // NOI18N
    }

    /**
     * Update the label of the supplied MBean tree node.
     */
    // Call on EDT
    private void updateNotificationsNodeLabel(
            DefaultMutableTreeNode node, String label) {
        // Find Notifications TabButton and update text
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)
                mbeansTab.getTree().getLastSelectedPathComponent();
        if (node != selectedNode) {
            return;
        }
        invalidate();
        mbeansTab.getButtonAt(2).setText(label);
        validate();
        repaint();
    }

    /**
     * Clear button action.
     */
    // Call on EDT
    private void clearCurrentNotifications() {
        mbeanNotifications.clearCurrentNotifications();
        if (mbeanNotifications.isListenerRegistered(mbean)) {
            // Update notifs in MBean tree "Notifications[0]".
            //
            // Notification buffer has been cleared with a listener been
            // registered so add "[0]" at the end of the node label.
            //
            clearNotifications0();
        } else {
            // Update notifs in MBean tree "Notifications".
            //
            // Notification buffer has been cleared without a listener been
            // registered so don't add "[0]" at the end of the node label.
            //
            clearNotifications();
        }
    }

    // Call on EDT
    private void clear() {
        mbeanAttributes.stopCellEditing();
        mbeanAttributes.emptyTable();
        mbeanAttributes.removeAttributes();
        mbeanOperations.removeOperations();
        mbeanNotifications.stopCellEditing();
        mbeanNotifications.emptyTable();
        mbeanNotifications.disableNotifications();
        mbeanInfo.emptyInfoTable();
        mbean = null;
        currentNode = null;
    }

    /**
     * Notification listener: handles asynchronous reception
     * of MBean operation results and MBean notifications.
     */
    // Call on EDT
    public void handleNotification(Notification e, Object handback) {
        // Operation result
        if (e.getType().equals(XMBeanOperations.OPERATION_INVOCATION_EVENT)) {
            final Object message;
            if (handback == null) {
                JTextArea textArea = new JTextArea("null"); // NOI18N
                textArea.setEditable(false);
                textArea.setEnabled(true);
                textArea.setRows(textArea.getLineCount());
                message = textArea;
            } else {
                Component comp = mbeansTab.getDataViewer().
                        createOperationViewer(handback, mbean);
                if (comp == null) {
                    JTextArea textArea = new JTextArea(handback.toString());
                    textArea.setEditable(false);
                    textArea.setEnabled(true);
                    textArea.setRows(textArea.getLineCount());
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    Dimension d = scrollPane.getPreferredSize();
                    if (d.getWidth() > 400 || d.getHeight() > 250) {
                        scrollPane.setPreferredSize(new Dimension(400, 250));
                    }
                    message = scrollPane;
                } else {
                    if (!(comp instanceof JScrollPane)) {
                        comp = new JScrollPane(comp);
                    }
                    Dimension d = comp.getPreferredSize();
                    if (d.getWidth() > 400 || d.getHeight() > 250) {
                        comp.setPreferredSize(new Dimension(400, 250));
                    }
                    message = comp;
                }
            }
            new ThreadDialog(
                    (Component) e.getSource(),
                    message,
                    Resources.getText("LBL_OperationReturnValue"), // NOI18N
                    JOptionPane.INFORMATION_MESSAGE).run();
        }
        // Got notification
        else if (e.getType().equals(
                XMBeanNotifications.NOTIFICATION_RECEIVED_EVENT)) {
            DefaultMutableTreeNode emitter = (DefaultMutableTreeNode) handback;
            Long received = (Long) e.getUserData();
            updateReceivedNotifications(emitter, received.longValue());
        }
    }

    /**
     * Action listener: handles actions in panel buttons
     */
    // Call on EDT
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            // Refresh button
            if (button == refreshButton) {
                refreshAttributes();
                return;
            }
            // Clear button
            if (button == clearButton) {
                clearCurrentNotifications();
                return;
            }
            // Subscribe button
            if (button == subscribeButton) {
                registerListener();
                return;
            }
            // Unsubscribe button
            if (button == unsubscribeButton) {
                unregisterListener();
                return;
            }
        }
    }
}
