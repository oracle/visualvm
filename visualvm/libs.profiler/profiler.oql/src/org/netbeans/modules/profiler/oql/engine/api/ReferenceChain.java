/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
package org.netbeans.modules.profiler.oql.engine.api;

import java.lang.ref.WeakReference;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

/**
 * Represents a chain of references to some target object
 *
 * @author      Bill Foote
 */
final public class ReferenceChain {
    private WeakReference<Object> obj;	// Object referred to
    ReferenceChain next;	// Next in chain
    private Heap heap;
    private long id;
    private char type;
    
    private static char TYPE_INSTANCE = 0;
    private static char TYPE_CLASS = 1;
    
    public ReferenceChain(Heap heap, Object obj, ReferenceChain next) {
        this.obj = new WeakReference(obj);
        this.next = next;
        this.heap = heap;
        
        if (obj instanceof Instance) {
            type = TYPE_INSTANCE;
            id = ((Instance)obj).getInstanceId();
        } else if (obj instanceof JavaClass) {
            type = TYPE_CLASS;
            id = ((JavaClass)obj).getJavaClassId();
        }
    }

    public Object getObj() {
        Object o = obj.get();
        if (o == null) {
            if (type == TYPE_INSTANCE) {
                o = heap.getInstanceByID(id);
            } else if (type == TYPE_CLASS) {
                o = heap.getJavaClassByID(id);
            }
            obj = new WeakReference(o);
        }
        return o;
    }

    public ReferenceChain getNext() {
        return next;
    }
    
    public boolean contains(Object obj) {
        ReferenceChain tmp = this;
        while (tmp != null) {
            if (tmp.getObj().equals(obj)) return true;
            tmp = tmp.next;
        }
        return false;
    }

    public int getDepth() {
        int count = 1;
        ReferenceChain tmp = next;
        while (tmp != null) {
            count++;
            tmp = tmp.next;
        }
        return count;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (obj instanceof Instance) {
            Instance inst = (Instance)obj;
            sb.append(inst.getJavaClass().getName()).append("#").append(inst.getInstanceNumber());
        } else if (obj instanceof JavaClass) {
            sb.append("class of ").append(((JavaClass)obj).getName());
        }
        sb.append(next != null ? ("->" + next.toString()) : "");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReferenceChain other = (ReferenceChain) obj;
        if (this.obj != other.obj && (this.obj == null || !this.obj.equals(other.obj))) {
            return false;
        }
        if (this.next != other.next && (this.next == null || !this.next.equals(other.next))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.obj != null ? this.obj.hashCode() : 0);
        hash = 79 * hash + (this.next != null ? this.next.hashCode() : 0);
        return hash;
    }
}
