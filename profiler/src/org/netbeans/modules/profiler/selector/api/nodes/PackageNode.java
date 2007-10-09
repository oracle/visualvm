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
import org.netbeans.api.java.source.ClassIndex;
import static org.netbeans.api.java.source.ClassIndex.*;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.modules.profiler.selector.api.SelectorChildren;
import org.netbeans.modules.profiler.selector.api.SelectorNode;
import org.netbeans.modules.profiler.ui.Utils;
import org.openide.filesystems.FileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;


/**
 *
 * @author Jaroslav Bachorik
 */
public class PackageNode extends ContainerNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class PackageChildren extends SelectorChildren<PackageNode> {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected List<SelectorNode> prepareChildren(PackageNode parent) {
            List<SelectorNode> nodes = new ArrayList<SelectorNode>();
            List<PackageNode> subs = getSubpackages(parent);
            List<ClassNode> classes = getClasses(parent);
            nodes.addAll(subs);
            nodes.addAll(classes);

            return nodes;
        }

        private List<ClassNode> getClasses(final PackageNode parent) {
            final List<ClassNode> nodes = new ArrayList<ClassNode>();
            JavaSource source = JavaSource.create(parent.cpInfo, new FileObject[0]);

            try {
                source.runUserActionTask(new CancellableTask<CompilationController>() {
                        public void cancel() {
                        }

                        public void run(CompilationController controller)
                                 throws Exception {
                            controller.toPhase(JavaSource.Phase.PARSED);

                            PackageElement pelem = controller.getElements().getPackageElement(parent.getName());

                            if (pelem != null) {
                                for (TypeElement type : ElementFilter.typesIn(pelem.getEnclosedElements())) {
                                    if ((type.getKind() == ElementKind.CLASS) || (type.getKind() == ElementKind.ENUM)) {
                                        nodes.add(new ClassNode(parent.cpInfo, Utils.CLASS_ICON, type, parent));
                                    }
                                }
                            } else {
                                LOGGER.log(Level.FINEST, "Package name {0} resulted into a NULL element", parent.getName()); // NOI18N
                            }
                        }
                    }, true);
            } catch (IOException ex) {
                LOGGER.severe(ex.getLocalizedMessage());
            }

            Collections.sort(nodes, ClassNode.COMPARATOR);

            return nodes;
        }

        private List<PackageNode> getSubpackages(final PackageNode parent) {
            ClassIndex index = parent.cpInfo.getClassIndex();
            List<PackageNode> nodes = new ArrayList<PackageNode>();

            for (String pkgName : index.getPackageNames(parent.getName() + ".", true, parent.scope)) { // NOI18N
                nodes.add(new PackageNode(parent.cpInfo, pkgName, parent, parent.scope));
            }

            Collections.sort(nodes, COMPARATOR);

            return nodes;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String DEFAULT_NAME = "<default>"; // NOI18N
    static final Comparator COMPARATOR = new Comparator<PackageNode>() {
        public int compare(PackageNode o1, PackageNode o2) {
            if (o1.getNodeName().equals(PackageNode.DEFAULT_NAME)) {
                return -1;
            }

            return o1.toString().compareTo(o2.toString());
        }
    };

    private static final Logger LOGGER = Logger.getLogger(PackageNode.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final ClientUtils.SourceCodeSelection signature;
    private final ClasspathInfo cpInfo;
    private final Set<SearchScope> scope;
    private final String name;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of PackageNode */
    public PackageNode(final ClasspathInfo cpInfo, final String name, final ContainerNode parent, final Set<SearchScope> scope) {
        super(stripName(defaultizeName(name)), Utils.PACKAGE_ICON, parent);
        this.name = name;
        this.cpInfo = cpInfo;
        this.signature = new ClientUtils.SourceCodeSelection(name + ".*", null, null); // NOI18N
        this.scope = scope;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    @Override
    public ClientUtils.SourceCodeSelection getSignature() {
        return signature;
    }

    protected SelectorChildren getChildren() {
        return new PackageChildren();
    }

    ClasspathInfo getCpInfo() {
        return cpInfo;
    }

    private static String defaultizeName(String name) {
        return ((name == null) || (name.length() == 0)) ? DEFAULT_NAME : name;
    }

    private static String stripName(String name) {
        int lastDot = name.lastIndexOf('.'); // NOI18N

        if (lastDot > -1) {
            return name.substring(lastDot + 1);
        }

        return name;
    }
}
