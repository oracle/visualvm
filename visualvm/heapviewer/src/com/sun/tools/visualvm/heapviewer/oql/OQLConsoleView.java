/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.oql;

import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedSplitPane;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.ClassNode;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNodeFilter;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.model.TextNode;
import com.sun.tools.visualvm.heapviewer.ui.HTMLView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.PluggableTreeTableView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import com.sun.tools.visualvm.heapviewer.utils.HeapUtils;
import javax.swing.JScrollPane;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine.ObjectVisitor;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;
import org.netbeans.modules.profiler.oql.engine.api.ReferenceChain;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "OQLConsoleView_CannotResolveClassMsg=Cannot resolve class",
    "OQLConsoleView_CannotResolveInstanceMsg=Cannot resolve instance"
})
public class OQLConsoleView extends HeapViewerFeature {
    
    private static final Color SEPARATOR_COLOR = UIManager.getColor("Separator.foreground");
    
    private final HeapContext context;
    
    private ProfilerToolbar toolbar;
    private ProfilerToolbar pluginsToolbar;
    private ProfilerToolbar resultsToolbar;
    
    private JComponent component;
    
    private JButton runButton;
    private JButton cancelButton;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    
    private OQLEngine engine;
    private OQLEditor editor;
    
    private JPanel resultsContainer;
    private final HTMLView htmlView;
    private final PluggableTreeTableView objectsView;
    
    private JToggleButton rObjects;
    private JToggleButton rHTML;
    
    private final AtomicBoolean analysisRunning = new AtomicBoolean(false);
    private final ExecutorService progressUpdater = Executors.newSingleThreadExecutor();
    private boolean queryValid;
    
    // TODO: synchronize!
    private Set<HeapViewerNode> nodeResults;
    
    
    public OQLConsoleView(HeapContext context, HeapViewerActions actions) {
        super("java_objects_oql", "OQL Console", "OQL Console", Icons.getIcon(HeapWalkerIcons.OQL_CONSOLE), 1000);
        
        this.context = context;
        Heap heap = context.getFragment().getHeap();
        
        engine = null;
        if (OQLEngine.isOQLSupported()) try {
            engine = new OQLEngine(context.getFragment().getHeap());
        } catch (Exception e) {
//            ProfilerLogger.log(e);
        }
        
        if (engine != null) {
            TreeTableViewColumn[] ownColumns = new TreeTableViewColumn[] {
                new TreeTableViewColumn.Name(heap),
                new TreeTableViewColumn.Count(heap, false, false),
                new TreeTableViewColumn.OwnSize(heap, true, true),
                new TreeTableViewColumn.RetainedSize(heap, true, false)
            };

            objectsView = new PluggableTreeTableView("java_objects_oql", context, actions, ownColumns) {
                protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) {
                    if (nodeResults == null) return new HeapViewerNode[] { new TextNode("<no query executed yet>") };
                    else if (nodeResults.isEmpty()) return new HeapViewerNode[] { new TextNode("<no results>") };
                    else return nodeResults.toArray(HeapViewerNode.NO_NODES);
                }
            };
            objectsView.setViewName("Results");

            htmlView = new HTMLView("java_objects_oql", context, actions, "<p>&nbsp;&nbsp;&lt;no query executed yet&gt</p>") {
                protected HeapViewerNode nodeForURL(URL url, HeapContext context) {
                    return OQLConsoleView.getNode(url, context);
                }
            };
        } else {
            objectsView = null;
            htmlView = null;
        }
    }
    
    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        if (engine != null) {
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);

            toolbar.add(new GrayLabel("OQL Query:"));
            toolbar.addSpace(2);

            runButton = new JButton("Run", Icons.getIcon(GeneralIcons.START)) {
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
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    executeQuery();
                }
            };

            cancelButton = new JButton(Icons.getIcon(GeneralIcons.STOP)) {
                protected void fireActionPerformed(ActionEvent e) {
                    cancelQuery();
                }
            };
            cancelButton.setEnabled(false);

            progressLabel = new GrayLabel("Executing...");
            progressLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 5));
            progressLabel.setVisible(false);

            progressBar = new JProgressBar(JProgressBar.HORIZONTAL) {
                public Dimension getPreferredSize() {
                    Dimension dim = super.getPreferredSize();
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
            progressBar.setVisible(false);

            toolbar.add(runButton);
    //        toolbar.addSpace(2);
            toolbar.add(cancelButton);
    //        toolbar.addSpace(6);
            toolbar.add(progressLabel);
    //        toolbar.addSpace(5);
            toolbar.add(progressBar);

            resultsToolbar = ProfilerToolbar.create(false);

            resultsToolbar.addSpace(2);
            resultsToolbar.addSeparator();
            resultsToolbar.addSpace(5);

            resultsToolbar.add(new GrayLabel("Results:"));
            resultsToolbar.addSpace(3);

            ButtonGroup resultsBG = new ButtonGroup();

            rObjects = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS), true) {
                protected void fireItemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (resultsContainer != null) ((CardLayout)resultsContainer.getLayout()).first(resultsContainer);
                        if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(true);
                    }
                }
            };
            rObjects.setToolTipText("Objects");
            resultsBG.add(rObjects);
            resultsToolbar.add(rObjects);
            rHTML = new JToggleButton(Icons.getIcon(HeapWalkerIcons.PROPERTIES)) {
                protected void fireItemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (resultsContainer != null) ((CardLayout)resultsContainer.getLayout()).last(resultsContainer);
                        if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(false);
                    }
                }
            };
            rHTML.setToolTipText("HTML");
            resultsBG.add(rHTML);
            resultsToolbar.add(rHTML);

            if (objectsView.hasPlugins()) {
                pluginsToolbar = ProfilerToolbar.create(false);
    //            detailsToolbar.addSpace(2);
    //            detailsToolbar.addSeparator();
                pluginsToolbar.addSpace(8);

                pluginsToolbar.add(new GrayLabel("Details:"));
                pluginsToolbar.addSpace(2);

                pluginsToolbar.add(objectsView.getToolbar());

                resultsToolbar.add(pluginsToolbar);
            }

            toolbar.add(resultsToolbar);

            editor = new OQLEditor(engine);

            resultsContainer = new JPanel(new CardLayout());
            resultsContainer.add(objectsView.getComponent());
            resultsContainer.add(new ResultsView(htmlView.getComponent()));

            JExtendedSplitPane masterSplit = new JExtendedSplitPane(JExtendedSplitPane.VERTICAL_SPLIT, true, resultsContainer, new EditorView(editor));
            BasicSplitPaneDivider masterDivider = ((BasicSplitPaneUI)masterSplit.getUI()).getDivider();
            masterDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, SEPARATOR_COLOR));
            masterDivider.setDividerSize(6);
            masterSplit.setBorder(BorderFactory.createEmptyBorder());
            masterSplit.setResizeWeight(0.70d);

            component = new JPanel(new BorderLayout());
            component.add(masterSplit, BorderLayout.CENTER);

            editor.setScript("select x from java.io.File x");

            updateUIState();
        } else {
            component = new JPanel(new BorderLayout());
            component.setOpaque(true);
            component.setBackground(UIUtils.getProfilerResultsBackground());
            
            JLabel l = new JLabel("<OQL engine not available>", JLabel.CENTER);
            l.setEnabled(false);
            l.setOpaque(false);
            component.add(l, BorderLayout.CENTER);
        }
    }
    
    
    private void executeQuery() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (nodeResults == null) nodeResults = new HashSet();
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
                } catch (OQLException e) {

                }
                finalizeQuery(null);
            }
        });
    }
    
    public void queryStarted(final BoundedRangeModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateUIState();
                toolbar.remove(resultsToolbar);
                progressLabel.setVisible(true);
                progressBar.setVisible(true);
                progressBar.setModel(model);
            }
        });
    }

    public void queryFinished(final String result) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateUIState();
                progressLabel.setVisible(false);
                progressBar.setVisible(false);
                progressBar.setModel(null);
                toolbar.add(resultsToolbar);
                objectsView.reloadView();
                
                if (result != null) {
                    htmlView.setText(result);
                    if (result.length() > 0 && nodeResults.isEmpty()) rHTML.setSelected(true);
                }
            }
        });
    }
    
    private void finalizeQuery(String result) {
        analysisRunning.compareAndSet(true, false);
        queryFinished(result);
    }
    
    
    private void updateUIState() {
        if (analysisRunning.get()) {
            runButton.setEnabled(false);
            cancelButton.setEnabled(true);
            editor.setEditable(false);
            editor.setEnabled(false);
        } else {
            runButton.setEnabled(editor.getScript().length() > 0 && queryValid);
            cancelButton.setEnabled(false);
//            saveButton.setEnabled(editor.getScript().length() > 0 && queryValid);
            editor.setEditable(true);
            editor.setEnabled(true);
        }
    }
    
//    private void setResult(String result) {
//        htmlView.setText(result);
//    }
    
    
    private void executeQueryImpl(final String oqlQuery) {
        final BoundedRangeModel progressModel = new DefaultBoundedRangeModel(0, 10, 0, 100);

//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
                new RequestProcessor("OQL Query Processor").post(new Runnable() { //NOI18N
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
                            engine.executeQuery(oqlQuery, new ObjectVisitor() {
                                public boolean visit(Object o) {
//                                    System.err.println(">>> Visiting object " + o);
                                    sb.append(oddRow[0] ?
                                        "<tr><td style='background-color: " + // NOI18N
                                        oddRowBackgroundString + ";'>" : "<tr><td>"); // NOI18N
                                    oddRow[0] = !oddRow[0];
                                    dump(o, sb);
                                    sb.append("</td></tr>"); // NOI18N
                                    return counter.decrementAndGet() == 0 || (!analysisRunning.get() && !engine.isCancelled()); // process all hits while the analysis is running
                                }
                            });

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
                        } catch (OQLException oQLException) {
                            StringBuilder errorMessage = new StringBuilder();
                            errorMessage.append("<h2>").append("Query error").append("</h2>"); // NOI18N
                            errorMessage.append("Bad OQL query"); // NOI18N
                            errorMessage.append("<hr>"); // noi18n
                            errorMessage.append(oQLException.getLocalizedMessage().replace("\n", "<br>").replace("\r", "<br>"));
                            
                            finalizeQuery(errorMessage.toString());
                        }
                    }

                });
//            }
//        });
    }
    
    private void dump(Object o, StringBuilder sb) {
        if (o == null) {
            return;
        }
        if (o instanceof Instance) {
            Instance i = (Instance)o;
            nodeResults.add(new InstanceNode(i));
            sb.append(HeapUtils.instanceToHtml(i, true, context.getFragment().getHeap(), null));
        } else if (o instanceof JavaClass) {
            JavaClass c = (JavaClass)o;
            nodeResults.add(new ClassNode(c));
            sb.append(HeapUtils.classToHtml(c));
        } else if (o instanceof ReferenceChain) {
            ReferenceChain rc = (ReferenceChain) o;
            boolean first = true;
            while (rc != null) {
                if (!first) {
                    sb.append("-&gt;"); // NOI18N
                } else {
                    first = false;
                }
                o = rc.getObj();
                if (o instanceof Instance) {
                    Instance i = (Instance)o;
                    nodeResults.add(new InstanceNode(i));
                    sb.append(HeapUtils.instanceToHtml(i, true, context.getFragment().getHeap(), null));
                } else if (o instanceof JavaClass) {
                    JavaClass c = (JavaClass)o;
                    nodeResults.add(new ClassNode(c));
                    sb.append(HeapUtils.classToHtml(c));
                }
                rc = rc.getNext();
            }
        } else if (o instanceof Map) {
            Set<Map.Entry> entries = ((Map)o).entrySet();
            sb.append("<span><b>{</b><br/>"); // NOI18N
            boolean first = true;
            for(Map.Entry entry : entries) {
                if (!first) {
                    sb.append(",<br/>"); // NOI18N
                } else {
                    first = false;
                }
                sb.append(entry.getKey().toString().replace("<", "&lt;").replace(">", "&gt;")); // NOI18N
                sb.append(" = "); // NOI18N
                dump(unwrap(entry.getValue()), sb);
            }
            sb.append("<br/><b>}</b></span>"); // NOI18N
        } else if (o instanceof Object[]) {
            sb.append("<span><b>[</b>&nbsp;"); // NOI18N
            boolean first = true;
            for (Object obj1 : (Object[]) o) {
                if (!first) {
                    sb.append(", "); // NOI18N
                } else {
                    first = false;
                }
                dump(unwrap(obj1), sb);
            }
            sb.append("&nbsp;<b>]</b></span>"); // NOI18N
        } else {
            sb.append(o.toString());
        }
    }
    
    private Object unwrap(Object obj1) {
        Object obj2 = engine.unwrapJavaObject(obj1, true);
        return obj2 != null ? obj2 : obj1;
    }
    
    private static HeapViewerNode getNode(URL url, HeapContext context) {
        String urls = url.toString();
                
        if (HeapUtils.isInstance(urls)) {
            final Instance instance = HeapUtils.instanceFromHtml(urls, context.getFragment().getHeap());
            if (instance != null) return new InstanceNode(instance);
            else ProfilerDialogs.displayError(Bundle.OQLConsoleView_CannotResolveInstanceMsg());
        } else if (HeapUtils.isClass(urls)) {
            JavaClass javaClass = HeapUtils.classFromHtml(urls, context.getFragment().getHeap());
            if (javaClass != null) return new ClassNode(javaClass);
            else ProfilerDialogs.displayError(Bundle.OQLConsoleView_CannotResolveClassMsg());
        }

        return null;
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
        
        EditorView(OQLEditor editor) {
            super(new BorderLayout());
            
            editor.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            editor.setBackground(UIUtils.getProfilerResultsBackground());
            editor.addPropertyChangeListener(OQLEditor.VALIDITY_PROPERTY, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    queryValid = (Boolean)evt.getNewValue();
                    updateUIState();
                }
            });
            
            final JEditorPane ref = editor.getEditor();
            
            JTextField tf = new JTextField(" 1234 ");
            tf.setBorder(BorderFactory.createEmptyBorder());
            tf.setMargin(new Insets(0, 0, 0, 0));
            tf.setFont(editor.getFont());
            final int w = tf.getPreferredSize().width;
            
            final JEditorPane rows = new JEditorPane() {
                public Dimension getPreferredSize() {
                    Dimension dim = ref.getPreferredSize();
                    dim.width = w;
                    return dim;
                }
                public void setBackground(Color c) {
                    super.setBackground(new Color(245, 245, 245));
                }
            };
            
            rows.setFont(editor.getFont());
            rows.setEditorKit(ref.getEditorKit());
            
            rows.setCaret(new FollowingCaret(ref));
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < 1000; i++) {
                if (i < 10) sb.append(" ");
                if (i < 100) sb.append("  ");
//                if (i < 1000) sb.append(" ");
                sb.append(Integer.toString(i) + " \n");
            }
            rows.setText(sb.toString());
            rows.setEditable(false);
            rows.setEnabled(false);
            
            Insets margin = ref.getMargin();
            if (margin == null) margin = new Insets(0, 0, 0, 0);
            rows.setMargin(new Insets(margin.top, 0, margin.bottom, 0));
            
            Border border = ref.getBorder();
            if (border != null) {
                margin = border.getBorderInsets(ref);
                if (margin == null) margin = new Insets(0, 0, 0, 0);
                rows.setBorder(BorderFactory.createEmptyBorder(margin.top, -1, margin.bottom, 0));
            }
            
            JPanel editorContainer = new JPanel(new BorderLayout());
            editorContainer.add(rows, BorderLayout.WEST);
            editorContainer.add(editor, BorderLayout.CENTER);

            JScrollPane editorScroll = new JScrollPane(editorContainer,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            editorScroll.setBorder(BorderFactory.createEmptyBorder());
            editorScroll.setViewportBorder(BorderFactory.createEmptyBorder());
            editorScroll.getVerticalScrollBar().setUnitIncrement(10);
            editorScroll.getHorizontalScrollBar().setUnitIncrement(10);
            
            add(editorScroll, BorderLayout.CENTER);
//            add(new ScrollableContainer(editorContainer), BorderLayout.CENTER);
        }
        
    }
    
    private class ResultsView extends JPanel {
        
        ResultsView(JComponent results) {
            super(new BorderLayout());
            
            add(new ScrollableContainer(results), BorderLayout.CENTER);
        }
        
    }
    
    
    private static class FollowingCaret implements Caret {
                
        private final List<ChangeListener> listeners = new ArrayList();
        private int dot;

        FollowingCaret(final JTextComponent tc) {
            tc.getCaret().addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    setDot(followedPosition(tc));
                }
            });
            setDot(followedPosition(tc));
        }

        private static int followedPosition(JTextComponent tc) {
            Element root = tc.getDocument().getDefaultRootElement();
            return root.getElementIndex(tc.getCaretPosition()) * 6;
        }

        public void install(JTextComponent c) {}
        public void deinstall(JTextComponent c) {}
        public void paint(Graphics g) {}
        public void addChangeListener(ChangeListener l) { listeners.add(l); }
        public void removeChangeListener(ChangeListener l) { listeners.remove(l); }
        public boolean isVisible() { return false; }
        public void setVisible(boolean v) {}
        public boolean isSelectionVisible() { return false; }
        public void setSelectionVisible(boolean v) {}
        public void setMagicCaretPosition(Point p) {}
        public Point getMagicCaretPosition() { return new Point(0, 0); }
        public void setBlinkRate(int rate) {}
        public int getBlinkRate() { return 1; }
        public int getDot() { return dot; }
        public int getMark() { return dot; }
        public void setDot(int dot) {
            this.dot = dot;
            ChangeEvent e = new ChangeEvent(this);
            for (ChangeListener l : listeners) l.stateChanged(e);
        }
        public void moveDot(int dot) {}

    }
    
}
