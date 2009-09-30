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

package org.netbeans.modules.profiler.heapwalk;

import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.heapwalk.model.AbstractHeapWalkerNode;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNode;
import org.netbeans.modules.profiler.heapwalk.model.HeapWalkerNodeFactory;
import org.netbeans.modules.profiler.heapwalk.ui.FieldsBrowserControllerUI;
import org.openide.util.NbBundle;
//import java.util.Comparator;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JPanel;


/**
 *
 * @author Jiri Sedlacek
 */
public class FieldsBrowserController extends AbstractController {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Handler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public HeapFragmentWalker getHeapFragmentWalker();

        public void showClass(JavaClass javaClass);

        public void showInstance(Instance instance);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

//    private class FieldsComparator implements Comparator {
//        //~ Instance fields ------------------------------------------------------------------------------------------------------
//
//        private boolean sortingOrder;
//        private int sortingColumn;
//
//        //~ Constructors ---------------------------------------------------------------------------------------------------------
//
//        public FieldsComparator(int sortingColumn, boolean sortingOrder) {
//            this.sortingColumn = sortingColumn;
//            this.sortingOrder = sortingOrder;
//        }
//
//        //~ Methods --------------------------------------------------------------------------------------------------------------
//
//        public int compare(Object o1, Object o2) {
//            FieldValue field1 = sortingOrder ? (FieldValue) o1 : (FieldValue) o2;
//            FieldValue field2 = sortingOrder ? (FieldValue) o2 : (FieldValue) o1;
//
//            switch (sortingColumn) {
//                case 0: // Name
//                    return field1.getField().getName().compareTo(field2.getField().getName());
//                case 1: // Type
//                    return field1.getField().getType().getName().compareTo(field2.getField().getType().getName());
//                case 2: // Value
//                    return field1.getValue().compareTo(field2.getValue());
//                default:
//                    throw new RuntimeException("Unsupported compare operation for " + o1 + ", " + o2); // NOI18N
//            }
//        }
//    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NONE_STRING = NbBundle.getMessage(FieldsBrowserController.class,
                                                                  "FieldsBrowserController_NoneString"); // NOI18N
    private static final String NO_INSTANCE_SELECTED_STRING = NbBundle.getMessage(FieldsBrowserController.class,
                                                                                  "FieldsBrowserController_NoInstanceSelectedString"); // NOI18N
    private static final String NO_CLASS_SELECTED_STRING = NbBundle.getMessage(FieldsBrowserController.class,
                                                                               "FieldsBrowserController_NoClassSelectedString"); // NOI18N
                                                                                                                                 // -----
    public static final int ROOT_INSTANCE = 0;
    public static final int ROOT_CLASS = 1;

    // --- Public interface ------------------------------------------------------
    public static final AbstractHeapWalkerNode EMPTY_INSTANCE_NODE = new AbstractHeapWalkerNode(null) {
        protected String computeName() {
            return NO_INSTANCE_SELECTED_STRING;
        }

        protected String computeType() {
            return NONE_STRING;
        }

        protected String computeValue() {
            return NONE_STRING;
        }

        protected String computeSize() {
            return ""; // NOI18N
        }

        protected String computeRetainedSize() {
            return ""; // NOI18N
        }

        protected Icon computeIcon() {
            return null;
        }

        public boolean isLeaf() {
            return true;
        }
    };

    public static final AbstractHeapWalkerNode EMPTY_CLASS_NODE = new AbstractHeapWalkerNode(null) {
        protected String computeName() {
            return NO_CLASS_SELECTED_STRING;
        }

        protected String computeType() {
            return NONE_STRING;
        }

        protected String computeValue() {
            return NONE_STRING;
        }
        
        protected String computeSize() {
            return ""; // NOI18N
        }
        
        protected String computeRetainedSize() {
            return ""; // NOI18N
        }

        protected Icon computeIcon() {
            return null;
        }

        public boolean isLeaf() {
            return true;
        }
    };


    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Handler instancesControllerHandler;
    private Instance instance;
    private JavaClass javaClass;
    private int rootMode;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public FieldsBrowserController(Handler instancesControllerHandler, int rootMode) {
        this.instancesControllerHandler = instancesControllerHandler;
        this.rootMode = rootMode;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Internal interface ----------------------------------------------------
    public Handler getInstancesControllerHandler() {
        return instancesControllerHandler;
    }

    public HeapWalkerNode getFilteredSortedFields(String filterValue, int sortingColumn, boolean sortingOrder) {
        if (rootMode == ROOT_INSTANCE) {
            if (instance == null) {
                return EMPTY_INSTANCE_NODE;
            }

            return getSortedFields(getFilteredFields(getFields(instance), filterValue), sortingColumn, sortingOrder);
        } else if (rootMode == ROOT_CLASS) {
            if (javaClass == null) {
                return EMPTY_CLASS_NODE;
            }

            return getSortedFields(getFilteredFields(getFields(javaClass), filterValue), sortingColumn, sortingOrder);
        } else {
            return null;
        }
    }

    public void setInstance(Instance instance) {
        this.rootMode = ROOT_INSTANCE;
        this.instance = instance;
        this.javaClass = null;
        update();
    }

    public void setJavaClass(JavaClass javaClass) {
        this.rootMode = ROOT_CLASS;
        this.instance = null;
        this.javaClass = javaClass;
        update();
    }

    public int getRootMode() {
        return rootMode;
    }

    public void createNavigationHistoryPoint() {
        instancesControllerHandler.getHeapFragmentWalker().createNavigationHistoryPoint();
    }

    public void navigateToClass(JavaClass javaClass) {
        instancesControllerHandler.showClass(javaClass);
    }

    public void navigateToInstance(Instance instance) {
        instancesControllerHandler.showInstance(instance);
    }

    public void showInstance(Instance instance) {
        if (this.instance != instance) {
            setInstance(instance);
        }
    }

    public void showJavaClass(JavaClass javaClass) {
        if (this.javaClass != javaClass) {
            setJavaClass(javaClass);
        }
    }

    // --- Private implementation ------------------------------------------------
    public void update() {
        ((FieldsBrowserControllerUI) getPanel()).update();
    }

    protected AbstractButton createControllerPresenter() {
        return ((FieldsBrowserControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new FieldsBrowserControllerUI(this);
    }

    private HeapWalkerNode getFields(final Instance instance) {
        return HeapWalkerNodeFactory.createRootInstanceNode(instance, "this", // NOI18N
                                                            new Runnable() {
            public void run() {
                ((FieldsBrowserControllerUI) getPanel()).refreshView();
            }
        }, HeapWalkerNode.MODE_FIELDS, instancesControllerHandler.getHeapFragmentWalker().getHeapFragment());
    }

    private HeapWalkerNode getFields(final JavaClass javaClass) {
        return HeapWalkerNodeFactory.createRootClassNode(javaClass, "class", // NOI18N
                                                         new Runnable() {
            public void run() {
                ((FieldsBrowserControllerUI) getPanel()).refreshView();
            }
        }, HeapWalkerNode.MODE_FIELDS, instancesControllerHandler.getHeapFragmentWalker().getHeapFragment());
    }

    private HeapWalkerNode getFilteredFields(HeapWalkerNode fields, String filterValue) {
        //    ArrayList filteredFields = new ArrayList();
        //    
        //    Iterator fieldsIterator = fields.iterator();
        //    while (fieldsIterator.hasNext()) {
        //      FieldValue field = (FieldValue)fieldsIterator.next();
        //      if (matchesFilter(field)) filteredFields.add(field);
        //    }
        //    
        //    return filteredFields;
        return fields;
    }

    private HeapWalkerNode getSortedFields(HeapWalkerNode filteredFields, int sortingColumn, boolean sortingOrder) {
        //Collections.sort(filteredFields, new FieldsComparator(sortingColumn, sortingOrder));
        return filteredFields;
    }

//    private boolean matchesFilter(FieldValue field) {
//        return true;
//    }
}
