/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl;

import com.sun.tools.visualvm.modules.tracer.impl.swing.TransparentToolBar;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProgressObject;
import com.sun.tools.visualvm.modules.tracer.impl.options.TracerOptions;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HorizontalLayout;
import com.sun.tools.visualvm.modules.tracer.impl.swing.SimpleSeparator;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerView extends DataSourceView {

    private static final int INDETERMINATE_PROGRESS_THRESHOLD =
                Integer.getInteger("visualvm.tracer.indeterminateProgressThreshold", 2500); // NOI18N

    private static final String IMAGE_PATH =
            "com/sun/tools/visualvm/modules/tracer/impl/resources/tracer.png"; // NOI18N

    private final TracerModel model;
    private final TracerController controller;

    private DataViewComponent dvc;
    private TimelineView timelineView;

    
    TracerView(TracerModel model, TracerController controller) {
        super(model.getDataSource(), "Tracer", new ImageIcon(
              ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 100, false);
        this.model = model;
        this.controller = controller;
    }


    // --- DataSourceView implementation ---------------------------------------

    protected DataViewComponent createComponent() {
        MasterViewSupport masterView = new MasterViewSupport();
        dvc = new DataViewComponent(masterView.getView(),
                new DataViewComponent.MasterViewConfiguration(false));

        dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.33, 0, 0.33, 0, 0.5, 0.5));

        String initiallyOpened = TracerOptions.getInstance().getInitiallyOpened();

        PackagesView packagesView = new PackagesView(model, controller);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Probes", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(packagesView.getView(), DataViewComponent.TOP_LEFT);
        if (!initiallyOpened.contains(TracerOptions.VIEW_PROBES))
            dvc.hideDetailsArea(DataViewComponent.TOP_LEFT);

        timelineView = new TimelineView(model);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Timeline", true), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(timelineView.getView(), DataViewComponent.TOP_RIGHT);
        if (!initiallyOpened.contains(TracerOptions.VIEW_TIMELINE))
            dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);

        return dvc;
    }


    // --- Master view implementation ------------------------------------------

    private class MasterViewSupport {

        private static final String START_IMAGE_PATH =
            "com/sun/tools/visualvm/modules/tracer/impl/resources/start.png"; // NOI18N
        private static final String STOP_IMAGE_PATH =
            "com/sun/tools/visualvm/modules/tracer/impl/resources/stop.png"; // NOI18N
        private static final String ERROR_IMAGE_PATH =
            "com/sun/tools/visualvm/modules/tracer/impl/resources/error.png"; // NOI18N

        private int commonControlHeight;

        private AbstractButton startButton;
        private AbstractButton stopButton;

        private SimpleSeparator toolbarSeparator;
        private JPanel toolbar;


        DataViewComponent.MasterView getView() {
            JComponent view = createComponents();
            initListeners();
            refreshState(model.areProbesDefined());
            return new DataViewComponent.MasterView("Tracer", null, view);
        }


        private void refreshState(boolean probesDefined) {
            switch (controller.getState()) {
                case TracerController.STATE_SESSION_RUNNING:
                    startButton.setEnabled(false);
                    startButton.setSelected(false);
                    stopButton.setEnabled(true);
                    stopButton.requestFocusInWindow();
//                    clearToolbar();
                    updateViewsOnSessionStart();
                    addChartControls();
                    break;
                case TracerController.STATE_SESSION_INACTIVE:
                    startButton.setEnabled(probesDefined);
                    startButton.setSelected(false);
                    stopButton.setEnabled(false);
                    if (startButton.isEnabled() && KeyboardFocusManager.
                        getCurrentKeyboardFocusManager().getFocusOwner() == null)
                        startButton.requestFocusInWindow();
                    break;
                case TracerController.STATE_SESSION_IMPOSSIBLE:
                    startButton.setEnabled(false);
                    stopButton.setEnabled(false);
                    break;
                case TracerController.STATE_SESSION_STARTING:
                    startButton.setEnabled(false);
                    stopButton.setEnabled(false);
                    startButton.setFocusable(false);
                    startButton.setFocusable(true);
                    addProgress();
                    timelineView.reset();
                    break;
                case TracerController.STATE_SESSION_STOPPING:
                    startButton.setEnabled(false);
                    startButton.setSelected(false);
                    stopButton.setEnabled(false);
                    stopButton.setFocusable(false);
                    stopButton.setFocusable(true);
                    String error = controller.getErrorMessage();
                    if (error != null) addMessage(error);
                    break;
            }
        }

        private void initListeners() {
            controller.addListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    refreshState(model.areProbesDefined());
                }
            });

            model.addListener(new TracerModel.Listener() {
                public void probeAdded(TracerProbe probe) {
                    refreshState(true);
                    updateViewsOnProbesChange();
                }
                public void probeRemoved(TracerProbe probe, boolean probesDefined) {
                    refreshState(probesDefined);
                    updateViewsOnProbesChange();
                }
            });
        }

        private void updateViewsOnSessionStart() {
            String onSessionStart = TracerOptions.getInstance().getOnSessionStart();
            if (!onSessionStart.equals(TracerOptions.VIEWS_UNCHANGED)) {
                // Probes
                setProbesVisible(onSessionStart.contains(TracerOptions.VIEW_PROBES));
                // Timeline
                setTimelineVisible(onSessionStart.contains(TracerOptions.VIEW_TIMELINE));
            }
        }

        private void updateViewsOnProbesChange() {
            String onSessionStart = TracerOptions.getInstance().getOnProbeAdded();
            if (!onSessionStart.equals(TracerOptions.VIEWS_UNCHANGED)) {
                // Probes
                setProbesVisible(onSessionStart.contains(TracerOptions.VIEW_PROBES));
                // Timeline
                setTimelineVisible(onSessionStart.contains(TracerOptions.VIEW_TIMELINE));
            }
        }

        private void setProbesVisible(boolean visible) {
            if (visible) dvc.showDetailsArea(DataViewComponent.TOP_LEFT);
            else dvc.hideDetailsArea(DataViewComponent.TOP_LEFT);
        }

        private void setTimelineVisible(boolean visible) {
            if (visible) dvc.showDetailsArea(DataViewComponent.TOP_RIGHT);
            else dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);
        }

        private void addMessage(String text) {
            toolbar.removeAll();
            toolbar.setLayout(new HorizontalLayout(false));
            toolbar.add(new JLabel(text, new ImageIcon(ImageUtilities.
                                   loadImage(ERROR_IMAGE_PATH)), JLabel.CENTER));
            toolbarSeparator.setVisible(true);
        }

        private void addProgress() {
            TracerProgressObject progress = controller.getProgress();
            if (progress != null) {
                final JProgressBar p = new JProgressBar(0, progress.getSteps());
                p.setPreferredSize(new Dimension(120, p.getPreferredSize().height + 2));
                p.setBorder(BorderFactory.createEmptyBorder());
                String text = progress.getText();
                final JLabel l = new JLabel(text == null ? "" : text); // NOI18N
                l.setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 0));
                p.setValue(progress.getCurrentStep());
                final Timer t = new Timer(INDETERMINATE_PROGRESS_THRESHOLD, null);
                t.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        p.setIndeterminate(true);
                        t.stop();
                    }
                });
                progress.addListener(new TracerProgressObject.Listener() {
                    public void progressChanged(int addedSteps, int currentStep, String text) {
                        t.stop();
                        p.setIndeterminate(false);
                        p.setValue(currentStep);
                        l.setText(text == null ? "" : text); // NOI18N
                        t.start();
                    }
                });
                JLabel s = new JLabel("Starting:");
                s.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
                toolbar.removeAll();
                toolbar.setLayout(new HorizontalLayout(true));
                toolbar.add(s);
                toolbar.add(p);
                toolbar.add(l);
                toolbarSeparator.setVisible(true);
            }
        }

        private void addChartControls() {
            TransparentToolBar tb = new TransparentToolBar();

            Dimension size = new Dimension(commonControlHeight, commonControlHeight);

            JButton c1 = new JButton(timelineView.zoomInAction());
            c1.setMinimumSize(size);
            c1.setPreferredSize(size);
            c1.setMaximumSize(size);
            tb.addItem(c1);

            JButton c2 = new JButton(timelineView.zoomOutAction());
            c2.setMinimumSize(size);
            c2.setPreferredSize(size);
            c2.setMaximumSize(size);
            tb.addItem(c2);

            JButton c3 = new JButton(timelineView.toggleViewAction());
            c3.setMinimumSize(size);
            c3.setPreferredSize(size);
            c3.setMaximumSize(size);
            tb.addItem(c3);

            JPanel sp1 = new JPanel(null) {
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.width = 14;
                    return d;
                }
            };
            tb.addItem(sp1);

            ButtonGroup bg = new ButtonGroup();

            AbstractButton b1 = timelineView.mouseZoom();
            bg.add(b1);
            b1.setMinimumSize(size);
            b1.setPreferredSize(size);
            b1.setMaximumSize(size);
            tb.addItem(b1);

            AbstractButton b2 = timelineView.mouseHScroll();
            bg.add(b2);
            b2.setMinimumSize(size);
            b2.setPreferredSize(size);
            b2.setMaximumSize(size);
            tb.addItem(b2);

            AbstractButton b3 = timelineView.mouseVScroll();
            bg.add(b3);
            b3.setMinimumSize(size);
            b3.setPreferredSize(size);
            b3.setMaximumSize(size);
            tb.addItem(b3);

            toolbar.removeAll();
            toolbar.setLayout(new HorizontalLayout(false));
            toolbar.add(tb);
            toolbarSeparator.setVisible(true);
        }

        private void clearToolbar() {
            toolbar.removeAll();
            toolbarSeparator.setVisible(false);
        }

        private JComponent createComponents() {
            JPanel view = new JPanel(new HorizontalLayout(false, 3));
            view.setBorder(BorderFactory.createEmptyBorder(15, 8, 15, 8));
            view.setOpaque(false);

            startButton = new JToggleButton("Start", new ImageIcon(ImageUtilities.
                                                     loadImage(START_IMAGE_PATH))) {
                protected void fireActionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() { controller.startSession(); }
                    });
                }
            };
            Insets i = startButton.getMargin();
            i.left = Math.min(i.left, 10);
            i.right = i.left;
            startButton.setMargin(i);
            view.add(startButton);

            stopButton = new JButton("Stop", new ImageIcon(ImageUtilities.
                                             loadImage(STOP_IMAGE_PATH))) {
                protected void fireActionPerformed(ActionEvent e) {
                    startButton.requestFocusInWindow();
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() { controller.stopSession(); }
                    });
                }
            };
            ((JButton)stopButton).setDefaultCapable(false);
            stopButton.setMargin(i);
            view.add(stopButton);

            toolbarSeparator = new SimpleSeparator(SwingConstants.VERTICAL);
            toolbarSeparator.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            toolbarSeparator.setVisible(false);
            view.add(toolbarSeparator);

            toolbar = new JPanel(null);
            toolbar.setOpaque(false);
            view.add(toolbar);
//
//            add(new JButton("Zoom In"));
//            add(new JButton("Zoom Pad"));
//            add(new JButton("Zoom Out"));
//
//            SimpleSeparator s2 = new SimpleSeparator(SwingConstants.VERTICAL);
//            s2.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
//            add(s2);
//
//            add(new JToggleButton("Sel None"));
//            add(new JToggleButton("Sel Line"));
//            add(new JToggleButton("Sel Rect"));

            Dimension size1 = startButton.getPreferredSize();
            commonControlHeight = size1.height;

            Dimension size2 = stopButton.getPreferredSize();
            commonControlHeight = Math.max(commonControlHeight, size2.height);

            Action tmpAction = new AbstractAction(null, startButton.getIcon()) {
                public void actionPerformed(ActionEvent e) {}
            };
            commonControlHeight = Math.max(commonControlHeight, new JToolBar().
                                           add(tmpAction).getPreferredSize().height);

            size1.height = commonControlHeight;
            startButton.setMinimumSize(size1);
            startButton.setPreferredSize(size1);
            startButton.setMaximumSize(size1);

            size2.height = commonControlHeight;
            stopButton.setMinimumSize(size2);
            stopButton.setPreferredSize(size2);
            stopButton.setMaximumSize(size2);

            return view;
        }

    }

}
