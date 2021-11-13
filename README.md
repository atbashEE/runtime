# Atbash Runtime

Goal: A small modular Jakarta EE Core Profile runtime.


Details:

- Modular, start only what needed or repackage runtime to a minimum size.
- Support Jakarta EE 10 Core Profile
- Cloud Native
- Extensible
- DevOps in mind (configure, monitor, ...)
- Runtime and Server mode

Current status

version 0.1

Proof of Concept

- Integrate Jetty, Jersey, and Weld.
- Based on Jakarta EE 9.1

Any Jakarta EE 9.1 based application using Servlet, REST, CDI, JSON-P, and/or JSON-B can be run using.
`java -jar runtime-main.jar <path to war>`


