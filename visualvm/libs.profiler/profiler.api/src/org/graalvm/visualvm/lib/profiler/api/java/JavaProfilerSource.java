/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.api.java;

import java.util.Set;
import org.graalvm.visualvm.lib.profiler.api.ProfilerSource;
import org.graalvm.visualvm.lib.profiler.spi.java.AbstractJavaProfilerSource;
import org.netbeans.api.editor.mimelookup.MimeLookup;
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
     * @return Returns {@linkplain SourceClassInfo} of a top level class
     */
    public SourceClassInfo getTopLevelClass() {
        return impl.getTopLevelClass(getFile());
    }

    /**
     * List all top level classes contained in the source
     * @return Returns a set of {@linkplain SourceClassInfo} instances from a source
     */
    public Set<SourceClassInfo> getClasses() {
        return impl.getClasses(getFile());
    }
    
    /**
     * Lists all main classes contained in the source
     * @return Returns a set of {@linkplain SourceClassInfo} instances from a source
     */
    public Set<SourceClassInfo> getMainClasses() {
        return impl.getMainClasses(getFile());
    }
    
    /**
     * Lists all constructors contained in the source
     * @return Returns a set of {@linkplain SourceMethodInfo} instances from the source
     */
    public Set<SourceMethodInfo> getConstructors() {
        return impl.getConstructors(getFile());
    }
    
    /**
     * Finds a class present on the given position in the source
     * @param position The position in the source
     * @return Returns a {@linkplain SourceClassInfo} for the class present on the given position
     */
    public SourceClassInfo getEnclosingClass(final int position) {
        return impl.getEnclosingClass(getFile(), position);
    }
    
    /**
     * Finds a method present on the given position in the source
     * @param position The position in the source
     * @return Returns a {@linkplain SourceMethodInfo} for the method present on the given position
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
     * Resolves a method at the given position<br>
     * In order to resolve the method there must be the method definition or invocation
     * at the given position.
     * @param position The position to check for method definition or invocation
     * @return Returns the {@linkplain SourceMethodInfo} for the method definition or invocation at the given position or NULL if there is none
     */
    public SourceMethodInfo resolveMethodAtPosition(int position) {
        return impl.resolveMethodAtPosition(getFile(), position);
    }

    /**
     * Resolves a class at the given position<br>
     * In order to resolve the class there must be the class definition or reference
     * at the given position.
     * @param position The position to check for class definition or reference
     * @param resolveField Should the class be resolved from a variable type too?
     * @return Returns the {@linkplain SourceClassInfo} for the class definition or reference at the given position or NULL if there is none
     */
    public SourceClassInfo resolveClassAtPosition(int position, boolean resolveField) {
        return impl.resolveClassAtPosition(getFile(), position, resolveField);
    }

    @Override
    public boolean isRunnable() {
        return isApplet() || isTest() || !getMainClasses().isEmpty();
    }
}
