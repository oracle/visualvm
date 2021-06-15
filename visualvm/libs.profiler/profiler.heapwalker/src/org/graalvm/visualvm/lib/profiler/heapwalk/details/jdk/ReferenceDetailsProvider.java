/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.lang.ref.Reference;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class ReferenceDetailsProvider extends DetailsProvider.Basic {

    private static final String FEEBLE_REF_MASK = "com.oracle.svm.core.heap.FeebleReference+";  // NOI18N

    public ReferenceDetailsProvider() {
        super(Reference.class.getName() + "+", FEEBLE_REF_MASK);                // NOI18N
    }

    public String getDetailsString(String className, Instance instance) {
        if (FEEBLE_REF_MASK.equals(className)) {
            Object value = instance.getValueOfField("rawReferent");             // NOI18N
            if (value instanceof Instance) {
                return getRefDetail(value);
            }
        } else {
            Object value = instance.getValueOfField("referent");                // NOI18N
            if (value instanceof Instance) {
                return getRefDetail(value);
            }
            value = instance.getValueOfField("feeble");                         // NOI18N
            if (value instanceof Instance) {
                return DetailsUtils.getInstanceString((Instance) value);
            }
            value = instance.getValueOfField("bootImageStrongValue");           // NOI18N
            if (value instanceof Instance) {
                return getRefDetail(value);
            }
        }
        return null;
    }

    private String getRefDetail(Object value) {
        Instance i = (Instance)value;
        // TODO: can create cycle?
        String s = DetailsUtils.getInstanceString(i);
        s = s == null ? "#" + i.getInstanceNumber() : ": " + s;                 // NOI18N
        return BrowserUtils.getSimpleType(i.getJavaClass().getName()) + s;
    }
    
}
