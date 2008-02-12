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
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Class for accessing the explorer tree.
 *
 * @author Jiri Sedlacek
 */
public class ExplorerSupport {

    private static ExplorerSupport sharedInstance;

    private JTree mainTree;
    private DefaultTreeModel mainTreeModel;


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
     * Returns a TreePath instance of given ExplorerNode.
     * 
     * @param node ExplorerNode to get the TreePath for.
     * @return TreePath instance of given ExplorerNode.
     */
    public TreePath getTreePath(ExplorerNode node) {
        return new TreePath(mainTreeModel.getPathToRoot(node));
    }

    /**
     * Expands given ExplorerNode in explorer tree.
     * 
     * @param node ExplorerNode to be expanded.
     */
    public void expandNode(final ExplorerNode node) {
        SwingUtilities.invokeLater(new Runnable() {
           public void run() { mainTree.expandPath(getTreePath(node)); } 
        });
    }
    
    /**
     * Collapses given ExplorerNode in explorer tree.
     * 
     * @param node ExplorerNode to be collapsed.
     */
    public void collapseNode(final ExplorerNode node) {
        SwingUtilities.invokeLater(new Runnable() {
           public void run() { mainTree.collapsePath(getTreePath(node)); } 
        });
    }
    
    /**
     * Returns true if given ExplorerNode appears expanded in explorer tree, false otherwise.
     * 
     * @param node ExplorerNode to get the expansion state for.
     * @return true if given ExplorerNode appears expanded in explorer tree, false otherwise.
     */
    public boolean isNodeExpanded(ExplorerNode node) {
        return mainTree.isExpanded(getTreePath(node));
    }
    
    /**
     * Selects an ExplorerNode representing given DataSource in explorer tree.
     * 
     * @param dataSource DataSource to be selected.
     */
    public void selectDataSource(DataSource dataSource) {
        ExplorerNode node = ExplorerModelSupport.sharedInstance().getNodeFor(dataSource);
        if (node != null) selectNode(node);
    }
    
    /**
     * Selects given ExplorerNode in explorer tree.
     * 
     * @param node ExplorerNode to be selected.
     */
    public void selectNode(final ExplorerNode node) {
        SwingUtilities.invokeLater(new Runnable() {
           public void run() { getTreeSelectionModel().setSelectionPath(getTreePath(node)); } 
        });
    }
    
    /**
     * Returns TreeSelectionModel of explorer tree.
     * 
     * @return TreeSelectionModel of explorer tree.
     */
    public TreeSelectionModel getTreeSelectionModel() {
        return mainTree.getSelectionModel();
    }
    
    /**
     * Opens the explorer tree window (Applications).
     */
    public void openExplorer() {
        ExplorerTopComponent.getInstance().open();
    }
            
    
    private ExplorerSupport() {
        mainTree = ExplorerUI.instance().getTree();
        mainTreeModel = ExplorerModelSupport.sharedInstance().getExplorerModel();
        OpenDataSourceSupport.getInstance().initialize();
    }

}
