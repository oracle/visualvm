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

package org.netbeans.modules.profiler.ppoints;

import org.netbeans.api.project.Project;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ppoints.ui.ValidityAwarePanel;
import org.netbeans.modules.profiler.ppoints.ui.ValidityListener;
import org.netbeans.modules.profiler.ppoints.ui.WizardPanel1UI;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.Component;
import java.awt.Dimension;
import java.text.MessageFormat;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;


/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilingPointWizard implements WizardDescriptor.Iterator {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // --- WizardPanel for selecting Profiling Point type & Project --------------
    class WizardPanel1 extends WizardPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Project selectedProjectRef;
        private int selectedPPFactoryIndexRef;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public HelpCtx getHelp() {
            Component customizer = getComponent();

            if ((customizer == null) || !(customizer instanceof HelpCtx.Provider)) {
                return null;
            }

            return ((HelpCtx.Provider) customizer).getHelpCtx();
        }

        public String getName() {
            return WIZARD_STEP1_CAPTION;
        }

        public Component createComponent() {
            WizardPanel1UI component = new WizardPanel1UI();
            component.addValidityListener(this);
            component.init(ppFactories);
            setValid(component.areSettingsValid());

            return component;
        }

        public void hiding() {
            selectedPPFactoryIndex = ((WizardPanel1UI) getComponent()).getSelectedIndex();

            if (selectedPPFactoryIndex != selectedPPFactoryIndexRef) {
                settingsChanged = true;
            }

            selectedProject = ((WizardPanel1UI) getComponent()).getSelectedProject();

            if ((selectedProject == null) || !selectedProject.equals(selectedProjectRef)) {
                settingsChanged = true;
            }
        }

        public void showing() {
            selectedPPFactoryIndexRef = selectedPPFactoryIndex;
            ((WizardPanel1UI) getComponent()).setSelectedIndex(selectedPPFactoryIndex);
            selectedProjectRef = selectedProject;

            if (selectedProject == null) {
                selectedProject = Utils.getCurrentProject();
            }

            ((WizardPanel1UI) getComponent()).setSelectedProject(selectedProject);
        }
    }

    // --- WizardPanel for customizing Profiling Point properties ----------------
    class WizardPanel2 extends WizardPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private ValidityAwarePanel customizer;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public HelpCtx getHelp() {
            if ((customizer == null) || !(customizer instanceof HelpCtx.Provider)) {
                return null;
            }

            return ((HelpCtx.Provider) customizer).getHelpCtx();
        }

        public String getName() {
            return WIZARD_STEP2_CAPTION;
        }

        public Component createComponent() {
            JScrollPane customizerScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            customizerScrollPane.setBorder(BorderFactory.createEmptyBorder());
            customizerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
            customizerScrollPane.setOpaque(false);
            customizerScrollPane.getViewport().setOpaque(false);

            return customizerScrollPane;
        }

        public void hiding() {
            if ((profilingPoint != null) && (customizer != null)) {
                profilingPoint.setValues(customizer);
            }

            unregisterCustomizerListener();
        }

        public void notifyClosed() {
            releaseCurrentCustomizer();
            profilingPoint = null;
        }

        public void showing() {
            if ((customizer == null) || settingsChanged) {
                releaseCurrentCustomizer();
                createNewCustomizer();
                settingsChanged = false;
            }

            setValid(customizer.areSettingsValid());
            registerCustomizerListener();
        }

        private void createNewCustomizer() {
            // TODO: selectedPPFactoryIndex or selectedProject could be -1/null, create() can return null
            profilingPoint = ppFactories[selectedPPFactoryIndex].create(selectedProject);
            customizer = profilingPoint.getCustomizer();
            ((JScrollPane) getComponent()).setViewportView(customizer);
        }

        private void registerCustomizerListener() {
            if (customizer != null) {
                customizer.addValidityListener(this);
            }
        }

        private void releaseCurrentCustomizer() {
            ((JScrollPane) getComponent()).setViewportView(null);
            customizer = null;
        }

        private void unregisterCustomizerListener() {
            if (customizer != null) {
                customizer.removeValidityListener(this);
            }
        }
    }

    // --- Abstract WizardPanel implementation -----------------------------------
    private abstract class WizardPanel implements WizardDescriptor.Panel, ValidityListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        protected boolean valid = true;
        private Component component;
        private EventListenerList listenerList = new EventListenerList();

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public abstract String getName();

        public Component getComponent() {
            if (component == null) {
                component = createComponent();
                component.setName(getName());
                component.setPreferredSize(preferredPanelSize);
            }

            return component;
        }

        public void setValid(boolean valid) {
            if (this.valid != valid) {
                this.valid = valid;
                fireChangeListenerStateChanged(this);
            }

            ;
        }

        public boolean isValid() {
            return valid;
        }

        public abstract Component createComponent();

        /** Registers ChangeListener to receive events.
         * @param listener The listener to register.
         */
        public synchronized void addChangeListener(ChangeListener listener) {
            listenerList.add(ChangeListener.class, listener);
        }

        public void hiding() {
        }

        public void notifyClosed() {
        }

        public void readSettings(Object settings) {
        }

        /** Removes ChangeListener from the list of listeners.
         * @param listener The listener to remove.
         */
        public synchronized void removeChangeListener(ChangeListener listener) {
            listenerList.remove(ChangeListener.class, listener);
        }

        public void showing() {
        }

        public void storeSettings(Object settings) {
        }

        public void validityChanged(boolean isValid) {
            setValid(isValid);
        }

        /** Notifies all registered listeners about the event.
         *
         * @param param Parameter #1 of the <CODE>ChangeEvent<CODE> constructor.
         */
        protected void fireChangeListenerStateChanged(Object param) {
            ChangeEvent e = null;
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == ChangeListener.class) {
                    if (e == null) {
                        e = new ChangeEvent(param);
                    }

                    ((ChangeListener) listeners[i + 1]).stateChanged(e);
                }
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ANOTHER_PP_EDITED_MSG = NbBundle.getMessage(ProfilingPointWizard.class,
                                                                            "ProfilingPointWizard_AnotherPpEditedMsg"); // NOI18N
    private static final String NO_PPS_FOUND_MSG = NbBundle.getMessage(ProfilingPointWizard.class,
                                                                       "ProfilingPointWizard_NoPpsFoundMsg"); // NOI18N
    private static final String WIZARD_TITLE = NbBundle.getMessage(ProfilingPointWizard.class, "ProfilingPointWizard_WizardTitle"); // NOI18N
    private static final String WIZARD_STEP1_CAPTION = NbBundle.getMessage(ProfilingPointWizard.class,
                                                                           "ProfilingPointWizard_WizardStep1Caption"); // NOI18N
    private static final String WIZARD_STEP2_CAPTION = NbBundle.getMessage(ProfilingPointWizard.class,
                                                                           "ProfilingPointWizard_WizardStep2Caption"); // NOI18N
                                                                                                                       // -----
    private static ProfilingPointWizard defaultInstance;
    private static final Dimension DEFAULT_PREFERRED_PANEL_SIZE = new Dimension(440, 330);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Dimension preferredPanelSize = null;
    private ProfilingPoint profilingPoint;
    private Project selectedProject;
    private WizardDescriptor wizardDescriptor;

    // --- Wizard runtime implementation -----------------------------------------
    private ProfilingPointFactory[] ppFactories;
    private WizardPanel[] wizardPanels;
    private String[] wizardSteps;
    private boolean settingsChanged;
    private int currentPanel;
    private int selectedPPFactoryIndex;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ProfilingPointWizard() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ProfilingPointWizard getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new ProfilingPointWizard();
        }

        return defaultInstance;
    }

    public WizardDescriptor getWizardDescriptor() {
        return getWizardDescriptor(null);
    }

    public WizardDescriptor getWizardDescriptor(Project project) {
        ValidityAwarePanel showingCustomizer = ProfilingPointsManager.getDefault().getShowingCustomizer();

        if (showingCustomizer != null) {
            NetBeansProfiler.getDefaultNB().displayWarningAndWait(ANOTHER_PP_EDITED_MSG);
            SwingUtilities.getWindowAncestor(showingCustomizer).requestFocus();
            showingCustomizer.requestFocusInWindow();

            return null;
        } else {
            //      profilingPoint = null;
            settingsChanged = true;
            currentPanel = 0;
            selectedPPFactoryIndex = 0;
            selectedProject = project;
            initWizardDescriptor();
            initWizardPanels();

            if (ppFactories.length > 0) {
                getCurrentWizardPanel().showing();

                return wizardDescriptor;
            } else {
                NetBeansProfiler.getDefaultNB().displayError(NO_PPS_FOUND_MSG);

                return null;
            }
        }
    }

    // --- WizardDescriptor.Iterator implementation ------------------------------
    public synchronized void addChangeListener(javax.swing.event.ChangeListener listener) {
    }

    public WizardDescriptor.Panel current() {
        return getCurrentWizardPanel();
    }

    public ProfilingPoint finish() {
        ProfilingPoint result = profilingPoint;

        if (wizardPanels != null) {
            wizardPanels[currentPanel].hiding();

            for (int i = 0; i < wizardPanels.length; i++) {
                wizardPanels[i].notifyClosed(); // Will invoke profilingPoint = null
            }

            preferredPanelSize = wizardPanels[currentPanel].getComponent().getSize(); // Persist customized size
        }

        return result;
    }

    public boolean hasNext() {
        return currentPanel < (wizardSteps.length - 1);
    }

    public boolean hasPrevious() {
        return currentPanel > 0;
    }

    public String name() {
        return getCurrentWizardPanel().getName();
    }

    public void nextPanel() {
        getCurrentWizardPanel().hiding();
        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(++currentPanel)); // NOI18N
        getCurrentWizardPanel().showing();
    }

    public void previousPanel() {
        getCurrentWizardPanel().hiding();
        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(--currentPanel)); // NOI18N
        getCurrentWizardPanel().showing();
    }

    public synchronized void removeChangeListener(javax.swing.event.ChangeListener listener) {
    }

    private WizardPanel getCurrentWizardPanel() {
        return wizardPanels[currentPanel];
    }

    private void initWizardDescriptor() {
        wizardDescriptor = new WizardDescriptor(this);
        wizardDescriptor.setTitle(WIZARD_TITLE);
        wizardDescriptor.setTitleFormat(new MessageFormat("{0}")); // NOI18N

        wizardDescriptor.putProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, Boolean.TRUE); // NOI18N
        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, Boolean.TRUE); // NOI18N
        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, Boolean.TRUE); // NOI18N
        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(0)); // NOI18N
    }

    private void initWizardPanels() {
        ppFactories = ProfilingPointsManager.getDefault().getProfilingPointFactories();
        wizardPanels = new WizardPanel[] { new WizardPanel1(), new WizardPanel2() };
        wizardSteps = new String[wizardPanels.length];

        for (int i = 0; i < wizardPanels.length; i++) {
            wizardSteps[i] = wizardPanels[i].getName();
        }

        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_DATA, wizardSteps); // NOI18N

        if (preferredPanelSize == null) {
            preferredPanelSize = new Dimension(DEFAULT_PREFERRED_PANEL_SIZE);

            Dimension firstPanelSize = ((WizardPanel1UI) (wizardPanels[0].getComponent())).getMinSize();
            preferredPanelSize.width = Math.max(preferredPanelSize.width, firstPanelSize.width);
            preferredPanelSize.height = Math.max(preferredPanelSize.height, firstPanelSize.height);
        }
    }
}
