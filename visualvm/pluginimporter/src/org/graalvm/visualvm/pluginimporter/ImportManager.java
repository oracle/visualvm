/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.pluginimporter;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.autoupdate.ui.api.PluginManager;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.awt.Notification;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jirka Rechtacek
 */
public class ImportManager extends Panel {

    private final File srcCluster;
    private final File dest;
    private final PluginImporter importer;
    private static ImportManager INSTANCE = null;
    private List<Boolean> checkedToInstall = Collections.emptyList();
    private List<Boolean> checkedToImport = Collections.emptyList();
    private List<UpdateElement> toInstall = Collections.emptyList();
    private List<UpdateElement> toImport = Collections.emptyList();
    private Notification currentNotification = null;
    private JButton bImport;
    private JButton bNo;

    /**
     * Creates new form ImportManager
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public ImportManager(File src, File dest, PluginImporter importer) {
        this.srcCluster = src;
        this.dest = dest;
        this.importer = importer;
        initialize();
        INSTANCE = this;
    }

    public PluginImporter getPluginImporter() {
        return importer;
    }

    private void initialize() {
        toInstall = new ArrayList<>(importer.getPluginsAvailableToInstall());
        Collections.sort(toInstall, new Comparator<UpdateElement>() {
            @Override
            public int compare(UpdateElement o1, UpdateElement o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        checkedToInstall = new ArrayList<Boolean>(Collections.nCopies(importer.getPluginsAvailableToInstall().size(), Boolean.TRUE));

        toImport = new ArrayList<>(importer.getPluginsToImport());
        Collections.sort(toImport, new Comparator<UpdateElement>() {
            @Override
            public int compare(UpdateElement o1, UpdateElement o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        checkedToImport = new ArrayList<>(Collections.nCopies(toImport.size(), Boolean.FALSE));

        initComponents();

        tpBroken.setEnabled(!importer.getBrokenPlugins().isEmpty());
        lBroken.setEnabled(!importer.getBrokenPlugins().isEmpty());
        if (!importer.getBrokenPlugins().isEmpty()) {
            tpBroken.setText(importer.getBrokenPlugins().toString());
        }
        refreshUI();
    }

    public static ImportManager getInstance() {
        return INSTANCE;
    }

    public void notifyAvailable() {
        remindLater();
        String msg = NbBundle.getMessage(ImportManager.class,
                "ImportNotifier_PluginAvailableForImport", // NOI18N
                toImport.size() + toInstall.size());
        String details = NbBundle.getMessage(ImportManager.class,
                "ImportNotifier_PluginAvailableForImport_Details", // NOI18N
                getClusterName());
        MyAction a = new MyAction();
        synchronized (this) {
            if (null != currentNotification) {
                currentNotification.clear();
            }
            Notification notification = NotificationDisplayer.getDefault().notify(msg,
                    ImageUtilities.loadImageIcon("org/graalvm/visualvm/pluginimporter/import.png", false), //NOI18N
                    details,
                    a);
            a.notification = notification;
            currentNotification = notification;
        }
    }

    private class MyAction extends AbstractAction {

        final JButton bRemindLaterButton = new JButton();
        final JButton bImportButton = new JButton();
        final JButton bNoButton = new JButton();
        private Notification notification;

        public MyAction() {
            Mnemonics.setLocalizedText(bRemindLaterButton, NbBundle.getMessage(ImportManager.class, "ImportNotifier_bRemindLater"));
            Mnemonics.setLocalizedText(bImportButton, NbBundle.getMessage(ImportManager.class, "ImportNotifier_bImport"));
            Mnemonics.setLocalizedText(bNoButton, NbBundle.getMessage(ImportManager.class, "ImportNotifier_bNo"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ImportManager ui = ImportManager.getInstance();
            ui.getPluginImporter().reinspect();
            ui.initialize();

            ui.attachButtons(bImportButton, bNoButton);
            DialogDescriptor dd = new DialogDescriptor(
                    ui,
                    NbBundle.getMessage(Installer.class, "Installer_DialogTitle"),
                    true,
                    new Object[]{bImportButton, bNoButton, bRemindLaterButton},
                    NotifyDescriptor.OK_OPTION,
                    DialogDescriptor.BOTTOM_ALIGN,
                    null,
                    null);
            dd.setClosingOptions(new Object[]{bImportButton, bNoButton, bRemindLaterButton});
            DialogDisplayer.getDefault().createDialog(dd).setVisible(true);
            if (bImportButton.equals(dd.getValue()) || bNoButton.equals(dd.getValue())) {
                ui.dontRemind();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (null != notification) {
                            notification.clear();
                        }
                    }
                });
            } else if (bRemindLaterButton.equals(dd.getValue())) {
                ui.remindLater();
            }
        }
    }

    public void attachButtons(JButton bImport, JButton bNo) {
        this.bImport = bImport;
        this.bNo = bNo;
        bImport.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                if (source instanceof JButton) {
                    RequestProcessor.getDefault().post(() -> doImport());
                }
            }
        });
        refreshUI();
    }

    public void remindLater() {
        Preferences p = NbPreferences.forModule(Installer.class);
        p.put(Installer.KEY_IMPORT_FROM, srcCluster.toString());
    }

    public void dontRemind() {
        Preferences p = NbPreferences.forModule(Installer.class);
        try {
            p.clear();
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private String getClusterName() {
        String defaultUserdirRoot = System.getProperty ("netbeans.default_userdir_root"); // NOI18N

        if (defaultUserdirRoot != null) {
            if (srcCluster.getAbsolutePath().startsWith(defaultUserdirRoot)) {
                return "VisualVM "+srcCluster.getName();
            }
        }
        return srcCluster.toString();
    }

    private boolean doImport() {
        boolean res = true;
        if (checkedToImport.indexOf(Boolean.TRUE) != -1) {
            final Collection<UpdateElement> reallyToImport = new HashSet<UpdateElement>();
            for (UpdateElement el : toImport) {
                if (checkedToImport.get(toImport.indexOf(el))) {
                    reallyToImport.add(el);
                }
            }
            if (reallyToImport.size() > 0) {
                final ProgressHandle handle = ProgressHandleFactory.createHandle(NbBundle.getMessage(ImportManager.class, ("ImportManager.Progress.Name")));
                final JComponent progressComp = ProgressHandleFactory.createProgressComponent(handle);
                final JLabel detailLabel = new JLabel(NbBundle.getMessage(ImportManager.class, "ImportManager.Progress.Label"));
                detailLabel.setHorizontalAlignment(SwingConstants.LEFT);
                setProgressComponent(detailLabel, progressComp);

                try {
                    importer.importPlugins(reallyToImport, srcCluster, dest, handle);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    toImport.clear();
                    checkedToImport.clear();
                }

                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            detailLabel.setVisible(false);
                            progressComp.setVisible(false);
                            tToImport.setModel(getModel(toImport, checkedToImport));
                            refreshUI();
                        }
                    });
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        try {
            dontRemind();
            if (checkedToInstall.indexOf(Boolean.TRUE) != -1) {
                final OperationContainer<InstallSupport> oc = OperationContainer.createForInstall();
                for (UpdateElement el : toInstall) {
                    if (checkedToInstall.get(toInstall.indexOf(el))) {
                        OperationContainer.OperationInfo<InstallSupport> info = oc.add(el);
                        if (info != null) {
                            for (UpdateElement required : info.getRequiredElements()) {
                                if (!required.getUpdateUnit().isPending()) {
                                    oc.add(required);
                                }
                            }
                        }
                    }
                }
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            PluginManager.openInstallWizard(oc, true);
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    toInstall.clear();
                                    checkedToInstall.clear();
                                }
                            });
                        }
                    });
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } finally {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        tToInstall.setModel(getModel(toInstall, checkedToInstall));
                        refreshUI();
                    }
                });
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
        return res;
    }

    private void setProgressComponent(final JLabel detail, final JComponent progressComponent) {
        if (SwingUtilities.isEventDispatchThread()) {
            setProgressComponentInAwt(detail, progressComponent);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setProgressComponentInAwt(detail, progressComponent);
                }
            });
        }
    }

    private void setProgressComponentInAwt(JLabel detail, JComponent progressComponent) {
        assert pProgress != null;
        assert SwingUtilities.isEventDispatchThread() : "Must be called in EQ.";

        progressComponent.setMinimumSize(progressComponent.getPreferredSize());

        pProgress.setVisible(true);

        java.awt.GridBagConstraints gridBagConstraints;

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        pProgress.add(progressComponent, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        pProgress.add(detail, gridBagConstraints);

        validate();
    }

    private TableModel getModel(final List<UpdateElement> plugins, final List<Boolean> checked) {
        return new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return plugins.size();
            }

            @Override
            public int getColumnCount() {
                return 2;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return checked.get(rowIndex);
                    case 1:
                        return plugins.get(rowIndex).getDisplayName();
                }
                return null;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return Boolean.class;
                    case 1:
                        return String.class;
                }
                return null;
            }

            @Override
            public String getColumnName(int column) {
                switch (column) {
                    case 0:
                        return NbBundle.getMessage(ImportManager.class, "ImportNotifier_Install");
                    case 1:
                        return NbBundle.getMessage(ImportManager.class, "ImportNotifier_Plugin");
                }
                return null;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        return true;
                    case 1:
                        return false;
                }
                return false;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                switch (columnIndex) {
                    case 0:
                        checked.set(rowIndex, ((Boolean) aValue));
                        refreshUI();
                        break;
                    case 1:
                        assert false : "Name is not editable.";
                        break;
                }
            }

        };
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane3 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        lToInstall = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tToInstall = new javax.swing.JTable();
        lToImport = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tToImport = new javax.swing.JTable();
        lBroken = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tpBroken = new javax.swing.JTextPane();
        lDesc = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        pProgress = new javax.swing.JPanel();

        jScrollPane3.setViewportView(jTextPane1);

        lToInstall.setLabelFor(lToInstall);
        org.openide.awt.Mnemonics.setLocalizedText(lToInstall, org.openide.util.NbBundle.getMessage(ImportManager.class, "ImportManager.lToInstall.text")); // NOI18N

        tToInstall.setModel(getModel (toInstall, checkedToInstall));
        tToInstall.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tToInstall.setOpaque(false);
        jScrollPane1.setViewportView(tToInstall);

        lToImport.setLabelFor(tToImport);
        org.openide.awt.Mnemonics.setLocalizedText(lToImport, org.openide.util.NbBundle.getMessage(ImportManager.class, "ImportManager.lToImport.text")); // NOI18N

        tToImport.setModel(getModel (toImport, checkedToImport));
        tToImport.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tToImport.setOpaque(false);
        jScrollPane2.setViewportView(tToImport);

        lBroken.setLabelFor(tpBroken);
        org.openide.awt.Mnemonics.setLocalizedText(lBroken, org.openide.util.NbBundle.getMessage(ImportManager.class, "ImportManager.lBroken.text")); // NOI18N

        tpBroken.setEditable(false);
        tpBroken.setEnabled(false);
        tpBroken.setOpaque(false);
        jScrollPane4.setViewportView(tpBroken);

        lDesc.setFont(lDesc.getFont().deriveFont(lDesc.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(lDesc, org.openide.util.NbBundle.getMessage(ImportManager.class, "ImportManager.lDesc.text", new Object[] {getClusterName()})); // NOI18N

        pProgress.setLayout(new java.awt.GridBagLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pProgress, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane4)
                    .addComponent(lToImport, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lToInstall, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lDesc, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lBroken, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lDesc)
                .addGap(7, 7, 7)
                .addComponent(lToInstall)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 123, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lToImport)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lBroken)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JLabel lBroken;
    private javax.swing.JLabel lDesc;
    private javax.swing.JLabel lToImport;
    private javax.swing.JLabel lToInstall;
    private javax.swing.JPanel pProgress;
    private javax.swing.JTable tToImport;
    private javax.swing.JTable tToInstall;
    private javax.swing.JTextPane tpBroken;
    // End of variables declaration//GEN-END:variables

    private void refreshUI() {
        lToImport.setEnabled(toImport.size() > 0);
        tToImport.setEnabled(toImport.size() > 0);

        lToInstall.setEnabled(toInstall.size() > 0);
        tToInstall.setEnabled(toInstall.size() > 0);

        TableColumn activeColumn = tToImport.getColumnModel().getColumn(0);
        activeColumn.setMaxWidth(tToImport.getTableHeader().getHeaderRect(0).width);
        activeColumn = tToInstall.getColumnModel().getColumn(0);
        activeColumn.setMaxWidth(tToInstall.getTableHeader().getHeaderRect(0).width);

        if (bImport != null) {
            bImport.setEnabled(checkedToInstall.indexOf(Boolean.TRUE) != -1 || checkedToImport.indexOf(Boolean.TRUE) != -1);
        }
    }

}
