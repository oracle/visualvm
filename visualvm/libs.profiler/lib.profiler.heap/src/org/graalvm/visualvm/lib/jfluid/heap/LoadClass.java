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


/**
 *
 * @author Tomas Hurka
 */
class LoadClass extends HprofObject {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final LoadClassSegment loadClassSegment;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LoadClass(LoadClassSegment segment, long offset) {
        super(offset);
        loadClassSegment = segment;
        assert getHprofBuffer().get(offset) == HprofHeap.LOAD_CLASS;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    long getClassObjectID() {
        return getHprofBuffer().getID(fileOffset + loadClassSegment.classIDOffset);
    }

    String getName() {
        return convertToName(getVMName());
    }

    long getNameID() {
        return getHprofBuffer().getID(fileOffset + loadClassSegment.nameStringIDOffset);
    }

    String getVMName() {
        StringSegment stringSegment = loadClassSegment.hprofHeap.getStringSegment();

        return stringSegment.getStringByID(getNameID());
    }

    private HprofByteBuffer getHprofBuffer() {
        return loadClassSegment.hprofHeap.dumpBuffer;
    }

    private static String convertToName(String vmName) {
        String name = vmName.replace('/', '.'); // NOI18N
        int i;

        for (i = 0; i < name.length(); i++) {
            if (name.charAt(i) != '[') { // NOI18N    // arrays
                break;
            }
        }

        if (i != 0) {
            name = name.substring(i);

            char firstChar = name.charAt(0);

            if (firstChar == 'L') { // NOI18N      // object array
                name = name.substring(1, name.length() - 1);
            } else {
                switch (firstChar) {
                    case 'C':
                        name = "char"; // NOI18N
                        break;
                    case 'B':
                        name = "byte"; // NOI18N
                        break;
                    case 'I':
                        name = "int"; // NOI18N
                        break;
                    case 'Z':
                        name = "boolean"; // NOI18N
                        break;
                    case 'F':
                        name = "float"; // NOI18N
                        break;
                    case 'D':
                        name = "double"; // NOI18N
                        break;
                    case 'S':
                        name = "short"; // NOI18N
                        break;
                    case 'J':
                        name = "long"; // NOI18N
                        break;
                }
            }

            for (; i > 0; i--) {
                name = name.concat("[]"); // NOI18N
            }
        }

        return name;
    }
}
