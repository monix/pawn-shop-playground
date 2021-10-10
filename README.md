# Pawn shop

⚠️ Work In Progress! ⚠️

This repo aims to be resource and *real example project* for building an application based on microservices by using 
monix and its sub-projects.

### Domain 

The application simulates the very bare logic of a possible backend for an auction shop.
Where it defines an endpoint for adding `Items` and `Actions`, an action being `Buy`, `Sell` or `Pawn`. 
On the other hand it also supports to fetch items with its actions history by the _id_, _name_, _category_ or _state_.

### Architecture

After having introduced the _domain_, let's jum to the technical side. 

The platform is composed in two services, the _dispatcher_ and _worker_, and the communication is done via `gRPC` and `kafka`.

![Mini Platform Diagram](/mini-platform-diagram.png)

## Dispatcher
 The dispatcher represents the data entrypoint (**http**) and also acts like a router to dispatching 
 incoming requests to the available workers (**gRPC**). 
It is characterized for not implementing any database access,
 but rather for organising and orchestrating the workload on the workers.

#### Http

Implements a **http server** that acts as the data entry point of the platform to add and fetch new `items`.
The http routes are pretty minimal:
- POST */item/add*
- POST */item/action/buy*
- POST */item/action/sell*
- POST */item/action/pawn*
- GET */item/fetch/{id}*
- GET */item/fetch?name={name}*
- GET */item/fetch?category={category}*
- GET */item/fetch?state={state}*


#### Grpc 
 - **Server**: The grpc server implementation simply expects `JoinRequest`s from the workers, returning `JoinResponse`s which 
 can be either `Joined` or `Rejected`. This acts like a very basic protocol for allowing to dynamically add new workers to the quorum,
and providing scalability to the platform. 
   Note: Nowadays this could be implemented much more easily by relying on an external discovery service and grpc load balancer.
 
- **Client**: The dispatcher also acts as a grpc client that sends _transactions_, _operations_ and _fetch_ requests to
	its workers. As explained previously, there is an internal grpc load balancer that randomly chooses
  a different worker to.

## Workers



They can be added on demand, scalable depending on the work flow.
The grpc protocol is only designed for requesting (reading) data, on the other hand, 
the data to be written is published into a broker, in which the worker will keep consuming and persisting to the database.

#### Grpc

- **Client**
Just right after starting the app, they will send the _JoinRequest_ grpc call to the dispatcher.
  Then, if the response is a `Joined` it means that the worker was added to the quorum and that it will
  start receiving grpc requests.
  
- **Server**
The grpc server will only start after we have received a `Joined` confirmation from the dispatcher, at that point the worker will be entitled to receive `fetch` requests.
   
#### Kafka
The workers are continuously consuming events from the four different kafka topics (`item`, `buy-actions`, `sell-actions` and `pawn-actions`) that
will be persisted afterwards to its respective collection in `MongoDb`.
As the logic is shared between the four kind of events, it's implementation is generalized in the `InboundFlow` _type-class_.
In order to storing the `protobuf` events directly to `Mongo`, it was required to define some [Codecs](/worker/src/main/scala/monix/mini/platform/worker/mongo/Codecs.scala).  

## Feeder 

A microservice that feeds _Redis_ with a list of _Fraudulent_ people that is downloaded from a datasource stored in _S3_.

	

## Future plans:
 - Write tests and create a CI pipeline.
 - If a write to the dispatcher failed or took more than X time, the dispatcher will cache that event to redis temporarly, 
and there will be a scheduled job that checks if there is cached data, and if so, it will try to send them to one of the worker again.
- At the end of each request, we will send to the kafka `long_term_storage` topic.
- Refactor the dispatcher and worker apps to read the config files in a safe way, as how currently the feeder does, using `monix.bio.IO` that
will and returning `IO[ConfigurationErrors, Config]` instead of using `loadOrThrow`. 
