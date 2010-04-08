/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.coderegion.CodeRegionResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot.NoDataAvailableException;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.openide.util.NbBundle;
import java.io.*;
import java.lang.management.ThreadInfo;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import javax.management.openmbean.CompositeData;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.results.cpu.StackTraceSnapshotBuilder;


public class LoadedSnapshot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(LoadedSnapshot.class.getName());

    // -----
    // I18N String constants
    private static final String ILLEGAL_SNAPSHOT_DATA_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                "LoadedSnapshot_IllegalSnapshotDataMsg"); // NOI18N
    private static final String INVALID_SNAPSHOT_FILE_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                "LoadedSnapshot_InvalidSnapshotFileMsg"); // NOI18N
    private static final String UNSUPPORTED_SNAPSHOT_VERSION_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                       "LoadedSnapshot_UnsupportedSnapshotVersionMsg"); // NOI18N
    private static final String WRONG_SNAPSHOT_TYPE_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                              "LoadedSnapshot_WrongSnapshotTypeMsg"); // NOI18N
    private static final String CANNOT_READ_SNAPSHOT_DATA_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                    "LoadedSnapshot_CannotReadSnapshotDataMsg"); // NOI18N
    private static final String CANNOT_READ_SETTINGS_DATA_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                    "LoadedSnapshot_CannotReadSettingsDataMsg"); // NOI18N
    private static final String UNRECOGNIZED_SNAPSHOT_TYPE_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                     "LoadedSnapshot_UnrecognizedSnapshotTypeMsg"); // NOI18N
    private static final String SNAPSHOT_DATA_CORRUPTED_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                  "LoadedSnapshot_SnapshotDataCorruptedMsg"); // NOI18N
    private static final String SNAPSHOT_FILE_SHORT_MSG = NbBundle.getMessage(LoadedSnapshot.class,
                                                                              "LoadedSnapshot_SnapshotFileShortMsg"); // NOI18N
    private static final String SNAPSHOT_FILE_CORRUPTED = NbBundle.getMessage(LoadedSnapshot.class,
                                                                              "LoadedSnapshot_SnapshotFileCorrupted"); // NOI18N
    private static final String SNAPSHOT_FILE_CORRUPTED_REASON = NbBundle.getMessage(LoadedSnapshot.class,
                                                                                     "LoadedSnapshot_SnapshotFileCorruptedReason"); // NOI18N
    private static final String OUT_OF_MEMORY_LOADING = NbBundle.getMessage(LoadedSnapshot.class,
                                                                            "LoadedSnapshot_OutOfMemoryLoadingMsg"); // NOI18N
                                                                                                                     // -----

    //  private static final boolean DEBUG = true; //System.getProperty("org.netbeans.modules.profiler.LoadedSnapshot") != null; // TODO [m7] : change to property
    public static final int SNAPSHOT_TYPE_UNKNOWN = 0;
    public static final int SNAPSHOT_TYPE_CPU = 1;
    public static final int SNAPSHOT_TYPE_CODEFRAGMENT = 2;
    public static final int SNAPSHOT_TYPE_MEMORY_ALLOCATIONS = 4;
    public static final int SNAPSHOT_TYPE_MEMORY_LIVENESS = 8;
    public static final int SNAPSHOT_TYPE_MEMORY = SNAPSHOT_TYPE_MEMORY_ALLOCATIONS | SNAPSHOT_TYPE_MEMORY_LIVENESS;
    public static final String PROFILER_FILE_MAGIC_STRING = "nBpRoFiLeR"; // NOI18N
    private static final byte SNAPSHOT_FILE_VERSION_MAJOR = 1;
    private static final byte SNAPSHOT_FILE_VERSION_MINOR = 1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private File file;
    private ProfilingSettings settings;
    private Project project = null;
    private ResultsSnapshot snapshot;
    private boolean saved = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new LoadedSnapshot.
     *
     * @param snapshot The actual snapshot results data
     * @param settings ProfilingSettings used to obtain this snapshot
     * @param file     The FileObject in which this snapshot is saved or null if it is not yet saved (in-memory only)
     */
    public LoadedSnapshot(ResultsSnapshot snapshot, ProfilingSettings settings, File file, Project project) {
        if (snapshot == null) {
            throw new IllegalArgumentException();
        }

        if (settings == null) {
            throw new IllegalArgumentException();
        }

        this.snapshot = snapshot;
        this.settings = settings;
        this.file = file;
        this.project = project;
    }

    private LoadedSnapshot() {
        // for persistence only
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setFile(File file) {
        this.file = file;
        saved = true;
    }

    /**
     * @return The File in which this snapshot is saved or null if it is not yet saved (in-memory only)
     */
    public File getFile() {
        return file;
    }

    public Project getProject() {
        return project;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public boolean isSaved() {
        return saved;
    }

    /**
     * @return ProfilingSettings used to obtain this snapshot
     */
    public ProfilingSettings getSettings() {
        return settings;
    }

    /**
     * @return The actual snapshot results data
     */
    public ResultsSnapshot getSnapshot() {
        return snapshot;
    }

    public int getType() {
        if (snapshot instanceof CPUResultsSnapshot) {
            return SNAPSHOT_TYPE_CPU;
        } else if (snapshot instanceof CodeRegionResultsSnapshot) {
            return SNAPSHOT_TYPE_CODEFRAGMENT;
        } else if (snapshot instanceof LivenessMemoryResultsSnapshot) {
            return SNAPSHOT_TYPE_MEMORY_LIVENESS;
        } else if (snapshot instanceof AllocMemoryResultsSnapshot) {
            return SNAPSHOT_TYPE_MEMORY_ALLOCATIONS;
        } else {
            throw new IllegalStateException(ILLEGAL_SNAPSHOT_DATA_MSG);
        }
    }

    /**
     * Will load a snapshot into memory from the provided stream and return the snapshot representation.
     *
     * @param dis Stream to read from, typically data from file.
     * @return The loaded snapshot or null if failed to load (has already been reported to the user)
     * @throws IOException If unexpected error occurred while loading (should be reported to the user)
     */
    public static LoadedSnapshot loadSnapshot(DataInputStream dis)
                                       throws IOException {
        dis.mark(100);
        try {
            LoadedSnapshot ls = new LoadedSnapshot();

            if (ls.load(dis)) {
                return ls;
            } else {
                return null;
            }
        } catch (IOException ex) {
            if (INVALID_SNAPSHOT_FILE_MSG.equals(ex.getMessage())) {
                dis.reset();
                return loadSnapshotFromStackTraces(dis);
            }
            throw ex;
        }
    }

    private static LoadedSnapshot loadSnapshotFromStackTraces(DataInputStream dis) throws IOException {
        SamplesInputStream is = new SamplesInputStream(dis);
        StackTraceSnapshotBuilder builder = new StackTraceSnapshotBuilder();
        ThreadsSample sample = is.readSample();
        long startTime = sample.getTime();

        for ( ;sample != null; sample = is.readSample()) {
            builder.addStacktrace(sample.getTinfos(),sample.getTime());
            
        }
        CPUResultsSnapshot snapshot;
        try {
            snapshot = builder.createSnapshot(startTime);
        } catch (NoDataAvailableException ex) {
            throw new IOException(ex);
        }
        return new LoadedSnapshot(snapshot, ProfilingSettingsPresets.createCPUPreset(), null, null);
    }

    public void setProject(Project project) {
        this.project = project;
    }

    static void writeToStream(CPUResultsSnapshot snapshot, DataOutputStream dos) throws IOException {
        LoadedSnapshot loadedSnapshot = new LoadedSnapshot(snapshot,
                ProfilingSettingsPresets.createCPUPreset(), null, null);
        loadedSnapshot.save(dos);
    }

    public void save(DataOutputStream dos) throws IOException, OutOfMemoryError {
        // todo [performance] profile memory use during the save operation
        // there is ~80MB bytes used for byte[], for the length of uncompressed data ~20MB
        Properties props = new Properties();
        settings.store(props);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("save properties: --------------------------------------------------------------"); // NOI18N
            LOGGER.finest(settings.debug());
            LOGGER.finest("-------------------------------------------------------------------------------"); // NOI18N
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000); // ~1MB pre-allocated
        BufferedOutputStream bufBaos = new BufferedOutputStream(baos);
        DataOutputStream snapshotDataStream = new DataOutputStream(bufBaos);

        ByteArrayOutputStream baos2 = new ByteArrayOutputStream(10000); // ~10kB pre-allocated
        BufferedOutputStream bufBaos2 = new BufferedOutputStream(baos2);
        DataOutputStream settingsDataStream = new DataOutputStream(bufBaos2);

        try {
            snapshot.writeToStream(snapshotDataStream);
            snapshotDataStream.flush();
            props.store(settingsDataStream, ""); //NOI18N
            settingsDataStream.flush();

            byte[] snapshotBytes = baos.toByteArray();
            byte[] compressedBytes = new byte[snapshotBytes.length];

            Deflater d = new Deflater();
            d.setInput(snapshotBytes);
            d.finish();

            int compressedLen = d.deflate(compressedBytes);
            int uncompressedLen = snapshotBytes.length;

            // binary file format:
            // 1. magic number: "nbprofiler"
            // 2. int type
            // 3. int length of snapshot data size
            // 4. snapshot data bytes
            // 5. int length of settings data size
            // 6. settings data bytes (.properties plain text file format)
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("save version:" + SNAPSHOT_FILE_VERSION_MAJOR //NOI18N
                              + "." + SNAPSHOT_FILE_VERSION_MINOR); // NOI18N
                LOGGER.finest("save type:" + getType()); // NOI18N
                LOGGER.finest("length of uncompressed snapshot data:" + uncompressedLen); // NOI18N
                LOGGER.finest("save length of snapshot data:" + compressedLen); // NOI18N
                LOGGER.finest("length of settings data:" + baos2.size()); // NOI18N
            }

            dos.writeBytes(PROFILER_FILE_MAGIC_STRING); // 1. magic number: "nbprofiler"
            dos.writeByte(SNAPSHOT_FILE_VERSION_MAJOR); // 2. file version
            dos.writeByte(SNAPSHOT_FILE_VERSION_MINOR); // 3. file version
            dos.writeInt(getType()); // 4. int type
            dos.writeInt(compressedLen); // 5. int length of compressed snapshot data size
            dos.writeInt(uncompressedLen); // 5. int length of compressed snapshot data size
            dos.write(compressedBytes, 0, compressedLen); // 6. compressed snapshot data bytes
            dos.writeInt(baos2.size()); // 7. int length of settings data size
            dos.write(baos2.toByteArray()); // 8. settings data bytes (.properties plain text file format)
        } catch (OutOfMemoryError e) {
            baos = null;
            bufBaos = null;
            snapshotDataStream = null;
            baos2 = null;
            bufBaos2 = null;
            settingsDataStream = null;

            throw e;
        } finally {
            if (snapshotDataStream != null) {
                snapshotDataStream.close();
            }

            if (settingsDataStream != null) {
                settingsDataStream.close();
            }
        }
    }

    public String toString() {
        String snapshotString = "snapshot = " + snapshot.toString(); // NOI18N
        String fileString = "file = " + ((file == null) ? "null" : file.toString()); // NOI18N
        String projectString = "project = "
                               + ((project == null) ? "null" : ProjectUtils.getInformation(project).getDisplayName()); // NOI18N

        return "Loaded Results Snapshot, " + snapshotString + ", " + projectString + ", " + fileString; // NOI18N
    }

    private static String getCorruptedMessage(IOException e) {
        String message = e.getMessage();

        if (message == null) {
            if (e instanceof EOFException) {
                return MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON, new Object[] { SNAPSHOT_FILE_SHORT_MSG });
            } else {
                return SNAPSHOT_FILE_CORRUPTED;
            }
        } else {
            return MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON, new Object[] { message });
        }
    }

    private boolean load(DataInputStream dis) throws IOException {
        try {
            Properties props = new Properties();
            settings = new ProfilingSettings();

            // binary file format:
            // 1. magic number: "nbprofiler"
            // 2. int type
            // 3. int length of snapshot data size
            // 4. snapshot data bytes
            // 5. int length of settings data size
            // 6. settings data bytes (.properties plain text file format)

            // 1. magic number: "nbprofiler"
            byte[] magicArray = new byte[PROFILER_FILE_MAGIC_STRING.length()];
            int len = dis.read(magicArray);

            if ((len != PROFILER_FILE_MAGIC_STRING.length()) || !PROFILER_FILE_MAGIC_STRING.equals(new String(magicArray))) {
                throw new IOException(INVALID_SNAPSHOT_FILE_MSG);
            }

            // 2. int type
            byte majorVersion = dis.readByte();
            byte minorVersion = dis.readByte();

            if (majorVersion > SNAPSHOT_FILE_VERSION_MAJOR) {
                throw new IOException(MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON,
                                                           new Object[] { UNSUPPORTED_SNAPSHOT_VERSION_MSG }));
            }

            // 3. int type
            int type = dis.readInt();

            if (type == -1) {
                throw new IOException(MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON,
                                                           new Object[] { WRONG_SNAPSHOT_TYPE_MSG }));
            }

            // 4. int length of snapshot data size
            int compressedDataLen = dis.readInt();
            int uncompressedDataLen = dis.readInt();
            byte[] dataBytes = new byte[compressedDataLen];

            // 5. snapshot data bytes
            int readLen1 = dis.read(dataBytes, 0, compressedDataLen);

            if (compressedDataLen != readLen1) {
                throw new IOException(MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON,
                                                           new Object[] { CANNOT_READ_SNAPSHOT_DATA_MSG }));
            }

            // 6. int length of settings data size
            int settingsLen = dis.readInt();
            byte[] settingsBytes = new byte[settingsLen];

            // 7. settings data bytes (.properties plain text file format)
            int readLen2 = dis.read(settingsBytes);

            if (settingsLen != readLen2) {
                throw new IOException(MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON,
                                                           new Object[] { CANNOT_READ_SETTINGS_DATA_MSG }));
            }

            // Process read data:
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("load version:" + majorVersion + "." + minorVersion); // NOI18N
                LOGGER.finest("load type:" + type); // NOI18N
                LOGGER.finest("load length of snapshot data:" + compressedDataLen); // NOI18N
                LOGGER.finest("uncompressed length of snapshot data:" + uncompressedDataLen); // NOI18N
                LOGGER.finest("load length of settings data:" + settingsLen); // NOI18N
            }

            switch (type) {
                case SNAPSHOT_TYPE_CPU:
                    snapshot = new CPUResultsSnapshot();

                    break;
                case SNAPSHOT_TYPE_CODEFRAGMENT:
                    snapshot = new CodeRegionResultsSnapshot();

                    break;
                case SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                    snapshot = new AllocMemoryResultsSnapshot();

                    break;
                case SNAPSHOT_TYPE_MEMORY_LIVENESS:
                    snapshot = new LivenessMemoryResultsSnapshot();

                    break;
                default:
                    throw new IOException(MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON,
                                                               new Object[] { UNRECOGNIZED_SNAPSHOT_TYPE_MSG })); // not supported
            }

            Inflater d = new Inflater();
            d.setInput(dataBytes, 0, dataBytes.length);

            byte[] decompressedBytes = new byte[uncompressedDataLen];

            try {
                int decLen = d.inflate(decompressedBytes);

                if (decLen != uncompressedDataLen) {
                    throw new IOException(MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON,
                                                               new Object[] { SNAPSHOT_DATA_CORRUPTED_MSG }));
                }
            } catch (DataFormatException e) {
                throw new IOException(MessageFormat.format(SNAPSHOT_FILE_CORRUPTED_REASON,
                                                           new Object[] { SNAPSHOT_DATA_CORRUPTED_MSG }));
            }

            d.end();

            ByteArrayInputStream bais = new ByteArrayInputStream(decompressedBytes);
            BufferedInputStream bufBais = new BufferedInputStream(bais);
            DataInputStream dataDis = new DataInputStream(bufBais);

            try {
                snapshot.readFromStream(dataDis);
            } catch (IOException e) {
                throw new IOException(getCorruptedMessage(e));
            } finally {
                dataDis.close();
            }

            ByteArrayInputStream bais2 = new ByteArrayInputStream(settingsBytes);
            BufferedInputStream bufBais2 = new BufferedInputStream(bais2);
            DataInputStream settingsDis = new DataInputStream(bufBais2);

            try {
                props.load(settingsDis);
            } catch (IOException e) {
                throw new IOException(getCorruptedMessage(e));
            } finally {
                settingsDis.close();
            }

            settings.load(props);

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("load properties: --------------------------------------------------------------"); // NOI18N
                LOGGER.finest(settings.debug());
                LOGGER.finest("-------------------------------------------------------------------------------"); // NOI18N
            }
        } catch (OutOfMemoryError e) {
            NetBeansProfiler.getDefaultNB().displayError(OUT_OF_MEMORY_LOADING);

            return false;
        }

        return true;
    }

    static class SamplesInputStream {
        static final String ID = "NPSS"; // NetBeans Profiler samples stream, it must match org.netbeans.core.ui.sampler.SamplesOutputStream.ID

        int version;
        ObjectInputStream in;
        Map<Long,ThreadInfo> threads;
        
        SamplesInputStream(File file) throws IOException {
            this(new FileInputStream(file));
        }

        SamplesInputStream(InputStream is) throws IOException {
            readHeader(is);
            in = new ObjectInputStream(new GZIPInputStream(is));
            threads = new HashMap(128);
        }

        ThreadsSample readSample() throws IOException {
            long time;
            ThreadInfo infos[];
            int sameThreads;
            Map<Long,ThreadInfo> newThreads;
            
            try {
                time = in.readLong();
            } catch (EOFException ex) {
                return null;
            }
            newThreads = new HashMap(threads.size());
            sameThreads = in.readInt();
            for (int i=0;i<sameThreads;i++) {
                Long tid = Long.valueOf(in.readLong());
                ThreadInfo oldThread = threads.get(tid);
                assert oldThread != null;
                newThreads.put(tid,oldThread);
            }
            infos = new ThreadInfo[in.readInt()];
            for (int i = 0 ; i < infos.length; i++) {
                CompositeData infoData;
                ThreadInfo thread;
                
                try {
                    infoData = (CompositeData) in.readObject();
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                thread = ThreadInfo.from(infoData);
                newThreads.put(Long.valueOf(thread.getThreadId()),thread);
            }
            threads = newThreads;
            return new ThreadsSample(time,threads.values());
        }

        void close() throws IOException {
            in.close();
        }

        private void readHeader(InputStream is) throws IOException {
            String id;
            byte[] idarr = new byte[ID.length()];

            is.read(idarr);
            id = new String(idarr);
            if (!ID.equals(id)) {
                new IOException("Invalid header "+id); // NOI18N
            }
            version = is.read();
        }
    }

    static final class ThreadsSample {
        private final long time;
        private final ThreadInfo[] tinfos;

        ThreadsSample(long t, Collection<ThreadInfo> tis) {
            time = t;
            tinfos = tis.toArray(new ThreadInfo[tis.size()]);
        }

        long getTime() {
            return time;
        }

        ThreadInfo[] getTinfos() {
            return tinfos;
        }
    }
}
/* Code to do persist into a ZIP file
   public void save (ZipOutputStream zos) throws IOException {
     Properties props = new Properties ();
     settings.store(props);
     Properties versionProps = new Properties ();
     versionProps.put("major", ""+SNAPSHOT_FILE_VERSION_MAJOR);
     versionProps.put("minor", ""+SNAPSHOT_FILE_VERSION_MINOR);
     ByteArrayOutputStream baos = new ByteArrayOutputStream(1000000); // ~1MB pre-allocated
     BufferedOutputStream bufBaos = new BufferedOutputStream(baos);
     DataOutputStream snapshotDataStream = new DataOutputStream(bufBaos);
     try {
       snapshot.writeToStream(snapshotDataStream);
       snapshotDataStream.flush();
       // binary file format is ZIP file with the following content:
       // 1. version properties stored in file <type>.properties
       // 2. snapshot data in file "data"
       // 3. settings properties in file "settings.properties"
       if (DEBUG) {
         System.err.println("LoadedSnapshot.DEBUG: save version:" + SNAPSHOT_FILE_VERSION_MAJOR + "." + SNAPSHOT_FILE_VERSION_MINOR);
         System.err.println("LoadedSnapshot.DEBUG: save type:" + getType());
         System.err.println("LoadedSnapshot.DEBUG: save length of snapshot data:" + baos.size());
       }
       // 1. store version data, in the form of properties file named by type of results
       switch (getType ()) {
         case SNAPSHOT_TYPE_CPU: zos.putNextEntry(new ZipEntry("cpu.properties")); break;
         case SNAPSHOT_TYPE_CODEFRAGMENT: zos.putNextEntry(new ZipEntry("fragment.properties")); break;
         case SNAPSHOT_TYPE_MEMORY_ALLOCATIONS: zos.putNextEntry(new ZipEntry("allocations.properties")); break;
         case SNAPSHOT_TYPE_MEMORY_LIVENESS: zos.putNextEntry(new ZipEntry("liveness.properties")); break;
         default: throw new IllegalStateException();
       }
       versionProps.store(zos, "");
       zos.flush();
       zos.closeEntry();
       // 2. store data into file "data"
       zos.putNextEntry(new ZipEntry("data"));
       writeBytes (zos, baos.toByteArray());
       zos.flush();
       zos.closeEntry();
       // 3. store properties as "settings.properties"
       zos.putNextEntry(new ZipEntry("settings.properties"));
       props.store(zos, "");
       zos.flush();
       zos.closeEntry();
     } finally {
       snapshotDataStream.close ();
     }
   }
   public void load (ZipInputStream zis) throws IOException {
     Properties settingsProps = null;
     Properties versionProps = null;
     int type = SNAPSHOT_TYPE_UNKNOWN;
     byte[] dataBytes = null;
     ZipEntry ze = zis.getNextEntry();
     while (ze != null) {
       if (ze.isDirectory()) continue;
       String name = ze.getName();
       if (name.equals ("data")) {
         dataBytes = readBytes (zis);
       } else if (name.equals ("settings.properties")) {
         settingsProps = new Properties ();
         settingsProps.load(zis);
       } else if (type == SNAPSHOT_TYPE_UNKNOWN && name.endsWith(".properties")) {
         if (name.equals ("cpu.properties")) type = SNAPSHOT_TYPE_CPU;
         else if (name.equals ("fragment.properties")) type = SNAPSHOT_TYPE_CODEFRAGMENT;
         else if (name.equals ("allocations.properties")) type = SNAPSHOT_TYPE_MEMORY_ALLOCATIONS;
         else if (name.equals ("liveness.properties")) type = SNAPSHOT_TYPE_MEMORY_LIVENESS;
         if (type != SNAPSHOT_TYPE_UNKNOWN) {
           versionProps = new Properties ();
           versionProps.load(zis);
         }
       }
       ze = zis.getNextEntry();
     }
     if (dataBytes == null) throw new IOException ("The file is not a valid NetBeans Profiler Snapshot file: missing results data");
     if (settingsProps == null) throw new IOException ("The file is not a valid NetBeans Profiler Snapshot file: missing settings data");
     if (versionProps == null) throw new IOException ("The file is not a valid NetBeans Profiler Snapshot file: missing type and version data");
     // binary file format is ZIP file with the following content:
     // 1. version properties stored in file <type>.properties
     // 2. snapshot data in file "data"
     // 3. settings properties in file "settings.properties"
     // 2. int type
   /*    byte majorVersion = zis.readByte();
       byte minorVersion = zis.readByte();
       if (majorVersion > SNAPSHOT_FILE_VERSION_MAJOR) throw new IOException("Snapshot file is corrupted: unsupported file version");
 * /
   /*    // 5. snapshot data bytes
       int readLen1 = zis.read(dataBytes);
       if (lenData1 != readLen1) throw new IOException("Snapshot file is corrupted: cannot read snapshot data");
       // 6. int length of settings data size
       int lenData2 = zis.readInt();
       byte[] settingsBytes = new byte [lenData2];
       // 7. settings data bytes (.properties plain text file format)
       int readLen2 = zis.read(settingsBytes);
       if (lenData2 != readLen2) throw new IOException("Snapshot file is corrupted: cannot read settings data");
 * /
       // Process read data:
   /*
       if (DEBUG) {
         System.err.println("LoadedSnapshot.DEBUG: load version:" + majorVersion + "."+minorVersion);
         System.err.println("LoadedSnapshot.DEBUG: load type:" + type);
         System.err.println("LoadedSnapshot.DEBUG: load length of snapshot data:" + lenData1);
         System.err.println("LoadedSnapshot.DEBUG: load length of settings data:" + lenData2);
       }
 * /
       switch (type) {
         case SNAPSHOT_TYPE_CPU: snapshot = new CPUResultsSnapshot (); break;
         case SNAPSHOT_TYPE_CODEFRAGMENT: snapshot = new CodeRegionResultsSnapshot (); break;
         case SNAPSHOT_TYPE_MEMORY_ALLOCATIONS: snapshot = new AllocMemoryResultsSnapshot(); break;
         case SNAPSHOT_TYPE_MEMORY_LIVENESS: snapshot = new LivenessMemoryResultsSnapshot(); break;
         default: throw new IOException ("Snapshot file is corrupted: unrecognized snapshot type"); // not supported
       }
       ByteArrayInputStream bais = new ByteArrayInputStream(dataBytes);
       BufferedInputStream bufBais = new BufferedInputStream(bais);
       DataInputStream dataDis = new DataInputStream(bufBais);
       try {
         snapshot.readFromStream(dataDis);
       } catch (IOException e) {
         ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
         throw new IOException (getCorruptedMessage(e));
       } finally {
         dataDis.close ();
       }
       settings = new ProfilingSettings();
       settings.load(settingsProps);
     }
     private byte[] readBytes(ZipInputStream zis) throws IOException {
       int ch1 = zis.read();
       int ch2 = zis.read();
       int ch3 = zis.read();
       int ch4 = zis.read();
       if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
       int length = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
       System.err.println("Loading length:"+length);
       if (length < 0) throw new IOException("Wrong data size: "+length);
       // TODO: if length is too large by error, we should not crash by allocating too much memory, but what is too much?
       byte[] bytes = new byte[length];
       zis.read(bytes, 0, length);
       for (int i = 0; i < 10; i++) {
         System.err.println("byte["+i+"]="+bytes[i]);
       }
       for (int i = bytes.length-10; i < bytes.length; i++) {
         System.err.println("byte["+i+"]="+bytes[i]);
       }
       return bytes;
     }
     private void writeBytes(ZipOutputStream zos, byte[] bytes) throws IOException {
       int v = bytes.length;
       System.err.println("Storing length:"+v);
       for (int i = 0; i < 10; i++) {
         System.err.println("byte["+i+"]="+bytes[i]);
       }
       for (int i = bytes.length-10; i < bytes.length; i++) {
         System.err.println("byte["+i+"]="+bytes[i]);
       }
       zos.write((v >>> 24) & 0xFF);
       zos.write((v >>> 16) & 0xFF);
       zos.write((v >>>  8) & 0xFF);
       zos.write((v >>>  0) & 0xFF);
       zos.write(bytes);
     }
     private static String getCorruptedMessage (IOException e) {
       String message = e.getMessage();
       if (message == null) {
         if (e instanceof EOFException) return "Snapshot file is corrupted: file too short";
         else return "Snapshot file is corrupted";
       }
       else return "Snapshot file is corrupted: " + message;
     }
     public static LoadedSnapshot loadSnapshot (ZipInputStream zis) throws IOException {
       LoadedSnapshot ls = new LoadedSnapshot ();
       ls.load (zis);
       return ls;
     }
 */
