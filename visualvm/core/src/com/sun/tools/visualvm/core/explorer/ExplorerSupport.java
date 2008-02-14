/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

/**
 * Class for accessing the explorer tree.
 *
 * @author Jiri Sedlacek
 */
public class ExplorerSupport {

    private static ExplorerSupport sharedInstance;

    private JTree mainTree;
//    private DefaultTreeModel mainTreeModel;
    
    private Set<ExplorerSelectionListener> selectionListeners = Collections.synchronizedSet(new HashSet());


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
     * Selects an ExplorerNode representing given DataSource in explorer tree.
     * 
     * @param dataSource DataSource to be selected.
     */
    public void selectDataSource(DataSource dataSource) {
        if (dataSource == null) return;
        ExplorerNode node = getNode(dataSource);
        if (node != null) mainTree.setSelectionPath(getPath(node));
    }
    
    public void clearSelection() {
        mainTree.clearSelection();
    }
    
    public DataSource getSelectedDataSource() {
        return getDataSource(mainTree.getSelectionPath());
    }
    
    public void addSelectionListener(ExplorerSelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    public void removeSelectionListener(ExplorerSelectionListener listener) {
        selectionListeners.remove(listener);
    }
    
    /**
     * Opens the explorer tree window (Applications).
     */
    public void openExplorer() {
        ExplorerTopComponent.getInstance().open();
    }
    
    
    private DataSource getDataSource(TreePath path) {
        if (path == null) return null;
        ExplorerNode node = (ExplorerNode)path.getLastPathComponent();
        return node.getUserObject();
    }
    
    private ExplorerNode getNode(DataSource dataSource) {
        return ExplorerModelBuilder.getInstance().getNodeFor(dataSource);
    }
    
    private TreePath getPath(ExplorerNode node) {
        return new TreePath(node.getPath());
    }
            
    
    private ExplorerSupport() {
        mainTree = ExplorerUI.instance().getTree();
        mainTree.addTreeSelectionListener(new ExplorerTreeSelectionListener());
//        mainTreeModel = ExplorerModelBuilder.getInstance().getModel();
        OpenDataSourceSupport.getInstance().initialize();
    }
    
    
    private class ExplorerTreeSelectionListener implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            DataSource selectedDataSource = getSelectedDataSource();
            Set<ExplorerSelectionListener> listeners = new HashSet(selectionListeners);
            for (ExplorerSelectionListener listener : listeners) listener.selectionChanged(selectedDataSource);
        }
        
    }

}
