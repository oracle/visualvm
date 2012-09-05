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
 * Implementation of our custom serialization. We use our own, specialized, simplified and optimized flavor of
 * object serialization, to improve performance and avoid various things that standard serialization performes
 * behind the scenes, such as loading of many classes, generating some classes on-the-fly, and so on. These
 * actions can cause various undesirable side effects when used for such a sensitive thing as profiling.
 * However, this kind of serialization is not completely automatic, and some manual changes in this class are
 * required every time a new Command or Response subclass is created. Read the comments in this file to see
 * where the changes should be made.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class WireIO {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.wireprotocol.WireIO") != null; // NOI18N
    private static final int IS_SIMPLE_COMMAND = 1;
    private static final int IS_COMPLEX_COMMAND = 2;
    private static final int IS_SIMPLE_RESPONSE = 3;
    private static final int IS_COMPLEX_RESPONSE = 4;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private long wasAlive;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public WireIO(ObjectOutputStream pout, ObjectInputStream pin) {
        out = pout;
        in = pin;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object receiveCommandOrResponse() throws IOException {
        int code = in.read();

        /*if (code == -1) {
           if (DEBUG) System.out.println("WireIO.DEBUG: received end of stream code");
           return null; // end of stream, no more data
           }*/
        wasAlive = System.currentTimeMillis();
        switch (code) {
            case IS_SIMPLE_COMMAND:

                Command c = new Command(in.read());

                if (DEBUG) {
                    System.out.println("WireIO.DEBUG: received simple command: " + c); // NOI18N
                }

                return c;
            case IS_COMPLEX_COMMAND: {
                int cmdType = in.read();

                if (DEBUG) {
                    System.out.println("WireIO.DEBUG: received complex command type: " // NOI18N
                                       + Command.cmdTypeToString(cmdType));
                }

                Command cmd = null;

                switch (cmdType) {
                    case Command.METHOD_INVOKED_FIRST_TIME:
                        cmd = new MethodInvokedFirstTimeCommand();

                        break;
                    case Command.CLASS_LOADED:
                        cmd = new ClassLoadedCommand();

                        break;
                    case Command.MESSAGE:
                        cmd = new AsyncMessageCommand();

                        break;
                    case Command.METHOD_LOADED:
                        cmd = new MethodLoadedCommand();

                        break;
                    case Command.ROOT_CLASS_LOADED:
                        cmd = new RootClassLoadedCommand();

                        break;
                    case Command.INITIATE_PROFILING:
                        cmd = new InitiateProfilingCommand();

                        break;
                    case Command.SET_CHANGEABLE_INSTR_PARAMS:
                        cmd = new SetChangeableInstrParamsCommand();

                        break;
                    case Command.SET_UNCHANGEABLE_INSTR_PARAMS:
                        cmd = new SetUnchangeableInstrParamsCommand();

                        break;
                    case Command.EVENT_BUFFER_DUMPED:
                        cmd = new EventBufferDumpedCommand();

                        break;
                    case Command.INSTRUMENT_METHOD_GROUP:
                        cmd = new InstrumentMethodGroupCommand();

                        break;
                    case Command.GET_METHOD_NAMES_FOR_JMETHOD_IDS:
                        cmd = new GetMethodNamesForJMethodIdsCommand();

                        break;
                    case Command.GET_DEFINING_CLASS_LOADER:
                        cmd = new GetDefiningClassLoaderCommand();

                        break;
                    case Command.TAKE_HEAP_DUMP:
                        cmd = new TakeHeapDumpCommand();

                        break;
                    case Command.GET_CLASSID:
                        cmd = new GetClassIdCommand();

                        break;
                    default:
                        throw new IOException("JFluid wire protocol error: received unknown command type. Value: " // NOI18N
                                              + cmdType);
                }

                cmd.readObject(in);

                if (DEBUG) {
                    System.out.println("WireIO.DEBUG:         command is: " + cmd); // NOI18N
                }

                return cmd;
            }
            case IS_SIMPLE_RESPONSE:

                Response simpleResp = new Response(in.readBoolean());

                if (in.read() != 0) {
                    simpleResp.setErrorMessage(in.readUTF());
                } else {
                    simpleResp.setErrorMessage(null);
                }

                if (DEBUG) {
                    System.out.println("WireIO.DEBUG: received simple response " + simpleResp); // NOI18N
                }

                return simpleResp;
            case IS_COMPLEX_RESPONSE:

                int respType = in.read();

                if (DEBUG) {
                    System.out.println("WireIO.DEBUG: received complex response " // NOI18N
                                       + Response.respTypeToString(respType));
                }

                Response resp = null;

                switch (respType) {
                    case Response.CODE_REGION_CPU_RESULTS:
                        resp = new CodeRegionCPUResultsResponse();

                        break;
                    case Response.INSTRUMENT_METHOD_GROUP:
                        resp = new InstrumentMethodGroupResponse();

                        break;
                    case Response.INTERNAL_STATS:
                        resp = new InternalStatsResponse();

                        break;
                    case Response.VM_PROPERTIES:
                        resp = new VMPropertiesResponse();

                        break;
                    case Response.DUMP_RESULTS:
                        resp = new DumpResultsResponse();

                        break;
                    case Response.OBJECT_ALLOCATION_RESULTS:
                        resp = new ObjectAllocationResultsResponse();

                        break;
                    case Response.METHOD_NAMES:
                        resp = new MethodNamesResponse();

                        break;
                    case Response.THREAD_LIVENESS_STATUS:
                        resp = new ThreadLivenessStatusResponse();

                        break;
                    case Response.MONITORED_NUMBERS:
                        resp = new MonitoredNumbersResponse();

                        break;
                    case Response.DEFINING_LOADER:
                        resp = new DefiningLoaderResponse();

                        break;
                    case Response.CALIBRATION_DATA:
                        resp = new CalibrationDataResponse();

                        break;
                    case Response.CLASSID_RESPONSE:
                        resp = new GetClassIdResponse();

                        break;
                    case Response.HEAP_HISTOGRAM:
                        resp = new HeapHistogramResponse();
                        
                        break;
                    default:
                        throw new IOException("JFluid wire protocol error: received unknown response type. Value: " + respType); // NOI18N
                }

                resp.setYes(in.readBoolean());

                if (in.read() != 0) {
                    resp.setErrorMessage(in.readUTF());
                } else {
                    resp.setErrorMessage(null);
                }

                resp.readObject(in);

                if (DEBUG) {
                    System.out.println("WireIO.DEBUG:    response is: " + resp); // NOI18N
                }

                return resp;
            default:

                if (DEBUG) {
                    System.out.println("WireIO.DEBUG: received unknown code: " + code); // NOI18N
                }

                throw new IOException("JFluid wire protocol error: code does not correspond to command or response. Value " + code); // NOI18N
        }
    }

    public synchronized void sendComplexCommand(Command cmd)
                                         throws IOException {
        if (DEBUG) {
            System.out.println("WireIO.DEBUG: gonna send complex command: " + cmd); // NOI18N
        }

        out.write(IS_COMPLEX_COMMAND);
        out.write(cmd.getType());
        cmd.writeObject(out);
        out.flush();
    }

    public synchronized void sendComplexResponse(Response resp)
                                          throws IOException {
        if (DEBUG) {
            System.out.println("WireIO.DEBUG: gonna send response: " + resp); // NOI18N
        }

        out.write(IS_COMPLEX_RESPONSE);
        out.write(resp.getType());
        out.writeBoolean(resp.yes());

        String errorMessage = resp.getErrorMessage();

        if (errorMessage == null) {
            out.write(0);
        } else {
            out.write(1);
            out.writeUTF(errorMessage);
        }

        resp.writeObject(out);
        out.flush();
    }

    public synchronized void sendSimpleCommand(int cmdType)
                                        throws IOException {
        if (DEBUG) {
            System.out.println("WireIO.DEBUG: gonna send simple command:" + Command.cmdTypeToString(cmdType)); // NOI18N
        }

        out.write(IS_SIMPLE_COMMAND);
        out.write(cmdType);
        out.flush();
    }

    public synchronized void sendSimpleResponse(boolean yes, String errorMessage)
                                         throws IOException {
        if (DEBUG) {
            System.out.println("WireIO.DEBUG: gonna send simple response: yes: " + yes // NOI18N
                               + ", errorMessage: " + errorMessage // NOI18N
                               );
        }

        out.write(IS_SIMPLE_RESPONSE);
        out.writeBoolean(yes);

        if (errorMessage == null) {
            out.write(0);
        } else {
            out.write(1);
            out.writeUTF(errorMessage);
        }

        out.flush();
    }

    public long wasAlive() {
        return wasAlive;
    }
}
