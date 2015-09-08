/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.results;

import java.util.Collections;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;


/**
 * This interface must be implemented by every CCT node.
 *
 * @author Jiri Sedlacek
 */
public abstract class CCTNode implements TreeNode {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract CCTNode getChild(int index);

    public abstract CCTNode[] getChildren();

    public abstract int getIndexOfChild(Object child);

    public abstract int getNChildren();

    public abstract CCTNode getParent();

    //public boolean hasChildren();
    
    
    // --- Filtering support ---
    
    private boolean filtered;
    
    public CCTNode createFilteredNode() { return null; }
    
    protected void setFilteredNode() { filtered = true; }
    
    public boolean isFiltered() { return filtered; }
    
    public void merge(CCTNode node) {}
    
    // ---
    
    
    //--- TreeNode adapter ---
    public Enumeration<CCTNode> children() {
        final CCTNode[] _children = getChildren();
        final int _childrenCount = _children == null ? 0 : _children.length;
        
        if (_childrenCount == 0) return Collections.emptyEnumeration();
        
        return new Enumeration<CCTNode>() {
            private int index = 0;
            public boolean hasMoreElements() { return index < _childrenCount; }
            public CCTNode nextElement()     { return _children[index++]; }
        };
    }
    
    public boolean isLeaf() {
        return getChildCount() == 0;
    }
    
    public boolean getAllowsChildren() {
        return true;
    }
    
    public int getIndex(TreeNode node) {
        return getIndexOfChild(node);
    }
    
    public int getChildCount() {
        return getNChildren();
    }
    
    public TreeNode getChildAt(int index) {
        return getChild(index);
    }
    //---
}
