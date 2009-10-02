/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.utils;

import org.openide.awt.Mnemonics;
import org.openide.awt.MouseUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 * Browses and allows to choose a project's main class.
 *
 * @author Tomas Hurka
 * @author Jiri Rechtacek
 */
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
    public MainClassChooser(FileObject[] sourcesRoots) {
        this(sourcesRoots, null);
    }

    public MainClassChooser(FileObject[] sourcesRoots, String subtitle) {
        dialogSubtitle = subtitle;
        initComponents();
        initClassesView(sourcesRoots);
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
        return new Object[] { NbBundle.getMessage(MainClassChooser.class, "LBL_ChooseMainClass_WARMUP_MESSAGE") }; // NOI18N
    }

    private void initClassesView(final FileObject[] sourcesRoots) {
        possibleMainClasses = null;
        jMainClassList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jMainClassList.setListData(getWarmupList());
        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    possibleMainClasses = SourceUtils.findMainClasses(sourcesRoots);

                    if (possibleMainClasses.isEmpty()) {
                        jMainClassList.setListData(new String[] {
                                                       NbBundle.getMessage(MainClassChooser.class,
                                                                           "LBL_ChooseMainClass_NO_CLASSES_NODE")
                                                   }); // NOI18N
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
            .setAccessibleDescription(NbBundle.getBundle(MainClassChooser.class).getString("AD_MainClassChooser")); // NOI18N
        jLabel1.setLabelFor(jMainClassList);
        Mnemonics.setLocalizedText(jLabel1, NbBundle.getBundle(MainClassChooser.class).getString("CTL_AvaialableMainClasses")); // NOI18N
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
                      .setAccessibleDescription(NbBundle.getBundle(MainClassChooser.class).getString("AD_jMainClassList")); // NOI18N

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
