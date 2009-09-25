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

package org.netbeans.lib.profiler.wireprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Request from the client to the back end to initiate TA instrumentation of the given type.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 * @author Ian Formanek
 */
public class InitiateInstrumentationCommand extends Command {

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String[] classNames;
    private String[] profilingPointHandlers;
    private int[] profilingPointIDs;
    private String[] profilingPointInfos;
    private boolean instrSpawnedThreads;
    private boolean startProfilingPointsActive;
    private int instrType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public InitiateInstrumentationCommand(int instrType, String[] classNames,
                                          int[] ppIDs, String[] ppHandlers, String[] ppInfos,
                                          boolean instrSpawnedThreads, boolean startProfilingPointsActive) {
        super(INITIATE_INSTRUMENTATION);
        if ((classNames == null)) {
            classNames = new String[] { " " }; // NOI18N
        } else if (classNames[0] == null) {
            classNames[0] = " "; // NOI18N
        }

        this.instrType = instrType;
        this.classNames = classNames;
        profilingPointIDs = ppIDs;
        profilingPointHandlers = ppHandlers;
        profilingPointInfos = ppInfos;
        this.instrSpawnedThreads = instrSpawnedThreads;
        this.startProfilingPointsActive = startProfilingPointsActive;
    }
    
    /** Legacy support for single root instrumentation */
    public InitiateInstrumentationCommand(int instrType, String className, boolean instrSpawnedThreads,
                                          boolean startProfilingPointsActive) {
        this(instrType,
             className==null ? new String[]{" "} : new String[]{className},
             null,null,null,
             instrSpawnedThreads,startProfilingPointsActive);
    }


    /** This is a special method only called to setup the connection in ProfilerClient.connectToServer() - see comments there */
    public InitiateInstrumentationCommand(int instrType, String className) {
        this(instrType,className,false,false);
    }

    // Custom serialzation support
    InitiateInstrumentationCommand() {
        super(INITIATE_INSTRUMENTATION);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean getInstrSpawnedThreads() {
        return instrSpawnedThreads;
    }

    public void setInstrType(int t) {
        instrType = t;
    }

    public int getInstrType() {
        return instrType;
    }

    public String[] getProfilingPointHandlers() {
        return profilingPointHandlers;
    }

    public int[] getProfilingPointIDs() {
        return profilingPointIDs;
    }

    public String[] getProfilingPointInfos() {
        return profilingPointInfos;
    }

    public String getRootClassName() {
        return classNames[0];
    } // Legacy support for one root

    public String[] getRootClassNames() {
        return classNames;
    }

    public boolean isStartProfilingPointsActive() {
        return startProfilingPointsActive;
    }

    // for debugging
    public String toString() {
        return super.toString() + ", instrType = " + instrType; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        instrType = in.readInt();

        int len = in.readInt();
        classNames = new String[len];

        for (int i = 0; i < len; i++) {
            classNames[i] = in.readUTF().intern(); // Interning is important, since checks are through '=='
        }

        instrSpawnedThreads = in.readBoolean();
        startProfilingPointsActive = in.readBoolean();

        try {
            profilingPointIDs = (int[]) in.readObject();
            profilingPointHandlers = (String[]) in.readObject();
            profilingPointInfos = (String[]) in.readObject();
        } catch (ClassNotFoundException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(instrType);
        out.writeInt(classNames.length);

        for (int i = 0; i < classNames.length; i++) {
            out.writeUTF(classNames[i]);
        }

        out.writeBoolean(instrSpawnedThreads);
        out.writeBoolean(startProfilingPointsActive);
        out.writeObject(profilingPointIDs);
        out.writeObject(profilingPointHandlers);
        out.writeObject(profilingPointInfos);
    }
}
