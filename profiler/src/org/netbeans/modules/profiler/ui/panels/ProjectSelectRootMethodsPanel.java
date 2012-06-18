/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.profiler.ui.panels;

import java.awt.*;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.api.ProgressDisplayer;
import org.netbeans.modules.profiler.selector.ui.RootSelectorTree;
import org.openide.DialogDescriptor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.swing.*;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.java.ProfilerTypeUtils;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourcePackageInfo;
import org.netbeans.modules.profiler.api.project.ProjectStorage;
import org.netbeans.modules.profiler.selector.api.SelectionTreeBuilderFactory;
import org.netbeans.modules.profiler.selector.api.SelectionTreeBuilderType;
import org.netbeans.modules.profiler.selector.spi.SelectionTreeBuilder;
import org.netbeans.modules.profiler.selector.ui.TreePathSearch;
import org.netbeans.modules.profiler.ui.ProfilerProgressDisplayer;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.*;
import org.openide.util.Lookup.Provider;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "SelectRootMethodsPanel_NoSelectionProviders_MSG=No selection view providers available. Falling back to manual selector.",
    "SelectRootMethodsPanel_ShowAllProjectsLabel=&Show All Projects",
    "SelectRootMethodsPanel_SelectViewLabel=Select &View: ",
    "SelectRootMethodsPanel_AdvancedButtonText=&Advanced..."
})
final public class ProjectSelectRootMethodsPanel extends JPanel implements HelpCtx.Provider {
    final private static Logger LOG = Logger.getLogger(ProjectSelectRootMethodsPanel.class.getName());
    
    private static ProjectSelectRootMethodsPanel instance = null;
    // -----
    protected static final Dimension PREFERRED_TOPTREE_DIMENSION = new Dimension(500, 250);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private HTMLTextArea hintArea;
    private JButton okButton;
    private JComboBox treeBuilderList;
    private Lookup.Provider currentProject;
    private List<Lookup.Provider> additionalProjects = new ArrayList<Provider>();
    
    private RequestProcessor rp = new RequestProcessor("SRM-UI Processor", 1); // NOI18N
    private RootSelectorTree pkgTreeView;
//    private ProjectSelectorPanel projectListPanel;
    private JButton additionalProjectsSelector;
    private volatile boolean changingBuilderList = false;

    private static final String HELP_CTX_KEY = "ProjectSelectRootMethodsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public static synchronized ProjectSelectRootMethodsPanel getDefault() {
        if (instance == null) {
            Runnable initializer = new Runnable() {
                public void run() { instance = new ProjectSelectRootMethodsPanel(); }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                initializer.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(initializer);
                } catch (Exception e) {
                    initializer.run();
                }
            }
        }

        return instance;
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    /** Creates a new instance of ProjectSelectRootMethodsPanel */
    private ProjectSelectRootMethodsPanel() {
        initComponents(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public static boolean canBeShown(Lookup ctx) {
        return RootSelectorTree.canBeShown(ctx);
    }

    /**
     * This method handles selecting root methods for a single profiling session
     * The method also displays the UI for selecting root methods
     * @param project The project to select root methods for or <b>null</b> for global mode
     * @param currentSelection The current root method selection (valid for the profiling session)
     * @return Returns the array of newly selected root methods or <b>null</b> to signal that no root methods were selected
     */
    public ClientUtils.SourceCodeSelection[] getRootMethods(final Lookup.Provider project,
            final ClientUtils.SourceCodeSelection[] currentSelection) {
        if (project == null)
            return RootMethodsPanel.getSelectedRootMethods(currentSelection, project);
        
//        projectListPanel.setProject(project);

        currentProject = project;
        
        unpersist();
        
        pkgTreeView.reset();

        PropertyChangeListener pcl = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                refreshBuilderList();
            }
        };

        try {
            pkgTreeView.addPropertyChangeListener(RootSelectorTree.SELECTION_TREE_VIEW_LIST_PROPERTY, pcl);

            updateSelector(new Runnable() {

                @Override
                public void run() {
                    pkgTreeView.setSelection(currentSelection, getContext());
                }
            });

            if (pkgTreeView.getBuilderTypes().isEmpty()) {
                LOG.fine(Bundle.SelectRootMethodsPanel_NoSelectionProviders_MSG());
                return RootMethodsPanel.getSelectedRootMethods(currentSelection, project);
            }

            final DialogDescriptor dd = new DialogDescriptor(this,
                    Bundle.SelectRootMethodsPanel_Title(),
                    true, new Object[]{okButton, DialogDescriptor.CANCEL_OPTION},
                    okButton, DialogDescriptor.BOTTOM_ALIGN, null, null);

            Object[] additionalOptions = getAdditionalOptions();

            if ((additionalOptions != null) && (additionalOptions.length > 0)) {
                dd.setAdditionalOptions(additionalOptions);
            }

            final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
            
            Cancellable c = new Cancellable() {
                @Override
                public boolean cancel() {
                    dd.setValue(DialogDescriptor.CANCEL_OPTION);
                    d.setVisible(false);
                    pkgTreeView.setCancelHandler(null);
                    return true;
                }
            };
            
            pkgTreeView.setCancelHandler(c);
            
            d.pack(); // To properly layout HTML hint area
            d.setVisible(true);

            //    ClientUtils.SourceCodeSelection[] rootMethods = this.currentSelectionSet.toArray(new ClientUtils.SourceCodeSelection[this.currentSelectionSet.size()]);

            ClientUtils.SourceCodeSelection[] selection = pkgTreeView.getSelection();
            
            if (dd.getValue().equals(okButton)) {
                persist();
                return selection;
            }
            
            this.currentProject = null;
            
            return null;
        } finally {
            pkgTreeView.removePropertyChangeListener(RootSelectorTree.SELECTION_TREE_VIEW_LIST_PROPERTY, pcl);
        }
    }

    @NbBundle.Messages({
        "TIT_EditProjects=Edit Projects",
        "LBL_EditProjects=&Edit Projects..."
    })
    protected void initComponents(final Container container) {
        GridBagConstraints gridBagConstraints;

        additionalProjectsSelector = new JButton(Bundle.LBL_EditProjects());
        Mnemonics.setLocalizedText(additionalProjectsSelector, Bundle.LBL_EditProjects());
        
        okButton = new JButton(Bundle.SelectRootMethodsPanel_OkButtonText());

        ProgressDisplayer pd = ProfilerProgressDisplayer.getDefault();
        
        pkgTreeView = new RootSelectorTree(pd, new TreePathSearch.ClassIndex() {

            @Override
            public List<SourceClassInfo> getClasses(String pattern, Lookup context) {
                Lookup.Provider project = context.lookup(Lookup.Provider.class);
                
                if (project != null) {
                    List<SourceClassInfo> srcClzs = new ArrayList<SourceClassInfo>(ProfilerTypeUtils.findClasses(pattern, EnumSet.of(SourcePackageInfo.Scope.SOURCE), project));
                    List<SourceClassInfo> libClzs = new ArrayList<SourceClassInfo>(ProfilerTypeUtils.findClasses(pattern, EnumSet.of(SourcePackageInfo.Scope.DEPENDENCIES), project));

                    Collections.sort(srcClzs, SourceClassInfo.COMPARATOR);
                    Collections.sort(libClzs, SourceClassInfo.COMPARATOR);

                    List<SourceClassInfo> scis = new ArrayList<SourceClassInfo>(srcClzs);
                    scis.addAll(libClzs);

                    return scis;
                }
                
                return Collections.EMPTY_LIST;
            }
        });
        
        additionalProjectsSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProjectSelectorPanel psp = new ProjectSelectorPanel() {

                    @Override
                    protected Provider getCurrentProject() {
                        return currentProject;
                    }
                };
                
                psp.setSelection(additionalProjects);
                
                DialogDescriptor dd = new DialogDescriptor(psp, Bundle.TIT_EditProjects());
                Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                d.setVisible(true);
                if (dd.getValue() == DialogDescriptor.OK_OPTION) {
                    additionalProjects = psp.getSelection();
                    updateSelectorProjects();
                }
            }
        });
        
//        advancedLogicalPackageTree.setNodeFilter(getNodeFilter());

//        projectListPanel = new ProjectSelectorPanel(currentProject) {
//            @Override
//            protected void projectSelected(Provider project) {
//                currentProject = project;
//                updateSelectorProjects();
//            }
//        };
        
        treeBuilderList = new JComboBox();
        treeBuilderList.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (changingBuilderList) {
                    return;
                }

                if (e.getStateChange() == ItemEvent.SELECTED) {
                    rp.post(new Runnable() {

                        @Override
                        public void run() {
                            pkgTreeView.setBuilderType((SelectionTreeBuilderType) e.getItem());
                        }
                    });
                }
            }
        });

        container.setLayout(new GridBagLayout());

        hintArea = new HTMLTextArea() {

            @Override
            public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                return new Dimension(1, super.getPreferredSize().height);
            }
        };

        pkgTreeView.setRowHeight(UIUtils.getDefaultRowHeight() + 2);

        pkgTreeView.setPreferredSize(PREFERRED_TOPTREE_DIMENSION);
                
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(3, 10, 0, 10);
        container.add(pkgTreeView, gridBagConstraints);

        JPanel comboPanel = new JPanel();
        comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.X_AXIS));
        JLabel label = new JLabel();
        Mnemonics.setLocalizedText(label, Bundle.SelectRootMethodsPanel_SelectViewLabel());
        label.setLabelFor(treeBuilderList);
        
        comboPanel.add(label);
        comboPanel.add(treeBuilderList);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
        gridBagConstraints.insets = new Insets(5, 10, 5, 10);
        container.add(comboPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0;
        gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new Insets(5, 10, 5, 10);
        container.add(additionalProjectsSelector, gridBagConstraints);
        
        // hintArea
        String hintString = getHintString();

        if ((hintString != null) && (hintString.length() > 0)) {
            Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
            Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                    panelBackground.getBlue() - 10);
            hintArea.setText(hintString);
            hintArea.setEnabled(false);
            hintArea.setDisabledTextColor(Color.darkGray);
            hintArea.setBackground(hintBackground);
            hintArea.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.insets = new Insets(5, 10, 5, 10);
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            container.add(hintArea, gridBagConstraints);
        }
    }

    private Object[] getAdditionalOptions() {
        JButton button = new JButton() {
            @Override
            protected void fireActionPerformed(ActionEvent e) {
                RequestProcessor.getDefault().post(new Runnable() {
                    @Override
                    public void run() {
                        final ClientUtils.SourceCodeSelection[] methods =
                                RootMethodsPanel.getSelectedRootMethods(
                                pkgTreeView.getSelection(), currentProject);
                        if (methods != null) updateSelector(new Runnable() {
                            @Override
                            public void run() {
                                pkgTreeView.setSelection(methods, getContext());
                            }
                        });
                    }
                });
            }
        };
        Mnemonics.setLocalizedText(button,
                Bundle.SelectRootMethodsPanel_AdvancedButtonText());
        return new Object[] { button };
    }

    private String getHintString() {
        return null;
    }

    private void refreshBuilderList() {
        List<SelectionTreeBuilderType> builderTypes = pkgTreeView.getBuilderTypes();
        if (builderTypes == null || builderTypes.isEmpty()) return;
        
        try {
            changingBuilderList = true;

            treeBuilderList.setModel(new DefaultComboBoxModel(builderTypes.toArray(new SelectionTreeBuilderType[builderTypes.size()])));

            treeBuilderList.setSelectedIndex(0);
            pkgTreeView.setBuilderType((SelectionTreeBuilderType)treeBuilderList.getItemAt(0));
        } finally {
            changingBuilderList = false;
        }
    }

    private void updateSelector(Runnable updater) {
        final ProgressHandle ph = ProgressHandleFactory.createHandle(Bundle.SelectRootMethodsPanel_ParsingProjectStructureMessage());
        CommonUtils.runInEventDispatchThreadAndWait(new Runnable() {
            @Override
            public void run() {
                ph.setInitialDelay(500);
                ph.start();
            }
        });

        try {
            treeBuilderList.setEnabled(false);
            pkgTreeView.setEnabled(false);
            okButton.setEnabled(false);
            updater.run();
        } finally {
            ph.finish();
            okButton.setEnabled(true);
            pkgTreeView.setEnabled(true);
            treeBuilderList.setEnabled(true);
        }
    }

    private void updateSelectorProjects() {
        updateSelector(new Runnable() {

            @Override
            public void run() {
                pkgTreeView.setContext(getContext());
            }
        });
    }

    private Lookup getContext() {
        List<SelectionTreeBuilder> builders = new ArrayList<SelectionTreeBuilder>();
        
        if (currentProject != null) {
            builders.addAll(SelectionTreeBuilderFactory.buildersFor(currentProject));
        }

        for(Lookup.Provider p : additionalProjects) {
            builders.addAll(SelectionTreeBuilderFactory.buildersFor(p));
        }

        return Lookups.fixed((Object[]) builders.toArray(new SelectionTreeBuilder[builders.size()]));
    }
    
    private static final String PROP_PROJECTLIST = "projectList"; // NOI18N
    private static final String PROPS_FILE_NAME = "root_selector.properties"; // NOI18N
    
    private void persist() {
        Properties props = loadProjectProperties();
        if (props != null) {
            StringBuilder sb = new StringBuilder();
            for (Lookup.Provider p : additionalProjects) {
                sb.append(ProjectUtilities.getProjectDirectory(p).getPath()).append(File.pathSeparator);
            }
            props.setProperty(PROP_PROJECTLIST, sb.toString());
            
            saveProjectProperties(props);
            additionalProjects.clear(); // don't want any leaks here
        }
    }
    
    private void unpersist() {
        Properties props = loadProjectProperties();
        if (props != null) {
            additionalProjects.clear();
            String data = props.getProperty(PROP_PROJECTLIST, ""); // NOI18N
            StringTokenizer st = new StringTokenizer(data, File.pathSeparator);
                
            while (st.hasMoreTokens()) {
                String dir = st.nextToken();
                FileObject pDir = FileUtil.toFileObject(new File(dir));
                Lookup.Provider p = ProjectUtilities.getProject(pDir);
                additionalProjects.add(p);
            }
        }
    }
    
    private Properties loadProjectProperties() {
        InputStream is = null;
        try {
            Properties p = new Properties();
            is = getProjectPropertiesFile().getInputStream();
            p.load(is);
            return p;
        } catch (IOException e) {
            
        } finally {
            try {
                is.close();
            } catch (Exception e) {}
        }
        return null;
    }

    private void saveProjectProperties(Properties props) {
        OutputStream os = null;
        try {
            os = getProjectPropertiesFile().getOutputStream();
            props.store(os, ""); // NOI18N
        } catch (IOException e) {
            
        } finally {
            try {
                os.close();
            } catch (Exception e) {}
        }
    }
    
    private FileObject getProjectPropertiesFile() throws IOException {
        FileObject propsFolder = ProjectStorage.getSettingsFolder(currentProject, true);
        FileObject propsFile = propsFolder.getFileObject(PROPS_FILE_NAME);

        if (propsFile == null) {
            propsFile = propsFolder.createData(PROPS_FILE_NAME);
        }

        return propsFile;
    }
}
