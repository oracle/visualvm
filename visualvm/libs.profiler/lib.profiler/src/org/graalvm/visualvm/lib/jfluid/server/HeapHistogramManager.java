/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.server;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.HeapHistogramResponse;

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
        try {
            if (isJrockitVM) {
                histogram = computeHistogramJRockit(in);
            } else {    // HotSpot
                histogram = computeHistogramImpl(in);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return histogram;
    }

    private HeapHistogramResponse computeHistogramJRockit(InputStream in) throws IOException {
        long totalHeapBytes = 0;
        long totalHeapInstances = 0;
        long totalInstances = 0;
        long totalBytes = 0;
        Map classesMap = new HashMap(1024);
        Date time = new Date();
        Scanner sc = new Scanner(in);
        sc.nextLine();
        sc.skip('-');
        sc.nextLine();

        while (sc.hasDigit()) {
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

    private HeapHistogramResponse computeHistogramImpl(InputStream in) throws IOException {
        long totalBytes = 0;
        long totalInstances = 0;
        long totalHeapBytes = 0;
        long totalHeapInstances = 0;
        long totalPermGenBytes = 0;
        long totalPermgenInstances = 0;
        Map permGenMap = new HashMap(1024);
        Map classesMap = new HashMap(1024);
        Date time = new Date();
        Scanner sc = new Scanner(in);

        while(!sc.hasNext("------------------")) {      // NOI18N
            sc.nextLine();
        }
        sc.skip('-');       // NOI18N
        sc.nextLine();

        while (sc.hasDigit()) {
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
        if ("Total".equals(sc.next())) {   // NOI18N
            totalInstances = sc.nextLong();
            totalBytes = sc.nextLong();
        }
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

        ClassInfoImpl(Scanner sc) throws IOException {
            String jvmName;

            sc.skipWord();
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
                    case ProfilerInterface.BOOLEAN:
                        name = BOOLEAN_TEXT;
                        break;
                    case ProfilerInterface.CHAR:
                        name = CHAR_TEXT;
                        break;
                    case ProfilerInterface.BYTE:
                        name = BYTE_TEXT;
                        break;
                    case ProfilerInterface.SHORT:
                        name = SHORT_TEXT;
                        break;
                    case ProfilerInterface.INT:
                        name = INT_TEXT;
                        break;
                    case ProfilerInterface.LONG:
                        name = LONG_TEXT;
                        break;
                    case ProfilerInterface.FLOAT:
                        name = FLOAT_TEXT;
                        break;
                    case ProfilerInterface.DOUBLE:
                        name = DOUBLE_TEXT;
                        break;
                    case ProfilerInterface.REFERENCE:
                        name = jvmName.substring(index + 2, jvmName.length() - 1);
                        break;
                    default:
                        System.err.println("Uknown name " + jvmName);   // NOI18N
                        name = jvmName;
                }
                for (int i = 0; i <= index; i++) {
                    name = name.concat("[]");           // NOI18N
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

        JRockitClassInfoImpl(Scanner sc) throws IOException {
            String jvmName;

            sc.skipWord();// skip unused 99.9%
            bytes = computeBytes(sc);
            instances = sc.nextLong();
            sc.skipWord(); // diff unused
            jvmName = sc.next();
            name = convertJVMName(jvmName.replace('/', '.'));
        }

        private long computeBytes(Scanner sc) throws IOException {
            long bytes = sc.nextLong();
            char multi = Character.toUpperCase(sc.nextChar());
            if ('K' == multi) {  // NOI18N
                bytes *= 1024;
            } else if ('M'== multi) {   // NOI18N
                bytes *= 1024 * 1024;
            } else if ('G' == multi) {   // NOI18N
                bytes *= 1024 * 1024 * 1024L;
            }
            return bytes;
        }
    }

    private static class Scanner {

        Reader reader;
        StringBuffer token;

        Scanner(InputStream is) throws UnsupportedEncodingException {
            reader = new BufferedReader(new InputStreamReader(new FullInputStream(is), "UTF-8"));   // NOI18N
            token = new StringBuffer(128);
        }

        void nextLine() throws IOException {
            int ch;
            int ch2;
            
            do {
                ch = reader.read();
            } while (!isNewLine(ch));
            reader.mark(1);
            ch2 = reader.read();
            if (isNewLine(ch2)) {
                if (ch2 != ch) {
                    return;
                }
            }
            reader.reset();
            return;
        }

        void skip(char s) throws IOException {
            int ch;

            do {
                reader.mark(1);
                ch = reader.read();
            } while (ch == s);
            reader.reset();
            return;
        }

        long nextLong() throws IOException {
            int ch;
            long number = 0;

            skipWhitespace();
            while (true) {
                reader.mark(1);
                ch = reader.read();
                if (!Character.isDigit(ch)) {
                    reader.reset();
                    return number;
                }
                number *= 10;
                number += ch - '0';     // NOI18N
            }
        }

        String next() throws IOException {
            int ch;

            skipWhitespace();
            token.setLength(0);
            while (true) {
                reader.mark(1);
                ch = reader.read();
                if (Character.isWhitespace(ch)) {
                    reader.reset();
                    return token.toString();
                }
                token.append((char) ch);
            }
        }

        char nextChar() throws IOException {
            return (char) reader.read();
        }

        void skipWhitespace() throws IOException {
            int ch;
            
            do {
                reader.mark(1);
                ch = reader.read();
            } while (Character.isWhitespace(ch));
            reader.reset();
        }

        boolean hasDigit() throws IOException {
            boolean digit;
            int ch;
            
            skipWhitespace();
            reader.mark(1);
            ch = reader.read();
            digit = Character.isDigit(ch);
            reader.reset();
            return digit;
        }

        void skipWord() throws IOException {
            int ch;
            
            skipWhitespace();
            do {
                reader.mark(1);
                ch = reader.read();
            } while (!Character.isWhitespace(ch));
            reader.reset();
        }

        void close() throws IOException {
            reader.close();
        }

        private boolean isNewLine(int ch) {
            if (ch == '\r') {    // NOI18N
                return true;
            }
            if (ch == '\n') {    // NOI18N
                return true;
            }
            return false;
        }

        private boolean hasNext(String string) throws IOException {
            reader.mark(string.length());
            for (int i = 0; i < string.length(); i++) {
                int ch = reader.read();
                if (ch != string.charAt(i)) {
                    reader.reset();
                    return false;
                }
            }
            reader.reset();
            return true;
        }
    }

    private static class FullInputStream extends FilterInputStream {

        byte[] data;

        FullInputStream(InputStream is) {
            super(is);
            data = new byte[256];
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int n = 0;

            while (n < len) {
                int count;
                int remaining = len - n;
                
                if (remaining > data.length) {
                    remaining = data.length;
                }
                count = super.read(data, 0, remaining);
                if (count < 0) {
                    if (n == 0) {   // nothing was read -> EOF
                        return -1;
                    }
                    return n;
                }
                System.arraycopy(data, 0, b, off + n, count);
                n += count;
            }
            return n;
        }
    }
}
