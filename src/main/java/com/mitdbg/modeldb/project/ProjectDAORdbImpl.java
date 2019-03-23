package com.mitdbg.modeldb.project;

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
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.ModelDBHibernateUtil;
import com.mitdbg.modeldb.Project;
import com.mitdbg.modeldb.RdbmsUtils;
import com.mitdbg.modeldb.entities.KeyValueEntity;
import com.mitdbg.modeldb.entities.ProjectEntity;
import com.mitdbg.modeldb.entities.TagsMapping;
import io.grpc.protobuf.StatusProto;

public class ProjectDAORdbImpl implements ProjectDAO {

  private static final Logger LOGGER = Logger.getLogger(ProjectDAORdbImpl.class.getName());

  @Transactional
  public void checkIfEntityAlreadyExists(Project project, Boolean isInsert) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      StringBuilder stringQueryBuilder = new StringBuilder("From ProjectEntity p where ");
      if (isInsert) {
        stringQueryBuilder.append(" p." + ModelDBConstants.NAME + " = :projectName ");
      } else {
        stringQueryBuilder.append(" p." + ModelDBConstants.ID + " = :projectId ");
      }

      Query query = session.createQuery(stringQueryBuilder.toString());
      if (isInsert) {
        query.setParameter("projectName", project.getName());
      } else {
        query.setParameter("projectId", project.getId());
      }
      Boolean existStatus = (query.uniqueResult() != null);
      transaction.commit();

      // Throw error if it is an insert request and project with same name already exists
      if (existStatus && isInsert) {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("Project already exists in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (!existStatus && !isInsert) {
        // Throw error if it is an update request and project with given name does not exist
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Project does not exist in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }

  private ProjectEntity getUpdatedProjectEntity(
      ProjectEntity existingProject, ProjectEntity newProject) {
    if (!newProject.getName().isEmpty()) {
      existingProject.setName(newProject.getName());
    }
    if (!newProject.getDescription().isEmpty()) {
      existingProject.setDescription(newProject.getDescription());
    }

    return existingProject;
  }

  @Override
  @Transactional
  public Project insertProject(Project project) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      checkIfEntityAlreadyExists(project, true);
      Transaction transaction = session.beginTransaction();
      session.save(RdbmsUtils.convertFromProjectToProjectEntity(project));
      transaction.commit();
      LOGGER.log(Level.INFO, "Project created successfully");
      return project;
    }
  }

  @Override
  @Transactional
  public Project updateProject(String projectId, Project project)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      checkIfEntityAlreadyExists(project, false);
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.load(ProjectEntity.class, projectId);
      projectObj =
          getUpdatedProjectEntity(
              projectObj, RdbmsUtils.convertFromProjectToProjectEntity(project));
      session.update(projectObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "Project updated successfully");
      return projectObj.getProtoObject();
    }
  }

  @Override
  public Project updateProjectAttributes(String projectId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);

      KeyValueEntity updatedKeyValueObj =
          RdbmsUtils.convertFromKeyValueToKeyValueEntity(
              projectObj, ModelDBConstants.ATTRIBUTES, attribute);

      List<KeyValueEntity> existingKeyValues = projectObj.getAttributes();
      if (!existingKeyValues.isEmpty()) {
        for (int index = 0; index < existingKeyValues.size(); index++) {
          KeyValueEntity existingKeyValue = existingKeyValues.get(index);
          if (existingKeyValue.getKey().equals(attribute.getKey())) {
            existingKeyValue.setKey(updatedKeyValueObj.getKey());
            existingKeyValue.setValue(updatedKeyValueObj.getValue());
            existingKeyValue.setValue_type(updatedKeyValueObj.getValue_type());
            break;
          }
        }
      } else {
        projectObj.getAttributes().add(updatedKeyValueObj);
      }
      session.saveOrUpdate(projectObj);
      transaction.commit();
      return projectObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public List<KeyValue> getProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      if (getAll) {
        ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
        transaction.commit();
        return projectObj.getProtoObject().getAttributesList();
      } else {
        StringBuilder stringQueryBuilder = new StringBuilder("From KeyValueEntity kv where ");
        stringQueryBuilder.append("kv." + ModelDBConstants.KEY + " in (:keys) AND ");
        stringQueryBuilder.append("kv.projectEntity." + ModelDBConstants.ID + " = :projectId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameterList("keys", attributeKeyList);
        query.setParameter("projectId", projectId);
        List<KeyValueEntity> keyValueEntities = query.list();
        transaction.commit();
        return RdbmsUtils.convertFromKeyValueEntityListToKeyValues(keyValueEntities);
      }
    }
  }

  @SuppressWarnings({"unchecked"})
  @Override
  @Transactional
  public List<Project> getProjects() throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      List<ProjectEntity> projectEntities = session.createQuery("FROM ProjectEntity").list();
      transaction.commit();
      LOGGER.log(Level.INFO, "Project getting successfully");
      return RdbmsUtils.convertFromProjectsToProjectEntityList(projectEntities);
    }
  }

  @Override
  @Transactional
  public Project addProjectTags(String projectId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      List<TagsMapping> newTagMappings =
          RdbmsUtils.convertFromTagListToTagMappingList(projectObj, tagsList);
      projectObj.getTags().addAll(newTagMappings);
      session.saveOrUpdate(projectObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "Project tags added successfully");
      return projectObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public Project deleteProjectTags(String projectId, List<String> projectTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      StringBuilder stringQueryBuilder = new StringBuilder("delete from TagsMapping tm WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(" tm.projectEntity." + ModelDBConstants.ID + " = :projectId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("projectId", projectId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" tm." + ModelDBConstants.TAGS + " in (:tags)");
        stringQueryBuilder.append(" AND tm.projectEntity." + ModelDBConstants.ID + " = :projectId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("tags", projectTagList);
        query.setParameter("projectId", projectId);
        query.executeUpdate();
      }

      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      transaction.commit();
      LOGGER.log(Level.INFO, "Project tags deleted successfully");
      return projectObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public List<Project> getProjects(String key, String value) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      StringBuilder stringQueryBuilder = new StringBuilder("From ProjectEntity p where ");
      stringQueryBuilder.append(" p." + key + " = :value ");

      Query query = session.createQuery(stringQueryBuilder.toString());
      query.setParameter("value", value);
      List<ProjectEntity> projectEntities = query.list();
      transaction.commit();
      return RdbmsUtils.convertFromProjectsToProjectEntityList(projectEntities);
    }
  }

  @Override
  @Transactional
  public List<Project> getProjectByIds(List<String> sharedProjectIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      String queryString = "From ProjectEntity p where p.id in (:ids)";
      Query query = session.createQuery(queryString);
      query.setParameterList("ids", sharedProjectIds);
      List<ProjectEntity> projectEntities = query.list();
      transaction.commit();
      LOGGER.log(Level.INFO, "Project by Ids getting successfully");
      return RdbmsUtils.convertFromProjectsToProjectEntityList(projectEntities);
    }
  }

  @Override
  @Transactional
  public Project addProjectAttributes(String projectId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      projectObj
          .getAttributes()
          .addAll(
              RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
                  projectObj, ModelDBConstants.ATTRIBUTES, attributesList));
      session.saveOrUpdate(projectObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "Project attributes added successfully");
      return projectObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public Project deleteProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      StringBuilder stringQueryBuilder = new StringBuilder("delete from KeyValueEntity kv WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(" kv.projectEntity." + ModelDBConstants.ID + " = :projectId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("projectId", projectId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" kv." + ModelDBConstants.KEY + " in (:keys)");
        stringQueryBuilder.append(" AND kv.projectEntity." + ModelDBConstants.ID + " = :projectId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("keys", attributeKeyList);
        query.setParameter("projectId", projectId);
        query.executeUpdate();
      }
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      transaction.commit();
      return projectObj.getProtoObject();
    }
  }

  @Override
  @Transactional
  public List<String> getProjectTags(String projectId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.get(ProjectEntity.class, projectId);
      transaction.commit();
      return projectObj.getProtoObject().getTagsList();
    }
  }

  @Override
  @Transactional
  public Boolean deleteProject(String projectId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ProjectEntity projectObj = session.load(ProjectEntity.class, projectId);
      // Delete the object
      session.delete(projectObj);
      transaction.commit();
      LOGGER.log(Level.INFO, "Project deleted successfully");
      return true;
    }
  }
}
