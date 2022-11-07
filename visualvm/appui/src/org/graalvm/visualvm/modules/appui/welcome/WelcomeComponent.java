/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.BorderLayout;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import org.openide.ErrorManager;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * The welcome screen.
 * @author  Richard Gregor, S. Aubrecht
 */
public class WelcomeComponent extends TopComponent {
    static final long serialVersionUID=6021472310161712674L;
    private static final String PREFERRED_ID = "VisualVMWelcome";   //NOI18N
    private static WeakReference<WelcomeComponent> component = new WeakReference<>(null);
    private JComponent content;

    private boolean initialized = false;
    
    private WelcomeComponent(){
        setLayout(new BorderLayout());
        setName(NbBundle.getMessage(WelcomeComponent.class, "LBL_Tab_Title"));   //NOI18N
        content = null;
        initialized = false;
        putClientProperty( "activateAtStartup", Boolean.TRUE ); //NOI18N
    }
    
    @Override protected String preferredID(){
        return PREFERRED_ID;    //NOI18N
    }
    
    /**
     * #38900 - lazy addition of GUI components
     */    
    
    private void doInitialize() {
        initAccessibility();
        
        if( null == content ) {
            content = new StartPageContent();

            add( content, BorderLayout.CENTER );
            setFocusable( false );
        }
    }
        
    /* Singleton accessor. As WelcomeComponent is persistent singleton this
     * accessor makes sure that WelcomeComponent is deserialized by window system.
     * Uses known unique TopComponent ID "VisualVMWelcome" to get WelcomeComponent instance
     * from window system. "VisualVMWelcome" is name of settings file defined in module layer.
     */
    public static synchronized WelcomeComponent findComp() {
        WelcomeComponent wc = component.get();
        if (wc == null) {
            TopComponent tc = WindowManager.getDefault().findTopComponent(PREFERRED_ID); // NOI18N
            if (tc != null) {
                if (tc instanceof WelcomeComponent) {
                    wc = (WelcomeComponent)tc;
                    component = new WeakReference<>(wc);
                } else {
                    //Incorrect settings file?
                    IllegalStateException exc = new IllegalStateException
                    ("Incorrect settings file. Unexpected class returned." // NOI18N
                    + " Expected:" + WelcomeComponent.class.getName() // NOI18N
                    + " Returned:" + tc.getClass().getName()); // NOI18N
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, exc);
                    //Fallback to accessor reserved for window system.
                    wc = WelcomeComponent.createComp();
                }
            } else {
                //WelcomeComponent cannot be deserialized
                //Fallback to accessor reserved for window system.
                wc = WelcomeComponent.createComp();
            }
        }       
        return wc;
    }
    
    /* Singleton accessor reserved for window system ONLY. Used by window system to create
     * WelcomeComponent instance from settings file when method is given. Use <code>findComp</code>
     * to get correctly deserialized instance of WelcomeComponent. */
    public static synchronized  WelcomeComponent createComp() {
        WelcomeComponent wc = component.get();
        if(wc == null) {
            wc = new WelcomeComponent();
            component = new WeakReference<>(wc);
        }
        return wc;
    }
    
    /** Overriden to explicitely set persistence type of WelcomeComponent
     * to PERSISTENCE_ALWAYS */
    @Override public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }
    
    private void initAccessibility(){
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(WelcomeComponent.class, "ACS_Welcome_DESC")); // NOI18N
    }

    /**
     * Called when <code>TopComponent</code> is about to be shown.
     * Shown here means the component is selected or resides in it own cell
     * in container in its <code>Mode</code>. The container is visible and not minimized.
     * <p><em>Note:</em> component
     * is considered to be shown, even its container window
     * is overlapped by another window.</p>
     * @since 2.18
     *
     * #38900 - lazy addition of GUI components
     *
     */
    @Override protected void componentShowing() {
        if (!initialized) {
            initialized = true;
            doInitialize();
        }
        if( null != content && getComponentCount() == 0 ) {
            //notify components down the hierarchy tree that they should 
            //refresh their content (e.g. RSS feeds)
            add( content, BorderLayout.CENTER );
        }
        super.componentShowing();
//        setActivatedNodes( new Node[] {} );
    }

    private static boolean firstTimeOpen = true;
    @Override 
    protected void componentOpened() {
        super.componentOpened();
        if( firstTimeOpen ) {
            firstTimeOpen = false;
            if( !WelcomeOptions.getDefault().isShowOnStartup() ) {
                close();
            }
        }
    }
    
    @Override protected void componentHidden() {
        super.componentHidden();
        if( null != content ) {
            //notify components down the hierarchy tree that they no long 
            //need to periodically refresh their content (e.g. RSS feeds)
            remove( content );
        }
    }

    @Override
    public void requestFocus() {
        if( null != content )
            content.requestFocus();
    }

    @Override
    public boolean requestFocusInWindow() {
        if( null != content )
            return content.requestFocusInWindow();
        return super.requestFocusInWindow();
    }
}

