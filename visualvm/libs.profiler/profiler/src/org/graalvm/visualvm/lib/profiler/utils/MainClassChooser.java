/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.utils;

import org.openide.awt.Mnemonics;
import org.openide.awt.MouseUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.graalvm.visualvm.lib.profiler.api.java.ProfilerTypeUtils;
import org.graalvm.visualvm.lib.profiler.api.java.SourceClassInfo;
import org.openide.util.Lookup;


/**
 * Browses and allows to choose a project's main class.
 *
 * @author Tomas Hurka
 * @author Jiri Rechtacek
 */
@NbBundle.Messages({
    "CTL_SelectAvaialableMainClasses=&Select the main class for profiling:",
    "CTL_AvaialableMainClasses=&Main classes:",
    "AD_jMainClassList=N/A",
    "AD_MainClassChooser=N/A",
    "LBL_ChooseMainClass_NO_CLASSES_NODE=<No main classes found>",
    "LBL_ChooseMainClass_WARMUP_MESSAGE=Initializing view, please wait ..."
})
public class MainClassChooser extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Used only from unit tests to suppress check of main method. If value
    // is different from null it will be returned instead.
    public static Boolean unitTestingSupport_hasMainMethodResult = null;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private javax.swing.JLabel jLabel1;
    private javax.swing.JList jMainClassList;
    private javax.swing.JScrollPane jScrollPane1;
    private ChangeListener changeListener;
    private Collection<String> possibleMainClasses;
    private String dialogSubtitle = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates new form MainClassChooser
     */
    public MainClassChooser(Lookup.Provider project) {
        this(project, null);
    }

    public MainClassChooser(Lookup.Provider project, String subtitle) {
        dialogSubtitle = subtitle;
        initComponents();
        initClassesView(project);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the selected main class.
     *
     * @return name of class or null if no class with the main method is selected
     */
    public String getSelectedMainClass() {
        if (isValidMainClassName(jMainClassList.getSelectedValue())) {
            return (String) jMainClassList.getSelectedValue();
        } else {
            return null;
        }
    }

    public void addChangeListener(ChangeListener l) {
        changeListener = l;
    }

    /**
     * Checks if given file object contains the main method.
     *
     * @param classFO file object represents java
     * @return false if parameter is null or doesn't contain SourceCookie
     *         or SourceCookie doesn't contain the main method
     */
    public static boolean hasMainMethod(FileObject classFO) {
        //    return MDRUtils.hasMainMethod(classFO);
        return false;
    }

    public void removeChangeListener(ChangeListener l) {
        changeListener = null;
    }

    private boolean isValidMainClassName(Object value) {
        return (possibleMainClasses != null) && (possibleMainClasses.contains(value));
    }

    private Object[] getWarmupList() {
        return new Object[] { Bundle.LBL_ChooseMainClass_WARMUP_MESSAGE() };
    }

    private void initClassesView(final Lookup.Provider project) {
        possibleMainClasses = new ArrayList<String>();
        jMainClassList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jMainClassList.setListData(getWarmupList());
        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    for(SourceClassInfo sci : ProfilerTypeUtils.getMainClasses(project)) {
                        possibleMainClasses.add(sci.getQualifiedName());
                    }

                    if (possibleMainClasses.isEmpty()) {
                        jMainClassList.setListData(new String[] {
                                                       Bundle.LBL_ChooseMainClass_NO_CLASSES_NODE()
                                                   });
                    } else {
                        Object[] arr = possibleMainClasses.toArray();
                        // #46861, sort name of classes
                        Arrays.sort(arr);
                        jMainClassList.setListData(arr);
                        jMainClassList.setSelectedIndex(0);
                    }
                }
            });
        jMainClassList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent evt) {
                    if (changeListener != null) {
                        changeListener.stateChanged(new ChangeEvent(evt));
                    }
                }
            });
        // support for double click to finish dialog with selected class
        jMainClassList.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    if (MouseUtils.isDoubleClick(e)) {
                        if (getSelectedMainClass() != null) {
                            if (changeListener != null) {
                                changeListener.stateChanged(new ChangeEvent(e));
                            }
                        }
                    }
                }

                public void mousePressed(MouseEvent e) {
                }

                public void mouseReleased(MouseEvent e) {
                }

                public void mouseEntered(MouseEvent e) {
                }

                public void mouseExited(MouseEvent e) {
                }
            });

        if (dialogSubtitle != null) {
            Mnemonics.setLocalizedText(jLabel1, dialogSubtitle);
        }
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jMainClassList = new javax.swing.JList();

        setLayout(new java.awt.GridBagLayout());

        setPreferredSize(new java.awt.Dimension(380, 300));
        getAccessibleContext()
            .setAccessibleDescription(Bundle.AD_MainClassChooser());
        jLabel1.setLabelFor(jMainClassList);
        Mnemonics.setLocalizedText(jLabel1, Bundle.CTL_AvaialableMainClasses());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 2, 12);
        add(jLabel1, gridBagConstraints);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(100, 200));
        jScrollPane1.setViewportView(jMainClassList);
        jMainClassList.getAccessibleContext()
                      .setAccessibleDescription(Bundle.AD_jMainClassList());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 12);
        add(jScrollPane1, gridBagConstraints);
    }
}
