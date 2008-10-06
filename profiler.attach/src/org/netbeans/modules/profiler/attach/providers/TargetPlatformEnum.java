/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.attach.providers;

import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Jaroslav Bachorik
 */
public class TargetPlatformEnum {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String[] jvmNames = new String[] {
                                                 IntegrationUtils.PLATFORM_JAVA_50, IntegrationUtils.PLATFORM_JAVA_60,
                                                 IntegrationUtils.PLATFORM_JAVA_70, IntegrationUtils.PLATFORM_JAVA_CVM
                                             };
    public static final TargetPlatformEnum JDK5 = new TargetPlatformEnum(0);
    public static final TargetPlatformEnum JDK6 = new TargetPlatformEnum(1);
    public static final TargetPlatformEnum JDK7 = new TargetPlatformEnum(2);
    public static final TargetPlatformEnum JDK_CVM = new TargetPlatformEnum(3);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int jvmIndex = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private TargetPlatformEnum(int index) {
        this.jvmIndex = index;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof TargetPlatformEnum)) {
            return false;
        }

        return ((TargetPlatformEnum) obj).jvmIndex == this.jvmIndex;
    }

    public static Iterator iterator() {
        List jvmList = new ArrayList(4);
        jvmList.add(JDK5);
        jvmList.add(JDK6);
        jvmList.add(JDK7);
        jvmList.add(JDK_CVM);

        return jvmList.listIterator();
    }

    public String toString() {
        return jvmNames[this.jvmIndex];
    }
}
