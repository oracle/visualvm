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

import java.lang.reflect.*;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 *
 * @author poonam
 */
public class ProxyListener implements java.lang.reflect.InvocationHandler {    
    SAModelImpl model = null;
  
    public ProxyListener(SAModelImpl model) {
          this.model = model;
    }
    
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        Object result = null;
        try {
            System.out.print("begin method "
             + m.getName() + "(");
            for(int i=0; i<args.length; i++) {
                if(i>0) System.out.print(",");
                    System.out.print(" " +
                    args[i].toString());
         }
         System.out.println(" )");
         if (m.getName().equals("showThreadOopInspector")) {
             showThreadOopInspector(args[0]);
         } else if (m.getName().equals("showInspector")) {
            showInspector(args[0]);
         } else if (m.getName().equals("showThreadStackMemory")) {
             showStackMemoryPanel(args[0]);

         } else if (m.getName().equals("showThreadInfo")) {

         } else if (m.getName().equals("showJavaStackTrace")) {
            showJavaStackTrace(args[0]);

         } else if (m.getName().equals("showCodeViewer")) {

         } else if (m.getName().equals("showLiveness")) {
            showLivenessPanel(args[0]);
         }         
         
         } catch (Exception e) {
           throw new RuntimeException
           ("unexpected invocation exception: " +
                                  e.getMessage());
         } finally {
             System.out.println(
             "end method " + m.getName());
         }
         return result;
     }
    void showJavaStackTrace(Object thread) {

        SAView view = model.getView();
        view.updateStackTraceView(thread);
    }
    void showLivenessPanel(Object o) {
        NotifyDescriptor nd = new NotifyDescriptor.Message("Not yet implemented", NotifyDescriptor.INFORMATION_MESSAGE) ;
        DialogDisplayer.getDefault().notify(nd);
    }

    void showStackMemoryPanel(Object thread) {
        NotifyDescriptor nd = new NotifyDescriptor.Message("Not yet implemented", NotifyDescriptor.INFORMATION_MESSAGE) ;
        DialogDisplayer.getDefault().notify(nd);
    }

    void showThreadOopInspector(Object o) {       
        SAObject thread = new SAObject(o);     
        SAObject threadObj = null;
        try {
            threadObj = thread.invokeSA("getThreadObj");
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        }
  
        showInspector(threadObj.instance);
    }
    public void showInspector(Object oopObject) {        
        SAView view = model.getView();
        view.updateOopInspectorView(oopObject);
    }
  }

