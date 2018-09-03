/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.truffle.stagent;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 *
 * @author Tomas Hurka
 */
public class TruffleJMX {

    static boolean DEBUG = true;

    /**
     * @param args the command line arguments
     */
    public static void agentmain(final String agentArgs, final Instrumentation inst) throws MalformedObjectNameException, InstantiationException, IllegalAccessException, InterruptedException {
        Class TruffleClass = null;
        try {
            ClassLoader systemCl = ClassLoader.getSystemClassLoader();
            Class contextClass = systemCl.loadClass("org.graalvm.polyglot.Context");
            Method createMethod = contextClass.getMethod("create", String[].class);
            Object context = createMethod.invoke(null, new Object[] {new String[0]});
            if (DEBUG) System.out.println("Context " + context.getClass());
            if (DEBUG) System.out.println("Context ClassLoader" + context.getClass().getClassLoader());
            Field implField = context.getClass().getDeclaredField("impl");
            implField.setAccessible(true);
            Object impl = implField.get(context);
            if (DEBUG) System.out.println("Context Impl: " + impl);
            if (DEBUG) System.out.println("Context Impl: " + impl.getClass().getClassLoader());
            URL classUrl = ClassLoader.getSystemResource("org/graalvm/visualvm/sampler/truffle/stagent/Truffle.class");
            JarURLConnection connection = (JarURLConnection) classUrl.openConnection();
            if (DEBUG) System.out.println("URL "+classUrl);
            if (DEBUG) System.out.println("URL "+connection.getJarFileURL());
            TruffleClassLoader truffleLoader = new TruffleClassLoader(impl.getClass().getClassLoader());
            URLClassLoader ur = new AgentClassLoader(new URL[] {connection.getJarFileURL()}, truffleLoader);
            if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.api.TruffleStackTraceElement"));
            if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.api.impl.TruffleLocator"));
            if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.tools.profiler.StackTraces"));
            TruffleClass = ur.loadClass(Truffle.class.getName());
            if (DEBUG) System.out.println("Class "+TruffleClass+" ClassLoader "+TruffleClass.getClassLoader());
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        }
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName mxbeanName = new ObjectName("com.truffle:type=Threading");
        try {
            mbs.registerMBean(TruffleClass.newInstance(), mxbeanName);
        } catch (InstanceAlreadyExistsException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MBeanRegistrationException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotCompliantMBeanException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
