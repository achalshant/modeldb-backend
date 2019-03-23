package com.mitdbg.modeldb.job;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.Job;
import com.mitdbg.modeldb.JobStatusEnum.JobStatus;

public interface JobDAO {

  /**
   * Insert Job entity in database.
   *
   * @param Job job
   * @return Job insertedJob
   * @throws InvalidProtocolBufferException
   */
  Job insertJob(Job job) throws InvalidProtocolBufferException;

  /**
   * Get Job entity using given jobId from database.
   *
   * @param entityFieldKey --> key like ModelDBConstants.ID, ModelDBConstants.NAME etc.
   * @param entityFieldValue --> value of key like job.id etc.
   * @return Job job
   * @throws InvalidProtocolBufferException
   */
  Job getJob(String entityFieldKey, String entityFieldValue) throws InvalidProtocolBufferException;

  /**
   * Delete the Job from database using jobId.
   *
   * @param String jobId
   * @return Boolean updated status
   */
  Boolean deleteJob(String jobId);

  /**
   * Update Job entity(jobStatus, endTime) in database using jobId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param String jobId
   * @param String jobStatus
   * @param String endTime
   * @return Job updatedJob
   * @throws InvalidProtocolBufferException
   */
  Job updateJob(String jobId, JobStatus jobStatus, String endTime)
      throws InvalidProtocolBufferException;
}
