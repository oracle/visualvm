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

package org.graalvm.visualvm.lib.profiler.snaptracer.logs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;

/** Reads log records from file.
 *
 * @author Tomas Hurka
 */
public final class LogReader {

    private static final Logger LOG = Logger.getLogger(LogRecords.class.getName());

    private FileObject logFile;
//    private int records;
//    private long startTime;
    private NavigableMap<Long,LogRecord> recordList;

    public LogReader(FileObject f) {
        logFile = f;
        recordList = new TreeMap();
    }


    public void load() throws IOException {
        InputStream is = new BufferedInputStream(logFile.getInputStream(),32768);
        try {
            LogRecords.scan(is, new LogHandler());
        } finally {
            is.close();
        }
    }

    public LogRecord getRecordFor(long time) {
        Map.Entry<Long,LogRecord> entry = recordList.floorEntry(new Long(time));

        if (entry != null) {
            return entry.getValue();
        }
        return null;
    }

    class LogHandler extends Handler {

        @Override
        public void publish(LogRecord record) {
//            System.out.println("Record "+ records++);
//            if (startTime == 0) {
//                startTime = record.getMillis();
//                System.out.println("Start date: "+new Date(startTime));
//            } else {
//                System.out.println("Time: "+(record.getMillis()-startTime));
//            }
//            System.out.println(record.getMessage());
            recordList.put(new Long(record.getMillis()), record);
        }

        @Override
        public void flush() {
//            System.out.println("Flush");
        }

        @Override
        public void close() throws SecurityException {
//           System.out.println("Close");
        }

    }
}
