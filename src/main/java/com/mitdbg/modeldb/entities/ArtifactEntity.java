package com.mitdbg.modeldb.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import com.mitdbg.modeldb.Artifact;

@Entity
@Table(name = "artifact")
public class ArtifactEntity {

  public ArtifactEntity() {}

  public ArtifactEntity(Object entity, String fieldType, Artifact artifact) {
    setKey(artifact.getKey());
    setPath(artifact.getPath());
    setArtifact_type(artifact.getArtifactTypeValue());

    if (entity instanceof ProjectEntity) {
      setProjectEntity(entity);
    } else if (entity instanceof ExperimentEntity) {
      setExperimentEntity(entity);
    } else if (entity instanceof ExperimentRunEntity) {
      setExperimentRunEntity(entity);
    }

    this.fieldType = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "ar_key", columnDefinition = "TEXT")
  private String key;

  @Column(name = "ar_path", columnDefinition = "TEXT")
  private String path;

  @Column(name = "artifact_type")
  private Integer artifact_type;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "project_id", nullable = true)
  private ProjectEntity projectEntity;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "experiment_id", nullable = true)
  private ExperimentEntity experimentEntity;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "experiment_run_id", nullable = true)
  private ExperimentRunEntity experimentRunEntity;

  @Column(name = "entity_name", length = 50)
  private String entityName;

  @Column(name = "field_type", length = 50)
  private String fieldType;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Integer getArtifact_type() {
    return artifact_type;
  }

  public void setArtifact_type(Integer artifactType) {
    this.artifact_type = artifactType;
  }

  public ProjectEntity getProjectEntity() {
    return projectEntity;
  }

  private void setProjectEntity(Object entity) {
    this.projectEntity = (ProjectEntity) entity;
    this.entityName = this.projectEntity.getClass().getSimpleName();
  }

  public ExperimentEntity getExperimentEntity() {
    return experimentEntity;
  }

  private void setExperimentEntity(Object entity) {
    this.experimentEntity = (ExperimentEntity) entity;
    this.entityName = this.experimentEntity.getClass().getSimpleName();
  }

  public ExperimentRunEntity getExperimentRunEntity() {
    return experimentRunEntity;
  }

  private void setExperimentRunEntity(Object entity) {
    this.experimentRunEntity = (ExperimentRunEntity) entity;
    this.entityName = this.experimentRunEntity.getClass().getSimpleName();
  }

  public String getFieldType() {
    return fieldType;
  }

  public void setFieldType(String fieldType) {
    this.fieldType = fieldType;
  }

  public Artifact getProtoKeyValue() {
    return Artifact.newBuilder()
        .setKey(getKey())
        .setPath(getPath())
        .setArtifactTypeValue(getArtifact_type())
        .build();
  }
}
