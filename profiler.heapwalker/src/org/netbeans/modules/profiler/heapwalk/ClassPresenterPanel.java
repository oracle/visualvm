/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

import java.awt.*;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker.StateEvent;
import org.openide.util.NbBundle;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ClassPresenterPanel_InstancesCountString=Instances: {0}",
    "ClassPresenterPanel_InstanceSizeString=Instance size: {0}",
    "ClassPresenterPanel_TotalSizeString=Total size: {0}",
    "ClassPresenterPanel_RetainedSizeString=Retained size: {0}",
    "ClassPresenterPanel_RetainedSizesString=Compute Retained Sizes"
})
public class ClassPresenterPanel extends JPanel implements HeapFragmentWalker.StateListener {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class HeaderRenderer extends JPanel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        private JLabel classIcon;
        private JLabel packageName;
        private JLabel className;
        
        
        HeaderRenderer() {
            setLayout(new BorderLayout());
            
            classIcon = new JLabel();
            classIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, classIcon.getIconTextGap()));
            packageName = new JLabel();
            packageName.setFont(packageName.getFont().deriveFont(Font.PLAIN));
            packageName.setBorder(BorderFactory.createEmptyBorder());
            className = new JLabel();
            className.setFont(packageName.getFont().deriveFont(Font.BOLD));
            className.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            
            add(classIcon, BorderLayout.WEST);
            add(packageName, BorderLayout.CENTER);
            add(className, BorderLayout.EAST);
        }
        
        public void setIcon(Icon icon) {
            classIcon.setIcon(icon);
        }

        public void setText(String text) {
            int classNameIndex = text.lastIndexOf('.'); // NOI18N

            if (classNameIndex == -1) {
                packageName.setText(""); // NOI18N
                className.setText(text);
            } else {
                classNameIndex++;
                packageName.setText(text.substring(0, classNameIndex));
                className.setText(text.substring(classNameIndex));
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Icon ICON_CLASS = Icons.getIcon(LanguageIcons.CLASS);

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
            String instancesCount = Bundle.ClassPresenterPanel_InstancesCountString(javaClass.getInstancesCount());
            String instanceSize = (javaClass.getInstanceSize() != -1)
                                  ? ("  |  " // NOI18N
                                  + Bundle.ClassPresenterPanel_InstanceSizeString(javaClass.getInstanceSize())) : ""; // NOI18N
            String allInstancesSize = (javaClass.getAllInstancesSize() != -1)
                                      ? ("  |  " // NOI18N
                                      + Bundle.ClassPresenterPanel_TotalSizeString(javaClass.getAllInstancesSize()))
                                      : ""; // NOI18N
            String classDetails = javaClass.isArray() ? (instancesCount + allInstancesSize)
                                                      : (instancesCount + instanceSize + allInstancesSize);
            if (heapFragmentWalker.getRetainedSizesStatus() == HeapFragmentWalker.RETAINED_SIZES_COMPUTED)
                classDetails += "  |  " + Bundle.ClassPresenterPanel_RetainedSizeString(javaClass.getRetainedSizeByClass()); // NOI18N
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
    
    public void refresh() {}

    public void stateChanged(StateEvent e) {
        updateActions(e.getRetainedSizesStatus());
    }

    public void updateActions(int retainedSizesStatus) {
        switch (retainedSizesStatus) {
            case HeapFragmentWalker.RETAINED_SIZES_UNSUPPORTED:
            case HeapFragmentWalker.RETAINED_SIZES_COMPUTED:
                actionsRenderer.setVisible(false);
                refresh();
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
        Color borderColor = UIManager.getLookAndFeel().getID().equals("Metal") ? // NOI18N
            UIManager.getColor("Button.darkShadow") : UIManager.getColor("Button.shadow"); // NOI18N
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor), BorderFactory.createEmptyBorder(2, 5, 2, 5)));
        setOpaque(true);
        setBackground(UIUtils.getDarker(UIUtils.getProfilerResultsBackground()));

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
                    BrowserUtils.performTask(new Runnable() {
                        public void run() {
                            heapFragmentWalker.computeRetainedSizes(true, true);
                        }
                    });
                }
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        actionsRenderer.setBorder(BorderFactory.createEmptyBorder());
        actionsRenderer.setForeground(UIManager.getColor("ToolTip.foreground")); // NOI18N
        actionsRenderer.setFont(UIManager.getFont("ToolTip.font")); // NOI18N
        actionsRenderer.setText("&nbsp;&nbsp;|&nbsp;&nbsp;<a href='#'>" + Bundle.ClassPresenterPanel_RetainedSizesString() + "</a>"); // NOI18N
        actionsRenderer.setOpaque(false);
        actionsRenderer.setVisible(false);

        JPanel detailsContainer = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        detailsContainer.setOpaque(false);
        detailsContainer.add(detailsRenderer);
        detailsContainer.add(actionsRenderer);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        add(headerRenderer, c);
        
        JPanel filler = new JPanel(null);
        filler.setOpaque(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(filler, c);
        
        c = new GridBagConstraints();
        c.gridx = 2;
        add(detailsContainer, c);
    }
}
