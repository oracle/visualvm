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

package org.netbeans.lib.profiler.instrumentation;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.utils.Wildcards;
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

            for (int i = 0; i < classNames.length; i++) {
                String name = classNames[i].replace('/', '.'); // NOI18N;

                if (!rootClasses.contains(name)) {
                    rootClasses.add(name);
                }
            }

            return rootClasses;
        }

        return null;
    }
}
