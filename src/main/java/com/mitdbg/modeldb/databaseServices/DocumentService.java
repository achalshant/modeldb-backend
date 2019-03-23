package com.mitdbg.modeldb.databaseServices;

import java.util.List;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;

public interface DocumentService {

  /*
   * Check availability of given collection name in database, if is not exist then create collection
   * with given collection. This method called from each entity service Impl constructor.
   *
   * @param String collection
   */
  void checkCollectionAvailability(String collection);

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#insertOne(java.lang.String,
   * com.google.protobuf.MessageOrBuilder)
   */
  void insertOne(MessageOrBuilder object) throws InvalidProtocolBufferException;

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#find(java.lang.String)
   */
  List<?> find();

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#findListByKey(java.lang.String,
   * java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String)
   */
  List<?> findListByKey(
      String key, String value, Integer pageNumber, Integer pageLimit, String order, String sortBy);

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#findByKey(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  Object findByKey(String key, String value);

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#findByKey(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  List<?> findListByKey(String key, String value);

  /**
   * @param queryObj --> queryObj is used to build the where clause
   * @param projectionObj --> projectionObj is used to build selected fields e.g. get only IDs or
   *     get ID and Name
   * @param sortObj --> sortObj is used to build the sort criteria based on a key
   * @param recordLimit --> recordLimit slices the top number of records
   * @return List<?> --> return list of object based on given parameters
   */
  public List<?> findListByObject(
      Object queryObj, Object projectionObj, Object sortObj, Integer recordLimit);

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#findByObject(java.lang.Object)
   */
  Object findByObject(Object queryObj);

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#deleteOne(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  Boolean deleteOne(String collectionName, String key, String value);

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#updateOne(java.lang.String,
   * java.lang.String, java.lang.String, com.google.protobuf.MessageOrBuilder)
   */
  long updateOne(String key, String value, MessageOrBuilder newObject)
      throws InvalidProtocolBufferException;

  /*
   * @see com.mitdbg.modeldb.repository.PersistenceNoSQLService#updateOne(java.lang.String,
   * org.bson.Document, org.bson.Document)
   */
  long updateOne(Object queryObj, Object updateObj) throws InvalidProtocolBufferException;

  void insertOne(Object object) throws InvalidProtocolBufferException;

  /**
   * Get list of entity according to the specified aggregation pipeline in @param.
   *
   * @param List<Object> queryObj --> List of Aggregates documents
   * @return List<Object> --> Return list of entity base on aggregate query
   */
  List<?> findListByAggregateObject(List<?> queryObj);
}
