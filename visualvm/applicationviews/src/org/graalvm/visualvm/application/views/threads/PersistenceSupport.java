/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.views.threads;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;

/**
 *
 * @author Jiri Sedlacek
 */
final class PersistenceSupport {

    private static final String THREADS_DATA_FILE = "threads.dat"; // NOI18N
    private static final String THREADS_SNAPSHOT_HEADER = "ThreadsDataManagerSnapshot"; // NOI18N
    private static final int THREADS_SNAPSHOT_VERSION = 1;

    private static final String PROP_PREFIX = "ApplicationThreadsModel_";  // NOI18N

    static final String SNAPSHOT_VERSION = PROP_PREFIX + "version"; // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;


    static boolean supportsStorage(Storage storage) {
        return storage.getCustomProperty(SNAPSHOT_VERSION) != null;
    }

    static void saveDataManager(VisualVMThreadsDataManager dm, Storage storage) {
        if (dm == null) return;

        File dir = storage.getDirectory();
        OutputStream os = null;

        try {
            os = new FileOutputStream(new File(dir, THREADS_DATA_FILE));
            saveDataManager(dm, os);
            storage.setCustomProperty(SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);
        } catch (Exception e) {
            // TODO: log it
        } finally {
            try {
                if (os != null) os.close();
            } catch (Exception e) {
                // TODO: log it
            }
        }
    }

    static VisualVMThreadsDataManager loadDataManager(Storage storage) {
        File dir = storage.getDirectory();
        InputStream is = null;

        try {
            is = new FileInputStream(new File(dir, THREADS_DATA_FILE));
            return loadDataManager(is);
        } catch (Exception e) {
            // TODO: log it
            return null;
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                // TODO: log it
            }
        }
    }


    private synchronized static void saveDataManager(VisualVMThreadsDataManager dm, OutputStream os) throws IOException {
        DataOutputStream dos = null;
        try {
            synchronized(dm) {
                int tcount = dm.getThreadsCount();

                dos = new DataOutputStream(os);

                dos.writeUTF(THREADS_SNAPSHOT_HEADER); // Snapshot format
                dos.writeInt(THREADS_SNAPSHOT_VERSION); // Snapshot version

                dos.writeLong(dm.getStartTime()); // Start time
                dos.writeLong(dm.getEndTime()); // End time
                dos.writeInt(tcount); // Threads count
                dos.writeInt(dm.getDaemonThreadCount()); // Daemon threads count

                for (int tidx = 0; tidx < tcount; tidx++) {
                    ThreadData tdata = dm.getThreadData(tidx);
                    int scount = tdata.size();
                    dos.writeUTF(tdata.getName()); // Thread name
                    dos.writeInt(scount); // Number of thread states
                    for (int sidx = 0; sidx < scount; sidx++) {
                        dos.writeLong(tdata.getTimeStampAt(sidx)); // State timestamp
                        dos.writeByte(tdata.getStateAt(sidx)); // Thread state
                    }
                }
            }
        } finally {
            if (dos != null) dos.close();
        }
    }

    private static VisualVMThreadsDataManager loadDataManager(InputStream is) throws IOException {
        try (DataInputStream dis = new DataInputStream(is)) {

            if (!THREADS_SNAPSHOT_HEADER.equals(dis.readUTF()))
                throw new IOException("Unknown snapshot format"); // NOI18N
            if (THREADS_SNAPSHOT_VERSION != dis.readInt())
                throw new IOException("Unsupported snapshot version"); // NOI18N

            long stime = dis.readLong(); // Start time
            long etime = dis.readLong(); // End time
            int tcount = dis.readInt(); // Threads count
            int dtcount = dis.readInt(); // Daemon threads count
            ThreadData[] tdata = new ThreadData[tcount];

            for (int tidx = 0; tidx < tcount; tidx++) {
                ThreadData td = new ThreadData(dis.readUTF(), ""); // NOI18N // Thread name
                int scount = dis.readInt(); // Number of thread states
                for (int sidx = 0; sidx < scount; sidx++)
                    td.add(dis.readLong(), dis.readByte()); // State timestamp, thread state
                tdata[tidx] = td;
            }

            return new SavedThreadsDataManager(stime, etime, dtcount, tdata);
        }
    }


    private static class SavedThreadsDataManager extends VisualVMThreadsDataManager {

        private final long startTime;
        private final long endTime;
        private final int daemonThreads;
        private final ThreadData[] threadData;

        SavedThreadsDataManager(long startTime, long endTime,
                                       int daemonThreads, ThreadData[] threadData) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.daemonThreads = daemonThreads;
            this.threadData = threadData;
        }

        public int getThreadCount() { return getThreadsCount(); }
        public int getDaemonThreadCount() { return daemonThreads; }

        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }

        public int getThreadsCount() { return threadData.length; }
        public String getThreadName(int index) { return threadData[index].getName(); }
        public String getThreadClassName(int index) { return threadData[index].getClassName(); }

        public ThreadData getThreadData(int index) { return threadData[index]; }
        void cleanup() {}

    }

}
