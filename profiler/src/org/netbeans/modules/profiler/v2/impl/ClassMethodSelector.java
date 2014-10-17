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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.utils.formatting.DefaultMethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.Formattable;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.java.ProfilerTypeUtils;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.netbeans.modules.profiler.api.java.SourcePackageInfo;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ClassMethodSelector {
    
    private static final WeakProcessor PROCESSOR = new WeakProcessor("Profiler ClassMethodSelector Processor"); // NOI18N
    private static final MethodNameFormatter METHOD_FORMATTER = new DefaultMethodNameFormatter(DefaultMethodNameFormatter.VERBOSITY_METHOD);
    
    public static SourceClassInfo selectClass(Lookup.Provider project) {
        // TODO: wait for finished scan
        
        UI ui = UI.forProject(project, false);

        DialogDescriptor dd = new DialogDescriptor(ui, "Select Class", true,
                                                   new Object[] { ui.getOKButton(), DialogDescriptor.CANCEL_OPTION },
                                                   ui.getOKButton(), DialogDescriptor.BOTTOM_ALIGN, null, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);
        
        return dd.getValue() == ui.getOKButton() ? ui.selectedClass() : null;
    }
    
    public static SourceMethodInfo selectMethod(Lookup.Provider project) {
        // TODO: wait for finished scan
        
        UI ui = UI.forProject(project, true);

        DialogDescriptor dd = new DialogDescriptor(ui, "Select Method", true,
                                                   new Object[] { ui.getOKButton(), DialogDescriptor.CANCEL_OPTION },
                                                   ui.getOKButton(), DialogDescriptor.BOTTOM_ALIGN, null, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);
        
        return dd.getValue() == ui.getOKButton() ? ui.selectedMethod() : null;
    }
    
    
    private static Collection<SourcePackageInfo> getPackages(Lookup.Provider project, boolean sources, boolean dependencies) {
        // TODO: optimize
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
    
    
    private ClassMethodSelector() {}
    
    
    // --- UI ------------------------------------------------------------------
    
    private static class UI extends JPanel {
        
        private final JButton okButton;
        
        private final JList projectList;
        private final DefaultListModel<Lookup.Provider> projectListModel;
        
        private final JList packageList;
        private final DefaultListModel<SourcePackageInfo> packageListModel;
        private final AbstractButton packagesSourcesB;
        private final AbstractButton packagesDependenciesB;
        
        private final JList classesList;
        private final DefaultListModel<SourceClassInfo> classesListModel;
        private final AbstractButton classesInnerB;
        private final AbstractButton classesAnonymousB;
        
        private final JList methodsList;
        private final DefaultListModel<SourceMethodInfo> methodsListModel;
        private final AbstractButton methodsInheritedB;
        private final AbstractButton methodsNonPublicB;
        private final AbstractButton methodsStaticB;
        
        
        static UI forProject(Lookup.Provider project, boolean method) {
            return new UI(project, method);
        }
        
        JButton getOKButton() {
            return okButton;
        }
        
        SourceClassInfo selectedClass() {
            return (SourceClassInfo)classesList.getSelectedValue();
        }
        
        SourceMethodInfo selectedMethod() {
            return (SourceMethodInfo)methodsList.getSelectedValue();
        }
        
        
        private UI(Lookup.Provider project, boolean method) {
            super(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JPanel selectors = new JPanel(new GridLayout(1, method ? 4 : 3, 10, 10));
            add(selectors, BorderLayout.CENTER);
            
            final int listWidth = 200;
            
            class HintRenderer extends LabelRenderer {
                
                HintRenderer() {
                    super(true);
                    setHorizontalAlignment(CENTER);
                    setForeground(UIUtils.getDisabledLineColor());
                }
                
                void setup(Boolean mode, Dimension size) {
                    if (Boolean.FALSE.equals(mode)) setText("Computing...");
                    else if (Boolean.TRUE.equals(mode)) setText("No items");
                    else setText("No selection");
                    setSize(size);
                }
                
            }
            final HintRenderer listHint = new HintRenderer();
            
            final Preferences pref = NbPreferences.forModule(ClassMethodSelector.class);
            
            projectListModel = new DefaultListModel();
            final FilteredListModel<Lookup.Provider> filteredProjects = new FilteredListModel<Lookup.Provider>(projectListModel) {
                protected boolean matchesFilter(Lookup.Provider proj, String filter) {
                    return ProjectUtilities.getDisplayName(proj).contains(filter);
                }
            };
            projectList = new JList(filteredProjects) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = listWidth;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getModel().getSize() == 0) {
                        listHint.setup(true, getSize());
                        listHint.paint(g);
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
                    if (!e.getValueIsAdjusting()) {
                        Lookup.Provider sel = (Lookup.Provider)projectList.getSelectedValue();
                        projectSelected(sel);
                    }
                }
            });
            
            JLabel projectsLabel = new JLabel("Projects:", JLabel.LEADING);
            projectsLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JToolBar projectsTools = new FilteringToolBar() {
                protected void filterChanged(String filter) {
                    filteredProjects.setFilter(filter);
                }
            };
            JPanel projectsPanel = new JPanel(new BorderLayout());
            projectsPanel.add(projectsLabel, BorderLayout.NORTH);
            projectsPanel.add(new JScrollPane(projectList), BorderLayout.CENTER);
            projectsPanel.add(projectsTools, BorderLayout.SOUTH);
            selectors.add(projectsPanel);
            
            packageListModel = new DefaultListModel();
            final FilteredListModel<SourcePackageInfo> filteredPackages = new FilteredListModel<SourcePackageInfo>(packageListModel) {
                protected boolean matchesFilter(SourcePackageInfo pkg, String filter) {
                    return pkg.getBinaryName().contains(filter);
                }
            };
            packageList = new JList(filteredPackages) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = listWidth;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getModel().getSize() == 0) {
                        if (!isEnabled()) listHint.setup(false, getSize());
                        else if (projectList.getSelectedValue() != null) listHint.setup(true, getSize());
                        else listHint.setup(null, getSize());
                        listHint.paint(g);
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
                    if (!e.getValueIsAdjusting()) {
                        SourcePackageInfo sel = (SourcePackageInfo)packageList.getSelectedValue();
                        packageSelected(sel);
                    }
                }
            });
            
            JLabel packagesLabel = new JLabel("Packages:", JLabel.LEADING);
            packagesLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JPanel packagesPanel = new JPanel(new BorderLayout());
            packagesPanel.add(packagesLabel, BorderLayout.NORTH);
            packagesPanel.add(new JScrollPane(packageList), BorderLayout.CENTER);
            packagesSourcesB = new JToggleButton(Icons.getIcon(LanguageIcons.CONSTRUCTORS)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    projectSelected((Lookup.Provider)projectList.getSelectedValue());
                    pref.putBoolean("Profiler.CMS.packagesSourcesB", packagesSourcesB.isSelected()); // NOI18N
                }
            };
            packagesSourcesB.setToolTipText("Show project packages");
            packagesSourcesB.setSelected(pref.getBoolean("Profiler.CMS.packagesSourcesB", true)); // NOI18N
            packagesDependenciesB = new JToggleButton(Icons.getIcon(LanguageIcons.JAR)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    projectSelected((Lookup.Provider)projectList.getSelectedValue());
                    pref.putBoolean("Profiler.CMS.packagesDependenciesB", packagesDependenciesB.isSelected()); // NOI18N
                }
            };
            packagesDependenciesB.setToolTipText("Show dependencies packages");
            packagesDependenciesB.setSelected(pref.getBoolean("Profiler.CMS.packagesDependenciesB", false)); // NOI18N
            JToolBar packagesTools = new FilteringToolBar() {
                protected void filterChanged(String filter) {
                    filteredPackages.setFilter(filter);
                }
            };
            packagesTools.add(Box.createHorizontalStrut(2));
            packagesTools.addSeparator();
            packagesTools.add(Box.createHorizontalStrut(2));
            packagesTools.add(packagesSourcesB);
            packagesTools.add(Box.createHorizontalStrut(2));
            packagesTools.add(packagesDependenciesB);
            packagesPanel.add(packagesTools, BorderLayout.SOUTH);
            selectors.add(packagesPanel);
            
            classesListModel = new DefaultListModel();
            final FilteredListModel<SourceClassInfo> filteredClasses = new FilteredListModel<SourceClassInfo>(classesListModel) {
                protected boolean matchesFilter(SourceClassInfo cls, String filter) {
                    return cls.getSimpleName().contains(filter);
                }
            };
            classesList = new JList(filteredClasses) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = listWidth;
                    return dim;
                }
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    
                    if (getModel().getSize() == 0) {
                        if (!isEnabled()) listHint.setup(false, getSize());
                        else if (packageList.getSelectedValue() != null) listHint.setup(true, getSize());
                        else listHint.setup(null, getSize());
                        listHint.paint(g);
                    }
                }
            };
            classesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
                    if (!e.getValueIsAdjusting()) {
                        SourceClassInfo sel = (SourceClassInfo)classesList.getSelectedValue();
                        classSelected(sel);
                    }
                }
            });
            
            JLabel classesLabel = new JLabel("Classes:", JLabel.LEADING);
            classesLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JPanel classesPanel = new JPanel(new BorderLayout());
            classesPanel.add(classesLabel, BorderLayout.NORTH);
            classesPanel.add(new JScrollPane(classesList), BorderLayout.CENTER);
            classesInnerB = new JToggleButton(Icons.getIcon(LanguageIcons.CLASS)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    packageSelected((SourcePackageInfo)packageList.getSelectedValue());
                    classesAnonymousB.setEnabled(isSelected());
                    pref.putBoolean("Profiler.CMS.classesInnerB", classesInnerB.isSelected()); // NOI18N
                }
            };
            classesInnerB.setToolTipText("Show inner classes");
            classesInnerB.setSelected(pref.getBoolean("Profiler.CMS.classesInnerB", true)); // NOI18N
            classesAnonymousB = new JToggleButton(Icons.getIcon(LanguageIcons.CLASS_ANONYMOUS)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    if (isEnabled()) packageSelected((SourcePackageInfo)packageList.getSelectedValue());
                    pref.putBoolean("Profiler.CMS.classesAnonymousB", classesAnonymousB.isSelected()); // NOI18N
                }
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                    if (!isEnabled()) setSelected(false);
                }
            };
            classesAnonymousB.setToolTipText("Show anonymous classes");
            classesAnonymousB.setSelected(pref.getBoolean("Profiler.CMS.classesAnonymousB", false)); // NOI18N
            JToolBar classesTools = new FilteringToolBar() {
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
            classesPanel.add(classesTools, BorderLayout.SOUTH);
            selectors.add(classesPanel);
            
            if (method) {
                methodsListModel = new DefaultListModel();
                final FilteredListModel<SourceMethodInfo> filteredMethods = new FilteredListModel<SourceMethodInfo>(methodsListModel) {
                    protected boolean matchesFilter(SourceMethodInfo mtd, String filter) {
                        return mtd.getName().contains(filter);
                    }
                };
                methodsList = new JList(filteredMethods) {
                    public Dimension getPreferredScrollableViewportSize() {
                        Dimension dim = super.getPreferredScrollableViewportSize();
                        dim.width = listWidth;
                        return dim;
                    }
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        
                        if (getModel().getSize() == 0) {
                            if (!isEnabled()) listHint.setup(false, getSize());
                            else if (classesList.getSelectedValue() != null) listHint.setup(true, getSize());
                            else listHint.setup(null, getSize());
                            listHint.paint(g);
                        }
                    }
                };
                methodsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
                        if (!e.getValueIsAdjusting()) {
                            List<SourceMethodInfo> sel = (List<SourceMethodInfo>)methodsList.getSelectedValuesList();
                            methodsSelected(sel);
                        }
                    }
                });
                
                JLabel methodsLabel = new JLabel("Methods:", JLabel.LEADING);
                methodsLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
                JPanel methodsPanel = new JPanel(new BorderLayout());
                methodsPanel.add(methodsLabel, BorderLayout.NORTH);
                methodsPanel.add(new JScrollPane(methodsList), BorderLayout.CENTER);
                methodsInheritedB = new JToggleButton(Icons.getIcon(LanguageIcons.METHOD_INHERITED)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        classSelected((SourceClassInfo)classesList.getSelectedValue());
                        pref.putBoolean("Profiler.CMS.methodsInheritedB", methodsInheritedB.isSelected()); // NOI18N
                    }
                };
                methodsInheritedB.setToolTipText("Show inherited methods");
                methodsInheritedB.setSelected(pref.getBoolean("Profiler.CMS.methodsInheritedB", false));
                methodsNonPublicB = new JToggleButton(Icons.getIcon(LanguageIcons.METHOD_PRIVATE)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        classSelected((SourceClassInfo)classesList.getSelectedValue());
                        pref.putBoolean("Profiler.CMS.methodsNonPublicB", methodsNonPublicB.isSelected()); // NOI18N
                    }
                };
                methodsNonPublicB.setToolTipText("Show non-public methods");
                methodsNonPublicB.setSelected(pref.getBoolean("Profiler.CMS.methodsNonPublicB", true));
                methodsStaticB = new JToggleButton(Icons.getIcon(LanguageIcons.METHOD_PUBLIC_STATIC)) {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        classSelected((SourceClassInfo)classesList.getSelectedValue());
                        pref.putBoolean("Profiler.CMS.methodsStaticB", methodsStaticB.isSelected()); // NOI18N
                    }
                };
                methodsStaticB.setToolTipText("Show static methods");
                methodsStaticB.setSelected(pref.getBoolean("Profiler.CMS.methodsStaticB", true));
                JToolBar methodsTools = new FilteringToolBar() {
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
                methodsPanel.add(methodsTools, BorderLayout.SOUTH);
                selectors.add(methodsPanel);
            } else {
                methodsListModel = null;
                methodsList = null;
                methodsInheritedB = null;
                methodsNonPublicB = null;
                methodsStaticB = null;
            }
            
            okButton = new JButton("OK");
            okButton.setEnabled(false);
            
            initData(project);
        }
        
        private void initData(Lookup.Provider project) {
            // TODO: add possibility to include custom jar/folder
            // TODO: add possibility to read classes (histogram) from live app
            Lookup.Provider[] projects = ProjectUtilities.getSortedProjects(
                                         ProjectUtilities.getOpenedProjects());
            for (Lookup.Provider prj : projects) projectListModel.addElement(prj);
            if (project != null) projectList.setSelectedValue(project, true);
        }
        
        private void projectSelected(final Lookup.Provider project) {
            packageListModel.clear();
            packageList.setEnabled(project == null);
            
            classesListModel.clear();
            if (methodsListModel != null) methodsListModel.clear();
            
            if (project != null) {
                final boolean sources = packagesSourcesB.isSelected();
                final boolean dependencies = packagesDependenciesB.isSelected();
                
                PROCESSOR.post(new Runnable() {
                    public void run() {
                        final Collection<SourcePackageInfo> packages = getPackages(project, sources, dependencies);
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
        
        private void packageSelected(final SourcePackageInfo pkg) {
            classesListModel.clear();
            classesList.setEnabled(pkg == null);
            
            if (methodsListModel != null) methodsListModel.clear();
            
            if (pkg != null) {
                final boolean inner = classesInnerB.isSelected();
                final boolean anonymous = classesAnonymousB.isSelected();
                
                PROCESSOR.post(new Runnable() {
                    public void run() {
                        final Collection<SourceClassInfo> classes = getClasses(pkg, inner, anonymous);
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
        
        private void classSelected(final SourceClassInfo cls) {
            if (methodsListModel == null) {
                okButton.setEnabled(cls != null);
            } else {
                methodsListModel.clear();
                methodsList.setEnabled(cls == null);
                
                if (cls != null) {
                    final boolean inherited = methodsInheritedB.isSelected();
                    final boolean nonpublic = methodsNonPublicB.isSelected();
                    final boolean staticc   = methodsStaticB.isSelected();
                    
                    PROCESSOR.post(new Runnable() {
                        public void run() {
                            final Collection<SourceMethodInfo> methods = getMethods(cls, inherited, nonpublic, staticc);
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
        }
        
        private void methodsSelected(List<SourceMethodInfo> methods) {
            okButton.setEnabled(!methods.isEmpty());
        }
        
    }
    
    
    private static abstract class FilteringToolBar extends JToolBar {
        
        private final List<Component> hiddenComponents = new ArrayList();
        private final AbstractButton filterButton;
        
        public FilteringToolBar() {
            setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
            setBorderPainted(false);
            setFloatable(false);
            setRollover(true);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            
            filterButton = new JToggleButton(Icons.getIcon(GeneralIcons.FILTER)) {
                protected void fireActionPerformed(ActionEvent e) {
                    if (isSelected()) showFilter(); else hideFilter();
                }
            };
            filterButton.setToolTipText("Filter items");
            add(filterButton);
        }
        
        
        protected abstract void filterChanged(String filter);
        
        
        private void showFilter() {
            filterButton.setSelected(true);
            
            final JTextField f = new JTextField();
            f.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e)  { changed(); }
                public void removeUpdate(DocumentEvent e)  { changed(); }
                public void changedUpdate(DocumentEvent e) { changed(); }
                private void changed() { filterChanged(f.getText().trim()); }
            });
            f.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { if (esc(e)) hideFilter(); }
                public void keyReleased(KeyEvent e) { esc(e); }
                private boolean esc(KeyEvent e) {
                    boolean esc = e.getKeyCode() == KeyEvent.VK_ESCAPE;
                    if (esc) e.consume();
                    return esc;
                }
            });
            
            for (int i = 1; i < getComponentCount(); i++)
                hiddenComponents.add(getComponent(i));
            
            for (Component c : hiddenComponents) remove(c);
            
            add(Box.createHorizontalStrut(3));
            add(f);
            f.requestFocusInWindow();
            
            invalidate();
            revalidate();
            doLayout();
            repaint();
        }
        
        private void hideFilter() {
            filterChanged(null);
            
            remove(2);
            remove(1);
            for (Component c : hiddenComponents) add(c);
            
            filterButton.setSelected(false);
            filterButton.requestFocusInWindow();
            
            invalidate();
            revalidate();
            doLayout();
            repaint();
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
