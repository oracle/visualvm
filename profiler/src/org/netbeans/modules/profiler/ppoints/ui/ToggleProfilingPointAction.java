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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.ui.components.ThinBevelBorder;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ppoints.CodeProfilingPoint;
import org.netbeans.modules.profiler.ppoints.GlobalProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPointFactory;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.netbeans.modules.profiler.ppoints.Utils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.util.Utilities;


/**
 *
 * @author Jiri Sedlacek
 */
public class ToggleProfilingPointAction extends AbstractAction implements AWTEventListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class ProfilingPointsSwitcher extends JFrame {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final String NO_ACTION_NAME = CANCEL_STRING;
        private static final Icon NO_ACTION_ICON = null;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Dimension size;
        private JLabel label;
        private JPanel previewPanel;
        private ProfilingPointFactory ppFactory;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProfilingPointsSwitcher() {
            super(SWITCHER_WINDOW_CAPTION);
            initProperties();
            initComponents();
            setProfilingPointFactory(null, -1);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setProfilingPointFactory(ProfilingPointFactory ppFactory, int index) {
            this.ppFactory = ppFactory;

            if (ppFactory != null) {
                label.setText(ppFactory.getType());
                label.setIcon(ppFactory.getIcon());
            } else {
                label.setText(NO_ACTION_NAME);
                label.setIcon(NO_ACTION_ICON);
            }

            Component selected = null;

            if ((index >= 0) && (index < previewPanel.getComponentCount())) {
                selected = previewPanel.getComponent(index);
            }

            for (Component c : previewPanel.getComponents()) {
                if (c == selected) {
                    Border empt1 = BorderFactory.createEmptyBorder(2, 2, 2, 2);
                    Border sel = BorderFactory.createMatteBorder(1, 1, 1, 1, SystemColor.textHighlight);
                    Border empt2 = BorderFactory.createEmptyBorder(0, 2, 0, 2);
                    Border comp1 = BorderFactory.createCompoundBorder(empt2, sel);
                    Border comp2 = BorderFactory.createCompoundBorder(comp1, empt1);
                    ((JComponent) c).setBorder(comp2);
                } else {
                    ((JComponent) c).setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
                }
            }
        }

        public ProfilingPointFactory getProfilingPointFactory() {
            return ppFactory;
        }

        public void setVisible(boolean visible) {
            if (visible) {
                if (size == null) {
                    size = getSize();
                }

                TopComponent editor = WindowManager.getDefault().getRegistry().getActivated();
                Rectangle b = editor.getBounds();
                Point location = new Point((b.x + (b.width / 2)) - (size.width / 2), (b.y + (b.height / 2)) - (size.height / 2));
                SwingUtilities.convertPointToScreen(location, editor);
                setLocation(location);
            }

            super.setVisible(visible);
        }

        private void initComponents() {
            setLayout(new BorderLayout());

            previewPanel = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
            previewPanel.setBorder(BorderFactory.createEmptyBorder(4, 7, 2, 7));

            label = new JLabel();
            label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 7, 7, 7),
                                                               new ThinBevelBorder(BevelBorder.LOWERED)));
            label.setBorder(BorderFactory.createCompoundBorder(label.getBorder(), BorderFactory.createEmptyBorder(4, 3, 4, 3)));
            label.setFont(label.getFont().deriveFont(Font.BOLD));

            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createRaisedBevelBorder());
            p.add(previewPanel, BorderLayout.NORTH);
            p.add(label, BorderLayout.CENTER);

            add(p, BorderLayout.CENTER);
        }

        private void initPanel(ProfilingPointFactory[] ppFactories) {
            Dimension prefSize = new Dimension(230, 0);

            for (int i = 0; i < ppFactories.length; i++) {
                JLabel previewIcon = new JLabel(ppFactories[i].getIcon());
                previewIcon.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
                previewPanel.add(previewIcon);

                setProfilingPointFactory(ppFactories[i], i);
                pack();

                Dimension currPrefSize = getPreferredSize();
                prefSize = new Dimension(Math.max(prefSize.width, currPrefSize.width),
                                         Math.max(prefSize.height, currPrefSize.height));
            }

            setProfilingPointFactory(null, ppFactories.length);
            setSize(prefSize);
        }

        private void initProperties() {
            setAlwaysOnTop(true);
            setUndecorated(true);
            setResizable(false);
            WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
                public void run() { setIconImage(WindowManager.getDefault().getMainWindow().getIconImage()); }
            });
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ACTION_NAME = NbBundle.getMessage(ToggleProfilingPointAction.class,
                                                                  "ToggleProfilingPointAction_ActionName"); // NOI18N
    private static final String ACTION_DESCR = NbBundle.getMessage(ToggleProfilingPointAction.class,
                                                                   "ToggleProfilingPointAction_ActionDescr"); // NOI18N
    private static final String PROFILING_PROGRESS_MSG = NbBundle.getMessage(ToggleProfilingPointAction.class,
                                                                             "ToggleProfilingPointAction_ProfilingProgressMsg"); // NOI18N
    private static final String BAD_SOURCE_MSG = NbBundle.getMessage(ToggleProfilingPointAction.class,
                                                                     "ToggleProfilingPointAction_BadSourceMsg"); // NOI18N
    private static final String CANCEL_STRING = NbBundle.getMessage(ToggleProfilingPointAction.class,
                                                                    "ToggleProfilingPointAction_CancelString"); // NOI18N
    private static final String SWITCHER_WINDOW_CAPTION = NbBundle.getMessage(ToggleProfilingPointAction.class,
                                                                              "ToggleProfilingPointAction_SwitcherWindowCaption"); // NOI18N
    private static final String INVALID_SHORTCUT_MSG = NbBundle.getMessage(ToggleProfilingPointAction.class,
                                                                              "ToggleProfilingPointAction_InvalidShortcutMsg"); // NOI18N
                                                                                                                                   // -----
    
    private static ToggleProfilingPointAction instance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ProfilingPointsSwitcher ppSwitcher;
    private ProfilingPointFactory[] ppFactories;
    private boolean warningDialogOpened = false;
    private int currentFactory;

    private KeyStroke acceleratorKeyStroke;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ToggleProfilingPointAction() {
        putValue(Action.NAME, ACTION_NAME);
        putValue(Action.SHORT_DESCRIPTION, ACTION_DESCR);
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }
    
    public static synchronized ToggleProfilingPointAction getInstance() {
        if (instance == null) instance = new ToggleProfilingPointAction();
        return instance;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isEnabled() {
        return true;
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        acceleratorKeyStroke = Utilities.stringToKey(e.getActionCommand());
        if (acceleratorKeyStroke == null || acceleratorKeyStroke.getModifiers() == 0) {
            NetBeansProfiler.getDefaultNB().displayError(MessageFormat.format(INVALID_SHORTCUT_MSG, new Object[] { ACTION_NAME }));
            return;
        }
        
        if (warningDialogOpened) {
            return;
        }

        if (ProfilingPointsManager.getDefault().isProfilingSessionInProgress()) {
            warningDialogOpened = true;
            NetBeansProfiler.getDefaultNB().displayWarning(PROFILING_PROGRESS_MSG);
            warningDialogOpened = false;

            return;
        }

        if (Utils.getCurrentLocation(0).equals(CodeProfilingPoint.Location.EMPTY)) {
            warningDialogOpened = true;
            NetBeansProfiler.getDefaultNB().displayWarning(BAD_SOURCE_MSG);
            warningDialogOpened = false;

            return;
        }
        
        ProfilingPointsSwitcher chooserFrame = getChooserFrame();

        if (chooserFrame.isVisible()) {
            nextFactory();
            chooserFrame.setProfilingPointFactory((currentFactory == ppFactories.length) ? null : ppFactories[currentFactory],
                                                  currentFactory);
        } else {
            if (currentlyInEditor()) {
                resetFactories();
                chooserFrame.setProfilingPointFactory((currentFactory == ppFactories.length) ? null : ppFactories[currentFactory],
                                                      currentFactory);
                Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
                chooserFrame.setVisible(true);
            }
        }
    }

    public void eventDispatched(AWTEvent event) {
        if (!(event instanceof KeyEvent)) return;
        
        KeyStroke eventKeyStroke = KeyStroke.getKeyStrokeForEvent((KeyEvent)event);
        if (acceleratorKeyStroke == null || eventKeyStroke == null) return;
        
        int acceleratorModifiers = acceleratorKeyStroke.getModifiers();
        if (acceleratorModifiers == 0) return;
        
        if (acceleratorModifiers != eventKeyStroke.getModifiers()) modifierKeyStateChanged();
    }

    private boolean currentlyInEditor() {
        // Get focused TopComponent
        TopComponent top1 = WindowManager.getDefault().getRegistry().getActivated();

        if (top1 == null) {
            return false;
        }

        // Get most active editor
        JTextComponent editor = EditorRegistry.lastFocusedComponent();

        if (editor == null) {
            return false;
        }

        // Check if Java source
        Document document = editor.getDocument();

        if (document == null) {
            return false;
        }

        FileObject fileObject = NbEditorUtilities.getFileObject(document);

        if ((fileObject == null) || !fileObject.getExt().equalsIgnoreCase("java")) {
            return false; // NOI18N
        }

        // Get editor TopComponent
        TopComponent top2 = NbEditorUtilities.getOuterTopComponent(editor);

        if (top2 == null) {
            return false;
        }

        // Return whether focused TopComponent == editor TopComponent
        return top1 == top2;
    }
    
    private synchronized ProfilingPointsSwitcher getChooserFrame() {
        if (ppSwitcher == null) {
            ppSwitcher = new ProfilingPointsSwitcher();
            ppSwitcher.addWindowListener(new WindowAdapter() {
                public void windowDeactivated(WindowEvent event) {
                    ppSwitcher.setVisible(false);
                }
            });
        }
        
        return ppSwitcher;
    }

    private synchronized void modifierKeyStateChanged() {
        if (ProfilingPointsManager.getDefault().isProfilingSessionInProgress()) {
            return;
        }

        ProfilingPointsSwitcher chooserFrame = getChooserFrame();

        if (chooserFrame.isVisible()) {
            ProfilingPointFactory ppFactory = chooserFrame.getProfilingPointFactory();
            Project project = Utils.getCurrentProject();

            if ((ppFactory != null) && (project != null)) {
                ProfilingPoint ppoint = ppFactory.create(project);

                if (ppoint != null) {
                    ProfilingPointsManager.getDefault().addProfilingPoint(ppoint);

                    if (ppoint instanceof GlobalProfilingPoint) {
                        SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    if (!ProfilingPointsWindow.getDefault().isOpened()) {
                                        ProfilingPointsWindow.getDefault().open();
                                        ProfilingPointsWindow.getDefault().requestVisible();
                                    }
                                }
                            });
                    }

                    ppoint.customize();
                }
            }
        }

        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        chooserFrame.setVisible(false);
    }

    private void nextFactory() {
        currentFactory++;

        if (currentFactory > ppFactories.length) {
            currentFactory = 0;
        }
    }

    private void resetFactories() {
        if (ppFactories == null) {
            ppFactories = ProfilingPointsManager.getDefault().getProfilingPointFactories();
            getChooserFrame().initPanel(ppFactories);
        }

        currentFactory = 0;
    }
}
