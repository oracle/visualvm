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

import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.modules.profiler.selector.api.SelectorChildren;
import org.netbeans.modules.profiler.selector.api.SelectorNode;
import org.netbeans.modules.profiler.ui.Utils;
import org.netbeans.modules.profiler.utils.SourceUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MethodsNode extends ContainerNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class Children extends GreedySelectorChildren<MethodsNode> {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected List<?extends SelectorNode> prepareChildren(final MethodsNode parent) {
            final List<MethodNode> methodNodes = new ArrayList<MethodNode>();

            try {
                JavaSource js = JavaSource.create(parent.cpInfo, new FileObject[0]);
                js.runUserActionTask(new CancellableTask<CompilationController>() {
                        public void cancel() {
                        }

                        public void run(CompilationController controller)
                                 throws Exception {
                            controller.toPhase(Phase.ELEMENTS_RESOLVED);

                            TypeElement classElement = SourceUtils.resolveClassByName(parent.getClassHandle().getBinaryName(),
                                                                                      controller);
                            List<ExecutableElement> methods = ElementFilter.methodsIn(parent.isShowingInheritedMethods()
                                                                                      ? controller.getElements()
                                                                                                  .getAllMembers(classElement)
                                                                                      : classElement.getEnclosedElements());

                            for (ExecutableElement method : methods) {
                                MethodNode methodNode = new MethodNode(parent.cpInfo, method, parent);

                                if (methodNode.getSignature() != null) {
                                    methodNodes.add(methodNode);
                                }
                            }
                        }
                    }, true);
                Collections.sort(methodNodes, MethodNode.COMPARATOR);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return methodNodes;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final ClasspathInfo cpInfo;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of MethodsNode */
    public MethodsNode(final ClasspathInfo cpInfo, final ClassNode parent) {
        super(NbBundle.getMessage(MethodsNode.class, "Methods_DisplayName"), Utils.METHODS_ICON, parent); // NOI18N
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
