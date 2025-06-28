/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.attach;

import org.graalvm.visualvm.application.jvm.HeapHistogram;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author Tomas Hurka
 */
class HeapHistogramImpl extends HeapHistogram {
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
    private static final Map<String,String> permGenNames = new HashMap<>();
    static {
        permGenNames.put("<methodKlass>","Read-Write Method Metadata");      // NOI18N
        permGenNames.put("<constMethodKlass>","Read-Only Method Metadata");     // NOI18N
        permGenNames.put("<methodDataKlass>","Method Profiling Information");     // NOI18N
        permGenNames.put("<constantPoolKlass>","Constant Pool Metadata");     // NOI18N
        permGenNames.put("<constantPoolCacheKlass>","Class Resolution Optimization Metadata");     // NOI18N
        permGenNames.put("<symbolKlass>","VM Symbol Metadata");     // NOI18N
        permGenNames.put("<compiledICHolderKlass>","Inline Cache Metadata");     // NOI18N
        permGenNames.put("<instanceKlassKlass>","Instance Class Metadata");     // NOI18N
        permGenNames.put("<objArrayKlassKlass>","Object Array Class Metadata");     // NOI18N
        permGenNames.put("<typeArrayKlassKlass>","Scalar Array Class Metadata");     // NOI18N
        permGenNames.put("<klassKlass>","Base Class Metadata");     // NOI18N
        permGenNames.put("<arrayKlassKlass>","Base Array Class Metadata");     // NOI18N
    }
    Set<ClassInfo> classes;
    Set<ClassInfo> permGenClasses;
    Date time;
    long totalBytes;
    long totalInstances;
    long totalHeapBytes;
    long totalHeapInstances;
    long totalPermGenBytes;
    long totalPermgenInstances;
    
    HeapHistogramImpl() {
    }
    
    HeapHistogramImpl(InputStream in) {
        Map<String,ClassInfoImpl> classesMap = new HashMap<>(1024);
        Map<String,ClassInfoImpl> permGenMap = new HashMap<>(1024);
        time = new Date();
        Scanner sc = new Scanner(in, "UTF-8");  // NOI18N
        sc.useRadix(10);
        while(!sc.hasNext("-+")) {
            sc.nextLine();
        }
        sc.skip("-+");
        sc.nextLine();

        
        while(sc.hasNext("[0-9]+:")) {  // NOI18N
            ClassInfoImpl newClInfo = new ClassInfoImpl(sc);
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
        classes = new HashSet<>(classesMap.values());
        permGenClasses = new HashSet<>(permGenMap.values());
    }

    void storeClassInfo(final ClassInfoImpl newClInfo, final Map<String, ClassInfoImpl> map) {
        ClassInfoImpl oldClInfo = map.get(newClInfo.getName());
        if (oldClInfo == null) {
            map.put(newClInfo.getName(),newClInfo);
        } else {
            oldClInfo.bytes += newClInfo.getBytes();
            oldClInfo.instances += newClInfo.getInstancesCount();               
        }
    }
    
    public Date getTime() {
        return (Date) time.clone();
    }
    
    public Set<ClassInfo> getHeapHistogram() {
        return classes;
    }
    
    public long getTotalInstances() {
        return totalInstances;
    }
    
    public long getTotalBytes() {
        return totalBytes;
    }

    public long getTotalHeapInstances() {
        return totalHeapInstances;
    }

    public long getTotalHeapBytes() {
        return totalHeapBytes;
    }

    public Set<ClassInfo> getPermGenHistogram() {
        return permGenClasses;
    }

    public long getTotalPerGenInstances() {
        return totalPermgenInstances;
    }

    public long getTotalPermGenHeapBytes() {
        return totalPermGenBytes;
    }
    
    static class ClassInfoImpl extends ClassInfo {
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
            sc.nextLine();  // skip module name on JDK 9
            permGen = jvmName.charAt(0) == '<';     // NOI18N
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
                return getName().equals(((ClassInfoImpl)obj).getName());
            }
            return false;
        }


        private boolean isPermGen() {
            return permGen;
        }
        
        String convertJVMName(String jvmName) {
            String name = null;
            int index = jvmName.lastIndexOf('[');     // NOI18N
            
            if (index != -1) {
                switch(jvmName.charAt(index+1)) {
                    case BOOLEAN_CODE:
                        name=BOOLEAN_TEXT;
                        break;
                    case CHAR_CODE:
                        name=CHAR_TEXT;
                        break;
                    case BYTE_CODE:
                        name=BYTE_TEXT;
                        break;
                    case SHORT_CODE:
                        name=SHORT_TEXT;
                        break;
                    case INT_CODE:
                        name=INT_TEXT;
                        break;
                    case LONG_CODE:
                        name=LONG_TEXT;
                        break;
                    case FLOAT_CODE:
                        name=FLOAT_TEXT;
                        break;
                    case DOUBLE_CODE:
                        name=DOUBLE_TEXT;
                        break;
                    case OBJECT_CODE:
                        name=jvmName.substring(index+2,jvmName.length()-1);
                        break;
                    default:
                        System.err.println("Unknown name "+jvmName);     // NOI18N
                        name = jvmName;
                }
                for (int i=0;i<=index;i++) {
                    name+="[]";
                }
            } else if (isPermGen()) {
                name = permGenNames.get(jvmName);
            }
            if (name == null) {
                name = jvmName;
            }
            return name.intern();
        }
    }
    
}
