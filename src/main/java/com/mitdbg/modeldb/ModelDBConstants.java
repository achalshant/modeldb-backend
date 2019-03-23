package com.mitdbg.modeldb;

public interface ModelDBConstants {

  // Default Credential
  String DEFAULT_USERNAME = "vertaMDBAdmin";
  String DEFAULT_PASSWORD = "vertaMDBAdmin@123";

  // ArtifactStore constants
  String CLIENT_KEY = "client_key";
  String CLOUD_STORAGE_KEY = "cloud_storage_key";
  String CLOUD_STORAGE_FILE_PATH = "cloud_storage_file_path";
  String ENTITY_ID = "entity_id";
  String ENTITY_NAME = "entity_name";

  // Project Entity constants
  String ID = "id";
  String NAME = "name";
  String ATTRIBUTES = "attributes";
  String KEY = "key";
  String VALUE = "value";
  String TAGS = "tags";

  // Experiment Entity constants
  String PROJECT_ID = "project_id";

  // ExperimentRun Entity constants
  String EXPERIMENT_ID = "experiment_id";
  String OBSERVATIONS = "observations";
  String METRICS = "metrics";
  String DATASETS = "datasets";
  String ARTIFACTS = "artifacts";
  String HYPERPARAMETERS = "hyperparameters";
  String CLOUD_ARTIFACTS = "cloud_artifacts";
  String DATE_CREATED = "date_created";

  // Common constants
  String ORDER_ASC = "asc";
  String ORDER_DESC = "desc";
  String ADD = "add";
  String UPDATE = "update";
  String GET = "get";
  String DELETE = "delete";

  // job entity
  String JOB_ID = "job_id";
}
