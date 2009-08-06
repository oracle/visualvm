/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.jvmstat.application;

import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.properties.PropertiesPanel;
import com.sun.tools.visualvm.core.properties.PropertiesProvider;
import com.sun.tools.visualvm.host.Host;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public class HostPropertiesProvider extends PropertiesProvider<Host> {

    private static final String PROP_JSTATD_PORT = "prop_jstatd_port"; // NOI18N
    private static final String PROP_JSTATD_REFRESH = "prop_jstatd_refresh"; // NOI18N


    public HostPropertiesProvider() {
        super("jstatd", "Configures jvmstat connections to the Host", 10);
    }


    public boolean supportsDataSource(Host host) {
        return true;
    }

    public PropertiesPanel createPanel(Host host) {
        return new ConnectionsCustomizer(getDescriptorsEx(host));
    }

    public void propertiesDefined(PropertiesPanel panel, Host host) {
        ConnectionsCustomizer customizer = (ConnectionsCustomizer)panel;
        setDescriptors(host, customizer.getDescriptors());
    }

    public void propertiesChanged(PropertiesPanel panel, Host host) {
        ConnectionsCustomizer customizer = (ConnectionsCustomizer)panel;
        setDescriptorsEx(host, customizer.getDescriptors());
    }

    public void propertiesCancelled(PropertiesPanel panel, Host host) {
        // Nothing to do
    }


    public static void initializeLocalhost() {
        Host host = Host.LOCALHOST;
        setDescriptors(host, getDescriptorsEx(null)); // TODO: handle customizations!
    }


    static List<ConnectionDescriptor> getDescriptors(Host host) {
        List<ConnectionDescriptor> list = new ArrayList();

        if (host != null) {
            Storage storage = host.getStorage();
            int index = 0;
            String port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + index); // NOI18N
            while (port != null) {
                String refresh = storage.getCustomProperty(PROP_JSTATD_REFRESH + "." + index); // NOI18N
                try {
                    list.add(new ConnectionDescriptor(Integer.parseInt(port), Integer.parseInt(refresh)));
                } catch (NumberFormatException e) {
                    // TODO: log it
                }
                port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + ++index); // NOI18N
            }
        }

        return list;
    }

    private static List<ConnectionDescriptor> getDescriptorsEx(Host host) {
        List<ConnectionDescriptor> list = getDescriptors(host);
        if (host == null) list.add(ConnectionDescriptor.createDefault());
        return list;
    }

    private static void setDescriptors(Host host, List<ConnectionDescriptor> descriptors) {
        Storage storage = host.getStorage();
        clearDescriptors(storage);
        for (int i = 0; i < descriptors.size(); i++) {
            ConnectionDescriptor descriptor = descriptors.get(i);
            storage.setCustomProperty(PROP_JSTATD_PORT + "." + i, // NOI18N
                    Integer.toString(descriptor.getPort()));
            storage.setCustomProperty(PROP_JSTATD_REFRESH + "." + i, // NOI18N
                    Integer.toString(descriptor.getRefreshRate()));
        }
    }

    private static void setDescriptorsEx(Host host, List<ConnectionDescriptor> newDescriptors) {
        // Cache old descriptors
        List<ConnectionDescriptor> oldDescriptors = getDescriptorsEx(host);

        // Set new descriptors
        setDescriptors(host, newDescriptors);

        // Resolve added descriptors
        Set<ConnectionDescriptor> added = new HashSet(newDescriptors);
        added.removeAll(oldDescriptors);

        // Resolve removed descriptors
        Set<ConnectionDescriptor> removed = new HashSet(oldDescriptors);
        removed.removeAll(newDescriptors);

        // Resolve changed descriptors
        Set<ConnectionDescriptor> changed = new HashSet(newDescriptors);
        changed.retainAll(oldDescriptors);
        Iterator<ConnectionDescriptor> iterator = changed.iterator();
        while (iterator.hasNext()) {
            ConnectionDescriptor descriptor1 = iterator.next();
            ConnectionDescriptor descriptor2 = oldDescriptors.get(
                    newDescriptors.indexOf(descriptor1));
            if (descriptor1.getRefreshRate() == descriptor2.getRefreshRate())
                iterator.remove();
        }

        // TODO: implement JvmstatApplicationProvider.connectionsChanged:
//        if (!added.isEmpty() || !removed.isEmpty() || !changed.isEmpty())
//            JvmstatApplicationProvider.sharedInstance().connectionsChanged(
//                    host, added, removed, changed);
    }

    private static void clearDescriptors(Storage storage) {
        int index = 0;
        String port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + index); // NOI18N
        while (port != null) {
            storage.clearCustomProperties(new String[] {
                PROP_JSTATD_PORT + "." + index, PROP_JSTATD_REFRESH + "." + index // NOI18N
            });
            port = storage.getCustomProperty(PROP_JSTATD_PORT + "." + ++index); // NOI18N
        }
    }

}
