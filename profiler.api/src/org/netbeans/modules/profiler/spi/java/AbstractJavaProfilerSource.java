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
package org.netbeans.modules.profiler.spi.java;

import java.util.Collections;
import java.util.Set;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.SourceMethodInfo;
import org.openide.filesystems.FileObject;

/**
 * An SPI for {@linkplain JavaProfilerSource} functionality providers
 * @author Jaroslav Bachorik
 */
public interface AbstractJavaProfilerSource {
    final public static AbstractJavaProfilerSource NULL = new AbstractJavaProfilerSource() {

        @Override
        public boolean isTest(FileObject fo) {
            return false;
        }

        @Override
        public boolean isApplet(FileObject fo) {
            return false;
        }

        @Override
        public SourceClassInfo getTopLevelClass(FileObject fo) {
            return null;
        }

        @Override
        public Set<SourceClassInfo> getClasses(FileObject fo) {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<SourceClassInfo> getMainClasses(FileObject fo) {
            return Collections.EMPTY_SET;
        }

        @Override
        public Set<SourceMethodInfo> getConstructors(FileObject fo) {
            return Collections.EMPTY_SET;
        }

        @Override
        public SourceClassInfo getEnclosingClass(FileObject fo, int position) {
            return null;
        }

        @Override
        public SourceMethodInfo getEnclosingMethod(FileObject fo, int position) {
            return null;
        }

        @Override
        public boolean isInstanceOf(FileObject fo, String[] classNames, boolean allRequired) {
            return false;
        }

        @Override
        public boolean isInstanceOf(FileObject fo, String className) {
            return false;
        }

        @Override
        public boolean hasAnnotation(FileObject fo, String[] annotationNames, boolean allRequired) {
            return false;
        }

        @Override
        public boolean hasAnnotation(FileObject fo, String annotation) {
            return false;
        }

        @Override
        public boolean isOffsetValid(FileObject fo, int offset) {
            return false;
        }

        @Override
        public SourceMethodInfo resolveMethodAtPosition(FileObject fo, int position) {
            return null;
        }

        @Override
        public SourceClassInfo resolveClassAtPosition(FileObject fo, int position, boolean resolveField) {
            return null;
        }
    };
    
    /**
     * @param fo The source file. Must not be NULL
     * @return Returns true if the source represents a junit tet
     */
    boolean isTest(FileObject fo);

    /**
     * @param fo The source file. Must not be NULL
     * @return Returns true if the source is a java applet
     */
    boolean isApplet(FileObject fo);

    /**
     * @param fo The source file. Must not be NULL
     * @return Returns {@linkplain ClassInfo} of a top level class
     */
    SourceClassInfo getTopLevelClass(FileObject fo);
    
    /**
     * Lists all top level classes contained in the source
     * @param fo The source file. Must not be NULL
     * @return Returns a set of {@linkplain ClassInfo} instances from a source
     */
    Set<SourceClassInfo> getClasses(FileObject fo);

    /**
     * Lists all main classes contained in the source
     * @param fo The source file. Must not be NULL
     * @return Returns a set of {@linkplain ClassInfo} instances from a source
     */
    Set<SourceClassInfo> getMainClasses(FileObject fo);
    
    /**
     * Lists all constructors contained in the source
     * @param fo The source file. Must not be NULL
     * @return Returns a set of {@linkplain MethodInfo} instances from the source
     */
    Set<SourceMethodInfo> getConstructors(FileObject fo);

    /**
     * Finds a class present on the given position in the source
     * @param fo The source file. Must not be NULL
     * @param position The position in the source
     * @return Returns a {@linkplain ClassInfo} for the class present on the given position
     */
    SourceClassInfo getEnclosingClass(FileObject fo, final int position);

    /**
     * Finds a method present on the given position in the source
     * @param fo The source file. Must not be NULL
     * @param position The position in the source
     * @return Returns a {@linkplain MethodInfo} for the method present on the given position
     */
    SourceMethodInfo getEnclosingMethod(FileObject fo, final int position);

    /**
     * Checks whether the source represents any or all of the provided superclasses/interfaces
     * @param fo The source file. Must not be NULL
     * @param classNames A list of required superclasses/interfaces
     * @param allRequired Require all(TRUE)/any(FALSE) provided superclasses/interfaces to match
     * @return Returns TRUE if the source represents any or all of the provided classes/interfaces
     */
    boolean isInstanceOf(FileObject fo, String[] classNames, boolean allRequired);

    /**
     * Checks whether the source represents the provided superclass/interface
     * @param fo The source file. Must not be NULL
     * @param className The required superclass/interface
     * @return Returns TRUE if the source represents the provided superclass/interface
     */
    boolean isInstanceOf(FileObject fo, String className);

    /**
     * Checks whether the source contains any/all provided annotations
     * @param fo The source file. Must not be NULL
     * @param annotationNames A list of required annotations
     * @param allRequired Require all(TRUE)/any(FALSE) provided annotations to match
     * @return Returns TRUE if the source contains any or all of the provided annotations
     */
    boolean hasAnnotation(FileObject fo, String[] annotationNames, boolean allRequired);

    /**
     * Checks whether the source contains the provided annotation
     * @param fo The source file. Must not be NULL
     * @param annotation The required annotation
     * @return Returns TRUE if the source contains the provided annotation
     */
    boolean hasAnnotation(FileObject fo, String annotation);

    /**
     * Is the given offset valid within a particular source
     * @param fo The source file. Must not be NULL
     * @param offset The offset to check
     * @return Returns TRUE if the offset is valid for the source
     */
    boolean isOffsetValid(FileObject fo, int offset);
    
    /**
     * Resolves a method at the given position<br/>
     * In order to resolve the method there must be the method definition or invocation
     * at the given position.
     * @param fo The source file. Must not be NULL
     * @param position The position to check for method definition or invocation
     * @return Returns the {@linkplain MethodInfo} for the method definition or invocation at the given position or NULL if there is none
     */
    SourceMethodInfo resolveMethodAtPosition(FileObject fo, int position);
    
    /**
     * Resolves a class at the given position<br/>
     * In order to resolve the class there must be the class definition or reference
     * at the given position.
     * @param fo The source file. Must not be NULL
     * @param position The position to check for class definition or reference
     * @param resolveField Should the class be resolved from a variable type too?
     * @return Returns the {@linkplain ClassInfo} for the class definition or reference at the given position or NULL if there is none
     */
    SourceClassInfo resolveClassAtPosition(FileObject fo, int position, boolean resolveField);
}
