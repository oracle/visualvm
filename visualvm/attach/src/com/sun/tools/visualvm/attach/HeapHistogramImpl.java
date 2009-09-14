/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.attach;

import com.sun.tools.visualvm.application.jvm.HeapHistogram;
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
public class HeapHistogramImpl extends HeapHistogram {
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
    private static final Map<String,String> permGenNames = new HashMap();
    static {
        permGenNames.put("<methodKlass>","Read-Write Method Metadata");
        permGenNames.put("<constMethodKlass>","Read-Only Method Metadata");
        permGenNames.put("<methodDataKlass>","Method Profiling Information");
        permGenNames.put("<constantPoolKlass>","Constant Pool Metadata");
        permGenNames.put("<constantPoolCacheKlass>","Class Resolution Optimization Metadata");
        permGenNames.put("<symbolKlass>","VM Symbol Metadata");
        permGenNames.put("<compiledICHolderKlass>","Inline Cache Metadata");
        permGenNames.put("<instanceKlassKlass>","Instance Class Metadata");
        permGenNames.put("<objArrayKlassKlass>","Object Array Class Metadata");
        permGenNames.put("<typeArrayKlassKlass>","Scalar Array Class Metadata");
        permGenNames.put("<klassKlass>","Base Class Metadata");
        permGenNames.put("<arrayKlassKlass>","Base Array Class Metadata");
    }
    private final Set<ClassInfo> classes;
    private final Set<ClassInfo> permGenClasses;
    private final Date time;
    private final long totalBytes;
    private final long totalInstances;
    private long totalHeapBytes;
    private long totalHeapInstances;
    private long totalPermGenBytes;
    private long totalPermgenInstances;
    
    
    HeapHistogramImpl(InputStream in) {
        Map<String,ClassInfoImpl> classesMap = new HashMap(1024);
        Map<String,ClassInfoImpl> permGenMap = new HashMap(1024);
        time = new Date();
        Scanner sc = new Scanner(in, "UTF-8");  // NOI18N
        sc.useRadix(10);
        sc.nextLine();
        sc.nextLine();
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
        classes = new HashSet(classesMap.values());
        permGenClasses = new HashSet(permGenMap.values());
    }

    private void storeClassInfo(final ClassInfoImpl newClInfo, final Map<String, ClassInfoImpl> map) {
        ClassInfoImpl oldClInfo = map.get(newClInfo.getName());
        if (oldClInfo == null) {
            map.put(newClInfo.getName(),newClInfo);
        } else {
            oldClInfo.bytes += newClInfo.getBytes();
            oldClInfo.instances += newClInfo.getInstancesCount();               
        }
    }
    
    public Date getTime() {
        return time;
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
    
    public static class ClassInfoImpl {
        private long instances;
        private long bytes;
        private String name;
        private boolean permGen;


        public ClassInfoImpl() {
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
                return getName().equals(((ClassInfoImpl)obj).getName());
            }
            return false;
        }


        private boolean isPermGen() {
            return permGen;
        }
        
        private String convertJVMName(String jvmName) {
            String name = null;
            int index = jvmName.lastIndexOf('[');
            
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
                        System.err.println("Uknown name "+jvmName);
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
