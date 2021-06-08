/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.modules.threadinspect;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import org.graalvm.visualvm.uisupport.UISupport;
import org.graalvm.visualvm.uisupport.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 */
final class ThreadsInspector extends JPanel implements DataRemovedListener<Application>,
                                                       PropertyChangeListener {

    private static final Color BACKGROUND;
    private static final Color ITEM_HIGHLIGHT;
    private static final Color SPLITTER_HIGHLIGHT;

    private static final int SPACING = getPresenterSpacing();

    private static final String EMPTY = "E"; // NOI18N
    private static final String DATA  = "D"; // NOI18N

    static {
        BACKGROUND = UISupport.getDefaultBackground();

        int darkerR = BACKGROUND.getRed() - 11;
        if (darkerR < 0) darkerR += 26;
        int darkerG = BACKGROUND.getGreen() - 11;
        if (darkerG < 0) darkerG += 26;
        int darkerB = BACKGROUND.getBlue() - 11;
        if (darkerB < 0) darkerB += 26;
        ITEM_HIGHLIGHT = new Color(darkerR, darkerG, darkerB);

        darkerR = BACKGROUND.getRed() - 20;
        if (darkerR < 0) darkerR += 40;
        darkerG = BACKGROUND.getGreen() - 20;
        if (darkerG < 0) darkerG += 40;
        darkerB = BACKGROUND.getBlue() - 20;
        if (darkerB < 0) darkerB += 40;
        SPLITTER_HIGHLIGHT = new Color(darkerR, darkerG, darkerB);
    }


    private final Application application;

    private Engine threadEngine;
    private Set<Long> selectedThreads;

    private JButton refreshButton;
    private JPanel threadsContainer;
    private JPanel threadsContainerContainer;
    private CardLayout detailsLayout;
    private JPanel detailsContainer;
    private HTMLTextArea threadsDetails;

    private Long focusedThreadId = Long.MIN_VALUE;

    private boolean internalDetailsChange;


    public ThreadsInspector(Application application) {
        this.application = application;

        initUI();
        showProgress();
        initThreads();
    }


    public void dataRemoved(Application application) {
        disableUI();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        disableUI();
    }

    
    private void initUI() {
        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
    }

    private void initThreads() {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                if (application.getState() != Stateful.STATE_AVAILABLE) {
                    showError("Application finished");
                } else {
                    threadEngine = Engine.getEngine(application);
                    if (threadEngine == null) {
                        showError("Cannot access threads using JMX.");
                    } else {
                        application.notifyWhenRemoved(ThreadsInspector.this);
                        application.addPropertyChangeListener(Stateful.PROPERTY_STATE,
                                    WeakListeners.propertyChange(ThreadsInspector.this,
                                    application));

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                createUI();
                                refreshData();
                            }
                        });
                    }
                }
            }
        });
    }

    private void refreshData() {
        if (!refreshButton.isEnabled()) return;
        
        if (application.getState() != Stateful.STATE_AVAILABLE) {
            disableUI();
            return;
        }

        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                final List<ThreadInfo> tinfs = threadEngine.getThreadInfos();
                if (tinfs == null) {
                    disableUI();
                    return;
                }

                if (selectedThreads == null) selectedThreads = new HashSet();

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        threadsContainer.removeAll();
                        List<Long> toDisplay = new ArrayList();
                        Set<Long> selectedZombies = new HashSet(selectedThreads);

                        for (ThreadInfo tinfo : tinfs) {
                            String name = tinfo.getThreadName();
                            final long id = tinfo.getThreadId();
                            selectedZombies.remove(id);
                            final JCheckBox cb = new JCheckBox(name, selectedThreads.
                                                         contains(id)) {
                                protected void fireActionPerformed(ActionEvent e) {
                                    focusedThreadId = id;
                                    if (!selectedThreads.remove(id))
                                        selectedThreads.add(id);
                                    refreshData();
                                }
                                public Dimension getPreferredSize() {
                                    Dimension size = super.getPreferredSize();
                                    size.height += SPACING;
                                    return size;
                                }
                            };
                            cb.setOpaque(false);

                            JPanel cbp = new JPanel(null) {
                                public Dimension getPreferredSize() {
                                    Dimension size = cb.getPreferredSize();
                                    size.width += 8;
                                    return size;
                                }
                                public void doLayout() {
                                    cb.setBounds(4, 0, getWidth() - 8, getHeight());
                                }
                                public void setEnabled(boolean enabled) {
                                    super.setEnabled(enabled);
                                    for (Component c : getComponents())
                                        c.setEnabled(enabled);
                                }
                            };
                            cbp.setOpaque(true);
                            cbp.setBackground(threadsContainer.getComponentCount() %
                                              2 == 0 ? BACKGROUND : ITEM_HIGHLIGHT);
                            cbp.add(cb, BorderLayout.CENTER);
                            threadsContainer.add(cbp);
                            
                            if (focusedThreadId == id) {
                                cb.requestFocusInWindow();
                                focusedThreadId = Long.MIN_VALUE;
                            }

                            if (cb.isSelected()) toDisplay.add(id);
                        }

                        selectedThreads.removeAll(selectedZombies);

                        // Workaround for JDK7 bug, JScrollPane doesn't layout
                        // correctly when in a not-selected JTabPane and updated
                        // lazily. Overriding isValidateRoot() on JScrollPane
                        // to return false also works around this problem.
                        threadsContainerContainer.invalidate();
                        threadsContainerContainer.validate();

                        if (!toDisplay.isEmpty()) displayStackTraces(toDisplay);
                        else showDetails(""); // NOI18N
                    }
                });
            }
        });
    }

    private void displayStackTraces(final List<Long> toDisplay) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                final String text = threadEngine.getStackTraces(toDisplay);
                if (text != null) SwingUtilities.invokeLater(new Runnable() {
                    public void run() { showDetails(text); }
                });
            }
        });
    }

    private void disableUI() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                refreshButton.setEnabled(false);
                threadsContainer.setEnabled(false);
                
                Component focused = KeyboardFocusManager.
                        getCurrentKeyboardFocusManager().getFocusOwner();
                if (focused != null && focused.getParent() == threadsContainer)
                        threadsDetails.requestFocusInWindow();
            }
        });
    }

    private void showProgress() {
        JLabel waitLabel = new JLabel("Resolving threads...", SwingConstants.CENTER);
        waitLabel.setEnabled(false);
        add(waitLabel, BorderLayout.CENTER);
    }

    private void showError(final String error) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                removeAll();
                add(new HTMLTextArea("<b>Unable to inspect threads.</b><br>" +
                    error), BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        });
    }

    private void showDetails(String text) {
        internalDetailsChange = true;
        threadsDetails.setText(text);
        
        if (text.isEmpty()) detailsLayout.show(detailsContainer, EMPTY);
        else detailsLayout.show(detailsContainer, DATA);
    }

    private void createUI() {
        JLabel hintLabel = new JLabel("<Select thread(s) to display stack traces>");
        hintLabel.setHorizontalAlignment(JLabel.CENTER);
        hintLabel.setOpaque(false);
        hintLabel.setEnabled(false);

        detailsLayout = new CardLayout();
        detailsContainer = new JPanel(detailsLayout);
        detailsContainer.setOpaque(false);
        detailsContainer.add(hintLabel, EMPTY);

        refreshButton = new JButton("Refresh") {
            protected void fireActionPerformed(ActionEvent e) { refreshData(); }
        };

        threadsContainer = new JPanel(new VerticalLayout(false)) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents()) c.setEnabled(enabled);
            }
        };
        threadsContainer.setOpaque(false);
        
        threadsDetails = new HTMLTextArea() {
            private Rectangle lastR;
            public void scrollRectToVisible(Rectangle r) {
                if (internalDetailsChange) internalDetailsChange = false;
                else if (!r.equals(lastR)) super.scrollRectToVisible(r);
                lastR = r;
            }
        };
        threadsDetails.setForeground(new Color(0xcc, 0x33, 0));
        detailsContainer.add(threadsDetails, DATA);
        showDetails(""); // NOI18N

        threadsContainerContainer = new JPanel(new BorderLayout(0, 5));
        threadsContainerContainer.setOpaque(false);
        threadsContainerContainer.add(new ScrollableContainer(threadsContainer), BorderLayout.CENTER);
        threadsContainerContainer.add(refreshButton, BorderLayout.SOUTH);

        final CustomizedSplitPaneUI detailsVerticalSplitterUI = new CustomizedSplitPaneUI();
        JExtendedSplitPane splitPane = new JExtendedSplitPane(JExtendedSplitPane.
                HORIZONTAL_SPLIT, threadsContainerContainer, new ScrollableContainer(detailsContainer)) {
            public void updateUI() {
                if (getUI() != detailsVerticalSplitterUI)
                    setUI(detailsVerticalSplitterUI);

                setBorder(null);
                setOpaque(false);
                setDividerSize(6);
                setContinuousLayout(true);

                final BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).
                                                       getDivider();
                divider.setBackground(BACKGROUND);
                divider.setBorder(null);

                divider.addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        divider.setBackground(SPLITTER_HIGHLIGHT);
                        divider.repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        divider.setBackground(BACKGROUND);
                        divider.repaint();
                    }
                });
            }
        };
        splitPane.setDividerLocation(250);

        removeAll();
        add(splitPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }


    private static int getPresenterSpacing() {
        if (UISupport.isNimbusLookAndFeel()) return 6;
        else if (UISupport.isGTKLookAndFeel()) return 4;
        else return 2;
    }
    
    private static class CustomizedSplitPaneUI extends BasicSplitPaneUI {
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
                public void paint(Graphics g) {
                    Dimension size = getSize();
                    g.setColor(getBackground());
                    g.fillRect(0, 0, size.width, size.height);
                }
            };
        }
    }

}
