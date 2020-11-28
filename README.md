# mini-platform

This project aims to show a real case example of a microservices platform (small) that is heavily based in monix and it's subprojects.

The platform is composed by three application, the master, slave and archiver. (see monix-mini-platform.png) 

1 - Master: The core of the platform is master server, which expect data to be sent through some external http endpoints. 
	It also implements a grpc server that expects manifest requests to be sent from the slaves (this allows to dynamically discover new slaves being added to the plafotm).
	The master does not implement any access to the main mongodb database, instead he ask the slaves to handle those read and write operations.
	So it also act as a grpc client that sends fetch and persist request to the slaves. 
	It will implement an internal grpc load balancer.
	If a write to the master failed or took more than X time, the master will cache that request to redis temporarly, 
	and there will be one scheduled job that checks if there is cached data, and if so it will try to send it to one of the slaves again.
	At the end of each request, we will send to the kafka `long_term_storage` topic.

2 - Slaves: Can be added on demand, they will send a manifest grpc call to the master once they are added.
			Also implement a grpc stub that expects calls from the master to fetch and persist data.

3 - Archiver: A service that reads from the `long_term_storage` kafka topic, and buffers till X events have been consumed, then it uploads them to S3.
