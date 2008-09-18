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

package org.netbeans.lib.profiler.instrumentation;

import org.netbeans.lib.profiler.classfile.*;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.utils.MiscUtils;
import java.io.IOException;
import java.util.*;


/**
 * Basic utility methods used by all scaner classes.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class ClassManager implements JavaClassConstants, CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final RuntimeProfilingPoint[] EMPTY_PROFILEPOINT_ARRAY = new RuntimeProfilingPoint[0];

    /**
     * Sorts profiling points by bytecode index so that injector can insert them sequentaly.
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

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected ClassManager(ProfilingSessionStatus status) {
        this.status = status;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Filters profiling points for given class.
     *
     * @param points profiling points to scan
     * @param classInfo searches for points in this class
     * @return RuntimeProfilingPoint[] array of profiling points inside the specified method
     */
    protected static RuntimeProfilingPoint[] getRuntimeProfilingPoints(RuntimeProfilingPoint[] points, ClassInfo classInfo) {
        List newPoints = null;

        String className = classInfo.getName().replace('/', '.'); // NOI18N

        for (int i = 0; i < points.length; i++) {
            RuntimeProfilingPoint point = points[i];

            if (className.equals(point.getClassName()) && point.resolve(classInfo)) {
                if (newPoints == null) {
                    newPoints = new ArrayList(2);
                }

                newPoints.add(point);
            }
        }

        if (newPoints == null) {
            return EMPTY_PROFILEPOINT_ARRAY;
        }

        return (RuntimeProfilingPoint[]) newPoints.toArray(new RuntimeProfilingPoint[newPoints.size()]);
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

        for (int i = 0; i < points.length; i++) {
            RuntimeProfilingPoint point = points[i];

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

        return (RuntimeProfilingPoint[]) newPoints.toArray(new RuntimeProfilingPoint[newPoints.size()]);
    }

    /**
     * Filters profiling points for given class and method.
     *
     * @param points profiling points to scan
     * @param classInfo searches for points in this class
     * @param methodIdx method index in the given class
     * @return RuntimeProfilingPoint[] array of profiling points inside the specified method
     */
    protected static RuntimeProfilingPoint[] getRuntimeProfilingPoints(RuntimeProfilingPoint[] points, ClassInfo classInfo,
                                                                       int methodIdx) {
        List newPoints = null;
        String className = classInfo.getName().replace('/', '.'); // NOI18N

        for (int i = 0; i < points.length; i++) {
            RuntimeProfilingPoint point = points[i];

            if (className.equals(point.getClassName())) {
                if (point.resolve(classInfo)) {
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

        return (RuntimeProfilingPoint[]) newPoints.toArray(new RuntimeProfilingPoint[newPoints.size()]);
    }

    /**
     * Returns a ClassInfo for a given non-array class name. If actualCPLength >= 0 is provided, the constant
     * pool length in the returned ClassInfo is set to that value. Otherwise it is not touched, i.e. remains
     * the same as for the .class file on the CLASSPATH.
     */
    protected static DynamicClassInfo javaClassForName(String className, int classLoaderId) {
        try {
            return ClassRepository.lookupClass(className, classLoaderId);
        } catch (IOException ex2) {
            MiscUtils.printWarningMessage("Error reading class " + className); // NOI18N
            MiscUtils.printWarningMessage(ex2.getMessage());
        } catch (ClassFormatError er) {
            MiscUtils.printWarningMessage(er.getMessage());
        }

        return null;
    }

    protected static BaseClassInfo javaClassForObjectArrayType(String elementTypeName) {
        BaseClassInfo clazz = ClassRepository.lookupSpecialClass("[" + elementTypeName); // NOI18N

        return clazz;
    }

    protected static BaseClassInfo javaClassForPrimitiveArrayType(int arrayTypeId) {
        BaseClassInfo clazz = ClassRepository.lookupSpecialClass(PRIMITIVE_ARRAY_TYPE_NAMES[arrayTypeId]);

        return clazz;
    }

    /** This is currently used only in memory profiling */
    protected static BaseClassInfo javaClassOrPlaceholderForName(String className, int classLoaderId) {
        return ClassRepository.lookupClassOrCreatePlaceholder(className, classLoaderId);
    }

    protected static BaseClassInfo loadedJavaClassOrExistingPlaceholderForName(String className, int classLoaderId) {
        return ClassRepository.lookupLoadedClass(className, classLoaderId, true);
    }

    protected static void registerPlaceholder(PlaceholderClassInfo pci) {
        ClassRepository.addPlaceholder(pci);
    }

    protected static void resetLoadedClassData() {
        ClassRepository.clearCache();
    }

    /**
     * Given a list of classes (normally all classes currently loaded by the JVM), deterime those that are loaded using
     * custom classloaders, get their cached bytecodes from the JVM, and put them into ClassRepository.
     */
    protected static void storeClassFileBytesForCustomLoaderClasses(String[] loadedClasses, int[] loadedClassLoaderIds,
                                                                    byte[][] cachedClassFileBytes) {
        int nClasses = loadedClasses.length;

        for (int i = 0; i < nClasses; i++) {
            if (cachedClassFileBytes[i] != null) {
                ClassRepository.addVMSuppliedClassFile(loadedClasses[i], loadedClassLoaderIds[i], cachedClassFileBytes[i]);
            }
        }
    }
}
