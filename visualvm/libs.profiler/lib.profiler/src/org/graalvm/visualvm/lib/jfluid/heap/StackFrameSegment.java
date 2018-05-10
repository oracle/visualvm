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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
class StackFrameSegment extends TagBounds {

    private static final int FRAME_DIV = 512;
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    HprofHeap hprofHeap;
    final int methodIDOffset;
    final int stackFrameIDOffset;
    final int lengthOffset;
    final int sourceIDOffset;
    final int methodSignatureIDOffset;
    final int timeOffset;
    final int classSerialNumberOffset;
    final int lineNumberOffset;
    private Map idToFrame;
    private Map classCache = Collections.synchronizedMap(new LoadClassCache());

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    StackFrameSegment(HprofHeap heap, long start, long end) {
        super(HprofHeap.STACK_FRAME, start, end);

        int idSize = heap.dumpBuffer.getIDSize();
        hprofHeap = heap;
        timeOffset = 1;
        lengthOffset = timeOffset + 4;
        stackFrameIDOffset = lengthOffset + 4;
        methodIDOffset = stackFrameIDOffset + idSize;
        methodSignatureIDOffset = methodIDOffset + idSize;
        sourceIDOffset = methodSignatureIDOffset + idSize;
        classSerialNumberOffset = sourceIDOffset + idSize;
        lineNumberOffset = classSerialNumberOffset + 4;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    StackFrame getStackFrameByID(long stackFrameID) {
        Long initialOffset;
        long[] offset;
        
        initIdToFrame();
        initialOffset = (Long) idToFrame.get(new Long(stackFrameID/FRAME_DIV));
        if (initialOffset == null) {
            initialOffset = new Long(startOffset);
        }
        offset = new long[] { initialOffset.longValue() };
        while (offset[0] < endOffset) {
            long start = offset[0];
            long frameID = readStackFrameTag(offset);

            if (frameID == stackFrameID) {
                return new StackFrame(this, start);
            }
        }
        return null;
    }

    private HprofByteBuffer getDumpBuffer() {
        return  hprofHeap.dumpBuffer;
    }

    private long readStackFrameTag(long[] offset) {
        long start = offset[0];

        if (hprofHeap.readTag(offset) != HprofHeap.STACK_FRAME) {
            return 0;
        }

        return getDumpBuffer().getID(start + stackFrameIDOffset);
    }
    
    private synchronized void initIdToFrame() {
        if (idToFrame == null) {
            long[] offset = new long[] { startOffset };

            idToFrame = new HashMap();
            while (offset[0] < endOffset) {
                long start = offset[0];
                long frameID = readStackFrameTag(offset);
                Long frameIDMask = new Long(frameID/FRAME_DIV);
                Long minOffset = (Long) idToFrame.get(frameIDMask);
                
                if (minOffset == null || minOffset > start) {
                    idToFrame.put(frameIDMask, new Long(start));
                }
            }
//            System.out.println("idToFrame size:"+idToFrame.size());
        }
    }
    
    String getClassNameBySerialNumber(int classSerialNumber) {
        Integer classSerialNumberObj = Integer.valueOf(classSerialNumber);
        String className = (String) classCache.get(classSerialNumberObj);
        
        if (className == null) {
            LoadClass loadClass = hprofHeap.getLoadClassSegment().getClassBySerialNumber(classSerialNumber);
            
            if (loadClass != null) {
                className = loadClass.getName();
            } else {
                className = "N/A";      // NOI18N
            }
            classCache.put(classSerialNumberObj, className);
        }
        return className;
    }

    private static class LoadClassCache extends LinkedHashMap {
        private static final int SIZE = 1000;
        
        LoadClassCache() {
            super(SIZE,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size() > SIZE) {
                return true;
            }
            return false;
        }
    }

}
