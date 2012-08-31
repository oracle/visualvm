/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Base class for all Commands, i.e. wire protocol messages that can be sent by both client and
 * server at any moment. Some Commands contain nothing but their type, and therefore instances of
 * this Command class can be used to transfer them. Others contain additional information, and
 * therefore specialized subclasses of Command are used for them.
 *
 * @author Misha Dmitriev
 */
public class Command {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int CHECK_CONNECTION = 1;
    public static final int START_TARGET_APP = 2;
    public static final int CLASS_LOADED = 3;
    public static final int SET_CHANGEABLE_INSTR_PARAMS = 4;
    public static final int SET_UNCHANGEABLE_INSTR_PARAMS = 5;
    public static final int CPU_RESULTS_EXIST = 6;
    public static final int INSTRUMENT_METHOD_GROUP = 7;
    public static final int GET_CODE_REGION_CPU_RESULTS = 8;
    public static final int DEACTIVATE_INJECTED_CODE = 9;
    public static final int SUSPEND_TARGET_APP = 10;
    public static final int RESUME_TARGET_APP = 11;
    public static final int TERMINATE_TARGET_JVM = 12;
    public static final int INITIATE_PROFILING = 13;
    public static final int MESSAGE = 14;
    public static final int SHUTDOWN_OK = 15; // profiled VM can proceed with shutdown
    public static final int GET_THREAD_LIVENESS_STATUS = 16;
    public static final int ROOT_CLASS_LOADED = 17;
    public static final int SHUTDOWN_INITIATED = 18; // profiled VM shutdown initiated
    public static final int SHUTDOWN_COMPLETED = 19; // profiled VM shutdown completed, clean up
    public static final int INSTRUMENT_REFLECTION = 20;
    public static final int DEINSTRUMENT_REFLECTION = 21;
    public static final int METHOD_LOADED = 22;
    public static final int METHOD_INVOKED_FIRST_TIME = 23;
    public static final int GET_INTERNAL_STATS = 24;
    public static final int DETACH = 25;
    public static final int EVENT_BUFFER_DUMPED = 26;
    public static final int DUMP_EXISTING_RESULTS = 27;
    public static final int GET_VM_PROPERTIES = 28;
    public static final int RESET_PROFILER_COLLECTORS = 29;
    public static final int GET_OBJECT_ALLOCATION_RESULTS = 30;
    public static final int GET_METHOD_NAMES_FOR_JMETHOD_IDS = 31;
    public static final int GET_MONITORED_NUMBERS = 32;
    public static final int RUN_GC = 33;
    public static final int RUN_CALIBRATION_AND_GET_DATA = 34;
    public static final int GET_DEFINING_CLASS_LOADER = 35;
    public static final int CLASS_LOADER_UNLOADING = 36;
    public static final int GET_STORED_CALIBRATION_DATA = 37;
    public static final int RESULTS_AVAILABLE = 38;
    public static final int TAKE_SNAPSHOT = 39;
    public static final int DUMP_EXISTING_RESULTS_LIVE = 40;
    public static final int TAKE_HEAP_DUMP = 41;
    public static final int GET_CLASSID = 42;
    public static final int STILL_ALIVE = 43;
    public static final int PREPARE_DETACH = 44;
    public static final int GET_HEAP_HISTOGRAM = 45;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int type; // One of the above constants determining the Command type.

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public Command(int type) {
        this.type = type;
    }

    // Custom serialization support
    Command() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getType() {
        return type;
    }

    public static String cmdTypeToString(int type) {
        switch (type) {
            case CHECK_CONNECTION:
                return "CHECK_CONNECTION"; // NOI18N
            case START_TARGET_APP:
                return "START_TARGET_APP"; // NOI18N
            case CLASS_LOADED:
                return "CLASS_LOADED"; // NOI18N
            case SET_CHANGEABLE_INSTR_PARAMS:
                return "SET_CHANGEABLE_INSTR_PARAMS"; // NOI18N
            case SET_UNCHANGEABLE_INSTR_PARAMS:
                return "SET_UNCHANGEABLE_INSTR_PARAMS"; // NOI18N
            case CPU_RESULTS_EXIST:
                return "CPU_RESULTS_EXIST"; // NOI18N
            case INSTRUMENT_METHOD_GROUP:
                return "INSTRUMENT_METHOD_GROUP"; // NOI18N
            case GET_CODE_REGION_CPU_RESULTS:
                return "GET_CODE_REGION_CPU_RESULTS"; // NOI18N
            case DEACTIVATE_INJECTED_CODE:
                return "DEACTIVATE_INJECTED_CODE"; // NOI18N
            case SUSPEND_TARGET_APP:
                return "SUSPEND_TARGET_APP"; // NOI18N
            case RESUME_TARGET_APP:
                return "RESUME_TARGET_APP"; // NOI18N
            case TERMINATE_TARGET_JVM:
                return "TERMINATE_TARGET_JVM"; // NOI18N
            case INITIATE_PROFILING:
                return "INITIATE_PROFILING"; // NOI18N
            case MESSAGE:
                return "MESSAGE"; // NOI18N
            case SHUTDOWN_OK:
                return "SHUTDOWN_OK"; // NOI18N
            case GET_THREAD_LIVENESS_STATUS:
                return "GET_THREAD_LIVENESS_STATUS"; // NOI18N
            case ROOT_CLASS_LOADED:
                return "ROOT_CLASS_LOADED"; // NOI18N
            case SHUTDOWN_INITIATED:
                return "SHUTDOWN_INITIATED"; // NOI18N
            case SHUTDOWN_COMPLETED:
                return "SHUTDOWN_COMPLETED"; // NOI18N
            case INSTRUMENT_REFLECTION:
                return "INSTRUMENT_REFLECTION"; // NOI18N
            case DEINSTRUMENT_REFLECTION:
                return "DEINSTRUMENT_REFLECTION"; // NOI18N
            case METHOD_LOADED:
                return "METHOD_LOADED"; // NOI18N
            case METHOD_INVOKED_FIRST_TIME:
                return "METHOD_INVOKED_FIRST_TIME"; // NOI18N
            case GET_INTERNAL_STATS:
                return "GET_INTERNAL_STATS"; // NOI18N
            case DETACH:
                return "DETACH"; // NOI18N
            case EVENT_BUFFER_DUMPED:
                return "EVENT_BUFFER_DUMPED"; // NOI18N
            case DUMP_EXISTING_RESULTS:
                return "DUMP_EXISTING_RESULTS"; // NOI18N
            case GET_VM_PROPERTIES:
                return "GET_VM_PROPERTIES"; // NOI18N
            case RESET_PROFILER_COLLECTORS:
                return "RESET_PROFILER_COLLECTORS"; // NOI18N
            case GET_OBJECT_ALLOCATION_RESULTS:
                return "GET_OBJECT_ALLOCATION_RESULTS"; // NOI18N
            case GET_METHOD_NAMES_FOR_JMETHOD_IDS:
                return "GET_METHOD_NAMES_FOR_JMETHOD_IDS"; // NOI18N
            case GET_MONITORED_NUMBERS:
                return "MONITORED_NUMBERS"; // NOI18N
            case RUN_GC:
                return "RUN_GC"; // NOI18N
            case RUN_CALIBRATION_AND_GET_DATA:
                return "RUN_CALIBRATION_AND_GET_DATA"; // NOI18N
            case GET_DEFINING_CLASS_LOADER:
                return "GET_DEFINING_CLASSLOADER"; // NOI18N
            case CLASS_LOADER_UNLOADING:
                return "CLASS_LOADER_UNLOADING"; // NOI18N
            case GET_STORED_CALIBRATION_DATA:
                return "GET_STORED_CALIBRATION_DATA"; // NOI18N
            case RESULTS_AVAILABLE:
                return "RESULTS_AVAILABLE"; // NOI18N
            case TAKE_SNAPSHOT:
                return "TAKE_SNAPSHOT"; // NOI18N
            case DUMP_EXISTING_RESULTS_LIVE:
                return "DUMP_EXISTING_RESULTS_LIVE"; // NOI18N
            case TAKE_HEAP_DUMP:
                return "TAKE_HEAP_DUMP"; // NOI18N
            case GET_CLASSID:
                return "GET_CLASSID"; // NOI18N
            case STILL_ALIVE:
                return "STILL_ALIVE"; // NOI18N
            case PREPARE_DETACH:
                return "PREPARE_DETACH"; // NOI18N
        }

        return "Unknown command"; // NOI18N
    }

    // For debugging
    public String toString() {
        return cmdTypeToString(type);
    }

    void setType(int type) {
        this.type = type;
    }

    void readObject(ObjectInputStream in) throws IOException {
    }

    void writeObject(ObjectOutputStream out) throws IOException {
    }
}
