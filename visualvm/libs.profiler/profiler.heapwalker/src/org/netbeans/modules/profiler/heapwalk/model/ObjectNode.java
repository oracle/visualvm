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


import org.openide.util.NbBundle;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Instance;


/**
 * Represents org.netbeans.lib.profiler.heap.Instance
 * (which is not PrimitiveArrayInstance nor ObjectArrayInstance)
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ObjectNode_LoopToString=(loop to {0})"
})
public class ObjectNode extends InstanceNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class ArrayItem extends ObjectNode implements org.netbeans.modules.profiler.heapwalk.model.ArrayItem {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String ownerArrayType;
        private int itemIndex;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ArrayItem(int itemIndex, Instance instance, HeapWalkerNode parent) {
            this(itemIndex, instance, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
        }

        public ArrayItem(int itemIndex, Instance instance, HeapWalkerNode parent, int mode) {
            super(instance, null, parent, mode);

            this.itemIndex = itemIndex;
            this.ownerArrayType = parent.getType();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getItemIndex() {
            return itemIndex;
        }

        protected String computeName() {
            String name = "[" + itemIndex + "]"; // NOI18N

            if (isLoop()) {
                name += (" " + Bundle.ObjectNode_LoopToString(BrowserUtils.getFullNodeName(getLoopTo())));
            }

            if ((getMode() == HeapWalkerNode.MODE_REFERENCES) && getInstance().isGCRoot()) {
                HeapWalkerNode root = BrowserUtils.getRoot(this);

                if (root instanceof RootNode) {
                    GCRoot gcRoot = ((RootNode) root).getGCRoot(getInstance());

                    if (gcRoot != null) {
                        name += (" (" + gcRoot.getKind() + ")"); // NOI18N
                    }
                }
            }

            return name;
        }

        protected String computeType() {
            if (!hasInstance()) {
                return "<" + BrowserUtils.getArrayItemType(ownerArrayType) + ">"; // NOI18N
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

    public abstract static class RootNode extends ObjectNode implements org.netbeans.modules.profiler.heapwalk.model.RootNode {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public RootNode(Instance instance, String name, HeapWalkerNode parent) {
            super(instance, name, parent);
        }

        public RootNode(Instance instance, String name, HeapWalkerNode parent, int mode) {
            super(instance, name, parent, mode);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public abstract void refreshView();
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ObjectNode(Instance instance, String name, HeapWalkerNode parent) {
        super(instance, name, parent);
    }

    public ObjectNode(Instance instance, String name, HeapWalkerNode parent, int mode) {
        super(instance, name, parent, mode);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isArray() {
        return false;
    }

    protected ChildrenComputer getChildrenComputer() {
        return new ChildrenComputer() {
            public HeapWalkerNode[] computeChildren() {
                HeapWalkerNode[] children = null;

                if (getMode() == HeapWalkerNode.MODE_FIELDS) {
                    if (hasInstance()) {
                        ArrayList fieldValues = new ArrayList();
                        fieldValues.addAll(getInstance().getFieldValues());
                        fieldValues.addAll(getInstance().getStaticFieldValues());

                        if (fieldValues.size() == 0) {
                            // Instance has no fields
                            children = new HeapWalkerNode[1];
                            children[0] = HeapWalkerNodeFactory.createNoFieldsNode(ObjectNode.this);
                        } else {
                            // Instance has at least one field
                            children = new HeapWalkerNode[fieldValues.size()];

                            for (int i = 0; i < children.length; i++) {
                                children[i] = HeapWalkerNodeFactory.createFieldNode((FieldValue) fieldValues.get(i),
                                                                                    ObjectNode.this);
                            }
                        }
                    } else {
                        children = new HeapWalkerNode[0];
                    }
                } else if (getMode() == HeapWalkerNode.MODE_REFERENCES) {
                    children = HeapWalkerNodeFactory.createReferences(ObjectNode.this);
                }

                return children;
            }
        };
    }

    protected Icon computeIcon() {
        ImageIcon icon = BrowserUtils.ICON_INSTANCE;

        if ((getMode() == HeapWalkerNode.MODE_REFERENCES) && getInstance().isGCRoot()) {
            icon = BrowserUtils.createGCRootIcon(icon);
        }

        return processLoopIcon(icon);
    }

    protected String computeName() {
        if ((getMode() == HeapWalkerNode.MODE_REFERENCES) && getInstance().isGCRoot()) {
            HeapWalkerNode root = BrowserUtils.getRoot(this);

            if (root instanceof org.netbeans.modules.profiler.heapwalk.model.RootNode) {
                GCRoot gcRoot = ((org.netbeans.modules.profiler.heapwalk.model.RootNode) root).getGCRoot(getInstance());

                if (gcRoot != null) {
                    return super.computeName() + " (" + gcRoot.getKind() + ")"; // NOI18N
                }
            }
        }

        return super.computeName();
    }
}
