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

package org.netbeans.lib.profiler.instrumentation;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.classfile.BaseClassInfo;
import org.netbeans.lib.profiler.classfile.ClassLoaderTable;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.client.ClientUtils.SourceCodeSelection;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.lib.profiler.wireprotocol.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A high-level interface to all method instrumentation operations.
 * <p/>
 * Instrumentor subclasses find methods/classes to be instrumented.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 * @author Ian Formanek
 */
public class Instrumentor implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // TODO [release]: change value to TRUE to remove the print code below entirely by compiler
    private static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.instrumentation.Instrumentor") != null; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CodeRegionMethodInstrumentor crms;
    private MemoryProfMethodInstrumentor oms;
    private ProfilerEngineSettings settings;
    private ProfilingSessionStatus status;
    private RecursiveMethodInstrumentor ms;
    private RootMethods rootMethods;

    // Data for the case of code region instrumentation
    private SourceCodeSelection savedSourceCodeSelection;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of Instrumentor. A single instance is created by ProfileClient and reused on subsequent
     * profiling sessions.
     *
     * @param status   ProfilingSessionStatus used for profiling
     * @param settings Engine settings - same instance is reused for all profiling sessions, the settings are modified
     *                 each time before the session is started.
     */
    public Instrumentor(ProfilingSessionStatus status, ProfilerEngineSettings settings) {
        this.status = status;
        this.settings = settings;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getClassId(String className, int classLoaderId) {
        BaseClassInfo clazz;

        if (className.charAt(0) == '[') { // array , need special lookup
                                          // strip L and ; from className, see ClassFileParser.classNameAtCPIndex

            if (className.endsWith(";")) {
                int elIndex = className.indexOf('L');
                className = new StringBuffer(className).deleteCharAt(className.length() - 1).deleteCharAt(elIndex).toString();
            }

            clazz = ClassRepository.lookupSpecialClass(className);
        } else {
            clazz = ClassRepository.lookupClassOrCreatePlaceholder(className, classLoaderId);
        }

        if (clazz == null) {
            System.err.println("Warning: could not find class " + className + " loaded by the VM on the class path");

            // warning already issued in ClassRepository.lookupClass method, no need to do it again
            return -1;
        }

        return clazz.getInstrClassId();
    }

    public synchronized InstrumentMethodGroupCommand getCommandToUnprofileClasses(boolean[] unprofiledClassStatusArray) {
        ObjLivenessMethodInstrumentor olms = (ObjLivenessMethodInstrumentor) oms;
        Object[] ret = olms.getMethodsToInstrumentUponClassUnprofiling(unprofiledClassStatusArray);

        if (ret == null) {
            return new InstrumentMethodGroupCommand(null);
        } else {
            return new InstrumentMethodGroupCommand(INSTR_OBJECT_LIVENESS, (String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2],
                                                    null, oms.getNInstantiatableClasses());
        }
    }

    // --------------------------------------- Public interface ----------------------------------------------------------
    public String[] getRootClassNames() {
        List /*<String>*/ rootClassNames = rootMethods.getRootClassNames();
        RuntimeProfilingPoint[] pps = settings.getRuntimeProfilingPoints();

        if ((rootClassNames == null) && (pps.length > 0)) {
            rootClassNames = new ArrayList();
        }

        for (int i = 0; i < pps.length; i++) {
            RuntimeProfilingPoint pp = pps[i];
            String className = pp.getClassName();

            if (!rootClassNames.contains(className)) {
                rootClassNames.add(className);
            }
        }

        if (rootClassNames == null) {
            return null;
        }

        return (String[]) rootClassNames.toArray(new String[rootClassNames.size()]);
    }

    public void setSavedSourceCodeSelection(SourceCodeSelection[] s) {
        savedSourceCodeSelection = s[0];
    }

    public void setStatusInfoFromSourceCodeSelection(SourceCodeSelection[] s)
                                              throws ClassNotFoundException, BadLocationException, IOException, ClassFormatError {
        ClassRepository.CodeRegionBCI location = null;

        if (s.length > 0) {
            SourceCodeSelection sel = s[0];

            if (sel.definedViaSourceLines()) {
                status.instrStartLine = sel.getStartLine();
                status.instrEndLine = sel.getEndLine();
            }
        }

        rootMethods = new RootMethods(s);
    }

    public synchronized InstrumentMethodGroupCommand createClearAllInstrumentationCommand() {
        Object[] ret = null;

        switch (status.currentInstrType) {
            case INSTR_RECURSIVE_FULL:
            case INSTR_RECURSIVE_SAMPLED:
            case INSTR_OBJECT_ALLOCATIONS:
            case INSTR_OBJECT_LIVENESS:
                ms = null; // Free some memory
                ret = (new MiscInstrumentationOps(status)).getOrigCodeForAllInstrumentedMethods();

                break;
            case INSTR_CODE_REGION:
                ret = (new MiscInstrumentationOps(status)).getOrigCodeForSingleInstrumentedMethod(rootMethods);

                break;
        }

        ms = null;
        oms = null;
        crms = null; // Free some memory

        if (ret == null) {
            return new InstrumentMethodGroupCommand(null);
        } else {
            return new InstrumentMethodGroupCommand(INSTR_NONE, (String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2], null, 0);
        }
    }

    public synchronized InstrumentMethodGroupResponse createFollowUpInstrumentMethodGroupResponse(Command cmd) {
        if (cmd instanceof ClassLoadedCommand) {
            ClassLoadedCommand clcmd = (ClassLoadedCommand) cmd;
            int[] thisAndParentLoaderData = clcmd.getThisAndParentLoaderData();

            if (DEBUG) {
                System.err.println("Instrumentor.DEBUG: Class loaded command: " + cmd.toString()); // NOI18N
            }

            byte[] classFileBytes = clcmd.getClassFileBytes();

            if (classFileBytes != null) {
                ClassRepository.addVMSuppliedClassFile(clcmd.getClassName(), thisAndParentLoaderData[0], classFileBytes);
            }

            ClassLoaderTable.addChildAndParent(thisAndParentLoaderData);
        } else if (cmd instanceof MethodLoadedCommand) {
            MethodLoadedCommand mcmd = (MethodLoadedCommand) cmd;

            if (DEBUG) {
                System.err.println("Instrumentor.DEBUG: Method loaded command: " + mcmd.toString()); // NOI18N
            }
        }

        InstrumentMethodGroupResponse imgr = null;

        switch (status.currentInstrType) {
            case INSTR_RECURSIVE_FULL:
            case INSTR_RECURSIVE_SAMPLED:
                imgr = createFollowUpInstrumentMethodGroupResponseForCallGraph(cmd);

                break;
            case INSTR_CODE_REGION: // Follow-up can happen only if the same class is loaded with a different loader
                                    // Just in case this is say MethodInvokedFirstTimeCommand generated from the previously
                                    // active CPU instrumentation

                if (!(cmd instanceof ClassLoadedCommand)) {
                    return new InstrumentMethodGroupResponse(null);
                }

                imgr = createFollowUpInstrumentMethodGroupResponseForCodeRegion((ClassLoadedCommand) cmd);

                break;
            case INSTR_OBJECT_ALLOCATIONS:
            case INSTR_OBJECT_LIVENESS:

                // Just in case this is say MethodInvokedFirstTimeCommand generated from the previously
                // active CPU instrumentation
                if (!(cmd instanceof ClassLoadedCommand)) {
                    return new InstrumentMethodGroupResponse(null);
                }

                imgr = createFollowUpInstrumentMethodGroupResponseForMemoryProfiling((ClassLoadedCommand) cmd);

                break;
            default:
                imgr = new InstrumentMethodGroupResponse(null);
        }

        return imgr;
    }

    public synchronized InstrumentMethodGroupResponse createInitialInstrumentMethodGroupResponse(RootClassLoadedCommand cmd)
        throws ClassNotFoundException, BadLocationException {
        ClassLoaderTable.initTable(cmd.getParentLoaderIds());

        InstrumentMethodGroupResponse imgr = null;

        switch (status.currentInstrType) {
            case INSTR_RECURSIVE_FULL:
            case INSTR_RECURSIVE_SAMPLED:
                imgr = createInitialInstrumentMethodGroupResponseForCallGraph(cmd.getAllLoadedClassNames(),
                                                                              cmd.getAllLoadedClassLoaderIds(),
                                                                              cmd.getCachedClassFileBytes());

                break;
            case INSTR_CODE_REGION:
                imgr = createInitialInstrumentMethodGroupResponseForCodeRegion(cmd.getAllLoadedClassNames(),
                                                                               cmd.getAllLoadedClassLoaderIds(),
                                                                               cmd.getCachedClassFileBytes());

                break;
            case INSTR_OBJECT_ALLOCATIONS:
            case INSTR_OBJECT_LIVENESS:
                imgr = createInitialInstrumentMethodGroupResponseForMemoryProfiling(status.currentInstrType,
                                                                                    cmd.getAllLoadedClassNames(),
                                                                                    cmd.getAllLoadedClassLoaderIds(),
                                                                                    cmd.getCachedClassFileBytes());

                break;
            default:
                System.err.println(ENGINE_WARNING
                                   + "Instrumentor.createInitialInstrumentMethodGroupResponse() called with INSTR_NONE?" // NOI18N
                                   );
                System.err.println(PLEASE_REPORT_PROBLEM);
                imgr = new InstrumentMethodGroupResponse(null);

                break;
        }

        return imgr;
    }

    /**
     * This is called every time just before the target application is started or right after we attach to it.
     * It resets the internal data for loaded/instrumented classes etc.
     */
    public void resetPerVMInstanceData() {
        ClassRepository.clearCache();
    }

    private InstrumentMethodGroupResponse createFollowUpInstrumentMethodGroupResponseForCallGraph(Command cmd) {
        Object[] ret = null;

        // It may happen that if profiling is modified during intensive class loading, some class load message from
        // server may be already in the pipeline and eventually get here despite the change, and before the relevant
        // Method Scaner is initialized. This check should prevent problems caused by this inconsistency.
        if (ms == null) {
            return new InstrumentMethodGroupResponse(null);
        }

        if (cmd instanceof MethodInvokedFirstTimeCommand) {
            int id = ((MethodInvokedFirstTimeCommand) cmd).getMethodId();
            //System.out.println("--------- Received method invoked event for id = " + id + ", method = "
            // + status.instrMethodClasses[id] + "." + status.instrMethodNames[id] + status.instrMethodSignatures[id]);
            status.beginTrans(false);

            try {
                if ((id >= status.getInstrMethodClasses().length) || (status.getInstrMethodClasses()[id] == null)) {
                    // Defensive programming: this situation may happen if something went wrong with previous deinstrumentation,
                    // so some old methodEntry() call isn't removed and gets called. Avoid a crash and issue a warning instead
                    return new InstrumentMethodGroupResponse(null);
                }

                ret = ms.getMethodsToInstrumentUponMethodInvocation(status.getInstrMethodClasses()[id],
                                                                    status.getClassLoaderIds()[id],
                                                                    status.getInstrMethodNames()[id],
                                                                    status.getInstrMethodSignatures()[id]);
            } finally {
                status.endTrans();
            }
        } else if (cmd instanceof ClassLoadedCommand) {
            ClassLoadedCommand ccmd = (ClassLoadedCommand) cmd;
            //System.out.println("--------- Received class load event for class " + ccmd.getClassName());
            ret = ms.getMethodsToInstrumentUponClassLoad(ccmd.getClassName(), ccmd.getThisAndParentLoaderData()[0],
                                                         ccmd.getThreadInCallGraph());
        } else if (cmd instanceof MethodLoadedCommand) {
            MethodLoadedCommand mcmd = (MethodLoadedCommand) cmd;
            //System.out.println("--------- Recieved method load event for " + mcmd.getClassName() + "."
            // + mcmd.getMethodName() + mcmd.getMethodSignature());
            ret = ms.getMethodsToInstrumentUponReflectInvoke(mcmd.getClassName(), mcmd.getClassLoaderId(), mcmd.getMethodName(),
                                                             mcmd.getMethodSignature());
        }

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        } else {
            return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[3], (boolean[]) ret[2], 0);
        }
    }

    private InstrumentMethodGroupResponse createFollowUpInstrumentMethodGroupResponseForCodeRegion(ClassLoadedCommand cmd) {
        //System.out.println("--------- Received class load event for class " + cmd.getClassName());
        // It may happen that if profiling is modified during intensive class loading, some class load message from
        // server may be already in the pipeline and eventually get here despite the change, and before the relevant
        // Method Scaner is initialized. This check should prevent problems caused by this inconsistency.
        if (crms == null) {
            return new InstrumentMethodGroupResponse(null);
        }

        Object[] ret = crms.getFollowUpInstrumentCodeRegionResponse(cmd.getThisAndParentLoaderData()[0]);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        } else {
            return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2], null, 0);
        }
    }

    private InstrumentMethodGroupResponse createFollowUpInstrumentMethodGroupResponseForMemoryProfiling(ClassLoadedCommand cmd) {
        //System.out.println("--------- Received class load event for class " + cmd.getClassName());
        // It may happen that if profiling is modified during intensive class loading, some class load message from
        // server may be already in the pipeline and eventually get here despite the change, and before the relevant
        // Method Scaner is initialized. This check should prevent problems caused by this inconsistency.
        if (oms == null) {
            return new InstrumentMethodGroupResponse(null);
        }

        Object[] ret = oms.getMethodsToInstrumentUponClassLoad(cmd.getClassName(), cmd.getThisAndParentLoaderData()[0]);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        } else {
            int maxInstrClassId = status.getNInstrClasses();

            return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2], null, maxInstrClassId);
        }
    }

    // ------------------------------------ Transitive method closure instrumentation ------------------------------------
    private InstrumentMethodGroupResponse createInitialInstrumentMethodGroupResponseForCallGraph(String[] loadedClasses,
                                                                                                 int[] loadedClassLoaderIds,
                                                                                                 byte[][] cachedClassFileBytes) {
        //System.err.println("*** Received root class load event for class names: ");
        //for (int i = 0; i < rootClassNames.length; i++) System.err.println("  " + rootClassNames[i] + "." +
        // rootMethodNames[i] + rootMethodSignatures[i]);
        //System.err.println("*** Number of target VM loaded classes: " + loadedClasses.length);
        //System.err.println("*** Root classes are at positions:");
        //for (int i = 0; i < loadedClasses.length; i++) {
        //System.err.println(loadedClasses[i]);
        //for (int j = 0; j < rootClassNames.length; j++) {
        //  if (loadedClasses[i].equals(rootClassNames[j])) System.err.println("  " + i + " - " + rootClassNames[j]);
        //}
        //}
        Object[] ret;

        switch (settings.getInstrScheme()) {
            case INSTRSCHEME_LAZY:
                ms = new RecursiveMethodInstrumentor1(status, settings);

                break;
            case INSTRSCHEME_EAGER:
                ms = new RecursiveMethodInstrumentor2(status, settings);

                break;
            case INSTRSCHEME_TOTAL:
                ms = new RecursiveMethodInstrumentor3(status, settings);

                break;
        }

        ret = ms.getInitialMethodsToInstrument(loadedClasses, loadedClassLoaderIds, cachedClassFileBytes, rootMethods);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        } else {
            return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[3], (boolean[]) ret[2], 0);
        }
    }

    // ---------------------------------- Code region instrumentation ----------------------------------------------------
    private InstrumentMethodGroupResponse createInitialInstrumentMethodGroupResponseForCodeRegion(String[] loadedClasses,
                                                                                                  int[] loadedClassLoaderIds,
                                                                                                  byte[][] loadedClassBytes)
        throws ClassNotFoundException, BadLocationException {
        CodeRegionMethodInstrumentor.resetLoadedClassData();
        ClassManager.storeClassFileBytesForCustomLoaderClasses(loadedClasses, loadedClassLoaderIds, loadedClassBytes);

        crms = new CodeRegionMethodInstrumentor(status, savedSourceCodeSelection);

        Object[] ret = crms.getInitialInstrumentCodeRegionResponse(loadedClasses, loadedClassLoaderIds);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        }

        return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2], null, 0);
    }

    // -------------------------------------- Memory profiling instrumentation -------------------------------------------
    private InstrumentMethodGroupResponse createInitialInstrumentMethodGroupResponseForMemoryProfiling(int instrType,
                                                                                                       String[] loadedClasses,
                                                                                                       int[] loadedClassLoaderIds,
                                                                                                       byte[][] cachedClassFileBytes) {
        //System.out.println("+++++++++ Received memory profiling instrumentation initialization event of type "
        // + instrType);
        //System.out.println("+++++++++ Number of target VM loaded classes: " + loadedClasses.length);
        oms = new ObjLivenessMethodInstrumentor(status, settings, (instrType == INSTR_OBJECT_LIVENESS));

        Object[] ret = oms.getInitialMethodsToInstrument(loadedClasses, loadedClassLoaderIds, cachedClassFileBytes);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        } else {
            int maxInstrClassId = oms.getNInstantiatableClasses() + status.getNInstrClasses();

            return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2], null, maxInstrClassId);
        }
    }
}
