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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

/**
 * This example overrides application.properties with the properties provided for testcontainers MySql in the method initialize.
 * For this example we don´t need application-test.properties
 *
 * How it works:
 * 1. Starts MySql testcontainers assigning container to mysql variable
 * 2. Overrides application.properties connection to the db with those from the started MySql testcontainer (see Initializer.initialize())
 *    So in this case you don´t need application-test.properties
 * 3. Starts db with the overridden properties, thus it connects to the MySql in the container
 * 4. Also we use RANDOM_PORT with extracting it to the baseUrl varriable
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {MysqlExampleOverrideMainDbProperties.Initializer.class})
public class MysqlExampleOverrideMainDbProperties {

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

  static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      Map<String, String> envMap = Arrays.stream(mysql.getContainerInfo().getConfig().getEnv())
          .map(envStr -> envStr.split("="))
          .filter(envArr -> envArr.length > 1)
          .collect(Collectors.toMap(envArr -> envArr[0], envArr -> envArr[1]));
      String url = String.format("jdbc:mysql://%s:%s/%s", mysql.getContainerIpAddress(), mysql.getFirstMappedPort(), envMap.get("MYSQL_DATABASE"));

      TestPropertyValues.of(
          "spring.datasource.url=" + url,
          "spring.datasource.username=" + envMap.get("MYSQL_USER"),
          "spring.datasource.password=" + envMap.get("MYSQL_PASSWORD")
      ).applyTo(configurableApplicationContext.getEnvironment());
    }
  }
}
