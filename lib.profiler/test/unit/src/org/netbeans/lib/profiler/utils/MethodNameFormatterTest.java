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

package org.netbeans.lib.profiler.utils;

import junit.framework.TestCase;


/**
 * TestCase for Method Name formatter class.
 *
 * @author Ian Formanek
 */
public final class MethodNameFormatterTest extends TestCase {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String[] patternSources = {
                                                       "java/lang/String", "concat", "(Ljava.lang.String;)Ljava.lang.String;",
                                                       "java/lang/String", "getBytes", "()[B", "classNameInDefaultPackage", "doIt",
                                                       "()V", "foo/bar/LongArrays", "createArray", "([[[[[[[[I)[[[[[[[[[F",
                                                       "foo/bar/Constr", "", "()V", "foo/bar/Constr", "<init>", "()V",
                                                       "foo/bar/Constr", null, null, "foo/bar/Constr", "<clinit>", null,
                                                   };
    private static final String[] patternResults = {
                                                       "java.lang.String", // formattedClass
    "java.lang.String.concat(java.lang.String)", // formattedClassAndMethod
    "concat(java.lang.String)", // formattedMethod
    "java.lang.String.concat(java.lang.String) : java.lang.String", // fullFormattedClassAndMethod
    "concat(java.lang.String) : java.lang.String", // fullFormattedMethod
    "java.lang.String", // paramsString
    "java.lang.String", // returnType
    "java.lang.String", // formattedClass
    "java.lang.String.getBytes()", // formattedClassAndMethod
    "getBytes()", // formattedMethod
    "java.lang.String.getBytes() : byte[]", // fullFormattedClassAndMethod
    "getBytes() : byte[]", // fullFormattedMethod
    "", // paramsString
    "byte[]", // returnType
    "classNameInDefaultPackage", // formattedClass
    "classNameInDefaultPackage.doIt()", // formattedClassAndMethod
    "doIt()", // formattedMethod
    "classNameInDefaultPackage.doIt() : void", // fullFormattedClassAndMethod
    "doIt() : void", // fullFormattedMethod
    "", // paramsString
    "void", // returnType
    "foo.bar.LongArrays", // formattedClass
    "foo.bar.LongArrays.createArray(int[][][][][][][][])", // formattedClassAndMethod
    "createArray(int[][][][][][][][])", // formattedMethod
    "foo.bar.LongArrays.createArray(int[][][][][][][][]) : float[][][][][][][][][]", // fullFormattedClassAndMethod
    "createArray(int[][][][][][][][]) : float[][][][][][][][][]", // fullFormattedMethod
    "int[][][][][][][][]", // paramsString
    "float[][][][][][][][][]", // returnType
    "foo.bar.Constr", // formattedClass
    "foo.bar.Constr.<init>()", // formattedClassAndMethod
    "<init>()", // formattedMethod
    "foo.bar.Constr.<init>()", // fullFormattedClassAndMethod
    "<init>()", // fullFormattedMethod
    "", // paramsString
    "", // returnType
    "foo.bar.Constr", // formattedClass
    "foo.bar.Constr.<init>()", // formattedClassAndMethod
    "<init>()", // formattedMethod
    "foo.bar.Constr.<init>()", // fullFormattedClassAndMethod
    "<init>()", // fullFormattedMethod
    "", // paramsString
    "", // returnType
    "foo.bar.Constr", // formattedClass
    "foo.bar.Constr", // formattedClassAndMethod
    "", // formattedMethod
    "foo.bar.Constr", // fullFormattedClassAndMethod
    "", // fullFormattedMethod
    "", // paramsString
    "", // returnType
    "foo.bar.Constr", // formattedClass
    "foo.bar.Constr.<clinit>", // formattedClassAndMethod
    "<clinit>", // formattedMethod
    "foo.bar.Constr.<clinit>", // fullFormattedClassAndMethod
    "<clinit>", // fullFormattedMethod
    "", // paramsString
    "", // returnType
                                                   };

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Test to ensure that the formatting of test methods is as expected.
     */
    public void testFormatting() {
        MethodNameFormatter mnf;

        int count = 0;

        for (int i = 0; (i + 2) < patternSources.length; i += 3) {
            mnf = new MethodNameFormatter(patternSources[i], patternSources[i + 1], patternSources[i + 2]);

            assertEquals("Wrong results for getFormattedClass " + mnf, patternResults[count++], mnf.getFormattedClass());
            assertEquals("Wrong results for getFormattedClassAndMethod " + mnf, patternResults[count++],
                         mnf.getFormattedClassAndMethod());
            assertEquals("Wrong results for getFormattedMethod " + mnf, patternResults[count++], mnf.getFormattedMethod());
            assertEquals("Wrong results for getFullFormattedClassAndMethod " + mnf, patternResults[count++],
                         mnf.getFullFormattedClassAndMethod());
            assertEquals("Wrong results for getFullFormattedMethod " + mnf, patternResults[count++], mnf.getFullFormattedMethod());
            assertEquals("Wrong results for getParamsString " + mnf, patternResults[count++], mnf.getParamsString());
            assertEquals("Wrong results for getReturnType " + mnf, patternResults[count++], mnf.getReturnType());
        }
    }

    protected void setUp() throws Exception {
    }
}
