/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.profiler.heapwalk.details.jdk;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class NetDetailsProvider extends DetailsProvider.Basic {
    
    private static final String URL_MASK = "java.net.URL";                          // NOI18N
    private static final String INET4_ADDRESS_MASK = "java.net.Inet4Address";       // NOI18N
    private static final String INET6_ADDRESS_MASK = "java.net.Inet6Address";       // NOI18N
    private static final String NETWORK_IF_MASK = "java.net.NetworkInterface";      // NOI18N
    private static final String IF_ADDRESS_MASK = "java.net.InterfaceAddress+";     // NOI18N
    private static final String URL_CONN_MASK = "java.net.URLConnection+";          // NOI18N
    private static final String URI_MASK = "java.net.URI";                          // NOI18N
    private static final String HTTP_COOKIE_MASK = "java.net.HttpCookie";           // NOI18N
    private static final String INET_SOCKET_ADDRERSS = "java.net.InetSocketAddress+"; // NOI18N           // NOI18N
    
    public NetDetailsProvider() {
        super(URL_MASK, INET4_ADDRESS_MASK, INET6_ADDRESS_MASK, NETWORK_IF_MASK,
              IF_ADDRESS_MASK, URL_CONN_MASK, URI_MASK, HTTP_COOKIE_MASK,
              INET_SOCKET_ADDRERSS);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (URL_MASK.equals(className)) {                                           // URL
            String file = DetailsUtils.getInstanceFieldString(instance, "file", heap); // NOI18N
            String host = DetailsUtils.getInstanceFieldString(instance, "host", heap);  // NOI18N
            String protocol = DetailsUtils.getInstanceFieldString(instance, "protocol", heap);  // NOI18N
            int port = DetailsUtils.getIntFieldValue(instance, "port", -1); // NOI18N
            if (file != null && protocol != null) {
                try {
                    return new URL(protocol,host,port,file).toExternalForm();
                } catch (MalformedURLException ex) {
                    
                }
            }
            // fallback
            return DetailsUtils.getInstanceFieldString(instance, "path", heap); // NOI18N;
        } else if (INET4_ADDRESS_MASK.equals(className) ||                          // Inet4Address
                   INET6_ADDRESS_MASK.equals(className)) {                          // Inet6Address
            String host;
            Instance holder = (Instance)instance.getValueOfField("holder");                     // NOI18N
            if (holder != null) {
                // JDK-8015743, variant with holder (6u65, 7u45, 8)
                host = DetailsUtils.getInstanceFieldString(
                         holder, "hostName", heap);                               // NOI18N
                if (INET4_ADDRESS_MASK.equals(className)) {
                    instance = (Instance) holder;
                } else {
                    instance = (Instance) instance.getValueOfField("holder6");
                }
            } else {
                host = DetailsUtils.getInstanceFieldString(
                         instance, "hostName", heap);                               // NOI18N
            }
            String addr = null;
            if (!"0.0.0.0".equals(host)) {                                           // NOI18N
                if (INET4_ADDRESS_MASK.equals(className)) {
                    int address = DetailsUtils.getIntFieldValue(
                                  instance, "address", 0);                          // NOI18N
                    addr = getHostInet4Address(address);
                } else {
                    List<String> items = DetailsUtils.getPrimitiveArrayFieldValues(
                                           instance, "ipaddress");                  // NOI18N
                    byte[] ipaddress = DetailsUtils.getByteArray(items);
                    boolean scope_ifname_set = DetailsUtils.getBooleanFieldValue
                                           (instance, "scope_ifname_set", false);   // NOI18N
                    String scope_ifname = null;
                    if (scope_ifname_set) {
                        // java.net.NetworkInterface
                        Object ifname = instance.getValueOfField("scope_ifname");   // NOI18N
                        if (ifname instanceof Instance) {
                            // java.net.NetworkInterface.name [java.lang.String]
                            scope_ifname = DetailsUtils.getInstanceFieldString(
                                           instance, "name", heap);                 // NOI18N
                        }
                    }
                    boolean scope_id_set = DetailsUtils.getBooleanFieldValue
                                           (instance, "scope_id_set", false);       // NOI18N
                    int scope_id = DetailsUtils.getIntFieldValue
                                           (instance, "scope_id", 0);               // NOI18N
                    addr = getHostInet6Address(ipaddress, scope_ifname_set,
                                           scope_ifname, scope_id_set, scope_id);
                }
            }
            if (host == null) {
                return addr;
            }
            if (addr == null) {
                return host;
            }
            return host+" ["+addr+"]";                                              // NOI18N
        } else if (NETWORK_IF_MASK.equals(className)) {                             // NetworkInterface
            String name = DetailsUtils.getInstanceFieldString(
                                           instance, "name", heap);                 // NOI18N
            if (name == null) name = new String();
            String displayName = DetailsUtils.getInstanceFieldString(
                                           instance, "displayName", heap);          // NOI18N
            if (displayName == null) displayName = new String();
            if (!name.isEmpty() && !displayName.isEmpty()) name += " - ";           // NOI18N
            return name + displayName;
        } else if (IF_ADDRESS_MASK.equals(className)) {                             // InterfaceAddress+
            String address = DetailsUtils.getInstanceFieldString(
                                           instance, "address", heap);              // NOI18N
            if (address == null) address = "";
            String broadcast = DetailsUtils.getInstanceFieldString(
                                           instance, "broadcast", heap);            // NOI18N
            if (broadcast == null) broadcast = new String();
            short maskLength = DetailsUtils.getShortFieldValue(
                                           instance, "maskLength", (short)0);       // NOI18N
            return address + "/" + maskLength + " [" + broadcast + "]";             // NOI18N
        } else if (URL_CONN_MASK.equals(className)) {                               // URLConnection+
            String url = DetailsUtils.getInstanceFieldString(
                                           instance, "url", heap);                  // NOI18N
            if (url == null) url = "";
            return /*instance.getJavaClass().getName() + ":" +*/ url;               // NOI18N
        } else if (URI_MASK.equals(className)) {                                    // URI
            String name = DetailsUtils.getInstanceFieldString(
                                           instance, "string", heap);                 // NOI18N
            if (name != null) return name;
            String scheme = DetailsUtils.getInstanceFieldString(
                                           instance, "scheme", heap);               // NOI18N
            String path = DetailsUtils.getInstanceFieldString(
                                           instance, "path", heap);                 // NOI18N
            String schemeSpecificPart = DetailsUtils.getInstanceFieldString(
                                           instance, "schemeSpecificPart", heap);   // NOI18N
            String host = DetailsUtils.getInstanceFieldString(
                                           instance, "host", heap);                 // NOI18N
            String userInfo = DetailsUtils.getInstanceFieldString(
                                           instance, "userInfo", heap);             // NOI18N
            int port = DetailsUtils.getIntFieldValue(
                                           instance, "name", -1);                   // NOI18N
            String authority = DetailsUtils.getInstanceFieldString(
                                           instance, "authority", heap);            // NOI18N
            String query = DetailsUtils.getInstanceFieldString(
                                           instance, "query", heap);                // NOI18N
            String fragment = DetailsUtils.getInstanceFieldString(
                                           instance, "fragment", heap);             // NOI18N
            return defineURIString(scheme, path, schemeSpecificPart, host,
                                   userInfo, port, authority, query, fragment);
        } else if (HTTP_COOKIE_MASK.equals(className)) {                            // HttpCookie
            String name = DetailsUtils.getInstanceFieldString(
                                           instance, "name", heap);                 // NOI18N
            String value = DetailsUtils.getInstanceFieldString(
                                           instance, "value", heap);                // NOI18N
            return name + "=" + value;                                              // NOI18N
        } else if (INET_SOCKET_ADDRERSS.equals(className)) {
            String host = DetailsUtils.getInstanceFieldString(instance, "hostname", heap);  // NOI18N
            String address = DetailsUtils.getInstanceFieldString(instance, "addr", heap); // NOI18N
            int port = DetailsUtils.getIntFieldValue(instance, "port", -1); // NOI18N
            StringBuilder str = new StringBuilder();
            if (host != null) {
                str.append(host);
            }
            if (address != null) {
                if (host != null) {
                    str.append("[").append(address).append("]");    // NOI18N
                } else {
                    str.append(address);
                }
            }
            str.append(":").append(port);       // NOI18N
            return str.toString();
        }
        return null;
    }
    
    
    // --- Inet4Address.getHostAddress() ---------------------------------------
    
    private static String getHostInet4Address(int address) {
        return numericToTextFormatInet4(getInet4Address(address));
    }
    
    private static byte[] getInet4Address(int address) {
        byte[] addr = new byte[4];

        addr[0] = (byte) ((address >>> 24) & 0xFF);
        addr[1] = (byte) ((address >>> 16) & 0xFF);
        addr[2] = (byte) ((address >>> 8) & 0xFF);
        addr[3] = (byte) (address & 0xFF);
        return addr;
    }
    
    private static String numericToTextFormatInet4(byte[] src) {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." +                      // NOI18N
               (src[2] & 0xff) + "." + (src[3] & 0xff);                             // NOI18N
    }
    
    
    // --- Inet6Address.getHostAddress() ---------------------------------------
    
    public String getHostInet6Address(byte[] ipaddress, boolean scope_ifname_set,
                                      String scope_ifname, boolean scope_id_set,
                                      int scope_id) {
        String s = numericToTextFormatInet6(ipaddress);
        if (scope_ifname_set) { /* must check this first */
            s = s + "%" + scope_ifname;                                             // NOI18N
        } else if (scope_id_set) {
            s = s + "%" + scope_id;                                                 // NOI18N
        }
        return s;
    }
    
    private static String numericToTextFormatInet6(byte[] src) {
        StringBuilder sb = new StringBuilder(39);
        for (int i = 0; i < 8; i++) {
            sb.append(Integer.toHexString(((src[i<<1]<<8) & 0xff00)
                                          | (src[(i<<1)+1] & 0xff)));
            if (i < 7) {
               sb.append(":");                                                      // NOI18N
            }
        }
        return sb.toString();
    }
    
    
    // --- URI.defineString() --------------------------------------------------
    
    private String defineURIString(String scheme, String path,
            String schemeSpecificPart, String host, String userInfo, int port,
            String authority, String query, String fragment) {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');                                                         // NOI18N
        }
        if (path == null) {
            sb.append(schemeSpecificPart);
        } else {
            if (host != null) {
                sb.append("//");                                                    // NOI18N
                if (userInfo != null) {
                    sb.append(userInfo);
                    sb.append('@');                                                 // NOI18N
                }
                boolean needBrackets = ((host.indexOf(':') >= 0)                    // NOI18N
                                    && !host.startsWith("[")                        // NOI18N
                                    && !host.endsWith("]"));                        // NOI18N
                if (needBrackets) sb.append('[');                                   // NOI18N
                sb.append(host);
                if (needBrackets) sb.append(']');                                   // NOI18N
                if (port != -1) {
                    sb.append(':');                                                 // NOI18N
                    sb.append(port);
                }
            } else if (authority != null) {
                sb.append("//");                                                    // NOI18N
                sb.append(authority);
            }
            if (path != null)
                sb.append(path);
            if (query != null) {
                sb.append('?');                                                     // NOI18N
                sb.append(query);
            }
        }
        if (fragment != null) {
            sb.append('#');                                                         // NOI18N
            sb.append(fragment);
        }
        return sb.toString();
    }
    
}
