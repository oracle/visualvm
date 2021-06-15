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

import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;


/**
 * Shows a warning that no main class is set and allows choose a main class.
 *
 * @author Tomas Hurka
 * @author Jiri Rechtacek
 */
@NbBundle.Messages({
    "AD_MainClassWarning=N/A"
})
public class MainClassWarning extends JPanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private JPanel jPanel1;
    private String message;
    private Lookup.Provider project;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates new form LibrariesChooser
     */
    public MainClassWarning(String message, Lookup.Provider project) {
        this.project = project;
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
        jPanel1 = new MainClassChooser(project, Bundle.CTL_SelectAvaialableMainClasses());

        setLayout(new java.awt.GridBagLayout());

        getAccessibleContext()
            .setAccessibleDescription(Bundle.AD_MainClassWarning());
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
