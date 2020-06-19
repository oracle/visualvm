#!/bin/bash

# NetBeans 11.3 FCS
REV=6b879cb782eaa4f13a731aff82eada11289a66f7
REV_L10N=1180ea1ceb30
ZIPNAME=nb113_platform_`date "+%d%m%Y"`

set -e

mkdir -p build/nb/
cd build/nb/
BUILD_ROOT=`pwd`
if [ -e netbeans ]; then
  cd netbeans
  git fetch
else
  git clone https://github.com/apache/incubator-netbeans/ netbeans
  cd netbeans
fi

if [ -e l10n ]; then
  cd l10n
  hg pull 
else
  hg clone https://hg.netbeans.org/releases/l10n/
  cd l10n
fi

hg up -C $REV_L10N
hg status
cd ..

git checkout -f $REV
patch -p1 <<'EOF'
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
diff --git a/nbbuild/cluster.properties b/nbbuild/cluster.properties
index 48b940b88152..b5101f2abe25 100644
--- a/nbbuild/cluster.properties
+++ b/nbbuild/cluster.properties
@@ -218,16 +218,13 @@ nb.cluster.platform=\
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
         libs.felix,\
-        libs.flatlaf,\
         libs.javafx,\
         libs.jna,\
         libs.jna.platform,\
@@ -250,14 +247,9 @@ nb.cluster.platform=\
         net.java.html.json,\
         net.java.html.sound,\
         netbinox,\
-        o.apache.commons.codec,\
-        o.apache.commons.io,\
-        o.apache.commons.logging,\
         o.n.core,\
         o.n.html.ko4j,\
         o.n.html.xhr4j,\
-        o.n.swing.laf.dark,\
-        o.n.swing.laf.flatlaf,\
         o.n.swing.outline,\
         o.n.swing.plaf,\
         o.n.swing.tabcontrol,\
@@ -276,7 +268,6 @@ nb.cluster.platform=\
         openide.options,\
         openide.text,\
         openide.util.enumerations,\
-        openide.util.ui.svg,\
         openide.windows,\
         options.api,\
         options.keymap,\
EOF
git status

OPTS=-Dbuild.compiler.debuglevel=source,lines

git clean -fdX
cd nbbuild
ant $OPTS -Dname=platform rebuild-cluster
ant $OPTS -Dname=harness rebuild-cluster

zip -r $BUILD_ROOT/$ZIPNAME.zip netbeans

git clean -fdX
ant $OPTS -Dlocales=ja,zh_CN -Dname=platform rebuild-cluster
ant $OPTS -Dlocales=ja,zh_CN -Dname=harness rebuild-cluster

zip -r $BUILD_ROOT/$ZIPNAME-ml.zip netbeans

rm -rf netbeans
unzip $BUILD_ROOT/$ZIPNAME.zip
