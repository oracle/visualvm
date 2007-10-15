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

package org.netbeans.modules.profiler.utils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ElementUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.ui.ElementOpen;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.utils.VMUtils;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import static javax.lang.model.util.ElementFilter.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.swing.JEditorPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;


/**
 * Utilities for interaction with the source representation (Java metamodel) in NetBeans IDE
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Jaroslav Bachorik
 * @author Jiri Sedlacek
 */
public final class SourceUtils {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class ResolvedClass {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String vmClassName;
        private TypeElement jclass;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ResolvedClass(TypeElement jclass, String className) {
            this.jclass = jclass;
            this.vmClassName = className;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public TypeElement getJClass() {
            return jclass;
        }

        public String getVMClassName() {
            return vmClassName;
        }
    }

    public static class ResolvedMethod {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private ExecutableElement method;
        private String vmClassName;
        private String vmMethodName;
        private String vmMethodSignature;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ResolvedMethod(ExecutableElement method, String className, String methodName, String methodSignature) {
            this.method = method;
            this.vmClassName = className;
            this.vmMethodName = methodName;
            this.vmMethodSignature = methodSignature;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public ExecutableElement getMethod() {
            return method;
        }

        public String getVMClassName() {
            return vmClassName;
        }

        public String getVMMethodName() {
            return vmMethodName;
        }

        public String getVMMethodSignature() {
            return vmMethodSignature;
        }
    }

    private static final class DeclaredTypeResolver implements TypeVisitor<TypeElement, Void> {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public TypeElement visit(TypeMirror t, Void p) {
            return null;
        }

        public TypeElement visit(TypeMirror t) {
            return null;
        }

        public TypeElement visitArray(ArrayType t, Void p) {
            return null;
        }

        public TypeElement visitDeclared(DeclaredType t, Void p) {
            return (TypeElement) t.asElement();
        }

        public TypeElement visitError(ErrorType t, Void p) {
            return null;
        }

        public TypeElement visitExecutable(ExecutableType t, Void p) {
            return null;
        }

        public TypeElement visitNoType(NoType t, Void p) {
            return null;
        }

        public TypeElement visitNull(NullType t, Void p) {
            return null;
        }

        public TypeElement visitPrimitive(PrimitiveType t, Void p) {
            return null;
        }

        public TypeElement visitTypeVariable(TypeVariable t, Void p) {
            return null;
        }

        public TypeElement visitUnknown(TypeMirror t, Void p) {
            return null;
        }

        public TypeElement visitWildcard(WildcardType t, Void p) {
            return null;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(SourceUtils.class.getName());
    private static final FileObject[] NOFILES = new FileObject[0];
    private static final ClassPath EMPTY_CLASSPATH = ClassPathSupport.createClassPath(NOFILES);
    private static final String JAVA_MIME_TYPE = "text/x-java"; // NOI18N
    private static final String VM_CONSTRUCTUR_SIG = "<init>"; // NOI18N
    private static final String VM_INITIALIZER_SIG = "<clinit>"; // NOI18N
    private static final String[] APPLET_CLASSES = new String[] { "java.applet.Applet", "javax.swing.JApplet" }; // NOI18N
    private static final String[] TEST_CLASSES = new String[] { "junit.framework.TestCase", "junit.framework.TestSuite" }; // NOI18N
                                                                                                                           // -----
                                                                                                                           // I18N String constants
    private static final String NO_SOURCE_FOUND_MESSAGE = NbBundle.getMessage(SourceUtils.class, "MDRUtils_NoSourceFoundMessage");
    private static final String OPENING_SOURCE_MSG = NbBundle.getMessage(SourceUtils.class, "MDRUtils_OpeningSourceMsg"); // NOI18N
                                                                                                                          // -----
    private static final DeclaredTypeResolver declaredTypeResolver = new DeclaredTypeResolver();

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static boolean isApplet(FileObject javaFile) {
        return isJavaFile(javaFile) && isInstanceOf(javaFile, APPLET_CLASSES, false); // NOI18N
    }

    /**
     * Returns all constructors of a java class in format of ClientUtils.SourceCodeSelection array
     */
    public static ClientUtils.SourceCodeSelection[] getClassConstructors(FileObject classFile) {
        Set<ClientUtils.SourceCodeSelection> constructors = new HashSet<ClientUtils.SourceCodeSelection>();
        JavaSource js = JavaSource.forFileObject(classFile);

        if (js == null) {
            return null; // not java source
        }

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(final CompilationController controller)
                             throws Exception {
                        // Controller has to be in some advanced phase, otherwise controller.getCompilationUnit() == null
                        if (controller.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {
                            return;
                        }

                        TreePathScanner<Void, Void> scanner = new TreePathScanner<Void, Void>() {
                            public Void visitMethod(MethodTree node, Void p) {
                                Void retValue;
                                ExecutableElement method = (ExecutableElement) controller.getTrees().getElement(getCurrentPath());
                                retValue = super.visitMethod(node, p);

                                return retValue;
                            }
                        };

                        scanner.scan(controller.getCompilationUnit(), null);
                    }
                }, true);
        } catch (IOException e) {
            Profiler.debug(e);
            e.printStackTrace();
        }

        return constructors.toArray(new ClientUtils.SourceCodeSelection[constructors.size()]);
    }

    /**
     * Returns the FileObject of the most active editor document
     * MUST BE INVOKED ON EDT
     * @return A FileObject or null
     */
    public static FileObject getCurrentFileInEditor() {
        TopComponent tc = TopComponent.getRegistry().getActivated();

        if (tc != null) {
            return tc.getLookup().lookup(FileObject.class);
        }

        return null;
    }

    /**
     * Returns the caret position within the active editor document
     * converted into line number
     * MUST BE INVOKED ON EDT
     * @return The line number or -1
     */
    public static int getCurrentLineInEditor() {
        return getLineForOffsetInEditor(getCurrentOffsetInEditor());
    }

    /**
     * Returns the caret position within the active editor document
     * MUST BE INVOKED ON EDT
     * @return The caret offset or -1
     */
    public static int getCurrentOffsetInEditor() {
        JTextComponent mostActiveEditor = org.netbeans.editor.Registry.getMostActiveComponent();

        if ((mostActiveEditor != null) && (mostActiveEditor.getCaret() != null)) {
            return mostActiveEditor.getCaretPosition();
        }

        return -1;

        //        int offset = -1;
        //        TopComponent tc = TopComponent.getRegistry().getActivated();
        //
        //        if (tc != null) {
        //            EditorCookie ec = tc.getLookup().lookup(EditorCookie.class);
        //
        //            if (ec != null) {
        //                for (JEditorPane pane : ec.getOpenedPanes()) {
        //                    int position = pane.getCaretPosition();
        //
        //                    if (position > -1) {
        //                        offset = position;
        //
        //                        break;
        //                    }
        //                }
        //            }
        //        }
        //
        //        return offset;
    }

    public static boolean isCurrentOffsetValid() {
        return isOffsetValid(getCurrentFileInEditor(), getCurrentOffsetInEditor());
    }

    /**
     * Returns the project the currently activated document belongs to
     * MUST BE INVOKED ON EDT
     * @return The most active project or null
     */
    public static Project getCurrentProjectInEditor() {
        TopComponent tc = TopComponent.getRegistry().getActivated();

        if (tc != null) {
            return tc.getLookup().lookup(Project.class);
        }

        return null;
    }

    /**
     * Returns a collection of all class names in the project's default package
     */
    public static Collection<String> getDefaultPackageClassNames(Project project) {
        final Collection<String> classNames = new ArrayList<String>();

        JavaSource js = getSources(project);
        final Set<ElementHandle<TypeElement>> types = getProjectTypes(project, js);

        for (ElementHandle<TypeElement> typeHandle : types) {
            int firstPkgSeparIndex = typeHandle.getQualifiedName().indexOf('.');

            if (firstPkgSeparIndex <= 0) {
                classNames.add(typeHandle.getQualifiedName().substring(firstPkgSeparIndex + 1));
            }
        }

        return classNames;
    }

    public static String getEnclosingClassName(FileObject profiledClassFile, final int position) {
        final OutputParameter<String> result = new OutputParameter<String>(null);

        if (isJavaFile(profiledClassFile)) {
            JavaSource js = JavaSource.forFileObject(profiledClassFile);

            if (js == null) {
                return null; // not java source
            }

            try {
                js.runUserActionTask(new CancellableTask<CompilationController>() {
                        public void cancel() {
                        }

                        public void run(final CompilationController controller)
                                 throws Exception {
                            if (controller.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {
                                return;
                            }

                            TypeElement parentClass = controller.getTreeUtilities().scopeFor(position).getEnclosingClass();

                            if (parentClass != null) {
                                // no enclosing class found (i.e. cursor at import)
                                result.setValue(ElementUtilities.getBinaryName(parentClass));
                            }
                        }
                    }, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return result.getValue();
    }

    public static boolean isExecutableMethod(ExecutableElement method) {
        if (method == null) {
            return false;
        }

        Set<Modifier> modifiers = method.getModifiers();

        if (modifiers.contains(Modifier.ABSTRACT) || modifiers.contains(Modifier.NATIVE)) {
            return false;
        }

        return true;
    }

    /**
     * Check if given FileObject represents a java class which extends or implements provided class or interface
     *
     * @param fo        a FileObject representing Java source/class file
     * @param className String name, fully qualified using . as package separator, of class we are interested in
     * @return true if the class in the FileObject is a subclass of the specified class
     */
    public static boolean isInstanceOf(final FileObject fo, final String className) {
        return isInstanceOf(fo, new String[] { className }, true);
    }

    /**
     * Check if given FileObject represents a java class which extends or implements all the provided classes or interfaces
     *
     * @param fo        a FileObject representing Java source/class file
     * @param classNames array of classnames, fully qualified using . as package separator
     * @return true if the class in the FileObject is a subclass of the specified class
     */
    public static boolean isInstanceOf(final FileObject fo, final String[] classNames, final boolean allRequired) {
        final boolean[] result = new boolean[] { false };

        // get javasource for the java file
        JavaSource js = JavaSource.forFileObject(fo);

        if (js == null) {
            return false; // not java source
        }

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(final CompilationController controller)
                             throws Exception {
                        // Controller has to be in some advanced phase, otherwise controller.getCompilationUnit() == null
                        if (controller.toPhase(Phase.ELEMENTS_RESOLVED).compareTo(Phase.ELEMENTS_RESOLVED) < 0) {
                            return;
                        }

                        Elements elements = controller.getElements();
                        Trees trees = controller.getTrees();
                        Types types = controller.getTypes();

                        Collection<TypeElement> classElements = new ArrayList<TypeElement>();

                        for (String className : classNames) {
                            TypeElement resolvedElement = elements.getTypeElement(className);

                            if (resolvedElement != null) {
                                classElements.add(resolvedElement);
                            }
                        }

                        if (classElements.isEmpty()) {
                            result[0] = false;

                            return;
                        }

                        CompilationUnitTree cu = controller.getCompilationUnit();
                        List<?extends Tree> topLevels = cu.getTypeDecls();

                        for (Tree topLevel : topLevels) {
                            if (topLevel.getKind() == Tree.Kind.CLASS) {
                                TypeElement type = (TypeElement) trees.getElement(TreePath.getPath(cu, topLevel));

                                if (type != null) {
                                    Set<Modifier> modifiers = type.getModifiers();

                                    if (modifiers.contains(Modifier.PUBLIC) && (classElements != null)) {
                                        boolean rslt = allRequired;

                                        for (TypeElement classElement : classElements) {
                                            if (classElement == null) {
                                                continue;
                                            }

                                            if (allRequired) {
                                                rslt = rslt && types.isSubtype(type.asType(), classElement.asType());

                                                if (!rslt) {
                                                    break;
                                                }
                                            } else {
                                                rslt = rslt || types.isSubtype(type.asType(), classElement.asType());

                                                if (rslt) {
                                                    break;
                                                }
                                            }
                                        }

                                        result[0] = rslt;

                                        if (rslt) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }, true);
        } catch (IOException e) {
            Profiler.debug(e);
            e.printStackTrace();
        }

        return result[0];
    }

    /**
     * Tests the given node for it being a java class
     * @param aNode The node to be tested
     * @return Returns true if the node represents a java class
     */
    public static boolean isJavaClass(Node aNode) {
        if (aNode == null) {
            return false;
        }

        DataObject dObject = aNode.getLookup().lookup(DataObject.class);

        if (dObject != null) {
            return isJavaFile(dObject.getPrimaryFile());
        }

        return false;
    }

    /**
     * Tests the file object for it being a java file
     * @param f The file object to be tested
     * @return Returns true if the file being tested is of MIME type text/x-java
     */
    public static boolean isJavaFile(FileObject f) {
        return JAVA_MIME_TYPE.equals(f.getMIMEType()); //NOI18N
    }

    /**
     * Calculates the line number for a given offset
     * MUST BE INVOKED ON EDT
     * @return Returns the line number within the active editor document or -1
     */
    public static int getLineForOffsetInEditor(int offset) {
        if (offset == -1) {
            return -1;
        }

        TopComponent tc = TopComponent.getRegistry().getActivated();

        if (tc != null) {
            EditorCookie ec = tc.getLookup().lookup(EditorCookie.class);

            if (ec != null) {
                return NbDocument.findLineNumber(ec.getDocument(), offset);
            }
        }

        return -1;
    }

    public static boolean isOffsetValid(FileObject editorFile, int offset) {
        if (editorFile == null) {
            return false;
        }

        return validateOffset(editorFile, offset) != -1;
    }

    public static boolean isRunnable(FileObject javaFile) {
        if (isTest(javaFile) || isApplet(javaFile)) {
            return true;
        }

        if (isJavaFile(javaFile)) {
            return !org.netbeans.api.java.source.SourceUtils.getMainClasses(javaFile).isEmpty();
        }

        return false;
    }

    /**
     * Returns the tuple of start/end selection offset in the currently activated editor
     * @return Tuple [startOffset, endOffset] or [-1, -1] if there is no selection
     */
    public static int[] getSelectionOffsets() {
        int[] indexes = new int[] { -1, -1 };
        TopComponent tc = TopComponent.getRegistry().getActivated();

        if (tc != null) {
            EditorCookie ec = tc.getLookup().lookup(EditorCookie.class);

            if (ec != null) {
                for (JEditorPane pane : ec.getOpenedPanes()) {
                    int selStart = pane.getSelectionStart();

                    if (selStart > -1) {
                        indexes[0] = selStart;
                        indexes[1] = pane.getSelectionEnd();

                        break;
                    }
                }
            }
        }

        return indexes;
    }

    public static String[] getSubclassesNames(final String className, Project project) {
        final Set subclasses = new HashSet();

        final JavaSource js = getSources(project);

        try {
            // use the prepared javasource repository and perform a task
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        if (controller.toPhase(Phase.ELEMENTS_RESOLVED).compareTo(Phase.ELEMENTS_RESOLVED) < 0) {
                            return;
                        }

                        TypeElement superClass = resolveClassByName(className, controller);

                        if (superClass != null) {
                            if (superClass.getKind() == ElementKind.INTERFACE) {
                                subclasses.addAll(findImplementorsResolved(js.getClasspathInfo(), className));
                            }
                        }
                    }
                }, false);
        } catch (IOException ex) {
            Profiler.debug(ex);
        }

        int index = 0;
        String[] subclassesNames = new String[subclasses.size()];

        Iterator it = subclasses.iterator();

        while (it.hasNext()) {
            TypeElement subclass = (TypeElement) it.next();
            subclassesNames[index++] = getVMClassName(subclass);
        }

        return subclassesNames;
    }

    public static boolean isTest(FileObject fo) {
        return isJavaFile(fo) && isInstanceOf(fo, TEST_CLASSES, false); // NOI18N
    }

    public static String getToplevelClassName(FileObject profiledClassFile) {
        final String[] result = new String[1];

        if (isJavaFile(profiledClassFile)) {
            JavaSource js = JavaSource.forFileObject(profiledClassFile);

            if (js == null) {
                return null; // not java source
            }

            try {
                js.runUserActionTask(new CancellableTask<CompilationController>() {
                        public void cancel() {
                        }

                        public void run(final CompilationController controller)
                                 throws Exception {
                            // Controller has to be in some advanced phase, otherwise controller.getCompilationUnit() == null
                            if (controller.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0) {
                                return;
                            }

                            TreePathScanner<String, Void> scanner = new TreePathScanner<String, Void>() {
                                public String visitClass(ClassTree node, Void p) {
                                    return ElementUtilities.getBinaryName((TypeElement) controller.getTrees()
                                                                                                  .getElement(getCurrentPath()));
                                }
                            };

                            result[0] = scanner.scan(controller.getCompilationUnit(), null);
                        }
                    }, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return result[0];
    }

    // Correctly formats class name, same result as for CompilationInfo.getElementUtilities().getBinaryName(jClass)
    public static String getVMClassName(TypeElement jClass) {
        if (jClass.getSimpleName().length() == 0) {
            // This is implemented in ClassSymbol.flatName() but not published
            String userClassName = jClass.toString(); // <anonymous my.Class$X>, IS LOCALIZED!!!
            String className = userClassName.substring(0, userClassName.length() - 1); // remove trailing ">"
            int classNameStart = className.lastIndexOf(" "); // find divider between "anonymous" and classname
            className = className.substring(classNameStart + 1);

            return className;
        } else {
            TypeElement currentClass = jClass;
            StringBuffer vmClassName = new StringBuffer();

            while (isEnclosingElement(currentClass.getEnclosingElement())) {
                vmClassName.insert(0, "$" + currentClass.getSimpleName().toString()); // NOI18N
                currentClass = (TypeElement) currentClass.getEnclosingElement();
            }

            vmClassName.insert(0, currentClass.getQualifiedName().toString());

            return vmClassName.toString();
        }
    }

    public static String getVMMethodName(ExecutableElement method) {
        // Constructor returns <init>
        // Static initializer returns <clinit>
        // Method returns its simple name
        return method.getSimpleName().toString();
    }

    /**
     * Constructs the VM signature for the given executable element (method, constructor ...)
     * @param method The executable element to create the VM sginature for (method, constructor ...)
     * @param ci org.netbeans.api.java.source.CompilationInfo instance
     * @return Returns the textual representation of a VM signature valid for the given executable element
     */
    public static String getVMMethodSignature(ExecutableElement method, CompilationInfo ci) {
        return getSignature(method, ci);
    }

    public static String getVMMethodSignature(final ExecutableElement method, final ClasspathInfo cpInfo) {
        final OutputParameter<String> signature = new OutputParameter<String>("");

        FileObject file = org.netbeans.api.java.source.SourceUtils.getFile(ElementHandle.create(method), cpInfo);
        JavaSource js = JavaSource.create(cpInfo, new FileObject[] { file });

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        signature.setValue(getSignature(method, controller));
                    }
                }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return signature.getValue();
    }

    /**
     * Constructs the VM signature for the given executable element (method, constructor ...)
     * @param ee The executable element to create the VM sginature for (method, constructor ...)
     * @return Returns the textual representation of a VM signature valid for the given executable element
     *
     * @deprecated
     */
    public static String getVMSignature(ExecutableElement ee) {
        String constructedSig = "("; // NOI18N
        final List<?extends VariableElement> paramsList = ee.getParameters();

        for (VariableElement param : paramsList) {
            constructedSig += VMUtils.typeToVMSignature(param.asType().toString());
        }

        if (ee.getKind().equals(ElementKind.CONSTRUCTOR)) {
            constructedSig += ")V"; // NOI18N
        } else {
            constructedSig += (")" + VMUtils.typeToVMSignature(ee.getReturnType().toString())); // NOI18N
        }

        return constructedSig;
    }

    public static FileObject findFileObjectByClassName(final String className, final Project project) {
        if (className == null) {
            return null;
        }

        final OutputParameter<FileObject> resolvedFileObject = new OutputParameter(null);

        final JavaSource js = getSources(project);

        try {
            // use the prepared javasource repository and perform a task
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        TypeElement resolvedClass = resolveClassByName(className, controller);

                        if (resolvedClass != null) {
                            resolvedFileObject.setValue(org.netbeans.api.java.source.SourceUtils.getFile(ElementHandle.create(resolvedClass),
                                                                                                         controller
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .getClasspathInfo()));
                        }
                    }
                }, false);
        } catch (IOException ex) {
            Profiler.debug(ex);
        }

        return resolvedFileObject.getValue();
    }

    public static Set<ElementHandle<TypeElement>> findImplementors(ClasspathInfo cpInfo, final String superType) {
        final Set<ClassIndex.SearchKind> kind = new HashSet<ClassIndex.SearchKind>(Arrays.asList(new ClassIndex.SearchKind[] {
                                                                                                     ClassIndex.SearchKind.IMPLEMENTORS
                                                                                                 }));
        final Set<ClassIndex.SearchScope> scope = new HashSet<ClassIndex.SearchScope>(Arrays.asList(new ClassIndex.SearchScope[] {
                                                                                                        ClassIndex.SearchScope.SOURCE,
                                                                                                        ClassIndex.SearchScope.DEPENDENCIES
                                                                                                    }));

        final OutputParameter<Set<ElementHandle<TypeElement>>> implementors = new OutputParameter<Set<ElementHandle<TypeElement>>>(new HashSet<ElementHandle<TypeElement>>());

        JavaSource js = JavaSource.create(cpInfo, new FileObject[0]);

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        TypeElement superElement = controller.getElements().getTypeElement(superType);

                        if (!superElement.getModifiers().contains(Modifier.FINAL)) {
                            implementors.setValue(controller.getClasspathInfo().getClassIndex()
                                                            .getElements(ElementHandle.create(superElement), kind, scope));
                        }
                    }
                }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return implementors.getValue();
    }

    public static Set<TypeElement> findImplementorsResolved(ClasspathInfo cpInfo, final String superType) {
        final Set<ClassIndex.SearchKind> kind = new HashSet<ClassIndex.SearchKind>(Arrays.asList(new ClassIndex.SearchKind[] {
                                                                                                     ClassIndex.SearchKind.IMPLEMENTORS
                                                                                                 }));
        final Set<ClassIndex.SearchScope> scope = new HashSet<ClassIndex.SearchScope>(Arrays.asList(new ClassIndex.SearchScope[] {
                                                                                                        ClassIndex.SearchScope.SOURCE,
                                                                                                        ClassIndex.SearchScope.DEPENDENCIES
                                                                                                    }));

        final Set<TypeElement> implementors = new HashSet<TypeElement>();

        JavaSource js = JavaSource.create(cpInfo, new FileObject[0]);

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        if (controller.toPhase(Phase.ELEMENTS_RESOLVED).compareTo(Phase.ELEMENTS_RESOLVED) < 0) {
                            return;
                        }

                        TypeElement superElement = controller.getElements().getTypeElement(superType);

                        if (!superElement.getModifiers().contains(Modifier.FINAL)) {
                            for (ElementHandle<TypeElement> handle : controller.getClasspathInfo().getClassIndex()
                                                                               .getElements(ElementHandle.create(superElement),
                                                                                            kind, scope)) {
                                implementors.add(handle.resolve(controller));
                            }
                        }
                    }
                }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return implementors;
    }

    /**
     * Searches all the given source roots for classes containing "main" methods
     * @param roots The source roots
     * @return Returns a collection of all classes found within given source roots containing "main" method
     */
    public static Collection<String> findMainClasses(FileObject[] roots) {
        final Collection<String> classNames = new ArrayList<String>();

        Collection<ElementHandle<TypeElement>> mainElements = org.netbeans.api.java.source.SourceUtils.getMainClasses(roots);

        for (ElementHandle<TypeElement> mainElement : mainElements) {
            classNames.add(mainElement.getQualifiedName());
        }

        return classNames;
    }

    public static boolean hasMainMethod(FileObject javaFile) {
        if (!isJavaFile(javaFile)) {
            return false;
        }

        return !org.netbeans.api.java.source.SourceUtils.getMainClasses(javaFile).isEmpty();
    }

    public static void openSource(Project project, final String className, final String methodName, final String signature) {
        // *** logging stuff ***
        Profiler.debug("Open Source: Project: " + ((project == null) ? "null" : ProjectUtilities.getProjectName(project))); // NOI18N
        Profiler.debug("Open Source: Class name: " + className); // NOI18N
        Profiler.debug("Open Source: Method name: " + methodName); // NOI18N
        Profiler.debug("Open Source: Method sig: " + signature); // NOI18N
                                                                 // *********************
                                                                 // create the javasource repository for all the source files

        final JavaSource js = getSources(project);

        try {
            // use the prepared javasource repository and perform a task
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        controller.toPhase(Phase.ELEMENTS_RESOLVED);

                        Element destinationElement = null;

                        // resolve the class by name
                        TypeElement classElement = resolveClassByName(className, controller);

                        if ((methodName != null) && (methodName.length() > 0)) {
                            // if a method name has been specified try to resolve the method
                            if (classElement != null) {
                                destinationElement = resolveMethodByName(classElement, methodName, signature);
                            }
                        }

                        if (destinationElement == null) {
                            // unsuccessful attempt to resolve a method -> use the class instead
                            destinationElement = classElement;
                        }

                        if (destinationElement != null) {
                            Profiler.debug("Opening element: " + destinationElement); // NOI18N

                            final Element openElement = destinationElement;
                            String st = MessageFormat.format(OPENING_SOURCE_MSG, new Object[] { openElement.toString() });
                            final String finalStatusText = st + " ..."; // NOI18N
                            StatusDisplayer.getDefault().setStatusText(finalStatusText);

                            IDEUtils.runInEventDispatchThread(new Runnable() {
                                    // manipulates the TopComponent - must be executed in EDT
                                    public void run() {
                                        // opens the source code on the found method position
                                        if (!ElementOpen.open(js.getClasspathInfo(), openElement)) {
                                            Profiler.getDefault()
                                                    .displayError(MessageFormat.format(NO_SOURCE_FOUND_MESSAGE,
                                                                                       new Object[] { className }));
                                        }

                                        if (finalStatusText.equals(StatusDisplayer.getDefault().getStatusText())) {
                                            StatusDisplayer.getDefault().setStatusText(""); // NOI18N
                                        }
                                    }
                                });
                        } else {
                            Profiler.getDefault()
                                    .displayError(MessageFormat.format(NO_SOURCE_FOUND_MESSAGE, new Object[] { className }));
                        }
                    }
                }, false);
        } catch (IOException ex) {
            Profiler.debug(ex);
        }
    }

    public static ResolvedClass resolveClassAtPosition(final FileObject fo, final int position, final boolean resolveField) {
        // Get JavaSource for given FileObject
        JavaSource js = JavaSource.forFileObject(fo);

        if (js == null) {
            return null; // not java source
        }

        // Final holder of resolved method
        final OutputParameter<ResolvedClass> resolvedClass = new OutputParameter(null);

        // Resolve the method
        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController ci)
                             throws Exception {
                        if (ci.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {
                            return;
                        }

                        TreePath path = ci.getTreeUtilities().pathFor(position);

                        if (path == null) {
                            return;
                        }

                        Element element = ci.getTrees().getElement(path);

                        if (element == null) {
                            return;
                        }

                        // resolve class/enum at cursor
                        if ((element.getKind() == ElementKind.CLASS) || (element.getKind() == ElementKind.ENUM)) {
                            TypeElement jclass = (TypeElement) element;
                            String vmClassName = ElementUtilities.getBinaryName(jclass);
                            resolvedClass.setValue(new ResolvedClass(jclass, vmClassName));

                            return;
                        }

                        // resolve field at cursor
                        if (resolveField
                                && ((element.getKind() == ElementKind.FIELD) || (element.getKind() == ElementKind.LOCAL_VARIABLE))
                                && (element.asType().getKind() == TypeKind.DECLARED)) {
                            TypeElement jclass = getDeclaredType(element.asType());
                            String vmClassName = ElementUtilities.getBinaryName(jclass);
                            resolvedClass.setValue(new ResolvedClass(jclass, vmClassName));

                            return;
                        }
                    }
                }, true);
        } catch (IOException ioex) {
            Profiler.debug(ioex);
            ioex.printStackTrace();

            return null;
        }

        return resolvedClass.getValue();
    }

    /**
     * Resolves a class by its name
     * @param className The name of the class to be resolved
     * @param controller The compilation controller to be used to resolve the class
     * @return Returns a TypeElement representing the resolved class or NULL
     */
    public static TypeElement resolveClassByName(String className, final CompilationController controller) {
        if ((className == null) || (controller == null)) {
            return null;
        }

        // 1. try to resolve the class
        TypeElement mainClass = controller.getElements().getTypeElement(className.replace('$', '.')); // NOI18N

        if (mainClass == null) {
            // 2. probably an anonymous inner class; try to move to the "ELEMENTS_RESOLVED" phase
            try {
                controller.toPhase(Phase.RESOLVED);

                int innerSeparatorIndex = className.indexOf('$'); // NOI18N

                if (innerSeparatorIndex > 0) {
                    final String origClassName = className;
                    className = className.substring(0, innerSeparatorIndex);
                    mainClass = controller.getElements().getTypeElement(className);

                    if (mainClass != null) {
                        FileObject fo = org.netbeans.api.java.source.SourceUtils.getFile(ElementHandle.create(mainClass),
                                                                                         controller.getClasspathInfo());
                        final OutputParameter<TypeElement> mainClassElement = new OutputParameter<TypeElement>(mainClass);

                        try {
                            JavaSource.forFileObject(fo).runUserActionTask(new CancellableTask<CompilationController>() {
                                    private volatile boolean isCancelled = false;

                                    public void cancel() {
                                        isCancelled = true;
                                    }

                                    public void run(final CompilationController cc)
                                             throws Exception {
                                        cc.toPhase(Phase.RESOLVED);

                                        TreePathScanner<Void, String> scanner = new TreePathScanner<Void, String>() {
                                            public Void visitClass(ClassTree node, String p) {
                                                if (isCancelled) {
                                                    return null;
                                                }

                                                Element classElement = cc.getTrees().getElement(getCurrentPath());

                                                if ((classElement != null) && (classElement.getKind() == ElementKind.CLASS)) {
                                                    if (ElementUtilities.getBinaryName((TypeElement) classElement).equals(p)) {
                                                        mainClassElement.setValue((TypeElement) classElement);

                                                        return null;
                                                    }
                                                }

                                                ;

                                                return super.visitClass(node, p);
                                            }
                                        };

                                        scanner.scan(cc.getCompilationUnit(), origClassName);
                                    }
                                }, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mainClass = mainClassElement.getValue();
                    }
                }
            } catch (IOException e) {
                ProfilerLogger.log(e);
            }
        }

        if (mainClass != null) {
            Profiler.debug("Resolved: " + mainClass); // NOI18N
        } else {
            Profiler.debug("Could not resolve: " + className); // NOI18N
        }

        if (mainClass == null) {
            StatusDisplayer.getDefault()
                           .setStatusText(NbBundle.getMessage(SourceUtils.class, "MDRUtils_ClassNotResolvedMessage", className)); // notify user
        }

        return mainClass;
    }

    public static ResolvedMethod resolveMethodAtPosition(final FileObject fo, final int position) {
        // Get JavaSource for given FileObject
        JavaSource js = JavaSource.forFileObject(fo);

        if (js == null) {
            return null; // not java source
        }

        // Final holder of resolved method
        final OutputParameter<ResolvedMethod> resolvedMethod = new OutputParameter(null);

        // Resolve the method
        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController ci)
                             throws Exception {
                        if (ci.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {
                            return;
                        }

                        TreePath path = ci.getTreeUtilities().pathFor(position);

                        if (path == null) {
                            return;
                        }

                        Element element = ci.getTrees().getElement(path);

                        if ((element != null)
                                && ((element.getKind() == ElementKind.METHOD) || (element.getKind() == ElementKind.CONSTRUCTOR)
                                       || (element.getKind() == ElementKind.STATIC_INIT))) {
                            ExecutableElement method = (ExecutableElement) element;
                            String vmClassName = ElementUtilities.getBinaryName((TypeElement) method.getEnclosingElement());
                            String vmMethodName = getVMMethodName(method);
                            String vmMethodSignature = getVMMethodSignature(method, ci);
                            resolvedMethod.setValue(new ResolvedMethod(method, vmClassName, vmMethodName, vmMethodSignature));
                        }
                    }
                }, true);
        } catch (IOException ioex) {
            Profiler.debug(ioex);
            ioex.printStackTrace();

            return null;
        }

        return resolvedMethod.getValue();
    }

    private static TypeElement getDeclaredType(TypeMirror type) {
        return type.accept(declaredTypeResolver, null);
    }

    private static boolean isEnclosingElement(Element element) {
        if (element == null) {
            return false;
        }

        ElementKind kind = element.getKind();

        return (kind == ElementKind.CLASS) || (kind == ElementKind.ENUM) || (kind == ElementKind.INTERFACE);
    }

    /**
     * Checks an ExecutableElement for being a main method
     * @param method The method to be tested
     * @return Returns true if the method is named "main", return type is void and is STATIC
     */
    private static boolean isMainMethod(ExecutableElement method) {
        return org.netbeans.api.java.source.SourceUtils.isMainMethod(method);

        //    if (method == null || method.getKind() != ElementKind.METHOD) returnfalse;
        //
        //    return method.getSimpleName().contentEquals("main") && method.getReturnType().getKind() == TypeKind.VOID && method.getModifiers().contains(Modifier.STATIC); // NOI18N
    }

    /**
     * Converts list of parameters to a single string with the signature
     * @param params A list of method parameters
     * @return string with the vm signature of the parameters
     */
    private static String getParamsSignature(List<?extends VariableElement> params, CompilationInfo ci) {
        StringBuffer ret = new StringBuffer();
        Iterator<?extends VariableElement> it = params.iterator();

        while (it.hasNext()) {
            TypeMirror type = it.next().asType();
            String realTypeName = getRealTypeName(type, ci);
            String typeVMSignature = VMUtils.typeToVMSignature(realTypeName);
            ret.append(typeVMSignature);
        }

        return ret.toString();
    }

    /**
     * Returns all types (classes) defined on the given source roots
     */
    private static Set<ElementHandle<TypeElement>> getProjectTypes(FileObject[] roots, JavaSource js) {
        final Set<ClassIndex.SearchScope> scope = new HashSet<ClassIndex.SearchScope>();
        scope.add(ClassIndex.SearchScope.SOURCE);

        if (js != null) {
            return js.getClasspathInfo().getClassIndex().getDeclaredTypes("", ClassIndex.NameKind.CASE_INSENSITIVE_PREFIX, scope); // NOI18N
        }

        return null;
    }

    /**
     * Returns all types (classes) defined within a project
     */
    private static Set<ElementHandle<TypeElement>> getProjectTypes(Project project, JavaSource js) {
        return getProjectTypes(ProjectUtilities.getSourceRoots(project, true), js);
    }

    private static String getRealTypeName(TypeMirror type, CompilationInfo ci) {
        TypeKind typeKind = type.getKind();

        if (typeKind.isPrimitive()) {
            return type.toString(); // primitive type, return its name
        }

        switch (typeKind) {
            case VOID:

                // VOID type, return "void" - will be converted later by VMUtils.typeToVMSignature
                return type.toString();
            case DECLARED:

                // Java class (also parametrized - "ArrayList<String>" or "ArrayList<T>"), need to generate correct innerclass signature using "$"
                return ElementUtilities.getBinaryName(getDeclaredType(type));
            case ARRAY:

                // Array means "String[]" or "T[]" and also varargs "Object ... args"
                return getRealTypeName(((ArrayType) type).getComponentType(), ci) + "[]"; // NOI18N
            case TYPEVAR:

                // TYPEVAR means "T" or "<T extends String>" or "<T extends List&Runnable>"
                List<?extends TypeMirror> subTypes = ci.getTypes().directSupertypes(type);

                if (subTypes.size() == 0) {
                    return "java.lang.Object"; // NOI18N // Shouldn't happen
                }

                if ((subTypes.size() > 1) && subTypes.get(0).toString().equals("java.lang.Object")
                        && getDeclaredType(subTypes.get(1)).getKind().isInterface()) {
                    // NOI18N
                    // Master type is interface
                    return getRealTypeName(subTypes.get(1), ci);
                } else {
                    // Master type is class
                    return getRealTypeName(subTypes.get(0), ci);
                }
            case WILDCARD:

                // WILDCARD means "<?>" or "<? extends Number>" or "<? super T>", shouldn't occur here
                throw new IllegalArgumentException("Unexpected WILDCARD parameter: " + type); // NOI18N
            default:

                // Unexpected parameter type
                throw new IllegalArgumentException("Unexpected type parameter: " + type + " of kind " + typeKind); // NOI18N
        }
    }

    /**
     * @param method Method
     * @param types javax.lang.model.util.Types instance
     * @return String representation of VM-type method signature
     */
    private static String getSignature(ExecutableElement method, CompilationInfo ci) {
        try {
            switch (method.getKind()) {
                case METHOD:
                case CONSTRUCTOR:
                case STATIC_INIT:

                    //case INSTANCE_INIT: // not supported
                    String paramsVMSignature = getParamsSignature(method.getParameters(), ci);
                    String retTypeVMSignature = VMUtils.typeToVMSignature(getRealTypeName(method.getReturnType(), ci));

                    return "(" + paramsVMSignature + ")" + retTypeVMSignature; //NOI18N
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warning(e.getMessage());
        }

        return null;
    }

    /**
     * Returns the JavaSource repository of a given project or global JavaSource if no project is provided
     */
    private static JavaSource getSources(Project project) {
        if (project == null) {
            return getSources((FileObject[]) null);
        } else {
            return getSources(ProjectUtilities.getSourceRoots(project, true));
        }
    }

    /**
     * Returns the JavaSource repository for given source roots
     */
    private static JavaSource getSources(FileObject[] roots) {
        //    findMainClasses(roots);
        // prepare the classpath based on the source roots
        ClassPath srcPath;
        ClassPath bootPath;
        ClassPath compilePath;

        if (roots == null) {
            srcPath = ClassPathSupport.createProxyClassPath(GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)
                                                                              .toArray(new ClassPath[0]));
            bootPath = JavaPlatform.getDefault().getBootstrapLibraries();
            compilePath = ClassPathSupport.createProxyClassPath(GlobalPathRegistry.getDefault().getPaths(ClassPath.COMPILE)
                                                                                  .toArray(new ClassPath[0]));
        } else {
            srcPath = ClassPathSupport.createClassPath(roots);
            bootPath = ClassPath.getClassPath(roots[0], ClassPath.BOOT);
            compilePath = ClassPath.getClassPath(roots[0], ClassPath.COMPILE);
        }

        // create ClassPathInfo for JavaSources only -> (bootPath, classPath, sourcePath)
        final ClasspathInfo cpInfo = ClasspathInfo.create(bootPath, compilePath, srcPath);

        // create the javasource repository for all the source files
        return JavaSource.create(cpInfo, Collections.<FileObject>emptyList());
    }

    /**
     * Compares the desired textual method name with a name of particualt executable element (method, constructor ...)
     * @param vmName The name to match against. Can be a real method name, "<init>" or "<cinit>"
     * @param ee The executable element to use in matching
     * @return Returns true if the given textual name matches the name of the executable element
     */
    private static boolean methodNameMatch(final String vmName, final ExecutableElement ee) {
        switch (ee.getKind()) {
            // for method use textual name matching
            case METHOD:
                return ee.getSimpleName().contentEquals(vmName);

            // for constructor use the special <init> name
            case CONSTRUCTOR:
                return vmName.equals(VM_CONSTRUCTUR_SIG);

            // for initializer use the special <cinit> name
            case STATIC_INIT:
            case INSTANCE_INIT:
                return vmName.equals(VM_INITIALIZER_SIG);
        }

        // default fail-over
        return false;
    }

    /**
     * Compares the desired textual representation of a VM signature with a VM signature of the provided ExecutableElement (method, constructor ...)
     * @param vmSig The desired VM signature
     * @param ee The executable element to compare the signature to (method, constructor ...)
     * @return Returns true if the signature of the executable element matches the desired signature
     */
    private static boolean methodSignatureMatch(final String vmSig, final ExecutableElement ee) {
        // heuristic: it is hard to distinguish where innerclass starts in CallableFeature params, so let's not deal with
        // this at all
        final String vmSigCheck = vmSig.replaceAll("\\$", "/"); // NOI18N

        return getVMSignature(ee).equals(vmSigCheck);
    }

    /**
     * Resolves a method by its name, signature and parent class
     * @param parentClass The parent class
     * @param methodName The method name
     * @param signature The VM signature of the method
     * @return Returns an ExecutableElement representing the method or null
     */
    private static ExecutableElement resolveMethodByName(TypeElement parentClass, String methodName, String signature) {
        // TODO: static initializer
        if ((parentClass == null) || (methodName == null) || (signature == null)) {
            return null;
        }

        ExecutableElement foundMethod = null;
        boolean found = false;

        List<ExecutableElement> methods = null;

        if (methodName.equals(VM_CONSTRUCTUR_SIG)) {
            methods = constructorsIn(parentClass.getEnclosedElements());

            //    } else if (methodName.equals(VM_INITIALIZER_SIG)) {
            //      methods = constructorsIn(parentClass.getEnclosedElements());
        } else {
            // retrieve all defined methods
            methods = methodsIn(parentClass.getEnclosedElements());
        }

        // loop over all methods
        for (ExecutableElement method : methods) {
            // match the current method against the required method name and signature
            if (methodNameMatch(methodName, method)) {
                if (methodSignatureMatch(signature, method)) {
                    foundMethod = method;
                    found = true;

                    break;
                }

                foundMethod = method; // keeping the track of the closest match
            }
        }

        if (!found) {
            Profiler.debug("Could not find exact signature match, opening at first method with same name: " + foundMethod); // NOI18N
        }

        return foundMethod;
    }

    private static int validateOffset(FileObject editorDoc, final int toValidate) {
        final OutputParameter<Integer> validated = new OutputParameter<Integer>(-1);

        JavaSource js = JavaSource.forFileObject(editorDoc);

        if (js != null) {
            try {
                js.runUserActionTask(new Task<CompilationController>() {
                        public void run(CompilationController controller)
                                 throws Exception {
                            controller.toPhase(JavaSource.Phase.RESOLVED);
                            validated.setValue(-1); // non-validated default

                            Scope sc = controller.getTreeUtilities().scopeFor(toValidate);

                            if (sc.getEnclosingClass() != null) {
                                    validated.setValue(toValidate);
                            }
                        }
                    }, true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return validated.getValue();
    }
}
