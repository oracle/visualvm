/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.console.r;

import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.console.r.engine.REngine;
import org.graalvm.visualvm.heapviewer.console.r.engine.REngine.ObjectVisitor;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.ui.HTMLView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.graalvm.polyglot.Context;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.JExtendedSplitPane;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ActionPopupButton;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RConsoleView_Name=R Console",
    "RConsoleView_Description=R Console",
    "RConsoleView_CannotResolveClassMsg=Cannot resolve class",
    "RConsoleView_CannotResolveInstanceMsg=Cannot resolve instance",
    "RConsoleView_NothingExecuted=<no script executed yet>",
//    "OQLConsoleView_NoResults=<no results>",
//    "OQLConsoleView_ViewName=Results",
    "RConsoleView_RQuery=R Query:",
    "RConsoleView_RunAction=Run",
    "RConsoleView_RunActionTooltip=Execute R script",
    "RConsoleView_CancelAction=Cancel",
    "RConsoleView_CancelActionTooltip=Cancel R script execution",
    "RConsoleView_LoadAction=Load Script",
    "RConsoleView_LoadActionTooltip=Load R script",
    "RConsoleView_SaveAction=Save Script",
    "RConsoleView_SaveActionTooltip=Save R script",
//    "OQLConsoleView_EditAction=Edit Scripts",
//    "OQLConsoleView_EditActionTooltip=Edit Saved R scripts",
    "RConsoleView_ExecutingProgress=Executing..."
//    "OQLConsoleView_Results=Results:",
//    "OQLConsoleView_ObjectsTooltip=Objects",
//    "OQLConsoleView_HTMLTooltip=Results:",
//    "OQLConsoleView_Details=Details:",
//    "OQLConsoleView_EngineNotAvailable=<R engine not available>"
})
class RConsoleView extends HeapViewerFeature {
    
    private static final Color SEPARATOR_COLOR = UIManager.getColor("Separator.foreground"); // NOI18N

    private static final Logger LOGGER = Logger.getLogger(RConsoleView.class.getName());
    
    private final HeapContext context;
    
    private ProfilerToolbar toolbar;
//    private ProfilerToolbar pluginsToolbar;
//    private ProfilerToolbar resultsToolbar;
    private ProfilerToolbar graphsToolbar;
    private ProfilerToolbar progressToolbar;
    
    private JComponent component;
    
    private Action runAction;
    private Action cancelAction;
    private Action loadAction;
    private Action saveAction;
//    private Action editAction;
    
    private JLabel progressLabel;
    private JProgressBar progressBar;
    
    private REngine engine;
    private REditorComponent editor;
    
    private JPanel resultsContainer;
    private final HTMLView htmlView;
//    private final PluggableTreeTableView objectsView;
    
    private JPanel graphsContainer;
    private RPlotPanel graphsPanel;
    
    private JToggleButton rResults;
    private JToggleButton rGraphs;
    
    private final AtomicBoolean analysisRunning = new AtomicBoolean(false);
    private final ExecutorService progressUpdater = Executors.newSingleThreadExecutor();
    private boolean queryValid;
//    
//    // TODO: synchronize!
    private Set<HeapViewerNode> nodeResults;
    
    private RQueries.Query currentQuery;
    
    
    public RConsoleView(HeapContext context, HeapViewerActions actions) {
        super("java_objects_rconsole", Bundle.RConsoleView_Name(), Bundle.RConsoleView_Description(), createIcon(), 1100); // NOI18N
        
        this.context = context;
        Heap heap = context.getFragment().getHeap();
        
//        engine = null;
//        if (REngine.isSupported()) try {
//            engine = new REngine(heap);
//        } catch (Exception e) {
//            ProfilerLogger.log(e);
//        }
        
//        if (engine != null) {
//            TreeTableViewColumn[] ownColumns = new TreeTableViewColumn[] {
//                new TreeTableViewColumn.Name(heap),
//                new TreeTableViewColumn.Count(heap, false, false),
//                new TreeTableViewColumn.OwnSize(heap, true, true),
//                new TreeTableViewColumn.RetainedSize(heap, true, false)
//            };
//
//            objectsView = new PluggableTreeTableView("java_objects_oql", context, actions, ownColumns) { // NOI18N
//                protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
//                    if (nodeResults == null) return new HeapViewerNode[] { new TextNode(Bundle.OQLConsoleView_NothingExecuted()) };
//                    else if (nodeResults.isEmpty()) return new HeapViewerNode[] { new TextNode(Bundle.OQLConsoleView_NoResults()) };
//                    else return nodeResults.toArray(HeapViewerNode.NO_NODES);
//                }
//            };
//            objectsView.setViewName(Bundle.OQLConsoleView_ViewName());

            String htmlS = "<initializing R engine...>".replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
            htmlView = new HTMLView("java_objects_rconsole", context, actions, "<p>&nbsp;&nbsp;" + htmlS + "</p>") { // NOI18N
                @Override
                protected String computeData(HeapContext context, String viewID) {
                    if (REngine.isSupported()) try {
                        engine = new REngine(heap);
                    } catch (Exception e) {
                        ProfilerLogger.log(e);
                    }
                    
                    updateUIState();
                    
                    if (engine != null) {
//                        SwingUtilities.invokeLater(new Runnable() {
//                            public void run() { graphsPanel.setContext(engine.getContext()); graphsPanel.repaint(); }
//                        });
                        
                        String htmlS = Bundle.RConsoleView_NothingExecuted().replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
                        return "<p>&nbsp;&nbsp;" + htmlS + "</p>"; // NOI18N
                    } else {
                        String htmlS = "<R engine not available>".replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
                        return "<p>&nbsp;&nbsp;" + htmlS + "</p>"; // NOI18N
                    }
                }
                protected HeapViewerNode nodeForURL(URL url, HeapContext context) {
                    return RConsoleView.getNode(url, context);
                }
            };
//        } else {
////            objectsView = null;
//            htmlView = null;
//        }
    }

    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    private void executeQuery() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (nodeResults == null) nodeResults = new HashSet<>();
                else nodeResults.clear();
//                requestFocus();
                executeQueryImpl(editor.getScript());
            }
        });
    }
    
    private void cancelQuery() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    engine.cancelQuery();
                } catch (Exception e) {

                }
                finalizeQuery(null);
            }
        });
    }
    
    public void queryStarted(final BoundedRangeModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateUIState();
                graphsToolbar.getComponent().setVisible(false);
                progressToolbar.getComponent().setVisible(true);
                progressBar.setModel(model);
            }
        });
    }

    public void queryFinished(final String result) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateUIState();
                progressToolbar.getComponent().setVisible(false);
                graphsToolbar.getComponent().setVisible(rGraphs.isSelected());
                progressBar.setModel(new DefaultBoundedRangeModel(0, 0, 0, 0));
//                objectsView.reloadView();
                
                if (result != null) {
                    htmlView.setText(result);
//                    if (result.length() > 0 && nodeResults.isEmpty()) rHTML.setSelected(true);
                }
            }
        });
    }
    
    private void finalizeQuery(String result) {
        analysisRunning.compareAndSet(true, false);
        queryFinished(result);
    }
    
    
    private void updateUIState() {
        int scriptLength = editor.getScript().trim().length();
        
//        saveAction.setEnabled(scriptLength > 0);
        
        if (analysisRunning.get()) {
            runAction.setEnabled(false);
            cancelAction.setEnabled(true);
            loadAction.setEnabled(false);
            editor.setEditable(false);
        } else {
            runAction.setEnabled(engine != null && scriptLength > 0 && queryValid);
            cancelAction.setEnabled(false);
            loadAction.setEnabled(true);
            editor.setEditable(true);
        }
    }
    
    
    private void executeQueryImpl(final String rQuery) {
        Color b = graphsPanel.getBackground();
        final String rgb = "rgb(" + b.getRed() + ", " + b.getGreen() + ", " + b.getBlue() + ", maxColorValue = 255)";
        
        final BoundedRangeModel progressModel = new DefaultBoundedRangeModel(0, 10, 0, 100);

//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
                new RequestProcessor("R Query Processor").post(new Runnable() { // NOI18N
                    public void run() {
                        final AtomicInteger counter = new AtomicInteger(100);
                        progressModel.setMaximum(100);

                        final StringBuilder sb = new StringBuilder();
                        final boolean[] oddRow = new boolean[1];
                        Color oddRowBackground = UIUtils.getDarker(
                                        UIUtils.getProfilerResultsBackground());
                        final String oddRowBackgroundString =
                                "rgb(" + oddRowBackground.getRed() + "," + //NOI18N
                                         oddRowBackground.getGreen() + "," + //NOI18N
                                         oddRowBackground.getBlue() + ")"; //NOI18N

                        sb.append("<table border='0' width='100%'>"); // NOI18N

                        try {
                            analysisRunning.compareAndSet(false, true);
                            queryStarted(progressModel);
                            progressUpdater.submit(new ProgressUpdater(progressModel));
                            
                            Context rContext = engine.getContext();
                            
                            Image rImage = graphsPanel.createPlotImage();
                            Graphics rGraphics = rImage.getGraphics();
                            int rImageW = rImage.getWidth(graphsPanel);
                            int rImageH = rImage.getHeight(graphsPanel);
                            
                            Boolean renderingQuality = graphsPanel.getRenderingQuality();
                            if (renderingQuality != null && rGraphics instanceof Graphics2D) {
                                Graphics2D g2 = (Graphics2D)rGraphics;
                                Object antialiasing = renderingQuality ? RenderingHints.VALUE_ANTIALIAS_ON :
                                                                         RenderingHints.VALUE_ANTIALIAS_OFF;
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
                                Object text_antialiasing = renderingQuality ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON :
                                                                              RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
                                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, text_antialiasing);
                            }
                            
                            rContext.eval("R", "function(g, w, h) { grDevices:::awt(w, h, g); }").execute(rGraphics, rImageW, rImageH);
                            
                            rContext.eval("R","library(grid); grid.rect(width = 1, height = 1, gp = gpar(col = " + rgb + ", fill = " + rgb + "));");
                            
                            engine.executeQuery(rQuery, new ObjectVisitor() {
                                public boolean visit(Object o) {
                                    sb.append(oddRow[0] ?
                                        "<tr><td style='background-color: " + // NOI18N
                                        oddRowBackgroundString + ";'>" : "<tr><td>"); // NOI18N
                                    oddRow[0] = !oddRow[0];
                                    dump(o, sb);
                                    sb.append("</td></tr>"); // NOI18N
                                    return counter.decrementAndGet() == 0 || (!analysisRunning.get() && !engine.isCancelled()); // process all hits while the analysis is running
                                }
                            });
                            
                            rContext.eval("R","dev.off();");
                            rGraphics.dispose();
                            
                            graphsPanel.repaint();

                            if (counter.get() == 0) {
                                sb.append("<tr><td>");  // NOI18N
                                sb.append("&lt;too many results&gt");      // NOI18N
                                sb.append("</td></tr>");   // NOI18N
                            } else if (counter.get() == 100) {
                                sb.append("<tr><td>"); // NOI18N
                                sb.append("&lt;no results&gt"); // NOI18N
                                sb.append("</td></tr>" ); // NOI18N
                            }

                            sb.append("</table>"); // NOI18N

                            finalizeQuery(sb.toString());
                        } catch (Exception oQLException) {
                            LOGGER.log(Level.INFO, "Error executing R", oQLException);   // NOI18N
                            StringBuilder errorMessage = new StringBuilder();
                            String exceptionMsg = oQLException.getLocalizedMessage();
                            errorMessage.append("<h2>").append("Query error").append("</h2>"); // NOI18N
                            errorMessage.append("Bad R query"); // NOI18N
                            errorMessage.append("<hr>"); // NOI18N
                            if (exceptionMsg != null) {
                                errorMessage.append(exceptionMsg.replace("\n", "<br>").replace("\r", "<br>"));  // NOI18N
                            }
                            finalizeQuery(errorMessage.toString());
                        }
                    }

                });
//            }
//        });
    }
    
    
    private void dump(Object o, StringBuilder sb) {
        String text = o.toString();
        
        text = text.replace(" ", "&nbsp;");
        sb.append("<code>").append(text).append("</code>\n");
    }

    private void init() {
        toolbar = ProfilerToolbar.create(false);
//        if (engine != null) {
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);

            toolbar.add(new GrayLabel(Bundle.RConsoleView_RQuery()));
            toolbar.addSpace(2);
            
            runAction = new AbstractAction(Bundle.RConsoleView_RunAction(), Icons.getIcon(GeneralIcons.START)) {
                {
                    putValue(Action.SHORT_DESCRIPTION, Bundle.RConsoleView_RunActionTooltip());
                }
                public void actionPerformed(ActionEvent e) {
                    executeQuery();
                }
            };

            JButton runButton = new JButton(runAction) {
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.width += 6;
                    return d;
                }
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            runButton.putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N
            
            cancelAction = new AbstractAction(Bundle.RConsoleView_CancelAction(), Icons.getIcon(GeneralIcons.STOP)) {
                {
                    putValue(Action.SHORT_DESCRIPTION, Bundle.RConsoleView_CancelActionTooltip());
                }
                public void actionPerformed(ActionEvent e) {
                    cancelQuery();
                }
            };

            JButton cancelButton = new JButton(cancelAction);
            cancelButton.setHideActionText(true);
            
            loadAction = new AbstractAction(Bundle.RConsoleView_LoadAction(), RQueries.ICON_LOAD) {
                {
                    putValue(Action.SHORT_DESCRIPTION, Bundle.RConsoleView_LoadActionTooltip());
                }
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() instanceof JComponent) {
                        JPopupMenu p = new JPopupMenu();
                        RQueries.instance().populateLoadQuery(p, currentQuery, new RQueries.Handler() {
                            protected void querySelected(RQueries.Query query) {
                                currentQuery = query;
                                if (editor != null) editor.setScript(currentQuery.getScript());
                            }
                        });
                        
                        JComponent c = (JComponent)e.getSource();
                        if (p.getComponentCount() > 0) {
                            if (c.getClientProperty("POPUP_LEFT") != null) p.show(c, c.getWidth() + 1, 0); // NOI18N
                            else p.show(c, 0, c.getHeight() + 1);
                        }
                        
                    }
                }
            };
            
            JButton loadButton = new JButton(loadAction);
            loadButton.setHideActionText(true);
            
            saveAction = new AbstractAction(Bundle.RConsoleView_SaveAction(), RQueries.ICON_SAVE) {
                {
                    putValue(Action.SHORT_DESCRIPTION, Bundle.RConsoleView_SaveActionTooltip());
                }
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() instanceof JComponent) {
                        JPopupMenu p = new JPopupMenu();
                        RQueries.instance().populateSaveQuery(p, currentQuery, editor.getScript(), new RQueries.Handler() {
                            protected void querySelected(RQueries.Query query) {
                                currentQuery = query;
                            }
                        });
                        
                        JComponent c = (JComponent)e.getSource();
                        if (p.getComponentCount() > 0) {
                            if (c.getClientProperty("POPUP_LEFT") != null) p.show(c, c.getWidth() + 1, 0); // NOI18N
                            else p.show(c, 0, c.getHeight() + 1);
                        }
                        
                    }
                }
            };
            
            JButton saveButton = new JButton(saveAction);
            saveButton.setHideActionText(true);
//            
//            editAction = new AbstractAction(Bundle.OQLConsoleView_EditAction(), Icons.getIcon(HeapWalkerIcons.RULES)) {
//                {
//                    putValue(Action.SHORT_DESCRIPTION, Bundle.OQLConsoleView_EditActionTooltip());
//                }
//                public void actionPerformed(ActionEvent e) {
//                    OptionsDisplayer.getDefault().open(HeapViewerOptionsCategory.OPTIONS_HANDLE);
//                }
//            };
//            
//            JButton editButton = new JButton(editAction);
//            editButton.setHideActionText(true);

            graphsPanel = new RPlotPanel();
            
            graphsToolbar = ProfilerToolbar.create(false);

            graphsToolbar.addSpace(2);
            graphsToolbar.addSeparator();
            graphsToolbar.addSpace(5);

            graphsToolbar.add(new GrayLabel("Rendering Quality:"));
            graphsToolbar.addSpace(2);
            
            Action defaultA = new AbstractAction("Default") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    graphsPanel.setRenderingQuality(null);
                }
            };
            Action highA = new AbstractAction("High") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    graphsPanel.setRenderingQuality(Boolean.TRUE);
                }
            };
            Action lowA = new AbstractAction("Low") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    graphsPanel.setRenderingQuality(Boolean.FALSE);
                }
            };
            
            graphsToolbar.add(new ActionPopupButton(defaultA, highA, lowA));
            
            progressToolbar = ProfilerToolbar.create(false);
            progressToolbar.getComponent().setVisible(false);
            
            progressToolbar.addSpace(2);
            progressToolbar.addSeparator();
            progressToolbar.addSpace(5);
            
            progressLabel = new GrayLabel(Bundle.RConsoleView_ExecutingProgress());
            progressToolbar.add(progressLabel);
            
            progressToolbar.addSpace(8);

            progressBar = new JProgressBar(JProgressBar.HORIZONTAL) {
                public Dimension getPreferredSize() {
                    Dimension dim = super.getMinimumSize();
                    dim.width = 120;
                    return dim;
                }
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            progressToolbar.add(progressBar);

            toolbar.add(runButton);
    //        toolbar.addSpace(2);
            toolbar.add(cancelButton);
            
            toolbar.addSpace(5);
            
            toolbar.add(loadButton);
            toolbar.add(saveButton);
//            toolbar.add(editButton);

//            resultsToolbar = ProfilerToolbar.create(false);
//
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);

            toolbar.add(new GrayLabel("Results:"));
            toolbar.addSpace(3);

//            ButtonGroup resultsBG = new ButtonGroup();

            String rResultsPath = RConsoleView.class.getPackage().getName().replace('.', '/') + "/properties.png";
            Image rResultsImage = ImageUtilities.loadImage(rResultsPath, true);
            rResults = new JToggleButton(new ImageIcon(rResultsImage), true) {
                protected void fireItemStateChanged(ItemEvent e) {
//                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (resultsContainer != null) resultsContainer.setVisible(isSelected());
//                    }
                }
            };
            rResults.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            rResults.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
            rResults.setToolTipText("Text");
//            resultsBG.add(rResults);
            toolbar.add(rResults);
            
            String rGraphsPath = RConsoleView.class.getPackage().getName().replace('.', '/') + "/showGraphs.png";
            Image rGraphsImage = ImageUtilities.loadImage(rGraphsPath, true);
            rGraphs = new JToggleButton(new ImageIcon(rGraphsImage), true) {
                protected void fireItemStateChanged(ItemEvent e) {
//                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (graphsContainer != null) {
                            boolean selected = isSelected();
                            graphsToolbar.getComponent().setVisible(selected && !progressToolbar.getComponent().isVisible());
                            graphsContainer.setVisible(selected);
                        }
//                    }
                }
            };
            rGraphs.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            rGraphs.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
            rGraphs.setToolTipText("Graphs");
//            resultsBG.add(rGraphs);
            toolbar.add(rGraphs);
//
//            if (objectsView.hasPlugins()) {
//                pluginsToolbar = ProfilerToolbar.create(false);
//    //            detailsToolbar.addSpace(2);
//    //            detailsToolbar.addSeparator();
//                pluginsToolbar.addSpace(8);
//
//                pluginsToolbar.add(new GrayLabel(Bundle.OQLConsoleView_Details()));
//                pluginsToolbar.addSpace(2);
//
//                pluginsToolbar.add(objectsView.getToolbar());
//
//                resultsToolbar.add(pluginsToolbar);
//            }
//
//            toolbar.add(resultsToolbar);

            toolbar.add(graphsToolbar);
            
            toolbar.add(progressToolbar);

            editor = new REditorComponent(/*engine*/) {
                protected void validityChanged(boolean valid) {
                    queryValid = valid;
                    updateUIState();
                }
            };

            resultsContainer = new JPanel(new CardLayout());
//            resultsContainer.add(objectsView.getComponent());
            resultsContainer.add(new ResultsView(htmlView.getComponent()));
            
//            graphsPanel = new RPlotPanel();
            graphsContainer = new JPanel(new BorderLayout());
            graphsContainer.add(graphsPanel, BorderLayout.CENTER);
            
            MultiSplitContainer resultsSplit = new MultiSplitContainer();
            resultsSplit.add(resultsContainer);
            resultsSplit.add(graphsContainer);

            JExtendedSplitPane masterSplit = new JExtendedSplitPane(JExtendedSplitPane.VERTICAL_SPLIT, true, resultsSplit, new EditorView(editor));
            BasicSplitPaneDivider masterDivider = ((BasicSplitPaneUI)masterSplit.getUI()).getDivider();
            masterDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, SEPARATOR_COLOR));
            masterDivider.setDividerSize(6);
            masterSplit.setBorder(BorderFactory.createEmptyBorder());
            masterSplit.setResizeWeight(0.70d);

            component = new JPanel(new BorderLayout());
            component.add(masterSplit, BorderLayout.CENTER);

            updateUIState();
//        } else {
//            component = new JPanel(new BorderLayout());
//            component.setOpaque(true);
//            component.setBackground(UIUtils.getProfilerResultsBackground());
//            
//            JLabel l = new JLabel(Bundle.OQLConsoleView_EngineNotAvailable(), JLabel.CENTER);
//            l.setEnabled(false);
//            l.setOpaque(false);
//            component.add(l, BorderLayout.CENTER);
//        }
    }
    
    
    private static HeapViewerNode getNode(URL url, HeapContext context) {
        String urls = url.toString();
                
        if (HeapUtils.isInstance(urls)) {
            final Instance instance = HeapUtils.instanceFromHtml(urls, context.getFragment().getHeap());
            if (instance != null) return new InstanceNode(instance);
            else ProfilerDialogs.displayError(Bundle.RConsoleView_CannotResolveInstanceMsg());
        } else if (HeapUtils.isClass(urls)) {
            JavaClass javaClass = HeapUtils.classFromHtml(urls, context.getFragment().getHeap());
            if (javaClass != null) return new ClassNode(javaClass);
            else ProfilerDialogs.displayError(Bundle.RConsoleView_CannotResolveClassMsg());
        }

        return null;
    }
    
    
    private static ImageIcon createIcon() {
        String consolePath = RConsoleView.class.getPackage().getName().replace('.', '/') + "/rConsole.png";
        Image consoleImage = ImageUtilities.loadImage(consolePath, true);
        
        String badgePath = RConsoleView.class.getPackage().getName().replace('.', '/') + "/rBadge.png";
        Image badgeImage = ImageUtilities.loadImage(badgePath, true);
        
        return new ImageIcon(ImageUtilities.mergeImages(consoleImage, badgeImage, 0, 0));
    }
    
    
    private class ProgressUpdater implements Runnable {

        private final BoundedRangeModel progressModel;

        ProgressUpdater(BoundedRangeModel model) {
            progressModel = model;
        }

        public void run() {
            while (analysisRunning.get()) {
                final int newVal;
                int val = progressModel.getValue() + 10;
                
                if (val > progressModel.getMaximum()) {
                    val = progressModel.getMinimum();
                }
                newVal = val;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressModel.setValue(newVal);
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    
    private class EditorView extends JPanel {
        
        EditorView(REditorComponent editor) {
            super(new BorderLayout());
            
            editor.clearScrollBorders();
            add(editor, BorderLayout.CENTER);
//            add(new ScrollableContainer(editorContainer), BorderLayout.CENTER);

            JToolBar controls = new JToolBar(JToolBar.VERTICAL);
            controls.setFloatable(false);
            controls.setBorderPainted(false);
            controls.add(runAction);
            controls.add(cancelAction);
            controls.addSeparator();
            controls.add(loadAction).putClientProperty("POPUP_LEFT", Boolean.TRUE); // NOI18N
            controls.add(saveAction).putClientProperty("POPUP_LEFT", Boolean.TRUE); // NOI18N
//            controls.add(editAction).putClientProperty("POPUP_LEFT", Boolean.TRUE); // NOI18N
            
            JPanel controlsContainer = new JPanel(new BorderLayout());
            controlsContainer.setOpaque(false);
            controlsContainer.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground")), // NOI18N
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)));
            controlsContainer.add(controls, BorderLayout.CENTER);
            add(controlsContainer, BorderLayout.WEST);
            
            // size to always show Run and Stop buttons
            int h = controls.getComponent(0).getPreferredSize().height;
            h += controls.getComponent(1).getPreferredSize().height + 2;
            setMinimumSize(new Dimension(0, h));
        }
        
    }
    
    
    private class ResultsView extends JPanel {
        
        ResultsView(JComponent results) {
            super(new BorderLayout());
            add(new ScrollableContainer(results), BorderLayout.CENTER);
        }
        
    }
    
}
