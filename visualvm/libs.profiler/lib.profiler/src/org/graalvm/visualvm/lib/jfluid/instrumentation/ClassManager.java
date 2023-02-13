/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import java.io.IOException;
import java.util.*;
import org.graalvm.visualvm.lib.jfluid.classfile.*;
import org.graalvm.visualvm.lib.jfluid.client.RuntimeProfilingPoint;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.utils.MiscUtils;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.RootClassLoadedCommand;


/**
 * Basic utility methods used by all scanner classes.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ClassManager implements JavaClassConstants, CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final RuntimeProfilingPoint[] EMPTY_PROFILEPOINT_ARRAY = new RuntimeProfilingPoint[0];

    /**
     * Sorts profiling points by bytecode index so that injector can insert them sequentially.
     */
    private static Comparator ByBciComparator = new Comparator() {
        public int compare(Object aa, Object bb) {
            RuntimeProfilingPoint a = (RuntimeProfilingPoint) aa;
            RuntimeProfilingPoint b = (RuntimeProfilingPoint) bb;

            return a.getBci() - b.getBci();
        }
    };


    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected ProfilingSessionStatus status;
    protected ClassRepository classRepo;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected ClassManager(ClassRepository repo, ProfilingSessionStatus status) {
        this.status = status;
        classRepo = repo;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Filters profiling points for given class.
     *
     * @param points profiling points to scan
     * @param classInfo searches for points in this class
     * @return RuntimeProfilingPoint[] array of profiling points inside the specified method
     */
    protected RuntimeProfilingPoint[] getRuntimeProfilingPoints(RuntimeProfilingPoint[] points, ClassInfo classInfo) {
        List newPoints = null;

        String className = classInfo.getName().replace('/', '.'); // NOI18N
        for (RuntimeProfilingPoint point : points) {
            if (className.equals(point.getClassName()) && point.resolve(classRepo, classInfo)) {
                if (newPoints == null) {
                    newPoints = new ArrayList(2);
                }

                newPoints.add(point);
            }
        }

        if (newPoints == null) {
            return EMPTY_PROFILEPOINT_ARRAY;
        }

        return (RuntimeProfilingPoint[]) newPoints.toArray(new RuntimeProfilingPoint[0]);
    }

    /**
     * Filters profiling points for given method.
     *
     * @param points profiling points to scan
     * @param methodIdx method index in the given class
     * @return RuntimeProfilingPoint[] array of profiling points inside the specified method
     */
    protected static RuntimeProfilingPoint[] getRuntimeProfilingPoints(RuntimeProfilingPoint[] points, int methodIdx) {
        List newPoints = null;

        for (RuntimeProfilingPoint point : points) {
            if (point.getMethodIdx() == methodIdx) {
                if (newPoints == null) {
                    newPoints = new ArrayList(2);
                }

                newPoints.add(point);
            }
        }

        if (newPoints == null) {
            return EMPTY_PROFILEPOINT_ARRAY;
        } else if (newPoints.size() > 1) {
            Collections.sort(newPoints, ByBciComparator);
        }

        return (RuntimeProfilingPoint[]) newPoints.toArray(new RuntimeProfilingPoint[0]);
    }

    /**
     * Filters profiling points for given class and method.
     *
     * @param points profiling points to scan
     * @param classInfo searches for points in this class
     * @param methodIdx method index in the given class
     * @return RuntimeProfilingPoint[] array of profiling points inside the specified method
     */
    protected RuntimeProfilingPoint[] getRuntimeProfilingPoints(RuntimeProfilingPoint[] points, ClassInfo classInfo,
                                                                       int methodIdx) {
        List newPoints = null;
        String className = classInfo.getName().replace('/', '.'); // NOI18N
        for (RuntimeProfilingPoint point : points) {
            if (className.equals(point.getClassName())) {
                if (point.resolve(classRepo, classInfo)) {
                    if (point.getMethodIdx() == methodIdx) {
                        if (newPoints == null) {
                            newPoints = new ArrayList(2);
                        }

                        newPoints.add(point);
                    }
                }
            }
        }

        if (newPoints == null) {
            return EMPTY_PROFILEPOINT_ARRAY;
        } else if (newPoints.size() > 1) {
            Collections.sort(newPoints, ByBciComparator);
        }

        return (RuntimeProfilingPoint[]) newPoints.toArray(new RuntimeProfilingPoint[0]);
    }

    /**
     * Returns a ClassInfo for a given non-array class name. If actualCPLength &gt;= 0 is provided, the constant
     * pool length in the returned ClassInfo is set to that value. Otherwise it is not touched, i.e. remains
     * the same as for the .class file on the CLASSPATH.
     */
    protected DynamicClassInfo javaClassForName(String className, int classLoaderId) {
        try {
            return classRepo.lookupClass(className, classLoaderId);
        } catch (IOException ex2) {
            MiscUtils.printWarningMessage("Error reading class " + className); // NOI18N
            MiscUtils.printWarningMessage(ex2.getMessage());
        } catch (ClassFormatError er) {
            MiscUtils.printWarningMessage(er.getMessage());
        }

        return null;
    }

    protected BaseClassInfo javaClassForObjectArrayType(String elementTypeName) {
        return classRepo.lookupSpecialClass("[" + elementTypeName); // NOI18N
    }

    protected BaseClassInfo javaClassForPrimitiveArrayType(int arrayTypeId) {
        return classRepo.lookupSpecialClass(PRIMITIVE_ARRAY_TYPE_NAMES[arrayTypeId]);
    }

    protected BaseClassInfo lookupSpecialClass(String className) {
        return classRepo.lookupSpecialClass(className);
    }

    protected Enumeration getClassEnumerationWithAllVersions() {
        return classRepo.getClassEnumerationWithAllVersions();
    }

    protected List getAllClassVersions(String className) {
        return classRepo.getAllClassVersions(className);
    }

    /** This is currently used only in memory profiling */
    protected BaseClassInfo javaClassOrPlaceholderForName(String className, int classLoaderId) {
        return classRepo.lookupClassOrCreatePlaceholder(className, classLoaderId);
    }

    protected BaseClassInfo loadedJavaClassOrExistingPlaceholderForName(String className, int classLoaderId) {
        return classRepo.lookupLoadedClass(className, classLoaderId, true);
    }

    protected void registerPlaceholder(PlaceholderClassInfo pci) {
        classRepo.addClassInfo(pci);
    }

    protected void resetLoadedClassData() {
        classRepo.clearCache();
    }

    /**
     * Given a list of classes (normally all classes currently loaded by the JVM), determine those that are loaded using
     * custom classloaders, get their cached bytecodes from the JVM, and put them into ClassRepository.
     */
    protected void storeClassFileBytesForCustomLoaderClasses(RootClassLoadedCommand rootLoaded) {
        String[] loadedClasses = rootLoaded.getAllLoadedClassNames();
        byte[][] cachedClassFileBytes = rootLoaded.getCachedClassFileBytes();
        int[] loadedClassLoaderIds = rootLoaded.getAllLoadedClassLoaderIds();
        int[] superClasses = rootLoaded.getAllLoaderSuperClassIds();
        int[][] interfaceNames = rootLoaded.getAllLoadedInterfaceIds();
        int nClasses = loadedClasses.length;
        Set allInterfacesIndexes = new HashSet();

        for (int i = 0; i < nClasses; i++) {
            if (cachedClassFileBytes[i] != null) {
                String name = loadedClasses[i];
                int loaderId = loadedClassLoaderIds[i];
                byte[] bytes = cachedClassFileBytes[i];
                if (bytes != null && bytes.length == 0) {
                    String superClass;
                    int sidx = superClasses[i];
                    if (sidx != -1 ) {
                        superClass = loadedClasses[sidx];
                    } else {
                        superClass = OBJECT_SLASHED_CLASS_NAME;
                    }
                    int[] interfaceNamedIdxs = interfaceNames[i];
                    List interfaces = new ArrayList();
                    for (int j = 0; j < interfaceNamedIdxs.length; j++) {
                        int iidx = interfaceNamedIdxs[j];
                        if (iidx != -1) {
                            interfaces.add(loadedClasses[iidx]);
                            allInterfacesIndexes.add(Integer.valueOf(iidx));
                        }
                    }
                    classRepo.addVMSuppliedClassFile(name, loaderId, bytes, superClass, (String[])interfaces.toArray(new String[0]));
                } else {
                    classRepo.addVMSuppliedClassFile(name, loaderId, bytes);
                }
            }
        }
        // set interfaces
        for (Object intIndex : allInterfacesIndexes) {
            int iidx = ((Integer)intIndex).intValue();
            if (cachedClassFileBytes[iidx] != null) {
                DynamicClassInfo iface = javaClassForName(loadedClasses[iidx], loadedClassLoaderIds[iidx]);
                iface.setInterface();
            }
        }
    }
}
