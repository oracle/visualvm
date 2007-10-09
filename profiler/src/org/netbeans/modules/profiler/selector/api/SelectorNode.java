/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.selector.api;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.components.tree.CheckTreeNode;
import org.netbeans.modules.profiler.selector.api.nodes.ContainerNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.Icon;
import javax.swing.tree.TreeNode;


/**
 *
 * @author Jaroslav Bachorik
 */
public class SelectorNode extends CheckTreeNode {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private SelectorChildren children;
    private String nodeName;
    private boolean valid = true;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of SelectorNode */
    public SelectorNode(String displayName, String name, Icon icon, SelectorChildren children) {
        this(displayName, name, icon, children, null);
    }

    public SelectorNode(String displayName, String name, Icon icon, SelectorChildren children, ContainerNode parent) {
        super(displayName, icon);
        this.nodeName = name;
        this.children = children;

        if (this.children != null) {
            this.children.setParent(this);
        }

        setParent(parent);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public TreeNode getChildAt(int childIndex) {
        int size = children.getNodes().size();

        return (TreeNode) (((childIndex <= size) && (childIndex >= 0)) ? children.getNodes().get(childIndex) : null);
    }

    public int getChildCount() {
        return children.getNodeCount();
    }

    public int getChildCount(boolean forceRefresh) {
        return children.getNodeCount(forceRefresh);
    }

    public int getIndex(TreeNode node) {
        return children.getNodes().indexOf(node);
    }

    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    public Collection<ClientUtils.SourceCodeSelection> getRootMethods(boolean all) {
        Collection<ClientUtils.SourceCodeSelection> roots = new ArrayList<ClientUtils.SourceCodeSelection>();

        if (all || isFullyChecked()) {
            ClientUtils.SourceCodeSelection signature = getSignature();

            if (signature != null) {
                roots.add(signature);
            }
        }

        return roots;
    }

    public final Collection<ClientUtils.SourceCodeSelection> getRootMethods() {
        return getRootMethods(false);
    }

    public String getNodeName() {
        return nodeName;
    }

    public ContainerNode getParent() {
        TreeNode parent = super.getParent();

        if ((parent == null) || (!(parent instanceof ContainerNode))) {
            return null;
        }

        return (ContainerNode) parent;
    }

    public Project getProject() {
        ContainerNode parent = getParent();

        return (parent != null) ? parent.getProject() : null;
    }

    public boolean isShowingInheritedMethods() {
        ContainerNode parent = getParent();

        return (parent != null) ? parent.isShowingInheritedMethods() : false;
    }

    public ClientUtils.SourceCodeSelection getSignature() {
        return null;
    }

    public boolean isValid() {
        return valid;
    }

    public Enumeration children() {
        return Collections.enumeration(children.getNodes());
    }

    //  @Override
    //  public TreeNode getParent() {
    //    return parent;
    //  }
    //  
    //  public void setParent(ContainerNode parent) {
    //    this.parent = parent;
    //  }
    public void detach() {
        this.parent = null;
    }

    @Override
    public boolean equals(Object anotherNode) {
        if (anotherNode == null) {
            return false;
        }

        if (!(anotherNode instanceof SelectorNode)) {
            return false;
        }

        if ((((SelectorNode) anotherNode).getSignature() != null) && (getSignature() != null)) {
            return (((SelectorNode) anotherNode).getSignature().equals(getSignature()));
        }

        return ((SelectorNode) anotherNode).getNodeName().equals(getNodeName());
    }

    @Override
    public int hashCode() {
        return getNodeName().hashCode() + ((getSignature() != null) ? getSignature().hashCode() : 0);
    }

    public String toString() {
        return getUserObject().toString();
    }

    protected void setChildren(SelectorChildren children) {
        this.children = children;
        this.children.setParent(this);
    }

    protected void setValid(boolean value) {
        valid = value;
    }
}
