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
import com.mitdbg.modeldb.Experiment;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.RdbmsUtils;

@Entity
@Table(name = "experiment")
public class ExperimentEntity {

  public ExperimentEntity() {}

  public ExperimentEntity(ProjectEntity projectEntity, Experiment experiment)
      throws InvalidProtocolBufferException {
    setId(experiment.getId());
    setProjectEntity(projectEntity);
    setName(experiment.getName());
    setDate_created(experiment.getDateCreated());
    setDate_updated(experiment.getDateUpdated());
    setDescription(experiment.getDescription());
    setAttributes(
        RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
            this, ModelDBConstants.ATTRIBUTES, experiment.getAttributesList()));
    setTags(RdbmsUtils.convertFromTagListToTagMappingList(this, experiment.getTagsList()));
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "project_id", nullable = false)
  private ProjectEntity projectEntity;

  @OneToMany(
      targetEntity = ExperimentRunEntity.class,
      mappedBy = "experimentEntity",
      cascade = CascadeType.ALL)
  private List<ExperimentRunEntity> experimentRuns;

  @Column(name = "name")
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "experimentEntity",
      cascade = CascadeType.ALL)
  private List<KeyValueEntity> attributes;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "experimentEntity",
      cascade = CascadeType.ALL)
  private List<TagsMapping> tags;

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

  public List<ExperimentRunEntity> getExperimentRuns() {
    return experimentRuns;
  }

  public void setExperimentRuns(List<ExperimentRunEntity> experimentRuns) {
    this.experimentRuns = experimentRuns;
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

  public List<KeyValueEntity> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<KeyValueEntity> attributes) {
    this.attributes = attributes;
  }

  public List<TagsMapping> getTags() {
    return tags;
  }

  public void setTags(List<TagsMapping> tagsMapping) {
    this.tags = tagsMapping;
  }

  public Experiment getProtoObject() throws InvalidProtocolBufferException {
    return Experiment.newBuilder()
        .setId(getId())
        .setProjectId(getProjectEntity().getId())
        .setName(getName())
        .setDescription(getDescription())
        .setDateCreated(getDate_created())
        .setDateUpdated(getDate_updated())
        .addAllAttributes(RdbmsUtils.convertFromKeyValueEntityListToKeyValues(getAttributes()))
        .addAllTags(RdbmsUtils.convertFromTagsMappingListToTagList(getTags()))
        .build();
  }
}
