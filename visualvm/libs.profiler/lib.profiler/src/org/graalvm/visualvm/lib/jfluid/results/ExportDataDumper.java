/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An instance of this class is passed to code that generates text representation of profiling
 * results for export. It is intended that the text generating code periodically checks the size
 * of the StringBuffer it uses for storage, and if it's above some critical value, dumps it using
 * the code below. If there is an error during this process, it is not returned immediately to avoid
 * making text generator code too complex - instead the caller can eventually retrieve the error
 * using the getCaughtException() method.
 *
 * @author Misha Dmitriev
 * @author Petr Cyhelsky
 */
public class ExportDataDumper {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int BUFFER_SIZE = 32000; //roughly 32 kB buffer

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    OutputStreamWriter osw;
    BufferedOutputStream bos;
    IOException caughtEx;
    int numExceptions=0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ExportDataDumper(FileOutputStream fw) {
        bos = new BufferedOutputStream(fw, BUFFER_SIZE);
        try {
            osw = new OutputStreamWriter(bos, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            numExceptions++;
            Logger.getLogger(ExportDataDumper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public IOException getCaughtException() {
        return caughtEx;
    }

    public int getNumExceptions() {
        return numExceptions;
    }
    
    public void dumpByte(byte b) {
        if (caughtEx != null) {
            return;
        }

        try {
            bos.write(b);
        } catch (IOException ex) {
            caughtEx = ex;
            System.out.println(b);
            numExceptions++;
            System.err.println(ex.getMessage());
        }
    }

    public void dumpData(CharSequence s) {
        if (caughtEx != null) {
            return;
        }

        try {
            if (s!=null) osw.append(s);
        } catch (IOException ex) {
            caughtEx = ex;
            System.out.println(s);
            numExceptions++;
            System.err.println(ex.getMessage());
        }
    }

    public void close() {
        try {
            osw.close();
            bos.close();
        } catch (IOException ex) {
            caughtEx = ex;
            System.err.println(ex.getMessage());
        }
    }

    public void dumpDataAndClose(StringBuffer s) {
        dumpData(s);
        close();
    }

    public BufferedOutputStream getOutputStream() {
        return bos;
    }
}
