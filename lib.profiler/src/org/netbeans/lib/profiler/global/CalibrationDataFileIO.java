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

package org.netbeans.lib.profiler.global;

import java.io.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * Reading and saving calibration data file.
 *
 * @author  Misha Dmitriev
 */
public class CalibrationDataFileIO {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.global.Bundle"); // NOI18N
    private static final String CALIBRATION_FILE_NOT_EXIST_MSG = messages.getString("CalibrationDataFileIO_CalibrationFileNotExistMsg"); // NOI18N
    private static final String CALIBRATION_FILE_NOT_READABLE_MSG = messages.getString("CalibrationDataFileIO_CalibrationFileNotReadableMsg"); // NOI18N
    private static final String CALIBRATION_DATA_CORRUPTED_PREFIX = messages.getString("CalibrationDataFileIO_CalibrationDataCorruptedPrefix"); // NOI18N
    private static final String SHORTER_THAN_EXPECTED_STRING = messages.getString("CalibrationDataFileIO_ShorterThanExpectedString"); // NOI18N
    private static final String ORIGINAL_MESSAGE_STRING = messages.getString("CalibrationDataFileIO_OriginalMessageString"); // NOI18N
    private static final String RERUN_CALIBRATION_MSG = messages.getString("CalibrationDataFileIO_ReRunCalibrationMsg"); // NOI18N
    private static final String ERROR_WRITING_CALIBRATION_FILE_PREFIX = messages.getString("CalibrationDataFileIO_ErrorWritingCalibrationFilePrefix"); // NOI18N
    private static final String REEXECUTE_CALIBRATION_MSG = messages.getString("CalibrationDataFileIO_ReExecuteCalibrationMsg"); // NOI18N
                                                                                                                                 // -----
    private static String errorMessage;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getErrorMessage() {
        String res = errorMessage;
        errorMessage = null;

        return res;
    }

    /**
     * Reads the saved calibration data file.
     * Returns -1 in case of a fatal error (cannot read the calibration file), -2 if file exists but data is corrupted,
     * 1 if file does not exist, and 0 if file has been read successfully.
     */
    public static int readSavedCalibrationData(ProfilingSessionStatus status) {
        String fn = null;

        try {
            fn = getCalibrationDataFileName(status.targetJDKVersionString);
        } catch (IOException e) {
            errorMessage = e.getMessage();

            return -1;
        }

        File savedDataFile = new File(fn);

        if (!savedDataFile.exists()) {
            errorMessage = CALIBRATION_FILE_NOT_EXIST_MSG;

            return 1;
        }

        if (!savedDataFile.canRead()) {
            errorMessage = MessageFormat.format(CALIBRATION_FILE_NOT_READABLE_MSG, new Object[] { savedDataFile.toString() });

            return -1;
        }

        try {
            FileInputStream fiStream = new FileInputStream(savedDataFile);
            ObjectInputStream oiStream = new ObjectInputStream(fiStream);

            status.methodEntryExitCallTime = (double[]) oiStream.readObject();
            status.methodEntryExitInnerTime = (double[]) oiStream.readObject();
            status.methodEntryExitOuterTime = (double[]) oiStream.readObject();
            status.timerCountsInSecond = (long[]) oiStream.readObject();

            fiStream.close();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String prefix = CALIBRATION_DATA_CORRUPTED_PREFIX;

            if (errorMessage == null) {
                if (e instanceof java.io.EOFException) {
                    errorMessage = prefix + " " + SHORTER_THAN_EXPECTED_STRING; // NOI18N
                }
            } else {
                errorMessage = prefix + "\n" + ORIGINAL_MESSAGE_STRING + " " + errorMessage; // NOI18N
            }

            errorMessage += ("\n" + RERUN_CALIBRATION_MSG + "\n"); // NOI18N

            return -2;
        }

        return 0;
    }

    public static boolean saveCalibrationData(ProfilingSessionStatus status) {
        try {
            FileOutputStream foStream = new FileOutputStream(getCalibrationDataFileName(status.targetJDKVersionString));
            ObjectOutputStream ooStream = new ObjectOutputStream(foStream);

            ooStream.writeObject(status.methodEntryExitCallTime);
            ooStream.writeObject(status.methodEntryExitInnerTime);
            ooStream.writeObject(status.methodEntryExitOuterTime);
            ooStream.writeObject(status.timerCountsInSecond);

            foStream.close();
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            String prefix = ERROR_WRITING_CALIBRATION_FILE_PREFIX;
            errorMessage = prefix + "\n" + ORIGINAL_MESSAGE_STRING + "\n" + errorMessage; // NOI18N
                                                                                          // status.remoteProfiling below means that we actually perform off-line calibration on the remote target machine.
                                                                                          // In that case, the message that follows, which is meaningful in case of local machine calibration, doesn't make sense.

            if (!status.remoteProfiling) {
                errorMessage += ("\n" + REEXECUTE_CALIBRATION_MSG + "\n"); // NOI18N
            }

            return false;
        }

        return true;
    }

    public static boolean validateCalibrationInput(String javaVersionString, String javaExecutable) {
        if ((javaVersionString != null) && (javaExecutable != null)) {
            if (CommonConstants.JDK_15_STRING.equals(javaVersionString)
                    || CommonConstants.JDK_16_STRING.equals(javaVersionString)
                    || CommonConstants.JDK_17_STRING.equals(javaVersionString)) {
                if (new File(javaExecutable).exists()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static String getCalibrationDataFileName(String targetJDKVerString)
                                              throws IOException {
        String fileName = "machinedata" + "." + targetJDKVerString; // NOI18N

        return Platform.getProfilerUserDir() + File.separator + fileName;
    }
}
