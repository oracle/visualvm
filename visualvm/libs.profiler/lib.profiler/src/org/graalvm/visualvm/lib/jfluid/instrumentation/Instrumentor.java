/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.classfile.BaseClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassLoaderTable;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassRepository;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils.SourceCodeSelection;
import org.graalvm.visualvm.lib.jfluid.client.RuntimeProfilingPoint;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.ClassLoadedCommand;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.Command;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.InstrumentMethodGroupCommand;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.InstrumentMethodGroupResponse;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.MethodInvokedFirstTimeCommand;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.MethodLoadedCommand;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.RootClassLoadedCommand;


/**
 * A high-level interface to all method instrumentation operations.
 * <p>
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
    private static final boolean DEBUG = System.getProperty("org.graalvm.visualvm.lib.jfluid.instrumentation.Instrumentor") != null; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CodeRegionMethodInstrumentor crms;
    private MemoryProfMethodInstrumentor oms;
    private ProfilerEngineSettings settings;
    private ProfilingSessionStatus status;
    private RecursiveMethodInstrumentor ms;
    private RootMethods rootMethods;
    private ClassRepository classRepo;

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
     * @param repo     ClassRepository to access profiled classes
     */
    public Instrumentor(ClassRepository repo, ProfilingSessionStatus status, ProfilerEngineSettings settings) {
        this.status = status;
        this.settings = settings;
        classRepo = repo;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getClassId(String className, int classLoaderId) {
        BaseClassInfo clazz;
        InstrumentationFilter filter = settings.getInstrumentationFilter();

        if (className.charAt(0) == '[') { // array , need special lookup
                                          // strip L and ; from className, see ClassFileParser.classNameAtCPIndex

            if (className.endsWith(";")) {
                int elIndex = className.indexOf('L');
                className = new StringBuffer(className).deleteCharAt(className.length() - 1).deleteCharAt(elIndex).toString();
            }
            String filterName = StringUtils.userFormClassName(className);
            if (!filter.passes(filterName.replace('.', '/'))) { // NOI18N
                return -1;
            }

            clazz = classRepo.lookupSpecialClass(className);
            if (clazz.getInstrClassId() == -1) {
                clazz.setInstrClassId(oms.getNextClassId(className));
            }
        } else {
            if (!filter.passes(className.replace('.', '/'))) { // NOI18N
                return -1;
            }
            clazz = classRepo.lookupClassOrCreatePlaceholder(className, classLoaderId);
        }

        if (clazz == null) {
            System.err.println("Warning: could not find class " + className + " loaded by the VM on the class path");

            // warning already issued in ClassRepository.lookupClass method, no need to do it again
            return -1;
        }

        if (clazz.getInstrClassId() == -1) {
            System.err.println("Warning: " + clazz.getNameAndLoader() + " does not have instrClassId");            
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

        for (RuntimeProfilingPoint pp : pps) {
            String className = pp.getClassName();

            if (!rootClassNames.contains(className)) {
                rootClassNames.add(className);
            }
        }

        if (rootClassNames == null) {
            return null;
        }

        return (String[]) rootClassNames.toArray(new String[0]);
    }

    public void setSavedSourceCodeSelection(SourceCodeSelection[] s) {
        savedSourceCodeSelection = s[0];
    }

    public void setStatusInfoFromSourceCodeSelection(SourceCodeSelection[] s)
                                              throws ClassNotFoundException, BadLocationException, IOException, ClassFormatError {
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
                ret = (new MiscInstrumentationOps(classRepo, status)).getOrigCodeForAllInstrumentedMethods();

                break;
            case INSTR_CODE_REGION:
                ret = (new MiscInstrumentationOps(classRepo, status)).getOrigCodeForSingleInstrumentedMethod(rootMethods);

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
                classRepo.addVMSuppliedClassFile(clcmd.getClassName().replace('.','/'), thisAndParentLoaderData[0], classFileBytes);
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
                imgr = createInitialInstrumentMethodGroupResponseForCallGraph(cmd);

                break;
            case INSTR_CODE_REGION:
                imgr = createInitialInstrumentMethodGroupResponseForCodeRegion(cmd);

                break;
            case INSTR_OBJECT_ALLOCATIONS:
            case INSTR_OBJECT_LIVENESS:
                imgr = createInitialInstrumentMethodGroupResponseForMemoryProfiling(status.currentInstrType, cmd);

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
        classRepo.clearCache();
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
    private InstrumentMethodGroupResponse createInitialInstrumentMethodGroupResponseForCallGraph(RootClassLoadedCommand rootLoaded) {
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
                ms = new RecursiveMethodInstrumentor1(classRepo, status, settings);

                break;
            case INSTRSCHEME_EAGER:
                ms = new RecursiveMethodInstrumentor2(classRepo, status, settings);

                break;
            case INSTRSCHEME_TOTAL:
                ms = new RecursiveMethodInstrumentor3(classRepo, status, settings);

                break;
        }

        ret = ms.getInitialMethodsToInstrument(rootLoaded, rootMethods);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        } else {
            return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[3], (boolean[]) ret[2], 0);
        }
    }

    // ---------------------------------- Code region instrumentation ----------------------------------------------------
    private InstrumentMethodGroupResponse createInitialInstrumentMethodGroupResponseForCodeRegion(RootClassLoadedCommand rootLoaded)
        throws ClassNotFoundException, BadLocationException {
        classRepo.clearCache();

        crms = new CodeRegionMethodInstrumentor(classRepo, status, savedSourceCodeSelection);

        Object[] ret = crms.getInitialInstrumentCodeRegionResponse(rootLoaded);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        }

        return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2], null, 0);
    }

    // -------------------------------------- Memory profiling instrumentation -------------------------------------------
    private InstrumentMethodGroupResponse createInitialInstrumentMethodGroupResponseForMemoryProfiling(int instrType,
                                                                         RootClassLoadedCommand rootLoaded) {
        //System.out.println("+++++++++ Received memory profiling instrumentation initialization event of type "
        // + instrType);
        //System.out.println("+++++++++ Number of target VM loaded classes: " + loadedClasses.length);
        oms = new ObjLivenessMethodInstrumentor(classRepo, status, settings, (instrType == INSTR_OBJECT_LIVENESS));

        Object[] ret = oms.getInitialMethodsToInstrument(rootLoaded);

        if (ret == null) {
            return new InstrumentMethodGroupResponse(null);
        } else {
            int maxInstrClassId = oms.getNInstantiatableClasses() + status.getNInstrClasses();

            return new InstrumentMethodGroupResponse((String[]) ret[0], (int[]) ret[1], (byte[][]) ret[2], null, maxInstrClassId);
        }
    }
}
