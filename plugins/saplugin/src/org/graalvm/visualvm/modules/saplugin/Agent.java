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

import org.graalvm.visualvm.core.datasource.DataSource;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;
import org.openide.util.Exceptions;

class Agent {

    private static Map<DataSource,Agent> agentMap = new HashMap();
    private static Map<File,SAWrapper> classloaderMap = new HashMap();
    
    
    static Agent createAgent(File jdkHome,File saJarFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException  {
       /*synchronized (agentMap) {
            Agent agent = agentMap.get(saJarFile);
            if (agent == null) {
                agent = new Agent(jdkHome,saJarFile);
                agentMap.put(saJarFile,agent);
            }
            return agent;
        }
      */
        Agent agent = new Agent(jdkHome,saJarFile);
        return agent;
    }
    
    private SAWrapper saClassLoader = null;
    //private final SAObject bugspotAgent;
    private final SAObject hotspotAgent;
    private VM vm;
    private Arguments args;
    private Object saListener = null;

    private Agent(File jdkHome,File saJarFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException {
     //   saClassLoader = classloaderMap.get(jdkHome);
     //   if (saClassLoader == null) {
            saClassLoader = new SAWrapper(jdkHome,saJarFile);
      //      classloaderMap.put(jdkHome, saClassLoader);
      //  }
        hotspotAgent = new SAObject(saClassLoader.HotspotAgent().newInstance());
    }

    boolean attach(int pid) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        hotspotAgent.invoke("attach",pid);  // NOI18N
        return true; //isJavaMode();
    }

    boolean attach(String executable,String coredump) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        hotspotAgent.invoke("attach",executable,coredump);  // NOI18N
        return true;
    }
    
    boolean attach(String remoteServer) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        hotspotAgent.invoke("attach",remoteServer); // NOI18N
        return true;
    }
    
    void detach() throws IllegalAccessException, InvocationTargetException {
        hotspotAgent.invoke("detach");  // NOI18N
    }

    VM getVM() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (vm == null) {
            Class vmClass = saClassLoader.VM();
            Object saVM = vmClass.getMethod("getVM").invoke(null);  // NOI18N
            vm = new VM(saVM);
        }
        return vm;
    }
    
    Arguments getArguments() throws ClassNotFoundException {
        if (args == null) {
            args = new Arguments(saClassLoader.Arguments());
        }
        return args;
    }
    
    SAObject getHeapHprofBinWriter() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new SAObject(saClassLoader.HeapHprofBinWriter().newInstance());
    }
       
    
/*    private boolean isJavaMode() throws IllegalAccessException, InvocationTargetException {
        Boolean b = (Boolean) hotspotAgent.invoke("isJavaMode");    // NOI18N
        return b.booleanValue();
    }  
    */
/////////////////////////////////////////////////
    
    
    JavaStackTracePanel createJavaStackTracePanel() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new JavaStackTracePanel(saClassLoader.JavaStackTracePanel().newInstance());
    }
    JavaThreadsPanel createJavaThreadsPanel() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new JavaThreadsPanel(saClassLoader.JavaThreadsPanel().newInstance());
    }
    CodeViewerPanel createCodeViewerPanel() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new CodeViewerPanel(saClassLoader.CodeViewerPanel().newInstance());
    }
    FindPointerPanel createFindPointerPanel() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new FindPointerPanel(saClassLoader.FindPanel().newInstance());
    }
    FindInHeapPanel createFindInHeapPanel() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new FindInHeapPanel(saClassLoader.FindInHeapPanel().newInstance());
    }
    FindInCodeCachePanel createFindInCodeCachePanel() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new FindInCodeCachePanel(saClassLoader.FindInCodeCachePanel().newInstance());
    }
    
    
    Inspector createOopInspector() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return new Inspector(saClassLoader.OopInspector().newInstance());
    }
    Inspector createOopInspector(Object root) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Constructor con = saClassLoader.OopInspector().getDeclaredConstructor(new Class[] { saClassLoader.SimpleTreeNode()});
        Object instance = null;
        try {
            instance = con.newInstance(root);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return new Inspector(instance);
    }
    OopTreeNodeAdapter createOopTreeNodeAdapter(Object oopObject, Object fieldID) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        //return new OopTreeNodeAdapter(saClassLoader.OopTreeNodeAdapter().newInstance());
        Constructor con = saClassLoader.OopTreeNodeAdapter().getDeclaredConstructor(new Class[] { saClassLoader.Oop(), saClassLoader.FieldIdentifier() });
        Object instance = null;
        try {
            instance = con.newInstance(new Object[]{oopObject, fieldID});
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new OopTreeNodeAdapter(instance);
    }
    
    Object SAListener(SAModelImpl model) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      if (saListener == null) {
            saListener =  Proxy.newProxyInstance(saClassLoader.loader, new Class[] {saClassLoader.SAListener()}, new ProxyListener(model));
      }
        return saListener;
    }
}
