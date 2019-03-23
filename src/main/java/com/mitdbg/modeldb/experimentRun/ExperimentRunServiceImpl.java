package com.mitdbg.modeldb.experimentRun;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.artifactstore.ArtifactStoreGrpc;
import com.mitdbg.artifactstore.ArtifactStoreGrpc.ArtifactStoreBlockingStub;
import com.mitdbg.artifactstore.DeleteArtifact;
import com.mitdbg.artifactstore.GetArtifact;
import com.mitdbg.modeldb.AddExperimentRunAttributes;
import com.mitdbg.modeldb.AddExperimentRunTags;
import com.mitdbg.modeldb.App;
import com.mitdbg.modeldb.Artifact;
import com.mitdbg.modeldb.ArtifactTypeEnum.ArtifactType;
import com.mitdbg.modeldb.CreateExperimentRun;
import com.mitdbg.modeldb.DeleteExperiment;
import com.mitdbg.modeldb.DeleteExperimentRun;
import com.mitdbg.modeldb.DeleteExperimentRunAttributes;
import com.mitdbg.modeldb.DeleteExperimentRunTags;
import com.mitdbg.modeldb.ExperimentRun;
import com.mitdbg.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceImplBase;
import com.mitdbg.modeldb.FindExperimentRuns;
import com.mitdbg.modeldb.GetArtifacts;
import com.mitdbg.modeldb.GetAttributes;
import com.mitdbg.modeldb.GetDatasets;
import com.mitdbg.modeldb.GetExperimentRunById;
import com.mitdbg.modeldb.GetExperimentRunByName;
import com.mitdbg.modeldb.GetExperimentRunsInExperiment;
import com.mitdbg.modeldb.GetExperimentRunsInProject;
import com.mitdbg.modeldb.GetHyperparameters;
import com.mitdbg.modeldb.GetJobId;
import com.mitdbg.modeldb.GetMetrics;
import com.mitdbg.modeldb.GetObservations;
import com.mitdbg.modeldb.GetTags;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.LogArtifact;
import com.mitdbg.modeldb.LogAttribute;
import com.mitdbg.modeldb.LogDataset;
import com.mitdbg.modeldb.LogHyperparameter;
import com.mitdbg.modeldb.LogJobId;
import com.mitdbg.modeldb.LogMetric;
import com.mitdbg.modeldb.LogObservation;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.Observation;
import com.mitdbg.modeldb.SortExperimentRuns;
import com.mitdbg.modeldb.TopExperimentRunsSelector;
import com.mitdbg.modeldb.UpdateExperimentRunNameOrDescription;
import com.mitdbg.modeldb.artifactStore.ArtifactStoreDAO;
import com.mitdbg.modeldb.experiment.ExperimentDAO;
import com.mitdbg.modeldb.project.ProjectDAO;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;

public class ExperimentRunServiceImpl extends ExperimentRunServiceImplBase {

  private static final Logger LOGGER = Logger.getLogger(ExperimentRunServiceImpl.class.getName());
  private ExperimentRunDAO experimentRunDAO = null;
  private ProjectDAO projectDAO = null;
  private ExperimentDAO experimentDAO = null;
  private ArtifactStoreDAO artifactStoreDAO = null;
  private App app = App.getInstance();

  public ExperimentRunServiceImpl(
      ExperimentRunDAO experimentRunDAO,
      ProjectDAO projectDAO,
      ExperimentDAO experimentDAO,
      ArtifactStoreDAO artifactStoreDAO) {
    this.experimentRunDAO = experimentRunDAO;
    this.projectDAO = projectDAO;
    this.experimentDAO = experimentDAO;
    this.artifactStoreDAO = artifactStoreDAO;
  }

  /**
   * Convert CreateExperimentRun request to Experiment object. This method generate the
   * ExperimentRun Id using UUID and put it in ExperimentRun object.
   *
   * @param CreateExperimentRun request
   * @return ExperimentRun experimentRun
   */
  private ExperimentRun getExperimentRunFromRequest(CreateExperimentRun request) {

    if (request.getProjectId().isEmpty()
        || request.getExperimentId().isEmpty()
        || request.getName().isEmpty()) {

      String errorMessage =
          "ProjectID OR Experiment ID OR ExperimentRun name is not found in CreateExperimentRun request.";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(CreateExperimentRun.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    /*
     * Create ExperimentRun entity from given CreateExperimentRun request. generate UUID and put as
     * id in ExperimentRun for uniqueness.
     */
    ExperimentRun.Builder experimentRunBuilder =
        ExperimentRun.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setExperimentId(request.getExperimentId())
            .setName(request.getName())
            .setDescription(request.getDescription())
            .setDateCreated(request.getDateCreated())
            .setDateUpdated(request.getDateUpdated())
            .setStartTime(request.getStartTime())
            .setEndTime(request.getEndTime())
            .setCodeVersion(request.getCodeVersion())
            .addAllTags(request.getTagsList())
            .addAllAttributes(request.getAttributesList())
            .addAllHyperparameters(request.getHyperparametersList())
            .addAllArtifacts(request.getArtifactsList())
            .addAllDatasets(request.getDatasetsList())
            .addAllMetrics(request.getMetricsList())
            .addAllObservations(request.getObservationsList())
            .addAllFeatures(request.getFeaturesList());

    return experimentRunBuilder.build();
  }

  /**
   * Convert CreateExperimentRun request to ExperimentRun entity and insert in database.
   *
   * @param CreateExperimentRun request, CreateExperimentRun.Response response
   * @return void
   */
  @Override
  public void createExperimentRun(
      CreateExperimentRun request, StreamObserver<CreateExperimentRun.Response> responseObserver) {
    try {

      ExperimentRun experimentRun = getExperimentRunFromRequest(request);
      experimentRun = experimentRunDAO.insertExperimentRun(experimentRun);
      responseObserver.onNext(
          CreateExperimentRun.Response.newBuilder().setExperimentRun(experimentRun).build());
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
              .addDetails(Any.pack(CreateExperimentRun.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteExperimentRun(
      DeleteExperimentRun request, StreamObserver<DeleteExperimentRun.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in DeleteExperimentRun request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperiment.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Boolean status = experimentRunDAO.deleteExperimentRun(request.getId());
      responseObserver.onNext(DeleteExperimentRun.Response.newBuilder().setStatus(status).build());
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
              .addDetails(Any.pack(DeleteExperimentRun.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentRunsInProject(
      GetExperimentRunsInProject request,
      StreamObserver<GetExperimentRunsInProject.Response> responseObserver) {
    try {

      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID is not found in GetExperimentRunsInProject request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunsInProject.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<ExperimentRun> experimentRunList =
          experimentRunDAO.getExperimentRunsFromEntity(
              ModelDBConstants.PROJECT_ID,
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getSortOrder(),
              request.getSortBy());
      responseObserver.onNext(
          GetExperimentRunsInProject.Response.newBuilder()
              .addAllExperimentRuns(experimentRunList)
              .build());
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
              .addDetails(Any.pack(GetExperimentRunsInProject.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentRunsInExperiment(
      GetExperimentRunsInExperiment request,
      StreamObserver<GetExperimentRunsInExperiment.Response> responseObserver) {
    try {

      if (request.getExperimentId().isEmpty()) {
        String errorMessage =
            "Experiment ID is not found in GetExperimentRunsInExperiment request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunsInExperiment.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<ExperimentRun> experimentRunList =
          experimentRunDAO.getExperimentRunsFromEntity(
              ModelDBConstants.EXPERIMENT_ID,
              request.getExperimentId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getSortOrder(),
              request.getSortBy());
      responseObserver.onNext(
          GetExperimentRunsInExperiment.Response.newBuilder()
              .addAllExperimentRuns(experimentRunList)
              .build());
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
              .addDetails(Any.pack(GetExperimentRunsInExperiment.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentRunById(
      GetExperimentRunById request,
      StreamObserver<GetExperimentRunById.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in GetExperimentRunById request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<ExperimentRun> experimentRunList =
          experimentRunDAO.getExperimentRuns(ModelDBConstants.ID, request.getId());

      if (experimentRunList.isEmpty()) {
        String errorMessage = "ExperimentRun not found for given Id : " + request.getId();
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      responseObserver.onNext(
          GetExperimentRunById.Response.newBuilder()
              .setExperimentRun(experimentRunList.get(0))
              .build());
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
              .addDetails(Any.pack(GetExperimentRunById.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentRunByName(
      GetExperimentRunByName request,
      StreamObserver<GetExperimentRunByName.Response> responseObserver) {
    try {

      if (request.getName().isEmpty()) {
        String errorMessage = "ExperimentRun name is not found in GetExperimentRunByName request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<ExperimentRun> experimentRunList =
          experimentRunDAO.getExperimentRuns(ModelDBConstants.NAME, request.getName());

      responseObserver.onNext(
          GetExperimentRunByName.Response.newBuilder()
              .addAllExperimentRuns(experimentRunList)
              .build());
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
              .addDetails(Any.pack(GetExperimentRunByName.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void updateExperimentRunNameOrDescription(
      UpdateExperimentRunNameOrDescription request,
      StreamObserver<UpdateExperimentRunNameOrDescription.Response> responseObserver) {

    try {
      if (request.getId().isEmpty()) {
        String errorMessage =
            "ExperimentRun ID is not found in UpdateExperimentRunNameOrDescription request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(
                    Any.pack(UpdateExperimentRunNameOrDescription.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun experimentRun =
          ExperimentRun.newBuilder()
              .setId(request.getId())
              .setName(request.getName())
              .setDescription(request.getDescription())
              .build();

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.updateExperimentRun(request.getId(), experimentRun);

      responseObserver.onNext(
          UpdateExperimentRunNameOrDescription.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
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
              .addDetails(
                  Any.pack(UpdateExperimentRunNameOrDescription.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void addExperimentRunTags(
      AddExperimentRunTags request,
      StreamObserver<AddExperimentRunTags.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in AddExperimentRunTags request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentRunTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.addExperimentRunTags(request.getId(), request.getTagsList());
      responseObserver.onNext(
          AddExperimentRunTags.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
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
              .addDetails(Any.pack(AddExperimentRunTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentRunTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in GetExperimentRunTags request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<String> experimentRunTags = experimentRunDAO.getExperimentRunTags(request.getId());
      responseObserver.onNext(GetTags.Response.newBuilder().addAllTags(experimentRunTags).build());
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
              .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteExperimentRunTags(
      DeleteExperimentRunTags request,
      StreamObserver<DeleteExperimentRunTags.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()
          || (request.getTagsList().isEmpty() && !request.getDeleteAll())) {
        String errorMessage =
            "ExperimentRun ID OR ExperimentRun tag list is not found OR delete_all flag is not set true in DeleteExperimentRunTags request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentRunTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteExperimentRunTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentRunTags.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
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
              .addDetails(Any.pack(DeleteExperimentRunTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void addExperimentRunAttributes(
      AddExperimentRunAttributes request,
      StreamObserver<AddExperimentRunAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty() || request.getAttributesList().isEmpty()) {
        String errorMessage =
            "ExperimentRun ID OR Attribute key OR Attributes value is not found in AddExperimentRunAttributes request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentRunAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.addExperimentRunAttributes(request.getId(), request.getAttributesList());
      responseObserver.onNext(
          AddExperimentRunAttributes.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
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
              .addDetails(Any.pack(AddExperimentRunAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteExperimentRunAttributes(
      DeleteExperimentRunAttributes request,
      StreamObserver<DeleteExperimentRunAttributes.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()
          || (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll())) {
        String errorMessage =
            "ExperimentRun ID OR ExperimentRun attribute list is not found OR delete_all flag is not set true  in DeleteExperimentRunAttributes request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentRunAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteExperimentRunAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentRunAttributes.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
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
              .addDetails(Any.pack(DeleteExperimentRunAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void logObservation(
      LogObservation request, StreamObserver<LogObservation.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()
          || (!request.getObservation().hasArtifact()
              && !request.getObservation().hasAttribute())) {
        String errorMessage =
            "ExperimentRun ID OR New Observation is not found in LogObservation request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogObservation.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logObservation(request.getId(), request.getObservation());
      responseObserver.onNext(
          LogObservation.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
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
              .addDetails(Any.pack(LogObservation.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getObservations(
      GetObservations request, StreamObserver<GetObservations.Response> responseObserver) {
    try {

      if (request.getId().isEmpty() || request.getObservationKey().isEmpty()) {
        String errorMessage =
            "ExperimentRun ID OR Observation key is not found in GetObservations request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetObservations.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Observation> observations =
          experimentRunDAO.getObservationByKey(request.getId(), request.getObservationKey());
      responseObserver.onNext(
          GetObservations.Response.newBuilder().addAllObservations(observations).build());
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
              .addDetails(Any.pack(GetObservations.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void logMetric(LogMetric request, StreamObserver<LogMetric.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()
          || (request.getMetric().getKey() == null || request.getMetric().getValue() == null)) {
        String errorMessage =
            "ExperimentRun ID OR New Metric KeyValue is not found in LogMetric request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogMetric.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> metricList = experimentRunDAO.getExperimentRunMetrics(request.getId());
      for (KeyValue metric : metricList) {
          if (metric.getKey() == request.getMetric().getKey()) {
              Status status =
                  Status.newBuilder()
                      .setCode(Code.ALREADY_EXISTS.getNumber())
                      .setMessage("Metric being logged already exists.")
                      .addDetails(Any.pack(LogMetric.Response.getDefaultInstance()))
                      .build();
              throw StatusProto.toStatusRuntimeException(status);
          }
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logMetric(request.getId(), request.getMetric());
      responseObserver.onNext(
          LogMetric.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
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
              .addDetails(Any.pack(LogMetric.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getMetrics(GetMetrics request, StreamObserver<GetMetrics.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in GetMetrics request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetMetrics.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> metricList = experimentRunDAO.getExperimentRunMetrics(request.getId());
      responseObserver.onNext(GetMetrics.Response.newBuilder().addAllMetrics(metricList).build());
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
              .addDetails(Any.pack(GetMetrics.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void logDataset(LogDataset request, StreamObserver<LogDataset.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()
          || (request.getDataset().getKey().isEmpty()
              || request.getDataset().getPath().isEmpty())) {
        String errorMessage = "ExperimentRun ID OR New DataSet is not found in LogDataset request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogDataset.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logDataSet(request.getId(), request.getDataset());
      responseObserver.onNext(
          LogDataset.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
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
              .addDetails(Any.pack(LogDataset.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getDatasets(
      GetDatasets request, StreamObserver<GetDatasets.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in GetDatasets request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetDatasets.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Artifact> dataSetList = experimentRunDAO.getExperimentRunDataSets(request.getId());
      responseObserver.onNext(
          GetDatasets.Response.newBuilder().addAllDatasets(dataSetList).build());
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
              .addDetails(Any.pack(GetDatasets.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void logArtifact(
      LogArtifact request, StreamObserver<LogArtifact.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()
          || (request.getArtifact().getKey().isEmpty()
              || request.getArtifact().getPath().isEmpty())) {
        String errorMessage =
            "ExperimentRun ID OR New Artifact is not found in LogArtifact request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logArtifact(request.getId(), request.getArtifact());
      responseObserver.onNext(
          LogArtifact.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
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
              .addDetails(Any.pack(LogArtifact.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in GetArtifacts request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetArtifacts.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Artifact> artifactList = experimentRunDAO.getExperimentRunArtifacts(request.getId());
      responseObserver.onNext(
          GetArtifacts.Response.newBuilder().addAllArtifacts(artifactList).build());
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
              .addDetails(Any.pack(GetArtifacts.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void logHyperparameter(
      LogHyperparameter request, StreamObserver<LogHyperparameter.Response> responseObserver) {
    try {
      if (request.getId().isEmpty() || request.getHyperparameter().getKey().isEmpty()) {
        String errorMessage =
            "ExperimentRun ID OR New Hyperparameter is not found in LogHyperparameter request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogHyperparameter.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> hyperparameterList =
          experimentRunDAO.getExperimentRunHyperparameters(request.getId());
      for (KeyValue hyperparameter : hyperparameterList) {
          if (hyperparameter.getKey() == request.getHyperparameter().getKey()) {
              Status status =
                  Status.newBuilder()
                      .setCode(Code.ALREADY_EXISTS.getNumber())
                      .setMessage("Hyperparameter being logged already exists.")
                      .addDetails(Any.pack(LogHyperparameter.Response.getDefaultInstance()))
                      .build();
              throw StatusProto.toStatusRuntimeException(status);
          }
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logHyperparameter(request.getId(), request.getHyperparameter());
      responseObserver.onNext(
          LogHyperparameter.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
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
              .addDetails(Any.pack(LogHyperparameter.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getHyperparameters(
      GetHyperparameters request, StreamObserver<GetHyperparameters.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessaeg = "ExperimentRun ID is not found in GetHyperparameters request.";
        LOGGER.log(Level.WARNING, errorMessaeg);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessaeg)
                .addDetails(Any.pack(GetHyperparameters.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> hyperparameterList =
          experimentRunDAO.getExperimentRunHyperparameters(request.getId());
      responseObserver.onNext(
          GetHyperparameters.Response.newBuilder()
              .addAllHyperparameters(hyperparameterList)
              .build());
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
              .addDetails(Any.pack(GetHyperparameters.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void logAttribute(
      LogAttribute request, StreamObserver<LogAttribute.Response> responseObserver) {
    try {
      if (request.getId().isEmpty() || request.getAttribute().getKey().isEmpty()) {
        String errorMessage =
            "ExperimentRun ID OR New Attribute is not found in LogAttribute request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogAttribute.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logAttribute(request.getId(), request.getAttribute());
      responseObserver.onNext(
          LogAttribute.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
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
              .addDetails(Any.pack(LogAttribute.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentRunAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()
          || (request.getAttributeKeysList().isEmpty() && !request.getGetAll())) {
        String errorMessage =
            "ExperimentRun ID OR Attribute list is not found OR get_all flag is not set true in GetAttributes request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> attributeList =
          experimentRunDAO.getExperimentRunAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());
      responseObserver.onNext(
          GetAttributes.Response.newBuilder().addAllAttributes(attributeList).build());
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
              .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void findExperimentRuns(
      FindExperimentRuns request, StreamObserver<FindExperimentRuns.Response> responseObserver) {
    try {

      if (request.getProjectId().isEmpty()
          && request.getExperimentId().isEmpty()
          && request.getExperimentRunIdsList().isEmpty()) {
        String errorMessage =
            "Project ID OR Experiment ID OR ExperimentRun Id's is not found in FindExperimentRuns request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindExperimentRuns.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<ExperimentRun> experimentRuns = experimentRunDAO.findExperimentRuns(request);
      responseObserver.onNext(
          FindExperimentRuns.Response.newBuilder().addAllExperimentRuns(experimentRuns).build());
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
              .addDetails(Any.pack(FindExperimentRuns.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void sortExperimentRuns(
      SortExperimentRuns request, StreamObserver<SortExperimentRuns.Response> responseObserver) {

    try {
      if (request.getExperimentRunIdsList().isEmpty() || request.getSortKey().isEmpty()) {
        String errorMessage =
            "ExperimentRun Id's OR sort key is not found in SortExperimentRuns request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(SortExperimentRuns.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<ExperimentRun> experimentRuns = experimentRunDAO.sortExperimentRuns(request);
      responseObserver.onNext(
          SortExperimentRuns.Response.newBuilder().addAllExperimentRuns(experimentRuns).build());
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
              .addDetails(Any.pack(SortExperimentRuns.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getTopExperimentRuns(
      TopExperimentRunsSelector request,
      StreamObserver<TopExperimentRunsSelector.Response> responseObserver) {

    try {
      if ((request.getProjectId().isEmpty()
              && request.getExperimentId().isEmpty()
              && request.getExperimentRunIdsList().isEmpty())
          || request.getSortKey().isEmpty()) {
        String errorMessage =
            "Project ID OR Experiment ID OR Experiment IDs OR Sort key is not found in FindExperimentRuns request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(TopExperimentRunsSelector.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<ExperimentRun> experimentRuns = experimentRunDAO.getTopExperimentRuns(request);
      responseObserver.onNext(
          TopExperimentRunsSelector.Response.newBuilder()
              .addAllExperimentRuns(experimentRuns)
              .build());
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
              .addDetails(Any.pack(TopExperimentRunsSelector.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void logJobId(
      LogJobId request, StreamObserver<com.mitdbg.modeldb.LogJobId.Response> responseObserver) {
    try {
      if (request.getId().isEmpty() || request.getJobId().isEmpty()) {
        String errorMessage = "ExperimentRun ID OR Job ID is not found in LogAttribute request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogJobId.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logJobId(request.getId(), request.getJobId());
      responseObserver.onNext(
          LogJobId.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
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
              .addDetails(Any.pack(LogJobId.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getJobId(
      GetJobId request, StreamObserver<com.mitdbg.modeldb.GetJobId.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID is not found in LogAttribute request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetJobId.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String jobId = experimentRunDAO.getJobId(request.getId());
      responseObserver.onNext(GetJobId.Response.newBuilder().setJobId(jobId).build());
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
              .addDetails(Any.pack(GetJobId.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }
}
