package com.mitdbg.modeldb;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  JobTest.class,
  ProjectTest.class,
  ExperimentTest.class,
  ExperimentRunTest.class,
  ArtifactStoreTest.class,
  MongoInitTest.class
})
public class TestSequenceSuite {}
