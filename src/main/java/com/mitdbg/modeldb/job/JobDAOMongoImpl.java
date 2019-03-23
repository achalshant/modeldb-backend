package com.mitdbg.modeldb.job;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mitdbg.modeldb.App;
import com.mitdbg.modeldb.Job;
import com.mitdbg.modeldb.JobStatusEnum.JobStatus;
import com.mitdbg.modeldb.ModelDBConstants;
import com.mitdbg.modeldb.databaseServices.DocumentService;
import io.grpc.protobuf.StatusProto;

public class JobDAOMongoImpl implements JobDAO {

  private static final Logger LOGGER = Logger.getLogger(JobDAOMongoImpl.class.getName());
  private String jobEntity = null;
  DocumentService documentService = null;

  public JobDAOMongoImpl(DocumentService documentService) {
    App app = App.getInstance();
    this.jobEntity = app.getJobEntity();
    this.documentService = documentService;
    documentService.checkCollectionAvailability(jobEntity);
  }

  /**
   * Convert list of MongoDB document object to list of Job entity object.
   *
   * @param List<Document> documents : list of MongoDB data object.
   * @return List<Job> : Job entity List
   * @throws InvalidProtocolBufferException
   */
  private List<Job> getJobFromDocuments(List<Document> documents)
      throws InvalidProtocolBufferException {
    List<Job> jobList = new ArrayList<>();
    for (Document document : documents) {
      Job job = convertJobFromDocument(document);
      jobList.add(job);
    }
    return jobList;
  }

  /**
   * Convert MongoDB Document object to Job entity. Here remove MongoDB "_ID" because if _id is
   * their then both the structure MongoDB Document and Job Entity is different and direct
   * conversion is not possible that's why "_ID" is remove.
   *
   * @param Document document : MongoDB data object.
   * @return Job job : ProtocolBuffer Object of Job entity.
   * @throws InvalidProtocolBufferException
   */
  private Job convertJobFromDocument(Document document) throws InvalidProtocolBufferException {
    Job.Builder builder = Job.newBuilder();
    document.remove("_id");
    JsonFormat.parser().merge(document.toJson(), builder);
    return builder.build();
  }

  @Override
  public Job insertJob(Job job) throws InvalidProtocolBufferException {
    documentService.insertOne(job);
    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, job.getId());
    return convertJobFromDocument(document);
  }

  @Override
  public Job getJob(String entityFieldKey, String entityFieldValue)
      throws InvalidProtocolBufferException {
    Document document = (Document) documentService.findByKey(entityFieldKey, entityFieldValue);
    return convertJobFromDocument(document);
  }

  @Override
  public Boolean deleteJob(String jobId) {
    return documentService.deleteOne(jobEntity, ModelDBConstants.ID, jobId);
  }

  @Override
  public Job updateJob(String jobId, JobStatus jobStatus, String endTime)
      throws InvalidProtocolBufferException {
    Document document = (Document) documentService.findByKey(ModelDBConstants.ID, jobId);
    Job existingJob = convertJobFromDocument(document);
    existingJob = existingJob.toBuilder().setEndTime(endTime).setJobStatus(jobStatus).build();
    Long updatedCount =
        documentService.updateOne(ModelDBConstants.ID, existingJob.getId(), existingJob);
    if (updatedCount > 0) {
      return existingJob;
    } else {
      String errorMessage = "Added Job status or endTime is already present in Job";
      LOGGER.log(Level.WARNING, errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.ALREADY_EXISTS_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }
}
