/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.details.jdk;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class LangDetailsProvider extends DetailsProvider.Basic {
    private static final String ENUM_MASK = "java.lang.Enum+";                    // NOI18N
    private static final String STACKTRACE_MASK = "java.lang.StackTraceElement";    // NOI18N
    
    public LangDetailsProvider() {
        super(ENUM_MASK, STACKTRACE_MASK);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (ENUM_MASK.equals(className)) {                                      // Enum+
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
            int ordinal = DetailsUtils.getIntFieldValue(instance, "ordinal", -1); // NOI18N
            if (name != null) {
                if (ordinal != -1) {
                    return name+" ("+ordinal+")";       // NOI18N
                }
                return name;
            }
        } else if (STACKTRACE_MASK.equals(className)) {                         // StackTraceElement
            String declaringClass = DetailsUtils.getInstanceFieldString(instance, "declaringClass", heap); // NOI18N
            if (declaringClass != null) {
                String methodName = DetailsUtils.getInstanceFieldString(instance, "methodName", heap); // NOI18N
                String fileName = DetailsUtils.getInstanceFieldString(instance, "fileName", heap); // NOI18N
                int lineNumber = DetailsUtils.getIntFieldValue(instance, "lineNumber", -1); // NOi18N                
                if (methodName == null) methodName = "Unknown method";   // NOI18N
                StackTraceElement ste = new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
                return ste.toString();
            }
        }
        
        return null;
    }
    
}
