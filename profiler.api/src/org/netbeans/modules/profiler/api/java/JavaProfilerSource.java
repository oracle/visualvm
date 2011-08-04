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
 * Java source file representation
 * 
 * @author Jaroslav Bachorik
 */
final public class JavaProfilerSource extends ProfilerSource {
    final private AbstractJavaProfilerSource impl;
    
    /**
     * Factory method for obtaining a {@linkplain JavaProfilerSource} from a file or NULL
     * @return Returns a {@linkplain JavaProfilerSource} instance or NULL
     */
    public static JavaProfilerSource createFrom(FileObject fo) {
        if (fo == null || !fo.isValid()) return null;
        
        Lookup lkp = MimeLookup.getLookup(fo.getMIMEType());
        AbstractJavaProfilerSource impl = lkp.lookup(AbstractJavaProfilerSource.class);
        if (impl == null && fo.isData() && fo.hasExt("java")) // NOI18N
            impl = Lookup.getDefault().lookup(AbstractJavaProfilerSource.class);
        if (impl == null) {
            return null;
        }
        return new JavaProfilerSource(fo, impl);
    }
    
    private JavaProfilerSource(FileObject file, AbstractJavaProfilerSource impl) {
        super(file);
        this.impl = impl;
    }
    
    /**
     * 
     * @return Returns true if the source represents a junit tet
     */
    public boolean isTest() {
        return impl.isTest(getFile());
    }
    
    /**
     * 
     * @return Returns true if the source is a java applet
     */
    public boolean isApplet() {
        return impl.isApplet(getFile());
    }

    /**
     * 
     * @return Returns {@linkplain ClassInfo} of a top level class
     */
    public SourceClassInfo getTopLevelClass() {
        return impl.getTopLevelClass(getFile());
    }
    
    /**
     * List all top level classes contained in the source
     * @return Returns a set of {@linkplain ClassInfo} instances from a source
     */
    public Set<SourceClassInfo> getClasses() {
        return impl.getClasses(getFile());
    }
    
    /**
     * Lists all main classes contained in the source
     * @return Returns a set of {@linkplain ClassInfo} instances from a source
     */
    public Set<SourceClassInfo> getMainClasses() {
        return impl.getMainClasses(getFile());
    }
    
    /**
     * Lists all constructors contained in the source
     * @return Returns a set of {@linkplain MethodInfo} instances from the source
     */
    public Set<SourceMethodInfo> getConstructors() {
        return impl.getConstructors(getFile());
    }
    
    /**
     * Finds a class present on the given position in the source
     * @param position The position in the source
     * @return Returns a {@linkplain ClassInfo} for the class present on the given position
     */
    public SourceClassInfo getEnclosingClass(final int position) {
        return impl.getEnclosingClass(getFile(), position);
    }
    
    /**
     * Finds a method present on the given position in the source
     * @param position The position in the source
     * @return Returns a {@linkplain MethodInfo} for the method present on the given position
     */
    public SourceMethodInfo getEnclosingMethod(final int position) {
        return impl.getEnclosingMethod(getFile(), position);
    }
    
    /**
     * Checks whether the source represents any or all of the provided superclasses/interfaces
     * @param classNames A list of required superclasses/interfaces
     * @param allRequired Require all(TRUE)/any(FALSE) provided superclasses/interfaces to match
     * @return Returns TRUE if the source represents any or all of the provided classes/interfaces
     */
    public boolean isInstanceOf(String[] classNames, boolean allRequired) {
        return impl.isInstanceOf(getFile(), classNames, allRequired);
    }
    
    /**
     * Checks whether the source represents the provided superclass/interface
     * @param className The required superclass/interface
     * @return Returns TRUE if the source represents the provided superclass/interface
     */
    public boolean isInstanceOf(String className) {
        return impl.isInstanceOf(getFile(), className);
    }
    
    /**
     * Checks whether the source contains any/all provided annotations
     * @param annotationNames A list of required annotations
     * @param allRequired Require all(TRUE)/any(FALSE) provided annotations to match
     * @return Returns TRUE if the source contains any or all of the provided annotations
     */
    public boolean hasAnnotation(String[] annotationNames, boolean allRequired) {
        return impl.hasAnnotation(getFile(), annotationNames, allRequired);
    }
    
    /**
     * Checks whether the source contains the provided annotation
     * @param annotation The required annotation
     * @return Returns TRUE if the source contains the provided annotation
     */
    public boolean hasAnnotation(String annotation) {
        return impl.hasAnnotation(getFile(), annotation);
    }
    
    /**
     * Is the given offset valid within a particular source
     * @param offset The offset to check
     * @return Returns TRUE if the offset is valid for the source
     */
    public boolean isOffsetValid(int offset) {
        return impl.isOffsetValid(getFile(), offset);
    }
    
    /**
     * Resolves a method at the given position<br/>
     * In order to resolve the method there must be the method definition or invocation
     * at the given position.
     * @param position The position to check for method definition or invocation
     * @return Returns the {@linkplain MethodInfo} for the method definition or invocation at the given position or NULL if there is none
     */
    public SourceMethodInfo resolveMethodAtPosition(int position) {
        return impl.resolveMethodAtPosition(getFile(), position);
    }

    /**
     * Resolves a class at the given position<br/>
     * In order to resolve the class there must be the class definition or reference
     * at the given position.
     * @param position The position to check for class definition or reference
     * @param resolveField Should the class be resolved from a variable type too?
     * @return Returns the {@linkplain ClassInfo} for the class definition or reference at the given position or NULL if there is none
     */
    public SourceClassInfo resolveClassAtPosition(int position, boolean resolveField) {
        return impl.resolveClassAtPosition(getFile(), position, resolveField);
    }

    @Override
    public boolean isRunnable() {
        return isApplet() || isTest() || !getMainClasses().isEmpty();
    }
}
