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

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.rules;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import org.openide.util.NbBundle;


//@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule.class)
public class DeepSize extends IteratingRule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class Customizer extends JPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        JTextField txtFld;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        Customizer() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            JLabel caption = new JLabel(NbBundle.getMessage(DeepSize.class, "LBL_ClassName"));
            caption.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            add(caption);
            txtFld = new JTextField(className, 15);
            txtFld.getDocument().addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        className = txtFld.getText();
                    }

                    public void insertUpdate(DocumentEvent e) {
                        className = txtFld.getText();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        className = txtFld.getText();
                    }
                });
            add(txtFld);
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    Walker walker;
    private static String className = "java.io.File"; // NOI18N

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DeepSize() {
        super(NbBundle.getMessage(DeepSize.class, "LBL_DS_Name"),
                NbBundle.getMessage(DeepSize.class, "LBL_DS_Desc"),
                ""); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(DeepSize.class, "LBL_DS_LongDesc");
    }

    @Override
    protected JComponent createCustomizer() {
        return new Customizer();
    }

    protected void perform(Instance hm) {
        walker.walk(hm);
    }

    protected @Override void prepareRule(MemoryLint context) {
        setClassNamePattern(className);
        walker = new Walker();
    }

    @Override
    protected String resultsHeader() {
        return NbBundle.getMessage(DeepSize.class, "LBL_DS_ResHeader", Utils.printClass(getContext(), className));
    }

    protected @Override void summary() {
        Distribution res = walker.getResults();
        String str = res.toString();
        str = str.replace("\n", "<br>"); // NOI18N
        str = str.replace("  ", "&nbsp;&nbsp;"); // NOI18N
        getContext().appendResults(str);
    }
}
