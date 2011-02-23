/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.modules.tracer.dtrace;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.SessionInitializationException;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import com.sun.tools.visualvm.modules.tracer.TracerProbeDescriptor;
import com.sun.tools.visualvm.modules.tracer.TracerProgressObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.opensolaris.os.dtrace.Consumer;
import org.opensolaris.os.dtrace.DTraceException;
import org.opensolaris.os.dtrace.LocalConsumer;

/**
 *
 * @author Tomas Hurka
 */
class DTracePackage extends TracerPackage.SessionAware<Application>{

    static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(
            "com/sun/tools/visualvm/modules/tracer/dtrace/resources/cpuSmall.png", true)); // NOI18N
    private static final String NAME = "DTrace Probes";
    private static final String DESCR = "Provides low level system metrics using the DTrace technology.";
    private static final int POSITION = 120;
    private static final String DSCRIPT_FILE =
            "com/sun/tools/visualvm/modules/tracer/dtrace/resources/probes.d"; // NOI18N

    private Consumer consumer;
    private Application application;
    private String probe;
    private TracerProgressObject progress;
    private TracerProbeDescriptor cpusDesc;
    private TracerProbeDescriptor syscallDesc;
    private TracerProbeDescriptor bytesIODesc;
    private TracerProbeDescriptor jvmOverhead;

    DTracePackage(Application app) {
        super(NAME,DESCR,ICON,POSITION);
        application = app;
    }

    @Override
    public TracerProbeDescriptor[] getProbeDescriptors() {
        cpusDesc = CpusMonitorProbe.createDescriptor(ICON, true);
        syscallDesc = SyscallsProbe.createDescriptor(ICON, true);
        bytesIODesc = BytesIOProbe.createDescriptor(ICON, true);
        jvmOverhead = JVMOverheadProbe.createDescriptor(ICON, true);
        return new TracerProbeDescriptor[] { cpusDesc, syscallDesc, bytesIODesc, jvmOverhead };
    }

    @Override
    public TracerProbe<Application> getProbe(TracerProbeDescriptor descriptor) {
        if (descriptor == cpusDesc) {
            return new CpusMonitorProbe(this);
        }
        if (descriptor == syscallDesc) {
            return new SyscallsProbe(this);
        }
        if (descriptor == bytesIODesc) {
            return new BytesIOProbe(this);
        }
        if (descriptor == jvmOverhead) {
            return new JVMOverheadProbe(this);
        }
        return null;
    }

    @Override
    protected TracerProgressObject sessionInitializing(TracerProbe<Application>[] probes, Application app, int refresh) {
        return progress = new TracerProgressObject(5);
    }


    @Override
    protected void sessionStarting(TracerProbe<Application>[] probes, Application app) throws SessionInitializationException {
        try {
            consumer = new LocalConsumer();
//            consumer.addConsumerListener(new ConsumerAdapter() {
//
//                @Override
//                public void dataReceived(DataEvent e) throws ConsumerException {
//                    System.out.println(e);
//                }
//
//            });
            progress.addStep("Loading script");
            probe = readScript(DSCRIPT_FILE);
            progress.addStep("Opening dtrace connection");
            consumer.open();
            progress.addStep("Attaching to process");
            consumer.grabProcess(app.getPid());
            progress.addStep("Compiling");
            consumer.compile(probe, (String[])null);
            progress.addStep("Enabling");
            consumer.enable();
        } catch (IOException ex) {
            throw new SessionInitializationException(ex.getMessage(), ex);
        } catch (DTraceException ex) {
            throw new SessionInitializationException(ex.getMessage(), ex);
        }
    }

    @Override
    protected void sessionRunning(TracerProbe<Application>[] probes, Application app) {
        try {
            consumer.go();
        } catch (DTraceException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    protected void sessionStopping(TracerProbe<Application>[] probes, Application dataSource) {
        consumer.stop();
        consumer.close();
    }

    int getProcessors() {
        return ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    }

    private static String readScript(String scriptFile) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(DTracePackage.class.
                getClassLoader().getResourceAsStream(scriptFile)));
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null)
            sb.append(line).append("\n"); // NOI18N
        return sb.toString();
    }

    Consumer getConsumer() {
        return consumer;
    }

}
