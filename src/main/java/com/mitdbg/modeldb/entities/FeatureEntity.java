package com.mitdbg.modeldb.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.Feature;

@Entity
@Table(name = "Feature")
public class FeatureEntity {

  public FeatureEntity() {}

  public FeatureEntity(Object entity, Feature feature) {
    setName(feature.getName());

    if (entity instanceof ProjectEntity) {
      setProjectEntity(entity);
    } else if (entity instanceof ExperimentEntity) {
      setExperimentEntity(entity);
    } else if (entity instanceof ExperimentRunEntity) {
      setExperimentRunEntity(entity);
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "feature", columnDefinition = "TEXT")
  private String name;

  @ManyToOne
  @JoinColumn(name = "project_id", nullable = true)
  private ProjectEntity projectEntity;

  @ManyToOne
  @JoinColumn(name = "experiment_id", nullable = true)
  private ExperimentEntity experimentEntity;

  @ManyToOne
  @JoinColumn(name = "experiment_run_id", nullable = true)
  private ExperimentRunEntity experimentRunEntity;

  @Column(name = "entity_name", length = 50)
  private String entityName;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  private void setExperimentEntity(Object experimentEntity) {
    this.experimentEntity = (ExperimentEntity) experimentEntity;
    this.entityName = this.experimentEntity.getClass().getSimpleName();
  }

  public ExperimentRunEntity getExperimentRunEntity() {
    return experimentRunEntity;
  }

  private void setExperimentRunEntity(Object experimentRunEntity) {
    this.experimentRunEntity = (ExperimentRunEntity) experimentRunEntity;
    this.entityName = this.experimentRunEntity.getClass().getSimpleName();
  }

  public Feature getProtoObject() {
    return Feature.newBuilder().setName(getName()).build();
  }
}
