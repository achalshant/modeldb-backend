package com.mitdbg.modeldb.artifactStore;

import com.google.protobuf.InvalidProtocolBufferException;

public interface ArtifactStoreDAO {

  /**
   * Keep clientKey, cloudStorageKey and cloud file path as a reference mapping in
   * ArtifactStoreMapping entity. this mapping use for get file from cloud, delete file from cloud.
   *
   * @param entityName --> this is collection name of entity whatever you define in config.yaml (Ex:
   *     project, experiment, experimentRun)
   * @param entityId --> Unique key of entity (Ex: projectId, experimentId, experimentRunId)
   * @param clientKey -- > key is send by user
   * @param cloudStorageKey -- > cloud storage key, ArtifactStore server generate this unique key
   *     using clientKey and UUID for cloud and ModelDB backend only store key mapping.
   * @param cloud_storage_file_path -- > cloud storage file path, after uploading file on cloud
   *     ArtifactStore server return cloud file path and ModelDB backend store path with mapping key
   *     in mongo.
   * @throws InvalidProtocolBufferException
   */
  void storeKeyMapping(
      String entityName,
      String entityId,
      String clientKey,
      String cloudStorageKey,
      String cloud_storage_file_path)
      throws InvalidProtocolBufferException;

  /**
   * Find mapping for given client key if mapping found then return cloud storage key of cloud
   * storage file. using that cloud storage key you can get file from cloud OR delete file from
   * cloud. if mapping not found then return null.
   *
   * @param entityName --> this is collection name of entity whatever you define in config.yaml (Ex:
   *     project, experiment, experimentRun)
   * @param entityId --> Unique key of entity (Ex: projectId, experimentId, experimentRunId)
   * @param clientKey -- > key is send by user
   * @return cloudStorageKey -- > this key identify cloud storage file.
   */
  String getCloudStorageKey(String entityName, String entityId, String clientKey);

  /**
   * Delete clientKey, cloudStorageKey reference mapping from database.
   *
   * @param cloudStorageKey --> cloud storage key is unique so we delete mapping using this key
   * @return status --> return true false base on delete status.
   */
  Boolean deleteStoreKeyMapping(String cloudStorageKey);
}
