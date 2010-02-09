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
 * Instances of this class are sent back by both client and back end (server) in response to some Commands.
 * An instance of the base Response class is used to signal just success or failure (with possible additional
 * error message). Instances of its subclasses are used to pass additional information.
 *
 * @author Misha Dmitriev
 */
public class Response {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Each new response class should have its own identifier, listed here.
    public static final int NO_TYPE = 0;
    public static final int CODE_REGION_CPU_RESULTS = 1;
    public static final int INSTRUMENT_METHOD_GROUP = 2;
    public static final int INTERNAL_STATS = 3;
    public static final int VM_PROPERTIES = 4;
    public static final int DUMP_RESULTS = 5;
    public static final int OBJECT_ALLOCATION_RESULTS = 6;
    public static final int METHOD_NAMES = 7;
    public static final int THREAD_LIVENESS_STATUS = 8;
    public static final int MONITORED_NUMBERS = 9;
    public static final int DEFINING_LOADER = 10;
    public static final int CALIBRATION_DATA = 11;
    public static final int CLASSID_RESPONSE = 12;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected String errorMessage;
    protected boolean yes;
    private int type;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public Response(boolean yes) {
        type = NO_TYPE;
        this.yes = yes;
    }

    public Response(String errorMessage) {
        type = NO_TYPE;
        this.errorMessage = errorMessage;
    }

    protected Response(boolean yes, int type) {
        this.yes = yes;
        this.type = type;
    }

    // Custom serialization support
    Response() {
        type = NO_TYPE;
    }

    Response(int type) {
        this.type = type;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isOK() {
        return errorMessage == null;
    }

    public int getType() {
        return type;
    }

    // For debugging
    public String toString() {
        String s = respTypeToString(type);

        return s + (isOK() ? (" Ok, " + (yes() ? "yes" : "no")) : (" Error, " + errorMessage)); // NOI18N
    }

    public boolean yes() {
        return yes;
    }

    void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }

    void setType(int type) {
        this.type = type;
    }

    void setYes(boolean yes) {
        this.yes = yes;
    }

    void readObject(ObjectInputStream in) throws IOException {
    }

    static String respTypeToString(int type) {
        if (type != NO_TYPE) {
            switch (type) {
                case CODE_REGION_CPU_RESULTS:
                    return "CODE_REGION_CPU_RESULTS"; // NOI18N
                case INSTRUMENT_METHOD_GROUP:
                    return "INSTRUMENT_METHOD_GROUP"; // NOI18N
                case INTERNAL_STATS:
                    return "INTERNAL_STATS"; // NOI18N
                case VM_PROPERTIES:
                    return "VM_PROPERTIES"; // NOI18N
                case DUMP_RESULTS:
                    return "DUMP_RESULTS"; // NOI18N
                case OBJECT_ALLOCATION_RESULTS:
                    return "OBJECT_ALLOCATION_RESULTS"; // NOI18N
                case METHOD_NAMES:
                    return "METHOD_NAMES"; // NOI18N
                case THREAD_LIVENESS_STATUS:
                    return "THREAD_LIVENESS_STATUS"; // NOI18N
                case MONITORED_NUMBERS:
                    return "MONITORED_NUMBERS"; // NOI18N
                case DEFINING_LOADER:
                    return "DEFINING_LOADER"; // NOI18N
                case CALIBRATION_DATA:
                    return "CALIBRATION_DATA"; // NOI18N
                case CLASSID_RESPONSE:
                    return "CLASSID_RESPONSE"; // NOI18N
                default:
                    return "Unknown response"; // NOI18N
            }
        } else {
            return "NO TYPE"; // NOI18N
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
    }
}
