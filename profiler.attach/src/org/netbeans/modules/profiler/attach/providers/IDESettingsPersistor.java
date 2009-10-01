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

package org.netbeans.modules.profiler.attach.providers;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import org.netbeans.modules.profiler.utilities.queries.SettingsFolderQuery;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class IDESettingsPersistor implements SettingsPersistor {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void loadSettings() {
        Properties settings = loadProperties();
        parsePersistableSettings(settings);
    }

    public void storeSettings() {
        Properties settings = loadProperties();
        Properties newSettings = preparePersistableSettings();
        mergeProperties(newSettings, settings);

        storeProperties(settings);
    }

    protected abstract String getSettingsFileName();

    protected abstract void parsePersistableSettings(final Properties settings);

    protected abstract Properties preparePersistableSettings();

    private File getSettingsFile() {
        FileObject settingsFO = null;

        try {
            settingsFO = SettingsFolderQuery.getDefault().getSettingsFolder(true);
        } catch (IOException e) {
            // IGNORE
        }

        File settingsDir = (settingsFO == null) ? null : FileUtil.toFile(settingsFO);

        return new File(settingsDir, getSettingsFileName());
    }

    private Properties loadProperties() {
        FileInputStream fis = null;
        Properties settings = new Properties();

        try {
            fis = new FileInputStream(getSettingsFile());
            settings.load(fis);
        } catch (IOException e) {
            // IGNORE
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
                // IGNORE
            }
        }

        return settings;
    }

    private void mergeProperties(final Properties sourceProperties, final Properties destProperties) {
        Set propKeySet = sourceProperties.keySet();

        for (Iterator it = propKeySet.iterator(); it.hasNext();) {
            String key = (String) it.next();
            String value = sourceProperties.getProperty(key);
            destProperties.setProperty(key, sourceProperties.getProperty(key));
        }
    }

    private void storeProperties(final Properties properties) {
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(getSettingsFile());
            properties.store(fos, ""); // NOI18N
        } catch (IOException e) {
            // IGNORE
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                // IGNORE
            }
        }
    }
}
