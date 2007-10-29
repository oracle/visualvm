CURDIR=`pwd`
XTEST_HOME=$CURDIR/../../../../xtest
NB_HOME=$CURDIR/../../../../nbbuild/netbeans
echo "XTEST_HOME="$XTEST_HOME
echo "JFLUID_HOME="$JFLUID_HOME
echo "NB_HOME="$NB_HOME

ant runtests -Dxtest.basedir=. -Dxtest.config=generated \
-Dnetbeans.dest.dir=$NB_HOME -Dxtest.source.location=ide \
-Dxtest.home=$XTEST_HOME -Dxtest.distdir=. -Dpermit.jdk6.builds=true \
-Dxtest.profiler.dest.dir=$NB_HOME
