/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.oql.engine.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ReachableObjects {
    private ReachableExcludes excludes;
    private Instance root;
    
    public ReachableObjects(Instance root, final ReachableExcludes excludes) {
        this.root = root;
        this.excludes = excludes;
    }

    public Instance getRoot() {
        return root;
    }

    public Iterator<Instance> getReachables() {
        return new TreeIterator<Instance, Instance>(root) {

            @Override
            protected Iterator<Instance> getSameLevelIterator(Instance popped) {
                Collection<Instance> instances = new ArrayList<Instance>();
                for(Object fv : popped.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        if (excludes == null || !excludes.isExcluded(getFQFieldName(((FieldValue)fv).getField()))) {
                            Instance i = ((ObjectFieldValue)fv).getInstance();
                            if (i != null) {
                                instances.add(i);
                            }
                        }
                    }
                }
                if (popped instanceof ObjectArrayInstance) {
                    instances.addAll(((ObjectArrayInstance)popped).getValues());
                }
                return instances.iterator();
            }

            @Override
            protected Iterator<Instance> getTraversingIterator(Instance popped) {
                Collection<Instance> instances = new ArrayList<Instance>();
                for(Object fv : popped.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        if (excludes == null || !excludes.isExcluded(getFQFieldName(((FieldValue)fv).getField()))) {
                            Instance i = ((ObjectFieldValue)fv).getInstance();
                            if (i != null) {
                                instances.add(i);
                            }
                        }
                    }
                }
                if (popped instanceof ObjectArrayInstance) {
                    instances.addAll(((ObjectArrayInstance)popped).getValues());
                }
                return instances.iterator();
            }
        };
    }

    public long getTotalSize() {
        return -1;
    }

    private String getFQFieldName(Field fld) {
        return fld.getDeclaringClass().getName() + "." + fld.getName();
    }
}
