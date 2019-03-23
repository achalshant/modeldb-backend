package com.mitdbg.modeldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.mitdbg.modeldb.ArtifactTypeEnum.ArtifactType;
import com.mitdbg.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import com.mitdbg.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
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
public class IntegrationTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;

  public static final Logger LOGGER = Logger.getLogger(IntegrationTest.class.getName());

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
    this.channel = grpcCleanup.register(channelBuilder.maxInboundMessageSize(1024).build());
  }

  @After
  public void clientClose() {
    if (!channel.isShutdown()) {
      channel.shutdownNow();
    }
  }

  private CreateProject createProjectRequest() {
    String projectName = "project_" + Calendar.getInstance().getTimeInMillis();

    List<KeyValue> metadataList = new ArrayList<>();
    for (int count = 0; count < 5; count++) {
      Value stringValue =
          Value.newBuilder()
              .setStringValue(
                  "attribute_" + count + "_" + Calendar.getInstance().getTimeInMillis() + "_value")
              .build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey("attribute_" + count + "_" + Calendar.getInstance().getTimeInMillis())
              .setValue(stringValue)
              .setValueType(ValueType.STRING)
              .build();
      metadataList.add(keyValue);
    }

    CreateProject request =
        CreateProject.newBuilder()
            .setName(projectName)
            .setDescription("This is a project description.")
            .addTags("tag_" + Calendar.getInstance().getTimeInMillis())
            .addTags("tag_" + +Calendar.getInstance().getTimeInMillis())
            .addAllMetadata(metadataList)
            .build();
    return request;
  }

  private CreateExperiment createExperimentRequest(String projectId) {
    List<KeyValue> attributeList = new ArrayList<KeyValue>();
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
    stringValue =
        Value.newBuilder()
            .setStringValue("attributes_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    attributeList.add(
        KeyValue.newBuilder()
            .setKey("attribute_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
            .build());

    CreateExperiment request =
        CreateExperiment.newBuilder()
            .setProjectId(projectId)
            .setName("experiment_" + Calendar.getInstance().getTimeInMillis())
            .setDescription("This is a experiment description.")
            .addTags("tag_" + Calendar.getInstance().getTimeInMillis())
            .addTags("tag_" + +Calendar.getInstance().getTimeInMillis())
            .addAllAttributes(attributeList)
            .build();
    return request;
  }

  private CreateExperimentRun createExperimentRunRequest(String projectId, String experimentId) {

    List<String> tags = new ArrayList<String>();
    tags.add("Tag 1 " + Calendar.getInstance().getTimeInMillis());
    tags.add("Tag 2 " + Calendar.getInstance().getTimeInMillis());

    List<KeyValue> attributeList = new ArrayList<KeyValue>();
    Value intValue =
        Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
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

    List<KeyValue> hyperparameters = new ArrayList<KeyValue>();
    intValue =
        Value.newBuilder().setNumberValue(1 + Calendar.getInstance().getTimeInMillis()).build();
    hyperparameters.add(
        KeyValue.newBuilder()
            .setKey("hyperparameters_" + Calendar.getInstance().getTimeInMillis())
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
    Value listValue =
        Value.newBuilder().setListValue(ListValue.newBuilder().addValues(intValue)).build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("metrics_" + Calendar.getInstance().getTimeInMillis())
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build());
    stringValue =
        Value.newBuilder()
            .setStringValue("metrics_value_" + Calendar.getInstance().getTimeInMillis())
            .build();
    metrics.add(
        KeyValue.newBuilder()
            .setKey("metrics_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .setValueType(ValueType.STRING)
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
            .setStringValue("observation_value_" + Calendar.getInstance().getTimeInMillis())
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
            .setName("ExperimentRun Name")
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
  public void a_AllEntityCRUDTest() throws IOException {
    LOGGER.info("\n\n All Entity CRUD Test start................................\n\n");
    try {
      final ProjectServiceBlockingStub projectServiceStub =
          ProjectServiceGrpc.newBlockingStub(channel);

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      ExperimentRunServiceBlockingStub experimentRunServiceStub =
          ExperimentRunServiceGrpc.newBlockingStub(channel);

      LOGGER.info("\n\n Create Project test start................................\n\n");
      CreateProject createProjectRequest = createProjectRequest();
      CreateProject.Response createProjectRequestResponse =
          projectServiceStub.createProject(createProjectRequest);
      Project project = createProjectRequestResponse.getProject();
      assertEquals(true, project != null);
      LOGGER.info("Create Project test successfully executed");
      LOGGER.info("\n\n Create Project test stop................................\n\n");

      LOGGER.info("\n\n Create Experiment test start................................\n\n");
      CreateExperiment createExperimentRequest = createExperimentRequest(project.getId());
      CreateExperiment.Response createExperimentResponse =
          experimentServiceStub.createExperiment(createExperimentRequest);
      Experiment experiment = createExperimentResponse.getExperiment();
      assertEquals(true, experiment != null);

      CreateExperiment createExperimentRequest2 = createExperimentRequest(project.getId());
      CreateExperiment.Response createExperimentResponse2 =
          experimentServiceStub.createExperiment(createExperimentRequest2);
      Experiment experiment2 = createExperimentResponse2.getExperiment();
      assertEquals(true, experiment2 != null);
      LOGGER.info("Create Experiment test successfully executed");
      LOGGER.info("\n\n Create Experiment test stop................................\n\n");

      LOGGER.info("\n\n Create ExperimentRun test start................................\n\n");
      CreateExperimentRun createExperimentRunRequest =
          createExperimentRunRequest(project.getId(), experiment.getId());
      CreateExperimentRun.Response createExperimentRunResponse =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest);
      ExperimentRun experimentRun = createExperimentRunResponse.getExperimentRun();
      assertEquals(true, experimentRun != null);

      CreateExperimentRun createExperimentRunRequest2 =
          createExperimentRunRequest(project.getId(), experiment.getId());
      CreateExperimentRun.Response createExperimentRunResponse2 =
          experimentRunServiceStub.createExperimentRun(createExperimentRunRequest2);
      ExperimentRun experimentRun2 = createExperimentRunResponse2.getExperimentRun();
      assertEquals(true, experimentRun2 != null);
      LOGGER.info("Create ExperimentRun test successfully executed");
      LOGGER.info("\n\n Create ExperimentRun test stop................................\n\n");

      LOGGER.info("\n\n Get Project test start................................\n\n");
      GetProjects getProjects = GetProjects.newBuilder().build();
      GetProjects.Response getProjectResponse = projectServiceStub.getProjects(getProjects);
      assertEquals(true, getProjectResponse.getProjectsList().get(0) != null);
      LOGGER.info("Get project test successfully executed");
      LOGGER.info("\n\n Get project test stop................................\n\n");

      LOGGER.info("\n\n Get Experiment test start................................\n\n");
      GetExperimentById getExperimentRequest =
          GetExperimentById.newBuilder().setId(experiment.getId()).build();
      GetExperimentById.Response getExperimentResponse =
          experimentServiceStub.getExperimentById(getExperimentRequest);
      assertEquals(true, getExperimentResponse.getExperiment() != null);
      LOGGER.info("Get Experiment test successfully executed");
      LOGGER.info("\n\n Get Experiment of project test stop................................\n\n");

      LOGGER.info("\n\n Get ExperimentRun test start................................\n\n");
      GetExperimentRunById request =
          GetExperimentRunById.newBuilder().setId(experimentRun.getId()).build();
      GetExperimentRunById.Response response =
          experimentRunServiceStub.getExperimentRunById(request);
      assertEquals(true, response.getExperimentRun() != null);
      LOGGER.info("Get ExperimentRun test successfully executed");
      LOGGER.info("\n\n Get ExperimentRun test stop................................\n\n");

      LOGGER.info("\n\n Delete ExperimentRun test start................................\n\n");
      DeleteExperimentRun deleteExperimentRunRequest =
          DeleteExperimentRun.newBuilder().setId(experimentRun.getId()).build();
      DeleteExperimentRun.Response deleteExperimentRunResponse =
          experimentRunServiceStub.deleteExperimentRun(deleteExperimentRunRequest);
      assertEquals(true, deleteExperimentRunResponse.getStatus());
      LOGGER.info("Delete ExperimentRun test successfully executed");
      LOGGER.info("\n\n Delete ExperimentRun test stop................................\n\n");

      LOGGER.info("\n\n Delete Experiment test start................................\n\n");
      DeleteExperiment deleteExperimentRequest =
          DeleteExperiment.newBuilder().setId(experiment.getId()).build();
      DeleteExperiment.Response deleteExperimentResponse =
          experimentServiceStub.deleteExperiment(deleteExperimentRequest);
      assertEquals(true, deleteExperimentResponse.getStatus());
      LOGGER.info("Delete Experiment test successfully executed");
      LOGGER.info("\n\n Delete Experiment test stop................................\n\n");

      LOGGER.info("\n\n Project delete test start................................\n\n");
      DeleteProject deleteProjectRequest =
          DeleteProject.newBuilder().setId(project.getId()).build();
      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProjectRequest);
      assertEquals(true, deleteProjectResponse.getStatus());
      LOGGER.info("Project delete test successfully executed");
      LOGGER.info("\n\n Project delete test stop................................\n\n");

    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      status.getCode();
      status.getDescription();

      LOGGER.log(
          Level.WARNING,
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      assertTrue(false);
    }

    LOGGER.info("\n\n All Entity CRUD Test stop................................\n\n");
  }
}
