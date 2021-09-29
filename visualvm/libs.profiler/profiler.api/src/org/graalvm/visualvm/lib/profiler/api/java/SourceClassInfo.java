/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.lib.profiler.api.java;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.openide.filesystems.FileObject;

/**
 * A simplified java class descriptor
 */
/**
 *
 * @author Jaroslav Bachorik
 */
abstract public class SourceClassInfo {
    final public static Comparator<SourceClassInfo> COMPARATOR = new Comparator<SourceClassInfo>() {
        @Override
        public int compare(SourceClassInfo o1, SourceClassInfo o2) {
            return o1.getVMName().compareTo(o2.getVMName());
        }
    };

    final private static Pattern anonymousInnerClassPattern = Pattern.compile(".*?\\$[0-9]*$");

    private String simpleName, qualName, vmName;

    public SourceClassInfo(String name, String fqn, String vmName) {
        this.simpleName = name;
        this.qualName = fqn;
        this.vmName = vmName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SourceClassInfo other = (SourceClassInfo) obj;
        if (!Objects.equals(vmName, other.vmName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.vmName != null ? this.vmName.hashCode() : 0);
        return hash;
    }

    /**
     *
     * @return Returns the class simple name (the last part of the FQN)
     */
    final public String getSimpleName() {
        return simpleName;
    }

    /**
     *
     * @return Returns the class FQN
     */
    final public String getQualifiedName() {
        return qualName;
    }

    /**
     *
     * @return Returns the VM internal class name
     */
    final public String getVMName() {
        return vmName;
    }

    /**
     *
     * @return Returns true if the class is an anonymous inner class, false otherwise
     */
    public boolean isAnonymous() {
        return isAnonymous(qualName);
    }
    
    abstract public FileObject getFile();
    abstract public Set<SourceMethodInfo> getMethods(boolean all);
    abstract public Set<SourceClassInfo> getSubclasses();
    abstract public Set<SourceClassInfo> getInnerClases();
    abstract public Set<SourceMethodInfo> getConstructors();
    abstract public SourceClassInfo getSuperType();
    abstract public Set<SourceClassInfo> getInterfaces();
    
    final protected boolean isAnonymous(String className) {
        return anonymousInnerClassPattern.matcher(className).matches();
    }
}
