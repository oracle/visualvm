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


/**
 *
 * @author Jaroslav Bachorik
 */
public class Wildcards {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String ALLWILDCARD = "*"; // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static boolean matchesWildcard(String wildcard, String loadedClassName) {
        //    System.err.println("Matches wildcard: "+loadedClassName+", wild: "+wildcard + " : " + (loadedClassName.startsWith(wildcard) && (loadedClassName.indexOf('/', wildcard.length()) == -1)));
        boolean packageWildcard = false;
        if (wildcard.endsWith(Wildcards.ALLWILDCARD)) { // package wild card - instrument all classes including subpackages
            wildcard = Wildcards.unwildPackage(wildcard);
            packageWildcard = true;
        }
        if (!loadedClassName.startsWith(wildcard)) {
            return false;
        }
        return packageWildcard || (loadedClassName.indexOf('/', wildcard.length()) == -1); // NOI18N
    }


    public static boolean isMethodWildcard(String methodName) {
        return (methodName != null) ? (methodName.equals(ALLWILDCARD) || methodName.equals("<all>")) : false; // NOI18N
    }

    public static boolean isPackageWildcard(String className) {
        if (className == null) {
            return false;
        }

        return (className.length() == 0 // empty string is default package wildcard
        ) || className.endsWith("/") // ends with '/', means package wildcard // NOI18N
               || className.endsWith(".") // ends with '.', means package wildcard // NOI18N
               || className.endsWith(ALLWILDCARD); // ends with the default WILDCARD (*)
    }

    public static String unwildPackage(String packageMask) {
        if (packageMask == null) {
            return null;
        }

        //    System.out.print("Performing unwildPackage() : " + packageMask);
        if (packageMask.endsWith(ALLWILDCARD)) {
            //      String newPk = packageMask.substring(0, packageMask.length() - 2);
            //      System.out.println(" -> " + newPk);
            return packageMask.substring(0, packageMask.length() - 1);
        }

        return packageMask;
    }
}
