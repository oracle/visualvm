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

package org.netbeans.modules.profiler.heapwalk.model;

import org.netbeans.lib.profiler.heap.*;
import org.openide.util.NbBundle;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.Icon;


/**
 *
 * @author Jiri Sedlacek
 */
public class HeapWalkerNodeFactory {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NONE_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class, "HeapWalkerNodeFactory_NoneString"); // NOI18N
    private static final String NO_FIELDS_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class,
                                                                       "HeapWalkerNodeFactory_NoFieldsString"); // NOI18N
    private static final String NO_REFERENCES_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class,
                                                                           "HeapWalkerNodeFactory_NoReferencesString"); // NOI18N
    private static final String NO_ITEMS_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class,
                                                                      "HeapWalkerNodeFactory_NoItemsString"); // NOI18N
    private static final String SEARCHING_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class,
                                                                       "HeapWalkerNodeFactory_SearchingString"); // NOI18N
    private static final String OUT_OF_MEMORY_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class,
                                                                           "HeapWalkerNodeFactory_OutOfMemoryString"); // NOI18N
    private static final String ARRAY_CONTAINER_NAME_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class,
                                                                                  "HeapWalkerNodeFactory_ArrayContainerNameString"); // NOI18N
    private static final String ARRAY_CONTAINER_VALUE_STRING = NbBundle.getMessage(HeapWalkerNodeFactory.class,
                                                                                   "HeapWalkerNodeFactory_ArrayContainerValueString"); // NOI18N
                                                                                                                                       // -----
    public static int ITEMS_COLLAPSE_UNIT_SIZE = 500;
    public static int ITEMS_COLLAPSE_THRESHOLD = 2000;
    public static int ITEMS_COLLAPSE_UNIT_THRESHOLD = 5000;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static HeapWalkerNode createArrayItemContainerNode(final ArrayNode array, final int startIndex, final int endIndex) {
        return new AbstractHeapWalkerNode(array) {
                protected String computeName() {
                    return MessageFormat.format(ARRAY_CONTAINER_NAME_STRING, new Object[] { startIndex, endIndex });
                }

                protected String computeType() {
                    return BrowserUtils.getArrayItemType(array.getType());
                }

                protected String computeValue() {
                    return MessageFormat.format(ARRAY_CONTAINER_VALUE_STRING, new Object[] { (endIndex - startIndex + 1) });
                }

                protected Icon computeIcon() {
                    return null;
                }

                public boolean isLeaf() {
                    return false;
                }

                protected HeapWalkerNode[] computeChildren() {
                    return BrowserUtils.lazilyCreateChildren(this, getChildrenComputer());
                }

                protected ChildrenComputer getChildrenComputer() {
                    return new ChildrenComputer() {
                            public HeapWalkerNode[] computeChildren() {
                                int itemsCount = endIndex - startIndex + 1;
                                HeapWalkerNode[] children = new HeapWalkerNode[itemsCount];

                                boolean primitiveArray = array instanceof PrimitiveArrayNode;
                                List values = primitiveArray ? ((PrimitiveArrayInstance) (array.getInstance())).getValues()
                                                             : ((ObjectArrayInstance) (array.getInstance())).getValues();

                                for (int i = 0; i < itemsCount; i++) {
                                    if (primitiveArray) {
                                        children[i] = createPrimitiveArrayItemNode((PrimitiveArrayNode) array, startIndex + i,
                                                                                   (String) values.get(startIndex + i));
                                    } else {
                                        children[i] = createObjectArrayItemNode((ObjectArrayNode) array, startIndex + i,
                                                                                (Instance) values.get(startIndex + i));
                                    }
                                }

                                return children;
                            }
                        };
                }
            };
    }

    public static ClassNode createClassNode(JavaClass javaClass, String name, HeapWalkerNode parent) {
        return new ClassNode(javaClass, name, parent, (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode());
    }

    public static HeapWalkerFieldNode createFieldNode(FieldValue fieldValue, HeapWalkerNode parent) {
        if (fieldValue instanceof ObjectFieldValue) {
            Instance instance = ((ObjectFieldValue) fieldValue).getInstance();

            if (instance instanceof PrimitiveArrayInstance) {
                return new PrimitiveArrayFieldNode((ObjectFieldValue) fieldValue, parent);
            } else if (instance instanceof ObjectArrayInstance) {
                return new ObjectArrayFieldNode((ObjectFieldValue) fieldValue, parent);
            } else {
                return new ObjectFieldNode((ObjectFieldValue) fieldValue, parent);
            }
        } else {
            return new PrimitiveFieldNode(fieldValue, parent);
        }
    }

    public static HeapWalkerInstanceNode createInstanceNode(Instance instance, String name, HeapWalkerNode parent) {
        int mode = (parent == null) ? HeapWalkerNode.MODE_FIELDS : parent.getMode();

        if (instance instanceof PrimitiveArrayInstance) {
            return new PrimitiveArrayNode((PrimitiveArrayInstance) instance, name, parent, mode);
        } else if (instance instanceof ObjectArrayInstance) {
            return new ObjectArrayNode((ObjectArrayInstance) instance, name, parent, mode);
        } else {
            return new ObjectNode(instance, name, parent, mode);
        }
    }

    public static HeapWalkerNode createNoFieldsNode(HeapWalkerNode parent) {
        return new AbstractHeapWalkerNode(parent) {
                protected String computeName() {
                    return NO_FIELDS_STRING;
                }

                protected String computeType() {
                    return NONE_STRING;
                }

                protected String computeValue() {
                    return NONE_STRING;
                }

                protected Icon computeIcon() {
                    return null;
                }
            };
    }
    
    public static boolean isNoFieldsNode(HeapWalkerNode node) {
        return NO_FIELDS_STRING.equals(node.getName());
    }

    public static HeapWalkerNode createNoItemsNode(HeapWalkerNode parent) {
        return new AbstractHeapWalkerNode(parent) {
                protected String computeName() {
                    return NO_ITEMS_STRING;
                }

                protected String computeType() {
                    return NONE_STRING;
                }

                protected String computeValue() {
                    return NONE_STRING;
                }

                protected Icon computeIcon() {
                    return null;
                }
            };
    }
    
    public static boolean isNoItemsNode(HeapWalkerNode node) {
        return NO_ITEMS_STRING.equals(node.getName());
    }

    public static HeapWalkerNode createNoReferencesNode(HeapWalkerNode parent) {
        return new AbstractHeapWalkerNode(parent) {
                protected String computeName() {
                    return NO_REFERENCES_STRING;
                }

                protected String computeType() {
                    return NONE_STRING;
                }

                protected String computeValue() {
                    return NONE_STRING;
                }

                protected Icon computeIcon() {
                    return null;
                }
            };
    }
    
    public static boolean isNoReferencesNode(HeapWalkerNode node) {
        return NO_REFERENCES_STRING.equals(node.getName());
    }

    public static HeapWalkerNode createOOMNode(HeapWalkerNode parent) {
        return new AbstractHeapWalkerNode(parent) {
                protected String computeName() {
                    return OUT_OF_MEMORY_STRING;
                }

                protected String computeType() {
                    return "";
                } // NOI18N

                protected String computeValue() {
                    return "";
                } // NOI18N

                protected Icon computeIcon() {
                    return org.netbeans.modules.profiler.ui.Utils.ERROR_ICON;
                }
            };
    }
    
    public static boolean isOOMNode(HeapWalkerNode node) {
        return OUT_OF_MEMORY_STRING.equals(node.getName());
    }

    public static HeapWalkerNode createObjectArrayItemNode(ObjectArrayNode array, int itemIndex, Instance instance) {
        if (instance instanceof PrimitiveArrayInstance) {
            return new PrimitiveArrayNode.ArrayItem(itemIndex, (PrimitiveArrayInstance) instance, array);
        } else if (instance instanceof ObjectArrayInstance) {
            return new ObjectArrayNode.ArrayItem(itemIndex, (ObjectArrayInstance) instance, array);
        } else {
            return new ObjectNode.ArrayItem(itemIndex, instance, array);
        }
    }

    public static HeapWalkerNode createPrimitiveArrayItemNode(PrimitiveArrayNode array, int itemIndex, String value) {
        return new PrimitiveFieldNode.ArrayItem(itemIndex, BrowserUtils.getArrayItemType(array.getType()), value, array);
    }

    public static HeapWalkerNode createProgressNode(HeapWalkerNode parent) {
        return new AbstractHeapWalkerNode(parent) {
                protected String computeName() {
                    return SEARCHING_STRING;
                }

                protected String computeType() {
                    return "";
                } // NOI18N

                protected String computeValue() {
                    return "";
                } // NOI18N

                protected Icon computeIcon() {
                    return BrowserUtils.ICON_PROGRESS;
                }
            };
    }
    
    public static boolean isProgressNode(HeapWalkerNode node) {
        return SEARCHING_STRING.equals(node.getName());
    }

    public static HeapWalkerNode createReferenceNode(Value value, HeapWalkerNode parent) {
        if (value instanceof ObjectFieldValue) {
            return new ObjectFieldNode((ObjectFieldValue) value, parent);
        } else if (value instanceof ArrayItemValue) {
            ArrayItemValue arrayValue = (ArrayItemValue) value;

            return new ObjectArrayNode.ArrayItem(arrayValue.getIndex(), (ObjectArrayInstance) arrayValue.getDefiningInstance(),
                                                 parent);
        } else {
            return null;
        }
    }

    public static ClassNode createRootClassNode(JavaClass javaClass, String name, final Runnable refresher, int mode,
                                                final Heap heap) {
        return new ClassNode.RootNode(javaClass, name, null, mode) {
                public void refreshView() {
                    refresher.run();
                }

                public GCRoot getGCRoot(Instance inst) {
                    return heap.getGCRoot(inst);
                }
                ;
                public JavaClass getJavaClassByID(long javaclassId) {
                    return heap.getJavaClassByID(javaclassId);
                }
                ;
            };
    }

    public static HeapWalkerInstanceNode createRootInstanceNode(Instance instance, String name, final Runnable refresher,
                                                                int mode, final Heap heap) {
        if (instance instanceof PrimitiveArrayInstance) {
            return new PrimitiveArrayNode.RootNode((PrimitiveArrayInstance) instance, name, null, mode) {
                    public void refreshView() {
                        refresher.run();
                    }

                    public GCRoot getGCRoot(Instance inst) {
                        return heap.getGCRoot(inst);
                    }
                    ;
                    public JavaClass getJavaClassByID(long javaclassId) {
                        return heap.getJavaClassByID(javaclassId);
                    }
                    ;
                };
        } else if (instance instanceof ObjectArrayInstance) {
            return new ObjectArrayNode.RootNode((ObjectArrayInstance) instance, name, null, mode) {
                    public void refreshView() {
                        refresher.run();
                    }

                    public GCRoot getGCRoot(Instance inst) {
                        return heap.getGCRoot(inst);
                    }
                    ;
                    public JavaClass getJavaClassByID(long javaclassId) {
                        return heap.getJavaClassByID(javaclassId);
                    }
                    ;
                };
        } else {
            return new ObjectNode.RootNode(instance, name, null, mode) {
                    public void refreshView() {
                        refresher.run();
                    }

                    public GCRoot getGCRoot(Instance inst) {
                        return heap.getGCRoot(inst);
                    }
                    ;
                    public JavaClass getJavaClassByID(long javaclassId) {
                        return heap.getJavaClassByID(javaclassId);
                    }
                    ;
                };
        }
    }
    
    public static boolean isMessageNode(HeapWalkerNode node) {
        return isNoFieldsNode(node) ||
               isNoItemsNode(node) ||
               isNoReferencesNode(node) ||
               isNoReferencesNode(node) ||
               isOOMNode(node) ||
               isProgressNode(node);
    }
}
