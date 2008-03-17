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

package com.sun.tools.visualvm.saagent;

import com.sun.tools.visualvm.tools.sa.SaModel;
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
    
    SaModelImpl(File jdkHome,File sajar,int id) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException, InvocationTargetException, NoSuchMethodException {
        agent = Agent.getAgent(jdkHome,sajar);
        pid = id;
        readData();
    }
    
    SaModelImpl(File jdkHome,File sajar,File execFile,File coreFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, MalformedURLException, NoSuchMethodException {
        agent = Agent.getAgent(jdkHome,sajar);
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
                        hprofWrite.invoke("write",file);
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
    
    public String getJVMFlags() {
        return jvmFlags;
    }
    
    public String getJVMArgs() {
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
                    sysProp = agent.getVM().getSystemProperties();
                }
            } finally {
                agent.detach();
            }
        }
    }
}