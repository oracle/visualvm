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


import org.netbeans.lib.profiler.ui.components.JTitledPanel;
import org.netbeans.modules.profiler.heapwalk.HintsController;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.modules.profiler.ui.Utils;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class HintsControllerUI extends JTitledPanel {
    // -----
    // I18N String constants
    private static final String VIEW_TITLE_HINTS = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_ViewTitleHints"); // NOI18N
    private static final String FIND_BUTTON_TITLE = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_FindButton"); // NOI18N
    private static final String FIND_BUTTON_TOOLTIP = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_FindButtonTooltip"); // NOI18N
    private static final String LABEL1_STRING = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_Label1String"); // NOI18N
    private static final String LABEL2_STRING = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_Label2String"); // NOI18N
    
//    private static ImageIcon ICON_HINTS = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/heapwalk/ui/resources/suggestion.png", false); // NOI18N
    
    private static final Number OBJECTS_DEFAULT = 20;
    private static final int OBJECTS_MAX = 100;
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private HintsController hintsController;
    // --- UI definition ---------------------------------------------------------
    private JPanel hintsTextContainer;
    private JSpinner spinner;
    private JLabel textLabel1;
    private JLabel textLabel2;
    private JButton findButton;
    private HTMLTextArea dataArea;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    // --- Constructors ----------------------------------------------------------
    public HintsControllerUI(HintsController hintsController) {
        super(VIEW_TITLE_HINTS, Utils.FIND_ACTION_ICON, true);
        
        this.hintsController = hintsController;
       
        initComponents();
        
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    // --- Internal interface ----------------------------------------------------
    
    
    // --- Public interface ------------------------------------------------------
    public void setResult(String result) {
        dataArea.setText(result);
        findButton.setEnabled(true);
    }
    
    private void initComponents() {
        GridBagConstraints constraints;
        final int SPINNER_HEIGHT = new JTextField().getPreferredSize().height;
        
        setLayout(new BorderLayout());
        
        // hintsTextContainer
        hintsTextContainer = new JPanel(new GridBagLayout());
        hintsTextContainer.setOpaque(false);
        hintsTextContainer.setBorder(BorderFactory.createMatteBorder(5, 5, 0, 5,
                                        UIUtils.getProfilerResultsBackground()));

        // text
        textLabel1 = new JLabel(LABEL1_STRING);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(4, 5, 0, 5);
        hintsTextContainer.add(textLabel1, constraints);
        
        // Spinner
        spinner = new JExtendedSpinner(new SpinnerNumberModel(OBJECTS_DEFAULT, 1, OBJECTS_MAX, 1)) {
            public Dimension getPreferredSize() { return new Dimension(45, SPINNER_HEIGHT); }
            public Dimension getMinimumSize()   { return getPreferredSize(); }
        };
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(4, 0, 0, 0);
        hintsTextContainer.add(spinner, constraints);
        
        // text
        textLabel2 = new JLabel(LABEL2_STRING);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(4, 5, 0, 5);
        hintsTextContainer.add(textLabel2, constraints);

        // findButton
        findButton = new JButton(FIND_BUTTON_TITLE) {
            protected void fireActionPerformed(ActionEvent event) {
                findButton.setEnabled(false);
                int selectedValue = ((Number)spinner.getValue()).intValue();
                hintsController.computeBiggestObjects(selectedValue);
            }
        };
        findButton.setToolTipText(FIND_BUTTON_TOOLTIP);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(4, 5, 0, 5);
        hintsTextContainer.add(findButton, constraints);
        
        // Filler panel
        JPanel fillerPanel = new JPanel(null);
        fillerPanel.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        hintsTextContainer.add(fillerPanel, constraints);
        
        // dataArea
        dataArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                if (url == null) return;
                hintsController.showURL(url);
            }
        };
        
        JScrollPane dataAreaScrollPane = new JScrollPane(dataArea,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataAreaScrollPane.setBorder(BorderFactory.createMatteBorder(10, 5, 5, 5,
                                        UIUtils.getProfilerResultsBackground()));
        dataAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        dataAreaScrollPane.getHorizontalScrollBar().setUnitIncrement(10);



        JPanel contentsPanel = new JPanel();
        contentsPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, getTitleBorderColor()));
        contentsPanel.setLayout(new BorderLayout());
        contentsPanel.setOpaque(true);
        contentsPanel.setBackground(dataArea.getBackground());
        contentsPanel.add(hintsTextContainer, BorderLayout.NORTH);
        contentsPanel.add(dataAreaScrollPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(contentsPanel, BorderLayout.CENTER);

        // UI tweaks
        setBackground(dataArea.getBackground());
        
    }
    
    
    // --- Private implementation ------------------------------------------------
    

}
