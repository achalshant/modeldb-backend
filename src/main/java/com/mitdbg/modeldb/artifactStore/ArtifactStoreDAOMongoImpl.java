package com.mitdbg.modeldb.artifactStore;

import org.bson.Document;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.App;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.databaseServices.DocumentService;

public class ArtifactStoreDAOMongoImpl implements ArtifactStoreDAO {

  private DocumentService documentService = null;
  private String artifactStoreMappingEntity = null;

  public ArtifactStoreDAOMongoImpl(DocumentService documentService) {
    App app = App.getInstance();
    this.artifactStoreMappingEntity = app.getArtifactStoreMappingEntity();
    this.documentService = documentService;
    documentService.checkCollectionAvailability(artifactStoreMappingEntity);
  }

  /* (non-Javadoc)
   * @see com.mitdbg.modeldb.artifactStore.ArtifactStoreDAO#storeKeyMapping(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void storeKeyMapping(
      String entityName,
      String entityId,
      String clientKey,
      String cloudStorageKey,
      String cloud_storage_file_path)
      throws InvalidProtocolBufferException {

    Document queryExperimentRun = new Document();
    queryExperimentRun.append(ModelDBConstants.ENTITY_NAME, entityName);
    queryExperimentRun.append(ModelDBConstants.ENTITY_ID, entityId);
    queryExperimentRun.append(ModelDBConstants.CLIENT_KEY, clientKey);
    queryExperimentRun.append(ModelDBConstants.CLOUD_STORAGE_KEY, cloudStorageKey);
    queryExperimentRun.append(ModelDBConstants.CLOUD_STORAGE_FILE_PATH, cloud_storage_file_path);

    documentService.insertOne(queryExperimentRun);
  }

  /* (non-Javadoc)
   * @see com.mitdbg.modeldb.artifactStore.ArtifactStoreDAO#getCloudStorageKey(java.lang.String)
   */
  @Override
  public String getCloudStorageKey(String entityName, String entityId, String clientKey) {

    Document queryObj = new Document();
    queryObj.put(ModelDBConstants.ENTITY_NAME, entityName);
    queryObj.put(ModelDBConstants.ENTITY_ID, entityId);
    queryObj.put(ModelDBConstants.CLIENT_KEY, clientKey);
    Document document = (Document) documentService.findByObject(queryObj);
    if (document != null) {
      return document.getString(ModelDBConstants.CLOUD_STORAGE_KEY);
    }
    return null;
  }

  /* (non-Javadoc)
   * @see com.mitdbg.modeldb.artifactStore.ArtifactStoreDAO#deleteStoreKeyMapping(java.lang.String)
   */
  @Override
  public Boolean deleteStoreKeyMapping(String cloudStorageKey) {
    return documentService.deleteOne(
        artifactStoreMappingEntity, ModelDBConstants.CLOUD_STORAGE_KEY, cloudStorageKey);
  }
}
