#!/bin/bash

REV=1132e7bb80b0b3560
REV_L10N=1180ea1ceb30
ZIPNAME=nb90_visualvm_`date "+%d%m%Y"`

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
  hg clone http://hg.netbeans.org/releases/l10n/
  cd l10n
fi

hg up -C $REV_L10N
hg status
cd ..

git checkout -f $REV
git status

PROFILER_LIGHT=-Dnb.cluster.profiler.depends=nb.cluster.platform
PROFILER_LIGHT="$PROFILER_LIGHT -Dnb.cluster.profiler=\
lib.profiler,\
lib.profiler.charts,\
lib.profiler.common,\
lib.profiler.ui,\
profiler,\
profiler.api,\
profiler.attach,\
profiler.heapwalker,\
profiler.oql,\
profiler.snaptracer,\
profiler.utilities"
PROFILER_LIGHT="$PROFILER_LIGHT -Dvalidation.nb.cluster.profiler=profiler"

OPTS=-Dbuild.compiler.debuglevel=source,lines

cd nbbuild
ant clean
ant $OPTS -Dname=platform rebuild-cluster
ant $OPTS -Dname=harness rebuild-cluster
ant $OPTS -Dname=profiler rebuild-cluster $PROFILER_LIGHT

# cleanup remote packs and cvm support
rm -rf netbeans/ide/
rm -rf netbeans/profiler/remote-pack-defs/
rm -rf netbeans/profiler/lib/deployed/cvm/
rm -f netbeans/profiler/lib/jfluid-server-cvm.jar

zip -r $BUILD_ROOT/$ZIPNAME.zip netbeans

ant clean
ant $OPTS -Dlocales=ja,zh_CN -Dname=platform rebuild-cluster
ant $OPTS -Dlocales=ja,zh_CN -Dname=harness rebuild-cluster
ant $OPTS -Dlocales=ja,zh_CN -Dname=profiler rebuild-cluster $PROFILER_LIGHT

# cleanup remote packs and cvm support
rm -rf netbeans/ide/
rm -rf netbeans/profiler/remote-pack-defs/
rm -rf netbeans/profiler/lib/deployed/cvm/
rm -f netbeans/profiler/lib/jfluid-server-cvm.jar

zip -r $BUILD_ROOT/$ZIPNAME-ml.zip netbeans

rm -rf netbeans
unzip $BUILD_ROOT/$ZIPNAME.zip
