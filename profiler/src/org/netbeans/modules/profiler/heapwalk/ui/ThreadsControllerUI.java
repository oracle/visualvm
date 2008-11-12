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

import java.awt.Color;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.util.ImageUtilities;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.heapwalk.ThreadsController;
import org.openide.util.RequestProcessor;


/**
 *
 * @author Jiri Sedlacek
 */
public class ThreadsControllerUI extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // --- Presenter -------------------------------------------------------------
    private static class Presenter extends JToggleButton {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static ImageIcon ICON_INSTANCE = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/threadsWindow.png")); // NOI18N

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Presenter() {
            super();
            setText(VIEW_CAPTION);
            setToolTipText(VIEW_DESCR);
            setIcon(ICON_INSTANCE);
            setMargin(new java.awt.Insets(getMargin().top, getMargin().top, getMargin().bottom, getMargin().top));
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String VIEW_CAPTION = "Threads"; // NOI18N
    private static final String VIEW_DESCR = "Threads"; // NOI18N
    // -----                                                                                 

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AbstractButton presenter;
    private ThreadsController threadsController;
    private HTMLTextArea threadsDumpArea;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public ThreadsControllerUI(ThreadsController threadsController) {
        this.threadsController = threadsController;

        initComponents();
        resolveThreads();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public AbstractButton getPresenter() {
        if (presenter == null) {
            presenter = new Presenter();
        }

        return presenter;
    }

    // --- Private implementation ------------------------------------------------------
    
    private void resolveThreads() {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                final String stackTrace = threadsController.getStackTrace();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String header = "<nobr><b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/resources/threadsWindow.png'>&nbsp;&nbsp;"
                             + "<span style=\"color:black\">Threads at the heap dump:</span></b><br><hr><br></nobr>";
                        threadsDumpArea.setForeground(new Color(0xcc, 0x33, 0));
                        threadsDumpArea.setText(header + stackTrace);
                        try { threadsDumpArea.setCaretPosition(0); } catch (Exception e) {}
                    }
                });
            }
        });
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // Top separator
        JSeparator separator = new JSeparator() {
            public Dimension getMaximumSize() {
                return new Dimension(super.getMaximumSize().width, 1);
            }

            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 1);
            }
        };

        separator.setBackground(getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(separator, constraints);

        // threads dump area
        threadsDumpArea = new HTMLTextArea()  {
            protected void showURL(URL url) {
                threadsController.showURL(url);
            }
        };
        threadsDumpArea.setText("<nobr><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/progress.png'>&nbsp;"
                             + "Resolving threads from the heap dump...</nobr>"); // NOI18N
        JScrollPane dataAreaScrollPane = new JScrollPane(threadsDumpArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataAreaScrollPane.setBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.setBackground(threadsDumpArea.getBackground());
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 0, 5);
        add(dataAreaScrollPane, constraints);
        
        // UI tweaks
        setBackground(threadsDumpArea.getBackground());
    }
}
