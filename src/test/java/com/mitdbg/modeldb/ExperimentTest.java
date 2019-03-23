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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.mitdbg.modeldb.ExperimentServiceGrpc.ExperimentServiceBlockingStub;
import com.mitdbg.modeldb.ExperimentServiceGrpc.ExperimentServiceStub;
import com.mitdbg.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import com.mitdbg.modeldb.ValueTypeEnum.ValueType;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExperimentTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;

  public static final Logger LOGGER = Logger.getLogger(ExperimentTest.class.getName());

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

  @Test
  public void a_experimentCreateTest() throws IOException {
    LOGGER.info("\n\n Create Experiment test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      List<KeyValue> attributeList = new ArrayList<KeyValue>();
      Value stringValue =
          Value.newBuilder()
              .setStringValue("attribute_" + Calendar.getInstance().getTimeInMillis() + "_value")
              .build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey("attribute_1_" + Calendar.getInstance().getTimeInMillis())
              .setValue(stringValue)
              .build();
      attributeList.add(keyValue);

      Value intValue = Value.newBuilder().setNumberValue(12345).build();
      keyValue =
          KeyValue.newBuilder()
              .setKey("attribute_2_" + Calendar.getInstance().getTimeInMillis())
              .setValue(intValue)
              .setValueType(ValueType.NUMBER)
              .build();
      attributeList.add(keyValue);

      Value listValue =
          Value.newBuilder()
              .setListValue(
                  ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
              .build();
      keyValue =
          KeyValue.newBuilder()
              .setKey("attribute_3_" + Calendar.getInstance().getTimeInMillis())
              .setValue(listValue)
              .setValueType(ValueType.LIST)
              .build();
      attributeList.add(keyValue);

      CreateExperiment request =
          CreateExperiment.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .setName("experiment_" + Calendar.getInstance().getTimeInMillis())
              .setDescription("This is a experiment description.")
              .setDateCreated(Calendar.getInstance().getTimeInMillis())
              .setDateUpdated(Calendar.getInstance().getTimeInMillis())
              .addTags("tag_" + Calendar.getInstance().getTimeInMillis())
              .addTags("tag_" + +Calendar.getInstance().getTimeInMillis())
              .addAllAttributes(attributeList)
              .build();

      CreateExperiment.Response response = experimentServiceStub.createExperiment(request);
      LOGGER.info("Experiment created : " + response.getExperiment());
      assertEquals(request.getName(), response.getExperiment().getName());

      try {
        experimentServiceStub.createExperiment(request);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
        assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
      }
    } else {
      LOGGER.log(Level.WARNING, "Project not found in database");
    }
    LOGGER.info("\n\n Create Experiment test stop................................\n\n");
  }

  @Test
  public void a_experimentCreateNegativeTest() throws IOException {
    LOGGER.info("\n\n Create Experiment Negative test start................................\n\n");

    ExperimentServiceStub experimentServiceStub = ExperimentServiceGrpc.newStub(channel);

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
            .setName("experiment_" + Calendar.getInstance().getTimeInMillis())
            .setDescription("This is a experiment description.")
            .addTags("tag_" + Calendar.getInstance().getTimeInMillis())
            .addTags("tag_" + +Calendar.getInstance().getTimeInMillis())
            .addAllAttributes(attributeList)
            .build();

    experimentServiceStub.createExperiment(
        request,
        new StreamObserver<CreateExperiment.Response>() {

          public void onNext(CreateExperiment.Response value) {}

          public void onError(Throwable t) {
            Status status = Status.fromThrowable(t);
            status.getCode();
            status.getDescription();
            LOGGER.info("Response error : " + status.getDescription());

            assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
          }

          public void onCompleted() {
            LOGGER.info(
                "\n\n Create Experiment Negative test stop................................\n\n");
          }
        });
  }

  @Test
  public void b_getExperimentsInProject() throws IOException {
    LOGGER.info("\n\n Get Experiment of project test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      GetExperimentsInProject getExperiment =
          GetExperimentsInProject.newBuilder()
              .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();
      try {
        GetExperimentsInProject.Response experimentResponse =
            experimentServiceStub.getExperimentsInProject(getExperiment);

        LOGGER.log(
            Level.INFO,
            "GetExperimentsInProject.Response " + experimentResponse.getExperimentsCount());
        assertEquals(
            true,
            experimentResponse.getExperimentsList() != null
                || experimentResponse.getExperimentsList().size() == 0);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status.getCode() + " Description : " + status.getDescription());
      }
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Get Experiment of project test stop................................\n\n");
  }

  @Test
  public void b_getExperimentsWithPaginationInProject() throws IOException {
    LOGGER.info(
        "\n\n Get Experiment with pagination of project test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
          ExperimentServiceGrpc.newBlockingStub(channel);

      try {

        Integer pageLimit = 2;
        for (int pageNumber = 1; pageNumber < 100; pageNumber++) {
          GetExperimentsInProject getExperiment =
              GetExperimentsInProject.newBuilder()
                  .setProjectId(getProjectsResponse.getProjectsList().get(0).getId())
                  .setPageNumber(pageNumber)
                  .setPageLimit(pageLimit)
                  .setSortOrder(ModelDBConstants.ORDER_ASC)
                  .setSortBy(ModelDBConstants.NAME)
                  .build();

          GetExperimentsInProject.Response experimentResponse =
              experimentServiceStub.getExperimentsInProject(getExperiment);

          if (experimentResponse.getExperimentsList() != null
              && experimentResponse.getExperimentsList().size() > 0) {

            LOGGER.info(
                "GetExperimentsInProject Response : "
                    + experimentResponse.getExperimentsCount()
                    + "\n"
                    + experimentResponse.getExperimentsList());

            assertEquals(true, experimentResponse.getExperimentsList() != null);

          } else {
            LOGGER.log(Level.WARNING, "More Experiment not found in database");
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
      }
    } else {
      LOGGER.info("Project not found in database. ");
    }

    LOGGER.info(
        "\n\n Get Experiment with pagination of project test stop................................\n\n");
  }

  @Test
  public void b_getExperimentsInProjectNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Experiment of project Negative test start................................\n\n");

    final ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);

    GetExperimentsInProject getExperiment = GetExperimentsInProject.newBuilder().build();
    try {
      experimentServiceStub.getExperimentsInProject(getExperiment);
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      status.getCode();
      status.getDescription();

      LOGGER.log(
          Level.WARNING,
          "Error Code : " + status.getCode() + " Description : " + status.getDescription());

      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

      getExperiment = GetExperimentsInProject.newBuilder().setProjectId("hjhfdkshjfhdsk").build();
      try {
        experimentServiceStub.getExperimentsInProject(getExperiment);
      } catch (StatusRuntimeException ex) {
        Status status2 = Status.fromThrowable(ex);
        status2.getCode();
        status2.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : " + status2.getCode() + " Description : " + status2.getDescription());

        assertTrue(Status.UNAVAILABLE.getCode().equals(status2.getCode()));
      }
    }

    LOGGER.info(
        "\n\n Get Experiment of project Negative test stop................................\n\n");
  }

  @Test
  public void c_getExperimentById() throws IOException {
    LOGGER.info("\n\n Get Experiment by ID test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          GetExperimentById experimentRequest =
              GetExperimentById.newBuilder()
                  .setId(experimentResponse.getExperimentsList().get(0).getId())
                  .build();

          try {
            GetExperimentById.Response response =
                experimentServiceStub.getExperimentById(experimentRequest);
            LOGGER.info(
                "UpdateExperimentNameOrDescription Response : \n" + response.getExperiment());
            assertEquals(true, response.getExperiment() != null);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Get Experiment by ID of project test stop................................\n\n");
  }

  @Test
  public void c_getExperimentByIdNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Experiment by ID Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          GetExperimentById experimentRequest = GetExperimentById.newBuilder().build();

          try {
            experimentServiceStub.getExperimentById(experimentRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());

            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            experimentRequest = GetExperimentById.newBuilder().setId("jdhfjkdhsfhdskjf").build();
            try {
              experimentServiceStub.getExperimentById(experimentRequest);
            } catch (StatusRuntimeException ex) {
              Status status2 = Status.fromThrowable(ex);
              status2.getCode();
              status2.getDescription();

              LOGGER.log(
                  Level.WARNING,
                  "Error Code : "
                      + status2.getCode()
                      + " Description : "
                      + status2.getDescription());

              assertTrue(Status.NOT_FOUND.getCode().equals(status2.getCode()));
            }
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Get Experiment by ID Negative test stop................................\n\n");
  }

  @Test
  public void d_updateExperimentNameOrDescription() throws IOException {
    LOGGER.info(
        "\n\n Update Experiment Name & Discription test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          UpdateExperimentNameOrDescription upDescriptionRequest =
              UpdateExperimentNameOrDescription.newBuilder()
                  .setId(experimentResponse.getExperimentsList().get(0).getId())
                  .setName(
                      "Test Update Experiment Name Or Description "
                          + Calendar.getInstance().getTimeInMillis())
                  .setDescription(
                      "This is update from UpdateExperimentNameOrDescription "
                          + Calendar.getInstance().getTimeInMillis())
                  .build();

          try {
            UpdateExperimentNameOrDescription.Response response =
                experimentServiceStub.updateExperimentNameOrDescription(upDescriptionRequest);
            LOGGER.info("UpdateExperimentNameOrDescription Response : " + response.getExperiment());
            assertEquals(true, response.getExperiment() != null);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Update Experiment Name & Discription test stop................................\n\n");
  }

  @Test
  public void d_updateExperimentNameOrDescriptionNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Update Experiment Name & Discription Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          Experiment experiment = experimentResponse.getExperimentsList().get(0);
          UpdateExperimentNameOrDescription upDescriptionRequest =
              UpdateExperimentNameOrDescription.newBuilder()
                  .setName(
                      "Test Update Experiment Name Or Description "
                          + Calendar.getInstance().getTimeInMillis())
                  .setDescription(
                      "This is update from UpdateExperimentNameOrDescription "
                          + Calendar.getInstance().getTimeInMillis())
                  .build();

          try {
            experimentServiceStub.updateExperimentNameOrDescription(upDescriptionRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            upDescriptionRequest =
                UpdateExperimentNameOrDescription.newBuilder()
                    .setId(experiment.getId())
                    .setName(experiment.getName())
                    .setDescription(experiment.getDescription())
                    .build();

            try {
              experimentServiceStub.updateExperimentNameOrDescription(upDescriptionRequest);
            } catch (StatusRuntimeException ex) {
              Status status2 = Status.fromThrowable(ex);
              status2.getCode();
              status2.getDescription();

              LOGGER.log(
                  Level.WARNING,
                  "Error Code : "
                      + status2.getCode()
                      + " Description : "
                      + status2.getDescription());
              assertEquals(Status.ALREADY_EXISTS.getCode(), status2.getCode());
            }
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Update Experiment Name & Discription Negative test stop................................\n\n");
  }

  @Test
  public void e_addExperimentTags() throws IOException {
    LOGGER.info("\n\n Add Experiment tags test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          List<String> tags = new ArrayList<String>();
          tags.add("Test Update tag " + Calendar.getInstance().getTimeInMillis());
          tags.add("Test Update tag 2 " + Calendar.getInstance().getTimeInMillis());

          AddExperimentTags updateExperimentTags =
              AddExperimentTags.newBuilder()
                  .setId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllTags(tags)
                  .build();

          try {
            AddExperimentTags.Response response =
                experimentServiceStub.addExperimentTags(updateExperimentTags);
            LOGGER.info("AddExperimentTags Response : " + response.getExperiment());
            assertEquals(true, response.getExperiment() != null);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Add Experiment tags test stop................................\n\n");
  }

  @Test
  public void e_addExperimentTagsNegativeTest() throws IOException {
    LOGGER.info("\n\n Add Experiment tags Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          Experiment experiment = experimentResponse.getExperimentsList().get(0);
          List<String> tags = new ArrayList<String>();
          tags.add("Test Update tag " + Calendar.getInstance().getTimeInMillis());
          tags.add("Test Update tag 2 " + Calendar.getInstance().getTimeInMillis());

          AddExperimentTags updateExperimentTags =
              AddExperimentTags.newBuilder().addAllTags(tags).build();

          try {
            experimentServiceStub.addExperimentTags(updateExperimentTags);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          updateExperimentTags =
              AddExperimentTags.newBuilder()
                  .setId(experiment.getId())
                  .addAllTags(experiment.getTagsList())
                  .build();

          try {
            experimentServiceStub.addExperimentTags(updateExperimentTags);
          } catch (StatusRuntimeException ex) {
            Status status2 = Status.fromThrowable(ex);
            assertEquals(Status.ALREADY_EXISTS.getCode(), status2.getCode());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Add Experiment tags Negative test stop................................\n\n");
  }

  @Test
  public void ee_getExperimentTags() throws IOException {
    LOGGER.info("\n\n Get Experiment tags test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          Experiment experiment = experimentResponse.getExperimentsList().get(0);
          GetTags getExperimentTags = GetTags.newBuilder().setId(experiment.getId()).build();

          GetTags.Response response = experimentServiceStub.getExperimentTags(getExperimentTags);
          LOGGER.info("GetExperimentTags Response : " + response.getTagsList());
          assertEquals(experiment.getTagsList(), response.getTagsList());
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Get Experiment tags test stop................................\n\n");
  }

  @Test
  public void ee_getExperimentTagsNegativeTest() throws IOException {
    LOGGER.info("\n\n Get Experiment tags Negative test start................................\n\n");

    ExperimentServiceBlockingStub experimentServiceStub =
        ExperimentServiceGrpc.newBlockingStub(channel);

    GetTags getExperimentTags = GetTags.newBuilder().build();

    try {
      experimentServiceStub.getExperimentTags(getExperimentTags);
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("\n\n Get Experiment tags Negative test stop................................\n\n");
  }

  @Test
  public void f_deleteExperimentTags() throws IOException {
    LOGGER.info("\n\n Delete Experiment tags test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          try {
            Experiment experiment = experimentResponse.getExperimentsList().get(0);

            List<String> removableTags = experiment.getTagsList();
            if (experiment.getTagsList().size() > 1) {
              removableTags =
                  experiment.getTagsList().subList(0, experiment.getTagsList().size() - 1);
            }

            DeleteExperimentTags deleteExperimentTags =
                DeleteExperimentTags.newBuilder()
                    .setId(experiment.getId())
                    .addAllTags(removableTags)
                    .build();

            DeleteExperimentTags.Response response =
                experimentServiceStub.deleteExperimentTags(deleteExperimentTags);
            LOGGER.info(
                "DeleteExperimentTags Response : " + response.getExperiment().getTagsList());
            assertTrue(response.getExperiment().getTagsList().size() <= 1);

            if (response.getExperiment().getTagsList().size() > 0) {
              deleteExperimentTags =
                  DeleteExperimentTags.newBuilder()
                      .setId(experiment.getId())
                      .setDeleteAll(true)
                      .build();

              response = experimentServiceStub.deleteExperimentTags(deleteExperimentTags);
              LOGGER.info(
                  "DeleteExperimentTags Response : " + response.getExperiment().getTagsList());
              assertTrue(response.getExperiment().getTagsList().size() == 0);
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
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Delete Experiment tags test stop................................\n\n");
  }

  @Test
  public void f_deleteExperimentTagsNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Delete Experiment tags Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          DeleteExperimentTags deleteExperimentTags = DeleteExperimentTags.newBuilder().build();

          try {
            experimentServiceStub.deleteExperimentTags(deleteExperimentTags);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          deleteExperimentTags =
              DeleteExperimentTags.newBuilder()
                  .setId(experimentResponse.getExperimentsList().get(0).getId())
                  .setDeleteAll(true)
                  .build();
          try {
            experimentServiceStub.deleteExperimentTags(deleteExperimentTags);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Delete Experiment tags Negative test stop................................\n\n");
  }

  @Test
  public void g_addAttribute() throws IOException {
    LOGGER.info("\n\n Add Experiment attribute test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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
          Value stringValue =
              Value.newBuilder()
                  .setStringValue(
                      "Attributes_Value_add_" + Calendar.getInstance().getTimeInMillis())
                  .build();
          KeyValue keyValue =
              KeyValue.newBuilder()
                  .setKey("Attributes_add " + Calendar.getInstance().getTimeInMillis())
                  .setValue(stringValue)
                  .setValueType(ValueType.STRING)
                  .build();

          AddAttributes addAttributesRequest =
              AddAttributes.newBuilder()
                  .setId(experimentResponse.getExperimentsList().get(0).getId())
                  .setAttribute(keyValue)
                  .build();

          try {
            AddAttributes.Response response =
                experimentServiceStub.addAttribute(addAttributesRequest);
            LOGGER.info("AddAttributes Response : " + response.getStatus());
            assertEquals(true, response.getStatus());
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Add Experiment attribute test stop................................\n\n");
  }

  @Test
  public void g_addAttributeNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Add Experiment attribute Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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
          Value stringValue =
              Value.newBuilder()
                  .setStringValue(
                      "Attributes_Value_add_" + Calendar.getInstance().getTimeInMillis())
                  .build();
          KeyValue keyValue =
              KeyValue.newBuilder()
                  .setKey("Attributes_add " + Calendar.getInstance().getTimeInMillis())
                  .setValue(stringValue)
                  .setValueType(ValueType.STRING)
                  .build();

          AddAttributes addAttributesRequest =
              AddAttributes.newBuilder().setAttribute(keyValue).build();

          try {
            experimentServiceStub.addAttribute(addAttributesRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          addAttributesRequest =
              AddAttributes.newBuilder().setId("dhfjkdshfd").setAttribute(keyValue).build();

          try {
            experimentServiceStub.addAttribute(addAttributesRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(
                Status.ALREADY_EXISTS.getCode().equals(status.getCode())
                    || Status.NOT_FOUND.getCode().equals(status.getCode()));
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Add Experiment attribute Negative test stop................................\n\n");
  }

  @Test
  public void gg_addExperimentAttributes() throws IOException {
    LOGGER.info("\n\n Add Experiment attributes test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          AddExperimentAttributes addAttributesRequest =
              AddExperimentAttributes.newBuilder()
                  .setId(experimentResponse.getExperimentsList().get(0).getId())
                  .addAllAttributes(attributeList)
                  .build();

          try {
            AddExperimentAttributes.Response response =
                experimentServiceStub.addExperimentAttributes(addAttributesRequest);
            LOGGER.info("AddExperimentAttributes Response : " + response.getExperiment());
            assertTrue(response.getExperiment().getAttributesList().containsAll(attributeList));
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Add Experiment attributes test stop................................\n\n");
  }

  @Test
  public void gg_addExperimentAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Add Experiment attributes Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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
                  .setValueType(ValueType.BLOB)
                  .build());

          AddExperimentAttributes addAttributesRequest =
              AddExperimentAttributes.newBuilder().addAllAttributes(attributeList).build();

          try {
            experimentServiceStub.addExperimentAttributes(addAttributesRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            addAttributesRequest =
                AddExperimentAttributes.newBuilder()
                    .setId(experimentResponse.getExperimentsList().get(0).getId())
                    .build();
            try {
              experimentServiceStub.addExperimentAttributes(addAttributesRequest);
            } catch (StatusRuntimeException ex) {
              status = Status.fromThrowable(ex);
              assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
            }
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Add Experiment attributes Negative test stop................................\n\n");
  }

  @Test
  public void h_getExperimentAttributes() throws IOException {
    LOGGER.info("\n\n Get Experiment attributes test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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
          Experiment experiment = experimentResponse.getExperimentsList().get(0);

          List<KeyValue> attributes = experiment.getAttributesList();
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
                  .setId(experiment.getId())
                  .addAllAttributeKeys(keys)
                  .build();

          GetAttributes.Response response =
              experimentServiceStub.getExperimentAttributes(getAttributesRequest);
          assertEquals(keys.size(), response.getAttributesList().size());
          LOGGER.info("getExperimentAttributes Response : " + response.getAttributesList());

          getAttributesRequest =
              GetAttributes.newBuilder().setId(experiment.getId()).setGetAll(true).build();

          response = experimentServiceStub.getExperimentAttributes(getAttributesRequest);
          assertEquals(attributes.size(), response.getAttributesList().size());
          LOGGER.info("getExperimentAttributes Response : " + response.getAttributesList());
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Get Experiment attributes test stop................................\n\n");
  }

  @Test
  public void h_getExperimentAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Experiment attribute Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          GetAttributes getAttributesRequest = GetAttributes.newBuilder().build();

          try {
            experimentServiceStub.getExperimentAttributes(getAttributesRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          getAttributesRequest =
              GetAttributes.newBuilder().setId("dsfdsfdsfds").setGetAll(true).build();

          try {
            experimentServiceStub.getExperimentAttributes(getAttributesRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertTrue(Status.NOT_FOUND.getCode().equals(status.getCode()));
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Get Experiment attribute Negative test stop................................\n\n");
  }

  @Test
  public void hh_deleteExperimentAttributes() throws IOException {
    LOGGER.info("\n\n Delete Experiment Attributes test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          Experiment experiment = experimentResponse.getExperimentsList().get(0);

          List<KeyValue> attributes = experiment.getAttributesList();
          LOGGER.info("Attributes size : " + attributes.size());

          if (attributes.size() == 0) {
            LOGGER.warning("Experiment Attributes not found in database ");
            assertTrue(false);
            return;
          }

          List<String> keys = new ArrayList<>();
          for (int index = 0; index < attributes.size() - 1; index++) {
            KeyValue keyValue = attributes.get(index);
            keys.add(keyValue.getKey());
          }
          LOGGER.info("Attributes key size : " + keys.size());

          DeleteExperimentAttributes deleteExperimentAttributes =
              DeleteExperimentAttributes.newBuilder()
                  .setId(experiment.getId())
                  .addAllAttributeKeys(keys)
                  .build();

          DeleteExperimentAttributes.Response response =
              experimentServiceStub.deleteExperimentAttributes(deleteExperimentAttributes);
          LOGGER.info("DeleteExperimentAttributes Response : " + response.getExperiment());
          assertTrue(response.getExperiment().getAttributesList().size() <= 1);

          if (response.getExperiment().getAttributesList().size() != 0) {
            deleteExperimentAttributes =
                DeleteExperimentAttributes.newBuilder()
                    .setId(experiment.getId())
                    .setDeleteAll(true)
                    .build();

            response = experimentServiceStub.deleteExperimentAttributes(deleteExperimentAttributes);
            LOGGER.info("DeleteExperimentAttributes Response : " + response.getExperiment());
            assertTrue(response.getExperiment().getAttributesList().size() == 0);
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Delete Experiment Attributes test stop................................\n\n");
  }

  @Test
  public void hh_deleteExperimentAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Delete Experiment Attributes Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          Experiment experiment = experimentResponse.getExperimentsList().get(0);
          DeleteExperimentAttributes deleteExperimentAttributes =
              DeleteExperimentAttributes.newBuilder().build();

          try {
            experimentServiceStub.deleteExperimentAttributes(deleteExperimentAttributes);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            try {
              deleteExperimentAttributes =
                  DeleteExperimentAttributes.newBuilder()
                      .setId(experiment.getId())
                      .setDeleteAll(true)
                      .build();

              experimentServiceStub.deleteExperimentAttributes(deleteExperimentAttributes);
            } catch (StatusRuntimeException ex) {
              status = Status.fromThrowable(ex);
              LOGGER.log(
                  Level.WARNING,
                  "Error Code : " + status.getCode() + " Description : " + status.getDescription());
              assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
            }
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Delete Experiment Attributes Negative test stop................................\n\n");
  }

  @Test
  public void i_getExperimentByName() throws IOException {
    LOGGER.info("\n\n Get Experiment by name test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          Experiment experiment = experimentResponse.getExperimentsList().get(0);
          GetExperimentByName experimentRequest =
              GetExperimentByName.newBuilder()
                  .setProjectId(experiment.getProjectId())
                  .setName(experiment.getName())
                  .build();

          try {
            GetExperimentByName.Response response =
                experimentServiceStub.getExperimentByName(experimentRequest);
            LOGGER.info("GetExperimentByName Response : \n" + response.getExperiment());
            assertEquals(true, response.getExperiment() != null);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          }
        } else {
          LOGGER.info("Experiment's not found in database.. ");
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
      LOGGER.info("Project's not found in database.. ");
    }

    LOGGER.info(
        "\n\n Get Experiment by name of project test stop................................\n\n");
  }

  @Test
  public void i_getExperimentByNameNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Experiment by name Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          GetExperimentByName experimentRequest = GetExperimentByName.newBuilder().build();

          try {
            experimentServiceStub.getExperimentByName(experimentRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());

            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            Experiment experiment = experimentResponse.getExperimentsList().get(0);
            experimentRequest =
                GetExperimentByName.newBuilder().setName(experiment.getName()).build();
            try {
              experimentServiceStub.getExperimentByName(experimentRequest);
            } catch (StatusRuntimeException ex) {
              Status status2 = Status.fromThrowable(ex);
              status2.getCode();
              status2.getDescription();

              LOGGER.log(
                  Level.WARNING,
                  "Error Code : "
                      + status2.getCode()
                      + " Description : "
                      + status2.getDescription());

              assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
            }
          }
        } else {
          LOGGER.info("Experiment's not found in database.. ");
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
      LOGGER.info("Project's not found in database.. ");
    }

    LOGGER.info(
        "\n\n Get Experiment by name Negative test stop................................\n\n");
  }

  @Test
  @Ignore
  public void z_deleteExperimentNegativeTest() throws IOException {
    LOGGER.info("\n\n Delete Experiment Negative test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          DeleteExperiment deleteExperimentRequest = DeleteExperiment.newBuilder().build();

          try {
            experimentServiceStub.deleteExperiment(deleteExperimentRequest);
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Delete Experiment Negative test stop................................\n\n");
  }

  @Test
  @Ignore
  public void z_deleteExperiment() throws IOException {
    LOGGER.info("\n\n Delete Experiment test start................................\n\n");

    GetProjects getProjects = GetProjects.newBuilder().build();

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      final ExperimentServiceBlockingStub experimentServiceStub =
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

          DeleteExperiment deleteExperimentRequest =
              DeleteExperiment.newBuilder()
                  .setId(experimentResponse.getExperimentsList().get(0).getId())
                  .build();
          LOGGER.info("Experiment Id : " + experimentResponse.getExperimentsList().get(0).getId());

          try {
            DeleteExperiment.Response response =
                experimentServiceStub.deleteExperiment(deleteExperimentRequest);
            LOGGER.info("DeleteExperiment Response : " + response.getStatus());
            assertEquals(true, response.getStatus());
          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
          }
        } else {
          LOGGER.info("Experiment not found in database ");
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
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Delete Experiment test stop................................\n\n");
  }
}
