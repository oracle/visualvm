/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.ui.panels;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.openide.awt.Mnemonics;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ProjectSelectorPanel extends javax.swing.JPanel implements HelpCtx.Provider {
    private static final String HELP_CTX_KEY = "ProjectSelectorPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    
    private List<Lookup.Provider> pool = new ArrayList<Lookup.Provider>();
    private List<Lookup.Provider> selection = new ArrayList<Lookup.Provider>();
    
    private AbstractListModel selectionModel = new AbstractListModel() {

        @Override
        public int getSize() {
            return selection.size();
        }

        @Override
        public Object getElementAt(int index) {
            return selection.get(index);
        }
    };
    private AbstractListModel poolModel = new AbstractListModel() {

        @Override
        public int getSize() {
            return pool.size();
        }

        @Override
        public Object getElementAt(int index) {
            return pool.get(index);
        }
    };
    
    final private Action selectProjectAction = new AbstractAction("selectProject") {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (projectList.getModel().getSize() == 0) return; // #217389
            Object[] vals = projectList.getSelectedValues();
            Lookup.Provider[] projs = new Lookup.Provider[vals.length];
            System.arraycopy(vals, 0, projs, 0, vals.length);
            selectProjects(projs);
        }
    };
    
    final private Action unselectProjectAction = new AbstractAction("unselectProject") {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectionList.getModel().getSize() == 0) return; // #217389
            Object[] vals = selectionList.getSelectedValues();
            Lookup.Provider[] projs = new Lookup.Provider[vals.length];
            System.arraycopy(vals, 0, projs, 0, vals.length);
            unselectProjects(projs);
        }
    };
    
    /**
     * Creates new form ProjectSelectorPanel
     */
    public ProjectSelectorPanel() {
        initComponents();
        postInit();
        loadPool();
    }
    
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblAvailable = new javax.swing.JLabel();
        scroller = new javax.swing.JScrollPane();
        projectList = new javax.swing.JList();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        lblSelected = new javax.swing.JLabel();
        scroller1 = new javax.swing.JScrollPane();
        selectionList = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();

        lblAvailable.setDisplayedMnemonic('A');
        lblAvailable.setLabelFor(scroller);
        lblAvailable.setText(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.lblAvailable.text")); // NOI18N

        projectList.setModel(poolModel);
        projectList.setVisibleRowCount(13);
        projectList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                projectListMouseClicked(evt);
            }
        });
        scroller.setViewportView(projectList);

        addButton.setAction(selectProjectAction);
        addButton.setText(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.addButton.text")); // NOI18N
        addButton.setToolTipText(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.addButton.toolTipText")); // NOI18N
        addButton.setMargin(new java.awt.Insets(2, 8, 2, 8));

        removeButton.setAction(unselectProjectAction);
        removeButton.setText(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.removeButton.text")); // NOI18N
        removeButton.setToolTipText(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.removeButton.toolTipText")); // NOI18N
        removeButton.setActionCommand(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.removeButton.actionCommand")); // NOI18N
        removeButton.setMargin(new java.awt.Insets(2, 8, 2, 8));

        lblSelected.setDisplayedMnemonic('S');
        lblSelected.setLabelFor(scroller1);
        lblSelected.setText(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.lblSelected.text")); // NOI18N

        selectionList.setModel(selectionModel);
        selectionList.setVisibleRowCount(13);
        selectionList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectionListMouseClicked(evt);
            }
        });
        scroller1.setViewportView(selectionList);

        jLabel1.setForeground(javax.swing.UIManager.getDefaults().getColor("Button.shadow"));
        jLabel1.setText(org.openide.util.NbBundle.getMessage(ProjectSelectorPanel.class, "ProjectSelectorPanel.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(scroller, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                        .addGap(5, 5, 5)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(addButton)
                            .addComponent(removeButton))
                        .addGap(6, 6, 6))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblAvailable)
                        .addGap(125, 125, 125)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblSelected)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(scroller1, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                        .addGap(13, 13, 13))))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAvailable)
                    .addComponent(lblSelected))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(scroller)
                    .addComponent(scroller1))
                .addGap(10, 10, 10)
                .addComponent(jLabel1)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void projectListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_projectListMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() == 2) {
            selectProjectAction.actionPerformed(new ActionEvent(evt.getSource(), ActionEvent.ACTION_PERFORMED, "selectProject"));
        }
    }//GEN-LAST:event_projectListMouseClicked

    private void selectionListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_selectionListMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() == 2) {
            unselectProjectAction.actionPerformed(new ActionEvent(evt.getSource(), ActionEvent.ACTION_PERFORMED, "unselectProject"));
        }
    }//GEN-LAST:event_selectionListMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblAvailable;
    private javax.swing.JLabel lblSelected;
    private javax.swing.JList projectList;
    private javax.swing.JButton removeButton;
    private javax.swing.JScrollPane scroller;
    private javax.swing.JScrollPane scroller1;
    private javax.swing.JList selectionList;
    // End of variables declaration//GEN-END:variables

    private void loadPool() {
        Lookup.Provider[] projects = ProjectUtilities.getOpenedProjects();
        Lookup.Provider curProj = getCurrentProject();
        if (curProj != null && projects != null) {
            Lookup.Provider[] newProjs = new Lookup.Provider[projects.length - 1];
            int cntr = 0;
            for(int i=0;i<projects.length;i++) {
                if (!projects[i].equals(curProj)) {
                    newProjs[cntr++] = projects[i];
                }
            }
            projects = newProjs;
        }
        pool.addAll(Arrays.asList(projects));
    }
    
    private void selectProjects(Lookup.Provider ... project) {
        addProjects(selectionModel, selection, project);
        removeProjects(poolModel, pool, project);
    }
    
    private void unselectProjects(Lookup.Provider ... project) {
        addProjects(poolModel, pool, project);
        removeProjects(selectionModel, selection, project);
    }
    
    private void addProjects(AbstractListModel model, List<Lookup.Provider> data, Lookup.Provider ... project) {
       data.addAll(Arrays.asList(project));
        
        ListDataListener[] listeners = model.getListDataListeners();
        if (listeners.length == 0) return;
        
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED,
                data.size() - project.length, data.size());
        for(ListDataListener l : model.getListDataListeners()) l.intervalAdded(e);
    }
    
    private void removeProjects(AbstractListModel model, List<Lookup.Provider> data, Lookup.Provider ... project) {
        data.removeAll(Arrays.asList(project));
        
        ListDataListener[] listeners = model.getListDataListeners();
        if (listeners.length == 0) return;
        
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED,
                data.size(), data.size() + project.length);        
        for(ListDataListener l : listeners) l.intervalRemoved(e);
    }
    
    public void setSelection(List<Lookup.Provider> selection) {
        this.selection.clear();
        this.selection.addAll(selection);

        int oldI = this.pool.size();
        this.pool.removeAll(selection);
        
        for(ListDataListener l : selectionModel.getListDataListeners()) {
            l.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, this.selection.size()));
        }
        
        for(ListDataListener l : poolModel.getListDataListeners()) {
            l.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, oldI));
            l.contentsChanged(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, this.pool.size()));
        }
    }
    
    public List<Lookup.Provider> getSelection() {
        return new ArrayList<Lookup.Provider>(selection);
    }
    
    protected Lookup.Provider getCurrentProject() {
        return null;
    }
    
    private void postInit() {
        final ListCellRenderer r = projectList.getCellRenderer();
        
        ListCellRenderer r1 = new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    ((JLabel)c).setText(ProjectUtilities.getDisplayName((Lookup.Provider)value));
                    ((JLabel)c).setIcon(ProjectUtilities.getIcon((Lookup.Provider)value));
                }
                return c;
            }
        };
        
        projectList.setCellRenderer(r1);
        selectionList.setCellRenderer(r1);
        
        projectList.getActionMap().put("selectProject", selectProjectAction);
        selectionList.getActionMap().put("unselectProject", unselectProjectAction);
        projectList.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "selectProject");
        selectionList.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "unselectProject");
    }
}
