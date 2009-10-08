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
    private DynamicClassInfo superClass; // Superclass as a DynamicClassInfo (just name is inconvenient when multiple classloaders are used)
    private String classFileLocation; // Directory or .jar file where the .class file is located.
    private int[] baseCPoolCount;
    private char[] instrMethodIds; // Ids assigned to instrumented methods, 0 for uninstrumented methods
    private DynamicClassInfo[] interfacesDCI; // Ditto for superinterfaces

    // Data used by our call graph revelation mechanism, to mark classes/methods according to their reachability,
    // scannability, etc. properties
    private char[] methodScanStatus;

    // On JDK 1.5, we save methodinfos for all instrumented methods (until they are deinstrumented), so when methods from
    // same class are instrumented one-by-one and redefineClasses is used for each of them, we don't have to regenerate the
    // code for each previously instrumented method over and over again.
    private byte[][] modifiedAndSavedMethodInfos;
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
            (new ClassFileParser()).parseClassFile(classFileBytes, this);

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
            if (name == "java/lang/Object") {
                subclasses = new ArrayList(500); // NOI18N
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
        int loaderId = getLoaderId();
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

        if ((superClass == null) || (superClass.getName() == "java/lang/Object")) {
            return false; // NOI18N
        }

        return superClass.implementsInterface(intfName);
    }

    public void saveMethodInfo(int idx, byte[] methodInfo) {
        if (modifiedAndSavedMethodInfos == null) {
            modifiedAndSavedMethodInfos = new byte[methodNames.length][];
        }

        modifiedAndSavedMethodInfos[idx] = methodInfo;
    }

    public void unsetMethodInstrumented(int i) {
        methodScanStatus[i] &= (~8);
        nInstrumentedMethods--;
    }

    public void unsetMethodSpecial(int i) {
        methodScanStatus[i] &= (~128);
    }

    private int getBCLenForModifiedAndSavedMethodInfo(int idx) {
        byte[] methodInfo = modifiedAndSavedMethodInfos[idx];
        int bcLenPos = methodBytecodesOffsets[idx] - 4;

        return (((methodInfo[bcLenPos++] & 255) << 24) + ((methodInfo[bcLenPos++] & 255) << 16)
               + ((methodInfo[bcLenPos++] & 255) << 8) + (methodInfo[bcLenPos++] & 255));
    }
}
