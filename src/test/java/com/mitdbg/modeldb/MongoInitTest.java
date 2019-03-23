package com.mitdbg.modeldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.yaml.snakeyaml.Yaml;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.grpc.inprocess.InProcessServerBuilder;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MongoInitTest {

  public static final Logger LOGGER = Logger.getLogger(IntegrationTest.class.getName());

  public Map<String, Object> readYamlProperties(String fileName) throws IOException {

    // --------------- Start reading properties from config.yaml file --------------------------
    InputStream inputStream = new FileInputStream(fileName);
    Yaml yaml = new Yaml();
    Map<String, Object> prop = (Map<String, Object>) yaml.load(inputStream);
    LOGGER.info("Properties map" + prop);
    // --------------- Finish reading properties from config.yaml file --------------------------

    return prop;
  }

  public MongoClient getMongoClient(Map<String, Object> databasePropMap) {
    Map<String, Object> mongoDBPropMap =
        (Map<String, Object>) databasePropMap.get("MongoDBConfiguration");

    String databaseName = (String) mongoDBPropMap.get("mongoDBDatabaseName");
    String host = (String) mongoDBPropMap.get("mongoDBHost");
    Integer port = (Integer) mongoDBPropMap.get("mongoDBPort");
    String configUsername = (String) mongoDBPropMap.get("mongoDBUsername");
    String configPassword = (String) mongoDBPropMap.get("mongoDBPassword");

    MongoClient mongoClient =
        new MongoClient(new ServerAddress(host, port), MongoClientOptions.builder().build());
    return mongoClient;
  }

  @Test
  public void a_checkMongoInitWithoutCredential() throws Exception {
    LOGGER.info(
        "\n\n Check mongo init without credential test start................................\n\n");

    String rootPath = System.getProperty("user.dir");
    rootPath = rootPath + "\\src\\test\\java\\com\\mitdbg\\modeldb\\mongoTest1Config.yaml";
    Map<String, Object> propertiesMap = readYamlProperties(rootPath);
    Map<String, Object> databasePropMap = (Map<String, Object>) propertiesMap.get("test-database");

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder serverBuilder =
        InProcessServerBuilder.forName(serverName).directExecutor();

    App.initializeServicesBaseOnDataBase(serverBuilder, databasePropMap, propertiesMap);

    MongoClient mongoClient = getMongoClient(databasePropMap);

    Map<String, Object> mongoDBPropMap =
        (Map<String, Object>) databasePropMap.get("MongoDBConfiguration");

    String databaseName = (String) mongoDBPropMap.get("mongoDBDatabaseName");

    MongoDatabase database = mongoClient.getDatabase(databaseName);

    assertEquals(databaseName, database.getName());

    LOGGER.info(
        "\n\n Check mongo init without credential test stop................................\n\n");
  }

  @Test
  public void b_checkMongoInitWithCredential() throws Exception {
    LOGGER.info(
        "\n\n Check mongo init with credential test start................................\n\n");

    String rootPath = System.getProperty("user.dir");
    rootPath = rootPath + "\\src\\test\\java\\com\\mitdbg\\modeldb\\mongoTest2Config.yaml";
    Map<String, Object> propertiesMap = readYamlProperties(rootPath);
    Map<String, Object> databasePropMap = (Map<String, Object>) propertiesMap.get("test-database");

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder serverBuilder =
        InProcessServerBuilder.forName(serverName).directExecutor();

    App.initializeServicesBaseOnDataBase(serverBuilder, databasePropMap, propertiesMap);

    MongoClient mongoClient = getMongoClient(databasePropMap);

    Map<String, Object> mongoDBPropMap =
        (Map<String, Object>) databasePropMap.get("MongoDBConfiguration");

    String databaseName = (String) mongoDBPropMap.get("mongoDBDatabaseName");
    String configUsername = (String) mongoDBPropMap.get("mongoDBUsername");

    MongoDatabase database = mongoClient.getDatabase(databaseName);

    Boolean doesAdminUserExist = App.doesUserExistInMongo(database, configUsername);

    assertTrue(database.getName().equals(databaseName) && doesAdminUserExist);

    LOGGER.info(
        "\n\n Check mongo init with credential test stop................................\n\n");
  }
}
