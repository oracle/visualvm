/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
