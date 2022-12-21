/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author Tomas Hurka
 */
class Agent {

    private static Map<File,Agent> agentMap = new HashMap<>();
    
    static Agent getAgent(File jdkHome,File saLibFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException  {
        synchronized (agentMap) {
            Agent agent = agentMap.get(saLibFile);
            if (agent == null) {
                agent = new Agent(jdkHome,saLibFile);
                agentMap.put(saLibFile,agent);
            }
            return agent;
        }
    }
    
    private SAWrapper saClassLoader;
    private final SAObject hotspotAgent;
    private VM vm;
    private Arguments args;

    private Agent(File jdkHome,File saLibFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, MalformedURLException {
        saClassLoader = new SAWrapper(jdkHome,saLibFile);
        hotspotAgent = new SAObject(saClassLoader.HotSpotAgent().newInstance());
    }

    boolean attach(int pid) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        hotspotAgent.invoke("attach",pid);  // NOI18N
        return true;
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
            Class<?> vmClass = saClassLoader.VM();
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
    
}
