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

package org.netbeans.lib.profiler.classfile;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.lib.profiler.classfile.ClassInfo.StackMapFrame.FrameType;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.instrumentation.JavaClassConstants;


/**
 * A representation of a binary Java class, that is relatively compact - it does not contain method bodies,
 * and contains only a subset of information from the constant pool. Method bodies (or, more precisely, byte
 * arrays representing either full MethodInfos as defined in JVM Specification, or just method bodies), can
 * be obtained individually on demand.
 *
 * This class is abstract, since it contains a single abstract method that actually returns the class file bytes
 * for the whole class. Concrete subclasses of this class may choose to simply store this byte array, or retrieve
 * it e.g. from disk on demand.
 *
 * @author Misha Dmitirev
 * @author Tomas Hurka
 */
public abstract class ClassInfo extends BaseClassInfo implements JavaClassConstants, CommonConstants {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class LineNumberTables {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private char[][] lineNumbers;
        private char[][] startPCs;
        private boolean hasTable;

        //~ Constructors -------------------------------------------------------------------------------------------------------------

        LineNumberTables(ClassInfo ci) {
            byte[] classBuf = null;

            try {
                classBuf = ci.getClassFileBytes();
            } catch (IOException ex1) { // Should not happen - class file already loaded once by this time
            } catch (ClassNotFoundException ex2) {
            } // Ditto

            int nMethods = ci.getMethodNames().length;
            startPCs = new char[nMethods][];
            lineNumbers = new char[nMethods][];

            for (int i = 0; i < nMethods; i++) {
                int ofs = ci.methodInfoOffsets[i] + ci.lineNumberTablesOffsets[i];

                if (ofs == -1) {
                    continue; // Abstract or native method, or no line number tables in this class
                }

                hasTable = true;

                int tableLen = ci.lineNumberTablesLengths[i];
                char[] startPC = startPCs[i] = new char[tableLen];
                char[] lineNumber = lineNumbers[i] = new char[tableLen];

                for (int j = 0; j < tableLen; j++) {
                    startPC[j] = (char) (((classBuf[ofs++] & 255) << 8) + (classBuf[ofs++] & 255));
                    lineNumber[j] = (char) (((classBuf[ofs++] & 255) << 8) + (classBuf[ofs++] & 255));
                }
            }
        }
        
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public char[][] getStartPCs() {
            return startPCs;
        }

        int[] getMinAndMaxLinesForMethod(int methodIdx) {
            int[] lines = new int[2];

            if (startPCs[methodIdx] == null) { // No line number table for this method - return special value
                lines[0] = lines[1] = -1;

                return lines;
            }

            lines[0] = 10000000;
            lines[1] = -10000000;

            char[] lns = lineNumbers[methodIdx];

            for (int i = 0; i < lns.length; i++) {
                if (lns[i] < lines[0]) {
                    lines[0] = lns[i];
                }

                if (lns[i] > lines[1]) {
                    lines[1] = lns[i];
                }
            }

            return lines;
        }

        int bciForLineNo(int methodIdx, int lineNo) {
            char[] spcs = startPCs[methodIdx];

            if (spcs == null) {
                return -1;
            }

            int tableLen = spcs.length;
            char[] lns = lineNumbers[methodIdx];

            int minLine = 100000000;
            int bestLine = 100000000;
            int maxLine = 0;

            int curLine = -1;
            int bestBCI = 100000000;

            for (int i = 0; i < tableLen; i++) {
                curLine = lns[i];

                if (curLine > maxLine) {
                    maxLine = curLine;
                }

                if (curLine < minLine) {
                    minLine = curLine;
                }

                if (curLine == lineNo) { // Perfect match
                    bestBCI = spcs[i];

                    break;
                } else if ((curLine > lineNo) && (curLine <= bestLine)) { // Update bci/line

                    if (spcs[i] < bestBCI) { // ..but check first if it's the smallest bci for this line.
                                             // The whole issue is due to 'while() { }' effectively compiled as 'do { } while()', where for the actual
                                             // line of the 'while' statementwe get two different bci's in the line number table:
                                             // 1. the one for the initial 'goto' that transfers us to the condition check block in the end of the loop body
                                             // 2. the first bci of that condition check block.
                                             // Whether we hit this line as the first or the last line of our code fragment, the smallest bci is a correct answer.
                        bestBCI = spcs[i];
                        bestLine = curLine;
                    }
                }
            }

            // Found a valid matching line if there is a perfect match or at least the specified
            // line is within this method's line number table.
            if ((curLine == lineNo) || ((lineNo >= minLine) && (lineNo <= maxLine))) {
                return bestBCI;
            } else {
                return -1;
            }
        }

        int lineNoForBci(int methodIdx, int bci) {
            char[] spcs = startPCs[methodIdx];

            if (spcs == null) {
                return -1;
            }

            int tableLen = spcs.length;
            char[] lns = lineNumbers[methodIdx];

            int bestLine = -1;

            for (int i = 0; i < tableLen; i++) {
                if (spcs[i] > bci) {
                    break; // reached in last cycle
                }

                bestLine = lns[i];
            }

            return bestLine;
        }

        private boolean hasTable() {
            return hasTable;
        }
    }

    public static class LocalVariableTables {

        public static final int ATTR_SIZE = 10;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private char[][] lengths;
        private char[][] startPCs;
        private boolean hasTable;

        //~ Constructors -------------------------------------------------------------------------------------------------------------

        LocalVariableTables(ClassInfo ci) {
            this(ci, ci.localVariableTablesOffsets, ci.localVariableTablesLengths);
        }
        
        private LocalVariableTables(ClassInfo ci, int[] tablesOffsets, char[] tablesLengths) {
            byte[] classBuf = null;

            try {
                classBuf = ci.getClassFileBytes();
            } catch (IOException ex1) { // Should not happen - class file already loaded once by this time
            } catch (ClassNotFoundException ex2) {
            } // Ditto

            int nMethods = ci.getMethodNames().length;
            startPCs = new char[nMethods][];
            lengths = new char[nMethods][];

            for (int i = 0; i < nMethods; i++) {
                int tableLen = tablesLengths[i];

                if (tableLen == 0) {
                    continue;
                }
                int ofs = ci.methodInfoOffsets[i] + tablesOffsets[i];
                char[] startPC = startPCs[i] = new char[tableLen];
                char[] length = lengths[i] = new char[tableLen];

                for (int j = 0; j < tableLen; j++, ofs+=ATTR_SIZE ) {
                    int offset = ofs;
                    startPC[j] = (char) (((classBuf[offset++] & 255) << 8) + (classBuf[offset++] & 255));
                    length[j] = (char) (((classBuf[offset++] & 255) << 8) + (classBuf[offset++] & 255));
                }
                hasTable = true;
            }
        }
        
        //~ Methods --------------------------------------------------------------------------------------------------------------

        char[][] getStartPCs() {
            return startPCs;
        }

        char[][] getLengts() {
            return lengths;
        }

        public boolean hasTable() {
            return hasTable;
        }
        
        public void updateTable(int injectionPos, int injectedBytesCount, int methodIdx) {
            if (hasTable()) {
                char[] startPC = getStartPCs()[methodIdx];
                char[] lengths = getLengts()[methodIdx];

                if (startPC != null) {
                    for (int i = 0; i < startPC.length; i++) {
                        char currentBCI = startPC[i];
                        if (currentBCI >= injectionPos) {
                            startPC[i] = (char)(currentBCI + injectedBytesCount);
                        } else {
                            char currentLength = lengths[i];
                            if (currentBCI + currentLength > injectionPos) {
                                 lengths[i] = (char)(currentLength + injectedBytesCount);
                            }
                        }
                    }
                }
            }
        }
    
        public void writeTable(final byte[] buffer, int locVarTablePtr, int methodIdx) {
            char[] startPC = getStartPCs()[methodIdx];
            char[] lengths = getLengts()[methodIdx];

            if (startPC != null) {
                for (int i = 0; i < startPC.length; i++, locVarTablePtr+=ATTR_SIZE) {
                    putU2(buffer, locVarTablePtr, startPC[i]);
                    putU2(buffer, locVarTablePtr + 2, lengths[i]);
                }
            }
        }  
    }
        
    public static class LocalVariableTypeTables extends LocalVariableTables {

        LocalVariableTypeTables(ClassInfo ci) {
            super(ci, ci.localVariableTypeTablesOffsets, ci.localVariableTypeTablesLengths);
        }
    }
//    public static Logger LOG = Logger.getLogger(ClassInfo.class.getName());   
    public class StackMapTables {
        private StackMapFrame[][] frames;
        private byte[][] framesBytes;
        private boolean hasTable;
         
        StackMapTables() {
            byte[] classBuf = null;

            try {
                classBuf = ClassInfo.this.getClassFileBytes();
            } catch (IOException ex1) { // Should not happen - class file already loaded once by this time
            } catch (ClassNotFoundException ex2) {
            } // Ditto

            int nMethods = ClassInfo.this.getMethodNames().length;
            frames = new StackMapFrame[nMethods][];
            framesBytes = new byte[nMethods][];
            for (int i = 0; i < nMethods; i++) {
                int tableLen = ClassInfo.this.stackMapTablesLengths[i];

                if (tableLen == 0) {
                    continue;
                }
                int startOfs = ClassInfo.this.methodInfoOffsets[i] + ClassInfo.this.stackMapTablesOffsets[i];
                int ofs = startOfs;
                StackMapFrame[] frms = frames[i] = new StackMapFrame[tableLen];
//                LOG.finer("Class "+ClassInfo.this.name+" method "+ClassInfo.this.getMethodName(i));
                for (int j = 0; j < tableLen; j++) {
                    frms[j] = new StackMapFrame(classBuf,ofs);
                    ofs+=frms[j].getSize();
//                    LOG.finer(frms[j].toString());
                }
                int len = ofs - startOfs;
                framesBytes[i] = new byte[len+tableLen+2*(StackMapFrame.FrameType.FULL_FRAME.size()+2*3)];
                System.arraycopy(classBuf,startOfs,framesBytes[i],0,len);
                hasTable = true;
            }
        }

        public boolean hasTable() {
            return hasTable;
        }

        public void updateTable(int injectionPos, int injectedBytesCount, int methodIdx, boolean changeTypeIsInjectNewInstr, boolean injectionBindsToFollowingInstruction) {
            String method = getMethodName(methodIdx);
            if (hasTable()) {
                StackMapFrame[] frms = frames[methodIdx];
                
                if (frms != null) {
                    int bciIter = -1;
                    boolean  offsetAdjusted = false; // only need to adjust one offset
                    
                    for (StackMapFrame frame : frms) {
                        int offsetDelta = frame.getOffsetDelta();
                        bciIter += offsetDelta;

                        if (!offsetAdjusted) {
                            if (adjustOffset(bciIter, injectionPos, changeTypeIsInjectNewInstr, injectionBindsToFollowingInstruction)) {
                                setOffsetDelta(methodIdx, frame, offsetDelta + injectedBytesCount);
                                offsetAdjusted = true;
                            }
                        }
                        frame.updateUnitilializedList(injectionPos, injectedBytesCount, changeTypeIsInjectNewInstr, injectionBindsToFollowingInstruction);
                    }
                }                
            }
        }
        
        public int getNumberOfFrames(int methodIdx) {
            StackMapFrame[] frms = frames[methodIdx];
            
            if (frms != null) {
                return frms.length;
            }
            return 0;
        }

        public byte[] getAttributeHeader(int methodIdx) {
            byte[] header = new byte[8];
            putU2(header,0,ClassInfo.this.stackMapTableCPindex);
            return header;
        }
        
        public byte[] writeTable(int methodIdx) {
            StackMapFrame[] frms = frames[methodIdx];
            byte frameBytes[] = framesBytes[methodIdx];
            
            if (frms != null) {
                byte[] ret;
                int offset = 0;
                
                for (StackMapFrame frame : frms) {
                    frame.writeFrame(frameBytes, offset);
                    offset+=frame.getSize();
                }
                ret = new byte[offset];
                System.arraycopy(frameBytes,0,ret,0,offset);
                return ret;
            }
            return null;
        }

        void addFullStackMapFrameEntry(int methodIdx, int endPC, int[] locals, int[] stacks) {
            StackMapFrame[] frms = frames[methodIdx];
            StackMapFrame[] newFrms;

            if (frms != null) {
                int bciIter = -1;

                for (StackMapFrame frame : frms) {
                    bciIter += frame.getOffsetDelta();
                }
                newFrms = frames[methodIdx] = new StackMapFrame[frms.length+1];
                System.arraycopy(frms,0,newFrms,0,frms.length);
                newFrms[frms.length] = new FullStackMapFrame(endPC - bciIter, locals, stacks);
            } else {
                newFrms = frames[methodIdx] = new StackMapFrame[1];
                newFrms[0] = new FullStackMapFrame(endPC + 1, locals, stacks);
                framesBytes[methodIdx] = new byte[newFrms[0].getSize()];
                hasTable = true;
            }
        }

        private void setOffsetDelta(int methodIdx, StackMapFrame frame, int newOffsetDelta) {
            FrameType frameType = frame.frameType;
            
            if (frameType.equals(FrameType.SAME) && newOffsetDelta > 63) {
                 extendFrame(methodIdx,frame,2);
                frame.setFrameType(FrameType.SAME_FRAME_EXTENDED);
            }
            if (frameType.equals(FrameType.SAME_LOCALS_1_STACK_ITEM) && newOffsetDelta > 63) {
                extendFrame(methodIdx,frame,2);
                frame.setFrameType(frameType.SAME_LOCALS_1_STACK_ITEM_EXTENDED);
            }
            frame.setOffsetDelta(newOffsetDelta);
        }

        private void extendFrame(int methodIdx, StackMapFrame frame, int addBytes) {
             StackMapFrame[] frms = frames[methodIdx];
             byte[] data = framesBytes[methodIdx];
             int offset = 0;
             
             for (StackMapFrame f : frms) {
                 if (f == frame) {
                     break;
                 }
                 offset += f.getSize();
             }
             System.arraycopy(data,offset,data,offset+addBytes,data.length-offset-addBytes);
        }
    }
    
    static class StackMapFrame {
        enum FrameType {
            SAME(1),
            SAME_LOCALS_1_STACK_ITEM(1),
            SAME_LOCALS_1_STACK_ITEM_EXTENDED(3),
            CHOP(3),
            SAME_FRAME_EXTENDED(3),
            APPEND(3),
            FULL_FRAME(7);
                    
            private int frameSize;

            int size() {return frameSize;}

            FrameType(int size){
               frameSize = size;
            }
        }
        
        FrameType frameType;
        int storedOffsetDelta;
        int size;
        boolean modified;
        boolean frameModified;
        boolean uninitializedListModified;
        List<Integer> uninitializedList;
        
        StackMapFrame(FrameType type, int offset, int s) {
            frameType = type;
            storedOffsetDelta = offset - 1;
            size = type.size()+s;
        }
        
        StackMapFrame(byte[] buffer, int offset) {
            int type;
            
            type = buffer[offset++] & 0xff;
            if (type <= 63) {
                frameType = FrameType.SAME;
                storedOffsetDelta = type;
            } else if (type <= 127) {
                frameType = FrameType.SAME_LOCALS_1_STACK_ITEM;
                storedOffsetDelta = type - 64;
                size = getVerificationTypeInfoSize(buffer[offset]);
                storeUninitializedVariableInfo(buffer,offset,0);
            } else if (type <= 246) {
                throw new IllegalArgumentException("Type: "+type);
            } else if (type == 247) {
                frameType = FrameType.SAME_LOCALS_1_STACK_ITEM_EXTENDED;
                storedOffsetDelta = getU2(buffer,offset);
                offset+=2;
                size = getVerificationTypeInfoSize(buffer[offset]);
                storeUninitializedVariableInfo(buffer,offset,0);
            } else if (type <= 250) {
                frameType = FrameType.CHOP;
                storedOffsetDelta = getU2(buffer,offset);
            } else if (type == 251) {
                frameType = FrameType.SAME_FRAME_EXTENDED;
                storedOffsetDelta = getU2(buffer,offset);
            } else if (type <= 254) {
                frameType = FrameType.APPEND;
                storedOffsetDelta = getU2(buffer,offset);
                offset+=2;
                int locals = type - 251;
                for (int i=0; i<locals; i++) {
                    int typeInfoSize = getVerificationTypeInfoSize(buffer[offset]);
                    size += typeInfoSize;
                    storeUninitializedVariableInfo(buffer,offset,i);
                    offset += typeInfoSize;
                }
            } else if (type == 255) {
                frameType = FrameType.FULL_FRAME;
                storedOffsetDelta = getU2(buffer,offset);
                offset+=2;
                int locals = getU2(buffer,offset);
                offset+=2;
//                LOG.finer("Locals: "+locals);
                for (int i = 0; i<locals; i++) {
                    int typeInfoSize = getVerificationTypeInfoSize(buffer[offset]);
                    size += typeInfoSize;
                    storeUninitializedVariableInfo(buffer,offset,i);
                    offset += typeInfoSize;
                }
                int stacks = getU2(buffer,offset);
                offset+=2;
//                LOG.finer("Stacks: "+stacks);
                for (int i=0; i<stacks; i++) {
                    int typeInfoSize = getVerificationTypeInfoSize(buffer[offset]);
                    size += typeInfoSize;
                    storeUninitializedVariableInfo(buffer,offset,locals+i);
                    offset += typeInfoSize;
                }
            } else {
                throw new IllegalArgumentException("Type: "+type);
            }
            size+=frameType.size();
        }

        int getSize() {
            return size;
        }
        
        void setFrameType(FrameType newFrameType) {
            int frameSizeDiff = newFrameType.size() - frameType.size();
            frameType = newFrameType;
            size+=frameSizeDiff;
            frameModified = true;
        }
        
        private int getVerificationTypeInfoSize(byte type) {
//            LOG.finer("VerificationTypeInfo: "+(type+0));
            switch (type) {
                case 0: // ITEM_Top
                case 1: // ITEM_Integrer
                case 2: // ITEM_Float
                case 3: // ITEM_Double
                case 4: // ITEM_Long
                case 5: // ITEM_Null
                case 6: // ITEM_UnitializedThis
                    return 1;
                case 7: // ITEM_Object
                case 8: // ITEM_Uninitialized
                    return 3;
                default:
                    throw new IllegalArgumentException("Type "+type);
            }
        }

        public String toString() {
            // for debugging
            return "StackMapFrame "+frameType+" offsetDelta "+getOffsetDelta()+" size "+getSize();
        }

        private int getOffsetDelta() {
            return storedOffsetDelta + 1;
        }

        private void setOffsetDelta(int newOffsetDelta) {
            storedOffsetDelta = newOffsetDelta - 1;
            modified = true;
        }
        
        private void updateUnitilializedList(int injectionPos, int injectedBytesCount, boolean changeTypeIsInjectNewInstr, boolean injectionBindsToFollowingInstruction) {
            if (uninitializedList != null) {
                for (int i = 0; i<uninitializedList.size();i++) {
                    Integer off = uninitializedList.get(i);
                    if (off != null) {
                        int uninitializedOffset = off.intValue();
                        if (adjustOffset(uninitializedOffset, injectionPos, changeTypeIsInjectNewInstr, injectionBindsToFollowingInstruction)) {
                            uninitializedList.set(i,Integer.valueOf(uninitializedOffset+injectedBytesCount));
                            uninitializedListModified = true;
                        }
                    }
                }
            }
        }

        private void storeUninitializedVariableInfo(byte[] buffer, int offset, int listIndex) {
            byte type = buffer[offset++]; 
            if (type == 8) { // ITEM_Unitialized
                if (uninitializedList == null) {
                    uninitializedList = new ArrayList();
                }
                while (uninitializedList.size() < listIndex+1) {
                    uninitializedList.add(null);
                }
                uninitializedList.set(listIndex,Integer.valueOf(getU2(buffer,offset)));
//                LOG.finer("ITEM_Unitialized "+Integer.valueOf(getU2(buffer,offset)));
            }
        }

        void writeFrame(byte[] ret, int newFrameOffset) {
            if (modified) {
//                LOG.finer("Updating "+frameType+" new offset "+getOffsetDelta()+" old type "+Integer.valueOf(ret[newFrameOffset]&0xff));
                if (frameModified) {
                    switch (frameType) {
                        case SAME_LOCALS_1_STACK_ITEM_EXTENDED:
                            ret[newFrameOffset] = (byte)247;
                            break;
                        case SAME_FRAME_EXTENDED:
                            ret[newFrameOffset] = (byte)251;
                            break;
                    }
                }
                switch (frameType) {
                    case SAME:
                        ret[newFrameOffset] = (byte)(storedOffsetDelta & 0x3F);
                        break;
                    case SAME_LOCALS_1_STACK_ITEM:
                        ret[newFrameOffset] = (byte)(64 + (storedOffsetDelta & 0x3F));
                        break;
                    case SAME_LOCALS_1_STACK_ITEM_EXTENDED:
                    case CHOP:
                    case SAME_FRAME_EXTENDED:
                    case APPEND:
                    case FULL_FRAME:
                        putU2(ret, newFrameOffset+1, storedOffsetDelta);
                        break;
                }
            }
            if (uninitializedListModified) {
                switch (frameType) {
                    case SAME_LOCALS_1_STACK_ITEM:
                        putU2(ret,newFrameOffset+2,uninitializedList.get(0).intValue());
                        break;
                    case SAME_LOCALS_1_STACK_ITEM_EXTENDED:
                        putU2(ret,newFrameOffset+3,uninitializedList.get(0).intValue());
                        break;
                    case APPEND: {
                        int offset = newFrameOffset+3;
                        for (Integer off : uninitializedList) {
                            byte type = ret[offset];
                            int typeInfoSize = getVerificationTypeInfoSize(type);

                            if (type == 8) { // ITEM_Unitialized
                                putU2(ret,offset+1,off.intValue());
                            }
                            offset += typeInfoSize;
                        }
                        break;
                    }
                    case FULL_FRAME: {
                        int offset = newFrameOffset+3;
                        int locals = getU2(ret,offset);
                        
                        offset+=2;
//                        LOG.finer("Locals: "+locals);
                        for (int i=0; i<locals; i++) {
                            byte type = ret[offset];
                            int typeInfoSize = getVerificationTypeInfoSize(type);
                            
                            if (type == 8) { // ITEM_Unitialized
                                putU2(ret,offset+1,uninitializedList.get(i).intValue());
                            }
                            offset += typeInfoSize;
                        }
                        int stacks = getU2(ret,offset);
                        offset+=2;
//                        LOG.finer("Stacks: "+stacks);
                        for (int i=0; i<stacks; i++) {
                            byte type = ret[offset];
                            int typeInfoSize = getVerificationTypeInfoSize(type);
                            
                            if (type == 8) { // ITEM_Unitialized
                                putU2(ret,offset+1,uninitializedList.get(locals+i).intValue());
                            }
                            offset += typeInfoSize;
                        }
                        break;
                    }
                }
            }
        }
    }

    static class FullStackMapFrame extends StackMapFrame {
        int[] localsCPIdx;
        int[] stacksCPIdx;
        
        FullStackMapFrame(int delta, int[] locals, int stacks[]) {
            super(FrameType.FULL_FRAME, delta, stacks.length*3+((locals.length>0 && locals[0] == 0)?locals.length:3*locals.length));
            localsCPIdx = locals;
            stacksCPIdx = stacks;
        }
        
        void writeFrame(byte[] ret, int offset) {
            ret[offset++] = (byte)255; // FULL_FRAME
            putU2(ret,offset,storedOffsetDelta); // offset_delta
            offset+=2;
            putU2(ret,offset,localsCPIdx.length); // locals
            offset+=2;
            for (int i=0; i<localsCPIdx.length;i++) {
                int cpIndex = localsCPIdx[i];
                
                if (cpIndex == 0) {
                   ret[offset++] = 0;  // ITEM_Top
                } else {
                    ret[offset++] = 7; // ITEM_Object
                    putU2(ret,offset,cpIndex);
                    offset+=2;
                }
            }
            putU2(ret,offset,stacksCPIdx.length); // stacks
            offset+=2;
            for (int i=0; i<stacksCPIdx.length;i++) {
                ret[offset++] = 7; // ITEM_Object
                putU2(ret,offset,stacksCPIdx[i]);  // cpool_index
                offset+=2;
            }            
        }
    }
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    String packageName;
    String superName;
    char[] cpoolRefsToClassIdx; // Cpool indices of ClassEntry entires for referenced classes
    String[] cpoolRefsToClassName; // Names of classes referenced from cpool, including array classes.
    String[][] cpoolRefsToMethodClassNameAndSig;

    // In all signatures we replace the 'L' and ';' symbols that enclose non-primitive type names with '@' and '#' respectively,
    // so that class names inside signatures can be located fast and unambiguously.  
    char[] cpoolRefsToMethodIdx; // Cpool indices of MethodEntry entries for referenced methods
                                 // The following array consists of "referenced method's class name, name, signature" triplets.
                                 // Defining classes names are trimmed of enclosing 'L' and ';' symbols
    int[] exceptionTableStartOffsets; // Relative offsets within a MethodInfo
    String[] interfaces;
    char[] lineNumberTablesLengths;
    int[] lineNumberTablesOffsets; // Relative offsets within a MethodInfo
    int localVaribaleTableCPindex;
    char[] localVariableTablesLengths;
    int[] localVariableTablesOffsets; // Relative offsets within a MethodInfo
    int localVaribaleTypeTableCPindex;
    char[] localVariableTypeTablesLengths;
    int[] localVariableTypeTablesOffsets; // Relative offsets within a MethodInfo
    int stackMapTableCPindex;
    char[] stackMapTablesLengths;
    int[] stackMapTablesOffsets; // Relative offsets within a MethodInfo
    char[] methodAccessFlags;
    char[] methodBytecodesLengths;
    int[] methodBytecodesOffsets; // Relative offsets within a MethodInfo
    int[] methodInfoLengths;
    int[] methodInfoOffsets;
    String[] methodNames;
    String[] methodSignatures;
    String[] nestedClassNames;
    int majorVersion;  // class file major version
    int classIndex; // constant pool entry index representing this class
    char accessFlags; // isInterface flag included
    int attrsStartOfs; // Ditto for class attributes
    int cpoolStartOfs; // Starting offset, in bytes, of the original cpool (cpool length char included)
    int fieldsStartOfs; // Ditto for fields
    int intermediateDataStartOfs; // Ditto for intermediate data (class flags, name, super, etc.)
    int methodsStartOfs; // Ditto for methods
    int origCPoolCount; // The number of entries in the original cpool of this class
    private LineNumberTables lineNumberTables;
    private LocalVariableTables localVariableTables;
    private LocalVariableTypeTables localVariableTypeTables;
    private StackMapTables stackMapTables;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected ClassInfo(String className, int loaderId) {
        super(className, loaderId);
        packageName = getPackageName(name);
    }

    /**
     * This constructor is used for creation of temporary instances of ClassInfo, typically to just find out something about
     * class contained in a given .class file.
     */
    ClassInfo(byte[] buf) throws ClassFormatError {
        super("", 0); // NOI18N

        try {
            new ClassFileParser().parseClassFile(buf, this);
        } catch (ClassFileParser.ClassFileReadException ex) {
            throw new ClassFormatError(ex.getMessage());
        }

        packageName = getPackageName(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getMajorVersion() {
        return majorVersion;
    }
    
    public boolean isAbstract() {
        return Modifier.isAbstract(accessFlags);
    }

    public int getExceptionTableStartOffsetInMethodInfo(int i) {
        return exceptionTableStartOffsets[i];
    }

    public int getExceptionTableCount(int i) {
        int startOfs = getExceptionTableStartOffsetInMethodInfo(i);
        byte[] methodInfo = getMethodInfo(i);
        
        return ((methodInfo[startOfs] & 0xFF) << 8) + (methodInfo[startOfs + 1] & 0xFF);
    }
    
    public int getLocalVariableTableStartOffsetInMethodInfo(int i) {
        return localVariableTablesOffsets[i];
    }

    public int getLocalVariableTypeTableStartOffsetInMethodInfo(int i) {
        return localVariableTypeTablesOffsets[i];
    }

    public int getStackMapTableStartOffsetInMethodInfo(int i) {
        return stackMapTablesOffsets[i];
    }

    public boolean isInterface() {
        return Modifier.isInterface(accessFlags);
    }

    public String[] getInterfaceNames() {
        return interfaces;
    }

    public LineNumberTables getLineNumberTables() {
        initLineNumberTables();

        return lineNumberTables;
    }

    public LocalVariableTables getLocalVariableTables() {
        initLocalVariableTables();
        return localVariableTables;
    }

    public LocalVariableTypeTables getLocalVariableTypeTables() {
        initLocalVariableTypeTables();
        return localVariableTypeTables;
    }

    public StackMapTables getStackMapTables() {
        initStackMapTables();
        return stackMapTables;
    }
    
    public void resetTables() {
        lineNumberTables = null;
        localVariableTables = null;
        localVariableTypeTables = null;
        stackMapTables = null;
    }
    
    public boolean isMethodAbstract(int i) {
        return Modifier.isAbstract(methodAccessFlags[i]);
    }

    public byte[] getMethodBytecode(int i) {
        try {
            byte[] classFile = getClassFileBytes();
            byte[] res = new byte[methodBytecodesLengths[i]];
            System.arraycopy(classFile, methodInfoOffsets[i] + methodBytecodesOffsets[i], res, 0, methodBytecodesLengths[i]);

            return res;
        } catch (IOException ex1) {
            return null; // Should not happen - class file already loaded once by this time
        } catch (ClassNotFoundException ex2) {
            return null;
        } // Ditto
    }

    public int getMethodBytecodeOffsetInMethodInfo(int i) {
        return methodBytecodesOffsets[i];
    }

    public int getMethodBytecodesLength(int i) {
        return methodBytecodesLengths[i];
    }

    public boolean isMethodFinal(int i) {
        return Modifier.isFinal(methodAccessFlags[i]);
    }

    public int getMethodIndex(String name, String sig) {
        if (methodNames == null) {
            return -1;
        }
        for (int i = 0; i < methodNames.length; i++) {
            if ((methodNames[i] == name) && (methodSignatures[i] == sig)) {
                return i;
            }
        }

        return -1;
    }

    public byte[] getMethodInfo(int i) {
        try {
            byte[] classFile = getClassFileBytes();
            byte[] res = new byte[methodInfoLengths[i]];
            System.arraycopy(classFile, methodInfoOffsets[i], res, 0, methodInfoLengths[i]);

            return res;
        } catch (IOException ex1) {
            return null; // Should not happen - class file already loaded once by this time
        } catch (ClassNotFoundException ex2) {
            return null;
        } // Ditto
    }

    public int getMethodInfoLength(int i) {
        return methodInfoLengths[i];
    }

    public String getMethodName(int i) {
        return methodNames[i];
    }

    public String[] getMethodNames() {
        if (methodNames == null) {
            return new String[0];
        }
        return methodNames;
    }

    public boolean isMethodNative(int i) {
        return Modifier.isNative(methodAccessFlags[i]);
    }

    public boolean isMethodPrivate(int i) {
        return Modifier.isPrivate(methodAccessFlags[i]);
    }

    public boolean isMethodProtected(int i) {
        return Modifier.isProtected(methodAccessFlags[i]);
    }

    public boolean isMethodPublic(int i) {
        return Modifier.isPublic(methodAccessFlags[i]);
    }

    public String getMethodSignature(int i) {
        return methodSignatures[i];
    }

    public String[] getMethodSignatures() {
        return methodSignatures;
    }

    public boolean isMethodStatic(int i) {
        return Modifier.isStatic(methodAccessFlags[i]);
    }

    public int[] getMinAndMaxLinesForMethod(int methodIdx) {
        initLineNumberTables();

        return lineNumberTables.getMinAndMaxLinesForMethod(methodIdx);
    }

    public String[] getNestedClassNames() {
        return nestedClassNames;
    }

    public int getOrigAttrsStartOfs() {
        return attrsStartOfs;
    }

    public int getOrigCPoolCount() {
        return origCPoolCount;
    }

    public int getOrigCPoolStartOfs() {
        return cpoolStartOfs;
    }

    public int getOrigFieldsStartOfs() {
        return fieldsStartOfs;
    }

    public int getOrigIntermediateDataStartOfs() {
        return intermediateDataStartOfs;
    }

    public int getOrigMethodsStartOfs() {
        return methodsStartOfs;
    }

    public String getRefClassName(int refClassIdx) {
        for (int i = 0; i < cpoolRefsToClassIdx.length; i++) {
            if (cpoolRefsToClassIdx[i] == refClassIdx) {
                return cpoolRefsToClassName[i];
            }
        }

        return null;
    }

    public int getCPIndexOfClass(String className) {
        int cpIndex = -1;
        for (int i=0; i<cpoolRefsToClassName.length; i++) {
            if (cpoolRefsToClassName[i].equals(className)) {
                cpIndex = cpoolRefsToClassIdx[i];
            }
        }
        return cpIndex;
    }
    
    public String[] getRefMethodsClassNameAndSig(int refMethodIdx) {
        for (int i = 0; i < cpoolRefsToMethodIdx.length; i++) {
            if (cpoolRefsToMethodIdx[i] == refMethodIdx) {
                return cpoolRefsToMethodClassNameAndSig[i];
            }
        }

        return null;
    }

    public String getSuperclassName() {
        return superName;
    }

    public int bciForMethodAndLineNo(int methodIdx, int lineNo) {
        initLineNumberTables();

        return lineNumberTables.bciForLineNo(methodIdx, lineNo);
    }

    /** Check if the given method's opcode at bci is goto. or goto_w. If so, find and return the bci of the previous opcode */
    public int checkIfAtGoTo(int methodIdx, int bci) { // TODO CHECK: unused method

        byte[] codeBytes = getMethodBytecode(methodIdx);
        int codeAtBCI = codeBytes[bci] & 0xFF;

        if ((codeAtBCI != opc_goto) && (codeAtBCI != opc_goto_w)) {
            return bci;
        }

        return findPreviousBCI(codeBytes, bci);
    }

    public boolean containsMethod(String name, String sig) { // TODO CHECK: unused method

        return (getMethodIndex(name, sig) != -1);
    }

    /** For given bytecode offset bci, return the offset of the bytecode before the one at bci */
    public static int findPreviousBCI(byte[] codeBytes, int bci) {
        int prev_offset = 0;

        for (int offset = 0; offset < bci;) {
            prev_offset = offset;

            int opcode = codeBytes[offset] & 0xFF;

            if (opcode == opc_wide) {
                opcode = codeBytes[offset + 1] & 0xFF;

                if (((opcode >= opc_iload) && (opcode <= opc_aload)) || ((opcode >= opc_istore) && (opcode <= opc_astore))
                        || (opcode == opc_ret)) {
                    offset += 4;
                } else if (opcode == opc_iinc) {
                    offset += 6;
                } else {
                    offset++;
                }
            } else {
                switch (opcode) {
                    case opc_tableswitch: {
                        int tbl = (offset + 1 + 3) & (~3); // four byte boundry
                        long default_skip = intAt(codeBytes, tbl, 0);
                        long low = intAt(codeBytes, tbl, 1);
                        long high = intAt(codeBytes, tbl, 2);
                        tbl += (3 << 2); // three int header
                        offset = tbl + (int) ((high - low + 1) << 2);

                        break;
                    }
                    case opc_lookupswitch: {
                        int tbl = (offset + 1 + 3) & (~3); // four byte boundry
                        long default_skip = intAt(codeBytes, tbl, 0);
                        int npairs = (int) intAt(codeBytes, tbl, 1);
                        int nints = npairs * 2;
                        tbl += (2 << 2); // two int header
                        offset = tbl + (nints << 2);

                        break;
                    }
                    default:
                        offset += opc_length[opcode];

                        break;
                }
            }
        }

        return prev_offset;
    }

    public int lineNoForMethodAndBci(int methodIdx, int bci) { // TODO CHECK: unused method
        initLineNumberTables();

        return lineNumberTables.lineNoForBci(methodIdx, bci);
    }

    /**
     * Returns a {method idx, best BCI} pair for the given source line number in this class. If no suitable method is
     * found, returns {-1, -1}. If this class doesn't have any line number tables (because it's abstract or because
     * it was compiled without tables), returns {-2, -2}.
     */
    public int[] methodIdxAndBestBCIForLineNo(int lineNo) {
        initLineNumberTables();

        if (!lineNumberTables.hasTable()) {
            return new int[] { -2, -2 };
        }

        int nMethods = methodNames.length;

        // We need to take into account the fact that for a constructor/class initializer the line numbers may span
        // a much larger range than the constructor body. That's due to instance/static initialization statements
        // that can be scattered about the whole class. If we put the cursor into a method that is between two
        // initializers, we may well get a constructor as the "best match" for the given line. Thus, we first
        // search normal methods, and only if this fails - constructors and class initializer.
        for (int i = 0; i < nMethods; i++) {
            if ((methodNames[i] == "<init>") || (methodNames[i] == "<clinit>")) { // NOI18N
                continue;
            }

            int bestBCI = lineNumberTables.bciForLineNo(i, lineNo);

            if (bestBCI != -1) {
                return new int[] { i, bestBCI };
            }
        }

        // No success with ordinary methods - try constructors now
        for (int i = 0; i < nMethods; i++) {
            if ((methodNames[i] != "<init>") && (methodNames[i] != "<clinit>")) { // NOI18N
                continue;
            }

            int bestBCI = lineNumberTables.bciForLineNo(i, lineNo);

            if (bestBCI != -1) {
                return new int[] { i, bestBCI };
            }
        }

        return new int[] { -1, -1 };
    }

    // WARNING: this call doesn't check if the method in superClass is not private, final, static or constructor. This is done for
    // speedup, since we call it only in the context when it is already known that the above is true.
    public int overridesVirtualMethod(ClassInfo superClass, int superMethodIdx) { // TODO CHECK: unused method

        int idx = getMethodIndex(superClass.methodNames[superMethodIdx], superClass.methodSignatures[superMethodIdx]);

        if (idx == -1) {
            return -1;
        }

        if (superClass.isMethodPublic(superMethodIdx) || superClass.isMethodProtected(superMethodIdx)) {
            return idx;
        } else if (superClass.packageName == this.packageName) {
            return idx;
        } else {
            return -1;
        }
    }

    //-------------------------------------- Protected methods -------------------------------------------

    /** Returns the class file bytes for this class. */
    protected abstract byte[] getClassFileBytes() throws IOException, ClassNotFoundException;

    /**
     * Returns package name for the given class. In case of no package, returns an
     * empty, but non-null string. Returned string is interned.
     */
    protected static String getPackageName(String clazzName) {
        int ldi = clazzName.lastIndexOf('/'); // For convenience, we use system-internal slashes, not dots

        if (ldi == -1) {
            return ""; // NOI18N
        } else {
            return clazzName.substring(0, ldi).intern();
        }
    }

    /** Given the table at the specified index, return the specified entry */
    static long intAt(byte[] codeBytes, int tbl, int entry) { // TODO CHECK: unused method

        int base = tbl + (entry << 2);

        return (codeBytes[base] << 24) | ((codeBytes[base + 1] & 0xFF) << 16) | ((codeBytes[base + 2] & 0xFF) << 8)
               | (codeBytes[base + 3] & 0xFF);
    }

    static void putU2(byte[] buf, int pos, int value) {
        buf[pos] = (byte) ((value >> 8) & 0xFF);
        buf[pos + 1] = (byte) (value & 0xFF);
    }

    static int getU2(byte[] buf, int pos) {
        return ((buf[pos] & 0xFF) << 8) + (buf[pos + 1] & 0xFF);
    }

    private static boolean adjustOffset(int bciIter, int injectionPos, boolean changeTypeIsInjectNewInstr, boolean injectionBindsToFollowingInstruction) {
        boolean adjustOffset = false;
        if (bciIter > injectionPos) {
            adjustOffset = true;
        } else if (changeTypeIsInjectNewInstr) {
            if (injectionPos == 0 && bciIter == 0) {
                adjustOffset = true;
            } else if (!injectionBindsToFollowingInstruction && bciIter >= injectionPos) {
                adjustOffset = true;
            }
        }
        return adjustOffset;
    }    
    //----------------------------------------- Private implementation -----------------------------------

    private synchronized void initLineNumberTables() {
        if (lineNumberTables == null) {
            lineNumberTables = new LineNumberTables(this);
        }
    }

    private synchronized void initLocalVariableTables() {
        if (localVariableTables == null) {
            localVariableTables = new LocalVariableTables(this);
        }
    }

    private synchronized void initLocalVariableTypeTables() {
        if (localVariableTypeTables == null) {
            localVariableTypeTables = new LocalVariableTypeTables(this);
        }
    }

    private synchronized void initStackMapTables() {
        if (stackMapTables == null) {
            stackMapTables = new StackMapTables();
        }
    }
}
