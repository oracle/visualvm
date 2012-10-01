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

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.lib.profiler.heap.*;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;


/**
 * Constants and utilities for Fields Browser
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "BrowserUtils_OutOfMemoryMsg=<html><b>Out of memory in HeapWalker</b><br><br>To avoid this error, increase the -Xmx value<br>in the etc/netbeans.conf file in NetBeans IDE installation directory.</html>",
    "BrowserUtils_TruncatedMsg=...<truncated>...",
    "BrowserUtils_PathCopiedToClipboard=Path from root copied to the clipboard."
})
public class BrowserUtils {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class GroupingInfo {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public int collapseUnitSize;
        public int containersCount;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        GroupingInfo(int containersCount, int collapseUnitSize) {
            this.containersCount = containersCount;
            this.collapseUnitSize = collapseUnitSize;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final ImageIcon ICON_INSTANCE = Icons.getImageIcon(LanguageIcons.INSTANCE);
    public static final ImageIcon ICON_PRIMITIVE = Icons.getImageIcon(LanguageIcons.PRIMITIVE);
    public static final ImageIcon ICON_ARRAY = Icons.getImageIcon(LanguageIcons.ARRAY);
    public static final ImageIcon ICON_PROGRESS = Icons.getImageIcon(HeapWalkerIcons.PROGRESS);
    public static final ImageIcon ICON_STATIC = Icons.getImageIcon(HeapWalkerIcons.STATIC);
    public static final ImageIcon ICON_LOOP = Icons.getImageIcon(HeapWalkerIcons.LOOP);
    public static final ImageIcon ICON_GCROOT = Icons.getImageIcon(HeapWalkerIcons.GC_ROOT);
    private static final RequestProcessor requestProcessor = new RequestProcessor("HeapWalker Processor", 3); // NOI18N

    private static final int MAX_FULLNAME_LENGTH = 100;

    private static final Set<String> PRIMITIVE_TYPES = new HashSet<String>(
            Arrays.asList("char", "byte", "short", "int", "long", "float", "double", "boolean"));

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Get item class of an array. (e.g. <code>byte[]</code> for <code>byte[][]</code>)
     * @return <code>arrayTypeName</code> without last '[]'
     * */
    public static String getArrayItemType(String arrayTypeName) {
        int arrayBracketsIdx = arrayTypeName.lastIndexOf('['); // NOI18N

        return ((arrayBracketsIdx == -1) ? arrayTypeName : arrayTypeName.substring(0, arrayBracketsIdx));
    }

    /** Get base class of an array. (e.g. <code>byte</code> for <code>byte[][]</code>)
     * @return <code>arrayTypeName</code> without any trailing '[]'
     * */
    public static String getArrayBaseType(String arrayTypeName) {
        int arrayBracketsIdx = arrayTypeName.indexOf('['); // NOI18N
        return ((arrayBracketsIdx == -1) ? arrayTypeName : arrayTypeName.substring(0, arrayBracketsIdx));
    }

    public static String getFullNodeName(HeapWalkerNode node) {
        StringBuilder sb = new StringBuilder();

        while (!node.isRoot()) {
            int length = sb.length();
            if (length < MAX_FULLNAME_LENGTH) {
                String nodeName = getNodeName(node);
                sb.insert(0, "." + nodeName); // NOI18N
                node = node.getParent();
            } else {
                sb.delete(0, Bundle.BrowserUtils_TruncatedMsg().length());
                sb.insert(0, Bundle.BrowserUtils_TruncatedMsg());
                break;
            }
        }

        sb.insert(0, getNodeName(node));
        return sb.toString();
    }

    public static GroupingInfo getGroupingInfo(int itemsCount) {
        int childrenCount = itemsCount;

        int collapseUnitSize = HeapWalkerNodeFactory.ITEMS_COLLAPSE_UNIT_SIZE;
        int containersCount = (int) Math.ceil((float) childrenCount / (float) collapseUnitSize);

        while ((containersCount > HeapWalkerNodeFactory.ITEMS_COLLAPSE_UNIT_THRESHOLD)
                   && (collapseUnitSize < HeapWalkerNodeFactory.ITEMS_COLLAPSE_UNIT_THRESHOLD)) {
            collapseUnitSize += HeapWalkerNodeFactory.ITEMS_COLLAPSE_UNIT_SIZE;
            containersCount = (int) Math.ceil((float) childrenCount / (float) collapseUnitSize);
        }

        return new GroupingInfo(containersCount, collapseUnitSize);
    }

    public static HeapWalkerNode getRoot(HeapWalkerNode node) {
        while ((node != null) && !node.isRoot()) {
            node = node.getParent();
        }

        return (node == null) ? null : node;
    }

    public static String getSimpleType(String fullType) {
        int simpleTypeIdx = fullType.lastIndexOf('.'); // NOI18N

        if (simpleTypeIdx == -1) {
            return fullType;
        } else {
            if (fullType.startsWith("<")) { // NOI18N

                return "<" + fullType.substring(simpleTypeIdx + 1); // NOI18N
            } else {
                return fullType.substring(simpleTypeIdx + 1);
            }
        }
    }

    public static boolean isStaticField(FieldValue fieldValue) {
        return fieldValue.getField().isStatic();
    }

    public static HeapWalkerNode computeChildrenToNearestGCRoot(InstanceNode instanceNode) {
        HeapWalkerNode node = instanceNode;
        Instance instance = instanceNode.getInstance();
        Instance nextInstance = instance.getNearestGCRootPointer();
        HeapWalkerNode[] children = null;

        while (!instance.equals(nextInstance)) {
            if (nextInstance == null || node == null) {
                node = null;
                break;
            }

            if (children == null) {
                if (node instanceof InstanceNode && !((InstanceNode)node).currentlyHasChildren()) {
                    InstanceNode inode = (InstanceNode)node;
                    children = inode.getChildrenComputer().computeChildren();
                    inode.setChildren(children);
                } else {
                    children = node.getChildren();
                }
            }

            for (int i = 0; i < children.length; i++) {
                HeapWalkerNode child = children[i];
                if (child instanceof InstanceNode) {
                    if (((InstanceNode)child).getInstance().equals(nextInstance)) {
                        node = child;
                        children = null;
                        break;
                    }
                } else if (child instanceof InstancesContainerNode) {
                    if (((InstancesContainerNode)child).getInstances().contains(nextInstance)) {
                        node = child;
                        children = null;
                        break;
                    }
                }
            }

            instance = nextInstance;
            nextInstance = nextInstance.getNearestGCRootPointer();
        }

        return node;
    }

    public static ImageIcon createGCRootIcon(ImageIcon icon) {
        return new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), ICON_GCROOT.getImage(), 0, 0));
    }

    public static ImageIcon createLoopIcon(ImageIcon icon) {
        return new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), ICON_LOOP.getImage(), 0, 0));
    }

    public static ImageIcon createStaticIcon(ImageIcon icon) {
        return new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), ICON_STATIC.getImage(), 0, 0));
    }

    public static HeapWalkerNode[] lazilyCreateChildren(final HeapWalkerNode parent, final ChildrenComputer childrenComputer) {
        SwingUtilities.invokeLater(new Runnable() { // allow repaint expanded node first
            public void run() {
                performTask(new Runnable() { // compute progressChildren in separate thread, TODO: use RequestProcessor with single thread!
                    public void run() {
                        if (parent instanceof AbstractHeapWalkerNode) {
                            boolean oome = false;
                            HeapWalkerNode[] computedChildren;

                            try {
                                computedChildren = childrenComputer.computeChildren();
                            } catch (OutOfMemoryError e) {
                                oome = true;
                                computedChildren = new HeapWalkerNode[] { HeapWalkerNodeFactory.createOOMNode(parent) };
                            }

                            final HeapWalkerNode[] computedChildrenF = computedChildren;
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    ((AbstractHeapWalkerNode) parent).changeChildren(computedChildrenF);
                                    HeapWalkerNode root = getRoot(parent);
                                    if (root instanceof RootNode) ((RootNode)root).refreshView();
                                }
                            });

                            if (oome) ProfilerDialogs.displayError(Bundle.BrowserUtils_OutOfMemoryMsg());
                        }
                    }
                });
            }
        });

        return new HeapWalkerNode[] { HeapWalkerNodeFactory.createProgressNode(parent) };
    }
    
    public static void copyPathFromRoot(final TreePath path) {
        performTask(new Runnable() {
            public void run() {
                StringSelection s = new StringSelection(pathFromRoot(path));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
                ProfilerDialogs.displayInfo(Bundle.BrowserUtils_PathCopiedToClipboard());
            }
        });
    }
    
    private static String pathFromRoot(TreePath path) {
        int m = ((HeapWalkerNode)path.getLastPathComponent()).getMode();
        Object[] nodes = path.getPath();
        StringBuilder b = new StringBuilder();
        int s = nodes.length;
        for (int i = 0; i < s; i++) {
            HeapWalkerNode n = (HeapWalkerNode)nodes[i];
            if (m == HeapWalkerNode.MODE_FIELDS) fieldFromRoot(n, b, i, s);
            else referenceFromRoot(n, b, i, s);
            b.append("\n"); // NOI18N
        }
        return b.toString().replace("].[", ""); // NOI18N
    }
    
    private static void fieldFromRoot(HeapWalkerNode n, StringBuilder b, int i, int s) {
        if (i == 0) {
            b.append(n.getName());
            b.append("     - "); // NOI18N
            b.append("value: "); // NOI18N
            b.append(n.getType());
            b.append(" "); // NOI18N
            b.append(n.getValue());
        } else {
            indent(b, i);
            b.append("-> "); // NOI18N
            b.append(n.getName());
            b.append("     - "); // NOI18N
            b.append("class: "); // NOI18N
            b.append(n.getParent().getType());
            b.append(", "); // NOI18N
            b.append("value: "); // NOI18N
            b.append(n.getType());
            b.append(" "); // NOI18N
            b.append(n.getValue());
        }
    }
    
    private static void referenceFromRoot(HeapWalkerNode n, StringBuilder b, int i, int s) {
        if (i == 0) {
            b.append(n.getName());
            b.append("     - "); // NOI18N
            b.append("value: "); // NOI18N
            b.append(n.getType());
            b.append(" "); // NOI18N
            b.append(n.getValue());
        } else {
            indent(b, i);
            b.append("<- "); // NOI18N
            b.append(n.getName());
            b.append("     - "); // NOI18N
            b.append("class: "); // NOI18N
            b.append(n.getType());
            b.append(", "); // NOI18N
            b.append("value: "); // NOI18N
            b.append(n.getParent().getType());
            b.append(" "); // NOI18N
            b.append(n.getParent().getValue());
        }
    }
    
    private static void indent(StringBuilder b, int i) {
        while (i-- > 0) b.append(" "); // NOI18N
    }

    public static void performTask(Runnable task) {
        requestProcessor.post(task);
    }
    
    public static void performTask(Runnable task, int timeToWait) {
        requestProcessor.post(task, timeToWait);
    }

    private static String getNodeName(HeapWalkerNode node) {
        String name = node.getName();

        if (name.endsWith(")")) { // NOI18N
            // filters out additional information in name, i.e. GC root type
            name = name.substring(0, name.indexOf('(')).trim(); // NOI18N
        }

        return name;
    }
    
    /** Check if the className is primitive type.
     */
    public static boolean isPrimitiveType(String className) {
        return PRIMITIVE_TYPES.contains(className);
    }

}
