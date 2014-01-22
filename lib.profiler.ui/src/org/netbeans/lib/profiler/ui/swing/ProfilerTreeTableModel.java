/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.lib.profiler.ui.swing;

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
        
        protected void fireStructureChanged() {
            if (listeners != null)
                for (Listener listener : listeners)
                    listener.structureChanged();
        }
        
        protected void fireRootChanged(TreeNode oldRoot, TreeNode newRoot) {
            if (listeners != null)
                for (Listener listener : listeners)
                    listener.rootChanged(oldRoot, newRoot);
        }
        
    }
    
    
    public static interface Listener {
        
        public void structureChanged();
        
        public void rootChanged(TreeNode oldRoot, TreeNode newRoot);
        
    }
    
}
