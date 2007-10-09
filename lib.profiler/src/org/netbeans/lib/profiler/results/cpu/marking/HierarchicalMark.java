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

package org.netbeans.lib.profiler.results.cpu.marking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 *
 * @author Jaroslav Bachorik
 */
public class HierarchicalMark extends Mark {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final HierarchicalMark DEFAULT = new HierarchicalMark("DEFAULT", "Overall", null); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HierarchicalMark parent;
    private final Set children;
    private final Set rootPath;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of HierarchicalMark */
    public HierarchicalMark(final String label, final String description, final HierarchicalMark parent) {
        super(label, description, ((parent == null) || (parent == DEFAULT)));
        this.parent = ((parent == null) ? HierarchicalMark.DEFAULT : parent);

        if (!isDefault) {
            this.rootPath = new LinkedHashSet(this.parent.getRootPath());
        } else {
            this.rootPath = new HashSet();
        }

        this.rootPath.add(label);

        this.children = new HashSet();

        if ((this.parent != null) && (this.parent != DEFAULT)) {
            this.parent.addChild(this);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Set getChildren() {
        return Collections.unmodifiableSet(children);
    }

    public Collection getLabels() {
        return new ArrayList(rootPath);
    }

    public HierarchicalMark getParent() {
        return parent;
    }

    public boolean isRoot() {
        return (parent == null) || (parent == DEFAULT);
    }

    public Set getRootPath() {
        return rootPath;
    }

    public Object clone() {
        return new HierarchicalMark(label, description, parent);
    }

    protected void addChild(HierarchicalMark child) {
        children.add(child);
    }
}
