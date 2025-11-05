# Heartbeat Implementation - Group 2

- Shahmir Khan
- Andrew Bradbury
- Ryan Yocum

## Overview

This project demonstrates the **Heartbeat** tactic for fault detection of an autonomous self-driving vehicle. The monitored functionality is **Obstacle Detection**, a critical availability-related service. The system ensures that the **Collision Detector process** (simulating obstacle detection) is alive and responsive by requiring it to send heartbeat signals to the **Car Controller process** periodically.  

If heartbeats are missing for a defined interval, the Car Controller flags the failure and takes corrective action.

## Components

### 1. `Camera.java`

- Represents a simple camera module.
- Provides a `isObjectDetected()` method that simulates object/obstacle detection.

### 2. `CollisionDetector.java`

- Acts as the **critical process** that needs monitoring.
- Uses the `Camera` class to simulate object detection events.
- Sends periodic **heartbeats** to the Car Controller.
- Includes **random failure** that causes the module to fail to publish a health signal.
  - The Collision Detector shuts down after failing 3 consecutive health checks.

### 3. `CarController.java`

- Monitors incoming heartbeat messages over a TCP socket.
- Maintains the timestamp of the last received heartbeat.
- If no heartbeat is received within **5 seconds**, it declares the Collision Detector unresponsive and can take further action.
  - For the purposes of this activity, the Car Controller simply stops running once it no longer has a connected client. *For legal purposes, we do not advise that a car stop running if its submodules appear unresponsive.*

## Compiling & Running This Project

### Requirements

- Java 21+
- Standard JDK libraries (no external dependencies)

Navigate to the `src` directory before executing the following commands.

### Compilation

```bash
javac *.java
```

### Running

Start the **CarController**.

```bash
java CarController
```

### Output

```bash
CarController started, waiting for connections...
```

In a second terminal, start the **CollisionDetector**.

```bash
java CollisionDetector
```

Now observe:

- Collision Detector prints object detection and health status.
- Car Controller logs heartbeats.
- If heartbeats stop for 5+ seconds (due to random failure), Car Controller logs:

```bash
No heartbeat received for 5 seconds, taking action!
```

## Libraries Used

- `java.io.PrintWriter` for writing client output to a server
- `java.net` for `ServerSocket` and `Socket`
- `java.util.random`
- `java.util.Scanner` for reading input from a client
- `java.lang.Thread`

## Note

This project is intended for submission to the `Rochester Institute of Technology Software Engineering Department`
