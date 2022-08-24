/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
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
    private static final String INET_SOCKET_ADDRERSS_MASK = "java.net.InetSocketAddress+"; // NOI18N           // NOI18N
    private static final String INET_SOCKET_ADDR_HOLDER_MASK = "java.net.InetSocketAddress$InetSocketAddressHolder"; // NOI18N

    public NetDetailsProvider() {
        super(URL_MASK, INET4_ADDRESS_MASK, INET6_ADDRESS_MASK, NETWORK_IF_MASK,
              IF_ADDRESS_MASK, URL_CONN_MASK, URI_MASK, HTTP_COOKIE_MASK,
              INET_SOCKET_ADDRERSS_MASK, INET_SOCKET_ADDR_HOLDER_MASK);
    }
    
    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case URL_MASK: {                 // URL
                String file = DetailsUtils.getInstanceFieldString(instance, "file"); // NOI18N
                String host = DetailsUtils.getInstanceFieldString(instance, "host");  // NOI18N
                String protocol = DetailsUtils.getInstanceFieldString(instance, "protocol");  // NOI18N
                int port = DetailsUtils.getIntFieldValue(instance, "port", -1); // NOI18N
                if (file != null && protocol != null) {
                    try {
                        return new URL(protocol,host,port,file).toExternalForm();
                    } catch (MalformedURLException ex) {

                    }
                }
                // fallback
                return DetailsUtils.getInstanceFieldString(instance, "path"); // NOI18N;
            }
            case INET4_ADDRESS_MASK:   // Inet4Address
            case INET6_ADDRESS_MASK: { // Inet6Address
                String host;
                Instance holder = (Instance)instance.getValueOfField("holder");                     // NOI18N
                if (holder != null) {
                    // JDK-8015743, variant with holder (6u65, 7u45, 8)
                    host = DetailsUtils.getInstanceFieldString(holder, "hostName");                               // NOI18N
                    if (INET4_ADDRESS_MASK.equals(className)) {
                        instance = (Instance) holder;
                    } else {
                        instance = (Instance) instance.getValueOfField("holder6");
                    }
                } else {
                    host = DetailsUtils.getInstanceFieldString(instance, "hostName");                               // NOI18N
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
                                scope_ifname = DetailsUtils.getInstanceFieldString(instance, "name");                 // NOI18N
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
            }
            case NETWORK_IF_MASK: {      // NetworkInterface
                String name = DetailsUtils.getInstanceFieldString(instance, "name");                 // NOI18N
                if (name == null) name = new String();
                String displayName = DetailsUtils.getInstanceFieldString(instance, "displayName");          // NOI18N
                if (displayName == null) displayName = new String();
                if (!name.isEmpty() && !displayName.isEmpty()) name += " - ";           // NOI18N
                return name + displayName;
            }
            case IF_ADDRESS_MASK: { // InterfaceAddress+
                String address = DetailsUtils.getInstanceFieldString(instance, "address");              // NOI18N
                if (address == null) address = "";
                String broadcast = DetailsUtils.getInstanceFieldString(instance, "broadcast");            // NOI18N
                if (broadcast == null) broadcast = new String();
                short maskLength = DetailsUtils.getShortFieldValue(
                        instance, "maskLength", (short)0);       // NOI18N
                return address + "/" + maskLength + " [" + broadcast + "]";                             // NOI18N
            }
            case URL_CONN_MASK: {    // URLConnection+
                String url = DetailsUtils.getInstanceFieldString(instance, "url");                  // NOI18N
                if (url == null) url = "";
                return /*instance.getJavaClass().getName() + ":" +*/ url;               // NOI18N
            }
            case URI_MASK: {                                    // URI
                String name = DetailsUtils.getInstanceFieldString(instance, "string");                 // NOI18N
                if (name != null) return name;
                String scheme = DetailsUtils.getInstanceFieldString(instance, "scheme");               // NOI18N
                String path = DetailsUtils.getInstanceFieldString(instance, "path");                 // NOI18N
                String schemeSpecificPart = DetailsUtils.getInstanceFieldString(instance, "schemeSpecificPart");   // NOI18N
                String host = DetailsUtils.getInstanceFieldString(instance, "host");                 // NOI18N
                String userInfo = DetailsUtils.getInstanceFieldString(instance, "userInfo");             // NOI18N
                int port = DetailsUtils.getIntFieldValue(
                        instance, "name", -1);                   // NOI18N
                String authority = DetailsUtils.getInstanceFieldString(instance, "authority");            // NOI18N
                String query = DetailsUtils.getInstanceFieldString(instance, "query");                // NOI18N
                String fragment = DetailsUtils.getInstanceFieldString(instance, "fragment");             // NOI18N
                return defineURIString(scheme, path, schemeSpecificPart, host,
                        userInfo, port, authority, query, fragment);
            }
            case HTTP_COOKIE_MASK: {                            // HttpCookie
                String name = DetailsUtils.getInstanceFieldString(instance, "name");                 // NOI18N
                String value = DetailsUtils.getInstanceFieldString(instance, "value");                // NOI18N
                return name + "=" + value;                                              // NOI18N
            }
            case INET_SOCKET_ADDRERSS_MASK: {
                String holder = DetailsUtils.getInstanceFieldString(instance, "holder");  // NOI18N
                if (holder != null) return holder;
                return getInetSocketAddress(instance);
            }
            case INET_SOCKET_ADDR_HOLDER_MASK:
                return getInetSocketAddress(instance);
            default:
                break;
        }
        return null;
    }

    private String getInetSocketAddress(Instance instance) {
        String host = DetailsUtils.getInstanceFieldString(instance, "hostname");  // NOI18N
        String address = DetailsUtils.getInstanceFieldString(instance, "addr"); // NOI18N
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
