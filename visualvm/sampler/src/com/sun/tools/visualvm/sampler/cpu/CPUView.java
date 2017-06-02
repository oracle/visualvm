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

package com.sun.tools.visualvm.sampler.cpu;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.ApplicationMethod;
import com.sun.tools.visualvm.sampler.AbstractSamplerSupport;
import com.sun.tools.visualvm.uisupport.TransparentToolBar;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.openide.awt.Actions;
import org.openide.util.*;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Jiri Sedlacek
 */
final class CPUView extends JPanel {

    private final AbstractSamplerSupport.Refresher refresher;
    private boolean forceRefresh = false;
    
    private final CPUSamplerSupport.SnapshotDumper snapshotDumper;
    private final CPUSamplerSupport.ThreadDumper threadDumper;

    private SampledLivePanel resultsPanel;


    CPUView(Application app, AbstractSamplerSupport.Refresher refresher, CPUSamplerSupport.SnapshotDumper
            snapshotDumper, CPUSamplerSupport.ThreadDumper threadDumper) {
        this.refresher = refresher;
        this.snapshotDumper = snapshotDumper;
        this.threadDumper = threadDumper;
        
        initComponents(app);

        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) CPUView.this.refresher.refresh();
                }
            }
        });
    }


    void initSession() {
        if (resultsPanel != null) {
            remove(resultsPanel);
            resultsPanel = null;
        }
        snapshotButton.setEnabled(false);
        add(noDataLabel, BorderLayout.CENTER);
        noDataLabel.invalidate();
        validate();
        repaint();
    }

    void setResultsPanel(SampledLivePanel resultsPanel) {
        this.resultsPanel = resultsPanel;
    }

    void refresh() {
        if (noDataLabel.getParent() == this) {
            remove(noDataLabel);
            resultsPanel.setPreferredSize(new Dimension(1, 1));
            JScrollPane resultsScroll = (JScrollPane)resultsPanel.getComponent(0);
            resultsScroll.setBorder(BorderFactory.createEmptyBorder());
            resultsScroll.setViewportBorder(BorderFactory.createEmptyBorder());
            add(resultsPanel, BorderLayout.CENTER);
            resultsPanel.invalidate();
            validate();
            repaint();
        }

        if (!isShowing() || (pauseButton.isSelected() && !forceRefresh)) return;
        forceRefresh = false;
        resultsPanel.updateLiveResults();

        snapshotButton.setEnabled(snapshotDumper != null);
    }

    void terminate() {
//        refreshRateLabel.setEnabled(false);
//        refreshCombo.setEnabled(false);
//        refreshUnitsLabel.setEnabled(false);
        pauseButton.setEnabled(false);
        refreshButton.setEnabled(false);
        threaddumpButton.setEnabled(false);
    }


    private void initComponents(Application app) {
        setLayout(new BorderLayout());
        setOpaque(false);

        final TransparentToolBar toolBar = new TransparentToolBar();

//        refreshRateLabel = new JLabel("Refresh: ");
//        refreshRateLabel.setToolTipText("Live results refresh rate [ms]");
//        toolBar.add(refreshRateLabel);
//
//        Integer[] refreshRates = new Integer[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
//        refreshCombo = new JComboBox(refreshRates) {
//            public Dimension getMinimumSize() { return getPreferredSize(); }
//            public Dimension getMaximumSize() { return getPreferredSize(); }
//        };
//        refreshCombo.setToolTipText("Live results refresh rate [ms]");
//        refreshCombo.setEditable(false);
//        refreshCombo.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                refresher.setRefreshRate((Integer)refreshCombo.getSelectedItem());
//            }
//        });
//        refreshCombo.setSelectedItem(refresher.getRefreshRate());
//        refreshCombo.setRenderer(new ComboRenderer(refreshCombo));
//        toolBar.add(refreshCombo);
//
//        refreshUnitsLabel = new JLabel(" ms.  ");
//        refreshUnitsLabel.setToolTipText("Live results refresh rate [ms]");
//        toolBar.add(refreshUnitsLabel);

        pauseButton = new JToggleButton() {
            protected void fireActionPerformed(ActionEvent event) {
                boolean selected = pauseButton.isSelected();
                refreshButton.setEnabled(selected);
                if (!selected) refresher.refresh();
            }
        };
        pauseButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/sampler/resources/pause.png", true))); // NOI18N
        pauseButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Pause_results")); // NOI18N
        pauseButton.setOpaque(false);
        toolBar.addItem(pauseButton);

        refreshButton = new JButton() {
            protected void fireActionPerformed(ActionEvent event) {
                forceRefresh = true;
                refresher.refresh();
            }
        };
        refreshButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/sampler/resources/update.png", true))); // NOI18N
        refreshButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Update_results")); // NOI18N
        refreshButton.setEnabled(pauseButton.isSelected());
        refreshButton.setOpaque(false);
        toolBar.addItem(refreshButton);

        toolBar.addSeparator();

        snapshotButton = new JButton(NbBundle.getMessage(CPUView.class, "LBL_Snapshot"), // NOI18N)
                new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/sampler/resources/snapshot.png", true))) { // NOI18N)
            protected void fireActionPerformed(ActionEvent event) {
                snapshotDumper.takeSnapshot((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        };
        snapshotButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Snapshot")); // NOI18N
        snapshotButton.setOpaque(false);
        snapshotButton.setEnabled(false);
        toolBar.addItem(snapshotButton);

        toolBar.addSeparator();
        fillInToolbar(app, toolBar);

        toolBar.addFiller();

        threaddumpButton = new JButton(NbBundle.getMessage(CPUView.class, "LBL_Thread_dump")) { // NOI18N
            protected void fireActionPerformed(ActionEvent event) {
                threadDumper.takeThreadDump((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        };
        threaddumpButton.setToolTipText(NbBundle.getMessage(CPUView.class, "TOOLTIP_Thread_dump")); // NOI18N
        threaddumpButton.setOpaque(false);
        threaddumpButton.setEnabled(threadDumper != null);
        toolBar.addItem(threaddumpButton);

        int maxHeight = pauseButton.getPreferredSize().height;
        maxHeight = Math.max(maxHeight, refreshButton.getPreferredSize().height);
        maxHeight = Math.max(maxHeight, snapshotButton.getPreferredSize().height);
        maxHeight = Math.max(maxHeight, threaddumpButton.getPreferredSize().height);

        int width = pauseButton.getPreferredSize().width;
        Dimension size = new Dimension(maxHeight, maxHeight);
        pauseButton.setMinimumSize(size);
        pauseButton.setPreferredSize(size);
        pauseButton.setMaximumSize(size);

        width = refreshButton.getPreferredSize().width;
        size = new Dimension(maxHeight, maxHeight);
        refreshButton.setMinimumSize(size);
        refreshButton.setPreferredSize(size);
        refreshButton.setMaximumSize(size);

        width = snapshotButton.getPreferredSize().width;
        size = new Dimension(width + 5, maxHeight);
        snapshotButton.setMinimumSize(size);
        snapshotButton.setPreferredSize(size);
        snapshotButton.setMaximumSize(size);

        width = threaddumpButton.getPreferredSize().width;
        size = new Dimension(width + 5, maxHeight);
        threaddumpButton.setMinimumSize(size);
        threaddumpButton.setPreferredSize(size);
        threaddumpButton.setMaximumSize(size);

        add(TransparentToolBar.withSeparator(toolBar), BorderLayout.NORTH);
        
        noDataLabel = new JLabel(NbBundle.getMessage(CPUView.class, "LBL_No_data"), // NOI18N
                                 SwingConstants.CENTER);
        
    }

    private void fillInToolbar(final Application app, final TransparentToolBar toolBar) throws MissingResourceException {
        List<? extends Action> actions = Utilities.actionsForPath("VisualVM/CPUView");
        final InstanceContent ic = new InstanceContent();
        ic.add(app);
        AbstractLookup lookup = new AbstractLookup(ic);
        class L implements TableModelListener, Runnable, ListSelectionListener {
            ClientUtils.SourceCodeSelection sss;
            ApplicationMethod am;

            @Override
            public void tableChanged(TableModelEvent e) {
                JTable t = findJTable(CPUView.this);
                if (t == null) {
                    return;
                }
                int row = t.getSelectedRow();
                if (sss != null) {
                    ic.remove(sss);
                }
                if (am != null) {
                    ic.remove(am);
                }
                if (row == -1) {
                    return;
                }
                String classAndMethod = t.getModel().getValueAt(row, 0).toString();
                int lastDot = classAndMethod.lastIndexOf('.');
                String clazzName = classAndMethod.substring(0, lastDot);
                int param = classAndMethod.lastIndexOf('(');
                if (param == -1) {
                    param = classAndMethod.length();
                }
                String methodName = classAndMethod.substring(lastDot + 1, param);
                String signature = classAndMethod.substring(param);

                ic.add(sss = new ClientUtils.SourceCodeSelection(clazzName, methodName, signature));
                ic.add(am = new ApplicationMethod(app, clazzName, methodName, signature));
            }

            @Override
            public void run() {
                if (EventQueue.isDispatchThread()) {
                    JTable t = findJTable(CPUView.this);
                    if (t != null) {
                        t.getSelectionModel().addListSelectionListener(this);
                    } else {
                        RequestProcessor.getDefault().schedule(this, 100, TimeUnit.MILLISECONDS);
                    }
                    tableChanged(null);
                } else {
                    EventQueue.invokeLater(this);
                }
            }

            @Override
            public void valueChanged(ListSelectionEvent e) {
                tableChanged(null);
            }
        }
        final L listener = new L();
        listener.run();

        for (Action a : actions) {
            if (a instanceof ContextAwareAction) {
                a = ((ContextAwareAction) a).createContextAwareInstance(lookup);
            }
            JButton b = new JButton();
            Actions.connect(b, a);
//            b.setText((String) a.getValue(Action.NAME));
//            b.addActionListener(a);
//            b.setOpaque(false);
            toolBar.addItem(b);
        }
    }

    private static JTable findJTable(Component c) {
        if (c instanceof JTable) {
            return (JTable) c;
        }
        if (c instanceof JComponent) {
            Component[] arr = ((JComponent) c).getComponents();
            for (Component component : arr) {
                JTable t = findJTable(component);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }
    
//    private JLabel refreshRateLabel;
//    private JLabel refreshUnitsLabel;
//    private JComboBox refreshCombo;
    private AbstractButton snapshotButton;
    private AbstractButton pauseButton;
    private AbstractButton refreshButton;
    private AbstractButton threaddumpButton;
    private JLabel noDataLabel;

    
//    private static class ComboRenderer implements ListCellRenderer {
//
//        private ListCellRenderer renderer;
//
//        ComboRenderer(JComboBox combo) {
//            renderer = combo.getRenderer();
//            if (renderer instanceof JLabel)
//                ((JLabel)renderer).setHorizontalAlignment(JLabel.TRAILING);
//        }
//
//        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
//            return renderer.getListCellRendererComponent(list, NumberFormat.getInstance().format(value), index, isSelected, cellHasFocus);
//        }
//
//    }

}
