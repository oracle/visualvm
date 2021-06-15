/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.utils;


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
            if (packageWildcard && loadedClassName.equals(wildcard.substring(0,wildcard.length()-1))) {
                return true;
            }
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
            return packageMask.substring(0, packageMask.length() - 1).intern();
        }

        return packageMask.intern();
    }
}
