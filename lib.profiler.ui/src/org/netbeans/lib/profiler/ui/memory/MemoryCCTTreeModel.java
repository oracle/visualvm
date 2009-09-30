/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.memory;

import org.netbeans.lib.profiler.results.memory.*;
import javax.swing.tree.*;


/**
 * Implementation of TreeModel for Memory CCT Trees
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public class MemoryCCTTreeModel implements TreeModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private PresoObjAllocCCTNode root;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of MemoryCCTTreeModel */
    public MemoryCCTTreeModel(PresoObjAllocCCTNode root) {
        this.root = root;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object getChild(Object obj, int index) {
        if (obj == null) {
            return null;
        }

        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode) obj;

        return node.getChild(index);
    }

    public int getChildCount(Object obj) {
        if (obj == null) {
            return -1;
        }

        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode) obj;

        return node.getNChildren();
    }

    public int getIndexOfChild(Object parentObj, Object childObj) {
        if ((parentObj == null) || (childObj == null)) {
            return -1;
        }

        PresoObjAllocCCTNode parent = (PresoObjAllocCCTNode) parentObj;
        PresoObjAllocCCTNode child = (PresoObjAllocCCTNode) childObj;

        return parent.getIndexOfChild(child);
    }

    public boolean isLeaf(Object obj) {
        if (obj == null) {
            return true;
        }

        PresoObjAllocCCTNode node = (PresoObjAllocCCTNode) obj;

        return (node.getNChildren() == 0);
    }

    public Object getRoot() {
        return root;
    }

    public void addTreeModelListener(javax.swing.event.TreeModelListener treeModelListener) {
    }

    public void removeTreeModelListener(javax.swing.event.TreeModelListener treeModelListener) {
    }

    // --------------------------------------------------------------  

    // TreeModel interface methods that we don't implement
    public void valueForPathChanged(javax.swing.tree.TreePath treePath, Object obj) {
    }
}
