/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.impl;

import com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl;
import java.io.IOException;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jaroslav Bachorik
 */
public class Deployer {
    final private static String CONFIG_KEY = "deployer.shadow"; // NOI18N

    public static DeployerImpl forDeploymentConfig(FileObject cfgRoot) {
        FileObject deployerCfg = cfgRoot.getFileObject(Deployer.CONFIG_KEY);

        if (deployerCfg == null) return null;
        
        Object dObj = null;
        try {
            // handle the shadow
            String origFilePath = (String)deployerCfg.getAttribute("originalFile"); // NOI18N
            if (origFilePath != null) {
                FileObject origFile = deployerCfg.getFileSystem().getRoot().getFileObject(origFilePath);
                if (origFile != null) {
                    dObj = origFile.getAttribute("instance"); // NOI18N
                }
            }
        } catch (IOException e) {
            dObj = null;
        }
        if (dObj == null) {
            dObj = deployerCfg.getAttribute("instance"); // NOI18N
        }
        if (dObj != null && dObj instanceof DeployerImpl) {
            return (DeployerImpl)dObj;
        }
        return null;
    }

    public static DeployerImpl.Config createConfig(DeployerImpl deployer, FileObject deploymentCfg) {
        FileObject deployerCfg = deploymentCfg.getFileObject(Deployer.CONFIG_KEY);
        
        return deployerCfg != null ? deployer.configFor(deployerCfg) : null;
    }
}
