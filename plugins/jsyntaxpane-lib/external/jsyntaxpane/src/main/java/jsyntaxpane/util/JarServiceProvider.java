/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License 
 *       at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 */
package jsyntaxpane.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author subwiz
 */
public class JarServiceProvider {

    private static final Logger LOG = Logger.getLogger(JarServiceProvider.class.getName());

    private static ClassLoader globalLoader = ClassLoader.getSystemClassLoader();
    /**
     * Prevent anyone from instantiating this class.  
     * Just use the static method
     */
    private JarServiceProvider() {
    }

    public static Class loadClass(String className) throws ClassNotFoundException {
        return globalLoader.loadClass(className);
    }

    /**
     * Return an Object array from the file in META-INF/resources/{classname}
     * @param cls
     * @return
     * @throws java.io.IOException
     */
    public static List<Object> getServiceProviders(Class cls) throws IOException {
        ArrayList<Object> l = new ArrayList<Object>();
        if (globalLoader != null) {
            String serviceFile = "META-INF/services/" + cls.getName();
            Enumeration<URL> e = globalLoader.getResources(serviceFile);
            while (e.hasMoreElements()) {
                URL u = e.nextElement();
                InputStream is = u.openStream();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(
                            new InputStreamReader(is, Charset.forName("UTF-8")));
                    String str = null;
                    while ((str = br.readLine()) != null) {
                        int commentStartIdx = str.indexOf("#");
                        if (commentStartIdx != -1) {
                            str = str.substring(0, commentStartIdx);
                        }
                        str = str.trim();
                        if (str.length() == 0) {
                            continue;
                        }
                        try {
                            Object obj = globalLoader.loadClass(str).newInstance();
                            l.add(obj);
                        } catch (Exception ex) {
                            LOG.warning("Could not load: " + str);
                            LOG.warning(ex.getMessage());
                        }
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            }
        }
        return l;
    }

    /**
     * Read a file in the META-INF/services location.  File name will be
     * fully qualified classname, in all lower-case, appended with ".properties"
     * If no file is found, then a an empty Property instance will be returned
     * @param clazz
     * @return Property file read.
     */
    public static Properties readProperties(Class clazz) {
        return readProperties(clazz.getName());
    }

    /**
     * Read a file in the META-INF/services named name appended with 
     * ".properties"
     * If no file is found, then a an empty Property instance will be returned
     * @param name name of file (use dots to separate subfolders).
     * @return Property file read.
     */
    public static Properties readProperties(String name) {
        Properties props = new Properties();
        if (globalLoader != null) {
            InputStream is = null;
            try {
                String serviceFile = "META-INF/services/" +
                        name.toLowerCase() + ".properties";
                Enumeration<URL> locs = globalLoader.getResources(serviceFile);
                while(locs.hasMoreElements()) {
                    URL loc = locs.nextElement();
                    if (loc != null) {
                        try {
                            is = loc.openStream();
                            Properties p = new Properties();
                            p.load(is);
                            props.putAll(p);
                        } finally {
                            if (is != null) {
                                is.close();
                            }
                        }

                    }
                }
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            } finally {
                try {
                    // maybe the is was not open coz we did not find the file
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
        return props;
    }

    /**
     * Read a file in the META-INF/services named name appended with 
     * ".properties", and returns it as a <code>Map<String, String></code>
     * If no file is found, then a an empty Property instance will be returned
     * @param name name of file (use dots to separate subfolders).
     * @return Map of keys and values
     */
    public static Map<String, String> readStringsMap(String name) {
        Properties props = readProperties(name);
        HashMap<String, String> map = new HashMap<String, String>();
        if (props != null) {
            for (Map.Entry e : props.entrySet()) {
                map.put(e.getKey().toString(), e.getValue().toString());
            }
        }
        return map;
    }

    public static void setGlobalLoader(ClassLoader loader) {
        globalLoader = loader;
    }
}
