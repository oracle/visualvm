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

import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.instrumentation.BadLocationException;
import org.netbeans.lib.profiler.utils.FileOrZipEntry;
import org.netbeans.lib.profiler.utils.MiscUtils;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;


/**
 * A collection of several static methods for general class file reading functionality. Allows to set
 * a class path, read a class from class path, generate a class that does not have a .class file (such
 * as an array class), etc. It also keeps track of classes ever loaded by it, and allows one to iterate
 * over these classes.
 *
 * @author Tomas Hurka
 * @author Misha Dmitirev
 */
public abstract class ClassRepository implements CommonConstants {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // ------------------------ Method-class-source related stuff --------------------------------
    public static class CodeRegionBCI {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public String className;
        public String methodName;
        public String methodSignature;
        public int bci0;
        public int bci1;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CodeRegionBCI(String className, String methodName, String methodSignature, int bci0, int bci1) {
            this.className = className;
            this.methodName = methodName;
            this.methodSignature = methodSignature;
            this.bci0 = bci0;
            this.bci1 = bci1;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String toString() {
            return "CodeRegionBCI [" // NOI18N
                   + "className: " + className // NOI18N
                   + ", methodName: " + methodName // NOI18N
                   + ", methodSignature: " + methodSignature // NOI18N
                   + ", bci0: " + bci0 // NOI18N
                   + ", bci1: " + bci1 // NOI18N
                   + "]"; // NOI18N
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // The below class file location signals to ClassFileCache that the class file should have been already supplied by the VM.
    static final String LOCATION_VMSUPPLIED = "<VM_SUPPLIED>"; // NOI18N
    private static ClassPath classPath;
    private static Hashtable classes;
    private static Set notFoundClasses;
    private static Map definingClassLoaderMap;

    static {
        clearCache();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static List getAllClassVersions(String className) {
        className = className.replace('.', '/').intern(); // NOI18N

        Object entry = classes.get(className);

        if (entry != null) { // A single class or placeholder, or a group of them for this name, exists

            List ret = new ArrayList();

            if (entry instanceof BaseClassInfo) {
                ret.add(entry);
            } else {
                ret = ((SameNameClassGroup) entry).getAll();
            }

            return ret;
        } else {
            return null;
        }
    }

    public static Enumeration getClassEnumerationWithAllVersions() {
        class ClassesEnumeration implements Enumeration {
            private Enumeration baseEnum;
            private Object nextElement;
            private List classes;
            private int idx;

            ClassesEnumeration(Enumeration baseEnum) {
                this.baseEnum = baseEnum;
            }

            public boolean hasMoreElements() {
                if (nextElement == null) {
                    return baseEnum.hasMoreElements();
                } else {
                    return true;
                }
            }

            public Object nextElement() {
                if (nextElement != null) {
                    Object ret = nextElement;
                    idx++;

                    if (idx == classes.size()) {
                        nextElement = null;
                    } else {
                        nextElement = classes.get(idx);
                    }

                    return ret;
                } else {
                    Object next = baseEnum.nextElement();

                    if (next instanceof SameNameClassGroup) {
                        SameNameClassGroup g = (SameNameClassGroup) next;
                        classes = g.getAll();
                        next = classes.get(0);

                        if (classes.size() > 1) {
                            idx = 1;
                            nextElement = classes.get(1);
                        } else {
                            classes = null;
                        }
                    }

                    return next;
                }
            }
        }

        return new ClassesEnumeration(classes.elements());
    }

    public static ClassPath getClassPath() {
        return classPath;
    }

    /**
     * Returns names of all classes that can be located on the given classpath.
     * Since this method performs directory scanning, it is recommended to call it once and cache the results.
     */
    public static List getClassesOnClasspath(List classPathElementList) { // TODO CHECK: unused method

        List list = new ArrayList();
        list.addAll(classPathElementList);

        List res = new ArrayList();

        for (Iterator e = list.iterator(); e.hasNext();) {
            String dirOrJar = (String) e.next();

            if (!(dirOrJar.endsWith(".jar") || dirOrJar.endsWith(".zip"))) // NOI18N
             {
                MiscUtils.getAllClassesInDir(dirOrJar, "", true, res); // NOI18N
            } else {
                MiscUtils.getAllClassesInJar(dirOrJar, true, res);
            }
        }

        return res;
    }

    public static CodeRegionBCI getMethodForSourceRegion(ClassInfo clazz, int startLine, int endLine)
                                                  throws ClassNotFoundException, IOException, BadLocationException {
        if (startLine > endLine) {
            return null; // Just in case...
        }

        int[] idxAndBCI0 = clazz.methodIdxAndBestBCIForLineNo(startLine);
        int methodIdx = idxAndBCI0[0];

        if (methodIdx >= 0) {
            String methodName = clazz.getMethodNames()[methodIdx];

            if ((methodName == "<init>") || (methodName == "<clinit>")) { // NOI18N
                                                                          // See the comment in ClassInfo.methodIdxAndBestBCIForLineNo() regarding initializers scattered about the class text.
                                                                          // Check if a method in a nested class matches the same spot in the source code.

                CodeRegionBCI res = getMethodForSourceRegionInNestedClasses(clazz, startLine, endLine);

                if (res != null) {
                    return res;
                }
            }

            int[] minAndMaxLines = clazz.getMinAndMaxLinesForMethod(methodIdx);

            if (endLine <= minAndMaxLines[1]) {
                endLine++; // That's because we will need to inject code after the *last bytecode corresponding to endLine*
            }

            int[] idxAndBCI1 = clazz.methodIdxAndBestBCIForLineNo(endLine);

            // Now let's check if start and end lines are within the same method.
            // If the end line is definitely within some other method, it's an error and we return.
            // However, it may just cover one or more of '}'s in the end of this method, and these lines
            // are just not within this method's line number table. If so, assume that the end line is
            // the last line of the this method.
            if (methodIdx != idxAndBCI1[0]) {
                if (idxAndBCI1[0] != -1) { // Definitely this line belongs to some other method

                    return null;
                } else { // Couldn't find the line - assume it's the last line of the same method
                    idxAndBCI1[0] = methodIdx;

                    // Need to find the bci of the last instruction in this method. It can only be a "return" or 'goto' ('goto_w').
                    // In either case, we should put the call before this instruction, since it would make no sense after it.
                    byte[] codeBytes = clazz.getMethodBytecode(methodIdx);
                    idxAndBCI1[1] = ClassInfo.findPreviousBCI(codeBytes, codeBytes.length);
                }
            }

            // Now here is another issue. It appears that at least "while() { }" is effectively compiled as "do..while",
            // i.e. the condition check is located after the block, not before. Which leads to the problem: if the
            // user points at the line with "while" as a first region line, the "exact" bytecode offset for this particular
            // line may be greater than the offset for the next line after "while"! This leads to incorrect measurements
            // results at best and to the JVM crash during bytecode oop map generation at worst. To handle this, we
            // currently use heuristics which just looks up the line with the smallest bci in between startLine and endLine.
            int bestBCI0 = idxAndBCI0[1];

            for (int lineNo = startLine + 1; lineNo < (endLine - 1); lineNo++) {
                int otherBestBCI0 = clazz.bciForMethodAndLineNo(methodIdx, lineNo);

                if (otherBestBCI0 < bestBCI0) {
                    bestBCI0 = otherBestBCI0;
                }
            }

            // Finally, check if the last bci is of the "goto" opcode. If so, we should actually return the bci of the
            // previous opcode, since injecting code probe right after the "goto", as it will be done if no measures are
            // taken, makes no sense. This code will not work as intended, and most likely will be just unreachable.
            // THIS IS INCORRECT. We inject code *before*, not after, the given bytecode. So it can be goto as well.
            //idxAndBCI1[1] = clazz.checkIfAtGoTo(methodIdx, idxAndBCI1[1]);
            return new CodeRegionBCI(clazz.getName(), clazz.getMethodNames()[methodIdx], clazz.getMethodSignatures()[methodIdx],
                                     bestBCI0, idxAndBCI1[1]);
        } else if (methodIdx == -2) { // No line number tables in this class
            throw new BadLocationException("Class does not have source line number tables.\nRecompile it with appropriate options."); // NOI18N
        }

        // Suitable method not found. Look at nested classes, if there are any.
        return getMethodForSourceRegionInNestedClasses(clazz, startLine, endLine);
    }

    /*  public static int[] getMinAndMaxLinesForMethod(String className, String methodName, String methodSignature)  // TODO CHECK: unused method
       throws IOException, ClassFormatError {
       ClassInfo clazz = lookupClassOnAllPaths(className);
       String methodNames[] = clazz.getMethodNames();
       String methodSignatures[] = clazz.getMethodSignatures();
       methodName = methodName.intern();
       methodSignature = methodSignature.intern();
       int idx = clazz.getMethodIndex(methodName, methodSignature);
       if (idx != -1)
         return clazz.getMinAndMaxLinesForMethod(idx);
       else
         return null;
       }
     */
    public static CodeRegionBCI getMethodMinAndMaxBCI(ClassInfo clazz, String methodName, String methodSignature) {
        methodName = methodName.intern();
        methodSignature = methodSignature.intern();

        int idx = clazz.getMethodIndex(methodName, methodSignature);

        if (idx == -1) {
            return null;
        }

        // Note that className and clazz.getName() may be different, e.g. if className is specified as "x.y.Outer.Inner",
        // when the correct format understood by the rest of JFluid is "x.y.Outer$Inner".
        return new CodeRegionBCI(clazz.getName(), methodName, methodSignature, 0, clazz.getMethodBytecode(idx).length - 1);
    }

    public static void addPlaceholder(PlaceholderClassInfo pci) {
        BaseClassInfo singleExistingClazzOrPCI = null;
        SameNameClassGroup classGroup = null;
        String className = pci.getName();

        Object entry = classes.get(className);

        if (entry != null) { // A single class or placeholder, or a group of them for this name, exists

            if (entry instanceof BaseClassInfo) {
                singleExistingClazzOrPCI = (BaseClassInfo) entry;
                classGroup = new SameNameClassGroup();
                classGroup.add(singleExistingClazzOrPCI);
                classGroup.add(pci);
                classes.put(className, classGroup);
            } else { // entry is a SameNameClassGroup
                classGroup = (SameNameClassGroup) entry;
                classGroup.add(pci);
            }
        } else { // An entry with this name doesn't exist
            classes.put(className, pci);
        }
    }

    /** Adds a VM-supplied class file to the class file cache, but not to this repository's hashtable yet. */
    public static void addVMSuppliedClassFile(String className, int classLoaderId, byte[] buf) {
        className = className.replace('.', '/').intern(); // NOI18N
        ClassFileCache.getDefault().addVMSuppliedClassFile(className, classLoaderId, buf);
    }

    /** Should be called after profiling finishes to cleanup any static data, close opened files, etc. */
    public static void cleanup() {
        clearCache();

        if (classPath != null) {
            classPath.close();
            classPath = null;
        }
    }

    /** Will reset any cached data, will not reset data pertinent to session in progress */
    public static void clearCache() {
        classes = new Hashtable();
        ClassFileCache.resetDefaultCache();
        notFoundClasses = new HashSet();
        definingClassLoaderMap = new HashMap();
    }

    /**
     * This is the ClassRepository internal class path initialization method. The class path is initialized to the
     * combination of the running VM's boot, extension and main class paths (if they are available; otherwise only
     * the main path is obtained from the tool's settings), plus the secondary class path from the settings.
     *
     * @param workingDir            working directory, needed in case the given paths are in the local form
     * @param classPaths            the 3 elements should be the user, extension, and boot class paths, respectively
     */
    public static void initClassPaths(String workingDir, String[] classPaths) {
        List userClassPathElementList = MiscUtils.getPathComponents(classPaths[0], true, workingDir);
        List bootClassPathElementList = MiscUtils.getPathComponents(classPaths[2], true, workingDir);

        String extPath = classPaths[1];
        List extClassPathElementList = new ArrayList();

        // Extension class path needs special handling, since it consists of directories, which contain .jars
        // So we need to find all these .jars in all these dirs and add them to extClassPathElementList
        List dirs = MiscUtils.getPathComponents(extPath, true, workingDir);

        for (Iterator e = dirs.iterator(); e.hasNext();) {
            File extDir = new File((String) e.next());
            String[] extensions = extDir.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        name = name.toLowerCase();

                        return name.endsWith(".zip") || name.endsWith(".jar"); // NOI18N
                    }
                });

            if (extensions == null) {
                continue;
            }

            for (int i = 0; i < extensions.length; i++) {
                String extJar = extDir.getAbsolutePath() + File.separatorChar + extensions[i];
                List allJarComponents = MiscUtils.getPathComponents(extJar,true,workingDir);
                extClassPathElementList.addAll(allJarComponents);
            }
        }

        List list = new ArrayList();
        list.addAll(bootClassPathElementList);
        list.addAll(extClassPathElementList);
        list.addAll(userClassPathElementList);

        StringBuilder buf = new StringBuilder();

        for (Iterator e = list.iterator(); e.hasNext();) {
            buf.append((String) e.next());

            if (e.hasNext()) {
                buf.append(File.pathSeparatorChar);
            }
        }

        classPath = new ClassPath(buf.toString(), true);

        notFoundClasses = new HashSet();
    }

    /**
     * Lookup a class in the class repository. If it's not there, look it up on the classpath (for classes with 0 loader)
     * or in the cache of VM-supplied classes. If the class is not found anywhere, reports this and returns null.
     * Guaranteed to return a real class or null, but not a placeholder. Should not be called for special (array)
     * classes - there is lookupSpecialClass() for that.
     */
    public static DynamicClassInfo lookupClass(String className, int classLoaderId)
                                        throws IOException, ClassFormatError {
        return lookupClass(className, classLoaderId, true);
    }

    /**
     * Lookup a class in the class repository. If it's not there, don't bother checking the classpath etc. - just
     * return an instance of PlaceholderClassInfo. The rationale is that we may not ever need the real class for
     * className; and when we need it, lookupClass() above will deliver it.
     */
    public static BaseClassInfo lookupClassOrCreatePlaceholder(String className, int classLoaderId) {
        BaseClassInfo singleExistingClazzOrPCI = null;
        BaseClassInfo clazzOrPCI = null;
        SameNameClassGroup classGroup = null;
        className = className.replace('.', '/').intern(); // NOI18N

        Object entry = classes.get(className);

        if (entry != null) { // A single class or placeholder, or a group of them for this name, exists

            if (entry instanceof BaseClassInfo) {
                singleExistingClazzOrPCI = (BaseClassInfo) entry;
                clazzOrPCI = SameNameClassGroup.checkForCompatibility(singleExistingClazzOrPCI, classLoaderId);
            } else { // entry is a SameNameClassGroup
                classGroup = (SameNameClassGroup) entry;
                clazzOrPCI = classGroup.findCompatibleClass(classLoaderId);
            }

            if (clazzOrPCI != null) { // Found compatible class or placeholder

                return clazzOrPCI;
            } else { // Non-null entry for this class name, but no compatible class or placeholder
                clazzOrPCI = new PlaceholderClassInfo(className, classLoaderId);

                if (classGroup != null) {
                    classGroup.add(clazzOrPCI);
                } else { // There is already a single incompatible class or placeholder in classes - create a new class group
                    classGroup = new SameNameClassGroup();
                    classGroup.add(singleExistingClazzOrPCI);
                    classGroup.add(clazzOrPCI);
                    classes.put(className, classGroup);
                }

                return clazzOrPCI;
            }
        } else { // An entry with this name doesn't exist
            clazzOrPCI = new PlaceholderClassInfo(className, classLoaderId);
            classes.put(className, clazzOrPCI);

            return clazzOrPCI;
        }
    }

    /**
     * Lookup a class in the class repository, only among those currently loaded by the VM.
     * If there is no loaded class and allowExistingPlaceholder is true, also check for an existing placeholders.
     * Returns either a loaded class, or if allowed an existing placeholder, or null, but not a new placeholder.
     */
    public static BaseClassInfo lookupLoadedClass(String className, int classLoaderId, boolean allowExistingPlaceholder) {
        BaseClassInfo singleExistingClazzOrPCI = null;
        BaseClassInfo clazzOrPCI = null;
        className = className.replace('.', '/').intern(); // NOI18N

        Object entry = classes.get(className);

        if (entry != null) { // A single class or placeholder, or a group of them for this name, exists

            if (entry instanceof BaseClassInfo) {
                singleExistingClazzOrPCI = (BaseClassInfo) entry;
                clazzOrPCI = SameNameClassGroup.checkForCompatibility(singleExistingClazzOrPCI, classLoaderId);
            } else { // entry is a SameNameClassGroup

                SameNameClassGroup classGroup = (SameNameClassGroup) entry;
                clazzOrPCI = classGroup.findCompatibleClass(classLoaderId);
            }

            if (clazzOrPCI != null) { // Found compatible class or placeholder

                if (!(clazzOrPCI instanceof PlaceholderClassInfo)) {
                    return clazzOrPCI;
                } else if (allowExistingPlaceholder) {
                    return clazzOrPCI;
                }
            }
        }

        return null;
    }

    /**
     * Used only for special classes, such as array classes, that don't have a .class file on the class path. If a class
     * with the given name does not exist, a BaseClassInfo is created for it immediately.
     */
    public static BaseClassInfo lookupSpecialClass(String className) {
        if (className.indexOf('.') != -1) { // NOI18N
            className = className.replace('.', '/').intern(); // NOI18N
        }

        BaseClassInfo clazz = (BaseClassInfo) classes.get(className);

        if (clazz == null) {
            clazz = new BaseClassInfo(className, 0); // For now, we don't distinguish between Object array classes for different loaders (if such a thing exists)
            classes.put(className, clazz);
        }

        return clazz;
    }

    static int getDefiningClassLoaderId(String className, int classLoaderId) {
        String classId = className + "#" + classLoaderId; // NOI18N
        Integer loaderInt = (Integer) definingClassLoaderMap.get(classId);

        if (loaderInt != null) {
            return loaderInt.intValue();
        }

        int loader = -1;

        try {
            loader = TargetAppRunner.getDefault().getProfilerClient().getDefiningClassLoaderId(className, classLoaderId);
        } catch (Exception ex) {
            // Don't bother about reporting an exception - somebody will do that later
        }

        definingClassLoaderMap.put(classId, Integer.valueOf(loader));

        return loader;
    }

    private static CodeRegionBCI getMethodForSourceRegionInNestedClasses(ClassInfo clazz, int startLine, int endLine)
        throws ClassNotFoundException, IOException, ClassFormatError {
        String className = clazz.getName();
        String[] nestedClassNames = clazz.getNestedClassNames();
        int classNameLen = className.length();

        if (nestedClassNames != null) {
            for (int i = 0; i < nestedClassNames.length; i++) {
                if (!(nestedClassNames[i].startsWith(className) && (nestedClassNames[i].length() > classNameLen))) {
                    continue;
                }

                try {
                    ClassInfo nestedClass = lookupClass(nestedClassNames[i], clazz.getLoaderId());

                    if (nestedClass != null) {
                        CodeRegionBCI res = getMethodForSourceRegion(nestedClass, startLine, endLine);

                        if (res != null) {
                            return res;
                        }
                    }
                } catch (BadLocationException ex) {
                    // Clearly if we got into this method, there was a line number table in the upper level class. So the BadLocationException
                    // that can only be thrown if no line number table is found in this particular nested class is a bogus and misleading.
                    return null;
                }
            }
        }

        return null;
    }

    private static DynamicClassInfo checkForVMSuppliedClass(String className, int classLoaderId)
                                                     throws IOException, ClassFormatError {
        int realLoaderId = ClassFileCache.getDefault().hasVMSuppliedClassFile(className, classLoaderId);

        if (realLoaderId != -1) {
            String classFileLoc = (LOCATION_VMSUPPLIED + realLoaderId).intern();
            return new DynamicClassInfo(className, classLoaderId, classFileLoc);
        } else {
            return null;
        }
    }

    private static DynamicClassInfo lookupClass(String className, int classLoaderId, boolean reportIfNotFound)
                                         throws IOException, ClassFormatError {
        BaseClassInfo singleExistingClazzOrPCI = null;
        BaseClassInfo clazzOrPCI = null;
        SameNameClassGroup classGroup = null;
        className = className.replace('.', '/').intern(); // NOI18N

        Object entry = classes.get(className);

        if (entry != null) { // A single class or placeholder, or a group of them for this name, exists

            if (entry instanceof BaseClassInfo) {
                singleExistingClazzOrPCI = (BaseClassInfo) entry;
                clazzOrPCI = SameNameClassGroup.checkForCompatibility(singleExistingClazzOrPCI, classLoaderId);
            } else { // entry is a SameNameClassGroup
                classGroup = (SameNameClassGroup) entry;
                clazzOrPCI = classGroup.findCompatibleClass(classLoaderId);
            }

            if (clazzOrPCI != null) { // Found compatible class or placeholder

                if (!(clazzOrPCI instanceof PlaceholderClassInfo)) {
                    return (DynamicClassInfo) clazzOrPCI;
                } else { // Found a compatible placeholder

                    PlaceholderClassInfo pci = (PlaceholderClassInfo) clazzOrPCI;
                    DynamicClassInfo clazz = tryLoadRealClass(className, classLoaderId, reportIfNotFound);

                    if (clazz != null) { // Found a real class for this placeholder
                        pci.transferDataIntoRealClass(clazz);

                        if (classGroup != null) {
                            classGroup.replace(pci, clazz);
                        } else {
                            classes.put(className, clazz);
                        }

                        return clazz;
                    } else {
                        return null; // Didn't find a real class for this placeholder
                    }
                }
            } else { // Non-null entry for this class name, but no compatible class or placeholder

                DynamicClassInfo clazz = tryLoadRealClass(className, classLoaderId, reportIfNotFound);

                if (clazz != null) { // Managed to load a right class

                    if (classGroup != null) {
                        classGroup.add(clazz);
                    } else { // There is already a single incompatible class or placeholder in classes - create a new class group
                        classGroup = new SameNameClassGroup();
                        classGroup.add(singleExistingClazzOrPCI);
                        classGroup.add(clazz);
                        classes.put(className, classGroup);
                    }

                    return clazz;
                } else {
                    return null; // Could not load a right class
                }
            }
        } else { // An entry with this name doesn't exist

            DynamicClassInfo clazz = tryLoadRealClass(className, classLoaderId, reportIfNotFound);

            if (clazz != null) {
                classes.put(className, clazz);

                return clazz;
            } else {
                return null;
            }
        }
    }

    private static DynamicClassInfo tryLoadRealClass(String className, int classLoaderId, boolean reportIfNotFound)
                                              throws IOException, ClassFormatError {
        DynamicClassInfo clazz = null;
        int loader = classLoaderId;

        do {
            // In case of remote profiling, even system classes, that we otherwise can look up on disk locally, are
            // supplied by the VM. That's why we always call checkForVMSuppliedClass first.
            clazz = checkForVMSuppliedClass(className, loader);

            if (clazz == null) {
                if (((loader == 0) || (loader == -1)) && (classPath != null)) { // sanity check; to prevent NPE in case the classPath hasn't been initialized (shouldn't happen anyway)
                    clazz = classPath.getClassInfoForClass(className, loader);
                }
            }

            if ((clazz != null) || (loader == 0)) {
                break;
            }

            // Try parent loader - in some cases a class can be initially requested with the loader of its subclass
            loader = ClassLoaderTable.getParentLoader(loader);
        } while (loader >= 0);

        if (clazz == null) {
            // In some cases, the class loader graph for the app may be a non-tree structure, i.e. one class loader may delegate
            // not just to its parent loader, but to some other loader(s) as well. In that case, our last resort is to ask the
            // initiating loader itself for this class, and then get its defining loader.
            loader = getDefiningClassLoaderId(className, classLoaderId);

            if (loader != -1) {
                clazz = checkForVMSuppliedClass(className, loader); // See above about remote profiling

                if (clazz == null) {
                    if (loader == 0) {
                        clazz = classPath.getClassInfoForClass(className, loader);
                    }
                }
            }
        }

        if ((clazz == null) && reportIfNotFound) {
            if (!notFoundClasses.contains(className)) {
                MiscUtils.printWarningMessage("class " + className + ", ldr = " + classLoaderId + " not found anywhere"); // NOI18N
                notFoundClasses.add(className);
            }
        }

        return clazz;
    }

    //----------------------------------- Debugging -----------------------------------

    /*
       private static void dumpLineTable(Method method) {
         LineNumberTable lnt = method.getLineNumberTable();
         if (lnt == null) return;
         LineNumber[] lns = lnt.getLineNumberTable();
         System.out.println("Line number table for " + method.getName() + "." + method.getSignature());
         for (int i = 0; i < lns.length; i++) {
           System.out.println(lns[i].getLineNumber() + " " + lns[i].getStartPC());
         }
       }
     */
}
