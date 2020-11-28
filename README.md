# mini-platform

This project aims to show a real case example of a mini microservices platform that is heavily based in monix and its subprojects.

The platform is composed by three applications, the master, slave and archiver. (see monix-mini-platform.png) 

1 - **Master**: The core of the platform is the master server, which expect data to be sent through some external http endpoints. 
	It also implements a grpc server that expects manifest requests to be sent from the slaves, allowing to dynamically discover new slaves being added to the plafotm.
	The master does not implement any access to the main mongodb database, instead he ask the slaves to handle those read and write operations.
	So it also acts as a grpc client that sends _fetch_ and _persist_ request to the slaves. 
	It will implement an internal grpc load balancer (i.e using Round Robin) to alternate requests to the different _slaves_.
	If a write to the master failed or took more than X time, the master will cache that event to redis temporarly, 
	and there will be a scheduled job that checks if there is cached data, and if so, it will try to send them to one of the slaves again.
	At the end of each request, we will send to the kafka `long_term_storage` topic.

2 - **Slaves**: Can be added on demand, they will send a manifest grpc call to the master once they are added.
			Also implement a grpc stub that expects calls from the master to fetch and persist data.

3 - **Archiver**: A service that reads from the `long_term_storage` kafka topic, and buffers till X events have been consumed, then it uploads them to S3.
