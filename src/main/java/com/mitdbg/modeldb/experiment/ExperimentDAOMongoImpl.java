package com.mitdbg.modeldb.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.App;
import com.mitdbg.modeldb.Experiment;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.ModelDBUtils;
import com.mitdbg.modeldb.databaseServices.DocumentService;
import io.grpc.protobuf.StatusProto;

public class ExperimentDAOMongoImpl implements ExperimentDAO {

  private static final Logger LOGGER = Logger.getLogger(ExperimentDAOMongoImpl.class.getName());
  private String experimentEntity = null;
  private String experimentRunEntity = null;
  DocumentService documentService = null;

  public ExperimentDAOMongoImpl(DocumentService documentService) {
    App app = App.getInstance();
    this.experimentEntity = app.getExperimentEntity();
    this.experimentRunEntity = app.getExperimentRunEntity();

    this.documentService = documentService;
    documentService.checkCollectionAvailability(experimentEntity);
  }

  /**
   * Convert MongoDB Document object to Experiment entity. Here remove MongoDB "_ID" because if _id
   * is their then both the structure MongoDB Document and Experiment Entity is different and direct
   * conversion is not possible that's why "_ID" is remove.
   *
   * @param Document document : MongoDB data object.
   * @return Experiment experiment : ProtocolBuffer Object of Experiment entity.
   * @throws InvalidProtocolBufferException
   */
  private Experiment convertExperimentFromDocument(Document document)
      throws InvalidProtocolBufferException {
    Experiment.Builder builder = Experiment.newBuilder();

    document.remove("_id");

    JsonFormat.parser().merge(document.toJson(), builder);
    return builder.build();
  }

  /**
   * Convert list of MongoDB document object to list of Experiment entity object.
   *
   * @param List<Document> documents : list of MongoDB data object.
   * @return List<Experiment> : Experiment entity List
   * @throws InvalidProtocolBufferException
   */
  private List<Experiment> getExperimentsFromDocuments(List<Document> documents)
      throws InvalidProtocolBufferException {

    List<Experiment> experimentList = new ArrayList<>();
    for (Document document : documents) {
      Experiment experiment = convertExperimentFromDocument(document);
      experimentList.add(experiment);
    }
    return experimentList;
  }

  private void checkEntityAlreadyExist(Experiment experiment) {

    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.PROJECT_ID, experiment.getProjectId());
    queryDoc.append(ModelDBConstants.NAME, experiment.getName());
    Document existingExperiment = (Document) documentService.findByObject(queryDoc);

    if (existingExperiment != null && !existingExperiment.isEmpty()) {
      Status status =
          Status.newBuilder()
              .setCode(Code.ALREADY_EXISTS_VALUE)
              .setMessage("Experiment already exist in database")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Insert Experiment entity in database.
   *
   * @param Experiment experiment
   * @return Experiment insertedExperiment
   * @throws InvalidProtocolBufferException
   */
  public Experiment insertExperiment(Experiment experiment) throws InvalidProtocolBufferException {

    checkEntityAlreadyExist(experiment);

    documentService.insertOne(experiment);
    Document document =
        (Document) documentService.findByKey(ModelDBConstants.ID, experiment.getId());
    return convertExperimentFromDocument(document);
  }

  /**
   * Update Experiment entity in database using experimentId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String experimentId, Experiment experiment
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  public Experiment updateExperiment(String experimentId, Experiment experiment)
      throws InvalidProtocolBufferException {
    long updatedCount = documentService.updateOne(ModelDBConstants.ID, experimentId, experiment);
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentId);
      return convertExperimentFromDocument(document);
    } else {
      String errorMessage = "Updated value is already present in Experiment";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Get Experiment entity using given experimentID from database.
   *
   * @param String experimentId
   * @return Experiment insertedExperiment
   * @throws InvalidProtocolBufferException
   */
  public Experiment getExperiment(String experimentId) throws InvalidProtocolBufferException {
    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentId);
    if (document != null) {
      return convertExperimentFromDocument(document);
    } else {
      String errorMessage = "Experiment not found for given ID : " + experimentId;
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Get List of Experiment entity using given projectId from database.
   *
   * @param String projectId
   * @return List<Experiment> list of experiment
   * @throws InvalidProtocolBufferException
   */
  public List<Experiment> getExperimentsInProject(
      String projectId, Integer pageNumber, Integer pageLimit, String order, String sortBy)
      throws InvalidProtocolBufferException {
    List<Experiment> experiments = new ArrayList<>();
    List<Document> documentList =
        (List<Document>)
            documentService.findListByKey(
                ModelDBConstants.PROJECT_ID, projectId, pageNumber, pageLimit, order, sortBy);
    if (!documentList.isEmpty()) {
      experiments = getExperimentsFromDocuments(documentList);
    }
    return experiments;
  }

  /**
   * Return experiment using given key value list. keyValue has key as ModelDBConstants.PROJECT_ID
   * etc. and value as experiment.projectId
   *
   * @param keyValue --> list of KeyValue
   * @return Experiment entity.
   * @throws InvalidProtocolBufferException
   */
  @Override
  public Experiment getExperiment(List<KeyValue> keyValues) throws InvalidProtocolBufferException {
    Document queryDocument = new Document();
    for (KeyValue keyValue : keyValues) {
      Value value = keyValue.getValue();
      switch (value.getKindCase()) {
        case NUMBER_VALUE:
          queryDocument.append(keyValue.getKey(), value.getNumberValue());
          break;
        case STRING_VALUE:
          queryDocument.append(keyValue.getKey(), value.getStringValue());
          break;
        case BOOL_VALUE:
          queryDocument.append(keyValue.getKey(), value.getBoolValue());
          break;
        default:
          Status invalidValueTypeError =
              Status.newBuilder()
                  .setCode(Code.UNIMPLEMENTED_VALUE)
                  .setMessage(
                      "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE")
                  .build();
          throw StatusProto.toStatusRuntimeException(invalidValueTypeError);
      }
    }

    Document document = (Document) documentService.findByObject(queryDocument);
    if (document != null) {
      return convertExperimentFromDocument(document);
    } else {
      String errorMessage = "Experiment not found in database";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Add List of Experiment Tags in database.
   *
   * @param String experimentId, List<String> tagsList
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  public Experiment addExperimentTags(String experimentId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    Document queryExperiment = new Document();
    queryExperiment.append(ModelDBConstants.ID, experimentId);

    Document updatedExperiment = new Document();
    updatedExperiment.append(ModelDBConstants.TAGS, new Document("$each", tagsList));

    long updatedCount =
        documentService.updateOne(queryExperiment, new Document("$addToSet", updatedExperiment));
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentId);
      return convertExperimentFromDocument(document);
    } else {
      String errorMessage = "Added tags value is already present in Experiment";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Fetch Experiment Tags from database using experimentId.
   *
   * @param String experimentId
   * @return List<String> experimentTags
   * @throws InvalidProtocolBufferException
   */
  @Override
  public List<String> getExperimentTags(String experimentId) throws InvalidProtocolBufferException {
    Document queryObj = new Document(ModelDBConstants.ID, experimentId);

    Document projectionDoc = new Document();
    projectionDoc.append(ModelDBConstants.TAGS, 1);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryObj, projectionDoc, null, null);

    if (!documentList.isEmpty()) {
      Document document = documentList.get(0);
      Experiment experiment = convertExperimentFromDocument(document);
      return experiment.getTagsList();
    } else {
      String errorMessage = "Experiment tags not found in database";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Delete Experiment Tags from database using ExperimentId.
   *
   * @param tagList
   * @param deleteAll
   * @param String experimentId
   * @return Experiment experiment
   * @throws InvalidProtocolBufferException
   */
  public Experiment deleteExperimentTags(
      String experimentId, List<String> experimentTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    Document queryExperiment = new Document();
    queryExperiment.append(ModelDBConstants.ID, experimentId);

    Document updateQueryDoc = null;
    Document updatedExperiment = new Document();
    if (deleteAll) {
      updatedExperiment.append(ModelDBConstants.TAGS, 1);
      updateQueryDoc = new Document("$unset", updatedExperiment);
    } else {
      updatedExperiment.append(ModelDBConstants.TAGS, experimentTagList);
      updateQueryDoc = new Document("$pullAll", updatedExperiment);
    }

    long updatedCount = documentService.updateOne(queryExperiment, updateQueryDoc);
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentId);
      return convertExperimentFromDocument(document);
    } else {
      String errorMessage =
          "The tags field of Experiment is already deleted Or tags not found in the Experiment";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Add List of Attribute in Experiment Attributes list in database.
   *
   * @param String experimentId
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  public Experiment addExperimentAttributes(String experimentId, List<KeyValue> attributes)
      throws InvalidProtocolBufferException {

    Document queryExperiment = new Document();
    queryExperiment.append(ModelDBConstants.ID, experimentId);

    Document updatedExperiment = new Document();

    List<Document> attributeDocList = new ArrayList<>();
    for (KeyValue attribute : attributes) {
      String json = ModelDBUtils.getStringFromProtoObject(attribute);
      Document documentAttributes = Document.parse(json);
      attributeDocList.add(documentAttributes);
    }
    updatedExperiment.append(ModelDBConstants.ATTRIBUTES, new Document("$each", attributeDocList));

    long updatedCount =
        documentService.updateOne(queryExperiment, new Document("$push", updatedExperiment));
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentId);
      return convertExperimentFromDocument(document);
    } else {
      String errorMessage =
          "Added attributes value is already present in Experiment OR Invalid Experiment ID found";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Fetch Experiment Attributes from database using experimentId.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param String experimentId
   * @return List<KeyValue> experimentAttributes.
   * @throws InvalidProtocolBufferException
   */
  public List<KeyValue> getExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.ID, experimentId);

    Document projectionDoc = new Document();
    projectionDoc.append(ModelDBConstants.ATTRIBUTES, 1);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryDoc, projectionDoc, null, null);

    if (!documentList.isEmpty()) {
      Document document = documentList.get(0);
      Experiment experiment = convertExperimentFromDocument(document);
      if (!getAll) {
        List<KeyValue> attributes = new ArrayList<>();
        experiment
            .getAttributesList()
            .forEach(
                attribute -> {
                  if (attributeKeyList.contains(attribute.getKey())) {
                    attributes.add(attribute);
                  }
                });
        return attributes;
      } else {
        return experiment.getAttributesList();
      }
    } else {
      String errorMessage = "Experiment not found in database for ID :" + experimentId;
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Delete Experiment Attributes in database using experimentId.
   *
   * @param Boolean deleteAll
   * @param List<String> attributeKeyList
   * @param String experimentId
   * @return Experiment experiment
   * @throws InvalidProtocolBufferException
   */
  @Override
  public Experiment deleteExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.ID, experimentId);

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

    long updatedCount = documentService.updateOne(queryDoc, updateQueryDoc);
    if (updatedCount > 0) {
      Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentId);
      return convertExperimentFromDocument(document);
    } else {
      String errorMessage =
          "The Attribute field of Experiment is already deleted Or tags not found in the Experiment";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  /**
   * Delete the Experiment in database using experimentId.
   *
   * <p>Add logic of Deleting ExperimentRun associated with Experiment.
   *
   * @param String experimentId, KeyValue attribute
   * @return Boolean updated status
   */
  public Boolean deleteExperiment(String experimentId) {
    documentService.deleteOne(experimentRunEntity, ModelDBConstants.EXPERIMENT_ID, experimentId);
    return documentService.deleteOne(experimentEntity, ModelDBConstants.ID, experimentId);
  }
}
