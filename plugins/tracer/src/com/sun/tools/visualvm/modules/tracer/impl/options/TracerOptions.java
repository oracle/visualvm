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

package com.sun.tools.visualvm.modules.tracer.impl.options;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TracerOptions {

    private static final String PROP_INITIALLY_OPEN = "TracerOptions.initiallyOpen"; // NOI18N
    private static final String PROP_PROBE_ADDED = "TracerOptions.probeAdded"; // NOI18N
    private static final String PROP_SESSION_STARTED = "TracerOptions.sessionStarted"; // NOI18N
    public static final String VIEWS_UNCHANGED = ""; // NOI18N
    public static final String VIEW_PROBES = "KEY_probes"; // NOI18N
    public static final String VIEW_TIMELINE = "KEY_timeline"; // NOI18N
    public static final String VIEW_SETTINGS = "KEY_settings"; // NOI18N
    public static final String VIEW_DETAILS = "KEY_details"; // NOI18N
    static final String INITIALLY_OPEN_DEFAULT = VIEW_PROBES;
    static final String PROBE_ADDED_DEFAULT = VIEW_PROBES + "," + VIEW_TIMELINE; // NOI18N
    static final String SESSION_STARTED_DEFAULT = VIEW_TIMELINE;

    private static final String PROP_ZOOM_MODE = "TracerOptions.zoomMode"; // NOI18N
    private static final String KEY_FIXED_SCALE = "KEY_fixedScale"; // NOI18N
    private static final String KEY_SCALE_TO_FIT = "KEY_scaleToFit"; // NOI18N
    public static final String FIXED_SCALE = "fixed scale";
    public static final String SCALE_TO_FIT = "scale to fit";

    private static final String PROP_MOUSE_WHEEL_ACTION = "TracerOptions.mouseWheelAction"; // NOI18N
    private static final String KEY_MOUSE_WHEEL_ZOOMS = "KEY_mouseWheelZooms"; // NOI18N
    private static final String KEY_MOUSE_WHEEL_HSCROLLS = "KEY_mouseWheelHScrolls"; // NOI18N
    private static final String KEY_MOUSE_WHEEL_VSCROLLS = "KEY_mouseWheelVScrolls"; // NOI18N
    public static final String MOUSE_WHEEL_ZOOMS = "zooms";
    public static final String MOUSE_WHEEL_HSCROLLS = "scrolls horizontally";
    public static final String MOUSE_WHEEL_VSCROLLS = "scrolls vertically";


    private static TracerOptions INSTANCE;

    private final Preferences prefs;


    public static synchronized TracerOptions getInstance() {
        if (INSTANCE == null) INSTANCE = new TracerOptions();
        return INSTANCE;
    }


    void setInitiallyOpened(String opened) {
        prefs.put(PROP_INITIALLY_OPEN, opened);
    }

    public String getInitiallyOpened() {
        return prefs.get(PROP_INITIALLY_OPEN, INITIALLY_OPEN_DEFAULT);
    }

    void setOnProbeAddded(String opened) {
        prefs.put(PROP_PROBE_ADDED, opened);
    }

    public String getOnProbeAdded() {
        return prefs.get(PROP_PROBE_ADDED, PROBE_ADDED_DEFAULT);
    }

    void setOnSessionStart(String opened) {
        prefs.put(PROP_SESSION_STARTED, opened);
    }

    public String getOnSessionStart() {
        return prefs.get(PROP_SESSION_STARTED, SESSION_STARTED_DEFAULT);
    }

    void setZoomMode(String zoomMode) {
        if (SCALE_TO_FIT.equals(zoomMode))
            prefs.put(PROP_ZOOM_MODE, KEY_SCALE_TO_FIT);
        else
            prefs.put(PROP_ZOOM_MODE, KEY_FIXED_SCALE);
    }

    public String getZoomMode() {
        String zoomMode = prefs.get(PROP_ZOOM_MODE, KEY_FIXED_SCALE);
        if (KEY_SCALE_TO_FIT.equals(zoomMode)) return SCALE_TO_FIT;
        return FIXED_SCALE;
    }

    void setMouseWheelAction(String mouseWheelAction) {
        if (MOUSE_WHEEL_HSCROLLS.equals(mouseWheelAction))
            prefs.put(PROP_MOUSE_WHEEL_ACTION, KEY_MOUSE_WHEEL_HSCROLLS);
        else if (MOUSE_WHEEL_VSCROLLS.equals(mouseWheelAction))
            prefs.put(PROP_MOUSE_WHEEL_ACTION, KEY_MOUSE_WHEEL_VSCROLLS);
        else
            prefs.put(PROP_MOUSE_WHEEL_ACTION, KEY_MOUSE_WHEEL_ZOOMS);
    }

    public String getMouseWheelAction() {
        String mouseWheelAction = prefs.get(PROP_MOUSE_WHEEL_ACTION, KEY_MOUSE_WHEEL_ZOOMS);
        if (KEY_MOUSE_WHEEL_HSCROLLS.equals(mouseWheelAction)) return MOUSE_WHEEL_HSCROLLS;
        else if (KEY_MOUSE_WHEEL_VSCROLLS.equals(mouseWheelAction)) return MOUSE_WHEEL_VSCROLLS;
        return MOUSE_WHEEL_ZOOMS;
    }


    private TracerOptions() {
        prefs = NbPreferences.forModule(TracerOptions.class);
    }

}
