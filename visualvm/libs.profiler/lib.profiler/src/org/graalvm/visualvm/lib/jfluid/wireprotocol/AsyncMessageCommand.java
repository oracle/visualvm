/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.wireprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Asynchronous message that the server may occasionally send to the client.
 *
 * @author Misha Dmitriev
 */
public class AsyncMessageCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String message;
    private boolean positive;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AsyncMessageCommand(boolean positive, String message) {
        super(MESSAGE);
        this.positive = positive;

        if (message == null) {
            message = ""; // NOI18N
        }

        this.message = message;
    }

    // Custom serialization support
    AsyncMessageCommand() {
        super(MESSAGE);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getMessage() {
        return message;
    }

    public boolean isPositive() {
        return positive;
    }

    public String toString() {
        return "Async message: " + (isPositive() ? "positive" : "negative") + ", message = " + message; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        positive = in.readBoolean();
        message = in.readUTF();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(positive);
        out.writeUTF(message);
    }
}
