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

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.modules.profiler.selector.api.SelectorChildren;
import org.netbeans.modules.profiler.selector.api.SelectorNode;
import org.netbeans.modules.profiler.ui.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import javax.swing.Icon;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class ContainerNode extends SelectorNode {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of ContainerNode
     */
    public ContainerNode(String displayName, String name, Icon icon, ContainerNode parent) {
        super(displayName, name, icon, null, parent);
        setChildren(getChildren());
    }

    public ContainerNode(String displayName, String name, Icon icon) {
        super(displayName, name, icon, null);
        setChildren(getChildren());
    }

    public ContainerNode(String name, Icon icon) {
        this(name, name, icon);
    }

    public ContainerNode(String name, Icon icon, ContainerNode parent) {
        this(name, name, icon, parent);
    }

    public ContainerNode() {
        this("", Utils.NO_ICON);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public Collection<ClientUtils.SourceCodeSelection> getRootMethods(boolean all) {
        Collection<ClientUtils.SourceCodeSelection> roots = new ArrayList<ClientUtils.SourceCodeSelection>();
        ClientUtils.SourceCodeSelection signature = getSignature();

        if ((all || isFullyChecked()) && (signature != null)) {
            roots.add(signature);
        } else if (((all || isFullyChecked()) && (signature == null)) || isPartiallyChecked()) {
            Enumeration children = children();

            while (children.hasMoreElements()) {
                roots.addAll(((SelectorNode) children.nextElement()).getRootMethods(all));
            }
        }

        //    if ((all || isFullyChecked()) && signature != null) {
        //      if (getChildCount(true) > 1) {
        //        roots.add(signature);
        //      } else {
        //        if (!getChildrenRootMethods(roots, all)) {
        //          roots.add(signature);
        //        }
        //      }
        //    } else if (((all || isFullyChecked()) && signature == null) || isPartiallyChecked()) {
        //      if (!getChildrenRootMethods(roots, all)) {
        //        roots.add(signature);
        //      };
        //    }
        return roots;
    }

    protected abstract SelectorChildren getChildren();

    //  @Override
    //  public Collection<ClientUtils.SourceCodeSelection> getRootMethods() {
    //    Collection<ClientUtils.SourceCodeSelection> roots = new ArrayList<ClientUtils.SourceCodeSelection>();
    //    if (isFullyChecked() || isPartiallyChecked()) {
    //      Enumeration children = children();
    //      while(children.hasMoreElements()) {
    //        roots.addAll(((SelectorNode)children.nextElement()).getRootMethods());
    //      }
    //    }
    //    return roots;
    //  }
    private boolean getChildrenRootMethods(final Collection<ClientUtils.SourceCodeSelection> roots, boolean all) {
        boolean foundRmethods = false;
        Enumeration children = children();

        while (children.hasMoreElements()) {
            Collection<ClientUtils.SourceCodeSelection> rmethods = ((SelectorNode) children.nextElement()).getRootMethods(all);

            if (!rmethods.isEmpty()) {
                foundRmethods = true;
            }

            roots.addAll(rmethods);
        }

        return foundRmethods;
    }
}
