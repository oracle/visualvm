/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer;

/**
 * Signals that a TracerPackage/TracerProbe failed to initialize for a new
 * Tracer session. Preferably provides a message to notify the user about
 * the failure.
 *
 * @author Jiri Sedlacek
 */
public final class SessionInitializationException extends Exception {

    private final String userMessage;


    /**
     * Creates a new instance of SessionInitializationException with defined
     * log message and default user message.
     *
     * @param logMessage log message
     */
    public SessionInitializationException(String logMessage) {
        this(null, logMessage);
    }

    /**
     * Creates a new instance of SessionInitializationException with defined
     * log message and cause and default user message.
     *
     * @param logMessage log message
     * @param cause exception cause
     */
    public SessionInitializationException(String logMessage,
                                          Throwable cause) {
        this(null, logMessage, cause);
    }

    /**
     * Creates a new instance of SessionInitializationException with defined
     * user message and log message.
     *
     * @param userMessage user message
     * @param logMessage log message
     */
    public SessionInitializationException(String userMessage,
                                          String logMessage) {
        super(logMessage);
        this.userMessage = userMessage;
    }

    /**
     * Creates a new instance of SessionInitializationException with defined
     * user message, log message and cause.
     *
     * @param userMessage user message
     * @param logMessage log message
     * @param cause exception cause
     */
    public SessionInitializationException(String userMessage,
                                          String logMessage,
                                          Throwable cause) {
        super(logMessage, cause);
        this.userMessage = userMessage;
    }


    /**
     * Returns an user message to be displayed in Tracer UI. The message should
     * be short, for example "Probe XYZ failed to initialize" or "Probe XYZ
     * failed to connect to target application."
     *
     * @return user message to be displayed in Tracer UI
     */
    public String getUserMessage() {
        return userMessage;
    }

}
