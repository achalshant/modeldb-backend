package com.mitdbg.modeldb.entities;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.Project;
import com.mitdbg.modeldb.RdbmsUtils;

@Entity
@Table(name = "project")
public class ProjectEntity {

  public ProjectEntity() {}

  public ProjectEntity(Project project) throws InvalidProtocolBufferException {
    setId(project.getId());
    setName(project.getName());
    setDate_created(project.getDateCreated());
    setDate_updated(project.getDateUpdated());
    setDescription(project.getDescription());
    setAttributes(
        RdbmsUtils.convertFromKeyValuesToKeyValueEntityList(
            this, ModelDBConstants.ATTRIBUTES, project.getAttributesList()));
    setTags(RdbmsUtils.convertFromTagListToTagMappingList(this, project.getTagsList()));
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @OneToMany(
      targetEntity = ExperimentEntity.class,
      mappedBy = "projectEntity",
      cascade = CascadeType.ALL)
  private List<ExperimentEntity> experiments;

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
      mappedBy = "projectEntity",
      cascade = CascadeType.ALL)
  private List<KeyValueEntity> attributes;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "projectEntity",
      cascade = CascadeType.ALL)
  private List<TagsMapping> tags;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<ExperimentEntity> getExperiments() {
    return experiments;
  }

  public void setExperiments(List<ExperimentEntity> experiments) {
    this.experiments = experiments;
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

  public void setTags(List<TagsMapping> tags) {
    this.tags = tags;
  }

  public Project getProtoObject() throws InvalidProtocolBufferException {
    return Project.newBuilder()
        .setId(getId())
        .setName(getName())
        .setDescription(getDescription())
        .setDateCreated(getDate_created())
        .setDateUpdated(getDate_updated())
        .addAllAttributes(RdbmsUtils.convertFromKeyValueEntityListToKeyValues(getAttributes()))
        .addAllTags(RdbmsUtils.convertFromTagsMappingListToTagList(getTags()))
        .build();
  }
}
