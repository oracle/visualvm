# VisualVM (master) sources repository

VisualVM is a visual tool integrating commandline JDK tools and lightweight profiling capabilities. See https://visualvm.github.io for details, downloads and documentation.

## Get the tools

Use Apache Ant 1.9.9 or above and Oracle JDK 8 to build VisualVM from this repository.

## Get the sources

First download or clone this repository into directory `visualvm`. There are two project suites included:
  * visualvm (`visualvm/visualvm`) - suite for the core VisualVM tool
  * plugins (`visualvm/plugins`) - suite for the VisualVM plugins available in Plugins Center

## Configure the dependencies

Then download and extract the [NetBeans Platform 14](https://github.com/oracle/visualvm/releases/download/2.1.5/nb140_platform_20220908.zip) into directory `visualvm/visualvm` (should create `visualvm/visualvm/netbeans`).

## Build and run VisualVM tool

To build VisualVM, use `ant build-zip` command in the `visualvm/visualvm` directory. To run VisualVM, use `ant run` command in the `visualvm/visualvm` directory.

## Build and run VisualVM plugins

To build or run the plugins suite, use `ant build` or `ant run` in the `visualvm/plugins` directory. This will automatically build the zip distribution of the core VisualVM tool into `visualvm/visualvm/dist/visualvm.zip` and extract it into the `visualvm/plugins/visualvm` directory. After that the build of the plugins suite continues to build each of the individual plugins. Running the plugins suite means starting VisualVM with all the plugins installed.

## Contributing
This project is not accepting external contributions at this time. For bugs or enhancement requests, please file a GitHub issue unless it’s security related. When filing a bug remember that the better written the bug is, the more likely it is to be fixed. If you think you’ve found a security vulnerability, do not raise a GitHub issue and follow the instructions in our [security policy](./SECURITY.md).

## Security
Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

## License
Copyright (c) 2017, 2023 Oracle and/or its affiliates.
Released under the GNU General Public License (GPL)
