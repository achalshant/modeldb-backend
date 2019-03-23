package com.mitdbg.modeldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.mitdbg.modeldb.ArtifactTypeEnum.ArtifactType;
import com.mitdbg.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import com.mitdbg.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import com.mitdbg.modeldb.OperatorEnum.Operator;
import com.mitdbg.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import com.mitdbg.modeldb.ValueTypeEnum.ValueType;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExperimentRunTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;

  private static final Logger LOGGER = Logger.getLogger(ExperimentRunTest.class.getName());

  @Before
  public void setServerAndService() throws Exception {

    Map<String, Object> propertiesMap = ModelDBUtils.readYamlProperties();
    Map<String, Object> databasePropMap = (Map<String, Object>) propertiesMap.get("test-database");

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder serverBuilder =
        InProcessServerBuilder.forName(serverName).directExecutor();
    InProcessChannelBuilder channelBuilder =
        InProcessChannelBuilder.forName(serverName).directExecutor();

    App.initializeServicesBaseOnDataBase(serverBuilder, databasePropMap, propertiesMap);

    grpcCleanup.register(serverBuilder.build().start());
    this.channel = grpcCleanup.register(channelBuilder.build());
  }

  @After
  public void clientClose() {
    if (!channel.isShutdown()) {
      channel.shutdownNow();
    }
  }

  private CreateExperimentRun createExperimentRunRequest(String projectId, String experimentId) {

    List<String> tags = new ArrayList<String>();
    tags.add("Tag 1 " + Calendar.getInstance().getTimeInMillis());
    tags.add("Tag 2 " + Calendar.getInstance().getTimeInMillis());

    int rangeMax = 20;
    int rangeMin = 1;
    Random randomNum = new Random();

    List<KeyValue> attributeList = new ArrayList<KeyValue>();
    Value intValue = Value.newBuilder().setNumberValue(1.1).build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build());

    double randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    List<KeyValue> hyperparameters = new ArrayList<KeyValue>();
    intValue = Value.newBuilder().setNumberValue(randomValue).build();
    hyperparameters.add(
        KeyValue.newBuilder()
            .setKey("tuning")
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    stringValue =
        Value.newBuilder()
            .setStringValue("hyperparameters_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    hyperparameters.add(
        KeyValue.newBuilder()
            .setKey("hyperparameters_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build());

    List<Artifact> artifactList = new ArrayList<Artifact>();
    artifactList.add(
        Artifact.newBuilder()
            .setKey("Google developer Artifact")
            .setPath(
                "https://www.google.co.in/imgres?imgurl=https%3A%2F%2Flh3.googleusercontent.com%2FFyZA5SbKPJA7Y3XCeb9-uGwow8pugxj77Z1xvs8vFS6EI3FABZDCDtA9ScqzHKjhU8av_Ck95ET-P_rPJCbC2v_OswCN8A%3Ds688&imgrefurl=https%3A%2F%2Fdevelopers.google.com%2F&docid=1MVaWrOPIjYeJM&tbnid=I7xZkRN5m6_z-M%3A&vet=10ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhyKAMwAw..i&w=688&h=387&bih=657&biw=1366&q=google&ved=0ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhyKAMwAw&iact=mrc&uact=8")
            .setArtifactType(ArtifactType.BLOB)
            .build());
    artifactList.add(
        Artifact.newBuilder()
            .setKey("Google Pay Artifact")
            .setPath(
                "https://www.google.co.in/imgres?imgurl=https%3A%2F%2Fpay.google.com%2Fabout%2Fstatic%2Fimages%2Fsocial%2Fknowledge_graph_logo.png&imgrefurl=https%3A%2F%2Fpay.google.com%2Fabout%2F&docid=zmoE9BrSKYr4xM&tbnid=eCL1Y6f9xrPtDM%3A&vet=10ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhwKAIwAg..i&w=1200&h=630&bih=657&biw=1366&q=google&ved=0ahUKEwjr1OiS0ufeAhWNbX0KHXpFAmQQMwhwKAIwAg&iact=mrc&uact=8")
            .setArtifactType(ArtifactType.IMAGE)
            .build());

    List<Artifact> datasets = new ArrayList<Artifact>();
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google developer datasets")
            .setPath("This is data artifect type in Google developer datasets")
            .setArtifactType(ArtifactType.MODEL)
            .build());
    datasets.add(
        Artifact.newBuilder()
            .setKey("Google Pay datasets")
            .setPath("This is data artifect type in Google Pay datasets")
            .setArtifactType(ArtifactType.DATA)
            .build());

    List<KeyValue> metrics = new ArrayList<KeyValue>();
    randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    intValue = Value.newBuilder().setNumberValue(randomValue).build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("accuracy")
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    intValue = Value.newBuilder().setNumberValue(randomValue).build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("loss")
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build());
    randomValue = rangeMin + (rangeMax - rangeMin) * randomNum.nextDouble();
    Value listValue =
        Value.newBuilder()
            .setListValue(
                ListValue.newBuilder()
                    .addValues(intValue)
                    .addValues(Value.newBuilder().setNumberValue(randomValue).build()))
            .build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("profit")
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build());

    List<Observation> observations = new ArrayList<Observation>();
    observations.add(
        Observation.newBuilder()
            .setArtifact(
                Artifact.newBuilder()
                    .setKey("Google developer Observation artifact")
                    .setPath("This is data artifect type in Google developer Observation artifact")
                    .setArtifactType(ArtifactType.DATA)
                    .build())
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build());
    stringValue =
        Value.newBuilder()
            .setStringValue("Observation_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    observations.add(
        Observation.newBuilder()
            .setAttribute(
                KeyValue.newBuilder()
                    .setKey("Observation Key " + Calendar.getInstance().getTimeInMillis())
                    .setValue(stringValue)
                    .setValueType(ValueType.STRING))
            .setTimestamp(Calendar.getInstance().getTimeInMillis())
            .build());

    List<Feature> features = new ArrayList<Feature>();
    features.add(Feature.newBuilder().setName("ExperimentRun Test case feature 1").build());
    features.add(Feature.newBuilder().setName("ExperimentRun Test case feature 2").build());

    CreateExperimentRun request =
        CreateExperimentRun.newBuilder()
            .setProjectId(projectId)
            .setExperimentId(experimentId)
            .setName("ExperimentRun Name " + Calendar.getInstance().getTimeInMillis())
            .setDescription("this is a ExperimentRun description")
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .setDateUpdated(Calendar.getInstance().getTimeInMillis())
            .setStartTime(Calendar.getInstance().getTime().getTime())
            .setEndTime(Calendar.getInstance().getTime().getTime())
            .setCodeVersion("1.0")
            .addAllTags(tags)
            .addAllAttributes(attributeList)
            .addAllHyperparameters(hyperparameters)
            .addAllArtifacts(artifactList)
            .addAllDatasets(datasets)
            .addAllMetrics(metrics)
            .addAllObservations(observations)
            .addAllFeatures(features)
            .build();

    return request;
  }

  @Test
  public void a_experimentRunCreateTest() throws IOException {
    LOGGER.info("\n\n Create ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          CreateExperimentRun request =
              createExperimentRunRequest(
                  getProjectsResponse.getProjectsList().get(0).getId(),
                  experimentResponse.getExperimentsList().get(0).getId());

          CreateExperimentRun.Response response =
              experimentRunServiceStub.createExperimentRun(request);

          LOGGER.info(
              "CreateExperimentRun Response : \n"
                  + ModelDBUtils.getStringFromProtoObject(response.getExperimentRun()));
          assertEquals(true, response.getExperimentRun() != null);

          try {
            experimentRunServiceStub.createExperimentRun(request);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Create ExperimentRun test stop................................\n\n");
  }

  @Test
  public void a_experimentRunCreateNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Create ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          CreateExperimentRun request =
              createExperimentRunRequest(
                  "", experimentResponse.getExperimentsList().get(0).getId());

          try {
            experimentRunServiceStub.createExperimentRun(request);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            LOGGER.info("CreateExperimentRun Response : \n" + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Create ExperimentRun Negative test stop................................\n\n");
  }

  @Test
  public void b_getExperimentRunFromProjectRunTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRun from Project test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      try {
        GetExperimentRunsInProject getExperiment =
            GetExperimentRunsInProject.newBuilder()
                .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
                .build();

        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentServiceStub.getExperimentRunsInProject(getExperiment);

        if (experimentRunResponse.getExperimentRunsList() != null) {

          LOGGER.info(
              "GetExperimentRunsInProject Response : "
                  + experimentRunResponse.getExperimentRunsCount()
                  + "\n"
                  + experimentRunResponse.getExperimentRunsList());
          assertEquals(true, experimentRunResponse.getExperimentRunsList() != null);
        } else {
          LOGGER.log(Level.WARNING, "More ExperimentRun not found in database");
          assertTrue(true);
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertTrue(false);
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRun from Project test stop................................\n\n");
  }

  @Test
  public void b_getExperimentRunWithPaginationFromProjectRunTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRun using pagination from Project test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      try {

        Integer pageLimit = 2;
        for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
          GetExperimentRunsInProject getExperiment =
              GetExperimentRunsInProject.newBuilder()
                  .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
                  .setPageNumber(pageNumber)
                  .setPageLimit(pageLimit)
                  .setSortOrder(ModelDBConstants.ORDER_ASC)
                  .setSortBy(ModelDBConstants.NAME)
                  .build();

          GetExperimentRunsInProject.Response experimentRunResponse =
              experimentServiceStub.getExperimentRunsInProject(getExperiment);

          if (experimentRunResponse.getExperimentRunsList() != null
              && experimentRunResponse.getExperimentRunsList().size() > 0) {

            LOGGER.info(
                "GetExperimentRunsInProject Response : "
                    + experimentRunResponse.getExperimentRunsCount()
                    + "\n"
                    + experimentRunResponse.getExperimentRunsList());

            assertEquals(true, experimentRunResponse.getExperimentRunsList() != null);

          } else {
            LOGGER.log(Level.WARNING, "More ExperimentRun not found in database");
            assertTrue(true);
            break;
          }
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertTrue(false);
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRun using pagination from Project test stop................................\n\n");
  }

  @Test
  public void b_getExperimentFromProjectRunNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRun from Project Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperiment = GetExperimentRunsInProject.newBuilder().build();
      try {
        experimentServiceStub.getExperimentRunsInProject(getExperiment);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
      }

      getExperiment = GetExperimentRunsInProject.newBuilder().setProjectId("sdfdsfsd").build();
      try {
        experimentServiceStub.getExperimentRunsInProject(getExperiment);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertTrue(Status.UNAVAILABLE.getCode().equals(status.getCode()));
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRun from Project Negative test stop................................\n\n");
  }

  @Test
  public void bb_getExperimentRunFromExperimentTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRun from Experiment test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      try {
        ExperimentServiceBlockingStub experimentServiceStub =
            ExperimentServiceGrpc.newBlockingStub(channel);

        GetExperimentsInProject getExperiment =
            GetExperimentsInProject.newBuilder()
                .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
                .build();

        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {
          Experiment experiment = experimentResponse.getExperimentsList().get(0);
          LOGGER.info("experiment Id : " + experiment.getId());

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          GetExperimentRunsInExperiment getExperimentRunsInExperiment =
              GetExperimentRunsInExperiment.newBuilder()
                  .setExperimentId(experiment.getId())
                  .build();

          GetExperimentRunsInExperiment.Response experimentRunResponse =
              experimentRunServiceStub.getExperimentRunsInExperiment(getExperimentRunsInExperiment);

          if (experimentRunResponse.getExperimentRunsList() != null) {

            LOGGER.info(
                "GetExperimentRunsInExperiment Response : "
                    + experimentRunResponse.getExperimentRunsCount()
                    + "\n"
                    + experimentRunResponse.getExperimentRunsList());
            assertEquals(true, experimentRunResponse.getExperimentRunsList() != null);
          } else {
            LOGGER.log(Level.WARNING, "More ExperimentRun not found in database");
            assertTrue(true);
          }
        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }

      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertTrue(false);
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRun from Experiment test stop................................\n\n");
  }

  @Test
  public void bb_getExperimentRunWithPaginationFromExperimentTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRun using pagination from Experiment test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      try {

        ExperimentServiceBlockingStub experimentServiceStub =
            ExperimentServiceGrpc.newBlockingStub(channel);

        GetExperimentsInProject getExperiment =
            GetExperimentsInProject.newBuilder()
                .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
                .build();

        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {
          Experiment experiment = experimentResponse.getExperimentsList().get(0);
          System.out.println("experiment Id : " + experiment.getId());

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          Integer pageLimit = 1;
          for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
            GetExperimentRunsInExperiment getExperimentRunsInExperiment =
                GetExperimentRunsInExperiment.newBuilder()
                    .setExperimentId(experiment.getId())
                    .setPageNumber(pageNumber)
                    .setPageLimit(pageLimit)
                    .setSortOrder(ModelDBConstants.ORDER_ASC)
                    .setSortBy(ModelDBConstants.NAME)
                    .build();

            GetExperimentRunsInExperiment.Response experimentRunResponse =
                experimentRunServiceStub.getExperimentRunsInExperiment(
                    getExperimentRunsInExperiment);

            if (experimentRunResponse.getExperimentRunsList() != null
                && experimentRunResponse.getExperimentRunsList().size() > 0) {

              LOGGER.info(
                  "GetExperimentRunsInExperiment Response : "
                      + experimentRunResponse.getExperimentRunsCount()
                      + "\n"
                      + experimentRunResponse.getExperimentRunsList());

              assertEquals(true, experimentRunResponse.getExperimentRunsList() != null);

            } else {
              LOGGER.log(Level.WARNING, "More ExperimentRun not found in database");
              assertTrue(true);
              break;
            }
          }
        }

      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertTrue(false);
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRun using pagination from Experiment test stop................................\n\n");
  }

  @Test
  public void bb_getExperimentFromExperimentNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRun from Experiment Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInExperiment getExperiment =
          GetExperimentRunsInExperiment.newBuilder().build();
      try {
        experimentServiceStub.getExperimentRunsInExperiment(getExperiment);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
      }

      getExperiment =
          GetExperimentRunsInExperiment.newBuilder().setExperimentId("sdfdsfsd").build();
      try {
        experimentServiceStub.getExperimentRunsInExperiment(getExperiment);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertTrue(Status.UNAVAILABLE.getCode().equals(status.getCode()));
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRun from Experiment Negative test stop................................\n\n");
  }

  @Test
  public void c_getExperimentRunByIdTest() throws IOException {
    LOGGER.info("\n\n Get ExperimentRunById test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetExperimentRunById request =
              GetExperimentRunById.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .build();

          GetExperimentRunById.Response response =
              experimentRunServiceStub.getExperimentRunById(request);

          LOGGER.info("getExperimentRunById Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Get ExperimentRunById test stop................................\n\n");
  }

  @Test
  public void c_getExperimentRunByIdNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRunById Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetExperimentRunById request = GetExperimentRunById.newBuilder().build();

          try {
            experimentRunServiceStub.getExperimentRunById(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          request = GetExperimentRunById.newBuilder().setId("fdsfd").build();

          try {
            experimentRunServiceStub.getExperimentRunById(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRunById Negative test stop................................\n\n");
  }

  @Test
  public void c_getExperimentRunByNameTest() throws IOException {
    LOGGER.info("\n\n Get ExperimentRunByName test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetExperimentRunByName request =
              GetExperimentRunByName.newBuilder()
                  .setName(experimentRunResponse.getExperimentRunsList().get(0).getName())
                  .build();

          GetExperimentRunByName.Response response =
              experimentRunServiceStub.getExperimentRunByName(request);

          LOGGER.info("getExperimentRunByName Response : \n" + response.getExperimentRunsList());
          assertEquals(true, response.getExperimentRunsList() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertTrue(false);
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Get ExperimentRunByName test stop................................\n\n");
  }

  @Test
  public void c_getExperimentRunByNameNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRunByName Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetExperimentRunByName request = GetExperimentRunByName.newBuilder().build();

          try {
            experimentRunServiceStub.getExperimentRunByName(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get ExperimentRunByName Negative test stop................................\n\n");
  }

  @Test
  public void d_updateExperimentRunNameOrDescription() throws IOException {
    LOGGER.info(
        "\n\n Update ExperimentRun Name & Discription test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          UpdateExperimentRunNameOrDescription request =
              UpdateExperimentRunNameOrDescription.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setName("ExperimentRun Name updated " + Calendar.getInstance().getTimeInMillis())
                  .setDescription(
                      "this is a ExperimentRun description updated "
                          + Calendar.getInstance().getTimeInMillis())
                  .build();

          UpdateExperimentRunNameOrDescription.Response response =
              experimentRunServiceStub.updateExperimentRunNameOrDescription(request);
          LOGGER.info(
              "UpdateExperimentRunNameOrDescription Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Update ExperimentRun Name & Discription test stop................................\n\n");
  }

  @Test
  public void d_updateExperimentRunNameOrDescriptionNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Update ExperimentRun Name & Discription Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          ExperimentRun experimentRun = experimentRunResponse.getExperimentRunsList().get(0);

          UpdateExperimentRunNameOrDescription request =
              UpdateExperimentRunNameOrDescription.newBuilder()
                  .setName("ExperimentRun Name updated " + Calendar.getInstance().getTimeInMillis())
                  .setDescription(
                      "this is a ExperimentRun description updated "
                          + Calendar.getInstance().getTimeInMillis())
                  .build();

          try {
            experimentRunServiceStub.updateExperimentRunNameOrDescription(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          request =
              UpdateExperimentRunNameOrDescription.newBuilder()
                  .setId(experimentRun.getId())
                  .setName(experimentRun.getName())
                  .setDescription(experimentRun.getDescription())
                  .build();

          try {
            experimentRunServiceStub.updateExperimentRunNameOrDescription(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Update ExperimentRun Name & Discription Negative test stop................................\n\n");
  }

  @Test
  public void e_addExperimentRunTags() throws IOException {
    LOGGER.info("\n\n Add ExperimentRun tags test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          List<String> tags = new ArrayList<String>();
          tags.add("Test Added tag " + Calendar.getInstance().getTimeInMillis());
          tags.add("Test Added tag 2 " + Calendar.getInstance().getTimeInMillis());

          AddExperimentRunTags request =
              AddExperimentRunTags.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .addAllTags(tags)
                  .build();

          AddExperimentRunTags.Response response =
              experimentRunServiceStub.addExperimentRunTags(request);
          LOGGER.info("AddExperimentRunTags Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Add ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void e_addExperimentRunTagsNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Add ExperimentRun tags Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          List<String> tags = new ArrayList<String>();
          tags.add("Test Added tag " + Calendar.getInstance().getTimeInMillis());
          tags.add("Test Added tag 2 " + Calendar.getInstance().getTimeInMillis());

          AddExperimentRunTags request = AddExperimentRunTags.newBuilder().addAllTags(tags).build();

          try {
            experimentRunServiceStub.addExperimentRunTags(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          request =
              AddExperimentRunTags.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .addAllTags(experimentRunResponse.getExperimentRunsList().get(0).getTagsList())
                  .build();

          try {
            experimentRunServiceStub.addExperimentRunTags(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Add ExperimentRun tags Negative test stop................................\n\n");
  }

  @Test
  public void ee_getExperimentRunTags() throws IOException {
    LOGGER.info("\n\n Get ExperimentRun tags test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          ExperimentRun experimentRun = experimentRunResponse.getExperimentRunsList().get(0);
          GetTags request = GetTags.newBuilder().setId(experimentRun.getId()).build();

          GetTags.Response response = experimentRunServiceStub.getExperimentRunTags(request);
          LOGGER.info("GetExperimentRunTags Response : \n" + response.getTagsList());
          assertEquals(experimentRun.getTagsList(), response.getTagsList());

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Get ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void ee_getExperimentRunTagsNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get ExperimentRun tags Negative test start................................\n\n");

    GetTags request = GetTags.newBuilder().build();
    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    try {
      experimentRunServiceStub.getExperimentRunTags(request);
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      LOGGER.log(
          Level.WARNING,
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info(
        "\n\n Get ExperimentRun tags Negative test stop................................\n\n");
  }

  @Test
  public void f_deleteExperimentRunTags() throws IOException {
    LOGGER.info("\n\n Delete ExperimentRun tags test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          ExperimentRun experimentRun = experimentRunResponse.getExperimentRunsList().get(0);
          List<String> removableTagList = new ArrayList<>();
          if (experimentRun.getTagsList().size() > 1) {
            removableTagList =
                experimentRun.getTagsList().subList(0, experimentRun.getTagsList().size() - 1);
          }
          DeleteExperimentRunTags request =
              DeleteExperimentRunTags.newBuilder()
                  .setId(experimentRun.getId())
                  .addAllTags(removableTagList)
                  .build();

          DeleteExperimentRunTags.Response response =
              experimentRunServiceStub.deleteExperimentRunTags(request);
          LOGGER.info(
              "DeleteExperimentRunTags Response : \n" + response.getExperimentRun().getTagsList());
          assertTrue(response.getExperimentRun().getTagsList().size() <= 1);

          if (response.getExperimentRun().getTagsList().size() > 0) {
            request =
                DeleteExperimentRunTags.newBuilder()
                    .setId(experimentRun.getId())
                    .setDeleteAll(true)
                    .build();

            response = experimentRunServiceStub.deleteExperimentRunTags(request);
            LOGGER.info(
                "DeleteExperimentRunTags Response : \n"
                    + response.getExperimentRun().getTagsList());
            assertTrue(response.getExperimentRun().getTagsList().size() == 0);
          }
        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Delete ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void fa_deleteExperimentRunTagsNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Delete ExperimentRun tags Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          DeleteExperimentRunTags request = DeleteExperimentRunTags.newBuilder().build();

          try {
            experimentRunServiceStub.deleteExperimentRunTags(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          request =
              DeleteExperimentRunTags.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setDeleteAll(true)
                  .build();

          try {
            experimentRunServiceStub.deleteExperimentRunTags(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Delete ExperimentRun tags Negative test stop................................\n\n");
  }

  @Test
  public void g_logObservationTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Observation in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          Value intValue =
              Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
          Observation observation =
              Observation.newBuilder()
                  .setAttribute(
                      KeyValue.newBuilder()
                          .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                          .setValue(intValue)
                          .setValueType(ValueType.NUMBER)
                          .build())
                  .setTimestamp(Calendar.getInstance().getTimeInMillis())
                  .build();

          LogObservation logObservationRequest =
              LogObservation.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setObservation(observation)
                  .build();

          LogObservation.Response response =
              experimentRunServiceStub.logObservation(logObservationRequest);

          LOGGER.info("LogObservation Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Observation in ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void g_logObservationNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Observation in ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          Value intValue =
              Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
          Observation observation =
              Observation.newBuilder()
                  .setAttribute(
                      KeyValue.newBuilder()
                          .setKey("New Added Key " + Calendar.getInstance().getTimeInMillis())
                          .setValue(intValue)
                          .setValueType(ValueType.NUMBER)
                          .build())
                  .setTimestamp(Calendar.getInstance().getTimeInMillis())
                  .build();

          LogObservation logObservationRequest =
              LogObservation.newBuilder().setObservation(observation).build();

          try {
            experimentRunServiceStub.logObservation(logObservationRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          ExperimentRun experimentRun = experimentRunResponse.getExperimentRunsList().get(0);
          logObservationRequest =
              LogObservation.newBuilder()
                  .setId("sdfsd")
                  .setObservation(experimentRun.getObservations(0))
                  .build();

          try {
            experimentRunServiceStub.logObservation(logObservationRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.ALREADY_EXISTS.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Observation in ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void h_getLogObservationTest() throws IOException {
    LOGGER.info(
        "\n\n Get Observation from ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetObservations getLogObservationRequest =
              GetObservations.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setObservationKey("Google developer Observation artifact")
                  .build();

          GetObservations.Response response =
              experimentRunServiceStub.getObservations(getLogObservationRequest);

          LOGGER.info(
              "GetObservations Response : "
                  + response.getObservationsCount()
                  + " \n"
                  + response.getObservationsList());
          assertEquals(true, response.getObservationsList() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Observation from ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void h_getLogObservationNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Observation from ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetObservations getLogObservationRequest =
              GetObservations.newBuilder()
                  .setObservationKey("Google developer Observation artifact")
                  .build();

          try {
            experimentRunServiceStub.getObservations(getLogObservationRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          getLogObservationRequest =
              GetObservations.newBuilder()
                  .setId("dfsdfs")
                  .setObservationKey("Google developer Observation artifact")
                  .build();

          try {
            experimentRunServiceStub.getObservations(getLogObservationRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Observation from ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void i_logMetricTest() throws IOException {
    LOGGER.info("\n\n  Log Metric in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {
          Value intValue =
              Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
          KeyValue keyValue =
              KeyValue.newBuilder()
                  .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
                  .setValue(intValue)
                  .setValueType(ValueType.NUMBER)
                  .build();

          LogMetric logMetricRequest =
              LogMetric.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setMetric(keyValue)
                  .build();

          LogMetric.Response response = experimentRunServiceStub.logMetric(logMetricRequest);

          LOGGER.info("LogMetric Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Metric in ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void i_logMetricNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Metric in ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {
          Value intValue =
              Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
          KeyValue keyValue =
              KeyValue.newBuilder()
                  .setKey("New Added Metric " + Calendar.getInstance().getTimeInMillis())
                  .setValue(intValue)
                  .setValueType(ValueType.NUMBER)
                  .build();

          LogMetric logMetricRequest = LogMetric.newBuilder().setMetric(keyValue).build();

          try {
            experimentRunServiceStub.logMetric(logMetricRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          logMetricRequest = LogMetric.newBuilder().setId("dfsdfsd").setMetric(keyValue).build();

          try {
            experimentRunServiceStub.logMetric(logMetricRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.ALREADY_EXISTS.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Metric in ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void j_getMetricsTest() throws IOException {
    LOGGER.info(
        "\n\n Get Metrics from ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetMetrics getMetricsRequest =
              GetMetrics.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .build();

          GetMetrics.Response response = experimentRunServiceStub.getMetrics(getMetricsRequest);

          LOGGER.info(
              "GetMetrics Response : "
                  + response.getMetricsCount()
                  + " \n"
                  + response.getMetricsList());
          assertEquals(true, response.getMetricsList() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Metrics from ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void j_getMetricsNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Metrics from ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetMetrics getMetricsRequest = GetMetrics.newBuilder().build();

          try {
            experimentRunServiceStub.getMetrics(getMetricsRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          getMetricsRequest = GetMetrics.newBuilder().setId("sdfdsfsd").build();

          try {
            experimentRunServiceStub.getMetrics(getMetricsRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Metrics from ExperimentRun tags Negative test stop................................\n\n");
  }

  @Test
  public void k_logDatasetTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Datasets in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          Artifact artifact =
              Artifact.newBuilder()
                  .setKey("Google Pay datasets")
                  .setPath("This is new added data artifect type in Google Pay datasets")
                  .setArtifactType(ArtifactType.MODEL)
                  .build();

          LogDataset logDatasetRequest =
              LogDataset.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setDataset(artifact)
                  .build();

          LogDataset.Response response = experimentRunServiceStub.logDataset(logDatasetRequest);

          LOGGER.info("LogDataset Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Datasets in ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void k_logDatasetNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Datasets in ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          Artifact artifact =
              Artifact.newBuilder()
                  .setKey("Google Pay datasets")
                  .setPath("This is new added data artifect type in Google Pay datasets")
                  .setArtifactType(ArtifactType.MODEL)
                  .build();

          LogDataset logDatasetRequest = LogDataset.newBuilder().setDataset(artifact).build();

          try {
            experimentRunServiceStub.logDataset(logDatasetRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          logDatasetRequest = LogDataset.newBuilder().setId("sdsdsa").setDataset(artifact).build();

          try {
            experimentRunServiceStub.logDataset(logDatasetRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.ALREADY_EXISTS.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Datasets in ExperimentRun tags Negative test stop................................\n\n");
  }

  @Test
  public void l_getDatasetsTest() throws IOException {
    LOGGER.info(
        "\n\n Get Datasets from ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetDatasets getDatasetsRequest =
              GetDatasets.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .build();

          GetDatasets.Response response = experimentRunServiceStub.getDatasets(getDatasetsRequest);

          LOGGER.info(
              "GetDatasets Response : "
                  + response.getDatasetsCount()
                  + " \n"
                  + response.getDatasetsList());
          assertEquals(true, response.getDatasetsList() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Datasets from ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void l_getDatasetsNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Datasets from ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetDatasets getDatasetsRequest = GetDatasets.newBuilder().build();

          try {
            experimentRunServiceStub.getDatasets(getDatasetsRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          getDatasetsRequest = GetDatasets.newBuilder().setId("sdfsdfdsf").build();

          try {
            experimentRunServiceStub.getDatasets(getDatasetsRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Datasets from ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void m_logArtifactTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Artifact in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          Artifact artifact =
              Artifact.newBuilder()
                  .setKey("Google Pay Artifact")
                  .setPath("46513216546" + Calendar.getInstance().getTimeInMillis())
                  .setArtifactType(ArtifactType.TENSORBOARD)
                  .build();

          LogArtifact logArtifactRequest =
              LogArtifact.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setArtifact(artifact)
                  .build();

          LogArtifact.Response response = experimentRunServiceStub.logArtifact(logArtifactRequest);

          LOGGER.info("LogArtifact Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Artifact in ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void m_logArtifactNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Artifact in ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          Artifact artifact =
              Artifact.newBuilder()
                  .setKey("Google Pay Artifact")
                  .setPath("46513216546" + Calendar.getInstance().getTimeInMillis())
                  .setArtifactType(ArtifactType.TENSORBOARD)
                  .build();

          LogArtifact logArtifactRequest = LogArtifact.newBuilder().setArtifact(artifact).build();
          try {
            experimentRunServiceStub.logArtifact(logArtifactRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          logArtifactRequest = LogArtifact.newBuilder().setId("asda").setArtifact(artifact).build();
          try {
            experimentRunServiceStub.logArtifact(logArtifactRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.ALREADY_EXISTS.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Artifact in ExperimentRun tags Negative test stop................................\n\n");
  }

  @Test
  public void n_getArtifactsTest() throws IOException {
    LOGGER.info(
        "\n\n Get Artifacts from ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetArtifacts getArtifactsRequest =
              GetArtifacts.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .build();

          GetArtifacts.Response response =
              experimentRunServiceStub.getArtifacts(getArtifactsRequest);

          LOGGER.info(
              "GetArtifacts Response : "
                  + response.getArtifactsCount()
                  + " \n"
                  + response.getArtifactsList());
          assertEquals(true, response.getArtifactsList() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Artifacts from ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void n_getArtifactsNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Artifacts from ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetArtifacts getArtifactsRequest = GetArtifacts.newBuilder().build();

          try {
            experimentRunServiceStub.getArtifacts(getArtifactsRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          getArtifactsRequest = GetArtifacts.newBuilder().setId("dssaa").build();

          try {
            experimentRunServiceStub.getArtifacts(getArtifactsRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Artifacts from ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void o_logHyperparameterTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Hyperparameter in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          Value blobValue =
              Value.newBuilder().setStringValue("this is a blob data example").build();
          KeyValue hyperparameter =
              KeyValue.newBuilder()
                  .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
                  .setValue(blobValue)
                  .setValueType(ValueType.BLOB)
                  .build();

          LogHyperparameter logHyperparameterRequest =
              LogHyperparameter.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setHyperparameter(hyperparameter)
                  .build();

          LogHyperparameter.Response response =
              experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);

          LOGGER.info("LogHyperparameter Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Hyperparameter in ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void o_logHyperparameterNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Hyperparameter in ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {
          Value blobValue =
              Value.newBuilder().setStringValue("this is a blob data example").build();
          KeyValue hyperparameter =
              KeyValue.newBuilder()
                  .setKey("Log new hyperparameter " + Calendar.getInstance().getTimeInMillis())
                  .setValue(blobValue)
                  .setValueType(ValueType.BLOB)
                  .build();

          LogHyperparameter logHyperparameterRequest =
              LogHyperparameter.newBuilder().setHyperparameter(hyperparameter).build();

          try {
            experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          logHyperparameterRequest =
              LogHyperparameter.newBuilder()
                  .setId("dsdsfs")
                  .setHyperparameter(hyperparameter)
                  .build();

          try {
            experimentRunServiceStub.logHyperparameter(logHyperparameterRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.ALREADY_EXISTS.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Hyperparameter in ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void p_getHyperparametersTest() throws IOException {
    LOGGER.info(
        "\n\n Get Hyperparameters from ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetHyperparameters getHyperparametersRequest =
              GetHyperparameters.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .build();

          GetHyperparameters.Response response =
              experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);

          LOGGER.info(
              "GetHyperparameters Response : "
                  + response.getHyperparametersCount()
                  + " \n"
                  + response.getHyperparametersList());
          assertEquals(true, response.getHyperparametersList() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Hyperparameters from ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void p_getHyperparametersNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Hyperparameters from ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetHyperparameters getHyperparametersRequest = GetHyperparameters.newBuilder().build();

          try {
            experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          getHyperparametersRequest = GetHyperparameters.newBuilder().setId("sdsssd").build();

          try {
            experimentRunServiceStub.getHyperparameters(getHyperparametersRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Hyperparameters from ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void q_logAttributeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Attribute in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {
          Value blobValue =
              Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
          KeyValue attribute =
              KeyValue.newBuilder()
                  .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
                  .setValue(blobValue)
                  .setValueType(ValueType.BLOB)
                  .build();

          LogAttribute logAttributeRequest =
              LogAttribute.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setAttribute(attribute)
                  .build();

          LogAttribute.Response response =
              experimentRunServiceStub.logAttribute(logAttributeRequest);

          LOGGER.info("LogAttribute Response : \n" + response.getExperimentRun());
          assertEquals(true, response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Attribute in ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void q_logAttributeNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Attribute in ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {
          Value blobValue =
              Value.newBuilder().setStringValue("this is a blob data example of attribute").build();
          KeyValue attribute =
              KeyValue.newBuilder()
                  .setKey("Log new attribute " + Calendar.getInstance().getTimeInMillis())
                  .setValue(blobValue)
                  .setValueType(ValueType.BLOB)
                  .build();

          LogAttribute logAttributeRequest =
              LogAttribute.newBuilder().setAttribute(attribute).build();

          try {
            experimentRunServiceStub.logAttribute(logAttributeRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          logAttributeRequest =
              LogAttribute.newBuilder().setId("sdsds").setAttribute(attribute).build();

          try {
            experimentRunServiceStub.logAttribute(logAttributeRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.ALREADY_EXISTS.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Log Attribute in ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void qq_addExperimentRunAttributes() throws IOException {
    LOGGER.info("\n\n Add ExperimentRun Attributes test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          List<KeyValue> attributeList = new ArrayList<KeyValue>();
          Value intValue = Value.newBuilder().setNumberValue(1.1).build();
          attributeList.add(
              KeyValue.newBuilder()
                  .setKey("attribute_1" + Calendar.getInstance().getTimeInMillis())
                  .setValue(intValue)
                  .setValueType(ValueType.NUMBER)
                  .build());
          Value stringValue =
              Value.newBuilder()
                  .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
                  .build();
          attributeList.add(
              KeyValue.newBuilder()
                  .setKey("attribute_2" + Calendar.getInstance().getTimeInMillis())
                  .setValue(stringValue)
                  .setValueType(ValueType.BLOB)
                  .build());

          AddExperimentRunAttributes request =
              AddExperimentRunAttributes.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .addAllAttributes(attributeList)
                  .build();

          AddExperimentRunAttributes.Response response =
              experimentRunServiceStub.addExperimentRunAttributes(request);
          LOGGER.info("AddExperimentRunAttributes Response : \n" + response.getExperimentRun());
          assertTrue(response.getExperimentRun().getAttributesList().containsAll(attributeList));

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Add ExperimentRun Attributes test stop................................\n\n");
  }

  @Test
  public void qq_addExperimentRunAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Add ExperimentRun attributes Negative test start................................\n\n");

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    AddExperimentRunAttributes request =
        AddExperimentRunAttributes.newBuilder().setId("xyz").build();

    try {
      experimentRunServiceStub.addExperimentRunAttributes(request);
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      LOGGER.log(
          Level.WARNING,
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

      AddExperimentRunAttributes addAttributesRequest =
          AddExperimentRunAttributes.newBuilder().build();
      try {
        experimentRunServiceStub.addExperimentRunAttributes(addAttributesRequest);
      } catch (StatusRuntimeException ex) {
        status = Status.fromThrowable(ex);
        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
      }
    }

    LOGGER.info(
        "\n\n Add ExperimentRun attributes Negative test stop................................\n\n");
  }

  @Test
  public void r_getExperimentRunAttributesTest() throws IOException {
    LOGGER.info(
        "\n\n Get Attributes from ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          ExperimentRun experimentRun = experimentRunResponse.getExperimentRunsList().get(0);

          List<KeyValue> attributes = experimentRun.getAttributesList();
          LOGGER.info("Attributes size : " + attributes.size());

          if (attributes.size() == 0) {
            LOGGER.warning("Experiment Attributes not found in database ");
            assertTrue(false);
            return;
          }

          List<String> keys = new ArrayList<>();
          if (attributes.size() > 1) {
            for (int index = 0; index < attributes.size() - 1; index++) {
              KeyValue keyValue = attributes.get(index);
              keys.add(keyValue.getKey());
            }
          } else {
            keys.add(attributes.get(0).getKey());
          }
          LOGGER.info("Attributes key size : " + keys.size());

          GetAttributes getAttributesRequest =
              GetAttributes.newBuilder()
                  .setId(experimentRun.getId())
                  .addAllAttributeKeys(keys)
                  .build();

          GetAttributes.Response response =
              experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);

          LOGGER.info(
              "GetAttributes Response : "
                  + response.getAttributesCount()
                  + " \n"
                  + response.getAttributesList());
          assertEquals(keys.size(), response.getAttributesList().size());

          getAttributesRequest =
              GetAttributes.newBuilder().setId(experimentRun.getId()).setGetAll(true).build();

          response = experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);

          LOGGER.info(
              "GetAttributes Response : "
                  + response.getAttributesCount()
                  + " \n"
                  + response.getAttributesList());
          assertEquals(attributes.size(), response.getAttributesList().size());

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Attributes from ExperimentRun tags test stop................................\n\n");
  }

  @Test
  public void r_getExperimentRunAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Attributes from ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetAttributes getAttributesRequest = GetAttributes.newBuilder().build();

          try {
            experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          getAttributesRequest = GetAttributes.newBuilder().setId("dssdds").setGetAll(true).build();

          try {
            experimentRunServiceStub.getExperimentRunAttributes(getAttributesRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Get Attributes from ExperimentRun Negative tags test stop................................\n\n");
  }

  @Test
  public void rrr_deleteExperimentRunAttributes() throws IOException {
    LOGGER.info(
        "\n\n Delete ExperimentRun Attributes test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          ExperimentRun experimentRun = experimentRunResponse.getExperimentRunsList().get(0);
          List<KeyValue> attributes = experimentRun.getAttributesList();
          LOGGER.info("Attributes size : " + attributes.size());
          List<String> keys = new ArrayList<>();
          for (int index = 0; index < attributes.size() - 1; index++) {
            KeyValue keyValue = attributes.get(index);
            keys.add(keyValue.getKey());
          }
          LOGGER.info("Attributes key size : " + keys.size());

          DeleteExperimentRunAttributes request =
              DeleteExperimentRunAttributes.newBuilder()
                  .setId(experimentRun.getId())
                  .addAllAttributeKeys(keys)
                  .build();

          DeleteExperimentRunAttributes.Response response =
              experimentRunServiceStub.deleteExperimentRunAttributes(request);
          LOGGER.info(
              "DeleteExperimentRunAttributes Response : \n"
                  + response.getExperimentRun().getAttributesList());
          assertTrue(response.getExperimentRun().getAttributesList().size() <= 1);

          if (response.getExperimentRun().getAttributesList().size() != 0) {
            request =
                DeleteExperimentRunAttributes.newBuilder()
                    .setId(experimentRun.getId())
                    .setDeleteAll(true)
                    .build();

            response = experimentRunServiceStub.deleteExperimentRunAttributes(request);
            LOGGER.info(
                "DeleteExperimentRunAttributes Response : \n"
                    + response.getExperimentRun().getAttributesList());
            assertTrue(response.getExperimentRun().getAttributesList().size() == 0);
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Delete ExperimentRun Attributes test stop................................\n\n");
  }

  @Test
  public void rrr_deleteExperimentRunAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Delete ExperimentRun Attributes Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          ExperimentRun experimentRun = experimentRunResponse.getExperimentRunsList().get(0);

          DeleteExperimentRunAttributes request =
              DeleteExperimentRunAttributes.newBuilder().build();

          try {
            experimentRunServiceStub.deleteExperimentRunAttributes(request);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            try {
              request =
                  DeleteExperimentRunAttributes.newBuilder()
                      .setId(experimentRun.getId())
                      .setDeleteAll(true)
                      .build();

              experimentRunServiceStub.deleteExperimentRunAttributes(request);
            } catch (StatusRuntimeException ex) {
              status = Status.fromThrowable(ex);
              LOGGER.log(
                  Level.WARNING,
                  "Error Code : " + status.getCode() + " Description : " + status.getDescription());
              assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
            }
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info(
        "\n\n Delete ExperimentRun Attributes Negative test stop................................\n\n");
  }

  @Test
  public void s_findExperimentRunsTest() throws IOException {
    LOGGER.info("\n\n FindExperimentRuns test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      Project project = getProjectsResponse.getProjectsList().get(0);
      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          GetExperimentRunsInProject getExperimentRunProjectRequest =
              GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();

          List<String> experimentRunIds = new ArrayList<>();

          try {
            GetExperimentRunsInProject.Response experimentRunResponse =
                experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

            if (experimentRunResponse.getExperimentRunsList() != null
                && experimentRunResponse.getExperimentRunsList().size() > 0) {
              for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
                experimentRunIds.add(experimentRun.getId());
              }
            }
            System.out.println("ExperimentRun Ids : " + experimentRunIds);
          } catch (StatusRuntimeException e) {
            Status.fromThrowable(e);
          }

          List<KeyValueQuery> predicates = new ArrayList<>();
          Value stringValueType = Value.newBuilder().setStringValue("").build();

          KeyValueQuery keyValueQuery =
              KeyValueQuery.newBuilder()
                  .setKey("metrics.loss")
                  .setValue(stringValueType)
                  .setOperator(Operator.LTE)
                  .build();
          predicates.add(keyValueQuery);

          FindExperimentRuns findExperimentRuns =
              FindExperimentRuns.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllExperimentRunIds(experimentRunIds)
                  .addAllPredicates(predicates)
                  // .setIdsOnly(true)
                  .build();

          try {
            experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
          } catch (StatusRuntimeException exc) {
            Status status = Status.fromThrowable(exc);
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          predicates = new ArrayList<>();
          Value numValue = Value.newBuilder().setNumberValue(17.1716586149719).build();

          Struct.Builder struct = Struct.newBuilder();
          struct.putFields("number_value", numValue);
          struct.build();
          Value structValue = Value.newBuilder().setStructValue(struct).build();

          keyValueQuery =
              KeyValueQuery.newBuilder()
                  .setKey("metrics.loss")
                  .setValue(structValue)
                  .setOperator(Operator.LTE)
                  .build();
          predicates.add(keyValueQuery);

          findExperimentRuns =
              FindExperimentRuns.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllExperimentRunIds(experimentRunIds)
                  .addAllPredicates(predicates)
                  // .setIdsOnly(true)
                  .build();

          try {
            experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
          } catch (StatusRuntimeException exc) {
            Status status = Status.fromThrowable(exc);
            assertEquals(Status.UNIMPLEMENTED.getCode(), status.getCode());
          }

          predicates = new ArrayList<>();
          keyValueQuery =
              KeyValueQuery.newBuilder()
                  .setKey("metrics.loss")
                  .setValue(numValue)
                  .setOperator(Operator.LTE)
                  .build();
          predicates.add(keyValueQuery);

          findExperimentRuns =
              FindExperimentRuns.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllExperimentRunIds(experimentRunIds)
                  .addAllPredicates(predicates)
                  // .setIdsOnly(true)
                  .build();

          FindExperimentRuns.Response response =
              experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
          response.getExperimentRunsList();
          LOGGER.info("FindExperimentRuns Response : " + response.getExperimentRunsList());
          assertTrue(response.getExperimentRunsList() != null);

          predicates = new ArrayList<>();
          numValue = Value.newBuilder().setNumberValue(15.1145829108347).build();
          keyValueQuery =
              KeyValueQuery.newBuilder()
                  .setKey("metrics.loss")
                  .setValue(numValue)
                  .setOperator(Operator.LTE)
                  .build();
          predicates.add(keyValueQuery);

          numValue = Value.newBuilder().setNumberValue(10.1875074891223).build();
          KeyValueQuery keyValueQuery2 =
              KeyValueQuery.newBuilder()
                  .setKey("metrics.accuracy")
                  .setValue(numValue)
                  .setOperator(Operator.EQ)
                  .build();
          predicates.add(keyValueQuery2);

          findExperimentRuns =
              FindExperimentRuns.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllExperimentRunIds(experimentRunIds)
                  .addAllPredicates(predicates)
                  // .setIdsOnly(true)
                  .build();

          response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
          response.getExperimentRunsList();
          LOGGER.info("FindExperimentRuns Response : " + response.getExperimentRunsList());
          assertTrue(response.getExperimentRunsList() != null);

          predicates = new ArrayList<>();
          numValue = Value.newBuilder().setNumberValue(2.0).build();
          keyValueQuery =
              KeyValueQuery.newBuilder()
                  .setKey("hyperparameters.tuning")
                  .setValue(numValue)
                  .setOperator(Operator.EQ)
                  .build();
          predicates.add(keyValueQuery);

          numValue = Value.newBuilder().setNumberValue(10.8274718461159).build();
          keyValueQuery2 =
              KeyValueQuery.newBuilder()
                  .setKey("metrics.accuracy")
                  .setValue(numValue)
                  .setOperator(Operator.GTE)
                  .build();
          predicates.add(keyValueQuery2);

          findExperimentRuns =
              FindExperimentRuns.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllExperimentRunIds(experimentRunIds)
                  .addAllPredicates(predicates)
                  // .setIdsOnly(true)
                  .build();

          response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
          response.getExperimentRunsList();
          LOGGER.info("FindExperimentRuns Response : " + response.getExperimentRunsList());
          assertTrue(response.getExperimentRunsList() != null);

          predicates = new ArrayList<>();
          Value stringValue = Value.newBuilder().setStringValue("1550837525154").build();
          keyValueQuery =
              KeyValueQuery.newBuilder()
                  .setKey("endTime")
                  .setValue(stringValue)
                  .setOperator(Operator.EQ)
                  .build();
          predicates.add(keyValueQuery);

          findExperimentRuns =
              FindExperimentRuns.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllExperimentRunIds(experimentRunIds)
                  .addAllPredicates(predicates)
                  // .setIdsOnly(true)
                  .build();

          response = experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
          response.getExperimentRunsList();
          LOGGER.info("FindExperimentRuns Response : " + response.getExperimentRunsList());
          assertTrue(response.getExperimentRunsList() != null);

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n FindExperimentRuns test stop................................\n\n");
  }

  @Test
  public void s_findExperimentRunsNegativeTest() throws IOException {
    LOGGER.info("\n\n FindExperimentRuns Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      Project project = getProjectsResponse.getProjectsList().get(0);
      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          FindExperimentRuns findExperimentRuns = FindExperimentRuns.newBuilder().build();

          try {
            experimentRunServiceStub.findExperimentRuns(findExperimentRuns);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n FindExperimentRuns Negative test stop................................\n\n");
  }

  @Test
  public void t_sortExperimentRunsTest() throws IOException {
    LOGGER.info("\n\n SortExperimentRuns test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      Project project = getProjectsResponse.getProjectsList().get(0);
      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          GetExperimentRunsInProject getExperimentRunProjectRequest =
              GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();

          List<String> experimentRunIds = new ArrayList<>();

          try {
            GetExperimentRunsInProject.Response experimentRunResponse =
                experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

            if (experimentRunResponse.getExperimentRunsList() != null
                && experimentRunResponse.getExperimentRunsList().size() > 0) {

              for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
                experimentRunIds.add(experimentRun.getId());
              }
            }
            System.out.println("ExperimentRun Ids : " + experimentRunIds);
          } catch (StatusRuntimeException e) {
            Status.fromThrowable(e);
          }

          SortExperimentRuns sortExperimentRuns =
              SortExperimentRuns.newBuilder()
                  .addAllExperimentRunIds(experimentRunIds)
                  .setSortKey("metrics.accuracy")
                  // .setIdsOnly(true)
                  .setAscending(true)
                  .build();

          SortExperimentRuns.Response response =
              experimentRunServiceStub.sortExperimentRuns(sortExperimentRuns);
          response.getExperimentRunsList();
          LOGGER.info("SortExperimentRuns Response : " + response.getExperimentRunsList());
          assertTrue(response.getExperimentRunsList() != null);

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n SortExperimentRuns test stop................................\n\n");
  }

  @Test
  public void t_sortExperimentRunsNegativeTest() throws IOException {
    LOGGER.info("\n\n SortExperimentRuns Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      Project project = getProjectsResponse.getProjectsList().get(0);
      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          SortExperimentRuns sortExperimentRuns =
              SortExperimentRuns.newBuilder().setSortKey("endTime").setIdsOnly(true).build();

          try {
            experimentRunServiceStub.sortExperimentRuns(sortExperimentRuns);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n SortExperimentRuns Negative test stop................................\n\n");
  }

  @Test
  public void u_getTopExperimentRunsTest() throws IOException {
    LOGGER.info("\n\n TopExperimentRuns test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      Project project = getProjectsResponse.getProjectsList().get(0);
      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);
          Experiment experiment = experimentResponse.getExperimentsList().get(0);

          TopExperimentRunsSelector topExperimentRunsSelector =
              TopExperimentRunsSelector.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .setSortKey("metrics.accuracy")
                  .setTopK(4)
                  .setAscending(true)
                  // .setIdsOnly(true)
                  .build();

          TopExperimentRunsSelector.Response response =
              experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
          response.getExperimentRunsList();
          LOGGER.info("TopExperimentRuns Response : " + response.getExperimentRunsList());
          LOGGER.info(
              "TopExperimentRuns Response Record count: "
                  + response.getExperimentRunsList().size());
          assertTrue(response.getExperimentRunsList() != null);

          topExperimentRunsSelector =
              TopExperimentRunsSelector.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .setSortKey("hyperparameters.tuning")
                  .setTopK(20000)
                  .setAscending(true)
                  .build();

          response = experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
          response.getExperimentRunsList();
          LOGGER.info("TopExperimentRuns Response : " + response.getExperimentRunsList());
          LOGGER.info(
              "TopExperimentRuns Response Record count: "
                  + response.getExperimentRunsList().size());
          assertTrue(response.getExperimentRunsList() != null);

          List<String> experimentRunIds = new ArrayList<>();

          try {
            GetExperimentRunsInProject getExperimentRunProjectRequest =
                GetExperimentRunsInProject.newBuilder().setProjectId(project.getId()).build();

            GetExperimentRunsInProject.Response experimentRunResponse =
                experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

            if (experimentRunResponse.getExperimentRunsList() != null
                && experimentRunResponse.getExperimentRunsList().size() > 0) {
              for (ExperimentRun experimentRun : experimentRunResponse.getExperimentRunsList()) {
                experimentRunIds.add(experimentRun.getId());
              }
            }
            LOGGER.log(Level.INFO, "ExperimentRun Ids : " + experimentRunIds);

            topExperimentRunsSelector =
                TopExperimentRunsSelector.newBuilder()
                    .addAllExperimentRunIds(experimentRunIds)
                    .setSortKey("endTime")
                    .setTopK(3)
                    // .setAscending(true)
                    // .setIdsOnly(true)
                    .build();

            response = experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
            response.getExperimentRunsList();
            LOGGER.info("TopExperimentRuns Response : " + response.getExperimentRunsList());
            assertTrue(response.getExperimentRunsList() != null);

          } catch (StatusRuntimeException e) {
            Status.fromThrowable(e);
          }

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n TopExperimentRuns test stop................................\n\n");
  }

  @Test
  public void u_getTopExperimentRunsNegativeTest() throws IOException {
    LOGGER.info("\n\n TopExperimentRuns Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      Project project = getProjectsResponse.getProjectsList().get(0);
      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder().setProjectId(project.getId()).build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        if (experimentResponse.getExperimentsList() != null
            && experimentResponse.getExperimentsList().size() > 0) {

          ExperimentRunServiceBlockingStub experimentRunServiceStub =
              ExperimentRunServiceGrpc.newBlockingStub(channel);

          TopExperimentRunsSelector topExperimentRunsSelector =
              TopExperimentRunsSelector.newBuilder()
                  .setProjectId(project.getId())
                  .setExperimentId(experimentResponse.getExperimentsList().get(0).getId())
                  .setTopK(4)
                  .setAscending(true)
                  .build();

          try {
            experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            topExperimentRunsSelector =
                TopExperimentRunsSelector.newBuilder()
                    .setSortKey("endTime")
                    .setTopK(4)
                    .setAscending(true)
                    .build();

            try {
              experimentRunServiceStub.getTopExperimentRuns(topExperimentRunsSelector);
            } catch (StatusRuntimeException exc) {
              status = Status.fromThrowable(exc);
              status.getCode();
              status.getDescription();

              LOGGER.log(
                  Level.WARNING,
                  "Error Code : " + status.getCode() + " Description : " + status.getDescription());
              assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
            }
          }

        } else {
          LOGGER.log(Level.WARNING, "Experiment not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n TopExperimentRuns Negative test stop................................\n\n");
  }

  @Test
  public void v_logJobIdTest() throws IOException {
    LOGGER.info("\n\n  Log Job Id in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          String jobId = "xyz";
          LogJobId logJobIdRequest =
              LogJobId.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setJobId(jobId)
                  .build();

          LogJobId.Response response = experimentRunServiceStub.logJobId(logJobIdRequest);

          LOGGER.info("LogJobId Response : \n" + response.getExperimentRun());
          assertEquals(jobId, response.getExperimentRun().getJobId());

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Log Job Id in ExperimentRun test stop................................\n\n");
  }

  @Test
  public void v_logJobIdNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Log Job Id in ExperimentRun Negative test start................................\n\n");

    String jobId = "xyz";
    LogJobId logJobIdRequest = LogJobId.newBuilder().setJobId(jobId).build();

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);
    try {

      experimentRunServiceStub.logJobId(logJobIdRequest);
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      status.getCode();
      status.getDescription();

      LOGGER.log(
          Level.WARNING,
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

      try {
        logJobIdRequest = LogJobId.newBuilder().setId("abc").build();
        experimentRunServiceStub.logJobId(logJobIdRequest);
      } catch (StatusRuntimeException e) {
        status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
      }
    }

    LOGGER.info(
        "\n\n Log Job Id in ExperimentRun Negative test stop................................\n\n");
  }

  @Test
  public void w_getJobIdTest() throws IOException {
    LOGGER.info("\n\n  Get Job Id in ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          GetJobId getJobIdRequest =
              GetJobId.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .build();

          GetJobId.Response response = experimentRunServiceStub.getJobId(getJobIdRequest);

          LOGGER.info("GetJobId Response : \n" + response.getJobId());
          assertEquals("xyz", response.getJobId());

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Get Job Id in ExperimentRun test stop................................\n\n");
  }

  @Test
  public void w_getJobIdNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n  Get Job Id in ExperimentRun Negative test start................................\n\n");

    GetJobId getJobIdRequest = GetJobId.newBuilder().build();

    ExperimentRunServiceBlockingStub experimentRunServiceStub =
        ExperimentRunServiceGrpc.newBlockingStub(channel);

    try {
      experimentRunServiceStub.getJobId(getJobIdRequest);
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);
      status.getCode();
      status.getDescription();

      LOGGER.log(
          Level.WARNING,
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info(
        "\n\n Get Job Id in ExperimentRun Negative test stop................................\n\n");
  }

  @Test
  @Ignore
  public void z_deleteExperimentRunTest() throws IOException {
    LOGGER.info("\n\n Delete ExperimentRun test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          DeleteExperimentRun request =
              DeleteExperimentRun.newBuilder()
                  .setId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .build();

          DeleteExperimentRun.Response response =
              experimentRunServiceStub.deleteExperimentRun(request);

          LOGGER.info("DeleteExperimentRun Response : \n" + response.getStatus());
          assertEquals(true, response.getStatus());

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Delete ExperimentRun test stop................................\n\n");
  }

  @Test
  @Ignore
  public void z_deleteExperimentRunNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Delete ExperimentRun Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      GetExperimentRunsInProject getExperimentRunProjectRequest =
          GetExperimentRunsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentRunsInProject.Response experimentRunResponse =
            experimentRunServiceStub.getExperimentRunsInProject(getExperimentRunProjectRequest);

        if (experimentRunResponse.getExperimentRunsList() != null
            && experimentRunResponse.getExperimentRunsList().size() > 0) {

          DeleteExperimentRun request = DeleteExperimentRun.newBuilder().build();

          try {
            experimentRunServiceStub.deleteExperimentRun(request);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }
        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun not found in database");
        }
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }

    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }

    LOGGER.info("\n\n Delete ExperimentRun Negative test stop................................\n\n");
  }
}
