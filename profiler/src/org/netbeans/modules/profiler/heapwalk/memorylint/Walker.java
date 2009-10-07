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

package org.netbeans.modules.profiler.heapwalk.memorylint;

import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.Type;


/**
 * An utility class capable of walking the object graph and counting all
 * found objects.
 *
 * @param T the entry type, which can add additional properties
 * @author nenik
 */
public final class Walker {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    private static final Type OBJECT = new Type() {
        public String getName() { return "object"; }
    };
    
    private class ArrayEntryValue implements ObjectFieldValue, Field {
        int idx;
        private Instance src;
        private Instance target;

        public ArrayEntryValue(int idx, Instance src, Instance target) {
            this.idx = idx;
            this.src = src;
            this.target = target;
        }
        
        public Instance getInstance() {
            return target;
        }

        public Field getField() {
            return this;
        }

        public String getValue() {
            return "Instance #" + target.getInstanceId();
        }

        public Instance getDefiningInstance() {
            return src;
        }

        public JavaClass getDeclaringClass() {
            return src.getJavaClass(); // XXX
        }

        public String getName() {
            return "[" + idx + "]";
        }

        public boolean isStatic() {
            return false;
        }

        public Type getType() {
            return OBJECT;
        }
        
    }
    
    public static interface Filter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean accept(ObjectFieldValue val);
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Distribution log = new Distribution();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public Walker() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Distribution getResults() {
        return log;
    }

    public void walk(Instance in) {
        walk(in, null);
    }

    public void walk(Instance in, Filter f) {
        Queue<Instance> q = new LinkedList<Instance>();
        q.add(in);

        log.add(in);

        while (!q.isEmpty()) {
            Instance act = q.poll();
            
            if (act instanceof ObjectArrayInstance) {
                List<Instance> out = ((ObjectArrayInstance)act).getValues();
                int i = 0;
                for (Instance target : out) {
                    if (target != null) {
                        if ((f == null || f.accept(new ArrayEntryValue(i, act, target))) && !log.isCounted(target)) {
                            log.add(target);
                            q.add(target);
                        }
                    }
                    i++;
                }
            }

            @SuppressWarnings("unchecked")
            List<FieldValue> out = (List<FieldValue>) act.getFieldValues();

            for (FieldValue fv : out) {
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue ofv = (ObjectFieldValue) fv;

                    if ((f != null) && !f.accept(ofv)) {
                        continue;
                    }

                    Instance target = ofv.getInstance();

                    if ((target != null) && !log.isCounted(target)) {
                        log.add(target);
                        q.add(target);
                    }
                }
            }
        }
    }
}
