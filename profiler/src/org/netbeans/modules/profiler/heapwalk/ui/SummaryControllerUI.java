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

package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import org.netbeans.modules.profiler.heapwalk.SummaryController;
import java.awt.Dimension;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.netbeans.lib.profiler.ui.components.JExtendedSplitPane;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class SummaryControllerUI extends JPanel {
    private SummaryController summaryController;
    private Presenter presenter;
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    private static class Presenter extends JToggleButton {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static ImageIcon ICON_INFO = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/infoTab.png", false); // NOI18N

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Presenter() {
            super();
            setText(VIEW_TITLE);
            setToolTipText(VIEW_DESCR);
            setIcon(ICON_INFO);
            setMargin(new java.awt.Insets(getMargin().top, getMargin().top, getMargin().bottom, getMargin().top));
        }
    }

    private static final String VIEW_TITLE = NbBundle.getMessage(SummaryControllerUI.class, "SummaryControllerUI_ViewTitle"); // NOI18N
    private static final String VIEW_DESCR = NbBundle.getMessage(SummaryControllerUI.class, "SummaryControllerUI_ViewDescr"); // NOI18N

    // --- Constructors ----------------------------------------------------------
    public SummaryControllerUI(SummaryController summaryController) {
        this.summaryController = summaryController;

        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public AbstractButton getPresenter() {
        if (presenter == null) {
            presenter = new Presenter();
        }

        return presenter;
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        
        final JPanel hintsPanel = summaryController.getHintsController().getPanel();

        JExtendedSplitPane contentsSplit = new JExtendedSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                               summaryController.getOverViewController().getPanel(), hintsPanel);
        contentsSplit.setResizeWeight(0.75d);

        tweakSplitPaneUI(contentsSplit);

        add(contentsSplit, BorderLayout.CENTER); 
    }
    
    private void tweakSplitPaneUI(JSplitPane splitPane) {
        splitPane.setBorder(null);
        splitPane.setDividerSize(3);

        if (!(splitPane.getUI() instanceof BasicSplitPaneUI)) {
            return;
        }

        BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();

        if (divider != null) {
            divider.setBorder(null);
        }
    }
}
