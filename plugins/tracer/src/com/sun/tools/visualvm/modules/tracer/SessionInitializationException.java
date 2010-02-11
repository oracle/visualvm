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

package com.sun.tools.visualvm.modules.tracer;

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
