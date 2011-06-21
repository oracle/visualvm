/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api.java;

import java.util.Set;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.modules.profiler.api.ProfilerSource;
import org.netbeans.modules.profiler.spi.java.AbstractJavaProfilerSource;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Bachorik
 */
final public class JavaProfilerSource extends ProfilerSource {
    final private AbstractJavaProfilerSource impl;
    
    public static interface ClassInfo {
        String getSimpleName();
        String getQualifiedName();
        String getVMName();
    }
    
    public static interface MethodInfo {
        String getClassName();
        String getName();
        String getSignature();
        String getVMName();
        
        boolean isExecutable();
    }
    
    final public static JavaProfilerSource createFrom(FileObject fo) {
        if (fo == null) return null;
        
        Lookup lkp = MimeLookup.getLookup(fo.getMIMEType());
        AbstractJavaProfilerSource impl = lkp.lookup(AbstractJavaProfilerSource.class);
        if (impl == null) {
            return null;
        }
        return new JavaProfilerSource(fo, impl);
    }
    
    private JavaProfilerSource(FileObject file, AbstractJavaProfilerSource impl) {
        super(file);
        this.impl = impl;
    }
    
    public boolean isTest() {
        return impl.isTest(getFile());
    }
    
    public boolean isApplet() {
        return impl.isApplet(getFile());
    }

    public ClassInfo getTopLevelClass() {
        return impl.getTopLevelClass(getFile());
    }
    
    public Set<ClassInfo> getMainClasses() {
        return impl.getMainClasses(getFile());
    }
    
    public Set<MethodInfo> getConstructors() {
        return impl.getConstructors(getFile());
    }
    
    public ClassInfo getEnclosingClass(final int position) {
        return impl.getEnclosingClass(getFile(), position);
    }
    
    public MethodInfo getEnclosingMethod(final int position) {
        return impl.getEnclosingMethod(getFile(), position);
    }
    
    public boolean isInstanceOf(String[] classNames, boolean allRequired) {
        return impl.isInstanceOf(getFile(), classNames, allRequired);
    }
    
    public boolean isInstanceOf(String className) {
        return impl.isInstanceOf(getFile(), className);
    }
    
    public boolean hasAnnotation(String[] annotationNames, boolean allRequired) {
        return impl.hasAnnotation(getFile(), annotationNames, allRequired);
    }
    
    public boolean hasAnnotation(String annotation) {
        return impl.hasAnnotation(getFile(), annotation);
    }
    
    public boolean isOffsetValid(int offset) {
        return impl.isOffsetValid(getFile(), offset);
    }
    
    public MethodInfo resolveMethodAtPosition(int position) {
        return impl.resolveMethodAtPosition(getFile(), position);
    }
    
    public ClassInfo resolveClassAtPosition(int position, boolean resolveField) {
        return impl.resolveClassAtPosition(getFile(), position, resolveField);
    }

    @Override
    public boolean isRunnable() {
        return isApplet() || isTest() || !getMainClasses().isEmpty();
    }
}
