package com.mitdbg.modeldb.experimentRun;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.App;
import com.mitdbg.modeldb.Artifact;
import com.mitdbg.modeldb.ExperimentRun;
import com.mitdbg.modeldb.FindExperimentRuns;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.ModelDBHibernateUtil;
import com.mitdbg.modeldb.Observation;
import com.mitdbg.modeldb.RdbmsUtils;
import com.mitdbg.modeldb.SortExperimentRuns;
import com.mitdbg.modeldb.TopExperimentRunsSelector;
import com.mitdbg.modeldb.entities.ExperimentEntity;
import com.mitdbg.modeldb.entities.ExperimentRunEntity;
import com.mitdbg.modeldb.entities.KeyValueEntity;
import com.mitdbg.modeldb.entities.ProjectEntity;
import io.grpc.protobuf.StatusProto;

public class ExperimentRunDAORdbImpl implements ExperimentRunDAO {

  private static final Logger LOGGER = Logger.getLogger(ExperimentRunDAORdbImpl.class.getName());
  private String experimentRunEntity = null;

  public ExperimentRunDAORdbImpl() {
    App app = App.getInstance();
    this.experimentRunEntity = app.getExperimentRunEntity();
  }

  @Transactional
  public void checkIfEntityAlreadyExists(ExperimentRun experimentRun, Boolean isInsert) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      StringBuilder stringQueryBuilder = new StringBuilder("From ExperimentRunEntity p where ");
      stringQueryBuilder.append("p." + ModelDBConstants.NAME + " = :experimentRunName ");

      Query query = session.createQuery(stringQueryBuilder.toString());
      query.setParameter("experimentRunName", experimentRun.getName());
      Boolean existStatus = (query.uniqueResult() != null);
      transaction.commit();

      // Throw error if it is an insert request and ExperimentRun with same name already exists
      if (existStatus && isInsert) {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("ExperimentRun already exists in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (!existStatus && !isInsert) {
        // Throw error if it is an update request and ExperimentRun with given name does not exist
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("ExperimentRun does not exist in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }

  @Override
  @Transactional
  public ExperimentRun insertExperimentRun(ExperimentRun experimentRun)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      checkIfEntityAlreadyExists(experimentRun, true);
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectEntity = session.get(ProjectEntity.class, experimentRun.getProjectId());
      ExperimentEntity experimentEntity =
          session.get(ExperimentEntity.class, experimentRun.getExperimentId());
      ExperimentRunEntity experimentRunObj =
          RdbmsUtils.convertFromExperimentRunToExperimentRunEntity(
              projectEntity, experimentEntity, experimentRun);
      session.saveOrUpdate(experimentRunObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "ExperimentRun created successfully");
      return experimentRun;
    }
  }

  @Override
  @Transactional
  public Boolean deleteExperimentRun(String experimentRunId) {
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
  public List<ExperimentRun> getExperimentRunsFromEntity(
      String entityKey,
      String entityValue,
      Integer pageNumber,
      Integer pageLimit,
      String order,
      String sortBy)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      StringBuilder stringQueryBuilder = new StringBuilder("FROM ExperimentRunEntity ee ");
      stringQueryBuilder.append(" WHERE ee." + entityKey + " = :entityValue ");

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
      query.setParameter("entityValue", entityValue);
      List<ExperimentRunEntity> experimentRUnEntities = query.list();
      transaction.commit();
      LOGGER.log(
          Level.INFO,
          "ExperimentRun getting successfully, list size : {0}",
          experimentRUnEntities.size());
      return RdbmsUtils.convertFromExperimentRunsToExperimentRunEntityList(experimentRUnEntities);
    }
  }

  @Override
  @Transactional
  public List<ExperimentRun> getExperimentRuns(String key, String value)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun updateExperimentRun(String experimentRunId, ExperimentRun experimentRun)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun addExperimentRunTags(String experimentRunId, List<String> tagsList)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun deleteExperimentRunTags(
      String experimentRunId, List<String> experimentRunTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun logObservation(String experimentRunId, Observation observation)
      throws InvalidProtocolBufferException {
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
  public List<Observation> getObservationByKey(String experimentRunId, String observationKey)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun logMetric(String experimentRunId, KeyValue metric)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      KeyValueEntity newMetrics =
          RdbmsUtils.convertFromKeyValueToKeyValueEntity(
              experimentRunEntityObj, ModelDBConstants.METRICS, metric);
      experimentRunEntityObj.getMetrics().add(newMetrics);
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public List<KeyValue> getExperimentRunMetrics(String experimentRunId)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun logDataSet(String experimentRunId, Artifact dataset)
      throws InvalidProtocolBufferException {
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
  public List<Artifact> getExperimentRunDataSets(String experimentRunId)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun logArtifact(String experimentRunId, Artifact artifact)
      throws InvalidProtocolBufferException {
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
  public List<Artifact> getExperimentRunArtifacts(String experimentRunId)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun logHyperparameter(String experimentRunId, KeyValue hyperparameter)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      KeyValueEntity newHyperparameter =
          RdbmsUtils.convertFromKeyValueToKeyValueEntity(
              experimentRunEntityObj, ModelDBConstants.HYPERPARAMETERS, hyperparameter);

      experimentRunEntityObj.getHyperparameters().add(newHyperparameter);
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public List<KeyValue> getExperimentRunHyperparameters(String experimentRunId)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun logAttribute(String experimentRunId, KeyValue attribute)
      throws InvalidProtocolBufferException {
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
  public List<KeyValue> getExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
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
  public List<ExperimentRun> findExperimentRuns(FindExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {
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
  public List<ExperimentRun> sortExperimentRuns(SortExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {
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
  public List<ExperimentRun> getTopExperimentRuns(TopExperimentRunsSelector queryParameters)
      throws InvalidProtocolBufferException {
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
  public List<String> getExperimentRunTags(String experimentRunId)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun addExperimentRunAttributes(
      String experimentRunId, List<KeyValue> attributesList) throws InvalidProtocolBufferException {
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
  public ExperimentRun deleteExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
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
  public ExperimentRun logJobId(String experimentRunId, String jobId)
      throws InvalidProtocolBufferException {
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
  public String getJobId(String experimentRunId) throws InvalidProtocolBufferException {
    // TODO Auto-generated method stub
    Status status =
        Status.newBuilder()
            .setCode(Code.UNIMPLEMENTED_VALUE)
            .setMessage("this method is under development")
            .build();
    throw StatusProto.toStatusRuntimeException(status);
  }
}
