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


import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.ui.SwingWorker;
import org.netbeans.lib.profiler.ui.components.JTitledPanel;
import org.netbeans.modules.profiler.heapwalk.HintsController;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class HintsControllerUI extends JTitledPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    
    // -----
    // I18N String constants
    private static final String VIEW_TITLE_HINTS = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_ViewTitleHints"); // NOI18N
    private static final String FIND_BUTTON_TITLE = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_FindButton"); // NOI18N
    private static final String FIND_BUTTON_TOOLTIP = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_FindButtonTooltip"); // NOI18N
    private static final String LABEL = NbBundle.getMessage(HintsControllerUI.class, "HintsControllerUI_Label"); // NOI18N
    private static ImageIcon ICON_HINTS = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/heapwalk/ui/resources/suggestion.png", false); // NOI18N
    private static final Number SPINNER_DEFAULT = 20;
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private HintsController hintsController;
    // --- UI definition ---------------------------------------------------------
    private JPanel hintsTextContainer;
    private JSpinner spinner;
    private JLabel textLabel;
    private JButton findButton;
    private HTMLTextArea dataArea;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    // --- Constructors ----------------------------------------------------------
    public HintsControllerUI(HintsController hintsController) {
        super(VIEW_TITLE_HINTS, ICON_HINTS, true);
        
        this.hintsController = hintsController;
        
        initComponents();
        
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    // --- Internal interface ----------------------------------------------------
    
    
    // --- Public interface ------------------------------------------------------
    public void update() {
        
    }
    
    private void initComponents() {
        GridBagConstraints constraints;
        final int SPINNER_HEIGHT = new JTextField().getPreferredSize().height;
        
        setLayout(new BorderLayout());
        
        // hintsTextContainer
        hintsTextContainer = new JPanel(new GridBagLayout());
        
        // Spinner
        spinner = new /*JExtendedSpinner*/JSpinner(new SpinnerNumberModel(SPINNER_DEFAULT, 1, 999, 1)) {
            public Dimension getPreferredSize() { return new Dimension(45, SPINNER_HEIGHT); }
            public Dimension getMinimumSize()   { return getPreferredSize(); }
        };
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 2, 3, 0);
        hintsTextContainer.add(spinner, constraints);
        
        // text
        textLabel = new JLabel(LABEL);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 4, 3, 0);
        hintsTextContainer.add(textLabel, constraints);
        
        // Filler panel
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 3, 0);
        hintsTextContainer.add(new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING)), constraints);
        
        // findButton
        findButton = new JButton(FIND_BUTTON_TITLE);
        findButton.setToolTipText(FIND_BUTTON_TOOLTIP);
        findButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int selectedValue = ((Number)spinner.getValue()).intValue();
                SwingWorker worker = new SwingWorker() {
                    String html;
                    
                    protected void doInBackground() {
                        int retainedSizesState = hintsController.getSummaryController().getHeapFragmentWalker().computeRetainedSizes(false);
                        if (retainedSizesState == HeapFragmentWalker.RETAINED_SIZES_COMPUTED) {
                            html = hintsController.findBiggestObjects(selectedValue);
                        } else {
                            html = "NO DATA";
                        }
                    }
                    
                    protected void done() {
                        dataArea.setText(html);
                        findButton.setEnabled(true);
                    }
                };
                worker.execute();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        findButton.setEnabled(false);
                    }
                });
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 10, 3, 5);
        hintsTextContainer.add(findButton, constraints);
        add(hintsTextContainer, BorderLayout.NORTH);
        
        // dataArea
        dataArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                if (url == null) return;
                hintsController.showURL(url);
            }
        };
        
        JScrollPane dataAreaScrollPane = new JScrollPane(dataArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataAreaScrollPane.setBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.setBackground(dataArea.getBackground());
        
        add(dataAreaScrollPane, BorderLayout.CENTER);
        
    }
    
    
    // --- Private implementation ------------------------------------------------
    

}
