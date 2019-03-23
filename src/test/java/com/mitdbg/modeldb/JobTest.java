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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.mitdbg.modeldb.JobServiceGrpc.JobServiceBlockingStub;
import com.mitdbg.modeldb.JobServiceGrpc.JobServiceStub;
import com.mitdbg.modeldb.JobStatusEnum.JobStatus;
import com.mitdbg.modeldb.JobTypeEnum.JobType;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JobTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ManagedChannel channel = null;

  private static final Logger LOGGER = Logger.getLogger(JobTest.class.getName());
  private Job testJob = null;

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
  public void a_jobCreateTest() throws IOException {
    LOGGER.info("\n\n Create Job test start................................\n\n");

    JobServiceStub jobServiceStub = JobServiceGrpc.newStub(channel);

    List<KeyValue> metadataList = new ArrayList<>();
    for (int count = 0; count < 3; count++) {
      Value stringValue =
          Value.newBuilder()
              .setStringValue(
                  "Job metadata_"
                      + count
                      + "_"
                      + Calendar.getInstance().getTimeInMillis()
                      + "_value")
              .build();
      KeyValue keyValue =
          KeyValue.newBuilder()
              .setKey("Job metadata_" + count + "_" + Calendar.getInstance().getTimeInMillis())
              .setValue(stringValue)
              .build();
      metadataList.add(keyValue);
    }

    CreateJob request =
        CreateJob.newBuilder()
            .setDescription("This is a job description.")
            .setStartTime(String.valueOf(Calendar.getInstance().getTimeInMillis()))
            .addAllMetadata(metadataList)
            .setJobStatus(JobStatus.NOT_STARTED)
            .setJobType(JobType.KUBERNETES_JOB)
            .build();

    jobServiceStub.createJob(
        request,
        new StreamObserver<CreateJob.Response>() {

          public void onNext(CreateJob.Response value) {
            testJob = value.getJob();
            try {
              LOGGER.log(Level.INFO, "Job detail : \n" + JsonFormat.printer().print(testJob));
            } catch (InvalidProtocolBufferException e) {
              e.printStackTrace();
            }
            LOGGER.info("Job Created Successfully..");
            assertTrue(request.getStartTime().equals(testJob.getStartTime()));
          }

          public void onError(Throwable t) {}

          public void onCompleted() {
            LOGGER.info("\n\n Create Job test stop................................\n\n");
          }
        });
  }

  @Test
  public void a_jobCreateNegativeTest() throws IOException {
    LOGGER.info("\n\n Create Job Negative test start................................\n\n");

    if (testJob == null) {
      a_jobCreateTest();
    }

    JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    CreateJob request =
        CreateJob.newBuilder()
            .setDescription("This is a job description.")
            .setStartTime(String.valueOf(Calendar.getInstance().getTimeInMillis()))
            .setJobStatus(JobStatus.NOT_STARTED)
            .setJobType(JobType.KUBERNETES_JOB)
            .build();

    try {
      jobServiceStub.createJob(request);
    } catch (StatusRuntimeException ex) {
      Status status2 = Status.fromThrowable(ex);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
    }
    d_deleteJobTest();
    LOGGER.info("\n\n Create Job Negative test stop................................\n\n");
  }

  @Test
  public void b_updateJobTest() throws IOException {
    LOGGER.info("\n\n Update Job test start................................\n\n");

    if (testJob == null) {
      a_jobCreateTest();
    }

    final JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    UpdateJob updateJob =
        UpdateJob.newBuilder().setId(testJob.getId()).setJobStatus(JobStatus.IN_PROGRESS).build();

    UpdateJob.Response jobUpdateResponse = jobServiceStub.updateJob(updateJob);
    testJob = jobUpdateResponse.getJob();
    LOGGER.log(Level.INFO, "Job detail : \n" + JsonFormat.printer().print(testJob));
    assertTrue(updateJob.getJobStatus().equals(testJob.getJobStatus()));
    d_deleteJobTest();
    LOGGER.info("\n\n Update Job test stop................................\n\n");
  }

  @Test
  public void b_updateJobNegativeTest() throws IOException {
    LOGGER.info("\n\n Update Job Negative test start................................\n\n");

    if (testJob == null) {
      a_jobCreateTest();
    }

    final JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    UpdateJob updateJob = UpdateJob.newBuilder().setJobStatus(JobStatus.IN_PROGRESS).build();

    try {
      jobServiceStub.updateJob(updateJob);
    } catch (StatusRuntimeException ex) {
      Status status2 = Status.fromThrowable(ex);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
    }
    d_deleteJobTest();
    LOGGER.info("\n\n Update Job test Negative stop................................\n\n");
  }

  @Test
  public void c_getJobTest() throws IOException {
    LOGGER.info("\n\n Get Job test start................................\n\n");

    if (testJob == null) {
      a_jobCreateTest();
    }

    final JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    GetJob getJobRequest = GetJob.newBuilder().setId(testJob.getId()).build();

    GetJob.Response getJobResponse = jobServiceStub.getJob(getJobRequest);
    testJob = getJobResponse.getJob();
    LOGGER.log(Level.INFO, "Job detail : \n" + JsonFormat.printer().print(testJob));
    assertTrue(getJobRequest.getId().equals(testJob.getId()));
    d_deleteJobTest();
    LOGGER.info("\n\n Get Job test stop................................\n\n");
  }

  @Test
  public void c_getJobNegativeTest() throws IOException {
    LOGGER.info("\n\n Get Job Negative test start................................\n\n");

    if (testJob == null) {
      a_jobCreateTest();
    }

    final JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    GetJob getJobRequest = GetJob.newBuilder().build();

    try {
      jobServiceStub.getJob(getJobRequest);
    } catch (StatusRuntimeException ex) {
      Status status2 = Status.fromThrowable(ex);
      assertEquals(Status.INVALID_ARGUMENT.getCode(), status2.getCode());
    }
    d_deleteJobTest();
    LOGGER.info("\n\n Get Job Negative test stop................................\n\n");
  }

  @Test
  public void d_deleteJobTest() throws IOException {
    LOGGER.info("\n\n Delete Job test start................................\n\n");

    if (testJob == null) {
      a_jobCreateTest();
    }

    final JobServiceBlockingStub jobServiceStub = JobServiceGrpc.newBlockingStub(channel);

    DeleteJob deleteJobRequest = DeleteJob.newBuilder().setId(testJob.getId()).build();

    DeleteJob.Response getJobResponse = jobServiceStub.deleteJob(deleteJobRequest);
    if (getJobResponse.getStatus()) {
      testJob = null;
    }
    assertTrue(getJobResponse.getStatus());
    LOGGER.info("\n\n Delete Job test stop................................\n\n");
  }
}
