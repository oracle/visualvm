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
package org.netbeans.modules.profiler.heapwalk.details.jdk;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class AtomicDetailsProvider extends DetailsProvider.Basic {
    
    private static final String BOOLEAN_MASK = "java.util.concurrent.atomic.AtomicBoolean+";    // NOI18N
    private static final String INTEGER_MASK = "java.util.concurrent.atomic.AtomicInteger+";    // NOI18N
    private static final String LONG_MASK = "java.util.concurrent.atomic.AtomicLong+";          // NOI18N
    private static final String REFERENCE_MASK = "java.util.concurrent.atomic.AtomicReference+";// NOI18N

    public AtomicDetailsProvider() {
        super(BOOLEAN_MASK, INTEGER_MASK, LONG_MASK, REFERENCE_MASK);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (BOOLEAN_MASK.equals(className)) {
            int value = DetailsUtils.getIntFieldValue(instance, "value", 0);                    // NOI18N
            return Boolean.toString(value != 0);
        } else if (INTEGER_MASK.equals(className)) {
            int value = DetailsUtils.getIntFieldValue(instance, "value", 0);                    // NOI18N
            return Integer.toString(value);
        } else if (LONG_MASK.equals(className)) {
            long value = DetailsUtils.getLongFieldValue(instance, "value", 0);                  // NOI18N
            return Long.toString(value);
        } else if (REFERENCE_MASK.equals(className)) {
            Object value = instance.getValueOfField("value");                                   // NOI18N
            if (value instanceof Instance) {
                Instance i = (Instance)value;
                String s = DetailsUtils.getInstanceString(i, heap);
                s = s == null ? "#" + i.getInstanceNumber() : ": " + s;                         // NOI18N
                return BrowserUtils.getSimpleType(i.getJavaClass().getName()) + s;
            }
        }
        return null;
    }
    
}
