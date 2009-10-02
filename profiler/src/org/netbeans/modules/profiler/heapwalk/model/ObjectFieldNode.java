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

import org.netbeans.lib.profiler.heap.*;
import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 *
 * @author Jiri Sedlacek
 */
public class ObjectFieldNode extends ObjectNode implements HeapWalkerFieldNode {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ObjectFieldValue fieldValue;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ObjectFieldNode(ObjectFieldValue fieldValue, HeapWalkerNode parent) {
        this(fieldValue, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
    }

    public ObjectFieldNode(ObjectFieldValue fieldValue, HeapWalkerNode parent, int mode) {
        super((mode == HeapWalkerNode.MODE_FIELDS) ? fieldValue.getInstance() : fieldValue.getDefiningInstance(),
              fieldValue.getField().getName(), parent, mode);
        this.fieldValue = fieldValue;

        if (!isLoop() && (getMode() == HeapWalkerNode.MODE_REFERENCES) && isStatic()) {
            loopTo = computeClassLoopTo();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ObjectFieldValue getFieldValue() {
        return fieldValue;
    }

    public boolean isStatic() {
        return fieldValue.getField().isStatic();
    }

    protected Icon computeIcon() {
        ImageIcon icon = BrowserUtils.ICON_INSTANCE;

        if (isStatic()) {
            icon = BrowserUtils.createStaticIcon(icon);
        }

        if ((getMode() == HeapWalkerNode.MODE_REFERENCES) && getInstance().isGCRoot()) {
            icon = BrowserUtils.createGCRootIcon(icon);
        }

        return processLoopIcon(icon);
    }

    protected String computeType() {
        if ((getMode() == HeapWalkerNode.MODE_REFERENCES) && isStatic()) {
            return fieldValue.getField().getDeclaringClass().getName();
        }

        return super.computeType();
    }

    protected String computeValue() {
        if ((getMode() == HeapWalkerNode.MODE_REFERENCES) && isStatic()) {
            return "class " + BrowserUtils.getSimpleType(fieldValue.getField().getDeclaringClass().getName()); // NOI18N
        }

        return super.computeValue();
    }

    private HeapWalkerNode computeClassLoopTo() {
        JavaClass declaringClass = fieldValue.getField().getDeclaringClass();
        HeapWalkerNode parent = getParent();

        while ((parent != null) && parent instanceof HeapWalkerInstanceNode) {
            if (parent instanceof HeapWalkerFieldNode) {
                HeapWalkerFieldNode parentF = (HeapWalkerFieldNode) parent;

                if (parentF.isStatic() && parentF.getFieldValue().getField().getDeclaringClass().equals(declaringClass)) {
                    return parent;
                }
            }

            parent = parent.getParent();
        }

        return null;
    }
}
