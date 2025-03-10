# Server

Oscal CLI server (to avoid JVM warmup)

## Building

The project uses a Makefile to manage the build process. Here are the common commands:

### Initialize Submodules

```
make init-submodules
```

### Run Tests

```
make test
```

### Compile the Project

```
make compile
```

### Package the Application

```
make package
```

### Specifying OSCAL Version

You can specify a custom OSCAL Git repository and version (branch, tag, or commit hash) when building:

```
make package OSCAL_REPO=https://github.com/usnistgov/OSCAL.git OSCAL_VERSION=v1.1.0
```

This allows you to build against a specific version of OSCAL, which can be useful for testing or when you need to use features from a specific OSCAL release.

### Run the Application

```
make compile
mvn exec:java
```

### Other Available Commands

Run `make help` to see all available commands.

## Help

* [Vert.x Documentation](https://vertx.io/docs/)
* [Vert.x Stack Overflow](https://stackoverflow.com/questions/tagged/vert.x?sort=newest&pageSize=15)
* [Vert.x User Group](https://groups.google.com/forum/?fromgroups#!forum/vertx)
* [Vert.x Discord](https://discord.gg/6ry7aqPWXy)
