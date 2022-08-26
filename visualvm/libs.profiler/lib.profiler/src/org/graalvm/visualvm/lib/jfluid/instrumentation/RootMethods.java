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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Tomas Hurka
 */
class RootMethods {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // In case of multiple roots, here we have 1 to 1 correspondence between classNames, methodNames and methodSignatures
    // E.g. we may have X,foo,() and X,bar,() as the respective elements of these three arrays.
    String[] classNames;
    boolean[] classesWildcard;
    boolean[] markerMethods;
    String[] methodNames;
    String[] methodSignatures;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    RootMethods(ClientUtils.SourceCodeSelection[] roots) {
        classNames = new String[roots.length];
        methodNames = new String[roots.length];
        methodSignatures = new String[roots.length];
        classesWildcard = new boolean[roots.length];
        markerMethods = new boolean[roots.length];

        for (int i = 0; i < roots.length; i++) {
            ClientUtils.SourceCodeSelection s = roots[i];

            if (s.definedViaSourceLines()) {
                classNames = new String[] { s.getClassName() };
            } else if (s.definedViaMethodName()) {
                // Convert all the class names into slash form
                String rootName = classNames[i] = s.getClassName().replace('.', '/').intern(); // NOI18N
                                                                                               //System.err.println("root rootName: "+rootName);

                if (Wildcards.isPackageWildcard(rootName)) {
                    classesWildcard[i] = true;
                    classNames[i] = Wildcards.unwildPackage(rootName);
                    //System.err.println("Uses wildcard: "+rootClasses[i]);
                    // root method name and signature is not used in this case
                } else {
                    methodNames[i] = s.getMethodName().intern();
                    methodSignatures[i] = s.getMethodSignature().intern();
                    classesWildcard[i] = false;
                }
            } else { // The third case, when no root methods or code region is defined ("Instrument all spawned threads")
                classNames = methodNames = methodSignatures = new String[0];
            }

            markerMethods[i] = s.isMarkerMethod();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    List /*<String>*/ getRootClassNames() {
        if (classNames.length > 0) {
            List rootClasses = new ArrayList();

            for (String className : classNames) {
                String name = className.replace('/', '.'); // NOI18N;
                if (!rootClasses.contains(name)) {
                    rootClasses.add(name);
                }
            }

            return rootClasses;
        }

        return null;
    }
}
