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

import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JExtendedSplitPane;
import org.netbeans.modules.profiler.heapwalk.InstancesController;
import org.netbeans.modules.profiler.heapwalk.LegendPanel;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.net.URL;
import java.text.MessageFormat;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 *
 * @author Jiri Sedlacek
 */
public class InstancesControllerUI extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // --- Presenter -------------------------------------------------------------
    private static class Presenter extends JToggleButton {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static ImageIcon ICON_INSTANCE = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/heapwalk/ui/resources/instance.png")); // NOI18N

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Presenter() {
            super();
            setText(VIEW_CAPTION);
            setToolTipText(VIEW_DESCR);
            setIcon(ICON_INSTANCE);
            setMargin(new java.awt.Insets(getMargin().top, getMargin().top, getMargin().bottom, getMargin().top));
        }
    }

    // --- Legend utils ----------------------------------------------------------
    private class LegendUpdater implements HierarchyListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void hierarchyChanged(HierarchyEvent e) {
            JPanel fieldsBrowserPanel = instancesController.getFieldsBrowserController().getPanel();
            JPanel referencesBrowserPanel = instancesController.getReferencesBrowserController().getPanel();

            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                legendPanel.setGCRootVisible(referencesBrowserPanel.isShowing());
                legendPanel.setVisible(fieldsBrowserPanel.isShowing() || referencesBrowserPanel.isShowing());
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NO_CLASS_DEFINED_MSG = NbBundle.getMessage(InstancesControllerUI.class,
                                                                           "InstancesControllerUI_NoClassDefinedMsg"); // NOI18N
    private static final String VIEW_CAPTION = NbBundle.getMessage(InstancesControllerUI.class,
                                                                   "InstancesControllerUI_ViewCaption"); // NOI18N
    private static final String VIEW_DESCR = NbBundle.getMessage(InstancesControllerUI.class, "InstancesControllerUI_ViewDescr"); // NOI18N
                                                                                                                                  // -----

    // --- UI definition ---------------------------------------------------------
    private static final String DATA = "Data"; // NOI18N
    private static final String NO_DATA = "No data"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AbstractButton presenter;
    private CardLayout contents;
    private InstancesController instancesController;
    private JPanel dataPanel;
    private JPanel noDataPanel;
    private JSplitPane browsersSplit;
    private JSplitPane contentsSplit;
    private LegendPanel legendPanel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public InstancesControllerUI(InstancesController instancesController) {
        this.instancesController = instancesController;

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

    // --- Public interface ------------------------------------------------------
    public void update() {
        if (contents != null) { // ui already initialized

            if (instancesController.getSelectedClass() == null) {
                contents.show(this, NO_DATA);
            } else {
                contents.show(this, DATA);
            }
        }
    }

    private void initComponents() {
        JPanel fieldsBrowserPanel = instancesController.getFieldsBrowserController().getPanel();
        JPanel referencesBrowserPanel = instancesController.getReferencesBrowserController().getPanel();
        JPanel instancesListPanel = instancesController.getInstancesListController().getPanel();

        browsersSplit = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT, fieldsBrowserPanel, referencesBrowserPanel);
        tweakSplitPaneUI(browsersSplit);
        browsersSplit.setResizeWeight(0.5d);

        contentsSplit = new JExtendedSplitPane(JSplitPane.HORIZONTAL_SPLIT, instancesListPanel, browsersSplit);
        tweakSplitPaneUI(contentsSplit);
        contentsSplit.setDividerLocation(instancesListPanel.getPreferredSize().width);

        JPanel classPresenterPanel = instancesController.getClassPresenterPanel();
        classPresenterPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 3, 0,
                                                                                                         getBackground()),
                                                                         classPresenterPanel.getBorder()));

        legendPanel = new LegendPanel(true);

        dataPanel = new JPanel(new BorderLayout());
        dataPanel.add(classPresenterPanel, BorderLayout.NORTH);
        dataPanel.add(contentsSplit, BorderLayout.CENTER);
        dataPanel.add(legendPanel, BorderLayout.SOUTH);

        noDataPanel = new JPanel(new BorderLayout());
        noDataPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        HTMLTextArea hintArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                instancesController.getHeapFragmentWalker().switchToClassesView();
            }
        };

        hintArea.setBorder(BorderFactory.createEmptyBorder(10, 8, 8, 8));

        String hintText = MessageFormat.format(NO_CLASS_DEFINED_MSG,
                                               new Object[] {
                                                   "<a href='#'><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/class.png'></a>"
                                               }); // NOI18N
        hintArea.setText(hintText);
        noDataPanel.add(hintArea, BorderLayout.CENTER);

        contents = new CardLayout();
        setLayout(contents);
        add(noDataPanel, NO_DATA);
        add(dataPanel, DATA);

        LegendUpdater legendUpdater = new LegendUpdater();
        fieldsBrowserPanel.addHierarchyListener(legendUpdater);
        referencesBrowserPanel.addHierarchyListener(legendUpdater);
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
