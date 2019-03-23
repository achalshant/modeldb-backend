package com.mitdbg.modeldb.experimentRun;

import java.util.ArrayList;
import java.util.LinkedList;
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
import com.mitdbg.modeldb.Artifact;
import com.mitdbg.modeldb.ExperimentRun;
import com.mitdbg.modeldb.FindExperimentRuns;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.KeyValueQuery;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.ModelDBUtils;
import com.mitdbg.modeldb.Observation;
import com.mitdbg.modeldb.SortExperimentRuns;
import com.mitdbg.modeldb.TopExperimentRunsSelector;
import com.mitdbg.modeldb.databaseServices.DocumentService;
import io.grpc.protobuf.StatusProto;

public class ExperimentRunDAOMongoImpl implements ExperimentRunDAO {

  private static final Logger LOGGER = Logger.getLogger(ExperimentRunDAOMongoImpl.class.getName());
  private String experimentRunEntity = null;
  private DocumentService documentService = null;

  public ExperimentRunDAOMongoImpl(DocumentService documentService) {
    App app = App.getInstance();
    this.experimentRunEntity = app.getExperimentRunEntity();
    this.documentService = documentService;
    documentService.checkCollectionAvailability(experimentRunEntity);
  }

  /**
   * Convert MongoDB Document object to ExperimentRun entity. Here remove MongoDB "_ID" because if
   * _id is their then both the structure MongoDB Document and ExperimentRun Entity is different and
   * direct conversion is not possible that's why "_ID" is remove.
   *
   * @param Document document : MongoDB data object.
   * @return ExperimentRun experimentRun : ProtocolBuffer Object of ExperimentRun entity.
   * @throws InvalidProtocolBufferException
   */
  private ExperimentRun convertExperimentRunFromDocument(Document document)
      throws InvalidProtocolBufferException {
    ExperimentRun.Builder builder = ExperimentRun.newBuilder();

    document.remove("_id");

    JsonFormat.parser().merge(document.toJson(), builder);
    return builder.build();
  }

  /**
   * Convert list of MongoDB document object to list of ExperimentRun entity object.
   *
   * @param List<Document> documents : list of MongoDB data object.
   * @return List<ExperimentRun> : ExperimentRun entity List
   * @throws InvalidProtocolBufferException
   */
  private List<ExperimentRun> getExperimentRunsFromDocuments(List<Document> documents)
      throws InvalidProtocolBufferException {

    List<ExperimentRun> experimentRunList = new ArrayList<>();
    for (Document document : documents) {
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      experimentRunList.add(experimentRun);
    }
    return experimentRunList;
  }

  private void checkEntityAlreadyExist(ExperimentRun experimentRun) {

    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.PROJECT_ID, experimentRun.getProjectId());
    queryDoc.append(ModelDBConstants.EXPERIMENT_ID, experimentRun.getExperimentId());
    queryDoc.append(ModelDBConstants.NAME, experimentRun.getName());
    Document existingExperimentRun = (Document) documentService.findByObject(queryDoc);

    if (existingExperimentRun != null && !existingExperimentRun.isEmpty()) {
      Status status =
          Status.newBuilder()
              .setCode(Code.ALREADY_EXISTS_VALUE)
              .setMessage("ExperimentRun already exist in database")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun insertExperimentRun(ExperimentRun experimentRun)
      throws InvalidProtocolBufferException {

    checkEntityAlreadyExist(experimentRun);

    documentService.insertOne(experimentRun);
    Document document =
        (Document) documentService.findByKey(ModelDBConstants.ID, experimentRun.getId());
    return convertExperimentRunFromDocument(document);
  }

  public Boolean deleteExperimentRun(String experimentRunId) {
    return documentService.deleteOne(experimentRunEntity, ModelDBConstants.ID, experimentRunId);
  }

  public List<ExperimentRun> getExperimentRunsFromEntity(
      String entityKey,
      String entityValue,
      Integer pageNumber,
      Integer pageLimit,
      String order,
      String sortBy)
      throws InvalidProtocolBufferException {
    List<ExperimentRun> experimentRuns = new ArrayList<>();
    List<Document> documentList =
        (List<Document>)
            documentService.findListByKey(
                entityKey, entityValue, pageNumber, pageLimit, order, sortBy);
    if (!documentList.isEmpty()) {
      experimentRuns = getExperimentRunsFromDocuments(documentList);
    }
    return experimentRuns;
  }

  public List<ExperimentRun> getExperimentRuns(String key, String value)
      throws InvalidProtocolBufferException {

    List<ExperimentRun> experimentRuns = new ArrayList<>();
    Document queryDoc = new Document();
    queryDoc.append(key, value);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryDoc, null, null, null);
    if (documentList != null && documentList.size() > 0) {
      experimentRuns = getExperimentRunsFromDocuments(documentList);
    }
    return experimentRuns;
  }

  public ExperimentRun updateExperimentRun(String experimentRunId, ExperimentRun experimentRun)
      throws InvalidProtocolBufferException {
    long updatedCount =
        documentService.updateOne(ModelDBConstants.ID, experimentRunId, experimentRun);
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Updated value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun addExperimentRunTags(String experimentRunId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    updatedExperimentRun.append(ModelDBConstants.TAGS, new Document("$each", tagsList));

    long updatedCount =
        documentService.updateOne(
            queryExperimentRun, new Document("$addToSet", updatedExperimentRun));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added tags value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public List<String> getExperimentRunTags(String experimentRunId)
      throws InvalidProtocolBufferException {
    Document queryObj = new Document(ModelDBConstants.ID, experimentRunId);

    Document projectionDoc = new Document();
    projectionDoc.append(ModelDBConstants.TAGS, 1);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryObj, projectionDoc, null, null);

    if (!documentList.isEmpty()) {
      Document document = documentList.get(0);
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      return experimentRun.getTagsList();
    } else {
      String errorMessage = "ExperimentRun tags not found in database";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun deleteExperimentRunTags(
      String experimentRunId, List<String> experimentRunTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updateQueryDoc = null;
    Document updatedExperimentRun = new Document();
    if (deleteAll) {
      updatedExperimentRun.append(ModelDBConstants.TAGS, 1);
      updateQueryDoc = new Document("$unset", updatedExperimentRun);
    } else {
      updatedExperimentRun.append(ModelDBConstants.TAGS, experimentRunTagList);
      updateQueryDoc = new Document("$pullAll", updatedExperimentRun);
    }

    long updatedCount = documentService.updateOne(queryExperimentRun, updateQueryDoc);
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage =
          "The tags field of ExperimentRun is already deleted Or tags not found in the ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public ExperimentRun addExperimentRunAttributes(
      String experimentRunId, List<KeyValue> attributesList) throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRunDoc = new Document();
    List<Document> attributeDocList = new ArrayList<>();
    for (KeyValue attribute : attributesList) {
      String json = ModelDBUtils.getStringFromProtoObject(attribute);
      Document documentAttributes = Document.parse(json);
      attributeDocList.add(documentAttributes);
    }
    updatedExperimentRunDoc.append(
        ModelDBConstants.ATTRIBUTES, new Document("$each", attributeDocList));

    long updatedCount =
        documentService.updateOne(
            queryExperimentRun, new Document("$push", updatedExperimentRunDoc));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added attributes is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public ExperimentRun deleteExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.ID, experimentRunId);

    Document updateQueryDoc = null;
    Document updatedExperimentRun = new Document();
    if (deleteAll) {
      updatedExperimentRun.append(ModelDBConstants.ATTRIBUTES, 1);
      updateQueryDoc = new Document("$unset", updatedExperimentRun);
    } else {
      updatedExperimentRun.append(
          ModelDBConstants.ATTRIBUTES,
          new Document(ModelDBConstants.KEY, new Document("$in", attributeKeyList)));
      updateQueryDoc = new Document("$pull", updatedExperimentRun);
    }

    long updatedCount = documentService.updateOne(queryDoc, updateQueryDoc);
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage =
          "The Attribute field of ExperimentRun is already deleted Or tags not found in the ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun logObservation(String experimentRunId, Observation observation)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    String json = ModelDBUtils.getStringFromProtoObject(observation);
    Document documentAttributes = Document.parse(json);
    updatedExperimentRun.append(ModelDBConstants.OBSERVATIONS, documentAttributes);

    long updatedCount =
        documentService.updateOne(queryExperimentRun, new Document("$push", updatedExperimentRun));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added observation value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public List<Observation> getObservationByKey(String experimentRunId, String observationKey)
      throws InvalidProtocolBufferException {

    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
    if (document != null) {
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      List<Observation> filteredObservation = new ArrayList<Observation>();
      List<Observation> dataObservationList = experimentRun.getObservationsList();

      dataObservationList.forEach(
          observation -> {
            if (observation.hasArtifact()
                && observation.getArtifact().getKey().equals(observationKey)) {
              filteredObservation.add(observation);
            } else if (observation.hasAttribute()
                && observation.getAttribute().getKey().equals(observationKey)) {
              filteredObservation.add(observation);
            }
          });

      return filteredObservation;
    } else {
      String errorMessage = "ExperimentRun not found for given ID : " + experimentRunId;
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun logMetric(String experimentRunId, KeyValue metric)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    String json = ModelDBUtils.getStringFromProtoObject(metric);
    Document documentAttributes = Document.parse(json);
    updatedExperimentRun.append(ModelDBConstants.METRICS, documentAttributes);

    long updatedCount =
        documentService.updateOne(queryExperimentRun, new Document("$push", updatedExperimentRun));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added metrics value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public List<KeyValue> getExperimentRunMetrics(String experimentRunId)
      throws InvalidProtocolBufferException {

    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
    if (document != null) {
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      return experimentRun.getMetricsList();
    } else {
      String errorMessage = "Metrics not found in the ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun logDataSet(String experimentRunId, Artifact dataset)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    String json = ModelDBUtils.getStringFromProtoObject(dataset);
    Document documentAttributes = Document.parse(json);
    updatedExperimentRun.append(ModelDBConstants.DATASETS, documentAttributes);

    long updatedCount =
        documentService.updateOne(queryExperimentRun, new Document("$push", updatedExperimentRun));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added datasets value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public List<Artifact> getExperimentRunDataSets(String experimentRunId)
      throws InvalidProtocolBufferException {
    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
    if (document != null) {
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      return experimentRun.getDatasetsList();
    } else {
      String errorMessage = "Datasets not found in the ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun logArtifact(String experimentRunId, Artifact artifact)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    String json = ModelDBUtils.getStringFromProtoObject(artifact);
    Document documentAttributes = Document.parse(json);
    updatedExperimentRun.append(ModelDBConstants.ARTIFACTS, documentAttributes);

    long updatedCount =
        documentService.updateOne(queryExperimentRun, new Document("$push", updatedExperimentRun));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added artifacts value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public List<Artifact> getExperimentRunArtifacts(String experimentRunId)
      throws InvalidProtocolBufferException {
    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
    if (document != null) {
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      return experimentRun.getArtifactsList();
    } else {
      String errorMessage = "Artifacts not found in the ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun logHyperparameter(String experimentRunId, KeyValue hyperparameter)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    String json = ModelDBUtils.getStringFromProtoObject(hyperparameter);
    Document documentAttributes = Document.parse(json);
    updatedExperimentRun.append(ModelDBConstants.HYPERPARAMETERS, documentAttributes);

    long updatedCount =
        documentService.updateOne(queryExperimentRun, new Document("$push", updatedExperimentRun));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added hyperparameters value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public List<KeyValue> getExperimentRunHyperparameters(String experimentRunId)
      throws InvalidProtocolBufferException {
    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
    if (document != null) {
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      return experimentRun.getHyperparametersList();
    } else {
      String errorMessage = "Hyperparameters not found in the ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public ExperimentRun logAttribute(String experimentRunId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    String json = ModelDBUtils.getStringFromProtoObject(attribute);
    Document documentAttributes = Document.parse(json);
    updatedExperimentRun.append(ModelDBConstants.ATTRIBUTES, documentAttributes);

    long updatedCount =
        documentService.updateOne(queryExperimentRun, new Document("$push", updatedExperimentRun));
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added attributes value is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  public List<KeyValue> getExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {

    Document queryDoc = new Document();
    queryDoc.append(ModelDBConstants.ID, experimentRunId);

    Document projectionDoc = new Document();
    projectionDoc.append(ModelDBConstants.ATTRIBUTES, 1);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryDoc, projectionDoc, null, null);

    if (!documentList.isEmpty()) {
      Document document = documentList.get(0);
      ExperimentRun experimentRun = convertExperimentRunFromDocument(document);
      if (!getAll) {
        List<KeyValue> attributes = new ArrayList<>();
        experimentRun
            .getAttributesList()
            .forEach(
                attribute -> {
                  if (attributeKeyList.contains(attribute.getKey())) {
                    attributes.add(attribute);
                  }
                });
        return attributes;
      } else {
        return experimentRun.getAttributesList();
      }
    } else {
      String errorMessage = "Attributes not found in the ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public List<ExperimentRun> findExperimentRuns(FindExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {

    Document queryObj = new Document();

    if (!queryParameters.getProjectId().isEmpty()) {
      queryObj.append(ModelDBConstants.PROJECT_ID, queryParameters.getProjectId());
    }
    if (!queryParameters.getExperimentId().isEmpty()) {
      queryObj.append(ModelDBConstants.EXPERIMENT_ID, queryParameters.getExperimentId());
    }
    if (!queryParameters.getExperimentRunIdsList().isEmpty()) {
      queryObj.append(
          ModelDBConstants.ID, new Document("$in", queryParameters.getExperimentRunIdsList()));
    }

    List<KeyValueQuery> predicates = queryParameters.getPredicatesList();
    if (!predicates.isEmpty()) {

      List<Document> predicateDoc = new ArrayList<>();
      for (KeyValueQuery predicate : predicates) {
        LOGGER.log(Level.INFO, "Set predicate : \n" + predicate);
        Value value = predicate.getValue();
        Document operatorDocument = null;
        switch (value.getKindCase()) {
          case NUMBER_VALUE:
            operatorDocument =
                new Document(
                    "$" + predicate.getOperator().name().toLowerCase(), value.getNumberValue());
            break;
          case STRING_VALUE:
            if (!value.getStringValue().isEmpty()) {
              operatorDocument =
                  new Document(
                      "$" + predicate.getOperator().name().toLowerCase(), value.getStringValue());
            } else {
              continue;
            }
            break;
          case BOOL_VALUE:
            operatorDocument =
                new Document(
                    "$" + predicate.getOperator().name().toLowerCase(), value.getBoolValue());
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

        String[] keyArr = predicate.getKey().split("\\.");

        if (keyArr.length <= 1) {
          Document queryDoc = new Document(keyArr[keyArr.length - 1], operatorDocument);
          predicateDoc.add(queryDoc);
        } else {
          StringBuilder stringBuilder = new StringBuilder();
          for (int i = 0; i < keyArr.length - 1; i++) {
            stringBuilder.append(keyArr[i]);
          }
          Document queryDoc =
              new Document(ModelDBConstants.VALUE, operatorDocument)
                  .append(ModelDBConstants.KEY, keyArr[keyArr.length - 1]);

          Document finalQueryDoc = new Document();
          finalQueryDoc.append(keyArr[0], new Document("$elemMatch", queryDoc));
          predicateDoc.add(finalQueryDoc);
        }
      }

      if (predicateDoc.isEmpty() || predicates.size() != predicateDoc.size()) {
        String errorMessage = "Value does not exist in predicates";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      queryObj.append("$and", predicateDoc);
    }

    Document projectionDoc = new Document();
    if (queryParameters.getIdsOnly()) {
      projectionDoc.append(ModelDBConstants.ID, 1);
    }
    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryObj, projectionDoc, null, null);
    return getExperimentRunsFromDocuments(documentList);
  }

  @Override
  public List<ExperimentRun> sortExperimentRuns(SortExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {
    Document queryObj = new Document();

    queryObj.append(
        ModelDBConstants.ID, new Document("$in", queryParameters.getExperimentRunIdsList()));

    Document projectionDoc = new Document();
    if (queryParameters.getIdsOnly()) {
      projectionDoc.append(ModelDBConstants.ID, 1);
    }

    Integer order = queryParameters.getAscending() ? 1 : -1;
    String sortBy = queryParameters.getSortKey();

    String[] keyArr = sortBy.split("\\.");
    Document sortDoc = new Document();

    if (keyArr.length <= 1) {
      sortDoc.append(keyArr[keyArr.length - 1], order);
      List<Document> documentList =
          (List<Document>) documentService.findListByObject(queryObj, projectionDoc, sortDoc, null);
      return getExperimentRunsFromDocuments(documentList);
    } else {
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < keyArr.length - 1; i++) {
        stringBuilder.append(keyArr[i]);
      }

      List<Document> aggregateDocs = new ArrayList<>();
      aggregateDocs.add(new Document("$unwind", "$" + keyArr[0]));

      queryObj.append(
          String.join(".", stringBuilder.toString(), ModelDBConstants.KEY),
          keyArr[keyArr.length - 1]);
      aggregateDocs.add(new Document("$match", queryObj));
      aggregateDocs.add(
          new Document(
              "$sort",
              new Document(
                  String.join(".", stringBuilder.toString(), ModelDBConstants.VALUE), order)));
      aggregateDocs.add(new Document("$project", new Document(ModelDBConstants.ID, 1)));

      List<Document> documentList =
          (List<Document>) documentService.findListByAggregateObject(aggregateDocs);
      List<ExperimentRun> experimentRunList = getExperimentRunsFromDocuments(documentList);

      if (queryParameters.getIdsOnly()) {
        return experimentRunList;
      }

      List<Document> sortedExperimentRunDocList = new LinkedList<>();
      experimentRunList.forEach(
          experimentRun -> {
            Document experimentRunDocument =
                (Document) documentService.findByKey(ModelDBConstants.ID, experimentRun.getId());
            sortedExperimentRunDocList.add(experimentRunDocument);
          });

      return getExperimentRunsFromDocuments(sortedExperimentRunDocList);
    }
  }

  @Override
  public List<ExperimentRun> getTopExperimentRuns(TopExperimentRunsSelector queryParameters)
      throws InvalidProtocolBufferException {
    Document queryObj = new Document();

    if (!queryParameters.getProjectId().isEmpty()) {
      queryObj.append(ModelDBConstants.PROJECT_ID, queryParameters.getProjectId());
    }

    if (!queryParameters.getExperimentId().isEmpty()) {
      queryObj.append(ModelDBConstants.EXPERIMENT_ID, queryParameters.getExperimentId());
    }

    if (!queryParameters.getExperimentRunIdsList().isEmpty()) {
      queryObj.append(
          ModelDBConstants.ID, new Document("$in", queryParameters.getExperimentRunIdsList()));
    }

    Integer order = queryParameters.getAscending() ? 1 : -1;
    String sortBy = queryParameters.getSortKey();

    String[] keyArr = sortBy.split("\\.");
    Document sortDoc = new Document();

    if (keyArr.length <= 1) {
      sortDoc.append(keyArr[keyArr.length - 1], order);
      List<Document> documentList =
          (List<Document>)
              documentService.findListByObject(queryObj, null, sortDoc, queryParameters.getTopK());
      return getExperimentRunsFromDocuments(documentList);
    } else {
      StringBuilder stringBuilder = new StringBuilder();
      for (int i = 0; i < keyArr.length - 1; i++) {
        stringBuilder.append(keyArr[i]);
      }

      List<Document> aggregateDocs = new ArrayList<>();
      aggregateDocs.add(new Document("$unwind", "$" + keyArr[0]));

      queryObj.append(
          String.join(".", stringBuilder.toString(), ModelDBConstants.KEY),
          keyArr[keyArr.length - 1]);
      aggregateDocs.add(new Document("$match", queryObj));
      aggregateDocs.add(
          new Document(
              "$sort",
              new Document(
                  String.join(".", stringBuilder.toString(), ModelDBConstants.VALUE), order)));
      aggregateDocs.add(new Document("$project", new Document(ModelDBConstants.ID, 1)));

      List<Document> documentList =
          (List<Document>) documentService.findListByAggregateObject(aggregateDocs);
      List<ExperimentRun> experimentRunList = getExperimentRunsFromDocuments(documentList);

      List<Document> sortedExperimentRunDocList = new LinkedList<>();
      for (ExperimentRun experimentRun : experimentRunList) {
        Document experimentRunDocument =
            (Document) documentService.findByKey(ModelDBConstants.ID, experimentRun.getId());
        sortedExperimentRunDocList.add(experimentRunDocument);
      }
      if (sortedExperimentRunDocList.size() > queryParameters.getTopK()) {
        sortedExperimentRunDocList =
            sortedExperimentRunDocList.subList(0, queryParameters.getTopK());
      }

      return getExperimentRunsFromDocuments(sortedExperimentRunDocList);
    }
  }

  @Override
  public ExperimentRun logJobId(String experimentRunId, String jobId)
      throws InvalidProtocolBufferException {
    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ID, experimentRunId);

    Document updatedExperimentRun = new Document();
    updatedExperimentRun.append("$set", new Document(ModelDBConstants.JOB_ID, jobId));

    long updatedCount = documentService.updateOne(queryExperimentRun, updatedExperimentRun);
    if (updatedCount > 0) {
      Document document =
          (Document) documentService.findByKey(ModelDBConstants.ID, experimentRunId);
      return convertExperimentRunFromDocument(document);
    } else {
      String errorMessage = "Added jobId is already present in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public String getJobId(String experimentRunId) throws InvalidProtocolBufferException {
    Document queryObj = new Document();
    queryObj.append(ModelDBConstants.ID, experimentRunId);

    Document projectionDoc = new Document();
    projectionDoc.append(ModelDBConstants.JOB_ID, 1);

    List<Document> documentList =
        (List<Document>) documentService.findListByObject(queryObj, projectionDoc, null, null);
    List<ExperimentRun> experimentRuns = getExperimentRunsFromDocuments(documentList);
    ExperimentRun experimentRun = experimentRuns.get(0);
    if (experimentRun != null && !experimentRun.getJobId().isEmpty()) {
      return experimentRun.getJobId();
    } else {
      String errorMessage = "JobId does not exist in ExperimentRun";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }
}
