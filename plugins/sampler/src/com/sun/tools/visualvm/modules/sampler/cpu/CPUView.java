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

package com.sun.tools.visualvm.modules.sampler.cpu;

import com.sun.tools.visualvm.modules.sampler.AbstractSamplerSupport;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.text.NumberFormat;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
class CPUView extends JPanel {

    private final AbstractSamplerSupport.Refresher refresher;
    private boolean forceRefresh = false;
    
    private final CPUSamplerSupport.SnapshotDumper snapshotDumper;
    private final CPUSamplerSupport.ThreadDumper threadDumper;

    private SampledLivePanel resultsPanel;


    CPUView(AbstractSamplerSupport.Refresher refresher, CPUSamplerSupport.SnapshotDumper
            snapshotDumper, CPUSamplerSupport.ThreadDumper threadDumper) {
        this.refresher = refresher;
        this.snapshotDumper = snapshotDumper;
        this.threadDumper = threadDumper;
        
        initComponents();

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
            resultsScroll.setBorder(BorderFactory.createLoweredBevelBorder());
            resultsScroll.setViewportBorder(BorderFactory.createEmptyBorder());
            add(resultsPanel, BorderLayout.CENTER);
            resultsPanel.invalidate();
            validate();
            repaint();
        }

        if (!isShowing() || (pauseButton.isSelected() && !forceRefresh)) return;
        forceRefresh = false;
        resultsPanel.updateLiveResults();
//        refreshUI();
    }

    void terminate() {
        refreshRateLabel.setEnabled(false);
        refreshCombo.setEnabled(false);
        refreshUnitsLabel.setEnabled(false);
        pauseButton.setEnabled(false);
        refreshButton.setEnabled(false);
//        snapshotButton.setEnabled(false);
        threaddumpButton.setEnabled(false);
    }


    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);

        final JToolBar toolBar = new JToolBar();
        toolBar.setBorderPainted(false);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setOpaque(false);

        refreshRateLabel = new JLabel("Refresh: ");
        toolBar.add(refreshRateLabel);

        Integer[] refreshRates = new Integer[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
        refreshCombo = new JComboBox(refreshRates) {
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        refreshCombo.setEditable(false);
        refreshCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refresher.setRefreshRate((Integer)refreshCombo.getSelectedItem());
            }
        });
        refreshCombo.setSelectedItem(refresher.getRefreshRate());
        refreshCombo.setRenderer(new ComboRenderer(refreshCombo));
        toolBar.add(refreshCombo);

        refreshUnitsLabel = new JLabel(" ms.  ");
        toolBar.add(refreshUnitsLabel);

        pauseButton = new JToggleButton() {
            protected void fireActionPerformed(ActionEvent event) {
                boolean selected = pauseButton.isSelected();
                refreshButton.setEnabled(selected);
                if (!selected) refresher.refresh();
            }
        };
        pauseButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/modules/sampler/resources/pause.png", true))); // NOI18N
        pauseButton.setToolTipText("Pause");
        pauseButton.setOpaque(false);
        toolBar.add(pauseButton);

        refreshButton = new JButton() {
            protected void fireActionPerformed(ActionEvent event) {
                forceRefresh = true;
                refresher.refresh();
            }
        };
        refreshButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/modules/sampler/resources/update.png", true))); // NOI18N
        refreshButton.setToolTipText("Update");
        refreshButton.setEnabled(pauseButton.isSelected());
        refreshButton.setOpaque(false);
        toolBar.add(refreshButton);

        toolBar.addSeparator();

        snapshotButton = new JButton("Snapshot", new ImageIcon(ImageUtilities.loadImage(
                "com/sun/tools/visualvm/modules/sampler/resources/snapshot.png", true))) { // NOI18N)
            protected void fireActionPerformed(ActionEvent event) {
                snapshotDumper.takeSnapshot((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        };
        snapshotButton.setToolTipText("Take snapshot of collected results");
        snapshotButton.setOpaque(false);
        toolBar.add(snapshotButton);

        JPanel toolbarSpacer = new JPanel(null) {
            public Dimension getPreferredSize() {
                if (UIUtils.isGTKLookAndFeel() || UIUtils.isNimbusLookAndFeel()) {
                    int currentWidth = toolBar.getSize().width;
                    int minimumWidth = toolBar.getMinimumSize().width;
                    int extraWidth = currentWidth - minimumWidth;
                    return new Dimension(Math.max(extraWidth, 0), 0);
                } else {
                    return super.getPreferredSize();
                }
            }
        };
        toolbarSpacer.setOpaque(false);
        toolBar.add(toolbarSpacer);

        threaddumpButton = new JButton("Thread Dump") {
            protected void fireActionPerformed(ActionEvent event) {
                threadDumper.takeThreadDump((event.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) == 0);
            }
        };
        threaddumpButton.setToolTipText("Thread Dump");
        threaddumpButton.setOpaque(false);
        threaddumpButton.setEnabled(threadDumper != null);
        toolBar.add(threaddumpButton);

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

        toolBar.setBorder(BorderFactory.createEmptyBorder(4, 9, 3, 4));

        JPanel dataPanel = new JPanel(new BorderLayout());
        dataPanel.setOpaque(false);

        JPanel areaPanel = new JPanel(new BorderLayout());
        areaPanel.setOpaque(false);
        area = new HTMLTextArea("Results data...");
        area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        refreshUI();
        areaPanel.add(area, BorderLayout.NORTH);
        areaPanel.add(new JSeparator(), BorderLayout.SOUTH);

        dataPanel.add(areaPanel, BorderLayout.NORTH);

        add(toolBar, BorderLayout.NORTH);
        
        noDataLabel = new JLabel("<No Data>", SwingConstants.CENTER);
        
    }

    private void refreshUI() {
//        int selStart = area.getSelectionStart();
//        int selEnd   = area.getSelectionEnd();
//        area.setText(getBasicTelemetry());
//        area.select(selStart, selEnd);
    }

//    private String getBasicTelemetry() {
//        boolean deltas = baseClasses != null;
//        String sClasses = totalClasses == -1 ? "" : (deltas && totalClasses > 0 ? "+" : "") + NumberFormat.getInstance().format(totalClasses);
//        String sInstances = totalInstances == -1 ? "" : (deltas && totalInstances > 0 ? "+" : "") + NumberFormat.getInstance().format(totalInstances);
//        String sBytes = totalBytes == -1 ? "" : (deltas && totalBytes > 0 ? "+" : "") + NumberFormat.getInstance().format(totalBytes);
//        return "<nobr><b>Classes: </b>" + sClasses + "&nbsp;&nbsp;&nbsp;&nbsp;<b>Instances: </b>" + sInstances + "&nbsp;&nbsp;&nbsp;&nbsp;<b>Bytes: </b>" + sBytes + "</nobr>";
//    }

    
    private HTMLTextArea area;
    private JLabel refreshRateLabel;
    private JLabel refreshUnitsLabel;
    private JComboBox refreshCombo;
    private AbstractButton snapshotButton;
    private AbstractButton pauseButton;
    private AbstractButton refreshButton;
    private AbstractButton threaddumpButton;
    private JLabel noDataLabel;

    
    private static class ComboRenderer implements ListCellRenderer {

        private ListCellRenderer renderer;

        ComboRenderer(JComboBox combo) {
            renderer = combo.getRenderer();
            if (renderer instanceof JLabel)
                ((JLabel)renderer).setHorizontalAlignment(JLabel.TRAILING);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            return renderer.getListCellRendererComponent(list, NumberFormat.getInstance().format(value), index, isSelected, cellHasFocus);
        }

    }

}
