package com.mitdbg.modeldb.project;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.AddProjectAttributes;
import com.mitdbg.modeldb.AddProjectTags;
import com.mitdbg.modeldb.CreateProject;
import com.mitdbg.modeldb.DeleteProject;
import com.mitdbg.modeldb.DeleteProjectAttributes;
import com.mitdbg.modeldb.DeleteProjectTags;
import com.mitdbg.modeldb.Empty;
import com.mitdbg.modeldb.GetAttributes;
import com.mitdbg.modeldb.GetProjectById;
import com.mitdbg.modeldb.GetProjectByName;
import com.mitdbg.modeldb.GetProjects;
import com.mitdbg.modeldb.GetTags;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.Project;
import com.mitdbg.modeldb.ProjectServiceGrpc.ProjectServiceImplBase;
import com.mitdbg.modeldb.UpdateProjectAttributes;
import com.mitdbg.modeldb.UpdateProjectNameOrDescription;
import com.mitdbg.modeldb.VerifyConnectionResponse;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;

public class ProjectServiceImpl extends ProjectServiceImplBase {

  public static final Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class.getName());
  ProjectDAO projectDAO = null;

  public ProjectServiceImpl(ProjectDAO projectDAOImpl) {
    this.projectDAO = projectDAOImpl;
  }

  /**
   * Method to convert createProject request to Project object. This method generates the project Id
   * using UUID and puts it in Project object.
   *
   * @param CreateProject request
   * @return Project
   */
  private Project getProjectFromRequest(CreateProject request) {

    if (request.getName().isEmpty()) {

      LOGGER.log(Level.WARNING, "Project name is not found in request.");
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage("Project name is not found in request")
              .addDetails(Any.pack(CreateProject.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    /*
     * Create Project entity from given CreateProject request. generate UUID and put as id in
     * project for uniqueness. set above created List<KeyValue> attributes in project entity.
     */
    Project.Builder projectBuilder =
        Project.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setName(request.getName())
            .setDescription(request.getDescription())
            .setDateCreated(Calendar.getInstance().getTimeInMillis())
            .setDateUpdated(Calendar.getInstance().getTimeInMillis())
            .addAllAttributes(request.getMetadataList())
            .addAllTags(request.getTagsList());

    return projectBuilder.build();
  }

  /**
   * Convert CreateProject request to Project entity and insert in database.
   *
   * @param CreateProject request, CreateProject.Response response
   * @return void
   */
  @Override
  public void createProject(
      CreateProject request, StreamObserver<CreateProject.Response> responseObserver) {

    try {

      Project project = getProjectFromRequest(request);
      project = projectDAO.insertProject(project);
      responseObserver.onNext(CreateProject.Response.newBuilder().setProject(project).build());
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
              .addDetails(Any.pack(CreateProject.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  /**
   * Update project name Or Description in Project Entity. Create project object with updated data
   * from UpdateProjectNameOrDescription request and update in database.
   *
   * @param UpdateProjectNameOrDescription request, UpdateProjectNameOrDescription.Response response
   * @return void
   */
  @Override
  public void updateProjectNameOrDescription(
      UpdateProjectNameOrDescription request,
      StreamObserver<UpdateProjectNameOrDescription.Response> responseObserver) {

    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Project ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateProjectNameOrDescription.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Project project =
          Project.newBuilder()
              .setId(request.getId())
              .setName(request.getName())
              .setDescription(request.getDescription())
              .build();

      Project updatedProject = projectDAO.updateProject(request.getId(), project);

      responseObserver.onNext(
          UpdateProjectNameOrDescription.Response.newBuilder().setProject(updatedProject).build());
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
              .addDetails(Any.pack(UpdateProjectNameOrDescription.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void addProjectAttributes(
      AddProjectAttributes request,
      StreamObserver<AddProjectAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty() || request.getAttributesList().isEmpty()) {
        String errorMessage =
            "Project ID OR Attribute key OR Attribute value is not found in AddProjectAttributes request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddProjectAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Project updatedProject =
          projectDAO.addProjectAttributes(request.getId(), request.getAttributesList());
      responseObserver.onNext(
          AddProjectAttributes.Response.newBuilder().setProject(updatedProject).build());
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
              .addDetails(Any.pack(AddProjectAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  /**
   * Updates the project Attributes field from the Project Entity.
   *
   * @param UpdateProjectAttributes request, UpdateProjectAttributes.Response response
   * @return void
   */
  @Override
  public void updateProjectAttributes(
      UpdateProjectAttributes request,
      StreamObserver<UpdateProjectAttributes.Response> responseObserver) {

    try {
      // Request Parameter Validation
      if (request.getId().isEmpty() || request.getAttribute().getKey().isEmpty()) {
        String errorMessage = "Project ID OR Attribute key is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateProjectAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Project updatedProject =
          projectDAO.updateProjectAttributes(request.getId(), request.getAttribute());
      responseObserver.onNext(
          UpdateProjectAttributes.Response.newBuilder().setProject(updatedProject).build());
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
              .addDetails(Any.pack(UpdateProjectAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  /**
   * This method provide List<KeyValue> attributes of given projectId in GetProjectAttributes
   * request.
   *
   * @param GetProjectAttributes request, GetProjectAttributes.Response response
   * @return void
   */
  @Override
  public void getProjectAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()
          || (request.getAttributeKeysList().isEmpty() && !request.getGetAll())) {
        String errorMessage =
            "Project ID OR attribute key list is not found OR get_all flag is not set true in GetAttributes request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> attributes =
          projectDAO.getProjectAttributes(
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
              .setMessage("Internal server error.")
              .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void deleteProjectAttributes(
      DeleteProjectAttributes request,
      StreamObserver<DeleteProjectAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()
          || (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll())) {
        String errorMessage =
            "Project ID OR Attribute list is not found OR delete_all flag is not set true in DeleteProjectAttributes request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteProjectAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Project updatedProject =
          projectDAO.deleteProjectAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteProjectAttributes.Response.newBuilder().setProject(updatedProject).build());
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
              .addDetails(Any.pack(DeleteProjectAttributes.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  /**
   * Add the Tags in project Tags field.
   *
   * @param AddProjectTags request, AddProjectTags.Response response
   * @return void
   */
  @Override
  public void addProjectTags(
      AddProjectTags request, StreamObserver<AddProjectTags.Response> responseObserver) {

    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Project ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddProjectTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Project updatedProject = projectDAO.addProjectTags(request.getId(), request.getTagsList());
      responseObserver.onNext(
          AddProjectTags.Response.newBuilder().setProject(updatedProject).build());
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
              .addDetails(Any.pack(AddProjectTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getProjectTags(GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Project ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<String> tags = projectDAO.getProjectTags(request.getId());
      responseObserver.onNext(GetTags.Response.newBuilder().addAllTags(tags).build());
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

  /**
   * Delete the project Tags field from the Project Entity.
   *
   * @param DeleteProjectTags request, DeleteProjectTags.Response response
   * @return void
   */
  @Override
  public void deleteProjectTags(
      DeleteProjectTags request, StreamObserver<DeleteProjectTags.Response> responseObserver) {

    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()
          || (request.getTagsList().isEmpty() && !request.getDeleteAll())) {
        String errorMessage =
            "Project ID OR Project tag list is not found OR OR delete_all flag is not set true in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteProjectTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Project updatedProject =
          projectDAO.deleteProjectTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteProjectTags.Response.newBuilder().setProject(updatedProject).build());
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
              .addDetails(Any.pack(DeleteProjectTags.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  /**
   * Get ProjectId from DeleteProject request and delete it from database.
   *
   * @param DeleteProject request, DeleteProject.Response response
   * @return void
   */
  @Override
  public void deleteProject(
      DeleteProject request, StreamObserver<DeleteProject.Response> responseObserver) {

    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Project ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteProject.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Boolean deletedStatus = projectDAO.deleteProject(request.getId());
      responseObserver.onNext(DeleteProject.Response.newBuilder().setStatus(deletedStatus).build());
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
              .addDetails(Any.pack(DeleteProject.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  /**
   * Gets all the projects belonging to the user and returns as response. If user auth is not
   * enabled, it returns all the projects from the database.
   *
   * @param GetProjects request, GetProjects.Response response
   * @return void
   */
  @Override
  public void getProjects(
      GetProjects request, StreamObserver<GetProjects.Response> responseObserver) {
    try {

      List<Project> projects = projectDAO.getProjects();
      responseObserver.onNext(GetProjects.Response.newBuilder().addAllProjectsByUser(projects).build());
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
              .addDetails(Any.pack(GetProjects.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getProjectById(
      GetProjectById request, StreamObserver<GetProjectById.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        String errorMessage = "Project ID is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetProjectById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Project> projects = projectDAO.getProjects(ModelDBConstants.ID, request.getId());
      responseObserver.onNext(
          GetProjectById.Response.newBuilder().setProject(projects.get(0)).build());
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
              .addDetails(Any.pack(GetProjectById.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getProjectByName(
      GetProjectByName request, StreamObserver<GetProjectByName.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getName().isEmpty()) {
        String errorMessage = "Project name is not found in request.";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetProjectByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Project> projects = projectDAO.getProjects(ModelDBConstants.NAME, request.getName());

      responseObserver.onNext(
          GetProjectByName.Response.newBuilder().addAllProjects(projects).build());
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
              .addDetails(Any.pack(GetProjectByName.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void verifyConnection(
      Empty request, StreamObserver<VerifyConnectionResponse> responseObserver) {

    responseObserver.onNext(VerifyConnectionResponse.newBuilder().setStatus(true).build());
    responseObserver.onCompleted();
  }
}
