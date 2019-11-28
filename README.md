# MySql testcontainers exmaple

Technologies used:
 - Spring 2
 - MySql
 - Docker
 - Testcontainers
 - RestAssured

This is the example of MySql running in testcontainers for tests.

*Note: for the PostgreSql see tutorial at: http://localhost:4000/spring-boot-integration-testing-done-right/*

It is a showcase for leveraging the power of Docker, Testcontainers and RestAssured to properly Integration Test a Spring Boot 2 Application.

As an example it is running an IntegrationTest which is bootstrapping a MySql Docker Container.

To run app in docker run docker mysql with this command: 
```
docker run -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=employeedb -p 3306:3306 -d mysql
```

Test starts container by itself.