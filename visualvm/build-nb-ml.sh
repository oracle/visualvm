#!/bin/sh

cd nbbuild
ant -Dname=platform rebuild-cluster
ant -Dname=harness rebuild-cluster
ant -Dname=profiler rebuild-cluster
zip -r /tmp/nb90_visualvm_`date "+%d%m%Y"`.zip netbeans
ant clean
ant -Dlocales=ja,zh_CN -Dname=platform rebuild-cluster
ant -Dlocales=ja,zh_CN -Dname=harness rebuild-cluster
ant -Dlocales=ja,zh_CN -Dname=profiler rebuild-cluster
zip -r /tmp/nb90_visualvm_`date "+%d%m%Y"`-ml.zip netbeans

