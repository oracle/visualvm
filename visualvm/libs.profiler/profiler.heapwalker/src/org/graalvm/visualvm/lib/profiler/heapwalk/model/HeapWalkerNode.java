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

package org.graalvm.visualvm.lib.profiler.heapwalk.model;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import javax.swing.Icon;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


/**
 * This interface must be implemented by each node used in Fields Browser
 * Note: currently implements CCTNode just for compatibility with TreeTableModel
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapWalkerNode extends CCTNode {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int MODE_FIELDS = 1;
    public static final int MODE_REFERENCES = 2;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract HeapWalkerNode getChild(int index);

    public abstract HeapWalkerNode[] getChildren();

    public abstract Icon getIcon();

//    public abstract int getIndexOfChild(Object child);

//    public abstract boolean isLeaf();

//    public abstract int getNChildren();

    public abstract String getName();

    public abstract HeapWalkerNode getParent();

    public abstract boolean isRoot();

    public abstract String getSimpleType();

    public abstract String getType();

    public abstract String getValue();
    
    public abstract String getDetails();

    public abstract String getSize();

    public abstract String getRetainedSize();
    
    // used for equals() and hashCode() implementation
    public abstract Object getNodeID();

    // used for testing children for null without lazy-populating invocation
    // note that if false, it means that children are not yet computed OR this node is leaf!
    public abstract boolean currentlyHasChildren();

    /**
     * Used to get information if node is used within Fields Browser or References Browser
     * There are two different algorithms for generating childs in both Browsers.
     */
    public abstract int getMode();
    
    
    public static TreePath fromNode(TreeNode node) {
        return fromNode(node, null);
    }
    
    public static TreePath fromNode(TreeNode node, TreeNode root) {
        List l = new ArrayList();
        while (node != root) {
            l.add(0, node);
            node = node.getParent();
        }
        if (node != null) l.add(0, node);
        return new TreePath(l.toArray(new Object[0]));
    }
}
