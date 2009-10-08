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

package org.netbeans.modules.profiler.attach.providers.scripted;

import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.openide.util.RequestProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.netbeans.modules.profiler.attach.providers.AbstractIntegrationProvider;
import org.netbeans.modules.profiler.attach.providers.IntegrationCategorizer;
import org.netbeans.modules.profiler.attach.providers.ValidationResult;
import org.netbeans.modules.profiler.attach.spi.ModificationException;
import org.netbeans.modules.profiler.attach.spi.RunException;
import org.netbeans.modules.profiler.attach.wizard.steps.NullWizardStep;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class AbstractScriptIntegrationProvider extends AbstractIntegrationProvider {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String installationPath = ""; // NOI18N

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AbstractScriptIntegrationProvider() {
        super();
        this.attachedWizard = new NullWizardStep();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setInstallationPath(String path) {
        this.installationPath = path;
    }

    public String getInstallationPath() {
        return this.installationPath;
    }

    public void categorize(IntegrationCategorizer categorizer) {
        categorizer.addAppserver(this, getAttachWizardPriority());
    }

    public void modify(AttachSettings attachSettings) throws ModificationException {
        try {
            modifyScriptForAttach(attachSettings);
        } catch (ScriptModificationException e) {
            throw new ModificationException(e);
        }
    }

    public void run(AttachSettings attachSettings) throws RunException {
        if (attachSettings.isRemote()) {
            //            System.err.println(MessageFormat.format(getStartTargetUnsupportedMessage(), new Object[] {attachSettings.debug()}));
            return;
        }

        final boolean isDirectAttach = attachSettings.isDirect();

        String targetOS = attachSettings.getHostOS();

        Collection commandsArray = prepareCommands(targetOS); // prepares the appserver independent part of the startup script

        final String[] commands = (String[]) commandsArray.toArray(new String[] {  });

        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    try {
                        BufferedReader input = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(commands)
                                                                                               .getInputStream()));

                        while (input.readLine() != null) {
                            ;
                        }

                        input.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();

                        //                    System.err.println(MessageFormat.format(getStartingErrorMessage(), new Object[] {ex}));
                    }
                }
            });
    }

    public abstract ValidationResult validateInstallation(final String targetOS, final String path);

    protected abstract boolean isBackupRequired();

    protected abstract ScriptHeaderModifier getHeaderModifier(final String targetOS);

    protected abstract String getModifiedScriptPath(final String targetOS, final boolean quoted);

    protected abstract String getScriptPath(final String targetOS, final boolean quoted);

    protected abstract String getWinConsoleString();

    protected abstract void generateCommands(String targetOS, Collection commandsArray);

    protected abstract void modifyScriptFileForDirectAttach(final String hostOS, final int port, final boolean isReplaceFile,
                                                            final StringBuffer buffer);

    protected abstract void modifyScriptFileForDynamicAttach(final String hostOS, final int port, final boolean isReplaceFile,
                                                             final StringBuffer buffer);

    protected String getDefaultScriptEncoding() {
        return null;
    }

    protected final void modifyScript(final String originalScriptName, final String modifiedScriptName,
                                      final ProfilerScriptModifier modifier, final AttachSettings attachSettings)
                               throws ScriptModificationException {
        modifyScript(originalScriptName, modifiedScriptName, modifier, attachSettings, null);
    }

    protected final void modifyScript(final String originalScriptName, final String modifiedScriptName,
                                      final ProfilerScriptModifier modifier, final AttachSettings attachSettings, String encoding)
                               throws ScriptModificationException {
        String targetOS = attachSettings.getHostOS();
        String lineBreak = IntegrationUtils.getLineBreak(targetOS);
        BufferedReader br = null;

        File scriptFile = null;
        File modifiedScriptFile = null;

        try {
            scriptFile = new File(originalScriptName);
            modifiedScriptFile = new File(modifiedScriptName);

            //            /******* !!!! Provide meaningful exceptions !!!! ***********/
            //
            //            if (IntegrationUtils.isFileModifiedForProfiler(scriptFile)) {
            ////            lastErrorMessage = MessageFormat.format(FILE_MODIFIED_MSG, new Object[] {catalinaScriptFilePath, getCatalinaScriptName(targetOS, targetJVM, supportedTarget)});
            ////            return false;
            //                return;
            //            }
            //
            //            if (!IntegrationUtils.copyFile(scriptFile, modifiedScriptFile)) {
            ////            lastErrorMessage = MessageFormat.format(ERROR_COPY_FILE_MSG, new Object[] {catalinaScriptFilePath, modifiedScriptFilePath});
            ////            return false;
            //                return;
            //            }
            //
            //            /***********************************************/
            String line;

            InputStreamReader fr = null;

            if (encoding == null) {
                fr = new InputStreamReader(new FileInputStream(scriptFile));
            } else {
                fr = new InputStreamReader(new FileInputStream(scriptFile), encoding);
            }

            if (encoding == null) {
                encoding = fr.getEncoding();
            }

            br = new BufferedReader(fr);

            StringBuffer buffer = new StringBuffer();

            StringBuffer lineBuffer = new StringBuffer();

            // copy config file from disk into memory buffer
            while ((line = br.readLine()) != null) {
                lineBuffer.append(line);
                modifier.lineRead(lineBuffer); // give a chance to the custom script modifier to modify the line on-the-fly
                buffer.append(lineBuffer);
                buffer.append(lineBreak);
                lineBuffer.delete(0, lineBuffer.length());
            }

            modifier.readDone();
            br.close();

            if (modifier.needsModification() && isBackupRequired()) {
                if (!IntegrationUtils.backupFile(scriptFile)) {
                    throw new ScriptModificationException("Can't backup script"); // NOI18N
                }
            }

            modifier.modifyScript(attachSettings, lineBreak, buffer); // let the custom script modifier modify the script in batch mode

            // flush modified config file from memory buffer to disk
            Writer fw = new OutputStreamWriter(new FileOutputStream(modifiedScriptFile), "UTF-8"); // NOI18N
            fw.write(buffer.toString());
            fw.flush();
            fw.close();

            if (!IntegrationUtils.isWindowsPlatform(targetOS)) {
                Runtime.getRuntime().exec("chmod a+x " // NOI18N
                                          + modifiedScriptFile.getAbsolutePath()); //NOI18N
            }
        } catch (IOException e) {
            if (isBackupRequired()) {
                if (!IntegrationUtils.restoreFile(scriptFile)) {
                    throw new ScriptModificationException("Restore corrupted"); // NOI18N
                }
            }

            throw new ScriptModificationException(e);
        } catch (ScriptModificationException e) {
            if (isBackupRequired()) {
                if (!IntegrationUtils.restoreFile(scriptFile)) {
                    throw new ScriptModificationException("Restore corrupted", e); // NOI18N
                }
            }

            throw e; // rethrow
        } finally {
            try {
                br.close();
            } catch (Exception e) {
            }
        }
    }

    protected final void modifyScript(final String originalScriptName, final ProfilerScriptModifier modifier,
                                      final AttachSettings attachSettings)
                               throws ScriptModificationException {
        modifyScript(originalScriptName, originalScriptName, modifier, attachSettings);
    }

    private void generateCompleteCommands(String targetOS, Collection commandsArray) {
        if (IntegrationUtils.isWindowsPlatform(targetOS)) {
            commandsArray.add("cmd.exe"); // NOI18N
            commandsArray.add("/K"); // NOI18N
            commandsArray.add("start"); // NOI18N
            commandsArray.add("\"" + getWinConsoleString() + "\""); // NOI18N
        } else if (IntegrationUtils.PLATFORM_MAC_OS.equals(targetOS)) {
            Collection startCommand = new ArrayList();
            StringBuffer startCommandBuffer = new StringBuffer(256);
            Iterator commandIt = startCommand.iterator();

            generateCommands(targetOS, startCommand);
            commandIt = startCommand.iterator();

            while (commandIt.hasNext()) {
                String args = (String) commandIt.next();

                startCommandBuffer.append(args);
                startCommandBuffer.append(" "); // NOI18N
            }

            commandsArray.add("/usr/bin/osascript"); // NOI18N
            commandsArray.add("-e"); // NOI18N
            commandsArray.add("tell Application \"Terminal\""); // NOI18N
            commandsArray.add("-e"); // NOI18N

            commandsArray.add("do script \"" + startCommandBuffer + "\""); // NOI18N
            commandsArray.add("-e"); // NOI18N
            commandsArray.add("end tell"); // NOI18N

            return;
        } else {
            commandsArray.add("xterm"); // NOI18N
            commandsArray.add("-sb"); // NOI18N
            commandsArray.add("-sl"); // NOI18N
            commandsArray.add("1000"); // NOI18N
            commandsArray.add("-e"); // NOI18N
        }

        generateCommands(targetOS, commandsArray);
    }

    private void modifyScriptForAttach(final AttachSettings attachSettings)
                                throws ScriptModificationException {
        final String targetOS = attachSettings.getHostOS();

        if (attachSettings.isDirect()) {
            modifyScript(getScriptPath(targetOS, false), getModifiedScriptPath(targetOS, false),
                         new ProfilerScriptModifier(getHeaderModifier(targetOS)) {
                    public void onModification(final AttachSettings attachSettings, final String lineBreak,
                                               StringBuffer scriptBuffer)
                                        throws ScriptModificationException {
                        modifyScriptFileForDirectAttach(targetOS, attachSettings.getPort(), false, scriptBuffer);
                    }

                    public void onLineRead(final StringBuffer line) {
                    }
                }, attachSettings, getDefaultScriptEncoding());
        } else {
            modifyScript(getScriptPath(targetOS, false), getModifiedScriptPath(targetOS, false),
                         new ProfilerScriptModifier(getHeaderModifier(targetOS)) {
                    public void onModification(final AttachSettings attachSettings, final String lineBreak,
                                               StringBuffer scriptBuffer)
                                        throws ScriptModificationException {
                        modifyScriptFileForDynamicAttach(targetOS, attachSettings.getPort(), false, scriptBuffer);
                    }

                    public void onLineRead(final StringBuffer line) {
                    }
                }, attachSettings, getDefaultScriptEncoding());
        }
    }

    private Collection prepareCommands(String targetOS) {
        Collection commandsArray = new ArrayList(15);
        generateCompleteCommands(targetOS, commandsArray);

        return commandsArray;
    }
}
