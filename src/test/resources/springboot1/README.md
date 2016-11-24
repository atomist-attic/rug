Created from template spring-rest-service, version 1.0.2

test1 microservice
===========================

${description}

@project_name

To run locally
--------------

This Spring Boot microservice is driven using Maven. To run locally simply execute the following from the command line:

```shell
> ./mvnw spring-boot:run
```

This will launch the microservice on port 8080 by default.

To run tests
------------

This microservice comes with some rudimentary tests as a good starting point for writing your own. Use the following command to execute the tests using Maven:

```shell
> ./mvnw test
```

To run inside a docker container
--------------------------------

You can build, pacakge and run this microservice using Docker right out of the box. First make sure you have build the microservice locally by executing the following Maven command:

```shell
> ./mvnw package
```

Now you have built the necessary artifacts, the next step is to build your docker image. From a terminal where you have access to Docker, execute the following command:

```shell
> docker build -t my_microservice .
```

You can now run your dockerized microservice with the following command:

```shell
> docker run -name my_microservice -i -t my_microservice
```

History
-------

Originally created by Atomist on 2016-07-12.
