# VisualVM for VS Code

This extension integrates the [VisualVM](https://visualvm.github.io) monitoring and troubleshooting tool into Visual Studio Code (VS Code).

## Features
* Easy installation of VisualVM.
* When starting an application from VS Code:
  - The application process is configured to display its folder name in VisualVM.
  - The application process PID is detected to invoke VisualVM actions when needed.
  - When started, the application process can be automatically opened in VisualVM.
* Shortcuts for VisualVM actions such as Thread dump, Heap dump, Start sampling and Start flight recording are available in a dedicated view within VS Code.
* The CPU Sampler filter can be automatically configured to include only application classes.
* Two-way integration: the Go to Source action in VisualVM opens the source code in VS Code.

## Requirements

Install the following in order to use this extension:
* VisualVM 2.1+ (we recommend using the latest version of VisualVM; you can [install directly from within VS Code](#configuring-visualvm)).
* Any JDK 8+ to run VisualVM and detect running processes using `jps`.

Either of these Java language servers must be installed to integrate VisualVM with application startup, to support application class filtering, and to provide the Go to Source feature:

* [Language Server for Java by Apache NetBeans](https://marketplace.visualstudio.com/items?itemName=ASF.apache-netbeans-java)
* [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

The extension also tightly integrates with the [Tools for Micronaut® framework](https://marketplace.visualstudio.com/items?itemName=oracle-labs-graalvm.micronaut-tools) to provide the best monitoring and profiling experience. For example:
* The VisualVM view can be easily displayed in the Micronaut Tools activity.
* CPU Sampler is configured for the selected Graal Cloud Native (GCN) application subproject.
* The Go to Source feature is configured for the selected GCN application subproject.

## Quick Start
Follow these steps to start using VisualVM with VS Code:

1. Install the **VisualVM for VS Code** extension.
2. Install **Language Server for Java by Apache NetBeans** or **Extension Pack for Java** (or both).
3. Open your application sources. Make sure it is configured correctly before integrating it with VisualVM.
4. Click **Download Latest VisualVM** in the VisualVM view and complete the required steps.
5. Invoke the **Configure** action displayed for the *When started* node in the *VisualVM* view, and select **Open Process**.
6. Start the application using the **Run Without Debugging** or **Start Debugging** action.

The application starts and its process is opened in VisualVM and displayed using the name of the source code folder within VS Code.

The Go to Source action in the VisualVM views, such as the Heap Viewer, CPU or Memory Sampler/Profiler results, opens the associated source code within VS Code.

## Configuring VisualVM

To download the latest VisualVM release from [https://visualvm.github.io](https://visualvm.github.io), use the **Download Latest VisualVM** action. 
Depending on the host OS, this will either download a ZIP archive or a macOS disk image (.dmg) file. 
The ZIP archive will be automatically extracted and registered for the extension. 
The macOS disk image must be installed and registered manually (see the next paragraph).

If an existing local VisualVM installation is already available on the system, or after manually installing a macOS disk image, use the **Select Local VisualVM Installation** action and point to the VisualVM installation directory. 
The extension supports **VisualVM 2.1+**, or the corresponding GraalVM component (select the GraalVM installation directory). 
We recommend using the latest version of VisualVM.

To manually register an existing local VisualVM installation, or to configure a specific VisualVM installation for the workspace, use the `visualvm.installation.visualvmPath` property to point to the VisualVM installation directory.

Custom VisualVM startup parameters (such as `--userdir`) including VM arguments (such as `-Xmx...`) can be defined using the `visualvm.startup.visualvmParameters` property.

By default, VisualVM runs using a defined/automatically found local JDK (see the [Configuring a JDK](#configuring-a-jdk) section). 
To define a custom JDK for running VisualVM, set the `visualvm.startup.useJdkPathForVisualvm` property to `false` and use the `--jdkhome` VisualVM startup parameter.

## Configuring a JDK

The extension requires a JDK for:

* Running VisualVM (if not disabled, see the [Configuring VisualVM](#configuring-visualvm) section).
* To detect locally running processes using the `jps` utility, either when starting an application, or for a manual process selection.
* For configuring the JDK source roots used by the `Go to Source` feature.

The extension can use any JDK to run the VisualVM tool and detect Java processes. 
However, it should match the JDK running a monitored application in order to correctly support Go to Source for the JDK classes. 
The JDK is located by searching through the following properties in order:
* Properties: `netbeans.jdkhome`, `java.jdt.ls.java.home`, `java.home`, `graalvm.home`.
* Environment variables: `JDK_HOME`, `JAVA_HOME`.

To override the JDK that was selected automatically, or to configure a specific JDK for the workspace, set the `visualvm.java.jdkPath` property to point to a local JDK installation directory (either in the VS Code Settings view, or manually in _settings.json_).

## VisualVM Actions in VS Code

### Start VisualVM
* *Start VisualVM* (action in the VisualVM view toolbar) starts the VisualVM, or brings its window to the front if it is already running.

### Process Node
* *Select Process*: Shows a list of all running processes available for monitoring, excluding those already being monitored.

* *Show in VisualVM*: Opens the currently selected process in VisualVM, and preselects the defined view. Use the `visualvm.behavior.preselectProcessView` property to define the view to be preselected (use `Current` for no change).

* *Stop Monitoring*: Clears the currently selected process, but does not stop the process or its monitoring in the VisualVM tool.

### When Started Node
* *Configure*: Defines the action to be taken when a new application process is started by VS Code. When configured, the process can be automatically opened in VisualVM, and a preconfigured sampling or flight recording session can be started.

  > Note: The *When started* node is only displayed if the automatic application process selection is enabled (`visualvm.integration.automaticallySelectProjectProcess` property is set to `true`), and no process has been selected yet, or it has not been selected manually using the *Select Process* action.

### Thread Dump Node
* *Take Thread Dump*: Takes a thread dump from a monitored process, and selects its view in VisualVM.

### Heap Dump Node
* *Take Heap Dump*: Takes a heap dump from a monitored process, and selects its view in VisualVM.

### CPU Sampler Node
* *Start CPU Sampling*: Starts a new CPU sampling session for a monitored process, and selects its view in VisualVM. The sampling session settings are defined by the *Filter* and *Sampling rate* subnodes.

* *Take Snapshot of Sampler Results*: Takes a snapshot of the collected data, and selects its view in VisualVM.

* *Stop Sampling*: Terminates the current sampling session, and selects its view in VisualVM.

### Memory Sampler Node
* *Start Memory Sampling*: Starts a new memory sampling session for the monitored process, and selects its view in VisualVM. The sampling session settings are defined by the *Sampling rate* subnode.

* *Take Snapshot of Sampler Results*: Takes a snapshot of the collected data, and selects its view in VisualVM.

* *Stop Sampling*: Terminates the current sampling session, and selects its view in VisualVM.

### JFR Node
* *Start Flight Recording*: Starts a new flight recording for a monitored process. The flight recorder preset to be used for the recording is defined by the *Settings* subnode.

* *Dump Flight Recording Data*: Dumps the data for the current flight recording, and selects its view in VisualVM.

* *Stop Flight Recording*: Terminates the current flight recording.

## Monitoring Multiple Processes Simultaneously

Whilst monitoring multiple processes concurrently is not a typical scenario, it is supported by the VisualVM Integration extension.

To start monitoring another process using VisualVM, start another application process using the *Run Without Debugging* or *Start Debugging* action, or use the **VisualVM: Select Process** command from the Command Palette.

> Note: The *When started* node is not available for a subsequent monitored process, and its respective *Process* node is removed immediately after monitoring of the process has been stopped.

## Troubleshooting

The VisualVM for VS Code extension customizes the way VS Code runs an application by adding extra VM arguments to the launch configuration. 
If the application process fails to start, it may be required to disable these customizations by setting the following properties to `false`:

* `visualvm.integration.automaticallySelectProjectProcess`
* `visualvm.integration.customizeDisplayNameForProjectProcess`

The extension also controls the VisualVM startup parameters. 
If the VisualVM fails to start, disable or tweak the following properties:

* `visualvm.startup.visualvmParameters`
* `visualvm.startup.useJdkPathForVisualvm`
* `visualvm.integration.enableGoToSource`

In case VisualVM fails to open source code using the `Go to Source` action in VS Code, or it opens another VS Code window, configure the following property:

* `visualvm.integration.visualStudioCodeParameters`

For detailed analysis of any issues encountered when using the extension, see the *VisualVM for VS Code* log in the VS Code *Output* view. 
Additionally, see the logs of any other extensions involved, or the *Extension Host* log.

For VisualVM specific troubleshooting, refer to the [VisualVM Troubleshooting Guide](https://visualvm.github.io/troubleshooting.html).


## Settings

| Name | Description | Default Value |
|---|---|---|
| `visualvm.java.jdkPath` | Path to a local JDK installation directory (leave empty to find automatically) |  |
| `visualvm.startup.useJdkPathForVisualvm` | Use a defined/automatically found local JDK installation to run VisualVM (not applicable if the selected VisualVM installation is a GraalVM component) | `true` |
| `visualvm.installation.visualvmPath` | Path to a local VisualVM 2.1+ installation directory (we recommend using the latest version of VisualVM) |  |
| `visualvm.startup.visualvmParameters` | Optional parameters for starting VisualVM (`--userdir`, `-J-Xmx`, and so on) |  |
| `visualvm.behavior.visualvmWindowToFront` | Bring a VisualVM window to front when a VisualVM action is invoked from within VS Code | `true` |
| `visualvm.behavior.preselectProcessView` | Preselected view for a process shown in VisualVM (either the Show in VisualVM action, or the Open Process action when started) | Monitor |
| `visualvm.integration.automaticallySelectProjectProcess` | Automatically select a started application process for monitoring | `true` |
| `visualvm.integration.customizeDisplayNameForProjectProcess` | Configure a started application process to display its folder name in VisualVM | `true` |
| `visualvm.integration.enableGoToSource` | Enable opening source code from VisualVM results in VS Code using the Go to Source action | `true` |
| `visualvm.integration.visualStudioCodeParameters` | Optional parameters for invoking VS Code launcher to open source code from VisualVM (`--user-data-dir`, `--extensions-dir`, and so on) |  |

## Provide Feedback or Seek Help

* [Request a feature](https://github.com/oracle/visualvm/issues/new?labels=enhancement)
* [File a bug](https://github.com/oracle/visualvm/issues/new?labels=bug)

## Contributing

We highly appreciate any feedback! Please let us know your ideas, missing features, or bugs found. Either [file a RFE/bug](https://github.com/oracle/visualvm/issues/new/choose) or [leave us a message](https://visualvm.github.io/feedback.html). For legal reasons, we cannot accept external pull requests. See 
[CONTRIBUTING](./CONTRIBUTING.md)
for details.

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

## License

Copyright (c) 2017, 2024 Oracle and/or its affiliates.
Released under the GNU General Public License, version 2, with the Classpath Exception.

## Release Notes

See the [CHANGELOG](CHANGELOG.md).