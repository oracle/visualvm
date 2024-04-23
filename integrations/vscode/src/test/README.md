# Proxy setup

In case of network behind the proxy, the following variables must be set:

- `http_proxy` - URL to proxy, incl. protocol and port, e.g. http://acme.com:80
- `no_proxy`   - URL patterns that must not use proxy. In particular, corporate/internal NPM module repositories must be enumerated in no_proxy env var.

Internally (in package.json), the globalAgent/bootstrap is used with `GLOBAL_AGENT_{HTTP,NO}_PROXY`
set to the appropriate env variable. The environment variables `http_proxy` and `no_proxy` are read by npm package manager. 


# Prepare for testing

Ensure that all necessary npm modules are installed. Run
- `npm install`
to update the local node_modules module cache, if any changes were pulled for `package.json`.

You need to compile the VisualVM extension itself, and the test code before launching the tests.
- `npm run compile`
- `npm run pretest`


# Run the tests from the CLI

Tests can be executed by `npm run test`. The test bootstrap will download a separate installation of vscode into `.vscode-test` directory and then duplicated into `output/a vscode-test` to test the space in path of vscode installation. The testing environment will use a **separate** extensions dir (`.vscode-test/extensions`) and user dir (`.vscode-test/user-data`). The tested vscode installation is completely separated from the development one.