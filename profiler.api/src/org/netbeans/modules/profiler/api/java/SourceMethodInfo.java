/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api.java;

import java.lang.reflect.Modifier;

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
        if ((this.className == null) ? (other.className != null) : !this.className.equals(other.className)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.signature == null) ? (other.signature != null) : !this.signature.equals(other.signature)) {
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
