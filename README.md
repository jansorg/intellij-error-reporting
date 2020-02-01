# Sample plugin to demonstrate error reporting

This repository is demonstrating error reporting. There's a corresponding article at [plugin-dev.com](https://www.plugin-dev.com/intellij/general/error-reporting/).

This repository demonstrates how to implement extension point `errorHandler` to send the exceptions to a Sentry endpoint.

This is how to run it:
```bash
./gradlew runIde
```

