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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.Value;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class HeapPatterns {

    // -----
    // I18N String constants
    private static final String INSTANCE_OF_STRING = NbBundle.getMessage(
            HeapPatterns.class, "HeapPatterns_InstanceOfString"); // NOI18N
    private static final String INSTANCES_OF_STRING = NbBundle.getMessage(
            HeapPatterns.class, "HeapPatterns_InstancesOfString"); // NOI18N
    // -----

    private static final String LINKED_LIST_CLASSNAME = "java.util.LinkedList"; // NOI18N
    private static final String LINKED_LIST_ENTRY_CLASSNAME = "java.util.LinkedList$Entry"; // NOI18N


    static HeapWalkerNode[] processReferencePatterns(InstanceNode parent, List references) {
        Instance instance = parent.getInstance();
        JavaClass classs = instance.getJavaClass();
        String className = classs.getName();

        if (LINKED_LIST_ENTRY_CLASSNAME.equals(className))
            return processLinkedListReferencePatterns(parent, instance, classs, references);

        return null;
    }

    // Resolves references for a LinkedList$Entry instance. Only works for standard
    // LinkedLists where each LinkedList$Entry can be referenced by up to 3 referrers
    // including 1 or 2 other LinkedList$Entries and up to 1 LinkedList.
    // Returns: 1 LinkedList$Entries node & 1 LinkedList node or 1 LinkedList node.
    private static HeapWalkerNode[] processLinkedListReferencePatterns(
            InstanceNode parent, Instance instance, JavaClass classs, List<Value> references) {

        Instance e1 = null;
        Instance e2 = null;
        
        Value v1 = null;
        Value v2 = null;
        Value v3 = null;

        Field f = null;

        boolean passed = false;
        boolean nested = references.size() != 3;

        while (v3 == null) {

            int referencesCount = references.size();
            if (referencesCount != 2 && referencesCount != 3) break;

            v1 = references.get(0);
            v2 = references.get(1);
            v3 = referencesCount == 2 ? null : references.get(2);

            Instance i1 = v1 instanceof ObjectFieldValue ? v1.getDefiningInstance() : null;
            Instance i2 = v2 instanceof ObjectFieldValue ? v2.getDefiningInstance() : null;
            Instance i3 = v3 instanceof ObjectFieldValue ? v3.getDefiningInstance() : null;
            if (i1 == null || i2 == null) break;

            JavaClass c1 = i1.getJavaClass();
            JavaClass c2 = i2.getJavaClass();
            JavaClass c3 = i3 == null ? null : i3.getJavaClass();

            if (classs.equals(c3)) {
                if (!classs.equals(c1)) {
                    Value v = v3;
                    Instance i = i3;
                    JavaClass c = c3;
                    v3 = v1;
                    i3 = i1;
                    c3 = c1;
                    v1 = v;
                    i1 = i;
                    c1 = c;
                } else if (!classs.equals(c2)) {
                    Value v = v3;
                    Instance i = i3;
                    JavaClass c = c3;
                    v3 = v2;
                    i3 = i2;
                    c3 = c2;
                    v2 = v;
                    i2 = i;
                    c2 = c;
                } else {
                    break;
                }
            }
            if (!classs.equals(c1) || !classs.equals(c2)) break;

            if (e1 == null) {
                e1 = i1;
                e2 = i2;
            }

            if (v3 == null) {
                Field f1 = ((ObjectFieldValue)v1).getField();
                Field f2 = ((ObjectFieldValue)v2).getField();

                if (f == null) {
                    f = f1;
                } else {
                    Field nextF = null;
                    if (f.equals(f1)) nextF = f1;
                    else if (f.equals(f2)) nextF = f2;
                    if (nextF == null) break;
                    f = nextF;
                }

                references = f == f1 ? i1.getReferences() : i2.getReferences();
            } else {
                passed = LINKED_LIST_CLASSNAME.equals(c3.getName());
            }

        }

        if (!passed) return null;

        String nodesCount;
        List<Instance> instances;

        if (e1.equals(e2)) {
            nodesCount = MessageFormat.format(INSTANCE_OF_STRING,
                    new Object[] { LINKED_LIST_ENTRY_CLASSNAME });
            instances = Collections.singletonList(e1);
        } else {
            nodesCount = MessageFormat.format(INSTANCES_OF_STRING,
                    new Object[] { 2, LINKED_LIST_ENTRY_CLASSNAME });
            instances = Arrays.asList(new Instance[] { e1, e2 });
        }
        String collapsedNodeName = "previous, next (" + nodesCount + ")"; // NOI18N

        HeapWalkerNode[] result;
        if (nested) {
            result = new HeapWalkerNode[1];
            result[0] = new InstancesContainerNode(collapsedNodeName, parent,
                                                   Collections.singletonList(v3),
                                                   instances);
        } else {
            result = new HeapWalkerNode[2];
            result[0] = HeapWalkerNodeFactory.createReferenceNode(v3, parent);
            result[1] = new InstancesContainerNode(collapsedNodeName, parent,
                                                   Collections.EMPTY_LIST,
                                                   instances);
        }

        return result;
    }

}
