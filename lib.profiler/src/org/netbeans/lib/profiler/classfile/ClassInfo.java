/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.classfile;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.instrumentation.JavaClassConstants;
import java.io.IOException;
import java.lang.reflect.Modifier;


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
 */
public abstract class ClassInfo extends BaseClassInfo implements JavaClassConstants, CommonConstants {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class LineNumberTables {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        char[][] lineNumbers;
        char[][] startPCs;

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
    char[] methodAccessFlags;
    char[] methodBytecodesLengths;
    int[] methodBytecodesOffsets; // Relative offsets within a MethodInfo
    int[] methodInfoLengths;
    int[] methodInfoOffsets;
    String[] methodNames;
    String[] methodSignatures;
    String[] nestedClassNames;
    char accessFlags; // isInterface flag included
    int attrsStartOfs; // Ditto for class attributes
    int cpoolStartOfs; // Starting offset, in bytes, of the original cpool (cpool length char included)
    int fieldsStartOfs; // Ditto for fields
    int intermediateDataStartOfs; // Ditto for intermediate data (class flags, name, super, etc.)
    int methodsStartOfs; // Ditto for methods
    int origCPoolCount; // The number of entries in the original cpool of this class
    short lineNumberTablesInitStatus; // 0 : not yet initialized, 1 : OK, -1 : no tables exist
    private LineNumberTables lineNumberTables;

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
            (new ClassFileParser()).parseClassFile(buf, this);
        } catch (ClassFileParser.ClassFileReadException ex) {
            throw new ClassFormatError(ex.getMessage());
        }

        packageName = getPackageName(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isAbstract() {
        return Modifier.isAbstract(accessFlags);
    }

    public int getExceptionTableStartOffsetInMethodInfo(int i) {
        return exceptionTableStartOffsets[i];
    }

    public boolean isInterface() {
        return Modifier.isInterface(accessFlags);
    }

    public String[] getInterfaceNames() {
        return interfaces;
    }

    public LineNumberTables getLineNumberTables() {
        if (lineNumberTables == null) {
            initLineNumberTables();
        }

        return lineNumberTables;
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
        if (lineNumberTables == null) {
            initLineNumberTables();
        }

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
        if (lineNumberTables == null) {
            initLineNumberTables();
        }

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

        if (lineNumberTables == null) {
            initLineNumberTables();
        }

        return lineNumberTables.lineNoForBci(methodIdx, bci);
    }

    /**
     * Returns a {method idx, best BCI} pair for the given source line number in this class. If no suitable method is
     * found, returns {-1, -1}. If this class doesn't have any line number tables (because it's abstract or because
     * it was compiled without tables), returns {-2, -2}.
     */
    public int[] methodIdxAndBestBCIForLineNo(int lineNo) {
        if (lineNumberTablesInitStatus == 0) {
            initLineNumberTables();
        }

        if (lineNumberTablesInitStatus == -1) {
            return new int[] { -2, -2 };
        }

        int nMethods = methodNames.length;

        // We need to take into account the fact that for a constructor/class initializer the line numbers may span
        // a much larger range than the constructor body. That's due to instance/static initialization statements
        // that can be scattered about the whole class. If we put the cursor into a method that is between two
        // initializers, we may well get a constructor as the "best match" for the given line. Thus, we first
        // search normal methods, and only if this fails - constructors and class initializer.
        for (int i = 0; i < nMethods; i++) {
            if ((methodNames[i] == "<init>") || (methodNames[i] == "<clinit>")) {
                continue; // NOI18N
            }

            int bestBCI = lineNumberTables.bciForLineNo(i, lineNo);

            if (bestBCI != -1) {
                return new int[] { i, bestBCI };
            }
        }

        // No success with ordinary methods - try constructors now
        for (int i = 0; i < nMethods; i++) {
            if ((methodNames[i] != "<init>") && (methodNames[i] != "<clinit>")) {
                continue; // NOI18N
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

    //----------------------------------------- Private implementation -----------------------------------

    /** Given the table at the specified index, return the specified entry */
    private static final long intAt(byte[] codeBytes, int tbl, int entry) { // TODO CHECK: unused method

        int base = tbl + (entry << 2);

        return (codeBytes[base] << 24) | ((codeBytes[base + 1] & 0xFF) << 16) | ((codeBytes[base + 2] & 0xFF) << 8)
               | (codeBytes[base + 3] & 0xFF);
    }

    private void initLineNumberTables() {
        byte[] classBuf = null;

        try {
            classBuf = getClassFileBytes();
        } catch (IOException ex1) { // Should not happen - class file already loaded once by this time
        } catch (ClassNotFoundException ex2) {
        } // Ditto

        int nMethods = methodNames.length;
        char[][] startPCs = new char[nMethods][];
        char[][] lineNumbers = new char[nMethods][];
        boolean lineNumberTablesExist = false;

        for (int i = 0; i < nMethods; i++) {
            int ofs = methodInfoOffsets[i] + lineNumberTablesOffsets[i];

            if (ofs == -1) {
                continue; // Abstract or native method, or no line number tables in this class
            }

            lineNumberTablesExist = true;

            int tableLen = lineNumberTablesLengths[i];
            char[] startPC = startPCs[i] = new char[tableLen];
            char[] lineNumber = lineNumbers[i] = new char[tableLen];

            for (int j = 0; j < tableLen; j++) {
                startPC[j] = (char) (((classBuf[ofs++] & 255) << 8) + (classBuf[ofs++] & 255));
                lineNumber[j] = (char) (((classBuf[ofs++] & 255) << 8) + (classBuf[ofs++] & 255));
            }
        }

        // Let's init this data anyway, to avoid NullPointerExceptions etc.
        lineNumberTables = new LineNumberTables();
        lineNumberTables.startPCs = startPCs;
        lineNumberTables.lineNumbers = lineNumbers;

        if (lineNumberTablesExist) {
            lineNumberTablesInitStatus = 1;
        } else {
            lineNumberTablesInitStatus = -1;
        }
    }
}
