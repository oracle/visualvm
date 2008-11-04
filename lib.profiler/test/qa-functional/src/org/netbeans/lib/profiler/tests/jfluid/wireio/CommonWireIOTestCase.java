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

package org.netbeans.lib.profiler.tests.jfluid.wireio;

import org.netbeans.lib.profiler.tests.jfluid.CommonProfilerTestCase;
import org.netbeans.lib.profiler.wireprotocol.Command;
import org.netbeans.lib.profiler.wireprotocol.WireIO;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


/**
 *
 * @author ehucka
 */
public class CommonWireIOTestCase extends CommonProfilerTestCase {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    protected class LoggingThread extends Thread {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        boolean prepared = false;
        private boolean running = true;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isPrepared() {
            return prepared;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public boolean isRunning() {
            return running;
        }

        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                ref("Server start to listen on port " + String.valueOf(PORT));
                prepared = true;

                Socket clientSocket = serverSocket.accept();

                WireIO wireIO = createWireIO(clientSocket);

                while (running) {
                    running &= simpleLogCommands(wireIO);
                }

                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static int PORT = 5140;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of CommonWireIOTestCase */
    public CommonWireIOTestCase(String name) {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public WireIO createWireIO(Socket clientSocket) {
        try {
            clientSocket.setTcpNoDelay(true); // Necessary at least on Solaris to avoid delays in e.g. readInt() etc.

            ObjectInputStream socketIn = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream socketOut = new ObjectOutputStream(clientSocket.getOutputStream());
            WireIO wireIO = new WireIO(socketOut, socketIn);

            return wireIO;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public WireIO createWireIOClient(Socket clientSocket) {
        try {
            clientSocket.setSoTimeout(0);
            clientSocket.setTcpNoDelay(true); // Necessary at least on Solaris to avoid delays in e.g. readInt() etc.

            ObjectOutputStream socketOut = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream socketIn = new ObjectInputStream(clientSocket.getInputStream());
            WireIO wireIO = new WireIO(socketOut, socketIn);

            return wireIO;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public boolean simpleLogCommands(WireIO wireIO) {
        try {
            Object o = wireIO.receiveCommandOrResponse();

            if (o == null) {
                ref("Connection interrupted.");

                return false;
            } else {
                if (o instanceof Command) {
                    ref(" received command " + o.toString());
                } else {
                    ref(" received object " + o.getClass().getName() + " " + o.toString());
                }
            }
        } catch (IOException ex) {
            return false;
        }

        return true;
    }
}
