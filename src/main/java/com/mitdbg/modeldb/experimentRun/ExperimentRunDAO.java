package com.mitdbg.modeldb.experimentRun;

import java.util.List;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.Artifact;
import com.mitdbg.modeldb.ExperimentRun;
import com.mitdbg.modeldb.FindExperimentRuns;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.Observation;
import com.mitdbg.modeldb.SortExperimentRuns;
import com.mitdbg.modeldb.TopExperimentRunsSelector;

public interface ExperimentRunDAO {

  /**
   * Insert ExperimentRun entity in database.
   *
   * @param ExperimentRun experimentRun
   * @return ExperimentRun insertedExperimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun insertExperimentRun(ExperimentRun experimentRun)
      throws InvalidProtocolBufferException;

  /**
   * Delete the ExperimentRun from database using experimentRunId.
   *
   * @param String experimentRunId
   * @return Boolean updated status
   */
  Boolean deleteExperimentRun(String experimentRunId);

  /**
   * Get List of ExperimentRun entity using given projectId from database.
   *
   * @param pageNumber --> page number use for pagination.
   * @param pageLimit --> page limit is per page record count.
   * @param sortBy -- > Use this field for filter data.
   * @param order --> this parameter has order like asc OR desc.
   * @param String entityKey --> like ModelDBConstants.PROJECT_ID, ModelDBConstants.EXPERIMENT_ID
   *     etc.
   * @param String entityValue --> like Project.id, experiment.id etc.
   * @return List<Experiment> experimentRunList
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> getExperimentRunsFromEntity(
      String entityKey,
      String entityValue,
      Integer pageNumber,
      Integer pageLimit,
      String order,
      String sortBy)
      throws InvalidProtocolBufferException;

  /**
   * Get ExperimentRun entity using given experimentRunId from database.
   *
   * @param String key --> key like ModelDBConstants.ID, ModelDBConstants.Name etc.
   * @param String value --> value like ExperimentRun.id, ExperimentRun.name etc.
   * @return List<ExperimentRun> experimentRuns
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> getExperimentRuns(String key, String value)
      throws InvalidProtocolBufferException;

  /**
   * Update ExperimentRun entity in database using experimentRunId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String experimentRunId, ExperimentRun experimentRun
   * @return ExperimentRun updatedExperimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun updateExperimentRun(String experimentRunId, ExperimentRun experimentRun)
      throws InvalidProtocolBufferException;

  /**
   * Add List of ExperimentRun Tags in database.
   *
   * @param String experimentRunId, List<String> tagsList
   * @return ExperimentRun updatedExperimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun addExperimentRunTags(String experimentRunId, List<String> tagsList)
      throws InvalidProtocolBufferException;

  /**
   * Delete ExperimentRun Tags from ExperimentRun entity.
   *
   * @param deleteAll
   * @param List<String> experimentRunTagList
   * @param String experimentRunId
   * @return ExperimentRun updatedExperimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun deleteExperimentRunTags(
      String experimentRunId, List<String> experimentRunTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has Observations list field. Add new Observation in that Observations List.
   *
   * @param experimentRunId
   * @param observation
   * @return ExperimentRun updated experimentRun entity
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun logObservation(String experimentRunId, Observation observation)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Observation> using @param observationKey from Observation field in ExperimentRun.
   *
   * @param experimentRunId
   * @param observationKey
   * @return List<Observation> observation list
   * @throws InvalidProtocolBufferException
   */
  List<Observation> getObservationByKey(String experimentRunId, String observationKey)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has Metrics list field. Add new metric in that Metrics List.
   *
   * @param experimentRunId
   * @param metric has KeyValue entity
   * @return ExperimentRun updated ExperimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun logMetric(String experimentRunId, KeyValue metric)
      throws InvalidProtocolBufferException;

  /**
   * Return List<KeyValue> metrics from ExperimentRun.
   *
   * @param experimentRunId
   * @return List<KeyValue> metric list
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentRunMetrics(String experimentRunId)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has datasets field. Add new dataset in that dataset List.
   *
   * @param experimentRunId
   * @param dataset has Artifact
   * @return ExperimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun logDataSet(String experimentRunId, Artifact dataset)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Artifact> dataset from ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<Artifact> dataset list from experimentRun
   * @throws InvalidProtocolBufferException
   */
  List<Artifact> getExperimentRunDataSets(String experimentRunId)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has artifacts field. Add new Artifact in that artifacts List.
   *
   * @param experimentRunId
   * @param artifact
   * @return ExperimentRun updated experimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun logArtifact(String experimentRunId, Artifact artifact)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Artifact> artifacts from ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<Artifact> artifact list from experimentRun
   * @throws InvalidProtocolBufferException
   */
  List<Artifact> getExperimentRunArtifacts(String experimentRunId)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has hyperparameters field. Add new hyperparameter in that hyperparameter List.
   *
   * @param experimentRunId
   * @param hyperparameter has KeyValue.
   * @return ExperimentRun updated experimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun logHyperparameter(String experimentRunId, KeyValue hyperparameter)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has hyperparameters field, Return List<KeyValue> hyperparameters from
   * ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<KeyValue> hyperparameter list
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentRunHyperparameters(String experimentRunId)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has attributes field. Add new attribute in that attribute List.
   *
   * @param experimentRunId
   * @param attribute has KeyValue.
   * @return ExperimentRun updated experimentRun.
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun logAttribute(String experimentRunId, KeyValue attribute)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has attributes field, Return List<KeyValue> attributes from ExperimentRun entity.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param experimentRunId
   * @return List<KeyValue> attribute list
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Return list of experimentRuns based on FindExperimentRuns queryParameters
   *
   * @param FindExperimentRuns queryParameters --> query parameters for filtering experimentRuns
   * @return List<ExperimentRun> -- return list of experimentRuns based on filter queryParameters
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> findExperimentRuns(FindExperimentRuns queryParameters)
      throws InvalidProtocolBufferException;

  /**
   * Return sorted list of experimentRuns based on SortExperimentRuns queryParameters
   *
   * @param SortExperimentRuns queryParameters --> query parameters for sorting experimentRuns
   * @return List<ExperimentRun> -- return list of experimentRuns based on sort queryParameters
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> sortExperimentRuns(SortExperimentRuns queryParameters)
      throws InvalidProtocolBufferException;

  /**
   * Return "Top n" (e.g. Top 5) experimentRuns after applying the sort queryParameters
   *
   * @param TopExperimentRunsSelector queryParameters --> query parameters for sorting and selecting
   *     "Top n" experimentRuns
   * @return List<ExperimentRun> -- return list of experimentRuns based on top selector
   *     queryParameters
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> getTopExperimentRuns(TopExperimentRunsSelector queryParameters)
      throws InvalidProtocolBufferException;

  /**
   * Fetch ExperimentRun Tags from database using experimentRunId.
   *
   * @param String experimentRunId
   * @return List<String> ExperimentRunTags.
   * @throws InvalidProtocolBufferException
   */
  List<String> getExperimentRunTags(String experimentRunId) throws InvalidProtocolBufferException;

  /**
   * Add attributes in database using experimentRunId.
   *
   * @param String experimentRunId
   * @param List<KeyValue> attributesList
   * @return ExperimentRun --> updated ExperimentRun entity
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun addExperimentRunAttributes(String experimentRunId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException;

  /**
   * Delete ExperimentRun Attributes in database using experimentRunId.
   *
   * @param Boolean deleteAll
   * @param List<String> attributeKeyList
   * @param String experimentRunId
   * @return ExperimentRun experimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun deleteExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Log JobId in ExperimentRun entity.
   *
   * @param String experimentRunId
   * @param String jobId
   * @return Experiment experimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun logJobId(String experimentRunId, String jobId)
      throws InvalidProtocolBufferException;

  /**
   * Get JobId from ExperimentRun entity.
   *
   * @param String experimentRunId
   * @return String jobId
   * @throws InvalidProtocolBufferException
   */
  String getJobId(String experimentRunId) throws InvalidProtocolBufferException;
}
