/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package org.netbeans.lib.profiler.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.wireprotocol.HeapHistogramResponse;

/**
 *
 * @author Tomas Hurka
 */
class HeapHistogramManager {

    private static final String BOOLEAN_TEXT = "boolean"; // NOI18N
    private static final String CHAR_TEXT = "char"; // NOI18N
    private static final String BYTE_TEXT = "byte"; // NOI18N
    private static final String SHORT_TEXT = "short"; // NOI18N
    private static final String INT_TEXT = "int"; // NOI18N
    private static final String LONG_TEXT = "long"; // NOI18N
    private static final String FLOAT_TEXT = "float"; // NOI18N
    private static final String DOUBLE_TEXT = "double"; // NOI18N
    private static final String VOID_TEXT = "void"; // NOI18N
    private static final char BOOLEAN_CODE = 'Z'; // NOI18N
    private static final char CHAR_CODE = 'C'; // NOI18N
    private static final char BYTE_CODE = 'B'; // NOI18N
    private static final char SHORT_CODE = 'S'; // NOI18N
    private static final char INT_CODE = 'I'; // NOI18N
    private static final char LONG_CODE = 'J'; // NOI18N
    private static final char FLOAT_CODE = 'F'; // NOI18N
    private static final char DOUBLE_CODE = 'D'; // NOI18N
    private static final char OBJECT_CODE = 'L'; // NOI18N
    private static final Map permGenNames = new HashMap();

    static {
        permGenNames.put("<methodKlass>", "Read-Write Method Metadata");       // NOI18N
        permGenNames.put("<constMethodKlass>", "Read-Only Method Metadata");   // NOI18N
        permGenNames.put("<methodDataKlass>", "Method Profiling Information"); // NOI18N
        permGenNames.put("<constantPoolKlass>", "Constant Pool Metadata");     // NOI18N
        permGenNames.put("<constantPoolCacheKlass>", "Class Resolution Optimization Metadata");  // NOI18N
        permGenNames.put("<symbolKlass>", "VM Symbol Metadata");               // NOI18N
        permGenNames.put("<compiledICHolderKlass>", "Inline Cache Metadata");  // NOI18N
        permGenNames.put("<instanceKlassKlass>", "Instance Class Metadata");   // NOI18N
        permGenNames.put("<objArrayKlassKlass>", "Object Array Class Metadata");  // NOI18N
        permGenNames.put("<typeArrayKlassKlass>", "Scalar Array Class Metadata"); // NOI18N
        permGenNames.put("<klassKlass>", "Base Class Metadata");                  // NOI18N
        permGenNames.put("<arrayKlassKlass>", "Base Array Class Metadata");       // NOI18N
    }
    Map classesIdMap = new HashMap(8000);
    boolean isJrockitVM;

    HeapHistogramManager() {
        String vmName = System.getProperty("java.vm.name"); // NOI18N

        if (vmName != null) {
            if ("BEA JRockit(R)".equals(vmName)) {  // NOI18N
                isJrockitVM = true;
            } else if ("Oracle JRockit(R)".equals(vmName)) {  // NOI18N
                isJrockitVM = true;
            }
        }
    }

    HeapHistogramResponse computeHistogram(InputStream in) {
        HeapHistogramResponse histogram;

        if (in == null) {
            return null;
        }
        if (isJrockitVM) {
            histogram = computeHistogramJRockit(in);
        } else {    // HotSpot
            histogram = computeHistogramImpl(in);
        }
        try {
            in.close();
        } catch (IOException ex) {
            System.err.println(CommonConstants.ENGINE_WARNING + getClass() + "cannot close InputStream");
        }
        return histogram;
    }

    private HeapHistogramResponse computeHistogramJRockit(InputStream in) {
        long totalHeapBytes = 0;
        long totalHeapInstances = 0;
        long totalInstances = 0;
        long totalBytes = 0;
        Map classesMap = new HashMap(1024);
        Date time = new Date();
        Scanner sc = new Scanner(in, "UTF-8");  // NOI18N
        sc.useRadix(10);
        sc.nextLine();
        sc.skip("-+");
        sc.nextLine();

        while (sc.hasNext("[0-9]+\\.[0-9]%")) {  // NOI18N
            JRockitClassInfoImpl newClInfo = new JRockitClassInfoImpl(sc);

            if (ProfilerInterface.serverInternalClassName(newClInfo.getName())) {
                continue;
            }
            storeClassInfo(newClInfo, classesMap);
            totalHeapBytes += newClInfo.getBytes();
            totalHeapInstances += newClInfo.getInstancesCount();
        }
        totalInstances = totalHeapInstances;
        totalBytes = totalHeapBytes;
        return getResponse(classesMap, time);
    }

    private HeapHistogramResponse computeHistogramImpl(InputStream in) {
        long totalBytes = 0;
        long totalInstances = 0;
        long totalHeapBytes = 0;
        long totalHeapInstances = 0;
        long totalPermGenBytes = 0;
        long totalPermgenInstances = 0;
        Map permGenMap = new HashMap(1024);
        Map classesMap = new HashMap(1024);
        int[] ids;
        long[] instances, bytes;
        int[] newids;
        String[] newNames;
        Map newClassNames;
        Date time = new Date();
        Scanner sc = new Scanner(in, "UTF-8");  // NOI18N

        sc.useRadix(10);
        sc.nextLine();
        sc.nextLine();
        sc.skip("-+");
        sc.nextLine();

        while (sc.hasNext("[0-9]+:")) {  // NOI18N
            ClassInfoImpl newClInfo = new ClassInfoImpl(sc);

            if (ProfilerInterface.serverInternalClassName(newClInfo.getName())) {
                continue;
            }
            if (newClInfo.isPermGen()) {
                storeClassInfo(newClInfo, permGenMap);
                totalPermGenBytes += newClInfo.getBytes();
                totalPermgenInstances += newClInfo.getInstancesCount();
            } else {
                storeClassInfo(newClInfo, classesMap);
                totalHeapBytes += newClInfo.getBytes();
                totalHeapInstances += newClInfo.getInstancesCount();
            }
        }
        sc.next("Total");   // NOI18N
        totalInstances = sc.nextLong();
        totalBytes = sc.nextLong();
        return getResponse(classesMap, time);
    }

    static void storeClassInfo(final ClassInfoImpl newClInfo, final Map map) {
        ClassInfoImpl oldClInfo = (ClassInfoImpl) map.get(newClInfo.getName());
        if (oldClInfo == null) {
            map.put(newClInfo.getName(), newClInfo);
        } else {
            oldClInfo.bytes += newClInfo.getBytes();
            oldClInfo.instances += newClInfo.getInstancesCount();
        }
    }

    private HeapHistogramResponse getResponse(Map classesMap, Date time) {
        Map newClassNames;
        int[] ids;
        long[] instances;
        long[] bytes;
        int[] newids;
        String[] newNames;
        newClassNames = new HashMap(100);
        ids = new int[classesMap.size()];
        instances = new long[classesMap.size()];
        bytes = new long[classesMap.size()];
        int outIndex = 0;
        Iterator it = classesMap.values().iterator();
        while (it.hasNext()) {
            ClassInfoImpl ci = (ClassInfoImpl) it.next();
            String name = ci.getName();
            Integer cindex = (Integer) classesIdMap.get(name);
            if (cindex == null) {
                cindex = new Integer(classesIdMap.size());
                classesIdMap.put(name, cindex);
                newClassNames.put(name, cindex);
            }
            ids[outIndex] = cindex.intValue();
            instances[outIndex] = ci.instances;
            bytes[outIndex] = ci.bytes;
            outIndex++;
        }
        newids = new int[newClassNames.size()];
        newNames = new String[newClassNames.size()];
        outIndex = 0;
        it = newClassNames.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry newClassEntry = (Map.Entry) it.next();
            newids[outIndex] = ((Integer) newClassEntry.getValue()).intValue();
            newNames[outIndex] = (String) newClassEntry.getKey();
            outIndex++;
        }
        return new HeapHistogramResponse(time, newNames, newids, ids, instances, bytes);
    }

    static class ClassInfoImpl {

        long instances;
        long bytes;
        String name;
        boolean permGen;

        ClassInfoImpl() {
        }

        ClassInfoImpl(Scanner sc) {
            String jvmName;

            sc.next();
            instances = sc.nextLong();
            bytes = sc.nextLong();
            jvmName = sc.next();
            permGen = jvmName.charAt(0) == '<';
            name = convertJVMName(jvmName);
        }

        public String getName() {
            return name;
        }

        public long getInstancesCount() {
            return instances;
        }

        public long getBytes() {
            return bytes;
        }

        public int hashCode() {
            return getName().hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof ClassInfoImpl) {
                return getName().equals(((ClassInfoImpl) obj).getName());
            }
            return false;
        }

        private boolean isPermGen() {
            return permGen;
        }

        String convertJVMName(String jvmName) {
            String name = null;
            int index = jvmName.lastIndexOf('[');

            if (index != -1) {
                switch (jvmName.charAt(index + 1)) {
                    case BOOLEAN_CODE:
                        name = BOOLEAN_TEXT;
                        break;
                    case CHAR_CODE:
                        name = CHAR_TEXT;
                        break;
                    case BYTE_CODE:
                        name = BYTE_TEXT;
                        break;
                    case SHORT_CODE:
                        name = SHORT_TEXT;
                        break;
                    case INT_CODE:
                        name = INT_TEXT;
                        break;
                    case LONG_CODE:
                        name = LONG_TEXT;
                        break;
                    case FLOAT_CODE:
                        name = FLOAT_TEXT;
                        break;
                    case DOUBLE_CODE:
                        name = DOUBLE_TEXT;
                        break;
                    case OBJECT_CODE:
                        name = jvmName.substring(index + 2, jvmName.length() - 1);
                        break;
                    default:
                        System.err.println("Uknown name " + jvmName);
                        name = jvmName;
                }
                for (int i = 0; i <= index; i++) {
                    name += "[]";
                }
            } else if (isPermGen()) {
                name = (String) permGenNames.get(jvmName);
            }
            if (name == null) {
                name = jvmName;
            }
            return name;
        }
    }

    static class JRockitClassInfoImpl extends ClassInfoImpl {

        JRockitClassInfoImpl(Scanner sc) {
            String jvmName;

            sc.next();  // skip unused 99.9%
            bytes = computeBytes(sc.next());
            instances = sc.nextLong();
            sc.next(); // diff unused
            jvmName = sc.next();
            name = convertJVMName(jvmName.replace('/', '.'));
        }

        private long computeBytes(String size) {
            String multi = size.substring(size.length() - 1);
            long bytes = Long.parseLong(size.substring(0, size.length() - 1));
            if ("K".equalsIgnoreCase(multi)) {  // NOI18N
                bytes *= 1024;
            } else if ("M".equalsIgnoreCase(multi)) {   // NOI18N
                bytes *= 1024 * 1024;
            } else if ("G".equalsIgnoreCase(multi)) {   // NOI18N
                bytes *= 1024 * 1024 * 1024L;
            }
            return bytes;
        }
    }
}
