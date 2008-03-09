/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package net.java.visualvm.btrace.datasource;

import com.sun.btrace.CommandListener;
import com.sun.btrace.client.Client;
import com.sun.btrace.comm.Command;
import com.sun.btrace.comm.MessageCommand;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class DeployTask implements Runnable {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");
    private Client client;
    private byte[] probeCode;
    final PipedOutputStream pos;
    final PipedInputStream pis;
    final PrintWriter probeWriter;
    final BufferedReader probeReader;

    public DeployTask(Client client, byte[] probeCode) {
        try {
            this.client = client;
            this.probeCode = probeCode;
            pos = new PipedOutputStream();
            pis = new PipedInputStream(pos);
            probeWriter = new PrintWriter(pos);
            probeReader = new BufferedReader(new InputStreamReader(pis));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public void stop() {
        try {
            client.sendExit();
        } catch (IOException e) {
        }
    }

    public Reader getReader() {
        return probeReader;
    }

    public void run() {
        try {
            client.submit(probeCode, new String[0], new CommandListener() {

                volatile private ProbeDataSource pds = null;

                public void onCommand(Command cmd) throws IOException {
                    switch (cmd.getType()) {
                        case Command.MESSAGE: {
                            if (pds == null) {
                                break;
                            }
                            probeWriter.println(DATE_FORMAT.format(new Date(((MessageCommand) cmd).getTime())) + " : " + ((MessageCommand) cmd).getMessage());
                            probeWriter.flush();
                            break;
                        }
                        case Command.EXIT: {
                            probeWriter.println("Probe finished ...");
                            probeWriter.flush();
                            probeWriter.close();
                            pos.close();
                            client.close();

                            if (pds != null) {
                                removeProbe(pds);
                            }
                            break;
                        }
                        case Command.SUCCESS: {
                            probeWriter.println("Probe initialized ...");
                            probeWriter.flush();

                            pds = prepareProbe(DeployTask.this);
                        }
                    }
                }
            });

        } catch (IOException iOException) {
            iOException.printStackTrace();
        }

    }

    abstract protected ProbeDataSource prepareProbe(DeployTask deployer);
    abstract protected void removeProbe(ProbeDataSource pds);
}
