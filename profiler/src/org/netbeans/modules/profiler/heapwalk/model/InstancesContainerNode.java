/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import javax.swing.Icon;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.Value;

/**
 *
 * @author Jiri Sedlacek
 */
public class InstancesContainerNode extends AbstractHeapWalkerNode {

    private final String name;
    private List<Value> childrenValues;
    private final List<Instance> instances;


    public InstancesContainerNode(String name, HeapWalkerNode parent, List<Value> childrenValues,
                                  List<Instance> instances) {
        super(parent);
        this.name = name;
        this.childrenValues = childrenValues;
        this.instances = instances;
    }


    public List<Instance> getInstances() {
        return instances;
    }


    protected String computeName() {
        return name;
    }

    protected String computeType() {
        return "-"; // NOI18N
    }

    protected String computeValue() {
        return "-"; // NOI18N
    }

    protected String computeSize() {
        return "-"; // NOI18N
    }

    protected String computeRetainedSize() {
        return "-"; // NOI18N
    }

    protected Icon computeIcon() {
        return BrowserUtils.ICON_INSTANCE;
    }

    protected HeapWalkerNode[] computeChildren() {
        HeapWalkerNode[] nodes = new HeapWalkerNode[childrenValues.size()];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = HeapWalkerNodeFactory.createReferenceNode(childrenValues.get(i), this);
        childrenValues = null;
        return nodes;
    }

}
