/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.explorer;

import org.graalvm.visualvm.core.datasource.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import org.openide.windows.WindowManager;

/**
 * Class for accessing the explorer tree.
 *
 * @author Jiri Sedlacek
 */
public final class ExplorerSupport {

    private static ExplorerSupport sharedInstance;

    private JTree mainTree;
    
    private Set<ExplorerSelectionListener> selectionListeners = Collections.synchronizedSet(new HashSet());
    private Set<ExplorerExpansionListener> expansionListeners = Collections.synchronizedSet(new HashSet());


    /**
     * Returns singleton instance of ExplorerSupport.
     * 
     * @return singleton instance of ExplorerSupport.
     */
    public static synchronized ExplorerSupport sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ExplorerSupport();
        return sharedInstance;
    }

    
    /**
     * Returns current DataSource position within its owner DataSource in explorer tree or -1 if the position cannot be determined.
     * 
     * @param dataSource DataSource for which to get the position.
     * @return current DataSource position within its owner DataSource in explorer tree or -1 if the position cannot be determined.
     */
    public int getDataSourcePosition(DataSource dataSource) {
        ExplorerNode node = getNode(dataSource);
        if (node == null) return -1;
        ExplorerNode parentNode = (ExplorerNode)node.getParent();
        if (parentNode == null) return -1;
        return parentNode.getIndex(node);
    }

    /**
     * Selects DataSource in explorer tree.
     * 
     * @param dataSource DataSource to be selected.
     */
    public void selectDataSource(final DataSource dataSource) {
        if (dataSource == null) return;
        selectDataSources(Collections.singleton(dataSource));
    }
    
    /**
     * Selects multiple DataSources in explorer tree.
     * 
     * @param dataSources DataSources to be selected.
     */
    public void selectDataSources(final Set<DataSource> dataSources) {
        if (dataSources.isEmpty()) return;
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                List<TreePath> selectedPaths = new ArrayList();
                for (DataSource dataSource : dataSources) {
                    ExplorerNode node = getNode(dataSource);
                    if (node != null) selectedPaths.add(getPath(node));
                }
                mainTree.setSelectionPaths(selectedPaths.isEmpty() ? null : selectedPaths.toArray(new TreePath[selectedPaths.size()]));
            } 
        });
    }
    
    /**
     * Clears selection of explorer tree.
     */
    public void clearSelection() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() { mainTree.clearSelection(); } 
        });
    }
    
    /**
     * Returns selected DataSources in explorer tree.
     * 
     * @return selected DataSources in explorer tree.
     */
    public Set<DataSource> getSelectedDataSources() {
        if (mainTree == null) return Collections.EMPTY_SET;
        
        TreePath[] selectedPaths = mainTree.getSelectionPaths();
        if (selectedPaths == null) return Collections.EMPTY_SET;
        
        Set<DataSource> selectedDataSources = new HashSet();
        for (TreePath treePath : selectedPaths) {
            DataSource dataSource = getDataSource(treePath);
            if (dataSource != null) selectedDataSources.add(dataSource);
        }
        return selectedDataSources;
    }
    
    /**
     * Adds a listener to receive notifications about explorer tree selection change.
     * 
     * @param listener listener to add.
     */
    public void addSelectionListener(ExplorerSelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    /**
     * Removes explorer tree selection listener.
     * @param listener listener to remove.
     */
    public void removeSelectionListener(ExplorerSelectionListener listener) {
        selectionListeners.remove(listener);
    }
    
    
    Set<DataSource> getExpandedDataSources(DataSource origin) {
        if (mainTree == null) return Collections.EMPTY_SET;
        
        Enumeration<TreePath> expandedPaths = mainTree.getExpandedDescendants(getPath(getNode(origin)));
        if (expandedPaths == null) return Collections.EMPTY_SET;
        
        Set<DataSource> expandedDataSources = new HashSet();
        while (expandedPaths.hasMoreElements()) {
            DataSource dataSource = getDataSource(expandedPaths.nextElement());
            if (dataSource != null) expandedDataSources.add(dataSource);
        }
        return expandedDataSources;
    }
    
    void expandDataSources(final Set<DataSource> dataSources) {
        if (dataSources.isEmpty()) return;
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                for (DataSource dataSource : dataSources) {
                    ExplorerNode node = getNode(dataSource);
                    if (node != null) mainTree.expandPath(getPath(node));
                }
            } 
        });
    }
    
    /**
     * Expands DataSource if displayed and collapsed in explorer tree.
     * 
     * @param dataSource DataSource to expand.
     */
    public void expandDataSource(DataSource dataSource) {
        expandNode(getNode(dataSource));
    }
    
    void expandNode(final ExplorerNode node) {
        if (node == null) return;
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() { 
                TreePath path = getPath(node);
                // For some reason expanding the path doesn't always work for a single invocation,
                // invoking twice to be sure
                mainTree.expandPath(path);
                mainTree.expandPath(path);
            } 
        });
    }
    
    /**
     * Collapses DataSource if displayed and expanded in explorer tree.
     * 
     * @param dataSource DataSource to collapse.
     */
    public void collapseDataSource(DataSource dataSource) {
        collapseNode(getNode(dataSource));
    }
    
    void collapseNode(final ExplorerNode node) {
        if (node == null) return;
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() { mainTree.collapsePath(getPath(node)); } 
        });
    }
    
    /**
     * Adds a listener to receive notifications about expanded/collapsed explorer tree nodes.
     * 
     * @param listener listener to add.
     */
    public void addExpansionListener(ExplorerExpansionListener listener) {
        expansionListeners.add(listener);
    }
    
    /**
     * Removes explorer tree expansion listener.
     * 
     * @param listener listener to remove.
     */
    public void removeExpansionListener(ExplorerExpansionListener listener) {
        expansionListeners.remove(listener);
    }
    
    
    DataSource getDataSource(TreePath path) {
        if (path == null) return null;
        ExplorerNode node = (ExplorerNode)path.getLastPathComponent();
        return node.getUserObject();
    }
    
    ExplorerNode getNode(DataSource dataSource) {
        return ExplorerModelBuilder.getInstance().getNodeFor(dataSource);
    }
    
    TreePath getPath(ExplorerNode node) {
        return new TreePath(node.getPath());
    }
            
    
    private ExplorerSupport() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                mainTree = ExplorerComponent.instance().getTree();
                mainTree.addTreeSelectionListener(new ExplorerTreeSelectionListener());
                mainTree.addTreeExpansionListener(new ExplorerTreeExpansionListener());
            }
        });
    }
    
    
    private class ExplorerTreeSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            Set<DataSource> selectedDataSources = getSelectedDataSources();
            Set<ExplorerSelectionListener> listeners = new HashSet(selectionListeners);
            for (ExplorerSelectionListener listener : listeners) listener.selectionChanged(selectedDataSources);
        }
        
    }
    
    private class ExplorerTreeExpansionListener implements TreeExpansionListener {

        public void treeExpanded(TreeExpansionEvent event) {
            DataSource expandedDataSource = getDataSource(event.getPath());
            if (expandedDataSource != null) {
                Set<ExplorerExpansionListener> listeners = new HashSet(expansionListeners);
                for (ExplorerExpansionListener listener : listeners) listener.dataSourceExpanded(expandedDataSource);
            }
        }

        public void treeCollapsed(TreeExpansionEvent event) {
            DataSource collapsedDataSource = getDataSource(event.getPath());
            if (collapsedDataSource != null) {
                Set<ExplorerExpansionListener> listeners = new HashSet(expansionListeners);
                for (ExplorerExpansionListener listener : listeners) listener.dataSourceCollapsed(collapsedDataSource);
            }
        }
        
    }

}
