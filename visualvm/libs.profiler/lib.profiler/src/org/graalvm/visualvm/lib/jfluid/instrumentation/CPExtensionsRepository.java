/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.instrumentation.ConstantPoolExtension.CPEntry;
import org.graalvm.visualvm.lib.jfluid.instrumentation.ConstantPoolExtension.PackedCPFragment;


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
    public static int miContents_AddParCharMethodIdx;
    public static int miContents_AddParByteMethodIdx;
    public static int miContents_AddParIntMethodIdx;
    public static int miContents_AddParBooleanMethodIdx;
    public static int miContents_AddParFloatMethodIdx;
    public static int miContents_AddParDoubleMethodIdx;
    public static int miContents_AddParShortMethodIdx;
    public static int miContents_AddParLongMethodIdx;
    public static int miContents_AddParObjectMethodIdx;
    public static int rootContents_MarkerExitMethodIdx;
    public static int rootContents_MarkerExitParMethodIdx;
    public static int miContents_HandleReflectInvokeMethodIdx;
    public static int miContents_HandleServletDoMethodIdx;
    public static int codeRegionContents_CodeRegionEntryMethodIdx;
    public static int codeRegionContents_CodeRegionExitMethodIdx;
    public static int memoryProfContents_ProfilePointHitMethodIdx;
    public static int memoryProfContents_TraceObjAllocMethodIdx; // Make sure it's the same for Obj Allocation and Obj Liveness, otherwise will have to change dependent code

    //------------------------------------ Private implementation -----------------------------------------------

    // Names and signatures of methods, calls to which we inject into TA code
    private static final String PROFRUNTIME_CPU_CLASS_NAME = "org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPU"; // NOI18N
    private static final String PROFRUNTIME_CPUFULL_CLASS_NAME = "org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr"; // NOI18N
    private static final String PROFRUNTIME_CPUSAMPLED_CLASS_NAME = "org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUSampledInstr"; // NOI18N
    private static final String PROFRUNTIME_CPUCODEREGION_CLASS_NAME = "org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUCodeRegion"; // NOI18N
    private static final String PROFRUNTIME_OBJALLOC_CLASS_NAME = "org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeObjAlloc"; // NOI18N
    private static final String PROFRUNTIME_OBJLIVENESS_CLASS_NAME = "org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeObjLiveness"; // NOI18N
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
    private static final String ADD_PARAMETER = "addParameter"; // NOI18N
    private static final String VOID_VOID_SIGNATURE = "()V"; // NOI18N
    private static final String CHAR_VOID_SIGNATURE = "(C)V"; // NOI18N
    private static final String BYTE_VOID_SIGNATURE = "(B)V"; // NOI18N
    private static final String INT_VOID_SIGNATURE = "(I)V"; // NOI18N
    private static final String BOOLEAN_VOID_SIGNATURE = "(Z)V"; // NOI18N
    private static final String FLOAT_VOID_SIGNATURE = "(F)V"; // NOI18N
    private static final String DOUBLE_VOID_SIGNATURE = "(D)V"; // NOI18N
    private static final String SHORT_VOID_SIGNATURE = "(S)V"; // NOI18N
    private static final String LONG_VOID_SIGNATURE = "(J)V"; // NOI18N
    private static final String OBJECT_VOID_SIGNATURE = "(Ljava/lang/Object;)V"; // NOI18N
    private static final String OBJECT_CHAR_VOID_SIGNATURE = "(Ljava/lang/Object;C)V"; // NOI18N
    private static final String REFLECT_METHOD_VOID_SIGNATURE = "(Ljava/lang/reflect/Method;)V"; // NOI18N
    private static final String JAVA_LANG_THROWABLE_NAME = "java/lang/Throwable"; // NOI18N
    private static final String STACK_MAP_TABLE_ATTRIBUTE = "StackMapTable"; // NOI18N

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
        int i = 0;
        int methodEntryMethodRefIdx = i;
        i = addMethod(PROFRUNTIME_CPUFULL_CLASS_NAME, METHOD_ENTRY_METHOD_NAME, CHAR_VOID_SIGNATURE, entries, i);
        int methodExitMethodRefIdx = i;
        i = addMethod(PROFRUNTIME_CPUFULL_CLASS_NAME, METHOD_EXIT_METHOD_NAME, CHAR_VOID_SIGNATURE, entries, i);
        int profilePointHitMethodRefIdx = i;
        addMethod(PROFRUNTIME_CPUFULL_CLASS_NAME, PROFILE_POINT_HIT, CHAR_VOID_SIGNATURE, entries, i);
        int profilerRuntimeClassRefIdx = getClassCPEntryIndex(PROFRUNTIME_CPUFULL_CLASS_NAME, entries);
        int charVoidSignatureIdx = getUtf8CPEntryIndex(CHAR_VOID_SIGNATURE, entries);
        standardCPFragments[INJ_RECURSIVE_NORMAL_METHOD] = new PackedCPFragment(entries);
        normalContents_ProfilePointHitMethodIdx = profilePointHitMethodRefIdx;
        normalContents_MethodEntryMethodIdx = methodEntryMethodRefIdx;
        normalContents_MethodExitMethodIdx = methodExitMethodRefIdx;

        // Create cpool contents for "sampled instrumentation" code injection, by replacing just
        // the instrumentation class name
        entries[getUtf8CPEntryIndex(PROFRUNTIME_CPUFULL_CLASS_NAME, entries)] = new CPEntry(PROFRUNTIME_CPUSAMPLED_CLASS_NAME);
        standardCPFragments[INJ_RECURSIVE_SAMPLED_NORMAL_METHOD] = new PackedCPFragment(entries);

        // Additional constant pool contents for rootEntry(char methodId) injection
        entries = new CPEntry[3];
        addMethod(profilerRuntimeClassRefIdx + 0x10000, ROOT_ENTRY_METHOD_NAME, charVoidSignatureIdx + 0x10000, entries, 0);
        standardCPFragments[INJ_RECURSIVE_ROOT_METHOD] = new PackedCPFragment(entries);
        rootContents_RootEntryMethodIdx = 0;

        // rootEntry() injection for sampled instrumentation is the same as for full instrumentation
        standardCPFragments[INJ_RECURSIVE_SAMPLED_ROOT_METHOD] = new PackedCPFragment(entries);

        // Additional constant pool contents for markerMethodEntry(char methodId) and markerMethodExit(char methodId) injection
        entries = new CPEntry[39];
        i = 0;
        int markerEntryMethodRefIdx = i;
        i = addMethod(profilerRuntimeClassRefIdx + 0x10000, MARKER_ENTRY_METHOD_NAME, charVoidSignatureIdx + 0x10000, entries, i);
        int markerExitMethodRefIdx = i;
        i = addMethod(profilerRuntimeClassRefIdx + 0x10000, MARKER_EXIT_METHOD_NAME, charVoidSignatureIdx + 0x10000, entries, i);
        
        rootContents_MarkerExitParMethodIdx = i;        
        i = addMethod(profilerRuntimeClassRefIdx + 0x10000, MARKER_EXIT_METHOD_NAME, OBJECT_CHAR_VOID_SIGNATURE, entries, i);
        miContents_AddParCharMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, CHAR_VOID_SIGNATURE, entries, i);
        miContents_AddParByteMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, BYTE_VOID_SIGNATURE, entries, i);
        miContents_AddParIntMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, INT_VOID_SIGNATURE, entries, i);
        miContents_AddParBooleanMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, BOOLEAN_VOID_SIGNATURE, entries, i);
        miContents_AddParFloatMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, FLOAT_VOID_SIGNATURE, entries, i);
        miContents_AddParDoubleMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, DOUBLE_VOID_SIGNATURE, entries, i);
        miContents_AddParShortMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, SHORT_VOID_SIGNATURE, entries, i);
        miContents_AddParLongMethodIdx = i;
        i = addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, LONG_VOID_SIGNATURE, entries, i);
        miContents_AddParObjectMethodIdx = i;
        addMethod(PROFRUNTIME_CPU_CLASS_NAME, ADD_PARAMETER, OBJECT_VOID_SIGNATURE, entries, i);
        standardCPFragments[INJ_RECURSIVE_MARKER_METHOD] = new PackedCPFragment(entries);
        rootContents_MarkerEntryMethodIdx = markerEntryMethodRefIdx;
        rootContents_MarkerExitMethodIdx = markerExitMethodRefIdx;

        // markerMethodEntry() injection for sampled instrumentation is the same as for full instrumentation
        standardCPFragments[INJ_RECURSIVE_SAMPLED_MARKER_METHOD] = new PackedCPFragment(entries);

        // Now initialize the constant pool contents added to class java.lang.reflect.Method, to support invoke() instrumentation    
        entries = new CPEntry[6];
        addMethod(PROFRUNTIME_CPU_CLASS_NAME, HANDLE_REFLECT_INVOKE_METHOD_NAME, REFLECT_METHOD_VOID_SIGNATURE, entries, 0);
        standardCPFragments[INJ_REFLECT_METHOD_INVOKE] = new PackedCPFragment(entries);
        miContents_HandleReflectInvokeMethodIdx = 0;

        // Now initialize the constant pool contents added to class javax.servlet.http.HttpServlet , 
        // to support doGet(), doPost(), doPut(), doDelete() servlet tracking    
        entries = new CPEntry[6];
        addMethod(PROFRUNTIME_CPU_CLASS_NAME, HANDLE_SERVLET_DO_METHOD_NAME, OBJECT_VOID_SIGNATURE, entries, 0);
        standardCPFragments[INJ_SERVLET_DO_METHOD] = new PackedCPFragment(entries);
        miContents_HandleServletDoMethodIdx = 0;

        // Initialize the constant pool contents used for code region profiling.
        entries = new CPEntry[9];
        i = 0;
        int codeRegionEntryMethodRefIdx = i;
        i = addMethod(PROFRUNTIME_CPUCODEREGION_CLASS_NAME, CODE_REGION_ENTRY_METHOD_NAME, VOID_VOID_SIGNATURE, entries, i);
        int codeRegionExitMethodRefIdx = i;
        addMethod(PROFRUNTIME_CPUCODEREGION_CLASS_NAME, CODE_REGION_EXIT_METHOD_NAME, VOID_VOID_SIGNATURE, entries, i);
        standardCPFragments[INJ_CODE_REGION] = new PackedCPFragment(entries);
        codeRegionContents_CodeRegionEntryMethodIdx = codeRegionEntryMethodRefIdx;
        codeRegionContents_CodeRegionExitMethodIdx = codeRegionExitMethodRefIdx;

        // Initialize the constant pool contents used for object allocation profiling
        entries = new CPEntry[10];
        i = 0;
        int objAllocTraceMethodRefIdx = i;
        i = addMethod(PROFRUNTIME_OBJALLOC_CLASS_NAME, TRACE_OBJ_ALLOC_METHOD_NAME, OBJECT_CHAR_VOID_SIGNATURE, entries, i);
        int memPprofilePointHitMethodRefIdx = i;
        addMethod(PROFRUNTIME_OBJALLOC_CLASS_NAME, PROFILE_POINT_HIT, CHAR_VOID_SIGNATURE, entries, i);
        standardCPFragments[INJ_OBJECT_ALLOCATIONS] = new PackedCPFragment(entries);
        memoryProfContents_TraceObjAllocMethodIdx = objAllocTraceMethodRefIdx;
        memoryProfContents_ProfilePointHitMethodIdx = memPprofilePointHitMethodRefIdx;

        // Create cpool contents for "object liveness profiling" code injection, by replacing just
        // the instrumentation class name
        entries[getUtf8CPEntryIndex(PROFRUNTIME_OBJALLOC_CLASS_NAME, entries)] = new CPEntry(PROFRUNTIME_OBJLIVENESS_CLASS_NAME);
        standardCPFragments[INJ_OBJECT_LIVENESS] = new PackedCPFragment(entries);

        /*memoryProfContents_TraceObjAllocMethodIdx = objAllocTraceMethodRefIdx;  // Same as above */
        
        entries = new CPEntry[1];
        entries[0] = new CPEntry(STACK_MAP_TABLE_ATTRIBUTE);
        standardCPFragments[INJ_STACKMAP] = new PackedCPFragment(entries);
        
        entries = new CPEntry[2];
        addClass(JAVA_LANG_THROWABLE_NAME, entries, 0);
        standardCPFragments[INJ_THROWABLE] = new PackedCPFragment(entries);

    }
    
    static int addMethod(int classRefIdx, String methodName, int signatureIdx, CPEntry[] entries, int index) {
        int methodRef = index;
        entries[index++] = new CPEntry(CONSTANT_Methodref);
        int nameAndTypeIdx = index;
        index = addNameAndType(methodName, signatureIdx, entries, index);
        entries[methodRef].setIndex1(classRefIdx);
        entries[methodRef].setIndex2(nameAndTypeIdx);
        return index;
    }

    static int addMethod(int classRefIdx, String methodName, String signature, CPEntry[] entries, int index) {
        int methodRef = index;
        entries[index++] = new CPEntry(CONSTANT_Methodref);
        int nameAndTypeIdx = index;
        index = addNameAndType(methodName, signature, entries, index);
        entries[methodRef].setIndex1(classRefIdx);
        entries[methodRef].setIndex2(nameAndTypeIdx);
        return index;
    }

    static int addMethod(String className, String methodName, String signature, CPEntry[] entries, int index) {
        int methodRef = index;
        entries[index++] = new CPEntry(CONSTANT_Methodref);
        int classIndex = getClassCPEntryIndex(className, entries);
        if (classIndex == -1) {
            classIndex = index;
            index = addClass(className, entries, index);
        }
        int nameAndTypeIdx = index;
        index = addNameAndType(methodName, signature, entries, index);
        entries[methodRef].setIndex1(classIndex);
        entries[methodRef].setIndex2(nameAndTypeIdx);
        return index;
    }
    
    static int addClass(String className, CPEntry[] entries, int index) {
        entries[index] = new CPEntry(CONSTANT_Class);
        entries[index].setIndex1(index+1);
        entries[index+1] = new CPEntry(className);
        return index+2;
    }
    
    static int addNameAndType(String methodName, String signature, CPEntry[] entries, int index) {
        int nameAndTypeIdx = index;
        entries[index++] = new CPEntry(CONSTANT_NameAndType);
        int nameIdx = getUtf8CPEntryIndex(methodName, entries);
        if (nameIdx == -1) {
            nameIdx = index;
            entries[index++] = new CPEntry(methodName);
        }
        int sigIdx = getUtf8CPEntryIndex(signature, entries);
        if (sigIdx == -1) {
            sigIdx = index;
            entries[index++] = new CPEntry(signature);
        }
        entries[nameAndTypeIdx].setIndex1(nameIdx);
        entries[nameAndTypeIdx].setIndex2(sigIdx);
        return index;
    }

    static int addNameAndType(String methodName, int signatureIdx, CPEntry[] entries, int index) {
        entries[index] = new CPEntry(CONSTANT_NameAndType);
        entries[index].setIndex1(index+1);
        entries[index].setIndex2(signatureIdx);
        entries[index+1] = new CPEntry(methodName);
        return index+2;
    }
    
    static boolean isUtf8CPEntry(CPEntry entry, String string) {
        return  entry != null && entry.tag == CONSTANT_Utf8 && entry.utf8.equals(string);
    }
    
    static int getUtf8CPEntryIndex(String string, CPEntry[] entries) {
        for (int i = 0; i < entries.length; i++) {
            if (isUtf8CPEntry(entries[i],string)) {
                return i;
            }
        }
        return -1;
    }

    static int getClassCPEntryIndex(String string, CPEntry[] entries) {
        for (int i = 0; i < entries.length; i++) {
            CPEntry e = entries[i];
            if (e != null && e.tag == CONSTANT_Class) {
                if (isUtf8CPEntry(entries[e.index1],string)) {
                    return i;                
                }
            }
        }
        return -1;
    }
}
