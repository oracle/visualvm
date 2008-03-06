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


package com.sun.tools.visualvm.modules.jconsole.options;

import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.NbPreferences;


public class JConsoleSettings implements ChangeListener
{
    static final long serialVersionUID = -1055706114162507505L;

    private static final String PROP_POLLING     = "POLLING"; // NOI18N
    private static final String PROP_TILE   = "TILE"; // NOI18N
    private static final String PROP_CLASSPATH  = "CLASSPATH"; // NOI18N
    private static final String PROP_PLUGINSPATH  = "PLUGINS"; // NOI18N
    private static final String PROP_VM_OPTIONS = "VM_OPTIONS"; //NOI18N
    private static final String PROP_OTHER_ARGS = "OTHER_ARGS"; //NOI18N
    private static final String PROP_URL = "DEFAULT_URL"; //NOI18N
    
    public static String NETBEANS_CLASS_PATH;
    private Preferences pref;
    
    private static Boolean greater;
    private static JConsoleSettings INSTANCE;
    
    JConsoleSettings() 
    {
        pref = NbPreferences.forModule(JConsoleSettings.class);
        setPolling(4);
        setTile(Boolean.TRUE);
    }
    
    public static boolean isNetBeansJVMGreaterThanJDK15() {
        if(greater == null) {
            //Check if we are running on 1.6 minimum
            try {
                Class.forName("javax.swing.SwingWorker");// NOI18N
               greater = true;
            }catch(ClassNotFoundException e) {
                greater = false;
            }
        }
        return greater;
    }
    
    public String displayName ()
    {
        return "JConsole settings";// NOI18N
    }    

    public static synchronized JConsoleSettings getDefault () 
    {
        if (INSTANCE == null) {
            INSTANCE = new JConsoleSettings();
        }
        return INSTANCE;
    }
    
    public void stateChanged (ChangeEvent e)
    {
    }
    
    public Boolean getTile()
    {
        return pref.getBoolean(PROP_TILE,Boolean.TRUE);
    }

    public void setTile(Boolean tile)
    {
        pref.putBoolean(PROP_TILE, tile);
    }
    
    public String getClassPath()
    {
        return pref.get(PROP_CLASSPATH,null);
    }

    public void setClassPath(String value)
    {
        pref.put(PROP_CLASSPATH, value);
    }
    
    public String getPluginsPath()
    {
        return pref.get(PROP_PLUGINSPATH,null);
    }

    public void setPluginsPath(String value)
    {
        pref.put(PROP_PLUGINSPATH, value);
    }
    
    public Integer getPolling()
    {
        return pref.getInt(PROP_POLLING,4);
    }
    
    public void setPolling(Integer polling)
    {   
        pref.putInt(PROP_POLLING, polling);
    }
    
    public String getVMOptions()
    {
        return pref.get(PROP_VM_OPTIONS,null);
    }
    
    public void setVMOptions(String other)
    {
        pref.put(PROP_VM_OPTIONS, other);
    }
    
    public String getOtherArgs()
    {
        return pref.get(PROP_OTHER_ARGS,null);
    }
    
    public void setOtherArgs(String other)
    {
        pref.put(PROP_OTHER_ARGS, other);
    }
    
    public String getDefaultUrl()
    {
        return pref.get(PROP_URL,null);
    }
    
    public void setDefaultUrl(String url)
    {
        pref.put(PROP_URL, url);
    }
    
}
