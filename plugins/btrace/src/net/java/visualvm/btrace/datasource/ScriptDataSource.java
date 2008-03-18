/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package net.java.visualvm.btrace.datasource;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import java.io.FileInputStream;
import java.io.Reader;
import net.java.visualvm.btrace.config.ProbeConfig;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ScriptDataSource extends DataSource {
    private ProbeConfig config;
    private Application application;
    private ScriptDataSourceProvider provider;
    private DeployTask task;
    
    public ScriptDataSource(ScriptDataSourceProvider provider, ProbeConfig config, Application master, DeployTask deployTask) {
        super(master);
        this.config = config;
        this.application = master;
        this.provider = provider;
        this.task = deployTask;
        FileInputStream ff;

    }

    public Application getApplication() {
        return application;
    }
    
    public void stop() {
        task.stop();
    }

    @Override
    public String toString() {
        return config.toString();
    }
    
    public ProbeConfig getConfig() {
        return config;
    }
    
    public Reader getReader() {
        return task.getReader();
    }
    
    public void shutdown() {
        // do cleanup here
    }
}
