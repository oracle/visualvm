/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sa;

import org.graalvm.visualvm.tools.sa.SaModel;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Properties;
import org.openide.ErrorManager;

/**
 *
 * @author Tomas Hurka
 */
public class SaModelImpl extends SaModel {
    private Agent agent;
    private int pid;
    String executable;
    String core;
    private Properties sysProp;
    private String jvmFlags;
    private String jvmArgs;
    private String commandLine;
    
    SaModelImpl(File jdkHome,File saLib,int id) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException, InvocationTargetException, NoSuchMethodException {
        agent = Agent.getAgent(jdkHome,saLib);
        pid = id;
        readData();
    }
    
    SaModelImpl(File jdkHome,File saLib,File execFile,File coreFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, MalformedURLException, NoSuchMethodException {
        agent = Agent.getAgent(jdkHome,saLib);
        executable = execFile.getAbsolutePath();
        core = coreFile.getAbsolutePath();
        readData();
    }
    
    public Properties getSystemProperties() {
        return sysProp;
    }
    
    public boolean takeHeapDump(String file){
        try {
            synchronized (agent) {
                try {
                    if (attach()) {
                        SAObject hprofWrite = agent.getHeapHprofBinWriter();
                        hprofWrite.invoke("write",file);    // NOI18N
                        return true;
                    }
                } finally {
                    agent.detach();
                }
            }
        } catch (Exception ex) {
            Throwable e = ex.getCause();
            ErrorManager.getDefault().notify(e == null ? ex : e);
        }
        return false;
    }
    
    public String takeThreadDump(){
        try {
            synchronized (agent) {
                try {
                    if (attach()) {
                        return new StackTrace(agent.getVM()).getStackTrace();
                    }
                } finally {
                    agent.detach();
                }
            }
        } catch (Exception ex) {
            Throwable e = ex.getCause();
            ErrorManager.getDefault().notify(e == null ? ex : e);
        }
        return null;
    }
    
    public String getJvmFlags() {
        return jvmFlags;
    }
    
    public String getJvmArgs() {
        return jvmArgs;
    }
    
    public String getJavaCommand() {
        return commandLine;
    }
    
    private boolean attach() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (core == null) {
            return agent.attach(pid);
        }
        return agent.attach(executable,core);
    }
    
    private void readData() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        synchronized (agent) {
            try {
                if (attach()) {
                    Arguments args = agent.getArguments();
                    jvmFlags = args.getJVMFlags();
                    jvmArgs = args.getJVMArgs();
                    commandLine = args.getJavaCommand();
                    sysProp = (Properties)agent.getVM().getSystemProperties().clone();
                }
            } finally {
                agent.detach();
            }
        }
    }
}
