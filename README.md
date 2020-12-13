# Mini Platform

This project aims to show a **real example** of a mini microservices platform in the _financial services domain_ that is heavily _built up_ mostly with in _monix and its subprojects_.

AThe platform entry point is an http endpoint that expects **transaction** and **operation** events to be sent
    - [Transaction](https://github.com/monix/mini-platform/blob/master/common/src/main/protobuf/protocol.proto#L35):
     Represents the client's action of sending a certain amount of money (transfering) to a different client. 
    - [Operation](https://github.com/monix/mini-platform/blob/master/common/src/main/protobuf/protocol.proto#L23):
     Represents the client's action of either doing a *deposit* or *withdraw*, and has to be physically done form one of the branches.
     
From these operation we can identify and register adjacent data, like the **interactions** with different clients when performing a transaction
 or the **branch** where an operation was realized. 
 
Apart of sending events, the http endpoint also exposes get operations to fetch the processed data, providing different alternatives to do so:
Fetch all the data or only (transactions, operations, interactions or branches) of an specific client.

Now that we have already introduced a bit the domain, let's jum to the technical overview and design:

The mini platform is composed by mainly three different components, the _dispatcher_, _worker_ and the _feeder_. 
They represent different type of applications which will be running in docker containers environment have its own purpose.   


![Mini Platform Diagram](/mini-platform-diagram.png)


### Dispatcher
 
 The core of the platform, it represents the http entry point that expects bank transactions/operations events, it is also
 caracterized for not implementing any database access, instead it communicates to the workers via _gRPC_ *proto*cols to let them do the hard work. 
 Grpc server: Implements a grpc endpoint that expects `JoinRequests` to be sent from the workers, returning the `JoinResponse` which 
 can be either `Joined` or `Rejected`. Allowing to dynamically add new worker to the cluster, and providing scalability to the platform.
 Grpc client: The dispatcher also acts as a grpc client that sends _transactions_, _operations_ and _fetch_ requests to
	the worker. It will implement an internal grpc load balancer (i.e using Round Robin) to alternate requests between the existing _slaves_.

### Workers

They can be added on demand, scalable depending on the work flow. 
Just right after starting the app, they will send the _JoinRequest_ grpc call to the dispatcher.
Then, once the Worker is part of the cluster, it will expect calls from the dispatcher to fetch and persist data.
    - Transaction/Operation Events: The different events will be persisted respectively in their _MongoDB_ collections for its later access, 
        but before that, the worker will check in _Redis_ whether the client that the money is being send (in transactions) or the client that 
        performs the withdraw (in operation) is a fraudster or not. In case it is, the event is marked as `Fraudulent` and not persisted in the db.
      In case the client was not identified as a fraudster, the event will be persisted in Mongo and the _interactions_ and _branches_ information in _Redis_ for
      its future faster access.   
    - Fetch: There are defined different http endpoints for retreiving all the data or only the _transactions_, _operations_, _interactions_ or _branches_ of an specific client.


### Feeder 

A microservice that feeds _Redis_ with a list of _Fraudulent_ people that is downloaded from a datasource stored in _S3_.

	

### Future plans:
 - Write tests and create a CI pipeline.
 - If a write to the dispatcher failed or took more than X time, the dispatcher will cache that event to redis temporarly, 
and there will be a scheduled job that checks if there is cached data, and if so, it will try to send them to one of the worker again.
- At the end of each request, we will send to the kafka `long_term_storage` topic.
- Refactor the dispatcher and worker apps to read the config files in a safe way, as how currently the feeder does, using `monix.bio.IO` that
will and returning `IO[ConfigurationErrors, Config]` instead of using `loadOrThrow`. 