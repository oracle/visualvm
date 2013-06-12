/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ProfilingResultsDispatcher;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsSnapshot;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.openide.windows.WindowManager;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.results.cpu.CPUResultsDiff;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.project.ProjectStorage;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;


/** An manager for management/notifications about obtainer profiling results.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "ResultsManager_SnapshotSaveFailedMsg=Failed to save snapshot: {0}",
    "ResultsManager_SnapshotCreateFailedMsg=Failed to create snapshot file: {0}",
    "ResultsManager_ProfiledAppTerminatedMsg=Failed to obtain results snapshot. The profiled application terminated.",
    "ResultsManager_DataNotAvailableMsg=Failed to obtain results snapshot. Data is not available yet.",
    "ResultsManager_OutOfMemoryMsg=Too much data collected - the profiler ran out of memory.\n\nCollected profiling data has been deleted and profiling resumed.\nTo avoid this error, increase the -Xmx value\nin the etc/netbeans.conf file in NetBeans IDE installation\nor decrease the amount of detail in profiling settings.",
    "ResultsManager_SnapshotDeleteFailedMsg=Failed to delete the snapshot file: {0}",
    "ResultsManager_CantFindSnapshotLocationMsg=Cannot find default location for snapshot in project: {0}",
    "ResultsManager_SnapshotCreateInProjectFailedMsg=Failed to create snapshot file in project: {0}",
    "ResultsManager_SnapshotLoadFailedMsg=Error while loading snapshot: {0}",
    "ResultsManager_SnapshotsLoadFailedMsg=Loading snapshots failed.",
    "ResultsManager_ObtainSavedSnapshotsFailedMsg=Failed to obtain list of saved snaphshots for project: {0}",
    "ResultsManager_SelectDirDialogCaption=Select Target Directory",
    "ResultsManager_SaveButtonName=Save",
    "ResultsManager_OverwriteFileDialogCaption=Overwrite Existing File?",
    "ResultsManager_OverwriteFileDialogMsg=The target folder already contains file {0}\n Do you want to overwrite this file?",
    "ResultsManager_FileDeleteFailedMsg=Cannot delete the existing file: {0}",
    "ResultsManager_SnapshotExportFailedMsg=Failed to save snapshot: {0}",
    "ResultsManager_SelectFileOrDirDialogCaption=Select File or Directory",
    "ResultsManager_ProfilerSnapshotFileFilter=Profiler Snapshot File (*.{0})",
    "ResultsManager_ProfilerHeapdumpFileFilter=Heap Dump File (*.{0})",
    "ResultsManager_OutOfMemorySavingMsg=<html><b>Not enough memory to save the snapshot.</b><br><br>To avoid this error, increase the -Xmx value<br>in the etc/netbeans.conf file in NetBeans IDE installation.</html>",
    "ResultsManager_CannotCompareSnapshotsMsg=<html><b>Cannot compare snapshots:</b><br><br>  {0}<br>  {1}<br><br>Make sure that both snaphots are the same type.</html>",
    "ResultsManager_DirectoryDoesntExistCaption=Selected Directory Does Not Exist",
    "ResultsManager_DirectoryDoesntExistMsg=The directory you have selected does not exist.\nDo you want to create the directory?",
    "ResultsManager_SnapshotLoadFailed=<html>Snapshot <b>{0}</b> failed to load</html>",
    "ResultsManager_CannotOpenSnapshotMsg=<html><b>Cannot open profiler snapshot.</b><br><br>Attempting to open null snapshot.<br>Check the logfile for details.</html>",
    "ResultsManager_CpuSnapshotDisplayName=cpu: {0}",
    "ResultsManager_MemorySnapshotDisplayName=mem: {0}",
    "ResultsManager_HeapSnapshotDisplayName=heap: {0}",
    "MSG_SavingSnapshots=Saving Snapshots",
    "MSG_SavingSnapshot=Saving Snapshot"
})
public final class ResultsManager {
    final private static Logger LOGGER = Logger.getLogger(ResultsManager.class.getName());
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //  /**
    //   * Resets collected results (either memory or CPU, depending on current mode).
    //   */
    //  public void resetCollectors() {
    //    reset();
    //    int instr = Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType();
    //    ProfilingResultsDispatcher.getDefault().reset();
    //    if (instr == CommonConstants.INSTR_RECURSIVE_FULL || instr == CommonConstants.INSTR_RECURSIVE_SAMPLED) {
    //      // TODO reset cpu profiling results collectors
    ////      CPUCallGraphBuilder.resetCollectors();
    //    } else if (instr == CommonConstants.INSTR_OBJECT_ALLOCATIONS || instr == CommonConstants.INSTR_OBJECT_LIVENESS) {
    //      MemoryCallGraphBuilder mcgb = Profiler.getDefault().getTargetAppRunner().getProfilerClient().getMemoryCallGraphBuilder();
    //      mcgb.resetCollectors();
    //    }
    //    fireResultsReset();
    //  }
    static class SelectedFile {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        FileObject folder;
        String fileExt;
        String fileName;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        SelectedFile(FileObject folder, String fileName, String fileExt) {
            this.folder = folder;
            this.fileName = fileName;
            this.fileExt = fileExt;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    public static final String SNAPSHOT_EXTENSION = "nps"; // NOI18N
    public static final String HEAPDUMP_EXTENSION = "hprof"; // NOI18N
    /* see  org.netbeans.modules.sampler.SampleOutputStream.FILE_EXT */ 
    public static final String STACKTRACES_SNAPSHOT_EXTENSION = "npss"; // NOI18N 

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList<LoadedSnapshot> loadedSnapshots = new ArrayList<LoadedSnapshot>();
    private File exportDir;
    private HashMap<FileObject, ProfilingSettings> settingsCache = new HashMap<FileObject, ProfilingSettings>();
    private HashMap<FileObject, Integer> typeCache = new HashMap<FileObject, Integer>();
    private Window mainWindow;
    private boolean resultsAvailable = false;
    
    private Lookup.Result<SnapshotsListener> snapshotListeners;
    private Lookup.Result<ResultsListener> resultsListeners;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    private ResultsManager() {
        Lookup l = Lookup.getDefault();
        snapshotListeners = l.lookupResult(SnapshotsListener.class);
        resultsListeners = l.lookupResult(ResultsListener.class);
    }

    private static class Singleton {
        final private static ResultsManager INSTANCE = new ResultsManager();
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ResultsManager getDefault() {
        return Singleton.INSTANCE;
    }

    public String getDefaultSnapshotFileName(LoadedSnapshot ls) {
        return "snapshot-" + ls.getSnapshot().getTimeTaken(); // NOI18N
    }
    
    public String getSnapshotDisplayName(LoadedSnapshot ls) {
        String name = ls.getFile() == null ? null : ls.getFile().getName();
        if (name == null) {
            name = getDefaultSnapshotFileName(ls);
        } else {
            int dotIndex = name.lastIndexOf('.'); // NOI18N
            if (dotIndex > 0 && dotIndex <= name.length() - 2)
                name = name.substring(0, dotIndex);
        }
        return getSnapshotDisplayName(name, ls.getType());
    }
        
    public String getSnapshotDisplayName(String fileName, int snapshotType) {
        String displayName;
        if (fileName.startsWith("snapshot-")) { // NOI18N
            String time = fileName.substring("snapshot-".length(), fileName.length()); // NOI18N
            try {
                long timeStamp = Long.parseLong(time);
                displayName = StringUtils.formatUserDate(new Date(timeStamp));
            } catch (NumberFormatException e) {
                // file name is probably customized
                displayName = fileName;
            }
        } else {
            displayName = fileName;
        }
        switch (snapshotType) {
            case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                return Bundle.ResultsManager_CpuSnapshotDisplayName(displayName);
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
            case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
                return Bundle.ResultsManager_MemorySnapshotDisplayName(displayName);
            default:
                return displayName;
        }
    }
    
    public String getHeapDumpDisplayName(String fileName) {
        String displayName;
        if (fileName.startsWith("heapdump-")) { // NOI18N
            String time = fileName.substring("heapdump-".length(), fileName.length()); // NOI18N
            try {
                long timeStamp = Long.parseLong(time);
                displayName = StringUtils.formatUserDate(new Date(timeStamp));
            } catch (NumberFormatException e) {
                // file name is probably customized
                displayName = fileName;
            }
        } else {
            displayName = fileName;
        }
        return Bundle.ResultsManager_HeapSnapshotDisplayName(displayName);
    }

    public LoadedSnapshot[] getLoadedSnapshots() {
        return loadedSnapshots.toArray(new LoadedSnapshot[0]);
    }

    public LoadedSnapshot getSnapshotFromFileObject(FileObject fo) {
        LoadedSnapshot ls = findAlreadyLoadedSnapshot(fo);

        if (ls != null) {
            return ls;
        }

        try {
            return loadSnapshotFromFileObject(fo);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, null, e);

            return null;
        }
    }

    public ProfilingSettings getSnapshotSettings(FileObject fo) {
        ProfilingSettings settings = settingsCache.get(fo);

        if ((settings == null) && !settingsCache.containsKey(fo)) {
            settings = readSettingsFromFile(fo);
            settingsCache.put(fo, settings);
        }

        return settings;
    }

    public int getSnapshotType(FileObject fo) {
        Integer type = typeCache.get(fo);

        if (type == null) {
            type = Integer.valueOf(readTypeFromFile(fo));
            typeCache.put(fo, type);
        }

        return type.intValue();
    }

    @ServiceProviders({@ServiceProvider(service=CPUCCTProvider.Listener.class), @ServiceProvider(service=MemoryCCTProvider.Listener.class)})
    public static final class ResultsMonitor implements CPUCCTProvider.Listener, MemoryCCTProvider.Listener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void cctEstablished(RuntimeCCTNode runtimeCCTNode, boolean empty) {
            if (!empty) {
                ResultsManager rm = getDefault();
                if (!rm.resultsAvailable() && !isSomeResultsAvailable()) {
                    return;
                }
                rm.resultsBecameAvailable();
            }
        }

        public void cctReset() {
            getDefault().resultsReset();
        }
        
        private boolean isSomeResultsAvailable() {
            // check that we have data for snapshot, CPU profiling can have non-empty
            // batch, but this batch can contain only marker methods
            ProfilerClient client = Profiler.getDefault().getTargetAppRunner().getProfilerClient();
            int instrType = client.getCurrentInstrType();

            if (instrType == ProfilerEngineSettings.INSTR_RECURSIVE_FULL ||
                instrType == ProfilerEngineSettings.INSTR_RECURSIVE_SAMPLED) {
                try {
                    // construct snapshot and check that it has some data
                    client.getCPUProfilingResultsSnapshot(false);
                } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
                    // we don't have data for snapshot
                    return false;
                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                    // target VM is down
                    return false;
                }
            }
            return true;
        }
    }

    public void closeSnapshot(LoadedSnapshot ls) {
        if (ls != null) {
            loadedSnapshots.remove(ls);
            fireSnapshotRemoved(ls);
        }
    }

    public void compareSnapshots(FileObject snapshot1FO, FileObject snapshot2FO) {
        LoadedSnapshot s1 = null;
        LoadedSnapshot s2 = null;

        try {
            s1 = findAlreadyLoadedSnapshot(snapshot1FO);

            if (s1 == null) {
                s1 = loadSnapshotFromFileObject(snapshot1FO);
            }

            s2 = findAlreadyLoadedSnapshot(snapshot2FO);

            if (s2 == null) {
                s2 = loadSnapshotFromFileObject(snapshot2FO);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, Bundle.ResultsManager_SnapshotLoadFailed(e.getMessage()), e);

            return;
        }

        if ((s1 != null) && (s2 != null)) {
            compareSnapshots(s1, s2);
        } else {
            ProfilerDialogs.displayError(Bundle.ResultsManager_SnapshotsLoadFailedMsg());
        }
    }

    public void compareSnapshots(LoadedSnapshot s1, LoadedSnapshot s2) {
        ResultsSnapshot snap1 = s1.getSnapshot();
        ResultsSnapshot snap2 = s2.getSnapshot();
        ResultsSnapshot diff = null;

        if (snap1 instanceof SampledMemoryResultsSnapshot && snap2 instanceof SampledMemoryResultsSnapshot) {
            diff = new SampledMemoryResultsDiff((SampledMemoryResultsSnapshot)snap1,
                                              (SampledMemoryResultsSnapshot)snap2);
        } else if (snap1 instanceof AllocMemoryResultsSnapshot && snap2 instanceof AllocMemoryResultsSnapshot) {
            diff = new AllocMemoryResultsDiff((AllocMemoryResultsSnapshot)snap1,
                                              (AllocMemoryResultsSnapshot)snap2);
        }
        else if (snap1 instanceof LivenessMemoryResultsSnapshot && snap2 instanceof LivenessMemoryResultsSnapshot) {
            diff = new LivenessMemoryResultsDiff((LivenessMemoryResultsSnapshot)snap1,
                                                 (LivenessMemoryResultsSnapshot)snap2);
        }
        else if (snap1 instanceof CPUResultsSnapshot && snap2 instanceof CPUResultsSnapshot) {
            diff = new CPUResultsDiff((CPUResultsSnapshot)snap1, (CPUResultsSnapshot)snap2);
        }

        if (diff != null) {
            SnapshotsDiffWindow sdw = SnapshotsDiffWindow.get(diff, s1, s2);
            sdw.open();
            sdw.requestActive();
        } else {
            ProfilerDialogs.displayError(Bundle.ResultsManager_CannotCompareSnapshotsMsg(
                                            s1.getFile().getName(), 
                                            s2.getFile().getName()));
        }
    }

    public void deleteSnapshot(FileObject snapshotFile) {
        LoadedSnapshot ls = findLoadedSnapshot(FileUtil.toFile(snapshotFile));

        try {
            snapshotFile.delete();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, Bundle.ResultsManager_SnapshotDeleteFailedMsg(e.getMessage()), e);

            return; // do not proceed with removing the snapshot from internal structures
        }

        if (ls != null) { // if the snapshot has been loaded, remove the internal structures as well
            loadedSnapshots.remove(ls);
            fireSnapshotRemoved(ls);
        }
    }

    public void exportSnapshots(final FileObject[] selectedSnapshots) {
        assert (selectedSnapshots != null);
        assert (selectedSnapshots.length > 0);

        final String[] fileName = new String[1], fileExt = new String[1];
        final FileObject[] dir = new FileObject[1];
        if (selectedSnapshots.length == 1) {
            SelectedFile sf = selectSnapshotTargetFile(selectedSnapshots[0].getName(),
                                selectedSnapshots[0].getExt().equals(HEAPDUMP_EXTENSION));

            if ((sf != null) && checkFileExists(sf)) {
                fileName[0] = sf.fileName;
                fileExt[0] = sf.fileExt;
                dir[0] = sf.folder;
            } else { // dialog cancelled by the user
                return;
            }
        } else {
            JFileChooser chooser = new JFileChooser();

            if (exportDir != null) {
                chooser.setCurrentDirectory(exportDir);
            }

            chooser.setDialogTitle(Bundle.ResultsManager_SelectDirDialogCaption());
            chooser.setApproveButtonText(Bundle.ResultsManager_SaveButtonName());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();

                if (!file.exists()) {
                    if (!ProfilerDialogs.displayConfirmation(
                            Bundle.ResultsManager_DirectoryDoesntExistMsg(), 
                            Bundle.ResultsManager_DirectoryDoesntExistCaption())) {
                        return; // cancelled by the user
                    }

                    file.mkdir();
                }

                exportDir = file;

                dir[0] = FileUtil.toFileObject(FileUtil.normalizeFile(file));
            } else { // dialog cancelled
                return;
            }
        }
        final ProgressHandle ph = ProgressHandleFactory.createHandle(Bundle.MSG_SavingSnapshots());
        ph.setInitialDelay(500);
        ph.start();
        ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < selectedSnapshots.length; i++) {
                        exportSnapshot(selectedSnapshots[i], dir[0], fileName[0] != null ? fileName[0] : selectedSnapshots[i].getName(), fileExt[0] != null ? fileExt[0] : selectedSnapshots[i].getExt());
                    }
                } finally {
                    ph.finish();
                }
            }
        });
    }

    public LoadedSnapshot findLoadedSnapshot(ResultsSnapshot snapshot) {
        Iterator it = loadedSnapshots.iterator();

        while (it.hasNext()) {
            LoadedSnapshot ls = (LoadedSnapshot) it.next();

            if (ls.getSnapshot() == snapshot) {
                return ls;
            }
        }

        return null;
    }

    public LoadedSnapshot findLoadedSnapshot(File snapshotFile) {
        Iterator it = loadedSnapshots.iterator();

        while (it.hasNext()) {
            LoadedSnapshot ls = (LoadedSnapshot) it.next();

            if ((ls.getFile() != null) && ls.getFile().equals(snapshotFile)) {
                return ls;
            }
        }

        return null;
    }
    
    private static final String HPROF_HEADER = "JAVA PROFILE 1.0"; // NOI18H
    private static final long MIN_HPROF_SIZE = 1024*1024L;
    public static boolean checkHprofFile(File file) {
        try {
            if (file.isFile() && file.canRead() && file.length()>MIN_HPROF_SIZE) { // heap dump must be 1M and bigger
                byte[] prefix = new byte[HPROF_HEADER.length()+4];
                RandomAccessFile raf = new RandomAccessFile(file,"r");  // NOI18H
                raf.readFully(prefix);
                raf.close();
                if (new String(prefix).startsWith(HPROF_HEADER)) {
                    return true;
                }
            }
        } catch (FileNotFoundException ex) {
            return false;
        } catch (IOException ex) {
            return false;
        }
        return false;
    }

    public FileObject[] listSavedHeapdumps(Lookup.Provider project, File directory) {
        try {
            FileObject snapshotsFolder = null;
                    
            if (project == null && directory != null) {
                snapshotsFolder = FileUtil.toFileObject(directory);
            } else {
                snapshotsFolder = ProjectStorage.getSettingsFolder(project, false);
            }

            if (snapshotsFolder == null) {
                return new FileObject[0];
            }

            snapshotsFolder.refresh();

            FileObject[] children = snapshotsFolder.getChildren();

            ArrayList /*<FileObject>*/ files = new ArrayList /*<FileObject>*/();

            for (int i = 0; i < children.length; i++) {
                FileObject child = children[i];
                if (checkHprofFile(FileUtil.toFile(children[i])))
                    files.add(child);
            }

            Collections.sort(files,
                             new Comparator() {
                    public int compare(Object o1, Object o2) {
                        FileObject f1 = (FileObject) o1;
                        FileObject f2 = (FileObject) o2;

                        return f1.getName().compareTo(f2.getName());
                    }
                });

            FileObject[] ret = new FileObject[files.size()];
            files.toArray(ret);

            return ret;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, Bundle.ResultsManager_ObtainSavedSnapshotsFailedMsg(e.getMessage()), e);

            return new FileObject[0];
        }
    }

    public FileObject[] listSavedSnapshots(Lookup.Provider project, File directory) {
        try {
            FileObject snapshotsFolder = null;
                    
            if (project == null && directory != null) {
                snapshotsFolder = FileUtil.toFileObject(directory);
            } else {
                snapshotsFolder = ProjectStorage.getSettingsFolder(project, false);
            }

            if (snapshotsFolder == null) {
                return new FileObject[0];
            }

            snapshotsFolder.refresh();

            FileObject[] children = snapshotsFolder.getChildren();

            ArrayList /*<FileObject>*/ files = new ArrayList /*<FileObject>*/();

            for (int i = 0; i < children.length; i++) {
                FileObject child = children[i];

                if (child.getExt().equalsIgnoreCase(SNAPSHOT_EXTENSION)) {
                    files.add(child);
                }
            }

            Collections.sort(files,
                             new Comparator() {
                    public int compare(Object o1, Object o2) {
                        FileObject f1 = (FileObject) o1;
                        FileObject f2 = (FileObject) o2;

                        return f1.getName().compareTo(f2.getName());
                    }
                });

            FileObject[] ret = new FileObject[files.size()];
            files.toArray(ret);

            return ret;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, Bundle.ResultsManager_ObtainSavedSnapshotsFailedMsg(e.getMessage()), e);

            return new FileObject[0];
        }
    }

    public LoadedSnapshot loadSnapshot(FileObject selectedFile) {
        try {
            return loadSnapshotImpl(selectedFile);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, Bundle.ResultsManager_SnapshotLoadFailed(e.getMessage()), e);

            return null;
        }
    }

    public LoadedSnapshot[] loadSnapshots(FileObject[] selectedFiles) {
        LoadedSnapshot[] ret = new LoadedSnapshot[selectedFiles.length];

        for (int i = 0; i < selectedFiles.length; i++) {
            try {
                if (selectedFiles[i] != null) {
                    ret[i] = loadSnapshotImpl(selectedFiles[i]);
                }
            } catch (IOException e) {
                ProfilerDialogs.displayError(Bundle.ResultsManager_SnapshotLoadFailedMsg(selectedFiles[i].getNameExt()));
            }
        }

        return ret;
    }
    
    public void openSnapshot(File snapshot) {
        File sf = FileUtil.normalizeFile(snapshot);
        FileObject snapshotFo = FileUtil.toFileObject(sf);
        openSnapshot(snapshotFo);
    }
    
    public void openSnapshot(FileObject snapshotFo) {
        try {
            DataObject snapshotDo = DataObject.find(snapshotFo);
            OpenCookie open = snapshotDo.getCookie(OpenCookie.class);
            if (open != null) {
                open.open();
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void openSnapshot(LoadedSnapshot ls) {
        openSnapshot(ls, CommonConstants.SORTING_COLUMN_DEFAULT, false); // target component decides which column will be used for sorting
    }

    public void openSnapshot(final LoadedSnapshot ls, final int sortingColumn, final boolean sortingOrder) {
        if (ls == null) ProfilerDialogs.displayError(Bundle.ResultsManager_CannotOpenSnapshotMsg());
        else CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                SnapshotResultsWindow srw = SnapshotResultsWindow.get(ls, sortingColumn, sortingOrder);
                srw.open();
                srw.requestActive();
            }
        });
    }

    public void openSnapshots(LoadedSnapshot[] loaded) {
        LoadedSnapshot loadedSnapshot = null;

        try {
            SnapshotResultsWindow srw = null;

            for (int i = 0; i < loaded.length; i++) {
                loadedSnapshot = loaded[i];

                if (loaded[i] != null) {
                    srw = SnapshotResultsWindow.get(loadedSnapshot);
                    srw.open();
                }
            }

            if (srw != null) {
                srw.requestActive(); // activate the last one
            }
        } catch (Exception e) {
            ProfilerLogger.log(e);

            ProfilerDialogs.displayError(Bundle.ResultsManager_SnapshotLoadFailed(
                    loadedSnapshot.getFile().getAbsolutePath()));
        }
    }

    public LoadedSnapshot prepareSnapshot() {
        return prepareSnapshot(true);
    }

    public LoadedSnapshot prepareSnapshot(boolean reqeustData) {
        ResultsSnapshot snapshot = null;

        if (!resultsAvailable()) {
            return null;
        }

        try {
            final TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();
            final ProfilerClient client = runner.getProfilerClient();
            final int currentInstrType = client.getCurrentInstrType();

            try {
                switch (currentInstrType) {
                    case ProfilerEngineSettings.INSTR_OBJECT_ALLOCATIONS:
                    case ProfilerEngineSettings.INSTR_OBJECT_LIVENESS:
                    case ProfilerEngineSettings.INSTR_NONE_MEMORY_SAMPLING:
                        snapshot = client.getMemoryProfilingResultsSnapshot(reqeustData);

                        break;
                    case ProfilerEngineSettings.INSTR_RECURSIVE_FULL:
                    case ProfilerEngineSettings.INSTR_RECURSIVE_SAMPLED:
                    case ProfilerEngineSettings.INSTR_NONE_SAMPLING:
                        snapshot = client.getCPUProfilingResultsSnapshot(reqeustData);

                        break;
                    case ProfilerEngineSettings.INSTR_CODE_REGION:
                        snapshot = client.getCodeRegionProfilingResultsSnapshot();

                        break;
                }
            } catch (ClientUtils.TargetAppOrVMTerminated e1) {
                ProfilerDialogs.displayWarning(Bundle.ResultsManager_ProfiledAppTerminatedMsg());
                ProfilerLogger.log(e1.getMessage());
            } catch (CPUResultsSnapshot.NoDataAvailableException e2) {
                LOGGER.log(Level.SEVERE, Bundle.ResultsManager_DataNotAvailableMsg(), e2);
            } catch (OutOfMemoryError e) {
                try {
                    reset(); // reset the client data
                    runner.resetTimers(); // reset the server data
                } catch (ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated) {
                    // the target app has died; clean up all client data
                    runner.getProfilerClient().resetClientData();
                }

                LOGGER.log(Level.SEVERE, Bundle.ResultsManager_OutOfMemoryMsg(), e);
            }
        } finally {
            if (snapshot != null) {
                ProfilingSettings settings = new ProfilingSettings();
                Profiler.getDefault().getLastProfilingSettings().copySettingsInto(settings);
                settings.setSettingsName(Profiler.getDefault().getLastProfilingSettings().getSettingsName());

                Lookup.Provider profiledProject = NetBeansProfiler.getDefaultNB().getProfiledProject();

                return new LoadedSnapshot(snapshot, settings, null, profiledProject);
            }
        }

        return null;
    }

    /**
     * This should be called when the app is restarted or "Reset Collected Results" is invoked (because once this happened,
     * there are all sorts of data that's going to be deleted/changed, and an attempt to do something with old results displayed
     * here can cause big problems). It should also set the results panel invisible (or is it already happening?) etc.
     */
    public void reset() {
        ProfilingResultsDispatcher.getDefault().reset();
        resultsReset();
    }

    public boolean resultsAvailable() {
        return resultsAvailable;
    }

    public boolean saveSnapshot(LoadedSnapshot snapshot, FileObject profFile) {
        FileLock lock = null;
        DataOutputStream dos = null;
        
        boolean isSaved = snapshot.isSaved();
        snapshot.setSaved(true); // Set the file as saved in advance to prevent saving it again
        try {
            lock = profFile.lock();

            OutputStream os = profFile.getOutputStream(lock);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            dos = new DataOutputStream(bos);
            //      System.out.println("Saving snapshot [" + snapshot.getSnapshot().getTimeTaken() + "]");
            snapshot.save(dos);
            dos.close();
            snapshot.setFile(FileUtil.toFile(profFile));
            snapshot.setSaved(true);
            fireSnapshotSaved(snapshot);
        } catch (IOException e) {
            snapshot.setSaved(isSaved);
            try {
                if (dos != null) {
                    dos.close();
                }

                if (lock != null) {
                    profFile.delete(lock);
                }
            } catch (Exception e2) {
            }

            ProfilerDialogs.displayError(Bundle.ResultsManager_SnapshotSaveFailedMsg(e.getMessage()));
            
            return false; // failure => we wont continue with firing the event
        } catch (OutOfMemoryError e) {
            snapshot.setSaved(isSaved);
            try {
                if (dos != null) {
                    dos.close();
                }

                if (lock != null) {
                    profFile.delete(lock);
                }
            } catch (Exception e2) {
            }

            ProfilerDialogs.displayError(Bundle.ResultsManager_OutOfMemorySavingMsg());

            return false; // failure => we wont continue with firing the event
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }

        return true;
    }

    public boolean saveSnapshot(LoadedSnapshot ls) {
        FileObject profFile = null;

        Lookup.Provider p = ls.getProject();
        FileObject saveDir = null;

        try {
            saveDir = ProjectStorage.getSettingsFolder(p, true);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, Bundle.ResultsManager_CantFindSnapshotLocationMsg(e.getMessage()), e);

            return false;
        }

        try {
            profFile = saveDir.createData(getDefaultSnapshotFileName(ls), SNAPSHOT_EXTENSION);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, Bundle.ResultsManager_SnapshotCreateInProjectFailedMsg(e.getMessage()), e);

            return false;
        }

        return saveSnapshot(ls, profFile);
    }

    public LoadedSnapshot takeSnapshot() {
        CommonUtils.runInEventDispatchThreadAndWait(new Runnable() {
                public void run() {
                    mainWindow = WindowManager.getDefault().getMainWindow();
                }
            });

        final Cursor cursor = mainWindow.getCursor();
        mainWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            LoadedSnapshot snapshot = prepareSnapshot();

            if (snapshot != null) {
                loadedSnapshots.add(snapshot);
                fireSnapshotTaken(snapshot);

                return snapshot;
            }
        } finally {
            mainWindow.setCursor(cursor);
        }

        return null;
    }

    protected void fireResultsAvailable() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "fireResultsAvailable", new Exception());
        }
        if (resultsListeners.allClasses().isEmpty()) {
            return;
        }

        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                for(ResultsListener rl : resultsListeners.allInstances()) {
                    rl.resultsAvailable();
                }
            }
        });
    }

    protected void fireResultsReset() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "fireResultsReset", new Exception());
        }
        if (resultsListeners.allClasses().isEmpty()) {
            return;
        }

        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                for(ResultsListener rl : resultsListeners.allInstances()) {
                    rl.resultsReset();
                }
            }
        });
    }

    protected void fireSnapshotLoaded(final LoadedSnapshot snapshot) {
        if (snapshotListeners.allClasses().isEmpty()) {
            return;
        }

        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                for(SnapshotsListener sl : snapshotListeners.allInstances()) {
                    sl.snapshotLoaded(snapshot);
                }
            }
        });
    }

    protected void fireSnapshotRemoved(final LoadedSnapshot snapshot) {
        if (snapshotListeners.allClasses().isEmpty()) {
            return;
        }

        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                for(SnapshotsListener sl : snapshotListeners.allInstances()) {
                    sl.snapshotRemoved(snapshot);
                }
            }
        });
    }

    protected void fireSnapshotSaved(final LoadedSnapshot snapshot) {
        if (snapshotListeners.allClasses().isEmpty()) {
            return;
        }

        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                for(SnapshotsListener sl : snapshotListeners.allInstances()) {
                    sl.snapshotSaved(snapshot);
                }
            }
        });
    }

    protected void fireSnapshotTaken(final LoadedSnapshot snapshot) {
        if (snapshotListeners.allClasses().isEmpty()) {
            return;
        }

        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                for(SnapshotsListener sl : snapshotListeners.allInstances()) {
                    sl.snapshotTaken(snapshot);
                }
            }
        });
    }

    void resultsBecameAvailable() {
        if (Profiler.getDefault().getProfilingState() == Profiler.PROFILING_INACTIVE) return; // Calibration, ignore
        resultsAvailable = true;
        fireResultsAvailable();
    }

    void resultsReset() {
        // NOTE: originally left as true for memory profiling due to Issue 60432, now seems to work correctly
        resultsAvailable = false;
        fireResultsReset();
    }

    // heapdump == true means selecting heapdump (*.hprof)
    SelectedFile selectSnapshotTargetFile(final String defaultName, final boolean heapdump) {
        String targetName;
        FileObject targetDir;

        // 1. let the user choose file or directory
        JFileChooser chooser = new JFileChooser();

        if (exportDir != null) {
            chooser.setCurrentDirectory(exportDir);
        }

        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle(Bundle.ResultsManager_SelectFileOrDirDialogCaption());
        chooser.setApproveButtonText(Bundle.ResultsManager_SaveButtonName());
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith("." + (heapdump ? HEAPDUMP_EXTENSION : SNAPSHOT_EXTENSION)); //NOI18N
                }

                public String getDescription() {
                    if (heapdump) {
                        return Bundle.ResultsManager_ProfilerHeapdumpFileFilter(HEAPDUMP_EXTENSION);
                    }
                    return Bundle.ResultsManager_ProfilerSnapshotFileFilter(SNAPSHOT_EXTENSION);
                }
            });

        if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow()) != JFileChooser.APPROVE_OPTION) {
            return null; // cancelled by the user
        }

        // 2. process both cases and extract file name and extension to use
        File file = chooser.getSelectedFile();
        String targetExt = heapdump ? HEAPDUMP_EXTENSION : SNAPSHOT_EXTENSION;

        if (file.isDirectory()) { // save to selected directory under default name
            exportDir = chooser.getCurrentDirectory();
            targetDir = FileUtil.toFileObject(FileUtil.normalizeFile(file));
            targetName = defaultName;
        } else { // save to selected file
            exportDir = chooser.getCurrentDirectory();

            targetDir = FileUtil.toFileObject(FileUtil.normalizeFile(exportDir));

            String fName = file.getName();

            // divide the file name into name and extension
            int idx = fName.lastIndexOf('.'); // NOI18N

            if (idx == -1) { // no extension
                targetName = fName;

                // extension will be used from source file
            } else { // extension exists
                targetName = fName.substring(0, idx);
                targetExt = fName.substring(idx + 1);
            }
        }

        // 3. return a newly created FileObject
        return new SelectedFile(targetDir, targetName, targetExt);
    }

    private boolean checkFileExists(SelectedFile sf) {
        // check if the file already exists and if so prompt the user
        FileObject existingFile = sf.folder.getFileObject(sf.fileName, sf.fileExt);

        if (existingFile != null) {
            if (!ProfilerDialogs.displayConfirmation(Bundle.ResultsManager_OverwriteFileDialogMsg(
                                                        sf.fileName + "." //NOI18N
                                                        + sf.fileExt), 
                                                     Bundle.ResultsManager_OverwriteFileDialogCaption())) {
                return false; // cancelled by the user
            }

            try {
                existingFile.delete();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, Bundle.ResultsManager_FileDeleteFailedMsg(e.getMessage()), e);

                return false;
            }
        }

        return true;
    }

    private void exportSnapshot(FileObject selectedSnapshot, FileObject targetFolder, String fileName, String fileExt) {
        if (checkFileExists(new SelectedFile(targetFolder, fileName, fileExt))) {
            try {
                FileUtil.copyFile(selectedSnapshot, targetFolder, fileName, fileExt);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, Bundle.ResultsManager_SnapshotExportFailedMsg(e.getMessage()), e);
            }
        }
    }

    private LoadedSnapshot findAlreadyLoadedSnapshot(FileObject selectedFile) {
        Iterator it = loadedSnapshots.iterator();
        File f = FileUtil.toFile(selectedFile);

        if (f == null) {
            return null;
        }

        while (it.hasNext()) {
            LoadedSnapshot ls = (LoadedSnapshot) it.next();

            if ((ls.getFile() != null) && (ls.getFile().equals(f))) {
                return ls;
            }
        }

        return null;
    }

    private Lookup.Provider findProjectForSnapshot(FileObject selectedFile) {
        return ProjectStorage.getProjectFromSettingsFolder(selectedFile.getParent());
    }

    private LoadedSnapshot loadSnapshotFromFileObject(FileObject selectedFile)
                                               throws IOException {
        DataInputStream dis = null;

        try {
            InputStream is = selectedFile.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            dis = new DataInputStream(bis);

            LoadedSnapshot ls = LoadedSnapshot.loadSnapshot(dis);

            if (ls != null) {
                ls.setFile(FileUtil.toFile(selectedFile));
                ls.setProject(findProjectForSnapshot(selectedFile));
            }

            return ls;
        } finally {
            if (dis != null) {
                dis.close();
            }
        }
    }

    private LoadedSnapshot loadSnapshotImpl(FileObject selectedFile)
                                     throws IOException {
        LoadedSnapshot ls = findAlreadyLoadedSnapshot(selectedFile);

        if (ls != null) {
            return ls;
        }

        ls = loadSnapshotFromFileObject(selectedFile);

        if (ls != null) {
            loadedSnapshots.add(ls);
            fireSnapshotLoaded(ls);
        }

        return ls;
    }

    private ProfilingSettings readSettingsFromFile(FileObject fo) {
        LoadedSnapshot ls = findAlreadyLoadedSnapshot(fo);

        if (ls != null) {
            return ls.getSettings();
        }

        DataInputStream dis = null;

        try {
            InputStream is = fo.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            dis = new DataInputStream(bis);

            // data before settings
            byte[] magicArray = new byte[LoadedSnapshot.PROFILER_FILE_MAGIC_STRING.length()];
            int len = dis.read(magicArray);

            if ((len != LoadedSnapshot.PROFILER_FILE_MAGIC_STRING.length())
                    || !LoadedSnapshot.PROFILER_FILE_MAGIC_STRING.equals(new String(magicArray))) {
                return null;
            }

            byte majorVersion = dis.readByte();
            byte minorVersion = dis.readByte();
            int type = dis.readInt();
            int compressedDataLen = dis.readInt();
            int uncompressedDataLen = dis.readInt();

            if (dis.skipBytes(compressedDataLen) != compressedDataLen) {
                return null;
            }

            // settings data
            int settingsLen = dis.readInt();
            byte[] settingsBytes = new byte[settingsLen];

            if (dis.read(settingsBytes) != settingsLen) {
                return null;
            }

            // create settings
            Properties props = new Properties();
            ProfilingSettings settings = new ProfilingSettings();
            ByteArrayInputStream bais2 = new ByteArrayInputStream(settingsBytes);
            BufferedInputStream bufBais2 = new BufferedInputStream(bais2);
            DataInputStream settingsDis = new DataInputStream(bufBais2);

            try {
                props.load(settingsDis);
            } catch (IOException e) {
                ProfilerLogger.log(e);

                return null;
            } finally {
                settingsDis.close();
            }

            settings.load(props);

            return settings;
        } catch (Exception e) {
            ProfilerLogger.log(e);

            return null;
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                ProfilerLogger.log(e);
            }
        }
    }

    private int readTypeFromFile(FileObject fo) {
        LoadedSnapshot ls = findAlreadyLoadedSnapshot(fo);

        if (ls != null) {
            return ls.getType();
        }

        DataInputStream dis = null;

        try {
            InputStream is = fo.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            dis = new DataInputStream(bis);

            byte[] magicArray = new byte[LoadedSnapshot.PROFILER_FILE_MAGIC_STRING.length()];
            int len = dis.read(magicArray);

            if ((len != LoadedSnapshot.PROFILER_FILE_MAGIC_STRING.length())
                    || !LoadedSnapshot.PROFILER_FILE_MAGIC_STRING.equals(new String(magicArray))) {
                return LoadedSnapshot.SNAPSHOT_TYPE_UNKNOWN;
            }

            byte majorVersion = dis.readByte();
            byte minorVersion = dis.readByte();
            int type = dis.readInt();

            return type;
        } catch (Exception e) {
            return LoadedSnapshot.SNAPSHOT_TYPE_UNKNOWN;
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
