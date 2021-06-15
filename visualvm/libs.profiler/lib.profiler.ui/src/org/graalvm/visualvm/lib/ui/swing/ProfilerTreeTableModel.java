/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.swing;

import java.util.HashSet;
import java.util.Set;
import javax.swing.tree.TreeNode;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ProfilerTreeTableModel {

    public TreeNode getRoot();

    public int getColumnCount();

    public Class getColumnClass(int column);

    public String getColumnName(int column);

    public void setValueAt(Object aValue, TreeNode node, int column);

    public Object getValueAt(TreeNode node, int column);

    public boolean isCellEditable(TreeNode node, int column);

    public void addListener(Listener listener);

    public void removeListener(Listener listener);


    public static abstract class Abstract implements ProfilerTreeTableModel {

        private TreeNode root;

        private Set<Listener> listeners;

        public Abstract(TreeNode root) {
            if (root == null) throw new NullPointerException("Root cannot be null"); // NOI18N
            this.root = root;
        }

        public void dataChanged() {
            fireDataChanged();
        }

        public void structureChanged() {
            fireStructureChanged();
        }

        public void childrenChanged(TreeNode node) {
            fireChildrenChanged(node);
        }

        public void setRoot(TreeNode newRoot) {
            TreeNode oldRoot = root;
            root = newRoot;
            fireRootChanged(oldRoot, newRoot);
        }

        public TreeNode getRoot() {
            return root;
        }

        public void addListener(Listener listener) {
            if (listeners == null) listeners = new HashSet();
            listeners.add(listener);
        }

        public void removeListener(Listener listener) {
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) listeners = null;
            }
        }
        
        protected void fireDataChanged() {
            if (listeners != null)
                for (Listener listener : listeners)
                    listener.dataChanged();
        }
        
        protected void fireStructureChanged() {
            if (listeners != null)
                for (Listener listener : listeners)
                    listener.structureChanged();
        }
        
        protected void fireChildrenChanged(TreeNode node) {
            if (listeners != null)
                for (Listener listener : listeners)
                    listener.childrenChanged(node);
        }
        
        protected void fireRootChanged(TreeNode oldRoot, TreeNode newRoot) {
            if (listeners != null)
                for (Listener listener : listeners)
                    listener.rootChanged(oldRoot, newRoot);
        }
        
    }
    
    
    public static interface Listener {
        
        public void dataChanged();
        
        public void structureChanged();
        
        public void childrenChanged(TreeNode node);
        
        public void rootChanged(TreeNode oldRoot, TreeNode newRoot);
        
    }
    
    public static class Adapter implements Listener {
        
        public void dataChanged() {}
        
        public void structureChanged() {}
        
        public void childrenChanged(TreeNode node) {}
        
        public void rootChanged(TreeNode oldRoot, TreeNode newRoot) {}
        
    }
    
}
