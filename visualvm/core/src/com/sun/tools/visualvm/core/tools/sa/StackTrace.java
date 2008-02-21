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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 *
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
class StackTrace {

    private VM vm;
    private SAObject heap;
    private SAObject objectClass;

    StackTrace(VM v) throws IllegalAccessException, InvocationTargetException {
        vm = v;
        heap = vm.getObjectHeap();
        objectClass = vm.getSystemDictionary().invokeSA("getObjectKlass");
    }

    public String getStackTrace() throws IllegalAccessException, InvocationTargetException {
        ByteArrayOutputStream data = new ByteArrayOutputStream(4096);
        PrintStream out = new PrintStream(data);
        SAObject threads = vm.getThreads();
        SAObject curThread = threads.invokeSA("first");

        for (;!curThread.isNull();curThread=curThread.invokeSA("next")) {
            Boolean isJavaThread = (Boolean) curThread.invoke("isJavaThread");
            if (!isJavaThread.booleanValue()) {
                out.print("VM ");
            }
            out.print("Thread ");
            curThread.invoke("printThreadIDOn",out);
            out.print(" \""+curThread.invoke("getThreadName")+"\"");
            out.print(": (state = ");
            out.print(curThread.invoke("getThreadState"));
            out.println(")");
            if (isJavaThread.booleanValue()) { // Java thread
                SAObject javaFrame = curThread.invokeSA("getLastJavaVFrameDbg");
                Object waitingToLockMonitor = curThread.invoke("getCurrentPendingMonitor");
                boolean objectWaitFrame = isJavaLangObjectWaitFrame(javaFrame);
                for (;!javaFrame.isNull();javaFrame=javaFrame.invokeSA("javaSender")) {
                    printJavaFrame(out, javaFrame);
                    printMonitors(out, javaFrame, waitingToLockMonitor, objectWaitFrame);
                    waitingToLockMonitor = null;
                    objectWaitFrame = false;
                }
            }
            out.println();
        }
        return data.toString();
    }
    
    private boolean isJavaLangObjectWaitFrame(SAObject javaFrame) throws IllegalAccessException, InvocationTargetException {
        if (!javaFrame.isNull()) {
            SAObject method = javaFrame.invokeSA("getMethod");
            SAObject klass = method.invokeSA("getMethodHolder");
            Boolean isNative = (Boolean) method.invoke("isNative");
            if (objectClass.equals(klass) && isNative.booleanValue()) {
                if ("wait".equals(method.invokeSA("getName").invoke("asString"))) {
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
            SAObject stackValueCollection = javaFrame.invokeSA("getLocals");
            Boolean isEmpty = (Boolean) stackValueCollection.invoke("isEmpty");
            if (!isEmpty.booleanValue()) {
                Object oopHandle = stackValueCollection.invoke("oopHandleAt", 0);
                printMonitor(out, oopHandle, "waiting on");
            }
        }
        try {
            List mList = (List) javaFrame.invoke("getMonitors");
            Object[] monitors = mList.toArray();
            for (int i = monitors.length - 1; i >= 0; i--) {
                SAObject monitorInfo = new SAObject(monitors[i]);
                Object ownerHandle = monitorInfo.invoke("owner");
                if (ownerHandle != null) {
                    String state = "locked";
                    if (waitingToLockMonitor != null) {
                        Object objectHandle = new SAObject(waitingToLockMonitor).invoke("object");
                        if (objectHandle.equals(ownerHandle)) {
                            state = "waiting to lock";
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
            sb.append("\t- " + state + " <" + ownerHandle + "> ");
            printOop(sb, ownerHandle);
            out.println(sb.toString());
        } catch (Exception e) {
            // Ignore...
        }
    }

    private void printOop(StringBuilder sb, Object oopHandle)
            throws IllegalAccessException, InvocationTargetException {
        SAObject oop = heap.invokeSA("newOop", oopHandle);
        if (!oop.isNull()) {
            sb.append("(a ");
            String monitorClassName = (String) oop.invokeSA("getKlass").invokeSA("getName").invoke("asString");
            sb.append(monitorClassName.replace('/', '.'));
            sb.append(")");
        } else {
            sb.append("(Raw Monitor)");
        }
    }
    
    private void printJavaFrame(final PrintStream out, final SAObject javaFrame) throws IllegalAccessException, InvocationTargetException {
        SAObject method = javaFrame.invokeSA("getMethod");
        
        out.print("\tat ");
        SAObject klass = method.invokeSA("getMethodHolder");
        String className = (String) klass.invokeSA("getName").invoke("asString");
        out.print(className.replace('/','.'));
        out.print(".");
        out.print(method.invokeSA("getName").invoke("asString"));
        Integer bci = (Integer) javaFrame.invoke("getBCI");
        out.print("(");
        if (((Boolean)method.invoke("isNative")).booleanValue()) {
            out.print("Native Method");
        } else {
            Integer lineNumber = (Integer) method.invoke("getLineNumberFromBCI",bci);
            SAObject sourceName = klass.invokeSA("getSourceFileName");
            
            if (lineNumber.intValue()!=-1  && !sourceName.isNull()) {
                out.print(sourceName.invoke("asString"));
                out.print(":");
                out.print(lineNumber);
            } else {
                out.print("bci=");
                out.print(bci);
            }
        }
        out.println(")");
    }
}
