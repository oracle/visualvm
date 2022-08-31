#!/bin/bash

# NetBeans 14 FCS
REV=c4f2d87113e1a2d2d4e21e8952e1f99612d5b3fd
BRANCH=release140
BUILD_DATE=`git log -n1 --date=format:'%d%m%Y' $0 | fgrep 'Date' | awk '{print $2}'`
ZIPNAME=nb140_platform_$BUILD_DATE

set -e

mkdir -p build/nb/
cd build/nb/
BUILD_ROOT=`pwd`
if [ -e $BUILD_ROOT/$ZIPNAME.zip ]; then
  echo "$BUILD_ROOT/$ZIPNAME.zip is upto date"
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
git cherry-pick -n -Xignore-all-space --strategy-option=theirs c6166f56bdefe2dee663b5bb26de86219e4c04ff
git restore --staged platform
patch -p1 <<'EOF'
diff --git a/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties b/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties
index 4d1dbb762b..8f2630e25e 100644
--- a/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties
+++ b/platform/o.n.swing.laf.flatlaf/src/org/netbeans/swing/laf/flatlaf/FlatLaf.properties
@@ -81,7 +81,7 @@ TabControlIcon.foreground=$ComboBox.buttonArrowColor
 TabControlIcon.disabledForeground=$ComboBox.buttonDisabledArrowColor
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
index 08dab861c9..9b967d5e92 100644
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
@@ -155,9 +154,7 @@ config.javadoc.stable=\
     db.core,\
     spi.quicksearch,\
     print,\
-    extexecution,\
-    lib.uihandler,\
-    uihandler
+    extexecution
 
 #FIXME: changes for retouche merge:
 #   editor/codetemplates,\
@@ -168,7 +165,6 @@ config.javadoc.devel=\
     jellytools.platform,\
     jellytools.ide,\
     core.multitabs,\
-    core.netigso,\
     gradle,\
     gradle.java,\
     o.n.swing.outline,\
diff --git a/nbbuild/cluster.properties b/nbbuild/cluster.properties
index ec87c9719b..4573ebdb9b 100644
--- a/nbbuild/cluster.properties
+++ b/nbbuild/cluster.properties
@@ -217,25 +217,18 @@ nb.cluster.platform=\
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
@@ -243,7 +236,6 @@ nb.cluster.platform=\
         libs.jsr223,\
         libs.junit4,\
         libs.junit5,\
-        libs.osgi,\
         libs.testng,\
         masterfs,\
         masterfs.linux,\
@@ -254,19 +246,8 @@ nb.cluster.platform=\
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
@@ -277,16 +258,13 @@ nb.cluster.platform=\
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
@@ -299,8 +277,7 @@ nb.cluster.platform=\
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
EOF
git status

OPTS=-Dbuild.compiler.debuglevel=source,lines

git clean -fdX
cd nbbuild
ant $OPTS -Dname=platform rebuild-cluster
ant $OPTS -Dname=harness rebuild-cluster

zip -r $BUILD_ROOT/$ZIPNAME.zip netbeans
