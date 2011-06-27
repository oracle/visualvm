/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.spi;

import org.netbeans.modules.profiler.api.TaskConfigurator.Configuration;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * An SPI interface for providing {@linkplain TaskConfigurator} functionality
 * @author Jaroslav Bachorik
 */
public interface TaskConfiguratorProvider {/**
     * Creates a new <i>"Attach Profiler"</i> task {@linkplain Configuration} from the given project
     * @param project The {@linkplain Project} to create the task configuration for
     * @return Returns correctly initialized task {@linkplain Configuration} instance
     */
    Configuration configureAttachProfilerTask(Lookup.Provider project);
    /**
     * Creates a new <i>"Modify Profiling"</i> task {@linkplain Configuration} from the given project
     * @param project The {@linkplain Project} to create the task configuration for
     * @param profiledFile The profiled file, if any. May be null if no single file is profiled/
     * @param isAttach Indicates whether the current profiling session is the result of <i>"Attach Profiler"</i> (<b>TRUE</b>), or <i>"Profile Project"</i> (<b>FALSE</b>)
     * @return Returns correctly initialized task {@linkplain Configuration} instance
     */
    Configuration configureModifyProfilingTask(Lookup.Provider project, FileObject profiledFile, boolean isAttach);
    /**
     * Creates a new <i>"Profile Project"</i> task {@linkplain Configuration} from the given project
     * @param project The {@linkplain Project} to create the task configuration for
     * @param profiledFile The file to profile (<i>"Profile File"</i>). May be null = <i>"Profile Project"</i>
     * @param enableOverride Indicates whether the default profiling settings derived from the project may be overriden (<b>TRUE</b>) or not (<b>FALSE</b>)
     * @return Returns correctly initialized task {@linkplain Configuration} instance
     */
    Configuration configureProfileProjectTask(Lookup.Provider project, FileObject profiledFile, boolean enableOverride);
}
