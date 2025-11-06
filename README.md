# Supermarket Inventory Tracking System - Tactic Implementation

- Shahmir Khan
- Dan Corcoran
- Tyler Jaafari
- Michael Ogunwale

## Overview

The Heartbeat Tactic is a fault detection mechanism used to ensure that all microservices and distributed store instances within the Supermarket Inventory Tracking System are operational. In this architecture, each store operates as a semi-independent node running its own set of microservices, such as `InventoryService`, `OrderService`, and `NotificationService`, that commincate with the central management system. The heartbeat tactic continuously monitors the availability and health of these nodes to detect and respond to failures in real time.

## Motivation

Because the system spans multiple physical store locations, network connectivity and service reliability can vary. Without active monitoring, a failed store instance could go undetected, leading to:

- Outdated or inconsistent inventory data
- Failed synchronization with central analytics or dashboards
- Inaccurate reporting of product availability across stores

Implementing a heartbeat mechanism ensures early fault detection, allowing the central controller or monitoring service to quickly recognize when a store or microservice has gone offline and initiate recovery procedures.

## Tactic Description

In this system, each distributed store instance and microservice periodically sends a “heartbeat” signal to a central monitoring service known as the Heartbeat Manager. These signals serve as continuous health updates confirming that the node is active and functioning normally.

Instead of the central manager polling every node, each node pushes a lightweight message (often just a timestamp and identifier) at a fixed interval. The Heartbeat Manager maintains a registry of all expected nodes and their last reported timestamps.

If a node fails to send a heartbeat within a predefined timeout window, it is marked as unresponsive or degraded, prompting follow-up actions such as:

- Logging the event for diagnostics
- Sending an alert to administrators
- Initiating automated recovery or restart scripts
- Temporarily routing requests to redundant or cached services

This "push-based" heartbeat strategy minimizes network overhead and scales more efficiently across dozens of store locations, since the central monitor doesn't need to maintain a large number of outgoing requests.

## Workflow

1. Service Initialization: Each store's microservice register themselves with the Heartbeat Manager upon startup
2. Heatbeat Emission: Each service sends a heartbeat message to the manager at a regular, and configurable, interval
3. Timestamp Tracking: The manager records the last received heartbeat per node in a "health" registry
4. Timeout Detection: A scheduled task periodically checks for stale timestamps. Any node exceeding the timeout threshold (or too missed heatbeat emissions) is flagged as unhealthy
5. Recovery Actions: Alerts, restarts, or rerouting logic can be triggered based on severity and predefined policies/logic

## Why This Matters for the SITS

Because each store operates semi-independently and connects over potentially unreliable networks, heartbeat signals provide a simple, consistent way to confirm operational status across all locations.

For example:

- If Store #7 loses network connectivity, its heartbeats stop reaching the central system. Within one or two missed intervals, the Heartbeat Manager detects the issue and flags the store as offline
- Administrators can then investigate or switch analytics/reporting to cached data until the node recovers
- Once connectivity returns, Store #7 resumes heartbeat transmissions, automatically restoring its “healthy” state

## Benefits

- Continuous Availability: Ensures early detection of faults in remote nodes
- Improved Reliability: Enables proactive recovery mechanisms before service degradation
- Operational Transparency: Centralized dashboard provides visibility into all store instances
- Scalability: Works seamlessly as new stores are added; each store simply registers with the Heartbeat Manager

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
