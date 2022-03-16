# Solution
## Description
---
Project consists of two microservices:

1. **Gateway** <br>
Implements http server with methods <br>
```
POST /wallet       # add btc to wallet
POST /wallet/stats # get by hour statistics
```

2. **Wallet** <br>
Stores by hour statistics in RocksDB


Since performance was in consideration in this task I have chosen **Kafka** as the main source of truth for its scalability. **Gateway** and **Wallet** are connected through **Kafka**.

In more real environment, for example, if we needed to process many wallets at the same time **Gateway** can be horizontally scaled by adding more instances. Requests for the same wallet will be routed to same **Kafka** partition and each partition can be processed by separate **Wallet** instance. Thus very high degree of scalability can be achieved.

Messages in **Kafka** are encoded in **Protobuf** for space optimization and faster encoding/decoding.
Protobufs are located in **Common** module.

I have chosen **RocksDB** for storing state for faster response on queries (no need to re-read partition on server restarts). It's very fast and has lightweight atomic batch writes (transaction are supported too). Each **Wallet** instance has its own **RocksDB** inside.

---

Note: Since it is known that there will be no more than 21mil BTC mined and mininal fracture of BTC is Satoshi (1/100mil of BTC) we can store BTC Amount in Long to avoid problems with floating-point arithmetics and space/speed optimisation <br>

Note: POST /wallet expects datetime to only increase (or be the same, but never decrease. With respect for timezone).

<br>

## Run
---
**sbt**, **Docker**, and **Docker Compose** should be pre-installed.

    sbt "docker:publishLocal" # build images
    docker-compose up         # run cluster
    docker-compose down       # shutdown cluster

---

##### Note 1: If you are having network errors after running **docker-compose-up** try disabling vpn.

##### Note 2: **NixOS** users need to run sbt after running **nix-shell** (see: https://github.com/scalapb/ScalaPB/issues/505)

<br>


## Validate cluster running
---
Get stats for wallet

    curl -X POST '127.0.0.1:8080/wallet/stats' \
    -H 'Content-Type: application/json' \
    -d '{"startDatetime":"2022-03-16T14:10:45+03:00","endDateTime": "2022-03-16T17:15:45+03:00" }'

Response

    [{"datetime":"2022-03-16T12:00:00Z","amount":0},
    {"datetime":"2022-03-16T13:00:00Z","amount":0},
    {"datetime":"2022-03-16T14:00:00Z","amount":0}]

Update amount

    curl -X POST '127.0.0.1:8080/wallet' \
    -H 'Content-Type: application/json' \
    -d '{"datetime":"2022-03-16T16:10:45+03:00","amount": 1000.0 }'

Get stats for wallet

    curl -X POST '127.0.0.1:8080/wallet/stats' \
    -H 'Content-Type: application/json' \
    -d '{"startDatetime":"2022-03-16T14:10:45+03:00","endDateTime": "2022-03-16T17:15:45+03:00" }'

Response

    [{"datetime":"2022-03-16T12:00:00Z","amount":0},
    {"datetime":"2022-03-16T13:00:00Z","amount":1000},
    {"datetime":"2022-03-16T14:00:00Z","amount":1000}]
