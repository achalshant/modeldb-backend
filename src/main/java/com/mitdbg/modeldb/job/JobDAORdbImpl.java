package com.mitdbg.modeldb.job;

import java.util.logging.Logger;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mitdbg.modeldb.App;
import com.mitdbg.modeldb.Job;
import com.mitdbg.modeldb.JobStatusEnum.JobStatus;

public class JobDAORdbImpl implements JobDAO {

  private static final Logger LOGGER = Logger.getLogger(JobDAORdbImpl.class.getName());
  private String jobEntity = null;

  public JobDAORdbImpl() {
    App app = App.getInstance();
    this.jobEntity = app.getJobEntity();
  }

  @Override
  public Job insertJob(Job job) throws InvalidProtocolBufferException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Job getJob(String entityFieldKey, String entityFieldValue)
      throws InvalidProtocolBufferException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Boolean deleteJob(String jobId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Job updateJob(String jobId, JobStatus jobStatus, String endTime)
      throws InvalidProtocolBufferException {
    // TODO Auto-generated method stub
    return null;
  }
}
