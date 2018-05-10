/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public class ReflectionDetailsProvider extends DetailsProvider.Basic {
    
    private static final String CLASS_MASK = "java.lang.Class";                     // NOI18N
    private static final String CONSTRUCTOR_MASK = "java.lang.reflect.Constructor"; // NOI18N
    private static final String METHOD_MASK = "java.lang.reflect.Method";           // NOI18N
    private static final String FIELD_MASK = "java.lang.reflect.Field";             // NOI18N
    private static final String PARAMETER_MASK = "java.lang.reflect.Parameter";     // NOI18N
    
    public ReflectionDetailsProvider() {
        super(CLASS_MASK,CONSTRUCTOR_MASK, METHOD_MASK, FIELD_MASK, PARAMETER_MASK);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (CLASS_MASK.equals(className)) {                                     // Class
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
            if (name == null && CLASS_MASK.equals(instance.getJavaClass().getName())) {
                JavaClass jclass = heap.getJavaClassByID(instance.getInstanceId());
                if (jclass != null) name = BrowserUtils.getSimpleType(jclass.getName());
//                if (jclass != null) name = jclass.getName();
            }
            return name;
        } else if (CONSTRUCTOR_MASK.equals(className)) {                        // Constructor
            Object value = instance.getValueOfField("clazz");                   // NOI18N
            if (value instanceof Instance) return getDetailsString("java.lang.Class", (Instance)value, heap); // NOI18N
        } else if (METHOD_MASK.equals(className)) {                             // Method
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        } else if (FIELD_MASK.equals(className)) {                              // Field
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        } else if (PARAMETER_MASK.equals(className)) {                          // Parameter
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        return null;
    }
    
}
