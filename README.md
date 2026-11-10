# Payara Community Tools for IntelliJ IDEA

[![License](https://img.shields.io/badge/License-EPL%202.0-red.svg)](http://www.eclipse.org/legal/epl-2.0)

### Development

The plugin targets IntelliJ IDEA 2026.3 (builds `263.7307.200` through `263.*`).
The SDK and Plugin Verifier target are pinned to the stable `2026.3` release in
`gradle.properties`. IntelliJ IDEA 2025.3 and later use the unified distribution.

Install JDK 25 and select it as `JAVA_HOME` and the Gradle JVM in IntelliJ IDEA.
Use the included Gradle 9.9.0 wrapper:

```sh
./gradlew clean check buildPlugin verifyPlugin
./gradlew runIde
```

The installable ZIP is generated in `build/distributions/`. Compatibility reports
are written to `build/reports/pluginVerifier/`. To debug the sandbox IDE, run
`./gradlew runIde --debug-jvm` and attach a debugger to localhost port 5005.
Application run configurations select their own JDK independently of the plugin's
build toolchain.

The Jenkins agent requires a JDK installation named `zulu-25` and uses the wrapper
to run the same checks. SpotBugs findings are reported in
`build/reports/spotbugs/`; they retain the existing non-blocking policy.

### Ecosystem Plugin Support
Support for the Payara IntelliJ Community Tools plugin is handled in the [Ecosystem Support Repository](https://github.com/payara/ecosystem-support)

### IntelliJ IDEA Payara Plugin Documentation
Full documentation for the configuration and usage of the Payara IntelliJ Community Tools plugin can be found in the [technical documentation](https://docs.payara.fish/community/docs/Technical%20Documentation/Ecosystem/IDE%20Integration/IntelliJ%20Plugin/Overview.html)
