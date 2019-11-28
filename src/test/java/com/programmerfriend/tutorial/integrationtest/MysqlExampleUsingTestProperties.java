package com.programmerfriend.tutorial.integrationtest;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.List;
import java.util.function.Consumer;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * This example overrides application.properties with the application-test.properties.
 * In this case we have to duplicate props in the application-test.properties and in the MySql testcontainers creation
 *
 * How it works:
 * 1. Starts MySql testcontainers assigning container to mysql variable
 * 2. Discards application.properties with the annotation AutoConfigureTestDatabase.Replace.NONE
 * 3. Loads application-test.properties
 *    So in this case you NEED application-test.properties. And value there should be the same as in started MySql testcontainer
 * 4. Starts db with the application-test.properties.
 *    As far as the props specified in the MySql testcontainers @ClassRull and application-test.properties are the same,
 *      datasource connects to the MySql in the container.
 * 5. lso we use RANDOM_PORT with extracting it to the baseUrl varriable
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MysqlExampleUsingTestProperties {

  @ClassRule
  public static GenericContainer mysql = new GenericContainer(new ImageFromDockerfile("mysql-employee")
      .withDockerfileFromBuilder(dockerfileBuilder -> {
        dockerfileBuilder.from("mysql:5.7.8")
            .env("MYSQL_ROOT_PASSWORD", "root")
            .env("MYSQL_DATABASE", "employeedb")
            .env("MYSQL_USER", "user")
            .env("MYSQL_PASSWORD", "password")
            .add("a_schema.sql", "/docker-entrypoint-initdb.d")
            .add("b_data.sql", "/docker-entrypoint-initdb.d");
      })
      .withFileFromClasspath("a_schema.sql", "schema.sql")
      .withFileFromClasspath("b_data.sql", "data.sql"))
      .withExposedPorts(3306)
      .withCreateContainerCmdModifier(
          new Consumer<CreateContainerCmd>() {
            @Override
            public void accept(CreateContainerCmd createContainerCmd) {
              createContainerCmd.withPortBindings(new PortBinding(Ports.Binding.bindPort(3306), new ExposedPort(3306)));
            }
          }
      )
      .waitingFor(Wait.forListeningPort());

  @Value("http://localhost:${local.server.port}")
  String baseUrl;

  @Autowired
  EmployeeRepository employeeRepository;

  @Test
  public void contextLoads() {
  }

  @Test
  public void testWriteToDb_afterBoot_shouldHaveEntries() {
    List<Employee> all = employeeRepository.findAll();
    Assertions.assertThat(all.size()).isEqualTo(7);
    Assertions.assertThat(all.get(0).getFirstName()).isEqualTo("George");
    Assertions.assertThat(all.get(0).getLastName()).isEqualTo("Franklin");
  }

  @Test
  public void testGet_returns_200_with_expected_employees() {
    when().
        get(baseUrl + "/employees").
    then()
        .statusCode(200)
        .body("size()", is(7))
        .body("[0].firstName", equalTo("George"))
        .body("[0].lastName", equalTo("Franklin"));
  }
}
