# graaljs-websocket
WebSocket Polyfill for Graal.js to execute [y-websocket server](https://github.com/yjs/y-websocket.git).

### Run

```bash
$ export JAVA_HOME=/jdk-21/
graaljs-websocket$ mvn package exec:exec -DskipTests
```

### Debugging Tests in Chrome

To debug the tests in _Chrome Dev Tools_, one can pass in special `inspectPort` parameter.
Use:
```bash
graaljs-websocket$ mvn test -DinspectPort=34567
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.apidesign.polyfill.websocket.WebSocketPolyfillTest
Debugger listening on ws://127.0.0.1:34567/f_eZx1hQjjybtrg00jYiSjhbF0wXmFIQyehlm9cIR7Y
For help, see: https://www.graalvm.org/tools/chrome-debugger
E.g. in Chrome open: devtools://devtools/bundled/js_app.html?ws=127.0.0.1:34567/f_eZx1hQjjybtrg00jYiSjhbF0wXmFIQyehlm9cIR7
```
Copy the above printed URL to _Chrome browser_ and debug JavaScript code
of the test.

Due to **incompatible changes** in Chrome Debugging protocol one has to use
an older version of _Chrome browser_. Download [109.0.5414.119 version here](https://dl.google.com/linux/chrome/deb/pool/main/g/google-chrome-stable/google-chrome-stable_109.0.5414.119-1_amd64.deb)

### Debugging Application in Chrome

The same `inspectPort` argument is also supported in the normal application
execution. Use:
```bash
graaljs-websocket$ mvn package exec:exec -DskipTests -DinspectPort=34567
```
to debug JavaScript code of the application in _Chrome Dev Tools_.
