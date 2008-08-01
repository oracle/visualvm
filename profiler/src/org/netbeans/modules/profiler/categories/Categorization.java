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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.lib.profiler.marker.Marker;
import org.netbeans.lib.profiler.results.cpu.marking.MarkMapping;
import org.netbeans.modules.profiler.utilities.Visitable;
import org.netbeans.modules.profiler.utilities.Visitor;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik
 */
public class Categorization implements Marker {

    private Set<CategoryBuilder> builders;
    private Project project;
    private Map<Category, Set<Mark>> inheritedMarkMap;
    private Map<Mark, Category> reverseMap;
    private CategoryContainer root = null;

    public Categorization(Project project, CategoryBuilder builder) {
        this(project, Collections.singleton(builder));
    }

    public Categorization(Project project, Collection<CategoryBuilder> builders) {
        this.builders = new HashSet<CategoryBuilder>(builders);
        this.project = project;
        this.inheritedMarkMap = null;
    }

    public synchronized void reset() {
        root = null;
        inheritedMarkMap = null;
        reverseMap = null;
    }

    private synchronized Map<Category, Set<Mark>> getInheritedMap() {
        if (inheritedMarkMap == null && reverseMap == null) {
            initInternals();
        }

        return inheritedMarkMap;
    }

    private synchronized Map<Mark, Category> getReverseMap() {
        if (inheritedMarkMap == null && reverseMap == null) {
            initInternals();
        }
        return reverseMap;
    }

    private void initInternals() {
        inheritedMarkMap = new HashMap<Category, Set<Mark>>();
        reverseMap = new WeakHashMap<Mark, Category>();

        Stack<Category> path = new Stack<Category>();
        path.add(getRoot());
        initInternals(path);
    }

    private void initInternals(Stack<Category> path) {
        Category currentCategory = path.peek();
        reverseMap.put(currentCategory.getAssignedMark(), currentCategory);

        for (Category category : path) {
            Set<Mark> marks = inheritedMarkMap.get(category);
            if (marks == null) {
                marks = new HashSet<Mark>();
                inheritedMarkMap.put(category, marks);
            }
            marks.add(currentCategory.getAssignedMark());
        }
        for (Category child : currentCategory.getSubcategories()) {
            path.push(child);
            initInternals(path);
            path.pop();
        }
    }

    public Category getRoot() {
        if (root == null) {
            root = new CategoryContainer("ROOT", NbBundle.getMessage(CategoryBuilder.class, "ROOT_CATEGORY_NAME"), Mark.DEFAULT); // NOI18N
            for (CategoryBuilder builder : builders) {
                root.addAll(builder.getRootCategory().getSubcategories());
            }
        }
        return root;
    }

    public Category getCategoryForMark(Mark mark) {
        return getReverseMap().get(mark);
    }

    public Set<Mark> getAllMarks(Category category) {
        Set<Mark> marks = getInheritedMap().get(category);
        return marks != null ? Collections.unmodifiableSet(marks) : Collections.EMPTY_SET;
    }

    public MarkMapping[] getMappings() {
        MarkerProcessor mp = new MarkerProcessor(project);
        getRoot().accept(new Visitor<Visitable<Category>, Void, CategoryDefinitionProcessor>() {

            public Void visit(Visitable<Category> visitable, CategoryDefinitionProcessor parameter) {
                visitable.getValue().processDefinitionsWith(parameter);
                return null;
            }
        }, mp);
        return mp.getMappings();
    }

    public Mark[] getMarks() {
        return getAllMarks(getRoot()).toArray(new Mark[0]);
    }
}
