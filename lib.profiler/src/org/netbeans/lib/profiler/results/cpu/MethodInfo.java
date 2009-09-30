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

package org.netbeans.lib.profiler.results.cpu;

import java.util.Collections;
import java.util.Set;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MethodInfo {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Set marks;
    private String className;
    private String methodName;
    private String signature;
    private String threadName;
    private int methodId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MethodInfo
     */
    public MethodInfo(int methodId, String className, String methodName, String signature, String threadName, Set marks) {
        this.methodId = methodId;
        this.methodName = methodName;
        this.className = className;
        this.signature = signature;
        this.marks = Collections.synchronizedSet(marks);
        this.threadName = threadName;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getClassName() {
        return className;
    }

    public Set getMarks() {
        return marks;
    }

    public int getMethodId() {
        return methodId;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    public String getThreadName() {
        return threadName;
    }

    public boolean isUnmarked() {
        return (marks == null) || (marks.size() == 0)
               || ((marks.size() == 1) && (marks.contains("PROJECT") || marks.contains("USER")))
               || ((marks.size() == 2) && (marks.contains("PROJECT") && marks.contains("USER"))); // NOI18N
    }

    public boolean equals(Object anotherObj) {
        if (anotherObj == null) {
            return false;
        }

        if (anotherObj instanceof MethodInfo) {
            return (this.methodId == ((MethodInfo) anotherObj).methodId)
                   && this.threadName.equals(((MethodInfo) anotherObj).threadName);
        }

        return false;
    }

    public int hashCode() {
        return methodId;
    }
}
