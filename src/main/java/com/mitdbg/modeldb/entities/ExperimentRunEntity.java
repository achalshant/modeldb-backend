package com.mitdbg.modeldb.entities;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.ExperimentRun;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.RdbmsUtils;

@Entity
@Table(name = "experimentRun")
public class ExperimentRunEntity {

  public ExperimentRunEntity() {}

  public ExperimentRunEntity(
      ProjectEntity projectEntity, ExperimentEntity experimentEntity, ExperimentRun experimentRun)
      throws InvalidProtocolBufferException {
    setId(experimentRun.getId());
    setProjectEntity(projectEntity);
    setExperimentEntity(experimentEntity);
    setName(experimentRun.getName());
    setDescription(experimentRun.getDescription());
    setDate_created(experimentRun.getDateCreated());
    setDate_updated(experimentRun.getDateUpdated());
    setStart_time(experimentRun.getStartTime());
    setEnd_time(experimentRun.getEndTime());
    setCode_version(experimentRun.getCodeVersion());
    setTags(RdbmsUtils.convertFromTagListToTagMappingList(this, experimentRun.getTagsList()));
    setAttributes(
        RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
            this, ModelDBConstants.ATTRIBUTES, experimentRun.getAttributesList()));
    setHyperparameters(
        RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
            this, ModelDBConstants.HYPERPARAMETERS, experimentRun.getHyperparametersList()));
    setArtifacts(
        RdbmsUtils.convertFromArtifactsToArtifactEntityList(
            this, ModelDBConstants.ARTIFACTS, experimentRun.getArtifactsList()));
    setDatasets(
        RdbmsUtils.convertFromArtifactsToArtifactEntityList(
            this, ModelDBConstants.DATASETS, experimentRun.getDatasetsList()));
    setMetrics(
        RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
            this, ModelDBConstants.METRICS, experimentRun.getMetricsList()));
    /*.addAllObservations(values)*/
    setFeatures(
        RdbmsUtils.convertFromFeatureListToFeatureMappingList(
            this, experimentRun.getFeaturesList()));
    setJob_id(experimentRun.getJobId());
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity projectEntity;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "experiment_id", nullable = false)
  private ExperimentEntity experimentEntity;

  @Column(name = "name")
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @Column(name = "start_time")
  private Long start_time;

  @Column(name = "end_time")
  private Long end_time;

  @Column(name = "code_version")
  private String code_version;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<TagsMapping> tags;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<KeyValueEntity> attributes;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<KeyValueEntity> hyperparameters;

  @OneToMany(
      targetEntity = ArtifactEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<ArtifactEntity> artifacts;

  @OneToMany(
      targetEntity = ArtifactEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<ArtifactEntity> datasets;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<KeyValueEntity> metrics;

  // repeated Observation observations = 26;

  @OneToMany(
      targetEntity = FeatureEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<FeatureEntity> features;

  @OneToMany(
      targetEntity = ArtifactEntity.class,
      mappedBy = "experimentRunEntity",
      cascade = CascadeType.ALL)
  private List<ArtifactEntity> cloudArtifacts;

  @Column(name = "job_id")
  private String job_id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ProjectEntity getProjectEntity() {
    return projectEntity;
  }

  public void setProjectEntity(ProjectEntity projectEntity) {
    this.projectEntity = projectEntity;
  }

  public ExperimentEntity getExperimentEntity() {
    return experimentEntity;
  }

  public void setExperimentEntity(ExperimentEntity experimentEntity) {
    this.experimentEntity = experimentEntity;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getDate_created() {
    return date_created;
  }

  public void setDate_created(Long dateCreated) {
    this.date_created = dateCreated;
  }

  public Long getDate_updated() {
    return date_updated;
  }

  public void setDate_updated(Long dateUpdated) {
    this.date_updated = dateUpdated;
  }

  public Long getStart_time() {
    return start_time;
  }

  public void setStart_time(Long startTime) {
    this.start_time = startTime;
  }

  public Long getEnd_time() {
    return end_time;
  }

  public void setEnd_time(Long endTime) {
    this.end_time = endTime;
  }

  public String getCode_version() {
    return code_version;
  }

  public void setCode_version(String codeVersion) {
    this.code_version = codeVersion;
  }

  public List<TagsMapping> getTags() {
    return tags;
  }

  public void setTags(List<TagsMapping> tags) {
    this.tags = tags;
  }

  public List<KeyValueEntity> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<KeyValueEntity> attributes) {
    this.attributes = attributes;
  }

  public List<KeyValueEntity> getHyperparameters() {
    return hyperparameters;
  }

  public void setHyperparameters(List<KeyValueEntity> hyperparameters) {
    this.hyperparameters = hyperparameters;
  }

  public List<ArtifactEntity> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<ArtifactEntity> artifacts) {
    this.artifacts = artifacts;
  }

  public List<ArtifactEntity> getDatasets() {
    return datasets;
  }

  public void setDatasets(List<ArtifactEntity> datasets) {
    this.datasets = datasets;
  }

  public List<KeyValueEntity> getMetrics() {
    return metrics;
  }

  public void setMetrics(List<KeyValueEntity> metrics) {
    this.metrics = metrics;
  }

  public List<FeatureEntity> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureEntity> features) {
    this.features = features;
  }

  public List<ArtifactEntity> getCloudArtifacts() {
    return cloudArtifacts;
  }

  public void setCloudArtifacts(List<ArtifactEntity> cloudArtifacts) {
    this.cloudArtifacts = cloudArtifacts;
  }

  public String getJob_id() {
    return job_id;
  }

  public void setJob_id(String jobId) {
    this.job_id = jobId;
  }

  public ExperimentRun getProtoObject() throws InvalidProtocolBufferException {
    return ExperimentRun.newBuilder()
        .setId(getId())
        .setProjectId(getProjectEntity().getId())
        .setExperimentId(getExperimentEntity().getId())
        .setName(getName())
        .setDescription(getDescription())
        .setDateCreated(getDate_created())
        .setDateUpdated(getDate_updated())
        .setStartTime(getStart_time())
        .setEndTime(getEnd_time())
        .setCodeVersion(getCode_version())
        .addAllTags(RdbmsUtils.convertFromTagsMappingListToTagList(getTags()))
        .addAllAttributes(RdbmsUtils.convertFromKeyValueEntityListToKeyValues(getAttributes()))
        .addAllHyperparameters(
            RdbmsUtils.convertFromKeyValueEntityListToKeyValues(getHyperparameters()))
        .addAllArtifacts(RdbmsUtils.convertFromArtifactEntityListToArtifacts(getArtifacts()))
        .addAllDatasets(RdbmsUtils.convertFromArtifactEntityListToArtifacts(getDatasets()))
        .addAllMetrics(RdbmsUtils.convertFromKeyValueEntityListToKeyValues(getMetrics()))
        /*.addAllObservations(values)*/
        .addAllFeatures(RdbmsUtils.convertFromFeatureEntityListToFeatureList(getFeatures()))
        .addAllTags(RdbmsUtils.convertFromTagsMappingListToTagList(getTags()))
        .setJobId(getJob_id())
        .build();
  }
}
