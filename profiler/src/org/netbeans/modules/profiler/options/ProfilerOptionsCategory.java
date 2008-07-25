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

package org.netbeans.modules.profiler.options;

import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.ui.panels.ProfilerOptionsPanel;
import org.netbeans.spi.options.AdvancedOption;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import org.netbeans.lib.profiler.ui.UIUtils;


public class ProfilerOptionsCategory extends AdvancedOption {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class ProfilerPanelController extends OptionsPanelController {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static ProfilerOptionsPanel settingsPanel = null;
        private static JScrollPane optionsComponent = null;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isChanged() {
            return !settingsPanel.currentSettingsEquals(ProfilerIDESettings.getInstance());
        }

        public JComponent getComponent() {
            return getComponent(Lookup.getDefault());
        }

        public JComponent getComponent(Lookup lookup) {
            if (settingsPanel == null) {
                settingsPanel = new ProfilerOptionsPanel();
                optionsComponent = new JScrollPane(settingsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                optionsComponent.setBorder(BorderFactory.createEmptyBorder());
                optionsComponent.setViewportBorder(BorderFactory.createEmptyBorder());
                if (UIUtils.isGTKLookAndFeel()) {
                    // Must be transparent for GTK
                    optionsComponent.getViewport().setOpaque(false);
                    optionsComponent.setOpaque(false);
                } else {
                    // JTabbedPane (container) has other than Panel.background color on Metal, Windows, Aqua
                    optionsComponent.getViewport().setBackground(settingsPanel.getBackground());
                    optionsComponent.setBackground(settingsPanel.getBackground());
                }
            }

            return optionsComponent;
        }

        public HelpCtx getHelpCtx() {
            return new HelpCtx(NbBundle.getMessage(ProfilerOptionsCategory.class, "ProfilerOptionsCategory_Help")); // NOI18N
        }

        public boolean isValid() {
            return true;
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        public void applyChanges() {
            settingsPanel.applySettings(ProfilerIDESettings.getInstance());
        }

        public void cancel() {
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

        public void update() {
            settingsPanel.init(ProfilerIDESettings.getInstance());

            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JScrollBar vScrollBar = optionsComponent.getVerticalScrollBar();

                        if (vScrollBar != null) {
                            vScrollBar.setValue(0);
                        }

                        JScrollBar hScrollBar = optionsComponent.getHorizontalScrollBar();

                        if (hScrollBar != null) {
                            hScrollBar.setValue(0);
                        }
                    }
                });
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String OPTIONS_CATEGORY_NAME = NbBundle.getMessage(ProfilerOptionsCategory.class,
                                                                            "ProfilerOptionsCategory_OptionsCategoryName"); // NOI18N
    private static final String TITLE = NbBundle.getMessage(ProfilerOptionsCategory.class, "ProfilerOptionsCategory_Title"); // NOI18N
                                                                                                                             // -----

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getDisplayName() {
        return TITLE;
    }

    public String getTooltip() {
        return TITLE;
    }

    public OptionsPanelController create() {
        return new ProfilerPanelController();
    }
}
