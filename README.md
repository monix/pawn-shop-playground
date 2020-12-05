# mini-platform

This project aims to show a real case example of a mini microservices platform that is heavily based in monix and its subprojects.

The platform is composed by the following applications, the master, slave, the provider and archiver. (see monix-mini-platform.png) 

1 - **Master**: The core of the platform is the master server, which expect data to be sent through some external http endpoints. 
	It also implements a grpc server that expects _JoinRequests_ to be sent from the slaves, allowing to dynamically add new to the quorum.
	There is no interaction to the database from the master, instead it asks the slaves to do that work.
	As a bank backend service, we expect _transactions_ and _operations_ to be sent directly from the client applications, 
	    - _Transaction_: It is any money movement between two clients, having then a `sender` and `receiver` of a certain amount of money.  
	    - _Operation_: On the other hand, the operation is the action of either doing a _deposit_ or _withdraw_, and it is 
	     identified by a single client that has to physically be present in one of the branches.
	      
	Back to the technical details, the master also acts as a grpc client that sends _transactions_, _operations_ and _fetch_ requests to
	the slaves. 
	It will implement an internal grpc load balancer (i.e using Round Robin) to alternate requests between the different _slaves_.

2 - **Slaves**: Can be added on demand, they will send a manifest grpc call to the master once they are added.
			Also implement a grpc stub that expects calls from the master to fetch and persist data.

3 - **Archiver**: A service that reads from the `long_term_storage` kafka topic, and buffers till X events have been consumed, then it uploads them to S3.


	
4 - Future plans:
 - If a write to the master failed or took more than X time, the master will cache that event to redis temporarly, 
and there will be a scheduled job that checks if there is cached data, and if so, it will try to send them to one of the slaves again.
- At the end of each request, we will send to the kafka `long_term_storage` topic.