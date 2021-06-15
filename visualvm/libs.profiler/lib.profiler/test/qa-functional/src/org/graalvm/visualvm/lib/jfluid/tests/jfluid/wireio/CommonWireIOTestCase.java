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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.wireio;

import org.graalvm.visualvm.lib.jfluid.tests.jfluid.CommonProfilerTestCase;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.Command;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.WireIO;
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
