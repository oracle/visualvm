/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.FilteringToolbar;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.utils.formatting.DefaultMethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.Formattable;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.java.ExternalPackages;
import org.netbeans.modules.profiler.api.java.ProfilerTypeUtils;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.netbeans.modules.profiler.api.java.SourcePackageInfo;
import org.netbeans.modules.profiler.v2.ProfilerSession;
import org.netbeans.modules.profiler.v2.SessionStorage;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ClassMethodSelector_selectClass=Select Class",
    "ClassMethodSelector_selectMethod=Select Method",
    "ClassMethodSelector_btnOk=OK",
    "ClassMethodSelector_capFromProject=From Project",
    "ClassMethodSelector_capFromJarFolder=From JAR/Folder",
    "ClassMethodSelector_capFiles=Files:",
    "ClassMethodSelector_jarsFoldersFilterDescr=.jar files/.class folders",
    "ClassMethodSelector_selectJarOrFolder=Select JAR File or Class Folder",
    "ClassMethodSelector_addFileOrFolder=Add file or folder",
    "ClassMethodSelector_removeSelectedItem=Remove selected item",
    "ClassMethodSelector_capProjects=Projects:",
    "ClassMethodSelector_capPackages=Packages:",
    "ClassMethodSelector_showProjectPackages=Show project packages",
    "ClassMethodSelector_showDependenciesPackages=Show dependencies packages",
    "ClassMethodSelector_capClasses=Classes:",
    "ClassMethodSelector_showInnerClasses=Show inner classes",
    "ClassMethodSelector_showAnonymousClasses=Show anonymous classes",
    "ClassMethodSelector_capMethods=Methods:",
    "ClassMethodSelector_showInheritedMethods=Show inherited methods",
    "ClassMethodSelector_showNonPublicMethods=Show non-public methods",
    "ClassMethodSelector_showStaticMethods=Show static methods",
    "ClassMethodSelector_lblComputing=Computing...",
    "ClassMethodSelector_lblNoItems=No items",
    "ClassMethodSelector_lblNoSelection=No selection",
    "ClassMethodSelector_lblFilterItems=Filter items"
})
public final class ClassMethodSelector {
    
    private static final WeakProcessor PROCESSOR = new WeakProcessor("Profiler ClassMethodSelector Processor"); // NOI18N
    private static final MethodNameFormatter METHOD_FORMATTER = new DefaultMethodNameFormatter(DefaultMethodNameFormatter.VERBOSITY_METHOD);
    
    public static List<SourceClassInfo> selectClasses(ProfilerSession session) {
        // TODO: wait for finished scan
        
        UI ui = UI.forSession(session, false);

        HelpCtx helpCtx = new HelpCtx("SelectClassDialog.HelpCtx"); // NOI18N
        DialogDescriptor dd = new DialogDescriptor(ui, Bundle.ClassMethodSelector_selectClass(), true,
                                                   new Object[] { ui.getOKButton(), DialogDescriptor.CANCEL_OPTION },
                                                   ui.getOKButton(), DialogDescriptor.BOTTOM_ALIGN, helpCtx, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);
        
        return dd.getValue() == ui.getOKButton() ? ui.selectedClasses() : Collections.EMPTY_LIST;
    }
    
    public static List<SourceMethodInfo> selectMethods(ProfilerSession session) {
        // TODO: wait for finished scan
        
        UI ui = UI.forSession(session, true);

        HelpCtx helpCtx = new HelpCtx("SelectMethodDialog.HelpCtx"); // NOI18N
        DialogDescriptor dd = new DialogDescriptor(ui, Bundle.ClassMethodSelector_selectMethod(), true,
                                                   new Object[] { ui.getOKButton(), DialogDescriptor.CANCEL_OPTION },
                                                   ui.getOKButton(), DialogDescriptor.BOTTOM_ALIGN, helpCtx, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);
        
        return dd.getValue() == ui.getOKButton() ? ui.selectedMethods() : Collections.EMPTY_LIST;
    }
    
    
    private ClassMethodSelector() {}
    
    
    // --- UI ------------------------------------------------------------------
    
    private static final int LIST_WIDTH = 200;
    private static final Preferences PREF = NbPreferences.forModule(ClassMethodSelector.class);
    
    private static class UI extends JPanel {
        
        private final JButton okButton;
        
        private final JPanel p_selectors;
        private final MethodSelector p_methodSelector;
        private final ClassSelector p_classSelector;
        private final PackageSelector p_packageSelector;
        private final ProjectSelector p_projectSelector;
        
        private final JPanel f_selectors;
        private final MethodSelector f_methodSelector;
        private final ClassSelector f_classSelector;
        private final PackageSelector f_packageSelector;
        private final FileSelector f_fileSelector;
        
        private JComponent selected;
        
        
        static UI forSession(ProfilerSession session, boolean method) {
            return new UI(session, method);
        }
        
        JButton getOKButton() {
            return okButton;
        }
        
        List<SourceClassInfo> selectedClasses() {
            if (p_selectors == selected && p_classSelector != null) {
                return p_classSelector.getAllSelected();
            } else if (f_selectors == selected && f_classSelector != null) {
                return f_classSelector.getAllSelected();
            }
            return null;
        }
        
        List<SourceMethodInfo> selectedMethods() {
            if (p_selectors == selected && p_methodSelector != null) {
                return p_methodSelector.getAllSelected();
            } else if (f_selectors == selected && f_methodSelector != null) {
                return f_methodSelector.getAllSelected();
            }
            return null;
        }
        
        
        private UI(final ProfilerSession session, final boolean method) {
            okButton = new JButton(Bundle.ClassMethodSelector_btnOk());
            okButton.setEnabled(false);
            
            // --- From Project ------------------------------------------------
            final boolean[] p_init = new boolean[1];
            if (session.getProject() != null) {
                if (method) {
                    p_methodSelector = new MethodSelector() {
                        void methodSelected() {
                            okButton.setEnabled(getSelected() != null);
                        }
                        void reload() {
                            init(p_classSelector.getSelected());
                        }
                    };
                } else {
                    p_methodSelector = null;
                }

                p_classSelector = new ClassSelector(method) {
                    void classSelected() {
                        if (!method) okButton.setEnabled(getSelected() != null);
                        else p_methodSelector.init(getSelected());
                    }
                    void reload() {
                        init(p_packageSelector.getSelected());
                    }
                };

                p_packageSelector = new PackageSelector(true) {
                    void packageSelected() {
                        p_classSelector.init(getSelected());
                    }
                    void reload() {
                        init(p_projectSelector.getSelected());
                    }
                };

                p_projectSelector = new ProjectSelector() {
                    void projectSelected() {
                        p_packageSelector.init(getSelected());
                    }
                };

                p_selectors = new JPanel(new GridLayout(1, method ? 4 : 3, 10, 10));
                p_selectors.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
                p_selectors.add(p_projectSelector);
                p_selectors.add(p_packageSelector);
                p_selectors.add(p_classSelector);
                if (method) p_selectors.add(p_methodSelector);
            } else {
                p_selectors = null;
                p_methodSelector = null;
                p_classSelector = null;
                p_packageSelector = null;
                p_projectSelector = null;
            }
            
            // --- From File ---------------------------------------------------
            final boolean[] f_init = new boolean[1];
            if (method) {
                f_methodSelector = new MethodSelector() {
                    void methodSelected() {
                        okButton.setEnabled(getSelected() != null);
                    }
                    void reload() {
                        init(f_classSelector.getSelected());
                    }
                };
            } else {
                f_methodSelector = null;
            }
            
            f_classSelector = new ClassSelector(method) {
                void classSelected() {
                    if (!method) okButton.setEnabled(getSelected() != null);
                    else f_methodSelector.init(getSelected());
                }
                void reload() {
                    init(f_packageSelector.getSelected());
                }
            };
            
            f_packageSelector = new PackageSelector(false) {
                void packageSelected() {
                    f_classSelector.init(getSelected());
                }
                void reload() {
                    init(f_fileSelector.getSelected());
                }
            };
            
            f_fileSelector = new FileSelector() {
                void fileSelected() {
                    f_packageSelector.init(getSelected());
                }
                SessionStorage getStorage() {
                    return session.getStorage();
                }
            };
            
            f_selectors = new JPanel(new GridLayout(1, method ? 4 : 3, 10, 10));
            f_selectors.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            f_selectors.add(f_fileSelector);
            f_selectors.add(f_packageSelector);
            f_selectors.add(f_classSelector);
            if (method) f_selectors.add(f_methodSelector);
            
            // --- Views -------------------------------------------------------
            
            JTabbedPane p = new JTabbedPane() {
                protected void fireStateChanged() {
                    super.fireStateChanged();
                    
                    Component sel = getSelectedComponent();
                    if (sel == p_selectors) {
                        // Mark selected
                        selected = p_selectors;
                        // Init projects
                        if (!p_init[0]) {
                            p_projectSelector.init(session.getProject());
                            p_init[0] = true;
                        }
                        // Update OK button
                        if (p_methodSelector != null) p_methodSelector.methodSelected();
                        else p_classSelector.classSelected();
                    } else if (sel == f_selectors) {
                        // Mark selected
                        selected = f_selectors;
                        // Init files
                        if (!f_init[0]) {
                            f_fileSelector.init();
                            f_init[0] = true;
                        }
                        // Update OK button
                        if (f_methodSelector != null) f_methodSelector.methodSelected();
                        else f_classSelector.classSelected();
                    }
                }
            };
            if (UIUtils.isAquaLookAndFeel()) {
                p.setBorder(BorderFactory.createEmptyBorder(0, -11, -13, -10));
            } else {
                Insets i = UIManager.getInsets("TabbedPane.contentBorderInsets"); // NOI18N
                if (i == null) p.setBorder(BorderFactory.createEmptyBorder());
                else p.setBorder(BorderFactory.createEmptyBorder(0, -i.left, -i.bottom, -i.right));
            }
            if (p_selectors != null) p.addTab(Bundle.ClassMethodSelector_capFromProject(), null, p_selectors, null);
            if (f_selectors != null) p.addTab(Bundle.ClassMethodSelector_capFromJarFolder(), null, f_selectors, null);
            
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(p, BorderLayout.CENTER);
        }
        
    }
    
    
    private static abstract class FileSelector extends JPanel {
        
        abstract void fileSelected();
        
        abstract SessionStorage getStorage();
        
        FileObject getSelected() {
            return fileList.getSelectedValue();
        }
        
        void init() {
            isInitialized = true;
            
            fileListModel.clear();
            fileList.setEnabled(!isInitialized);
            
            if (isInitialized) {
                PROCESSOR.post(new Runnable() {
                    public void run() {
                        SessionStorage _storage = getStorage();
                        final Collection<FileObject> files = getFiles(_storage);
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                fileList.setEnabled(true);
                                for (FileObject fo : files) fileListModel.addElement(fo);
                            }
                        });
                    }
                });
            }
        }
        
        private static Collection<FileObject> getFiles(SessionStorage _storage) {
            List<FileObject> files = new ArrayList();
            String _files = _storage.readFlag("CMS.files", ""); // NOI18N

            if (!_files.isEmpty()) for (String file : _files.split("#")) { // NOI18N
                File f = new File(file);
                FileObject fo = f.exists() ? FileUtil.toFileObject(
                                FileUtil.normalizeFile(f)) : null;
                if (fo != null) files.add(fo);
            }
            
            return files;
        }
        
        private void persist() {
            StringBuilder sb = new StringBuilder();
            Enumeration<FileObject> files = fileListModel.elements();
            while (files.hasMoreElements()) {
                FileObject file = files.nextElement();
                sb.append(FileUtil.toFile(file).getAbsolutePath());
                if (files.hasMoreElements()) sb.append("#"); // NOI18N
            }
            if (sb.length() == 0) getStorage().storeFlag("CMS.files", null); // NOI18N
            else getStorage().storeFlag("CMS.files", sb.toString()); // NOI18N
        }
        
        
        private final JList<FileObject> fileList;
        private final DefaultListModel<FileObject> fileListModel;
        private final AbstractButton addFileB;
        private final AbstractButton removeFileB;
        
        private boolean isInitialized;
        
        FileSelector() {
            fileListModel = new DefaultListModel();
            final FilteredListModel<FileObject> filteredFiles = new FilteredListModel<FileObject>(fileListModel) {
                protected boolean matchesFilter(FileObject file, String filter) {
                    return file.getNameExt().contains(filter);
                }
            };
            final HintRenderer hintRenderer = new HintRenderer();
            fileList = new JList(filteredFiles) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = LIST_WIDTH;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getModel().getSize() == 0) {
                        hintRenderer.setup(true, getSize());
                        hintRenderer.paint(g);
                    }
                }
            };
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            filteredFiles.setSelectionModel(fileList.getSelectionModel());
            fileList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer c = (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    FileObject file = (FileObject)value;
                    c.setText(file.getNameExt());
                    c.setIcon(Icons.getIcon(file.isFolder() ? LanguageIcons.LIBRARIES : LanguageIcons.JAR));
                    return c;
                }
            });
            fileList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        fileSelected();
                        removeFileB.setEnabled(fileList.getSelectedValue() != null);
                    }
                }
            });
            
            JLabel projectsLabel = new JLabel(Bundle.ClassMethodSelector_capFiles(), JLabel.LEADING);
            projectsLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JToolBar fileTools = new FilteringToolbar(Bundle.ClassMethodSelector_lblFilterItems()) {
                protected void filterChanged(String filter) {
                    filteredFiles.setFilter(filter);
                }
            };
            String iconMask = GeneralIcons.FOLDER;
            Image baseIcon = Icons.getImage(iconMask);
            Image addBadge = Icons.getImage(GeneralIcons.BADGE_ADD);
            Image addImage = ImageUtilities.mergeImages(baseIcon, addBadge, 0, 0);
            addFileB = new JButton(ImageUtilities.image2Icon(addImage)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    FileChooserBuilder b = new FileChooserBuilder(ClassMethodSelector.class);
                    b.setAcceptAllFileFilterUsed(false);
                    final File file = b.setFileFilter(new FileFilter() {
                        public boolean accept(File f) {
                            if (f.isDirectory()) {
                                return true;
                            }
                            String ext = null;
                            String n = f.getName();
                            int index = n.lastIndexOf("."); // NOI18N
                            if (index > -1) {
                                ext = n.substring(index + 1);
                            }
                            return ext != null && ext.equalsIgnoreCase("jar"); // NOI18N
                        }
                        public String getDescription() {
                            return Bundle.ClassMethodSelector_jarsFoldersFilterDescr();
                        }
                    }).setTitle(Bundle.ClassMethodSelector_selectJarOrFolder()).showOpenDialog();
                    if (file != null) {
                        FileObject f = FileUtil.toFileObject(FileUtil.normalizeFile(file));
                        if (f != null) {
                            fileListModel.addElement(f);
                            persist();
                        }
                    }
                }
            };
            addFileB.setToolTipText(Bundle.ClassMethodSelector_addFileOrFolder());
            Image removeBadge = Icons.getImage(GeneralIcons.BADGE_REMOVE);
            Image removeImage = ImageUtilities.mergeImages(baseIcon, removeBadge, 0, 0);
            removeFileB = new JButton(ImageUtilities.image2Icon(removeImage)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    List<FileObject> files = fileList.getSelectedValuesList();
                    if (!files.isEmpty()) {
                        for (FileObject file : files) fileListModel.removeElement(file);
                        persist();
                    }
                }
            };
            removeFileB.setToolTipText(Bundle.ClassMethodSelector_removeSelectedItem());
            removeFileB.setEnabled(fileList.getSelectedValue() != null);
            fileTools.add(Box.createHorizontalStrut(2));
            fileTools.addSeparator();
            fileTools.add(Box.createHorizontalStrut(2));
            fileTools.add(addFileB);
            fileTools.add(Box.createHorizontalStrut(2));
            fileTools.add(removeFileB);
            
            setOpaque(false);
            setLayout(new BorderLayout());
            add(projectsLabel, BorderLayout.NORTH);
            add(new JScrollPane(fileList), BorderLayout.CENTER);
            add(fileTools, BorderLayout.SOUTH);
        }
        
    }
    
    private static abstract class ProjectSelector extends JPanel {
        
        abstract void projectSelected();
        
        Lookup.Provider getSelected() {
            return projectList.getSelectedValue();
        }
        
        void init(final Lookup.Provider _project) {
            isInitialized = true;
            
            projectListModel.clear();
            projectList.setEnabled(!isInitialized);
            
            if (isInitialized) {
                PROCESSOR.post(new Runnable() {
                    public void run() {
                        final Lookup.Provider[] projects = getProjects();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                projectList.setEnabled(true);
                                for (Lookup.Provider prj : projects) projectListModel.addElement(prj);
                                if (_project != null) projectList.setSelectedValue(_project, true);
                            }
                        });
                    }
                });
            }
        }
        
        private static Lookup.Provider[] getProjects() {
            return ProjectUtilities.getSortedProjects(ProjectUtilities.getOpenedProjects());
        }
        
        
        private final JList<Lookup.Provider> projectList;
        private final DefaultListModel<Lookup.Provider> projectListModel;
        
        private boolean isInitialized;
        
        ProjectSelector() {
            projectListModel = new DefaultListModel();
            final FilteredListModel<Lookup.Provider> filteredProjects = new FilteredListModel<Lookup.Provider>(projectListModel) {
                protected boolean matchesFilter(Lookup.Provider proj, String filter) {
                    return ProjectUtilities.getDisplayName(proj).contains(filter);
                }
            };
            final HintRenderer hintRenderer = new HintRenderer();
            projectList = new JList(filteredProjects) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = LIST_WIDTH;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getModel().getSize() == 0) {
                        hintRenderer.setup(true, getSize());
                        hintRenderer.paint(g);
                    }
                }
            };
            projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            filteredProjects.setSelectionModel(projectList.getSelectionModel());
            projectList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer c = (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    Lookup.Provider val = (Lookup.Provider)value;
                    c.setText(ProjectUtilities.getDisplayName(val));
                    c.setIcon(ProjectUtilities.getIcon(val));
                    return c;
                }
            });
            projectList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) projectSelected();
                }
            });
            
            JLabel projectsLabel = new JLabel(Bundle.ClassMethodSelector_capProjects(), JLabel.LEADING);
            projectsLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JToolBar projectsTools = new FilteringToolbar(Bundle.ClassMethodSelector_lblFilterItems()) {
                protected void filterChanged(String filter) {
                    filteredProjects.setFilter(filter);
                }
            };
            
            setOpaque(false);
            setLayout(new BorderLayout());
            add(projectsLabel, BorderLayout.NORTH);
            add(new JScrollPane(projectList), BorderLayout.CENTER);
            add(projectsTools, BorderLayout.SOUTH);
        }
        
    }
    
    private static abstract class PackageSelector extends JPanel {
        
        abstract void packageSelected();
        
        abstract void reload();
        
        SourcePackageInfo getSelected() {
            return packageList.getSelectedValue();
        }
        
        void init(final Lookup.Provider _project) {
            isInitialized = _project != null;
            
            packageListModel.clear();
            packageList.setEnabled(!isInitialized);
            
            if (isInitialized) {
                final boolean sources = packagesSourcesB.isSelected();
                final boolean dependencies = packagesDependenciesB.isSelected();
                
                PROCESSOR.post(new Runnable() {
                    public void run() {
                        final Collection<SourcePackageInfo> packages = getProjectPackages(_project, sources, dependencies);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                packageList.setEnabled(true);
                                for (SourcePackageInfo pkg : packages)
                                   packageListModel.addElement(pkg); 
                            }
                        });
                    }
                });
            }
        }
        
        void init(final FileObject _file) {
            isInitialized = _file != null;
            
            packageListModel.clear();
            packageList.setEnabled(!isInitialized);
            
            if (isInitialized) {
                PROCESSOR.post(new Runnable() {
                    public void run() {
                        final Collection<SourcePackageInfo> packages = getFilePackages(_file);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                packageList.setEnabled(true);
                                for (SourcePackageInfo pkg : packages)
                                   packageListModel.addElement(pkg); 
                            }
                        });
                    }
                });
            }
        }
        
        private static Collection<SourcePackageInfo> getProjectPackages(Lookup.Provider project, boolean sources, boolean dependencies) {
            Set<SourcePackageInfo> packages = new HashSet();
            if (sources) packages.addAll(ProfilerTypeUtils.getPackages(false, SourcePackageInfo.Scope.SOURCE, project));
            if (dependencies) packages.addAll(ProfilerTypeUtils.getPackages(false, SourcePackageInfo.Scope.DEPENDENCIES, project));

            Set<SourcePackageInfo> sortedPackages = new TreeSet<SourcePackageInfo>(
                new Comparator<SourcePackageInfo>() {
                    public int compare(SourcePackageInfo p1, SourcePackageInfo p2) {
                        return p1.getBinaryName().compareTo(p2.getBinaryName());
                    }
                }
            );
            sortedPackages.addAll(packages);
            return sortedPackages;
        }
        
        private static Collection<SourcePackageInfo> getFilePackages(FileObject file) {
            Set<SourcePackageInfo> packages = new HashSet(ExternalPackages.forPath(file, true));

            Set<SourcePackageInfo> sortedPackages = new TreeSet<SourcePackageInfo>(
                new Comparator<SourcePackageInfo>() {
                    public int compare(SourcePackageInfo p1, SourcePackageInfo p2) {
                        return p1.getBinaryName().compareTo(p2.getBinaryName());
                    }
                }
            );
            sortedPackages.addAll(packages);
            return sortedPackages;
        }
        
        
        private final JList<SourcePackageInfo> packageList;
        private final DefaultListModel<SourcePackageInfo> packageListModel;
        private final AbstractButton packagesSourcesB;
        private final AbstractButton packagesDependenciesB;
        
        private boolean isInitialized;
        
        PackageSelector(boolean fromProject) {
            packageListModel = new DefaultListModel();
            final FilteredListModel<SourcePackageInfo> filteredPackages = new FilteredListModel<SourcePackageInfo>(packageListModel) {
                protected boolean matchesFilter(SourcePackageInfo pkg, String filter) {
                    return pkg.getBinaryName().contains(filter);
                }
            };
            final HintRenderer hintRenderer = new HintRenderer();
            packageList = new JList(filteredPackages) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = LIST_WIDTH;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getModel().getSize() == 0) {
                        if (!isEnabled()) hintRenderer.setup(false, getSize());
                        else if (isInitialized) hintRenderer.setup(true, getSize());
                        else hintRenderer.setup(null, getSize());
                        hintRenderer.paint(g);
                    }
                }
            };
            packageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            filteredPackages.setSelectionModel(packageList.getSelectionModel());
            packageList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer c = (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    SourcePackageInfo val = (SourcePackageInfo)value;
                    c.setText(val.getBinaryName());
                    c.setIcon(Icons.getIcon(LanguageIcons.PACKAGE));
                    return c;
                }
            });
            packageList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) packageSelected();
                }
            });
            
            JLabel packagesLabel = new JLabel(Bundle.ClassMethodSelector_capPackages(), JLabel.LEADING);
            packagesLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JToolBar packagesTools = new FilteringToolbar(Bundle.ClassMethodSelector_lblFilterItems()) {
                protected void filterChanged(String filter) {
                    filteredPackages.setFilter(filter);
                }
            };
            if (fromProject) {
                packagesSourcesB = new JToggleButton(Icons.getIcon(LanguageIcons.CONSTRUCTORS)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        reload();
                        PREF.putBoolean("Profiler.CMS.packagesSourcesB", packagesSourcesB.isSelected()); // NOI18N
                    }
                };
                packagesSourcesB.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                packagesSourcesB.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
                packagesSourcesB.setToolTipText(Bundle.ClassMethodSelector_showProjectPackages());
                packagesSourcesB.setSelected(PREF.getBoolean("Profiler.CMS.packagesSourcesB", true)); // NOI18N
                packagesDependenciesB = new JToggleButton(Icons.getIcon(LanguageIcons.JAR)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        reload();
                        PREF.putBoolean("Profiler.CMS.packagesDependenciesB", packagesDependenciesB.isSelected()); // NOI18N
                    }
                };
                packagesDependenciesB.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                packagesDependenciesB.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
                packagesDependenciesB.setToolTipText(Bundle.ClassMethodSelector_showDependenciesPackages());
                packagesDependenciesB.setSelected(PREF.getBoolean("Profiler.CMS.packagesDependenciesB", false)); // NOI18N
                
                packagesTools.add(Box.createHorizontalStrut(2));
                packagesTools.addSeparator();
                packagesTools.add(Box.createHorizontalStrut(2));
                packagesTools.add(packagesSourcesB);
                packagesTools.add(Box.createHorizontalStrut(2));
                packagesTools.add(packagesDependenciesB);
            } else {
                packagesSourcesB = null;
                packagesDependenciesB = null;
            }
            
            setOpaque(false);
            setLayout(new BorderLayout());
            add(packagesLabel, BorderLayout.NORTH);
            add(new JScrollPane(packageList), BorderLayout.CENTER);
            add(packagesTools, BorderLayout.SOUTH);
        }
        
    }
    
    private static abstract class ClassSelector extends JPanel {
        
        abstract void classSelected();
        
        abstract void reload();
        
        SourceClassInfo getSelected() {
            return classesList.getSelectedValue();
        }
        
        List<SourceClassInfo> getAllSelected() {
            return classesList.getSelectedValuesList();
        }
        
        void init(final SourcePackageInfo _package) {
            isInitialized = _package != null;
            
            classesListModel.clear();
            classesList.setEnabled(!isInitialized);
            
            if (isInitialized) {
                final boolean inner = classesInnerB.isSelected();
                final boolean anonymous = classesAnonymousB.isSelected();
                
                PROCESSOR.post(new Runnable() {
                    public void run() {
                        final Collection<SourceClassInfo> classes = getClasses(_package, inner, anonymous);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                classesList.setEnabled(true);
                                for (SourceClassInfo clss : classes)
                                   classesListModel.addElement(clss); 
                            }
                        });
                    }
                });
            }
        }
        
        private static Collection<SourceClassInfo> getClasses(SourcePackageInfo pkg, boolean inner, boolean anonymous) {
            Collection<SourceClassInfo> classes = pkg.getClasses();

            if (inner) {
                Set<SourceClassInfo> _classes = new HashSet();
                while (!classes.isEmpty()) {
                    SourceClassInfo cls = classes.iterator().next();
                    classes.remove(cls);
                    if (anonymous || !cls.isAnonymous()) _classes.add(cls);
                    classes.addAll(cls.getInnerClases());
                }
                classes = _classes;
            }

            Set<SourceClassInfo> sortedClasses = new TreeSet<SourceClassInfo>(
                new Comparator<SourceClassInfo>() {
                    public int compare(SourceClassInfo c1, SourceClassInfo c2) {
                        return c1.getSimpleName().compareTo(c2.getSimpleName());
                    }
                }
            );

            sortedClasses.addAll(classes);
            return sortedClasses;
        }
        
        
        private final JList<SourceClassInfo> classesList;
        private final DefaultListModel<SourceClassInfo> classesListModel;
        private final AbstractButton classesInnerB;
        private final AbstractButton classesAnonymousB;
        
        private boolean isInitialized;
        
        ClassSelector(boolean singleSelection) {
            classesListModel = new DefaultListModel();
            final FilteredListModel<SourceClassInfo> filteredClasses = new FilteredListModel<SourceClassInfo>(classesListModel) {
                protected boolean matchesFilter(SourceClassInfo cls, String filter) {
                    return cls.getSimpleName().contains(filter);
                }
            };
            final HintRenderer hintRenderer = new HintRenderer();
            classesList = new JList(filteredClasses) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = LIST_WIDTH;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getModel().getSize() == 0) {
                        if (!isEnabled()) hintRenderer.setup(false, getSize());
                        else if (isInitialized) hintRenderer.setup(true, getSize());
                        else hintRenderer.setup(null, getSize());
                        hintRenderer.paint(g);
                    }
                }
            };
            classesList.setSelectionMode(singleSelection ? ListSelectionModel.SINGLE_SELECTION :
                                         ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            filteredClasses.setSelectionModel(classesList.getSelectionModel());
            classesList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer c = (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    SourceClassInfo val = (SourceClassInfo)value;
                    c.setText(val.getSimpleName());
                    c.setIcon(Icons.getIcon(LanguageIcons.CLASS));
                    return c;
                }
            });
            classesList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) classSelected();
                }
            });
            
            JLabel classesLabel = new JLabel(Bundle.ClassMethodSelector_capClasses(), JLabel.LEADING);
            classesLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            classesInnerB = new JToggleButton(Icons.getIcon(LanguageIcons.CLASS)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    reload();
                    classesAnonymousB.setEnabled(isSelected());
                    PREF.putBoolean("Profiler.CMS.classesInnerB", classesInnerB.isSelected()); // NOI18N
                }
            };
            classesInnerB.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            classesInnerB.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
            classesInnerB.setToolTipText(Bundle.ClassMethodSelector_showInnerClasses());
            classesInnerB.setSelected(PREF.getBoolean("Profiler.CMS.classesInnerB", true)); // NOI18N
            classesAnonymousB = new JToggleButton(Icons.getIcon(LanguageIcons.CLASS_ANONYMOUS)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    reload();
                    PREF.putBoolean("Profiler.CMS.classesAnonymousB", classesAnonymousB.isSelected()); // NOI18N
                }
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                    if (!isEnabled()) setSelected(false);
                }
            };
            classesAnonymousB.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            classesAnonymousB.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
            classesAnonymousB.setToolTipText(Bundle.ClassMethodSelector_showAnonymousClasses());
            classesAnonymousB.setSelected(PREF.getBoolean("Profiler.CMS.classesAnonymousB", false)); // NOI18N
            JToolBar classesTools = new FilteringToolbar(Bundle.ClassMethodSelector_lblFilterItems()) {
                protected void filterChanged(String filter) {
                    filteredClasses.setFilter(filter);
                }
            };
            classesTools.add(Box.createHorizontalStrut(2));
            classesTools.addSeparator();
            classesTools.add(Box.createHorizontalStrut(2));
            classesTools.add(classesInnerB);
            classesTools.add(Box.createHorizontalStrut(2));
            classesTools.add(classesAnonymousB);
            
            setOpaque(false);
            setLayout(new BorderLayout());
            add(classesLabel, BorderLayout.NORTH);
            add(new JScrollPane(classesList), BorderLayout.CENTER);
            add(classesTools, BorderLayout.SOUTH);
        }
        
    }
    
    private static abstract class MethodSelector extends JPanel {
        
        abstract void methodSelected();
        
        abstract void reload();
        
        SourceMethodInfo getSelected() {
            return methodsList.getSelectedValue();
        }
        
        List<SourceMethodInfo> getAllSelected() {
            return methodsList.getSelectedValuesList();
        }
        
        void init(final SourceClassInfo _class) {
            isInitialized = _class != null;
            
            methodsListModel.clear();
            methodsList.setEnabled(!isInitialized);

            if (isInitialized) {
                final boolean inherited = methodsInheritedB.isSelected();
                final boolean nonpublic = methodsNonPublicB.isSelected();
                final boolean staticc   = methodsStaticB.isSelected();

                PROCESSOR.post(new Runnable() {
                    public void run() {
                        final Collection<SourceMethodInfo> methods = getMethods(_class, inherited, nonpublic, staticc);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                methodsList.setEnabled(true);
                                for (SourceMethodInfo mthd : methods)
                                   methodsListModel.addElement(mthd); 
                            }
                        });
                    }
                });
            }
        }
        
        private static Collection<SourceMethodInfo> getMethods(SourceClassInfo cls, boolean inherited, boolean nonpublic, boolean staticc) {
            Set<SourceMethodInfo> sortedMethods = new TreeSet<SourceMethodInfo>(
                new Comparator<SourceMethodInfo>() {
                    public int compare(SourceMethodInfo m1, SourceMethodInfo m2) {
                        Formattable f1 = METHOD_FORMATTER.formatMethodName("", m1.getName(), m1.getSignature()); // NOI18N
                        Formattable f2 = METHOD_FORMATTER.formatMethodName("", m2.getName(), m2.getSignature()); // NOI18N
                        return f1.toFormatted().compareTo(f2.toFormatted());
                    }
                }
            );

            Set<SourceMethodInfo> methods = new HashSet(cls.getConstructors());
            methods.addAll(cls.getMethods(inherited));
            for (SourceMethodInfo method : methods) {
                int modifiers = method.getModifiers();
                if ((nonpublic || Modifier.isPublic(modifiers)) &&
                    (staticc || !Modifier.isStatic(modifiers)))
                    sortedMethods.add(method);
            }

            return sortedMethods;
        }
        
        
        private final JList<SourceMethodInfo> methodsList;
        private final DefaultListModel<SourceMethodInfo> methodsListModel;
        private final AbstractButton methodsInheritedB;
        private final AbstractButton methodsNonPublicB;
        private final AbstractButton methodsStaticB;
        
        private boolean isInitialized;
            
        MethodSelector() {
            methodsListModel = new DefaultListModel();
            final FilteredListModel<SourceMethodInfo> filteredMethods = new FilteredListModel<SourceMethodInfo>(methodsListModel) {
                protected boolean matchesFilter(SourceMethodInfo mtd, String filter) {
                    return mtd.getName().contains(filter);
                }
            };
            final HintRenderer hintRenderer = new HintRenderer();
            methodsList = new JList(filteredMethods) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = LIST_WIDTH;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    if (getModel().getSize() == 0) {
                        if (!isEnabled()) hintRenderer.setup(false, getSize());
                        else if (isInitialized) hintRenderer.setup(true, getSize());
                        else hintRenderer.setup(null, getSize());
                        hintRenderer.paint(g);
                    }
                }
            };
            methodsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            filteredMethods.setSelectionModel(methodsList.getSelectionModel());
            methodsList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer c = (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    SourceMethodInfo val = (SourceMethodInfo)value;
                    c.setText(METHOD_FORMATTER.formatMethodName("", val.getName(), val.getSignature()).toFormatted()); // NOI18N
                    c.setIcon(Icons.getIcon(LanguageIcons.METHOD_PUBLIC));
                    return c;
                }
            });
            methodsList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) methodSelected();
                }
            });

            JLabel methodsLabel = new JLabel(Bundle.ClassMethodSelector_capMethods(), JLabel.LEADING);
            methodsLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            methodsInheritedB = new JToggleButton(Icons.getIcon(LanguageIcons.METHOD_INHERITED)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    reload();
                    PREF.putBoolean("Profiler.CMS.methodsInheritedB", methodsInheritedB.isSelected()); // NOI18N
                }
            };
            methodsInheritedB.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            methodsInheritedB.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
            methodsInheritedB.setToolTipText(Bundle.ClassMethodSelector_showInheritedMethods());
            methodsInheritedB.setSelected(PREF.getBoolean("Profiler.CMS.methodsInheritedB", false));
            methodsNonPublicB = new JToggleButton(Icons.getIcon(LanguageIcons.METHOD_PRIVATE)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    reload();
                    PREF.putBoolean("Profiler.CMS.methodsNonPublicB", methodsNonPublicB.isSelected()); // NOI18N
                }
            };
            methodsNonPublicB.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            methodsNonPublicB.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
            methodsNonPublicB.setToolTipText(Bundle.ClassMethodSelector_showNonPublicMethods());
            methodsNonPublicB.setSelected(PREF.getBoolean("Profiler.CMS.methodsNonPublicB", true));
            methodsStaticB = new JToggleButton(Icons.getIcon(LanguageIcons.METHOD_PUBLIC_STATIC)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    reload();
                    PREF.putBoolean("Profiler.CMS.methodsStaticB", methodsStaticB.isSelected()); // NOI18N
                }
            };
            methodsStaticB.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            methodsStaticB.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
            methodsStaticB.setToolTipText(Bundle.ClassMethodSelector_showStaticMethods());
            methodsStaticB.setSelected(PREF.getBoolean("Profiler.CMS.methodsStaticB", true));
            JToolBar methodsTools = new FilteringToolbar(Bundle.ClassMethodSelector_lblFilterItems()) {
                protected void filterChanged(String filter) {
                    filteredMethods.setFilter(filter);
                }
            };
            methodsTools.add(Box.createHorizontalStrut(2));
            methodsTools.addSeparator();
            methodsTools.add(Box.createHorizontalStrut(2));
            methodsTools.add(methodsInheritedB);
            methodsTools.add(Box.createHorizontalStrut(2));
            methodsTools.add(methodsStaticB);
            methodsTools.add(Box.createHorizontalStrut(2));
            methodsTools.add(methodsNonPublicB);
            
            setOpaque(false);
            setLayout(new BorderLayout());
            add(methodsLabel, BorderLayout.NORTH);
            add(new JScrollPane(methodsList), BorderLayout.CENTER);
            add(methodsTools, BorderLayout.SOUTH);
        }
        
    }
    
    
    private static class HintRenderer extends LabelRenderer {
                
        HintRenderer() {
            super(true);
            setHorizontalAlignment(CENTER);
            setForeground(UIUtils.getDisabledLineColor());
        }

        void setup(Boolean mode, Dimension size) {
            if (Boolean.FALSE.equals(mode)) setText(Bundle.ClassMethodSelector_lblComputing());
            else if (Boolean.TRUE.equals(mode)) setText(Bundle.ClassMethodSelector_lblNoItems());
            else setText(Bundle.ClassMethodSelector_lblNoSelection());
            setSize(size);
        }

    }
    
    
    private static abstract class FilteredListModel<E> implements ListModel<E> {
        
        private final Collection<ListDataListener> listeners;
        
        private final ListModel<E> data;
        
        private ListSelectionModel selection;
        private E selected;
        
        private String filter;
        private final List<Integer> indices;
        
        public FilteredListModel(ListModel data) {
            this.data = data;
            
            listeners = new HashSet();
            
            indices = new ArrayList();
            doFilter();
            
            this.data.addListDataListener(new ListDataListener() {
                public void intervalAdded(ListDataEvent e)   { doFilter(); }
                public void intervalRemoved(ListDataEvent e) { doFilter(); }
                public void contentsChanged(ListDataEvent e) { doFilter(); }
            });
        }
        
        
        protected abstract boolean matchesFilter(E item, String filter);
        
        
        public void setSelectionModel(final ListSelectionModel selection) {
            this.selection = selection;
            selection.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (!e.getValueIsAdjusting()) {
                        int index = selection.getMinSelectionIndex();
                        selected = index == -1 ? null : getElementAt(index);
                    }
                }
            });
        }
        
        
        public void setFilter(String filter) {
            if (filter != null && filter.isEmpty()) filter = null;
            if (Objects.equals(this.filter, filter)) return;
            
            this.filter = filter;
            doFilter();
        }
        
        public String getFilter() {
            return filter == null ? "" : filter; // NOI18N
        }
        

        public int getSize() {
            return filter == null ? data.getSize() : indices.size();
        }

        public E getElementAt(int index) {
            return data.getElementAt(filter == null ? index : indices.get(index));
        }

        public void addListDataListener(ListDataListener listener) {
            listeners.add(listener);
        }

        public void removeListDataListener(ListDataListener listener) {
            listeners.remove(listener);
        }
        
        
        private void doFilter() {
            indices.clear();
            
            int _selected = -1;
            
            if (filter != null) {
                for (int i = 0; i < data.getSize(); i++) {
                    E element = data.getElementAt(i);
                    if (matchesFilter(element, filter)) {
                        if (selection != null && element.equals(selected)) _selected = indices.size();
                        indices.add(i);
                    }
                }
            } else if (selection != null) {
                for (int i = 0; i < data.getSize(); i++) {
                    E element = data.getElementAt(i);
                    if (element.equals(selected)) _selected = i;
                }
            }
            
            if (!listeners.isEmpty()) {
                // TODO: should only fire events if data really changed
                ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, indices.size());
                for (ListDataListener listener : listeners) listener.contentsChanged(event);
            }
            
            if (selection != null) {
                if (_selected == -1) selection.clearSelection();
                else selection.setSelectionInterval(_selected, _selected);
            }
        }
        
    }
    
}
