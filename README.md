# Trading Order Matching Engine

## Overview
A Java-based trading order matching engine that simulates real-world stock exchange behavior using price-time priority.

## Features
- Buy/Sell order matching
- Partial order execution
- Thread-safe processing using PriorityBlockingQueue
- REST API using Spring Boot
- Logging and error handling

## Tech Stack
- Java
- Spring Boot
- Data Structures (Heap / Priority Queue)
- Multithreading (Concurrency)

## API Endpoint
POST /api/orders

Sample Request:
{
  "id": "1",
  "price": 100,
  "quantity": 10,
  "type": "BUY"
}

## Future Improvements
- WebSocket real-time updates
- Database integration
- UI dashboard
