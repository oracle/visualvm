/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.saplugin;

import org.graalvm.visualvm.tools.sa.SaModel;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Properties;
import org.openide.ErrorManager;
import org.openide.util.Exceptions;


public class SAModelImpl extends SaModel {
    private Agent agent;
    private int pid;
    String executable;
    String core;
    private Properties sysProp;
    private String jvmFlags;
    private String jvmArgs;
    private String commandLine;
    private SAView view;
    
    
    SAModelImpl(File jdkHome,File sajar,int id) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException, InvocationTargetException, NoSuchMethodException {
        agent = Agent.createAgent(jdkHome,sajar);
        pid = id;
        //readData();
    }
    
    SAModelImpl(File jdkHome,File sajar,File execFile,File coreFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, MalformedURLException, NoSuchMethodException {
        agent = Agent.createAgent(jdkHome,sajar);
        executable = execFile.getAbsolutePath();
        core = coreFile.getAbsolutePath();
        //readData();
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
  
    public boolean attach() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        if (core == null) {
            return agent.attach(pid);
        }
        return agent.attach(executable,core);
    }
    public void detach() throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        agent.detach();
    }

    public Agent getAgent() {
        return agent;
    }
    public void readData() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        synchronized (agent) {
            //try {
            //    if (attach()) {
                    Arguments args = agent.getArguments();
                    jvmFlags = args.getJVMFlags();
                    jvmArgs = args.getJVMArgs();
                    commandLine = args.getJavaCommand();
                    sysProp = (Properties)agent.getVM().getSystemProperties().clone();
             //   }
          //  } finally {
          //      agent.detach();
          //  }
        }
    }
    
//------------------------------------------------------------//
    
    public Inspector createOopInspector() {
        Inspector obj = null;
        try {
        obj = agent.createOopInspector();
        } catch (Exception e) {
        }
        return obj;
    }
    public Inspector createOopInspector(Object o) {
        Inspector obj = null;
        try {
        obj = agent.createOopInspector(o);
        } catch (Exception e) {
        }
        return obj;
    }
    
    public JavaStackTracePanel createJavaStackTracePanel() {
        JavaStackTracePanel obj = null;
        try {
        obj = agent.createJavaStackTracePanel();
        } catch (Exception e) {
        }
        return obj;
    }
    public JavaThreadsPanel createJavaThreadsPanel() {
        JavaThreadsPanel obj = null;
        try {
        obj = agent.createJavaThreadsPanel();
        } catch (Exception e) {
        }
        return obj;
    }
    public CodeViewerPanel createCodeViewerPanel() {
        CodeViewerPanel obj = null;
        try {
        obj = agent.createCodeViewerPanel();
        } catch (Exception e) {
        }
        return obj;
    }
    public FindPointerPanel createFindPointerPanel() {
        FindPointerPanel obj = null;
        try {
        obj = agent.createFindPointerPanel();
        } catch (Exception e) {
        }
        return obj;
    }
    public FindInHeapPanel createFindInHeapPanel() {
        FindInHeapPanel obj = null;
        try {
            obj = agent.createFindInHeapPanel();
        } catch (Exception e) {
        }
        return obj;
    }

    public FindInCodeCachePanel createFindInCodeCachePanel() {
        FindInCodeCachePanel obj = null;
        try {
            obj = agent.createFindInCodeCachePanel();
        } catch (Exception e) {
        }
        return obj;
    }
            
    public OopTreeNodeAdapter createOopTreeNodeAdapter(Object oopObject, Object fieldID) {
        OopTreeNodeAdapter obj = null;
        try {
        obj = agent.createOopTreeNodeAdapter(oopObject, fieldID);
        } catch (Exception e) {
        }
        return obj;
    }
    public Object getSAListener() {
        Object obj = null;
        try {
            obj = agent.SAListener(this);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstantiationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        }
        return obj;
    }

    public SAView getView() {
        return view;
    }

    public void setView(SAView view) {
        this.view = view;
    }
    
}
