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

package org.netbeans.modules.profiler.heapwalk.model;

import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.SwingUtilities;


/**
 * Implements common methods of all Fields Browser nodes
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractHeapWalkerNode implements HeapWalkerNode {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HeapWalkerNode parent;
    private Icon icon;
    private String name;
    private String type;
    private String value;
    private String size;
    private String retainedSize;
    private HeapWalkerNode[] children;
    private int mode = HeapWalkerNode.MODE_FIELDS;

    private Map<Object, Integer> indexes;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AbstractHeapWalkerNode(HeapWalkerNode parent) {
        this(parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
    }

    public AbstractHeapWalkerNode(HeapWalkerNode parent, int mode) {
        this.parent = parent;
        this.mode = mode;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HeapWalkerNode getChild(int i) {
        return getChildren()[i];
    }

    public HeapWalkerNode[] getChildren() {
        if (children == null) {
            children = computeChildren();
            indexes = null;
        }

        return children;
    }

    private Map<Object, Integer> getIndexes() {
        if (indexes == null) {
            HeapWalkerNode[] chldrn = getChildren();
            indexes = new HashMap(chldrn.length * 4 / 3);
            for (int i = 0; i < chldrn.length; i++)
                indexes.put(chldrn[i], i);
        }

        return indexes;
    }

    public Icon getIcon() {
        if (icon == null) {
            icon = computeIcon();
        }

        return icon;
    }

    public int getIndexOfChild(Object object) {
//        for (int i = 0; i < getChildren().length; i++) {
//            if (getChildren()[i] == object) {
//                return i;
//            }
//        }
//
//        return -1;
        Integer index = getIndexes().get(object);
        return index != null ? index : -1;
    }

    // Should be overridden for lazy populating children
    public boolean isLeaf() {
        return getNChildren() == 0;
    }

    public int getMode() {
        return mode;
    }

    public int getNChildren() {
        if (getChildren() == null) {
            return 0;
        } else {
            return getChildren().length;
        }
    }

    public String getName() {
        if (name == null) {
            name = computeName();
        }

        return name;
    }

    public HeapWalkerNode getParent() {
        return parent;
    }

    public boolean isRoot() {
        return getParent() == null;
    }

    public String getSimpleType() {
        return BrowserUtils.getSimpleType(getType());
    }

    public String getType() {
        if (type == null) {
            type = computeType();
        }

        return type;
    }

    public String getValue() {
        if (value == null) {
            value = computeValue();
        }

        return value;
    }

    public String getSize() {
        if (size == null) {
            size = computeSize();
        }

        return size;
    }

    public String getRetainedSize() {
        if (retainedSize == null) {
            retainedSize = computeRetainedSize();
        }

        return retainedSize;
    }

    // used for testing children for null without lazy-populating invocation
    // note that if false, it means that chilren are not yet computed OR this node is leaf!
    public boolean currentlyHasChildren() {
        return children != null;
    }

    public String toString() {
        return getName();
    }

    protected abstract Icon computeIcon();

    protected abstract String computeName();

    protected abstract String computeType();

    protected abstract String computeValue();

    protected abstract String computeSize();

    protected abstract String computeRetainedSize();

    // Used for explicit setting children, shouldn't be used!
    protected void setChildren(HeapWalkerNode[] children) {
        changeChildren(children);
    }

    // Should be overridden for lazy populating children
    protected HeapWalkerNode[] computeChildren() {
        return new HeapWalkerNode[0];
    }

    // Used for updating lazily created children, shouldn't be used for any other purpose!
    void changeChildren(final HeapWalkerNode[] children) {
        Runnable childrenChanger = new Runnable() {
            public void run() {
                AbstractHeapWalkerNode.this.children = children;
                indexes = null;
            }
        };
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(childrenChanger);
            } catch (Exception ex) {}
        } else {
            childrenChanger.run();
        }
    }
}
