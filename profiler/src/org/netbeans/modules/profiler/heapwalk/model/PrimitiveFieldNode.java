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


import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.lib.profiler.heap.FieldValue;


/**
 *
 * @author Jiri Sedlacek
 */
public class PrimitiveFieldNode extends AbstractHeapWalkerNode implements HeapWalkerFieldNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class ArrayItem extends PrimitiveFieldNode implements org.netbeans.modules.profiler.heapwalk.model.ArrayItem {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String type;
        private String value;
        private int itemIndex;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ArrayItem(int itemIndex, String type, String value, HeapWalkerNode parent) {
            this(itemIndex, type, value, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
        }

        public ArrayItem(int itemIndex, String type, String value, HeapWalkerNode parent, int mode) {
            super(null, parent, mode);

            this.itemIndex = itemIndex;
            this.type = type;
            this.value = value;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getItemIndex() {
            return itemIndex;
        }

        public boolean isStatic() {
            return false;
        }

        protected String computeName() {
            return "[" + itemIndex + "]"; // NOI18N
        }

        protected String computeType() {
            return type;
        }

        protected String computeValue() {
            return value;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldValue fieldValue;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public PrimitiveFieldNode(FieldValue fieldValue, HeapWalkerNode parent) {
        this(fieldValue, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
    }

    public PrimitiveFieldNode(FieldValue fieldValue, HeapWalkerNode parent, int mode) {
        super(parent, mode);
        this.fieldValue = fieldValue;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public FieldValue getFieldValue() {
        return fieldValue;
    }

    public boolean isLeaf() {
        return true;
    }

    public boolean isStatic() {
        return fieldValue.getField().isStatic();
    }

    protected Icon computeIcon() {
        ImageIcon icon = BrowserUtils.ICON_PRIMITIVE;

        if (isStatic()) {
            icon = BrowserUtils.createStaticIcon(icon);
        }

        return icon;
    }

    protected String computeName() {
        return fieldValue.getField().getName();
    }

    protected String computeType() {
        return fieldValue.getField().getType().getName();
    }

    protected String computeValue() {
        return fieldValue.getValue();
    }

    protected String computeSize() {
        return "-"; // NOI18N
    }

    protected String computeRetainedSize() {
        return "-"; // NOI18N
    }
}
