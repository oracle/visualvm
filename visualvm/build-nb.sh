#!/bin/bash

# NetBeans 19 FCS
REV=ba85468772292fd64f188f4022f9d49f77f00b89
BRANCH=release190
BUILD_DATE=`git log -n1 --date=format:'%Y%m%d' $0 | fgrep 'Date' | awk '{print $2}'`
ZIPNAME=nb190_platform_$BUILD_DATE

set -e

mkdir -p build/nb/
cd build/nb/
BUILD_ROOT=`pwd`
if [ -e $BUILD_ROOT/$ZIPNAME.zip ]; then
  echo "$BUILD_ROOT/$ZIPNAME.zip is up to date"
  exit
fi
if [ -e netbeans ]; then
  cd netbeans
  git fetch
  git clean -fdx
else
  git clone https://github.com/apache/netbeans netbeans
  cd netbeans
fi

git checkout $BRANCH
git reset --hard $REV
git revert --no-edit -n d55be1aff900a81b22081f7699fd16ab04e42553
git restore --staged .github/ apisupport/ harness/ platform/
patch -p1 <<'EOF'
diff --git a/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties b/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties
index e72a2ab534..450a437731 100644
--- a/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties
+++ b/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties
@@ -104,7 +104,7 @@ TabControlIcon.foreground=tint(@foreground,40%)
 TabControlIcon.disabledForeground=lighten($TabControlIcon.foreground,27%)
 TabControlIcon.rolloverBackground=$Button.toolbar.hoverBackground
 TabControlIcon.pressedBackground=$Button.toolbar.pressedBackground
-TabControlIcon.close.rolloverBackground=#c74f50
+TabControlIcon.close.rolloverBackground=#7d7d7d
 TabControlIcon.close.rolloverForeground=#fff
 TabControlIcon.arc=2

diff --git a/platform/o.n.swing.tabcontrol/src/org/netbeans/swing/tabcontrol/plaf/AquaVectorTabControlIcon.java b/platform/o.n.swing.tabcontrol/src/org/netbeans/swing/tabcontrol/plaf/AquaVectorTabControlIcon.java
index f1bbdfaae3..46b7aba999 100644
--- a/platform/o.n.swing.tabcontrol/src/org/netbeans/swing/tabcontrol/plaf/AquaVectorTabControlIcon.java
+++ b/platform/o.n.swing.tabcontrol/src/org/netbeans/swing/tabcontrol/plaf/AquaVectorTabControlIcon.java
@@ -200,11 +200,11 @@ final class AquaVectorTabControlIcon extends VectorIcon {
             /* Red, with some transparency to blend onto the background. Chrome would have
             (244, 65, 54, 255), here, but the value below works better with our expected
             backgrounds. */
-            bgColor = new Color(255, 35, 25, 215);
+            bgColor = new Color(125, 125, 125, 215);
         } else if (buttonState == TabControlButton.STATE_PRESSED) {
             fgColor = Color.WHITE;
             // Slightly darker red. Chrome would have (196, 53, 43, 255) here; see above.
-            bgColor = new Color(185, 43, 33, 215);
+            bgColor = new Color(105, 105, 105, 215);
         } else if (buttonState == TabControlButton.STATE_DISABLED) {
             // Light grey (via transparent black to work well on any background).
             fgColor = new Color(0, 0, 0, 60);
diff --git a/platform/openide.awt/src/org/openide/awt/AquaVectorCloseButton.java b/platform/openide.awt/src/org/openide/awt/AquaVectorCloseButton.java
index 4adfc32095..7712a2f8b3 100644
--- a/platform/openide.awt/src/org/openide/awt/AquaVectorCloseButton.java
+++ b/platform/openide.awt/src/org/openide/awt/AquaVectorCloseButton.java
@@ -57,10 +57,10 @@ final class AquaVectorCloseButton extends VectorIcon {
         Color fgColor = new Color(0, 0, 0, 168);
         if (state == State.ROLLOVER) {
             fgColor = Color.WHITE;
-            bgColor = new Color(255, 35, 25, 215);
+            bgColor = new Color(125, 125, 125, 215);
         } else if (state == State.PRESSED) {
             fgColor = Color.WHITE;
-            bgColor = new Color(185, 43, 33, 215);
+            bgColor = new Color(105, 105, 105, 215);
         }
         if (bgColor.getAlpha() > 0) {
             double circPosX = (width - d) / 2.0;
diff --git a/nbbuild/build.properties b/nbbuild/build.properties
index 743fc7e3f5..f30d2bc922 100644
--- a/nbbuild/build.properties
+++ b/nbbuild/build.properties
@@ -134,7 +134,6 @@ config.javadoc.stable=\
     api.debugger.jpda,\
     project.ant,\
     project.ant.ui,\
-    api.visual,\
     api.java,\
     api.java.classpath,\
     api.search,\
@@ -171,15 +170,12 @@ config.javadoc.forwarded.devel=\
     editor.bracesmatching,\
     editor.lib,\
     editor,\
-    lib.uihandler,\
-    uihandler,\
     spi.editor.hints

 # List of javadocs under development
 config.javadoc.devel=\
     junit,\
     core.multitabs,\
-    core.netigso,\
     gradle,\
     gradle.java,\
     o.n.swing.outline,\
diff --git a/nbbuild/cluster.properties b/nbbuild/cluster.properties
index 25f6bb112c..705729ca0f 100644
--- a/nbbuild/cluster.properties
+++ b/nbbuild/cluster.properties
@@ -163,25 +163,18 @@ nb.cluster.platform=\
         core.multitabs,\
         core.multiview,\
         core.nativeaccess,\
-        core.netigso,\
         core.network,\
-        core.osgi,\
         core.output2,\
         core.ui,\
         core.windows,\
         editor.mimelookup,\
         editor.mimelookup.impl,\
         favorites,\
-        htmlui,\
-        janitor,\
         javahelp,\
         junitlib,\
         keyring,\
         keyring.fallback,\
         keyring.impl,\
-        lib.uihandler,\
-        libs.batik.read,\
-        libs.felix,\
         libs.flatlaf,\
         libs.javafx,\
         libs.jna,\
@@ -189,7 +182,6 @@ nb.cluster.platform=\
         libs.jsr223,\
         libs.junit4,\
         libs.junit5,\
-        libs.osgi,\
         libs.testng,\
         masterfs,\
         masterfs.linux,\
@@ -200,19 +192,8 @@ nb.cluster.platform=\
         net.java.html,\
         net.java.html.boot,\
         net.java.html.boot.fx,\
-        net.java.html.boot.script,\
-        net.java.html.geo,\
         net.java.html.json,\
-        net.java.html.sound,\
-        netbinox,\
-        o.apache.commons.codec,\
-        o.apache.commons.io,\
-        o.apache.commons.lang3,\
-        o.apache.commons.logging,\
         o.n.core,\
-        o.n.html.ko4j,\
-        o.n.html.presenters.spi,\
-        o.n.html.xhr4j,\
         o.n.swing.laf.dark,\
         o.n.swing.laf.flatlaf,\
         o.n.swing.outline,\
@@ -223,16 +204,13 @@ nb.cluster.platform=\
         openide.compat,\
         openide.dialogs,\
         openide.execution,\
-        openide.execution.compat8,\
         openide.explorer,\
-        openide.filesystems.compat8,\
         openide.filesystems.nb,\
         openide.io,\
         openide.loaders,\
         openide.nodes,\
         openide.options,\
         openide.text,\
-        openide.util.ui.svg,\
         openide.windows,\
         options.api,\
         options.keymap,\
@@ -245,8 +223,7 @@ nb.cluster.platform=\
         spi.actions,\
         spi.quicksearch,\
         templates,\
-        templatesui,\
-        uihandler
+        templatesui
 validation.nb.cluster.platform=\
         o.n.core,\
         core.windows,\
diff --git a/harness/apisupport.harness/nbproject/project.properties b/harness/apisupport.harness/nbproject/project.properties
index 7db6d57275..4b8b94fa03 100644
--- a/harness/apisupport.harness/nbproject/project.properties
+++ b/harness/apisupport.harness/nbproject/project.properties
@@ -91,7 +91,10 @@ bundled.tasks=\
     org/netbeans/nbbuild/XMLUtil*.class,\
     org/netbeans/nbbuild/extlibs/DownloadBinaries*.class,\
     org/netbeans/nbbuild/extlibs/ConfigureProxy*.class,\
-    org/netbeans/nbbuild/extlibs/MavenCoordinate.class
+    org/netbeans/nbbuild/extlibs/MavenCoordinate.class,\
+    org/netbeans/nbbuild/extlibs/ReleaseFilesCopy*.class,\
+    org/netbeans/nbbuild/extlibs/ReleaseFilesExtra*.class,\
+    org/netbeans/nbbuild/extlibs/ReleaseFilesLicense*.class
 
 test.unit.cp.extra=${netbeans.dest.dir}/harness/jnlp/jnlp-launcher.jar
 javadoc.arch=${basedir}/arch.xml
diff --git a/harness/apisupport.harness/taskdefs.properties b/harness/apisupport.harness/taskdefs.properties
index 19a01429c9..0d8b86adef 100644
--- a/harness/apisupport.harness/taskdefs.properties
+++ b/harness/apisupport.harness/taskdefs.properties
@@ -40,3 +40,6 @@ parsemanifest=org.netbeans.nbbuild.ParseManifest
 autoupdate=org.netbeans.nbbuild.AutoUpdate
 downloadbinaries=org.netbeans.nbbuild.extlibs.DownloadBinaries
 processjsannotation=org.netbeans.nbbuild.ProcessJsAnnotationsTask
+releasefilescopy=org.netbeans.nbbuild.extlibs.ReleaseFilesCopy
+releasefilesextra=org.netbeans.nbbuild.extlibs.ReleaseFilesExtra
+releasefileslicense=org.netbeans.nbbuild.extlibs.ReleaseFilesLicense
diff --git a/harness/apisupport.harness/release/build.xml b/harness/apisupport.harness/release/build.xml
index 9171e4ef9c..b9c0abf73a 100644
--- a/harness/apisupport.harness/release/build.xml
+++ b/harness/apisupport.harness/release/build.xml
@@ -278,6 +278,17 @@
         </javadoc>
     </target>
 
+    <target name="javadoc-zip" depends="javadoc" description="Simple javadoc zip creation intended for use in maven repository.">
+        <zip zipfile="${netbeans.javadoc.dir}/${code.name.base.dashes}.zip" basedir="${netbeans.javadoc.dir}/${code.name.base.dashes}"/>
+    </target>
+
+    <target name="sources-zip" depends="build-init" description="Simple sources zip creation intended for use in maven repository.">
+        <mkdir dir="${netbeans.zipped.sources.dir}"/>
+        <zip zipfile="${netbeans.zipped.sources.dir}/${code.name.base.dashes}.zip">
+            <zipfileset dir="${src.dir}" />
+        </zip>
+    </target>
+
     <target name="javadoc-nb" depends="init,javadoc" if="netbeans.home">
         <nbbrowse file="${netbeans.javadoc.dir}/${code.name.base.dashes}/index.html"/>
     </target>
diff --git a/platform/autoupdate.services/libsrc/org/netbeans/updater/resources/autoupdate-catalog-2_8.dtd b/platform/autoupdate.services/libsrc/org/netbeans/updater/resources/autoupdate-catalog-2_8.dtd
index 074e63671515..0901373bf327 100644
--- a/platform/autoupdate.services/libsrc/org/netbeans/updater/resources/autoupdate-catalog-2_8.dtd
+++ b/platform/autoupdate.services/libsrc/org/netbeans/updater/resources/autoupdate-catalog-2_8.dtd
@@ -64,6 +64,7 @@
                    OpenIDE-Module-Name CDATA #REQUIRED
                    OpenIDE-Module-Specification-Version CDATA #REQUIRED
                    OpenIDE-Module-Implementation-Version CDATA #IMPLIED
+                   OpenIDE-Module-Build-Version CDATA #IMPLIED
                    OpenIDE-Module-Module-Dependencies CDATA #IMPLIED
                    OpenIDE-Module-Package-Dependencies CDATA #IMPLIED
                    OpenIDE-Module-Java-Dependencies CDATA #IMPLIED
diff --git a/platform/applemenu/src/org/netbeans/modules/applemenu/CtrlClickHack.java b/platform/applemenu/src/org/netbeans/modules/applemenu/CtrlClickHack.java
deleted file mode 100644
index 97c2795dc3..0000000000
--- a/platform/applemenu/src/org/netbeans/modules/applemenu/CtrlClickHack.java
+++ /dev/null
@@ -1,106 +0,0 @@
-/*
- * Licensed to the Apache Software Foundation (ASF) under one
- * or more contributor license agreements.  See the NOTICE file
- * distributed with this work for additional information
- * regarding copyright ownership.  The ASF licenses this file
- * to you under the Apache License, Version 2.0 (the
- * "License"); you may not use this file except in compliance
- * with the License.  You may obtain a copy of the License at
- *
- *   http://www.apache.org/licenses/LICENSE-2.0
- *
- * Unless required by applicable law or agreed to in writing,
- * software distributed under the License is distributed on an
- * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
- * KIND, either express or implied.  See the License for the
- * specific language governing permissions and limitations
- * under the License.
- */
-
-package org.netbeans.modules.applemenu;
-
-import java.awt.AWTEvent;
-import java.awt.event.AWTEventListener;
-import java.awt.event.FocusEvent;
-import java.awt.event.InputEvent;
-import java.awt.event.MouseEvent;
-import java.lang.ref.Reference;
-import java.lang.ref.WeakReference;
-import java.lang.reflect.Field;
-import java.lang.reflect.Method;
-import javax.swing.text.Caret;
-import javax.swing.text.JTextComponent;
-
-/**
- * hack for issue #67799, on macosx with single button mouse,
- * make Ctrl-Click work as right click on multiselections
- *
- * Also handles issue #90371 - on Macintosh, JTextComponents
- * are never sent focus lost events, resulting in multiple
- * blinking carets.  Hack tracks last known JTextComponent
- * and sets its cursor to invisible if any other component 
- * gains focus (on Mac OS, getOppositeComponent() 
- * frequently returns null when coming from a JTextComponent)
- *
- * @author ttran, Tim Boudreau
- */
-public class CtrlClickHack implements AWTEventListener {
-    private Reference<JTextComponent> lastFocusedTextComponent = null;
-
-    public void eventDispatched(AWTEvent e) {
-        if (!(e instanceof MouseEvent) && !(e instanceof FocusEvent)) {
-            return;
-        }
-        if (e instanceof FocusEvent) {
-            FocusEvent fe = (FocusEvent) e;
-            if (fe.getID() == FocusEvent.FOCUS_GAINED) {
-                if (fe.getOppositeComponent() instanceof JTextComponent) {
-                    JTextComponent jtc = (JTextComponent) fe.getOppositeComponent();
-                    if (null != jtc) {
-                        Caret caret = jtc.getCaret();
-                        if (null != caret) {
-                            caret.setVisible(false);
-                        }
-                    }
-                } else {
-                    JTextComponent jtc = lastFocusedTextComponent == null ? null :
-                        lastFocusedTextComponent.get();
-                    if (null != jtc) {
-                        Caret caret = jtc.getCaret();
-                        if (null != caret)
-                            caret.setVisible(false);
-                    }
-                }
-                if (fe.getComponent() instanceof JTextComponent) {
-                    JTextComponent jtc = (JTextComponent) fe.getComponent();
-                    lastFocusedTextComponent = new WeakReference<JTextComponent>(jtc);
-                    if (null != jtc) {
-                        Caret caret = jtc.getCaret();
-                        if (null != caret) {
-                            caret.setVisible(true);
-                        }
-                    }
-                }
-            }
-            return;
-        }
-        MouseEvent evt = (MouseEvent) e;
-        if (evt.getModifiers() != (InputEvent.BUTTON1_MASK | InputEvent.CTRL_MASK)) {
-            return;
-        }
-        try {
-            Field f1 = InputEvent.class.getDeclaredField("modifiers");
-            Field f2 = MouseEvent.class.getDeclaredField("button");
-            Method m = MouseEvent.class.getDeclaredMethod("setNewModifiers", new Class[] {});
-            f1.setAccessible(true);
-            f1.setInt(evt, InputEvent.BUTTON3_MASK);
-            f2.setAccessible(true);
-            f2.setInt(evt, MouseEvent.BUTTON3);
-            m.setAccessible(true);
-            m.invoke(evt, new Object[] {});
-        } catch (Exception ex) {
-            ex.printStackTrace();
-        }
-    }
-    
-}
diff --git a/platform/applemenu/src/org/netbeans/modules/applemenu/Install.java b/platform/applemenu/src/org/netbeans/modules/applemenu/Install.java
index e848b2e4e0..a3d28f8bb9 100644
--- a/platform/applemenu/src/org/netbeans/modules/applemenu/Install.java
+++ b/platform/applemenu/src/org/netbeans/modules/applemenu/Install.java
@@ -31,13 +31,10 @@ import org.openide.util.Utilities;
  * @author  Tim Boudreau
  */
 public class Install extends ModuleInstall {
-    private CtrlClickHack listener;
     private Class adapter;
 
     @Override
     public void restored () {
-        listener = new CtrlClickHack();
-        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK);
         if (Utilities.isMac() ) {
             String pn = "apple.laf.useScreenMenuBar"; // NOI18N
             if (System.getProperty(pn) == null) {
@@ -65,10 +62,6 @@ public class Install extends ModuleInstall {
     
     @Override
     public void uninstalled () {
-         if (listener != null) {
-            Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
-            listener = null;
-         }
         if (Utilities.isMac() && adapter != null) {
             try {
                 Method m = adapter.getDeclaredMethod("uninstall", new Class[0] );   // NOI18N
EOF
git status

OPTS=-Dbuild.compiler.debuglevel=source,lines
SHORT_REV=`git rev-parse --short HEAD`
git clean -fdX
cd nbbuild
ant $OPTS -Dname=platform -Dbuildnumber=$BUILD_DATE-$SHORT_REV rebuild-cluster
ant $OPTS -Dname=harness -Dbuildnumber=$BUILD_DATE-$SHORT_REV rebuild-cluster

zip -r $BUILD_ROOT/$ZIPNAME.zip netbeans
