package com.mitdbg.modeldb.job;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.CreateJob;
import com.mitdbg.modeldb.DeleteJob;
import com.mitdbg.modeldb.GetJob;
import com.mitdbg.modeldb.Job;
import com.mitdbg.modeldb.JobServiceGrpc.JobServiceImplBase;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.UpdateJob;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;

public class JobServiceImpl extends JobServiceImplBase {

  private static final Logger LOGGER = Logger.getLogger(JobServiceImpl.class.getName());
  private JobDAO jobDAO = null;

  public JobServiceImpl(JobDAO jobDAO) {
    this.jobDAO = jobDAO;
  }

  /**
   * Method to convert createJob request to Job object. This method generates the job Id using UUID
   * and puts it in Job object.
   *
   * @param CreateJob request
   * @return Job
   */
  private Job getJobFromRequest(CreateJob request) {

    if (request.getStartTime().isEmpty()) {
      String errorMessage = "Job start time is not found in request.";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(CreateJob.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    /*
     * Create Job entity from given CreateJob request. generate UUID and put as id in
     * job for uniqueness. set above created List<KeyValue> attributes in job entity.
     */
    Job.Builder jobBuilder =
        Job.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setDescription(request.getDescription())
            .setStartTime(request.getStartTime())
            .setEndTime(request.getEndTime())
            .addAllMetadata(request.getMetadataList())
            .setJobStatus(request.getJobStatus())
            .setJobType(request.getJobType());

    return jobBuilder.build();
  }

  @Override
  public void createJob(CreateJob request, StreamObserver<CreateJob.Response> responseObserver) {
    try {

      Job job = getJobFromRequest(request);
      job = jobDAO.insertJob(job);
      responseObserver.onNext(CreateJob.Response.newBuilder().setJob(job).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage("Internal server error.")
              .addDetails(Any.pack(CreateJob.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getJob(GetJob request, StreamObserver<GetJob.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Job ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetJob.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Job job = jobDAO.getJob(ModelDBConstants.ID, request.getId());
      responseObserver.onNext(GetJob.Response.newBuilder().setJob(job).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage("Internal server error.")
              .addDetails(Any.pack(GetJob.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void updateJob(UpdateJob request, StreamObserver<UpdateJob.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Job ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateJob.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Job job = jobDAO.updateJob(request.getId(), request.getJobStatus(), request.getEndTime());
      responseObserver.onNext(UpdateJob.Response.newBuilder().setJob(job).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage("Internal server error.")
              .addDetails(Any.pack(UpdateJob.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteJob(DeleteJob request, StreamObserver<DeleteJob.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Job ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteJob.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Boolean deletedStatus = jobDAO.deleteJob(request.getId());
      responseObserver.onNext(DeleteJob.Response.newBuilder().setStatus(deletedStatus).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage("Internal server error.")
              .addDetails(Any.pack(DeleteJob.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }
}
