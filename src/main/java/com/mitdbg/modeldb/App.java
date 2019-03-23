package com.mitdbg.modeldb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import com.mitdbg.modeldb.artifactStore.ArtifactStoreDAO;
import com.mitdbg.modeldb.artifactStore.ArtifactStoreDAOMongoImpl;
import com.mitdbg.modeldb.artifactStore.ArtifactStoreDAORdbImpl;
import com.mitdbg.modeldb.databaseServices.DocumentService;
import com.mitdbg.modeldb.databaseServices.MongoService;
import com.mitdbg.modeldb.experiment.ExperimentDAO;
import com.mitdbg.modeldb.experiment.ExperimentDAOMongoImpl;
import com.mitdbg.modeldb.experiment.ExperimentDAORdbImpl;
import com.mitdbg.modeldb.experiment.ExperimentServiceImpl;
import com.mitdbg.modeldb.experimentRun.ExperimentRunDAO;
import com.mitdbg.modeldb.experimentRun.ExperimentRunDAOMongoImpl;
import com.mitdbg.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import com.mitdbg.modeldb.experimentRun.ExperimentRunServiceImpl;
import com.mitdbg.modeldb.job.JobDAO;
import com.mitdbg.modeldb.job.JobDAOMongoImpl;
import com.mitdbg.modeldb.job.JobDAORdbImpl;
import com.mitdbg.modeldb.job.JobServiceImpl;
import com.mitdbg.modeldb.project.ProjectDAO;
import com.mitdbg.modeldb.project.ProjectDAOMongoImpl;
import com.mitdbg.modeldb.project.ProjectDAORdbImpl;
import com.mitdbg.modeldb.project.ProjectServiceImpl;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/** This class is entry point of modeldb server. */
@SuppressWarnings("unchecked")
public class App {

  private static final Logger LOGGER = Logger.getLogger(App.class.getName());
  private String projectEntity = null;
  private String experimentEntity = null;
  private String experimentRunEntity = null;
  private String artifactStoreMappingEntity = null;
  private ManagedChannel artifactServerChannel = null;
  private String collaboratorEntity = null;
  private String jobEntity = null;
  private static App app = null;

  public static App getInstance() {
    if (app == null) {
      app = new App();
    }
    return app;
  }

  public static void main(String[] args) throws Exception {

    // --------------- Start reading properties --------------------------
    Map<String, Object> propertiesMap = ModelDBUtils.readYamlProperties();
    // --------------- End reading properties --------------------------

    // --------------- Start Initialize modelDB gRPC server --------------------------
    Map<String, Object> grpcServerMap = (Map<String, Object>) propertiesMap.get("grpcServer");
    if (grpcServerMap == null) {
      throw new ModelDBException("grpcServer configuration not found in properties.");
    }

    Integer grpcServerPort = (Integer) grpcServerMap.get("port");
    LOGGER.info("grpc server port number founded. ");
    ServerBuilder<?> serverBuilder = ServerBuilder.forPort(grpcServerPort);

    Map<String, Object> databasePropMap = (Map<String, Object>) propertiesMap.get("database");

    // ----------------- Start Initialize database, mongoDB & modelDB services with DAO ---------
    initializeServicesBaseOnDataBase(serverBuilder, databasePropMap, propertiesMap);
    // ----------------- Finish Initialize database, mongoDB & modelDB services with DAO --------

    Server server = serverBuilder.build();
    // --------------- Finish Initialize modelDB gRPC server --------------------------

    // --------------- Start modelDB gRPC server --------------------------
    server.start();
    LOGGER.log(Level.SEVERE, "Server started, listening on : {0} ", grpcServerPort);

    // ----------- Don't exit the main thread. Wait until server is terminated -----------
    server.awaitTermination();
  }

  public static void initializeServicesBaseOnDataBase(
      ServerBuilder<?> serverBuilder,
      Map<String, Object> databasePropMap,
      Map<String, Object> propertiesMap)
      throws ModelDBException {

    App app = App.getInstance();

    // --------------- Start Initialize Entity name from configuration --------------------------
    Map<String, Object> entityNameMap = (Map<String, Object>) propertiesMap.get("entities");

    String projectEntity = (String) entityNameMap.get("projectEntity");
    app.setProjectEntity(projectEntity);
    String experimentEntity = (String) entityNameMap.get("experimentEntity");
    app.setExperimentEntity(experimentEntity);
    String experimentRunEntity = (String) entityNameMap.get("experimentRunEntity");
    app.setExperimentRunEntity(experimentRunEntity);
    String artifactStoreMappingEntity = (String) entityNameMap.get("artifactStoreMappingEntity");
    app.setArtifactStoreMappingEntity(artifactStoreMappingEntity);
    String jobEntityString = (String) entityNameMap.get("jobEntity");
    app.setJobEntity(jobEntityString);
    // --------------- Finish Initialize Entity name from configuration --------------------------

    // --------------- Start Initialize Artifact Store server and Create channel -----------------
    Map<String, Object> artifactStoreServerPropMap =
        (Map<String, Object>) propertiesMap.get("artifactStore_grpcServer");

    String artifactStoreServerHost = (String) artifactStoreServerPropMap.get("host");
    Integer artifactStoreServerPort = (Integer) artifactStoreServerPropMap.get("port");

    ManagedChannel artifactServerChannel =
        ManagedChannelBuilder.forTarget(artifactStoreServerHost + ":" + artifactStoreServerPort)
            .usePlaintext()
            .build();
    app.setArtifactServerChannel(artifactServerChannel);

    // --------------- Finish Initialize Artifact Store server and Create channel ----------------

    // --------------- Start Initialize Database base on configuration --------------------------
    if (databasePropMap.isEmpty()) {
      throw new ModelDBException("database properties not found in config.");
    }
    LOGGER.info("Database properties found");

    String DBType = (String) databasePropMap.get("DBType");
    String databaseName = null, host = null, configUsername = null, configPassword = null;
    Integer port;
    switch (DBType) {
      case "mongodb":

        // --------------- Start Initialize MongoDB Database base on configuration ---------------
        Map<String, Object> mongoDBPropMap =
            (Map<String, Object>) databasePropMap.get("MongoDBConfiguration");

        databaseName = (String) mongoDBPropMap.get("mongoDBDatabaseName");
        host = (String) mongoDBPropMap.get("mongoDBHost");
        port = (Integer) mongoDBPropMap.get("mongoDBPort");
        configUsername = (String) mongoDBPropMap.get("mongoDBUsername");
        configPassword = (String) mongoDBPropMap.get("mongoDBPassword");

        MongoClient mongoClient =
            new MongoClient(new ServerAddress(host, port), MongoClientOptions.builder().build());

        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCredential credential = null;

        if (configUsername != null && !configUsername.isEmpty()) {
          /**
           * If username and password not found in config.yaml, then check if specified username
           * exists. If the user exists, reuse existing, otherwise create new user
           */
          Boolean doesAdminUserExist = doesUserExistInMongo(database, configUsername);
          if (!doesAdminUserExist) {
            createNewMongoUser(database, configUsername, configPassword);
          }
          credential =
              MongoCredential.createCredential(
                  configUsername, databaseName, configPassword.toCharArray());
        } else {
          /**
           * If username is not set in config.yaml, create a new user with the default credentials.
           * In case such a user already exists, use this user.
           */
          Boolean doesAdminUserExist =
              doesUserExistInMongo(database, ModelDBConstants.DEFAULT_USERNAME);
          if (!doesAdminUserExist) {
            createNewMongoUser(
                database, ModelDBConstants.DEFAULT_USERNAME, ModelDBConstants.DEFAULT_PASSWORD);
          }
          credential =
              MongoCredential.createCredential(
                  ModelDBConstants.DEFAULT_USERNAME,
                  databaseName,
                  ModelDBConstants.DEFAULT_PASSWORD.toCharArray());
        }
        mongoClient =
            new MongoClient(
                new ServerAddress(host, port), credential, MongoClientOptions.builder().build());
        database = mongoClient.getDatabase(databaseName);

        LOGGER.info("MongoDB Client configured with server");
        // --------------- Finish Initialize MongoDB Database base on configuration --------------

        // --------------- Start Initialize MongoDB Service and modelDB services -----------------
        initializeMongoService(serverBuilder, database);
        // --------------- Start Initialize MongoDB Service and modelDB services -----------------
        break;
      case "rdbms":

        // --------------- Start Initialize MongoDB Database base on configuration ---------------
        ModelDBHibernateUtil.databasePropMap = databasePropMap;
        ModelDBHibernateUtil.getSessionFactory();

        LOGGER.info("RDBMS configured with server");
        // --------------- Finish Initialize mySQL Database base on configuration --------------

        // --------------- Start Initialize mySQL Service and modelDB services -----------------
        initializeRDBMSServices(serverBuilder);
        // --------------- Start Initialize mySQL Service and modelDB services -----------------
        break;

      default:
        throw new ModelDBException(
            "Please enter valid database name (DBType) in config.yaml file.");
    }

    // --------------- Finish Initialize Database base on configuration --------------------------

  }

  private static void createNewMongoUser(MongoDatabase database, String username, String password) {
    Document createUserCommand =
        new Document("createUser", username)
            .append("pwd", password)
            .append(
                "roles",
                Collections.singletonList(
                    new Document("role", "dbOwner").append("db", database.getName())));
    database.runCommand(createUserCommand);
  }

  public static Boolean doesUserExistInMongo(MongoDatabase database, String username) {
    Document getUsersInfoCommand = new Document("usersInfo", 1);

    // If test user exists from a previous run, drop it
    Document existingUsersDocument = database.runCommand(getUsersInfoCommand);
    ArrayList existingUserList = (ArrayList) existingUsersDocument.get("users");

    Boolean doesAdminUserExist = false;
    if (existingUserList != null && !existingUserList.isEmpty()) {
      for (Object object : existingUserList) {
        Document existingUserDocument = (Document) object;
        if (existingUserDocument.getString("user").equalsIgnoreCase(username)) {
          List<Document> rolesList = (List<Document>) existingUserDocument.get("roles");
          for (Document roleDoc : rolesList) {
            String userRole = roleDoc.getString("role");
            if (userRole.equalsIgnoreCase("dbOwner")) {
              doesAdminUserExist = true;
              break;
            }
          }
          if (!doesAdminUserExist) {
            throw new RuntimeException(
                "Given user dont have a 'dbOwner' access control, system aspect the config user has 'dbOwner' access control");
          }
          break;
        }
      }
    }

    return doesAdminUserExist;
  }

  private static void initializeMongoService(
      ServerBuilder<?> serverBuilder, MongoDatabase database) {
    DocumentService documentService = new MongoService(database);

    // --------------- Start Initialize Project ServiceImpl & DAO --------------------------

    documentService = new MongoService(database);
    ProjectDAO projectDAO = new ProjectDAOMongoImpl(documentService);
    serverBuilder.addService(new ProjectServiceImpl(projectDAO));
    LOGGER.info("Project serviceImpl & DAO initialized");
    // --------------- Finish Initialize Project ServiceImpl & DAO --------------------------

    // --------------- Start Initialize Experiment ServiceImpl & DAO --------------------------
    documentService = new MongoService(database);
    ExperimentDAO experimentDAO = new ExperimentDAOMongoImpl(documentService);
    serverBuilder.addService(new ExperimentServiceImpl(experimentDAO, projectDAO));
    LOGGER.info("Experiment serviceImpl & DAO initialized");
    // --------------- Finish Initialize Experiment ServiceImpl & DAO --------------------------

    // --------------- Start Initialize ExperimentRun ServiceImpl & DAO with ArtifactStoreMapping
    // DAO --------------------------
    documentService = new MongoService(database);
    ExperimentRunDAO experimentRunDAO = new ExperimentRunDAOMongoImpl(documentService);

    documentService = new MongoService(database);
    ArtifactStoreDAO artifactStoreDAO = new ArtifactStoreDAOMongoImpl(documentService);
    serverBuilder.addService(
        new ExperimentRunServiceImpl(
            experimentRunDAO, projectDAO, experimentDAO, artifactStoreDAO));

    LOGGER.info("ExperimentRun serviceImpl & DAO initialized");
    // --------------- Finish Initialize ExperimentRun ServiceImpl & DAO with ArtifactStoreMapping
    // DAO --------------------------

    // --------------- Start Initialize Job ServiceImpl --------------------------
    documentService = new MongoService(database);
    JobDAO jobDAO = new JobDAOMongoImpl(documentService);
    serverBuilder.addService(new JobServiceImpl(jobDAO));
    // --------------- Stop Initialize Job ServiceImpl --------------------------

    LOGGER.info("All services initialized and resolved dependency before server start");
  }

  private static void initializeRDBMSServices(ServerBuilder<?> serverBuilder) {

    // --------------- Start Initialize Project ServiceImpl & DAO --------------------------
    ProjectDAO projectDAO = new ProjectDAORdbImpl();
    serverBuilder.addService(new ProjectServiceImpl(projectDAO));
    LOGGER.info("Project serviceImpl & DAO initialized");
    // --------------- Finish Initialize Project ServiceImpl & DAO --------------------------

    // --------------- Start Initialize Experiment ServiceImpl & DAO --------------------------
    ExperimentDAO experimentDAO = new ExperimentDAORdbImpl();
    serverBuilder.addService(new ExperimentServiceImpl(experimentDAO, projectDAO));
    LOGGER.info("Experiment serviceImpl & DAO initialized");
    // --------------- Finish Initialize Experiment ServiceImpl & DAO --------------------------

    // --------------- Start Initialize ExperimentRun ServiceImpl & DAO with ArtifactStoreMapping
    // DAO --------------------------
    ExperimentRunDAO experimentRunDAO = new ExperimentRunDAORdbImpl();

    ArtifactStoreDAO artifactStoreDAO = new ArtifactStoreDAORdbImpl();
    serverBuilder.addService(
        new ExperimentRunServiceImpl(
            experimentRunDAO, projectDAO, experimentDAO, artifactStoreDAO));

    LOGGER.info("ExperimentRun serviceImpl & DAO initialized");
    // --------------- Finish Initialize ExperimentRun ServiceImpl & DAO with ArtifactStoreMapping
    // DAO --------------------------

    // --------------- Start Initialize Job ServiceImpl --------------------------
    JobDAO jobDAO = new JobDAORdbImpl();
    serverBuilder.addService(new JobServiceImpl(jobDAO));
    // --------------- Stop Initialize Job ServiceImpl --------------------------

    LOGGER.info("All services initialized and resolved dependency before server start");
  }

  public void setProjectEntity(String projectEntity) {
    this.projectEntity = projectEntity;
  }

  public String getProjectEntity() {
    return projectEntity;
  }

  public String getExperimentEntity() {
    return experimentEntity;
  }

  public void setExperimentEntity(String experimentEntity) {
    this.experimentEntity = experimentEntity;
  }

  public String getExperimentRunEntity() {
    return experimentRunEntity;
  }

  public void setExperimentRunEntity(String experimentRunEntity) {
    this.experimentRunEntity = experimentRunEntity;
  }

  public String getArtifactStoreMappingEntity() {
    return artifactStoreMappingEntity;
  }

  public void setArtifactStoreMappingEntity(String artifactStoreMappingEntity) {
    this.artifactStoreMappingEntity = artifactStoreMappingEntity;
  }

  public ManagedChannel getArtifactServerChannel() {
    return artifactServerChannel;
  }

  public void setArtifactServerChannel(ManagedChannel artifactServerChannel) {
    this.artifactServerChannel = artifactServerChannel;
  }

  public String getJobEntity() {
    return jobEntity;
  }

  public void setJobEntity(String jobEntity) {
    this.jobEntity = jobEntity;
  }
}
