/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.netbeans.lib.profiler.global.Platform;


/**
 * Loading bytes for classes that are known to be loaded by system and bootstrap class loaders.
 *
 * @author Tomas Hurka
 * @author  Misha Dmitriev
 */
public class ClassBytesLoader {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Map jarCache = new HashMap();

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static byte[] getClassFileBytes(String className) {
        String resourceName = className.replace('.', '/') + ".class"; // NOI18N
        URL classUrl = ClassLoader.getSystemResource(resourceName);
        // in case the classname is a synthetic class there is no resource defining its bytecode
        if (classUrl == null) {
            if (Platform.getJDKVersionNumber() == Platform.JDK_CVM) {
                // No room on device to do this
                return null;
            }
            System.err.println("***Profiler agent warning: could not get .class file for a synthetic class " + className
                               + " in ClassBytesLoader.getClassFileBytes"); // NOI18N
            return null;
        }
        String proto = classUrl.getProtocol();

        if (proto == null) { // Should not happen, this is a critical error message
            System.err.println("***Profiler agent critical error: could not get .class file for class " + className
                               + " in ClassBytesLoader.getClassFileBytes"); // NOI18N

            return null;
        }

        //System.err.println("Reading "+classUrl);
        try {
            if (proto.equals("jar")) { // NOI18N

                return readJar(classUrl);
            } else if (proto.equals("file")) { // NOI18N

                return readFile(classUrl);
            } else {
                System.err.println("***Profiler agent critical error: Invalid URL " + classUrl); // NOI18N

                return null;
            }
        } catch (IOException ex) {
            System.err.println("*** Profiler agent critical error: caught IOException in ClassBytesLoadergetClassFileBytes: "
                               + ex); // NOI18N
            ex.printStackTrace();
            System.err.println("*** End Profiler agent critical error message ---------------------------"); // NOI18N

            return null;
        } catch (URISyntaxException ex) {
            System.err.println("*** Profiler agent critical error: caught URISyntaxException in ClassBytesLoadergetClassFileBytes: "
                               + ex); // NOI18N
            ex.printStackTrace();
            System.err.println("*** End Profiler agent critical error message ---------------------------"); // NOI18N

            return null;
        }
    }

    /** This method just initializes an experimentally determined set of classes that may be called indirectly by
     *  getClassFileBytes() above (through ClassLoader.getSystemResourceAsStream().
     */
    public static void preloadClasses() {
        getClassFileBytes("sun.misc.Launcher"); // NOI18N
        new java.io.FilePermission("*", "read"); // NOI18N

        java.util.HashMap h = new java.util.HashMap();
        h.keySet().iterator();
    }

    private static byte[] readFile(final URL classUrl)
                            throws IOException, URISyntaxException {
        URI uri = new URI(classUrl.toString());
        File file = new File(uri);
        RandomAccessFile f = new RandomAccessFile(file, "r");
        byte[] buf = new byte[(int) f.length()];

        f.readFully(buf);
        //System.err.println("Size "+buf.length);
        f.close();

        return buf;
    }

    private static byte[] readJar(final URL classUrl) throws IOException {
        String filePart = classUrl.getFile();
        int sep = filePart.lastIndexOf('!');
        String file = filePart.substring(0, sep);
        String entry = filePart.substring(sep + 2);
        ZipFile jarFile = (ZipFile) jarCache.get(file);
        InputStream is;
        ZipEntry zipEntry;
        byte[] buf;
        int pos = 0;

        if (jarFile == null) {
            jarFile = new ZipFile(new File(URI.create(file)));
            jarCache.put(file, jarFile);
        }

        zipEntry = jarFile.getEntry(entry);
        is = jarFile.getInputStream(zipEntry);
        buf = new byte[(int) zipEntry.getSize()];

        while (pos < buf.length) {
            pos += is.read(buf, pos, buf.length - pos);
        }

        is.close();

        //System.err.println("Size "+buf.length);
        return buf;
    }
}
