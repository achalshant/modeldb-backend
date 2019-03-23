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
import com.mitdbg.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import com.mitdbg.modeldb.ProjectServiceGrpc.ProjectServiceStub;
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
public class ProjectTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;

  private static final Logger LOGGER = Logger.getLogger(ProjectTest.class.getName());

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
  public void a_aVerifyConnection() throws IOException {
    LOGGER.info("\n\n Verify connection test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    VerifyConnectionResponse response =
        projectServiceStub.verifyConnection(Empty.newBuilder().build());
    assertTrue(response.getStatus());
    LOGGER.info("Verify connection Successfully..");

    System.out.println("\n\n Verify connection test stop................................\n\n");
  }

  @Test
  public void a_projectCreateTest() throws IOException {
    LOGGER.info("\n\n Create Project test start................................\n\n");

    ProjectServiceStub projectServiceStub = ProjectServiceGrpc.newStub(channel);

    List<KeyValue> metadataList = new ArrayList<>();
    Value stringValue =
        Value.newBuilder()
            .setStringValue("attribute_" + Calendar.getInstance().getTimeInMillis() + "_value")
            .build();
    KeyValue keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_1_" + Calendar.getInstance().getTimeInMillis())
            .setValue(stringValue)
            .build();
    metadataList.add(keyValue);

    Value intValue = Value.newBuilder().setNumberValue(12345).build();
    keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_2_" + Calendar.getInstance().getTimeInMillis())
            .setValue(intValue)
            .setValueType(ValueType.NUMBER)
            .build();
    metadataList.add(keyValue);

    Value listValue =
        Value.newBuilder()
            .setListValue(ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
            .build();
    keyValue =
        KeyValue.newBuilder()
            .setKey("attribute_3_" + Calendar.getInstance().getTimeInMillis())
            .setValue(listValue)
            .setValueType(ValueType.LIST)
            .build();
    metadataList.add(keyValue);

    String projectName = "project_" + Calendar.getInstance().getTimeInMillis();
    CreateProject request =
        CreateProject.newBuilder()
            .setName(projectName)
            .setDescription("This is a project description.")
            .addTags("tag_" + Calendar.getInstance().getTimeInMillis())
            .addTags("tag_" + +Calendar.getInstance().getTimeInMillis())
            .addAllMetadata(metadataList)
            .build();

    projectServiceStub.createProject(
        request,
        new StreamObserver<CreateProject.Response>() {

          public void onNext(CreateProject.Response value) {
            LOGGER.info("Project Created Successfully..");
            assertEquals(projectName, value.getProject().getName());

            ProjectServiceBlockingStub projectServiceStub =
                ProjectServiceGrpc.newBlockingStub(channel);
            try {
              projectServiceStub.createProject(request);
            } catch (StatusRuntimeException e) {
              Status status = Status.fromThrowable(e);
              LOGGER.log(
                  Level.WARNING,
                  "Error Code : " + status.getCode() + " Description : " + status.getDescription());
              assertEquals(Status.ALREADY_EXISTS.getCode(), status.getCode());
            }
          }

          public void onError(Throwable t) {}

          public void onCompleted() {
            LOGGER.info("\n\n Create Project test stop................................\n\n");
          }
        });
  }

  @Test
  public void b_projectCreateNegativeTest() throws IOException {
    LOGGER.info("\n\n Create Project Negative test start................................\n\n");

    ProjectServiceStub projectServiceStub = ProjectServiceGrpc.newStub(channel);

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
              .build();
      metadataList.add(keyValue);
    }

    CreateProject request =
        CreateProject.newBuilder()
            .setDescription("This is a project description.")
            .addTags("tag_" + Calendar.getInstance().getTimeInMillis())
            .addTags("tag_" + +Calendar.getInstance().getTimeInMillis())
            .addAllMetadata(metadataList)
            .build();

    projectServiceStub.createProject(
        request,
        new StreamObserver<CreateProject.Response>() {

          public void onNext(CreateProject.Response value) {}

          public void onError(Throwable t) {
            Status status = Status.fromThrowable(t);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());

            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
          }

          public void onCompleted() {}
        });
  }

  @Test
  public void c_updateProjectNameOrDescription() throws IOException {
    LOGGER.info(
        "\n\n Update Project Name & Discription test start................................\n\n");

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      String projectId = getProjectsResponse.getProjectsList().get(0).getId();
      UpdateProjectNameOrDescription upDescriptionRequest =
          UpdateProjectNameOrDescription.newBuilder()
              .setId(projectId)
              .setName(
                  "Test Update Project Name Or Description "
                      + Calendar.getInstance().getTimeInMillis())
              .setDescription(
                  "This is update from UpdateProjectNameOrDescription."
                      + Calendar.getInstance().getTimeInMillis())
              .build();

      UpdateProjectNameOrDescription.Response response =
          projectServiceStub.updateProjectNameOrDescription(upDescriptionRequest);
      LOGGER.info("UpdateProjectNameOrDescription Response : " + response.getProject());
      assertEquals(projectId, response.getProject().getId());
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Update Project Name & Discription test stop................................\n\n");
  }

  @Test
  public void d_updateProjectNameOrDescriptionNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Update Project Name & Discription Negative test start................................\n\n");

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      UpdateProjectNameOrDescription upDescriptionRequest =
          UpdateProjectNameOrDescription.newBuilder()
              .setName(
                  "Test Update Project Name Or Description "
                      + Calendar.getInstance().getTimeInMillis())
              .setDescription(
                  "This is update from UpdateProjectNameOrDescription."
                      + Calendar.getInstance().getTimeInMillis())
              .build();

      try {
        projectServiceStub.updateProjectNameOrDescription(upDescriptionRequest);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        LOGGER.info("UpdateProjectNameOrDescription Response : " + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

        Project project = getProjectsResponse.getProjectsList().get(0);
        LOGGER.info("Project Id : " + project.getId());
        upDescriptionRequest =
            UpdateProjectNameOrDescription.newBuilder()
                .setId(project.getId())
                .setName(project.getName())
                .setDescription(project.getDescription())
                .build();
        try {
          projectServiceStub.updateProjectNameOrDescription(upDescriptionRequest);
        } catch (StatusRuntimeException ex) {
          Status status2 = Status.fromThrowable(ex);
          assertEquals(Status.ALREADY_EXISTS.getCode(), status2.getCode());
        }
      }
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info(
        "\n\n Update Project Name & Discription test stop................................\n\n");
  }

  @Test
  public void e_addProjectAttributes() throws IOException {
    LOGGER.info("\n\n Add Project Attributes test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);
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

      AddProjectAttributes addProjectAttributesRequest =
          AddProjectAttributes.newBuilder()
              .setId(project.getId())
              .addAllAttributes(attributeList)
              .build();

      AddProjectAttributes.Response response =
          projectServiceStub.addProjectAttributes(addProjectAttributesRequest);
      LOGGER.info("Added Project Attributes: \n" + response.getProject());
      assertTrue(response.getProject().getAttributesList().containsAll(attributeList));

    } else {
      LOGGER.info("Project not found in database ");
    }

    System.out.println("\n\n Add Project Attributes test stop................................\n\n");
  }

  @Test
  public void e_addProjectAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Add Project Attributes Negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);
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

      AddProjectAttributes addProjectAttributesRequest =
          AddProjectAttributes.newBuilder().addAllAttributes(attributeList).build();

      try {
        projectServiceStub.addProjectAttributes(addProjectAttributesRequest);
      } catch (StatusRuntimeException e) {
        Status status = Status.fromThrowable(e);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

        addProjectAttributesRequest =
            AddProjectAttributes.newBuilder().setId(project.getId()).build();
        try {
          projectServiceStub.addProjectAttributes(addProjectAttributesRequest);
        } catch (StatusRuntimeException ex) {
          status = Status.fromThrowable(ex);
          assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
        }
      }

    } else {
      LOGGER.info("Project not found in database ");
    }

    System.out.println(
        "\n\n Add Project Attributes Negative test stop................................\n\n");
  }

  @Test
  public void e_updateProjectAttributes() throws IOException {
    LOGGER.info("\n\n Update Project Attributes test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);
      List<KeyValue> attributes = project.getAttributesList();
      Value stringValue =
          Value.newBuilder()
              .setStringValue(
                  "attribute_1542193772147_updated_test_value"
                      + Calendar.getInstance().getTimeInMillis())
              .build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(1).getKey())
              .setValue(stringValue)
              .setValueType(ValueType.STRING)
              .build();
      UpdateProjectAttributes updateProjectAttributesRequest =
          UpdateProjectAttributes.newBuilder()
              .setId(project.getId())
              .setAttribute(keyValue)
              .build();

      UpdateProjectAttributes.Response response =
          projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
      LOGGER.info("Updated Project : \n" + response.getProject());
      assertTrue(response.getProject().getAttributesList().contains(keyValue));

      Value intValue =
          Value.newBuilder().setNumberValue(Calendar.getInstance().getTimeInMillis()).build();
      keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(2).getKey())
              .setValue(intValue)
              .setValueType(ValueType.NUMBER)
              .build();
      updateProjectAttributesRequest =
          UpdateProjectAttributes.newBuilder()
              .setId(project.getId())
              .setAttribute(keyValue)
              .build();

      response = projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
      LOGGER.info("Updated Project : \n" + response.getProject());
      assertTrue(response.getProject().getAttributesList().contains(keyValue));

      Value listValue =
          Value.newBuilder()
              .setListValue(
                  ListValue.newBuilder().addValues(intValue).addValues(stringValue).build())
              .build();
      keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(0).getKey())
              .setValue(listValue)
              .setValueType(ValueType.LIST)
              .build();
      updateProjectAttributesRequest =
          UpdateProjectAttributes.newBuilder()
              .setId(project.getId())
              .setAttribute(keyValue)
              .build();

      response = projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
      LOGGER.info("Updated Project : \n" + response.getProject());
      assertTrue(response.getProject().getAttributesList().contains(keyValue));
    } else {
      LOGGER.info("Project not found in database ");
    }

    System.out.println(
        "\n\n Update Project Attributes test stop................................\n\n");
  }

  @Test
  public void e_updateProjectAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Update Project Attributes Negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);
      List<KeyValue> attributes = project.getAttributesList();
      Value stringValue = Value.newBuilder().setStringValue("attribute_updated_test_value").build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey(attributes.get(0).getKey())
              .setValue(stringValue)
              .setValueType(ValueType.STRING)
              .build();
      UpdateProjectAttributes updateProjectAttributesRequest =
          UpdateProjectAttributes.newBuilder().setAttribute(keyValue).build();

      try {
        projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
      } catch (StatusRuntimeException ex) {
        Status status = Status.fromThrowable(ex);
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

        updateProjectAttributesRequest =
            UpdateProjectAttributes.newBuilder()
                .setId("sfds")
                .setAttribute(project.getAttributesList().get(0))
                .build();
        try {
          projectServiceStub.updateProjectAttributes(updateProjectAttributesRequest);
        } catch (StatusRuntimeException e) {
          Status status2 = Status.fromThrowable(ex);
          assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
        }
      }
    } else {
      LOGGER.info("Project not found in database ");
    }

    System.out.println(
        "\n\n Update Project Attributes Negative test stop................................\n\n");
  }

  @Test
  public void f_addProjectTags() throws IOException {
    LOGGER.info("\n\n Add Project Tags test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      List<String> tagsList = new ArrayList<String>();
      tagsList.add("Add Test Tag " + Calendar.getInstance().getTimeInMillis());
      tagsList.add("Add Test Tag 2 " + Calendar.getInstance().getTimeInMillis());
      AddProjectTags addProjectTagsRequest =
          AddProjectTags.newBuilder().setId(project.getId()).addAllTags(tagsList).build();

      AddProjectTags.Response response = projectServiceStub.addProjectTags(addProjectTagsRequest);
      LOGGER.info("Tags added in server : " + response.getProject());
      assertTrue(response.getProject().getTagsList().containsAll(tagsList));

    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Add Project tags test stop................................\n\n");
  }

  @Test
  public void g_addProjectNegativeTags() throws IOException {
    LOGGER.info("\n\n Add Project Tags Negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      List<String> tagsList = new ArrayList<String>();
      tagsList.add("Add Test Tag " + Calendar.getInstance().getTimeInMillis());
      tagsList.add("Add Test Tag 2 " + Calendar.getInstance().getTimeInMillis());
      AddProjectTags addProjectTagsRequest =
          AddProjectTags.newBuilder().addAllTags(tagsList).build();

      try {
        projectServiceStub.addProjectTags(addProjectTagsRequest);
      } catch (StatusRuntimeException ex) {
        Status status = Status.fromThrowable(ex);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : "
                + status.getCode().value()
                + " "
                + status.getCode()
                + " Description : "
                + status.getDescription());
        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

        Project project = getProjectsResponse.getProjectsList().get(0);
        addProjectTagsRequest =
            AddProjectTags.newBuilder().setId("sdasd").addAllTags(project.getTagsList()).build();

        try {
          projectServiceStub.addProjectTags(addProjectTagsRequest);
        } catch (StatusRuntimeException e) {
          Status status2 = Status.fromThrowable(e);
          status2.getCode();
          status2.getDescription();
          assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
        }
      }
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Add Project tags Negative test stop................................\n\n");
  }

  @Test
  public void gg_getProjectTags() throws IOException {
    LOGGER.info("\n\n Get Project Tags test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      GetTags deleteProjectTagsRequest = GetTags.newBuilder().setId(project.getId()).build();

      GetTags.Response response = projectServiceStub.getProjectTags(deleteProjectTagsRequest);
      LOGGER.info("Tags deleted in server : " + response.getTagsList());
      assertTrue(project.getTagsList().containsAll(response.getTagsList()));
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Get Project tags test stop................................\n\n");
  }

  @Test
  public void gg_getProjectTagsNegativeTest() throws IOException {
    LOGGER.info("\n\n Get Project Tags Negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetTags deleteProjectTagsRequest = GetTags.newBuilder().build();

    try {
      projectServiceStub.getProjectTags(deleteProjectTagsRequest);
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      status.getCode();
      status.getDescription();
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info("\n\n Get Project tags Negative test stop................................\n\n");
  }

  @Test
  public void h_deleteProjectTags() throws IOException {
    LOGGER.info("\n\n Delete Project Tags test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      try {
        List<String> removableTags = project.getTagsList();
        if (removableTags.size() == 0) {
          LOGGER.info("Project Tags not found in database ");
          assertTrue(false);
          return;
        }
        if (project.getTagsList().size() > 1) {
          removableTags = project.getTagsList().subList(0, project.getTagsList().size() - 1);
        }
        DeleteProjectTags deleteProjectTagsRequest =
            DeleteProjectTags.newBuilder().setId(project.getId()).addAllTags(removableTags).build();

        DeleteProjectTags.Response response =
            projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
        LOGGER.info("Tags deleted in server : " + response.getProject().getTagsList());
        assertTrue(response.getProject().getTagsList().size() <= 1);

        if (response.getProject().getTagsList().size() > 0) {
          deleteProjectTagsRequest =
              DeleteProjectTags.newBuilder().setId(project.getId()).setDeleteAll(true).build();

          response = projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
          LOGGER.info("Tags deleted in server : " + response.getProject().getTagsList());
          assertTrue(response.getProject().getTagsList().size() == 0);
        }
      } catch (StatusRuntimeException ex) {
        Status status = Status.fromThrowable(ex);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : "
                + status.getCode().value()
                + " "
                + status.getCode()
                + " Description : "
                + status.getDescription());
        assertTrue(false);
      }
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Delete Project tags test stop................................\n\n");
  }

  @Test
  public void h_deleteProjectTagsNegativeTest() throws IOException {
    LOGGER.info("\n\n Delete Project Tags Negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      DeleteProjectTags deleteProjectTagsRequest = DeleteProjectTags.newBuilder().build();

      try {
        projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
      } catch (StatusRuntimeException ex) {
        Status status = Status.fromThrowable(ex);
        status.getCode();
        status.getDescription();

        LOGGER.log(
            Level.WARNING,
            "Error Code : "
                + status.getCode().value()
                + " "
                + status.getCode()
                + " Description : "
                + status.getDescription());

        assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

        deleteProjectTagsRequest =
            DeleteProjectTags.newBuilder().setId(project.getId()).setDeleteAll(true).build();
        try {
          projectServiceStub.deleteProjectTags(deleteProjectTagsRequest);
        } catch (StatusRuntimeException exe) {
          Status status2 = Status.fromThrowable(exe);
          status2.getCode();
          status2.getDescription();

          LOGGER.log(
              Level.WARNING,
              "Error Code : "
                  + status2.getCode().value()
                  + " "
                  + status2.getCode()
                  + " Description : "
                  + status2.getDescription());

          assertEquals(Status.ALREADY_EXISTS.getCode(), status2.getCode());
        }
      }
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Delete Project tags Negative test stop................................\n\n");
  }

  @Test
  public void i_getProjectAttributes() throws IOException {
    LOGGER.info("\n\n Get Project Attributes test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      List<KeyValue> attributes = project.getAttributesList();
      LOGGER.info("Attributes size : " + attributes.size());

      if (attributes.size() == 0) {
        LOGGER.warning("Project Attributes not found in database ");
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

      GetAttributes getProjectAttributesRequest =
          GetAttributes.newBuilder().setId(project.getId()).addAllAttributeKeys(keys).build();

      GetAttributes.Response response =
          projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
      LOGGER.info(response.getAttributesList().toString());
      assertEquals(keys.size(), response.getAttributesList().size());

      getProjectAttributesRequest =
          GetAttributes.newBuilder().setId(project.getId()).setGetAll(true).build();

      response = projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
      LOGGER.info(response.getAttributesList().toString());
      assertEquals(project.getAttributesList().size(), response.getAttributesList().size());
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Get Project Attributes test stop................................\n\n");
  }

  @Test
  public void i_getProjectAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Get Project Attributes Negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetAttributes getProjectAttributesRequest = GetAttributes.newBuilder().build();

    try {
      projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
    } catch (StatusRuntimeException e) {
      Status status = Status.fromThrowable(e);
      status.getCode();
      status.getDescription();

      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

      getProjectAttributesRequest =
          GetAttributes.newBuilder().setId("jfhdsjfhdsfjk").setGetAll(true).build();
      try {
        projectServiceStub.getProjectAttributes(getProjectAttributesRequest);
      } catch (StatusRuntimeException ex) {
        Status status2 = Status.fromThrowable(ex);
        status2.getCode();
        status2.getDescription();

        assertTrue(
            Status.INTERNAL.getCode().equals(status2.getCode())
                || Status.PERMISSION_DENIED.getCode().equals(status2.getCode()));
      }
    }

    LOGGER.info(
        "\n\n Get Project Attributes Negative test stop................................\n\n");
  }

  @Test
  public void ii_deleteProjectAttributesTest() throws IOException {
    LOGGER.info("\n\n Delete Project Attributes test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      List<KeyValue> attributes = project.getAttributesList();
      LOGGER.info("Attributes size : " + attributes.size());
      if (attributes.size() == 0) {
        LOGGER.warning("Project Attributes not found in database ");
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

      DeleteProjectAttributes deleteProjectAttributesRequest =
          DeleteProjectAttributes.newBuilder()
              .setId(project.getId())
              .addAllAttributeKeys(keys)
              .build();

      DeleteProjectAttributes.Response response =
          projectServiceStub.deleteProjectAttributes(deleteProjectAttributesRequest);
      LOGGER.info("Attributes deleted in server : " + response.getProject());
      assertTrue(response.getProject().getAttributesList().size() <= 1);

      if (response.getProject().getAttributesList().size() != 0) {
        deleteProjectAttributesRequest =
            DeleteProjectAttributes.newBuilder().setId(project.getId()).setDeleteAll(true).build();
        response = projectServiceStub.deleteProjectAttributes(deleteProjectAttributesRequest);
        LOGGER.info("Attributes deleted in server : " + response.getProject());
        assertTrue(response.getProject().getAttributesList().size() == 0);
      }

    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Delete Project Attributes test stop................................\n\n");
  }

  @Test
  public void ii_deleteProjectAttributesNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n Delete Project Attributes Negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    DeleteProjectAttributes deleteProjectAttributesRequest =
        DeleteProjectAttributes.newBuilder().build();

    try {
      projectServiceStub.deleteProjectAttributes(deleteProjectAttributesRequest);
    } catch (StatusRuntimeException ex) {
      Status status = Status.fromThrowable(ex);

      assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
    }

    LOGGER.info(
        "\n\n Delete Project Attributes Negative test stop................................\n\n");
  }

  @Test
  public void j_getProjectById() throws IOException {
    LOGGER.info("\n\n Get Project by ID test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      try {
        GetProjectById getProject = GetProjectById.newBuilder().setId(project.getId()).build();

        GetProjectById.Response response = projectServiceStub.getProjectById(getProject);
        LOGGER.info("Response List : " + response.getProject());
        assertTrue(response.getProject() != null);
      } catch (StatusRuntimeException ex) {
        Status status2 = Status.fromThrowable(ex);
        status2.getCode();
        status2.getDescription();

        LOGGER.info("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
      }
    } else {
      LOGGER.info("Project's not found in database.. ");
      assertTrue(false);
    }

    LOGGER.info("\n\n Get project by ID test stop................................\n\n");
  }

  @Test
  public void j_getProjectByIdNegativeTest() throws IOException {
    LOGGER.info("\n\n Get Project by ID negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    try {
      GetProjectById getProject = GetProjectById.newBuilder().build();

      projectServiceStub.getProjectById(getProject);
    } catch (StatusRuntimeException e) {
      Status status2 = Status.fromThrowable(e);
      status2.getCode();
      status2.getDescription();

      assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
    }

    LOGGER.info("\n\n Get project by ID negative test stop................................\n\n");
  }

  @Test
  public void k_getProjectByName() throws IOException {
    LOGGER.info("\n\n Get Project by name test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {

      Project project = getProjectsResponse.getProjectsList().get(0);

      try {
        GetProjectByName getProject =
            GetProjectByName.newBuilder().setName(project.getName()).build();

        GetProjectByName.Response response = projectServiceStub.getProjectByName(getProject);
        LOGGER.info("Response list of Projects : " + response.getProjectsList());
        assertTrue(response.getProjectsList() != null);
      } catch (StatusRuntimeException ex) {
        Status status2 = Status.fromThrowable(ex);
        status2.getCode();
        status2.getDescription();

        LOGGER.info("Error Code : " + status2.getCode() + " Error : " + status2.getDescription());
      }
    } else {
      LOGGER.info("Project's not found in database.. ");
      assertTrue(false);
    }

    LOGGER.info("\n\n Get project by name test stop................................\n\n");
  }

  @Test
  public void k_getProjectByNameNegativeTest() throws IOException {
    LOGGER.info("\n\n Get Project by name negative test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    try {
      GetProjectByName getProject = GetProjectByName.newBuilder().build();

      projectServiceStub.getProjectByName(getProject);
    } catch (StatusRuntimeException e) {
      Status status2 = Status.fromThrowable(e);
      status2.getCode();
      status2.getDescription();

      assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
    }

    LOGGER.info("\n\n Get project by name negative test stop................................\n\n");
  }

  @Test
  public void l_getProjects() throws IOException {
    LOGGER.info("\n\n Get Project test start................................\n\n");

    ProjectServiceBlockingStub projectServiceStub = ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response response = projectServiceStub.getProjects(getProjects);
    LOGGER.info("GetProjects Count : " + response.getProjectsCount());
    LOGGER.info("Response List : " + response.getProjectsList());
    assertTrue(response.getProjectsList() != null);

    LOGGER.info("\n\n Get project test stop................................\n\n");
  }

  @Test
  public void x_projectDeleteNegativeTest() throws IOException {

    LOGGER.info("\n\n Project delete Negative test start................................\n\n");

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      DeleteProject deleteProject = DeleteProject.newBuilder().build();

      try {
        projectServiceStub.deleteProject(deleteProject);
      } catch (StatusRuntimeException e) {
        Status status2 = Status.fromThrowable(e);
        status2.getCode();
        status2.getDescription();

        assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
      }
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Project delete Negative test stop................................\n\n");
  }

  @Test
  @Ignore
  public void y_projectDeleteTest() throws IOException {

    LOGGER.info("\n\n Project delete test start................................\n\n");

    final ProjectServiceBlockingStub projectServiceStub =
        ProjectServiceGrpc.newBlockingStub(channel);

    GetProjects getProjects = GetProjects.newBuilder().build();

    GetProjects.Response getProjectsResponse = projectServiceStub.getProjects(getProjects);

    if (getProjectsResponse.getProjectsList() != null
        && getProjectsResponse.getProjectsList().size() > 0) {
      DeleteProject deleteProject =
          DeleteProject.newBuilder()
              .setId(getProjectsResponse.getProjectsList().get(0).getId())
              .build();

      DeleteProject.Response deleteProjectResponse =
          projectServiceStub.deleteProject(deleteProject);
      LOGGER.info(deleteProjectResponse.toString());
      assertTrue(deleteProjectResponse.getStatus());
    } else {
      LOGGER.info("Project not found in database ");
    }

    LOGGER.info("\n\n Project delete test stop................................\n\n");
  }
}
