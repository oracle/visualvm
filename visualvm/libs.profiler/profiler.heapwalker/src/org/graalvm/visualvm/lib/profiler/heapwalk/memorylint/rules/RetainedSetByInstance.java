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

import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import org.openide.util.NbBundle;


//@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule.class)
public class RetainedSetByInstance extends IteratingRule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class Customizer extends JPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        JTextField txtFld;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        Customizer() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            JLabel caption = new JLabel(NbBundle.getMessage(RetainedSetByInstance.class, "LBL_ClassName"));
            caption.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            add(caption);
            txtFld = new JTextField(CLASSNAME, 15);
            txtFld.getDocument().addDocumentListener(new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        RetainedSetByInstance.this.updateClassName(txtFld.getText());
                    }

                    public void insertUpdate(DocumentEvent e) {
                        RetainedSetByInstance.this.updateClassName(txtFld.getText());
                    }

                    public void removeUpdate(DocumentEvent e) {
                        RetainedSetByInstance.this.updateClassName(txtFld.getText());
                    }
                });
            add(txtFld);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static String CLASSNAME = "java.io.File";  // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Heap heap;
    private Set<Histogram> allDocs = new HashSet<>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RetainedSetByInstance() {
        super(NbBundle.getMessage(RetainedSetByInstance.class, "LBL_RSBI_Name"),
                NbBundle.getMessage(RetainedSetByInstance.class, "LBL_RSBI_Desc"),
                CLASSNAME);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(RetainedSetByInstance.class, "LBL_RSBI_LongDesc");
    }

    public void perform(Instance hm) {
        Set<Instance> retained = Utils.getRetainedSet(hm, heap);
        Histogram<Histogram.Entry> hist = new Histogram<>();

        for (Instance i : retained) {
            String key = Utils.printClass(getContext(), i.getJavaClass().getName());
            hist.add(key, new Histogram.Entry(i.getSize()));
        }

        allDocs.add(hist);
    }

    @Override
    protected JComponent createCustomizer() {
        return new Customizer();
    }

    @Override
    protected void prepareRule(MemoryLint context) {
        heap = context.getHeap();
    }
    
    @Override
    protected String resultsHeader() {
        return "<h2>" + getDisplayName() + " (" + Utils.printClass(getContext(), CLASSNAME) + ")</h2>"; // NOI18N
    }

    @Override
    protected void summary() {
        for (Histogram h : allDocs) {
//            getContext().appendResults("<hr>Histogram of retained size:<br>");
            getContext().appendResults(h.toString(0));
        }
    }

    void updateClassName(String className) {
        CLASSNAME = className;
        setClassNamePattern(className);
    }
}
