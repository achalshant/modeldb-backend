package com.mitdbg.modeldb.project;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.App;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.ModelDBUtils;
import com.mitdbg.modeldb.Project;
import com.mitdbg.modeldb.databaseServices.DocumentService;
import io.grpc.protobuf.StatusProto;

public class ProjectDAOMongoImpl implements ProjectDAO {

  private static final Logger LOGGER = Logger.getLogger(ProjectDAOMongoImpl.class.getName());
  private String projectEntity = null;
  private String experimentEntity = null;
  private String experimentRunEntity = null;

  DocumentService documentService = null;

  public ProjectDAOMongoImpl(DocumentService documentService) {
    App app = App.getInstance();
    this.projectEntity = app.getProjectEntity();
    this.experimentEntity = app.getExperimentEntity();
    this.experimentRunEntity = app.getExperimentRunEntity();

    this.documentService = documentService;
    documentService.checkCollectionAvailability(projectEntity);
  }
  /**
   * Convert list of MongoDB document object to list of Project entity object.
   *
   * @param List<Document> documents : list of MongoDB data object.
   * @return List<Project> : project entity List
   * @throws InvalidProtocolBufferException
   */
  private List<Project> getProjectFromDocuments(List<Document> documents)
      throws InvalidProtocolBufferException {

    List<Project> projectList = new ArrayList<>();
    for (Document document : documents) {
      Project project = convertProjectFromDocument(document);
      projectList.add(project);
    }
    return projectList;
  }

  /**
   * Convert MongoDB Document object to Project entity. Here remove MongoDB "_ID" because if _id is
   * their then both the structure MongoDB Document and Project Entity is different and direct
   * conversion is not possible that's why "_ID" is remove.
   *
   * @param Document document : MongoDB data object.
   * @return Project project : ProtocolBuffer Object of Project entity.
   * @throws InvalidProtocolBufferException
   */
  private Project convertProjectFromDocument(Document document)
      throws InvalidProtocolBufferException {
    Project.Builder builder = Project.newBuilder();

    document.remove("_id");

    JsonFormat.parser().merge(document.toJson(), builder);
    return builder.build();
  }

  private void checkEntityAlreadyExist(Project project) {
    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.NAME, project.getName());
    Document existingProject = (Document) documentService.findByObject(queryDoc);

    if (existingProject != null && !existingProject.isEmpty()) {
      Status status =
          Status.newBuilder()
              .setCode(Code.ALREADY_EXISTS_VALUE)
              .setMessage("Project already exist in database")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Insert Project entity in database.
   *
   * @param ProjectEntity project
   * @return void
   */
  public Project insertProject(Project project) throws InvalidProtocolBufferException {

    checkEntityAlreadyExist(project);

    documentService.insertOne(project);
    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, project.getId());
    return convertProjectFromDocument(document);
  }

  /**
   * Update Project entity in database using projectId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String projectId, Project project
   * @return Project updated Project entity
   */
  public Project updateProject(String projectId, Project project)
      throws InvalidProtocolBufferException {
    long updatedCount = documentService.updateOne(ModelDBConstants.ID, projectId, project);
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, project.getId());
      return convertProjectFromDocument(document);
    } else {
      String errorMessage = "Updated value is already present in Project";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public Project addProjectAttributes(String projectId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException {
    Document queryProject = new Document();
    queryProject.append(ModelDBConstants.ID, projectId);

    Document updatedProject = new Document();
    List<Document> attributeDocList = new ArrayList<>();
    for (KeyValue attribute : attributesList) {
      String json = ModelDBUtils.getStringFromProtoObject(attribute);
      Document documentAttributes = Document.parse(json);
      attributeDocList.add(documentAttributes);
    }
    updatedProject.append(ModelDBConstants.ATTRIBUTES, new Document("$each", attributeDocList));

    long updatedCount =
        documentService.updateOne(queryProject, new Document("$push", updatedProject));
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, projectId);
      return convertProjectFromDocument(document);
    } else {
      String errorMessage = "Added attributes is already present in Project";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Update Project Attributes in database using projectId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String projectId, KeyValue attribute
   * @return Project updated Project entity
   */
  public Project updateProjectAttributes(String projectId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    Document queryProject = new Document();
    queryProject.append(ModelDBConstants.ID, projectId);
    queryProject.append(
        ModelDBConstants.ATTRIBUTES + "." + ModelDBConstants.KEY, attribute.getKey());

    Document updatedProject = new Document();
    String json = ModelDBUtils.getStringFromProtoObject(attribute);
    Document documentAttributes = Document.parse(json);
    updatedProject.append(ModelDBConstants.ATTRIBUTES + ".$", documentAttributes);

    long updatedCount =
        documentService.updateOne(queryProject, new Document("$set", updatedProject));
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, projectId);
      return convertProjectFromDocument(document);
    } else {
      String errorMessage = "Updated value is already present in Project";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Fetch Project Attributes from database using projectId.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param String projectId
   * @return List<KeyValue> projectAttributes.
   * @throws InvalidProtocolBufferException
   */
  public List<KeyValue> getProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {

    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.ID, projectId);

    Document projectionDoc = new Document();
    projectionDoc.append(ModelDBConstants.ATTRIBUTES, 1);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryDoc, projectionDoc, null, null);

    if (documentList != null) {
      Document document = documentList.get(0);
      Project project = convertProjectFromDocument(document);
      if (!getAll) {
        List<KeyValue> attributes = new ArrayList<>();
        project
            .getAttributesList()
            .forEach(
                attribute -> {
                  if (attributeKeyList.contains(attribute.getKey())) {
                    attributes.add(attribute);
                  }
                });
        return attributes;
      } else {
        return project.getAttributesList();
      }
    } else {
      String errorMessage = "Projects not found in database";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public Project deleteProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    Document queryProject = new Document();
    queryProject.append(ModelDBConstants.ID, projectId);

    Document updateQueryDoc = null;
    Document updatedProject = new Document();
    if (deleteAll) {
      updatedProject.append(ModelDBConstants.ATTRIBUTES, 1);
      updateQueryDoc = new Document("$unset", updatedProject);
    } else {
      updatedProject.append(
          ModelDBConstants.ATTRIBUTES,
          new Document(ModelDBConstants.KEY, new Document("$in", attributeKeyList)));
      updateQueryDoc = new Document("$pull", updatedProject);
    }

    long updatedCount = documentService.updateOne(queryProject, updateQueryDoc);
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, projectId);
      return convertProjectFromDocument(document);
    } else {
      String errorMessage =
          "The attributes field of Project is already deleted Or attributes not found in the Project";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Delete the Project in database using projectId.
   *
   * <p>Add logic of Deleting Experiment & ExperimentRun associated with project.
   *
   * @param String projectId
   * @return Boolean updated status
   */
  public Boolean deleteProject(String projectId) {
    documentService.deleteOne(experimentRunEntity, ModelDBConstants.PROJECT_ID, projectId);
    documentService.deleteOne(experimentEntity, ModelDBConstants.PROJECT_ID, projectId);
    return documentService.deleteOne(projectEntity, ModelDBConstants.ID, projectId);
  }

  /**
   * Fetch All the Project from database base on user details.
   *
   * @return List<Project> projects.
   * @throws InvalidProtocolBufferException
   */
  public List<Project> getProjects() throws InvalidProtocolBufferException {
    List<Project> projects = new ArrayList<>();

    List<Document> documentList = (List<Document>) documentService.find();
    if (!documentList.isEmpty()) {
      projects = getProjectFromDocuments(documentList);
    }

    return projects;
  }

  /**
   * Update Project Tags in database using projectId.
   *
   * @param String projectId, List<String> tagsList
   * @return Project updated Project entity
   * @throws InvalidProtocolBufferException
   */
  public Project addProjectTags(String projectId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    Document queryProject = new Document();
    queryProject.append(ModelDBConstants.ID, projectId);

    Document updatedProject = new Document();
    updatedProject.append(ModelDBConstants.TAGS, new Document("$each", tagsList));

    long updatedCount =
        documentService.updateOne(queryProject, new Document("$addToSet", updatedProject));
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, projectId);
      return convertProjectFromDocument(document);
    } else {
      String errorMessage = "Added value is already present in Project";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public List<String> getProjectTags(String projectId) throws InvalidProtocolBufferException {
    Document queryObj = new Document(ModelDBConstants.ID, projectId);

    Document projectionDoc = new Document();
    projectionDoc.append(ModelDBConstants.TAGS, 1);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryObj, projectionDoc, null, null);

    if (!documentList.isEmpty()) {
      Document document = documentList.get(0);
      Project project = convertProjectFromDocument(document);
      return project.getTagsList();
    } else {
      String errorMessage = "Project tags not found in database";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Delete Project Tags in database using projectId.
   *
   * @param projectTagList
   * @param deleteAll
   * @param String projectId
   * @return Project project
   * @throws InvalidProtocolBufferException
   */
  public Project deleteProjectTags(String projectId, List<String> projectTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    Document queryProject = new Document();
    queryProject.append(ModelDBConstants.ID, projectId);

    Document updateQueryDoc = null;
    Document updatedProject = new Document();
    if (deleteAll) {
      updatedProject.append(ModelDBConstants.TAGS, 1);
      updateQueryDoc = new Document("$unset", updatedProject);
    } else {
      updatedProject.append(ModelDBConstants.TAGS, projectTagList);
      updateQueryDoc = new Document("$pullAll", updatedProject);
    }

    long updatedCount = documentService.updateOne(queryProject, updateQueryDoc);
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, projectId);
      return convertProjectFromDocument(document);
    } else {
      String errorMessage =
          "The tags field of Project is already deleted Or tags not found in the Project";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Fetch the Project based on key and value from database.
   *
   * @param key --> key like ModelDBConstants.ID,ModelDBConstants.NAME etc.
   * @param value -- > value is project.Id, project.name etc.
   * @return Project project --> based on search return project entity.
   * @throws InvalidProtocolBufferException
   */
  @Override
  public List<Project> getProjects(String key, String value) throws InvalidProtocolBufferException {

    Document queryObj = new Document();
    queryObj.put(key, value);

    List<Document> documents =
        (List<Document>) documentService.findListByObject(queryObj, null, null, null);
    if (!documents.isEmpty()) {
      return getProjectFromDocuments(documents);
    } else {
      String errorMessage = "Project not found in database";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public List<Project> getProjectByIds(List<String> sharedProjectIds)
      throws InvalidProtocolBufferException {
    Document queryProject = new Document();
    queryProject.append(ModelDBConstants.ID, new Document("$in", sharedProjectIds));

    List<Document> projectDocumentList =
        (List<Document>) documentService.findListByObject(queryProject, null, null, null);
    return getProjectFromDocuments(projectDocumentList);
  }
}
