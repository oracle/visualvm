/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
        
        public void dataChanged() {
            fireDataChanged();
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
        
        protected void fireRootChanged(TreeNode oldRoot, TreeNode newRoot) {
            if (listeners != null)
                for (Listener listener : listeners)
                    listener.rootChanged(oldRoot, newRoot);
        }
        
    }
    
    
    public static interface Listener {
        
        public void dataChanged();
        
        public void structureChanged();
        
        public void rootChanged(TreeNode oldRoot, TreeNode newRoot);
        
    }
    
    public static class Adapter implements Listener {
        
        public void dataChanged() {}
        
        public void structureChanged() {}
        
        public void rootChanged(TreeNode oldRoot, TreeNode newRoot) {}
        
    }
    
}
