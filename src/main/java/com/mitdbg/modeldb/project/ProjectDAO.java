package com.mitdbg.modeldb.project;

import java.util.List;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.Project;

public interface ProjectDAO {

  /**
   * Insert Project entity in database.
   *
   * @param ProjectEntity project
   * @return void
   */
  Project insertProject(Project project) throws InvalidProtocolBufferException;

  /**
   * Update Project entity in database using projectId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String projectId, Project project
   * @return Project updated Project entity
   */
  Project updateProject(String projectId, Project project) throws InvalidProtocolBufferException;

  /**
   * Update Project Attributes in database using projectId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String projectId, KeyValue attribute
   * @return Project updated Project entity
   */
  Project updateProjectAttributes(String projectId, KeyValue attribute)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Project Attributes from database using projectId.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param String projectId
   * @return List<KeyValue> projectAttributes.
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete the Project in database using projectId.
   *
   * <p>TODO : Add logic of Deleting Experiment & ExperimentRun associated with project.
   *
   * @param String projectId
   * @return Boolean updated status
   */
  Boolean deleteProject(String projectId);

  /**
   * Fetch All the Project from database bases on user details.
   *
   * @return List<Project> projects.
   * @throws InvalidProtocolBufferException
   */
  List<Project> getProjects() throws InvalidProtocolBufferException;

  /**
   * Update Project Tags in database using projectId.
   *
   * @param String projectId, List<String> tagsList
   * @return Project updated Project entity
   * @throws InvalidProtocolBufferException
   */
  Project addProjectTags(String projectId, List<String> tagsList)
      throws InvalidProtocolBufferException;

  /**
   * Delete Project Tags in database using projectId.
   *
   * @param projectTagList
   * @param deleteAll
   * @param String projectId
   * @return Project project
   * @throws InvalidProtocolBufferException
   */
  Project deleteProjectTags(String projectId, List<String> projectTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Fetch the Projects based on key and value from database.
   *
   * @param key --> key like ModelDBConstants.ID,ModelDBConstants.NAME etc.
   * @param value -- > value is project.Id, project.name etc.
   * @return Project project --> based on search return project entity.
   * @throws InvalidProtocolBufferException
   */
  List<Project> getProjects(String key, String value) throws InvalidProtocolBufferException;

  List<Project> getProjectByIds(List<String> sharedProjectIds)
      throws InvalidProtocolBufferException;

  /**
   * Add attributes in database using projectId.
   *
   * @param String projectId
   * @param List<KeyValue> attributesList
   * @return Project --> updated Project entity
   */
  Project addProjectAttributes(String projectId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException;

  /**
   * Delete Project Attributes in database using projectId.
   *
   * @param deleteAll
   * @param attributeKeyList
   * @param String projectId
   * @return Project project
   * @throws InvalidProtocolBufferException
   */
  Project deleteProjectAttributes(
      String projectId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Project Tags from database using projectId.
   *
   * @param String projectId
   * @return List<String> projectTags.
   * @throws InvalidProtocolBufferException
   */
  List<String> getProjectTags(String projectId) throws InvalidProtocolBufferException;
}
