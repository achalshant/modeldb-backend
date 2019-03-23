package com.mitdbg.modeldb.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.protobuf.Any;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.AddAttributes;
import com.mitdbg.modeldb.AddExperimentAttributes;
import com.mitdbg.modeldb.AddExperimentTags;
import com.mitdbg.modeldb.CreateExperiment;
import com.mitdbg.modeldb.DeleteExperiment;
import com.mitdbg.modeldb.DeleteExperimentAttributes;
import com.mitdbg.modeldb.DeleteExperimentAttributes.Response;
import com.mitdbg.modeldb.DeleteExperimentTags;
import com.mitdbg.modeldb.Experiment;
import com.mitdbg.modeldb.ExperimentServiceGrpc.ExperimentServiceImplBase;
import com.mitdbg.modeldb.GetAttributes;
import com.mitdbg.modeldb.GetExperimentById;
import com.mitdbg.modeldb.GetExperimentByName;
import com.mitdbg.modeldb.GetExperimentsInProject;
import com.mitdbg.modeldb.GetTags;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.UpdateExperimentNameOrDescription;
import com.mitdbg.modeldb.project.ProjectDAO;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;

public class ExperimentServiceImpl extends ExperimentServiceImplBase {

  private static final Logger LOGGER = Logger.getLogger(ExperimentServiceImpl.class.getName());
  private ExperimentDAO experimentDAO = null;
  private ProjectDAO projectDAO = null;

  public ExperimentServiceImpl(ExperimentDAO experimentDAO, ProjectDAO projectDAO) {
    this.experimentDAO = experimentDAO;
    this.projectDAO = projectDAO;
  }

  /**
   * Convert CreateExperiment request to Experiment object. This method generate the experiment Id
   * using UUID and put it in Experiment object.
   *
   * @param CreateExperiment request
   * @return Experiment experiment
   */
  private Experiment getExperimentFromRequest(CreateExperiment request) {

    if (request.getProjectId().isEmpty() || request.getName().isEmpty()) {
      String errorMessage =
          "ProjectID OR Experiment name is not found in CreateExperiment request.";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(CreateExperiment.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    /*
     * Create Experiment entity from given CreateExperiment request. generate UUID and put as id in
     * Experiment for uniqueness.
     */
    Experiment.Builder experimentBuilder =
        Experiment.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setName(request.getName())
            .setDescription(request.getDescription())
            .setDateCreated(request.getDateCreated())
            .setDateUpdated(request.getDateUpdated())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(request.getTagsList());

    return experimentBuilder.build();
  }

  /**
   * Convert CreateExperiment request to Experiment entity and insert in database.
   *
   * @param CreateExperiment request, CreateExperiment.Response response
   * @return void
   */
  @Override
  public void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    try {

      Experiment experiment = getExperimentFromRequest(request);
      experiment = experimentDAO.insertExperiment(experiment);
      responseObserver.onNext(
          CreateExperiment.Response.newBuilder().setExperiment(experiment).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(CreateExperiment.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentsInProject(
      GetExperimentsInProject request,
      StreamObserver<GetExperimentsInProject.Response> responseObserver) {
    try {

      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID is not found in GetExperimentsInProject request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentsInProject.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Experiment> experimentList =
          experimentDAO.getExperimentsInProject(
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getSortOrder(),
              request.getSortBy());
      responseObserver.onNext(
          GetExperimentsInProject.Response.newBuilder().addAllExperiments(experimentList).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(GetExperimentsInProject.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentById(
      GetExperimentById request, StreamObserver<GetExperimentById.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Experiment experiment = experimentDAO.getExperiment(request.getId());
      responseObserver.onNext(
          GetExperimentById.Response.newBuilder().setExperiment(experiment).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(GetExperimentById.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentByName(
      GetExperimentByName request, StreamObserver<GetExperimentByName.Response> responseObserver) {
    try {

      if (request.getProjectId().isEmpty() || request.getName().isEmpty()) {
        String errorMessage = "Experiment name OR projectId is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> keyValue = new ArrayList<>();
      Value projectIdValue = Value.newBuilder().setStringValue(request.getProjectId()).build();
      Value nameValue = Value.newBuilder().setStringValue(request.getName()).build();
      keyValue.add(
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.PROJECT_ID)
              .setValue(projectIdValue)
              .build());
      keyValue.add(KeyValue.newBuilder().setKey(ModelDBConstants.NAME).setValue(nameValue).build());

      Experiment experiment = experimentDAO.getExperiment(keyValue);
      responseObserver.onNext(
          GetExperimentByName.Response.newBuilder().setExperiment(experiment).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(GetExperimentByName.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  /**
   * Update Experiment name Or Description in Experiment Entity. Create Experiment object with
   * updated data from UpdateExperimentNameOrDescription request and update in database.
   *
   * @param UpdateExperimentNameOrDescription request, UpdateExperimentNameOrDescription.Response
   *     response
   * @return void
   */
  @Override
  public void updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request,
      StreamObserver<UpdateExperimentNameOrDescription.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        LOGGER.log(Level.WARNING, "Experiment ID is not found in request.");
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Experiment ID is not found in request")
                .addDetails(
                    Any.pack(UpdateExperimentNameOrDescription.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Experiment experiment =
          Experiment.newBuilder()
              .setId(request.getId())
              .setName(request.getName())
              .setDescription(request.getDescription())
              .build();

      Experiment updatedExperiment = experimentDAO.updateExperiment(request.getId(), experiment);

      responseObserver.onNext(
          UpdateExperimentNameOrDescription.Response.newBuilder()
              .setExperiment(updatedExperiment)
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
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(UpdateExperimentNameOrDescription.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void addExperimentTags(
      AddExperimentTags request, StreamObserver<AddExperimentTags.Response> responseObserver) {

    try {
      if (request.getId().isEmpty()) {
        LOGGER.log(Level.WARNING, "Experiment ID is not found in request.");
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Experiment ID is not found in request")
                .addDetails(Any.pack(AddExperimentTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Experiment updatedExperiment =
          experimentDAO.addExperimentTags(request.getId(), request.getTagsList());
      responseObserver.onNext(
          AddExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(AddExperimentTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<String> experimentTags = experimentDAO.getExperimentTags(request.getId());
      responseObserver.onNext(GetTags.Response.newBuilder().addAllTags(experimentTags).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteExperimentTags(
      DeleteExperimentTags request,
      StreamObserver<DeleteExperimentTags.Response> responseObserver) {

    try {
      if (request.getId().isEmpty()
          || (request.getTagsList().isEmpty() && !request.getDeleteAll())) {
        String errorMessage =
            "Experiment ID OR Experiment tag list is not found OR delete_all flag is not set true in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(DeleteExperimentTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void addAttribute(
      AddAttributes request, StreamObserver<AddAttributes.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        LOGGER.log(Level.WARNING, "Experiment ID is not found in request.");
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Experiment ID is not found in request")
                .addDetails(Any.pack(AddAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      experimentDAO.addExperimentAttributes(request.getId(), Arrays.asList(request.getAttribute()));
      responseObserver.onNext(AddAttributes.Response.newBuilder().setStatus(true).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(AddAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void addExperimentAttributes(
      AddExperimentAttributes request,
      StreamObserver<AddExperimentAttributes.Response> responseObserver) {
    try {

      if (request.getId().isEmpty() || request.getAttributesList().isEmpty()) {
        String errorMessage = "Experiment ID OR Attribute list is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Experiment experiment =
          experimentDAO.addExperimentAttributes(request.getId(), request.getAttributesList());
      responseObserver.onNext(
          AddExperimentAttributes.Response.newBuilder().setExperiment(experiment).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(AddExperimentAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()
          || (request.getAttributeKeysList().isEmpty() && !request.getGetAll())) {
        LOGGER.log(
            Level.WARNING,
            "Experiment ID OR Attribute list is not found OR get_all flag is not set true in request.");
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Experiment ID is not found in request")
                .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> attributes =
          experimentDAO.getExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());
      responseObserver.onNext(
          GetAttributes.Response.newBuilder().addAllAttributes(attributes).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteExperimentAttributes(
      DeleteExperimentAttributes request, StreamObserver<Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()
          || (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll())) {
        String errorMessage =
            "Experiment ID OR Experiment attribute list is not found OR delete_all flag is not set true  in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentAttributes.Response.newBuilder()
              .setExperiment(updatedExperiment)
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
              .addDetails(Any.pack(DeleteExperimentAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteExperiment(
      DeleteExperiment request, StreamObserver<DeleteExperiment.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        LOGGER.log(Level.WARNING, "Experiment ID is not found in request.");
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Experiment ID is not found in request")
                .addDetails(Any.pack(DeleteExperiment.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Boolean status = experimentDAO.deleteExperiment(request.getId());
      responseObserver.onNext(DeleteExperiment.Response.newBuilder().setStatus(status).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(" Internal server error.")
              .addDetails(Any.pack(DeleteExperiment.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }
}
