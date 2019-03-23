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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.Value.Builder;
import com.mitdbg.modeldb.KeyValue;
import com.mitdbg.modeldb.ModelDBUtils;

@Entity
@Table(name = "keyvalue")
public class KeyValueEntity {

  public KeyValueEntity() {}

  public KeyValueEntity(Object entity, String fieldType, KeyValue keyValue)
      throws InvalidProtocolBufferException {
    setKey(keyValue.getKey());
    setValue(ModelDBUtils.getStringFromProtoObject(keyValue.getValue()));
    setValue_type(keyValue.getValueTypeValue());

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

  @Column(name = "kv_key", columnDefinition = "TEXT")
  private String key;

  @Column(name = "kv_value", columnDefinition = "TEXT")
  private String value;

  @Column(name = "value_type")
  private Integer value_type;

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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getValue_type() {
    return value_type;
  }

  public void setValue_type(Integer valueType) {
    this.value_type = valueType;
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

  private void setExperimentRunEntity(Object entity) {
    this.experimentRunEntity = (ExperimentRunEntity) entity;
    this.entityName = this.experimentRunEntity.getClass().getSimpleName();
  }

  public KeyValue getProtoKeyValue() throws InvalidProtocolBufferException {
    Value.Builder valueBuilder = Value.newBuilder();
    valueBuilder = (Builder) ModelDBUtils.getProtoObjectFromString(getValue(), valueBuilder);
    return KeyValue.newBuilder()
        .setKey(getKey())
        .setValue(valueBuilder.build())
        .setValueTypeValue(getValue_type())
        .build();
  }
}
