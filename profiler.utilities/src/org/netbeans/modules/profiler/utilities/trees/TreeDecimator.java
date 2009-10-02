/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.utilities.trees;

import java.util.List;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class TreeDecimator<T> {
    /**
     * Will decimate a tree given its root node and filter
     * @param oldRoot The tree root
     * @param filter The filter to use
     * @return Returns a decimated version of the given tree; the new root is the first node that matches the filter
     */
    public T decimate(T oldRoot, NodeFilter<T> filter) {
        return decimate(oldRoot, null, filter);
    }

    /**
     * Returns all children of a specified node
     * @param aNode The node to return the children from
     * @return Returns all children of a specified node
     */
    protected abstract List<T> getChildren(T aNode);

    /**
     * Connects a list of children to the specified node
     * @param aNode A node to connect the children to
     * @param children A children to connect
     */
    protected abstract void attachChildren(T aNode, List<T> children);

    /**
     * Removes a child from the list of children of the specified node
     * @param aNode The node to remove a child from
     * @param child The node to remove
     */
    protected abstract void detachChild(T aNode, T child);

    /**
     * Removes all children from the specified node
     * @param aNode The node to remove all children from
     */
    protected abstract void detachChildren(T aNode);

    private T decimate(T oldRoot, T oldParent, NodeFilter<T> filter) {
        if (filter.match(oldRoot)) {
            if (oldParent != null) {
                detachChild(oldParent, oldRoot);
            }

            return oldRoot;
        }

        for (T node : getChildren(oldRoot)) {
            if (filter.maymatch(node)) {
                T found = decimate(node, oldRoot, filter);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}
