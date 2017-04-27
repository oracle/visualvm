# VisualVM 1.3.9 sources repository

VisualVM is a visual tool integrating commandline JDK tools and lightweight profiling capabilities. See https://visualvm.github.io for details and downloads.

## Get the tools

Use Apache Ant and Oracle JDK 7 to build VisualVM from this repository.

## Get the sources

First download or clone (and `git checkout release139`) this repository into directory `visualvm`. There are two project suites included:
  * visualvm (`visualvm/visualvm`) - suite for the core VisualVM tool
  * plugins (`visualvm/plugins`) - suite for the VisualVM plugins available in Plugins Center

## Get the dependencies
  
Then download the [NetBeans 8.0.2 platform and profiler binaries](https://github.com/oracle/visualvm/releases/download/1.3.9/nb802_visualvm_02102016.zip) and extract them into directory `visualvm/visualvm` (should create `visualvm/visualvm/netbeans`).

## Build and run VisualVM tool

To build VisualVM, use `ant build-zip` command in the `visualvm/visualvm` directory. To run VisualVM, use `ant run` command in the `visualvm/visualvm` directory.

## Build and run VisualVM plugins

To build or run the plugins suite, use `ant build` or `ant run` in the `visualvm/plugins` directory. This will automatically build the zip distribution of the core VisualVM tool into `visualvm/visualvm/dist/visualvm.zip` and extract it into the `visualvm/plugins/visualvm` directory. After that the build of the plugins suite continues to build each of the individual plugins. Running the plugins suite means starting VisualVM with all the plugins installed.
