/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.impl.swing.HorizontalLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class TracerView extends DataSourceView {

    private static final String IMAGE_PATH =
            "com/sun/tools/visualvm/modules/tracer/impl/resources/tracer.png"; // NOI18N

    private final TracerModel model;
    private final TracerController controller;

    
    public TracerView(TracerModel model, TracerController controller) {
        super(model.getDataSource(), "Tracer", new ImageIcon(
              ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 100, false);
        this.model = model;
        this.controller = controller;
    }


    // --- DataSourceView implementation ---------------------------------------

    protected DataViewComponent createComponent() {
        MasterViewSupport masterView = new MasterViewSupport();
        DataViewComponent dvc = new DataViewComponent(masterView.getView(),
                new DataViewComponent.MasterViewConfiguration(false));

        dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.33, 0, 0.33, 0, 0.5, 0.5));

        PackagesView packagesView = new PackagesView(model, controller);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Probes", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(packagesView.getView(), DataViewComponent.TOP_LEFT);

        TimelineView timelineView = new TimelineView(model);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Timeline", true), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(timelineView.getView(), DataViewComponent.TOP_RIGHT);

//
//        packagesView = new PackagesViewSupport(getDataSource(), packages);
//        timelineView = new TimelineViewSupport(getDataSource(), packages);
//        settingsView = new SettingsViewSupport(getDataSource(), packages);
//        detailsView = new DetailsViewSupport(getDataSource(), packages);
//
//        PackagesPanel.SelectionHandler handler1 = new PackagesPanel.SelectionHandler() {
//            public void descriptorSelected(TracerPackage p, TracerProbeDescriptor d) {
//                timelineView.addDescriptor(d, p);
////                System.err.println(">>> Selected: " + d.getProbeName() + " in " + p.getName());
//            }
//
//            public void descriptorUnselected(TracerPackage p, TracerProbeDescriptor d) {
//                timelineView.removeDescriptor(d);
////                System.err.println(">>> Unselected: " + d.getProbeName() + " in " + p.getName());
//            }
//        };
//
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Probes", true), DataViewComponent.TOP_LEFT);
//        dvc.addDetailsView(packagesView.getDetailsView(handler1), DataViewComponent.TOP_LEFT);
//
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Timeline", true), DataViewComponent.TOP_RIGHT);
//        dvc.addDetailsView(timelineView.getDetailsView(), DataViewComponent.TOP_RIGHT);
//
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Settings", true), DataViewComponent.BOTTOM_LEFT);
//        dvc.addDetailsView(settingsView.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
//        dvc.hideDetailsArea(DataViewComponent.BOTTOM_LEFT);
//
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Details", true), DataViewComponent.BOTTOM_RIGHT);
//        dvc.addDetailsView(detailsView.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
//        dvc.hideDetailsArea(DataViewComponent.BOTTOM_RIGHT);
//
//        packages = null;

        return dvc;
    }


    // --- Master view implementation ------------------------------------------

    private class MasterViewSupport {

        private AbstractButton startButton;
        private AbstractButton stopButton;


        public DataViewComponent.MasterView getView() {
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
                    break;
                case TracerController.STATE_SESSION_STOPPING:
                    startButton.setEnabled(false);
                    startButton.setSelected(false);
                    stopButton.setEnabled(false);
                    stopButton.setFocusable(false);
                    stopButton.setFocusable(true);
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
                }
                public void probeRemoved(TracerProbe probe, boolean probesDefined) {
                    refreshState(probesDefined);
                }
            });
        }

        private JComponent createComponents() {
            JPanel view = new JPanel(new HorizontalLayout());
            view.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            view.setOpaque(false);

//            setLayout(new HorizontalLayout());
//            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//            setOpaque(false);

            startButton = new JToggleButton("Start") {
                protected void fireActionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() { controller.startSession(); }
                    });
                }
            };
            view.add(startButton);

            stopButton = new JButton("Stop") {
                protected void fireActionPerformed(ActionEvent e) {
                    startButton.requestFocusInWindow();
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() { controller.stopSession(); }
                    });
                }
            };
            view.add(stopButton);

//            SimpleSeparator s1 = new SimpleSeparator(SwingConstants.VERTICAL);
//            s1.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
//            add(s1);
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

            return view;
        }

    }

}
