/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.oql.engine.api.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * Provides an iterator over instances of <I> using instances of <T> for traversal
 * @author Jaroslav Bachorik
 */
abstract public class TreeIterator<I, T> implements Iterator<I> {
    private Stack<T> toInspect = new Stack<T>();
    private Set<T> inspected = new HashSet<T>();

    private T popped = null;
    private Iterator<I> inspecting = null;

    public TreeIterator(T root) {
        toInspect.push(root);
        inspected.add(root);
    }
    
    public boolean hasNext() {
        setupIterator();
        return inspecting != null && inspecting.hasNext();
    }

    public I next() {
        setupIterator();

        if (inspecting == null || !inspecting.hasNext()) {
            throw new NoSuchElementException();
        }

        I retVal = inspecting.next();
        return retVal;
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
    }

    abstract protected Iterator<I> getSameLevelIterator(T popped);
    abstract protected Iterator<T> getTraversingIterator(T popped);

    private void setupIterator() {
        while (!toInspect.isEmpty() && (inspecting == null || !inspecting.hasNext())) {
            popped = toInspect.pop();
            if (popped != null) {
                inspecting = getSameLevelIterator(popped);
                Iterator<T> recurseIter = getTraversingIterator(popped);
                while (recurseIter.hasNext()) {
                    T inspectNext = recurseIter.next();
                    if (inspectNext == null) continue;
                    if (!inspected.contains(inspectNext)) {
                        toInspect.push(inspectNext);
                        inspected.add(inspectNext);
                    }
                }
            } else {
                inspecting = null;
            }
        }
    }
}
