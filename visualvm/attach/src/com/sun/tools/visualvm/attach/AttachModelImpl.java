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

package com.sun.tools.visualvm.attach;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.openide.ErrorManager;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 *
 * @author Tomas Hurka
 */
public class AttachModelImpl extends AttachModel {
    String pid;
    HotSpotVirtualMachine vm;
    private static final String LIVE_OBJECTS_OPTION = "-live";
    private static final String ALL_OBJECTS_OPTION = "-all";
    
    
    AttachModelImpl(Application app) {
        pid = Integer.toString(app.getPid());
    }
    
    public synchronized Properties getSystemProperties() {
        try {
            return getVirtualMachine().getSystemProperties();
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
        return null;
    }
    
    public synchronized boolean takeHeapDump(String fileName) {
        try {
            InputStream in = getVirtualMachine().dumpHeap(fileName,LIVE_OBJECTS_OPTION);
            String out = readToEOF(in);
            if (out.length()>0) {
                ErrorManager.getDefault().log(ErrorManager.USER,out);
            }
            return true;
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
        return false;
    }
    
    public synchronized String takeThreadDump() {
        try {
            InputStream in = getVirtualMachine().remoteDataDump("-l");
            return readToEOF(in);
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
        return null;
    }
    
    public synchronized String printFlag(String name) {
        try {
            InputStream in = getVirtualMachine().printFlag(name);
            return readToEOF(in);
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
        return null;
    }
    
    public synchronized void setFlag(String name, String value) {
        try {
            InputStream in = getVirtualMachine().setFlag(name,value);
            String out = readToEOF(in);
            if (out.length()>0) {
                ErrorManager.getDefault().log(ErrorManager.USER,out);                
            }
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.USER,ex);
        }
    }
    
    private HotSpotVirtualMachine getVirtualMachine() throws IOException {
        if (vm == null) {
            try {
                vm = (HotSpotVirtualMachine) VirtualMachine.attach(pid);
            } catch (Exception x) {
                throw new IOException(x.getLocalizedMessage(),x);
            }
        }
        return vm;
    }
    
    private String readToEOF(InputStream in) throws IOException {
        StringBuffer buffer = new StringBuffer(1024);
        byte b[] = new byte[256];
        int n;
        
        do {
            n = in.read(b);
            if (n > 0) {
                String s = new String(b, 0, n, "UTF-8");
                
                buffer.append(s);
            }
        } while (n > 0);
        in.close();
        return buffer.toString();
    }

    protected void finalize() throws Throwable {
        vm.detach();
        super.finalize();
    }
    
}