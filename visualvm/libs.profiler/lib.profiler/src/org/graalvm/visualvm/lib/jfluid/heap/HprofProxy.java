/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.Properties;


/**
 *
 * @author Tomas Hurka
 */
class HprofProxy {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofProxy() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    static Properties getProperties(Instance propertiesInstance) {
        Instance defaultsObj = (Instance) propertiesInstance.getValueOfField("defaults"); // NOI18N
        ObjectArrayDump entriesObj = (ObjectArrayDump) propertiesInstance.getValueOfField("table"); // NOI18N
        Properties props;

        if (defaultsObj != null) {
            props = new Properties(getProperties(defaultsObj));
        } else {
            props = new Properties();
        }
        if (entriesObj != null) {
            return getPropertiesFromTable(entriesObj, props, "key", "value");   // NOI18N
        } else {    // JDK 9
            Instance map = (Instance) propertiesInstance.getValueOfField("map"); // NOI18N
            if (map != null) {
                entriesObj = (ObjectArrayDump) map.getValueOfField("table"); // NOI18N
                return getPropertiesFromTable(entriesObj, props, "key", "val"); // NOI18N
            } else {    // old Hashtable
                entriesObj = (ObjectArrayDump) propertiesInstance.getValueOfField("elementData"); // NOI18N
                if (entriesObj != null) {
                    return getPropertiesFromTable(entriesObj, props, "key", "value");   // NOI18N
                }
            }
        }
        return null;
    }

    private static Properties getPropertiesFromTable(ObjectArrayDump entriesObj, Properties props, String keyName, String valueName) {
        for (Instance entry : entriesObj.getValues()) {
            for (; entry != null; entry = (Instance) entry.getValueOfField("next")) { // NOI18N
                Instance key = (Instance) entry.getValueOfField(keyName);
                Instance val = (Instance) entry.getValueOfField(valueName);
                if (key != null) {
                    props.setProperty(getString(key), getString(val));
                }
            }
        }
        
        return props;
    }

    static String getString(Instance stringInstance) {
        if (stringInstance == null) {
            return "*null*"; // NOI18N
        }
        String className = stringInstance.getJavaClass().getName();
        if (String.class.getName().equals(className)) {
            Byte coder = (Byte) stringInstance.getValueOfField("coder"); // NOI18N
            PrimitiveArrayDump chars = (PrimitiveArrayDump) stringInstance.getValueOfField("value"); // NOI18N
            if (chars != null) {
                Integer offset = (Integer) stringInstance.getValueOfField("offset"); // NOI18N
                Integer len = (Integer) stringInstance.getValueOfField("count"); // NOI18N
                if (offset == null) {
                    offset = Integer.valueOf(0);
                }
                if (len == null) {
                    len = new Integer(chars.getLength());
                }
                char[] charArr = getChars(chars, coder, offset.intValue(), len.intValue());

                return new String(charArr).intern();
            }
            return "*null*"; // NOI18N
        }
        // what? Non-string in system properties?
        return "*"+className+"#"+stringInstance.getInstanceNumber()+"*";  // NOI18N
    }

    private static char[] getChars(PrimitiveArrayDump chars, Byte coder, int offset, int len) {
        if (coder == null) {
            return chars.getChars(offset, len);
        }
        int cdr = coder.intValue();
        switch (cdr) {
            case 0: {
                char[] charArr = new char[len];
                byte[] bytes = chars.getBytes(offset, len);
                for (int i=0; i<bytes.length; i++) {
                    charArr[i] = (char)(bytes[i] & 0xff);
                }
                return charArr;
            }
            case 1: {
                final int HI_BYTE_SHIFT;
                final int LO_BYTE_SHIFT;
                int shifts[] = getStringUTF16ShiftBytes(chars.dumpClass.getHprof());
                char[] charArr = new char[len/2];
                byte[] bytes = chars.getBytes(offset, len);

                HI_BYTE_SHIFT = shifts[0];
                LO_BYTE_SHIFT = shifts[1];
                for (int i=0; i<bytes.length; i+=2) {
                    charArr[i/2] = (char) (((bytes[i] & 0xff) << HI_BYTE_SHIFT) |
                      ((bytes[i+1] & 0xff) << LO_BYTE_SHIFT));
                }
                return charArr;
            }
            default:
                return "*unknown coder*".toCharArray();
        }
    }
    
    private static int[] getStringUTF16ShiftBytes(Heap heap) {
        JavaClass utf16Class = heap.getJavaClassByName("java.lang.StringUTF16");                  // NOI18N
        Integer HI_BYTE_SHIFT = (Integer) utf16Class.getValueOfStaticField("HI_BYTE_SHIFT");      // NOI18N
        Integer LO_BYTE_SHIFT = (Integer) utf16Class.getValueOfStaticField("LO_BYTE_SHIFT");      // NOI18N
        
        return new int[] {HI_BYTE_SHIFT.intValue(),LO_BYTE_SHIFT.intValue()};
    }
}
