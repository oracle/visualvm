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

package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.BorderLayout;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JTitledPanel;
import org.netbeans.modules.profiler.heapwalk.OverviewController;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.net.URL;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class OverviewControllerUI extends JTitledPanel {
    
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    
    private static ImageIcon ICON_INFO = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/infoTab.png", false); // NOI18N
    private static final String VIEW_TITLE = NbBundle.getMessage(OverviewControllerUI.class, "OverviewControllerUI_ViewTitle"); // NOI18N
    private static final String VIEW_DESCR = NbBundle.getMessage(OverviewControllerUI.class, "OverviewControllerUI_ViewDescr"); // NOI18N
    private static final String IN_PROGRESS_MSG = NbBundle.getMessage(OverviewControllerUI.class, "OverviewControllerUI_InProgressMsg"); // NOI18N
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private AbstractButton presenter;
    private HTMLTextArea dataArea;
    
    // --- UI definition ---------------------------------------------------------
    private OverviewController overviewController;
    
    // --- Private implementation ------------------------------------------------
    private Instance instanceToSelect;
    private boolean showSysprops = false;
    private boolean showThreads = false;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    // --- Constructors ----------------------------------------------------------
    public OverviewControllerUI(OverviewController controller) {
        super(VIEW_TITLE,ICON_INFO,true);
        overviewController = controller;
        initComponents();
        refreshSummary();
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    // --- Public interface ------------------------------------------------------

    public void showInThreads(Instance instance) {
        if (!showThreads) {
            showThreads = true;
            instanceToSelect = instance;
            refreshSummary();
            return;
        }
        String referenceId = String.valueOf(instance.getInstanceId());
        
        dataArea.scrollToReference(referenceId);
        Document d = dataArea.getDocument();
        HTMLDocument doc = (HTMLDocument) d;
        HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
        for (; iter.isValid(); iter.next()) {
            AttributeSet a = iter.getAttributes();
            String nm = (String) a.getAttribute(HTML.Attribute.NAME);
            if ((nm != null) && nm.equals(referenceId)) {
                dataArea.select(iter.getStartOffset(),iter.getEndOffset());
                dataArea.requestFocusInWindow();
            }
        }
    }

    private void refreshSummary() {
        if (!showSysprops && !showThreads) {
            dataArea.setText(IN_PROGRESS_MSG);
        }
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                String summary = "<nobr>" + overviewController.computeSummary() + "</nobr>"; // NOI18N
                String environment = "<nobr>" + overviewController.computeEnvironment() + "</nobr>"; // NOI18N
                String properties = "<nobr>" + overviewController.computeSystemProperties(showSysprops) + "</nobr>"; // NOI18N
                String threads = "<nobr>" + overviewController.computeThreads(showThreads) + "</nobr>"; // NOI18N
                final String dataAreaText = summary + "<br><br>" // NOI18N
                        + environment + "<br><br>" // NOI18N
                        + properties + "<br><br>" // NOI18N
                        + threads;
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dataArea.setText(dataAreaText);
                        if (instanceToSelect != null) {
                            showInThreads(instanceToSelect);
                            instanceToSelect = null;
                        } else {
                            dataArea.setCaretPosition(0);
                        }
                    }
                });
            }
        });
    }
        
    private void initComponents() {
        // dataArea
        dataArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                if (url == null) return;
                String urls = url.toString();
                if (urls.equals(OverviewController.SHOW_SYSPROPS_URL)) {
                    showSysprops = true;
                    refreshSummary();
                } else if (urls.equals(OverviewController.SHOW_THREADS_URL)) {
                    showThreads = true;
                    refreshSummary();
                } else {
                    overviewController.showURL(urls);
                }
            }
        };
        dataArea.setSelectionColor(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        JScrollPane dataAreaScrollPane = new JScrollPane(dataArea,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataAreaScrollPane.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5,
                                        UIUtils.getProfilerResultsBackground()));
        dataAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        dataAreaScrollPane.getHorizontalScrollBar().setUnitIncrement(10);

        JPanel contentsPanel = new JPanel();
        contentsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, getTitleBorderColor()));
        contentsPanel.setLayout(new BorderLayout());
        contentsPanel.setOpaque(true);
        contentsPanel.setBackground(dataArea.getBackground());
        contentsPanel.add(dataAreaScrollPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(contentsPanel, BorderLayout.CENTER);
        
        // UI tweaks
        setBackground(dataArea.getBackground());
    }

}
