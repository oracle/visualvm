/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.heapwalk.memorylint.rules;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.memorylint.*;
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
import org.openide.util.NbBundle;


@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.heapwalk.memorylint.Rule.class)
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
    private Set<Histogram> allDocs = new HashSet<Histogram>();

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
        Histogram<Histogram.Entry> hist = new Histogram<Histogram.Entry>();

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
