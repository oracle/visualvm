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

package org.netbeans.modules.profiler.selector.api.nodes;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ElementUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.modules.profiler.selector.api.SelectorChildren;
import org.netbeans.modules.profiler.selector.api.SelectorNode;
import org.netbeans.modules.profiler.ui.Utils;
import org.netbeans.modules.profiler.utils.SourceUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;


/**
 *
 * @author Jaroslav Bachorik
 */
public class InnerClassesNode extends ContainerNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class Children extends GreedySelectorChildren<InnerClassesNode> {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected List<?extends SelectorNode> prepareChildren(final InnerClassesNode parent) {
            final Set<ClassNode> innerClassNodes = new HashSet<ClassNode>();

            try {
                JavaSource js = JavaSource.create(parent.cpInfo, new FileObject[0]);
                js.runUserActionTask(new CancellableTask<CompilationController>() {
                        public void cancel() {
                        }

                        public void run(CompilationController controller)
                                 throws Exception {
                            if (controller.toPhase(JavaSource.Phase.RESOLVED) != JavaSource.Phase.RESOLVED) {
                                return;
                            }

                            TypeElement classElement = SourceUtils.resolveClassByName(parent.getClassHandle().getBinaryName(),
                                                                                      controller);
                            List<TypeElement> elements = ElementFilter.typesIn(classElement.getEnclosedElements());

                            for (TypeElement element : elements) {
                                innerClassNodes.add(new ClassNode(parent.cpInfo, Utils.CLASS_ICON, element, parent));
                            }

                            addAnonymousInnerClasses(controller, parent, innerClassNodes);
                        }
                    }, true);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            List<ClassNode> innerClasses = new ArrayList<ClassNode>(innerClassNodes);
            Collections.sort(innerClasses, ClassNode.COMPARATOR);

            return innerClasses;
        }

        private static String getInterfaceNames(TypeElement classElement, CompilationInfo ci) {
            String ifcNames = null;
            List<?extends TypeMirror> ifcs = classElement.getInterfaces();

            if (ifcs != null) {
                StringBuilder sb = new StringBuilder();
                boolean firstRun = true;

                for (TypeMirror ifc : ifcs) {
                    Element superclassElement = ci.getTypes().asElement(ifc);

                    if ((superclassElement != null) && (superclassElement.getKind() == ElementKind.INTERFACE)) {
                        if (!firstRun) {
                            sb.append(", ");
                        }

                        sb.append(ElementUtilities.getBinaryName((TypeElement) superclassElement));
                    }
                }

                ifcNames = sb.toString();
            }

            return ifcNames;
        }

        private static String getSuperTypeClassName(TypeElement classElement, CompilationInfo ci) {
            String superClassName = null;

            TypeMirror superclass = classElement.getSuperclass();

            if (superclass != null) {
                Element superclassElement = ci.getTypes().asElement(superclass);

                if ((superclassElement != null) && (superclassElement.getKind() == ElementKind.CLASS)) {
                    String superclassName = ElementUtilities.getBinaryName((TypeElement) superclassElement);

                    if (!superclassName.equals("java.lang.Object")) {
                        superClassName = superclassName;
                    }
                }
            }

            return superClassName;
        }

        private void addAnonymousInnerClasses(final CompilationController controller, final InnerClassesNode parentClass,
                                              final Set<ClassNode> innerClassNodes)
                                       throws IOException {
            final Pattern anonymousInnerClassPattern = Pattern.compile("\\$[0-9]*");
            final int parentClassNameLength = parentClass.getClassHandle().getBinaryName().length();
            FileObject fo = org.netbeans.api.java.source.SourceUtils.getFile(parentClass.getClassHandle(),
                                                                             controller.getClasspathInfo());
            JavaSource.forFileObject(fo).runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(final CompilationController cc)
                             throws Exception {
                        cc.toPhase(JavaSource.Phase.RESOLVED);

                        TreePathScanner<Void, Void> scanner = new TreePathScanner<Void, Void>() {
                            @Override
                            public Void visitClass(ClassTree node, Void v) {
                                Element classElement = controller.getTrees().getElement(getCurrentPath());

                                if ((classElement != null) && (classElement.getKind() == ElementKind.CLASS)) {
                                    TypeElement innerClassElement = (TypeElement) classElement;
                                    String className = ElementUtilities.getBinaryName(innerClassElement);

                                    if (className.length() <= parentClassNameLength) {
                                        className = "";
                                    } else {
                                        className = className.substring(parentClassNameLength);
                                    }

                                    if (anonymousInnerClassPattern.matcher(className).matches()) {
                                        String implementedClassName = getSuperTypeClassName(innerClassElement, cc);

                                        if (implementedClassName == null) {
                                            implementedClassName = getInterfaceNames(innerClassElement, cc);
                                        }

                                        if (implementedClassName != null) {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append(className).append("[").append(implementedClassName).append("]"); // NOI18N
                                            className = sb.toString();
                                            innerClassNodes.add(new ClassNode(parentClass.cpInfo, Utils.CLASS_ICON,
                                                                              innerClassElement, className, parentClass));
                                        }
                                    }
                                }

                                super.visitClass(node, v);

                                return null;
                            }

                            @Override
                            public Void visitAssignment(AssignmentTree assTree, Void v) {
                                return super.visitAssignment(assTree, v);
                            }
                        };

                        scanner.scan(cc.getCompilationUnit(), null);
                    }
                }, false);
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final ClasspathInfo cpInfo;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of ConstructorsNode */
    public InnerClassesNode(final ClasspathInfo cpInfo, final ClassNode parent) {
        super(NbBundle.getMessage(InnerClassesNode.class, "InnerClasses_DisplayName"), Utils.CLASS_ICON, parent); // NOI18N
        this.cpInfo = cpInfo;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ElementHandle<TypeElement> getClassHandle() {
        return ((ClassNode) getParent()).getClassHandle();
    }

    protected SelectorChildren getChildren() {
        return new Children();
    }
}
