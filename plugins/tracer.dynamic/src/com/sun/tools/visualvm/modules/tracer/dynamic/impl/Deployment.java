/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.impl;

import com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl;
import com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl.Config;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jaroslav Bachorik
 */
public class Deployment {
    final private static String CONFIG_KEY = "deployment"; // NOI18N

    private DeployerImpl deployer;
    private DeployerImpl.Config config;

    public static Deployment forProbeConfig(FileObject cfgRoot) {
        FileObject deploymentCfg = cfgRoot.getFileObject(Deployment.CONFIG_KEY);

        DeployerImpl dpl = Deployer.forDeploymentConfig(deploymentCfg);
        DeployerImpl.Config cfg = Deployer.createConfig(dpl, deploymentCfg);

        if (dpl != null && cfg != null) {
            Deployment d = new Deployment();
            d.deployer =dpl;
            d.config = cfg;
            return d;
        }

        return null;
    }

    private Deployment() {
    }

    public Config getConfig() {
        return config;
    }

    public DeployerImpl getDeployer() {
        return deployer;
    }
}
