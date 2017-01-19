/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.details.basic;

import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.JavaClass;

/**
 *
 * @author Tomas Hurka
 */
public class StringDecoder {
    
    private final byte coder;
    private final List<String> values;
    private int HI_BYTE_SHIFT;
    private int LO_BYTE_SHIFT;

    public StringDecoder(Heap heap, byte c, List<String> val) {
        coder = c;
        values = val;
        if (coder == 1) {
            JavaClass utf16Class = heap.getJavaClassByName("java.lang.StringUTF16"); // NOI18N
            HI_BYTE_SHIFT = (Integer) utf16Class.getValueOfStaticField("HI_BYTE_SHIFT"); // NOI18N
            LO_BYTE_SHIFT = (Integer) utf16Class.getValueOfStaticField("LO_BYTE_SHIFT"); // NOI18N
        }
    }

    public int getStringLength() {
        int size = values.size();
        switch (coder) {
            case -1:
                return size;
            case 0:
                return size;
            case 1:
                return size / 2;
            default:
                return size;
        }
    }

    public String getValueAt(int index) {
        switch (coder) {
            case -1:
                return values.get(index);
            case 0: {
                char ch = (char) (Byte.valueOf(values.get(index)) & 0xff);
                return String.valueOf(ch);
            }
            case 1: {
                index *= 2;
                byte hiByte = Byte.valueOf(values.get(index));
                byte lowByte = Byte.valueOf(values.get(index + 1));
                char ch = (char) (((hiByte & 0xff) << HI_BYTE_SHIFT) |
                                 ((lowByte & 0xff) << LO_BYTE_SHIFT));
                return String.valueOf(ch);
            }
            default:
                return "?"; // NOI18N
        }
    }  
}
