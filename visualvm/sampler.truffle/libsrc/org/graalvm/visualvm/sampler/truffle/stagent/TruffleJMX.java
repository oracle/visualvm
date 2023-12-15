/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import sun.misc.Unsafe;

/**
 *
 * @author Tomas Hurka
 */
public class TruffleJMX {

    static boolean DEBUG = false;

    /**
     * @param args the command line arguments
     */
    public static void agentmain(final String agentArgs, final Instrumentation inst) throws MalformedObjectNameException, InstantiationException, IllegalAccessException, InterruptedException {
        try {
            Unsafe unsafe = Unsafe.getUnsafe();
            if (DEBUG) System.out.println("Unsafe "+unsafe);
            Object context = getContext();
            Object impl = getContextImpl(context);
            URL jarURL = getJarURL();
            if (impl == null) impl = context;
            URLClassLoader ur = getSamplerClassLoader(impl, unsafe, jarURL);
            Object truffle = getTruffleInstance(ur, unsafe);

            registerMXBean(truffle);
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
    }

    private static Object getContext() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, SecurityException, IllegalAccessException, IllegalArgumentException {
        // return org.graalvm.polyglot.Context.newBuilder().allowExperimentalOptions(true)
        ClassLoader systemCl = ClassLoader.getSystemClassLoader();
        Class contextClass = systemCl.loadClass("org.graalvm.polyglot.Context");
        Method builderMethod = contextClass.getMethod("newBuilder", String[].class);
        Object builder = builderMethod.invoke(null, new Object[] {new String[0]});
        Method allowExpMethod = builder.getClass().getMethod("allowExperimentalOptions", boolean.class);
        builder = allowExpMethod.invoke(builder, new Object[] {Boolean.TRUE});
        Method buildMethod = builder.getClass().getMethod("build");
        Object context = buildMethod.invoke(builder, new Object[]{});
        if (DEBUG) System.out.println("Context: " + context.getClass());
        if (DEBUG) System.out.println("Context ClassLoader: " + context.getClass().getClassLoader());
        return context;
    }

    private static Object getContextImpl(Object context) throws IllegalArgumentException, SecurityException, NoSuchFieldException, IllegalAccessException {
        // return context.impl or context.receiver or context.dispatch
        Field implField = getDeclaredField(context, "impl", "receiver", "dispatch");
        try {
            implField.setAccessible(true);
            Object impl = implField.get(context);
            if (DEBUG) System.out.println("Context Impl: " + impl);
            if (DEBUG && impl != null) System.out.println("Context Impl ClassLoader: " + impl.getClass().getClassLoader());
            return impl;
        } catch (RuntimeException ex) {
            if (ex.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
                return null;
            }
            throw ex;
        }
    }

    static Field getDeclaredField(Object obj, String... names) throws NoSuchFieldException {
        Map<String,Field> fields = new HashMap<>();
        for (Field f : obj.getClass().getDeclaredFields()) fields.put(f.getName(), f);
        for (String name : names) {
            Field f = fields.get(name);
            if (f!=null) return f;
        }
        throw new NoSuchFieldException(Arrays.toString(names));
    }

    private static URL getJarURL() throws IOException {
        URL classUrl = ClassLoader.getSystemResource("org/graalvm/visualvm/sampler/truffle/stagent/Truffle.class");
        JarURLConnection connection = (JarURLConnection) classUrl.openConnection();
        if (DEBUG) System.out.println("URL "+classUrl);
        if (DEBUG) System.out.println("URL "+connection.getJarFileURL());
        return connection.getJarFileURL();
    }

    private static URLClassLoader getSamplerClassLoader(Object impl, Unsafe unsafe, URL jarURL) throws IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalArgumentException {
        TruffleClassLoader truffleLoader = new TruffleClassLoader(impl.getClass().getClassLoader(), unsafe);
        URLClassLoader ur = new AgentClassLoader(new URL[] {jarURL}, truffleLoader);
        if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.api.TruffleStackTraceElement"));
        if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.api.impl.TruffleLocator"));
        //            if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.tools.profiler.StackTraces"));
        if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.polyglot.PolyglotEngineImpl"));
        if (DEBUG) System.out.println("Class "+ur.loadClass("com.oracle.truffle.tools.profiler.CPUSampler"));
        return ur;
    }

    private static Object getTruffleInstance(URLClassLoader ur, Unsafe unsafe) throws IllegalAccessException, SecurityException, IllegalArgumentException, NoSuchMethodException, InstantiationException, InvocationTargetException, ClassNotFoundException {
        Class TruffleClass = ur.loadClass(Truffle.class.getName());
        if (DEBUG) System.out.println("Class "+TruffleClass+" ClassLoader "+TruffleClass.getClassLoader());
        Constructor TruffleClassConstructor = TruffleClass.getConstructor(Unsafe.class);
        return TruffleClassConstructor.newInstance(unsafe);
    }

    private static void registerMXBean(Object truffle) {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName mxbeanName = new ObjectName("com.truffle:type=Threading");
            mbs.registerMBean(truffle, mxbeanName);
        } catch (InstanceAlreadyExistsException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MBeanRegistrationException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotCompliantMBeanException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MalformedObjectNameException ex) {
            Logger.getLogger(TruffleJMX.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
