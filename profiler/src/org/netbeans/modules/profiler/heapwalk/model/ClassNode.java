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


import org.openide.util.NbBundle;
import java.util.List;
import javax.swing.ImageIcon;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.JavaClass;


/**
 * Implements common methods of all Fields Browser nodes holding reference to org.netbeans.lib.profiler.heap.JavaClass
 *
 * @author Jiri Sedlacek
 */
public class ClassNode extends AbstractHeapWalkerNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public abstract static class RootNode extends ClassNode implements org.netbeans.modules.profiler.heapwalk.model.RootNode {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public RootNode(JavaClass javaClass, String name, HeapWalkerNode parent) {
            super(javaClass, name, parent);
        }

        public RootNode(JavaClass javaClass, String name, HeapWalkerNode parent, int mode) {
            super(javaClass, name, parent, mode);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public abstract void refreshView();
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NONE_STRING = NbBundle.getMessage(ClassNode.class, "ClassNode_NoneString"); // NOI18N
                                                                                                            // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JavaClass javaClass;
    private String name;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ClassNode(JavaClass javaClass, String name, HeapWalkerNode parent) {
        this(javaClass, name, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
    }

    public ClassNode(JavaClass javaClass, String name, HeapWalkerNode parent, int mode) {
        super(parent, mode);

        this.javaClass = javaClass;

        this.name = name;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JavaClass getJavaClass() {
        return javaClass;
    }

    public boolean isLeaf() {
        return false;
    }

    protected HeapWalkerNode[] computeChildren() {
        return BrowserUtils.lazilyCreateChildren(this,
                                                 new ChildrenComputer() {
                public HeapWalkerNode[] computeChildren() {
                    HeapWalkerNode[] children = null;

                    List fieldValues = getJavaClass().getStaticFieldValues();

                    if (fieldValues.size() == 0) {
                        // Instance has no fields
                        children = new HeapWalkerNode[1];
                        children[0] = HeapWalkerNodeFactory.createNoFieldsNode(ClassNode.this);
                    } else {
                        // Instance has at least one field
                        children = new HeapWalkerNode[fieldValues.size()];

                        for (int i = 0; i < children.length; i++) {
                            children[i] = HeapWalkerNodeFactory.createFieldNode((FieldValue) fieldValues.get(i), ClassNode.this);
                        }
                    }

                    return children;
                }
            });
    }

    protected ImageIcon computeIcon() {
        return BrowserUtils.ICON_INSTANCE;
    }

    protected String computeName() {
        return name;
    }

    protected String computeType() {
        return javaClass.getName();
    }

    protected String computeValue() {
        return NONE_STRING;
    }

    protected String computeSize() {
        return "-"; // NOI18N
    }

    protected String computeRetainedSize() {
        return "-"; // NOI18N
    }
}
