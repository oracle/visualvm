/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.client.RuntimeProfilingPoint;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;


/**
 * This class provides essentially a convenience static-method API that allows one to obtain a version of a particular
 * method, instrumented in a particular predefined way.
 *
 * @author Tomas Hurka
 * @author  Misha Dmitriev
 */
public class InstrumentationFactory implements CommonConstants {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static byte[] instrumentAsProiflePointHitMethod(DynamicClassInfo clazz, int methodIdx, int injType,
                                                           RuntimeProfilingPoint[] points) {
        Injector mi = new ProfilePointHitCallInjector(clazz, clazz.getBaseCPoolCount(injType), methodIdx, points,
                                                      CPExtensionsRepository.normalContents_ProfilePointHitMethodIdx);

        return mi.instrumentMethod();
    }

    public static byte[] instrumentAsReflectInvokeMethod(DynamicClassInfo clazz, int methodIdx) {
        Injector mi = new HandleReflectInvokeCallInjector(clazz, clazz.getBaseCPoolCount(INJ_REFLECT_METHOD_INVOKE), methodIdx);

        return mi.instrumentMethod();
    }

    public static byte[] instrumentAsServletDoMethod(DynamicClassInfo clazz, int methodIdx) {
        Injector mi = new HandleServletDoMethodCallInjector(clazz, clazz.getBaseCPoolCount(INJ_SERVLET_DO_METHOD), methodIdx);

        return mi.instrumentMethod();
    }

    public static byte[] instrumentCodeRegion(DynamicClassInfo clazz, int methodIdx, int bci0, int bci1) {
        Injector mi = new CodeRegionEntryExitCallsInjector(clazz, clazz.getBaseCPoolCount(INJ_CODE_REGION), methodIdx, bci0, bci1);

        return mi.instrumentMethod();
    }

    /** injType is either INJ_OBJECT_ALLOCATIONS or INJ_OBJECT_LIVENESS */
    public static byte[] instrumentForMemoryProfiling(DynamicClassInfo clazz, int methodIdx,
                                                      boolean[] allUnprofiledClassStatusArray, int injType,
                                                      RuntimeProfilingPoint[] points, InstrumentationFilter instrFilter,
                                                      boolean checkForOpcNew, boolean checkForOpcNewArray) {
        Injector mi = new ObjLivenessInstrCallsInjector(clazz, clazz.getBaseCPoolCount(injType), methodIdx,
                                                        allUnprofiledClassStatusArray, instrFilter,
                                                        checkForOpcNew, checkForOpcNewArray);
        mi.insertProfilingPoints(points, CPExtensionsRepository.memoryProfContents_ProfilePointHitMethodIdx);

        return mi.instrumentMethod();
    }

    /**
     * normalInjectionType is either INJ_RECURSIVE_NORMAL_METHOD or INJ_RECURSIVE_SAMPLED_NORMAL_METHOD
     * rootInjectionType is either INJ_RECURSIVE_ROOT_METHOD or INJ_RECURSIVE_SAMPLED_ROOT_METHOD
     */
    public static byte[] instrumentMethod(DynamicClassInfo clazz, int methodIdx, int normalInjectionType, int rootInjectionType,
                                          int markerInjectionType, int methodId, RuntimeProfilingPoint[] points) {
        int baseCPCount0 = clazz.getBaseCPoolCount(normalInjectionType);
        int baseCPCount1;
        int injType;

        if (clazz.isMethodRoot(methodIdx)) {
            baseCPCount1 = clazz.getBaseCPoolCount(rootInjectionType);
            injType = rootInjectionType;
        } else if (clazz.isMethodMarker(methodIdx)) {
            baseCPCount1 = clazz.getBaseCPoolCount(markerInjectionType);
            injType = markerInjectionType;
        } else {
            baseCPCount1 = 0;
            injType = normalInjectionType;
        }

        Injector mi = new MethodEntryExitCallsInjector(clazz, baseCPCount0, baseCPCount1, methodIdx, injType, methodId);
        mi.insertProfilingPoints(points, CPExtensionsRepository.normalContents_ProfilePointHitMethodIdx);

        byte[] res = mi.instrumentMethod();
        clazz.setInstrMethodId(methodIdx, methodId);

        return res;
    }
}
