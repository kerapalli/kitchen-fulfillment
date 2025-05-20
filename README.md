# README

Author: `Karthik Erapalli`

## Package Structure

src/main/java/com/css/challenge/
├── Main.java (Entry point)
├── client/
│   ├── Client.java
│   ├── Problem.java
│   └── Simulator.java
├── domain/
│   ├── Action.java 
│   ├── ActionType.java 
│   ├── Order.java 
│   ├── StorageType.java
│   └── Temperature.java
├── exception/
│   ├── InvalidOrderException.java
│   ├── OrderNotFoundException.java
│   ├── StorageFullException.java
├── service/
│   ├── ActionLogger.java (Interface)
│   ├── ActionLoggerImpl.java (Implementation)
│   ├── FreshnessTracker.java (Interface)
│   ├── FreshnessTrackerImpl.java (Implementation)
│   ├── OrderManager.java (Interface)
│   └── OrderManagerImpl.java (Implementation)
├── storage/
│   ├── Kitchen.java (Main storage system)
│   └── StorageUnit.java (Individual storage unit)
│── strategy/
│   ├── CompositeDiscardStrategy.java (Implementation)
│   ├── DiscardStrategy.java (Interface)
│   ├── FreshnessDiscardStrategy.java (Implementation)
│   └──  TemperatureMismatchDiscardStrategy.java (Implementation)



## How to run

The `Dockerfile` defines a self-contained Java/Gradle reference environment.
Build and run the program using [Docker](https://docs.docker.com/get-started/get-docker/):
```
$ docker build -t kitchen-fulfillment .
$ docker run --rm -it kitchen-fulfillment --args="--auth <token>"
```
Eg:-
```
$ docker run --rm -it kitchen-fulfillment --args="--auth fpxgqm73nskp"
```
Feel free to modify the `Dockerfile` as you see fit.

If java `21` or later is installed locally, run the program directly for convenience:
```
$ ./gradlew run --args="--auth=<token>"
```

## Discard criteria

`I choose the CompositeDiscardStrategy as default. The CompositeDiscardStrategy combines two critical factors in determining which order to discard:

- Freshness: It accounts for how close an order is to expiry, prioritizing removal of food that will soon be inedible. 
- Temperature Alignment: It considers whether food is being stored at its ideal temperature, recognizing that mismatched temperature storage accelerates quality degradation.

By considering both factors simultaneously, the strategy optimizes for overall food quality rather than focusing on a single dimension.

When the storage space is limited, the algorithm weighs freshness as the primary factor and applies a penalty multiplier when there is a
temperature mismatch.

This prevents 2 scenarios
- Nearly expired food at the proper temperature is kept while fresher food at improper temperature is discarded.
- Food that's slightly mismatched but very fresh is discarded over properly stored but almost expired food.
`
