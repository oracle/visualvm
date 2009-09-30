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

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.instrumentation.ConstantPoolExtension.CPEntry;
import org.netbeans.lib.profiler.instrumentation.ConstantPoolExtension.PackedCPFragment;


/**
 * A repository containing semi-prepared constant pool fragments for all kinds of instrumentation used in JFluid.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class CPExtensionsRepository implements JavaClassConstants, CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // These indices, adjusted properly for the base constant pool length for a given class, should be used as cpool indices
    // of various methods injected into the target app methods.
    public static int normalContents_MethodEntryMethodIdx;

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // These indices, adjusted properly for the base constant pool length for a given class, should be used as cpool indices
    // of various methods injected into the target app methods.
    public static int normalContents_MethodExitMethodIdx;
    public static int normalContents_ProfilePointHitMethodIdx;
    public static int rootContents_RootEntryMethodIdx;
    public static int rootContents_MarkerEntryMethodIdx;
    public static int rootContents_MarkerExitMethodIdx;
    public static int miContents_HandleReflectInvokeMethodIdx;
    public static int miContents_HandleServletDoMethodIdx;
    public static int codeRegionContents_CodeRegionEntryMethodIdx;
    public static int codeRegionContents_CodeRegionExitMethodIdx;
    public static int memoryProfContents_ProfilePointHitMethodIdx;
    public static int memoryProfContents_TraceObjAllocMethodIdx; // Make sure it's the same for Obj Allocation and Obj Liveness, otherwise will have to change dependent code

    //------------------------------------ Private implementation -----------------------------------------------

    // Names and signatures of methods, calls to which we inject into TA code
    private static final String PROFRUNTIME_CPU_CLASS_NAME = "org/netbeans/lib/profiler/server/ProfilerRuntimeCPU"; // NOI18N
    private static final String PROFRUNTIME_CPUFULL_CLASS_NAME = "org/netbeans/lib/profiler/server/ProfilerRuntimeCPUFullInstr"; // NOI18N
    private static final String PROFRUNTIME_CPUSAMPLED_CLASS_NAME = "org/netbeans/lib/profiler/server/ProfilerRuntimeCPUSampledInstr"; // NOI18N
    private static final String PROFRUNTIME_CPUCODEREGION_CLASS_NAME = "org/netbeans/lib/profiler/server/ProfilerRuntimeCPUCodeRegion"; // NOI18N
    private static final String PROFRUNTIME_OBJALLOC_CLASS_NAME = "org/netbeans/lib/profiler/server/ProfilerRuntimeObjAlloc"; // NOI18N
    private static final String PROFRUNTIME_OBJLIVENESS_CLASS_NAME = "org/netbeans/lib/profiler/server/ProfilerRuntimeObjLiveness"; // NOI18N
    private static final String ROOT_ENTRY_METHOD_NAME = "rootMethodEntry"; // NOI18N
    private static final String MARKER_ENTRY_METHOD_NAME = "markerMethodEntry"; // NOI18N
    private static final String MARKER_EXIT_METHOD_NAME = "markerMethodExit"; // NOI18N
    private static final String METHOD_ENTRY_METHOD_NAME = "methodEntry"; // NOI18N
    private static final String METHOD_EXIT_METHOD_NAME = "methodExit"; // NOI18N
    private static final String HANDLE_REFLECT_INVOKE_METHOD_NAME = "handleJavaLangReflectMethodInvoke"; // NOI18N
    private static final String CODE_REGION_ENTRY_METHOD_NAME = "codeRegionEntry"; // NOI18N
    private static final String CODE_REGION_EXIT_METHOD_NAME = "codeRegionExit"; // NOI18N
    private static final String TRACE_OBJ_ALLOC_METHOD_NAME = "traceObjAlloc"; // NOI18N
    private static final String PROFILE_POINT_HIT = "profilePointHit"; // NOI18N
    private static final String HANDLE_SERVLET_DO_METHOD_NAME = "handleServletDoMethod"; // NOI18N
    private static final String VOID_VOID_SIGNATURE = "()V"; // NOI18N
    private static final String CHAR_VOID_SIGNATURE = "(C)V"; // NOI18N
    private static final String VOID_OBJECT_SIGNATURE = "(Ljava/lang/Object;)V"; // NOI18N
    private static final String OBJECT_CHAR_VOID_SIGNATURE = "(Ljava/lang/Object;C)V"; // NOI18N
    private static final String REFLECT_METHOD_VOID_SIGNATURE = "(Ljava/lang/reflect/Method;)V"; // NOI18N
    private static final String OBJECT_CHAR_BOOLEAN_VOID_SIGNATURE = "(Ljava/lang/Object;CZ)V"; // NOI18N
    private static final String JAVA_LANG_THROWABLE_NAME = "java/lang/Throwable"; // NOI18N

    // Predefined constant pools for various kinds of instrumentation
    private static PackedCPFragment[] standardCPFragments;

    static {
        initCommonAddedContents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static PackedCPFragment getStandardCPFragment(int injectionType) {
        return standardCPFragments[injectionType];
    }

    // Create standard added cpool contents for each injection kind defined by JFluid.
    private static void initCommonAddedContents() {
        standardCPFragments = new PackedCPFragment[INJ_MAXNUMBER];

        // Initialize the "normal" recursive instrumentation added constant pool contents
        CPEntry[] entries = new CPEntry[12];
        int i = -1;
        entries[++i] = new CPEntry(CHAR_VOID_SIGNATURE);

        int charVoidSignatureIdx = i;
        entries[++i] = new CPEntry(CONSTANT_Class);

        int profilerRuntimeClassRefIdx = i;
        entries[++i] = new CPEntry(PROFRUNTIME_CPUFULL_CLASS_NAME);

        int profilerRuntimeClassNameIdx = i;
        entries[profilerRuntimeClassRefIdx].setIndex1(i);
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int methodEntryMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[methodEntryMethodRefIdx].setIndex2(i);

        int methodEntryNameAndTypeIdx = i;
        entries[++i] = new CPEntry(METHOD_ENTRY_METHOD_NAME);
        entries[methodEntryNameAndTypeIdx].setIndex1(i);
        entries[methodEntryNameAndTypeIdx].setIndex2(charVoidSignatureIdx);

        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int methodExitMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[methodExitMethodRefIdx].setIndex2(i);

        int methodExitNameAndTypeIdx = i;
        entries[++i] = new CPEntry(METHOD_EXIT_METHOD_NAME);
        entries[methodExitNameAndTypeIdx].setIndex1(i);
        entries[methodExitNameAndTypeIdx].setIndex2(charVoidSignatureIdx);

        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int profilePointHitMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[profilePointHitMethodRefIdx].setIndex2(i);

        int profilePointHitNameAndTypeIdx = i;
        entries[++i] = new CPEntry(PROFILE_POINT_HIT);
        entries[profilePointHitNameAndTypeIdx].setIndex1(i);
        entries[profilePointHitNameAndTypeIdx].setIndex2(charVoidSignatureIdx);
        normalContents_ProfilePointHitMethodIdx = profilePointHitMethodRefIdx;

        standardCPFragments[INJ_RECURSIVE_NORMAL_METHOD] = new PackedCPFragment(entries);
        normalContents_MethodEntryMethodIdx = methodEntryMethodRefIdx;
        normalContents_MethodExitMethodIdx = methodExitMethodRefIdx;

        // Create cpool contents for "sampled instrumentation" code injection, by replacing just
        // the instrumentation class name
        entries[profilerRuntimeClassNameIdx] = new CPEntry(PROFRUNTIME_CPUSAMPLED_CLASS_NAME);
        standardCPFragments[INJ_RECURSIVE_SAMPLED_NORMAL_METHOD] = new PackedCPFragment(entries);

        // Additional constant pool contents for rootEntry(char methodId) injection
        entries = new CPEntry[3];
        i = -1;
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx + 0x10000);

        int rootEntryMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[rootEntryMethodRefIdx].setIndex2(i);

        int rootEntryNameAndTypeIdx = i;
        entries[++i] = new CPEntry(ROOT_ENTRY_METHOD_NAME);
        entries[rootEntryNameAndTypeIdx].setIndex1(i);
        entries[rootEntryNameAndTypeIdx].setIndex2(charVoidSignatureIdx + 0x10000);

        standardCPFragments[INJ_RECURSIVE_ROOT_METHOD] = new PackedCPFragment(entries);
        rootContents_RootEntryMethodIdx = rootEntryMethodRefIdx;

        // rootEntry() injection for sampled instrumentation is the same as for full instrumentation
        standardCPFragments[INJ_RECURSIVE_SAMPLED_ROOT_METHOD] = new PackedCPFragment(entries);

        // Additional constant pool contents for markerMethodEntry(char methodId) and markerMethodExit(char methodId) injection
        entries = new CPEntry[6];
        i = -1;
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx + 0x10000);

        int markerEntryMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[markerEntryMethodRefIdx].setIndex2(i);

        int markerEntryNameAndTypeIdx = i;
        entries[++i] = new CPEntry(MARKER_ENTRY_METHOD_NAME);
        entries[markerEntryNameAndTypeIdx].setIndex1(i);
        entries[markerEntryNameAndTypeIdx].setIndex2(charVoidSignatureIdx + 0x10000);

        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx + 0x10000);

        int markerExitMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[markerExitMethodRefIdx].setIndex2(i);

        int markerExitNameAndTypeIdx = i;
        entries[++i] = new CPEntry(MARKER_EXIT_METHOD_NAME);
        entries[markerExitNameAndTypeIdx].setIndex1(i);
        entries[markerExitNameAndTypeIdx].setIndex2(charVoidSignatureIdx + 0x10000);

        standardCPFragments[INJ_RECURSIVE_MARKER_METHOD] = new PackedCPFragment(entries);
        rootContents_MarkerEntryMethodIdx = markerEntryMethodRefIdx;
        rootContents_MarkerExitMethodIdx = markerExitMethodRefIdx;

        // markerMethodEntry() injection for sampled instrumentation is the same as for full instrumentation
        standardCPFragments[INJ_RECURSIVE_SAMPLED_MARKER_METHOD] = new PackedCPFragment(entries);

        // Now initialize the constant pool contents added to class java.lang.reflect.Method, to support invoke() instrumentation    
        entries = new CPEntry[6];
        i = -1;
        entries[++i] = new CPEntry(REFLECT_METHOD_VOID_SIGNATURE);

        int methodSignatureIdx = i;
        entries[++i] = new CPEntry(CONSTANT_Class);
        profilerRuntimeClassRefIdx = i;
        entries[++i] = new CPEntry(PROFRUNTIME_CPU_CLASS_NAME);
        entries[profilerRuntimeClassRefIdx].setIndex1(i);
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int methodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[methodRefIdx].setIndex2(i);

        int nameAndTypeIdx = i;
        entries[++i] = new CPEntry(HANDLE_REFLECT_INVOKE_METHOD_NAME);
        entries[nameAndTypeIdx].setIndex1(i);
        entries[nameAndTypeIdx].setIndex2(methodSignatureIdx);

        standardCPFragments[INJ_REFLECT_METHOD_INVOKE] = new PackedCPFragment(entries);
        miContents_HandleReflectInvokeMethodIdx = methodRefIdx;

        // Now initialize the constant pool contents added to class javax.servlet.http.HttpServlet , 
        // to support doGet(), doPost(), doPut(), doDelete() servlet tracking    
        entries = new CPEntry[6];
        i = -1;
        entries[++i] = new CPEntry(VOID_OBJECT_SIGNATURE);

        int servetSignatureIdx = i;
        entries[++i] = new CPEntry(CONSTANT_Class);
        profilerRuntimeClassRefIdx = i;
        entries[++i] = new CPEntry(PROFRUNTIME_CPU_CLASS_NAME);
        entries[profilerRuntimeClassRefIdx].setIndex1(i);
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int servletRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[servletRefIdx].setIndex2(i);

        int servletNameAndTypeIdx = i;
        entries[++i] = new CPEntry(HANDLE_SERVLET_DO_METHOD_NAME);
        entries[servletNameAndTypeIdx].setIndex1(i);
        entries[servletNameAndTypeIdx].setIndex2(servetSignatureIdx);

        standardCPFragments[INJ_SERVLET_DO_METHOD] = new PackedCPFragment(entries);
        miContents_HandleServletDoMethodIdx = servletRefIdx;

        // Initialize the constant pool contents used for code region profiling.
        entries = new CPEntry[9];
        i = -1;
        entries[++i] = new CPEntry(VOID_VOID_SIGNATURE);

        int voidVoidSignatureIdx = i;
        entries[++i] = new CPEntry(CONSTANT_Class);
        profilerRuntimeClassRefIdx = i;
        entries[++i] = new CPEntry(PROFRUNTIME_CPUCODEREGION_CLASS_NAME);
        entries[profilerRuntimeClassRefIdx].setIndex1(i);
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int codeRegionEntryMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[codeRegionEntryMethodRefIdx].setIndex2(i);

        int codeRegionEntryNameAndTypeIdx = i;
        entries[++i] = new CPEntry(CODE_REGION_ENTRY_METHOD_NAME);
        entries[codeRegionEntryNameAndTypeIdx].setIndex1(i);
        entries[codeRegionEntryNameAndTypeIdx].setIndex2(voidVoidSignatureIdx);

        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int codeRegionExitMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[codeRegionExitMethodRefIdx].setIndex2(i);

        int codeRegionExitNameAndTypeIdx = i;
        entries[++i] = new CPEntry(CODE_REGION_EXIT_METHOD_NAME);
        entries[codeRegionExitNameAndTypeIdx].setIndex1(i);
        entries[codeRegionExitNameAndTypeIdx].setIndex2(voidVoidSignatureIdx);

        standardCPFragments[INJ_CODE_REGION] = new PackedCPFragment(entries);
        codeRegionContents_CodeRegionEntryMethodIdx = codeRegionEntryMethodRefIdx;
        codeRegionContents_CodeRegionExitMethodIdx = codeRegionExitMethodRefIdx;

        // Initialize the constant pool contents used for object allocation profiling
        entries = new CPEntry[10];
        i = -1;
        entries[++i] = new CPEntry(OBJECT_CHAR_VOID_SIGNATURE);

        int objCharVoidSignatureIdx = i;
        entries[++i] = new CPEntry(CHAR_VOID_SIGNATURE);

        int charVoidSignatureIdx2 = i;
        entries[++i] = new CPEntry(CONSTANT_Class);
        profilerRuntimeClassRefIdx = i;
        entries[++i] = new CPEntry(PROFRUNTIME_OBJALLOC_CLASS_NAME);
        entries[profilerRuntimeClassRefIdx].setIndex1(i);
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int objAllocTraceMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[objAllocTraceMethodRefIdx].setIndex2(i);

        int objAllocTraceNameAndTypeIdx = i;
        entries[++i] = new CPEntry(TRACE_OBJ_ALLOC_METHOD_NAME);
        entries[objAllocTraceNameAndTypeIdx].setIndex1(i);
        entries[objAllocTraceNameAndTypeIdx].setIndex2(objCharVoidSignatureIdx);

        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);

        int memPprofilePointHitMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[memPprofilePointHitMethodRefIdx].setIndex2(i);

        int memProfilePointHitNameAndTypeIdx = i;
        entries[++i] = new CPEntry(PROFILE_POINT_HIT);
        entries[memProfilePointHitNameAndTypeIdx].setIndex1(i);
        entries[memProfilePointHitNameAndTypeIdx].setIndex2(charVoidSignatureIdx2);
        memoryProfContents_ProfilePointHitMethodIdx = memPprofilePointHitMethodRefIdx;

        standardCPFragments[INJ_OBJECT_ALLOCATIONS] = new PackedCPFragment(entries);
        memoryProfContents_TraceObjAllocMethodIdx = objAllocTraceMethodRefIdx;

        // Initialize the constant pool contents used for object liveness profiling
        entries = new CPEntry[10];
        i = -1;
        entries[++i] = new CPEntry(OBJECT_CHAR_VOID_SIGNATURE);
        objCharVoidSignatureIdx = i;
        entries[++i] = new CPEntry(CHAR_VOID_SIGNATURE);
        entries[++i] = new CPEntry(CONSTANT_Class);
        profilerRuntimeClassRefIdx = i;
        entries[++i] = new CPEntry(PROFRUNTIME_OBJLIVENESS_CLASS_NAME);
        entries[profilerRuntimeClassRefIdx].setIndex1(i);
        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);
        objAllocTraceMethodRefIdx = i;
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[objAllocTraceMethodRefIdx].setIndex2(i);
        objAllocTraceNameAndTypeIdx = i;
        entries[++i] = new CPEntry(TRACE_OBJ_ALLOC_METHOD_NAME);
        entries[objAllocTraceNameAndTypeIdx].setIndex1(i);
        entries[objAllocTraceNameAndTypeIdx].setIndex2(objCharVoidSignatureIdx);

        entries[++i] = new CPEntry(CONSTANT_Methodref);
        entries[i].setIndex1(profilerRuntimeClassRefIdx);
        entries[++i] = new CPEntry(CONSTANT_NameAndType);
        entries[memPprofilePointHitMethodRefIdx].setIndex2(i);
        entries[++i] = new CPEntry(PROFILE_POINT_HIT);
        entries[memProfilePointHitNameAndTypeIdx].setIndex1(i);
        entries[memProfilePointHitNameAndTypeIdx].setIndex2(charVoidSignatureIdx2);

        standardCPFragments[INJ_OBJECT_LIVENESS] = new PackedCPFragment(entries);

        /*memoryProfContents_TraceObjAllocMethodIdx = objAllocTraceMethodRefIdx;  // Same as above */
    }
}
