export MAVEN_CENTRAL_REPO_URL=http://repo1.maven.org/maven2/
export MAVEN_NEXUS_LOCAL_DIR=/tmp/visualvm-nexus
export MAVEN_REPO=/tmp/maven-visualvm
export MAVEN_REPO_URL=file://$MAVEN_REPO
export MAVEN_VERSION=RELEASE139


/Users/thurka/Projects/Source/more-crippled-netbeans/main/nbbuild/netbeans/java/maven/bin/mvn \
  -DrepositoryUrl=$MAVEN_CENTRAL_REPO_URL \
  -DdeployUrl=$MAVEN_REPO_URL \
  -DforcedVersion=$MAVEN_VERSION \
  -DnetbeansInstallDirectory=/Users/thurka/Projects/Source/visualvm.src/visualvm/dist/visualvm \
  -DnetbeansNbmDirectory=/Users/thurka/Projects/Source/netbeans-releases/nbbuild/nbms \
  -DnexusIndexDirectory=$MAVEN_NEXUS_LOCAL_DIR \
  -DskipInstall=true \
  -DgroupIdPrefix=com.sun.tools.visualvm \
  org.codehaus.mojo:nb-repository-plugin:1.2:download org.codehaus.mojo:nb-repository-plugin:1.2:populate
