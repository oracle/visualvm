/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.truffle.details;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class PythonDetailsProvider extends DetailsProvider.Basic {

    private static final String PCLASS_MASK = "com.oracle.graal.python.runtime.standardtype.PythonClass+";   // NOI18N
    private static final String PFUNCTION_MASK = "com.oracle.graal.python.runtime.function.PFunction";   // NOI18N
    private static final String PNONE_MASK = "com.oracle.graal.python.runtime.datatype.PNone";   // NOI18N
    private static final String PLIST_MASK = "com.oracle.graal.python.runtime.sequence.PList";   // NOI18N
    private static final String BASIC_STORAGE_MASK = "com.oracle.graal.python.runtime.sequence.storage.BasicSequenceStorage+";   // NOI18N
    private static final String EMPTY_STORAGE_MASK = "com.oracle.graal.python.runtime.sequence.storage.EmptySequenceStorage"; // NOI18N
    private static final String PTUPLE_MASK = "com.oracle.graal.python.runtime.sequence.PTuple"; // NOI18N
    private static final String PMODULE_MASK = "com.oracle.graal.python.runtime.standardtype.PythonModule"; // NOI18N
    private static final String PBYTES_MASK = "com.oracle.graal.python.runtime.sequence.PBytes"; // NOI18N
    private static final String PCOMPLEX_MASK = "com.oracle.graal.python.runtime.datatype.PComplex"; // NOI18N
    private static final String PEXCEPTION_MASK = "com.oracle.graal.python.runtime.exception.PythonExceptionObject"; // NOI18N

    public PythonDetailsProvider() {
        super(PCLASS_MASK,PFUNCTION_MASK,PNONE_MASK,PLIST_MASK,BASIC_STORAGE_MASK,
              PTUPLE_MASK,PMODULE_MASK,PBYTES_MASK,EMPTY_STORAGE_MASK,
              PCOMPLEX_MASK, PEXCEPTION_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (PCLASS_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "className", heap);
        }
        if (PFUNCTION_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap);
        }
        if (PNONE_MASK.equals(className)) {
            return "None";
        }
        if (PLIST_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "store", heap);    // NOI18N
        }
        if (BASIC_STORAGE_MASK.equals(className)) {
            return DetailsUtils.getIntFieldValue(instance, "length", 0) + " items";
        }
        if (EMPTY_STORAGE_MASK.equals(className)) {
            return "0 items";
        }
        if (PTUPLE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "array", heap);    // NOI18N
        }
        if (PMODULE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        if (PBYTES_MASK.equals(className)) {
            return DetailsUtils.getPrimitiveArrayFieldString(instance, "bytes", 0, -1, ",", "...");
        }
        if (PCOMPLEX_MASK.equals(className)) {
            Double realObj = (Double) instance.getValueOfField("real");    // NOI18N
            Double imagObj = (Double) instance.getValueOfField("imag");    // NOI18N

            if (realObj != null && imagObj != null) {
                return complexToString(realObj.doubleValue(), imagObj.doubleValue());
            }
        }
        if (PEXCEPTION_MASK.equals(className)) {
             return DetailsUtils.getInstanceFieldString(instance, "message", heap); // NOI18N
        }
        return null;
    }

    private static String complexToString(double real, double imag) {
        if (real == 0.) {
            return toString(imag) + "j";
        } else {
            if (imag >= 0) {
                return String.format("(%s+%sj)", toString(real), toString(imag));
            } else {
                return String.format("(%s-%sj)", toString(real), toString(-imag));
            }
        }
    }

    private static String toString(double value) {
        if (value == Math.floor(value) && value <= Long.MAX_VALUE && value >= Long.MIN_VALUE) {
            return Long.toString((long) value);
        } else {
            return Double.toString(value);
        }
    }
}
