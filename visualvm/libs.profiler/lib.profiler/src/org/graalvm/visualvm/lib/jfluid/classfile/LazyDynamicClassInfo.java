/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.classfile;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A representation of a binary Java class, which bytecode is loaded lazily.
 * Superclass and interfaces need to be specified in constructor, since they are
 * needed very early. Without it bytecode will be read for all classes and there
 * will be no laziness.
 *
 * @author Tomas Hurka
 */
public class LazyDynamicClassInfo extends DynamicClassInfo {
    private boolean isInitilaized;
    private boolean isInterface;

    public LazyDynamicClassInfo(ClassPath cp, String className, int loaderId, String classFileLocation,
            String superClassName, String[] interfaceNames) throws IOException {
        super(cp, className, loaderId, classFileLocation, false);
        superName = superClassName;
        interfaces = interfaceNames;
    }

    public int getMethodIndex(String name, String sig) {
        if (initializeClassFile()) {
            return super.getMethodIndex(name, sig);
        }
        return -1;
    }

    public String[] getMethodNames() {
        if (initializeClassFile()) {
            return super.getMethodNames();
        }
        return new String[0];
    }

    public void preloadBytecode() {
        super.preloadBytecode();
        if (!isInitilaized) {
            classPath.preloadBytecode(getName(), getClassFileLocation());
        }
    }

    public boolean isInterface() {
        if (!isInitilaized) {
            return isInterface;
        }
        return super.isInterface();
    }

    public void setInterface() {
        isInterface = true;
    }

    private boolean initializeClassFile() {
        if (!isInitilaized) {
            isInitilaized = true;
            try {
                parseClassFile(getName());
                return true;
            } catch (ClassFormatError ex) {
                Logger.getLogger(LazyDynamicClassInfo.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(LazyDynamicClassInfo.class.getName()).log(Level.INFO, null, ex);
            }
            return false;
        }
        return true;
    }
}
