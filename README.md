# VisualVM (2.1.9) sources repository

VisualVM is a visual tool integrating commandline JDK tools and lightweight profiling capabilities. See https://visualvm.github.io for details, downloads and documentation.

## Get the tools

Use Apache Ant 1.9.15 or above and Oracle JDK 8 to build VisualVM from this repository.

## Get the sources

First download or clone this repository into directory `visualvm`. There are two project suites included:
  * visualvm (`visualvm/visualvm`) - suite for the core VisualVM tool
  * plugins (`visualvm/plugins`) - suite for the VisualVM plugins available in Plugins Center

## Configure the dependencies

Then download and extract the [NetBeans Platform 19](https://github.com/oracle/visualvm/releases/download/2.1.8/nb190_platform_20231030.zip) into directory `visualvm/visualvm` (should create `visualvm/visualvm/netbeans`).

## How to build

To build VisualVM, use `ant build-zip` command in the `visualvm/visualvm` directory. 

## How to run

To run VisualVM, use `ant run` command in the `visualvm/visualvm` directory.

## Build and run plugins

To build or run the plugins suite, use `ant build` or `ant run` in the `visualvm/plugins` directory. This will automatically build the zip distribution of the core VisualVM tool into `visualvm/visualvm/dist/visualvm.zip` and extract it into the `visualvm/plugins/visualvm` directory. After that the build of the plugins suite continues to build each of the individual plugins. Running the plugins suite means starting VisualVM with all the plugins installed.

## Generate the Maven artifacts

First prepare the binaries:

  1. Build VisualVM tool as described above
  2. Expand/decompress the generated `visualvm.zip` file in `visualvm/dist`.
  3. Generate the NBMs by running: `ant nbms`. This will generate a folder `build/updates` containing all the NBMs.

To generate the artifacts use [`org.apache.netbeans.utilities:nb-repository-plugin`](https://bits.netbeans.org/mavenutilities/nb-repository-plugin/index.html). Make sure the current directory is still `visualvm/visualvm`.

To install the artifacts into your local repository use the following command:

```
mvn \
-DnetbeansInstallDirectory=dist/visualvm   \
-DnetbeansNbmDirectory=build/updates   \
-DgroupIdPrefix=org.graalvm.visualvm  \
-DforcedVersion=RELEASE219   \
org.apache.netbeans.utilities:nb-repository-plugin:populate
```

To publish the artifacts into a remote repository use the following command:

```
mvn
-DnetbeansInstallDirectory=dist/visualvm   \
-DnetbeansNbmDirectory=build/updates   \
-DgroupIdPrefix=org.graalvm.visualvm  \
-DforcedVersion=RELEASE219   \
-DdeployUrl=<URL to the remote repo> \
-DdeployId=<repository id referenced in your settings.xml>   \
-DskipInstall=true  \
org.apache.netbeans.utilities:nb-repository-plugin:populate
```

For more information about `nb-repository-plugin` see https://bits.netbeans.org/mavenutilities/nb-repository-plugin/index.html

## Contributing

We highly appreciate any feedback! Please let us know your ideas, missing features, or bugs found. Either [file a RFE/bug](https://github.com/oracle/visualvm/issues/new/choose) or [leave us a message](https://visualvm.github.io/feedback.html). For legal reasons, we cannot accept external pull requests. See 
[CONTRIBUTING](./CONTRIBUTING.md)
for details.

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

## License

Copyright (c) 2017, 2023 Oracle and/or its affiliates.
Released under the GNU General Public License, version 2, with the Classpath Exception.
