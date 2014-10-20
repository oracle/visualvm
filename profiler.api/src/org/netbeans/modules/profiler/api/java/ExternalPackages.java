/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api.java;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import org.netbeans.lib.profiler.classfile.ClassFileParser;
import org.netbeans.lib.profiler.classfile.ClassInfo;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Jaroslv Bachorik
 */
final public class ExternalPackages {
    private static final Comparator<FileObject> pathComparator = new Comparator<FileObject>() {
        @Override
        public int compare(FileObject o1, FileObject o2) {
            return o1.getPath().compareTo(o2.getPath());
        }
    };
    
    private static class FilePackageInfo extends SourcePackageInfo {
        private FileObject root;
        private FileObject pkg;
        private Set<String> pkgsContent;
                
        public FilePackageInfo(FileObject root, FileObject pkg, Set<String> pkgsContent) {
            super(FileUtil.getRelativePath(root, pkg), FileUtil.getRelativePath(root, pkg).replace('/', '.'), SourcePackageInfo.Scope.SOURCE); // NOI18N
            this.pkg = pkg;
            this.root = root;
            this.pkgsContent = pkgsContent;
        }

        @Override
        public Collection<SourceClassInfo> getClasses() {
            Set<FileObject> clzs = new TreeSet(pathComparator);
            Enumeration<? extends FileObject> e = pkg.getData(false);
            while (e.hasMoreElements()) {
                FileObject clz = e.nextElement();
                if (clz.getExt().equalsIgnoreCase("class")) { // NOI18N
                    clzs.add(clz);
                }
            }
            ClassFileParser cfp = new ClassFileParser();
            Collection<SourceClassInfo> rslt = new ArrayList<SourceClassInfo>();
            for(final FileObject p : clzs) {
                if (p.getName().contains("$")) { // NOI18N
                    continue; // skipping inner classes
                }
                
                ClassInfo ci = new ClassInfo(p.getName(), 0) {
                    @Override
                    protected byte[] getClassFileBytes() throws IOException, ClassNotFoundException {
                        return p.asBytes();
                    }
                };
                try {
                    cfp.parseClassFile(p.asBytes(), ci);
                    rslt.add(new FileClassInfo(ci, root, p));
                } catch (IOException ex) {
                } catch (ClassFileParser.ClassFileReadException ex) {
                }
            }
            return rslt;
        }
        
        boolean hasClasses() {
            Enumeration<? extends FileObject> e = pkg.getData(false);
            while (e.hasMoreElements())
                if (e.nextElement().getExt().equalsIgnoreCase("class")) // NOI18N
                    return true;
            return false;
        }

        @Override
        public Collection<SourcePackageInfo> getSubpackages() {
            Set<FileObject> pkgs = new TreeSet(pathComparator);
            Enumeration<? extends FileObject> e = pkg.getFolders(false);
            while (e.hasMoreElements()) {
                pkgs.add(e.nextElement());
            }
            Collection<SourcePackageInfo> rslt = new ArrayList<SourcePackageInfo>();
            for(FileObject p : pkgs) {
                if (pkgsContent.contains(p.getPath())) {
                    rslt.add(new FilePackageInfo(root, p, pkgsContent));
                }
            }
            return rslt;
        }   
    }
    
    private static class FileClassInfo extends SourceClassInfo {
        private FileObject root, clazz;
        private ClassInfo ci;
        
        public FileClassInfo(ClassInfo ci, FileObject root, FileObject clazz) {
            super(clazz.getName(), FileUtil.getRelativePath(root, clazz).replace('/', '.').replace(".class", ""), FileUtil.getRelativePath(root, clazz).replace(".class", "")); // NOI18N
            this.ci = ci;
            this.clazz = clazz;
            this.root = root;
        }

        @Override
        public Set<SourceMethodInfo> getConstructors() {
            String[] names = ci.getMethodNames();
            Set<SourceMethodInfo> cts = new HashSet<SourceMethodInfo>();
            if (names != null) {
                for(int i=0;i<names.length;i++) {
                    if (names[i].equals("<init>")) { // NOI18N
                        cts.add(new FileMethodInfo(ci, i));
                    }
                }
            }
            return cts;
        }

        @Override
        public FileObject getFile() {
            return clazz;
        }

        @Override
        public Set<SourceClassInfo> getInnerClases() {
            Set<SourceClassInfo> rslt = new HashSet<SourceClassInfo>();
            ClassFileParser cfp = new ClassFileParser();
            Enumeration<? extends FileObject> siblings = clazz.getParent().getData(false);
            while (siblings.hasMoreElements()) {
                final FileObject sibling = siblings.nextElement();
                if (sibling.getName().contains("$") && sibling.getName().startsWith(clazz.getName()) && !sibling.getName().equals(clazz.getName())) { // NOI18N
                    ClassInfo ci = new ClassInfo(sibling.getName(), 0) {
                        @Override
                        protected byte[] getClassFileBytes() throws IOException, ClassNotFoundException {
                            return sibling.asBytes();
                        }
                    };
                    try {
                        cfp.parseClassFile(sibling.asBytes(), ci);
                        rslt.add(new FileClassInfo(ci, root, sibling));
                    } catch (IOException ex) {
                    } catch (ClassFileParser.ClassFileReadException ex) {
                    }
                }
            }
            return rslt;
        }

        @Override
        public Set<SourceClassInfo> getInterfaces() {
            Set<SourceClassInfo> ifcs = new HashSet<SourceClassInfo>();
            String[] iNames = ci.getInterfaceNames();
            if (iNames != null) {
                for(String name : iNames) {
                    ifcs.add(new PlainClassInfo(name));
                }
            }
            return ifcs;
        }

        @Override
        public Set<SourceMethodInfo> getMethods(boolean all) {
            String[] names = ci.getMethodNames();
            Set<SourceMethodInfo> cts = new HashSet<SourceMethodInfo>();
            if (names != null) {
                for(int i=0;i<names.length;i++) {
                    if (!names[i].equals("<init>")) { // NOI18N
                        cts.add(new FileMethodInfo(ci, i));
                    }
                }
            }
            return cts;
        }

        @Override
        public Set<SourceClassInfo> getSubclasses() {
            return Collections.EMPTY_SET;
        }

        @Override
        public SourceClassInfo getSuperType() {
            String superC = ci.getSuperclassName();
            if (superC == null) {
                superC = Object.class.getName();
            }
            return new PlainClassInfo(superC);
        }
        
    }
    
    private static class PlainClassInfo extends SourceClassInfo {
        PlainClassInfo(String className) {
            super(className, className, className);
        }
        @Override
        public FileObject getFile() {
            return null;
        }

        @Override
        public Set<SourceMethodInfo> getMethods(boolean all) {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<SourceClassInfo> getSubclasses() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<SourceClassInfo> getInnerClases() {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<SourceMethodInfo> getConstructors() {
            return Collections.EMPTY_SET;
        }

        @Override
        public SourceClassInfo getSuperType() {
            return new PlainClassInfo("");
        }

        @Override
        public Set<SourceClassInfo> getInterfaces() {
            return Collections.EMPTY_SET;
        }
    }
    
    private static class FileMethodInfo extends SourceMethodInfo {
        private static int getModifiers(ClassInfo ci, int mIndex) {
            int modifiers = 0;
            if (ci.isMethodAbstract(mIndex)) {
                modifiers += Modifier.ABSTRACT;
            }
            if (ci.isMethodPrivate(mIndex)) {
                modifiers += Modifier.PRIVATE;
            }
            if (ci.isMethodProtected(mIndex)) {
                modifiers += Modifier.PROTECTED;
            }
            if (ci.isMethodPublic(mIndex)) {
                modifiers += Modifier.PUBLIC;
            }
            if (ci.isMethodFinal(mIndex)) {
                modifiers += Modifier.FINAL;
            }
            if (ci.isMethodStatic(mIndex)) {
                modifiers += Modifier.STATIC;
            }
            if (ci.isMethodNative(mIndex)) {
                modifiers += Modifier.NATIVE;
            }
            return modifiers;
        }
        public FileMethodInfo(ClassInfo ci, int mIndex) {
            super(ci.getName().replace('/', '.'), ci.getMethodName(mIndex), ci.getMethodSignature(mIndex), ci.getMethodName(mIndex), false, getModifiers(ci, mIndex)); // NOI18N
        }
    }
    
    public static List<SourcePackageInfo> forPath(FileObject fo, boolean rec) {
        FileObject root = null;
        if (fo.getExt().equalsIgnoreCase("jar")) { // NOI18N
            if (FileUtil.isArchiveFile(fo)) {
                root = FileUtil.getArchiveRoot(fo);
            }
        } else if (fo.isFolder()) {
            root = fo;
        }
        if (root != null) {
            Queue<FileObject> stack = new ArrayDeque<FileObject>();
            Set<FileObject> packages = new TreeSet<FileObject>(pathComparator);
            Set<String> pkgsContent = new HashSet<String>();

            stack.offer(root);
            while (!stack.isEmpty()) {
                FileObject f = stack.poll();
                if (f != null) {
                    if (f.isData() && f.getExt().equalsIgnoreCase("class")) { // NOI18N
                        String path = f.getParent().getPath();
                        int i = path.lastIndexOf('/'); // NOI18N
                        while (i > -1) {
                            pkgsContent.add(path);
                            path = path.substring(0, i);
                            i = path.lastIndexOf('/'); // NOI18N
                        }
                        pkgsContent.add(path);
                    } else {
                        stack.addAll(Arrays.asList(f.getChildren()));
                    }
                }
            }
                    
            Enumeration<? extends FileObject> ePkgs = root.getFolders(false);
            while (ePkgs.hasMoreElements()) {
                FileObject pkg = ePkgs.nextElement();
                if (pkgsContent.contains(pkg.getPath())) {
                    packages.add(pkg);
                }
            }

            List<SourcePackageInfo> pkgis = new ArrayList<SourcePackageInfo>(packages.size());
            for (FileObject pkg : packages) {
                pkgis.add(new FilePackageInfo(root, pkg, pkgsContent));
            }
            
            if (rec) {
                Queue<SourcePackageInfo> _packages = new ArrayDeque(pkgis);
                pkgis.clear();
                while (!_packages.isEmpty()) {
                    FilePackageInfo pkg = (FilePackageInfo)_packages.poll();
                    if (pkg.hasClasses()) pkgis.add(pkg);
                    _packages.addAll(pkg.getSubpackages());
                }
            }

            return pkgis;
        }
        
        return Collections.EMPTY_LIST;
    }
}
