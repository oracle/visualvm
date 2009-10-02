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

package org.netbeans.modules.profiler.heapwalk.memorylint.rules;

import javax.swing.BorderFactory;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.memorylint.*;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.util.NbBundle;


@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.heapwalk.memorylint.Rule.class)
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
