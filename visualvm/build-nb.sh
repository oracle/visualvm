#!/bin/bash

# NetBeans 12.4 FCS
REV=21726744165c946ba6619bff89e98d5863f26e22
ZIPNAME=nb124_platform_`date "+%d%m%Y"`

set -e

mkdir -p build/nb/
cd build/nb/
BUILD_ROOT=`pwd`
if [ -e netbeans ]; then
  cd netbeans
  git fetch
else
  git clone https://github.com/apache/netbeans netbeans
  cd netbeans
fi

git checkout -f $REV
git cherry-pick -n da5d804660a142b2ce96b570ad7a40436e1859c1
git apply - <<'EOF'
diff --git a/platform/libs.jna.platform/external/binaries-list b/platform/libs.jna.platform/external/binaries-list
index 9acffb010f..0b881e49bd 100644
--- a/platform/libs.jna.platform/external/binaries-list
+++ b/platform/libs.jna.platform/external/binaries-list
@@ -15,4 +15,4 @@
 # specific language governing permissions and limitations
 # under the License.
 
-C535A5BDA553D7D7690356C825010DA74B2671B5 net.java.dev.jna:jna-platform:5.9.0
+FBED7D9669DBA47714AD0D4F4454290A997AEE69 net.java.dev.jna:jna-platform:5.10.0
diff --git a/platform/libs.jna.platform/external/jna-platform-5.9.0-license.txt b/platform/libs.jna.platform/external/jna-platform-5.10.0-license.txt
similarity index 99%
rename from platform/libs.jna.platform/external/jna-platform-5.9.0-license.txt
rename to platform/libs.jna.platform/external/jna-platform-5.10.0-license.txt
index 7ab5d5838b..93f6f38469 100644
--- a/platform/libs.jna.platform/external/jna-platform-5.9.0-license.txt
+++ b/platform/libs.jna.platform/external/jna-platform-5.10.0-license.txt
@@ -1,5 +1,5 @@
 Name: Java Native Access
-Version: 5.9.0
+Version: 5.10.0
 License: Apache-2.0
 Description: Dynamically access native libraries from Java without JNI.
 Origin: Java Native Access
diff --git a/platform/libs.jna.platform/nbproject/project.properties b/platform/libs.jna.platform/nbproject/project.properties
index b86335082c..247f130b0b 100644
--- a/platform/libs.jna.platform/nbproject/project.properties
+++ b/platform/libs.jna.platform/nbproject/project.properties
@@ -17,5 +17,5 @@
 
 is.autoload=true
 javac.source=1.6
-release.external/jna-platform-5.9.0.jar=modules/ext/jna-platform-5.9.0.jar
+release.external/jna-platform-5.10.0.jar=modules/ext/jna-platform-5.10.0.jar
 sigtest.gen.fail.on.error=false
diff --git a/platform/libs.jna.platform/nbproject/project.xml b/platform/libs.jna.platform/nbproject/project.xml
index c7fc274277..07d114bf37 100644
--- a/platform/libs.jna.platform/nbproject/project.xml
+++ b/platform/libs.jna.platform/nbproject/project.xml
@@ -47,8 +47,8 @@
                 <package>com.sun.jna.platform.wince</package>
             </public-packages>
             <class-path-extension>
-                <runtime-relative-path>ext/jna-platform-5.9.0.jar</runtime-relative-path>
-                <binary-origin>external/jna-platform-5.9.0.jar</binary-origin>
+                <runtime-relative-path>ext/jna-platform-5.10.0.jar</runtime-relative-path>
+                <binary-origin>external/jna-platform-5.10.0.jar</binary-origin>
             </class-path-extension>
         </data>
     </configuration>
diff --git a/platform/libs.jna/external/binaries-list b/platform/libs.jna/external/binaries-list
index ab565afa1b..47981ab434 100644
--- a/platform/libs.jna/external/binaries-list
+++ b/platform/libs.jna/external/binaries-list
@@ -15,4 +15,4 @@
 # specific language governing permissions and limitations
 # under the License.
 
-8F503E6D9B500CEFF299052D6BE75B38C7257758 net.java.dev.jna:jna:5.9.0
+7CF4C87DD802DB50721DB66947AA237D7AD09418 net.java.dev.jna:jna:5.10.0
diff --git a/platform/libs.jna/external/jna-5.9.0-license.txt b/platform/libs.jna/external/jna-5.10.0-license.txt
similarity index 99%
rename from platform/libs.jna/external/jna-5.9.0-license.txt
rename to platform/libs.jna/external/jna-5.10.0-license.txt
index 7ab5d5838b..93f6f38469 100644
--- a/platform/libs.jna/external/jna-5.9.0-license.txt
+++ b/platform/libs.jna/external/jna-5.10.0-license.txt
@@ -1,5 +1,5 @@
 Name: Java Native Access
-Version: 5.9.0
+Version: 5.10.0
 License: Apache-2.0
 Description: Dynamically access native libraries from Java without JNI.
 Origin: Java Native Access
diff --git a/platform/libs.jna/manifest.mf b/platform/libs.jna/manifest.mf
index 268c326a7f..dfc5d87c52 100644
--- a/platform/libs.jna/manifest.mf
+++ b/platform/libs.jna/manifest.mf
@@ -4,4 +4,4 @@ OpenIDE-Module: org.netbeans.libs.jna/2
 OpenIDE-Module-Install: org/netbeans/libs/jna/Installer.class
 OpenIDE-Module-Localizing-Bundle: org/netbeans/libs/jna/Bundle.properties
 AutoUpdate-Essential-Module: true
-OpenIDE-Module-Specification-Version: 2.7
+OpenIDE-Module-Specification-Version: 2.9
diff --git a/platform/libs.jna/nbproject/org-netbeans-libs-jna.sig b/platform/libs.jna/nbproject/org-netbeans-libs-jna.sig
index 2fda185287..a5ad4f21b8 100644
--- a/platform/libs.jna/nbproject/org-netbeans-libs-jna.sig
+++ b/platform/libs.jna/nbproject/org-netbeans-libs-jna.sig
@@ -269,7 +269,7 @@ fld public final static int POINTER_SIZE
 fld public final static int SIZE_T_SIZE
 fld public final static int WCHAR_SIZE
 fld public final static java.lang.String DEFAULT_ENCODING
-fld public final static java.lang.String VERSION = "5.9.0"
+fld public final static java.lang.String VERSION = "5.10.0"
 fld public final static java.lang.String VERSION_NATIVE = "6.1.1"
 fld public final static java.nio.charset.Charset DEFAULT_CHARSET
 innr public abstract interface static ffi_callback
diff --git a/platform/libs.jna/nbproject/project.properties b/platform/libs.jna/nbproject/project.properties
index a7efb245d5..4d01926a66 100644
--- a/platform/libs.jna/nbproject/project.properties
+++ b/platform/libs.jna/nbproject/project.properties
@@ -16,16 +16,16 @@
 # under the License.
 
 javac.source=1.6
-release.external/jna-5.9.0.jar=modules/ext/jna-5.9.0.jar
+release.external/jna-5.10.0.jar=modules/ext/jna-5.10.0.jar
 # Do not forget to rename native libs being extracted from the JAR when upgrading the JNA library, and patch org.netbeans.libs.jna.Installer as well.
-release.external/jna-5.9.0.jar!/com/sun/jna/darwin-x86-64/libjnidispatch.jnilib=modules/lib/x86_64/libjnidispatch-nb.jnilib
-release.external/jna-5.9.0.jar!/com/sun/jna/darwin-aarch64/libjnidispatch.jnilib=modules/lib/aarch64/libjnidispatch-nb.jnilib
-release.external/jna-5.9.0.jar!/com/sun/jna/linux-x86-64/libjnidispatch.so=modules/lib/amd64/linux/libjnidispatch-nb.so
-release.external/jna-5.9.0.jar!/com/sun/jna/linux-x86/libjnidispatch.so=modules/lib/i386/linux/libjnidispatch-nb.so
-release.external/jna-5.9.0.jar!/com/sun/jna/linux-aarch64/libjnidispatch.so=modules/lib/aarch64/linux/libjnidispatch-nb.so
-release.external/jna-5.9.0.jar!/com/sun/jna/win32-x86-64/jnidispatch.dll=modules/lib/amd64/jnidispatch-nb.dll
-release.external/jna-5.9.0.jar!/com/sun/jna/win32-x86/jnidispatch.dll=modules/lib/x86/jnidispatch-nb.dll
-release.external/jna-5.9.0.jar!/com/sun/jna/win32-aarch64/jnidispatch.dll=modules/lib/aarch64/jnidispatch-nb.dll
+release.external/jna-5.10.0.jar!/com/sun/jna/darwin-x86-64/libjnidispatch.jnilib=modules/lib/x86_64/libjnidispatch-nb.jnilib
+release.external/jna-5.10.0.jar!/com/sun/jna/darwin-aarch64/libjnidispatch.jnilib=modules/lib/aarch64/libjnidispatch-nb.jnilib
+release.external/jna-5.10.0.jar!/com/sun/jna/linux-x86-64/libjnidispatch.so=modules/lib/amd64/linux/libjnidispatch-nb.so
+release.external/jna-5.10.0.jar!/com/sun/jna/linux-x86/libjnidispatch.so=modules/lib/i386/linux/libjnidispatch-nb.so
+release.external/jna-5.10.0.jar!/com/sun/jna/linux-aarch64/libjnidispatch.so=modules/lib/aarch64/linux/libjnidispatch-nb.so
+release.external/jna-5.10.0.jar!/com/sun/jna/win32-x86-64/jnidispatch.dll=modules/lib/amd64/jnidispatch-nb.dll
+release.external/jna-5.10.0.jar!/com/sun/jna/win32-x86/jnidispatch.dll=modules/lib/x86/jnidispatch-nb.dll
+release.external/jna-5.10.0.jar!/com/sun/jna/win32-aarch64/jnidispatch.dll=modules/lib/aarch64/jnidispatch-nb.dll
 jnlp.verify.excludes=\
     modules/lib/amd64/jnidispatch-nb.dll,\
     modules/lib/x86/jnidispatch-nb.dll,\
diff --git a/platform/libs.jna/nbproject/project.xml b/platform/libs.jna/nbproject/project.xml
index b72dff6ae4..20f5f4e84f 100644
--- a/platform/libs.jna/nbproject/project.xml
+++ b/platform/libs.jna/nbproject/project.xml
@@ -48,8 +48,8 @@
                 <package>com.sun.jna.win32</package>
             </public-packages>
             <class-path-extension>
-                <runtime-relative-path>ext/jna-5.9.0.jar</runtime-relative-path>
-                <binary-origin>external/jna-5.9.0.jar</binary-origin>
+                <runtime-relative-path>ext/jna-5.10.0.jar</runtime-relative-path>
+                <binary-origin>external/jna-5.10.0.jar</binary-origin>
             </class-path-extension>
         </data>
     </configuration>
EOF
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
index 759f93abb5..b20e00f1a2 100644
--- a/nbbuild/build.properties
+++ b/nbbuild/build.properties
@@ -133,7 +133,6 @@ config.javadoc.stable=\
     api.debugger.jpda,\
     project.ant,\
     project.ant.ui,\
-    api.visual,\
     api.java,\
     api.java.classpath,\
     api.search,\
@@ -167,7 +166,6 @@ config.javadoc.devel=\
     jellytools.platform,\
     jellytools.ide,\
     core.multitabs,\
-    core.netigso,\
     gradle,\
     gradle.java,\
     o.n.swing.outline,\
diff --git a/nbbuild/cluster.properties b/nbbuild/cluster.properties
index ae56c143d4..2919799d01 100644
--- a/nbbuild/cluster.properties
+++ b/nbbuild/cluster.properties
@@ -206,7 +206,6 @@ nb.cluster.platform=\
         api.scripting,\
         api.search,\
         api.templates,\
-        api.visual,\
         applemenu,\
         autoupdate.cli,\
         autoupdate.services,\
@@ -217,24 +216,19 @@ nb.cluster.platform=\
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
-        janitor,\
         javahelp,\
         junitlib,\
         keyring,\
         keyring.fallback,\
         keyring.impl,\
         lib.uihandler,\
-        libs.batik.read,\
-        libs.felix,\
         libs.flatlaf,\
         libs.javafx,\
         libs.jna,\
@@ -242,7 +236,6 @@ nb.cluster.platform=\
         libs.jsr223,\
         libs.junit4,\
         libs.junit5,\
-        libs.osgi,\
         libs.testng,\
         masterfs,\
         masterfs.linux,\
@@ -257,10 +250,6 @@ nb.cluster.platform=\
         net.java.html.geo,\
         net.java.html.json,\
         net.java.html.sound,\
-        netbinox,\
-        o.apache.commons.codec,\
-        o.apache.commons.io,\
-        o.apache.commons.logging,\
         o.n.core,\
         o.n.html.ko4j,\
         o.n.html.xhr4j,\
@@ -284,7 +273,6 @@ nb.cluster.platform=\
         openide.options,\
         openide.text,\
         openide.util.enumerations,\
-        openide.util.ui.svg,\
         openide.windows,\
         options.api,\
         options.keymap,\
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
SHORT_REV=`git rev-parse --short HEAD`

git clean -fdX
cd nbbuild
ant $OPTS -Dname=platform -Dhg.id=$SHORT_REV rebuild-cluster
ant $OPTS -Dname=harness -Dhg.id=$SHORT_REV rebuild-cluster

zip -r $BUILD_ROOT/$ZIPNAME.zip netbeans

rm -rf netbeans
unzip $BUILD_ROOT/$ZIPNAME.zip
