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
 * Response containing the id of the defining class loader, sent to the client in response to the relevant request.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class DefiningLoaderResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int loaderId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DefiningLoaderResponse(int loaderId) {
        super(true, DEFINING_LOADER);

        if (loaderId == -1) {
            loaderId = 0; // At the client side we treat classes loaded by bootstrap and by system classloaders in the same way
        }

        this.loaderId = loaderId;
    }

    // Custom serialization support
    DefiningLoaderResponse() {
        super(true, DEFINING_LOADER);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getLoaderId() {
        return loaderId;
    }

    // For debugging
    public String toString() {
        return "DefiningLoaderResponse, loaderId: " + loaderId + ", " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        loaderId = in.readInt();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(loaderId);
    }
}
