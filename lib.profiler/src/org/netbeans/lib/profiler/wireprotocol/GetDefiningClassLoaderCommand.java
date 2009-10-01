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

package org.netbeans.lib.profiler.wireprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * A request to obtain the defining class loader for a given class and its initiating loader,
 * that the client sends to the server.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class GetDefiningClassLoaderCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String className;
    private int classLoaderId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // This is to avoid creating a new instance of this class every time a new class is loaded
    public GetDefiningClassLoaderCommand(String className, int classLoaderId) {
        super(GET_DEFINING_CLASS_LOADER);
        this.className = className.replace('/', '.'); // NOI18N
        this.classLoaderId = classLoaderId;
    }

    // Custom serialization support
    GetDefiningClassLoaderCommand() {
        super(GET_DEFINING_CLASS_LOADER);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getClassLoaderId() {
        return classLoaderId;
    }

    public String getClassName() {
        return className;
    }

    // For debugging
    public String toString() {
        return super.toString() + ", className: " + className + ", classLoaderId: " + classLoaderId; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        className = in.readUTF();
        classLoaderId = in.readInt();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(className);
        out.writeInt(classLoaderId);
    }
}
