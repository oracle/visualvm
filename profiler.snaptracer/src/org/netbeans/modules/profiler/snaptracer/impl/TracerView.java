/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl;

import java.awt.BorderLayout;
import java.io.IOException;
import org.netbeans.modules.profiler.snaptracer.TracerProbe;
import org.netbeans.modules.profiler.snaptracer.TracerProgressObject;
import org.netbeans.modules.profiler.snaptracer.impl.options.TracerOptions;
import org.netbeans.modules.profiler.snaptracer.impl.swing.DropdownButton;
import org.netbeans.modules.profiler.snaptracer.impl.swing.SimpleSeparator;
import org.netbeans.modules.profiler.snaptracer.impl.swing.VisibilityHandler;
import org.netbeans.modules.profiler.snaptracer.impl.timeline.TimelineSupport;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.netbeans.modules.profiler.snaptracer.TracerPackage;
import org.netbeans.modules.profiler.snaptracer.impl.swing.HorizontalLayout;
import org.netbeans.modules.profiler.snaptracer.impl.swing.TransparentToolBar;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.SampledCPUSnapshot;
import org.netbeans.modules.profiler.SnapshotResultsWindow;
import org.netbeans.modules.profiler.utils.GoToSourceHelper;
import org.netbeans.modules.profiler.utils.JavaSourceLocation;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerView /*extends DataSourceView*/ {

    private static final int INDETERMINATE_PROGRESS_THRESHOLD =
                Integer.getInteger("visualvm.tracer.indeterminateProgressThreshold", 2500); // NOI18N

    private static final String IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/tracer.png"; // NOI18N

    private final TracerModel model;
    private final TracerController controller;

//    private DataViewComponent dvc;
    private TimelineView timelineView;
    private DetailsView detailsView;

    
    TracerView(TracerModel model, TracerController controller) {
//        super(model.getTarget(), "Tracer", new ImageIcon(
//              ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 100, false);
        this.model = model;
        this.controller = controller;
    }


    // --- DataSourceView implementation ---------------------------------------

    protected JComponent createComponent() {
        
        final JPanel component = new JPanel(new BorderLayout());

        // create timeline support
        timelineView = new TimelineView(model);
        // add the timeline component to the UI
        component.add(timelineView.getView(), BorderLayout.NORTH);

        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                // add all registered probes to the timeline
                initProbes();
                // setup the timeline - zoom according to snapshot data
                initTimeline();
                // load the probes data
                initData(component);
                // init required listeners - timeline selection
                initListeners(component);
            }
        });


//////        final PackagesView packagesView = new PackagesView(model, controller);
//////        final JComponent packages = packagesView.getView();
//////        packages.setOpaque(true);
//////        packages.setBackground(UIUtils.getProfilerResultsBackground());
//////        timelineView = new TimelineView(model);
//////        detailsView = new DetailsView(model);
//////        MasterViewSupport masterView = new MasterViewSupport();
//////
//////
//////
//////        JPanel toolbar = new JPanel(new HorizontalLayout(false, 5));
//////        JLabel name = new JLabel("Snapshot " + new SimpleDateFormat().format(Calendar.getInstance().getTime()));
//////        name.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//////        toolbar.add(name);
//////        toolbar.add(new JButton("Metrics") {
//////            protected void fireActionPerformed(ActionEvent e) {
//////                JScrollPane s = new JScrollPane(packages);
//////                JPanel p = new JPanel(new BorderLayout());
//////                p.add(s, BorderLayout.CENTER);
//////                p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//////                p.setPreferredSize(new Dimension(350, 250));
//////                DialogDescriptor dd = new DialogDescriptor(p, "Define Snapshot Metrics");
//////                Dialog d = DialogDisplayer.getDefault().createDialog(dd);
//////                d.pack();
//////                d.setVisible(true);
//////                RequestProcessor.getDefault().post(new Runnable() {
//////                    public void run() {
//////                        controller.performSession();
//////                        initProbes();
//////                    }
//////                });
//////            }
//////        });
////////        component.add(toolbar, BorderLayout.NORTH);
//////
//////        JComponent controllerContainer = new JPanel(new BorderLayout());
//////        controllerContainer.setOpaque(true);
//////        controllerContainer.setBackground(UIUtils.getProfilerResultsBackground());
//////        controllerContainer.add(toolbar, BorderLayout.NORTH);
//////        controllerContainer.add(timelineView.getView(), BorderLayout.CENTER);
//////
//////
//////        JComponent masterContainer = new JPanel(new BorderLayout());
//////        masterContainer.add(controllerContainer, BorderLayout.NORTH);
//////
//////
//////        try {
//////            IdeSnapshot ss = new IdeSnapshot(new File("C:\\Users\\JiS\\Documents\\snapshot_tracer_test.npss"));
//////            LoadedSnapshot ls = ss.getCPUSnapshot(0, ss.getSamplesCount() - 1);
//////            SnapshotResultsWindow sw = new SnapshotResultsWindow(ls, 1, false);
//////            masterContainer.add(sw, BorderLayout.CENTER);
//////        } catch (Exception e) {}
        
        
//        component.add(masterView.getView(), BorderLayout.NORTH);
        
//        JComponent packages = packagesView.getView();
//        packages.setPreferredSize(new Dimension(300, 1));
//        component.add(packages, BorderLayout.WEST);
        
//        masterContainer.add(timelineView.getView(), BorderLayout.NORTH);
//        component.add(detailsView.getView(), BorderLayout.SOUTH);
        
//        dvc = new DataViewComponent(masterView.getView(),
//                new DataViewComponent.MasterViewConfiguration(false));
//
//        dvc.configureDetailsView(new DataViewComponent.DetailsViewConfiguration(0.33, 0, 0.33, 0, 0.5, 0.5));
//
//        String initiallyOpened = TracerOptions.getInstance().getInitiallyOpened();
//
//        
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Probes", true), DataViewComponent.TOP_LEFT);
//        dvc.addDetailsView(packagesView.getView(), DataViewComponent.TOP_LEFT);
//        if (!initiallyOpened.contains(TracerOptions.VIEW_PROBES))
//            dvc.hideDetailsArea(DataViewComponent.TOP_LEFT);
//
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Timeline", true), DataViewComponent.TOP_RIGHT);
//        dvc.addDetailsView(timelineView.getView(), DataViewComponent.TOP_RIGHT);
//        if (!initiallyOpened.contains(TracerOptions.VIEW_TIMELINE))
//            dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);
//
//        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Details", true), DataViewComponent.BOTTOM_RIGHT);
//        dvc.addDetailsView(detailsView.getView(), DataViewComponent.BOTTOM_RIGHT);
//        if (!initiallyOpened.contains(TracerOptions.VIEW_DETAILS))
//            dvc.hideDetailsArea(DataViewComponent.BOTTOM_RIGHT);

        return component;
    }
    
    private void initProbes() {
        List<TracerPackage> packages =
            TracerSupportImpl.getInstance().getPackages(model.getSnapshot());
        for (TracerPackage p : packages)
            model.addDescriptors(p, p.getProbeDescriptors());
    }

    private void initTimeline() {
        TimelineSupport support = model.getTimelineSupport();
        long start = model.firstTimestamp();
        if (start == -1) return;
        long end = model.lastTimestamp();
        if (end == -1) return;
        support.dataLoadingStarted(end - start);
    }

    private void initData(final JPanel component) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JLabel progress = new JLabel("Loading snapshot...", JLabel.CENTER);
                addContents(component, progress);

                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        controller.performSession();
                        controller.performAfterSession(new Runnable() {
                            public void run() {
                                TimelineSupport support = model.getTimelineSupport();
                                support.dataLoadingFinished();
                                support.selectAll();

                                int lastSample = model.getSamplesCount() - 1;
                                displaySnapshot(component, 0, lastSample);

//                                displayThreadDump(component, 10);
                            }
                        });
                    }
                });
            }
        });
    }

    private void initListeners(final JPanel component) {
        final TimelineSupport support = model.getTimelineSupport();
        support.addSelectionListener(
                new TimelineSupport.SelectionListener() {
            public void indexSelectionChanged() {
                final int startIndex = Math.min(support.getStartIndex(), support.getEndIndex());
                final int endIndex = Math.max(support.getStartIndex(), support.getEndIndex());
                
                JLabel progress = new JLabel("Processing selection...", JLabel.CENTER);
                addContents(component, progress);

                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
//                        long start = System.currentTimeMillis();
                        if (startIndex == endIndex) displayThreadDump(component, startIndex);
                        else displaySnapshot(component, startIndex, endIndex);
//                        System.err.println(">>> Processing selection took " + (System.currentTimeMillis() - start) + "ms");
                    }
                });
            }

            public void timeSelectionChanged(boolean timestampsSelected,
                                             boolean justHovering) {}
        });
    }
    
    private void displaySnapshot(final JPanel p, final int s1, final int s2) {
        LoadedSnapshot ls = null;
        long t1 = -1;
        long t2 = -1;
        try {
            ls = model.getSnapshot().getCPUSnapshot(s1, s2);
            t1 = model.getTimelineSupport().getTimestamp(s1);
            t2 = model.getTimelineSupport().getTimestamp(s2);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        final LoadedSnapshot lsF = ls;
        final long t1F = t1;
        final long t2F = t2;

        if (lsF != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SnapshotResultsWindow w = new SnapshotResultsWindow(lsF, 1, false);

                JLabel l = new JLabel("Snapshot from sample #" + s1 + ", time " +
                                      SimpleDateFormat.getDateTimeInstance().format
                                      (new Date(t1F)) + " to sample #" + s2 + ", time " +
                                      SimpleDateFormat.getDateTimeInstance().format
                                      (new Date(t2F)) + ":");

                JPanel c = new JPanel(new BorderLayout(0, 3));
                c.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
                c.add(l, BorderLayout.NORTH);
                c.add(w, BorderLayout.CENTER);

                addContents(p, c);
            }
        });
    }

    private void displayThreadDump(final JPanel p, final int s) {
        String td = null;
        long t = -1;
        try {
            td = model.getSnapshot().getThreadDump(s);
            t = model.getTimelineSupport().getTimestamp(s);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        final String tdF = td;
        final long tF = t;

        if (tdF != null) SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                HTMLTextArea a = new HTMLTextArea(tdF) {
                    protected void showURL(URL url) {
                        if (url == null) return;
                        String urls = url.toString();
                        TracerView.this.showURL(urls);
                    }                    
                };
                a.setCaretPosition(0);
                JScrollPane sp = new JScrollPane(a);
                
                JLabel l = new JLabel("Thread dump for sample #" + s + ", time " +
                                      SimpleDateFormat.getDateTimeInstance().format
                                      (new Date(tF)) + ":");

                JPanel c = new JPanel(new BorderLayout(0, 3));
                c.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
                c.add(l, BorderLayout.NORTH);
                c.add(sp, BorderLayout.CENTER);

                addContents(p, c);
            }
        });
    }

    private static void addContents(JComponent container, JComponent contents) {
        BorderLayout layout = (BorderLayout)container.getLayout();
        Component oldContents = layout.getLayoutComponent(BorderLayout.CENTER);
        if (oldContents != null) container.remove(oldContents);
        container.add(contents, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    void showURL(String urls) {
        if (urls.startsWith(SampledCPUSnapshot.OPEN_THREADS_URL)) {
            urls = urls.substring(SampledCPUSnapshot.OPEN_THREADS_URL.length());
            String parts[] = urls.split("\\|"); // NOI18N
            String className = parts[0];
            String method = parts[1];
            int linenumber = Integer.parseInt(parts[2]);
            GoToSourceHelper.openSource(null,new JavaSourceLocation(className, method, linenumber));
        }
    }
    
    // --- Master view implementation ------------------------------------------

    private class MasterViewSupport {

        private static final String START_IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/start.png"; // NOI18N
        private static final String STOP_IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/stop.png"; // NOI18N
        private static final String ERROR_IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/error.png"; // NOI18N
        private static final String SETTINGS_IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/settings.png"; // NOI18N
        private static final String CLPROBE_IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/probeClear.png"; // NOI18N
        private static final String CLMARK_IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/markClear.png"; // NOI18N
        private static final String EXPORT_IMAGE_PATH =
            "org/netbeans/modules/profiler/snaptracer/impl/resources/export.png"; // NOI18N

        private static final String SYSTEM_TOOLBAR = "systemToolbar"; // NOI18N
        private static final String CLIENT_TOOLBAR = "clientToolbar"; // NOI18N

        private int commonControlHeight;

        private AbstractButton startButton;
        private AbstractButton stopButton;

        private CardLayout toolbarLayout;
        private JPanel toolbar;
        private JPanel systemToolbar;
        private JPanel clientToolbar;

        private TransparentToolBar timelineToolbar;
        private TransparentToolBar selectionToolbar;
        private TransparentToolBar extraToolbar;

        private JButton clearRowSelectionButton;
        private JButton clearTimestampSelectionButton;

        private Action exportAllAction;
        private Action exportDetailsAction;


        JComponent getView() {
            JComponent view = createComponents();
            initListeners();
            refreshState(model.areProbesDefined());

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Dimension size = new Dimension(commonControlHeight, commonControlHeight);
                    createTimelineToolbar(size);
                    createSelectionToolbar(size);
                    createExtraToolbar(size);
                }
            });

            return view;
        }


        private void refreshState(boolean probesDefined) {
            switch (controller.getState()) {
                case TracerController.STATE_SESSION_RUNNING:
                    startButton.setEnabled(false);
                    startButton.setSelected(false);
                    stopButton.setEnabled(true);
                    stopButton.requestFocusInWindow();
                    resetSystemToolbar();
                    updateViewsOnSessionStart();
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
                    showProgress();
                    timelineView.reset();
                    break;
                case TracerController.STATE_SESSION_STOPPING:
                    startButton.setEnabled(false);
                    startButton.setSelected(false);
                    stopButton.setEnabled(false);
                    stopButton.setFocusable(false);
                    stopButton.setFocusable(true);
                    String error = controller.getErrorMessage();
                    if (error != null) showMessage(error);
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
                    updateViewsOnProbesChange(true);
                }
                public void probeRemoved(TracerProbe probe, boolean probesDefined) {
                    refreshState(probesDefined);
                    updateViewsOnProbesChange(probesDefined);
                }
            });

            final boolean dynamicSelection = TracerOptions.getInstance().getSelectionToolbar() == TracerOptions.SHOW_AS_NEEDED;
            model.getTimelineSupport().addSelectionListener(
                    new TimelineSupport.SelectionListener() {
//                public void rowSelectionChanged(boolean rowsSelected) {
//                    updateViewsOnSelectionChange(rowsSelected);
//                    updateSelectionToolbar(dynamicSelection);
//                }
                public void indexSelectionChanged() {}
                public void timeSelectionChanged(boolean timestampsSelected, boolean justHovering) {
                    updateSelectionToolbar(dynamicSelection);
                }
            });

            if (TracerOptions.getInstance().getTimelineToolbar() == TracerOptions.SHOW_AS_NEEDED)
                timelineView.registerViewListener(new VisibilityHandler() {
                    public void shown()  { showTimelineToolbar(); }
                    public void hidden() { hideTimelineToolbar(); }
                });
            
            detailsView.registerViewListener(new VisibilityHandler() {
                public void shown()  {}
                public void hidden() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            // Doesn't clear the selection when switching tabs and
                            // manipulating topcomponents
                            if (!detailsView.isShowing() && timelineView.isShowing())
                                clearSelections();
                        }
                    });
                }
            });

            if (TracerOptions.getInstance().getExtraToolbar() == TracerOptions.SHOW_AS_NEEDED)
                model.getTimelineSupport().addValuesListener(new TimelineSupport.ValuesListener() {
                    public void valuesAdded() { showExtraToolbar(); }
                    public void valuesReset() { hideExtraToolbar(); }
                });
        }

        private void clearSelections() {
            if (TracerOptions.getInstance().isClearSelection()) {
                timelineView.resetSelection();
//                model.getTimelineSupport().resetSelectedTimestamps();
            }
        }

        private void updateViewsOnSessionStart() {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String views = TracerOptions.getInstance().getOnSessionStart();
                    updateViews(views);
                }
            });
        }

        private void updateViewsOnProbesChange(boolean probesDefined) {
            String views = probesDefined ?
                TracerOptions.getInstance().getOnProbeAdded() :
                TracerOptions.getInstance().getOnProbeAdded2();
            updateViews(views);
        }

        private void updateViewsOnSelectionChange(boolean rowsSelected) {
            String views = rowsSelected ?
                TracerOptions.getInstance().getOnRowSelected() :
                TracerOptions.getInstance().getOnRowSelected2();
            updateViews(views);
        }

        private void updateViews(String views) {
            if (!views.equals(TracerOptions.VIEWS_UNCHANGED)) {
                // Probes
                setProbesVisible(views.contains(TracerOptions.VIEW_PROBES));
                // Timeline
                setTimelineVisible(views.contains(TracerOptions.VIEW_TIMELINE));
                // Details
                setDetailsVisible(views.contains(TracerOptions.VIEW_DETAILS));
            }
        }

        private void setProbesVisible(boolean visible) {
//            if (visible) dvc.showDetailsArea(DataViewComponent.TOP_LEFT);
//            else dvc.hideDetailsArea(DataViewComponent.TOP_LEFT);
        }

        private void setTimelineVisible(boolean visible) {
//            if (visible) dvc.showDetailsArea(DataViewComponent.TOP_RIGHT);
//            else dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);
        }

        private void setDetailsVisible(boolean visible) {
//            if (visible) dvc.showDetailsArea(DataViewComponent.BOTTOM_RIGHT);
//            else dvc.hideDetailsArea(DataViewComponent.BOTTOM_RIGHT);
        }

        private void showMessage(String text) {
            setSystemToolbarItem(new JLabel(text, new ImageIcon(ImageUtilities.
                                 loadImage(ERROR_IMAGE_PATH)), JLabel.CENTER));
        }

        private void showProgress() {
            final TracerProgressObject progress = controller.getProgress();
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
                        if (!progress.isFinished()) t.start();
                    }
                });
                JLabel s = new JLabel("Starting:");
                s.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
                JPanel container = new JPanel(new HorizontalLayout(true));
                container.setOpaque(false);
                container.add(s);
                container.add(p);
                container.add(l);
                setSystemToolbarItem(container);
            }
        }

        private void setSystemToolbarItem(Component c) {
            clearSystemToolbar();
            systemToolbar.add(c);
            systemToolbar.revalidate();
            systemToolbar.repaint();
            toolbarLayout.show(toolbar, SYSTEM_TOOLBAR);
        }

        private void resetSystemToolbar() {
            toolbarLayout.show(toolbar, CLIENT_TOOLBAR);
            clearSystemToolbar();
        }

        private void clearSystemToolbar() {
            while (systemToolbar.getComponentCount() > 1)
                systemToolbar.remove(systemToolbar.getComponentCount() - 1);
            systemToolbar.revalidate();
            systemToolbar.repaint();
        }


        private void createTimelineToolbar(Dimension size) {
            timelineToolbar = new TransparentToolBar();

            JButton c1 = new JButton(timelineView.zoomInAction());
            c1.setMinimumSize(size);
            c1.setPreferredSize(size);
            c1.setMaximumSize(size);
            timelineToolbar.addItem(c1);

            JButton c2 = new JButton(timelineView.zoomOutAction());
            c2.setMinimumSize(size);
            c2.setPreferredSize(size);
            c2.setMaximumSize(size);
            timelineToolbar.addItem(c2);

            JButton c3 = new JButton(timelineView.toggleViewAction());
            c3.setMinimumSize(size);
            c3.setPreferredSize(size);
            c3.setMaximumSize(size);
            timelineToolbar.addItem(c3);

            JPanel sp1 = new JPanel(null) {
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.width = 14;
                    return d;
                }
            };
            timelineToolbar.addItem(sp1);

            ButtonGroup bg = new ButtonGroup();

            AbstractButton b1 = timelineView.mouseZoom();
            bg.add(b1);
            b1.setMinimumSize(size);
            b1.setPreferredSize(size);
            b1.setMaximumSize(size);
            timelineToolbar.addItem(b1);

            AbstractButton b2 = timelineView.mouseHScroll();
            bg.add(b2);
            b2.setMinimumSize(size);
            b2.setPreferredSize(size);
            b2.setMaximumSize(size);
            timelineToolbar.addItem(b2);

            AbstractButton b3 = timelineView.mouseVScroll();
            bg.add(b3);
            b3.setMinimumSize(size);
            b3.setPreferredSize(size);
            b3.setMaximumSize(size);
            timelineToolbar.addItem(b3);

            JPanel sp2 = new JPanel(null) {
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.width = 14;
                    return d;
                }
            };
            timelineToolbar.addItem(sp2);

            DropdownButton d = new DropdownButton(new ImageIcon(
                    ImageUtilities.loadImage(SETTINGS_IMAGE_PATH)));
            d.setToolTipText("Settings");
            Action action21 = new AbstractAction("Show min/max values") {
                public Object getValue(String key) {
                    if (DropdownButton.KEY_BOOLVALUE.equals(key)) {
                        putValue(DropdownButton.KEY_BOOLVALUE, model.
                                 getTimelineSupport().isShowValuesEnabled());
                    }
                    return super.getValue(key);
                }
                public void actionPerformed(ActionEvent e) {
                    model.getTimelineSupport().setShowValuesEnabled(!model.
                            getTimelineSupport().isShowValuesEnabled());
                }
            };
            action21.putValue(DropdownButton.KEY_CLASS, Boolean.class);
            d.addAction(action21);

            Action action22 = new AbstractAction("Show row legend") {
                public Object getValue(String key) {
                    if (DropdownButton.KEY_BOOLVALUE.equals(key)) {
                        putValue(DropdownButton.KEY_BOOLVALUE, model.
                                 getTimelineSupport().isShowLegendEnabled());
                    }
                    return super.getValue(key);
                }
                public void actionPerformed(ActionEvent e) {
                    model.getTimelineSupport().setShowLegendEnabled(!model.
                            getTimelineSupport().isShowLegendEnabled());
                }
            };
            action22.putValue(DropdownButton.KEY_CLASS, Boolean.class);
            d.addAction(action22);

            d.setMinimumSize(size);
            d.setPreferredSize(size);
            d.setMaximumSize(size);
            timelineToolbar.addItem(d);

            TracerOptions options = TracerOptions.getInstance();
            int tbVis = options.getTimelineToolbar();
            if (tbVis == TracerOptions.SHOW_NEVER)
                timelineToolbar.setVisible(false);
            else if (tbVis == TracerOptions.SHOW_AS_NEEDED)
                timelineToolbar.setVisible(timelineView.isShowing());
            
            addClientToobarItem(timelineToolbar);
        }

        private void hideTimelineToolbar() {
            if (timelineToolbar != null) timelineToolbar.setVisible(false);
        }

        private void showTimelineToolbar() {
            if (timelineToolbar != null) timelineToolbar.setVisible(true);
        }


        private void createSelectionToolbar(Dimension size) {
            selectionToolbar = new TransparentToolBar();
            final TimelineSupport support = model.getTimelineSupport();

            clearRowSelectionButton = new JButton(new ImageIcon(
                    ImageUtilities.loadImage(CLPROBE_IMAGE_PATH))) {
                protected void fireActionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            timelineView.resetSelection();
                        }
                    });
                }
            };
            clearRowSelectionButton.setToolTipText("Clear selected probes");
            clearRowSelectionButton.setMinimumSize(size);
            clearRowSelectionButton.setPreferredSize(size);
            clearRowSelectionButton.setMaximumSize(size);
            selectionToolbar.addItem(clearRowSelectionButton);

            clearTimestampSelectionButton = new JButton(new ImageIcon(
                    ImageUtilities.loadImage(CLMARK_IMAGE_PATH))) {
                protected void fireActionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            support.resetSelectedTimestamps();
                        }
                    });
                }
            };
            clearTimestampSelectionButton.setToolTipText("Clear marks");
            clearTimestampSelectionButton.setMinimumSize(size);
            clearTimestampSelectionButton.setPreferredSize(size);
            clearTimestampSelectionButton.setMaximumSize(size);
            selectionToolbar.addItem(clearTimestampSelectionButton);

            TracerOptions options = TracerOptions.getInstance();
            int tbVis = options.getSelectionToolbar();
            if (tbVis == TracerOptions.SHOW_AS_NEEDED) {
                updateSelectionToolbar(true);
            } else {
                if (tbVis == TracerOptions.SHOW_NEVER)
                    selectionToolbar.setVisible(false);
                updateSelectionToolbar(false);
            }

            addClientToobarItem(selectionToolbar);
        }

        private void updateSelectionToolbar(boolean dynamicSelection) {
            TimelineSupport support = model.getTimelineSupport();
            boolean rowSelection = support.isRowSelection();
            boolean timestampSelection = support.isTimestampSelection(false);
            clearRowSelectionButton.setEnabled(rowSelection);
            clearTimestampSelectionButton.setEnabled(timestampSelection);
            if (dynamicSelection)
                selectionToolbar.setVisible(rowSelection || timestampSelection);
        }


        private void createExtraToolbar(Dimension size) {
            extraToolbar = new TransparentToolBar();

            DropdownButton d = new DropdownButton(new ImageIcon(
                    ImageUtilities.loadImage(EXPORT_IMAGE_PATH)));
            d.setToolTipText("Export data");
            exportAllAction = new AbstractAction("Export all data") {
                public void actionPerformed(ActionEvent e) {
//                    String title = DataSourceDescriptorFactory.
//                                   getDescriptor(model.getTarget()).getName();
//                    title = "Exported Tracer Data for " + title + " at " +
//                            new Date(System.currentTimeMillis()).toString();
                    String title = "Exported Tracer Data";
                    model.getTimelineSupport().exportAllValues(title);
                }
                public boolean isEnabled() {
                    return model.getTimelineSupport().hasData();
                }
            };
            d.addAction(exportAllAction);

            exportDetailsAction = new AbstractAction("Export Details table") {
                public void actionPerformed(ActionEvent e) {
//                    String title = DataSourceDescriptorFactory.
//                                   getDescriptor(model.getTarget()).getName();
//                    title = "Exported Tracer Details for " + title + " at " +
//                            new Date(System.currentTimeMillis()).toString();
                    String title = "Exported Tracer Data";
                    model.getTimelineSupport().exportDetailsValues(title);
                }
                public boolean isEnabled() {
                    return detailsView.hasData();
                }
            };
            d.addAction(exportDetailsAction);

            d.setMinimumSize(size);
            d.setPreferredSize(size);
            d.setMaximumSize(size);
            extraToolbar.addItem(d);

            TracerOptions options = TracerOptions.getInstance();
            int tbVis = options.getExtraToolbar();
            if (tbVis == TracerOptions.SHOW_NEVER)
                extraToolbar.setVisible(false);
            else if (tbVis == TracerOptions.SHOW_AS_NEEDED)
                extraToolbar.setVisible(exportAllAction.isEnabled());

            addClientToobarItem(extraToolbar);
        }

        private void hideExtraToolbar() {
            if (extraToolbar != null) extraToolbar.setVisible(false);
        }

        private void showExtraToolbar() {
            if (extraToolbar != null) extraToolbar.setVisible(true);
        }

        private void addClientToobarItem(Component c) {
            final SimpleSeparator s = createToolbarSeparator();
            new VisibilityHandler() {
                public void shown() { s.setVisible(true); }
                public void hidden() { s.setVisible(false); }
            }.handle(c);
            s.setVisible(c.isShowing());
            clientToolbar.add(s);
            clientToolbar.add(c);
            clientToolbar.revalidate();
            clientToolbar.repaint();
        }

        private void removeClientToolbarItem(Component c) {
            int index = -1;
            for (int i = 0; i < clientToolbar.getComponentCount(); i++)
                if (clientToolbar.getComponent(i) == c) {
                    index = i;
                    break;
                }
            if (index != -1) {
                clientToolbar.remove(index);
                clientToolbar.remove(index - 1); // separator
                clientToolbar.revalidate();
                clientToolbar.repaint();
            }
        }

        private SimpleSeparator createToolbarSeparator() {
            SimpleSeparator separator = new SimpleSeparator(SwingConstants.VERTICAL);
            separator.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            Dimension dim = separator.getPreferredSize();
            dim.height = commonControlHeight;
            separator.setPreferredSize(dim);
            return separator;
        }

        private JComponent createComponents() {
            JPanel view = new JPanel(new HorizontalLayout(true));
            view.setBorder(BorderFactory.createEmptyBorder(15, 8, 15, 8));
            view.setOpaque(false);

            startButton = new JToggleButton("Start", new ImageIcon(ImageUtilities.
                                                     loadImage(START_IMAGE_PATH))) {
//                protected void fireActionPerformed(ActionEvent e) {
//                    RequestProcessor.getDefault().post(new Runnable() {
//                        public void run() { controller.startSession(); }
//                    });
//                }
            };
            startButton.setToolTipText("Starts new Tracer session");
            Insets i = startButton.getMargin();
            i.left = Math.min(i.left, 10);
            i.right = i.left;
            startButton.setMargin(i);
            view.add(startButton);

            JPanel buttonGap = new JPanel(null);
            buttonGap.setOpaque(false);
            buttonGap.setPreferredSize(new Dimension(6, 1));
            view.add(buttonGap);

            stopButton = new JButton("Stop", new ImageIcon(ImageUtilities.
                                             loadImage(STOP_IMAGE_PATH))) {
//                protected void fireActionPerformed(ActionEvent e) {
//                    startButton.requestFocusInWindow();
//                    RequestProcessor.getDefault().post(new Runnable() {
//                        public void run() { controller.stopSession(); }
//                    });
//                }
            };
            stopButton.setToolTipText("Stops current Tracer session");
            ((JButton)stopButton).setDefaultCapable(false);
            stopButton.setMargin(i);
            view.add(stopButton);

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

            toolbarLayout = new CardLayout(0, 0);
            toolbar = new JPanel(toolbarLayout);
            toolbar.setOpaque(false);

            systemToolbar = new JPanel(new HorizontalLayout(false));
            systemToolbar.setOpaque(false);
            systemToolbar.add(createToolbarSeparator());
            toolbar.add(systemToolbar, SYSTEM_TOOLBAR);

            clientToolbar = new JPanel(new HorizontalLayout(false));
            clientToolbar.setOpaque(false);
            toolbar.add(clientToolbar, CLIENT_TOOLBAR);

            toolbarLayout.show(toolbar, CLIENT_TOOLBAR);
            view.add(toolbar);

//            if (TracerOptions.getInstance().isRefreshCustomizable()) {
//                JPanel refreshRateContainer = new JPanel(new HorizontalLayout(true, 4));
//                refreshRateContainer.setOpaque(false);
//
//                JLabel refreshRateLabel = new JLabel("Sample:");
//                refreshRateLabel.setToolTipText("Tracer sampling frequency");
//                refreshRateContainer.add(refreshRateLabel);
//
//                Integer[] refreshRates = new Integer[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
//                final JComboBox refreshCombo = new JComboBox(refreshRates) {
//                    public Dimension getMinimumSize() { return getPreferredSize(); }
//                    public Dimension getMaximumSize() { return getPreferredSize(); }
//                };
//                refreshCombo.setToolTipText("Tracer sampling frequency");
//                refreshCombo.setEditable(false);
//                refreshCombo.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        SwingUtilities.invokeLater(new Runnable() {
//                            public void run() {
//                                controller.setRefreshRate(
//                                        (Integer)refreshCombo.getSelectedItem());
//                            }
//                        });
//                    }
//                });
//                refreshCombo.setSelectedItem(Integer.valueOf(controller.getRefreshRate()));
//                refreshCombo.setRenderer(new CustomComboRenderer.Number(refreshCombo, null, false));
//                refreshRateContainer.add(refreshCombo);
//
//                JLabel refreshUnitsLabel = new JLabel("ms");
//                refreshUnitsLabel.setToolTipText("Tracer sampling frequency");
//                refreshRateContainer.add(refreshUnitsLabel);
//
//                addClientToobarItem(refreshRateContainer);
//            }

            return view;
        }

    }

}
