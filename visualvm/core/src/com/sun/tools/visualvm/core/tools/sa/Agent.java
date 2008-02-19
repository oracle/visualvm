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

package com.sun.tools.visualvm.core.tools.sa;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author Tomas Hurka
 */
class Agent {

    private static Map<File,Agent> agentMap = new WeakHashMap();
    
    static Agent getAgent(File jdkHome,File saJarFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException  {
        synchronized (agentMap) {
            Agent agent = agentMap.get(saJarFile);
            if (agent == null) {
                agent = new Agent(jdkHome,saJarFile);
                agentMap.put(saJarFile,agent);
            }
            return agent;
        }
    }
    private SAWrapper saClassLoader;
    private final SAObject bugspotAgent;
    private VM vm;
    private Arguments args;

    private Agent(File jdkHome,File saJarFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException {
        saClassLoader = new SAWrapper(jdkHome,saJarFile);
        bugspotAgent = new SAObject(saClassLoader.BugspotAgent().newInstance());
    }

    boolean attach(int pid) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        bugspotAgent.invoke("attach",pid);
        return isJavaMode();
    }

    boolean attach(String executable,String coredump) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        bugspotAgent.invoke("attach",executable,coredump);
        return isJavaMode();
    }
    
    boolean attach(String remoteServer) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        bugspotAgent.invoke("attach",remoteServer);
        return isJavaMode();
    }
    
    void detach() throws IllegalAccessException, InvocationTargetException {
        bugspotAgent.invoke("detach");
    }

    VM getVM() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (vm == null) {
            Class vmClass = saClassLoader.VM();
            Object saVM = vmClass.getMethod("getVM").invoke(null);
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
    
    
    private boolean isJavaMode() throws IllegalAccessException, InvocationTargetException {
        Boolean b = (Boolean) bugspotAgent.invoke("isJavaMode");
        return b.booleanValue();
    }    
    
}
