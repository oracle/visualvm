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

package org.netbeans.modules.profiler.heapwalk;

import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


/**
 *
 * @author Jiri Sedlacek
 */
public class LegendPanel extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String GC_ROOT_STRING = NbBundle.getMessage(ClassesListController.class,
                                                                     "ClassesListController_GcRootString"); // NOI18N
    private static final String ARRAY_TYPE_STRING = NbBundle.getMessage(ClassesListController.class,
                                                                        "ClassesListController_ArrayTypeString"); // NOI18N
    private static final String OBJECT_TYPE_STRING = NbBundle.getMessage(ClassesListController.class,
                                                                         "ClassesListController_ObjectTypeString"); // NOI18N
    private static final String PRIMITIVE_TYPE_STRING = NbBundle.getMessage(ClassesListController.class,
                                                                            "ClassesListController_PrimitiveTypeString"); // NOI18N
    private static final String STATIC_FIELD_STRING = NbBundle.getMessage(ClassesListController.class,
                                                                          "ClassesListController_StaticFieldString"); // NOI18N
    private static final String LOOP_STRING = NbBundle.getMessage(ClassesListController.class, "ClassesListController_LoopString"); // NOI18N
                                                                                                                                    // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JLabel gcRootLegend;
    private JLabel gcRootLegendDivider;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LegendPanel(boolean showGCRoot) {
        initComponents();
        setGCRootVisible(showGCRoot);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setGCRootVisible(boolean showGCRoot) {
        gcRootLegend.setVisible(showGCRoot);
        gcRootLegendDivider.setVisible(showGCRoot);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 4, 5));

        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 5, 0));

        gcRootLegend = new JLabel(GC_ROOT_STRING, BrowserUtils.ICON_GCROOT, SwingConstants.LEFT);
        gcRootLegendDivider = new JLabel("|"); // NOI18N

        legendPanel.add(new JLabel(ARRAY_TYPE_STRING, BrowserUtils.ICON_ARRAY, SwingConstants.LEFT));
        legendPanel.add(new JLabel("|")); // NOI18N
        legendPanel.add(new JLabel(OBJECT_TYPE_STRING, BrowserUtils.ICON_INSTANCE, SwingConstants.LEFT));
        legendPanel.add(new JLabel("|")); // NOI18N
        legendPanel.add(new JLabel(PRIMITIVE_TYPE_STRING, BrowserUtils.ICON_PRIMITIVE, SwingConstants.LEFT));
        legendPanel.add(new JLabel("|")); // NOI18N
        legendPanel.add(new JLabel(STATIC_FIELD_STRING, BrowserUtils.ICON_STATIC, SwingConstants.LEFT));
        legendPanel.add(new JLabel("|")); // NOI18N
        legendPanel.add(gcRootLegend);
        legendPanel.add(gcRootLegendDivider);
        legendPanel.add(new JLabel(LOOP_STRING, BrowserUtils.ICON_LOOP, SwingConstants.LEFT));

        //add(new JLabel("Legend:"), BorderLayout.WEST);
        add(legendPanel, BorderLayout.EAST);
    }
}
