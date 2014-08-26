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
import java.awt.GridLayout;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.java.ProfilerTypeUtils;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.netbeans.modules.profiler.api.java.SourcePackageInfo;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ClassMethodSelector {
    
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
    
    
    private static Collection<SourcePackageInfo> getPackages(Lookup.Provider project) {
        // TODO: return all packages containing at least one class
        return ProfilerTypeUtils.getPackages(false, SourcePackageInfo.Scope.SOURCE, project);
    }
    
    private static Collection<SourceClassInfo> getClasses(SourcePackageInfo pkg, boolean inner, boolean anonymous) {
        // TODO: add all inner & anonymous classes based on the parameters
        return pkg.getClasses();
    }
    
    private static Collection<SourceMethodInfo> getMethods(SourceClassInfo cls, boolean inherited) {
        // TODO: hide inherited methods based on the parameter
        return cls.getMethods(true);
    }
    
    
    private ClassMethodSelector() {}
    
    
    // --- UI ------------------------------------------------------------------
    
    private static class UI extends JPanel {
        
        private final JButton okButton;
        
        private final JList projectList;
        private final DefaultListModel<Lookup.Provider> projectListModel;
        
        private final JList packageList;
        private final DefaultListModel<SourcePackageInfo> packageListModel;
        
        private final JList classesList;
        private final DefaultListModel<SourceClassInfo> classesListModel;
        
        private final JList methodsList;
        private final DefaultListModel<SourceMethodInfo> methodsListModel;
        
        
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
            
            projectListModel = new DefaultListModel();
            projectList = new JList(projectListModel) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = listWidth;
                    return dim;
                }
            };
            projectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
            selectors.add(new JScrollPane(projectList));
            
            packageListModel = new DefaultListModel();
            packageList = new JList(packageListModel) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = listWidth;
                    return dim;
                }
            };
            packageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            packageList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    DefaultListCellRenderer c = (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    SourcePackageInfo val = (SourcePackageInfo)value;
                    c.setText(val.getSimpleName());
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
            selectors.add(new JScrollPane(packageList));
            
            classesListModel = new DefaultListModel();
            classesList = new JList(classesListModel) {
                public Dimension getPreferredScrollableViewportSize() {
                    Dimension dim = super.getPreferredScrollableViewportSize();
                    dim.width = listWidth;
                    return dim;
                }
            };
            classesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
            selectors.add(new JScrollPane(classesList));
            
            if (method) {
                methodsListModel = new DefaultListModel();
                methodsList = new JList(methodsListModel) {
                    public Dimension getPreferredScrollableViewportSize() {
                        Dimension dim = super.getPreferredScrollableViewportSize();
                        dim.width = listWidth;
                        return dim;
                    }
                };
                methodsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                methodsList.setCellRenderer(new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        DefaultListCellRenderer c = (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        SourceMethodInfo val = (SourceMethodInfo)value;
                        c.setText(val.getName());
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
                selectors.add(new JScrollPane(methodsList));
            } else {
                methodsListModel = null;
                methodsList = null;
            }
            
            okButton = new JButton("OK");
            okButton.setEnabled(false);
            
            initData(project);
        }
        
        private void initData(Lookup.Provider project) {
            // TODO: add possibility to not show other projects
            // TODO: add possibility to include custom jar/folder
            // TODO: add possibility to read classes (histogram) from live app
            for (Lookup.Provider prj : ProjectUtilities.getOpenedProjects())
                projectListModel.addElement(prj);
            if (project != null) projectList.setSelectedValue(project, true);
        }
        
        private void projectSelected(final Lookup.Provider project) {
            packageListModel.clear();
            classesListModel.clear();
            if (methodsListModel != null) methodsListModel.clear();
            
            if (project != null) {
                // TODO: display progress label for packages list
                processor().post(new Runnable() {
                    public void run() {
                        final Collection<SourcePackageInfo> packages = getPackages(project);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
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
            if (methodsListModel != null) methodsListModel.clear();
            
            if (pkg != null) {
                // TODO: display progress label for classes list
                processor().post(new Runnable() {
                    public void run() {
                        final Collection<SourceClassInfo> classes = getClasses(pkg, true, true);
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
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
            
                if (cls != null) {
                    // TODO: display progress label for methods list
                    processor().post(new Runnable() {
                        public void run() {
                            final Collection<SourceMethodInfo> methods = getMethods(cls, true);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
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
    
    
    // --- Processor -----------------------------------------------------------
    
    private static Reference<RequestProcessor> PROCESSOR;
    
    private static synchronized RequestProcessor processor() {
        RequestProcessor p = PROCESSOR != null ? PROCESSOR.get() : null;
        
        if (p == null) {
            p = new RequestProcessor("Profiler ClassMethodSelector Processor");
            PROCESSOR = new WeakReference(p);
        }
        
        return p;
    }
    
}
