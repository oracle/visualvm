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

package org.graalvm.visualvm.lib.profiler.spi;

import org.graalvm.visualvm.lib.profiler.spi.LoadGenPlugin.Result;
import org.openide.filesystems.FileObject;
import java.util.Collection;
import java.util.Set;
import org.openide.util.Lookup;


/**
 * Defines an interface for accessing Load Generator features from the profiler UI
 * @author Jaroslav Bachorik
 */
public interface LoadGenPlugin {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Callback {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        /**
         * Null-object implementation
         */
        public static final Callback NULL = new Callback() {
            public void afterStart(Result result) {
                // do nothing
            }

            public void afterStop(Result result) {
                // do nothing
            }
        };


        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * Called after the start() method has been finished
         * @param result Holds the result of the start() method
         */
        void afterStart(Result result);

        /**
         * Called after the stop() method has been finished
         * @param result Holds the result of the stop() method
         */
        void afterStop(Result result);
    }

    //~ Enumerations -------------------------------------------------------------------------------------------------------------

    public static enum Result {//~ Enumeration constant initializers ------------------------------------------------------------------------------------

        FAIL, SUCCESS, TIMEOUT;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the load generator status
     */
    boolean isRunning();

    /**
     * Retrieves the set of all supported file extensions; depends on the loadgenerator implementations installed in the system
     */
    Set<String> getSupportedExtensions();

    /**
     * Lists all supported loadgen scripts contained in the given project
     * @param project The project to search for scripts
     * @return Returns a list of FileObject instances representing loadgen scripts
     */
    Collection<FileObject> listScripts(Lookup.Provider project);

    /**
     * Runs a given loadgen script
     * @param scriptPath The path to the script to be run
     * @param callback Callback to be called upon finishing the start method; must not be null - us <code>Callback.NULL</code> instead
     */
    void start(String scriptPath, Callback callback);

    /**
     * Stops the last successfuly started load generator process
     */
    void stop();

    /**
     * Stops a load generator process givent the script path that started it
     * @param scriptPath The path to the loadgen script
     */
    void stop(String scriptPath);
}
