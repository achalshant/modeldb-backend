package com.mitdbg.modeldb;

import java.util.ArrayList;
import java.util.List;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.entities.ArtifactEntity;
import com.mitdbg.modeldb.entities.ExperimentEntity;
import com.mitdbg.modeldb.entities.ExperimentRunEntity;
import com.mitdbg.modeldb.entities.FeatureEntity;
import com.mitdbg.modeldb.entities.KeyValueEntity;
import com.mitdbg.modeldb.entities.ProjectEntity;
import com.mitdbg.modeldb.entities.TagsMapping;

public class RdbmsUtils {

  private RdbmsUtils() {}

  public static ProjectEntity convertFromProjectToProjectEntity(Project project)
      throws InvalidProtocolBufferException {
    return new ProjectEntity(project);
  }

  public static List<Project> convertFromProjectsToProjectEntityList(
      List<ProjectEntity> projectEntityList) throws InvalidProtocolBufferException {
    List<Project> projects = new ArrayList<>();
    if (projectEntityList != null) {
      for (ProjectEntity projectEntity : projectEntityList) {
        projects.add(projectEntity.getProtoObject());
      }
    }
    return projects;
  }

  public static ExperimentEntity convertFromExperimentToExperimentEntity(
      ProjectEntity projectEntity, Experiment experiment) throws InvalidProtocolBufferException {
    return new ExperimentEntity(projectEntity, experiment);
  }

  public static List<Experiment> convertFromExperimentsToExperimentEntityList(
      List<ExperimentEntity> experimentEntityList) throws InvalidProtocolBufferException {
    List<Experiment> experiments = new ArrayList<>();
    if (experimentEntityList != null) {
      for (ExperimentEntity experimentEntity : experimentEntityList) {
        experiments.add(experimentEntity.getProtoObject());
      }
    }
    return experiments;
  }

  public static List<ExperimentRun> convertFromExperimentRunsToExperimentRunEntityList(
      List<ExperimentRunEntity> experimentRunEntityList) throws InvalidProtocolBufferException {
    List<ExperimentRun> experimentRuns = new ArrayList<>();
    if (experimentRunEntityList != null) {
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntityList) {
        experimentRuns.add(experimentRunEntity.getProtoObject());
      }
    }
    return experimentRuns;
  }

  public static ExperimentRunEntity convertFromExperimentRunToExperimentRunEntity(
      ProjectEntity projectEntity, ExperimentEntity experimentEntity, ExperimentRun experimentRun)
      throws InvalidProtocolBufferException {
    return new ExperimentRunEntity(projectEntity, experimentEntity, experimentRun);
  }

  public static KeyValueEntity convertFromKeyValueToKeyValueEntity(
      Object entity, String fieldType, KeyValue keyValue) throws InvalidProtocolBufferException {
    return new KeyValueEntity(entity, fieldType, keyValue);
  }

  public static List<KeyValueEntity> convertFromKeyValuesToKeyValueEntityList(
      Object entity, String fieldType, List<KeyValue> keyValueList)
      throws InvalidProtocolBufferException {
    List<KeyValueEntity> attributeList = new ArrayList<>();
    if (keyValueList != null) {
      for (KeyValue keyValue : keyValueList) {
        KeyValueEntity keyValueEntity =
            convertFromKeyValueToKeyValueEntity(entity, fieldType, keyValue);
        attributeList.add(keyValueEntity);
      }
    }
    return attributeList;
  }

  public static List<KeyValue> convertFromKeyValueEntityListToKeyValues(
      List<KeyValueEntity> keyValueEntityList) throws InvalidProtocolBufferException {
    List<KeyValue> attributeList = new ArrayList<>();
    if (keyValueEntityList != null) {
      for (KeyValueEntity keyValue : keyValueEntityList) {
        attributeList.add(keyValue.getProtoKeyValue());
      }
    }
    return attributeList;
  }

  public static ArtifactEntity convertFromArtifactToArtifactEntity(
      Object entity, String fieldType, Artifact artifact) {
    return new ArtifactEntity(entity, fieldType, artifact);
  }

  public static List<ArtifactEntity> convertFromArtifactsToArtifactEntityList(
      Object entity, String fieldType, List<Artifact> artifactList) {
    List<ArtifactEntity> artifactEntityList = new ArrayList<>();
    if (artifactList != null) {
      for (Artifact artifact : artifactList) {
        ArtifactEntity artifactEntity =
            convertFromArtifactToArtifactEntity(entity, fieldType, artifact);
        artifactEntityList.add(artifactEntity);
      }
    }
    return artifactEntityList;
  }

  public static List<Artifact> convertFromArtifactEntityListToArtifacts(
      List<ArtifactEntity> artifactEntityList) {
    List<Artifact> artifactList = new ArrayList<>();
    if (artifactEntityList != null) {
      for (ArtifactEntity artifact : artifactEntityList) {
        artifactList.add(artifact.getProtoKeyValue());
      }
    }
    return artifactList;
  }

  public static List<TagsMapping> convertFromTagListToTagMappingList(
      Object entity, List<String> tagsList) {
    List<TagsMapping> tagsMappings = new ArrayList<>();
    if (tagsList != null) {
      for (String tag : tagsList) {
        TagsMapping tagsMapping = new TagsMapping(entity, tag);
        tagsMappings.add(tagsMapping);
      }
    }
    return tagsMappings;
  }

  public static List<String> convertFromTagsMappingListToTagList(List<TagsMapping> tagsMappings) {
    List<String> tags = new ArrayList<>();
    if (tagsMappings != null) {
      for (TagsMapping tagsMapping : tagsMappings) {
        tags.add(tagsMapping.getTag());
      }
    }
    return tags;
  }

  public static List<FeatureEntity> convertFromFeatureListToFeatureMappingList(
      Object entity, List<Feature> features) {
    List<FeatureEntity> featureEntities = new ArrayList<>();
    if (features != null) {
      for (Feature feature : features) {
        FeatureEntity featureEntity = new FeatureEntity(entity, feature);
        featureEntities.add(featureEntity);
      }
    }
    return featureEntities;
  }

  public static List<Feature> convertFromFeatureEntityListToFeatureList(
      List<FeatureEntity> featureEntities) {
    List<Feature> features = new ArrayList<>();
    if (featureEntities != null) {
      for (FeatureEntity featureEntity : featureEntities) {
        features.add(featureEntity.getProtoObject());
      }
    }
    return features;
  }
}
