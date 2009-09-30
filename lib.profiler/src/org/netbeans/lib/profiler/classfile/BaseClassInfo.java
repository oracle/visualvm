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

package org.netbeans.lib.profiler.classfile;


/**
 * Minimum representation of a class. Used as a base class for the full-fledged ClassInfo, but also
 * may used as is for e.g. array classes.
 *
 * @author Misha Dmitirev
 */
public class BaseClassInfo {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected String name;
    protected String nameAndLoader; // A combinarion of class name and loader, uniquely identifying this ClassInfo

    // Management of multiple versions for the same-named (but possibly not same-code) class, loaded by different classloaders
    protected int classLoaderId; // IDs of all loaders with which versions of this class are loaded

    // Data used by our object allocation instrumentation mechanism: integer class ID
    private int instrClassId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public BaseClassInfo(String className, int classLoaderId) {
        this.name = className.intern();
        this.classLoaderId = classLoaderId;
        nameAndLoader = (name + "#" + classLoaderId).intern(); // NOI18N
        instrClassId = -1;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setInstrClassId(int id) {
        instrClassId = id;
    }

    public int getInstrClassId() {
        return instrClassId;
    }

    public void setLoaderId(int loaderId) {
        classLoaderId = loaderId;
    }

    public int getLoaderId() {
        return classLoaderId;
    }

    public String getName() {
        return name;
    }

    public String getNameAndLoader() {
        return nameAndLoader;
    }

    public String toString() {
        return name;
    }
}
