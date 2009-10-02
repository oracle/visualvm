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

import java.util.Properties;
import org.netbeans.modules.profiler.actions.ModifyProfilingAction;
import org.netbeans.modules.profiler.actions.RerunAction;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.actions.CallableSystemAction;

/**
 *  This class enables actions (rerun last profiling session, modify profiling session)
 *  to react to renaming (deleting) of the profiled project.
 * @author cyhelsky
 */
class ProfilerControlPanel2Support implements FileChangeListener {

    private Properties properties;
    private String target;
    private FileObject script;

    void nullAll() {
        properties = null;
        target = null;
        unRegisterListener();
        script = null;
    }    

    boolean isActionAvailable() {
        return (this.target!=null)&&(this.script.isValid());
    }

    void setAll(FileObject script, String target, Properties properties) {
        this.script = script;
        this.target = target;
        this.properties = properties;
        registerListener();
    }

    FileObject getScript() {
        return script;
    }

    Properties getProperties() {
        return properties;
    }

    String getTarget() {
        return target;
    }

    private void registerListener() {
        if (this.script != null) {
            this.script.addFileChangeListener(FileUtil.weakFileChangeListener(this, this.script));
        }
    }

    private void unRegisterListener() {
        if (this.script!=null) {
            this.script.removeFileChangeListener(this);
        }
    }

    public void fileFolderCreated(FileEvent fe) {
    }

    public void fileDataCreated(FileEvent fe) {
    }

    public void fileChanged(FileEvent fe) {
    }

    public void fileDeleted(FileEvent fe) {
        this.nullAll();
        CallableSystemAction.get(ModifyProfilingAction.class).updateAction();
        CallableSystemAction.get(RerunAction.class).updateAction();
    }

    public void fileRenamed(FileRenameEvent fe) {
    }

    public void fileAttributeChanged(FileAttributeEvent fe) {
    }

}
