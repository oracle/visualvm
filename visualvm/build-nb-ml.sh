#!/bin/bash

# NetBeans 9.0 FCS
REV=19e5871a24d5b0feeee0d9a195eec2b6be12b444
REV_L10N=1180ea1ceb30
ZIPNAME=nb90_platform_`date "+%d%m%Y"`

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

OPTS=-Dbuild.compiler.debuglevel=source,lines

cd nbbuild
ant clean
ant download-all-extbins
ant $OPTS -Dname=platform rebuild-cluster
ant $OPTS -Dname=harness rebuild-cluster

zip -r $BUILD_ROOT/$ZIPNAME.zip netbeans

ant clean
ant download-all-extbins
ant $OPTS -Dlocales=ja,zh_CN -Dname=platform rebuild-cluster
ant $OPTS -Dlocales=ja,zh_CN -Dname=harness rebuild-cluster

zip -r $BUILD_ROOT/$ZIPNAME-ml.zip netbeans

rm -rf netbeans
unzip $BUILD_ROOT/$ZIPNAME.zip
