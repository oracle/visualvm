/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.classfile;

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
    
    public LazyDynamicClassInfo(String className, int loaderId, String classFileLocation,
            String superClassName, String[] interfaceNames) throws IOException {
        super(className, loaderId, classFileLocation, false);
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
            ClassFileCache.getDefault().preloadBytecode(getName(), getClassFileLocation());
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
