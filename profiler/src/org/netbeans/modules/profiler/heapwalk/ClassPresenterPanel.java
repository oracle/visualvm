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

import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker.StateEvent;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.net.URL;
import java.text.MessageFormat;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.openide.util.RequestProcessor;


/**
 *
 * @author Jiri Sedlacek
 */
public class ClassPresenterPanel extends JPanel implements HeapFragmentWalker.StateListener {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class HeaderRenderer extends JLabel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Dimension getMinimumSize() {
            return new Dimension(0, super.getMinimumSize().height);
        }

        public void setText(String text) {
            int classNameIndex = text.lastIndexOf("."); // NOI18N

            if (classNameIndex == -1) {
                super.setText(text);
            } else {
                classNameIndex++;

                String htmlText = "<html>" // NOI18N
                                  + text.substring(0, classNameIndex) + "<b>" // NOI18N
                                  + text.substring(classNameIndex) + "</b>" // NOI18N
                                  + "</html>"; // NOI18N
                super.setText(htmlText);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String INSTANCES_COUNT_STRING = NbBundle.getMessage(ClassPresenterPanel.class,
                                                                             "ClassPresenterPanel_InstancesCountString"); // NOI18N
    private static final String INSTANCE_SIZE_STRING = NbBundle.getMessage(ClassPresenterPanel.class,
                                                                           "ClassPresenterPanel_InstanceSizeString"); // NOI18N
    private static final String TOTAL_SIZE_STRING = NbBundle.getMessage(ClassPresenterPanel.class,
                                                                        "ClassPresenterPanel_TotalSizeString"); // NOI18N
    private static final String RETAINED_SIZES_STRING = NbBundle.getMessage(ClassPresenterPanel.class,
                                                                        "ClassPresenterPanel_RetainedSizesString"); // NOI18N
                                                                                                                // -----
    private static ImageIcon ICON_CLASS = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/heapwalk/ui/resources/class.png", false); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HeaderRenderer headerRenderer;
    private JLabel detailsRenderer;
    private HTMLLabel actionsRenderer;

    private HeapFragmentWalker heapFragmentWalker;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ClassPresenterPanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setJavaClass(JavaClass javaClass) {
        if (javaClass != null) {
            String className = javaClass.getName();
            String instancesCount = MessageFormat.format(INSTANCES_COUNT_STRING, new Object[] { javaClass.getInstancesCount() });
            String instanceSize = (javaClass.getInstanceSize() != -1)
                                  ? ("  |  " // NOI18N
                                  + MessageFormat.format(INSTANCE_SIZE_STRING, new Object[] { javaClass.getInstanceSize() })) : ""; // NOI18N
            String allInstancesSize = (javaClass.getAllInstancesSize() != -1)
                                      ? ("  |  " // NOI18N
                                      + MessageFormat.format(TOTAL_SIZE_STRING, new Object[] { javaClass.getAllInstancesSize() }))
                                      : ""; // NOI18N
            String classDetails = javaClass.isArray() ? (instancesCount + allInstancesSize)
                                                      : (instancesCount + instanceSize + allInstancesSize);
            headerRenderer.setText(className);
            detailsRenderer.setText(classDetails);
            actionsRenderer.setPreferredSize(new Dimension(actionsRenderer.getPreferredSize().width,
                                                           detailsRenderer.getPreferredSize().height));
        }
    }


    public void setHeapFragmentWalker(HeapFragmentWalker heapFragmentWalker) {
        if (this.heapFragmentWalker != null) this.heapFragmentWalker.removeStateListener(this);
        this.heapFragmentWalker = heapFragmentWalker;
        if (this.heapFragmentWalker != null) {
            this.heapFragmentWalker.addStateListener(this);
            updateActions(heapFragmentWalker.getRetainedSizesStatus());
        } else {
            updateActions(HeapFragmentWalker.RETAINED_SIZES_UNSUPPORTED);
        }
    }

    public void stateChanged(StateEvent e) {
        updateActions(e.getRetainedSizesStatus());
    }

    public void updateActions(int retainedSizesStatus) {
        switch (retainedSizesStatus) {
            case HeapFragmentWalker.RETAINED_SIZES_UNSUPPORTED:
            case HeapFragmentWalker.RETAINED_SIZES_COMPUTED:
                actionsRenderer.setVisible(false);
                break;
            case HeapFragmentWalker.RETAINED_SIZES_UNKNOWN:
            case HeapFragmentWalker.RETAINED_SIZES_CANCELLED:
                actionsRenderer.setVisible(true);
                actionsRenderer.setEnabled(true);
                break;
            case HeapFragmentWalker.RETAINED_SIZES_COMPUTING:
                actionsRenderer.setVisible(true);
                actionsRenderer.setEnabled(false);
                break;
        }
    }


    private void initComponents() {
        setLayout(new BorderLayout());
        Color borderColor = UIManager.getLookAndFeel().getID().equals("Metal") ? // NOI18N
            UIManager.getColor("Button.darkShadow") : UIManager.getColor("Button.shadow"); // NOI18N
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        setOpaque(true);
        //    titlePanel.setBackground(UIManager.getColor("ToolTip.background"));
        setBackground(new Color(245, 245, 245));

        headerRenderer = new HeaderRenderer();
        headerRenderer.setIcon(ICON_CLASS);
        headerRenderer.setForeground(UIManager.getColor("ToolTip.foreground")); // NOI18N
        headerRenderer.setFont(UIManager.getFont("ToolTip.font")); // NOI18N
        headerRenderer.setOpaque(false);

        detailsRenderer = new JLabel();
        detailsRenderer.setForeground(UIManager.getColor("ToolTip.foreground")); // NOI18N
        detailsRenderer.setFont(UIManager.getFont("ToolTip.font")); // NOI18N
        detailsRenderer.setOpaque(false);

        actionsRenderer = new HTMLLabel() {
            protected void showURL(URL url) {
                if (heapFragmentWalker != null) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            heapFragmentWalker.computeRetainedSizes(true);
                        }
                    });
                }
            }
        };
        actionsRenderer.setBorder(BorderFactory.createEmptyBorder());
        actionsRenderer.setForeground(UIManager.getColor("ToolTip.foreground")); // NOI18N
        actionsRenderer.setFont(UIManager.getFont("ToolTip.font")); // NOI18N
        actionsRenderer.setText("&nbsp;&nbsp;|&nbsp;&nbsp;<a href='#'>" + RETAINED_SIZES_STRING + "</a>"); // NOI18N
        actionsRenderer.setOpaque(false);
        actionsRenderer.setVisible(false);

        JPanel detailsContainer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        detailsContainer.setOpaque(false);
        detailsContainer.add(detailsRenderer);
        detailsContainer.add(actionsRenderer);

        add(headerRenderer, BorderLayout.WEST);
        add(detailsContainer, BorderLayout.EAST);
    }
}
