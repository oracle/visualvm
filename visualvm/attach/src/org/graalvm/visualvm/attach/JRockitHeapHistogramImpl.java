/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.attach.HeapHistogramImpl.ClassInfoImpl;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

/**
 *
 * @author Tomas Hurka
 */
class JRockitHeapHistogramImpl extends HeapHistogramImpl {
    
    JRockitHeapHistogramImpl(InputStream in) {
        Map<String,ClassInfoImpl> classesMap = new HashMap(1024);
        time = new Date();
        Scanner sc = new Scanner(in, "UTF-8");  // NOI18N
        sc.useRadix(10);
        sc.nextLine();
        sc.skip("-+");
        sc.nextLine();

        while(sc.hasNext("[0-9]+\\.[0-9]%")) {  // NOI18N
            JRockitClassInfoImpl newClInfo = new JRockitClassInfoImpl(sc);
            storeClassInfo(newClInfo, classesMap);
            totalHeapBytes += newClInfo.getBytes();
            totalHeapInstances += newClInfo.getInstancesCount();                
        }
        totalInstances = totalHeapInstances;
        totalBytes = totalHeapBytes;
        classes = new HashSet(classesMap.values());
        permGenClasses = Collections.EMPTY_SET;
    }
        
    static class JRockitClassInfoImpl extends ClassInfoImpl {
        
        JRockitClassInfoImpl(Scanner sc) {
            String jvmName;
            
            sc.next();  // skip unused 99.9%
            bytes = computeBytes(sc.next());
            instances = sc.nextLong();
            sc.next(); // diff unused
            jvmName = sc.next();
            name = convertJVMName(jvmName.replace('/','.'));     // NOI18N
        }

        private long computeBytes(String size) {
            String multi = size.substring(size.length()-1);
            long bytes = Long.parseLong(size.substring(0,size.length()-1));
            if ("K".equalsIgnoreCase(multi)) {  // NOI18N
                bytes*=1024;
            } else if ("M".equalsIgnoreCase(multi)) {   // NOI18N
                bytes*=1024*1024;
            } else if ("G".equalsIgnoreCase(multi)) {   // NOI18N
                bytes*=1024*1024*1024L;
            }
            return bytes;
        }

    }
}
