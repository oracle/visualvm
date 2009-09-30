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

package org.netbeans.lib.profiler.instrumentation;

import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import org.netbeans.lib.profiler.global.CommonConstants;


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
                                                      RuntimeProfilingPoint[] points) {
        Injector mi = new ObjLivenessInstrCallsInjector(clazz, clazz.getBaseCPoolCount(injType), methodIdx,
                                                        allUnprofiledClassStatusArray);
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
