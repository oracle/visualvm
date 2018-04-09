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

package org.netbeans.modules.profiler.heapwalk.model;

import org.netbeans.lib.profiler.heap.*;
import org.openide.util.NbBundle;
import java.util.List;


/**
 * Represents org.netbeans.lib.profiler.heap.ObjectArrayInstance
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ObjectArrayNode_ItemsNumberString=({0} items)",
    "ObjectArrayNode_LoopToString=(loop to {0})"
})
public class ObjectArrayNode extends ArrayNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class ArrayItem extends ObjectArrayNode implements org.netbeans.modules.profiler.heapwalk.model.ArrayItem {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private int itemIndex;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ArrayItem(int itemIndex, ObjectArrayInstance instance, HeapWalkerNode parent) {
            this(itemIndex, instance, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
        }

        public ArrayItem(int itemIndex, ObjectArrayInstance instance, HeapWalkerNode parent, int mode) {
            super(instance, null, parent, mode);

            this.itemIndex = itemIndex;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getItemIndex() {
            return itemIndex;
        }

        protected String computeName() {
            String name = "[" + itemIndex + "]"; // NOI18N

            if (isLoop()) {
                return name + " "
                       + Bundle.ObjectArrayNode_LoopToString(BrowserUtils.getFullNodeName(getLoopTo()));
            }

            return name;
        }

        protected String computeType() {
            if (!hasInstance()) {
                return "<" + BrowserUtils.getArrayItemType(getType()) + ">"; // NOI18N
            }

            return super.computeType();
        }
        
        private String nodeID;
        public Object getNodeID() {
            if (nodeID == null)
                nodeID = itemIndex + "#" + (hasInstance() ? getInstance().getInstanceId() : "null"); // NOI18N
            return nodeID;
        }
    }

    public abstract static class RootNode extends ObjectArrayNode implements org.netbeans.modules.profiler.heapwalk.model.RootNode {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public RootNode(ObjectArrayInstance instance, String name, HeapWalkerNode parent) {
            super(instance, name, parent);
        }

        public RootNode(ObjectArrayInstance instance, String name, HeapWalkerNode parent, int mode) {
            super(instance, name, parent, mode);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public abstract void refreshView();
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ObjectArrayNode(ObjectArrayInstance instance, String name, HeapWalkerNode parent) {
        super(instance, name, parent);
    }

    public ObjectArrayNode(ObjectArrayInstance instance, String name, HeapWalkerNode parent, int mode) {
        super(instance, name, parent, mode);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ObjectArrayInstance getInstance() {
        return (ObjectArrayInstance) super.getInstance();
    }

    public boolean isPrimitive() {
        return false;
    }

    protected ChildrenComputer getChildrenComputer() {
        return new ChildrenComputer() {
            public HeapWalkerNode[] computeChildren() {
                HeapWalkerNode[] children = null;

                if (getMode() == HeapWalkerNode.MODE_FIELDS) {
                    int fieldsSize = getInstance().getLength();

                    if (fieldsSize == 0) {
                        // Array has no items
                        children = new HeapWalkerNode[1];
                        children[0] = HeapWalkerNodeFactory.createNoItemsNode(ObjectArrayNode.this);
                    } else if (fieldsSize > HeapWalkerNodeFactory.ITEMS_COLLAPSE_UNIT_SIZE) {
                        int childrenCount = fieldsSize;
                        BrowserUtils.GroupingInfo groupingInfo = BrowserUtils.getGroupingInfo(childrenCount);
                        int containersCount = groupingInfo.containersCount;
                        int collapseUnitSize = groupingInfo.collapseUnitSize;

                        children = new HeapWalkerNode[containersCount];

                        for (int i = 0; i < containersCount; i++) {
                            int unitStartIndex = collapseUnitSize * i;
                            int unitEndIndex = Math.min(unitStartIndex + collapseUnitSize, childrenCount) - 1;
                            children[i] = HeapWalkerNodeFactory.createArrayItemContainerNode(ObjectArrayNode.this,
                                                                                             unitStartIndex, unitEndIndex);
                        }
                    } else {
                        // TODO: currently the below is a kind of logical view - fields view should also be available!
                        List fields = getInstance().getValues();
                        children = new HeapWalkerNode[fields.size()];

                        for (int i = 0; i < children.length; i++) {
                            children[i] = HeapWalkerNodeFactory.createObjectArrayItemNode(ObjectArrayNode.this, i,
                                                                                          (Instance) fields.get(i));
                        }
                    }
                } else if (getMode() == HeapWalkerNode.MODE_REFERENCES) {
                    children = HeapWalkerNodeFactory.createReferences(ObjectArrayNode.this);
                }

                return children;
            }
        };
    }

}
