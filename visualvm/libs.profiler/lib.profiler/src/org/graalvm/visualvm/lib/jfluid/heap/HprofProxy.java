/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.Iterator;
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
        Iterator enIt = entriesObj.getValues().iterator();
        while (enIt.hasNext()) {
            Instance entry = (Instance) enIt.next();
            
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
