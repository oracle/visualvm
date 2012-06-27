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
import java.util.ArrayList;


/**
 * A representation of a binary Java class that contains information for the class file itself, plus various status
 * bits used for proper instrumentation state accounting in JFluid.
 *
 * @author Tomas Hurka
 * @author Misha Dmitirev
 */
public class DynamicClassInfo extends ClassInfo {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList subclasses; // Subclasses as DynamicClassInfos
    private DynamicClassInfo superClass; // Superclass as a DynamicClassInfo (just name not is inconvenient when multiple classloaders are used)
    private String classFileLocation; // Directory or .jar file where the .class file is located.
    private int[] baseCPoolCount;
    private int java_lang_ThowableCPIndex; // constant pool index for java.lang.Throwable
    private char[] instrMethodIds; // Ids assigned to instrumented methods, 0 for uninstrumented methods
    private DynamicClassInfo[] interfacesDCI; // Ditto for superinterfaces

    // Data used by our call graph revelation mechanism, to mark classes/methods according to their reachability,
    // scannability, etc. properties
    private char[] methodScanStatus;

    // On JDK 1.5, we save methodinfos for all instrumented methods (until they are deinstrumented), so when methods from
    // same class are instrumented one-by-one and redefineClasses is used for each of them, we don't have to regenerate the
    // code for each previously instrumented method over and over again.
    private byte[][] modifiedAndSavedMethodInfos;
    private int[] modifiedMethodBytecodesLength;
    private int[] modifiledLocalVariableTableOffsets;
    private int[] modifiledLocalVariableTypeTableOffsets;
    private int[] modifiledStackMapTableOffsets;
    private boolean allMethodsMarkers = false;
    private boolean allMethodsRoots = false;
    private boolean hasUninstrumentedMarkerMethods;
    private boolean hasUninstrumentedRootMethods;
    private boolean isLoaded;

    // true if class was scanned for for HttpServlet.do*() methods
    private boolean servletDoMethodScanned;

    /** Data for supporting both 1.4.2-style constant pool incremental extending and 1.5-style redefinition as a whole */
    private int currentCPoolCount; // The current number of entries in the cpool of this class (increased due to instrumentation)
                                   // When we add entries to cpool for a particular injection type, its size before entries are added (base count) is stored
                                   // in this array's element corresponding to this injection type number (e.g. INJ_RECURSIVE_NORMAL_METHOD or INJ_CODE_REGION).
    private int nInstrumentedMethods;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DynamicClassInfo(String className, int loaderId, String classFileLocation)
                     throws IOException, ClassFormatError {
        super(className, loaderId);
        this.classFileLocation = classFileLocation;

        byte[] classFileBytes = getClassFileBytes();

        try {
            new ClassFileParser().parseClassFile(classFileBytes, this);

            if (!className.equals(name)) {
                throw new ClassFormatError("Mismatch between name in .class file and location for " + className // NOI18N
                                           + "\nYour class path setting may be incorrect."); // NOI18N
            }
        } catch (ClassFileParser.ClassFileReadException ex) {
            throw new ClassFormatError(ex.getMessage());
        }

        methodScanStatus = new char[methodNames.length];
        instrMethodIds = new char[methodNames.length];
        currentCPoolCount = origCPoolCount;
        baseCPoolCount = new int[INJ_MAXNUMBER];

        for (int i = 0; i < INJ_MAXNUMBER; i++) {
            baseCPoolCount[i] = -1;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAllMethodsMarkers() {
        allMethodsMarkers = true;
        hasUninstrumentedMarkerMethods = true;
    }

    public boolean getAllMethodsMarkers() {
        return allMethodsMarkers;
    }

    public void setAllMethodsRoots() {
        allMethodsRoots = true;
        hasUninstrumentedRootMethods = true;
    }

    public boolean getAllMethodsRoots() {
        return allMethodsRoots;
    }

    public void setBaseCPoolCount(int injType, int v) {
        baseCPoolCount[injType] = v;
    }

    public int getBaseCPoolCount(int injType) {
        return baseCPoolCount[injType];
    }

    public int getBaseCPoolCountLen() {
        return baseCPoolCount.length;
    }

    public byte[] getClassFileBytes() throws IOException {
        return ClassFileCache.getDefault().getClassFile(name, classFileLocation);
    }

    public String getClassFileLocation() {
        return classFileLocation;
    } // TODO CHECK: unused method

    public void setCurrentCPoolCount(int v) {
        currentCPoolCount = v;
    }

    public int getCurrentCPoolCount() {
        return currentCPoolCount;
    }

    public int getExceptionTableStartOffsetInMethodInfo(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            int bcLen = getBCLenForModifiedAndSavedMethodInfo(idx);

            return methodBytecodesOffsets[idx] + bcLen;
        } else {
            return super.getExceptionTableStartOffsetInMethodInfo(idx);
        }
    }

    public int getLocalVariableTableStartOffsetInMethodInfo(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            if (modifiledLocalVariableTableOffsets[idx] == 0) {
                int newOffset = getExceptionTableStartOffsetInMethodInfo(idx)+getExceptionTableCount(idx)*8+2;
                byte[] methodInfo = getMethodInfo(idx);
                int attrCount = getU2(methodInfo, newOffset); newOffset+=2;// Attribute (or rather sub-attribute) count

                for (int k = 0; k < attrCount; k++) {
                    int attrNameIdx = getU2(methodInfo, newOffset); newOffset+=2;
                    int attrLen = getU4(methodInfo, newOffset); newOffset+=4;

                    if (attrNameIdx==localVaribaleTableCPindex){
                        modifiledLocalVariableTableOffsets[idx] = newOffset+2;
                        break;
                    }
                    newOffset += attrLen;
                }
            }
            return modifiledLocalVariableTableOffsets[idx];
        } else {
            return super.getLocalVariableTableStartOffsetInMethodInfo(idx);
        }
    }
    
    public int getLocalVariableTypeTableStartOffsetInMethodInfo(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            if (modifiledLocalVariableTypeTableOffsets[idx] == 0) {
                int newOffset = getExceptionTableStartOffsetInMethodInfo(idx)+getExceptionTableCount(idx)*8+2;
                byte[] methodInfo = getMethodInfo(idx);
                int attrCount = getU2(methodInfo, newOffset); newOffset+=2;// Attribute (or rather sub-attribute) count

                for (int k = 0; k < attrCount; k++) {
                    int attrNameIdx = getU2(methodInfo, newOffset); newOffset+=2;
                    int attrLen = getU4(methodInfo, newOffset); newOffset+=4;

                    if (attrNameIdx==localVaribaleTypeTableCPindex){
                        modifiledLocalVariableTypeTableOffsets[idx] = newOffset+2;
                        break;
                    }
                    newOffset += attrLen;
                }
            }
            return modifiledLocalVariableTypeTableOffsets[idx];
        } else {
            return super.getLocalVariableTypeTableStartOffsetInMethodInfo(idx);
        }
    }
    
    public int getStackMapTableStartOffsetInMethodInfo(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            if (modifiledStackMapTableOffsets[idx] == 0) {
                int newOffset = getExceptionTableStartOffsetInMethodInfo(idx)+getExceptionTableCount(idx)*8+2;
                byte[] methodInfo = getMethodInfo(idx);
                int attrCount = getU2(methodInfo, newOffset); newOffset+=2;// Attribute (or rather sub-attribute) count

                for (int k = 0; k < attrCount; k++) {
                    int attrNameIdx = getU2(methodInfo, newOffset); newOffset+=2;
                    int attrLen = getU4(methodInfo, newOffset); newOffset+=4;

                    if (attrNameIdx==stackMapTableCPindex){
                        modifiledStackMapTableOffsets[idx] = newOffset+2;
                        break;
                    }
                    newOffset += attrLen;
                }
            }
            return modifiledStackMapTableOffsets[idx];
        } else {
            return super.getStackMapTableStartOffsetInMethodInfo(idx);
        }
    }
    
    public void setHasUninstrumentedMarkerMethods(boolean v) {
        hasUninstrumentedMarkerMethods = v;
    }

    public void setHasUninstrumentedRootMethods(boolean v) {
        hasUninstrumentedRootMethods = v;
    }

    public void setInstrMethodId(int i, int id) {
        instrMethodIds[i] = (char) id;
    }

    public char getInstrMethodId(int i) {
        return instrMethodIds[i];
    } // TODO CHECK: unused method

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public byte[] getMethodBytecode(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            byte[] methodInfo = modifiedAndSavedMethodInfos[idx];
            int bcLen = getBCLenForModifiedAndSavedMethodInfo(idx);
            byte[] ret = new byte[bcLen];
            System.arraycopy(methodInfo, methodBytecodesOffsets[idx], ret, 0, bcLen);

            return ret;
        } else {
            return super.getMethodBytecode(idx);
        }
    }

    public int getMethodBytecodesLength(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            return getBCLenForModifiedAndSavedMethodInfo(idx);
        } else {
            return super.getMethodBytecodesLength(idx);
        }
    }

    public byte[] getMethodInfo(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            return modifiedAndSavedMethodInfos[idx];
        } else {
            return super.getMethodInfo(idx);
        }
    }

    public int getMethodInfoLength(int idx) {
        if ((modifiedAndSavedMethodInfos != null) && (modifiedAndSavedMethodInfos[idx] != null)) {
            return modifiedAndSavedMethodInfos[idx].length;
        } else {
            return super.getMethodInfoLength(idx);
        }
    }

    public void setMethodInstrumented(int i) {
        methodScanStatus[i] |= 8;
        nInstrumentedMethods++;
    }

    public boolean isMethodInstrumented(int i) {
        return (methodScanStatus[i] & 8) != 0;
    }

    public void setMethodLeaf(int i) {
        methodScanStatus[i] |= 16;
    }

    public boolean isMethodLeaf(int i) {
        return (methodScanStatus[i] & 16) != 0;
    }

    public void setMethodMarker(int i) {
        methodScanStatus[i] |= 256;
        hasUninstrumentedMarkerMethods = true;
    }

    public boolean isMethodMarker(int i) {
        return allMethodsMarkers || ((methodScanStatus[i] & 256) != 0);
    }

    public void setMethodReachable(int i) {
        methodScanStatus[i] |= 1;
    }

    public boolean isMethodReachable(int i) {
        return (methodScanStatus[i] & 1) != 0;
    }

    public void setMethodRoot(int i) {
        methodScanStatus[i] |= 64;
        hasUninstrumentedRootMethods = true;
    }

    public boolean isMethodRoot(int i) {
        return allMethodsRoots || ((methodScanStatus[i] & 64) != 0);
    }

    public void setMethodScanned(int i) {
        methodScanStatus[i] |= 4; /* hasUninstrumentedScannedMethods = true; */
    }

    public boolean isMethodScanned(int i) {
        return (methodScanStatus[i] & 4) != 0;
    }

    public void setMethodSpecial(int i) {
        methodScanStatus[i] |= 128;
    }

    public boolean isMethodSpecial(int i) {
        return (methodScanStatus[i] & 128) != 0;
    }

    public void setMethodUnscannable(int i) {
        methodScanStatus[i] |= 2;
    }

    public boolean isMethodUnscannable(int i) {
        return (methodScanStatus[i] & 2) != 0;
    }

    public void setMethodVirtual(int i) {
        methodScanStatus[i] |= 32;
    }

    public boolean isMethodVirtual(int i) {
        return (methodScanStatus[i] & 32) != 0;
    }

    public byte[] getOrigMethodInfo(int idx) {
        return super.getMethodInfo(idx);
    }

    public int getOrigMethodInfoLength(int idx) {
        return super.getMethodInfoLength(idx);
    }

    public void setServletDoMethodScanned() {
        servletDoMethodScanned = true;
    }

    public boolean isServletDoMethodScanned() {
        return servletDoMethodScanned;
    }

    public boolean isSubclassOf(String superClass) {
        if (getName() == superClass) {
            return true;
        }

        DynamicClassInfo sc = getSuperClass();

        if ((sc == null) || (sc == this)) {
            return false;
        }

        return sc.isSubclassOf(superClass);
    }

    public ArrayList getSubclasses() {
        return subclasses;
    }

    public void setSuperClass(DynamicClassInfo sc) {
        superClass = sc;
    }

    public DynamicClassInfo getSuperClass() {
        return superClass;
    }

    public void setSuperInterface(DynamicClassInfo si, int idx) {
        if (interfacesDCI == null) {
            interfacesDCI = new DynamicClassInfo[interfaces.length];
        }

        interfacesDCI[idx] = si;
    }

    public DynamicClassInfo[] getSuperInterfaces() {
        return interfacesDCI;
    }

    public void addSubclass(DynamicClassInfo subclass) {
        if (subclasses == null) {
            if (name == "java/lang/Object") {  // NOI18N
                subclasses = new ArrayList(500);
            } else {
                subclasses = new ArrayList();
            }
        }

        subclasses.add(subclass);
    }

    public boolean hasInstrumentedMethods() {
        return (nInstrumentedMethods > 0);
    }

    public boolean hasUninstrumentedMarkerMethods() {
        return hasUninstrumentedMarkerMethods;
    }

    /*
       public boolean hasUninstrumentedScannedMethods()          { return hasUninstrumentedScannedMethods; }  // TODO CHECK: unused method
       public void setHasUninstrumentedScannedMethods(boolean v) { hasUninstrumentedScannedMethods = v; }  // TODO CHECK: unused method
     */
    public boolean hasUninstrumentedRootMethods() {
        return hasUninstrumentedRootMethods;
    }

    /**
     * Note that this method uses the name of the interface in question intentionally - its (few) callers
     * benefit from providing the name rather than a DynamicClassInfo.
     */
    public boolean implementsInterface(String intfName) {
        String[] intfs = getInterfaceNames();

        if (intfs != null) {
            for (int i = 0; i < intfs.length; i++) {
                if (intfName == intfs[i]) {
                    return true;
                }
            }

            DynamicClassInfo[] intfsDCI = getSuperInterfaces();

            if (intfsDCI != null) {
                for (int i = 0; i < intfsDCI.length; i++) {
                    DynamicClassInfo intfClazz = intfsDCI[i];

                    if ((intfClazz != null) && intfClazz.implementsInterface(intfName)) {
                        return true;
                    }
                }
            }
        }

        DynamicClassInfo superClass = getSuperClass();

        if ((superClass == null) || (superClass.getName() == "java/lang/Object")) {  // NOI18N
            return false;
        }

        return superClass.implementsInterface(intfName);
    }

    public void saveMethodInfo(int idx, byte[] methodInfo) {
        if (modifiedAndSavedMethodInfos == null) {
            modifiedAndSavedMethodInfos = new byte[methodNames.length][];
        }

        modifiedAndSavedMethodInfos[idx] = methodInfo;
        modifiedMethodBytecodesLength = new int[methodNames.length];
        modifiledLocalVariableTableOffsets = new int[methodNames.length];
        modifiledLocalVariableTypeTableOffsets = new int[methodNames.length];
        modifiledStackMapTableOffsets = new int[methodNames.length];
    }

    public void unsetMethodInstrumented(int i) {
        methodScanStatus[i] &= (~8);
        nInstrumentedMethods--;
    }

    public void unsetMethodSpecial(int i) {
        methodScanStatus[i] &= (~128);
    }

    public void addGlobalCatchStackMapTableEntry(int methodIdx, int endPC) {
        if (majorVersion >= 50) {
            boolean isStatic = isMethodStatic(methodIdx);
            boolean constructor = "<init>".equals(getMethodName(methodIdx));    // NOI18N
            int[] localsCPIdx = new int[0];
            int[] stacksCPIdx;
            
//            LOG.finer("Adding global catch for " + getName() + " method " + getMethodName(methodIdx));   // NOI18N
            if (stackMapTableCPindex == 0) {
                stackMapTableCPindex = getBaseCPoolCount(INJ_STACKMAP);
            }
            if (java_lang_ThowableCPIndex == 0) {
                java_lang_ThowableCPIndex = getCPIndexOfClass("java/lang/Throwable");   // NOI18N
                if (java_lang_ThowableCPIndex == -1) {
//                    LOG.finer("java/lang/Thowable not found in " + getName());   // NOI18N
                    java_lang_ThowableCPIndex = getBaseCPoolCount(INJ_THROWABLE);
                }
            }
            stacksCPIdx = new int[] {java_lang_ThowableCPIndex};
            if (!isStatic) {
                if (constructor) {
                    localsCPIdx = new int[] {0};
                } else {
                    localsCPIdx = new int[] {classIndex};
                }
            }
            getStackMapTables().addFullStackMapFrameEntry(methodIdx, endPC, localsCPIdx, stacksCPIdx);
        }
    }

    private int getBCLenForModifiedAndSavedMethodInfo(int idx) {
        if (modifiedMethodBytecodesLength[idx] == 0) {
            byte[] methodInfo = modifiedAndSavedMethodInfos[idx];
            int bcLenPos = methodBytecodesOffsets[idx] - 4;

            modifiedMethodBytecodesLength[idx] =  getU4(methodInfo,bcLenPos);
        }
        return modifiedMethodBytecodesLength[idx];
    }

    static int getU2(byte[] bytecodes, int pos) {
        return ((bytecodes[pos] & 0xFF) << 8) + (bytecodes[pos + 1] & 0xFF);
    }

    static int getU4(byte[] bytecodes, int pos) {
        return ((bytecodes[pos] & 0xFF) << 24) + ((bytecodes[pos + 1] & 0xFF) << 16) + ((bytecodes[pos + 2] & 0xFF) << 8)
               + (bytecodes[pos + 3] & 0xFF);
    }
}
