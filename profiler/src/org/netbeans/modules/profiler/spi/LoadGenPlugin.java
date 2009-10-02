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

package org.netbeans.modules.profiler.spi;

import org.netbeans.api.project.Project;
import org.netbeans.modules.profiler.spi.LoadGenPlugin.Result;
import org.openide.filesystems.FileObject;
import java.util.Collection;
import java.util.Set;


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
    Collection<FileObject> listScripts(Project project);

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
