/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.saplugin;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tomas Hurka
 */
class StackTrace {

    private VM vm;
    private SAObject heap;
    private SAObject objectClass;

    StackTrace(VM v) throws IllegalAccessException, InvocationTargetException {
        vm = v;
        heap = vm.getObjectHeap();
        objectClass = vm.getSystemDictionary().invokeSA("getObjectKlass");  // NOI18N
    }

    public String getStackTrace() throws IllegalAccessException, InvocationTargetException {
        ByteArrayOutputStream data = new ByteArrayOutputStream(4096);
        PrintStream out = new PrintStream(data);
        SAObject threads = vm.getThreads();
        SAObject curThread = threads.invokeSA("first"); // NOI18N

        for (;!curThread.isNull();curThread=curThread.invokeSA("next")) {   // NOI18N
            try {
                Boolean isJavaThread = (Boolean) curThread.invoke("isJavaThread");  // NOI18N
                if (!isJavaThread.booleanValue()) {
                    out.print("VM ");   // NOI18N
                }
                out.print("Thread ");   // NOI18N
                curThread.invoke("printThreadIDOn",out);    // NOI18N
                out.print(" \""+curThread.invoke("getThreadName")+"\"");    // NOI18N
                out.print(": (state = ");   // NOI18N
                out.print(curThread.invoke("getThreadState"));  // NOI18N
                out.println(")");
                if (isJavaThread.booleanValue()) { // Java thread
                    SAObject javaFrame = curThread.invokeSA("getLastJavaVFrameDbg");    // NOI18N
                    Object waitingToLockMonitor = curThread.invoke("getCurrentPendingMonitor"); // NOI18N
                    boolean objectWaitFrame = isJavaLangObjectWaitFrame(javaFrame);
                    for (;!javaFrame.isNull();javaFrame=javaFrame.invokeSA("javaSender")) { // NOI18N
                        printJavaFrame(out, javaFrame);
                        printMonitors(out, javaFrame, waitingToLockMonitor, objectWaitFrame);
                        waitingToLockMonitor = null;
                        objectWaitFrame = false;
                    }
                }
            } catch (Exception ex) {
                out.println("\t-- Error occurred during stack walking");
                Logger.getLogger(StackTrace.class.getName()).log(Level.INFO,"getStackTrace",ex);
            }
            out.println();
        }
        return data.toString();
    }
    
    private boolean isJavaLangObjectWaitFrame(SAObject javaFrame) throws IllegalAccessException, InvocationTargetException {
        if (!javaFrame.isNull()) {
            SAObject method = javaFrame.invokeSA("getMethod");  // NOI18N
            SAObject klass = method.invokeSA("getMethodHolder");    // NOI18N
            Boolean isNative = (Boolean) method.invoke("isNative"); // NOI18N
            if (objectClass.equals(klass) && isNative.booleanValue()) {
                if ("wait".equals(method.invokeSA("getName").invoke("asString"))) { // NOI18N
                    return true;
                }
            }
        }
        return false;
    }
    
    private void printMonitors(
            final PrintStream out, final SAObject javaFrame,
            Object waitingToLockMonitor, boolean objectWaitFrame)
            throws IllegalAccessException, InvocationTargetException {
        if (objectWaitFrame) {
            SAObject stackValueCollection = javaFrame.invokeSA("getLocals");    // NOI18N
            Boolean isEmpty = (Boolean) stackValueCollection.invoke("isEmpty"); // NOI18N
            if (!isEmpty.booleanValue()) {
                Object oopHandle = stackValueCollection.invoke("oopHandleAt", 0);   // NOI18N
                printMonitor(out, oopHandle, "waiting on"); // NOI18N
            }
        }
        try {
            List mList = (List) javaFrame.invoke("getMonitors");    // NOI18N
            Object[] monitors = mList.toArray();
            for (int i = monitors.length - 1; i >= 0; i--) {
                SAObject monitorInfo = new SAObject(monitors[i]);
                Object ownerHandle = monitorInfo.invoke("owner");   // NOI18N
                if (ownerHandle != null) {
                    String state = "locked";    // NOI18N
                    if (waitingToLockMonitor != null) {
                        Object objectHandle = new SAObject(waitingToLockMonitor).invoke("object");  // NOI18N
                        if (objectHandle.equals(ownerHandle)) {
                            state = "waiting to lock";  // NOI18N
                        }
                    }
                    printMonitor(out, ownerHandle, state);
                }
            }
        } catch (Exception e) {
            // Ignore...
        }
    }

    private void printMonitor(
            final PrintStream out,
            final Object ownerHandle,
            final String state) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\t- " + state + " <" + ownerHandle + "> ");  // NOI18N
            printOop(sb, ownerHandle);
            out.println(sb.toString());
        } catch (Exception e) {
            // Ignore...
        }
    }

    private void printOop(StringBuilder sb, Object oopHandle)
            throws IllegalAccessException, InvocationTargetException {
        SAObject oop = heap.invokeSA("newOop", oopHandle);  // NOI18N
        if (!oop.isNull()) {
            sb.append("(a ");   // NOI18N
            String monitorClassName = (String) oop.invokeSA("getKlass").invokeSA("getName").invoke("asString"); // NOI18N
            sb.append(monitorClassName.replace('/', '.'));
            sb.append(")");
        } else {
            sb.append("(Raw Monitor)"); // NOI18N
        }
    }
    
    private void printJavaFrame(final PrintStream out, final SAObject javaFrame) throws IllegalAccessException, InvocationTargetException {
        SAObject method = javaFrame.invokeSA("getMethod");  // NOI18N
        
        out.print("\tat "); // NOI18N
        SAObject klass = method.invokeSA("getMethodHolder");    // NOI18N
        String className = (String) klass.invokeSA("getName").invoke("asString");   // NOI18N
        out.print(className.replace('/','.'));
        out.print(".");
        out.print(method.invokeSA("getName").invoke("asString"));   // NOI18N
        Integer bci = (Integer) javaFrame.invoke("getBCI"); // NOI18N
        out.print("(");
        if (((Boolean)method.invoke("isNative")).booleanValue()) {  // NOI18N
            out.print("Native Method"); // NOI18N
        } else {
            Integer lineNumber = (Integer) method.invoke("getLineNumberFromBCI",bci);   // NOI18N
            SAObject sourceName = klass.invokeSA("getSourceFileName");  // NOI18N
            
            if (lineNumber.intValue()!=-1  && !sourceName.isNull()) {
                out.print(sourceName.invoke("asString"));   // NOI18N
                out.print(":");
                out.print(lineNumber);
            } else {
                out.print("bci=");  // NOI18N
                out.print(bci);
            }
        }
        out.println(")");
    }
}
