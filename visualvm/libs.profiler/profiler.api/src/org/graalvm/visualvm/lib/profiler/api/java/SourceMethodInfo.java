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

import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * A simplified java method descriptor
 */
/**
 *
 * @author Jaroslav Bachorik
 */
public class SourceMethodInfo {
    private String className, name, signature, vmName;
    private boolean execFlag;
    final private int modifiers;

    public SourceMethodInfo(String className, String name, String signature, String vmName, boolean execFlag, int modifiers) {
        this.className = className;
        this.name = name;
        this.signature = signature;
        this.vmName = vmName;
        this.execFlag = execFlag;
        this.modifiers = modifiers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SourceMethodInfo other = (SourceMethodInfo) obj;
        if (!Objects.equals(className, other.className)) {
            return false;
        }
        if (!Objects.equals(name, other.name)) {
            return false;
        }
        if (!Objects.equals(signature, other.signature)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.className != null ? this.className.hashCode() : 0);
        hash = 73 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 73 * hash + (this.signature != null ? this.signature.hashCode() : 0);
        return hash;
    }

    /**
     *
     * @return Returns the containing class FQN
     */
    final public String getClassName() {
        return className;
    }

    /**
     *
     * @return Returns the method name
     */
    final public String getName() {
        return name;
    }

    /**
     *
     * @return Returns the method signature
     */
    final public String getSignature() {
        return signature;
    }

    /**
     *
     * @return Returns the VM internal method name
     */
    final public String getVMName() {
        return vmName;
    }

    /**
     *
     * @return Returns TRUE if the method is executable (eg. main(String[]) or JSP main method)
     */
    final public boolean isExecutable() {
        return execFlag;
    }
    
    /**
     * 
     * @return Returns method's modifiers in the {@linkplain Modifier} format
     */
    final public int getModifiers() {
        return modifiers;
    }
    
}
