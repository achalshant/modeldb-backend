package com.mitdbg.modeldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
import com.google.protobuf.ByteString;
import com.mitdbg.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceBlockingStub;
import com.mitdbg.modeldb.ExperimentRunStoreArtifact.DeleteArtifact;
import com.mitdbg.modeldb.ExperimentRunStoreArtifact.GetArtifact;
import com.mitdbg.modeldb.ExperimentRunStoreArtifact.StoreArtifact;
import com.mitdbg.modeldb.ExperimentRunStoreArtifact.StoreArtifactWithStream;
import com.mitdbg.modeldb.ProjectServiceGrpc.ProjectServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArtifactStoreTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;

  private static final Logger LOGGER = Logger.getLogger(ArtifactStoreTest.class.getName());

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
  public void a_storeArtifactOnCloudTest() throws IOException {
    LOGGER.info(
        "\n\n store artifact on cloud ExperimentRun test start................................\n\n");

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
          LOGGER.info("ExperimentRun found..........");

          StoreArtifact storeArtifactRequest =
              StoreArtifact.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setFileKey("twain11-hp-bg.jpg")
                  .setClientFilepath("http://www.google.ro/logos/2011/twain11-hp-bg.jpg")
                  .build();

          LOGGER.info("StoreArtifact Request called");
          StoreArtifact.Response response =
              experimentRunServiceStub.storeArtifact(storeArtifactRequest);

          LOGGER.info("StoreArtifact Response : " + response.getExperimentRun());
          assertTrue(response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun's not found in database");
          assertTrue(false);
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
      LOGGER.log(Level.WARNING, "Project's not found in database");
      assertTrue(false);
    }

    LOGGER.info(
        "\n\n store artifact on cloud ExperimentRun test stop................................\n\n");
  }

  @Test
  public void aa_storeArtifactOnCloudNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n store artifact on cloud ExperimentRun Negative test start................................\n\n");

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
          LOGGER.info("ExperimentRun found..........");

          StoreArtifact storeArtifactRequest =
              StoreArtifact.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  // .setFileKey("twain11-hp-bg.jpg")
                  .setClientFilepath("http://www.google.ro/logos/2011/twain11-hp-bg.jpg")
                  .build();

          LOGGER.info("StoreArtifact Request called");

          try {
            StoreArtifact.Response response =
                experimentRunServiceStub.storeArtifact(storeArtifactRequest);

          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            storeArtifactRequest =
                StoreArtifact.newBuilder()
                    .setExperimentRunId(
                        experimentRunResponse.getExperimentRunsList().get(0).getId())
                    .setFileKey("twain11-hp-bg.jpg")
                    .setClientFilepath("http://www.google.ro/logos/2011/twain11-hp-bg.jpg")
                    .build();
            try {
              StoreArtifact.Response response =
                  experimentRunServiceStub.storeArtifact(storeArtifactRequest);

            } catch (StatusRuntimeException ex) {
              status = Status.fromThrowable(ex);
              status.getCode();
              status.getDescription();

              LOGGER.log(
                  Level.WARNING,
                  "Error Code : " + status.getCode() + " Description : " + status.getDescription());
              assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
            }
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun's not found in database");
          assertTrue(false);
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
      LOGGER.log(Level.WARNING, "Project's not found in database");
      assertTrue(false);
    }

    LOGGER.info(
        "\n\n store artifact on cloud ExperimentRun Negative test stop................................\n\n");
  }

  @Test
  public void aaa_storeStreamArtifactOnCloudTest() throws IOException {
    LOGGER.info(
        "\n\n store stream artifact on cloud ExperimentRun test start................................\n\n");

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
          LOGGER.info("ExperimentRun found..........");

          URL url = new URL("http://www.google.ro/logos/2011/twain11-hp-bg.jpg");
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();

          InputStream inputStream = connection.getInputStream();
          StoreArtifactWithStream storeArtifactRequest =
              StoreArtifactWithStream.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setFileKey("1twain11-hp-bg-123.jpg")
                  .setClientFile(ByteString.readFrom(inputStream))
                  .build();

          LOGGER.info("StoreArtifactWithStream Request called");
          StoreArtifactWithStream.Response response =
              experimentRunServiceStub.storeArtifactWithStream(storeArtifactRequest);

          LOGGER.info("StoreArtifactWithStream Response : " + response.getExperimentRun());
          assertTrue(response.getExperimentRun() != null);

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun's not found in database");
          assertTrue(false);
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
      LOGGER.log(Level.WARNING, "Project's not found in database");
      assertTrue(false);
    }

    LOGGER.info(
        "\n\n store stream artifact on cloud ExperimentRun test stop................................\n\n");
  }

  @Test
  @Ignore
  public void aaaa_storeStreamArtifactOnCloudNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n store stream artifact on cloud ExperimentRun Negative test start................................\n\n");

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
          LOGGER.info("ExperimentRun found..........");

          URL url = new URL("http://www.google.ro/logos/2011/twain11-hp-bg.jpg");
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
          InputStream inputStream = connection.getInputStream();
          StoreArtifactWithStream storeArtifactRequest =
              StoreArtifactWithStream.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setFileKey("1twain11-hp-bg-123.jpg")
                  .setClientFile(ByteString.readFrom(inputStream))
                  .build();

          LOGGER.info("StoreStreamArtifact Request called");
          try {
            StoreArtifactWithStream.Response response =
                experimentRunServiceStub.storeArtifactWithStream(storeArtifactRequest);

          } catch (StatusRuntimeException e) {
            Status status = Status.fromThrowable(e);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.NOT_FOUND.getCode(), status.getCode());

            storeArtifactRequest =
                StoreArtifactWithStream.newBuilder()
                    .setExperimentRunId(
                        experimentRunResponse.getExperimentRunsList().get(0).getId())
                    .setFileKey("1twain11-hp-bg-123.jpg")
                    .setClientFile(ByteString.readFrom(inputStream))
                    .build();
            try {
              StoreArtifactWithStream.Response response =
                  experimentRunServiceStub.storeArtifactWithStream(storeArtifactRequest);

            } catch (StatusRuntimeException ex) {
              status = Status.fromThrowable(ex);
              status.getCode();
              status.getDescription();

              LOGGER.log(
                  Level.WARNING,
                  "Error Code : " + status.getCode() + " Description : " + status.getDescription());
              assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());
            }
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun's not found in database");
          assertTrue(false);
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
      LOGGER.log(Level.WARNING, "Project's not found in database");
      assertTrue(false);
    }

    LOGGER.info(
        "\n\n store stream artifact on cloud ExperimentRun Negative test stop................................\n\n");
  }

  @Test
  public void b_getStoreArtifactFromCloudTest() throws IOException {
    LOGGER.info(
        "\n\n get store artifact from cloud ExperimentRun test start................................\n\n");

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
          LOGGER.info("ExperimentRun found..........");

          String key = "twain11-hp-bg.jpg";
          GetArtifact getStoreArtifactRequest =
              GetArtifact.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setKey(key)
                  .build();

          GetArtifact.Response response =
              experimentRunServiceStub.getStoreArtifact(getStoreArtifactRequest);

          String rootPath = System.getProperty("user.dir");
          File file = new File(rootPath + "\\" + key);
          FileOutputStream fileOutputStream = new FileOutputStream(file);
          fileOutputStream.write(response.getContents().toByteArray());
          fileOutputStream.close();
          LOGGER.info("GetArtifact Response : " + file.getAbsolutePath());
          assertTrue(file.exists());

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun's not found in database");
          assertTrue(false);
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
      LOGGER.log(Level.WARNING, "Project's not found in database");
      assertTrue(false);
    }

    LOGGER.info(
        "\n\n get store artifact from cloud ExperimentRun test stop................................\n\n");
  }

  @Test
  public void b_getStoreArtifactFromCloudNegativeTest() throws IOException {
    LOGGER.info(
        "\n\n get store artifact from cloud ExperimentRun Negative test start................................\n\n");

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
          LOGGER.info("ExperimentRun found..........");

          String key = "twain11-hp-bg.jpg";
          GetArtifact getStoreArtifactRequest =
              GetArtifact.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  // .setKey(key)
                  .build();

          try {
            GetArtifact.Response response =
                experimentRunServiceStub.getStoreArtifact(getStoreArtifactRequest);
          } catch (StatusRuntimeException ex) {
            Status status = Status.fromThrowable(ex);
            status.getCode();
            status.getDescription();

            LOGGER.log(
                Level.WARNING,
                "Error Code : " + status.getCode() + " Description : " + status.getDescription());
            assertEquals(Status.INVALID_ARGUMENT.getCode(), status.getCode());

            try {
              getStoreArtifactRequest =
                  GetArtifact.newBuilder()
                      .setExperimentRunId(
                          experimentRunResponse.getExperimentRunsList().get(0).getId())
                      .setKey(key)
                      .build();
              GetArtifact.Response response =
                  experimentRunServiceStub.getStoreArtifact(getStoreArtifactRequest);
            } catch (StatusRuntimeException e) {
              status = Status.fromThrowable(e);
              status.getCode();
              status.getDescription();

              LOGGER.log(
                  Level.WARNING,
                  "Error Code : " + status.getCode() + " Description : " + status.getDescription());
              assertEquals(Status.NOT_FOUND.getCode(), status.getCode());
            }
          }

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun's not found in database");
          assertTrue(false);
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
      LOGGER.log(Level.WARNING, "Project's not found in database");
      assertTrue(false);
    }

    LOGGER.info(
        "\n\n get store artifact from cloud ExperimentRun Negative test stop................................\n\n");
  }

  @Test
  @Ignore
  public void c_deleteStoreArtifactFromCloudTest() throws IOException {
    LOGGER.info(
        "\n\n delete store artifact from cloud ExperimentRun test start................................\n\n");

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

          DeleteArtifact storeArtifactRequest =
              DeleteArtifact.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setKey("twain11-hp-bg.jpg")
                  .build();

          DeleteArtifact.Response response =
              experimentRunServiceStub.deleteStoreArtifact(storeArtifactRequest);

          LOGGER.info("DeleteArtifact Response : " + response.getStatus());

          /*storeArtifactRequest =
              DeleteArtifact.newBuilder()
                  .setExperimentRunId(experimentRunResponse.getExperimentRunsList().get(0).getId())
                  .setKey("twain11-hp-bg-123.jpg")
                  .build();

          response =
              experimentRunServiceStub.deleteStoreArtifact(storeArtifactRequest);*/

          LOGGER.info("DeleteArtifact Response : " + response.getStatus());
          assertTrue(response.getStatus());

        } else {
          LOGGER.log(Level.WARNING, "ExperimentRun's not found in database");
          assertTrue(false);
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
      LOGGER.log(Level.WARNING, "Project's not found in database");
      assertTrue(false);
    }

    LOGGER.info(
        "\n\n delete store artifact from cloud ExperimentRun test stop................................\n\n");
  }
}
