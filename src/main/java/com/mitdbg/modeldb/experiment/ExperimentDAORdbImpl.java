package com.mitdbg.modeldb.experiment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.Experiment;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.ModelDBHibernateUtil;
import com.mitdbg.modeldb.RdbmsUtils;
import com.mitdbg.modeldb.entities.ExperimentEntity;
import com.mitdbg.modeldb.entities.KeyValueEntity;
import com.mitdbg.modeldb.entities.ProjectEntity;
import com.mitdbg.modeldb.entities.TagsMapping;
import io.grpc.protobuf.StatusProto;

public class ExperimentDAORdbImpl implements ExperimentDAO {

  private static final Logger LOGGER = Logger.getLogger(ExperimentDAORdbImpl.class.getName());

  @Transactional
  public void checkIfEntityAlreadyExists(Experiment experiment, Boolean isInsert) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      StringBuilder stringQueryBuilder = new StringBuilder("From ExperimentEntity ee where ");

      if (isInsert) {
        stringQueryBuilder.append("ee." + ModelDBConstants.NAME + " = :experimentName ");
        stringQueryBuilder.append(
            " AND ee.projectEntity." + ModelDBConstants.ID + " = :projectId ");
      } else {
        stringQueryBuilder.append(" ee." + ModelDBConstants.ID + " = :experimentId ");
      }

      Query query = session.createQuery(stringQueryBuilder.toString());
      if (isInsert) {
        query.setParameter("experimentName", experiment.getName());
        query.setParameter("projectId", experiment.getProjectId());
      } else {
        query.setParameter("experimentId", experiment.getId());
      }

      Boolean existStatus = (query.uniqueResult() != null);
      transaction.commit();

      // Throw error if it is an insert request and Experiment with same name already exists
      if (existStatus && isInsert) {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("Experiment already exists in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (!existStatus && !isInsert) {
        // Throw error if it is an update request and Experiment with given name does not exist
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Experiment does not exist in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }

  private ExperimentEntity getUpdatedExperimentEntity(
      ExperimentEntity existingExperimentEntity, ExperimentEntity newExperimentEntity) {
    if (!newExperimentEntity.getName().isEmpty()) {
      existingExperimentEntity.setName(newExperimentEntity.getName());
    }
    if (!newExperimentEntity.getDescription().isEmpty()) {
      existingExperimentEntity.setDescription(newExperimentEntity.getDescription());
    }

    return existingExperimentEntity;
  }

  @Override
  @Transactional
  public Experiment insertExperiment(Experiment experiment) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      checkIfEntityAlreadyExists(experiment, true);
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectEntity = session.get(ProjectEntity.class, experiment.getProjectId());
      session.save(RdbmsUtils.convertFromExperimentToExperimentEntity(projectEntity, experiment));
      transaction.commit();
      LOGGER.log(Level.INFO, "Experiment created successfully");
      return experiment;
    }
  }

  @Override
  @Transactional
  public Experiment updateExperiment(String experimentId, Experiment experiment)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      checkIfEntityAlreadyExists(experiment, false);
      Transaction transaction = session.beginTransaction();
      ExperimentEntity experimentObj = session.load(ExperimentEntity.class, experimentId);
      experimentObj =
          getUpdatedExperimentEntity(
              experimentObj,
              RdbmsUtils.convertFromExperimentToExperimentEntity(
                  experimentObj.getProjectEntity(), experiment));
      session.update(experimentObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "Experiment updated successfully");
      return experimentObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public Experiment getExperiment(String experimentId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      transaction.commit();
      LOGGER.log(Level.INFO, "Experiment getting successfully");
      if (experimentObj != null) {
        return experimentObj.getProtoObject();
      } else {
        String errorMessage = "Experiment not found for given ID : " + experimentId;
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }

  @Override
  @Transactional
  public List<Experiment> getExperimentsInProject(
      String projectId, Integer pageNumber, Integer pageLimit, String order, String sortBy)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      StringBuilder stringQueryBuilder = new StringBuilder("FROM ExperimentEntity ee ");
      stringQueryBuilder.append(
          " WHERE ee.projectEntity." + ModelDBConstants.ID + " = :projectId ");

      order = (order == null || order.isEmpty()) ? ModelDBConstants.ORDER_DESC : order;
      sortBy = (sortBy == null || sortBy.isEmpty()) ? ModelDBConstants.DATE_CREATED : sortBy;

      if (order.equalsIgnoreCase(ModelDBConstants.ORDER_ASC)) {
        stringQueryBuilder.append(" ORDER BY ee." + sortBy + " " + ModelDBConstants.ORDER_ASC);
      } else {
        stringQueryBuilder.append(" ORDER BY ee." + sortBy + " " + ModelDBConstants.ORDER_DESC);
      }
      Query query = session.createQuery(stringQueryBuilder.toString());
      if (pageNumber != null && pageLimit != null && pageNumber != 0 && pageLimit != 0) {
        // Calculate number of documents to skip
        Integer skips = pageLimit * (pageNumber - 1);
        query.setFirstResult(skips);
        query.setMaxResults(pageLimit);
      }
      query.setParameter("projectId", projectId);
      List<ExperimentEntity> experimentEntities = query.list();
      transaction.commit();
      LOGGER.log(
          Level.INFO,
          "Experiment getting successfully, list size : {0}",
          experimentEntities.size());
      return RdbmsUtils.convertFromExperimentsToExperimentEntityList(experimentEntities);
    }
  }

  @Override
  @Transactional
  public Experiment addExperimentTags(String experimentId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      List<TagsMapping> newTagMappings =
          RdbmsUtils.convertFromTagListToTagMappingList(experimentObj, tagsList);
      experimentObj.getTags().addAll(newTagMappings);
      session.saveOrUpdate(experimentObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "Experiment tags added successfully");
      return experimentObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public List<String> getExperimentTags(String experimentId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      transaction.commit();
      LOGGER.log(Level.INFO, "Experiment getting successfully");
      return experimentObj.getProtoObject().getTagsList();
    }
  }

  @Override
  @Transactional
  public Experiment deleteExperimentTags(
      String experimentId, List<String> experimentTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      StringBuilder stringQueryBuilder = new StringBuilder("delete from TagsMapping tm WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(
            " tm.experimentEntity." + ModelDBConstants.ID + " = :experimentId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("experimentId", experimentId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" tm." + ModelDBConstants.TAGS + " in (:tags)");
        stringQueryBuilder.append(
            " AND tm.experimentEntity." + ModelDBConstants.ID + " = :experimentId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("tags", experimentTagList);
        query.setParameter("experimentId", experimentId);
        query.executeUpdate();
      }
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      transaction.commit();
      LOGGER.log(Level.INFO, "Experiment tags deleted successfully");
      return experimentObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public Experiment addExperimentAttributes(String experimentId, List<KeyValue> attributes)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      if (experimentObj == null) {
        String errorMessage = "Invalid Experiment ID found";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      if (experimentObj.getAttributes() != null) {
        experimentObj
            .getAttributes()
            .addAll(
                RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
                    experimentObj, ModelDBConstants.ATTRIBUTES, attributes));
      } else {
        experimentObj.setAttributes(
            RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
                experimentObj, ModelDBConstants.ATTRIBUTES, attributes));
      }
      session.saveOrUpdate(experimentObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "Experiment attributes added successfully");
      return experimentObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public List<KeyValue> getExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      if (experimentObj == null) {
        String errorMessage = "Invalid Experiment ID found";
        LOGGER.log(Level.WARNING, errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      if (getAll) {
        transaction.commit();
        return experimentObj.getProtoObject().getAttributesList();
      } else {
        StringBuilder stringQueryBuilder = new StringBuilder("From KeyValueEntity kv where ");
        stringQueryBuilder.append("kv." + ModelDBConstants.KEY + " in (:keys) AND ");
        stringQueryBuilder.append(
            "kv.experimentEntity." + ModelDBConstants.ID + " = :experimentId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameterList("keys", attributeKeyList);
        query.setParameter("experimentId", experimentId);
        List<KeyValueEntity> keyValueEntities = query.list();
        transaction.commit();
        return RdbmsUtils.convertFromKeyValueEntityListToKeyValues(keyValueEntities);
      }
    }
  }

  @Override
  @Transactional
  public Experiment deleteExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      StringBuilder stringQueryBuilder = new StringBuilder("delete from KeyValueEntity kv WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(
            " kv.experimentEntity." + ModelDBConstants.ID + " = :experimentId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("experimentId", experimentId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" kv." + ModelDBConstants.KEY + " in (:keys)");
        stringQueryBuilder.append(
            " AND kv.experimentEntity." + ModelDBConstants.ID + " = :experimentId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("keys", attributeKeyList);
        query.setParameter("experimentId", experimentId);
        query.executeUpdate();
      }
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      transaction.commit();
      return experimentObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public Boolean deleteExperiment(String experimentId) {
    // TODO Auto-generated method stub
    Status status =
        Status.newBuilder()
            .setCode(Code.UNIMPLEMENTED_VALUE)
            .setMessage("this method is under development")
            .build();
    throw StatusProto.toStatusRuntimeException(status);
  }

  @Override
  @Transactional
  public Experiment getExperiment(List<KeyValue> keyValues) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      StringBuilder stringQueryBuilder = new StringBuilder("From ExperimentEntity ee where ");
      Map<String, Object> paramMap = new HashMap<>();
      for (int index = 0; index < keyValues.size(); index++) {
        KeyValue keyValue = keyValues.get(index);
        Value value = keyValue.getValue();
        String key = keyValue.getKey();

        switch (value.getKindCase()) {
          case NUMBER_VALUE:
            paramMap.put(key, value.getNumberValue());
            break;
          case STRING_VALUE:
            paramMap.put(key, value.getStringValue());
            break;
          case BOOL_VALUE:
            paramMap.put(key, value.getBoolValue());
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
        if (key.equals(ModelDBConstants.PROJECT_ID)) {
          String updatedKey = "projectEntity." + ModelDBConstants.ID;
          stringQueryBuilder.append(" ee." + updatedKey + " = :" + key);
        } else {
          stringQueryBuilder.append(" ee." + key + " = :" + key);
        }
        if (index < keyValues.size() - 1) {
          stringQueryBuilder.append(" AND ");
        }
      }
      Query query = session.createQuery(stringQueryBuilder.toString());
      for (Entry<String, Object> paramEntry : paramMap.entrySet()) {
        query.setParameter(paramEntry.getKey(), paramEntry.getValue());
      }
      ExperimentEntity experimentObj = (ExperimentEntity) query.uniqueResult();
      transaction.commit();
      return experimentObj.getProtoObject();
    }
  }
}
