package com.mitdbg.modeldb.artifactStore;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.App;

public class ArtifactStoreDAORdbImpl implements ArtifactStoreDAO {

  private String artifactStoreMappingEntity = null;

  public ArtifactStoreDAORdbImpl() {
    App app = App.getInstance();
    this.artifactStoreMappingEntity = app.getArtifactStoreMappingEntity();
  }

  @Override
  public void storeKeyMapping(
      String entityName,
      String entityId,
      String clientKey,
      String cloudStorageKey,
      String cloudStorageFilePath)
      throws InvalidProtocolBufferException {
    // TODO Auto-generated method stub

  }

  @Override
  public String getCloudStorageKey(String entityName, String entityId, String clientKey) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Boolean deleteStoreKeyMapping(String cloudStorageKey) {
    // TODO Auto-generated method stub
    return null;
  }
}
