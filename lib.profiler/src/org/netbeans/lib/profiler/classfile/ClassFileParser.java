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

import org.netbeans.lib.profiler.instrumentation.JavaClassConstants;
import org.netbeans.lib.profiler.utils.StringUtils;


/**
 * This class implements parsing a byte array representing a class file, generating a ClassInfo object.
 *
 * @author Misha Dmitirev
 */
public class ClassFileParser implements JavaClassConstants {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class ClassFileReadException extends Exception {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        ClassFileReadRuntimeException e;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private ClassFileReadException(ClassFileReadRuntimeException e) {
            this.e = e;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Throwable getCause() {
            return e;
        }

        public String getMessage() {
            return e.getMessage();
        }

        public String toString() {
            return e.toString();
        }
    }

    private static class ClassFileReadRuntimeException extends RuntimeException {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ClassFileReadRuntimeException(String msg) {
            super(msg);
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ClassInfo classInfo;
    private byte[] classBuf;
    private Object[] cpObjectCache;
    private int[] cpOffsets;
    private byte[] cpTags;
    private int curBufPos;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void parseClassFile(byte[] classFile, ClassInfo classInfo)
                        throws ClassFileReadException {
        classBuf = classFile;
        this.classInfo = classInfo;
        curBufPos = 0;

        try {
            readPreamble();
            readConstantPool();
            readIntermediate();
            skipFields();
            readMethods();
            readAttributes();
        } catch (ClassFileReadRuntimeException e) {
            throw new ClassFileReadException(e);
        }
    }

    private char getChar(int bufPos) {
        return (char) (((classBuf[bufPos++] & 255) << 8) + (classBuf[bufPos++] & 255));
    }

    private void badCPEntry(int entryNo) { // TODO CHECK: unused method
        throw classFileReadException("Constant pool entry " + entryNo + " : invalid type"); // NOI18N
    }

    private void badCPReference(int ofs, int i) {
        throw classFileReadException("Bad constant pool reference: " + ofs + " from entry " + i); // NOI18N
    }

    private ClassFileReadRuntimeException classFileReadException(String msg) {
        msg = "Error reading class " + classInfo.name + ":\n" + msg; // NOI18N

        return new ClassFileReadRuntimeException(msg);
    }

    /**
     * Read class name at the given CONSTANT_Utf8 constant pool index, and return it
     * trimmed of the possible 'L' prefix and ';' suffix.
     */
    private String classNameAtCPIndex(int idx) {
        if (cpTags[idx] != CONSTANT_Utf8) {
            throw classFileReadException("Constant pool entry " + idx + " should be UTF8 constant"); // NOI18N
        }

        int arrayLevel = 0;

        if (cpObjectCache[idx] == null) {
            int utf8Len = getChar(cpOffsets[idx]);
            int stPos = cpOffsets[idx] + 2;
            int initStPos = stPos;

            while (classBuf[stPos] == '[') { // NOI18N
                stPos++;
                arrayLevel++;
            }

            if (stPos != initStPos) {
                if (classBuf[stPos] == 'L') { // NOI18N   // Non-primitive array type
                    stPos++;
                    utf8Len--; // To get rid of the terminating ';'
                }
            }

            utf8Len = utf8Len - (stPos - initStPos);

            String res = StringUtils.utf8ToString(classBuf, stPos, utf8Len);

            for (int i = 0; i < arrayLevel; i++) {
                res = "[" + res; // NOI18N
            }

            cpObjectCache[idx] = res;
        }

        return (String) cpObjectCache[idx];
    }

    private ClassFileReadRuntimeException dataFormatError() { // TODO CHECK: unused method

        return classFileReadException("Data format error"); // NOI18N
    }

    private char nextChar() {
        return (char) (((classBuf[curBufPos++] & 255) << 8) + (classBuf[curBufPos++] & 255));
    }

    private int nextInt() {
        return ((classBuf[curBufPos++] & 255) << 24) + ((classBuf[curBufPos++] & 255) << 16)
               + ((classBuf[curBufPos++] & 255) << 8) + (classBuf[curBufPos++] & 255);
    }

    /**
     * This method actually reads only the information related to the nested classes, and
     * records only those of them which are first level nested classes of this class. The class
     * may also reference other classes which are not package members through the same
     * InnerClasses attribute - their names would be processed when their respective enclosing
     * classes are read.
     */
    private void readAttributes() {
        int i;
        int j;

        classInfo.attrsStartOfs = curBufPos;

        char attrCount = nextChar();

        for (i = 0; i < attrCount; i++) {
            int attrNameIdx = nextChar();
            int attrLen = nextInt();

            if (utf8AtCPIndex(attrNameIdx).equals("InnerClasses")) { // NOI18N

                int nOfClasses = nextChar();
                String[] nestedClasses = new String[nOfClasses];
                int curIdx = 0;
                int nonMemberClassCount = 0;

                for (j = 0; j < nOfClasses; j++) {
                    int innerClassInfoIdx = nextChar();
                    int outerClassInfoIdx = nextChar();
                    int innerClassNameIdx = nextChar();
                    char innerClassAccessFlags = nextChar();

                    String nestedClassFullName = classNameAtCPIndex(getChar(cpOffsets[innerClassInfoIdx]));

                    // We are not interested in references to nested classes whose enclosing class is not this one.
                    if (innerClassNameIdx != 0) {
                        String nestedClassSimpleName = utf8AtCPIndex(innerClassNameIdx);

                        if (!nestedClassFullName.equals(classInfo.name + "$" + nestedClassSimpleName)) { // NOI18N
                                                                                                         // Let's check if it's a local class, with the name like "EncClass$1$Local"

                            int count = nonMemberClassCount + 1;

                            if (!nestedClassFullName.equals(classInfo.name + "$" + count + "$" + nestedClassSimpleName)) {
                                continue; // NOI18N
                            } else {
                                nonMemberClassCount = count;
                            }
                        }
                    } else {
                        nonMemberClassCount++;

                        if (!nestedClassFullName.equals(classInfo.name + "$" + nonMemberClassCount)) {
                            continue; // NOI18N
                        }
                    }

                    nestedClasses[curIdx++] = nestedClassFullName;
                }

                if (curIdx == nOfClasses) {
                    classInfo.nestedClassNames = nestedClasses;
                } else if (curIdx > 0) {
                    // We found fewer nested classes for this class than we originally expected, but still more than 0.
                    // Create a new array to fit their number exactly.
                    classInfo.nestedClassNames = new String[curIdx];
                    System.arraycopy(nestedClasses, 0, classInfo.nestedClassNames, 0, curIdx);
                }

                break;
            } else {
                curBufPos += attrLen;
            }
        }
    }

    private void readConstantPool() {
        int methodRefsNo = 0;
        int classRefsNo = 0;

        classInfo.cpoolStartOfs = curBufPos;
        classInfo.origCPoolCount = nextChar();
        cpOffsets = new int[classInfo.origCPoolCount];
        cpTags = new byte[classInfo.origCPoolCount];

        int cpStart = curBufPos;
        int len;
        int i = 1;

        while (i < cpOffsets.length) {
            byte tag = classBuf[curBufPos++];
            cpOffsets[i] = curBufPos;
            cpTags[i] = tag;
            i++;

            switch (tag) {
                case CONSTANT_Utf8:
                    len = nextChar();
                    curBufPos += len;

                    break;
                case CONSTANT_Class:
                    classRefsNo++;
                case CONSTANT_String:
                    curBufPos += 2;

                    break;
                case CONSTANT_Fieldref:
                case CONSTANT_NameAndType:
                case CONSTANT_Integer:
                case CONSTANT_Float:
                    curBufPos += 4;

                    break;
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    methodRefsNo++;
                    curBufPos += 4;

                    break;
                case CONSTANT_Long:
                case CONSTANT_Double:
                    curBufPos += 8;
                    i++;

                    break;
                default:
                    throw classFileReadException("Bad constant pool tag: " + tag + " at " + Integer.toString(curBufPos - 1)); // NOI18N
            }
        }

        classInfo.cpoolRefsToMethodIdx = new char[methodRefsNo];
        classInfo.cpoolRefsToMethodClassNameAndSig = new String[methodRefsNo][3];
        classInfo.cpoolRefsToClassIdx = new char[classRefsNo];
        classInfo.cpoolRefsToClassName = new String[classRefsNo];

        int curMethodRef = 0;
        int curClassRef = 0;
        cpObjectCache = new Object[cpOffsets.length];

        for (i = 0; i < cpOffsets.length; i++) {
            int ofs = cpOffsets[i];

            if ((cpTags[i] == CONSTANT_Methodref) || (cpTags[i] == CONSTANT_InterfaceMethodref)) {
                classInfo.cpoolRefsToMethodIdx[curMethodRef] = (char) i;
                classInfo.cpoolRefsToMethodClassNameAndSig[curMethodRef] = new String[3];

                int classIdx = getChar(ofs);
                int nameAndTypeIdx = getChar(ofs + 2);

                if ((cpTags[classIdx] != CONSTANT_Class) || (cpTags[nameAndTypeIdx] != CONSTANT_NameAndType)) {
                    badCPReference(ofs, i);
                }

                classInfo.cpoolRefsToMethodClassNameAndSig[curMethodRef][0] = classNameAtCPIndex(getChar(cpOffsets[classIdx]));
                ofs = cpOffsets[nameAndTypeIdx];

                int nameIdx = getChar(ofs);
                int sigIdx = getChar(ofs + 2);

                if ((cpTags[nameIdx] != CONSTANT_Utf8) || (cpTags[sigIdx] != CONSTANT_Utf8)) {
                    badCPReference(ofs, i);
                }

                classInfo.cpoolRefsToMethodClassNameAndSig[curMethodRef][1] = utf8AtCPIndex(nameIdx);
                classInfo.cpoolRefsToMethodClassNameAndSig[curMethodRef][2] = signatureAtCPIndex(sigIdx);
                curMethodRef++;
            } else if (cpTags[i] == CONSTANT_Class) {
                classInfo.cpoolRefsToClassIdx[curClassRef] = (char) i;
                classInfo.cpoolRefsToClassName[curClassRef] = classNameAtCPIndex(getChar(ofs));
                curClassRef++;
            }
        }
    }

    private void readIntermediate() {
        int i;
        int classIdx;
        int superClassIdx;

        classInfo.intermediateDataStartOfs = curBufPos;
        classInfo.accessFlags = nextChar();
        classIdx = nextChar();

        if (cpTags[classIdx] != CONSTANT_Class) {
            throw classFileReadException("Bad reference to this class name"); // NOI18N
        }

        classInfo.name = classNameAtCPIndex(getChar(cpOffsets[classIdx]));
        superClassIdx = nextChar();

        if (cpTags[superClassIdx] != CONSTANT_Class) {
            if ((superClassIdx == 0) && classInfo.name.equals("java/lang/Object")) {
                classInfo.superName = "java/lang/Object"; // NOI18N
            } else {
                throw classFileReadException("Bad reference to super class name"); // NOI18N
            }
        } else {
            classInfo.superName = classNameAtCPIndex(getChar(cpOffsets[superClassIdx]));
        }

        char intfCount = nextChar();

        if (intfCount != 0) {
            classInfo.interfaces = new String[intfCount];

            for (i = 0; i < intfCount; i++) {
                classIdx = nextChar();

                if (cpTags[classIdx] != CONSTANT_Class) {
                    throw classFileReadException("Bad reference to an implemented interface"); // NOI18N
                }

                classInfo.interfaces[i] = classNameAtCPIndex(getChar(cpOffsets[classIdx]));
            }
        }
    }

    private void readMethods() {
        classInfo.methodsStartOfs = curBufPos;

        char methodCount = nextChar();

        if (methodCount == 0) {
            classInfo.methodNames = new String[0];

            return;
        }

        String[] names = new String[methodCount];
        String[] signatures = new String[methodCount];
        char[] accessFlags = new char[methodCount];
        int[] methodInfoOffsets = new int[methodCount];
        int[] methodInfoLengths = new int[methodCount];
        int[] bytecodeOffsets = new int[methodCount];
        char[] bytecodeLengths = new char[methodCount];
        int[] exceptionTableStartOffsets = new int[methodCount];
        int[] lineNumberTableOffsets = new int[methodCount];
        char[] lineNumberTableLengths = new char[methodCount];

        for (int i = 0; i < methodCount; i++) {
            methodInfoOffsets[i] = curBufPos;
            accessFlags[i] = nextChar();
            names[i] = utf8AtCPIndex(nextChar());
            signatures[i] = signatureAtCPIndex(nextChar());
            bytecodeOffsets[i] = 0;
            lineNumberTableOffsets[i] = 0;

            int attrCount = nextChar();

            for (int j = 0; j < attrCount; j++) {
                int attrNameIdx = nextChar();
                int attrLen = nextInt();

                if (utf8AtCPIndex(attrNameIdx).equals("Code")) { // NOI18N
                    curBufPos += 4; // Skip max_stack and max_locals

                    char codeLen = bytecodeLengths[i] = (char) nextInt();
                    bytecodeOffsets[i] = curBufPos - methodInfoOffsets[i];
                    curBufPos += codeLen;
                    exceptionTableStartOffsets[i] = curBufPos - methodInfoOffsets[i];

                    int count = nextChar(); // Exception table length
                    curBufPos += (8 * count); // Skip exception table
                    count = nextChar(); // Attribute (or rather sub-attribute) count

                    for (int k = 0; k < count; k++) {
                        attrNameIdx = nextChar();
                        attrLen = nextInt();

                        if (utf8AtCPIndex(attrNameIdx).equals("LineNumberTable")) { // NOI18N

                            char tableLen = lineNumberTableLengths[i] = nextChar();
                            lineNumberTableOffsets[i] = curBufPos - methodInfoOffsets[i];
                            curBufPos += (4 * tableLen);
                        } else {
                            curBufPos += attrLen;
                        }
                    }
                } else {
                    curBufPos += attrLen;
                }
            }

            methodInfoLengths[i] = curBufPos - methodInfoOffsets[i];
        }

        classInfo.methodNames = names;
        classInfo.methodSignatures = signatures;
        classInfo.methodAccessFlags = accessFlags;
        classInfo.methodInfoOffsets = methodInfoOffsets;
        classInfo.methodInfoLengths = methodInfoLengths;
        classInfo.methodBytecodesOffsets = bytecodeOffsets;
        classInfo.methodBytecodesLengths = bytecodeLengths;
        classInfo.exceptionTableStartOffsets = exceptionTableStartOffsets;
        classInfo.lineNumberTablesOffsets = lineNumberTableOffsets;
        classInfo.lineNumberTablesLengths = lineNumberTableLengths;
    }

    private void readPreamble() {
        int magic = nextInt();

        if (magic != JAVA_MAGIC) {
            throw classFileReadException("Illegal start of class file"); // NOI18N
        }

        int minorVersion = nextChar();
        int majorVersion = nextChar();

        if ((majorVersion > JAVA_MAJOR_VERSION)
                || (((majorVersion * 1000) + minorVersion) < ((JAVA_MIN_MAJOR_VERSION * 1000) + JAVA_MIN_MINOR_VERSION))) {
            if ((majorVersion != 49) && (majorVersion != 50) && (majorVersion != 51)) {
                String versionCode = majorVersion + "." + minorVersion; // NOI18N
                String message = "Unsupported class file version: " + versionCode; // NOI18N
                throw classFileReadException(message);
            }
        }
    }

    private String signatureAtCPIndex(int idx) {
        return utf8AtCPIndex(idx);
    }

    private void skipFields() {
        classInfo.fieldsStartOfs = curBufPos;

        char definedFieldCount = nextChar();

        for (int i = 0; i < definedFieldCount; i++) {
            curBufPos += 6; // skip 3 chars: flags, name index and signature index

            int attrCount = nextChar();

            for (int j = 0; j < attrCount; j++) {
                curBufPos += 2; // Skip char: attr name index

                int attrLen = nextInt();
                curBufPos += attrLen;
            }
        }
    }

    private String utf8AtCPIndex(int idx) {
        if (cpTags[idx] != CONSTANT_Utf8) {
            throw classFileReadException("Constant pool entry " + idx + " should be UTF8 constant"); // NOI18N
        }

        if (cpObjectCache[idx] == null) {
            int utf8Len = getChar(cpOffsets[idx]);
            cpObjectCache[idx] = StringUtils.utf8ToString(classBuf, cpOffsets[idx] + 2, utf8Len);
        }

        return (String) cpObjectCache[idx];
    }
}
