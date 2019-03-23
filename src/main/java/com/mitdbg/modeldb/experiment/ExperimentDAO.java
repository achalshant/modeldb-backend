package com.mitdbg.modeldb.experiment;

import java.util.List;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.Experiment;
import com.mitdbg.modeldb.KeyValue;

public interface ExperimentDAO {

  /**
   * Insert Experiment entity in database.
   *
   * @param Experiment experiment
   * @return Experiment insertedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment insertExperiment(Experiment experiment) throws InvalidProtocolBufferException;

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
  Experiment updateExperiment(String experimentId, Experiment experiment)
      throws InvalidProtocolBufferException;

  /**
   * Get Experiment entity using given experimentID from database.
   *
   * @param String experimentId
   * @return Experiment experiment
   * @throws InvalidProtocolBufferException
   */
  Experiment getExperiment(String experimentId) throws InvalidProtocolBufferException;

  /**
   * Get List of Experiment entity using given projectId from database.
   *
   * @param sortBy -- > Use this field for filter data.
   * @param order --> this parameter has order like asc OR desc.
   * @param String experimentId
   * @return List<Experiment> experimentList
   * @throws InvalidProtocolBufferException
   */
  List<Experiment> getExperimentsInProject(
      String projectId, Integer pageNumber, Integer pageLimit, String order, String sortBy)
      throws InvalidProtocolBufferException;

  /**
   * Add List of Experiment Tags in database.
   *
   * @param String experimentId, List<String> tagsList
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment addExperimentTags(String experimentId, List<String> tagsList)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Experiment Tags from database using experimentId.
   *
   * @param String experimentId
   * @return List<String> projectTags
   * @throws InvalidProtocolBufferException
   */
  List<String> getExperimentTags(String experimentId) throws InvalidProtocolBufferException;

  /**
   * Delete Experiment Tags from Experiment entity.
   *
   * @param tagList
   * @param deleteAll
   * @param String experimentId
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment deleteExperimentTags(
      String experimentId, List<String> experimentTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Add Attribute in Experiment Attributes list in database.
   *
   * @param String experimentId
   * @param List<KeyValue> attributes
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment addExperimentAttributes(String experimentId, List<KeyValue> attributes)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Experiment Attributes from database using experimentId.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param String experimentId
   * @return List<KeyValue> experimentAttributes.
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete Experiment Attributes in database using experimentId.
   *
   * @param deleteAll
   * @param attributeKeyList
   * @param String experimentId
   * @return Experiment experiment
   * @throws InvalidProtocolBufferException
   */
  Experiment deleteExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete the Experiment from database using experimentId.
   *
   * <p>TODO : Add logic of Deleting ExperimentRun associated with Experiment.
   *
   * @param String experimentId
   * @return Boolean updated status
   */
  Boolean deleteExperiment(String experimentId);

  /**
   * Return experiment using given key value list. keyValue has key as ModelDBConstants.PROJECT_ID
   * etc. and value as experiment.projectId
   *
   * @param keyValue --> list of KeyValue
   * @return Experiment entity.
   * @throws InvalidProtocolBufferException
   */
  Experiment getExperiment(List<KeyValue> keyValues) throws InvalidProtocolBufferException;
}
