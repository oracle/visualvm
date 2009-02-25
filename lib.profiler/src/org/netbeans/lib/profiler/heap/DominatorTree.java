/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.heap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Tomas Hurka
 */
class DominatorTree {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    
    private static final int BUFFER_SIZE = (64 * 1024) / 8;
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private HprofHeap heap;
    private LongBuffer multipleParents;
    private LongBuffer revertedMultipleParents;
    private LongBuffer currentMultipleParents;
    private DomMap map;
boolean print;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    DominatorTree(HprofHeap h, LongBuffer multiParents) {
        heap = h;
        multipleParents = multiParents;
        currentMultipleParents = multipleParents;
        try {
            map = new DomMap(multiParents.getSize(),heap.idToOffsetMap.ID_SIZE,4);
            revertedMultipleParents = multiParents.revertBuffer();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex.getLocalizedMessage(),ex);
        }
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------    
    
    synchronized void computeDominators() {
        boolean changed;
        try {
            do {
                currentMultipleParents.rewind();
                changed = computeOneLevel();
                switchParents();
            } while (changed);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        deleteBuffers();
    }
    
    private boolean computeOneLevel() throws IOException {
        boolean changed = false;
 //long changedL = 0;
 //long processed = 0;
 //long index = 0;
        for (;;) {
            long instanceId = readLong();
//index++;
            if (instanceId == 0) {  // end of level
                break;
            }
            DomMap.Entry idomEntry = map.get(instanceId);
            
            if (idomEntry == null || idomEntry.getIdom() != 0) {
//processed++;
                LongMap.Entry entry = heap.idToOffsetMap.get(instanceId);
//Instance iiii = heap.getInstanceByID(instanceId);
//if (iiii.getJavaClass().getName().equals("java.util.LinkedHashMap$Entry") && iiii.getInstanceNumber() == 666) {
//    System.out.println(iiii.getJavaClass().getName()+"#"+iiii.getInstanceNumber());
//}
                List refs = entry.getReferences();
                Iterator refIt = refs.iterator();
                long idomId = ((Long)refIt.next()).longValue();

                while(refIt.hasNext() && idomId != 0) {
                    long refId = ((Long)refIt.next()).longValue();
                    idomId = intersect(idomId,refId);
                }
                if (idomEntry == null) {
                    map.put(instanceId,idomId);
//changedL++;
                    changed = true;
                } else if (idomEntry.getIdom() != idomId) {
//                    if (print) {
//                        Instance iii = heap.getInstanceByID(instanceId);
//                        Instance idomII = heap.getInstanceByID(idomId);
//                        Instance oldIdom = heap.getInstanceByID(idomEntry.getIdom());
//                        System.out.println("Index:"+index);
//                        System.out.println("ID:   "+Long.toHexString(instanceId)+" "+iii.getJavaClass().getName()+"#"+iii.getInstanceNumber());
//                        System.out.println("Idom: "+Long.toHexString(idomId)+" "+idomII.getJavaClass().getName()+"#"+idomII.getInstanceNumber());
//                        System.out.println("OldIdm"+Long.toHexString(idomEntry.getIdom())+" "+oldIdom.getJavaClass().getName()+"#"+oldIdom.getInstanceNumber());
//                        System.out.println("---------------------------");
//                    }
                    idomEntry.setIdom(idomId);
//changedL++;
                    changed = true;
                }
            }
        }
//System.out.println("Changed:    "+changedL);
//System.out.println("Processed:  "+processed);
//print = changedL < 5;
 
        return changed;
    }
        
    private void deleteBuffers() {
        multipleParents.delete();
        revertedMultipleParents.delete();
    }
        
    private long readLong() throws IOException {
        return currentMultipleParents.readLong();
    }
    
    long getIdomId(long instanceId, LongMap.Entry entry) {
        DomMap.Entry domEntry = map.get(instanceId);
        if (domEntry != null) {
            return domEntry.getIdom();
        }
        if (entry == null) {
            entry = heap.idToOffsetMap.get(instanceId);
        }
        return entry.getNearestGCRootPointer();
    }

    private long getIdomId(long instanceId) {
        return getIdomId(instanceId,null);
    }
    
    private long intersect(long idomId, long refId) {
        if (idomId == refId) {
            return idomId;
        }
        if (idomId == 0 || refId == 0) {
            return 0;
        }
        Set leftIdoms = new HashSet(200);
        Set rightIdoms = new HashSet(200);
        long leftIdom = idomId;
        long rightIdom = refId;
        Long leftIdomObj = new Long(leftIdom);
        Long rightIdomObj = new Long(rightIdom);
        
        leftIdoms.add(leftIdomObj);
        rightIdoms.add(rightIdomObj);
        while(true) {
            if (leftIdom != 0) {
                leftIdom = getIdomId(leftIdom);
                leftIdomObj = new Long(leftIdom);
                if (rightIdoms.contains(leftIdomObj)) {
                    return leftIdom;
                }
                leftIdoms.add(leftIdomObj);
            }
            if (rightIdom != 0) {
                rightIdom = getIdomId(rightIdom);
                rightIdomObj = new Long(rightIdom);
                if (leftIdoms.contains(rightIdomObj)) {
                    return rightIdom;
                }
                rightIdoms.add(rightIdomObj);
            }
        }
    }

    private void switchParents() {
        if (currentMultipleParents == revertedMultipleParents) {
            currentMultipleParents = multipleParents;
        } else {
            currentMultipleParents = revertedMultipleParents;
        }
    }
}
