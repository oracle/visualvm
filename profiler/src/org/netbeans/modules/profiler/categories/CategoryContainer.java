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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.modules.profiler.utilities.Visitable;
import org.netbeans.modules.profiler.utilities.Visitor;

/**
 *
 * @author Jaroslav Bachorik
 */
public class CategoryContainer extends Category implements Iterable<Category> {
    private Set<Category> contained = new HashSet<Category>();

    public CategoryContainer(String id, String label, Mark mark) {
        super(id, label, mark);
    }
    
    public CategoryContainer(String name, String label) {
        super(name, label);
    }

    public int size() {
        return contained.size();
    }

    public boolean remove(Object o) {
        return contained.remove(o);
    }

    public Iterator<Category> iterator() {
        return contained.iterator();
    }

    public boolean isEmpty() {
        return contained.isEmpty();
    }

    public boolean contains(Object o) {
        return contained.contains(o);
    }

    public boolean add(Category e) {
        return contained.add(e);
    }

    public boolean addAll(Collection<Category> categories) {
        return contained.addAll(categories);
    }

    @Override
    public Set<Category> getSubcategories() {
        return Collections.unmodifiableSet(contained);
    }

    public <R, P> R accept(Visitor<Visitable<Category>, R, P> visitor, P parameter) {
        R result = null;
        result = visitor.visit(this, parameter);
        if (result != null) return result;
        for(Category cat : contained) {
            result = cat.accept(visitor, parameter);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

//    public <V extends Visitable, R, P> void accept(V, R, P> visitor, P parameter) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
    
//    public <V extends Visitable, R, P> R accept(Visitor<V, R, P> visitor, P parameter) {
////        processDefinitionsWith(processor);
//        for(V cat : contained) {
//            visitor.visit(cat, parameter);
//        }
//        return null;
//    }

}
