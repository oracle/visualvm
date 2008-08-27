/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.categories;

import org.netbeans.modules.profiler.categories.definitions.PackageCategoryDefinition;
import org.netbeans.modules.profiler.categories.definitions.SubtypeCategoryDefinition;
import org.netbeans.modules.profiler.categories.definitions.SingleTypeCategoryDefinition;
import org.netbeans.modules.profiler.categories.definitions.CustomCategoryDefinition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ElementUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.marker.ClassMarker;
import org.netbeans.lib.profiler.marker.CompositeMarker;
import org.netbeans.lib.profiler.marker.Marker;
import org.netbeans.lib.profiler.marker.MethodMarker;
import org.netbeans.lib.profiler.marker.PackageMarker;
import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.lib.profiler.results.cpu.marking.MarkMapping;
import org.netbeans.modules.profiler.AbstractProjectTypeProfiler;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jaroslav Bachorik
 */
public class MarkerProcessor extends  CategoryDefinitionProcessor implements Marker {
    private final static Logger LOGGER = Logger.getLogger(MarkerProcessor.class.getName());
    
    private MethodMarker mMarker = new MethodMarker();
    private ClassMarker cMarker = new ClassMarker();
    private PackageMarker pMarker = new PackageMarker();
    private CompositeMarker cmMarker = new CompositeMarker();

    private Project project;

    private ClasspathInfo cpInfo;
    private JavaSource js;
    public MarkerProcessor(Project project) {
        this.project = project;
        this.cpInfo = ProjectUtilities.getClasspathInfo(project, true);
        this.js = JavaSource.create(cpInfo, new FileObject[0]);
    }
    
    @Override
    public void process(SubtypeCategoryDefinition def) {
        if (def.getExcludes() == null && def.getIncludes() == null) {
            addInterfaceMarker(mMarker, def.getTypeName(), def.getAssignedMark());
        } else {
            if (def.getExcludes() != null) {
                addInterfaceMarker(mMarker, def.getTypeName(), def.getExcludes(), false, def.getAssignedMark());
            }
            if (def.getIncludes() != null) {
                addInterfaceMarker(mMarker, def.getTypeName(), def.getIncludes(), true, def.getAssignedMark());
            }
        }
    }

    @Override
    public void process(SingleTypeCategoryDefinition def) {
        if (def.getExcludes() == null && def.getIncludes() == null) {
            cMarker.addClassMark(def.getTypeName(), def.getAssignedMark());
        } else {
            if (def.getExcludes() != null) {
                addTypeMarker(mMarker, def.getTypeName(), def.getExcludes(), false, def.getAssignedMark());
            }
            if (def.getIncludes() != null) {
                addTypeMarker(mMarker, def.getTypeName(), def.getIncludes(), true, def.getAssignedMark());
            }
        }
    }

    @Override
    public void process(CustomCategoryDefinition def) {
        cmMarker.addMarker(def.getCustomMarker());
    }

    @Override
    public void process(PackageCategoryDefinition def) {
        pMarker.addPackageMark(def.getPackageName(), def.getAssignedMark(), def.isRecursive());
    }
    
    public MarkMapping[] getMappings() {
        List<MarkMapping> mappings = new ArrayList<MarkMapping>();
        mappings.addAll(Arrays.asList(mMarker.getMappings()));
        mappings.addAll(Arrays.asList(cMarker.getMappings()));
        mappings.addAll(Arrays.asList(pMarker.getMappings()));
        mappings.addAll(Arrays.asList(cmMarker.getMappings()));
        return mappings.toArray(new MarkMapping[mappings.size()]);
    }

    public Mark[] getMarks() {
        Set<Mark> marks = new HashSet<Mark>();
        marks.addAll(Arrays.asList(mMarker.getMarks()));
        marks.addAll(Arrays.asList(cMarker.getMarks()));
        marks.addAll(Arrays.asList(pMarker.getMarks()));
        marks.addAll(Arrays.asList(cmMarker.getMarks()));
        return marks.toArray(new Mark[marks.size()]);
    }
    
    protected void addInterfaceMarker(MethodMarker marker, String interfaceName, Mark mark) {
        addInterfaceMarker(marker, interfaceName, null, false, mark);
    }

    protected void addInterfaceMarker(final MethodMarker marker, final String interfaceName,
            final String[] methodNameRestriction, final boolean inclusive, final Mark mark) {
        
        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {

                public void cancel() {
                }

                public void run(CompilationController controller)
                        throws Exception {
                    doAddInterfaceMarker(marker, interfaceName, methodNameRestriction, inclusive, mark, controller);
                }
            }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void addInterfaceMarkers(final MethodMarker marker, final String[] interfaceNames, final Mark mark) {
        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {

                public void cancel() {
                }

                public void run(CompilationController controller)
                        throws Exception {
                    for (String interfaceName : interfaceNames) {
                        doAddInterfaceMarker(marker, interfaceName, null, false, mark, controller);
                    }
                }
            }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void addInterfaceMarkers(final MethodMarker marker, final String[] interfaceNames,
            final String[] methodNameRestriction, final boolean inclusive, final Mark mark) {
        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {

                public void cancel() {
                }

                public void run(CompilationController controller)
                        throws Exception {
                    for (String interfaceName : interfaceNames) {
                        doAddInterfaceMarker(marker, interfaceName, methodNameRestriction, inclusive, mark, controller);
                    }
                }
            }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void addTypeMarker(final MethodMarker marker, final String type, final Mark mark) {
        addTypeMarker(marker, type, new String[]{}, false, mark);
    }
    
    protected void addTypeMarker(final MethodMarker marker, final String type, final String[] methodNameRestriction,
            final boolean inclusive, final Mark mark) {
        final List<String> restrictors = (methodNameRestriction != null) ? Arrays.asList(methodNameRestriction) : new ArrayList();

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {

                public void cancel() {
                }

                public void run(CompilationController controller)
                        throws Exception {
                    TypeElement typeElement = controller.getElements().getTypeElement(type);
                    addTypeMethods(marker, typeElement, restrictors, inclusive, mark, controller);
                }
            }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private void addImplementorMethods(final MethodMarker marker, final TypeElement superElement, final List<String> restrictors,
            final boolean inclusive, final Mark mark, final CompilationController controller) {
        try {
            controller.toPhase(JavaSource.Phase.RESOLVED);
            // get all implementors of the superclass and add their marker methods
            final Set<ClassIndex.SearchKind> kind = new HashSet<ClassIndex.SearchKind>(Arrays.asList(new ClassIndex.SearchKind[]{
                        ClassIndex.SearchKind.IMPLEMENTORS
                    }));

            //    final Set<ClassIndex.SearchScope> scope = new HashSet<ClassIndex.SearchScope>(Arrays.asList(new ClassIndex.SearchScope[]{ClassIndex.SearchScope.SOURCE, ClassIndex.SearchScope.DEPENDENCIES}));
            final Set<ClassIndex.SearchScope> scope = new HashSet<ClassIndex.SearchScope>(Arrays.asList(new ClassIndex.SearchScope[]{
                        ClassIndex.SearchScope.SOURCE
                    }));

            Set<ElementHandle<TypeElement>> allImplementors = new HashSet<ElementHandle<TypeElement>>();
            Set<ElementHandle<TypeElement>> implementors = controller.getClasspathInfo().getClassIndex().getElements(ElementHandle.create(superElement), kind, scope);

            do {
                Set<ElementHandle<TypeElement>> tmpImplementors = new HashSet<ElementHandle<TypeElement>>();
                allImplementors.addAll(implementors);

                for (ElementHandle<TypeElement> element : implementors) {
                    tmpImplementors.addAll(controller.getClasspathInfo().getClassIndex().getElements(element, kind, scope));
                }

                implementors = tmpImplementors;
            } while (!implementors.isEmpty());

            for (ElementHandle<TypeElement> handle : allImplementors) {
                // resolve the implementor's type element
                TypeElement implementor = handle.resolve(controller);
                addTypeMethods(marker, implementor, restrictors, inclusive, mark, controller);
            }
        } catch (IOException e) {
            LOGGER.throwing(AbstractProjectTypeProfiler.class.getName(), "addImplementorMethods", e); // NOI18N
        }
    }

    private void addTypeMethods(final MethodMarker marker, final TypeElement type, final List<String> restrictors,
            final boolean inclusive, final Mark mark, final CompilationController controller) {
        if ((marker == null) || (type == null) || (restrictors == null) || (mark == null) || (controller == null)) {
            return;
        }
        try {
            controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

            // process all methods from the implementor
            for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
                if ((method.getKind() == ElementKind.METHOD) && !method.getModifiers().contains(Modifier.ABSTRACT)) {
                    if ((inclusive && restrictors.contains(method.getSimpleName().toString())) || (!inclusive && !restrictors.contains(method.getSimpleName().toString()))) {
                        try {
                            marker.addMethodMark(ElementUtilities.getBinaryName(type), method.getSimpleName().toString(),
                                    SourceUtils.getVMMethodSignature(method, controller), mark);
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.throwing(AbstractProjectTypeProfiler.class.getName(), "addTypeMethods", e); // NOI18N
        }
    }

    private void doAddInterfaceMarker(final MethodMarker marker, final String interfaceName,
            final String[] methodNameRestriction, final boolean inclusive, final Mark mark,
            final CompilationController controller)
            throws IllegalArgumentException {
        // store restriction array in a list to ease the "contains" checks
        List<String> restrictors = (methodNameRestriction != null) ? Arrays.asList(methodNameRestriction) : new ArrayList();

        // resolve the type element for the interface/supertype
        TypeElement superElement = controller.getElements().getTypeElement(interfaceName);

        if (superElement == null) {
            LOGGER.fine("Couldn't resolve type: " + interfaceName);

            return;
        }

        switch (superElement.getKind()) {
            case INTERFACE: {
                addImplementorMethods(marker, superElement, restrictors, inclusive, mark, controller);

                break;
            }
            case CLASS: {
                // add all superclass methods
                addTypeMethods(marker, superElement, restrictors, inclusive, mark, controller);

                if (!superElement.getModifiers().contains(Modifier.FINAL)) {
                    addImplementorMethods(marker, superElement, restrictors, inclusive, mark, controller);
                }

                break;
            }
        }
    }
}
