/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.jfluid.global;

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
    private static final String CALIBRATION_FILE_NOT_EXIST_MSG;
    private static final String CALIBRATION_FILE_NOT_READABLE_MSG;
    private static final String CALIBRATION_DATA_CORRUPTED_PREFIX;
    private static final String SHORTER_THAN_EXPECTED_STRING;
    private static final String ORIGINAL_MESSAGE_STRING;
    private static final String RERUN_CALIBRATION_MSG;
    private static final String ERROR_WRITING_CALIBRATION_FILE_PREFIX;
    private static final String REEXECUTE_CALIBRATION_MSG;
                                                                                                                                 // -----
    private static String errorMessage;

    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.global.Bundle"); // NOI18N
        CALIBRATION_FILE_NOT_EXIST_MSG = messages.getString("CalibrationDataFileIO_CalibrationFileNotExistMsg"); // NOI18N
        CALIBRATION_FILE_NOT_READABLE_MSG = messages.getString("CalibrationDataFileIO_CalibrationFileNotReadableMsg"); // NOI18N
        CALIBRATION_DATA_CORRUPTED_PREFIX = messages.getString("CalibrationDataFileIO_CalibrationDataCorruptedPrefix"); // NOI18N
        SHORTER_THAN_EXPECTED_STRING = messages.getString("CalibrationDataFileIO_ShorterThanExpectedString"); // NOI18N
        ORIGINAL_MESSAGE_STRING = messages.getString("CalibrationDataFileIO_OriginalMessageString"); // NOI18N
        RERUN_CALIBRATION_MSG = messages.getString("CalibrationDataFileIO_ReRunCalibrationMsg"); // NOI18N
        ERROR_WRITING_CALIBRATION_FILE_PREFIX = messages.getString("CalibrationDataFileIO_ErrorWritingCalibrationFilePrefix"); // NOI18N
        REEXECUTE_CALIBRATION_MSG = messages.getString("CalibrationDataFileIO_ReExecuteCalibrationMsg"); // NOI18N
    }
    
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
            errorMessage = MessageFormat.format(CALIBRATION_FILE_NOT_EXIST_MSG, new Object[] { savedDataFile.toString() });

            return 1;
        }

        if (!savedDataFile.canRead()) {
            errorMessage = MessageFormat.format(CALIBRATION_FILE_NOT_READABLE_MSG, new Object[] { savedDataFile.toString() });

            return -1;
        }

        FileInputStream fiStream = null;
        try {
            fiStream = new FileInputStream(savedDataFile);
            ObjectInputStream oiStream = new ObjectInputStream(fiStream);

            status.methodEntryExitCallTime = (double[]) oiStream.readObject();
            status.methodEntryExitInnerTime = (double[]) oiStream.readObject();
            status.methodEntryExitOuterTime = (double[]) oiStream.readObject();
            status.timerCountsInSecond = (long[]) oiStream.readObject();

            fiStream.close();
        } catch (Exception e) {
            errorMessage = e.getMessage();
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
        } finally {
            if (fiStream != null) {
                try {
                    fiStream.close();
                } catch (IOException e) {}
            }
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
            errorMessage = e.getMessage();
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
            if (!CommonConstants.JDK_UNSUPPORTED_STRING.equals(javaVersionString)
                   && !CommonConstants.JDK_CVM_STRING.equals(javaVersionString)) {
                if (new File(javaExecutable).exists()) {
                    return true;
                }
            }
        }

        return false;
    }

    public static String getCalibrationDataFileName(String targetJDKVerString)
                                              throws IOException {
        String fileName = "machinedata" + "." + targetJDKVerString; // NOI18N

        return Platform.getProfilerUserDir() + File.separator + fileName;
    }
}
