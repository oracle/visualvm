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
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.modules.profiler.selector.api.SelectorChildren;
import org.netbeans.modules.profiler.selector.api.SelectorNode;
import org.netbeans.modules.profiler.ui.Utils;
import org.netbeans.modules.profiler.utils.OutputParameter;
import org.netbeans.modules.profiler.utils.SourceUtils;
import org.openide.filesystems.FileObject;
import java.io.IOException;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.swing.Icon;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ConstructorNode extends SelectorNode {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ClientUtils.SourceCodeSelection rootMethod;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of MethodNode */
    public ConstructorNode(ClasspathInfo cpInfo, final ExecutableElement method, ConstructorsNode parent) {
        super(method.toString(), method.getSimpleName().toString(), getIcon(method), SelectorChildren.LEAF, parent);

        final OutputParameter<String> signature = new OutputParameter<String>(""); // NOI18N
        JavaSource js = JavaSource.create(cpInfo, new FileObject[0]);

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        signature.setValue(SourceUtils.getVMMethodSignature(method, controller));
                    }
                }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        rootMethod = new ClientUtils.SourceCodeSelection(getEnclosingClass(method).getQualifiedName().toString(),
                                                         method.getSimpleName().toString(), signature.getValue());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public ClientUtils.SourceCodeSelection getSignature() {
        return rootMethod;
    }

    private TypeElement getEnclosingClass(Element element) {
        Element parent = element.getEnclosingElement();

        if (parent != null) {
            if ((parent.getKind() == ElementKind.CLASS) || (parent.getKind() == ElementKind.ENUM)) {
                return (TypeElement) parent;
            } else {
                return getEnclosingClass(parent);
            }
        }

        return null;
    }

    private static Icon getIcon(ExecutableElement method) {
        Icon icon;

        if (method.getModifiers().contains(Modifier.PUBLIC)) {
            icon = Utils.CONSTRUCTOR_PUBLIC_ICON;
        } else if (method.getModifiers().contains(Modifier.PROTECTED)) {
            icon = Utils.CONSTRUCTOR_PROTECTED_ICON;
        } else if (method.getModifiers().contains(Modifier.PRIVATE)) {
            icon = Utils.CONSTRUCTOR_PRIVATE_ICON;
        } else {
            icon = Utils.CONSTRUCTOR_PACKAGE_ICON;
        }

        return icon;
    }
}
