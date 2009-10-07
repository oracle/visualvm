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

import org.openide.filesystems.FileObject;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;


/**
 * Shows a warning that no main class is set and allows choose a main class.
 *
 * @author Tomas Hurka
 * @author Jiri Rechtacek
 */
public class MainClassWarning extends JPanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private JPanel jPanel1;
    private String message;
    private FileObject[] sourcesRoots;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates new form LibrariesChooser
     */
    public MainClassWarning(String message, FileObject[] sourcesRoots) {
        this.sourcesRoots = sourcesRoots;
        this.message = message;
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the selected main class.
     *
     * @return name of class or null if no class with the main method is selected
     */
    public String getSelectedMainClass() {
        return ((MainClassChooser) jPanel1).getSelectedMainClass();
    }

    public void addChangeListener(ChangeListener l) {
        ((MainClassChooser) jPanel1).addChangeListener(l);
    }

    public void removeChangeListener(ChangeListener l) {
        ((MainClassChooser) jPanel1).removeChangeListener(l);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new MainClassChooser(sourcesRoots,
                                       org.openide.util.NbBundle.getBundle(MainClassWarning.class)
                                                                .getString("CTL_SelectAvaialableMainClasses")); // NOI18N

        setLayout(new java.awt.GridBagLayout());

        getAccessibleContext()
            .setAccessibleDescription(org.openide.util.NbBundle.getBundle(MainClassWarning.class).getString("AD_MainClassWarning")); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, this.message);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 6, 12);
        add(jLabel1, gridBagConstraints);

        jScrollPane1.setBorder(null);
        jScrollPane1.setViewportView(jPanel1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jScrollPane1, gridBagConstraints);
    }
}
