# Atbash Runtime

Goal: A small modular Jakarta EE Core Profile runtime.

(And a means to learn a lot by performing real world challenges)

Details:

- Modular, start only what needed or repackage runtime to a minimum size.
- Support Jakarta EE 10 Core Profile
- Cloud Native
- Extensible
- DevOps in mind (configure, monitor, ...)
- Runtime and Domain mode
- Running on JDK 11, JDK 17, JDK 18 and JDK 19 ea


Current status

**version 0.4**

Added Features

- Packager for creating custom runtimes.
- Support for _microprofile-config.properties_ files at alternative locations.
- Fix issue for certain values of `@ApplicationPath.value`
- `Sniffer` is no longer a singleton but instantiated for each deployment.

**version 0.3**


Powered By

- Jetty : 11.0.8
- Jersey : 3.0.4
- Weld : 4.0.3
- Picocli : 4.6.3
- Jackson Databind : 2.13.1

- Testcontainers : 1.16.3

Added Features

- Testing framework (based on top of testcontainers)
- Runtime Embedded version
- Arquillian Connector
- Custom MicroProfile Config implementation (passes TCK)
- Improved CDI support
- Major rework of Logging module
- Running configuration commands at startup
- Changed packaging (no more fat jar)
- (internal) Module Manager improvements

**version 0.2**

- Docker image
- Domain mode support (with remote CLI)
- Basic /health endpoint
- Restart runtime with previously deployed applications
- Java Flight Recorder integration

**version 0.1**

Proof of Concept

- Integrate Jetty, Jersey, and Weld.
- Based on Jakarta EE 9.1

Any Jakarta EE 9.1 based application using Servlet, REST, CDI, JSON-P, and/or JSON-B can be run using.
`java -jar runtime-main.jar <path to war>`


