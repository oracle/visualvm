/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.netbeans;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public class PlatformDetailsProvider extends DetailsProvider.Basic {

    private static final String STANDARD_MODULE = "org.netbeans.Module+"; // NOI18N
    private static final String MODULE_DATA = "org.netbeans.ModuleData+"; // NOI18N
    private static final String DEPENDENCY = "org.openide.modules.Dependency"; // NOI18N
    private static final String SPECIFICATION_VERSION = "org.openide.modules.SpecificationVersion"; // NOI18N
    private static final String ABSTRACT_NODE = "org.openide.nodes.AbstractNode+"; // NOI18N
    private static final String MULTI_FILE_ENTRY = "org.openide.loaders.MultiDataObject$Entry+"; // NOI18N
    private static final String DATA_OBJECT = "org.openide.loaders.DataObject+"; // NOI18N
    private static final String JAR_FILESYSTEM = "org.openide.filesystems.JarFileSystem+"; // NOI18N
    private static final String FILE_OBJ = "org.netbeans.modules.masterfs.filebasedfs.fileobjects.FileObj+"; // NOI18N
    private static final String FOLDER_OBJ = "org.netbeans.modules.masterfs.filebasedfs.fileobjects.FolderObj+"; // NOI18N
    private static final String FILE_NAME = "org.netbeans.modules.masterfs.filebasedfs.naming.FileName+"; // NOI18N
    private static final String FOLDER_NAME = "org.netbeans.modules.masterfs.filebasedfs.naming.FolderName+"; // NOI18N
    private static final String ABSTRACT_FOLDER = "org.openide.filesystems.AbstractFolder+"; // NOI18N
    private static final String BFS_BASE = "org.netbeans.core.startup.layers.BinaryFS$BFSBase+"; // NOI18N
    private static final String FIXED_0_7 = "org.openide.util.CharSequences$Fixed_0_7"; // NOI18N
    private static final String FIXED_8_15 = "org.openide.util.CharSequences$Fixed_8_15"; // NOI18N
    private static final String FIXED_16_23 = "org.openide.util.CharSequences$Fixed_16_23"; // NOI18N
    private static final String FIXED_1_10 = "org.openide.util.CharSequences$Fixed6Bit_1_10"; // NOI18N
    private static final String FIXED_11_20 = "org.openide.util.CharSequences$Fixed6Bit_11_20"; // NOI18N
    private static final String FIXED_21_30 = "org.openide.util.CharSequences$Fixed6Bit_21_30"; // NOI18N
    private static final String BYTE_BASED_SEQUENCE = "org.openide.util.CharSequences$ByteBasedSequence"; // NOI18N
    private static final String CHAR_BASED_SEQUENCE = "org.openide.util.CharSequences$CharBasedSequence"; // NOI18N
    private static final String REQUEST_PROCESSOR = "org.openide.util.RequestProcessor";     // NOI18N
    
    LinkedHashMap<Long, String> cache = new LinkedHashMap<Long, String>(10000) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > 10000;
        }
    };
    
    public PlatformDetailsProvider() {
        super(STANDARD_MODULE,MODULE_DATA,DEPENDENCY,SPECIFICATION_VERSION,
              ABSTRACT_NODE,MULTI_FILE_ENTRY,DATA_OBJECT,JAR_FILESYSTEM,
              FILE_OBJ,FOLDER_OBJ, FILE_NAME,FOLDER_NAME,ABSTRACT_FOLDER,
              BFS_BASE,
              FIXED_0_7,FIXED_8_15,FIXED_16_23,FIXED_1_10,FIXED_11_20,
              FIXED_21_30,BYTE_BASED_SEQUENCE,CHAR_BASED_SEQUENCE,
              REQUEST_PROCESSOR);
    }

    @Override
    public String getDetailsString(String className, Instance instance) {
        Long id = getUniqueInstanceId(instance);
        String s = cache.get(id);
        if (s != null) {
            return s;
        }
        s = getDetailsStringImpl(className, instance);
        cache.put(id, s);
        return s;
    }

    private String getDetailsStringImpl(String className, Instance instance) {
        switch (className) {
            case STANDARD_MODULE: {
                String codeName = DetailsUtils.getInstanceFieldString(instance, "codeName");     // NOI18N
                if (codeName != null) {
                    return codeName;
                }
                return DetailsUtils.getInstanceFieldString(instance, "data");     // NOI18N
            }
            case DEPENDENCY: {
                String name = DetailsUtils.getInstanceFieldString(instance, "name");     // NOI18N
                String version = DetailsUtils.getInstanceFieldString(instance, "version");     // NOI18N
                int type = DetailsUtils.getIntFieldValue(instance, "type", -1); // NOI18N
                int comparison = DetailsUtils.getIntFieldValue(instance, "comparison", -1); // NOI18N
                return DependencyResolver.toString(name, version, type, comparison);
            }
            case SPECIFICATION_VERSION: {
                PrimitiveArrayInstance digits = (PrimitiveArrayInstance) instance.getValueOfField("digits"); // NOI18N
                if (digits != null) {
                    StringBuilder specVersion = new StringBuilder();

                    for (Object d : digits.getValues()) {
                        specVersion.append(d);
                        specVersion.append('.');
                    }
                    return specVersion.substring(0, specVersion.length()-1);
                }
                break;
            }
            case MODULE_DATA: {
                String name = DetailsUtils.getInstanceFieldString(instance, "codeName");     // NOI18N
                String version = DetailsUtils.getInstanceFieldString(instance, "specVers");     // NOI18N
                String implVer = DetailsUtils.getInstanceFieldString(instance, "implVersion");       // NOI18N
                return String.format("%s [%s %s]", name, version, implVer);
            }
            case ABSTRACT_NODE: {
                String name = DetailsUtils.getInstanceFieldString(instance, "displayName"); // NOI18N

                if (name == null) {
                    name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                }
                if (name == null || name.isEmpty()) {
                    name = DetailsUtils.getInstanceFieldString(instance, "shortDescription"); // NOI18N
                }
                return name;
            }
            case JAR_FILESYSTEM:
                return DetailsUtils.getInstanceFieldString(instance, "foRoot"); // NOI18N
            case MULTI_FILE_ENTRY:
                return DetailsUtils.getInstanceFieldString(instance, "file"); // NOI18N
            case DATA_OBJECT:
                return DetailsUtils.getInstanceFieldString(instance, "primary"); // NOI18N
            case FILE_OBJ:
                return DetailsUtils.getInstanceFieldString(instance, "fileName"); // NOI18N
            case FOLDER_OBJ:
                return DetailsUtils.getInstanceFieldString(instance, "fileName"); // NOI18N
            case FILE_NAME:
            case FOLDER_NAME:
            case ABSTRACT_FOLDER:
            case BFS_BASE: {
                String nameString = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N

                if (nameString != null) {
                    String parentDetail = DetailsUtils.getInstanceFieldString(instance, "parent"); // NOI18N
                    if (parentDetail != null) {
                        String sep;

                        if (FILE_NAME.equals(className) || FOLDER_NAME.equals(className)) {
                            // FileObject on the disk - find correct file seperator
                            Heap heap = instance.getJavaClass().getHeap();
                            sep = getFileSeparator(heap);
                            if (parentDetail.endsWith(sep)) {
                                // do not duplicate separator
                                sep = "";
                            }
                        } else {
                            sep = "/";
                        }
                        nameString = parentDetail.concat(sep).concat(nameString);
                    }
                }
                return nameString;
            }
            case FIXED_0_7: {
                Integer i1 = (Integer) instance.getValueOfField("i1"); // NOI18N
                Integer i2 = (Integer) instance.getValueOfField("i2"); // NOI18N
                if (i1 != null && i2 != null) {
                    return new Fixed_0_7(i1, i2).toString();
                }
                break;
            }
            case FIXED_8_15: {
                Integer i1 = (Integer) instance.getValueOfField("i1"); // NOI18N
                Integer i2 = (Integer) instance.getValueOfField("i2"); // NOI18N
                Integer i3 = (Integer) instance.getValueOfField("i3"); // NOI18N
                Integer i4 = (Integer) instance.getValueOfField("i4"); // NOI18N
                if (i1 != null && i2 != null && i3 != null && i4 != null) {
                    return new Fixed_8_15(i1, i2, i3, i4).toString();
                }
                break;
            }
            case FIXED_16_23: {
                Long i1 = (Long) instance.getValueOfField("i1"); // NOI18N
                Long i2 = (Long) instance.getValueOfField("i2"); // NOI18N
                Long i3 = (Long) instance.getValueOfField("i3"); // NOI18N
                if (i1 != null && i2 != null && i3 != null) {
                    return new Fixed_16_23(i1, i2, i3).toString();
                }
                break;
            }
            case FIXED_1_10:{
                Long i1 = (Long) instance.getValueOfField("i"); // NOI18N
                if (i1 != null) {
                    return new Fixed6Bit_1_10(i1).toString();
                }
                break;
            }
            case FIXED_11_20:{
                Long i1 = (Long) instance.getValueOfField("i1"); // NOI18N
                Long i2 = (Long) instance.getValueOfField("i2"); // NOI18N
                if (i1 != null && i2 != null) {
                    return new Fixed6Bit_11_20(i1, i2).toString();
                }
                break;
            }
            case FIXED_21_30:{
                Long i1 = (Long) instance.getValueOfField("i1"); // NOI18N
                Long i2 = (Long) instance.getValueOfField("i2"); // NOI18N
                Long i3 = (Long) instance.getValueOfField("i3"); // NOI18N
                if (i1 != null && i2 != null && i3 != null) {
                    return new Fixed6Bit_21_30(i1, i2, i3).toString();
                }
                break;
            }
            case BYTE_BASED_SEQUENCE: {
                Object value = instance.getValueOfField("value");  // NOI18N
                if (value instanceof PrimitiveArrayInstance) {
                    PrimitiveArrayInstance bytesArr = (PrimitiveArrayInstance) value;
                    byte[] bytes = new byte[bytesArr.getLength()];
                    int i = 0;

                    for (String b : bytesArr.getValues()) {
                        bytes[i++] = Byte.valueOf(b);
                    }
                    return new String(bytes);
                }
                break;
            }
            case CHAR_BASED_SEQUENCE:
                return DetailsUtils.getInstanceFieldString(instance, "value");    // NOI18N
            case REQUEST_PROCESSOR:
                return DetailsUtils.getInstanceFieldString(instance, "name");     // NOI18N
            default:
                break;
        }
        return null;
    }
    
    private Long getUniqueInstanceId(Instance instance) {
        Heap heap = instance.getJavaClass().getHeap();
        long id = instance.getInstanceId()^System.identityHashCode(heap);
        
        return new Long(id);
    }

    private String getFileSeparator(Heap heap) {
        Long id = new Long(System.identityHashCode(heap));
        String sep = cache.get(id);
        if (sep == null) {
            sep = heap.getSystemProperties().getProperty("file.separator","/"); // NOI18N
            cache.put(id,sep);
        }
        return sep;
    }

    //<editor-fold defaultstate="collapsed" desc="Private Classes">

    /**
     * compact char sequence implementation for strings in range 0-7 characters
     * 8 + 2*4 = 16 bytes for all strings vs String impl occupying
     */
    private static final class Fixed_0_7 implements CompactCharSequence, Comparable<CharSequence> {

        private final int i1;
        private final int i2;

        private Fixed_0_7(int a1, int a2) {
            i1 = a1;
            i2 = a2;
        }

        @SuppressWarnings("fallthrough")
        private Fixed_0_7(byte[] b, int n) {
            int a1 = n;
            int a2 = 0;
            switch (n) {
                case 7:
                    a2 += (b[6] & 0xFF) << 24;
                case 6:
                    a2 += (b[5] & 0xFF) << 16;
                case 5:
                    a2 += (b[4] & 0xFF) << 8;
                case 4:
                    a2 += b[3] & 0xFF;
                case 3:
                    a1 += (b[2] & 0xFF) << 24;
                case 2:
                    a1 += (b[1] & 0xFF) << 16;
                case 1:
                    a1 += (b[0] & 0xFF) << 8;
                case 0:
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            i1 = a1;
            i2 = a2;
        }

        @Override
        public int length() {
            return i1 & 0xFF;
        }

        @Override
        public char charAt(int index) {
            int r = 0;
            switch (index) {
                case 0:
                    r = (i1 & 0xFF00) >> 8;
                    break;
                case 1:
                    r = (i1 & 0xFF0000) >> 16;
                    break;
                case 2:
                    r = (i1 >> 24) & 0xFF;
                    break;
                case 3:
                    r = i2 & 0xFF;
                    break;
                case 4:
                    r = (i2 & 0xFF00) >> 8;
                    break;
                case 5:
                    r = (i2 & 0xFF0000) >> 16;
                    break;
                case 6:
                    r = (i2 >> 24) & 0xFF;
                    break;
            }
            return (char) r;
        }

        @Override
        public String toString() {
            int n = length();
            char[] r = new char[n];
            for (int i = 0; i < n; i++) {
                r[i] = charAt(i);
            }
            return new String(r);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Fixed_0_7) {
                Fixed_0_7 otherString = (Fixed_0_7) object;
                return i1 == otherString.i1 && i2 == otherString.i2;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (int i = 0; i < length(); i++) {
                hash = 31 * hash + charAt(i);
            }
            return hash;
            //            return (i1 >> 4) + (i1 >> 8) + (i2 << 5) - i2;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return CharSequences.create(toString().substring(start, end));
        }

        @Override
        public int compareTo(CharSequence o) {
            return Comparator.compare(this, o);
        }
    }

    private static final long[] encodeTable = new long[] {
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
           -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1,
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, -1, -1, -1,
           -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
           25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, 63,
           -1, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
           51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1
    };

    private static final char[] decodeTable = new char[] {
          '0','1','2','3','4','5','6','7','8','9',
              'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O',
          'P','Q','R','S','T','U','V','W','X','Y','Z',
              'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o',
          'p','q','r','s','t','u','v','w','x','y','z',
                                                                  '.',     // for 'file.ext' names
                                                                      '_'
    };

    private static boolean is6BitChar(int d) {
        return d < 128 && encodeTable[d] >= 0;
    }

    private static long encode6BitChar(int d) {
        return encodeTable[d];
    }

    private static char decode6BitChar(int d) {
        return decodeTable[d];
    }

    private static final class Fixed6Bit_1_10 implements CompactCharSequence, Comparable<CharSequence> {

        // Length is in lower 4bits
        // then 6bits per symbol
        private final long i;

        private Fixed6Bit_1_10(long a1) {
            i = a1;
        }

        @SuppressWarnings("fallthrough")
        private Fixed6Bit_1_10(byte[] b, int n) {
            long a = n;
            switch (n) {
                case 10:
                    a |= encode6BitChar(b[9]) << 58;
                case 9:
                    a |= encode6BitChar(b[8]) << 52;
                case 8:
                    a |= encode6BitChar(b[7]) << 46;
                case 7:
                    a |= encode6BitChar(b[6]) << 40;
                case 6:
                    a |= encode6BitChar(b[5]) << 34;
                case 5:
                    a |= encode6BitChar(b[4]) << 28;
                case 4:
                    a |= encode6BitChar(b[3]) << 22;
                case 3:
                    a |= encode6BitChar(b[2]) << 16;
                case 2:
                    a |= encode6BitChar(b[1]) << 10;
                case 1:
                    a |= encode6BitChar(b[0]) << 4;
                    break;
                case 0:
                default:
                    throw new IllegalArgumentException();
            }
            i = a;
        }

        @Override
        public int length() {
            return (int) (i & 0x0FL);
        }

        @Override
        public char charAt(int index) {
            int r = 0;
            switch (index) {
                case 0:
                    r = (int) ((i >> 4) & 0x3FL);
                    break;
                case 1:
                    r = (int) ((i >> 10) & 0x3FL);
                    break;
                case 2:
                    r = (int) ((i >> 16) & 0x3FL);
                    break;
                case 3:
                    r = (int) ((i >> 22) & 0x3FL);
                    break;
                case 4:
                    r = (int) ((i >> 28) & 0x3FL);
                    break;
                case 5:
                    r = (int) ((i >> 34) & 0x3FL);
                    break;
                case 6:
                    r = (int) ((i >> 40) & 0x3FL);
                    break;
                case 7:
                    r = (int) ((i >> 46) & 0x3FL);
                    break;
                case 8:
                    r = (int) ((i >> 52) & 0x3FL);
                    break;
                case 9:
                    r = (int) ((i >> 58) & 0x3FL);
                    break;
            }
            return decode6BitChar(r);
        }

        @Override
        public String toString() {
            int n = length();
            char[] r = new char[n];
            for (int j = 0; j < n; j++) {
                r[j] = charAt(j);
            }
            return new String(r);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Fixed6Bit_1_10) {
                Fixed6Bit_1_10 otherString = (Fixed6Bit_1_10) object;
                return i == otherString.i;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (int j = 0; j < length(); j++) {
                hash = 31 * hash + charAt(j);
            }
            return hash;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return CharSequences.create(toString().substring(start, end));
        }

        @Override
        public int compareTo(CharSequence o) {
            return Comparator.compare(this, o);
        }
    }

    private static final class Fixed6Bit_11_20 implements CompactCharSequence, Comparable<CharSequence> {

        // Length is in lower 4bits of i1 and l2
        // then 6 bits per character
        private final long i1;
        private final long i2;

        private Fixed6Bit_11_20(long a1, long a2) {
            i1 = a1;
            i2 = a2;
        }

        @SuppressWarnings("fallthrough")
        private Fixed6Bit_11_20(byte[] b, int n) {
            long a1 = n & 0x0F;
            long a2 = (n >> 4) & 0x0F;
            switch (n) {
                case 20:
                    a2 |= encode6BitChar(b[19]) << 58;
                case 19:
                    a2 |= encode6BitChar(b[18]) << 52;
                case 18:
                    a2 |= encode6BitChar(b[17]) << 46;
                case 17:
                    a2 |= encode6BitChar(b[16]) << 40;
                case 16:
                    a2 |= encode6BitChar(b[15]) << 34;
                case 15:
                    a2 |= encode6BitChar(b[14]) << 28;
                case 14:
                    a2 |= encode6BitChar(b[13]) << 22;
                case 13:
                    a2 |= encode6BitChar(b[12]) << 16;
                case 12:
                    a2 |= encode6BitChar(b[11]) << 10;
                case 11:
                    a2 |= encode6BitChar(b[10]) << 4;
                case 10:
                    a1 |= encode6BitChar(b[9]) << 58;
                case 9:
                    a1 |= encode6BitChar(b[8]) << 52;
                case 8:
                    a1 |= encode6BitChar(b[7]) << 46;
                case 7:
                    a1 |= encode6BitChar(b[6]) << 40;
                case 6:
                    a1 |= encode6BitChar(b[5]) << 34;
                case 5:
                    a1 |= encode6BitChar(b[4]) << 28;
                case 4:
                    a1 |= encode6BitChar(b[3]) << 22;
                case 3:
                    a1 |= encode6BitChar(b[2]) << 16;
                case 2:
                    a1 |= encode6BitChar(b[1]) << 10;
                case 1:
                    a1 |= encode6BitChar(b[0]) << 4;
                    break;
                case 0:
                default:
                    throw new IllegalArgumentException();
            }
            i1 = a1;
            i2 = a2;
        }

        @Override
        public int length() {
            return (int) ((i1 & 0x0FL) + ((i2 & 0x0FL) << 4));
        }

        @Override
        public char charAt(int index) {
            int r = 0;
            switch (index) {
                case 0:
                    r = (int) ((i1 >> 4) & 0x3FL);
                    break;
                case 1:
                    r = (int) ((i1 >> 10) & 0x3FL);
                    break;
                case 2:
                    r = (int) ((i1 >> 16) & 0x3FL);
                    break;
                case 3:
                    r = (int) ((i1 >> 22) & 0x3FL);
                    break;
                case 4:
                    r = (int) ((i1 >> 28) & 0x3FL);
                    break;
                case 5:
                    r = (int) ((i1 >> 34) & 0x3FL);
                    break;
                case 6:
                    r = (int) ((i1 >> 40) & 0x3FL);
                    break;
                case 7:
                    r = (int) ((i1 >> 46) & 0x3FL);
                    break;
                case 8:
                    r = (int) ((i1 >> 52) & 0x3FL);
                    break;
                case 9:
                    r = (int) ((i1 >> 58) & 0x3FL);
                    break;
                case 10:
                    r = (int) ((i2 >> 4) & 0x3FL);
                    break;
                case 11:
                    r = (int) ((i2 >> 10) & 0x3FL);
                    break;
                case 12:
                    r = (int) ((i2 >> 16) & 0x3FL);
                    break;
                case 13:
                    r = (int) ((i2 >> 22) & 0x3FL);
                    break;
                case 14:
                    r = (int) ((i2 >> 28) & 0x3FL);
                    break;
                case 15:
                    r = (int) ((i2 >> 34) & 0x3FL);
                    break;
                case 16:
                    r = (int) ((i2 >> 40) & 0x3FL);
                    break;
                case 17:
                    r = (int) ((i2 >> 46) & 0x3FL);
                    break;
                case 18:
                    r = (int) ((i2 >> 52) & 0x3FL);
                    break;
                case 19:
                    r = (int) ((i2 >> 58) & 0x3FL);
                    break;
            }
            return decode6BitChar(r);
        }

        @Override
        public String toString() {
            int n = length();
            char[] r = new char[n];
            for (int j = 0; j < n; j++) {
                r[j] = charAt(j);
            }
            return new String(r);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Fixed6Bit_11_20) {
                Fixed6Bit_11_20 otherString = (Fixed6Bit_11_20) object;
                return i1 == otherString.i1 && i2 == otherString.i2;
            }
            return false;
        }

        @Override
        public int hashCode() {
            long res = i1 + 31 * i2;
            res = (res + (res >> 32)) & 0xFFFFFFFFL;
            return (int) res;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return CharSequences.create(toString().substring(start, end));
        }

        @Override
        public int compareTo(CharSequence o) {
            return Comparator.compare(this, o);
        }
    }

    private static final class Fixed6Bit_21_30 implements CompactCharSequence, Comparable<CharSequence> {

        // Length is in lower 4bits of i1 and l2
        // then 6 bits per character in i1, i2 and i3
        private final long i1;
        private final long i2;
        private final long i3;

        private Fixed6Bit_21_30(long a1, long a2, long a3) {
            i1 = a1;
            i2 = a2;
            i3 = a3;
        }

        @SuppressWarnings("fallthrough")
        private Fixed6Bit_21_30(byte[] b, int n) {
            long a1 = n & 0x0F;
            long a2 = (n >> 4) & 0x0F;
            long a3 = 0;
            switch (n) {
                case 30:
                    a3 |= encode6BitChar(b[29]) << 58;
                case 29:
                    a3 |= encode6BitChar(b[28]) << 52;
                case 28:
                    a3 |= encode6BitChar(b[27]) << 46;
                case 27:
                    a3 |= encode6BitChar(b[26]) << 40;
                case 26:
                    a3 |= encode6BitChar(b[25]) << 34;
                case 25:
                    a3 |= encode6BitChar(b[24]) << 28;
                case 24:
                    a3 |= encode6BitChar(b[23]) << 22;
                case 23:
                    a3 |= encode6BitChar(b[22]) << 16;
                case 22:
                    a3 |= encode6BitChar(b[21]) << 10;
                case 21:
                    a3 |= encode6BitChar(b[20]) << 4;
                case 20:
                    a2 |= encode6BitChar(b[19]) << 58;
                case 19:
                    a2 |= encode6BitChar(b[18]) << 52;
                case 18:
                    a2 |= encode6BitChar(b[17]) << 46;
                case 17:
                    a2 |= encode6BitChar(b[16]) << 40;
                case 16:
                    a2 |= encode6BitChar(b[15]) << 34;
                case 15:
                    a2 |= encode6BitChar(b[14]) << 28;
                case 14:
                    a2 |= encode6BitChar(b[13]) << 22;
                case 13:
                    a2 |= encode6BitChar(b[12]) << 16;
                case 12:
                    a2 |= encode6BitChar(b[11]) << 10;
                case 11:
                    a2 |= encode6BitChar(b[10]) << 4;
                case 10:
                    a1 |= encode6BitChar(b[9]) << 58;
                case 9:
                    a1 |= encode6BitChar(b[8]) << 52;
                case 8:
                    a1 |= encode6BitChar(b[7]) << 46;
                case 7:
                    a1 |= encode6BitChar(b[6]) << 40;
                case 6:
                    a1 |= encode6BitChar(b[5]) << 34;
                case 5:
                    a1 |= encode6BitChar(b[4]) << 28;
                case 4:
                    a1 |= encode6BitChar(b[3]) << 22;
                case 3:
                    a1 |= encode6BitChar(b[2]) << 16;
                case 2:
                    a1 |= encode6BitChar(b[1]) << 10;
                case 1:
                    a1 |= encode6BitChar(b[0]) << 4;
                    break;
                case 0:
                default:
                    throw new IllegalArgumentException();
            }
            i1 = a1;
            i2 = a2;
            i3 = a3;
        }

        @Override
        public int length() {
            return (int) ((i1 & 0x0FL) + ((i2 & 0x0FL) << 4));
        }

        @Override
        public char charAt(int index) {
            int r = 0;
            switch (index) {
                case 0:
                    r = (int) ((i1 >> 4) & 0x3FL);
                    break;
                case 1:
                    r = (int) ((i1 >> 10) & 0x3FL);
                    break;
                case 2:
                    r = (int) ((i1 >> 16) & 0x3FL);
                    break;
                case 3:
                    r = (int) ((i1 >> 22) & 0x3FL);
                    break;
                case 4:
                    r = (int) ((i1 >> 28) & 0x3FL);
                    break;
                case 5:
                    r = (int) ((i1 >> 34) & 0x3FL);
                    break;
                case 6:
                    r = (int) ((i1 >> 40) & 0x3FL);
                    break;
                case 7:
                    r = (int) ((i1 >> 46) & 0x3FL);
                    break;
                case 8:
                    r = (int) ((i1 >> 52) & 0x3FL);
                    break;
                case 9:
                    r = (int) ((i1 >> 58) & 0x3FL);
                    break;
                case 10:
                    r = (int) ((i2 >> 4) & 0x3FL);
                    break;
                case 11:
                    r = (int) ((i2 >> 10) & 0x3FL);
                    break;
                case 12:
                    r = (int) ((i2 >> 16) & 0x3FL);
                    break;
                case 13:
                    r = (int) ((i2 >> 22) & 0x3FL);
                    break;
                case 14:
                    r = (int) ((i2 >> 28) & 0x3FL);
                    break;
                case 15:
                    r = (int) ((i2 >> 34) & 0x3FL);
                    break;
                case 16:
                    r = (int) ((i2 >> 40) & 0x3FL);
                    break;
                case 17:
                    r = (int) ((i2 >> 46) & 0x3FL);
                    break;
                case 18:
                    r = (int) ((i2 >> 52) & 0x3FL);
                    break;
                case 19:
                    r = (int) ((i2 >> 58) & 0x3FL);
                    break;
                case 20:
                    r = (int) ((i3 >> 4) & 0x3FL);
                    break;
                case 21:
                    r = (int) ((i3 >> 10) & 0x3FL);
                    break;
                case 22:
                    r = (int) ((i3 >> 16) & 0x3FL);
                    break;
                case 23:
                    r = (int) ((i3 >> 22) & 0x3FL);
                    break;
                case 24:
                    r = (int) ((i3 >> 28) & 0x3FL);
                    break;
                case 25:
                    r = (int) ((i3 >> 34) & 0x3FL);
                    break;
                case 26:
                    r = (int) ((i3 >> 40) & 0x3FL);
                    break;
                case 27:
                    r = (int) ((i3 >> 46) & 0x3FL);
                    break;
                case 28:
                    r = (int) ((i3 >> 52) & 0x3FL);
                    break;
                case 29:
                    r = (int) ((i3 >> 58) & 0x3FL);
                    break;
            }
            return decode6BitChar(r);
        }

        @Override
        public String toString() {
            int n = length();
            char[] r = new char[n];
            for (int j = 0; j < n; j++) {
                r[j] = charAt(j);
            }
            return new String(r);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Fixed6Bit_21_30) {
                Fixed6Bit_21_30 otherString = (Fixed6Bit_21_30) object;
                return i1 == otherString.i1 && i2 == otherString.i2 && i3 == otherString.i3;
            }
            return false;
        }

        @Override
        public int hashCode() {
            long res = i1 + 31 * (i2 + i3 * 31);
            res = (res + (res >> 32)) & 0xFFFFFFFFL;
            return (int) res;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return CharSequences.create(toString().substring(start, end));
        }

        @Override
        public int compareTo(CharSequence o) {
            return Comparator.compare(this, o);
        }
    }

    /**
     * compact char sequence implementation for strings in range 8-15 characters
     * size: 8 + 4*4 = 24 bytes for all strings vs String impl occupying
     */
    private static final class Fixed_8_15 implements CompactCharSequence, Comparable<CharSequence> {

        private final int i1;
        private final int i2;
        private final int i3;
        private final int i4;

        private Fixed_8_15(int a1, int a2, int a3, int a4) {
            i1 = a1;
            i2 = a2;
            i3 = a3;
            i4 = a4;
        }

        @SuppressWarnings("fallthrough")
        private Fixed_8_15(byte[] b, int n) {
            int a1 = n;
            int a2 = 0;
            int a3 = 0;
            int a4 = 0;
            switch (n) {
                case 15:
                    a4 += (b[14] & 0xFF) << 24;
                case 14:
                    a4 += (b[13] & 0xFF) << 16;
                case 13:
                    a4 += (b[12] & 0xFF) << 8;
                case 12:
                    a4 += b[11] & 0xFF;
                case 11:
                    a3 += (b[10] & 0xFF) << 24;
                case 10:
                    a3 += (b[9] & 0xFF) << 16;
                case 9:
                    a3 += (b[8] & 0xFF) << 8;
                case 8:
                    a3 += b[7] & 0xFF;
                case 7:
                    a2 += (b[6] & 0xFF) << 24;
                case 6:
                    a2 += (b[5] & 0xFF) << 16;
                case 5:
                    a2 += (b[4] & 0xFF) << 8;
                case 4:
                    a2 += b[3] & 0xFF;
                case 3:
                    a1 += (b[2] & 0xFF) << 24;
                case 2:
                    a1 += (b[1] & 0xFF) << 16;
                case 1:
                    a1 += (b[0] & 0xFF) << 8;
                case 0:
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            i1 = a1;
            i2 = a2;
            i3 = a3;
            i4 = a4;
        }

        @Override
        public int length() {
            return i1 & 0xFF;
        }

        @Override
        public char charAt(int index) {
            int r = 0;
            switch (index) {
                case 0:
                    r = (i1 & 0xFF00) >> 8;
                    break;
                case 1:
                    r = (i1 & 0xFF0000) >> 16;
                    break;
                case 2:
                    r = (i1 >> 24) & 0xFF;
                    break;
                case 3:
                    r = i2 & 0xFF;
                    break;
                case 4:
                    r = (i2 & 0xFF00) >> 8;
                    break;
                case 5:
                    r = (i2 & 0xFF0000) >> 16;
                    break;
                case 6:
                    r = (i2 >> 24) & 0xFF;
                    break;
                case 7:
                    r = i3 & 0xFF;
                    break;
                case 8:
                    r = (i3 & 0xFF00) >> 8;
                    break;
                case 9:
                    r = (i3 & 0xFF0000) >> 16;
                    break;
                case 10:
                    r = (i3 >> 24) & 0xFF;
                    break;
                case 11:
                    r = i4 & 0xFF;
                    break;
                case 12:
                    r = (i4 & 0xFF00) >> 8;
                    break;
                case 13:
                    r = (i4 & 0xFF0000) >> 16;
                    break;
                case 14:
                    r = (i4 >> 24) & 0xFF;
                    break;
            }
            return (char) r;
        }

        @Override
        public String toString() {
            int n = length();
            char[] r = new char[n];
            for (int i = 0; i < n; i++) {
                r[i] = charAt(i);
            }
            return new String(r);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Fixed_8_15) {
                Fixed_8_15 otherString = (Fixed_8_15) object;
                return i1 == otherString.i1 && i2 == otherString.i2 && i3 == otherString.i3 && i4 == otherString.i4;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return i1 + 31 * (i2 + 31 * (i3 + 31 * i4));
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return CharSequences.create(toString().substring(start, end));
        }

        @Override
        public int compareTo(CharSequence o) {
            return Comparator.compare(this, o);
        }
    }

    /**
     * compact char sequence implementation for strings in range 16-23 characters
     * size: 8 + 3*8 = 32 bytes for all strings vs String impl occupying
     */
    private static final class Fixed_16_23 implements CompactCharSequence, Comparable<CharSequence> {

        private final long i1;
        private final long i2;
        private final long i3;

        private Fixed_16_23(long a1, long a2, long a3) {
            i1 = a1;
            i2 = a2;
            i3 = a3;
        }

        @SuppressWarnings("fallthrough")
        private Fixed_16_23(byte[] b, int n) {
            long a1 = 0;
            long a2 = 0;
            long a3 = 0;
            switch (n) {
                case 23:
                    a3 += (b[22] & 0xFFL) << 24;
                case 22:
                    a3 += (b[21] & 0xFF) << 16;
                case 21:
                    a3 += (b[20] & 0xFF) << 8;
                case 20:
                    a3 += (b[19] & 0xFF);
                    a3 <<= 32;
                case 19:
                    a3 += (b[18] & 0xFFL) << 24;
                case 18:
                    a3 += (b[17] & 0xFF) << 16;
                case 17:
                    a3 += (b[16] & 0xFF) << 8;
                case 16:
                    a3 += b[15] & 0xFF;
                case 15:
                    a2 += (b[14] & 0xFFL) << 24;
                case 14:
                    a2 += (b[13] & 0xFF) << 16;
                case 13:
                    a2 += (b[12] & 0xFF) << 8;
                case 12:
                    a2 += (b[11] & 0xFF);
                    a2 <<= 32;
                case 11:
                    a2 += (b[10] & 0xFFL) << 24;
                case 10:
                    a2 += (b[9] & 0xFF) << 16;
                case 9:
                    a2 += (b[8] & 0xFF) << 8;
                case 8:
                    a2 += b[7] & 0xFF;
                case 7:
                    a1 += (b[6] & 0xFFL) << 24;
                case 6:
                    a1 += (b[5] & 0xFF) << 16;
                case 5:
                    a1 += (b[4] & 0xFF) << 8;
                case 4:
                    a1 += (b[3] & 0xFF);
                    a1 <<= 32;
                case 3:
                    a1 += (b[2] & 0xFFL) << 24;
                case 2:
                    a1 += (b[1] & 0xFF) << 16;
                case 1:
                    a1 += (b[0] & 0xFF) << 8;
                case 0:
                    a1 += n;
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            i1 = a1;
            i2 = a2;
            i3 = a3;
        }

        @Override
        public int length() {
            return (int) (i1 & 0xFF);
        }

        @Override
        public char charAt(int index) {
            int r = 0;
            switch (index) {
                case 0:
                    r = (int) ((i1 >> 8) & 0xFFL);
                    break;
                case 1:
                    r = (int) ((i1 >> 16) & 0xFFL);
                    break;
                case 2:
                    r = (int) ((i1 >> 24) & 0xFFL);
                    break;
                case 3:
                    r = (int) ((i1 >> 32) & 0xFFL);
                    break;
                case 4:
                    r = (int) ((i1 >> 40) & 0xFFL);
                    break;
                case 5:
                    r = (int) ((i1 >> 48) & 0xFFL);
                    break;
                case 6:
                    r = (int) ((i1 >> 56) & 0xFFL);
                    break;
                case 7:
                    r = (int) (i2 & 0xFFL);
                    break;
                case 8:
                    r = (int) ((i2 >> 8) & 0xFFL);
                    break;
                case 9:
                    r = (int) ((i2 >> 16) & 0xFFL);
                    break;
                case 10:
                    r = (int) ((i2 >> 24) & 0xFFL);
                    break;
                case 11:
                    r = (int) ((i2 >> 32) & 0xFFL);
                    break;
                case 12:
                    r = (int) ((i2 >> 40) & 0xFFL);
                    break;
                case 13:
                    r = (int) ((i2 >> 48) & 0xFFL);
                    break;
                case 14:
                    r = (int) ((i2 >> 56) & 0xFFL);
                    break;
                case 15:
                    r = (int) (i3 & 0xFFL);
                    break;
                case 16:
                    r = (int) ((i3 >> 8) & 0xFFL);
                    break;
                case 17:
                    r = (int) ((i3 >> 16) & 0xFFL);
                    break;
                case 18:
                    r = (int) ((i3 >> 24) & 0xFFL);
                    break;
                case 19:
                    r = (int) ((i3 >> 32) & 0xFFL);
                    break;
                case 20:
                    r = (int) ((i3 >> 40) & 0xFFL);
                    break;
                case 21:
                    r = (int) ((i3 >> 48) & 0xFFL);
                    break;
                case 22:
                    r = (int) ((i3 >> 56) & 0xFFL);
                    break;
            }
            return (char) r;
        }

        @Override
        public String toString() {
            int n = length();
            char[] r = new char[n];
            for (int i = 0; i < n; i++) {
                r[i] = charAt(i);
            }
            return new String(r);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Fixed_16_23) {
                Fixed_16_23 otherString = (Fixed_16_23) object;
                return i1 == otherString.i1 && i2 == otherString.i2 && i3 == otherString.i3;
            }
            return false;
        }

        @Override
        public int hashCode() {
            long res = i1 + 31 * (i2 + 31 * i3);
            res = (res + (res >> 32)) & 0xFFFFFFFFL;
            return (int) res;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return CharSequences.create(toString().substring(start, end));
        }

        @Override
        public int compareTo(CharSequence o) {
            return Comparator.compare(this, o);
        }
    }

    /**
     * compact char sequence implementation based on char[] array
     * size: 8 + 4 + 4 (= 16 bytes) + sizeof ('value')
     * it is still more effective than String, because string stores length in field
     * and it costs 20 bytes aligned into 24
     */
    private final static class CharBasedSequence implements CompactCharSequence, Comparable<CharSequence> {

        private final char[] value;
        private int hash;

        private CharBasedSequence(char[] v) {
            value = v;
        }

        @Override
        public int length() {
            return value.length;
        }

        @Override
        public char charAt(int index) {
            return value[index];
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof CharBasedSequence) {
                CharBasedSequence otherString = (CharBasedSequence) object;
                if (hash != 0 && otherString.hash != 0) {
                    if (hash != otherString.hash) {
                        return false;
                    }
                }
                return Arrays.equals(value, otherString.value);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                int n = value.length;
                for (int i = 0; i < n; i++) {
                    h = 31 * h + value[i];
                }
                hash = h;
            }
            return h;
        }

        @Override
        public CharSequence subSequence(int beginIndex, int endIndex) {
            return CharSequences.create(value, beginIndex, endIndex-beginIndex);
        }

        @Override
        public String toString() {
            return new String(value);
        }

        @Override
        public int compareTo(CharSequence o) {
            return CharSequenceComparator.compareCharBasedWithOther(this, o);
        }
    }

    /**
     * compact char sequence implementation based on byte[]
     * size: 8 + 4 + 4 (= 16 bytes) + sizeof ('value')
     */
    private final static class ByteBasedSequence implements CompactCharSequence, Comparable<CharSequence> {

        private final byte[] value;
        private int hash;

        private ByteBasedSequence(byte[] b) {
            value = b;
        }

        @Override
        public int length() {
            return value.length;
        }

        @Override
        public char charAt(int index) {
            int r = value[index] & 0xFF;
            return (char) r;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof ByteBasedSequence) {
                ByteBasedSequence otherString = (ByteBasedSequence) object;
                if (hash != 0 && otherString.hash != 0) {
                    if (hash != otherString.hash) {
                        return false;
                    }
                }
                return Arrays.equals(value, otherString.value);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = hash;
            if (h == 0) {
                int n = value.length;
                for (int i = 0; i < n; i++) {
                    h = 31 * h + value[i];
                }
                hash = h;
            }
            return h;
        }

        @Override
        public CharSequence subSequence(int beginIndex, int endIndex) {
            return CharSequences.create(toChars(), beginIndex, endIndex-beginIndex);
        }

        @Override
        public String toString() {
            char[] r = toChars();
            return new String(r);
        }

        private char[] toChars() {
            int n = value.length;
            char[] r = new char[n];
            for (int i = 0; i < n; i++) {
                int c = value[i] & 0xFF;
                r[i] = (char) c;
            }
            return r;
        }

        @Override
        public int compareTo(CharSequence o) {
            return CharSequenceComparator.compareByteBasedWithOther(this, o);
        }
    }

    private static final CompactCharSequence EMPTY = new Fixed_0_7(new byte[0], 0);
    private static final CharSequenceComparator Comparator = new CharSequenceComparator();

    /**
     * performance tuned comparator to prevent charAt calls when possible
     */
    private static class CharSequenceComparator implements Comparator<CharSequence> {

        @Override
        public int compare(CharSequence o1, CharSequence o2) {
            if (o1 instanceof ByteBasedSequence) {
                return compareByteBasedWithOther((ByteBasedSequence)o1, o2);
            } else if (o2 instanceof ByteBasedSequence) {
                return -compareByteBasedWithOther((ByteBasedSequence) o2, o1);
            } else if (o1 instanceof CharBasedSequence) {
                return compareCharBasedWithOther((CharBasedSequence)o1, o2);
            } else if (o2 instanceof CharBasedSequence) {
                return -compareCharBasedWithOther((CharBasedSequence)o2, o1);
            }
            int len1 = o1.length();
            int len2 = o2.length();
            int n = Math.min(len1, len2);
            int k = 0;
            while (k < n) {
                char c1 = o1.charAt(k);
                char c2 = o2.charAt(k);
                if (c1 != c2) {
                    return c1 - c2;
                }
                k++;
            }
            return len1 - len2;
        }

        //<editor-fold defaultstate="collapsed" desc="Private methods">
        private static int compareByteBased(ByteBasedSequence bbs1, ByteBasedSequence bbs2) {
            int len1 = bbs1.value.length;
            int len2 = bbs2.value.length;
            int n = Math.min(len1, len2);
            int k = 0;
            while (k < n) {
                if (bbs1.value[k] != bbs2.value[k]) {
                    return (bbs1.value[k] & 0xFF) - (bbs2.value[k] & 0xFF);
                }
                k++;
            }
            return len1 - len2;
        }

        private static int compareCharBased(CharBasedSequence cbs1, CharBasedSequence cbs2) {
            int len1 = cbs1.value.length;
            int len2 = cbs2.value.length;
            int n = Math.min(len1, len2);
            int k = 0;
            while (k < n) {
                if (cbs1.value[k] != cbs2.value[k]) {
                    return cbs1.value[k] - cbs2.value[k];
                }
                k++;
            }
            return len1 - len2;
        }

        private static int compareByteBasedWithCharBased(ByteBasedSequence bbs1, CharBasedSequence cbs2) {
            int len1 = bbs1.value.length;
            int len2 = cbs2.value.length;
            int n = Math.min(len1, len2);
            int k = 0;
            while (k < n) {
                int c1 = bbs1.value[k] & 0xFF;
                int c2 = cbs2.value[k];
                if (c1 != c2) {
                    return c1 - c2;
                }
                k++;
            }
            return len1 - len2;
        }

        private static int compareByteBasedWithOther(ByteBasedSequence bbs1, CharSequence o2) {
            if (o2 instanceof ByteBasedSequence) {
                return compareByteBased(bbs1, (ByteBasedSequence) o2);
            } else if (o2 instanceof CharBasedSequence) {
                return compareByteBasedWithCharBased(bbs1, (CharBasedSequence) o2);
            }
            int len1 = bbs1.value.length;
            int len2 = o2.length();
            int n = Math.min(len1, len2);
            int k = 0;
            int c1, c2;
            while (k < n) {
                c1 = bbs1.value[k] & 0xFF;
                c2 = o2.charAt(k);
                if (c1 != c2) {
                    return c1 - c2;
                }
                k++;
            }
            return len1 - len2;
        }

        private static int compareCharBasedWithOther(CharBasedSequence cbs1, CharSequence o2) {
            if (o2 instanceof CharBasedSequence) {
                return compareCharBased(cbs1, (CharBasedSequence) o2);
            } else if (o2 instanceof ByteBasedSequence) {
                return -compareByteBasedWithCharBased((ByteBasedSequence) o2, cbs1);
            }
            int len1 = cbs1.value.length;
            int len2 = o2.length();
            int n = Math.min(len1, len2);
            int k = 0;
            int c1, c2;
            while (k < n) {
                c1 = cbs1.value[k];
                c2 = o2.charAt(k);
                if (c1 != c2) {
                    return c1 - c2;
                }
                k++;
            }
            return len1 - len2;
        }
        //</editor-fold>
    }

    private static class CharSequences {

        private static CharSequence create(String substring) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private static CharSequence create(char[] value, int beginIndex, int i) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        CharSequences() {
        }
    }

    /**
     * marker interface for compact char sequence implementations
     */
    private interface CompactCharSequence extends CharSequence {
    }
    
    
    private static class DependencyResolver {
        
        private final static int TYPE_MODULE = 1;
        private final static int TYPE_PACKAGE = 2;
        private final static int TYPE_JAVA = 3;
        private final static int TYPE_IDE = 4;
        private final static int TYPE_REQUIRES = 5;
        private final static int TYPE_NEEDS = 6;
        private final static int TYPE_RECOMMENDS = 7;
        private final static int COMPARE_SPEC = 1;
        private final static int COMPARE_IMPL = 2;
        private final static int COMPARE_ANY = 3;
        
        static String toString(String name, String version, int type, int comparison) {
            StringBuilder buf = new StringBuilder(100);

            if (type == TYPE_MODULE) {
                buf.append("module "); // NOI18N
            } else if (type == TYPE_PACKAGE) {
                buf.append("package "); // NOI18N
            } else if (type == TYPE_REQUIRES) {
                buf.append("requires "); // NOI18N
            } else if (type == TYPE_NEEDS) {
                buf.append("needs "); // NOI18N
            } else if (type == TYPE_RECOMMENDS) {
                buf.append("recommends "); // NOI18N
            }

            buf.append(name);

            if (comparison == COMPARE_IMPL) {
                buf.append(" = "); // NOI18N
                buf.append(version);
            } else if (comparison == COMPARE_SPEC) {
                buf.append(" > "); // NOI18N
                buf.append(version);
            }

            return buf.toString();
        }
        
    }

    //</editor-fold>

}
