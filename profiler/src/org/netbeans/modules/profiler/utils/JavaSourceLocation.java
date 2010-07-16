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
package org.netbeans.modules.profiler.utils;

/**
 *
 * @author Jaroslav Bachorik
 */
final public class JavaSourceLocation {

    final public String className;
    final public String methodName;
    final public String signature;
    final public int line;

    public JavaSourceLocation(String className) {
        this(className, null, null, Integer.MIN_VALUE);
    }

    public JavaSourceLocation(String className, String methodName) {
        this(className, methodName, null, Integer.MIN_VALUE);
    }

    public JavaSourceLocation(String className, String methodName, String signature) {
        this(className, methodName, signature, Integer.MIN_VALUE);
    }

    public JavaSourceLocation(String className, int line) {
        this(className, null, null, line);
    }

    public JavaSourceLocation(String className, String methodName, int line) {
        this(className, methodName, null, line);
    }

    public JavaSourceLocation(String className, String methodName, String signature, int line) {
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.line = line;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JavaSourceLocation other = (JavaSourceLocation) obj;
        if ((this.className == null) ? (other.className != null) : !this.className.equals(other.className)) {
            return false;
        }
        if ((this.methodName == null) ? (other.methodName != null) : !this.methodName.equals(other.methodName)) {
            return false;
        }
        if ((this.signature == null) ? (other.signature != null) : !this.signature.equals(other.signature)) {
            return false;
        }
        if (this.line != other.line) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + (this.className != null ? this.className.hashCode() : 0);
        hash = 71 * hash + (this.methodName != null ? this.methodName.hashCode() : 0);
        hash = 71 * hash + (this.signature != null ? this.signature.hashCode() : 0);
        hash = 71 * hash + this.line;
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(className);
        if (methodName != null) {
            sb.append("#").append(methodName);
            if (signature != null) {
                sb.append("(").append(signature).append(")");
            }
        }
        if (line > Integer.MIN_VALUE) {
            sb.append(":").append(line);
        }
        return sb.toString();
    }
}
